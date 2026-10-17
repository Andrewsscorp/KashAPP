package com.example.feature.gastoscompartidos.domain.engine

import com.example.core.error.DomainError
import com.example.core.result.Outcome
import com.example.data.local.entity.AccountEntity
import com.example.domain.models.shared.BudgetImpactPolicy
import com.example.domain.models.shared.RealMoneyImpactMode
import com.example.domain.models.shared.ReimbursementAccountingTreatment
import kotlin.math.abs
import kotlin.math.roundToLong

object RealMoneyImpactEngine {

    /**
     * Resultado de simulación en tiempo real que alimenta la tarjeta RealMoneyImpactSimulatorCard en la UI
     * y los casos de uso de persistencia.
     */
    data class RealMoneyImpactSimulation(
        val mode: RealMoneyImpactMode,
        val budgetPolicy: BudgetImpactPolicy,
        // 1. Impacto inmediato en Cuentas Bancarias / Tarjetas
        val immediateAccountBalanceDebit: Double,       // Cuánto baja hoy AccountEntity.balance (o sube quotaUsed en TC)
        val immediateGmf4x1000Debit: Double,            // Impuesto 4x1000 debitado hoy
        val projectedAccountBalanceAfter: Double,       // Nuevo balance real en el banco
        // 2. Impacto en Mental Accounting (Fondos Comprometidos holdBalance)
        val mentalHoldReserveAccountId: String?,        // Cuenta donde se bloquea dinero si YO debo a un tercero
        val mentalHoldReservedDelta: Double,            // +Monto bloqueado en AccountEntity.holdBalance
        val projectedFreeAvailableBalanceAfter: Double, // Nuevo availableBalance libre para gastar
        // 3. Impacto en Estado de Resultados (PyG) y Presupuesto
        val recognizedExpenseInPnlToday: Double,        // Gasto contable reconocido hoy en PyG
        val budgetSpentDeltaToday: Double,              // Cuánto consume hoy del tope en BudgetItemEntity.spentAmount
        // 4. Impacto en Balance General NIIF (Cuentas por Cobrar / Cuentas por Pagar)
        val accountsReceivableAssetDelta: Double,       // +Activo Corriente (Cuentas por Cobrar a Terceros)
        val accountsPayableLiabilityDelta: Double,      // +Pasivo Corriente (Cuentas por Pagar a Terceros)
        val netWorthImmediateDelta: Double,             // Impacto neto real sobre el Patrimonio Neto hoy
        // 5. Explicación clara para el usuario
        val headlineSummary: String,
        val detailedAccountingExplanation: String
    )

