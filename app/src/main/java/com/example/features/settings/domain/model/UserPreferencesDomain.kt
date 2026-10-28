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
    val allowScreenshots: Boolean = false,
    val pushNotifications: Boolean = true,
    val budgetAlertsEnabled: Boolean = true,
    val paymentRemindersEnabled: Boolean = true,
    val weeklySummaryEnabled: Boolean = true,
    val monthlySummaryEnabled: Boolean = true,
    val notificationSettings: NotificationSettingsConfig = NotificationSettingsConfig(),
    val wifiOnlySync: Boolean = false,
    val privacyModeEnabled: Boolean = false,
    val excludeAllAccountsFromDashboard: Boolean = false,
    val excludedAccountIds: Set<String> = emptySet(),
    val autoNoAffectInventoryForAssets: Boolean = false,
    val autoNoAffectDashboardForAssets: Boolean = false,
    val excludeAllFixedAssetsFromDashboard: Boolean = false,
    val privacyPolicyAccepted: Boolean = false,
    val privacyPolicyVersion: String = "1.0-LEY-1581-2012",
    val privacyPolicyAcceptedTimestamp: Long = 0L,
    val autoLockTimeoutMinutes: Int = 1,
    val hasPinEnabled: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
)

/**
 * 1. Presupuestos por Sobres (Granular)
 */
data class BudgetAlertPolicy(
    val isEnabled: Boolean = true,
    val warningThresholdPercent: Int = 80, // [50, 95] en incrementos de 5%
    val notifyOnCritical100Percent: Boolean = true,
    val suggestEnvelopeRebalance: Boolean = true
)

/**
 * 2. Tarjetas de Crédito y Pagos Programados
 */
data class PaymentAndCardAlertPolicy(
    val isEnabled: Boolean = true,
    val daysBeforeDueDate: Set<Int> = setOf(3, 1, 0), // {7, 5, 3, 1, 0} días antes
    val preferredAlertHour: Int = 9, // [0, 23]
    val preferredAlertMinute: Int = 0, // [0, 59]
    val includeMinimumPaymentPreview: Boolean = true
)

/**
 * 3. Sobregiro, Detección de Anomalías con IA y Seguridad
 */
data class SecurityAndAnomalyAlertPolicy(
    val overdraftImmediateAlert: Boolean = true,
    val mlUnusualExpenseAlert: Boolean = true,
    val failedAuthAttemptAlert: Boolean = true,
    val highValueTransactionThresholdCop: Long = 1_000_000L
)

/**
 * 4. Radar Tributario DIAN, UVT, Activos Fijos, CDTs y Deudas
 */
data class TaxAndPatrimonyAlertPolicy(
    val dianTaxCalendarAlerts: Boolean = true,
    val uvtThresholdWarningPercent: Int = 85,
    val gmfExemptionAlert: Boolean = true,
    val fixedAssetMaintenanceAndDepreciationAlerts: Boolean = true,
    val cdtMaturityAlerts: Boolean = true,
    val sharedDebtRemindersEnabled: Boolean = true
)

/**
 * 5. Resumen Financiero Semanal Consolidado
 */
data class WeeklyDigestPolicy(
    val isEnabled: Boolean = true,
    val dayOfWeekIso: Int = 1, // 1 = Lunes .. 7 = Domingo
    val hourOfDay: Int = 8,
    val minute: Int = 0,
    val includeAiInsights: Boolean = true
)

/**
 * 6. Privacidad en Pantalla de Bloqueo y Horario Silencioso (ISO 27001)
 */
data class PrivacyAndDeliveryPolicy(
    val maskAmountsOnLockScreen: Boolean = true,
    val requireBiometricForNotificationActions: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 7,
    val allowCriticalSecurityInQuietHours: Boolean = true
)

/**
 * Configuración global unificada de notificaciones de grado bancario.
 */
data class NotificationSettingsConfig(
    val budgetPolicy: BudgetAlertPolicy = BudgetAlertPolicy(),
    val paymentPolicy: PaymentAndCardAlertPolicy = PaymentAndCardAlertPolicy(),
    val securityPolicy: SecurityAndAnomalyAlertPolicy = SecurityAndAnomalyAlertPolicy(),
    val taxPolicy: TaxAndPatrimonyAlertPolicy = TaxAndPatrimonyAlertPolicy(),
    val weeklyDigestPolicy: WeeklyDigestPolicy = WeeklyDigestPolicy(),
    val privacyPolicy: PrivacyAndDeliveryPolicy = PrivacyAndDeliveryPolicy()
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
