package com.example.feature.reconciliation.model

/**
 * Parameters submitted to execute an account balance reconciliation.
 */
data class ReconciliationParams(
    val userId: String,
    val accountId: String,
    val accountName: String,
    val previousBalance: Double,
    val reconciledRealBalance: Double,
    val reconciliationDate: String, // "YYYY-MM-DD"
    val notes: String? = null,
    val reconciledByUserId: String,
    val reconciledByUserName: String,
    val deviceModel: String
)

/**
 * Result returned after atomic execution of an account balance reconciliation.
 */
data class ReconciliationExecutionResult(
    val success: Boolean,
    val reconciliationLogId: String,
    val adjustmentTransactionId: String? = null,
    val status: ReconciliationStatus,
    val discrepancyAmount: Double,
    val errorMessage: String? = null
)
