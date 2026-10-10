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
    val statusMessage: String? = null,
    val userName: String = "Andrés Felipe",
    val userEmail: String = "cdosuarez@gmail.com",
    val userPhone: String = "+57 300 123 4567"
)

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observePreferences("user_default").collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }
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

    fun executeLogout(onLogoutSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoggingOut = true, showLogoutConfirmationDialog = false) }
        viewModelScope.launch {
            repository.executeProtocolLogout("user_default")
            _uiState.update { it.copy(isLoggingOut = false) }
            onLogoutSuccess()
        }
    }

    class Factory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository) as T
        }
    }
}