    /**
     * Simula y calcula exactamente cómo afectará un gasto compartido al dinero real del usuario.
     */
    fun simulateExpenseImpact(
        mode: RealMoneyImpactMode,
        budgetPolicy: BudgetImpactPolicy,
        myPaidPrincipalAmount: Double,
        myGmf4x1000Amount: Double,
        myConsumptionShareAmount: Double,
        sourcePaymentAccount: AccountEntity?,
        holdReserveAccount: AccountEntity?,
        autoReserveOwedInHold: Boolean
    ): Outcome<RealMoneyImpactSimulation, DomainError> {
        val myNetDelta = roundCents(myPaidPrincipalAmount - myConsumptionShareAmount)
        // Si myNetDelta > 0 => Yo pagué de más y los demás ME DEBEN a mí.
        // Si myNetDelta < 0 => Otro pagó por mí y YO DEBO mi parte.

        // Determinar cuánto sale físicamente hoy de la cuenta bancaria/TC del usuario
        val bankDebitPrincipal = when (mode) {
            RealMoneyImpactMode.ACCRUAL_RECEIVABLE -> myPaidPrincipalAmount
            RealMoneyImpactMode.GROSS_CASHFLOW -> myPaidPrincipalAmount
            RealMoneyImpactMode.NET_SHARE_ONLY -> myConsumptionShareAmount
            RealMoneyImpactMode.DEFERRED_ON_SETTLEMENT -> 0.0
        }
        val totalBankDebitToday = roundCents(bankDebitPrincipal + if (bankDebitPrincipal > 0) myGmf4x1000Amount else 0.0)

        // Validar fondos disponibles o sobregiro si hay débito bancario inmediato
        if (totalBankDebitToday > 0.0 && sourcePaymentAccount != null) {
            val isCreditCard = sourcePaymentAccount.type.contains("CREDIT", ignoreCase = true)
            if (!isCreditCard && !sourcePaymentAccount.hasOverdraft) {
                val maxSpendable = sourcePaymentAccount.availableBalance - sourcePaymentAccount.minimumBalance
                if (totalBankDebitToday > maxSpendable + 0.01) {
                    return Outcome.Failure(
                        DomainError.Validation.InsufficientFunds(
                            available = maxSpendable.coerceAtLeast(0.0),
                            required = totalBankDebitToday
                        )
                    )
                }
            }
        }

        val currentAccBalance = sourcePaymentAccount?.balance ?: 0.0
        val projectedBalance = roundCents(currentAccBalance - totalBankDebitToday)

        // Determinar reserva en holdBalance (Mental Accounting) cuando YO quedo debiendo dinero (myNetDelta < 0)
        val owedByMe = if (myNetDelta < 0.0) abs(myNetDelta) else 0.0
        val shouldHoldInMentalAccounting = owedByMe > 0.0 &&
            autoReserveOwedInHold &&
            mode != RealMoneyImpactMode.NET_SHARE_ONLY &&
            mode != RealMoneyImpactMode.DEFERRED_ON_SETTLEMENT

        val targetHoldAcc = holdReserveAccount ?: sourcePaymentAccount
        val holdReservedToday = if (shouldHoldInMentalAccounting && targetHoldAcc != null) {
            roundCents(owedByMe)
        } else 0.0

        val baseAvailable = (targetHoldAcc?.availableBalance ?: currentAccBalance) - totalBankDebitToday
        val projectedFreeAvailable = roundCents((baseAvailable - holdReservedToday).coerceAtLeast(0.0))

        // Determinar Gasto Reconocido hoy en Estado de Resultados (PyG)
        val pnlExpenseToday = when (mode) {
            RealMoneyImpactMode.ACCRUAL_RECEIVABLE -> roundCents(myConsumptionShareAmount + myGmf4x1000Amount)
            RealMoneyImpactMode.GROSS_CASHFLOW -> roundCents(myPaidPrincipalAmount + myGmf4x1000Amount)
            RealMoneyImpactMode.NET_SHARE_ONLY -> roundCents(myConsumptionShareAmount + myGmf4x1000Amount)
            RealMoneyImpactMode.DEFERRED_ON_SETTLEMENT -> 0.0
        }

        // Determinar impacto hoy en el Presupuesto de la Categoría
        val budgetDeltaToday = when (budgetPolicy) {
            BudgetImpactPolicy.EXCLUDE_FROM_BUDGET -> 0.0
            BudgetImpactPolicy.MY_NET_SHARE_ONLY ->
                if (mode == RealMoneyImpactMode.DEFERRED_ON_SETTLEMENT) 0.0 else roundCents(myConsumptionShareAmount)
            BudgetImpactPolicy.FULL_OUTFLOW_UNTIL_REIMBURSED ->
                roundCents(bankDebitPrincipal)
        }

        // Determinar Cuentas por Cobrar (Activo NIIF) y Cuentas por Pagar (Pasivo NIIF)
        val receivableAssetDelta = when (mode) {
            RealMoneyImpactMode.ACCRUAL_RECEIVABLE -> if (myNetDelta > 0.0) myNetDelta else 0.0
            else -> 0.0
        }

        val payableLiabilityDelta = when (mode) {
            RealMoneyImpactMode.ACCRUAL_RECEIVABLE -> if (myNetDelta < 0.0) abs(myNetDelta) else 0.0
            else -> 0.0
        }

        // Variación del Patrimonio Neto = Activos (-Caja + CxC) - Pasivos (+CxP)
        val netWorthDelta = roundCents(-totalBankDebitToday + receivableAssetDelta - payableLiabilityDelta)

        val (headline, detail) = buildHumanExplanation(
            mode = mode,
            bankDebit = totalBankDebitToday,
            pnlExpense = pnlExpenseToday,
            receivable = receivableAssetDelta,
            payable = payableLiabilityDelta,
            holdReserved = holdReservedToday
        )

        return Outcome.Success(
            RealMoneyImpactSimulation(
                mode = mode,
                budgetPolicy = budgetPolicy,
                immediateAccountBalanceDebit = bankDebitPrincipal,
                immediateGmf4x1000Debit = if (bankDebitPrincipal > 0) myGmf4x1000Amount else 0.0,
                projectedAccountBalanceAfter = projectedBalance,
                mentalHoldReserveAccountId = if (holdReservedToday > 0.0) targetHoldAcc?.id else null,
                mentalHoldReservedDelta = holdReservedToday,
                projectedFreeAvailableBalanceAfter = projectedFreeAvailable,
                recognizedExpenseInPnlToday = pnlExpenseToday,
                budgetSpentDeltaToday = budgetDeltaToday,
                accountsReceivableAssetDelta = receivableAssetDelta,
                accountsPayableLiabilityDelta = payableLiabilityDelta,
                netWorthImmediateDelta = netWorthDelta,
                headlineSummary = headline,
                detailedAccountingExplanation = detail
            )
        )
    }

