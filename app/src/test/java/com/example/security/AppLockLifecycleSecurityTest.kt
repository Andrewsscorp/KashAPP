package com.example.security

import com.example.features.settings.domain.model.UserPreferencesDomain
import com.example.features.settings.domain.repository.SettingsRepository
import com.example.features.settings.security.AppLockLifecycleCoordinator
import com.example.features.settings.security.AutoLockTimeoutOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Suite de pruebas unitarias para el cerrojo de sesión con reloj monotónico (Pilar 01).
 * Verifica inmunidad ante manipulación de hora, tiempos de gracia y fail-closed.
 */
class AppLockLifecycleSecurityTest {

    private var fakeElapsedMs = 100_000L
    private lateinit var coordinator: AppLockLifecycleCoordinator
    private val fakePrefsFlow = MutableStateFlow(
        UserPreferencesDomain(
            userId = "user_test",
            biometricsEnabled = true,
            autoLockTimeoutMinutes = 1
        )
    )

    private val fakeSettingsRepo = object : SettingsRepository {
        override fun observePreferences(userId: String): Flow<UserPreferencesDomain> = fakePrefsFlow
        override suspend fun getPreferences(userId: String): UserPreferencesDomain = fakePrefsFlow.value
        override fun getPreferencesSnapshot(userId: String): UserPreferencesDomain = fakePrefsFlow.value
        override suspend fun updatePreferences(preferences: UserPreferencesDomain): Result<UserPreferencesDomain> {
            fakePrefsFlow.value = preferences
            return Result.success(preferences)
        }
        override suspend fun togglePrivacyMode(userId: String, enabled: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun setBaseCurrency(userId: String, currency: String): Result<Unit> = Result.success(Unit)
        override suspend fun setLanguage(userId: String, language: String): Result<Unit> = Result.success(Unit)
        override suspend fun setTheme(userId: String, theme: String): Result<Unit> = Result.success(Unit)
        override suspend fun setBiometricsEnabled(userId: String, enabled: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun setBudgetAlertsEnabled(userId: String, enabled: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun setPaymentRemindersEnabled(userId: String, enabled: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun setExcludeAllAccountsFromDashboard(userId: String, excluded: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun setAccountExcludedFromDashboard(userId: String, accountId: String, excluded: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun isAccountExcluded(userId: String, accountId: String): Boolean = false
        override suspend fun setAutoNoAffectInventoryForAssets(userId: String, enabled: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun applyNoAffectInventoryToAllAssets(userId: String, noAffect: Boolean): Result<Int> = Result.success(0)
        override suspend fun setAutoNoAffectDashboardForAssets(userId: String, enabled: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun applyNoAffectDashboardToAllAssets(userId: String, noAffect: Boolean): Result<Int> = Result.success(0)
        override suspend fun setExcludeAllFixedAssetsFromDashboard(userId: String, excluded: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun setPrivacyPolicyConsent(userId: String, accepted: Boolean, version: String): Result<Unit> = Result.success(Unit)
        override suspend fun isPrivacyPolicyAccepted(userId: String): Boolean = true
        override suspend fun executeProtocolLogout(userId: String): Result<Unit> = Result.success(Unit)
    }

    @Before
    fun setUp() {
        fakeElapsedMs = 100_000L
        coordinator = AppLockLifecycleCoordinator(
            settingsRepository = fakeSettingsRepo,
            monotonicClockMs = { fakeElapsedMs },
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        )
        coordinator.bindActiveUser("user_test")
        coordinator.onUserAuthenticated()
    }

    @Test
    fun testImmediateLockOnBackgroundReturn() {
        val lockRequired = coordinator.evaluateLockOnForegroundReturn(
            isBiometricOrPinEnabled = true,
            timeoutMinutes = AutoLockTimeoutOption.IMMEDIATE.minutes,
            backgroundedAtElapsedMs = fakeElapsedMs,
            nowElapsedMs = fakeElapsedMs + 500L
        )
        assertTrue("Con tiempo inmediato (0 min), debe bloquear al regresar", lockRequired)
    }

    @Test
    fun testOneMinuteTimeoutNotLockedAt59Seconds() {
        val backgroundedAt = fakeElapsedMs
        val now = backgroundedAt + 59_999L // 59.999 segundos después

        val lockRequired = coordinator.evaluateLockOnForegroundReturn(
            isBiometricOrPinEnabled = true,
            timeoutMinutes = AutoLockTimeoutOption.ONE_MINUTE.minutes,
            backgroundedAtElapsedMs = backgroundedAt,
            nowElapsedMs = now
        )
        assertFalse("A los 59 segundos no debe bloquear", lockRequired)
    }

    @Test
    fun testOneMinuteTimeoutLocksAt60Seconds() {
        val backgroundedAt = fakeElapsedMs
        val now = backgroundedAt + 60_000L // 60.000 ms = 1 min

        val lockRequired = coordinator.evaluateLockOnForegroundReturn(
            isBiometricOrPinEnabled = true,
            timeoutMinutes = AutoLockTimeoutOption.ONE_MINUTE.minutes,
            backgroundedAtElapsedMs = backgroundedAt,
            nowElapsedMs = now
        )
        assertTrue("Al cumplirse 60 segundos debe bloquear", lockRequired)
    }

    @Test
    fun testSystemClockTamperingBackwardsFailsClosed() {
        val backgroundedAt = fakeElapsedMs
        val anomalousTime = backgroundedAt - 5_000L // Reloj manipulado hacia atrás

        val lockRequired = coordinator.evaluateLockOnForegroundReturn(
            isBiometricOrPinEnabled = true,
            timeoutMinutes = AutoLockTimeoutOption.FIVE_MINUTES.minutes,
            backgroundedAtElapsedMs = backgroundedAt,
            nowElapsedMs = anomalousTime
        )
        assertTrue("Ante anomalía de reloj o reinicio (elapsed < 0), debe bloquear inmediatamente (fail-closed)", lockRequired)
    }

    @Test
    fun testExternalCameraGraceTokenConsumedOnce() {
        // Conceder token de gracia de 45 segundos para cámara/selector
        coordinator.grantExternalSystemFlowGrace(maxDurationMs = 45_000L)

        // Primer regreso a los 20 segundos
        val firstReturnTime = fakeElapsedMs + 20_000L
        val firstLock = coordinator.evaluateLockOnForegroundReturn(
            isBiometricOrPinEnabled = true,
            timeoutMinutes = AutoLockTimeoutOption.IMMEDIATE.minutes,
            backgroundedAtElapsedMs = fakeElapsedMs,
            nowElapsedMs = firstReturnTime
        )
        assertFalse("Dentro de los 45s de gracia, no debe bloquear a pesar de timeout inmediato", firstLock)

        // Segundo regreso posterior (token ya consumido)
        val secondReturnTime = firstReturnTime + 2_000L
        val secondLock = coordinator.evaluateLockOnForegroundReturn(
            isBiometricOrPinEnabled = true,
            timeoutMinutes = AutoLockTimeoutOption.IMMEDIATE.minutes,
            backgroundedAtElapsedMs = firstReturnTime,
            nowElapsedMs = secondReturnTime
        )
        assertTrue("El token de gracia es de un solo uso; subsecuentes retornos deben bloquear", secondLock)
    }

    @Test
    fun testExternalGraceTokenExpiredLocksApp() {
        coordinator.grantExternalSystemFlowGrace(maxDurationMs = 45_000L)

        // Regreso después de 46 segundos (expirado)
        val expiredTime = fakeElapsedMs + 46_000L
        val lockRequired = coordinator.evaluateLockOnForegroundReturn(
            isBiometricOrPinEnabled = true,
            timeoutMinutes = AutoLockTimeoutOption.IMMEDIATE.minutes,
            backgroundedAtElapsedMs = fakeElapsedMs,
            nowElapsedMs = expiredTime
        )
        assertTrue("Si el token expiró (>45s), debe bloquear", lockRequired)
    }
}
