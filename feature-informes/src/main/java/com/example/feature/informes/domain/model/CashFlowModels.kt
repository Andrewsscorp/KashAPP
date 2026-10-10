package com.example.feature.informes.domain.model

data class CashFlowEntryItem(
    val concept: String,
    val category: String,
    val amount: Double,
    val percentageOfTotal: Double = 0.0,
    val transactionCount: Int = 0
)

data class CashFlowStatementData(
    val initialBalance: Double = 0.0,
    val inflows: List<CashFlowEntryItem> = emptyList(),
    val totalInflows: Double = 0.0,
    val outflows: List<CashFlowEntryItem> = emptyList(),
    val totalOutflows: Double = 0.0,
    val netCashFlow: Double = 0.0,
    val finalBalance: Double = 0.0,
    val internalTransfersExcludedCount: Int = 0,
    val internalTransfersExcludedVolume: Double = 0.0
)
