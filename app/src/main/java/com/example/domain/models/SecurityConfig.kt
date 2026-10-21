package com.example.domain.models

/**
 * Domain representation of local security and lockout policies.
 */
data class SecurityConfig(
    val hasPinSet: Boolean,
    val isBiometricEnabled: Boolean,
    val failedAttempts: Int,
    val maxAllowedAttempts: Int = 3,
    val isLockedOut: Boolean,
    val lockoutRemainingSeconds: Long,
    val boundUserId: String? = null
)
