package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cached foreign exchange rates retrieved from the Frankfurter API and synced to Firebase.
 * Used for offline availability, instant local conversion, and reduced network overhead.
 */
@Entity(
    tableName = "exchange_rate_cache",
    indices = [
        Index(value = ["baseCurrency", "targetCurrency"], unique = true),
        Index(value = ["lastUpdatedMillis"])
    ]
)
data class ExchangeRateCacheEntity(
    @PrimaryKey
    val id: String, // Format: "${baseCurrency}_${targetCurrency}" e.g., "USD_COP"
    val baseCurrency: String,
    val targetCurrency: String,
    val rate: Double,
    val rateDate: String, // e.g. "2026-10-02"
    val provider: String = "Frankfurter",
    val lastUpdatedMillis: Long = System.currentTimeMillis(),
    val isStale: Boolean = false
)
