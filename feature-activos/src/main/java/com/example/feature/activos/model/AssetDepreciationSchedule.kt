package com.example.feature.activos.model

/**
 * Projected annual/monthly depreciation period data point.
 */
data class DepreciationScheduleEntry(
    val period: Int,
    val year: Int,
    val openingBookValue: Double,
    val depreciationAmount: Double,
    val accumulatedDepreciation: Double,
    val closingBookValue: Double
)

data class DepreciationSummary(
    val assetId: String,
    val assetName: String,
    val acquisitionValue: Double,
    val salvageValue: Double,
    val usefulLifeYears: Int,
    val depreciableBase: Double,
    val annualRatePercentage: Double,
    val monthlyDepreciation: Double,
    val schedule: List<DepreciationScheduleEntry>
)
