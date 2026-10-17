package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing Currencies (ISO-4217 and user-defined currencies).
 */
@Entity(tableName = "currencies")
data class CurrencyEntity(
    @PrimaryKey
    val id: String,
    val userId: String? = null,
    val code: String,
    val name: String,
    val symbol: String = "$",
    val manualExchangeRate: Double = 1.0,
    val isActive: Boolean = true,
    val isSystem: Boolean = false
)
