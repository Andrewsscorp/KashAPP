package com.example.feature.reconciliation.domain

import com.example.feature.reconciliation.data.repository.ReconciliationRepository
import com.example.feature.reconciliation.model.ReconciliationExecutionResult
import com.example.feature.reconciliation.model.ReconciliationParams

/**
 * Executes an atomic balance reconciliation, persisting transactions, audit logs and enqueuing outbox events.
 */
class ExecuteReconciliationUseCase(
    private val repository: ReconciliationRepository
) {
    suspend operator fun invoke(params: ReconciliationParams): ReconciliationExecutionResult {
        return repository.executeReconciliation(params)
    }
}
