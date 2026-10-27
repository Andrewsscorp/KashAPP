package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Immutable audit trail entity for account balance reconciliations (NIIF / GAAP / SOX).
 * Records historical checkpoints between internal system balances and physical bank/cash statements.
 */
@Entity(
    tableName = "account_reconciliation_logs",
    indices = [
        Index(value = ["userId", "accountId"]),
        Index(value = ["reconciliationTimestamp"])
    ]
)
data class AccountReconciliationLogEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val accountId: String,
    val accountName: String,
    val previousSystemBalance: Double,
    val reconciledRealBalance: Double,
    val discrepancyAmount: Double,
    val adjustmentTransactionId: String? = null,
    val reconciliationDate: String, // "YYYY-MM-DD"
    val reconciliationTime: String, // "HH:mm:ss"
    val reconciledByUserId: String,
    val reconciledByUserName: String,
    val notes: String? = null,
    val status: String, // "BALANCED", "ADJUSTED_SURPLUS", "ADJUSTED_DEFICIT"
    val deviceModel: String,
    val syncStatus: String = "PENDING",
    val reconciliationTimestamp: Long = System.currentTimeMillis()
)
