package com.example.domain.model.fixedassets

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

data class DepreciationScheduleEntry(
    val monthIndex: Int,
    val periodLabel: String,
    val monthlyDepreciation: Double,
    val accumulatedDepreciation: Double,
    val remainingBookValue: Double
)

object AssetFinancialCalculations {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Calculates the monthly depreciation amount based on method, acquisition value, residual value, and useful life.
     */
    fun calculateMonthlyDepreciation(
        acquisitionValue: Double,
        residualValue: Double,
        usefulLifeMonths: Int,
        method: DepreciationMethod = DepreciationMethod.STRAIGHT_LINE,
        currentMonthIndex: Int = 1
    ): Double {
        if (usefulLifeMonths <= 0) return 0.0
        val depreciableBase = max(0.0, acquisitionValue - residualValue)
        if (depreciableBase <= 0.0) return 0.0

        return when (method) {
            DepreciationMethod.STRAIGHT_LINE -> {
                depreciableBase / usefulLifeMonths.toDouble()
            }
            DepreciationMethod.DIGITS_SUM -> {
                // Sum of years' digits accelerated: sum = N*(N+1)/2
                val n = usefulLifeMonths.toDouble()
                val sumDigits = (n * (n + 1.0)) / 2.0
                val remainingMonths = max(1.0, (usefulLifeMonths - currentMonthIndex + 1).toDouble())
                (remainingMonths / sumDigits) * depreciableBase
            }
            DepreciationMethod.DECLINING_BALANCE -> {
                // Double declining balance monthly rate = (2 / usefulLifeMonths)
                val monthlyRate = 2.0 / usefulLifeMonths.toDouble()
                // Applied on declining base
                val currentBase = depreciableBase * Math.pow(1.0 - monthlyRate, max(0, currentMonthIndex - 1).toDouble())
                currentBase * monthlyRate
            }
        }
    }

    /**
     * Calculates the net book value (Valor Neto en Libros).
     * Cannot fall below the salvage/residual value.
     */
    fun calculateCurrentBookValue(
        acquisitionValue: Double,
        accumulatedDepreciation: Double,
        residualValue: Double = 0.0
    ): Double {
        val net = acquisitionValue - accumulatedDepreciation
        return max(residualValue, net)
    }

