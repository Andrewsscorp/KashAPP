package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an event in the Transactional Outbox / Durable Operation Queue.
 * Guarantees zero data loss, offline survival, and idempotent background synchronization.
 */
@Entity(
    tableName = "outbox_operations",
    indices = [
        Index(value = ["status", "nextRetryAt"]),
        Index(value = ["idempotencyKey"], unique = true)
    ]
)
data class OutboxOperationEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val entityType: String, // "TRANSACTION", "CATEGORY", "ACCOUNT", "BUDGET", "GOAL", "MERCHANT"
    val entityId: String,
    val operation: String, // "CREATE", "UPDATE", "DELETE"
    val payloadJson: String,
    val idempotencyKey: String,
    val status: String = "PENDING", // "PENDING", "SYNCING", "DELIVERED", "FAILED"
    val retryCount: Int = 0,
    val nextRetryAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastError: String? = null
)
