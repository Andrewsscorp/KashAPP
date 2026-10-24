package com.example.features.settings.security

import android.app.Activity
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import com.example.data.datasource.BiometricDataSource
import com.example.data.local.DatabaseEncryptionInfo
import com.example.data.local.KeystoreCryptoManager
import com.example.data.local.PassphraseManager
import com.example.data.local.PassphraseStrength
import com.example.data.local.dao.AuditSecurityLogDao
import com.example.data.local.dao.UserSessionDao
import com.example.data.local.entity.AuditSecurityLog
import com.example.data.local.entity.UserSessionEntity
import com.example.domain.models.SecurityConfig
import com.example.domain.repository.AuthRepository
import com.example.features.settings.domain.repository.SettingsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Representación real de una sesión activa en el dispositivo físico o sincronizada en base de datos.
 */
data class RealDeviceSessionItem(
    val sessionId: String,
    val userId: String,
    val deviceModelLabel: String,
    val osVersionLabel: String,
    val authProviderLabel: String,
    val lastActiveEpochMs: Long,
    val isCurrentDevice: Boolean
)

/**
 * Coordinador empresarial de Seguridad de Hardware, PIN PBKDF2, SQLCipher AES-256 y Sesiones Reales.
 * Conecta `SettingsSecurityScreen` con:
 * - `BiometricDataSource` (`BIOMETRIC_STRONG` + `CryptoObject` respaldado por Android Keystore)
 * - `WindowManager.LayoutParams.FLAG_SECURE` (bloqueo inmediato de capturas y grabación de pantalla)
 * - `PassphraseManager` (SQLCipher 4.6.0 AES-256 + PBKDF2-HMAC-SHA256 310.000 iteraciones OWASP)
 * - `AuthRepository` (PIN PBKDF2 con bloqueo exponencial anti-fuerza bruta)
 * - `UserSessionDao`, `AuditSecurityLogDao` y `FirebaseFirestore` (`users/{uid}/active_devices`)
 */
