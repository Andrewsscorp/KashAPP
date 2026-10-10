package com.example

import com.example.domain.usecase.CalculateNextBillingDatesUseCase
import com.example.domain.usecase.ValidateCreditLimitsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Banking Core Unit Test Suite for KashApp Credit Card (Pasivo Rotativo) module.
 * Verifies:
 * 1. Master Equation Rule: Cupo Total = Cupo Disponible + Cupo Utilizado + Cupo Retenido
 * 2. Auto-balancing: Total $12.000.000, Used $2.850.000 -> Available $9.150.000 (28.5% usage on $10M)
 * 3. Next Billing Cycle Engine & Weekend Rollover Alert (10/10/2026 Saturday -> 12/10/2026 Monday)
 * 4. Grace Period ($0 interest projected on 1 installment) vs French Amortization on 12 installments.
 */
class CreditCardBankingCoreTest {

    private lateinit var validateCreditLimitsUseCase: ValidateCreditLimitsUseCase
    private lateinit var calculateNextBillingDatesUseCase: CalculateNextBillingDatesUseCase

    @Before
    fun setUp() {
        validateCreditLimitsUseCase = ValidateCreditLimitsUseCase()
        calculateNextBillingDatesUseCase = CalculateNextBillingDatesUseCase()
    }

    @Test
    fun masterEquation_validBalances_evaluatesTrue() {
        val result = validateCreditLimitsUseCase(
            totalQuota = 12_000_000.0,
            availableQuota = 3_550_000.0,
            usedQuota = 8_450_000.0,
            retainedQuota = 0.0
        )

        assertTrue(result.isValid)
        assertEquals(12_000_000.0, result.totalQuota, 0.01)
        assertEquals(8_450_000.0, result.usedQuota, 0.01)
        assertEquals(3_550_000.0, result.availableQuota, 0.01)
    }

    @Test
    fun masterEquation_userChangesTotalAndUsed_autoBalancesAvailable() {
        // User changes Total to $12.000.000, used is $2.850.000
        val total = 12_000_000.0
        val used = 2_850_000.0
        val autoAvailable = validateCreditLimitsUseCase.calculateAvailableFromTotalAndUsed(total, used)

        assertEquals(9_150_000.0, autoAvailable, 0.01)

        val validation = validateCreditLimitsUseCase(
            totalQuota = total,
            availableQuota = autoAvailable,
            usedQuota = used
        )
        assertTrue(validation.isValid)
    }

    @Test
    fun masterEquation_reactiveUsagePercentage_calculatesAccurately() {
        // $2.850.000 of $10.000.000 is 28.5%
        val result = validateCreditLimitsUseCase(
            totalQuota = 10_000_000.0,
            availableQuota = 7_150_000.0,
            usedQuota = 2_850_000.0
        )

        assertTrue(result.isValid)
        assertEquals(28.5f, result.usagePercentage, 0.05f)
    }

    @Test
    fun billingCycleEngine_referenceSept27_detectsCutOffPassedAndCalculatesNextDates() {
        // Reference date: 27 de Septiembre 2026.
        // Cut-off = 25, Due day = 10.
        // Cut-off of Sept (25) already passed -> Next cut-off = 25/10/2026.
        // Next payment due = 10/10/2026.
        val refDate = LocalDate.of(2026, 9, 27)
        val cycle = calculateNextBillingDatesUseCase(
            cutOffDay = 25,
            paymentDueDay = 10,
            referenceDate = refDate,
            billedAmount = 8_450_000.0,
            purchasesRateEa = 22.0
        )

        assertEquals("25/10/2026", cycle.nextCutOffDateFormatted)
        assertEquals("10/10/2026", cycle.nextPaymentDueDateFormatted)

        // 10/10/2026 is Saturday -> System must alert rollover to Monday 12/10/2026!
        assertTrue(cycle.isWeekendPayment)
        assertNotNull(cycle.businessDayNotice)
        assertTrue(cycle.businessDayNotice!!.contains("lunes 12/10/2026"))
        assertEquals("12/10/2026", cycle.businessPaymentDueDateFormatted)
    }

    @Test
    fun interestLogic_singleInstallmentPurchases_hasGracePeriodZeroInterest() {
        val projection = calculateNextBillingDatesUseCase.calculateInstallmentProjection(
            amount = 500_000.0,
            installments = 1,
            annualRateEa = 22.0
        )

        assertEquals(1, projection.numberOfInstallments)
        assertEquals(500_000.0, projection.installmentMonthlyPayment, 0.01)
        assertEquals(0.0, projection.totalInterestPayable, 0.01)
        assertEquals(500_000.0, projection.totalAmountPayable, 0.01)
    }

    @Test
    fun interestLogic_multipleInstallments_frenchAmortizationCalculatesInterest() {
        val amount = 1_200_000.0
        val installments = 12
        val projection = calculateNextBillingDatesUseCase.calculateInstallmentProjection(
            amount = amount,
            installments = installments,
            annualRateEa = 22.0
        )

        assertEquals(12, projection.numberOfInstallments)
        assertTrue("Monthly payment should exceed simple principal division", projection.installmentMonthlyPayment > (amount / installments))
        assertTrue("Total interest payable should be greater than zero", projection.totalInterestPayable > 0.0)
        assertEquals(projection.installmentMonthlyPayment * installments, projection.totalAmountPayable, 0.05)
    }
}
