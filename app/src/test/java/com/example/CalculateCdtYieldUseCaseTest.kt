package com.example

import com.example.domain.usecase.CalculateCdtYieldUseCase
import com.example.domain.usecase.CreateCdtUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.math.roundToInt

class CalculateCdtYieldUseCaseTest {

    private val calculateYieldUseCase = CalculateCdtYieldUseCase()

    @Test
    fun calculateYield_10Million_10Point5EA_180Days_matchesFinancialFormula() {
        val amount = 10_000_000.0
        val rateEa = 10.50
        val termDays = 180
        val issueDate = LocalDate.of(2026, 9, 25)

        val result = calculateYieldUseCase(
            initialAmount = amount,
            interestRateEa = rateEa,
            termDays = termDays,
            issueDate = issueDate
        )

        // Tasa_180 = (1 + 0.1050)^(180/365) - 1 ≈ 0.050471168 (5.0471% ≈ 5.05%)
        // Rendimiento = 10_000_000 * 0.050471168 ≈ $504.711 COP (aprox. $505.000 COP)
        assertEquals(5.0471, result.termRatePercentage, 0.01)
        assertEquals(504711.68, result.estimatedYield, 50.0)
        assertEquals(10_504_711.68, result.totalEstimatedValue, 50.0)

        // Maturity Date: 25/09/2026 + 180 days = 24/03/2027
        assertEquals(LocalDate.of(2027, 3, 24), result.maturityDate)
    }

    @Test
    fun calculateYield_fullYear_yieldMatchesEaRateExactly() {
        val amount = 20_000_000.0
        val rateEa = 12.0
        val termDays = 365
        val issueDate = LocalDate.of(2026, 1, 1)

        val result = calculateYieldUseCase(
            initialAmount = amount,
            interestRateEa = rateEa,
            termDays = termDays,
            issueDate = issueDate
        )

        // For exactly 365 days, term rate should equal EA rate (12%)
        assertEquals(12.0, result.termRatePercentage, 0.001)
        assertEquals(2_400_000.0, result.estimatedYield, 0.01)
        assertEquals(LocalDate.of(2027, 1, 1), result.maturityDate)
    }

    @Test
    fun createCdtUseCase_locksLiquidityAndPersistsAccount() = runBlocking {
        val fakeRepo = FakeFinancialRepository()
        val createCdtUseCase = CreateCdtUseCase(fakeRepo, calculateYieldUseCase)

        val result = createCdtUseCase(
            userId = "user_123",
            name = "CDT Bancolombia 180 días",
            bankName = "Bancolombia",
            certificateNumber = "•••• 4827",
            initialAmount = 10_000_000.0,
            interestRateEa = 10.50,
            termDays = 180,
            issueDate = LocalDate.of(2026, 9, 25),
            actionAtMaturity = "ASK"
        )

        assertTrue(result.isSuccess)
        val (account, cdtConfig) = result.getOrThrow()

        // Account assertions: Liquidity must be 0 (illiquid asset), full balance in holdBalance
        assertEquals("CDT", account.type)
        assertEquals(10_000_000.0, account.balance, 0.0)
        assertEquals(0.0, account.availableBalance, 0.0)
        assertEquals(10_000_000.0, account.holdBalance, 0.0)
        assertTrue(account.generatesInterest)
        assertEquals(10.50, account.interestRateEa ?: 0.0, 0.0)

        // CDT metadata assertions
        assertEquals(180, cdtConfig.termDays)
        assertEquals("2026-09-25", cdtConfig.issueDate)
        assertEquals("2027-03-24", cdtConfig.maturityDate)
        assertEquals(504711.68, cdtConfig.estimatedYield, 50.0)
        assertEquals("ASK", cdtConfig.actionAtMaturity)
    }
}
