package com.example.feature.gastoscompartidos.domain.usecase

import com.example.core.error.DomainError
import com.example.core.id.IdGenerator
import com.example.core.result.Outcome
import com.example.data.local.dao.FinancialDao
import com.example.data.local.dao.SharedExpenseDao
import com.example.data.local.entity.TransactionEntity
import java.time.LocalDate

/**
 * Caso de Uso de Anulación Contable NIIF:
 * Marca el Gasto Compartido como VOIDED conservando el consecutivo en la bitácora (sin huecos de auditoría),
 * libera cualquier reserva retenida en holdBalance y revierte los saldos pendientes de los splits.
 */
class VoidSharedExpenseUseCase(
    private val sharedExpenseDao: SharedExpenseDao,
    private val financialDao: FinancialDao,
    private val idGenerator: IdGenerator? = null
) {
    data class VoidExecutionResult(
        val voidedConsecutiveCode: String,
        val refundedAccountAmount: Double,
        val releasedHoldAmount: Double
    )

    suspend fun execute(
        sharedExpenseId: String,
        voidReason: String,
        userId: String = "user_default"
    ): Outcome<VoidExecutionResult, DomainError> {
        if (voidReason.isBlank()) {
            return Outcome.Failure(DomainError.Validation.Blank)
        }
        val details = sharedExpenseDao.getSharedExpenseWithDetailsById(sharedExpenseId)
            ?: return Outcome.Failure(DomainError.NotFound.EntityNotFound("Gasto Compartido", sharedExpenseId))

        val expense = details.expense
        if (expense.status == "VOIDED") {
            return Outcome.Success(VoidExecutionResult(expense.consecutiveCode, 0.0, 0.0))
        }

        var releasedHold = 0.0
        var refundedBank = 0.0

        // Liberar holdBalance si existía reserva activa
        val holdAccId = expense.holdReserveAccountId
        if (expense.holdReservedAmount > 0.0 && holdAccId != null) {
            val acc = financialDao.getAccountById(holdAccId)
            if (acc != null) {
                releasedHold = expense.holdReservedAmount
                financialDao.updateAccount(
                    acc.copy(
                        holdBalance = (acc.holdBalance - releasedHold).coerceAtLeast(0.0),
                        availableBalance = acc.availableBalance + releasedHold
                    )
                )
            }
        }

        // Reintegrar saldo bancario si hubo débito directo de SELF_USER
        for (payer in details.payers) {
            val payerAccId = payer.kashAccountId
            if (payer.participantId == "SELF_USER" && payerAccId != null && payer.paidAmount > 0.0) {
                val totalRefund = payer.paidAmount + payer.gmfGeneratedAmount
                financialDao.addAccountBalance(payerAccId, totalRefund)
                refundedBank += totalRefund

                val acc = financialDao.getAccountById(payerAccId)
                if (acc != null && idGenerator != null) {
                    financialDao.insertTransaction(
                        TransactionEntity(
                            id = idGenerator.generateId("tx"),
                            userId = userId,
                            accountId = acc.id,
                            accountName = acc.name,
                            categoryId = expense.categoryId,
                            categoryName = expense.categoryNameSnapshot,
                            amount = totalRefund,
                            currency = expense.currency,
                            type = "INCOME",
                            date = LocalDate.now().toString(),
                            description = "[REVERSO ANULACIÓN ${expense.consecutiveCode}] ${voidReason.trim()}",
                            receiptUrl = null,
                            idempotencyKey = "void_${expense.id}_${payer.id}",
                            tags = "Anulado,${expense.consecutiveCode}",
                            sharedExpenseId = expense.id,
                            sharedConsecutiveCode = expense.consecutiveCode,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        val voidedSplits = details.splits.map {
            it.copy(remainingPendingAmount = 0.0, settlementStatus = "NOT_APPLICABLE")
        }
        sharedExpenseDao.updateSharedExpenseSplits(voidedSplits)
        sharedExpenseDao.voidSharedExpenseById(sharedExpenseId, voidReason.trim())

        return Outcome.Success(
            VoidExecutionResult(
                voidedConsecutiveCode = expense.consecutiveCode,
                refundedAccountAmount = refundedBank,
                releasedHoldAmount = releasedHold
            )
        )
    }
}
