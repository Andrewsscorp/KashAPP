package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_auto_captures",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "status"]),
        Index(value = ["idempotencyKey"], unique = true)
    ]
)
data class PendingAutoCaptureEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val sourcePackage: String,
    val detectedBankName: String,
    val transactionType: String,
    val amount: Double,
    val currency: String = "COP",
    val merchantOrCounterparty: String,
    val lastFourDigits: String? = null,
    val matchedAccountId: String? = null,
    val matchedAccountName: String? = null,
    val matchedCategoryId: String? = null,
    val matchedCategoryName: String? = null,
    val confidenceScore: Int = 90,
    val status: String = "PENDING",
    val linkedTransactionId: String? = null,
    val idempotencyKey: String,
    val notificationTimestamp: Long,
    val createdAt: Long = System.currentTimeMillis()
)
