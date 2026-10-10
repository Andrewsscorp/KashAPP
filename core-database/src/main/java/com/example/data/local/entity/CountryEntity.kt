package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing Countries with financial clearing parameters.
 */
@Entity(tableName = "countries")
data class CountryEntity(
    @PrimaryKey
    val id: String,
    val userId: String? = null,
    val isoCode: String,
    val name: String,
    val flagEmoji: String = "🌐",
    val defaultCurrencyCode: String = "COP",
    val clearingSystemName: String = "ACH",
    val isNumericClearingCode: Boolean = true
)
