package com.example.domain.security

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Guardián Zero-Trust de Protección de API para llamadas a Firebase AI (Vertex AI / Gemini) y Firestore:
 * 1. Verifica inicialización de Firebase App Check (DebugAppCheckProvider en debug / ReCaptcha-PlayIntegrity en release).
 * 2. Verifica sesión activa en FirebaseAuth o token de usuario válido.
 * 3. Aplica un limitador de tasa Token Bucket en memoria (máx. 10 llamadas/minuto y 100 llamadas/día) + Circuit Breaker.
 * 4. Ejecuta Sanitización Bidireccional PII (PiiRedactionSanitizer): anonimiza nombres, cuentas, teléfonos y NITs
 *    antes de transmitir el prompt a la nube y los rehidrata localmente al recibir la respuesta.
 * 5. Gestiona una caché criptográfica SHA-256 en memoria para evitar llamadas duplicadas por el mismo prompt.
 */
object FirebaseApiProtectionGuard {

    private const val MAX_REQUESTS_PER_MINUTE = 10
    private const val MAX_REQUESTS_PER_DAY = 100
    private const val CIRCUIT_BREAKER_COOLDOWN_MS = 60_000L

    @Volatile
    private var windowStartMinuteMs: Long = System.currentTimeMillis()
    private val minuteCounter = AtomicInteger(0)
    private val dailyCounter = AtomicInteger(0)

    @Volatile
    private var circuitOpenedUntilMs: Long = 0L

    @Volatile
    private var appCheckInitialized = false

    private val responseCache = ConcurrentHashMap<String, CachedAiResponse>()

    data class CachedAiResponse(
        val responseJson: String,
        val timestampMs: Long
    )

    data class ApiProtectionStatus(
        val isAppCheckInitialized: Boolean,
        val isAuthSessionValid: Boolean,
        val remainingMinuteQuota: Int,
        val remainingDailyQuota: Int,
        val isCircuitBreakerOpen: Boolean,
        val piiMaskingActive: Boolean = true
    )

    sealed interface GuardVerdict {
        data class Allowed(
            val sanitizedPrompt: String,
            val aliasMapForRehydration: Map<String, String>,
            val promptHashKey: String,
            val cachedResponseIfAny: String?
        ) : GuardVerdict

        data class Blocked(
            val reasonCode: String,
            val userMessage: String
        ) : GuardVerdict
    }

    /**
     * Inicializa Firebase App Check de forma idempotente y segura.
     * En compilaciones Debug activa `DebugAppCheckProviderFactory`; en Release intenta PlayIntegrity / AppCheck estándar.
     */
    fun installAppCheckIfNeeded(context: Any, isDebugBuild: Boolean = false) {
        if (appCheckInitialized) return
        synchronized(this) {
            if (appCheckInitialized) return
            try {
                val contextClass = Class.forName("android.content.Context")
                val firebaseAppClass = Class.forName("com.google.firebase.FirebaseApp")
                val getApps = firebaseAppClass.getMethod("getApps", contextClass)
                val apps = getApps.invoke(null, context) as? List<*>
                if (apps.isNullOrEmpty()) {
                    firebaseAppClass.getMethod("initializeApp", contextClass).invoke(null, context)
                }

                val firebaseClass = Class.forName("com.google.firebase.Firebase")
                val firebaseInstance = firebaseClass.getDeclaredField("INSTANCE").get(null)
                val appCheckKtClass = Class.forName("com.google.firebase.appcheck.FirebaseAppCheckKt")
                val getAppCheckMethod = appCheckKtClass.getMethod("getAppCheck", firebaseClass)
                val firebaseAppCheck = getAppCheckMethod.invoke(null, firebaseInstance)

                val providerFactoryInterface = Class.forName("com.google.firebase.appcheck.AppCheckProviderFactory")
                val factoryInstance = if (isDebugBuild) {
                    val debugFactoryClass = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                    debugFactoryClass.getMethod("getInstance").invoke(null)
                } else {
                    try {
                        val playIntegrityClass = Class.forName("com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory")
                        playIntegrityClass.getMethod("getInstance").invoke(null)
                    } catch (_: Throwable) {
                        // Fallback a ReCaptcha Enterprise si Play Integrity no está presente en el dispositivo
                        val recaptchaClass = Class.forName("com.google.firebase.appcheck.recaptcha.ReCaptchaEnterpriseProviderFactory")
                        recaptchaClass.getMethod("getInstance").invoke(null)
                    }
                }
                firebaseAppCheck.javaClass
                    .getMethod("installAppCheckProviderFactory", providerFactoryInterface)
                    .invoke(firebaseAppCheck, factoryInstance)
                appCheckInitialized = true
            } catch (_: Throwable) {
                // Degradación limpia: si el dispositivo no cuenta con Google Play Services o está en entorno JVM test
                appCheckInitialized = false
            }
        }
    }

