package com.example.features.settings.security

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import com.example.features.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Opciones corporativas de tiempo de bloqueo automático en segundo plano.
 */
enum class AutoLockTimeoutOption(val minutes: Int, val displayLabel: String) {
    IMMEDIATE(0, "Inmediatamente al minimizar"),
    ONE_MINUTE(1, "Después de 1 minuto"),
    FIVE_MINUTES(5, "Después de 5 minutos"),
    FIFTEEN_MINUTES(15, "Después de 15 minutos");

    val durationMs: Long get() = minutes * 60_000L

    companion object {
        fun fromMinutes(minutes: Int): AutoLockTimeoutOption =
            entries.firstOrNull { it.minutes == minutes } ?: IMMEDIATE
    }
}

/**
 * Estado reactivo del cerrojo de sesión en tiempo de ejecución.
 */
data class AppLockRuntimeState(
    val isLocked: Boolean = true,
    val isInForeground: Boolean = false,
    val configuredTimeoutMinutes: Int = 1,
    val lastBackgroundedElapsedMs: Long = 0L,
    val lastAuthenticatedElapsedMs: Long = 0L,
    val lockReason: String? = null
)

/**
 * Coordinador de ciclo de vida que protege a KashAPP cuando pasa a segundo plano.
 * Utiliza reloj monotónico (elapsedRealtime) inmune a cambios de hora del sistema.
 */
class AppLockLifecycleCoordinator(
    private val settingsRepository: SettingsRepository,
    private val monotonicClockMs: () -> Long = { SystemClock.elapsedRealtime() },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : Application.ActivityLifecycleCallbacks {

    private val _state = MutableStateFlow(AppLockRuntimeState())
    val state: StateFlow<AppLockRuntimeState> = _state.asStateFlow()

    private var startedActivityCount = 0
    private var activeUserId: String = "user_default"

    // Token temporal para flujos del sistema (Cámara OCR, Selector SAF, Google OAuth)
    @Volatile
    private var externalFlowGraceUntilElapsedMs: Long = 0L

    fun bindActiveUser(userId: String) {
        activeUserId = userId.ifBlank { "user_default" }
        scope.launch {
            settingsRepository.observePreferences(activeUserId).collect { prefs ->
                _state.update {
                    it.copy(configuredTimeoutMinutes = prefs.autoLockTimeoutMinutes)
                }
            }
        }
    }

    /**
     * Registra una autenticación exitosa (Huella, PIN o Google OAuth) y desbloquea la sesión.
     */
    fun onUserAuthenticated() {
        val now = monotonicClockMs()
        externalFlowGraceUntilElapsedMs = 0L
        _state.update {
            it.copy(
                isLocked = false,
                lastAuthenticatedElapsedMs = now,
                lastBackgroundedElapsedMs = 0L,
                lockReason = null
            )
        }
    }

    /**
     * Fuerza el bloqueo inmediato de la pantalla (ej. botón "Bloquear Bóveda Ahora" en el Dashboard).
     */
    fun lockNow(reason: String = "MANUAL_LOCK") {
        _state.update {
            it.copy(
                isLocked = true,
                lockReason = reason
            )
        }
    }

    /**
     * Otorga una ventana de gracia de un solo uso cuando la app lanza la cámara o el selector de archivos.
     */
    fun grantExternalSystemFlowGrace(maxDurationMs: Long = 45_000L) {
        externalFlowGraceUntilElapsedMs = monotonicClockMs() + maxDurationMs
    }

    /**
     * Evalúa determinísticamente si la sesión debe bloquearse al regresar de segundo plano.
     */
    internal fun evaluateLockOnForegroundReturn(
        isBiometricOrPinEnabled: Boolean,
        timeoutMinutes: Int,
        backgroundedAtElapsedMs: Long,
        nowElapsedMs: Long
    ): Boolean {
        if (!isBiometricOrPinEnabled) return false

        // Verificar si existe un token de gracia activo por cámara/selector de archivos
        val graceDeadline = externalFlowGraceUntilElapsedMs
        if (graceDeadline > 0L) {
            externalFlowGraceUntilElapsedMs = 0L // Consumir token (un solo uso)
            if (nowElapsedMs <= graceDeadline) {
                return false
            }
        }

        if (backgroundedAtElapsedMs <= 0L) return true
        val elapsedInBackground = nowElapsedMs - backgroundedAtElapsedMs

        // Si elapsedInBackground < 0, hubo reinicio o anomalía de reloj -> Fail-Closed (Bloquear)
        if (elapsedInBackground < 0L) return true

        val thresholdMs = AutoLockTimeoutOption.fromMinutes(timeoutMinutes).durationMs
        return elapsedInBackground >= thresholdMs
    }

    override fun onActivityStarted(activity: Activity) {
        // Ignorar la actividad aislada de reporte de crash
        if (activity.javaClass.simpleName == "CrashRecoveryActivity") return

        val wasInBackground = startedActivityCount == 0
        startedActivityCount++

        if (wasInBackground) {
            val now = monotonicClockMs()
            val currentSnapshot = _state.value
            val prefs = settingsRepository.getPreferencesSnapshot(activeUserId)
            val lockRequired = evaluateLockOnForegroundReturn(
                isBiometricOrPinEnabled = prefs.biometricsEnabled || prefs.hasPinEnabled,
                timeoutMinutes = currentSnapshot.configuredTimeoutMinutes,
                backgroundedAtElapsedMs = currentSnapshot.lastBackgroundedElapsedMs,
                nowElapsedMs = now
            )

            _state.update {
                it.copy(
                    isInForeground = true,
                    isLocked = it.isLocked || lockRequired,
                    lockReason = if (lockRequired) "BACKGROUND_TIMEOUT_EXPIRED" else it.lockReason
                )
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity.javaClass.simpleName == "CrashRecoveryActivity") return

        startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
        if (startedActivityCount == 0) {
            val now = monotonicClockMs()
            _state.update {
                it.copy(
                    isInForeground = false,
                    lastBackgroundedElapsedMs = now
                )
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
