package com.example.domain.repository

import com.example.domain.models.AuditLogEntry
import com.example.domain.models.AuthMethod
import com.example.domain.models.SecurityConfig
import com.example.domain.models.SessionState
import com.example.domain.models.UserAuthResult
import com.example.domain.models.UserSession
import kotlinx.coroutines.flow.Flow

/**
 * Domain interface for authentication and security management,
 * bridging Firebase Authentication with the local encrypted Room database
 * for seamless, offline-first user session management.
 */
interface AuthRepository {
    fun getSecurityConfig(): Flow<SecurityConfig>

    suspend fun authenticateWithPin(pin: String): UserAuthResult

    suspend fun setupPin(pin: String): Boolean

    suspend fun authenticateWithBiometric(isHardwareUnlocked: Boolean): UserAuthResult

    suspend fun authenticateWithGoogle(
        idToken: String?,
        email: String?,
        displayName: String?
    ): UserAuthResult

    suspend fun logSecurityEvent(
        eventType: String,
        method: AuthMethod,
        details: String
    )

    fun getAuditLogs(): Flow<List<AuditLogEntry>>

    suspend fun setBiometricEnabled(enabled: Boolean)

    suspend fun resetLockout()

    suspend fun logout()

    /**
     * Observes the active user session persisted in the local encrypted Room database.
     */
    fun getActiveSession(): Flow<UserSession?>

    /**
     * Retrieves the current active user session from the local encrypted Room database.
     */
    suspend fun getCurrentSession(): UserSession?

    /**
     * Observes the high-level reactive session lifecycle state (Initializing, Authenticated, Unauthenticated).
     */
    fun observeSessionState(): Flow<SessionState>

    /**
     * Bridges and synchronizes Firebase Authentication state with the local encrypted Room database.
     * If Firebase has an authenticated user, it updates or creates the local encrypted session.
     * If offline, returns the cached local encrypted session seamlessly.
     */
    suspend fun syncFirebaseSessionWithLocalDb(): Result<UserSession?>

    /**
     * Authenticates with Firebase using Email and Password and updates the local encrypted session.
     */
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<UserSession>

    /**
     * Registers a new account with Firebase using Email and Password and persists the local encrypted session.
     */
    suspend fun signUpWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String
    ): Result<UserSession>

    /**
     * Signs in anonymously via Firebase and persists a guest session in the local encrypted database.
     */
    suspend fun signInAnonymously(): Result<UserSession>

    /**
     * Updates the last active timestamp of a session to prevent expiration.
     */
    suspend fun touchSession(userId: String)

    /**
     * Clears all local encrypted session records.
     */
    suspend fun clearSession()
}