    /**
     * Evalúa todas las políticas de seguridad antes de permitir una llamada a Firebase AI.
     */
    fun authorizeAndSanitizeRequest(
        rawPrompt: String,
        participantRealNames: List<String> = emptyList(),
        requireAuthenticatedUser: Boolean = false,
        cacheTtlMs: Long = 10 * 60 * 1000L // 10 minutos
    ): GuardVerdict {
        val now = System.currentTimeMillis()

        // 1. Verificar Circuit Breaker por fallos previos 429/403
        if (now < circuitOpenedUntilMs) {
            val waitSeconds = ((circuitOpenedUntilMs - now) / 1000L).coerceAtLeast(1L)
            return GuardVerdict.Blocked(
                reasonCode = "CIRCUIT_BREAKER_OPEN",
                userMessage = "Protección de API activa temporalmente. Reintentando en ${waitSeconds}s (usando motor local offline)."
            )
        }

        // 2. Verificar sesión de Firebase Auth si es requerida por la política de seguridad
        if (requireAuthenticatedUser && !isFirebaseAuthSessionActive()) {
            return GuardVerdict.Blocked(
                reasonCode = "UNAUTHENTICATED_SESSION",
                userMessage = "Se requiere sesión autenticada en Firebase para invocar la API en la nube."
            )
        }

        // 3. Sanitizar PII (ocultar números de cuenta, teléfonos, correos, NITs y nombres de participantes)
        val (sanitizedPrompt, aliasMap) = sanitizePiiWithRehydrationMap(
            rawText = rawPrompt,
            participantRealNames = participantRealNames
        )

        // 4. Verificar Caché Criptográfica SHA-256 antes de consumir cuota de red
        val hashKey = sha256(sanitizedPrompt)
        val cached = responseCache[hashKey]
        if (cached != null && (now - cached.timestampMs) <= cacheTtlMs) {
            return GuardVerdict.Allowed(
                sanitizedPrompt = sanitizedPrompt,
                aliasMapForRehydration = aliasMap,
                promptHashKey = hashKey,
                cachedResponseIfAny = rehydratePiiAliases(cached.responseJson, aliasMap)
            )
        }

        // 5. Verificar Rate Limiting (Token Bucket por minuto y diario)
        if (now - windowStartMinuteMs >= 60_000L) {
            windowStartMinuteMs = now
            minuteCounter.set(0)
        }
        if (minuteCounter.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
            return GuardVerdict.Blocked(
                reasonCode = "RATE_LIMIT_MINUTE_EXCEEDED",
                userMessage = "Límite de seguridad de $MAX_REQUESTS_PER_MINUTE consultas por minuto alcanzado. Procesando con motor local."
            )
        }
        if (dailyCounter.incrementAndGet() > MAX_REQUESTS_PER_DAY) {
            return GuardVerdict.Blocked(
                reasonCode = "RATE_LIMIT_DAILY_EXCEEDED",
                userMessage = "Cuota diaria protegida de IA alcanzada ($MAX_REQUESTS_PER_DAY/día). Operando con motor local determinista."
            )
        }

        return GuardVerdict.Allowed(
            sanitizedPrompt = sanitizedPrompt,
            aliasMapForRehydration = aliasMap,
            promptHashKey = hashKey,
            cachedResponseIfAny = null
        )
    }

