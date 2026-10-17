package com.example.domain.models

/**
 * Domain representation of an active user authentication session,
 * bridged between Firebase Authentication and local encrypted Room persistence.
 */
data class UserSession(
    val userId: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val authProvider: String,
    val isEmailVerified: Boolean = false,
    val sessionCreatedAt: Long = System.currentTimeMillis(),
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val isAnonymous: Boolean = false,
    val idToken: String? = null
)

/**
 * Sealed representation of current user session lifecycle state.
 */
sealed class SessionState {
    object Initializing : SessionState()
    data class Authenticated(val session: UserSession) : SessionState()
    object Unauthenticated : SessionState()
}
