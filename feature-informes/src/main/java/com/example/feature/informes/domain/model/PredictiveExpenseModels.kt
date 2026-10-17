package com.example.feature.informes.domain.model

data class ProjectedExpenseCategory(
    val categoryName: String,
    val monthlyEstimatedAmount: Double,
    val percentageOfTotal: Double,
    val predictabilityType: PredictabilityLevel,
    val frequencyDescription: String,
    val historicalAverage3Months: Double,
    val projectedTrendPct: Double,
    val nextDueDateEstimated: String,
    val isEssential: Boolean
)

enum class PredictabilityLevel(val label: String, val certaintyPct: Int, val colorHex: String) {
    CONTRACTUAL_FIXED("Fijo Contractual (Ineludible)", 100, "#3B82F6"),
    OPERATIONAL_ESSENTIAL("Operativo Esencial", 85, "#10B981"),
    DISCRETIONARY("Discrecional / Variable", 55, "#F59E0B"),
    CONTINGENCY("Contingencia Recomendada", 30, "#EF4444")
}

data class FinancialRunwayProtection(
    val availableLiquidCash: Double,
    val monthlyBurnRate: Double,
    val runwayMonths: Double,
    val runwayDays: Int,
    val recommendedEmergencyFundTarget: Double,
    val currentProtectionCoveragePct: Double,
    val statusLabel: String,
    val statusColorHex: String,
    val shockAbsorptionDescription: String
)

data class PredictiveScenario(
    val scenarioName: String,
    val description: String,
    val horizonMonths: Int,
    val projectedIncome: Double,
    val projectedExpenses: Double,
    val projectedNetSavings: Double,
    val projectedFinalNetWorth: Double,
    val probabilityPct: Int,
    val colorHex: String
)

data class AdvancedProjectionsBundle(
    val runwayProtection: FinancialRunwayProtection,
    val dailyExpenseBurnRate: Double,
    val monthlyTotalProjectedExpense: Double,
    val monthlyTotalProjectedIncome: Double,
    val monthlyProjectedSurplus: Double,
    val projectedCategories: List<ProjectedExpenseCategory>,
    val multiHorizonScenarios: List<PredictiveScenario>,
    val upcoming30DaysForecast: List<DailyCashFlowForecastPoint>,
    val strategicRecommendations: List<String>
)

data class DailyCashFlowForecastPoint(
    val dayLabel: String,
    val dateString: String,
    val expectedInflow: Double,
    val expectedOutflow: Double,
    val projectedEndBalance: Double
)