class SecurityCenterCoordinator(
    private val context: Context,
    private val authRepository: AuthRepository?,
    private val settingsRepository: SettingsRepository?,
    private val userSessionDao: UserSessionDao?,
    private val auditSecurityLogDao: AuditSecurityLogDao?,
    private val keystoreCryptoManager: KeystoreCryptoManager = KeystoreCryptoManager(allowSoftwareFallbackForTests = true),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val biometricDataSource = BiometricDataSource(context, keystoreCryptoManager)
    private val passphraseManager = PassphraseManager.getInstance(context).also {
        it.setAuditSecurityLogDao(auditSecurityLogDao)
    }

    /**
     * Identificador único del hardware actual (Android ID) y nombre real del teléfono físico.
     */
    val currentDeviceId: String by lazy {
        runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "android_local_device"
    }

    val currentHardwareModelName: String by lazy {
        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        val fullName = if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model".trim()
        }
        fullName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            .ifBlank { "Dispositivo Android" }
    }

    val currentAndroidOsLabel: String by lazy {
        "Android ${Build.VERSION.RELEASE ?: "OS"} (API ${Build.VERSION.SDK_INT})"
    }

    // =========================================================================
    // 1. BIOMETRÍA REAL DE HARDWARE (BIOMETRIC_STRONG + ROOM + PREFS)
    // =========================================================================
    fun isBiometricHardwareAvailable(): Boolean {
        return runCatching { biometricDataSource.canAuthenticate() }.getOrDefault(false)
    }

    fun toggleBiometricLockWithHardwarePrompt(
        activity: FragmentActivity?,
        userId: String,
        targetEnabled: Boolean,
        onSuccess: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isBiometricHardwareAvailable()) {
            onError("Este dispositivo no tiene lector biométrico configurado (Huella / Rostro) en Ajustes de Android.")
            return
        }

        if (activity == null) {
            onError("No se encontró una Activity compatible para abrir el lector biométrico.")
            return
        }

        biometricDataSource.authenticateWithPrompt(
            activity = activity,
            title = if (targetEnabled) "Activar Bloqueo Biométrico" else "Desactivar Bloqueo Biométrico",
            subtitle = "Confirma tu identidad con huella o rostro",
            negativeButtonText = "Cancelar",
            onResult = { status ->
                when (status) {
                    is BiometricDataSource.BiometricAuthStatus.Success -> {
                        onSuccess(targetEnabled)
                    }
                    is BiometricDataSource.BiometricAuthStatus.Error -> {
                        onError(status.errString.toString())
                    }
                    is BiometricDataSource.BiometricAuthStatus.Failed -> {
                        onError("No se reconoció la huella o el rostro. Intenta de nuevo.")
                    }
                    is BiometricDataSource.BiometricAuthStatus.Unavailable -> {
                        onError("Hardware biométrico no disponible en este momento.")
                    }
                }
            }
        )
    }

    suspend fun persistBiometricSetting(userId: String, enabled: Boolean) = withContext(ioDispatcher) {
        val safeUser = userId.ifBlank { "user_default" }
        settingsRepository?.setBiometricsEnabled(safeUser, enabled)
        if (safeUser != "user_default") {
            settingsRepository?.setBiometricsEnabled("user_default", enabled)
        }
        // Sincronizar también en local_auth_config (Room) para que el Login screen lo respete
        authRepository?.setBiometricEnabled(enabled)
        logSecurityEvent(
            userId = safeUser,
            eventType = "BIOMETRIC_LOCK_TOGGLED",
            details = if (enabled) {
                "Bloqueo biométrico activado y verificado por hardware ($currentHardwareModelName)."
            } else {
                "Bloqueo biométrico desactivado tras verificación de identidad."
            }
        )
    }

    // =========================================================================
    // 2. POLÍTICA REAL DE CAPTURAS DE PANTALLA (WINDOW FLAG_SECURE EN VIVO)
    // =========================================================================
    suspend fun applyScreenshotPolicy(
        activity: Activity?,
        userId: String,
        allowScreenshots: Boolean
    ) = withContext(ioDispatcher) {
        val safeUser = userId.ifBlank { "user_default" }
        // 1. Aplicar inmediatamente sobre la ventana de Android en el hilo principal
        withContext(Dispatchers.Main) {
            activity?.window?.let { window ->
                if (allowScreenshots) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                }
            }
        }
        // 2. Persistir tanto en el usuario autenticado como en user_default (leído por MainActivity)
        settingsRepository?.setAllowScreenshots(safeUser, allowScreenshots)
        if (safeUser != "user_default") {
            settingsRepository?.setAllowScreenshots("user_default", allowScreenshots)
        }
        logSecurityEvent(
            userId = safeUser,
            eventType = "CONFIG_SCREENSHOTS_POLICY",
            details = if (allowScreenshots) {
                "Capturas de pantalla habilitadas (FLAG_SECURE desactivado)."
            } else {
                "Protección contra capturas y grabación de pantalla activada (FLAG_SECURE)."
            }
        )
    }

    // =========================================================================
    // 3. CIFRADO LOCAL SQLCIPHER AES-256 Y FRASE DE PASO MAESTRA (PBKDF2 310K)
    // =========================================================================
    fun getSqlCipherEncryptionInfo(): DatabaseEncryptionInfo {
        return passphraseManager.getEncryptionInfo()
    }

    fun evaluateMasterPassphraseStrength(passphrase: String): PassphraseStrength {
        return passphraseManager.evaluatePassphraseStrength(passphrase)
    }

    suspend fun configureCustomMasterPassphrase(
        userId: String,
        newPassphrase: String
    ): Result<DatabaseEncryptionInfo> = withContext(ioDispatcher) {
        runCatching {
            val strength = passphraseManager.evaluatePassphraseStrength(newPassphrase)
            require(strength.isValid) {
                strength.validationMessage ?: "La frase de paso debe tener al menos 6 caracteres."
            }
            val success = passphraseManager.setUserPassphrase(newPassphrase)
            require(success) { "No fue posible derivar y cifrar la clave con Android Keystore." }
            passphraseManager.getEncryptionInfo()
        }
    }

    suspend fun resetToHardwareKeystorePassphrase(userId: String): Result<DatabaseEncryptionInfo> = withContext(ioDispatcher) {
        runCatching {
            passphraseManager.resetToHardwareMasterPassphrase()
            passphraseManager.getEncryptionInfo()
        }
    }

    // =========================================================================
    // 4. GESTIÓN REAL DE PIN (PBKDF2-HMAC-SHA256 + BLOQUEO ANTI-FUERZA BRUTA)
    // =========================================================================
    fun observeSecurityConfig(): Flow<SecurityConfig> {
        return authRepository?.getSecurityConfig() ?: flowOf(
            SecurityConfig(
                hasPinSet = false,
                isBiometricEnabled = false,
                failedAttempts = 0,
                maxAllowedAttempts = 3,
                isLockedOut = false,
                lockoutRemainingSeconds = 0L
            )
        )
    }

    suspend fun createOrChangeSecurityPin(
        hasPinAlreadySet: Boolean,
        currentPin: String,
        newPin: String,
        confirmPin: String
    ): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val repo = authRepository ?: error("Repositorio de autenticación no disponible.")
            val cleanNew = newPin.filter { it.isDigit() }
            val cleanConfirm = confirmPin.filter { it.isDigit() }
            require(cleanNew.length in 4..8) { "El PIN debe contener entre 4 y 8 dígitos numéricos." }
            require(cleanNew == cleanConfirm) { "El nuevo PIN y su confirmación no coinciden." }

            val trivialPins = setOf(
                "0000", "1111", "2222", "3333", "4444", "5555", "6666", "7777", "8888", "9999",
                "1234", "4321", "000000", "111111", "123456", "654321"
            )
            require(cleanNew !in trivialPins && !cleanNew.all { it == cleanNew[0] }) {
                "El PIN elegido es demasiado predecible. Usa una combinación más segura."
            }

            if (!hasPinAlreadySet) {
                val created = repo.setupPin(cleanNew)
                require(created) { "No se pudo configurar el PIN de seguridad." }
                "¡PIN de seguridad (${cleanNew.length} dígitos) configurado con PBKDF2-HMAC-SHA256!"
            } else {
                val cleanCurrent = currentPin.filter { it.isDigit() }
                require(cleanCurrent.length in 4..8) { "Ingresa tu PIN actual para autorizar el cambio." }
                val res = repo.changePin(cleanCurrent, cleanNew)
                if (res.isFailure) {
                    throw (res.exceptionOrNull() ?: IllegalArgumentException("PIN actual incorrecto."))
                }
                "¡PIN de seguridad actualizado exitosamente!"
            }
        }
    }

    // =========================================================================
    // 5. SESIONES REALES EN DISPOSITIVO (ROOM UserSessionDao + FIRESTORE)
    // =========================================================================
    fun observeRealActiveSessions(currentUserId: String): Flow<List<RealDeviceSessionItem>> {
        val sessionsFlow = userSessionDao?.getAllSessionsFlow() ?: flowOf(emptyList())
        return sessionsFlow.combine(flowOf(currentUserId)) { roomSessions, activeUid ->
            val fbUid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()
            val effectiveUid = activeUid.takeIf { it.isNotBlank() && it != "user_default" }
                ?: fbUid
                ?: activeUid

            val mapped = roomSessions
                .filter { it.isActive }
                .map { session -> mapSessionToRealItem(session, effectiveUid) }

            if (mapped.isNotEmpty()) {
                mapped
            } else {
                listOf(
                    RealDeviceSessionItem(
                        sessionId = currentDeviceId,
                        userId = effectiveUid.ifBlank { "user_default" },
                        deviceModelLabel = "$currentHardwareModelName (Este dispositivo)",
                        osVersionLabel = currentAndroidOsLabel,
                        authProviderLabel = "Sesión local cifrada activa",
                        lastActiveEpochMs = System.currentTimeMillis(),
                        isCurrentDevice = true
                    )
                )
            }
        }
    }

    suspend fun syncCurrentDeviceHeartbeatToFirestore(userId: String) = withContext(ioDispatcher) {
        val firebaseUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull() ?: return@withContext
        runCatching {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(firebaseUser.uid)
                .collection("active_devices")
                .document(currentDeviceId)
                .set(
                    mapOf(
                        "deviceId" to currentDeviceId,
                        "deviceModel" to currentHardwareModelName,
                        "osVersion" to currentAndroidOsLabel,
                        "lastActiveEpochMs" to System.currentTimeMillis(),
                        "isActive" to true
                    ),
                    SetOptions.merge()
                )
                .await()
        }
    }

    /**
     * Revoca todas las sesiones secundarias en Room (UserSessionDao) y elimina otros dispositivos
     * registrados en Cloud Firestore, conservando únicamente la sesión del dispositivo físico actual.
     */
    suspend fun revokeAllOtherSessions(currentUserId: String): Result<Int> = withContext(ioDispatcher) {
        runCatching {
            val fbUid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()
            val safeUser = currentUserId.takeIf { it.isNotBlank() && it != "user_default" }
                ?: fbUid
                ?: currentUserId.ifBlank { "user_default" }
            var revokedCount = 0

            // 1. Desactivar en Room cualquier otra sesión activa distinta de la actual
            val activeSession = userSessionDao?.getActiveSessionByUserId(safeUser)
                ?: userSessionDao?.getActiveSession()

            userSessionDao?.deactivateAllSessions()
            if (activeSession != null) {
                userSessionDao?.insertOrUpdateSession(
                    activeSession.copy(
                        isActive = true,
                        lastActiveTimestamp = System.currentTimeMillis()
                    )
                )
            }

            // 2. Eliminar dispositivos secundarios en Firestore (users/{uid}/active_devices)
            val firebaseUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
            if (firebaseUser != null) {
                runCatching {
                    val devicesRef = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(firebaseUser.uid)
                        .collection("active_devices")
                    val snapshot = devicesRef.get().await()
                    for (doc in snapshot.documents) {
                        if (doc.id != currentDeviceId) {
                            doc.reference.delete().await()
                            revokedCount++
                        }
                    }
                }
            }

            logSecurityEvent(
                userId = safeUser,
                eventType = "OTHER_SESSIONS_REVOKED",
                details = "Se cerraron las demás sesiones activas conservando únicamente $currentHardwareModelName."
            )
            revokedCount
        }
    }

    // =========================================================================
    // 6. HISTORIAL REAL DE AUDITORÍA DE SEGURIDAD (AuditSecurityLogDao)
    // =========================================================================
    fun observeRecentSecurityLogs(userId: String, limit: Int = 8): Flow<List<AuditSecurityLog>> {
        val safeUser = userId.ifBlank { "user_default" }
        return auditSecurityLogDao?.getRecentLogsByUser(safeUser, limit) ?: flowOf(emptyList())
    }

    private fun mapSessionToRealItem(session: UserSessionEntity, activeUid: String): RealDeviceSessionItem {
        val isCurrent = session.userId == activeUid || activeUid.isBlank() || activeUid == "user_default"
        val providerLabel = when {
            session.authProvider.contains("google", ignoreCase = true) -> "Google OAuth 2.0 • Firebase"
            session.authProvider.contains("email", ignoreCase = true) || session.authProvider.contains("password", ignoreCase = true) -> "Correo y Contraseña • Firebase"
            session.authProvider.contains("pin", ignoreCase = true) -> "Bóveda Local PIN PBKDF2"
            else -> "Firebase Auth Seguro"
        }
        return RealDeviceSessionItem(
            sessionId = "${session.userId}_${session.lastLoginTimestamp}",
            userId = session.userId,
            deviceModelLabel = if (isCurrent) {
                "$currentHardwareModelName (Este dispositivo)"
            } else {
                "Sesión secundaria (${session.email ?: session.userId.take(8)})"
            },
            osVersionLabel = currentAndroidOsLabel,
            authProviderLabel = providerLabel,
            lastActiveEpochMs = session.lastActiveTimestamp,
            isCurrentDevice = isCurrent
        )
    }

    private suspend fun logSecurityEvent(userId: String, eventType: String, details: String) {
        runCatching {
            auditSecurityLogDao?.insertLog(
                AuditSecurityLog(
                    userId = userId,
                    timestamp = System.currentTimeMillis(),
                    eventType = eventType,
                    authMethod = "SECURITY_CENTER",
                    details = details
                )
            )
        }
    }
}
