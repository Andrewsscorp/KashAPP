package com.example.feature.informes.domain.model

enum class RatioHealthStatus(val label: String, val colorHex: String) {
    EXCELLENT("Óptimo", "#10B981"),
    HEALTHY("Saludable", "#34D399"),
    WARNING("Atención Requerida", "#F59E0B"),
    CRITICAL("Alerta Crítica", "#EF4444")
}

data class FinancialRatioItem(
    val id: String,
    val name: String,
    val category: String,
    val rawValue: Double,
    val formattedValue: String,
    val benchmark: String,
    val status: RatioHealthStatus,
    val formula: String,
    val interpretation: String,
    val recommendation: String
)

enum class AlertSeverityLevel {
    INFO,
    WARNING,
    CRITICAL
}

data class SmartFinancialAlert(
    val id: String,
    val title: String,
    val description: String,
    val severity: AlertSeverityLevel,
    val actionableRecommendation: String,
    val indicatorImpact: String
)

data class FutureProjectionPoint(
    val horizonMonths: Int,
    val horizonLabel: String,
    val projectedNetWorth: Double,
    val projectedIncome: Double,
    val projectedExpenses: Double,
    val projectedSavings: Double,
    val confidencePct: Int,
    val isPositiveTrend: Boolean,
    val narrativeSummary: String
)

data class FinancialAnalysisData(
    val overallHealthScore: Int = 85,
    val overallHealthLabel: String = "Solvente & En Crecimiento",
    val ratios: List<FinancialRatioItem> = emptyList(),
    val smartAlerts: List<SmartFinancialAlert> = emptyList(),
    val projections: List<FutureProjectionPoint> = emptyList(),
    val liquidityRatioValue: Double = 2.45,
    val acidTestValue: Double = 2.10,
    val debtLevelPct: Double = 28.5,
    val netMarginPct: Double = 34.2,
    val roaPct: Double = 7.8,
    val roePct: Double = 11.2,
    val savingsRatePct: Double = 32.0
)
