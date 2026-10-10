package com.example.feature.informes.domain.model

enum class SpecializedReportType(val label: String, val iconName: String) {
    INCOME("Ingresos", "TrendingUp"),
    EXPENSES("Gastos", "TrendingDown"),
    ACCOUNTS("Cuentas", "AccountBalance"),
    CREDIT_CARDS("Tarjetas", "CreditCard"),
    DEBTS("Deudas", "MoneyOff"),
    FIXED_ASSETS("Activos", "HomeRepairService"),
    INVESTMENTS("Inversiones", "ShowChart")
}

data class AccountReportItem(
    val id: String,
    val name: String,
    val bankName: String,
    val initialBalance: Double,
    val totalInflows: Double,
    val totalOutflows: Double,
    val currentBalance: Double,
    val transactionCount: Int,
    val currency: String
)

data class CreditCardReportItem(
    val accountId: String,
    val cardName: String,
    val bankName: String,
    val quotaTotal: Double,
    val quotaUsed: Double,
    val quotaAvailable: Double,
    val utilizationPct: Double,
    val currentPeriodExpenses: Double,
    val currentPeriodPayments: Double,
    val interestEstimate: Double,
    val pendingBalance: Double,
    val cutOffDay: Int,
    val paymentDueDay: Int
)

data class DebtReportItem(
    val id: String,
    val creditorName: String,
    val debtType: String,
    val initialBalance: Double,
    val currentBalance: Double,
    val totalPayments: Double,
    val amortizedCapital: Double,
    val interestPaid: Double,
    val nextDueDate: String,
    val status: String
)

data class AssetReportItem(
    val id: String,
    val assetCode: String,
    val name: String,
    val categoryName: String,
    val acquisitionCost: Double,
    val currentMarketValue: Double,
    val bookValue: Double,
    val accumulatedDepreciation: Double,
    val periodDepreciation: Double,
    val maintenanceExpense: Double,
    val isDisposedOrSold: Boolean,
    val status: String
)

data class InvestmentReportItem(
    val id: String,
    val name: String,
    val entityOrPlatform: String,
    val investedCapital: Double,
    val currentValue: Double,
    val generatedYields: Double,
    val returnPercentage: Double,
    val variationAmount: Double,
    val isPositive: Boolean
)

data class SpecializedReportsBundle(
    val activeReportType: SpecializedReportType = SpecializedReportType.INCOME,
    val accountReports: List<AccountReportItem> = emptyList(),
    val creditCardReports: List<CreditCardReportItem> = emptyList(),
    val debtReports: List<DebtReportItem> = emptyList(),
    val assetReports: List<AssetReportItem> = emptyList(),
    val investmentReports: List<InvestmentReportItem> = emptyList(),
    val recurringExpensesList: List<GroupedFinancialAmount> = emptyList(),
    val variableExpensesList: List<GroupedFinancialAmount> = emptyList(),
    val incomeByCategoryList: List<GroupedFinancialAmount> = emptyList(),
    val incomeBySourceList: List<GroupedFinancialAmount> = emptyList()
)
