package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Custom user-defined exchange rate for a currency pair with priority over automated rates,
 * optional validity date window, and on/off activation toggle.
 */
@Entity(
    tableName = "manual_exchange_rates",
    indices = [
        Index(value = ["fromCurrency", "toCurrency"], unique = true),
        Index(value = ["isActive"])
    ]
)
data class ManualExchangeRateEntity(
    @PrimaryKey
    val id: String, // Format: "${fromCurrency}_${toCurrency}" e.g., "USD_COP"
    val fromCurrency: String,
    val toCurrency: String,
    val rate: Double,
    val isActive: Boolean = true,
    val validFromDate: String, // e.g. "2026-10-01"
    val validToDate: String, // e.g. "2026-10-31"
    val note: String = "Ajuste manual · Usuario",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
