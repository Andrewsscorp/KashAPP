package com.example.features.settings.security

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PassphraseManager
import com.example.data.local.Pbkdf2PinHasher
import com.example.domain.models.AuditLogEntry
import com.example.domain.models.AuthMethod
import com.example.domain.models.SecurityConfig
import com.example.domain.models.SessionState
import com.example.domain.models.UserAuthResult
import com.example.domain.models.UserSession
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SettingsSecurityLogicTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private class FakeSecurityAuthRepository(initialPin: String? = null) : AuthRepository {
        private var storedHash: String = initialPin?.let { Pbkdf2PinHasher.hashPin(it) } ?: ""
        private var isBiometric: Boolean = false
        val configFlow = MutableStateFlow(
            SecurityConfig(
                hasPinSet = storedHash.isNotBlank(),
                isBiometricEnabled = false,
                failedAttempts = 0,
                maxAllowedAttempts = 3,
                isLockedOut = false,
                lockoutRemainingSeconds = 0L,
                boundUserId = "user_test_123"
            )
        )

        val currentConfig: SecurityConfig get() = configFlow.value

        override fun getSecurityConfig(): Flow<SecurityConfig> = configFlow

        override suspend fun authenticateWithPin(pin: String): UserAuthResult {
            if (storedHash.isBlank()) return UserAuthResult.Failure("No PIN set")
            return if (Pbkdf2PinHasher.verifyPin(pin, storedHash)) {
                UserAuthResult.Success(
                    userId = "user_test_123",
                    displayName = "Test User",
                    email = "test@kashapp.com",
                    method = AuthMethod.PIN
                )
            } else {
                UserAuthResult.Failure("Invalid PIN")
            }
        }

        override suspend fun setupPin(pin: String): Boolean {
            storedHash = Pbkdf2PinHasher.hashPin(pin)
            configFlow.value = configFlow.value.copy(hasPinSet = true)
            return true
        }

        override suspend fun changePin(currentPin: String, newPin: String): Result<Unit> {
            if (storedHash.isBlank()) return Result.failure(IllegalStateException("No hay un PIN configurado"))
            if (!Pbkdf2PinHasher.verifyPin(currentPin, storedHash)) {
                return Result.failure(IllegalArgumentException("El PIN actual ingresado es incorrecto."))
            }
            storedHash = Pbkdf2PinHasher.hashPin(newPin)
            return Result.success(Unit)
        }

        override suspend fun isPinConfigured(): Boolean = storedHash.isNotBlank()

        override suspend fun authenticateWithBiometric(result: BiometricPrompt.AuthenticationResult): UserAuthResult =
            UserAuthResult.Success(
                userId = "user_test_123",
                displayName = "Test User",
                email = "test@kashapp.com",
                method = AuthMethod.BIOMETRIC
            )

        override suspend fun authenticateWithGoogle(idToken: String?, email: String?, displayName: String?): UserAuthResult =
            UserAuthResult.Success(
                userId = "user_test_123",
                displayName = displayName ?: "Google User",
                email = email,
                method = AuthMethod.GOOGLE
            )

        override suspend fun logSecurityEvent(eventType: String, method: AuthMethod, details: String) {}

        override fun getAuditLogs(): Flow<List<AuditLogEntry>> = MutableStateFlow(emptyList())

        override suspend fun setBiometricEnabled(enabled: Boolean) {
            isBiometric = enabled
            configFlow.value = configFlow.value.copy(isBiometricEnabled = enabled)
        }

        override suspend fun resetLockout() {
            configFlow.value = configFlow.value.copy(failedAttempts = 0, isLockedOut = false, lockoutRemainingSeconds = 0L)
        }

        override suspend fun logout() {}

        override fun getActiveSession(): Flow<UserSession?> = MutableStateFlow(null)
        override suspend fun getCurrentSession(): UserSession? = null
        override suspend fun getLastSession(): UserSession? = null
        override fun observeSessionState(): Flow<SessionState> = MutableStateFlow(SessionState.Unauthenticated)
        override suspend fun syncFirebaseSessionWithLocalDb(): Result<UserSession?> = Result.success(null)
        override suspend fun signInWithEmailAndPassword(email: String, password: String): Result<UserSession> = Result.failure(UnsupportedOperationException())
        override suspend fun signUpWithEmailAndPassword(email: String, password: String, displayName: String): Result<UserSession> = Result.failure(UnsupportedOperationException())
        override suspend fun signInAnonymously(): Result<UserSession> = Result.failure(UnsupportedOperationException())
        override suspend fun touchSession(userId: String) {}
        override suspend fun clearSession() {}
        override suspend fun deleteAccountPermanently(userId: String): Result<Unit> = Result.success(Unit)
    }

    @Test
    fun `TEST-SEC-01 Pbkdf2PinHasher genera hash PBKDF2-HMAC-SHA256 con 600k iteraciones y verifica PIN correcto`() {
        val hash = Pbkdf2PinHasher.hashPin("2580")
        assertTrue("El hash debe indicar 600000 iteraciones", hash.contains("600000"))
        assertTrue("Verificación de PIN 2580 debe ser exitosa", Pbkdf2PinHasher.verifyPin("2580", hash))
        assertFalse("PIN incorrecto 9999 debe ser rechazado", Pbkdf2PinHasher.verifyPin("9999", hash))
    }

    @Test
    fun `TEST-SEC-02 Crear PIN cuando hasPinSet es false invoca setupPin y habilita el PIN en local_auth_config`() = runBlocking {
        val fakeAuth = FakeSecurityAuthRepository(initialPin = null)
        assertFalse(fakeAuth.isPinConfigured())

        val setupRes = fakeAuth.setupPin("4826")
        assertTrue(setupRes)
        assertTrue(fakeAuth.isPinConfigured())
        assertTrue(fakeAuth.currentConfig.hasPinSet)
    }

    @Test
    fun `TEST-SEC-03 Cambiar PIN cuando hasPinSet es true exige PIN actual correcto y rechaza PIN erroneo`() = runBlocking {
        val fakeAuth = FakeSecurityAuthRepository(initialPin = "1234")

        val wrongCurrent = fakeAuth.changePin(currentPin = "0000", newPin = "5678")
        assertTrue(wrongCurrent.isFailure)

        val validChange = fakeAuth.changePin(currentPin = "1234", newPin = "5678")
        assertTrue(validChange.isSuccess)

        // Ahora el nuevo PIN "5678" es el único válido
        val secondChange = fakeAuth.changePin(currentPin = "5678", newPin = "9012")
        assertTrue(secondChange.isSuccess)
    }

    @Test
    fun `TEST-SEC-04 Sincronizacion de biometria actualiza el estado en AuthRepository`() = runBlocking {
        val fakeAuth = FakeSecurityAuthRepository(initialPin = "1234")
        assertFalse(fakeAuth.currentConfig.isBiometricEnabled)

        fakeAuth.setBiometricEnabled(true)
        assertTrue(fakeAuth.currentConfig.isBiometricEnabled)

        fakeAuth.setBiometricEnabled(false)
        assertFalse(fakeAuth.currentConfig.isBiometricEnabled)
    }

    @Test
    fun `TEST-SEC-05 PassphraseManager evaluatePassphraseStrength evalua correctamente fortalezas y requisitos`() {
        val passphraseManager = PassphraseManager.getInstance(context)

        val empty = passphraseManager.evaluatePassphraseStrength("")
        assertFalse(empty.isValid)
        assertEquals(0, empty.score)
        assertEquals("Vacía", empty.label)

        val shortPass = passphraseManager.evaluatePassphraseStrength("abc")
        assertFalse(shortPass.isValid)
        assertEquals(20, shortPass.score)
        assertEquals("Muy Débil", shortPass.label)

        val moderate = passphraseManager.evaluatePassphraseStrength("KashApp2026")
        assertTrue(moderate.isValid)
        assertTrue("Score moderado/fuerte esperado", moderate.score >= 50)

        val military = passphraseManager.evaluatePassphraseStrength("KashApp!2026#VaultStrongKeyAES")
        assertTrue(military.isValid)
        assertTrue("Score militar esperado (>= 80)", military.score >= 80)
        assertEquals("Muy Segura (Cifrado Militar)", military.label)
    }

    @Test
    fun `TEST-SEC-06 SecurityCenterCoordinator rechaza PINs triviales, longitud invalida o confirmacion desigual`() = runBlocking {
        val fakeAuth = FakeSecurityAuthRepository(initialPin = null)
        val coordinator = SecurityCenterCoordinator(
            context = context,
            authRepository = fakeAuth,
            settingsRepository = null,
            userSessionDao = null,
            auditSecurityLogDao = null,
            ioDispatcher = Dispatchers.Unconfined
        )

        // PIN trivial "1234"
        val trivialRes = coordinator.createOrChangeSecurityPin(
            hasPinAlreadySet = false,
            currentPin = "",
            newPin = "1234",
            confirmPin = "1234"
        )
        assertTrue(trivialRes.isFailure)
        assertTrue(trivialRes.exceptionOrNull()?.message?.contains("predecible") == true)

        // Longitud menor a 4 dígitos
        val shortRes = coordinator.createOrChangeSecurityPin(
            hasPinAlreadySet = false,
            currentPin = "",
            newPin = "12",
            confirmPin = "12"
        )
        assertTrue(shortRes.isFailure)
        assertTrue(shortRes.exceptionOrNull()?.message?.contains("entre 4 y 8") == true)

        // Confirmación no coincide
        val mismatchRes = coordinator.createOrChangeSecurityPin(
            hasPinAlreadySet = false,
            currentPin = "",
            newPin = "4826",
            confirmPin = "4827"
        )
        assertTrue(mismatchRes.isFailure)
        assertTrue(mismatchRes.exceptionOrNull()?.message?.contains("no coinciden") == true)
    }

    @Test
    fun `TEST-SEC-07 SecurityCenterCoordinator crea y cambia PIN correctamente con hashes PBKDF2`() = runBlocking {
        val fakeAuth = FakeSecurityAuthRepository(initialPin = null)
        val coordinator = SecurityCenterCoordinator(
            context = context,
            authRepository = fakeAuth,
            settingsRepository = null,
            userSessionDao = null,
            auditSecurityLogDao = null,
            ioDispatcher = Dispatchers.Unconfined
        )

        // Creación inicial exitosa con PIN seguro
        val createRes = coordinator.createOrChangeSecurityPin(
            hasPinAlreadySet = false,
            currentPin = "",
            newPin = "4826",
            confirmPin = "4826"
        )
        assertTrue(createRes.isSuccess)
        assertTrue(fakeAuth.isPinConfigured())

        // Cambio exitoso requiriendo PIN actual
        val changeRes = coordinator.createOrChangeSecurityPin(
            hasPinAlreadySet = true,
            currentPin = "4826",
            newPin = "9371",
            confirmPin = "9371"
        )
        assertTrue(changeRes.isSuccess)

        // Autenticación con el nuevo PIN es exitosa
        val authRes = fakeAuth.authenticateWithPin("9371")
        assertTrue(authRes is UserAuthResult.Success)
    }
}
