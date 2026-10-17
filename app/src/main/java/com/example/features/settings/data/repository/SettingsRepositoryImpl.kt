package com.example.features.settings.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.features.settings.domain.model.UserPreferencesDomain
import com.example.features.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsRepositoryImpl(
    private val context: Context,
    private val auditSecurityLogDao: com.example.data.local.dao.AuditSecurityLogDao? = null,
    private val fixedAssetDao: com.example.data.local.dao.FixedAssetDao? = null
) : SettingsRepository {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("kashapp_enterprise_settings_prefs", Context.MODE_PRIVATE)
    }

    private val _preferencesFlow = MutableStateFlow(loadInitialPreferences())

    private fun loadInitialPreferences(): UserPreferencesDomain {
        return UserPreferencesDomain(
            userId = "user_default",
            language = prefs.getString("language", "es") ?: "es",
            baseCurrency = prefs.getString("base_currency", "COP") ?: "COP",
            theme = prefs.getString("theme", "SYSTEM") ?: "SYSTEM",
            fontSize = prefs.getString("font_size", "MEDIUM") ?: "MEDIUM",
            biometricsEnabled = prefs.getBoolean("biometrics_enabled", false),
            pushNotifications = prefs.getBoolean("push_notifications", true),
            budgetAlertsEnabled = prefs.getBoolean("budget_alerts_enabled", true),
            paymentRemindersEnabled = prefs.getBoolean("payment_reminders_enabled", true),
            weeklySummaryEnabled = prefs.getBoolean("weekly_summary_enabled", true),
            monthlySummaryEnabled = prefs.getBoolean("monthly_summary_enabled", true),
            wifiOnlySync = prefs.getBoolean("wifi_only_sync", false),
            privacyModeEnabled = prefs.getBoolean("privacy_mode_enabled", false),
            excludeAllAccountsFromDashboard = prefs.getBoolean("exclude_all_accounts_dashboard", false),
            excludedAccountIds = prefs.getStringSet("excluded_account_ids", emptySet()) ?: emptySet(),
            autoNoAffectInventoryForAssets = prefs.getBoolean("auto_no_affect_inventory_for_assets", false),
            autoNoAffectDashboardForAssets = prefs.getBoolean("auto_no_affect_dashboard_for_assets", false),
            excludeAllFixedAssetsFromDashboard = prefs.getBoolean("exclude_all_fixed_assets_dashboard", false),
            version = prefs.getLong("version", 1L)
        )
    }

    override fun observePreferences(userId: String): Flow<UserPreferencesDomain> {
        return _preferencesFlow.asStateFlow()
    }

    override suspend fun getPreferences(userId: String): UserPreferencesDomain {
        return _preferencesFlow.value
    }

    override suspend fun updatePreferences(preferences: UserPreferencesDomain): Result<UserPreferencesDomain> {
        val nextVersion = preferences.version + 1
        val updated = preferences.copy(version = nextVersion, updatedAt = System.currentTimeMillis())
        prefs.edit()
            .putString("language", updated.language)
            .putString("base_currency", updated.baseCurrency)
            .putString("theme", updated.theme)
            .putString("font_size", updated.fontSize)
            .putBoolean("biometrics_enabled", updated.biometricsEnabled)
            .putBoolean("push_notifications", updated.pushNotifications)
            .putBoolean("budget_alerts_enabled", updated.budgetAlertsEnabled)
            .putBoolean("payment_reminders_enabled", updated.paymentRemindersEnabled)
            .putBoolean("weekly_summary_enabled", updated.weeklySummaryEnabled)
            .putBoolean("monthly_summary_enabled", updated.monthlySummaryEnabled)
            .putBoolean("wifi_only_sync", updated.wifiOnlySync)
            .putBoolean("privacy_mode_enabled", updated.privacyModeEnabled)
            .putBoolean("exclude_all_accounts_dashboard", updated.excludeAllAccountsFromDashboard)
            .putStringSet("excluded_account_ids", updated.excludedAccountIds)
            .putBoolean("auto_no_affect_inventory_for_assets", updated.autoNoAffectInventoryForAssets)
            .putBoolean("auto_no_affect_dashboard_for_assets", updated.autoNoAffectDashboardForAssets)
            .putBoolean("exclude_all_fixed_assets_dashboard", updated.excludeAllFixedAssetsFromDashboard)
            .putLong("version", nextVersion)
            .apply()

        _preferencesFlow.value = updated
        return Result.success(updated)
    }

    override suspend fun togglePrivacyMode(userId: String, enabled: Boolean): Result<Unit> {
        val current = _preferencesFlow.value
        return updatePreferences(current.copy(privacyModeEnabled = enabled)).map { }
    }

    override suspend fun setBaseCurrency(userId: String, currency: String): Result<Unit> {
        val current = _preferencesFlow.value
        return updatePreferences(current.copy(baseCurrency = currency)).map { }
    }

    override suspend fun setLanguage(userId: String, language: String): Result<Unit> {
        val current = _preferencesFlow.value
        return updatePreferences(current.copy(language = language)).map { }
    }

    override suspend fun setTheme(userId: String, theme: String): Result<Unit> {
        val current = _preferencesFlow.value
        return updatePreferences(current.copy(theme = theme)).map { }
    }

    override suspend fun setBiometricsEnabled(userId: String, enabled: Boolean): Result<Unit> {
        val current = _preferencesFlow.value
        return updatePreferences(current.copy(biometricsEnabled = enabled)).map { }
    }

    override suspend fun setBudgetAlertsEnabled(userId: String, enabled: Boolean): Result<Unit> {
        val current = _preferencesFlow.value
        return updatePreferences(current.copy(budgetAlertsEnabled = enabled)).map { }
    }

    override suspend fun setPaymentRemindersEnabled(userId: String, enabled: Boolean): Result<Unit> {
        val current = _preferencesFlow.value
        return updatePreferences(current.copy(paymentRemindersEnabled = enabled)).map { }
    }

    override suspend fun setExcludeAllAccountsFromDashboard(userId: String, excluded: Boolean): Result<Unit> {
        val current = _preferencesFlow.value
        val res = updatePreferences(current.copy(excludeAllAccountsFromDashboard = excluded))
        try {
            auditSecurityLogDao?.insertLog(
                com.example.data.local.entity.AuditSecurityLog(
                    eventType = "CONFIG_EXCLUDE_ALL_ACCOUNTS_DASHBOARD",
                    authMethod = "SETTINGS",
                    details = "Ajuste General: ${if (excluded) "ACTIVADO - Todas las cuentas excluidas del Dashboard" else "DESACTIVADO - Las cuentas afectan el Dashboard normalmente"}"
                )
            )
        } catch (_: Exception) {}
        return res.map { }
    }

    override suspend fun setAccountExcludedFromDashboard(userId: String, accountId: String, excluded: Boolean): Result<Unit> {
        val current = _preferencesFlow.value
        val newSet = if (excluded) {
            current.excludedAccountIds + accountId
        } else {
            current.excludedAccountIds - accountId
        }
        val res = updatePreferences(current.copy(excludedAccountIds = newSet))
        try {
            auditSecurityLogDao?.insertLog(
                com.example.data.local.entity.AuditSecurityLog(
                    eventType = "CONFIG_ACCOUNT_DASHBOARD_EXCLUSION",
                    authMethod = "SETTINGS",
                    details = "Ajuste Individual: Cuenta ID $accountId ${if (excluded) "EXCLUIDA de métricas del Dashboard" else "INCLUIDA en métricas del Dashboard"}"
                )
            )
        } catch (_: Exception) {}
        return res.map { }
    }

    override suspend fun isAccountExcluded(userId: String, accountId: String): Boolean {
        val current = _preferencesFlow.value
        return current.excludeAllAccountsFromDashboard || current.excludedAccountIds.contains(accountId)
    }

    override suspend fun setAutoNoAffectInventoryForAssets(userId: String, enabled: Boolean): Result<Unit> {
        val current = _preferencesFlow.value
        val res = updatePreferences(current.copy(autoNoAffectInventoryForAssets = enabled))
        try {
            auditSecurityLogDao?.insertLog(
                com.example.data.local.entity.AuditSecurityLog(
                    eventType = "CONFIG_ASSETS_AUTO_NO_AFFECT_INVENTORY",
                    authMethod = "SETTINGS",
                    details = "Ajuste General Activos: ${if (enabled) "ACTIVADO - Nuevos activos marcarán 'No afecta inventarios' por defecto" else "DESACTIVADO - Nuevos activos afectarán inventarios por defecto"}"
                )
            )
        } catch (_: Exception) {}
        return res.map { }
    }

    override suspend fun applyNoAffectInventoryToAllAssets(userId: String, noAffect: Boolean): Result<Int> {
        return runCatching {
            val count = fixedAssetDao?.updateNoAffectInventoryGlobal(noAffect) ?: 0
            try {
                auditSecurityLogDao?.insertLog(
                    com.example.data.local.entity.AuditSecurityLog(
                        eventType = "MASS_UPDATE_ASSETS_NO_AFFECT_INVENTORY",
                        authMethod = "SETTINGS",
                        details = "Acción Masiva Activos: ${if (noAffect) "Aplicado 'No afecta inventarios'" else "Restablecido 'Afecta inventarios'"} a $count activos fijos existentes."
                    )
                )
            } catch (_: Exception) {}
            count
        }
    }

    override suspend fun setAutoNoAffectDashboardForAssets(userId: String, enabled: Boolean): Result<Unit> {
        val current = _preferencesFlow.value
        val res = updatePreferences(current.copy(autoNoAffectDashboardForAssets = enabled))
        try {
            auditSecurityLogDao?.insertLog(
                com.example.data.local.entity.AuditSecurityLog(
                    eventType = "CONFIG_ASSETS_AUTO_NO_AFFECT_DASHBOARD",
                    authMethod = "SETTINGS",
                    details = "Ajuste General Activos: ${if (enabled) "ACTIVADO - Nuevos activos marcarán 'No afecta dashboard' por defecto" else "DESACTIVADO - Nuevos activos afectarán dashboard por defecto"}"
                )
            )
        } catch (_: Exception) {}
        return res.map { }
    }

    override suspend fun setExcludeAllFixedAssetsFromDashboard(userId: String, excluded: Boolean): Result<Unit> {
        val current = _preferencesFlow.value
        val res = updatePreferences(current.copy(excludeAllFixedAssetsFromDashboard = excluded))
        try {
            auditSecurityLogDao?.insertLog(
                com.example.data.local.entity.AuditSecurityLog(
                    eventType = "CONFIG_ASSETS_EXCLUDE_ALL_DASHBOARD",
                    authMethod = "SETTINGS",
                    details = "Ajuste General Activos: ${if (excluded) "ACTIVADO - Todos los activos fijos quedan EXCLUIDOS de los totales del Dashboard" else "DESACTIVADO - Activos fijos se incluyen normalmente en el Dashboard"}"
                )
            )
        } catch (_: Exception) {}
        return res.map { }
    }

    override suspend fun applyNoAffectDashboardToAllAssets(userId: String, noAffect: Boolean): Result<Int> {
        return runCatching {
            val count = fixedAssetDao?.updateNoAffectDashboardGlobal(noAffect) ?: 0
            try {
                auditSecurityLogDao?.insertLog(
                    com.example.data.local.entity.AuditSecurityLog(
                        eventType = "MASS_UPDATE_ASSETS_NO_AFFECT_DASHBOARD",
                        authMethod = "SETTINGS",
                        details = "Acción Masiva Activos: ${if (noAffect) "Aplicado 'No afecta dashboard'" else "Restablecido 'Afecta dashboard'"} a $count activos fijos existentes en la base de datos."
                    )
                )
            } catch (_: Exception) {}
            count
        }
    }

    override suspend fun executeProtocolLogout(userId: String): Result<Unit> {
        // Enforce protocol: purge active user cache
        prefs.edit().clear().apply()
        _preferencesFlow.value = UserPreferencesDomain()
        return Result.success(Unit)
    }
}
