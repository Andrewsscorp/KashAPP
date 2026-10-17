package com.example.domain.models

/**
 * Domain representation of Credit Card billing cycle dates, grace period status, and alerts.
 */
data class CreditCardBillingCycle(
    val referenceDate: String, // e.g. "2026-09-27"
    val cutOffDay: Int,
    val paymentDueDay: Int,
    val nextCutOffDateFormatted: String, // e.g. "25/10/2026"
    val nextPaymentDueDateFormatted: String, // e.g. "10/10/2026"
    val businessPaymentDueDateFormatted: String, // e.g. "12/10/2026"
    val isWeekendPayment: Boolean,
    val businessDayNotice: String?, // "Tu fecha límite hábil será el lunes 12/10/2026"
    val currentBilledBalance: Double = 0.0,
    val isGracePeriodActiveForPurchases: Boolean = true,
    val singleQuotaInterestProjected: Double = 0.0, // $0 interest when 1 installment paid before due date
    val cashAdvanceDailyInterestNotice: String = "Los retiros y avances no tienen periodo de gracia y generan interés diario desde el primer segundo."
)

/**
 * Result evaluation for credit limit mathematical consistency.
 * Master Equation: Cupo Total = Cupo Disponible + Cupo Utilizado + Cupo Retenido.
 */
data class CreditLimitValidationResult(
    val isValid: Boolean,
    val totalQuota: Double,
    val availableQuota: Double,
    val usedQuota: Double,
    val retainedQuota: Double,
    val usagePercentage: Float, // 0f to 100f
    val paidPercentage: Float,  // 0f to 100f
    val errorMessage: String? = null
)

/**
 * Amortization quote projection for installment purchases on credit card.
 */
data class InstallmentProjection(
    val numberOfInstallments: Int,
    val annualRateEa: Double,
    val monthlyRateEffective: Double,
    val installmentMonthlyPayment: Double,
    val totalInterestPayable: Double,
    val totalAmountPayable: Double
)
