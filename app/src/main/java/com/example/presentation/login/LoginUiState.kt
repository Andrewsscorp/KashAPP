package com.example.presentation.login

import com.example.domain.models.AuthMethod
import com.example.domain.models.UserAuthResult
import com.example.domain.models.UserSession

/**
 * UI State for KashApp authentication flow.
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val authenticatedUser: UserAuthResult.Success? = null,
    val rememberedUser: UserSession? = null,
    val hasPinConfigured: Boolean = true,
    val isBiometricEnabled: Boolean = true,
    val failedAttempts: Int = 0,
    val remainingAttempts: Int = 3,
    val isLockedOut: Boolean = false,
    val lockoutSecondsRemaining: Long = 0L,
    val showPinDialog: Boolean = false,
    val showPinSetupDialog: Boolean = false,
    val autoPromptBiometricTrigger: Long = 0L,
    val lastAuthMethodUsed: AuthMethod? = null
)
