package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a physical or intangible fixed asset (Activo Fijo / Intangible).
 */
@Entity(
    tableName = "fixed_assets",
    indices = [
        Index("userId"),
        Index("type"),
        Index("category")
    ]
)
data class FixedAssetEntity(
    @PrimaryKey
    val id: String,
    val userId: String = "user_default",
    val name: String,
    val type: String, // "TANGIBLE" or "INTANGIBLE"
    val category: String, // "Vehículos", "Tecnología", "Inmuebles", "Maquinaria", "Software", "Licencias", etc.
    val description: String? = null,
    val acquisitionDate: String, // "YYYY-MM-DD"
    val acquisitionValue: Double,
    val currency: String = "COP",
    val hasDepreciation: Boolean = true,
    val depreciationMethod: String = "STRAIGHT_LINE", // "STRAIGHT_LINE", "DIGITS_SUM", "DECLINING_BALANCE"
    val usefulLifeMonths: Int = 60,
    val residualValue: Double = 0.0,
    val currentMarketValue: Double = acquisitionValue,
    val accumulatedDepreciation: Double = 0.0,
    val lastDepreciationDate: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val licensePlate: String? = null,
    val supplier: String? = null,
    val location: String? = null,
    val warrantyExpiryDate: String? = null,
    val notes: String? = null,
    val status: String = "ACTIVE", // "ACTIVE", "SOLD", "DISPOSED", "WRITTEN_OFF"
    val imageUrl: String? = null,
    val colorHex: String = "#0284C7",
    val includeInNetWorth: Boolean = true,
    val depreciableBase: String = "Valor de adquisición",
    val startConvention: String = "Desde fecha de adquisición",
    val frequency: String = "Mensual",
    val rounding: String = "Al peso",
    val impairmentTracking: Boolean = true,
    val revaluationEnabled: Boolean = false,
    val allowLifeChange: Boolean = false,
    val allowResidualChange: Boolean = false,
    val allowMethodChange: Boolean = false,
    val deletedAt: Long? = null,
    val stateNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val assetCode: String
        get() = when {
            id.startsWith("AF-") -> id
            id.contains("af_") -> "AF-" + id.substringAfter("af_").padStart(5, '0')
            else -> "AF-" + (kotlin.math.abs(id.hashCode()) % 100000).toString().padStart(5, '0')
        }
}
