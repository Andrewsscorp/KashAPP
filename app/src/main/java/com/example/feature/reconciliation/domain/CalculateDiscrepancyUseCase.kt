package com.example.feature.reconciliation.domain

import com.example.feature.reconciliation.model.ReconciliationDiscrepancy

/**
 * Pure domain use case to calculate reconciliation discrepancy and status.
 */
class CalculateDiscrepancyUseCase {
    operator fun invoke(previousSystemBalance: Double, reconciledRealBalance: Double): ReconciliationDiscrepancy {
        return ReconciliationDiscrepancy.calculate(previousSystemBalance, reconciledRealBalance)
    }
}
