package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an immutable financial transaction with ACID consistency,
 * idempotent key to prevent network duplicates, and offline sync tracking.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["userId", "date"]),
        Index(value = ["accountId"]),
        Index(value = ["idempotencyKey"], unique = true)
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val accountId: String,
    val accountName: String,
    val categoryId: String,
    val categoryName: String,
    val sourceId: String? = null,
    val sourceName: String? = null,
    val amount: Double,
    val currency: String,
    val type: String, // "EXPENSE", "INCOME", "TRANSFER"
    val date: String,
    val description: String?,
    val receiptUrl: String?,
    val idempotencyKey: String,
    val isRecurring: Boolean = false,
    val recurrenceFrequency: String = "Mensual",
    val saveAsTemplate: Boolean = false,
    val tags: String = "",
    val transferGroupId: String? = null,
    val syncStatus: String = "PENDING", // "PENDING", "SYNCED", "FAILED"
    val appliedExchangeRate: Double? = null,
    val exchangeRateSource: String? = null, // "AUTOMATIC_FRANKFURTER", "MANUAL", "OFFICIAL_TRM"
    val createdAt: Long = System.currentTimeMillis()
)
