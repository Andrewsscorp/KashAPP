package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["userId", "createdAtEpochMs"]),
        Index(value = ["userId", "isRead", "isArchived"]),
        Index(value = ["userId", "category"]),
        Index(value = ["userId", "dedupKey"], unique = true)
    ]
)
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val dedupKey: String,
    val title: String,
    val body: String,
    val category: String,
    val severity: String,
    val sourceOrigin: String,
    val deepLinkRoute: String,
    val actionLabel: String? = null,
    val actionPayloadJson: String? = null,
    val isRead: Boolean = false,
    val isArchived: Boolean = false,
    val snoozedUntilEpochMs: Long? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val readAtEpochMs: Long? = null,
    val syncedToFirestore: Boolean = false
)
