package com.example.features.settings.domain.model

/**
 * Enterprise domain model for user settings and preferences.
 */
data class UserPreferencesDomain(
    val userId: String = "user_default",
    val language: String = "es",
    val baseCurrency: String = "COP",
    val theme: String = "SYSTEM",
    val fontSize: String = "MEDIUM",
    val dashboardLayout: List<String> = listOf("net_worth", "cash_flow", "budgets", "goals", "accounts"),
    val biometricsEnabled: Boolean = false,
    val pushNotifications: Boolean = true,
    val budgetAlertsEnabled: Boolean = true,
    val paymentRemindersEnabled: Boolean = true,
    val weeklySummaryEnabled: Boolean = true,
    val monthlySummaryEnabled: Boolean = true,
    val wifiOnlySync: Boolean = false,
    val privacyModeEnabled: Boolean = false,
    val excludeAllAccountsFromDashboard: Boolean = false,
    val excludedAccountIds: Set<String> = emptySet(),
    val autoNoAffectInventoryForAssets: Boolean = false,
    val autoNoAffectDashboardForAssets: Boolean = false,
    val excludeAllFixedAssetsFromDashboard: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
)

enum class SettingsSubRoute {
    MAIN,
    MI_PERFIL,
    SEGURIDAD,
    AUDITORIA,
    NOTIFICACIONES,
    MONEDA_TRM,
    CATEGORIAS,
    CUENTAS_ENTIDADES,
    TIENDA,
    TERCEROS,
    ACTIVOS_FIJOS,
    APARIENCIA,
    PERSONALIZAR_INICIO,
    EXPORTAR_DATOS,
    COPIA_SEGURIDAD,
    AYUDA_SOPORTE,
    ACERCA_DE
}
