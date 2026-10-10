package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing Master Data for financial institutions (Banks, Digital Wallets, Cooperatives).
 * Enforces parametric routing (Clearing code / ACH, Country, Currency, FX, Visual branding).
 */
@Entity(tableName = "banks")
data class BankEntity(
    @PrimaryKey
    val id: String, // UUID
    val userId: String? = null, // null = Global system bank provided by KashApp; non-null = User-created bank
    val name: String, // VARCHAR(100) (e.g. "Cooperativa de Tunja")
    val clearingCode: String, // VARCHAR(20) (e.g. "001", "007", "051" - vital for ACH clearing)
    val countryCode: String = "CO", // ISO-3166 (e.g. "CO", "US", "ES", "MX")
    val countryName: String = "Colombia",
    val currencyCode: String = "COP", // ISO-4217 (e.g. "COP", "USD", "EUR")
    val logoUrl: String? = null, // URI or local asset path
    val colorHex: String = "#0284C7", // Visual accent for Mental Accounting
    val iconName: String = "account_balance", // Icon identifier
    val cityName: String? = null, // Associated city (e.g. "Bogotá", "Tunja", "Miami")
    val additionalInfo: String? = null, // Sanitized additional notes (max 200 chars)
    val isGlobal: Boolean = false, // true = Seeded system bank (immutable)
    val orderIndex: Int = 0 // Position in list
)
