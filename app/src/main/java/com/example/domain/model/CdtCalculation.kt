package com.example.domain.model

import java.time.LocalDate

/**
 * Data model for CDT yield and maturity calculations.
 */
data class CdtCalculation(
    val initialAmount: Double,
    val interestRateEa: Double,
    val termDays: Int,
    val issueDate: LocalDate,
    val maturityDate: LocalDate,
    val termRatePercentage: Double, // e.g. 5.0504%
    val estimatedYield: Double, // e.g. 505044.72
    val totalEstimatedValue: Double // initialAmount + estimatedYield
)