    /**
     * Calculates the number of months elapsed from a start date (YYYY-MM-DD) to an end date.
     */
    fun calculateMonthsElapsed(
        startDateStr: String,
        endDateStr: String = LocalDate.now().format(dateFormatter)
    ): Int {
        return try {
            val start = LocalDate.parse(startDateStr.take(10), dateFormatter)
            val end = LocalDate.parse(endDateStr.take(10), dateFormatter)
            val months = ChronoUnit.MONTHS.between(start, end).toInt()
            max(0, months)
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Calculates the remaining useful life in months.
     */
    fun calculateRemainingUsefulLifeMonths(
        acquisitionDateStr: String,
        usefulLifeMonths: Int,
        asOfDate: String = LocalDate.now().format(dateFormatter)
    ): Int {
        val elapsed = calculateMonthsElapsed(acquisitionDateStr, asOfDate)
        return max(0, usefulLifeMonths - elapsed)
    }

    /**
     * Calculates percentage of useful life consumed (0.0 to 1.0).
     */
    fun calculateLifeProgress(
        acquisitionDateStr: String,
        usefulLifeMonths: Int,
        asOfDate: String = LocalDate.now().format(dateFormatter)
    ): Float {
        if (usefulLifeMonths <= 0) return 1.0f
        val elapsed = calculateMonthsElapsed(acquisitionDateStr, asOfDate)
        return min(1.0f, elapsed.toFloat() / usefulLifeMonths.toFloat())
    }

    /**
     * Calculates percentage of financial depreciation relative to depreciable base.
     */
    fun calculateDepreciationProgress(
        acquisitionValue: Double,
        residualValue: Double,
        accumulatedDepreciation: Double
    ): Float {
        val base = max(1.0, acquisitionValue - residualValue)
        return (accumulatedDepreciation / base).toFloat().coerceIn(0.0f, 1.0f)
    }

    /**
     * Resolves the start date according to the "Convención de inicio" parameter:
     * - "Desde fecha de adquisición": exact purchase date.
     * - "Primer día del mes": forced to day 1 of purchase month.
     * - "Mes siguiente": advances to day 1 of the following month (month + 1, day 1).
     */
    fun resolveEffectiveStartDate(acquisitionDateStr: String, convention: String): LocalDate {
        val baseDate = try {
            LocalDate.parse(acquisitionDateStr.take(10), dateFormatter)
        } catch (_: Exception) {
            LocalDate.now()
        }
        return when {
            convention.contains("Mes siguiente", ignoreCase = true) || convention.contains("siguiente", ignoreCase = true) -> {
                baseDate.plusMonths(1).withDayOfMonth(1)
            }
            convention.contains("Primer día", ignoreCase = true) || convention.contains("primer", ignoreCase = true) -> {
                baseDate.withDayOfMonth(1)
            }
            else -> baseDate
        }
    }

    /**
     * Applies the rounding policy ("Redondeo"):
     * - "Al peso": Math.round() to nearest integer
     * - "A la centena": nearest 100
     * - "Al millar": nearest 1,000
     * - "Sin redondeo": floating point preserved
     */
    fun applyRounding(amount: Double, roundingMode: String): Double {
        return when {
            roundingMode.contains("centena", ignoreCase = true) -> {
                kotlin.math.round(amount / 100.0) * 100.0
            }
            roundingMode.contains("millar", ignoreCase = true) -> {
                kotlin.math.round(amount / 1000.0) * 1000.0
            }
            roundingMode.contains("peso", ignoreCase = true) -> {
                kotlin.math.round(amount)
            }
            else -> amount // "Sin redondeo"
        }
    }

    /**
     * Generates a monthly depreciation schedule projection for an asset up to its useful life.
     */
    fun generateSchedule(
        acquisitionValue: Double,
        residualValue: Double,
        usefulLifeMonths: Int,
        method: DepreciationMethod,
        startDateStr: String
    ): List<DepreciationScheduleEntry> {
        return generateScheduleAdvanced(
            acquisitionValue = acquisitionValue,
            residualValue = residualValue,
            usefulLifeMonths = usefulLifeMonths,
            method = method,
            startDateStr = startDateStr,
            startConvention = "Desde fecha de adquisición",
            frequency = "Mensual",
            rounding = "Sin redondeo",
            currentMarketValue = acquisitionValue,
            accumulatedDepreciation = 0.0
        )
    }

    /**
     * Advanced depreciation schedule projection generator taking all 5 calculation parameters into account:
     * - method: DepreciationMethod (STRAIGHT_LINE, DIGITS_SUM, DECLINING_BALANCE)
     * - depreciableBaseMode: "Valor de adquisición", "Valor neto ajustado", "Valor de reposición"
     * - startConvention: "Desde fecha de adquisición", "Primer día del mes", "Mes siguiente"
     * - frequency: "Mensual" (step=1), "Trimestral" (step=3), "Semestral" (step=6), "Anual" (step=12)
     * - rounding: "Al peso", "A la centena", "Al millar", "Sin redondeo"
     */
    fun generateScheduleAdvanced(
        acquisitionValue: Double,
        residualValue: Double,
        usefulLifeMonths: Int,
        method: DepreciationMethod,
        startDateStr: String,
        depreciableBaseMode: String = "Valor de adquisición",
        startConvention: String = "Desde fecha de adquisición",
        frequency: String = "Mensual",
        rounding: String = "Al peso",
        currentMarketValue: Double = acquisitionValue,
        accumulatedDepreciation: Double = 0.0
    ): List<DepreciationScheduleEntry> {
        val schedule = mutableListOf<DepreciationScheduleEntry>()
        val start = resolveEffectiveStartDate(startDateStr, startConvention)

        // Base de cálculo según selector
        val effectiveCost = when {
            depreciableBaseMode.contains("reposición", ignoreCase = true) || depreciableBaseMode.contains("reposicion", ignoreCase = true) -> {
                currentMarketValue
            }
            depreciableBaseMode.contains("neto", ignoreCase = true) || depreciableBaseMode.contains("ajustado", ignoreCase = true) -> {
                max(0.0, acquisitionValue - accumulatedDepreciation)
            }
            else -> acquisitionValue
        }

        val depreciableBase = max(0.0, effectiveCost - residualValue)
        if (depreciableBase <= 0.0 || usefulLifeMonths <= 0) return emptyList()

        val stepMonths = when {
            frequency.contains("anual", ignoreCase = true) -> 12
            frequency.contains("semestral", ignoreCase = true) -> 6
            frequency.contains("trimestral", ignoreCase = true) -> 3
            else -> 1 // Mensual
        }

        var currentAccum = 0.0
        var m = 1
        var periodCounter = 1

        while (m <= usefulLifeMonths && currentAccum < depreciableBase) {
            val date = start.plusMonths(m.toLong())

            // Sum depreciation over the step period
            var periodDepreciation = 0.0
            val endPeriodMonth = min(usefulLifeMonths, m + stepMonths - 1)
            for (subM in m..endPeriodMonth) {
                val monthlyAmt = calculateMonthlyDepreciation(effectiveCost, residualValue, usefulLifeMonths, method, subM)
                periodDepreciation += monthlyAmt
            }

            // Apply rounding to the period installment
            val roundedPeriodDep = applyRounding(periodDepreciation, rounding)
            val effectivePeriodDep = min(roundedPeriodDep, depreciableBase - currentAccum)
            currentAccum += effectivePeriodDep
            val bookVal = max(residualValue, effectiveCost - currentAccum)

            val periodLabel = if (stepMonths == 1) {
                date.format(DateTimeFormatter.ofPattern("MMM yyyy"))
            } else {
                "P$periodCounter (${date.format(DateTimeFormatter.ofPattern("MMM yyyy"))})"
            }

            schedule.add(
                DepreciationScheduleEntry(
                    monthIndex = m,
                    periodLabel = periodLabel,
                    monthlyDepreciation = effectivePeriodDep,
                    accumulatedDepreciation = currentAccum,
                    remainingBookValue = bookVal
                )
            )

            m += stepMonths
            periodCounter++
        }

        return schedule
    }

    /**
     * Calculates accumulated depreciation from an acquisition date in the past up to asOfDate.
     */
    fun calculateHistoricalDepreciation(
        acquisitionValue: Double,
        residualValue: Double,
        usefulLifeMonths: Int,
        method: DepreciationMethod,
        acquisitionDateStr: String,
        asOfDateStr: String = LocalDate.now().format(dateFormatter)
    ): Double {
        val monthsElapsed = calculateMonthsElapsed(acquisitionDateStr, asOfDateStr)
        if (monthsElapsed <= 0 || usefulLifeMonths <= 0) return 0.0
        val depreciableBase = max(0.0, acquisitionValue - residualValue)
        if (depreciableBase <= 0.0) return 0.0

        return when (method) {
            DepreciationMethod.STRAIGHT_LINE -> {
                val monthly = depreciableBase / usefulLifeMonths.toDouble()
                min(depreciableBase, monthly * monthsElapsed)
            }
            DepreciationMethod.DIGITS_SUM -> {
                val n = usefulLifeMonths.toDouble()
                val sumDigits = (n * (n + 1.0)) / 2.0
                var accum = 0.0
                for (m in 1..min(monthsElapsed, usefulLifeMonths)) {
                    val remainingMonths = max(1.0, (usefulLifeMonths - m + 1).toDouble())
                    accum += (remainingMonths / sumDigits) * depreciableBase
                }
                min(depreciableBase, accum)
            }
            DepreciationMethod.DECLINING_BALANCE -> {
                val monthlyRate = 2.0 / usefulLifeMonths.toDouble()
                var accum = 0.0
                for (m in 1..min(monthsElapsed, usefulLifeMonths)) {
                    val currentBase = depreciableBase * Math.pow(1.0 - monthlyRate, (m - 1).toDouble())
                    accum += currentBase * monthlyRate
                }
                min(depreciableBase, accum)
            }
        }
    }
}
