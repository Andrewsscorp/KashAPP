package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Groups double-entry transfer transactions (TRANSFER_OUT and TRANSFER_IN).
 */
@Entity(tableName = "transfer_groups")
data class TransferGroupEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val concept: String?,
    val sourceAccountId: String,
    val destinationAccountId: String,
    val amount: Double,
    val currency: String,
    val createdAt: Long = System.currentTimeMillis()
)
