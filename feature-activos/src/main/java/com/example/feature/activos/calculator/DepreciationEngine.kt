package com.example.feature.activos.calculator

import com.example.feature.activos.model.DepreciationScheduleEntry
import com.example.feature.activos.model.DepreciationSummary
import kotlin.math.max

/**
 * High-performance, multi-method depreciation calculation engine.
 * Computes exact amortization curves according to NIC 16 / IFRS IAS 16 guidelines.
 */
object DepreciationEngine {

    fun calculateStraightLine(
        assetId: String,
        assetName: String,
        acquisitionValue: Double,
        salvageValue: Double,
        usefulLifeYears: Int,
        startYear: Int = 2026
    ): DepreciationSummary {
        val safeLifeYears = max(1, usefulLifeYears)
        val depreciableBase = max(0.0, acquisitionValue - salvageValue)
        val annualDepreciation = depreciableBase / safeLifeYears
        val monthlyDepreciation = annualDepreciation / 12.0
        val annualRate = (1.0 / safeLifeYears) * 100.0

        val schedule = mutableListOf<DepreciationScheduleEntry>()
        var currentBookValue = acquisitionValue
        var accumulatedDep = 0.0

        for (yearIndex in 1..safeLifeYears) {
            val depThisYear = if (yearIndex == safeLifeYears) {
                // Ensure final book value equals salvage value precisely
                max(0.0, currentBookValue - salvageValue)
            } else {
                annualDepreciation
            }
            val opening = currentBookValue
            accumulatedDep += depThisYear
            currentBookValue = max(salvageValue, currentBookValue - depThisYear)

            schedule.add(
                DepreciationScheduleEntry(
                    period = yearIndex,
                    year = startYear + yearIndex - 1,
                    openingBookValue = opening,
                    depreciationAmount = depThisYear,
                    accumulatedDepreciation = accumulatedDep,
                    closingBookValue = currentBookValue
                )
            )
        }

        return DepreciationSummary(
            assetId = assetId,
            assetName = assetName,
            acquisitionValue = acquisitionValue,
            salvageValue = salvageValue,
            usefulLifeYears = safeLifeYears,
            depreciableBase = depreciableBase,
            annualRatePercentage = annualRate,
            monthlyDepreciation = monthlyDepreciation,
            schedule = schedule
        )
    }

    fun calculateDecliningBalance(
        assetId: String,
        assetName: String,
        acquisitionValue: Double,
        salvageValue: Double,
        usefulLifeYears: Int,
        factor: Double = 2.0,
        startYear: Int = 2026
    ): DepreciationSummary {
        val safeLifeYears = max(1, usefulLifeYears)
        val depreciableBase = max(0.0, acquisitionValue - salvageValue)
        val straightLineRate = 1.0 / safeLifeYears
        val acceleratedRate = (straightLineRate * factor).coerceAtMost(1.0)

        val schedule = mutableListOf<DepreciationScheduleEntry>()
        var currentBookValue = acquisitionValue
        var accumulatedDep = 0.0

        for (yearIndex in 1..safeLifeYears) {
            val opening = currentBookValue
            var depThisYear = opening * acceleratedRate

            if (opening - depThisYear < salvageValue || yearIndex == safeLifeYears) {
                depThisYear = max(0.0, opening - salvageValue)
            }

            accumulatedDep += depThisYear
            currentBookValue = max(salvageValue, opening - depThisYear)

            schedule.add(
                DepreciationScheduleEntry(
                    period = yearIndex,
                    year = startYear + yearIndex - 1,
                    openingBookValue = opening,
                    depreciationAmount = depThisYear,
                    accumulatedDepreciation = accumulatedDep,
                    closingBookValue = currentBookValue
                )
            )

            if (currentBookValue <= salvageValue) break
        }

        return DepreciationSummary(
            assetId = assetId,
            assetName = assetName,
            acquisitionValue = acquisitionValue,
            salvageValue = salvageValue,
            usefulLifeYears = safeLifeYears,
            depreciableBase = depreciableBase,
            annualRatePercentage = acceleratedRate * 100.0,
            monthlyDepreciation = (schedule.firstOrNull()?.depreciationAmount ?: 0.0) / 12.0,
            schedule = schedule
        )
    }
}
