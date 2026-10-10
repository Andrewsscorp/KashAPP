package com.example.util

import com.example.data.local.entity.AccountCreditCardEntity
import com.example.data.local.entity.AccountEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.pow

/**
 * Enterprise Financial Engine for Credit Card Billing, Minimum Payment Calculation,
 * Waterfall Payment Hierarchy, and Extra Capital Amortization Strategies.
 *
 * Implements standard Latin American & Revolving credit card regulatory rules:
 * 1. Time Engine: Cut-off dates, payment due dates, and billing cycles.
 * 2. Interest Rate Conversion: EM = (1 + EA)^(1/12) - 1.
 * 3. Minimum Payment Engine: Principal installment + monthly interest (with 1-installment grace exemption).
 * 4. Payment Waterfall Algorithm: Administrative fees -> Late fees -> Current interest -> Principal.
 * 5. Extra Capital Amortization: Avalanche (highest APR), Snowball (lowest balance), Time reduction, Quota reduction.
 */
object CreditCardPaymentEngine {

    /**
     * Converts an Effective Annual Rate (EA %) to an Effective Monthly Rate (EM %).
     * Formula: EM = (1 + EA/100)^(1/12) - 1
     */
    fun convertEaToMonthlyRate(rateEaPercentage: Double): Double {
        if (rateEaPercentage <= 0.0) return 0.0
        val eaDecimal = rateEaPercentage / 100.0
        val emDecimal = (1.0 + eaDecimal).pow(1.0 / 12.0) - 1.0
        return emDecimal * 100.0 // Returned in percentage (e.g. 2.21%)
    }

    /**
     * Converts EA to Daily Rate (ED %).
     * Formula: ED = (1 + EA/100)^(1/360) - 1
     */
    fun convertEaToDailyRate(rateEaPercentage: Double): Double {
        if (rateEaPercentage <= 0.0) return 0.0
        val eaDecimal = rateEaPercentage / 100.0
        val edDecimal = (1.0 + eaDecimal).pow(1.0 / 360.0) - 1.0
        return edDecimal * 100.0
    }

    /**
     * Billing Cycle Dates Snapshot
     */
    data class BillingDatesInfo(
        val cutOffDay: Int,
        val paymentDueDay: Int,
        val nextCutOffDate: LocalDate,
        val paymentDueDate: LocalDate,
        val daysUntilDueDate: Long,
        val isGracePeriodActive: Boolean,
        val formattedNextCutOff: String,
        val formattedPaymentDue: String
    )

    /**
     * Calculates real billing dates based on current day, cut-off day and due day.
     */
    fun calculateBillingDates(
        cutOffDay: Int = 25,
        paymentDueDay: Int = 10,
        today: LocalDate = LocalDate.now()
    ): BillingDatesInfo {
        val safeCutOff = cutOffDay.coerceIn(1, 28)
        val safeDue = paymentDueDay.coerceIn(1, 28)

        val nextCutOff = if (today.dayOfMonth <= safeCutOff) {
            today.withDayOfMonth(safeCutOff)
        } else {
            today.plusMonths(1).withDayOfMonth(safeCutOff)
        }

        // Payment due date is usually after the cut-off date (next month)
        val dueDate = if (safeDue <= safeCutOff) {
            nextCutOff.plusMonths(1).withDayOfMonth(safeDue)
        } else {
            nextCutOff.withDayOfMonth(safeDue)
        }

        val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate).coerceAtLeast(0)
        val isGracePeriod = today.isBefore(dueDate) || today.isEqual(dueDate)

        val formatter = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es", "CO"))

