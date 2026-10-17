package com.example.feature.informes.domain.model

data class EquityChangeRow(
    val concept: String,
    val amount: Double,
    val isPositive: Boolean,
    val note: String? = null
)

data class EquityStatementData(
    val initialEquity: Double = 0.0,
    val periodResult: Double = 0.0,
    val capitalContributions: Double = 0.0,
    val withdrawals: Double = 0.0,
    val valuationGainsOrLosses: Double = 0.0,
    val assetRevaluations: Double = 0.0,
    val otherAdjustments: Double = 0.0,
    val finalEquity: Double = 0.0,
    val changeRows: List<EquityChangeRow> = emptyList(),
    val netEquityGrowthPct: Double = 0.0
)
