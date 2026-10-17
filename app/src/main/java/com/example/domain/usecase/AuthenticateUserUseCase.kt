package com.example.domain.usecase

import androidx.biometric.BiometricPrompt
import com.example.domain.models.AuthMethod
import com.example.domain.models.SecurityConfig
import com.example.domain.models.UserAuthResult
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

/**
 * Pure domain UseCase that orchestrates authentication methods:
 * - Google Sign-In with Drive sync token
 * - Biometric prompt validation with hardware AES key
 * - PIN entry with PBKDF2 hash & anti-brute force lockout
 */
class AuthenticateUserUseCase(
    private val authRepository: AuthRepository
) {
    fun observeSecurityConfig(): Flow<SecurityConfig> {
        return authRepository.getSecurityConfig()
    }

    suspend fun executePin(pin: String): UserAuthResult {
        val result = authRepository.authenticateWithPin(pin)
        when (result) {
            is UserAuthResult.Success -> {
                authRepository.logSecurityEvent(
                    eventType = "AUTH_SUCCESS",
                    method = AuthMethod.PIN,
                    details = "Usuario autenticado con código PIN protegido con PBKDF2"
                )
            }
            is UserAuthResult.Failure -> {
                authRepository.logSecurityEvent(
                    eventType = if (result.isLockedOut) "LOCKOUT_ACTIVATED" else "AUTH_FAILED",
                    method = AuthMethod.PIN,
                    details = result.errorMessage
                )
            }
            UserAuthResult.Cancelled -> Unit
        }
        return result
    }

    suspend fun executeBiometric(result: BiometricPrompt.AuthenticationResult): UserAuthResult {
        val authResult = authRepository.authenticateWithBiometric(result)
        when (authResult) {
            is UserAuthResult.Success -> {
                authRepository.logSecurityEvent(
                    eventType = "AUTH_SUCCESS",
                    method = AuthMethod.BIOMETRIC,
                    details = "Usuario autenticado mediante huella dactilar y CryptoObject Keystore"
                )
            }
            is UserAuthResult.Failure -> {
                authRepository.logSecurityEvent(
                    eventType = "AUTH_FAILED",
                    method = AuthMethod.BIOMETRIC,
                    details = authResult.errorMessage
                )
            }
            UserAuthResult.Cancelled -> Unit
        }
        return authResult
    }

    suspend fun executeGoogle(
        idToken: String?,
        email: String?,
        displayName: String?
    ): UserAuthResult {
        val result = authRepository.authenticateWithGoogle(idToken, email, displayName)
        when (result) {
            is UserAuthResult.Success -> {
                authRepository.logSecurityEvent(
                    eventType = "AUTH_SUCCESS",
                    method = AuthMethod.GOOGLE,
                    details = "Acceso exitoso con Google Sign-In y token Drive appdata"
                )
            }
            is UserAuthResult.Failure -> {
                authRepository.logSecurityEvent(
                    eventType = "AUTH_FAILED",
                    method = AuthMethod.GOOGLE,
                    details = result.errorMessage
                )
            }
            UserAuthResult.Cancelled -> Unit
        }
        return result
    }

    suspend fun setupInitialPin(pin: String): Boolean {
        val success = authRepository.setupPin(pin)
        if (success) {
            authRepository.logSecurityEvent(
                eventType = "PIN_CONFIGURED",
                method = AuthMethod.PIN,
                    details = "PIN de seguridad establecido con salt y hash PBKDF2"
            )
        }
        return success
    }
}
