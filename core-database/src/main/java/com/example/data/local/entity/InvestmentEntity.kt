package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a Financial Investment in KashApp (Acciones, Criptomonedas, Bienes Raíces, Fondos, Renta Fija, Préstamos, etc.).
 * 
 * Implements double-entry accounting linkage to origin accounts, yield tracking, mark-to-market valuations,
 * and automatic net worth consolidation.
 */
@Entity(
    tableName = "investments",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["originAccountId"]),
        Index(value = ["status"]),
        Index(value = ["isFavorite"])
    ]
)
data class InvestmentEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val typeId: String, // "ACCIONES", "CRIPTO", "BIENES_RAICES", "FONDOS_ETF", "RENTA_FIJA", "PRESTAMO", "NEGOCIO", "COMMODITIES", "OTRO"
    val typeName: String,
    val institutionId: String? = null,
    val institutionName: String = "Bancolombia",
    val currencyCode: String = "COP",
    val acquisitionDate: String, // e.g. "2026-10-01"
    val status: String = "ACTIVE", // "ACTIVE", "CLOSED", "PAUSED"
    val initialAmount: Double,
    val currentValue: Double,
    val contributions: Double = 0.0,
    val withdrawals: Double = 0.0,
    val generatesYield: Boolean = false,
    val yieldRate: Double? = null,
    val rateType: String? = null, // "EA", "NOMINAL", "MONTHLY", "FIXED", "VARIABLE"
    val yieldPeriodicity: String? = null, // "MONTHLY", "QUARTERLY", "SEMIANNUAL", "ANNUALLY", "MATURITY"
    val valueUpdateDate: String? = null,
    val originAccountId: String,
    val originAccountName: String,
    val yieldAccountId: String? = null,
    val yieldAccountName: String? = null,
    val includeInNetWorth: Boolean = true,
    val netWorthValue: Double = 0.0,
    val iconName: String = "trending_up",
    val colorHex: String = "#0284C7",
    val isFavorite: Boolean = false,
    val tags: String = "",
    val notes: String? = null,
    val documentUri: String? = null,
    val documentCategory: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
