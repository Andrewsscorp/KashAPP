package com.example.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.features.settings.domain.model.SettingsSubRoute
import com.example.features.settings.domain.model.UserPreferencesDomain
import com.example.features.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: UserPreferencesDomain = UserPreferencesDomain(),
    val currentSubRoute: SettingsSubRoute = SettingsSubRoute.MAIN,
    val isLoggingOut: Boolean = false,
    val showLogoutConfirmationDialog: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val showDeleteAccountConfirmationDialog: Boolean = false,
    val statusMessage: String? = null,
    val userName: String = "Usuario",
    val userEmail: String = "",
    val userPhone: String = "",
    val userIdDisplay: String = "",
    val photoUrl: String? = null,
    val authProvider: String = "Firebase Auth Seguro",
    val isEmailVerified: Boolean = false,
    val memberSince: String = ""
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val authRepository: com.example.domain.repository.AuthRepository? = null,
    val userId: String = "user_default"
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observePreferences("user_default").collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }
        viewModelScope.launch {
            loadRealUserProfile()
        }
        viewModelScope.launch {
            authRepository?.getActiveSession()?.collect { session ->
                if (session != null) {
                    _uiState.update { current ->
                        current.copy(
                            userName = session.displayName?.takeIf { it.isNotBlank() } ?: current.userName,
                            userEmail = session.email?.takeIf { it.isNotBlank() } ?: current.userEmail,
                            userIdDisplay = session.userId,
                            photoUrl = session.photoUrl ?: current.photoUrl,
                            authProvider = session.authProvider,
                            isEmailVerified = session.isEmailVerified
                        )
                    }
                }
            }
        }
    }

    private fun loadRealUserProfile() {
        try {
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (firebaseUser != null) {
                val creationTime = firebaseUser.metadata?.creationTimestamp ?: 0L
                val creationFormatted = if (creationTime > 0) {
                    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(creationTime))
                } else "2026"

                val provider = firebaseUser.providerData.firstOrNull { it.providerId != "firebase" }?.providerId ?: "Firebase"
                val providerName = when (provider) {
                    "google.com" -> "Google Authentication"
                    "password" -> "Correo y Contraseña"
                    "phone" -> "Autenticación Telefónica"
                    else -> "Firebase Auth Seguro"
                }

                _uiState.update { current ->
                    current.copy(
                        userName = firebaseUser.displayName?.takeIf { it.isNotBlank() } ?: current.userName.ifBlank { "Usuario KashApp" },
                        userEmail = firebaseUser.email?.takeIf { it.isNotBlank() } ?: current.userEmail,
                        userPhone = firebaseUser.phoneNumber?.takeIf { it.isNotBlank() } ?: current.userPhone,
                        userIdDisplay = firebaseUser.uid,
                        photoUrl = firebaseUser.photoUrl?.toString() ?: current.photoUrl,
                        authProvider = providerName,
                        isEmailVerified = firebaseUser.isEmailVerified,
                        memberSince = creationFormatted
                    )
                }
            } else {
                viewModelScope.launch {
                    val localSession = authRepository?.getCurrentSession()
                    if (localSession != null) {
                        _uiState.update { current ->
                            current.copy(
                                userName = localSession.displayName?.takeIf { it.isNotBlank() } ?: "Usuario KashApp",
                                userEmail = localSession.email ?: "",
                                userIdDisplay = localSession.userId,
                                photoUrl = localSession.photoUrl,
                                authProvider = localSession.authProvider,
                                isEmailVerified = localSession.isEmailVerified
                            )
                        }
                    } else {
                        _uiState.update { current ->
                            current.copy(
                                userName = current.userName.ifBlank { "Usuario KashApp" },
                                userIdDisplay = "KASH-" + (kotlin.math.abs(userId.hashCode()) % 100000).toString().padStart(5, '0')
                            )
                        }
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    fun navigateToSubRoute(subRoute: SettingsSubRoute) {
        _uiState.update { it.copy(currentSubRoute = subRoute) }
    }

    fun navigateBackToMain() {
        _uiState.update { it.copy(currentSubRoute = SettingsSubRoute.MAIN) }
    }

    fun showLogoutDialog(show: Boolean) {
        _uiState.update { it.copy(showLogoutConfirmationDialog = show) }
    }

    fun togglePrivacyMode() {
        val current = _uiState.value.preferences.privacyModeEnabled
        viewModelScope.launch {
            repository.togglePrivacyMode("user_default", !current)
        }
    }

    fun setBaseCurrency(currency: String) {
        viewModelScope.launch {
            repository.setBaseCurrency("user_default", currency)
            _uiState.update { it.copy(statusMessage = "Moneda base cambiada a $currency") }
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            repository.setTheme("user_default", theme)
        }
    }

    fun toggleBiometrics(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBiometricsEnabled("user_default", enabled)
        }
    }

    fun toggleBudgetAlerts(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBudgetAlertsEnabled("user_default", enabled)
        }
    }

    fun togglePaymentReminders(enabled: Boolean) {
        viewModelScope.launch {
            repository.setPaymentRemindersEnabled("user_default", enabled)
        }
    }

    fun updateProfile(name: String, email: String, phone: String) {
        _uiState.update {
            it.copy(
                userName = name,
                userEmail = email,
                userPhone = phone,
                statusMessage = "Perfil actualizado con éxito"
            )
        }
    }

    fun showStatusMessage(message: String) {
        _uiState.update { it.copy(statusMessage = message) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun toggleAutoNoAffectInventory(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoNoAffectInventoryForAssets(userId, enabled)
            _uiState.update {
                it.copy(
                    statusMessage = if (enabled)
                        "Nuevos activos fijos se marcarán automáticamente como 'No afecta inventarios'"
                    else
                        "Nuevos activos fijos afectarán inventarios por defecto"
                )
            }
        }
    }

    fun applyNoAffectInventoryToAllAssets(noAffect: Boolean) {
        viewModelScope.launch {
            repository.applyNoAffectInventoryToAllAssets(userId, noAffect)
                .onSuccess { count ->
                    _uiState.update {
                        it.copy(
                            statusMessage = if (noAffect)
                                "Se aplicó 'No afecta inventarios' a $count activos fijos existentes"
                            else
                                "Se habilitó afectación de inventario para $count activos fijos"
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(statusMessage = "Error al actualizar activos: ${err.message}") }
                }
        }
    }

    fun toggleAutoNoAffectDashboard(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoNoAffectDashboardForAssets(userId, enabled)
            _uiState.update {
                it.copy(
                    statusMessage = if (enabled)
                        "Nuevos activos fijos se marcarán automáticamente como 'No afecta dashboard'"
                    else
                        "Nuevos activos fijos afectarán el dashboard por defecto"
                )
            }
        }
    }

    fun toggleExcludeAllFixedAssetsFromDashboard(excluded: Boolean) {
        viewModelScope.launch {
            repository.setExcludeAllFixedAssetsFromDashboard(userId, excluded)
            _uiState.update {
                it.copy(
                    statusMessage = if (excluded)
                        "Se excluyeron todos los activos fijos del Dashboard principal"
                    else
                        "Los activos fijos se incluyen en el Dashboard principal"
                )
            }
        }
    }

    fun applyNoAffectDashboardToAllAssets(noAffect: Boolean) {
        viewModelScope.launch {
            repository.applyNoAffectDashboardToAllAssets(userId, noAffect)
                .onSuccess { count ->
                    _uiState.update {
                        it.copy(
                            statusMessage = if (noAffect)
                                "Se aplicó 'No afecta dashboard' a $count activos fijos existentes"
                            else
                                "Se habilitó afectación al dashboard para $count activos fijos"
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(statusMessage = "Error al actualizar activos: ${err.message}") }
                }
        }
    }

    fun executeLogout(onLogoutSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoggingOut = true, showLogoutConfirmationDialog = false) }
        viewModelScope.launch {
            repository.executeProtocolLogout(userId)
            _uiState.update { it.copy(isLoggingOut = false) }
            onLogoutSuccess()
        }
    }

    fun showDeleteAccountDialog(show: Boolean) {
        _uiState.update { it.copy(showDeleteAccountConfirmationDialog = show) }
    }

    fun executeDeleteAccount(onDeleteSuccess: () -> Unit) {
        _uiState.update { it.copy(isDeletingAccount = true, showDeleteAccountConfirmationDialog = false) }
        viewModelScope.launch {
            authRepository?.deleteAccountPermanently(userId)
            repository.executeProtocolLogout(userId)
            _uiState.update { it.copy(isDeletingAccount = false, statusMessage = "Cuenta eliminada permanentemente.") }
            onDeleteSuccess()
        }
    }

    class Factory(
        private val repository: SettingsRepository,
        private val authRepository: com.example.domain.repository.AuthRepository? = null,
        private val userId: String = "user_default"
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository, authRepository, userId) as T
        }
    }
}
