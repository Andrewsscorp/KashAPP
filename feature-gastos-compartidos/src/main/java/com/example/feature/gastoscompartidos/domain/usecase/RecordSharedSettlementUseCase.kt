package com.example.feature.gastoscompartidos.domain.usecase

import com.example.core.error.DomainError
import com.example.core.id.IdGenerator
import com.example.core.result.Outcome
import com.example.data.local.dao.FinancialDao
import com.example.data.local.dao.OutboxDao
import com.example.data.local.dao.SharedExpenseDao
import com.example.data.local.entity.OutboxOperationEntity
import com.example.data.local.entity.SharedSettlementPaymentEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.engine.ConsecutiveSequenceEngine
import com.example.domain.models.shared.BudgetImpactPolicy
import com.example.domain.models.shared.RealMoneyImpactMode
import com.example.domain.models.shared.ReimbursementAccountingTreatment
import com.example.domain.models.shared.SequenceDocumentType
import com.example.domain.models.shared.SequenceResetPolicy
import com.example.domain.models.shared.SettlementPaymentMethod
import com.example.feature.gastoscompartidos.domain.engine.RealMoneyImpactEngine
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Caso de Uso ACID para registrar una Liquidación, Abono Parcial, Cruce (Netting) o Condonación (PG-xxxx / LQ-xxxx),
 * actualizando la cuenta bancaria real, liberando fondos retenidos en holdBalance y emitiendo comprobante con hash SHA-256.
 */
