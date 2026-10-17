package com.example.domain.usecase

import com.example.domain.models.CreditCardBillingCycle
import com.example.domain.models.InstallmentProjection
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.pow

/**
 * Banking Core UseCase calculating credit card billing cycles, cut-off dates, payment due dates,
 * weekend rollovers to the next business day (Monday), grace periods, and interest amortization.
 */
class CalculateNextBillingDatesUseCase {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("es", "CO"))

    operator fun invoke(
        cutOffDay: Int,
        paymentDueDay: Int,
        referenceDate: LocalDate = LocalDate.of(2026, 9, 27),
        billedAmount: Double = 8_450_000.0,
        purchasesRateEa: Double = 22.0
    ): CreditCardBillingCycle {
        val safeCutOff = cutOffDay.coerceIn(1, 28)
        val safePaymentDay = paymentDueDay.coerceIn(1, 28)

        val currentDay = referenceDate.dayOfMonth
        val nextCutOffDate: LocalDate
        val nextPaymentDueDate: LocalDate

        if (currentDay > safeCutOff) {
            // Cut-off already happened this month.
            // Next cut-off will be next month.
            nextCutOffDate = referenceDate.plusMonths(1).withDayOfMonth(safeCutOff)
            // Payment due date is for the cycle that just closed on safeCutOff of this month:
            nextPaymentDueDate = referenceDate.plusMonths(1).withDayOfMonth(safePaymentDay)
        } else {
            // Cut-off has not yet occurred this month:
            nextCutOffDate = referenceDate.withDayOfMonth(safeCutOff)
            // Payment due date is next month:
            nextPaymentDueDate = referenceDate.plusMonths(1).withDayOfMonth(safePaymentDay)
        }

        // Check if payment due date falls on Saturday or Sunday
        val dayOfWeek = nextPaymentDueDate.dayOfWeek
        val isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
        val businessPaymentDueDate = when (dayOfWeek) {
            DayOfWeek.SATURDAY -> nextPaymentDueDate.plusDays(2) // Move to Monday
            DayOfWeek.SUNDAY -> nextPaymentDueDate.plusDays(1)   // Move to Monday
            else -> nextPaymentDueDate
        }

        val businessNotice = if (isWeekend) {
            "Tu fecha límite hábil será el lunes ${businessPaymentDueDate.format(dateFormatter)}"
        } else null

        return CreditCardBillingCycle(
            referenceDate = referenceDate.format(dateFormatter),
            cutOffDay = safeCutOff,
            paymentDueDay = safePaymentDay,
            nextCutOffDateFormatted = nextCutOffDate.format(dateFormatter),
            nextPaymentDueDateFormatted = nextPaymentDueDate.format(dateFormatter),
            businessPaymentDueDateFormatted = businessPaymentDueDate.format(dateFormatter),
            isWeekendPayment = isWeekend,
            businessDayNotice = businessNotice,
            currentBilledBalance = billedAmount,
            isGracePeriodActiveForPurchases = true,
            singleQuotaInterestProjected = 0.0
        )
    }

    /**
     * French Amortization Engine for multiple installments based on EA rate.
     */
    fun calculateInstallmentProjection(
        amount: Double,
        installments: Int,
        annualRateEa: Double
    ): InstallmentProjection {
        if (amount <= 0 || installments <= 0) {
            return InstallmentProjection(
                numberOfInstallments = installments,
                annualRateEa = annualRateEa,
                monthlyRateEffective = 0.0,
                installmentMonthlyPayment = 0.0,
                totalInterestPayable = 0.0,
                totalAmountPayable = amount
            )
        }

        // Grace period for 1 installment
        if (installments == 1) {
            return InstallmentProjection(
                numberOfInstallments = 1,
                annualRateEa = annualRateEa,
                monthlyRateEffective = 0.0,
                installmentMonthlyPayment = amount,
                totalInterestPayable = 0.0,
                totalAmountPayable = amount
            )
        }

        // Convert Effective Annual (EA) to Monthly Effective:
        // i_monthly = (1 + EA/100)^(1/12) - 1
        val rateDecimal = annualRateEa / 100.0
        val monthlyRate = (1.0 + rateDecimal).pow(1.0 / 12.0) - 1.0

        // French Amortization Formula:
        // Cuota = Principal * [ i / (1 - (1+i)^-n) ]
        val factor = (1.0 + monthlyRate).pow(-installments)
        val monthlyPayment = if (1.0 - factor > 0.0) {
            amount * (monthlyRate / (1.0 - factor))
        } else {
            amount / installments
        }

        val totalAmount = monthlyPayment * installments
        val totalInterest = totalAmount - amount

        return InstallmentProjection(
            numberOfInstallments = installments,
            annualRateEa = annualRateEa,
            monthlyRateEffective = monthlyRate * 100.0,
            installmentMonthlyPayment = monthlyPayment,
            totalInterestPayable = totalInterest,
            totalAmountPayable = totalAmount
        )
    }
}
