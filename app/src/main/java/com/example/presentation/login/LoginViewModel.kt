package com.example.presentation.login

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.datasource.BiometricDataSource
import com.example.data.datasource.GoogleAuthDataSource
import com.example.domain.models.AuditLogEntry
import com.example.domain.models.AuthMethod
import com.example.domain.models.UserAuthResult
import com.example.domain.repository.AuthRepository
import com.example.domain.usecase.AuthenticateUserUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing authentication state, anti-brute force lockouts,
 * and interactions with Google Sign-In, BiometricPrompt, and PBKDF2 PIN.
 */
class LoginViewModel(
    private val authenticateUserUseCase: AuthenticateUserUseCase,
    private val authRepository: AuthRepository,
    private val googleAuthDataSource: GoogleAuthDataSource,
    private val biometricDataSource: BiometricDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val auditLogs: Flow<List<AuditLogEntry>> = authRepository.getAuditLogs()

    private var lockoutCountdownJob: Job? = null

    init {
        observeSecurityConfig()
        checkRegisteredSessionAndAutoPrompt()
    }

    private fun observeSecurityConfig() {
        viewModelScope.launch {
            try {
                authenticateUserUseCase.observeSecurityConfig().collect { config ->
                    _uiState.update { current ->
                        current.copy(
                            hasPinConfigured = config.hasPinSet,
                            isBiometricEnabled = config.isBiometricEnabled,
                            failedAttempts = config.failedAttempts,
                            remainingAttempts = (config.maxAllowedAttempts - config.failedAttempts).coerceAtLeast(0),
                            isLockedOut = config.isLockedOut,
                            lockoutSecondsRemaining = config.lockoutRemainingSeconds
                        )
                    }

                    if (config.isLockedOut && config.lockoutRemainingSeconds > 0) {
                        startLockoutCountdown(config.lockoutRemainingSeconds)
                    }
                }
            } catch (t: Throwable) {
                android.util.Log.e("LoginViewModel", "Notice observing security config: ${t.message}", t)
            }
        }
    }

    private fun startLockoutCountdown(initialSeconds: Long) {
        lockoutCountdownJob?.cancel()
        lockoutCountdownJob = viewModelScope.launch {
            var remaining = initialSeconds
            while (remaining > 0) {
                _uiState.update { it.copy(lockoutSecondsRemaining = remaining, isLockedOut = true) }
                delay(1000L)
                remaining--
            }
            authRepository.resetLockout()
            _uiState.update {
                it.copy(
                    isLockedOut = false,
                    lockoutSecondsRemaining = 0L,
                    remainingAttempts = 3,
                    failedAttempts = 0,
                    errorMessage = null
                )
            }
        }
    }

    fun onGoogleSignInClicked(activityContext: Context) {
        if (_uiState.value.isLockedOut) {
            _uiState.update { it.copy(errorMessage = "Dispositivo bloqueado temporalmente.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Conectando con Google y Drive...") }

            val googleResult = googleAuthDataSource.requestSignIn(activityContext)
            googleResult.fold(
                onSuccess = { credential ->
                    val authOutcome = authenticateUserUseCase.executeGoogle(
                        idToken = credential.idToken,
                        email = credential.email,
                        displayName = credential.displayName
                    )
                    handleAuthOutcome(authOutcome, AuthMethod.GOOGLE)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Error en Google Sign-In: ${error.localizedMessage ?: "Cancelado"}"
                        )
                    }
                }
            )
        }
    }

    fun onBiometricClicked(activity: FragmentActivity) {
        if (_uiState.value.isLockedOut) {
            _uiState.update { it.copy(errorMessage = "Dispositivo bloqueado temporalmente.") }
            return
        }

        if (!biometricDataSource.canAuthenticate()) {
            // No enrolled strong biometrics on hardware (e.g., emulator or unconfigured device).
            // Fall back to secure PIN or Google authentication. NEVER grant access.
            _uiState.update {
                it.copy(
                    showPinDialog = true,
                    errorMessage = "Sensor biométrico no disponible o sin huellas registradas. Usa tu PIN o cuenta de Google para acceder."
                )
            }
            return
        }

        biometricDataSource.authenticateWithPrompt(
            activity = activity,
            title = "KashApp Seguridad",
            subtitle = "Escanea tu huella para acceder a tu cuenta",
            negativeButtonText = "Usar PIN",
            onResult = { status ->
                when (status) {
                    is BiometricDataSource.BiometricAuthStatus.Success -> {
                        viewModelScope.launch {
                            val result = authenticateUserUseCase.executeBiometric(status.result)
                            handleAuthOutcome(result, AuthMethod.BIOMETRIC)
                        }
                    }
                    is BiometricDataSource.BiometricAuthStatus.Error -> {
                        if (status.errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            _uiState.update {
                                it.copy(showPinDialog = true, errorMessage = null)
                            }
                        } else {
                            _uiState.update {
                                it.copy(errorMessage = "Error biométrico: ${status.errString}")
                            }
                        }
                    }
                    BiometricDataSource.BiometricAuthStatus.Failed -> {
                        _uiState.update {
                            it.copy(errorMessage = "Huella no reconocida. Intenta de nuevo.")
                        }
                    }
                    BiometricDataSource.BiometricAuthStatus.Unavailable -> {
                        _uiState.update {
                            it.copy(
                                showPinDialog = true,
                                errorMessage = "Autenticación biométrica no disponible en este dispositivo. Ingresa tu PIN de seguridad."
                            )
                        }
                    }
                }
            }
        )
    }

    fun onPinClicked() {
        if (_uiState.value.isLockedOut) {
            _uiState.update { it.copy(errorMessage = "Dispositivo temporalmente bloqueado. Espera a que termine la cuenta regresiva.") }
            return
        }

        if (!_uiState.value.hasPinConfigured) {
            _uiState.update {
                it.copy(
                    showPinSetupDialog = true,
                    showPinDialog = false,
                    errorMessage = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                showPinDialog = true,
                showPinSetupDialog = false,
                errorMessage = null
            )
        }
    }

    fun dismissPinDialog() {
        _uiState.update { it.copy(showPinDialog = false, showPinSetupDialog = false, errorMessage = null) }
    }

    fun onPinSubmitted(pin: String) {
        if (_uiState.value.isLockedOut) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Verificando PIN protegido con PBKDF2...") }
            val outcome = authenticateUserUseCase.executePin(pin)
            handleAuthOutcome(outcome, AuthMethod.PIN)
        }
    }

    fun setupNewPin(pin: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Generando hash y salt PBKDF2...") }
            authenticateUserUseCase.setupInitialPin(pin)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    showPinDialog = false,
                    statusMessage = "PIN establecido con éxito. Ya puedes iniciar sesión."
                )
            }
        }
    }

    fun checkRegisteredSessionAndAutoPrompt() {
        viewModelScope.launch {
            try {
                val session = authRepository.getCurrentSession()
                    ?: authRepository.getLastSession()
                    ?: authRepository.syncFirebaseSessionWithLocalDb().getOrNull()

                val config = authenticateUserUseCase.observeSecurityConfig().first()

                _uiState.update { current ->
                    current.copy(
                        rememberedUser = session,
                        hasPinConfigured = config.hasPinSet,
                        isBiometricEnabled = config.isBiometricEnabled
                    )
                }

                if (config.isLockedOut) {
                    return@launch
                }

                if (_uiState.value.authenticatedUser != null) {
                    return@launch
                }

                // Auto-prompt on launch if registered user or security PIN is configured
                if (session != null || config.hasPinSet) {
                    if (config.isBiometricEnabled && biometricDataSource.canAuthenticate()) {
                        _uiState.update { it.copy(autoPromptBiometricTrigger = System.currentTimeMillis()) }
                    } else if (config.hasPinSet) {
                        _uiState.update { it.copy(showPinDialog = true) }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LoginViewModel", "Error in startup auto-prompt: ${e.message}", e)
            }
        }
    }

    fun consumeAutoPromptBiometric() {
        _uiState.update { it.copy(autoPromptBiometricTrigger = 0L) }
    }

    fun lockSession() {
        viewModelScope.launch {
            val session = authRepository.getCurrentSession() ?: authRepository.getLastSession()
            _uiState.update {
                it.copy(
                    authenticatedUser = null,
                    rememberedUser = session,
                    autoPromptBiometricTrigger = System.currentTimeMillis()
                )
            }
        }
    }

    fun onSwitchAccount() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update {
                it.copy(
                    authenticatedUser = null,
                    rememberedUser = null,
                    showPinDialog = false,
                    errorMessage = null
                )
            }
        }
    }

    private fun handleAuthOutcome(outcome: UserAuthResult, method: AuthMethod) {
        when (outcome) {
            is UserAuthResult.Success -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        authenticatedUser = outcome,
                        rememberedUser = it.rememberedUser ?: com.example.domain.models.UserSession(
                            userId = outcome.userId,
                            email = outcome.email,
                            displayName = outcome.displayName,
                            photoUrl = null,
                            authProvider = method.name.lowercase()
                        ),
                        showPinDialog = false,
                        errorMessage = null,
                        lastAuthMethodUsed = method,
                        statusMessage = "¡Bienvenido de nuevo a KashApp!"
                    )
                }
            }
            is UserAuthResult.Failure -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = outcome.errorMessage,
                        isLockedOut = outcome.isLockedOut,
                        lockoutSecondsRemaining = outcome.lockoutSecondsRemaining
                    )
                }
                if (outcome.isLockedOut && outcome.lockoutSecondsRemaining > 0) {
                    startLockoutCountdown(outcome.lockoutSecondsRemaining)
                }
            }
            UserAuthResult.Cancelled -> {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onLogout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update {
                it.copy(
                    authenticatedUser = null,
                    rememberedUser = null,
                    statusMessage = "Sesión cerrada correctamente",
                    errorMessage = null
                )
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