class RecordSharedSettlementUseCase(
    private val sharedExpenseDao: SharedExpenseDao,
    private val financialDao: FinancialDao,
    private val outboxDao: OutboxDao? = null,
    private val idGenerator: IdGenerator
) {
    constructor(
        sharedExpenseDao: SharedExpenseDao,
        financialDao: FinancialDao,
        idGenerator: IdGenerator
    ) : this(sharedExpenseDao, financialDao, null, idGenerator)

    data class RecordSettlementCommand(
        val userId: String = "user_default",
        val sharedExpenseId: String,
        val fromParticipantId: String,
        val toParticipantId: String,
        val paymentAmount: Double = 0.0,
        val amount: Double = paymentAmount,
        val currency: String = "COP",
        val settlementMethod: SettlementPaymentMethod = SettlementPaymentMethod.BANK_TRANSFER,
        val reimbursementTreatment: ReimbursementAccountingTreatment = ReimbursementAccountingTreatment.OFFSET_RECEIVABLE_ASSET,
        val kashAccountId: String? = null,
        val externalReferenceCode: String? = null,
        val proofDocumentLocalUri: String? = null,
        val notes: String? = null,
        val paymentDate: LocalDate = LocalDate.now(),
        val isFullGroupSettlementAct: Boolean = false,
        val idempotencyKey: String = "idemp_settlement_${System.currentTimeMillis()}"
    ) {
        val effectiveAmount: Double
            get() = if (paymentAmount > 0.0) paymentAmount else amount
    }

    data class SettlementCommand(
        val userId: String = "user_default",
        val sharedExpenseId: String,
        val fromParticipantId: String,
        val toParticipantId: String,
        val amount: Double = 0.0,
        val paymentAmount: Double = amount,
        val currency: String = "COP",
        val settlementMethod: SettlementPaymentMethod = SettlementPaymentMethod.BANK_TRANSFER,
        val reimbursementTreatment: ReimbursementAccountingTreatment = ReimbursementAccountingTreatment.OFFSET_RECEIVABLE_ASSET,
        val kashAccountId: String? = null,
        val externalReferenceCode: String? = null,
        val proofDocumentLocalUri: String? = null,
        val notes: String? = null,
        val paymentDate: LocalDate = LocalDate.now(),
        val isFullGroupSettlementAct: Boolean = false,
        val idempotencyKey: String = "idemp_settlement_${System.currentTimeMillis()}"
    )

    data class RecordedSettlementResult(
        val settlementId: String,
        val voucherConsecutiveCode: String,
        val ticketIntegrityHash: String,
        val accountingImpact: RealMoneyImpactEngine.SettlementAccountingImpact,
        val newExpenseStatus: String,
        val remainingExpenseDebt: Double = 0.0,
        val releasedHoldBalance: Double = accountingImpact.releaseFromHoldBalance,
        val updatedExpenseStatus: String = newExpenseStatus
    )

    suspend fun execute(
        command: SettlementCommand
    ): Outcome<RecordedSettlementResult, DomainError> = execute(
        RecordSettlementCommand(
            userId = command.userId,
            sharedExpenseId = command.sharedExpenseId,
            fromParticipantId = command.fromParticipantId,
            toParticipantId = command.toParticipantId,
            paymentAmount = if (command.paymentAmount > 0.0) command.paymentAmount else command.amount,
            amount = command.amount,
            currency = command.currency,
            settlementMethod = command.settlementMethod,
            reimbursementTreatment = command.reimbursementTreatment,
            kashAccountId = command.kashAccountId,
            externalReferenceCode = command.externalReferenceCode,
            proofDocumentLocalUri = command.proofDocumentLocalUri,
            notes = command.notes,
            paymentDate = command.paymentDate,
            isFullGroupSettlementAct = command.isFullGroupSettlementAct,
            idempotencyKey = command.idempotencyKey
        )
    )

    suspend fun execute(
        command: RecordSettlementCommand
    ): Outcome<RecordedSettlementResult, DomainError> {
        val effectivePaymentAmount = command.effectiveAmount
        if (effectivePaymentAmount <= 0.0) {
            return Outcome.Failure(DomainError.Validation.NegativeOrZeroAmount(effectivePaymentAmount))
        }

        val fullDetails = sharedExpenseDao.getSharedExpenseWithDetailsById(command.sharedExpenseId)
            ?: return Outcome.Failure(DomainError.NotFound.EntityNotFound("Gasto Compartido", command.sharedExpenseId))

        val expense = fullDetails.expense
        if (expense.status == "VOIDED") {
            return Outcome.Failure(
                DomainError.SharedExpense.CannotModifySettledOrVoidedExpense(
                    consecutiveCode = expense.consecutiveCode,
                    status = expense.status
                )
            )
        }
        val debtorSplit = fullDetails.splits.find { it.participantId == command.fromParticipantId }
            ?: return Outcome.Failure(DomainError.NotFound.EntityNotFound("Participante Deudor", command.fromParticipantId))

        if (effectivePaymentAmount > debtorSplit.remainingPendingAmount + 0.05) {
            return Outcome.Failure(
                DomainError.SharedExpense.OverSettlementAttempt(
                    pendingDebt = debtorSplit.remainingPendingAmount,
                    attemptedPayment = effectivePaymentAmount
                )
            )
        }

        // 1. Generar consecutivo PG-YYYYMM-XXXX o LQ-YYYY-XXX
        val docType = if (command.isFullGroupSettlementAct) {
            SequenceDocumentType.SETTLEMENT_ACT
        } else {
            SequenceDocumentType.PAYMENT_VOUCHER
        }
        val seqConfig = sharedExpenseDao.getSequenceConfig(
            userId = command.userId,
            sequenceType = docType.code,
            scopeGroupCode = "GLOBAL"
        )
        val seqSnapshot = ConsecutiveSequenceEngine.SequenceSnapshot(
            sequenceType = docType,
            prefix = seqConfig?.prefix ?: docType.defaultPrefix,
            patternTemplate = seqConfig?.patternTemplate ?: docType.defaultPattern,
            currentValue = seqConfig?.currentValue ?: 0L,
            resetPolicy = SequenceResetPolicy.entries.find { it.code == seqConfig?.resetPolicy } ?: SequenceResetPolicy.MONTHLY,
            lastResetPeriodKey = seqConfig?.lastResetPeriodKey ?: "%04d-%02d".format(command.paymentDate.year, command.paymentDate.monthValue)
        )
        val generatedVoucher = ConsecutiveSequenceEngine.generateNextAutomatic(
            snapshot = seqSnapshot,
            referenceDate = command.paymentDate
        )

        // 2. Calcular impacto contable NIIF y liberación de holdBalance
        val isUserReceiving = command.toParticipantId == "SELF_USER"
        val isUserPaying = command.fromParticipantId == "SELF_USER"
        val originalMode = RealMoneyImpactMode.entries.find { it.code == expense.realMoneyImpactMode }
            ?: RealMoneyImpactMode.ACCRUAL_RECEIVABLE
        val originalBudgetPolicy = BudgetImpactPolicy.entries.find { it.code == expense.budgetImpactPolicy }
            ?: BudgetImpactPolicy.MY_NET_SHARE_ONLY

        val accountingImpact = RealMoneyImpactEngine.computeSettlementImpact(
            isUserReceivingMoney = isUserReceiving,
            settlementAmount = effectivePaymentAmount,
            originalExpenseMode = originalMode,
            originalBudgetPolicy = originalBudgetPolicy,
            reimbursementTreatment = command.reimbursementTreatment,
            currentHoldReservedOnExpense = expense.holdReservedAmount
        )

        val nowMs = System.currentTimeMillis()
        val settlementId = idGenerator.generateId("sset")
        var kashAccountName: String? = null

        // 3. Afectar cuenta bancaria real si aplica
        val linkedTxId = if (command.kashAccountId != null && (isUserReceiving || isUserPaying) &&
            command.settlementMethod != SettlementPaymentMethod.FORGIVENESS &&
            command.settlementMethod != SettlementPaymentMethod.OFFSET_NETTING
        ) {
            val acc = financialDao.getAccountById(command.kashAccountId)
            if (acc != null) {
                kashAccountName = acc.name
                if (isUserReceiving) {
                    financialDao.addAccountBalance(acc.id, effectivePaymentAmount)
                } else if (isUserPaying) {
                    // Liberar primero holdBalance si estaba reservado en esta cuenta
                    if (accountingImpact.releaseFromHoldBalance > 0.0 && expense.holdReserveAccountId == acc.id) {
                        val updatedWithHoldRelease = acc.copy(
                            holdBalance = (acc.holdBalance - accountingImpact.releaseFromHoldBalance).coerceAtLeast(0.0),
                            availableBalance = acc.availableBalance + accountingImpact.releaseFromHoldBalance
                        )
                        financialDao.updateAccount(updatedWithHoldRelease)
                    }
                    financialDao.deductAccountBalance(acc.id, effectivePaymentAmount)
                }

                val txId = idGenerator.generateId("tx")
                financialDao.insertTransaction(
                    TransactionEntity(
                        id = txId,
                        userId = command.userId,
                        accountId = acc.id,
                        accountName = acc.name,
                        categoryId = expense.categoryId,
                        categoryName = expense.categoryNameSnapshot,
                        amount = effectivePaymentAmount,
                        currency = command.currency,
                        type = accountingImpact.transactionTypeForLedger,
                        date = command.paymentDate.toString(),
                        description = "[${generatedVoucher.formattedCode}] Liquidación de ${expense.consecutiveCode}",
                        receiptUrl = command.proofDocumentLocalUri,
                        idempotencyKey = "${command.idempotencyKey}_tx",
                        tags = "Liquidación,${generatedVoucher.formattedCode},${expense.consecutiveCode}",
                        sharedExpenseId = expense.id,
                        sharedSettlementId = settlementId,
                        sharedConsecutiveCode = generatedVoucher.formattedCode,
                        createdAt = nowMs
                    )
                )
                txId
            } else null
        } else null

        // 4. Actualizar Split del participante y Estado General del Gasto
        val newRemainingForDebtor = (debtorSplit.remainingPendingAmount - effectivePaymentAmount).coerceAtLeast(0.0)
        val newDebtorStatus = when {
            command.settlementMethod == SettlementPaymentMethod.FORGIVENESS && newRemainingForDebtor < 0.01 -> "FORGIVEN"
            newRemainingForDebtor < 0.01 -> "SETTLED"
            else -> "PARTIAL"
        }

        val updatedDebtorSplit = debtorSplit.copy(
            amountAlreadySettled = debtorSplit.amountAlreadySettled + effectivePaymentAmount,
            remainingPendingAmount = newRemainingForDebtor,
            settlementStatus = newDebtorStatus
        )
        sharedExpenseDao.updateSharedExpenseSplits(listOf(updatedDebtorSplit))

        val otherRemainingSum = fullDetails.splits
            .filter { it.id != debtorSplit.id }
            .sumOf { it.remainingPendingAmount }
        val totalRemainingOnExpense = otherRemainingSum + newRemainingForDebtor
        val newExpenseStatus = if (totalRemainingOnExpense < 0.01) "SETTLED" else "PARTIALLY_SETTLED"

        val updatedExpense = expense.copy(
            settledAmountTotal = expense.settledAmountTotal + effectivePaymentAmount,
            holdReservedAmount = (expense.holdReservedAmount - accountingImpact.releaseFromHoldBalance).coerceAtLeast(0.0),
            status = newExpenseStatus,
            updatedAt = nowMs
        )
        sharedExpenseDao.updateSharedExpenseHeader(updatedExpense)

        if (seqConfig != null) {
            sharedExpenseDao.upsertSequenceConfig(
                seqConfig.copy(
                    currentValue = generatedVoucher.nextSequenceValue,
                    lastResetPeriodKey = generatedVoucher.resolvedPeriodKey,
                    updatedAt = nowMs,
                    version = seqConfig.version + 1
                )
            )
        }

        val voucherHash = ConsecutiveSequenceEngine.computeTicketIntegrityHash(
            consecutiveCode = expense.consecutiveCode,
            receiptTicketCode = generatedVoucher.formattedCode,
            totalAmount = effectivePaymentAmount,
            currency = command.currency,
            expenseDate = command.paymentDate.toString(),
            participantSharesSignature = "${command.fromParticipantId}->${command.toParticipantId}:${"%.2f".format(java.util.Locale.US, effectivePaymentAmount)}"
        )

        val paymentEntity = SharedSettlementPaymentEntity(
            id = settlementId,
            userId = command.userId,
            voucherConsecutiveCode = generatedVoucher.formattedCode,
            isFullGroupSettlementAct = command.isFullGroupSettlementAct,
            groupId = expense.groupId,
            sharedExpenseId = expense.id,
            fromParticipantId = command.fromParticipantId,
            toParticipantId = command.toParticipantId,
            amount = effectivePaymentAmount,
            currency = command.currency,
            settlementMethod = command.settlementMethod.code,
            reimbursementTreatmentApplied = command.reimbursementTreatment.code,
            kashAccountId = command.kashAccountId,
            kashAccountNameSnapshot = kashAccountName,
            linkedTransactionId = linkedTxId,
            releasedHoldAmount = accountingImpact.releaseFromHoldBalance,
            externalReferenceCode = command.externalReferenceCode,
            proofDocumentLocalUri = command.proofDocumentLocalUri,
            ticketIntegrityHash = voucherHash,
            notes = command.notes,
            paymentDate = command.paymentDate.toString(),
            paymentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            idempotencyKey = command.idempotencyKey,
            createdAt = nowMs
        )
        sharedExpenseDao.insertSettlementPayment(paymentEntity)

        outboxDao?.insertOperation(
            OutboxOperationEntity(
                id = idGenerator.generateId("outbox"),
                userId = command.userId,
                entityType = "SHARED_SETTLEMENT",
                entityId = settlementId,
                operation = "CREATE",
                payloadJson = "{\"voucher\":\"${generatedVoucher.formattedCode}\",\"amount\":$effectivePaymentAmount}",
                idempotencyKey = "${command.idempotencyKey}_outbox",
                createdAt = nowMs
            )
        )

        return Outcome.Success(
            RecordedSettlementResult(
                settlementId = settlementId,
                voucherConsecutiveCode = generatedVoucher.formattedCode,
                ticketIntegrityHash = voucherHash,
                accountingImpact = accountingImpact,
                newExpenseStatus = newExpenseStatus,
                remainingExpenseDebt = totalRemainingOnExpense,
                releasedHoldBalance = accountingImpact.releaseFromHoldBalance,
                updatedExpenseStatus = newExpenseStatus
            )
        )
    }
}
