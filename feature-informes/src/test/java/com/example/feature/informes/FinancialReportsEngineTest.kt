package com.example.feature.informes

import com.example.feature.informes.domain.engine.FinancialCalculationEngine
import com.example.feature.informes.domain.model.ExportReportConfig
import com.example.feature.informes.domain.model.ExportReportFormat
import com.example.feature.informes.domain.model.FinancialPeriodFilter
import org.junit.Assert.*
import org.junit.Test

class FinancialReportsEngineTest {

    private val engine = FinancialCalculationEngine()

    @Test
    fun testAccountingBalanceValidation() {
        val filter = FinancialPeriodFilter()
        val summary = engine.computeSummary(
            accounts = emptyList(),
            transactions = emptyList(),
            assets = emptyList(),
            investments = emptyList(),
            creditCards = emptyList(),
            filter = filter
        )

        val balanceSheet = engine.computeBalanceSheet(
            summary = summary,
            accounts = emptyList(),
            assets = emptyList(),
            investments = emptyList(),
            creditCards = emptyList()
        )

        // Assets = Liabilities + Equity
        assertTrue("El balance general debe estar cuadrado", balanceSheet.isBalanced)
        assertEquals(0.0, balanceSheet.balanceDifference, 1.0)
        assertEquals(balanceSheet.totalAssets, balanceSheet.liabilitiesPlusEquity, 1.0)
    }

    @Test
    fun testNetWorthFormula() {
        val filter = FinancialPeriodFilter()
        val summary = engine.computeSummary(
            accounts = emptyList(),
            transactions = emptyList(),
            assets = emptyList(),
            investments = emptyList(),
            creditCards = emptyList(),
            filter = filter
        )

        assertEquals(summary.totalAssets - summary.totalLiabilities, summary.netWorth, 0.01)
    }

    @Test
    fun testCashFlowReconciliation() {
        val filter = FinancialPeriodFilter()
        val summary = engine.computeSummary(
            accounts = emptyList(),
            transactions = emptyList(),
            assets = emptyList(),
            investments = emptyList(),
            creditCards = emptyList(),
            filter = filter
        )

        val cashFlow = engine.computeCashFlow(summary)
        val expectedFinal = cashFlow.initialBalance + cashFlow.totalInflows - cashFlow.totalOutflows
        assertEquals(expectedFinal, cashFlow.finalBalance, 0.01)
        assertTrue(cashFlow.internalTransfersExcludedCount >= 0)
    }

    @Test
    fun testFinancialRatiosCalculated() {
        val filter = FinancialPeriodFilter()
        val summary = engine.computeSummary(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), filter)
        val balance = engine.computeBalanceSheet(summary, emptyList(), emptyList(), emptyList(), emptyList())
        val analysis = engine.computeFinancialAnalysis(summary, balance)

        assertTrue("Debe contener al menos 6 ratios", analysis.ratios.size >= 6)
        assertTrue(analysis.liquidityRatioValue > 0.0)
        assertTrue(analysis.debtLevelPct >= 0.0)
        assertTrue(analysis.projections.isNotEmpty())
    }

    @Test
    fun testZeroBalanceAccountsInBalanceSheet() {
        val filter = FinancialPeriodFilter()
        val zeroAccounts = listOf(
            com.example.data.local.entity.AccountEntity(
                id = "acc_zero_1",
                userId = "user_test",
                name = "Cuenta Nómina $0",
                balance = 0.0,
                type = "PAYROLL"
            ),
            com.example.data.local.entity.AccountEntity(
                id = "acc_zero_2",
                userId = "user_test",
                name = "Caja Efectivo $0",
                balance = 0.0,
                type = "CASH"
            )
        )
        val summary = engine.computeSummary(zeroAccounts, emptyList(), emptyList(), emptyList(), emptyList(), filter)
        val balance = engine.computeBalanceSheet(summary, zeroAccounts, emptyList(), emptyList(), emptyList())

        assertEquals(0.0, balance.totalCurrentAssets, 0.001)
        assertFalse(balance.currentAssets.any { it.id == "ca0" })
        assertTrue(balance.currentAssets.any { it.id == "ca2" })
        assertTrue(balance.currentAssets.any { it.id == "ca1" })
    }
}
