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
        try {
            com.example.data.local.SecurePreferencesVault.getEncryptedSettingsPreferences(context)
        } catch (t: Throwable) {
            android.util.Log.w("SettingsRepository", "Fallback a preferencias locales: ${t.message}")
            context.getSharedPreferences("kashapp_enterprise_settings_prefs", Context.MODE_PRIVATE)
        }
    }

    private val _userPreferencesFlows = java.util.concurrent.ConcurrentHashMap<String, MutableStateFlow<UserPreferencesDomain>>()
    private val _preferencesFlow = MutableStateFlow(loadPreferences("user_default"))

    private fun k(userId: String, key: String): String = if (userId.isBlank()) key else "${userId}_$key"

    private fun migrateLegacyNotificationPrefsIfNeeded(userId: String) {
        val migrationKey = k(userId, "notif_migrated_legacy_prefs")
        if (prefs.getBoolean(migrationKey, false)) return
        try {
            val legacyManager = com.example.presentation.payments.notification.UpcomingPaymentsNotificationPrefsManager(context)
            val legacyConfig = legacyManager.getConfig()
            prefs.edit()
                .putBoolean(k(userId, "notif_payment_enabled"), legacyConfig.enabled)
                .putStringSet(k(userId, "notif_payment_days"), setOf(legacyConfig.advanceNoticeDays.toString(), "1", "0"))
                .putInt(k(userId, "notif_payment_hour"), legacyConfig.preferredHour)
                .putInt(k(userId, "notif_payment_minute"), legacyConfig.preferredMinute)
                .putBoolean(migrationKey, true)
                .apply()
        } catch (_: Throwable) {
            prefs.edit().putBoolean(migrationKey, true).apply()
        }
    }

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
        migrateLegacyNotificationPrefsIfNeeded(uId)

        val budgetEnabled = prefs.getBoolean(k(uId, "notif_budget_enabled"), prefs.getBoolean(k(uId, "budget_alerts_enabled"), true))
        val paymentEnabled = prefs.getBoolean(k(uId, "notif_payment_enabled"), prefs.getBoolean(k(uId, "payment_reminders_enabled"), true))
        val weeklyEnabled = prefs.getBoolean(k(uId, "notif_weekly_enabled"), prefs.getBoolean(k(uId, "weekly_summary_enabled"), true))

        val daysStrings = prefs.getStringSet(k(uId, "notif_payment_days"), setOf("3", "1", "0")) ?: setOf("3", "1", "0")
        val daysSet = daysStrings.mapNotNull { it.toIntOrNull() }.toSet().ifEmpty { setOf(3, 1, 0) }

        val notificationConfig = com.example.features.settings.domain.model.NotificationSettingsConfig(
            budgetPolicy = com.example.features.settings.domain.model.BudgetAlertPolicy(
                isEnabled = budgetEnabled,
                warningThresholdPercent = prefs.getInt(k(uId, "notif_budget_threshold"), 80),
                notifyOnCritical100Percent = prefs.getBoolean(k(uId, "notif_budget_100"), true),
                suggestEnvelopeRebalance = prefs.getBoolean(k(uId, "notif_budget_rebalance"), true)
            ),
            paymentPolicy = com.example.features.settings.domain.model.PaymentAndCardAlertPolicy(
                isEnabled = paymentEnabled,
                daysBeforeDueDate = daysSet,
                preferredAlertHour = prefs.getInt(k(uId, "notif_payment_hour"), 9),
                preferredAlertMinute = prefs.getInt(k(uId, "notif_payment_minute"), 0),
                includeMinimumPaymentPreview = prefs.getBoolean(k(uId, "notif_payment_minimum"), true)
            ),
            securityPolicy = com.example.features.settings.domain.model.SecurityAndAnomalyAlertPolicy(
                overdraftImmediateAlert = prefs.getBoolean(k(uId, "notif_security_overdraft"), true),
                mlUnusualExpenseAlert = prefs.getBoolean(k(uId, "notif_security_ml_anomalies"), true),
                failedAuthAttemptAlert = prefs.getBoolean(k(uId, "notif_security_failed_auth"), true),
                highValueTransactionThresholdCop = prefs.getLong(k(uId, "notif_security_high_value_cop"), 1_000_000L)
            ),
            taxPolicy = com.example.features.settings.domain.model.TaxAndPatrimonyAlertPolicy(
                dianTaxCalendarAlerts = prefs.getBoolean(k(uId, "notif_tax_dian"), true),
                uvtThresholdWarningPercent = prefs.getInt(k(uId, "notif_tax_uvt_threshold"), 85),
                gmfExemptionAlert = prefs.getBoolean(k(uId, "notif_tax_gmf"), true),
                fixedAssetMaintenanceAndDepreciationAlerts = prefs.getBoolean(k(uId, "notif_tax_fixed_assets"), true),
                cdtMaturityAlerts = prefs.getBoolean(k(uId, "notif_tax_cdt"), true),
                sharedDebtRemindersEnabled = prefs.getBoolean(k(uId, "notif_tax_shared_debt"), true)
            ),
            weeklyDigestPolicy = com.example.features.settings.domain.model.WeeklyDigestPolicy(
                isEnabled = weeklyEnabled,
                dayOfWeekIso = prefs.getInt(k(uId, "notif_weekly_day_iso"), 1),
                hourOfDay = prefs.getInt(k(uId, "notif_weekly_hour"), 8),
                minute = prefs.getInt(k(uId, "notif_weekly_minute"), 0),
                includeAiInsights = prefs.getBoolean(k(uId, "notif_weekly_ai_insights"), true)
            ),
            privacyPolicy = com.example.features.settings.domain.model.PrivacyAndDeliveryPolicy(
                maskAmountsOnLockScreen = prefs.getBoolean(k(uId, "notif_privacy_mask_amounts"), true),
                requireBiometricForNotificationActions = prefs.getBoolean(k(uId, "notif_privacy_require_biometric"), true),
                quietHoursEnabled = prefs.getBoolean(k(uId, "notif_privacy_quiet_hours_enabled"), false),
                quietStartHour = prefs.getInt(k(uId, "notif_privacy_quiet_start_hour"), 22),
                quietEndHour = prefs.getInt(k(uId, "notif_privacy_quiet_end_hour"), 7),
                allowCriticalSecurityInQuietHours = prefs.getBoolean(k(uId, "notif_privacy_quiet_allow_critical"), true)
            )
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
            budgetAlertsEnabled = budgetEnabled,
            paymentRemindersEnabled = paymentEnabled,
            weeklySummaryEnabled = weeklyEnabled,
            monthlySummaryEnabled = prefs.getBoolean(k(uId, "monthly_summary_enabled"), prefs.getBoolean("monthly_summary_enabled", true)),
            notificationSettings = notificationConfig,
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
            autoLockTimeoutMinutes = prefs.getInt(k(uId, "auto_lock_timeout_minutes"), prefs.getInt("auto_lock_timeout_minutes", 1)),
            hasPinEnabled = prefs.getBoolean(k(uId, "has_pin_enabled"), prefs.getBoolean("has_pin_enabled", false)),
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

    override fun getPreferencesSnapshot(userId: String): UserPreferencesDomain {
        val uId = if (userId.isBlank()) "user_default" else userId
        return _userPreferencesFlows[uId]?.value ?: loadPreferences(uId)
    }

    override suspend fun updatePreferences(preferences: UserPreferencesDomain): Result<UserPreferencesDomain> {
        val uId = if (preferences.userId.isBlank()) "user_default" else preferences.userId
        val nextVersion = preferences.version + 1
        val updated = preferences.copy(userId = uId, version = nextVersion, updatedAt = System.currentTimeMillis())
        val notif = updated.notificationSettings
        prefs.edit()
            .putString(k(uId, "language"), updated.language)
            .putString(k(uId, "base_currency"), updated.baseCurrency)
            .putString(k(uId, "theme"), updated.theme)
            .putString(k(uId, "font_size"), updated.fontSize)
            .putBoolean(k(uId, "biometrics_enabled"), updated.biometricsEnabled)
            .putBoolean(k(uId, "allow_screenshots"), updated.allowScreenshots)
            .putBoolean(k(uId, "push_notifications"), updated.pushNotifications)
            .putBoolean(k(uId, "budget_alerts_enabled"), notif.budgetPolicy.isEnabled)
            .putBoolean(k(uId, "payment_reminders_enabled"), notif.paymentPolicy.isEnabled)
            .putBoolean(k(uId, "weekly_summary_enabled"), notif.weeklyDigestPolicy.isEnabled)
            .putBoolean(k(uId, "monthly_summary_enabled"), updated.monthlySummaryEnabled)
            .putBoolean(k(uId, "notif_budget_enabled"), notif.budgetPolicy.isEnabled)
            .putInt(k(uId, "notif_budget_threshold"), notif.budgetPolicy.warningThresholdPercent)
            .putBoolean(k(uId, "notif_budget_100"), notif.budgetPolicy.notifyOnCritical100Percent)
            .putBoolean(k(uId, "notif_budget_rebalance"), notif.budgetPolicy.suggestEnvelopeRebalance)
            .putBoolean(k(uId, "notif_payment_enabled"), notif.paymentPolicy.isEnabled)
            .putStringSet(k(uId, "notif_payment_days"), notif.paymentPolicy.daysBeforeDueDate.map { it.toString() }.toSet())
            .putInt(k(uId, "notif_payment_hour"), notif.paymentPolicy.preferredAlertHour)
            .putInt(k(uId, "notif_payment_minute"), notif.paymentPolicy.preferredAlertMinute)
            .putBoolean(k(uId, "notif_payment_minimum"), notif.paymentPolicy.includeMinimumPaymentPreview)
            .putBoolean(k(uId, "notif_security_overdraft"), notif.securityPolicy.overdraftImmediateAlert)
            .putBoolean(k(uId, "notif_security_ml_anomalies"), notif.securityPolicy.mlUnusualExpenseAlert)
            .putBoolean(k(uId, "notif_security_failed_auth"), notif.securityPolicy.failedAuthAttemptAlert)
            .putLong(k(uId, "notif_security_high_value_cop"), notif.securityPolicy.highValueTransactionThresholdCop)
            .putBoolean(k(uId, "notif_tax_dian"), notif.taxPolicy.dianTaxCalendarAlerts)
            .putInt(k(uId, "notif_tax_uvt_threshold"), notif.taxPolicy.uvtThresholdWarningPercent)
            .putBoolean(k(uId, "notif_tax_gmf"), notif.taxPolicy.gmfExemptionAlert)
            .putBoolean(k(uId, "notif_tax_fixed_assets"), notif.taxPolicy.fixedAssetMaintenanceAndDepreciationAlerts)
            .putBoolean(k(uId, "notif_tax_cdt"), notif.taxPolicy.cdtMaturityAlerts)
            .putBoolean(k(uId, "notif_tax_shared_debt"), notif.taxPolicy.sharedDebtRemindersEnabled)
            .putBoolean(k(uId, "notif_weekly_enabled"), notif.weeklyDigestPolicy.isEnabled)
            .putInt(k(uId, "notif_weekly_day_iso"), notif.weeklyDigestPolicy.dayOfWeekIso)
            .putInt(k(uId, "notif_weekly_hour"), notif.weeklyDigestPolicy.hourOfDay)
            .putInt(k(uId, "notif_weekly_minute"), notif.weeklyDigestPolicy.minute)
            .putBoolean(k(uId, "notif_weekly_ai_insights"), notif.weeklyDigestPolicy.includeAiInsights)
            .putBoolean(k(uId, "notif_privacy_mask_amounts"), notif.privacyPolicy.maskAmountsOnLockScreen)
            .putBoolean(k(uId, "notif_privacy_require_biometric"), notif.privacyPolicy.requireBiometricForNotificationActions)
            .putBoolean(k(uId, "notif_privacy_quiet_hours_enabled"), notif.privacyPolicy.quietHoursEnabled)
            .putInt(k(uId, "notif_privacy_quiet_start_hour"), notif.privacyPolicy.quietStartHour)
            .putInt(k(uId, "notif_privacy_quiet_end_hour"), notif.privacyPolicy.quietEndHour)
            .putBoolean(k(uId, "notif_privacy_quiet_allow_critical"), notif.privacyPolicy.allowCriticalSecurityInQuietHours)
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
            .putInt(k(uId, "auto_lock_timeout_minutes"), updated.autoLockTimeoutMinutes)
            .putBoolean(k(uId, "has_pin_enabled"), updated.hasPinEnabled)
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
        val notif = current.notificationSettings
        val updatedNotif = notif.copy(budgetPolicy = notif.budgetPolicy.copy(isEnabled = enabled))
        return updatePreferences(current.copy(budgetAlertsEnabled = enabled, notificationSettings = updatedNotif)).map { }
    }

    override suspend fun setPaymentRemindersEnabled(userId: String, enabled: Boolean): Result<Unit> {
        val current = getPreferences(userId)
        val notif = current.notificationSettings
        val updatedNotif = notif.copy(paymentPolicy = notif.paymentPolicy.copy(isEnabled = enabled))
        return updatePreferences(current.copy(paymentRemindersEnabled = enabled, notificationSettings = updatedNotif)).map { }
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
