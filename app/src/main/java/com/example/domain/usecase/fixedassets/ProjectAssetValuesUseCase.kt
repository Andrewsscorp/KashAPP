package com.example.domain.usecase.fixedassets

import com.example.data.local.entity.FixedAssetEntity
import com.example.domain.model.fixedassets.AssetCategory
import com.example.domain.model.fixedassets.AssetFinancialCalculations
import com.example.domain.model.fixedassets.AssetProjectionPoint
import com.example.domain.model.fixedassets.ConsolidatedAssetProjection
import com.example.domain.model.fixedassets.DepreciationMethod
import com.example.domain.model.fixedassets.ObsolescenceMilestone
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class ProjectAssetValuesUseCase {

    private val currentYear = LocalDate.now().year
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Calculates future projections over a horizon of 1, 3, 5, or 10 years for a collection of assets.
     */
    fun execute(assets: List<FixedAssetEntity>, horizonYears: Int): ConsolidatedAssetProjection {
        val activeAssets = assets.filter { it.status == "ACTIVE" }
        val points = mutableListOf<AssetProjectionPoint>()

        val currentTotalAcquisition = activeAssets.sumOf { it.acquisitionValue }
        val currentTotalAccumDep = activeAssets.sumOf { it.accumulatedDepreciation }
        val currentTotalBook = activeAssets.sumOf {
            AssetFinancialCalculations.calculateCurrentBookValue(it.acquisitionValue, it.accumulatedDepreciation, it.residualValue)
        }
        val currentTotalMarket = activeAssets.sumOf { it.currentMarketValue }

        // Point 0 (Today)
        points.add(
            AssetProjectionPoint(
                yearLabel = "Hoy ($currentYear)",
                yearOffset = 0,
                totalAcquisitionCost = currentTotalAcquisition,
                projectedBookValue = currentTotalBook,
                projectedAccumulatedDepreciation = currentTotalAccumDep,
                projectedMarketValue = currentTotalMarket,
                depreciationInYear = 0.0
            )
        )

        var previousAccumDep = currentTotalAccumDep

        for (offset in 1..horizonYears) {
            val targetYear = currentYear + offset
            val monthsFromNow = offset * 12

            var yearBookSum = 0.0
            var yearAccumDepSum = 0.0
            var yearMarketSum = 0.0

            for (asset in activeAssets) {
                val category = AssetCategory.fromString(asset.category)
                val method = DepreciationMethod.fromString(asset.depreciationMethod)

                // Additional months of depreciation
                val additionalMonths = monthsFromNow
                val depreciableBase = max(0.0, asset.acquisitionValue - asset.residualValue)

                val projectedAssetAccumDep: Double = if (asset.hasDepreciation && asset.usefulLifeMonths > 0) {
                    val monthlyRate = depreciableBase / asset.usefulLifeMonths.toDouble()
                    val totalDep = asset.accumulatedDepreciation + (monthlyRate * additionalMonths)
                    min(depreciableBase, totalDep)
                } else {
                    asset.accumulatedDepreciation
                }

                val projectedBook = AssetFinancialCalculations.calculateCurrentBookValue(
                    asset.acquisitionValue,
                    projectedAssetAccumDep,
                    asset.residualValue
                )

                // Market value projection:
                // Real Estate appreciates at ~4.5% p.a.
                // Vehicles depreciate in market at ~8% p.a. down to 15% residual.
                // Technology depreciates at ~20% p.a. down to 5% residual.
                val projectedMarket: Double = when {
                    category.isAppreciable -> {
                        asset.currentMarketValue * (1.0 + 0.045).pow(offset.toDouble())
                    }
                    category == AssetCategory.VEHICLES -> {
                        max(asset.acquisitionValue * 0.15, asset.currentMarketValue * (1.0 - 0.08).pow(offset.toDouble()))
                    }
                    category == AssetCategory.TECHNOLOGY -> {
                        max(asset.acquisitionValue * 0.05, asset.currentMarketValue * (1.0 - 0.20).pow(offset.toDouble()))
                    }
                    else -> {
                        max(asset.residualValue, asset.currentMarketValue * (1.0 - 0.05).pow(offset.toDouble()))
                    }
                }

                yearBookSum += projectedBook
                yearAccumDepSum += projectedAssetAccumDep
                yearMarketSum += projectedMarket
            }

            val depInYear = max(0.0, yearAccumDepSum - previousAccumDep)
            previousAccumDep = yearAccumDepSum

            points.add(
                AssetProjectionPoint(
                    yearLabel = "$targetYear (+${offset}a)",
                    yearOffset = offset,
                    totalAcquisitionCost = currentTotalAcquisition,
                    projectedBookValue = yearBookSum,
                    projectedAccumulatedDepreciation = yearAccumDepSum,
                    projectedMarketValue = yearMarketSum,
                    depreciationInYear = depInYear
                )
            )
        }

        // Obsolescence / Replacement milestones
        val milestones = activeAssets.mapNotNull { asset ->
            val category = AssetCategory.fromString(asset.category)
            if (category == AssetCategory.REAL_ESTATE) return@mapNotNull null

            val elapsedMonths = AssetFinancialCalculations.calculateMonthsElapsed(asset.acquisitionDate)
            val remainingMonths = max(0, asset.usefulLifeMonths - elapsedMonths)
            val remainingYears = (remainingMonths / 12.0)
            val estYear = currentYear + (remainingYears.toInt())

            val inflationAdj = (1.0 + 0.04).pow(remainingYears)
            val estimatedCost = asset.acquisitionValue * inflationAdj

            val urgency = when {
                remainingMonths <= 6 -> "CRÍTICO"
                remainingMonths <= 18 -> "PRÓXIMO A VENCER"
                else -> "ÓPTIMO"
            }

            ObsolescenceMilestone(
                assetId = asset.id,
                assetName = asset.name,
                category = category,
                currentAgeMonths = elapsedMonths,
                totalUsefulLifeMonths = asset.usefulLifeMonths,
                remainingLifeMonths = remainingMonths,
                estimatedReplacementYear = estYear,
                estimatedReplacementCost = estimatedCost,
                urgencyStatus = urgency
            )
        }.sortedBy { it.remainingLifeMonths }

        val endPoint = points.last()

        return ConsolidatedAssetProjection(
            horizonYears = horizonYears,
            points = points,
            currentTotalBookValue = currentTotalBook,
            projectedEndBookValue = endPoint.projectedBookValue,
            projectedEndMarketValue = endPoint.projectedMarketValue,
            totalProjectedDepreciation = endPoint.projectedAccumulatedDepreciation - currentTotalAccumDep,
            netCapitalAppreciationOrLoss = endPoint.projectedMarketValue - currentTotalAcquisition,
            milestones = milestones
        )
    }
}
