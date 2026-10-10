package com.example.domain.usecase.fixedassets

import com.example.data.local.entity.FixedAssetEntity
import com.example.domain.model.fixedassets.AssetFinancialCalculations
import com.example.domain.model.fixedassets.DepreciationMethod
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

class CalculateAssetDepreciationUseCase {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    data class AssetPendingDepreciation(
        val asset: FixedAssetEntity,
        val monthsToDepreciate: Int,
        val monthlyAmount: Double,
        val totalPendingAmount: Double,
        val newAccumulatedDepreciation: Double,
        val newBookValue: Double
    )

    /**
     * Determines how much depreciation should be run for a specific asset up to [targetDateStr].
     */
    fun calculatePendingForAsset(
        asset: FixedAssetEntity,
        targetDateStr: String = LocalDate.now().format(dateFormatter)
    ): AssetPendingDepreciation? {
        val state = com.example.domain.model.fixedassets.statemachine.AssetStateMachine.resolveState(asset.status)
        if (!asset.hasDepreciation || !state.allowsDepreciation || asset.deletedAt != null || asset.usefulLifeMonths <= 0) {
            return null
        }

        val depreciableBase = max(0.0, asset.acquisitionValue - asset.residualValue)
        if (asset.accumulatedDepreciation >= depreciableBase) {
            return null // Fully depreciated
        }

        val startDateStr = asset.lastDepreciationDate ?: asset.acquisitionDate
        val monthsElapsed = AssetFinancialCalculations.calculateMonthsElapsed(startDateStr, targetDateStr)
        val monthsToRun = max(1, monthsElapsed)

        val method = DepreciationMethod.fromString(asset.depreciationMethod)
        val monthlyRate = AssetFinancialCalculations.calculateMonthlyDepreciation(
            acquisitionValue = asset.acquisitionValue,
            residualValue = asset.residualValue,
            usefulLifeMonths = asset.usefulLifeMonths,
            method = method
        )

        val remainingDepreciable = max(0.0, depreciableBase - asset.accumulatedDepreciation)
        val rawTotal = monthlyRate * monthsToRun
        val effectiveTotal = min(remainingDepreciable, rawTotal)

        if (effectiveTotal <= 0.0) return null

        val newAccum = asset.accumulatedDepreciation + effectiveTotal
        val newBook = AssetFinancialCalculations.calculateCurrentBookValue(
            acquisitionValue = asset.acquisitionValue,
            accumulatedDepreciation = newAccum,
            residualValue = asset.residualValue
        )

        return AssetPendingDepreciation(
            asset = asset,
            monthsToDepreciate = monthsToRun,
            monthlyAmount = monthlyRate,
            totalPendingAmount = effectiveTotal,
            newAccumulatedDepreciation = newAccum,
            newBookValue = newBook
        )
    }

    /**
     * Calculates pending depreciation across a list of assets.
     */
    fun calculateBatchPending(
        assets: List<FixedAssetEntity>,
        targetDateStr: String = LocalDate.now().format(dateFormatter)
    ): List<AssetPendingDepreciation> {
        return assets.mapNotNull { calculatePendingForAsset(it, targetDateStr) }
    }
}
