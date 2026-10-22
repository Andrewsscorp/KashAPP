package com.example.features.settings.domain.repository

import com.example.features.settings.domain.model.UserPreferencesDomain
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observePreferences(userId: String): Flow<UserPreferencesDomain>
    suspend fun getPreferences(userId: String): UserPreferencesDomain
    suspend fun updatePreferences(preferences: UserPreferencesDomain): Result<UserPreferencesDomain>
    suspend fun togglePrivacyMode(userId: String, enabled: Boolean): Result<Unit>
    suspend fun setBaseCurrency(userId: String, currency: String): Result<Unit>
    suspend fun setLanguage(userId: String, language: String): Result<Unit>
    suspend fun setTheme(userId: String, theme: String): Result<Unit>
    suspend fun setBiometricsEnabled(userId: String, enabled: Boolean): Result<Unit>
    suspend fun setAllowScreenshots(userId: String, allowed: Boolean): Result<Unit> {
        val current = getPreferences(userId)
        return updatePreferences(current.copy(allowScreenshots = allowed)).map { }
    }
    suspend fun setBudgetAlertsEnabled(userId: String, enabled: Boolean): Result<Unit>
    suspend fun setPaymentRemindersEnabled(userId: String, enabled: Boolean): Result<Unit>
    suspend fun setExcludeAllAccountsFromDashboard(userId: String, excluded: Boolean): Result<Unit>
    suspend fun setAccountExcludedFromDashboard(userId: String, accountId: String, excluded: Boolean): Result<Unit>
    suspend fun isAccountExcluded(userId: String, accountId: String): Boolean
    suspend fun setAutoNoAffectInventoryForAssets(userId: String, enabled: Boolean): Result<Unit>
    suspend fun applyNoAffectInventoryToAllAssets(userId: String, noAffect: Boolean): Result<Int>
    suspend fun setAutoNoAffectDashboardForAssets(userId: String, enabled: Boolean): Result<Unit>
    suspend fun applyNoAffectDashboardToAllAssets(userId: String, noAffect: Boolean): Result<Int>
    suspend fun setExcludeAllFixedAssetsFromDashboard(userId: String, excluded: Boolean): Result<Unit>
    suspend fun setPrivacyPolicyConsent(userId: String, accepted: Boolean, version: String = "1.0-LEY-1581-2012"): Result<Unit>
    suspend fun isPrivacyPolicyAccepted(userId: String): Boolean
    suspend fun executeProtocolLogout(userId: String): Result<Unit>
}
