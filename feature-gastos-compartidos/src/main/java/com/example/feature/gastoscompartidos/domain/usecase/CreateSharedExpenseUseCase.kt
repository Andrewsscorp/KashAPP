package com.example.feature.gastoscompartidos.domain.usecase

import com.example.core.error.DomainError
import com.example.core.id.IdGenerator
import com.example.core.result.Outcome
import com.example.data.local.dao.FinancialDao
import com.example.data.local.dao.OutboxDao
import com.example.data.local.dao.SharedExpenseDao
import com.example.data.local.entity.OutboxOperationEntity
import com.example.data.local.entity.SharedExpenseEntity
import com.example.data.local.entity.SharedExpenseLineItemEntity
import com.example.data.local.entity.SharedExpensePayerEntity
import com.example.data.local.entity.SharedExpenseSplitEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.engine.ConsecutiveSequenceEngine
import com.example.domain.models.shared.BudgetImpactPolicy
import com.example.domain.models.shared.RealMoneyImpactMode
import com.example.domain.models.shared.SequenceDocumentType
import com.example.domain.models.shared.SequenceResetPolicy
import com.example.feature.gastoscompartidos.domain.engine.MultiPayerFinancialEngine
import com.example.feature.gastoscompartidos.domain.engine.RealMoneyImpactEngine
import com.example.feature.gastoscompartidos.domain.engine.SharedSplitCalculatorEngine
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class CreateSharedExpenseUseCase(
    private val sharedExpenseDao: SharedExpenseDao,
    private val financialDao: FinancialDao,
    private val outboxDao: OutboxDao,
    private val idGenerator: IdGenerator
) {

    data class CreateSharedExpenseCommand(
        val userId: String = "user_default",
        val groupId: String? = null,
        val groupCode: String = "GLOBAL",
        val manualConsecutiveOverride: String? = null,
        val externalInvoiceNumber: String? = null,
        val title: String,
        val description: String? = null,
        val merchantName: String? = null,
        val merchantNit: String? = null,
        val categoryId: String,
        val categoryName: String,
        val expenseDate: LocalDate = LocalDate.now(),
        val currency: String = "COP",
        val splitRequest: SharedSplitCalculatorEngine.SplitCalculationRequest,
        val payerInputs: List<MultiPayerFinancialEngine.PayerContributionInput>,
        val realMoneyImpactMode: RealMoneyImpactMode = RealMoneyImpactMode.ACCRUAL_RECEIVABLE,
        val budgetImpactPolicy: BudgetImpactPolicy = BudgetImpactPolicy.MY_NET_SHARE_ONLY,
        val autoReserveOwedInHold: Boolean = true,
        val preferredHoldReserveAccountId: String? = null,
        val receiptImageLocalUri: String? = null,
        val dueDateForSettlement: String? = null,
        val idempotencyKey: String
    )

    data class CreatedSharedExpenseResult(
        val sharedExpenseId: String,
        val consecutiveCode: String,
        val receiptTicketCode: String,
        val ticketIntegrityHash: String,
        val realMoneyImpact: RealMoneyImpactEngine.RealMoneyImpactSimulation,
        val splitSummary: SharedSplitCalculatorEngine.SplitCalculationSummary
    )

    suspend fun execute(
        command: CreateSharedExpenseCommand
    ): Outcome<CreatedSharedExpenseResult, DomainError> {
        // 1. Resolver Consecutivo de Gasto (GC-xxxx) y de Ticket (TK-xxxx)
        val expSeqEntity = sharedExpenseDao.getSequenceConfig(
            userId = command.userId,
            sequenceType = SequenceDocumentType.SHARED_EXPENSE.code,
            scopeGroupCode = command.groupCode
        ) ?: sharedExpenseDao.getSequenceConfig(
            userId = command.userId,
            sequenceType = SequenceDocumentType.SHARED_EXPENSE.code,
            scopeGroupCode = "GLOBAL"
        )

        val expSnapshot = ConsecutiveSequenceEngine.SequenceSnapshot(
            sequenceType = SequenceDocumentType.SHARED_EXPENSE,
            scopeGroupCode = expSeqEntity?.scopeGroupCode ?: "GLOBAL",
            prefix = expSeqEntity?.prefix ?: "GC",
            patternTemplate = expSeqEntity?.patternTemplate ?: "{PREFIX}-{YYYY}-{SEQ:4}",
            currentValue = expSeqEntity?.currentValue ?: 0L,
            resetPolicy = SequenceResetPolicy.entries.find { it.code == expSeqEntity?.resetPolicy } ?: SequenceResetPolicy.YEARLY,
            lastResetPeriodKey = expSeqEntity?.lastResetPeriodKey ?: command.expenseDate.year.toString()
        )

        val resolvedExpenseConsecutive = if (!command.manualConsecutiveOverride.isNullOrBlank()) {
            val norm = com.example.core.text.TextNormalizer.normalizeForUniqueness(command.manualConsecutiveOverride)
            val exists = sharedExpenseDao.existsExpenseConsecutive(command.userId, command.manualConsecutiveOverride, norm)
            when (val manualOut = ConsecutiveSequenceEngine.validateAndResolveManualOrOcrCode(
                rawManualInput = command.manualConsecutiveOverride,
                snapshot = expSnapshot,
                alreadyExistsInDb = exists,
                referenceDate = command.expenseDate,
                groupCode = command.groupCode
            )) {
                is Outcome.Failure -> return manualOut
                is Outcome.Success -> manualOut.value
            }
        } else {
            ConsecutiveSequenceEngine.generateNextAutomatic(
                snapshot = expSnapshot,
                referenceDate = command.expenseDate,
                groupCode = command.groupCode
            )
        }

        val ticketSnapshot = expSnapshot.copy(
            sequenceType = SequenceDocumentType.RECEIPT_TICKET,
            prefix = "TK",
            currentValue = resolvedExpenseConsecutive.nextSequenceValue - 1
        )
        val resolvedTicketConsecutive = ConsecutiveSequenceEngine.generateNextAutomatic(
            snapshot = ticketSnapshot,
            referenceDate = command.expenseDate,
            groupCode = command.groupCode
        )

        // 2. Evaluar Pagadores Múltiples (4x1000 e intereses de cuotas TC)
        val payerEval = MultiPayerFinancialEngine.evaluatePayers(
            inputs = command.payerInputs,
            prorateGmfAmongDebtors = command.splitRequest.prorateGmfAmongDebtors
        )

        // 3. Ejecutar el Motor Matemático de División con el número de consecutivo para rotación de centavos
        val enrichedSplitRequest = command.splitRequest.copy(
            gmf4x1000Amount = payerEval.myTotalGmfGenerated,
            creditCardInterestSurcharge = payerEval.totalCreditCardInterestSurchargeForGroup,
            consecutiveSequenceNumber = resolvedExpenseConsecutive.nextSequenceValue
        )

        val splitSummary = when (val splitOut = SharedSplitCalculatorEngine.calculateSplits(enrichedSplitRequest)) {
            is Outcome.Failure -> return splitOut
            is Outcome.Success -> splitOut.value
        }

        val mySplit = splitSummary.participantSplits.find { it.isSelf || it.participantId == "SELF_USER" }
        val myConsumptionShare = mySplit?.totalOwedShare ?: 0.0

        // 4. Evaluar y validar el Impacto en el Dinero Real del Usuario
        val myPrimaryPayerInput = command.payerInputs.find { it.participantId == "SELF_USER" }
        val holdAccount = if (command.preferredHoldReserveAccountId != null) {
            financialDao.getAccountById(command.preferredHoldReserveAccountId)
        } else {
            myPrimaryPayerInput?.kashAccount
        }

        val realMoneyImpact = when (val impactOut = RealMoneyImpactEngine.simulateExpenseImpact(
            mode = command.realMoneyImpactMode,
            budgetPolicy = command.budgetImpactPolicy,
            myPaidPrincipalAmount = payerEval.myTotalPrincipalPaid,
            myGmf4x1000Amount = payerEval.myTotalGmfGenerated,
            myConsumptionShareAmount = myConsumptionShare,
            sourcePaymentAccount = myPrimaryPayerInput?.kashAccount,
            holdReserveAccount = holdAccount,
            autoReserveOwedInHold = command.autoReserveOwedInHold
        )) {
            is Outcome.Failure -> return impactOut
            is Outcome.Success -> impactOut.value
        }

        // 5. Calcular Firma Criptográfica SHA-256 del Ticket
        val sharesSignature = splitSummary.participantSplits
            .filter { it.isIncluded }
            .joinToString(";") { "${it.participantId}:${"%.2f".format(java.util.Locale.US, it.totalOwedShare)}" }

        val integrityHash = ConsecutiveSequenceEngine.computeTicketIntegrityHash(
            consecutiveCode = resolvedExpenseConsecutive.formattedCode,
            receiptTicketCode = resolvedTicketConsecutive.formattedCode,
            totalAmount = splitSummary.effectiveTotalAmount,
            currency = command.currency,
            expenseDate = command.expenseDate.toString(),
            participantSharesSignature = sharesSignature
        )

        // 6. Persistir Entidades y Actualizar Cuentas Bancarias Reales
        val sharedExpenseId = idGenerator.generateId("sexp")
        val nowMs = System.currentTimeMillis()
        val timeFormatted = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))

        val linkedLedgerTxId = if (realMoneyImpact.immediateAccountBalanceDebit > 0.0 && myPrimaryPayerInput?.kashAccount != null) {
            val txId = idGenerator.generateId("tx")
            val acc = myPrimaryPayerInput.kashAccount
            val totalDebit = realMoneyImpact.immediateAccountBalanceDebit + realMoneyImpact.immediateGmf4x1000Debit
            financialDao.deductAccountBalance(acc.id, totalDebit)

            financialDao.insertTransaction(
                TransactionEntity(
                    id = txId,
                    userId = command.userId,
                    accountId = acc.id,
                    accountName = acc.name,
                    categoryId = command.categoryId,
                    categoryName = command.categoryName,
                    amount = totalDebit,
                    currency = command.currency,
                    type = "EXPENSE",
                    date = command.expenseDate.toString(),
                    description = "[${resolvedExpenseConsecutive.formattedCode}] ${command.title}",
                    receiptUrl = command.receiptImageLocalUri,
                    idempotencyKey = "${command.idempotencyKey}_ledger",
                    tags = "Compartido,${resolvedExpenseConsecutive.formattedCode}",
                    sharedExpenseId = sharedExpenseId,
                    sharedConsecutiveCode = resolvedExpenseConsecutive.formattedCode,
                    accountingRecognizedExpenseAmount = realMoneyImpact.recognizedExpenseInPnlToday,
                    receivableOrPayableDeltaAmount = realMoneyImpact.accountsReceivableAssetDelta - realMoneyImpact.accountsPayableLiabilityDelta,
                    createdAt = nowMs
                )
            )
            txId
        } else null

        // Si el usuario DEBE dinero y se activó la reserva en holdBalance (Mental Accounting)
        if (realMoneyImpact.mentalHoldReservedDelta > 0.0 && realMoneyImpact.mentalHoldReserveAccountId != null) {
            val targetAcc = financialDao.getAccountById(realMoneyImpact.mentalHoldReserveAccountId)
            if (targetAcc != null) {
                val updatedAcc = targetAcc.copy(
                    holdBalance = targetAcc.holdBalance + realMoneyImpact.mentalHoldReservedDelta,
                    availableBalance = (targetAcc.availableBalance - realMoneyImpact.mentalHoldReservedDelta).coerceAtLeast(0.0)
                )
                financialDao.updateAccount(updatedAcc)
            }
        }

        val expenseEntity = SharedExpenseEntity(
            id = sharedExpenseId,
            userId = command.userId,
            consecutiveCode = resolvedExpenseConsecutive.formattedCode,
            consecutiveNormalized = resolvedExpenseConsecutive.normalizedCode,
            isManualConsecutive = resolvedExpenseConsecutive.isManualOverride,
            receiptTicketCode = resolvedTicketConsecutive.formattedCode,
            externalInvoiceNumber = command.externalInvoiceNumber,
            groupId = command.groupId,
            title = command.title,
            description = command.description,
            merchantNameSnapshot = command.merchantName,
            merchantNit = command.merchantNit,
            categoryId = command.categoryId,
            categoryNameSnapshot = command.categoryName,
            expenseDate = command.expenseDate.toString(),
            expenseTime = timeFormatted,
            currency = command.currency,
            subtotalAmount = enrichedSplitRequest.subtotalAmount,
            taxIvaAmount = enrichedSplitRequest.taxIvaAmount,
            taxConsumptionAmount = enrichedSplitRequest.taxConsumptionAmount,
            tipAmount = enrichedSplitRequest.tipAmount,
            discountAmount = enrichedSplitRequest.discountAmount,
            gmfAmount = payerEval.myTotalGmfGenerated,
            creditCardInterestSurcharge = payerEval.totalCreditCardInterestSurchargeForGroup,
            totalAmount = splitSummary.effectiveTotalAmount,
            splitMethod = enrichedSplitRequest.splitMethod.code,
            taxAndTipProrationRule = enrichedSplitRequest.taxAndTipProrationRule.code,
            roundingRule = enrichedSplitRequest.roundingStepRule.code,
            roundingRemainderTarget = enrichedSplitRequest.roundingRemainderTarget.code,
            realMoneyImpactMode = command.realMoneyImpactMode.code,
            budgetImpactPolicy = command.budgetImpactPolicy.code,
            holdReserveAccountId = realMoneyImpact.mentalHoldReserveAccountId,
            holdReservedAmount = realMoneyImpact.mentalHoldReservedDelta,
            myPaidAmount = payerEval.myTotalPrincipalPaid,
            myConsumptionShareAmount = myConsumptionShare,
            myNetPositionDelta = payerEval.myTotalPrincipalPaid - myConsumptionShare,
            status = "PENDING_SETTLEMENT",
            receiptImageLocalUri = command.receiptImageLocalUri,
            ticketIntegrityHash = integrityHash,
            dueDateForSettlement = command.dueDateForSettlement,
            idempotencyKey = command.idempotencyKey,
            createdAt = nowMs,
            updatedAt = nowMs
        )

        val payerEntities = payerEval.contributions.map { p ->
            SharedExpensePayerEntity(
                id = idGenerator.generateId("spay"),
                sharedExpenseId = sharedExpenseId,
                participantId = p.participantId,
                paidAmount = p.paidPrincipalAmount,
                kashAccountId = p.kashAccountId,
                kashAccountNameSnapshot = p.kashAccountName,
                paymentInstrumentType = p.paymentInstrumentType,
                creditCardInstallments = p.creditCardInstallments,
                passCreditCardInterestToGroup = p.projectedCreditCardInterestTotal > 0.0,
                gmfGeneratedAmount = p.gmf4x1000Generated,
                linkedTransactionId = if (p.isSelfUser) linkedLedgerTxId else null
            )
        }

        val splitEntities = splitSummary.participantSplits.map { s ->
            SharedExpenseSplitEntity(
                id = idGenerator.generateId("sspl"),
                sharedExpenseId = sharedExpenseId,
                participantId = s.participantId,
                isIncluded = s.isIncluded,
                rawInputWeightOrPct = s.rawWeightOrPercentage,
                baseSubtotalShare = s.baseSubtotalShare,
                allocatedTaxAmount = s.allocatedTaxAmount,
                allocatedTipAmount = s.allocatedTipAmount,
                allocatedDiscountAmount = s.allocatedDiscountAmount,
                allocatedGmfAmount = s.allocatedGmfAndFinancialAmount,
                roundingAdjustment = s.roundingAdjustment,
                totalOwedShare = s.totalOwedShare,
                paidByThisParticipant = s.paidByThisParticipant,
                netBalanceForExpense = s.netBalanceForExpense,
                amountAlreadySettled = 0.0,
                remainingPendingAmount = s.remainingPendingAmount,
                settlementStatus = s.initialSettlementStatus.code
            )
        }

        val lineItemEntities = enrichedSplitRequest.ocrLineItems.mapIndexed { idx, item ->
            SharedExpenseLineItemEntity(
                id = idGenerator.generateId("sitem"),
                sharedExpenseId = sharedExpenseId,
                itemOrder = idx + 1,
                description = item.description,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                lineTotalPrice = item.lineTotalPrice,
                isTaxExempt = item.isTaxExempt,
                assignedParticipantIdsCsv = item.assignedParticipantIds.joinToString(",")
            )
        }

        val updatedExpSeq = expSeqEntity?.copy(
            currentValue = resolvedExpenseConsecutive.nextSequenceValue,
            lastResetPeriodKey = resolvedExpenseConsecutive.resolvedPeriodKey,
            updatedAt = nowMs,
            version = expSeqEntity.version + 1
        )

        sharedExpenseDao.insertCompleteSharedExpenseTransactional(
            updatedExpenseSeqConfig = updatedExpSeq,
            updatedTicketSeqConfig = null,
            expense = expenseEntity,
            payers = payerEntities,
            splits = splitEntities,
            lineItems = lineItemEntities
        )

        outboxDao.insertOperation(
            OutboxOperationEntity(
                id = idGenerator.generateId("outbox"),
                userId = command.userId,
                entityType = "SHARED_EXPENSE",
                entityId = sharedExpenseId,
                operation = "CREATE",
                payloadJson = "{\"consecutive\":\"${resolvedExpenseConsecutive.formattedCode}\",\"total\":${splitSummary.effectiveTotalAmount}}",
                idempotencyKey = "${command.idempotencyKey}_outbox",
                createdAt = nowMs
            )
        )

        return Outcome.Success(
            CreatedSharedExpenseResult(
                sharedExpenseId = sharedExpenseId,
                consecutiveCode = resolvedExpenseConsecutive.formattedCode,
                receiptTicketCode = resolvedTicketConsecutive.formattedCode,
                ticketIntegrityHash = integrityHash,
                realMoneyImpact = realMoneyImpact,
                splitSummary = splitSummary
            )
        )
    }
}
