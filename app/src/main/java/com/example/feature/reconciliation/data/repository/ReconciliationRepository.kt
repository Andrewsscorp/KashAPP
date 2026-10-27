package com.example.feature.reconciliation.data.repository

import com.example.data.local.entity.AccountReconciliationLogEntity
import com.example.feature.reconciliation.model.ReconciliationExecutionResult
import com.example.feature.reconciliation.model.ReconciliationParams
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining account reconciliation operations and audit history retrieval.
 */
interface ReconciliationRepository {
    fun observeReconciliations(userId: String): Flow<List<AccountReconciliationLogEntity>>
    fun observeReconciliationsForAccount(userId: String, accountId: String): Flow<List<AccountReconciliationLogEntity>>
    suspend fun getReconciliationsForAccount(userId: String, accountId: String): List<AccountReconciliationLogEntity>
    suspend fun executeReconciliation(params: ReconciliationParams): ReconciliationExecutionResult
}
