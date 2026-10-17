package com.example.domain.models

import java.time.YearMonth

/**
 * Domain Models for KashApp Financial Dashboard.
 * Free of Android UI framework dependencies.
 */

data class NetWorthSummary(
    val availableCash: Double = 0.0,
    val investments: Double = 0.0,
    val debts: Double = 0.0,
    val assets: Double = 0.0,
    val netWorthTotal: Double = 0.0,
    val variationPercentage: Double = 0.0
) {
    companion object {
        val EMPTY = NetWorthSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }
}

data class MonthlyKpiSummary(
    val incomeTotal: Double = 0.0,
    val incomeVariation: Double = 0.0,
    val expenseTotal: Double = 0.0,
    val expenseVariation: Double = 0.0,
    val savingsTotal: Double = 0.0,
    val savingsVariation: Double = 0.0,
    val budgetSpent: Double = 0.0,
    val budgetTotal: Double = 0.0,
    val budgetPercentage: Int = 0
) {
    companion object {
        val EMPTY = MonthlyKpiSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0)
    }
}

data class CashFlowPoint(
    val label: String,
    val income: Double,
    val expense: Double,
    val netBalance: Double
)

data class CashFlowTimeline(
    val points: List<CashFlowPoint> = emptyList(),
    val granularity: String = "Mes"
) {
    companion object {
        val EMPTY = CashFlowTimeline()
    }
}

data class CategoryExpenseShare(
    val categoryName: String,
    val amount: Double,
    val percentage: Int,
    val colorHex: Long
)

data class TransactionItemDto(
    val id: String,
    val dateFormatted: String,
    val description: String,
    val category: String,
    val account: String,
    val amount: Double,
    val isIncome: Boolean,
    val isTransfer: Boolean = false,
    val transferType: String? = null, // "TRANSFER_OUT" or "TRANSFER_IN"
    val driveFileId: String? = null
)


data class UpcomingBillDto(
    val id: String,
    val title: String,
    val dueDateFormatted: String,
    val amount: Double,
    val isIncome: Boolean,
    val daysRemaining: Int,
    val isPaid: Boolean = false,
    val iconType: String = "bill"
)

data class GoalProgressDto(
    val id: String,
    val title: String,
    val currentAmount: Double,
    val targetAmount: Double,
    val percentage: Int,
    val category: String = "Educación",
    val dueDate: String = "Diciembre 2027",
    val iconName: String = "school",
    val colorHex: String = "#8B5CF6"
)

data class CreditCardSummaryDto(
    val cardName: String,
    val usedLimit: Double,
    val totalLimit: Double,
    val usagePercentage: Int,
    val cutoffDate: String,
    val paymentDate: String,
    val cardColorHex: Long = 0xFF7C3AED
)

data class SystemAlertDto(
    val id: String,
    val message: String,
    val ruleType: String
)
