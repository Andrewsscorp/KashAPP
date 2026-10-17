package com.example.feature.gastoscompartidos

import com.example.core.result.Outcome
import com.example.data.local.entity.AccountEntity
import com.example.domain.models.shared.BudgetImpactPolicy
import com.example.domain.models.shared.RealMoneyImpactMode
import com.example.domain.models.shared.RoundingRemainderTarget
import com.example.domain.models.shared.RoundingStepRule
import com.example.domain.models.shared.SplitCalculationMethod
import com.example.domain.security.FirebaseApiProtectionGuard
import com.example.feature.gastoscompartidos.domain.engine.DebtSimplificationGraphEngine
import com.example.feature.gastoscompartidos.domain.engine.RealMoneyImpactEngine
import com.example.feature.gastoscompartidos.domain.engine.SharedSplitCalculatorEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedExpenseEnginesStage2Test {

    @Test
    fun `SharedSplitCalculatorEngine distributes 100000 COP among 3 people with zero cent loss`() {
        val request = SharedSplitCalculatorEngine.SplitCalculationRequest(
            subtotalAmount = 100000.0,
            splitMethod = SplitCalculationMethod.EQUAL,
            roundingStepRule = RoundingStepRule.ROUND_1,
            roundingRemainderTarget = RoundingRemainderTarget.PAYER,
            participants = listOf(
                SharedSplitCalculatorEngine.ParticipantSplitInput(
                    participantId = "SELF_USER",
                    displayName = "Andrés",
                    isSelf = true,
                    paidAmountAtRegister = 100000.0
                ),
                SharedSplitCalculatorEngine.ParticipantSplitInput(
                    participantId = "part_ana",
                    displayName = "Ana"
                ),
                SharedSplitCalculatorEngine.ParticipantSplitInput(
                    participantId = "part_carlos",
                    displayName = "Carlos"
                )
            )
        )

        val outcome = SharedSplitCalculatorEngine.calculateSplits(request)
        assertTrue(outcome is Outcome.Success)
        val summary = (outcome as Outcome.Success).value

        // Suma exacta = 100.000 COP (33.334 + 33.333 + 33.333)
        val sumOfShares = summary.participantSplits.sumOf { it.totalOwedShare }
        assertEquals(100000.0, sumOfShares, 0.001)
        val selfShare = summary.participantSplits.first { it.participantId == "SELF_USER" }
        assertEquals(33334.0, selfShare.totalOwedShare, 0.001)
        assertEquals(66666.0, selfShare.netBalanceForExpense, 0.001) // Le deben $66.666
    }

    @Test
    fun `SharedSplitCalculatorEngine ITEMIZED_OCR prorates 19pct IVA and 10pct Tip proportionally`() {
        val request = SharedSplitCalculatorEngine.SplitCalculationRequest(
            subtotalAmount = 100000.0,
            taxIvaAmount = 19000.0,
            tipAmount = 10000.0,
            splitMethod = SplitCalculationMethod.ITEMIZED_OCR,
            roundingStepRule = RoundingStepRule.ROUND_1,
            participants = listOf(
                SharedSplitCalculatorEngine.ParticipantSplitInput("SELF_USER", "Yo", isSelf = true, paidAmountAtRegister = 129000.0),
                SharedSplitCalculatorEngine.ParticipantSplitInput("part_2", "Laura")
            ),
            ocrLineItems = listOf(
                SharedSplitCalculatorEngine.OcrLineItemInput(
                    itemId = "1",
                    description = "Plato Fuerte",
                    unitPrice = 60000.0,
                    lineTotalPrice = 60000.0,
                    assignedParticipantIds = listOf("SELF_USER")
                ),
                SharedSplitCalculatorEngine.OcrLineItemInput(
                    itemId = "2",
                    description = "Pasta Carbonara",
                    unitPrice = 40000.0,
                    lineTotalPrice = 40000.0,
                    assignedParticipantIds = listOf("part_2")
                )
            )
        )

        val outcome = SharedSplitCalculatorEngine.calculateSplits(request)
        assertTrue(outcome is Outcome.Success)
        val summary = (outcome as Outcome.Success).value

        // Yo consumí 60% ($60.000 base + $11.400 IVA + $6.000 Propina = $77.400)
        // Laura consumió 40% ($40.000 base + $7.600 IVA + $4.000 Propina = $51.600)
        val mySplit = summary.participantSplits.first { it.participantId == "SELF_USER" }
        val lauraSplit = summary.participantSplits.first { it.participantId == "part_2" }

        assertEquals(77400.0, mySplit.totalOwedShare, 0.01)
        assertEquals(51600.0, lauraSplit.totalOwedShare, 0.01)
        assertEquals(129000.0, mySplit.totalOwedShare + lauraSplit.totalOwedShare, 0.01)
    }

    @Test
    fun `RealMoneyImpactEngine ACCRUAL_RECEIVABLE separates bank outflow from Pnl expense and creates CxC`() {
        val bankAcc = AccountEntity(
            id = "acc_bancolombia",
            userId = "user_default",
            name = "Ahorros Bancolombia",
            balance = 1500000.0,
            availableBalance = 1500000.0,
            isTaxExempt = true
        )

        val outcome = RealMoneyImpactEngine.simulateExpenseImpact(
            mode = RealMoneyImpactMode.ACCRUAL_RECEIVABLE,
            budgetPolicy = BudgetImpactPolicy.MY_NET_SHARE_ONLY,
            myPaidPrincipalAmount = 200000.0,
            myGmf4x1000Amount = 0.0,
            myConsumptionShareAmount = 50000.0,
            sourcePaymentAccount = bankAcc,
            holdReserveAccount = bankAcc,
            autoReserveOwedInHold = true
        )

        assertTrue(outcome is Outcome.Success)
        val sim = (outcome as Outcome.Success).value

        assertEquals(200000.0, sim.immediateAccountBalanceDebit, 0.01)
        assertEquals(1300000.0, sim.projectedAccountBalanceAfter, 0.01)
        assertEquals(50000.0, sim.recognizedExpenseInPnlToday, 0.01)
        assertEquals(50000.0, sim.budgetSpentDeltaToday, 0.01)
        assertEquals(150000.0, sim.accountsReceivableAssetDelta, 0.01)
        assertEquals(-50000.0, sim.netWorthImmediateDelta, 0.01)
    }

    @Test
    fun `RealMoneyImpactEngine reserves holdBalance when someone else pays and user owes share`() {
        val bankAcc = AccountEntity(
            id = "acc_bancolombia",
            userId = "user_default",
            name = "Ahorros Bancolombia",
            balance = 800000.0,
            availableBalance = 800000.0,
            holdBalance = 0.0,
            isTaxExempt = true
        )

        val outcome = RealMoneyImpactEngine.simulateExpenseImpact(
            mode = RealMoneyImpactMode.ACCRUAL_RECEIVABLE,
            budgetPolicy = BudgetImpactPolicy.MY_NET_SHARE_ONLY,
            myPaidPrincipalAmount = 0.0,
            myGmf4x1000Amount = 0.0,
            myConsumptionShareAmount = 65000.0,
            sourcePaymentAccount = bankAcc,
            holdReserveAccount = bankAcc,
            autoReserveOwedInHold = true
        )

        assertTrue(outcome is Outcome.Success)
        val sim = (outcome as Outcome.Success).value

        assertEquals(0.0, sim.immediateAccountBalanceDebit, 0.01)
        assertEquals(800000.0, sim.projectedAccountBalanceAfter, 0.01)
        assertEquals(65000.0, sim.mentalHoldReservedDelta, 0.01)
        assertEquals(735000.0, sim.projectedFreeAvailableBalanceAfter, 0.01)
        assertEquals(65000.0, sim.accountsPayableLiabilityDelta, 0.01)
    }

    @Test
    fun `DebtSimplificationGraphEngine reduces transitive 3-node chain to 1 single transfer`() {
        // Ana le debe $50.000 a Bruno, y Bruno le debe $50.000 a Carlos -> Ana le paga $50.000 a Carlos
        val report = DebtSimplificationGraphEngine.simplifyGroupDebts(
            listOf(
                DebtSimplificationGraphEngine.RawDirectedDebtEdge("p_ana", "Ana", "p_bruno", "Bruno", 50000.0, "GC-2026-0001"),
                DebtSimplificationGraphEngine.RawDirectedDebtEdge("p_bruno", "Bruno", "p_carlos", "Carlos", 50000.0, "GC-2026-0002")
            )
        )

        assertEquals(2, report.originalTransferCount)
        assertEquals(1, report.simplifiedTransferCount)
        assertEquals(1, report.transactionsSavedCount)
        val instruction = report.optimizedInstructions.single()
        assertEquals("p_ana", instruction.fromParticipantId)
        assertEquals("p_carlos", instruction.toParticipantId)
        assertEquals(50000.0, instruction.amount, 0.01)
    }

    @Test
    fun `FirebaseApiProtectionGuard masks PII before cloud call and rehydrates participant names`() {
        val raw = "Cena con Laura Gomez y Carlos Perez pagada desde cuenta 453201928374 y correo laura@gmail.com por $180.000"
        val (sanitized, aliasMap) = FirebaseApiProtectionGuard.sanitizePiiWithRehydrationMap(
            rawText = raw,
            participantRealNames = listOf("Laura Gomez", "Carlos Perez")
        )

        assertFalse(sanitized.contains("Laura Gomez"))
        assertFalse(sanitized.contains("Carlos Perez"))
        assertFalse(sanitized.contains("453201928374"))
        assertFalse(sanitized.contains("laura@gmail.com"))
        assertTrue(sanitized.contains("PARTICIPANT_1"))
        assertTrue(sanitized.contains("[REDACTED_ACCOUNT]"))
        assertTrue(sanitized.contains("[REDACTED_EMAIL]"))

        val fakeCloudReply = "Asignar $90.000 a PARTICIPANT_1 y $90.000 a PARTICIPANT_2"
        val restored = FirebaseApiProtectionGuard.rehydratePiiAliases(fakeCloudReply, aliasMap)
        assertTrue(restored.contains("Laura Gomez"))
        assertTrue(restored.contains("Carlos Perez"))
    }
}
