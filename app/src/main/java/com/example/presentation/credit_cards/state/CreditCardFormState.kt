package com.example.presentation.credit_cards.state

import com.example.domain.models.CreditCardBillingCycle
import com.example.domain.models.CreditLimitValidationResult
import com.example.domain.models.InstallmentProjection
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Robust UI State for the "Agregar Tarjeta de Crédito / Nueva Deuda" module.
 * Represents all form fields, real-time validations, animated quota progress,
 * and banking billing cycle calculations without mock/hardcoded values.
 */
data class CreditCardFormState(
    // Accordion expand/collapse states
    val isBasicInfoExpanded: Boolean = true,
    val isBalanceExpanded: Boolean = true,
    val isInterestExpanded: Boolean = false,
    val isInstallmentsExpanded: Boolean = false,
    val isPaymentConfigExpanded: Boolean = false,
    val isNetWorthExpanded: Boolean = false,
    val isDocumentsExpanded: Boolean = false,
    val isRemindersExpanded: Boolean = false,
    val isFavoritesExpanded: Boolean = false,
    val isDetailsExpanded: Boolean = false,

    // 1. Información básica (Clean defaults, no hardcoded mock data)
    val debtType: String = "Tarjeta de crédito",
    val debtName: String = "",
    val creditorBank: String = "Bancolombia",
    val availableBanks: List<String> = listOf(
        "Bancolombia",
        "Banco de Bogotá",
        "Davivienda",
        "BBVA Colombia",
        "Scotiabank Colpatria",
        "Banco Falabella",
        "Nu Colombia (Nubank)",
        "Banco Itaú",
        "Banco de Occidente",
        "Lulo Bank",
        "Fondo de Empleados / Cooperativa",
        "Entidad Externa / Particular"
    ),
    val currencyCode: String = "COP",
    val currencyName: String = "COP - Peso colombiano",
    val acquisitionDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
    val debtStatus: String = "Activa",
    val franchise: String = "VISA", // VISA, MASTERCARD, AMEX, DINERS, OTRA
    val category: String = "CLÁSICA", // CLÁSICA, ORO, PLATINUM, SIGNATURE, BLACK, INFINITE

    // 2. Saldos y Capital (Revolving Debt Core - Clean 0.0 starting points)
    val totalQuota: Double = 0.0,
    val usedQuota: Double = 0.0,
    val availableQuota: Double = 0.0,
    val retainedQuota: Double = 0.0,
    val updateDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
    val cutOffDate: String = "",
    val lastPaymentDate: String = "",
    val estimatedEndDate: String = "",

    // Billing Engine state
    val cutOffDay: Int = 15,
    val paymentDueDay: Int = 30,
    val billingCycle: CreditCardBillingCycle? = null,
    val limitValidation: CreditLimitValidationResult? = null,

    // 3. Intereses
    val generatesInterest: Boolean = true,
    val interestRateEa: Double = 24.50,
    val rateType: String = "Efectiva anual (EA)",
    val purchasesRateEa: Double = 24.50,
    val advancesRateEa: Double = 28.00,
    val hasGracePeriod: Boolean = true,
    val lateInterestRateMonthly: Double = 2.50,
    val interestCutoffDate: String = "",
    val currentInterestAmount: Double = 0.0,

    // 4. Plazos y Cuotas
    val totalInstallments: Int = 12,
    val paidInstallments: Int = 0,
    val pendingInstallments: Int = 12,
    val installmentValue: Double = 0.0,
    val frequency: String = "Mensual",
    val nextInstallmentDate: String = "",
    val amortizationSystem: String = "Francés (cuotas iguales)",
    val installmentProjection: InstallmentProjection? = null,

    // 5. Configuración de Pagos
    val paymentMethod: String = "Manual",
    val sourceAccount: String = "Seleccionar cuenta",
    val sourceAccountId: String? = null,
    val availableSourceAccounts: List<Pair<String, String>> = emptyList(),
    val regularPaymentDay: Int = 30,
    val minimumPaymentEnabled: Boolean = false,
    val minimumPaymentAmount: Double = 0.0,
    val scheduledPaymentEnabled: Boolean = false,
    val scheduledPaymentDate: String = "",
    val extraPaymentsEnabled: Boolean = false,
    val extraPaymentReduces: String = "Ambos", // Ambos, Plazo, Cuota

    // 6. Impacto en el Patrimonio Neto
    val includeInNetWorth: Boolean = true,
    val financesAsset: Boolean = false,
    val relatedAsset: String = "Selecciona un activo",
    val assetValue: Double = 0.0,

    // 7. Documentos adjuntos (Soporte digital real)
    val attachedDocumentUri: String? = null,
    val attachedDocumentName: String? = null,
    val attachedDocumentType: String? = null, // "PDF", "IMAGE", "DOCUMENT"
    val attachedDocumentSizeKb: Long = 0L,

    // 8. Recordatorios y Notificaciones reales
    val remindersEnabled: Boolean = true,
    val reminderDaysAhead: Int = 3, // 0 = mismo día, 1, 3, 5 días antes
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val lastTestNotificationTime: Long = 0L,

    // 9. Más detalles (Información extendida de la deuda)
    val referenceNumber: String = "",
    val creditorPhone: String = "",
    val paymentChannel: String = "PSE / App Bancaria",
    val selectedTags: List<String> = listOf("Personal"),
    val insuranceMonthlyFee: Double = 0.0,
    val notes: String = "",
    val isFavorite: Boolean = false,

    // Operation status
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)
