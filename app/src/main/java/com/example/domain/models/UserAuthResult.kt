package com.example.domain.models

/**
 * Domain outcome of an authentication attempt.
 */
sealed class UserAuthResult {
    data class Success(
        val userId: String,
        val displayName: String,
        val email: String?,
        val method: AuthMethod,
        val token: String? = null,
        val driveSyncEnabled: Boolean = false
    ) : UserAuthResult()

    data class Failure(
        val errorMessage: String,
        val remainingAttempts: Int? = null,
        val isLockedOut: Boolean = false,
        val lockoutSecondsRemaining: Long = 0L
    ) : UserAuthResult()

    object Cancelled : UserAuthResult()
}
