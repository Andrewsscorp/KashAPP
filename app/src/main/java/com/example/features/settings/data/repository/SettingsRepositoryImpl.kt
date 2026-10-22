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

    private val _userPreferencesFlows = java.util.concurrent.ConcurrentHashMap<String, MutableStateFlow<UserPreferencesDomain>>()
    private val _preferencesFlow = MutableStateFlow(loadPreferences("user_default"))

    private fun k(userId: String, key: String): String = if (userId.isBlank()) key else "${userId}_$key"

    private fun loadPreferences(userId: String): UserPreferencesDomain {
        val uId = if (userId.isBlank()) "user_default" else userId
        val accepted = prefs.getBoolean(k(uId, "privacy_policy_accepted"), prefs.getBoolean("privacy_policy_accepted", false))
        val version = prefs.getString(k(uId, "privacy_policy_version"), prefs.getString("privacy_policy_version", "1.0-LEY-1581-2012")) ?: "1.0-LEY-1581-2012"
        val timestamp = prefs.getLong(k(uId, "privacy_policy_accepted_timestamp"), prefs.getLong("privacy_policy_accepted_timestamp", 0L))
        com.example.domain.security.FirebaseApiProtectionGuard.setPrivacyConsent(
            granted = accepted,
            version = version,
            timestampMs = timestamp
        )
        return UserPreferencesDomain(
            userId = uId,
            language = prefs.getString(k(uId, "language"), prefs.getString("language", "es")) ?: "es",
            baseCurrency = prefs.getString(k(uId, "base_currency"), prefs.getString("base_currency", "COP")) ?: "COP",
            theme = prefs.getString(k(uId, "theme"), prefs.getString("theme", "SYSTEM")) ?: "SYSTEM",
            fontSize = prefs.getString(k(uId, "font_size"), prefs.getString("font_size", "MEDIUM")) ?: "MEDIUM",
            biometricsEnabled = prefs.getBoolean(k(uId, "biometrics_enabled"), prefs.getBoolean("biometrics_enabled", false)),
            allowScreenshots = prefs.getBoolean(k(uId, "allow_screenshots"), prefs.getBoolean("allow_screenshots", false)),
            pushNotifications = prefs.getBoolean(k(uId, "push_notifications"), prefs.getBoolean("push_notifications", true)),
            budgetAlertsEnabled = prefs.getBoolean(k(uId, "budget_alerts_enabled"), prefs.getBoolean("budget_alerts_enabled", true)),
            paymentRemindersEnabled = prefs.getBoolean(k(uId, "payment_reminders_enabled"), prefs.getBoolean("payment_reminders_enabled", true)),
            weeklySummaryEnabled = prefs.getBoolean(k(uId, "weekly_summary_enabled"), prefs.getBoolean("weekly_summary_enabled", true)),
            monthlySummaryEnabled = prefs.getBoolean(k(uId, "monthly_summary_enabled"), prefs.getBoolean("monthly_summary_enabled", true)),
            wifiOnlySync = prefs.getBoolean(k(uId, "wifi_only_sync"), prefs.getBoolean("wifi_only_sync", false)),
            privacyModeEnabled = prefs.getBoolean(k(uId, "privacy_mode_enabled"), prefs.getBoolean("privacy_mode_enabled", false)),
            excludeAllAccountsFromDashboard = prefs.getBoolean(k(uId, "exclude_all_accounts_dashboard"), prefs.getBoolean("exclude_all_accounts_dashboard", false)),
            excludedAccountIds = prefs.getStringSet(k(uId, "excluded_account_ids"), prefs.getStringSet("excluded_account_ids", emptySet())) ?: emptySet(),
            autoNoAffectInventoryForAssets = prefs.getBoolean(k(uId, "auto_no_affect_inventory_for_assets"), prefs.getBoolean("auto_no_affect_inventory_for_assets", false)),
            autoNoAffectDashboardForAssets = prefs.getBoolean(k(uId, "auto_no_affect_dashboard_for_assets"), prefs.getBoolean("auto_no_affect_dashboard_for_assets", false)),
            excludeAllFixedAssetsFromDashboard = prefs.getBoolean(k(uId, "exclude_all_fixed_assets_dashboard"), prefs.getBoolean("exclude_all_fixed_assets_dashboard", false)),
            privacyPolicyAccepted = accepted,
            privacyPolicyVersion = version,
            privacyPolicyAcceptedTimestamp = timestamp,
            version = prefs.getLong(k(uId, "version"), prefs.getLong("version", 1L))
        )
    }

    override fun observePreferences(userId: String): Flow<UserPreferencesDomain> {
        val uId = if (userId.isBlank()) "user_default" else userId
        return _userPreferencesFlows.getOrPut(uId) { MutableStateFlow(loadPreferences(uId)) }.asStateFlow()
    }

    override suspend fun getPreferences(userId: String): UserPreferencesDomain {
        val uId = if (userId.isBlank()) "user_default" else userId
        return _userPreferencesFlows.getOrPut(uId) { MutableStateFlow(loadPreferences(uId)) }.value
    }

    override suspend fun updatePreferences(preferences: UserPreferencesDomain): Result<UserPreferencesDomain> {
        val uId = if (preferences.userId.isBlank()) "user_default" else preferences.userId
        val nextVersion = preferences.version + 1
        val updated = preferences.copy(userId = uId, version = nextVersion, updatedAt = System.currentTimeMillis())
        prefs.edit()
            .putString(k(uId, "language"), updated.language)
            .putString(k(uId, "base_currency"), updated.baseCurrency)
            .putString(k(uId, "theme"), updated.theme)
            .putString(k(uId, "font_size"), updated.fontSize)
            .putBoolean(k(uId, "biometrics_enabled"), updated.biometricsEnabled)
            .putBoolean(k(uId, "allow_screenshots"), updated.allowScreenshots)
            .putBoolean(k(uId, "push_notifications"), updated.pushNotifications)
            .putBoolean(k(uId, "budget_alerts_enabled"), updated.budgetAlertsEnabled)
            .putBoolean(k(uId, "payment_reminders_enabled"), updated.paymentRemindersEnabled)
            .putBoolean(k(uId, "weekly_summary_enabled"), updated.weeklySummaryEnabled)
            .putBoolean(k(uId, "monthly_summary_enabled"), updated.monthlySummaryEnabled)
            .putBoolean(k(uId, "wifi_only_sync"), updated.wifiOnlySync)
            .putBoolean(k(uId, "privacy_mode_enabled"), updated.privacyModeEnabled)
            .putBoolean(k(uId, "exclude_all_accounts_dashboard"), updated.excludeAllAccountsFromDashboard)
            .putStringSet(k(uId, "excluded_account_ids"), updated.excludedAccountIds)
            .putBoolean(k(uId, "auto_no_affect_inventory_for_assets"), updated.autoNoAffectInventoryForAssets)
            .putBoolean(k(uId, "auto_no_affect_dashboard_for_assets"), updated.autoNoAffectDashboardForAssets)
            .putBoolean(k(uId, "exclude_all_fixed_assets_dashboard"), updated.excludeAllFixedAssetsFromDashboard)
            .putBoolean(k(uId, "privacy_policy_accepted"), updated.privacyPolicyAccepted)
            .putString(k(uId, "privacy_policy_version"), updated.privacyPolicyVersion)
            .putLong(k(uId, "privacy_policy_accepted_timestamp"), updated.privacyPolicyAcceptedTimestamp)
            .putLong(k(uId, "version"), nextVersion)
            .apply()

        _userPreferencesFlows.getOrPut(uId) { MutableStateFlow(updated) }.value = updated
        _preferencesFlow.value = updated
        return Result.success(updated)
    }

    override suspend fun togglePrivacyMode(userId: String, enabled: Boolean): Result<Unit> {
        val current = getPreferences(userId)
        return updatePreferences(current.copy(privacyModeEnabled = enabled)).map { }
    }

    override suspend fun setBaseCurrency(userId: String, currency: String): Result<Unit> {
        val current = getPreferences(userId)
        return updatePreferences(current.copy(baseCurrency = currency)).map { }
    }

    override suspend fun setLanguage(userId: String, language: String): Result<Unit> {
        val current = getPreferences(userId)
        return updatePreferences(current.copy(language = language)).map { }
    }

    override suspend fun setTheme(userId: String, theme: String): Result<Unit> {
        val current = getPreferences(userId)
        return updatePreferences(current.copy(theme = theme)).map { }
    }

    override suspend fun setBiometricsEnabled(userId: String, enabled: Boolean): Result<Unit> {
        val current = getPreferences(userId)
        return updatePreferences(current.copy(biometricsEnabled = enabled)).map { }
    }

    override suspend fun setAllowScreenshots(userId: String, allowed: Boolean): Result<Unit> {
        val current = getPreferences(userId)
        val res = updatePreferences(current.copy(allowScreenshots = allowed))
        try {
            auditSecurityLogDao?.insertLog(
                com.example.data.local.entity.AuditSecurityLog(
                    eventType = "CONFIG_SCREENSHOTS_POLICY",
                    authMethod = "SETTINGS_SECURITY",
                    details = "Ajuste de Capturas de Pantalla: ${if (allowed) "PERMITIDAS (FLAG_SECURE desactivado por el usuario)" else "BLOQUEADAS (FLAG_SECURE activo)"}"
                )
            )
        } catch (_: Exception) {}
        return res.map { }
    }

    override suspend fun setBudgetAlertsEnabled(userId: String, enabled: Boolean): Result<Unit> {
        val current = getPreferences(userId)
        return updatePreferences(current.copy(budgetAlertsEnabled = enabled)).map { }
    }

    override suspend fun setPaymentRemindersEnabled(userId: String, enabled: Boolean): Result<Unit> {
        val current = getPreferences(userId)
        return updatePreferences(current.copy(paymentRemindersEnabled = enabled)).map { }
    }

    override suspend fun setExcludeAllAccountsFromDashboard(userId: String, excluded: Boolean): Result<Unit> {
        val current = getPreferences(userId)
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
        val current = getPreferences(userId)
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
        val current = getPreferences(userId)
        return current.excludeAllAccountsFromDashboard || current.excludedAccountIds.contains(accountId)
    }

    override suspend fun setAutoNoAffectInventoryForAssets(userId: String, enabled: Boolean): Result<Unit> {
        val current = getPreferences(userId)
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
        val current = getPreferences(userId)
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
        val current = getPreferences(userId)
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

    override suspend fun setPrivacyPolicyConsent(userId: String, accepted: Boolean, version: String): Result<Unit> {
        val now = System.currentTimeMillis()
        val current = getPreferences(userId)
        val res = updatePreferences(
            current.copy(
                privacyPolicyAccepted = accepted,
                privacyPolicyVersion = version,
                privacyPolicyAcceptedTimestamp = if (accepted) now else 0L
            )
        )
        com.example.domain.security.FirebaseApiProtectionGuard.setPrivacyConsent(
            granted = accepted,
            version = version,
            timestampMs = if (accepted) now else 0L
        )
        try {
            auditSecurityLogDao?.insertLog(
                com.example.data.local.entity.AuditSecurityLog(
                    eventType = if (accepted) "PRIVACY_CONSENT_GRANTED" else "PRIVACY_CONSENT_REVOKED",
                    authMethod = "LEY_1581_CONSENT",
                    details = "Autorización previa e informada Ley 1581 de 2012 / Decreto 1377 de 2013: ${if (accepted) "OTORGADA" else "REVOCADA"}, versión: $version, timestamp UTC: $now"
                )
            )
        } catch (_: Exception) {}
        return res.map { }
    }

    override suspend fun isPrivacyPolicyAccepted(userId: String): Boolean {
        return getPreferences(userId).privacyPolicyAccepted
    }

    override suspend fun executeProtocolLogout(userId: String): Result<Unit> {
        // Enforce protocol: purge active user cache
        val uId = if (userId.isBlank()) "user_default" else userId
        val editor = prefs.edit()
        val allKeys = prefs.all.keys
        for (key in allKeys) {
            if (key.startsWith("${uId}_")) {
                editor.remove(key)
            }
        }
        editor.apply()
        _userPreferencesFlows.remove(uId)
        _preferencesFlow.value = UserPreferencesDomain()
        return Result.success(Unit)
    }
}
