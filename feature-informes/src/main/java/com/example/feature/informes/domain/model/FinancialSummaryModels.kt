package com.example.feature.informes.domain.model

data class FinancialEvolutionPoint(
    val periodLabel: String,
    val income: Double,
    val expenses: Double,
    val result: Double
)

data class DistributionCategoryItem(
    val categoryId: String,
    val categoryName: String,
    val amount: Double,
    val percentage: Double,
    val colorHex: String,
    val transactionCount: Int,
    val type: String = "GENERAL"
)

data class SummaryKpiItem(
    val title: String,
    val mainAmount: Double,
    val variationPct: Double,
    val countSubtitle: String,
    val trendUpIsGood: Boolean = true
)

data class FinancialSummaryData(
    val netWorth: Double = 0.0,
    val netWorthVariationPct: Double = 0.0,
    val netWorthTrend: String = "+8.4% vs. período anterior",
    val totalAssets: Double = 0.0,
    val assetsVariationPct: Double = 0.0,
    val assetCount: Int = 0,
    val totalLiabilities: Double = 0.0,
    val liabilitiesVariationPct: Double = 0.0,
    val pendingDebtsCount: Int = 0,
    val totalIncome: Double = 0.0,
    val incomeVariationPct: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val expenseVariationPct: Double = 0.0,
    val periodResult: Double = 0.0,
    val cashFlowInflows: Double = 0.0,
    val cashFlowOutflows: Double = 0.0,
    val netCashFlow: Double = 0.0,
    val evolutionChartPoints: List<FinancialEvolutionPoint> = emptyList(),
    val assetDistributions: List<DistributionCategoryItem> = emptyList(),
    val liabilityDistributions: List<DistributionCategoryItem> = emptyList(),
    val incomeDistributions: List<DistributionCategoryItem> = emptyList(),
    val expenseDistributions: List<DistributionCategoryItem> = emptyList()
)