    /**
     * Calcula el impacto contable y bancario cuando se registra una Liquidación / Abono (PG-xxxx).
     */
    data class SettlementAccountingImpact(
        val bankBalanceDeltaForUser: Double,            // +Si me pagan / -Si yo pago
        val releaseFromHoldBalance: Double,             // Monto a liberar de AccountEntity.holdBalance
        val receivableAssetReduction: Double,           // Reducción de Cuenta por Cobrar
        val payableLiabilityReduction: Double,          // Reducción de Cuenta por Pagar
        val pnlIncomeDelta: Double,                     // Solo > 0 si el usuario configuró RECORD_AS_OTHER_INCOME
        val pnlExpenseReversalDelta: Double,            // Solo < 0 si el usuario configuró NEGATIVE_EXPENSE_NETTING
        val budgetSpentAdjustmentDelta: Double,         // Libera presupuesto si la política fue FULL_OUTFLOW_UNTIL_REIMBURSED
        val transactionTypeForLedger: String            // "SHARED_REIMBURSEMENT" | "INCOME" | "EXPENSE" | "TRANSFER"
    )

    fun computeSettlementImpact(
        isUserReceivingMoney: Boolean,
        settlementAmount: Double,
        originalExpenseMode: RealMoneyImpactMode,
        originalBudgetPolicy: BudgetImpactPolicy,
        reimbursementTreatment: ReimbursementAccountingTreatment,
        currentHoldReservedOnExpense: Double
    ): SettlementAccountingImpact {
        val cleanAmount = roundCents(settlementAmount.coerceAtLeast(0.0))

        return if (isUserReceivingMoney) {
            // Un tercero ME PAGA lo que me debía
            val budgetReversal = if (originalBudgetPolicy == BudgetImpactPolicy.FULL_OUTFLOW_UNTIL_REIMBURSED) {
                -cleanAmount
            } else 0.0

            when (originalExpenseMode) {
                RealMoneyImpactMode.ACCRUAL_RECEIVABLE -> SettlementAccountingImpact(
                    bankBalanceDeltaForUser = cleanAmount,
                    releaseFromHoldBalance = 0.0,
                    receivableAssetReduction = cleanAmount,
                    payableLiabilityReduction = 0.0,
                    pnlIncomeDelta = 0.0,
                    pnlExpenseReversalDelta = 0.0,
                    budgetSpentAdjustmentDelta = budgetReversal,
                    transactionTypeForLedger = "TRANSFER" // Recuperación de Activo CxC, no distorsiona Ingresos PyG
                )
                RealMoneyImpactMode.GROSS_CASHFLOW -> {
                    when (reimbursementTreatment) {
                        ReimbursementAccountingTreatment.NEGATIVE_EXPENSE_NETTING -> SettlementAccountingImpact(
                            bankBalanceDeltaForUser = cleanAmount,
                            releaseFromHoldBalance = 0.0,
                            receivableAssetReduction = 0.0,
                            payableLiabilityReduction = 0.0,
                            pnlIncomeDelta = 0.0,
                            pnlExpenseReversalDelta = -cleanAmount,
                            budgetSpentAdjustmentDelta = -cleanAmount,
                            transactionTypeForLedger = "TRANSFER"
                        )
                        ReimbursementAccountingTreatment.RECORD_AS_OTHER_INCOME -> SettlementAccountingImpact(
                            bankBalanceDeltaForUser = cleanAmount,
                            releaseFromHoldBalance = 0.0,
                            receivableAssetReduction = 0.0,
                            payableLiabilityReduction = 0.0,
                            pnlIncomeDelta = cleanAmount,
                            pnlExpenseReversalDelta = 0.0,
                            budgetSpentAdjustmentDelta = budgetReversal,
                            transactionTypeForLedger = "INCOME"
                        )
                        ReimbursementAccountingTreatment.OFFSET_RECEIVABLE_ASSET -> SettlementAccountingImpact(
                            bankBalanceDeltaForUser = cleanAmount,
                            releaseFromHoldBalance = 0.0,
                            receivableAssetReduction = cleanAmount,
                            payableLiabilityReduction = 0.0,
                            pnlIncomeDelta = 0.0,
                            pnlExpenseReversalDelta = 0.0,
                            budgetSpentAdjustmentDelta = budgetReversal,
                            transactionTypeForLedger = "TRANSFER"
                        )
                    }
                }
                RealMoneyImpactMode.NET_SHARE_ONLY,
                RealMoneyImpactMode.DEFERRED_ON_SETTLEMENT -> SettlementAccountingImpact(
                    bankBalanceDeltaForUser = cleanAmount,
                    releaseFromHoldBalance = 0.0,
                    receivableAssetReduction = 0.0,
                    payableLiabilityReduction = 0.0,
                    pnlIncomeDelta = 0.0,
                    pnlExpenseReversalDelta = 0.0,
                    budgetSpentAdjustmentDelta = 0.0,
                    transactionTypeForLedger = "TRANSFER"
                )
            }
        } else {
            // YO LE PAGO a un tercero lo que le debía
            val holdToRelease = cleanAmount.coerceAtMost(currentHoldReservedOnExpense.coerceAtLeast(0.0))
            val recognizesDeferredExpenseNow = originalExpenseMode == RealMoneyImpactMode.GROSS_CASHFLOW ||
                originalExpenseMode == RealMoneyImpactMode.DEFERRED_ON_SETTLEMENT

            SettlementAccountingImpact(
                bankBalanceDeltaForUser = -cleanAmount,
                releaseFromHoldBalance = holdToRelease,
                receivableAssetReduction = 0.0,
                payableLiabilityReduction = if (originalExpenseMode == RealMoneyImpactMode.ACCRUAL_RECEIVABLE) cleanAmount else 0.0,
                pnlIncomeDelta = 0.0,
                pnlExpenseReversalDelta = if (recognizesDeferredExpenseNow) cleanAmount else 0.0,
                budgetSpentAdjustmentDelta = if (recognizesDeferredExpenseNow) cleanAmount else 0.0,
                transactionTypeForLedger = if (recognizesDeferredExpenseNow) "EXPENSE" else "TRANSFER"
            )
        }
    }

