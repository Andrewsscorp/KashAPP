package com.example.feature.reconciliation.domain

import com.example.data.local.entity.AccountReconciliationLogEntity
import com.example.feature.reconciliation.data.repository.ReconciliationRepository
import kotlinx.coroutines.flow.Flow

/**
 * Retrieves the immutable audit log history for an account or across all user accounts.
 */
class GetReconciliationHistoryUseCase(
    private val repository: ReconciliationRepository
) {
    fun observeHistory(userId: String, accountId: String? = null): Flow<List<AccountReconciliationLogEntity>> {
        return if (accountId.isNullOrBlank()) {
            repository.observeReconciliations(userId)
        } else {
            repository.observeReconciliationsForAccount(userId, accountId)
        }
    }
}
