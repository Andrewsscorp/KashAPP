package com.example.feature.informes.domain.model

data class FinancialLineItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val category: String,
    val amount: Double,
    val percentageOfTotal: Double = 0.0,
    val note: String? = null
)

data class BalanceSheetSection(
    val title: String,
    val totalAmount: Double,
    val items: List<FinancialLineItem>
)

data class BalanceSheetData(
    val currentAssets: List<FinancialLineItem> = emptyList(),
    val nonCurrentAssets: List<FinancialLineItem> = emptyList(),
    val totalCurrentAssets: Double = 0.0,
    val totalNonCurrentAssets: Double = 0.0,
    val totalAssets: Double = 0.0,

    val currentLiabilities: List<FinancialLineItem> = emptyList(),
    val nonCurrentLiabilities: List<FinancialLineItem> = emptyList(),
    val totalCurrentLiabilities: Double = 0.0,
    val totalNonCurrentLiabilities: Double = 0.0,
    val totalLiabilities: Double = 0.0,

    val initialEquity: Double = 0.0,
    val contributions: Double = 0.0,
    val retainedEarnings: Double = 0.0,
    val periodResult: Double = 0.0,
    val adjustmentsAndRevaluations: Double = 0.0,
    val totalEquity: Double = 0.0,

    val liabilitiesPlusEquity: Double = 0.0,
    val isBalanced: Boolean = true,
    val balanceDifference: Double = 0.0
)
