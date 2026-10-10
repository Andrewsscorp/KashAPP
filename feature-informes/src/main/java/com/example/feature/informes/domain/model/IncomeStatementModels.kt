package com.example.feature.informes.domain.model

data class GroupedFinancialAmount(
    val id: String,
    val name: String,
    val amount: Double,
    val transactionCount: Int,
    val percentageOfTotal: Double = 0.0,
    val variationPct: Double? = null,
    val iconName: String? = null
)

data class IncomeStatementData(
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val grossResult: Double = 0.0,
    val operatingExpenses: Double = 0.0,
    val netResult: Double = 0.0,
    val savingsMarginPct: Double = 0.0,
    val variationVsPreviousPeriodPct: Double = 0.0,

    val incomeByCategory: List<GroupedFinancialAmount> = emptyList(),
    val incomeBySource: List<GroupedFinancialAmount> = emptyList(),
    val incomeByAccount: List<GroupedFinancialAmount> = emptyList(),

    val expensesByCategory: List<GroupedFinancialAmount> = emptyList(),
    val expensesByAccount: List<GroupedFinancialAmount> = emptyList(),
    val expensesByMerchant: List<GroupedFinancialAmount> = emptyList(),
    val recurringExpensesTotal: Double = 0.0,
    val variableExpensesTotal: Double = 0.0
)
