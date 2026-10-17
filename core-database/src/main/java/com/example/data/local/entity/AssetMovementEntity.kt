package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a financial or maintenance movement of an asset
 * (Adquisición, Depreciación, Amortización, Mejora, Revaluación, Venta, Baja).
 */
@Entity(
    tableName = "asset_movements",
    indices = [
        Index("assetId"),
        Index("userId"),
        Index("type"),
        Index("date")
    ]
)
data class AssetMovementEntity(
    @PrimaryKey
    val id: String,
    val assetId: String,
    val userId: String = "user_default",
    val type: String, // "ACQUISITION", "DEPRECIATION", "IMPROVEMENT", "AMORTIZATION", "REVALUATION", "SALE", "DISPOSAL", "MAINTENANCE", "IMPAIRMENT"
    val title: String,
    val date: String, // "YYYY-MM-DD"
    val amount: Double,
    val notes: String? = null,
    val usefulLifeIncrementMonths: Int = 0,
    val newResidualValue: Double? = null,
    val costCenter: String? = null,
    val accountingAccount: String? = null,
    val providerOrVendor: String? = null,
    val invoiceOrDocNumber: String? = null,
    val documentUri: String? = null,
    val disposalReason: String? = null,
    val impairmentReason: String? = null,
    val financialImpact: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