    private fun buildHumanExplanation(
        mode: RealMoneyImpactMode,
        bankDebit: Double,
        pnlExpense: Double,
        receivable: Double,
        payable: Double,
        holdReserved: Double
    ): Pair<String, String> {
        return when (mode) {
            RealMoneyImpactMode.ACCRUAL_RECEIVABLE -> {
                if (receivable > 0.0) {
                    "Sale de tu banco hoy, pero solo tu cuota afecta tu Gasto Real" to
                        "Se descuentan $${bankDebit.toLong()} de tu cuenta bancaria hoy. En tu Estado de Resultados y Presupuesto solo se registran $${pnlExpense.toLong()} como gasto propio, y se crea una Cuenta por Cobrar (Activo NIIF) a tu favor por $${receivable.toLong()}."
                } else if (payable > 0.0) {
                    "Gasto devengado hoy + Reserva protegida en tu Dinero Libre" to
                        "No sale dinero de tu banco hoy, pero se registra tu gasto de $${pnlExpense.toLong()}, un Pasivo por Pagar de $${payable.toLong()} y se protegen $${holdReserved.toLong()} en Fondos Comprometidos (holdBalance) para que no gastes lo que debes."
                } else {
                    "Impacto exacto sobre tu cuota parte" to
                        "Tu desembolso coincide exactamente con tu consumo personal de $${pnlExpense.toLong()}."
                }
            }
            RealMoneyImpactMode.GROSS_CASHFLOW -> {
                "Modo Conservador: 100% asumido como gasto hasta que te reembolsen" to
                    "Se descuentan $${bankDebit.toLong()} de tu cuenta y se carga el 100% ($${pnlExpense.toLong()}) a tus gastos del mes hasta que tus amigos paguen su parte."
            }
            RealMoneyImpactMode.NET_SHARE_ONLY -> {
                "Débito exclusivo de tu cuota parte ($${bankDebit.toLong()})" to
                    "Tu cuenta bancaria y tu presupuesto solo disminuyen por tu consumo propio ($${pnlExpense.toLong()}), manteniendo el ticket total como soporte del grupo."
            }
            RealMoneyImpactMode.DEFERRED_ON_SETTLEMENT -> {
                "Sin afectación bancaria hoy (Diferido al liquidar el grupo)" to
                    "El gasto queda registrado en la bitácora del grupo con su consecutivo y afectará tus saldos reales únicamente cuando ejecutes la liquidación."
            }
        }
    }

    private fun roundCents(v: Double): Double = (v * 100.0).roundToLong() / 100.0
}
