package com.example

import androidx.biometric.BiometricPrompt
import com.example.domain.models.AuditLogEntry
import com.example.domain.models.AuthMethod
import com.example.domain.models.SecurityConfig
import com.example.domain.models.SessionState
import com.example.domain.models.UserAuthResult
import com.example.domain.models.UserSession
import com.example.domain.repository.AuthRepository
import com.example.domain.usecase.AuthenticateUserUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticateUserUseCaseTest {

    private class FakeAuthRepository : AuthRepository {
        val loggedEvents = mutableListOf<String>()

        override fun getSecurityConfig(): Flow<SecurityConfig> {
            return flowOf(
                SecurityConfig(
                    hasPinSet = true,
                    isBiometricEnabled = true,
                    failedAttempts = 0,
                    maxAllowedAttempts = 3,
                    isLockedOut = false,
                    lockoutRemainingSeconds = 0L
                )
            )
        }

        override suspend fun authenticateWithPin(pin: String): UserAuthResult {
            return if (pin == "1234") {
                UserAuthResult.Success("test_uid", "Test User", null, AuthMethod.PIN)
            } else {
                UserAuthResult.Failure("PIN incorrecto", remainingAttempts = 2)
            }
        }

        override suspend fun setupPin(pin: String): Boolean = true

        override suspend fun authenticateWithBiometric(result: BiometricPrompt.AuthenticationResult): UserAuthResult {
            return if (result.cryptoObject != null) {
                UserAuthResult.Success("bio_uid", "Bio User", null, AuthMethod.BIOMETRIC)
            } else {
                UserAuthResult.Failure("Biometric failed: CryptoObject ausente")
            }
        }

        override suspend fun authenticateWithGoogle(
            idToken: String?,
            email: String?,
            displayName: String?
        ): UserAuthResult {
            return UserAuthResult.Success(
                userId = "google_uid",
                displayName = displayName ?: "Google User",
                email = email,
                method = AuthMethod.GOOGLE,
                driveSyncEnabled = true
            )
        }

        override suspend fun logSecurityEvent(
            eventType: String,
            method: AuthMethod,
            details: String
        ) {
            loggedEvents.add("$eventType:${method.name}")
        }

        override fun getAuditLogs(): Flow<List<AuditLogEntry>> = flowOf(emptyList())

        override suspend fun setBiometricEnabled(enabled: Boolean) {}

        override suspend fun resetLockout() {}

        override suspend fun logout() {}

        override fun getActiveSession(): Flow<UserSession?> = flowOf(null)

        override suspend fun getCurrentSession(): UserSession? = null

        override suspend fun getLastSession(): UserSession? = null

        override fun observeSessionState(): Flow<SessionState> = flowOf(SessionState.Unauthenticated)

        override suspend fun syncFirebaseSessionWithLocalDb(): Result<UserSession?> = Result.success(null)

        override suspend fun signInWithEmailAndPassword(email: String, password: String): Result<UserSession> =
            Result.success(UserSession("test_uid", email, "Test User", null, "firebase_password"))

        override suspend fun signUpWithEmailAndPassword(
            email: String,
            password: String,
            displayName: String
        ): Result<UserSession> =
            Result.success(UserSession("new_uid", email, displayName, null, "firebase_password"))

        override suspend fun signInAnonymously(): Result<UserSession> =
            Result.success(UserSession("anon_uid", null, "Guest", null, "firebase_anonymous", isAnonymous = true))

        override suspend fun touchSession(userId: String) {}

        override suspend fun clearSession() {}

        override suspend fun deleteAccountPermanently(userId: String): Result<Unit> = Result.success(Unit)
    }

    @Test
    fun `executePin with valid code returns Success and logs AUTH_SUCCESS`() = runBlocking {
        val fakeRepo = FakeAuthRepository()
        val useCase = AuthenticateUserUseCase(fakeRepo)

        val result = useCase.executePin("1234")

        assertTrue(result is UserAuthResult.Success)
        assertEquals(AuthMethod.PIN, (result as UserAuthResult.Success).method)
        assertTrue(fakeRepo.loggedEvents.contains("AUTH_SUCCESS:PIN"))
    }

    @Test
    fun `executeBiometric with valid CryptoObject returns Success and logs audit trail`() = runBlocking {
        val fakeRepo = FakeAuthRepository()
        val useCase = AuthenticateUserUseCase(fakeRepo)

        val cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding").apply {
            init(javax.crypto.Cipher.ENCRYPT_MODE, javax.crypto.spec.SecretKeySpec(ByteArray(16), "AES"))
        }
        val cryptoObject = BiometricPrompt.CryptoObject(cipher)
        val promptResult = biometricAuthenticationResult(cryptoObject)

        val result = useCase.executeBiometric(promptResult)

        assertTrue(result is UserAuthResult.Success)
        assertEquals(AuthMethod.BIOMETRIC, (result as UserAuthResult.Success).method)
        assertTrue(fakeRepo.loggedEvents.contains("AUTH_SUCCESS:BIOMETRIC"))
    }

    @Test
    fun `executeBiometric with null CryptoObject returns Failure and logs error`() = runBlocking {
        val fakeRepo = FakeAuthRepository()
        val useCase = AuthenticateUserUseCase(fakeRepo)

        val promptResult = biometricAuthenticationResult(null)

        val result = useCase.executeBiometric(promptResult)

        assertTrue(result is UserAuthResult.Failure)
        assertTrue(fakeRepo.loggedEvents.contains("AUTH_FAILED:BIOMETRIC"))
    }

    @Test
    fun `executeGoogle returns Success with Drive sync enabled`() = runBlocking {
        val fakeRepo = FakeAuthRepository()
        val useCase = AuthenticateUserUseCase(fakeRepo)

        val result = useCase.executeGoogle("sample_token", "user@kash.app", "Kash User")

        assertTrue(result is UserAuthResult.Success)
        val success = result as UserAuthResult.Success
        assertEquals(AuthMethod.GOOGLE, success.method)
        assertTrue(success.driveSyncEnabled)
        assertTrue(fakeRepo.loggedEvents.contains("AUTH_SUCCESS:GOOGLE"))
    }
}
