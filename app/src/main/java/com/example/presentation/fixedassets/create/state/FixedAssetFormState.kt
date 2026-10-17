package com.example.presentation.fixedassets.create.state

import com.example.data.local.entity.FixedAssetEntity
import com.example.domain.model.fixedassets.AssetCategory
import com.example.domain.model.fixedassets.AssetFinancialCalculations
import com.example.domain.model.fixedassets.AssetType
import com.example.domain.model.fixedassets.DepreciationMethod
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

data class FixedAssetFormState(
    val assetId: String? = null,
    val name: String = "",
    val type: AssetType = AssetType.TANGIBLE,
    val category: AssetCategory = AssetCategory.VEHICLES,
    val categoryName: String = "Vehículos y Transporte",
    val categoryCode: String = "VEH",
    val categoryColorHex: String = "#0284C7",
    val categoryIconName: String = "directions_car",
    val imageUrl: String? = null,
    val description: String = "",
    val acquisitionDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
    val acquisitionValueText: String = "",
    val currency: String = "COP",
    val hasDepreciation: Boolean = true,
    val depreciationMethod: DepreciationMethod = DepreciationMethod.STRAIGHT_LINE,
    val usefulLifeMonths: Int = 60,
    val residualValueText: String = "",
    val brand: String = "",
    val model: String = "",
    val serialOrPlate: String = "",
    val location: String = "",
    val supplier: String = "",
    val notes: String = "",
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
    val noAffectInventory: Boolean = false,
    val status: String = "ACTIVE",
    val stateNotes: String? = null,
    val accumulatedDepreciation: Double = 0.0,
    val lastDepreciationDate: String? = null,
    val currentMarketValue: Double? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val acquisitionValue: Double
        get() = acquisitionValueText.toDoubleOrNull() ?: 0.0

    val residualValue: Double
        get() = residualValueText.toDoubleOrNull() ?: (acquisitionValue * category.defaultResidualPercent)

    val monthlyDepreciationPreview: Double
        get() = if (hasDepreciation) {
            AssetFinancialCalculations.calculateMonthlyDepreciation(
                acquisitionValue = acquisitionValue,
                residualValue = residualValue,
                usefulLifeMonths = usefulLifeMonths,
                method = depreciationMethod
            )
        } else 0.0

    val isValid: Boolean
        get() = name.isNotBlank() && acquisitionValue > 0.0 && usefulLifeMonths > 0

    fun toEntity(userId: String = "user_default"): FixedAssetEntity {
        val id = assetId ?: "asset_${UUID.randomUUID()}"
        return FixedAssetEntity(
            id = id,
            userId = userId,
            name = name.trim(),
            type = type.name,
            category = categoryName.ifBlank { category.title },
            description = description.ifBlank { null },
            acquisitionDate = acquisitionDate.ifBlank { LocalDate.now().toString() },
            acquisitionValue = acquisitionValue,
            currency = currency,
            hasDepreciation = hasDepreciation,
            depreciationMethod = depreciationMethod.name,
            usefulLifeMonths = usefulLifeMonths,
            residualValue = residualValue,
            currentMarketValue = currentMarketValue ?: acquisitionValue,
            accumulatedDepreciation = accumulatedDepreciation,
            lastDepreciationDate = lastDepreciationDate,
            brand = brand.ifBlank { null },
            model = model.ifBlank { null },
            serialNumber = if (category != AssetCategory.VEHICLES) serialOrPlate.ifBlank { null } else null,
            licensePlate = if (category == AssetCategory.VEHICLES) serialOrPlate.ifBlank { null } else null,
            location = location.ifBlank { null },
            supplier = supplier.ifBlank { null },
            notes = notes.ifBlank { null },
            status = status,
            stateNotes = stateNotes,
            imageUrl = imageUrl?.ifBlank { null },
            colorHex = categoryColorHex.ifBlank { colorHex },
            includeInNetWorth = includeInNetWorth,
            depreciableBase = depreciableBase,
            startConvention = startConvention,
            frequency = frequency,
            rounding = rounding,
            impairmentTracking = impairmentTracking,
            revaluationEnabled = revaluationEnabled,
            allowLifeChange = allowLifeChange,
            allowResidualChange = allowResidualChange,
            allowMethodChange = allowMethodChange,
            noAffectInventory = noAffectInventory,
            updatedAt = System.currentTimeMillis()
        )
    }

    companion object {
        fun fromEntity(entity: FixedAssetEntity): FixedAssetFormState {
            val cat = AssetCategory.fromString(entity.category)
            val typ = try { AssetType.valueOf(entity.type) } catch (_: Exception) { AssetType.TANGIBLE }
            val meth = DepreciationMethod.fromString(entity.depreciationMethod)

            return FixedAssetFormState(
                assetId = entity.id,
                name = entity.name,
                type = typ,
                category = cat,
                categoryName = entity.category,
                categoryCode = cat.name.take(3),
                categoryColorHex = entity.colorHex,
                categoryIconName = "category",
                imageUrl = entity.imageUrl,
                description = entity.description ?: "",
                acquisitionDate = entity.acquisitionDate,
                acquisitionValueText = if (entity.acquisitionValue > 0) entity.acquisitionValue.toLong().toString() else "",
                currency = entity.currency,
                hasDepreciation = entity.hasDepreciation,
                depreciationMethod = meth,
                usefulLifeMonths = entity.usefulLifeMonths,
                residualValueText = if (entity.residualValue > 0) entity.residualValue.toLong().toString() else "",
                brand = entity.brand ?: "",
                model = entity.model ?: "",
                serialOrPlate = entity.licensePlate ?: entity.serialNumber ?: "",
                location = entity.location ?: "",
                supplier = entity.supplier ?: "",
                notes = entity.notes ?: "",
                colorHex = entity.colorHex,
                includeInNetWorth = entity.includeInNetWorth,
                depreciableBase = entity.depreciableBase,
                startConvention = entity.startConvention,
                frequency = entity.frequency,
                rounding = entity.rounding,
                impairmentTracking = entity.impairmentTracking,
                revaluationEnabled = entity.revaluationEnabled,
                allowLifeChange = entity.allowLifeChange,
                allowResidualChange = entity.allowResidualChange,
                allowMethodChange = entity.allowMethodChange,
                noAffectInventory = entity.noAffectInventory,
                status = entity.status,
                stateNotes = entity.stateNotes,
                accumulatedDepreciation = entity.accumulatedDepreciation,
                lastDepreciationDate = entity.lastDepreciationDate,
                currentMarketValue = entity.currentMarketValue
            )
        }
    }
}