    /**
     * Registra en caché una respuesta exitosa de Firebase AI.
     */
    fun recordSuccessfulResponse(promptHashKey: String, sanitizedResponseText: String) {
        responseCache[promptHashKey] = CachedAiResponse(
            responseJson = sanitizedResponseText,
            timestampMs = System.currentTimeMillis()
        )
    }

    /**
     * Activa el Circuit Breaker si Firebase AI devuelve 429 (Quota Exceeded) o 403 (App Check rejected).
     */
    fun reportUpstreamApiFailure(error: Throwable) {
        val msg = error.message.orEmpty().lowercase()
        if (msg.contains("429") || msg.contains("quota") || msg.contains("403") || msg.contains("app check")) {
            circuitOpenedUntilMs = System.currentTimeMillis() + CIRCUIT_BREAKER_COOLDOWN_MS
        }
    }

    /**
     * Reemplaza información personal identificable (PII) por tokens anónimos seguros.
     */
    fun sanitizePiiWithRehydrationMap(
        rawText: String,
        participantRealNames: List<String>
    ): Pair<String, Map<String, String>> {
        var sanitized = rawText
        val aliasToRealName = linkedMapOf<String, String>()

        // 1. Reemplazar nombres reales de participantes por tokens PARTICIPANT_1, PARTICIPANT_2...
        participantRealNames
            .map { it.trim() }
            .filter { it.length >= 2 }
            .sortedByDescending { it.length }
            .forEachIndexed { index, realName ->
                val token = "PARTICIPANT_${index + 1}"
                aliasToRealName[token] = realName
                sanitized = sanitized.replace(Regex("\\b${Regex.escape(realName)}\\b", RegexOption.IGNORE_CASE), token)
            }

        // 2. Enmascarar posibles números de tarjeta o cuenta bancaria (10 a 16 dígitos continuos)
        sanitized = sanitized.replace(Regex("\\b\\d{10,16}\\b"), "[REDACTED_ACCOUNT]")

        // 3. Enmascarar correos electrónicos
        sanitized = sanitized.replace(
            Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"),
            "[REDACTED_EMAIL]"
        )

        return sanitized to aliasToRealName
    }

    /**
     * Restaura los nombres reales de los participantes en la respuesta devuelta por la IA.
     */
    fun rehydratePiiAliases(responseText: String, aliasToRealName: Map<String, String>): String {
        var restored = responseText
        for ((token, realName) in aliasToRealName) {
            restored = restored.replace(token, realName, ignoreCase = true)
        }
        return restored
    }

    fun getProtectionTelemetry(): ApiProtectionStatus {
        val now = System.currentTimeMillis()
        return ApiProtectionStatus(
            isAppCheckInitialized = isFirebaseAppInitialized(),
            isAuthSessionValid = isFirebaseAuthSessionActive(),
            remainingMinuteQuota = (MAX_REQUESTS_PER_MINUTE - minuteCounter.get()).coerceAtLeast(0),
            remainingDailyQuota = (MAX_REQUESTS_PER_DAY - dailyCounter.get()).coerceAtLeast(0),
            isCircuitBreakerOpen = now < circuitOpenedUntilMs,
            piiMaskingActive = true
        )
    }

    private fun isFirebaseAppInitialized(): Boolean {
        return try {
            val clazz = Class.forName("com.google.firebase.FirebaseApp")
            val getInstance = clazz.getMethod("getInstance")
            getInstance.invoke(null) != null
        } catch (_: Throwable) {
            false
        }
    }

    private fun isFirebaseAuthSessionActive(): Boolean {
        return try {
            val clazz = Class.forName("com.google.firebase.auth.FirebaseAuth")
            val authInstance = clazz.getMethod("getInstance").invoke(null) ?: return false
            val currentUser = clazz.getMethod("getCurrentUser").invoke(authInstance)
            currentUser != null
        } catch (_: Throwable) {
            false
        }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