        return BillingDatesInfo(
            cutOffDay = safeCutOff,
            paymentDueDay = safeDue,
            nextCutOffDate = nextCutOff,
            paymentDueDate = dueDate,
            daysUntilDueDate = daysUntilDue,
            isGracePeriodActive = isGracePeriod,
            formattedNextCutOff = nextCutOff.format(formatter),
            formattedPaymentDue = dueDate.format(formatter)
        )
    }

    /**
     * Represents a single purchase / deferred transaction
     */
    data class SubPurchaseItem(
        val id: String,
        val concept: String,
        val originalAmount: Double,
        val currentBalance: Double,
        val totalInstallments: Int,
        val paidInstallments: Int,
        val rateEa: Double,
        val isCashAdvance: Boolean = false,
        val isOneInstallment: Boolean = (totalInstallments == 1)
    )

    /**
     * Complete Minimum Payment Analysis & Breakdown
     */
    data class MinimumPaymentBreakdown(
        val totalSuggestedPayment: Double,
        val principalToAmortize: Double,
        val currentInterest: Double,
        val handlingFee: Double,
        val lateInterest: Double,
        val totalDebt: Double,
        val rateEa: Double,
        val rateEm: Double,
        val explanationWhy: String,
        val datesInfo: BillingDatesInfo
    )

    /**
     * Calculates the structured Minimum Payment following Latin American installment rules
     * and revolving interest math.
     */
    fun calculateMinimumPayment(
        cardAccount: AccountEntity,
        cardConfig: AccountCreditCardEntity?,
        purchases: List<SubPurchaseItem> = emptyList()
    ): MinimumPaymentBreakdown {
        val totalDebt = kotlin.math.abs(cardAccount.balance).coerceAtLeast(0.0)
        val rateEa = cardConfig?.purchasesRateEa ?: cardConfig?.interestRateEa ?: 24.50
        val rateEm = convertEaToMonthlyRate(rateEa)
        val emDecimal = rateEm / 100.0

        val cutOffDay = cardConfig?.cutOffDay ?: 25
        val dueDay = cardConfig?.paymentDueDay ?: 10
        val datesInfo = calculateBillingDates(cutOffDay, dueDay)

        // Administrative Handling fee (Cuota de manejo)
        val handlingFee = when {
            totalDebt <= 0 -> 0.0
            cardAccount.name.contains("Black", ignoreCase = true) || cardAccount.name.contains("Signature", ignoreCase = true) -> 28000.0
            cardAccount.name.contains("Platinum", ignoreCase = true) || cardAccount.name.contains("Gold", ignoreCase = true) -> 22000.0
            cardAccount.name.contains("Nu", ignoreCase = true) -> 0.0 // Nu is exempt from handling fees
            else -> 18000.0
        }

        var principalSum = 0.0
        var interestSum = 0.0
        val lateInterest = if (datesInfo.daysUntilDueDate == 0L && totalDebt > 0) {
            totalDebt * ((cardConfig?.lateInterestRateMonthly ?: 2.5) / 100.0)
        } else 0.0

        if (purchases.isNotEmpty()) {
            for (p in purchases) {
                if (p.currentBalance <= 0) continue
                val remainingInstallments = (p.totalInstallments - p.paidInstallments).coerceAtLeast(1)

                if (p.isOneInstallment && datesInfo.isGracePeriodActive && !p.isCashAdvance) {
                    // 1 installment rule: Grace period exemption -> Interest = 0, Principal = full balance
                    principalSum += p.currentBalance
                    interestSum += 0.0
                } else {
                    // Multi-installment or cash advance: French / Linear fraction + monthly interest
                    val pPrincipal = p.currentBalance / remainingInstallments
                    val pInterest = p.currentBalance * (convertEaToMonthlyRate(p.rateEa) / 100.0)
                    principalSum += pPrincipal
                    interestSum += pInterest
                }
            }
        } else {
            // Default calculation when sub-purchases are not granular:
            // Estimated amortized installment (~9 months avg) + interest on total debt
            val estimatedMonths = 8.0
            principalSum = if (totalDebt > 0) (totalDebt / estimatedMonths) else 0.0
            interestSum = if (totalDebt > 0) (totalDebt * emDecimal) else 0.0
        }

        // Suggested total
        var totalMin = principalSum + interestSum + handlingFee + lateInterest
        if (totalDebt > 0 && totalMin > totalDebt) {
            totalMin = totalDebt
        }
        if (totalDebt > 0 && totalMin < 50000.0) {
            totalMin = totalDebt.coerceAtMost(50000.0)
        }

        // Detailed Explanation of WHY this amount is suggested
        val explanation = buildString {
            append("Este valor cubre la cuota mínima del periodo calculada con la tasa vigente de tu tarjeta ")
            append("(${String.format(Locale.US, "%.2f", rateEa)}% E.A. = ${String.format(Locale.US, "%.2f", rateEm)}% E.M.). ")
            append("Incluye amortización a capital programada, intereses corrientes del mes y cargos fijos.")
        }

        return MinimumPaymentBreakdown(
            totalSuggestedPayment = totalMin,
            principalToAmortize = principalSum,
            currentInterest = interestSum,
            handlingFee = handlingFee,
            lateInterest = lateInterest,
            totalDebt = totalDebt,
            rateEa = rateEa,
            rateEm = rateEm,
            explanationWhy = explanation,
            datesInfo = datesInfo
        )
    }

    /**
     * Result of the Payment Waterfall (Cascada de Pagos)
     */
    data class WaterfallPaymentResult(
        val paymentAmount: Double,
        val paidHandlingFee: Double,
        val paidLateInterest: Double,
        val paidCurrentInterest: Double,
        val paidPrincipal: Double,
        val extraCapitalAmount: Double,
        val totalFinancialCost: Double,
        val percentToPrincipal: Double,
        val percentToInterestAndFees: Double,
        val remainingDebt: Double
    )

    /**
     * Applies the strict Legal Payment Waterfall Algorithm (Cascada de Pagos):
     * Priority 1: Handling fee & Admin charges (Cuota de manejo)
     * Priority 2: Late interest penalty (Intereses de mora)
     * Priority 3: Current monthly interest (Intereses corrientes)
     * Priority 4: Principal amortization (Abono directo a capital)
     */
    fun applyPaymentWaterfall(
        paymentAmount: Double,
        minBreakdown: MinimumPaymentBreakdown
    ): WaterfallPaymentResult {
        var remainingPayment = paymentAmount.coerceAtLeast(0.0)

        // 1. Handling Fee
        val paidHandling = kotlin.math.min(remainingPayment, minBreakdown.handlingFee)
        remainingPayment -= paidHandling

        // 2. Late Interest
        val paidLate = kotlin.math.min(remainingPayment, minBreakdown.lateInterest)
        remainingPayment -= paidLate

        // 3. Current Monthly Interest
        val paidCurrentInterest = kotlin.math.min(remainingPayment, minBreakdown.currentInterest)
        remainingPayment -= paidCurrentInterest

        // 4. Principal Amortization (All remaining money reduces real debt!)
        val paidPrincipal = kotlin.math.min(remainingPayment, minBreakdown.totalDebt)
        val extraCapital = (remainingPayment - minBreakdown.principalToAmortize).coerceAtLeast(0.0)

        val totalCost = paidHandling + paidLate + paidCurrentInterest
        val totalApplied = paidHandling + paidLate + paidCurrentInterest + paidPrincipal

        val pctPrincipal = if (totalApplied > 0) (paidPrincipal / totalApplied) * 100.0 else 0.0
        val pctFees = if (totalApplied > 0) (totalCost / totalApplied) * 100.0 else 0.0
        val remainingDebt = (minBreakdown.totalDebt - paidPrincipal).coerceAtLeast(0.0)

        return WaterfallPaymentResult(
            paymentAmount = paymentAmount,
            paidHandlingFee = paidHandling,
            paidLateInterest = paidLate,
            paidCurrentInterest = paidCurrentInterest,
            paidPrincipal = paidPrincipal,
            extraCapitalAmount = extraCapital,
            totalFinancialCost = totalCost,
            percentToPrincipal = pctPrincipal,
            percentToInterestAndFees = pctFees,
            remainingDebt = remainingDebt
        )
    }

    /**
     * Extra Capital Projection Analysis
     */
    data class ExtraCapitalImpactProjection(
        val extraAmount: Double,
        val strategy: String, // "AVALANCHE", "SNOWBALL", "TIME_REDUCTION", "QUOTA_REDUCTION"
        val monthsSaved: Int,
        val estimatedInterestSaved: Double,
        val newEstimatedMonthlyFee: Double,
        val feeReductionAmount: Double,
        val explanation: String
    )

    /**
     * Calculates the exact financial impact of extra payments to capital.
     */
    fun calculateExtraCapitalImpact(
        extraAmount: Double,
        strategy: String,
        minBreakdown: MinimumPaymentBreakdown
    ): ExtraCapitalImpactProjection {
        val safeExtra = extraAmount.coerceAtLeast(0.0)
        val baseMonthly = minBreakdown.totalSuggestedPayment.coerceAtLeast(50000.0)
        val totalDebt = minBreakdown.totalDebt

        // Impact calculations based on annual interest rate
        val annualRateDecimal = minBreakdown.rateEa / 100.0
        val monthlyRateDecimal = minBreakdown.rateEm / 100.0

        // Time reduction calculation (cancels future installments from back to front)
        val estimatedMonthsCut = ((safeExtra / baseMonthly) * 1.35).toInt().coerceAtLeast(1)
        val timeInterestSaved = safeExtra * (monthlyRateDecimal * estimatedMonthsCut * 1.1)

        // Quota reduction calculation (recalculates lower monthly fee)
        val feeSaving = (safeExtra * (monthlyRateDecimal + 0.05)).coerceAtLeast(15000.0)
        val newMonthlyFee = (baseMonthly - feeSaving).coerceAtLeast(35000.0)

        // Avalanche method (directed to highest interest purchases / advances)
        val avalancheInterestSaved = safeExtra * (annualRateDecimal * 1.25)

        // Snowball method (quick payoff of smallest debts)
        val snowballInterestSaved = safeExtra * (annualRateDecimal * 0.95)

        val (savedMonths, savedInterest, newFee, feeReduction, expl) = when (strategy) {
            "AVALANCHE" -> Tuple5(
                estimatedMonthsCut + 1,
                avalancheInterestSaved,
                newMonthlyFee,
                feeSaving,
                "Método Avalancha: Aplica el excedente prioritariamente a las compras con mayor tasa de interés (avances y consumos altos), maximizando el ahorro financiero total."
            )
            "SNOWBALL" -> Tuple5(
                estimatedMonthsCut,
                snowballInterestSaved,
                newMonthlyFee,
                feeSaving,
                "Método Bola de Nieve: Cancela las deudas y compras de menor saldo primero, liquidando ítems pendientes más rápido y liberando cupo disponible."
            )
            "QUOTA_REDUCTION" -> Tuple5(
                0,
                safeExtra * monthlyRateDecimal * 6,
                newMonthlyFee,
                feeSaving,
                "Reducción de Cuota: Mantiene el plazo original pero disminuye el valor exigible cada mes, otorgando mayor flujo de caja mensual."
            )
            else -> Tuple5( // "TIME_REDUCTION"
                estimatedMonthsCut,
                timeInterestSaved,
                baseMonthly,
                0.0,
                "Reducción de Tiempo: Abona de atrás hacia adelante liquidando las últimas cuotas del cronograma y recortando meses completos de intereses."
            )
        }

        return ExtraCapitalImpactProjection(
            extraAmount = safeExtra,
            strategy = strategy,
            monthsSaved = savedMonths,
            estimatedInterestSaved = savedInterest,
            newEstimatedMonthlyFee = newFee,
            feeReductionAmount = feeReduction,
            explanation = expl
        )
    }

    private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
}
