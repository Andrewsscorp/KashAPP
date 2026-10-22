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

    @Test
    fun testCreditCardLiabilityNoDoubleCounting() {
        val filter = FinancialPeriodFilter()
        val accounts = listOf(
            com.example.data.local.entity.AccountEntity(
                id = "acc_savings",
                userId = "user_test",
                name = "Ahorros Principal",
                balance = 10_000_000.0,
                type = "SAVINGS"
            ),
            com.example.data.local.entity.AccountEntity(
                id = "acc_cc_1",
                userId = "user_test",
                name = "Tarjeta Visa",
                balance = -2_500_000.0,
                type = "CREDIT_CARD"
            )
        )
        val cards = listOf(
            com.example.data.local.entity.AccountCreditCardEntity(
                id = "cc_1",
                accountId = "acc_cc_1",
                quotaTotal = 10_000_000.0,
                quotaAvailable = 7_500_000.0,
                quotaUsed = 2_500_000.0
            )
        )

        val summary = engine.computeSummary(accounts, emptyList(), emptyList(), emptyList(), cards, filter)
        val balance = engine.computeBalanceSheet(summary, accounts, emptyList(), emptyList(), cards)

        assertEquals(10_000_000.0, summary.totalAssets, 0.01)
        assertEquals("No debe duplicar el pasivo de TC", 2_500_000.0, summary.totalLiabilities, 0.01)
        assertEquals(7_500_000.0, summary.netWorth, 0.01)
        assertEquals(2_500_000.0, balance.totalLiabilities, 0.01)
        assertEquals(7_500_000.0, balance.totalEquity, 0.01)
        assertTrue(balance.isBalanced)
    }

    @Test
    fun testOpeningBalanceExcludedFromIncomeStatementAndCountedAsCapital() {
        val todayStr = java.time.LocalDate.now().toString()
        val filter = FinancialPeriodFilter()
        val accounts = listOf(
            com.example.data.local.entity.AccountEntity(
                id = "acc_1",
                userId = "user_test",
                name = "Cuenta Ahorros",
                balance = 12_500_000.0,
                type = "SAVINGS"
            )
        )
        val transactions = listOf(
            com.example.data.local.entity.TransactionEntity(
                id = "tx_init_acc_1",
                userId = "user_test",
                accountId = "acc_1",
                accountName = "Cuenta Ahorros",
                categoryId = "cat_initial_balance",
                categoryName = "Saldo Inicial",
                amount = 10_000_000.0,
                currency = "COP",
                type = "INCOME",
                date = todayStr,
                description = "Apertura de cuenta",
                receiptUrl = null,
                idempotencyKey = "idem_init_1"
            ),
            com.example.data.local.entity.TransactionEntity(
                id = "tx_sal_1",
                userId = "user_test",
                accountId = "acc_1",
                accountName = "Cuenta Ahorros",
                categoryId = "cat_salary",
                categoryName = "Salario",
                amount = 4_000_000.0,
                currency = "COP",
                type = "INCOME",
                date = todayStr,
                description = "Pago nómina",
                receiptUrl = null,
                idempotencyKey = "idem_sal_1"
            ),
            com.example.data.local.entity.TransactionEntity(
                id = "tx_exp_1",
                userId = "user_test",
                accountId = "acc_1",
                accountName = "Cuenta Ahorros",
                categoryId = "cat_food",
                categoryName = "Mercado",
                amount = 1_500_000.0,
                currency = "COP",
                type = "EXPENSE",
                date = todayStr,
                description = "Supermercado",
                receiptUrl = null,
                idempotencyKey = "idem_exp_1"
            )
        )

        val summary = engine.computeSummary(accounts, transactions, emptyList(), emptyList(), emptyList(), filter)
        val balance = engine.computeBalanceSheet(summary, accounts, emptyList(), emptyList(), emptyList())
        val equity = engine.computeEquityStatement(summary, balance, transactions)

        assertEquals("Ingresos operativos no deben incluir saldo inicial", 4_000_000.0, summary.totalIncome, 0.01)
        assertEquals(1_500_000.0, summary.totalExpenses, 0.01)
        assertEquals(2_500_000.0, summary.periodResult, 0.01)
        assertEquals("El saldo inicial debe ir a aportes de capital", 10_000_000.0, equity.capitalContributions, 0.01)
    }

    @Test
    fun testPredictiveEngineUsesRealCategoriesWithoutMockData() {
        val predictiveEngine = com.example.feature.informes.domain.engine.PredictiveExpenseEngine()
        val accounts = listOf(
            com.example.data.local.entity.AccountEntity(
                id = "acc_1",
                userId = "user_test",
                name = "Cuenta Ahorros",
                balance = 6_000_000.0,
                type = "SAVINGS"
            )
        )
        val transactions = listOf(
            com.example.data.local.entity.TransactionEntity(
                id = "tx_e1",
                userId = "user_test",
                accountId = "acc_1",
                accountName = "Cuenta Ahorros",
                categoryId = "cat_1",
                categoryName = "Alimentación Real",
                amount = 1_000_000.0,
                currency = "COP",
                type = "EXPENSE",
                date = java.time.LocalDate.now().toString(),
                description = "Compra",
                receiptUrl = null,
                idempotencyKey = "idem_e1"
            )
        )

        val proj = predictiveEngine.computeAdvancedProjections(
            accounts = accounts,
            transactions = transactions,
            currentNetWorth = 6_000_000.0,
            currentIncome = 3_000_000.0,
            currentExpenses = 1_000_000.0
        )

        assertEquals(1, proj.projectedCategories.size)
        assertEquals("Alimentación Real", proj.projectedCategories.first().categoryName)
        val baseScenario = proj.multiHorizonScenarios.first()
        assertEquals(6_000_000.0 + (2_000_000.0 * 12.0), baseScenario.projectedFinalNetWorth, 0.01)
    }
}
