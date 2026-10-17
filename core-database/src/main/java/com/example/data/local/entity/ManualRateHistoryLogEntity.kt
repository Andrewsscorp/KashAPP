package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Historical audit log of every manual exchange rate creation, update, or status change.
 */
@Entity(
    tableName = "manual_rate_history_logs",
    indices = [
        Index(value = ["pairId"]),
        Index(value = ["timestamp"])
    ]
)
data class ManualRateHistoryLogEntity(
    @PrimaryKey
    val id: String,
    val pairId: String, // e.g. "USD_COP"
    val fromCurrency: String,
    val toCurrency: String,
    val previousRate: Double?,
    val newRate: Double,
    val action: String, // "CREATED", "UPDATED", "ACTIVATED", "DEACTIVATED", "RESET"
    val reason: String,
    val changedBy: String = "Usuario",
    val timestamp: Long = System.currentTimeMillis()
)
