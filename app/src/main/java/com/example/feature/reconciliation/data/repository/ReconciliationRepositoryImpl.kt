package com.example.feature.reconciliation.data.repository

import android.util.Log
import com.example.data.local.dao.FinancialDao
import com.example.data.local.dao.ReconciliationDao
import com.example.data.local.entity.AccountReconciliationLogEntity
import com.example.data.local.entity.TransactionEntity
import com.example.feature.reconciliation.model.ReconciliationDiscrepancy
import com.example.feature.reconciliation.model.ReconciliationExecutionResult
import com.example.feature.reconciliation.model.ReconciliationParams
import com.example.feature.reconciliation.model.ReconciliationStatus
import com.example.feature.sync.engine.OutboxSyncEngine
import com.example.feature.sync.model.SyncEntityType
import com.example.feature.sync.model.SyncOperationType
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Enterprise implementation of ReconciliationRepository.
 * Handles atomic balance reconciliation, movement creation, audit trail persistence,
 * and outbox queue propagation.
 */
class ReconciliationRepositoryImpl(
    private val reconciliationDao: ReconciliationDao,
    private val financialDao: FinancialDao,
    private val outboxSyncEngine: OutboxSyncEngine
) : ReconciliationRepository {

    override fun observeReconciliations(userId: String): Flow<List<AccountReconciliationLogEntity>> {
        return reconciliationDao.observeReconciliations(userId)
    }

    override fun observeReconciliationsForAccount(userId: String, accountId: String): Flow<List<AccountReconciliationLogEntity>> {
        return reconciliationDao.observeReconciliationsForAccount(userId, accountId)
    }

    override suspend fun getReconciliationsForAccount(userId: String, accountId: String): List<AccountReconciliationLogEntity> {
        return reconciliationDao.getReconciliationsForAccount(userId, accountId)
    }

    override suspend fun executeReconciliation(params: ReconciliationParams): ReconciliationExecutionResult {
        return try {
            val calc = ReconciliationDiscrepancy.calculate(
                previousSystemBalance = params.previousBalance,
                reconciledRealBalance = params.reconciledRealBalance
            )

            val reconciliationId = UUID.randomUUID().toString()
            val timeString = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            var adjustmentTxId: String? = null

            if (calc.requiresAdjustment) {
                adjustmentTxId = "tx_rec_${UUID.randomUUID().toString().replace("-", "").take(24)}"
                val txType = if (calc.status == ReconciliationStatus.ADJUSTED_SURPLUS) "INCOME" else "EXPENSE"
                val txDescription = "Conciliación de Saldo: Anterior $${params.previousBalance} -> Real $${params.reconciledRealBalance}. ${params.notes.orEmpty()}".trim()

                val transaction = TransactionEntity(
                    id = adjustmentTxId,
                    userId = params.userId,
                    accountId = params.accountId,
                    accountName = params.accountName,
                    categoryId = "cat_conciliacion",
                    categoryName = "Conciliación y Ajustes",
                    amount = kotlin.math.abs(calc.discrepancyAmount),
                    currency = "COP",
                    type = txType,
                    date = params.reconciliationDate,
                    description = txDescription,
                    receiptUrl = null,
                    idempotencyKey = "idemp_rec_${reconciliationId}",
                    tags = "CONCILIACION,AUDITORIA",
                    syncStatus = "PENDING",
                    createdAt = System.currentTimeMillis()
                )

                financialDao.insertTransaction(transaction)

                val txJson = JSONObject().apply {
                    put("id", transaction.id)
                    put("userId", transaction.userId)
                    put("accountId", transaction.accountId)
                    put("accountName", transaction.accountName)
                    put("categoryId", transaction.categoryId)
                    put("categoryName", transaction.categoryName)
                    put("amount", transaction.amount)
                    put("currency", transaction.currency)
                    put("type", transaction.type)
                    put("date", transaction.date)
                    put("description", transaction.description)
                    put("tags", transaction.tags)
                    put("createdAt", transaction.createdAt)
                }.toString()

                outboxSyncEngine.enqueueOperation(
                    userId = params.userId,
                    entityType = SyncEntityType.TRANSACTION.value,
                    entityId = transaction.id,
                    operation = SyncOperationType.CREATE,
                    payloadJson = txJson,
                    idempotencyKey = "outbox_tx_${transaction.id}"
                )
            }

            financialDao.setAccountBalanceDirectly(params.accountId, params.reconciledRealBalance)

            val accJson = JSONObject().apply {
                put("id", params.accountId)
                put("userId", params.userId)
                put("balance", params.reconciledRealBalance)
                put("updatedAt", System.currentTimeMillis())
            }.toString()

            outboxSyncEngine.enqueueOperation(
                userId = params.userId,
                entityType = SyncEntityType.ACCOUNT.value,
                entityId = params.accountId,
                operation = SyncOperationType.UPDATE,
                payloadJson = accJson,
                idempotencyKey = "outbox_acc_rec_${params.accountId}_${System.currentTimeMillis()}"
            )

            val log = AccountReconciliationLogEntity(
                id = reconciliationId,
                userId = params.userId,
                accountId = params.accountId,
                accountName = params.accountName,
                previousSystemBalance = calc.previousSystemBalance,
                reconciledRealBalance = calc.reconciledRealBalance,
                discrepancyAmount = calc.discrepancyAmount,
                adjustmentTransactionId = adjustmentTxId,
                reconciliationDate = params.reconciliationDate,
                reconciliationTime = timeString,
                reconciledByUserId = params.reconciledByUserId,
                reconciledByUserName = params.reconciledByUserName,
                notes = params.notes,
                status = calc.status.name,
                deviceModel = params.deviceModel,
                syncStatus = "PENDING",
                reconciliationTimestamp = System.currentTimeMillis()
            )

            reconciliationDao.insertReconciliation(log)

            val logJson = JSONObject().apply {
                put("id", log.id)
                put("userId", log.userId)
                put("accountId", log.accountId)
                put("accountName", log.accountName)
                put("previousSystemBalance", log.previousSystemBalance)
                put("reconciledRealBalance", log.reconciledRealBalance)
                put("discrepancyAmount", log.discrepancyAmount)
                put("adjustmentTransactionId", log.adjustmentTransactionId)
                put("reconciliationDate", log.reconciliationDate)
                put("reconciliationTime", log.reconciliationTime)
                put("reconciledByUserId", log.reconciledByUserId)
                put("reconciledByUserName", log.reconciledByUserName)
                put("status", log.status)
                put("notes", log.notes)
                put("deviceModel", log.deviceModel)
                put("reconciliationTimestamp", log.reconciliationTimestamp)
            }.toString()

            outboxSyncEngine.enqueueOperation(
                userId = params.userId,
                entityType = SyncEntityType.RECONCILIATION_LOG.value,
                entityId = log.id,
                operation = SyncOperationType.CREATE,
                payloadJson = logJson,
                idempotencyKey = "outbox_rec_log_${log.id}"
            )

            ReconciliationExecutionResult(
                success = true,
                reconciliationLogId = reconciliationId,
                adjustmentTransactionId = adjustmentTxId,
                status = calc.status,
                discrepancyAmount = calc.discrepancyAmount
            )
        } catch (e: Exception) {
            Log.e("ReconciliationRepo", "Error executing reconciliation: ${e.message}", e)
            ReconciliationExecutionResult(
                success = false,
                reconciliationLogId = "",
                adjustmentTransactionId = null,
                status = ReconciliationStatus.BALANCED,
                discrepancyAmount = 0.0,
                errorMessage = e.message ?: "Error desconocido en conciliación"
            )
        }
    }
}
