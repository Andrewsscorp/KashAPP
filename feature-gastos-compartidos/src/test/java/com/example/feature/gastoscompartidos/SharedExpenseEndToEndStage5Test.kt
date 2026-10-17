package com.example.feature.gastoscompartidos

import com.example.core.result.Outcome
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.engine.ConsecutiveSequenceEngine
import com.example.domain.models.shared.BudgetImpactPolicy
import com.example.domain.models.shared.RealMoneyImpactMode
import com.example.domain.models.shared.ReimbursementAccountingTreatment
import com.example.domain.models.shared.RoundingStepRule
import com.example.domain.models.shared.SequenceDocumentType
import com.example.domain.models.shared.SequenceResetPolicy
import com.example.domain.models.shared.SplitCalculationMethod
import com.example.feature.gastoscompartidos.domain.engine.RealMoneyImpactEngine
import com.example.feature.gastoscompartidos.domain.engine.SharedSplitCalculatorEngine
import com.example.feature.informes.domain.engine.SharedAccountingStatementIntegrator
import com.example.feature.informes.domain.model.BalanceSheetData
import com.example.feature.informes.domain.model.FinancialLineItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SharedExpenseEndToEndStage5Test {

    @Test
    fun `End-to-End NIIF Flow from Shared Expense Creation to Balance Sheet and Settlement`() {
        // 1. ETAPA 1: Generar Consecutivo GC-2026-0001 y Ticket TK-2026-0001
        val refDate = LocalDate.of(2026, 10, 15)
        val gcSeq = ConsecutiveSequenceEngine.generateNextAutomatic(
            snapshot = ConsecutiveSequenceEngine.SequenceSnapshot(
                sequenceType = SequenceDocumentType.SHARED_EXPENSE,
                prefix = "GC",
                patternTemplate = "{PREFIX}-{YYYY}-{SEQ:4}",
                currentValue = 0L,
                resetPolicy = SequenceResetPolicy.YEARLY,
                lastResetPeriodKey = "2026"
            ),
            referenceDate = refDate
        )
        assertEquals("GC-2026-0001", gcSeq.formattedCode)

        // 2. ETAPA 2: Dividir un gasto de $300.000 COP entre 3 personas (Yo pagué los $300.000 desde Bancolombia)
        val splitOutcome = SharedSplitCalculatorEngine.calculateSplits(
            SharedSplitCalculatorEngine.SplitCalculationRequest(
                subtotalAmount = 300_000.0,
                splitMethod = SplitCalculationMethod.EQUAL,
                roundingStepRule = RoundingStepRule.ROUND_1,
                participants = listOf(
                    SharedSplitCalculatorEngine.ParticipantSplitInput("SELF_USER", "Andrés", isSelf = true, paidAmountAtRegister = 300_000.0),
                    SharedSplitCalculatorEngine.ParticipantSplitInput("part_ana", "Ana"),
                    SharedSplitCalculatorEngine.ParticipantSplitInput("part_luis", "Luis")
                )
            )
        )
        assertTrue(splitOutcome is Outcome.Success)
        val splitSummary = (splitOutcome as Outcome.Success).value
        val myShare = splitSummary.participantSplits.first { it.isSelf }.totalOwedShare
        assertEquals(100_000.0, myShare, 0.01)

        // 3. ETAPA 2: Simular impacto en Mi Dinero Real bajo Devengo NIIF (ACCRUAL_RECEIVABLE)
        val initialBankAccount = AccountEntity(
            id = "acc_1",
            userId = "user_default",
            name = "Ahorros Bancolombia",
            balance = 2_000_000.0,
            availableBalance = 2_000_000.0,
            isTaxExempt = true
        )

        val impactOutcome = RealMoneyImpactEngine.simulateExpenseImpact(
            mode = RealMoneyImpactMode.ACCRUAL_RECEIVABLE,
            budgetPolicy = BudgetImpactPolicy.MY_NET_SHARE_ONLY,
            myPaidPrincipalAmount = 300_000.0,
            myGmf4x1000Amount = 0.0,
            myConsumptionShareAmount = myShare,
            sourcePaymentAccount = initialBankAccount,
            holdReserveAccount = initialBankAccount,
            autoReserveOwedInHold = true
        )
        assertTrue(impactOutcome is Outcome.Success)
        val impact = (impactOutcome as Outcome.Success).value

        // El banco baja en $300.000 (queda en $1.700.000), mi gasto PyG es solo $100.000 y nace una CxC de $200.000
        assertEquals(1_700_000.0, impact.projectedAccountBalanceAfter, 0.01)
        assertEquals(100_000.0, impact.recognizedExpenseInPnlToday, 0.01)
        assertEquals(200_000.0, impact.accountsReceivableAssetDelta, 0.01)

        // 4. ETAPA 5: Verificar Estado de Resultados (PyG) vs Flujo de Efectivo (Cash Flow)
        val ledgerTx = TransactionEntity(
            id = "tx_shared_1",
            userId = "user_default",
            accountId = "acc_1",
            accountName = "Ahorros Bancolombia",
            categoryId = "cat_food",
            categoryName = "Alimentación",
            amount = 300_000.0,
            currency = "COP",
            type = "EXPENSE",
            date = "2026-10-15",
            description = "[GC-2026-0001] Cena Grupo",
            receiptUrl = null,
            idempotencyKey = "idem_tx_shared_1",
            sharedExpenseId = "sexp_1",
            sharedConsecutiveCode = "GC-2026-0001",
            accountingRecognizedExpenseAmount = impact.recognizedExpenseInPnlToday,
            receivableOrPayableDeltaAmount = impact.accountsReceivableAssetDelta
        )

        val recognizedPnlExpense = SharedAccountingStatementIntegrator.computeRecognizedPnlExpenses(listOf(ledgerTx))
        val realCashOutflow = SharedAccountingStatementIntegrator.computeRealCashOutflows(listOf(ledgerTx))
        assertEquals(100_000.0, recognizedPnlExpense, 0.01)
        assertEquals(300_000.0, realCashOutflow, 0.01)

        // 5. ETAPA 5: Verificar Balance General NIIF antes del reembolso
        // Banco = $1.700.000 + Cuenta por Cobrar = $200.000 => Total Activos = $1.900.000 (Patrimonio bajó solo $100.000)
        val baseBalanceSheet = BalanceSheetData(
            currentAssets = listOf(
                FinancialLineItem("ca2", "Bancos", "Ahorros Bancolombia", "Bancos", 1_700_000.0, 100.0)
            ),
            nonCurrentAssets = emptyList(),
            totalCurrentAssets = 1_700_000.0,
            totalNonCurrentAssets = 0.0,
            totalAssets = 1_700_000.0,
            currentLiabilities = emptyList(),
            nonCurrentLiabilities = emptyList(),
            totalCurrentLiabilities = 0.0,
            totalNonCurrentLiabilities = 0.0,
            totalLiabilities = 0.0,
            initialEquity = 1_700_000.0,
            contributions = 0.0,
            retainedEarnings = 0.0,
            periodResult = -100_000.0,
            adjustmentsAndRevaluations = 0.0,
            totalEquity = 1_700_000.0,
            liabilitiesPlusEquity = 1_700_000.0,
            isBalanced = true,
            balanceDifference = 0.0
        )

        val enrichedBs = SharedAccountingStatementIntegrator.enrichBalanceSheetWithSharedPortfolio(
            baseBalanceSheet = baseBalanceSheet,
            sharedSnapshot = SharedAccountingStatementIntegrator.SharedAccountingSnapshot(
                activeAccountsReceivableNiif = 200_000.0,
                openReceivableTicketsCount = 2,
                traceConsecutiveCodes = listOf("GC-2026-0001")
            )
        )

        assertEquals(1_900_000.0, enrichedBs.totalAssets, 0.01)
        assertEquals(1_900_000.0, enrichedBs.totalEquity, 0.01)
        assertTrue(enrichedBs.isBalanced)

        // 6. ETAPA 2 & 5: Ana y Luis pagan sus $200.000 (Liquidación PG-202610-0001)
        val settlementImpact = RealMoneyImpactEngine.computeSettlementImpact(
            isUserReceivingMoney = true,
            settlementAmount = 200_000.0,
            originalExpenseMode = RealMoneyImpactMode.ACCRUAL_RECEIVABLE,
            originalBudgetPolicy = BudgetImpactPolicy.MY_NET_SHARE_ONLY,
            reimbursementTreatment = ReimbursementAccountingTreatment.OFFSET_RECEIVABLE_ASSET,
            currentHoldReservedOnExpense = 0.0
        )

        // El banco sube en +$200.000 (vuelve a $1.900.000 en efectivo), la CxC se reduce en $200.000 (queda en $0)
        // y NO se infla el ingreso del Estado de Resultados (pnlIncomeDelta == 0.0)
        assertEquals(200_000.0, settlementImpact.bankBalanceDeltaForUser, 0.01)
        assertEquals(200_000.0, settlementImpact.receivableAssetReduction, 0.01)
        assertEquals(0.0, settlementImpact.pnlIncomeDelta, 0.01)
        assertEquals("TRANSFER", settlementImpact.transactionTypeForLedger)
    }
}
