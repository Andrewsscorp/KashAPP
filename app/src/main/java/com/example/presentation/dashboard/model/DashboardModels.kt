package com.example.presentation.dashboard.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

// ==========================================
// ENUMS
// ==========================================
enum class RiskLevel {
    HEALTHY,
    WARNING,
    CRITICAL
}

enum class AccountType {
    SAVINGS,
    CHECKING,
    CREDIT_CARD,
    INVESTMENT,
    CASH,
    CDT
}

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}

enum class InsightSeverity {
    INFO,
    WARNING,
    CRITICAL,
    SUCCESS
}

// ==========================================
// DATA STRUCTURES (IMMUTABLE & FINANCIAL PRECISION)
// ==========================================

@Immutable
data class PeriodState(
    val currentMonth: YearMonth = YearMonth.now(),
    val isHistorical: Boolean = false
) {
    val isCurrent: Boolean get() = currentMonth == YearMonth.now()
}

@Immutable
data class NetWorthState(
    val availableCash: BigDecimal = BigDecimal.ZERO,
    val investments: BigDecimal = BigDecimal.ZERO,
    val assets: BigDecimal = BigDecimal.ZERO,
    val liabilities: BigDecimal = BigDecimal.ZERO,
    val sparklinePoints: ImmutableList<Float> = persistentListOf(0f, 0f, 0f, 0f, 0f),
    val assetsForNetWorth: BigDecimal = assets
) {
    val totalNetWorth: BigDecimal
        get() = (availableCash + investments + assetsForNetWorth) - liabilities.abs()
}

@Immutable
data class HealthScore(
    val score: Int = 0,
    val riskLevel: RiskLevel = RiskLevel.HEALTHY,
    val insightMessage: String = "Inicia registrando tus cuentas y movimientos para calcular tu salud financiera."
)

@Immutable
data class KpiState(
    val income: BigDecimal = BigDecimal.ZERO,
    val expenses: BigDecimal = BigDecimal.ZERO,
    val savings: BigDecimal = BigDecimal.ZERO,
    val budgetLimit: BigDecimal = BigDecimal.ZERO
)

@Immutable
data class Account(
    val id: UUID,
    val name: String,
    val bankName: String,
    val balance: BigDecimal,
    val type: AccountType,
    val currency: String = "COP",
    val colorHex: String = "#0284C7",
    val allowsNegative: Boolean = false,
    val rawId: String = "",
    val isExcludedFromDashboard: Boolean = false
)

@Immutable
data class CashFlowEntry(
    val dateLabel: String,
    val totalIncome: BigDecimal,
    val totalExpense: BigDecimal
)

@Immutable
data class CategorySpend(
    val categoryId: UUID,
    val categoryName: String,
    val amount: BigDecimal,
    val hexColor: String
)

@Immutable
data class Transaction(
    val id: UUID,
    val mccIcon: String,
    val concept: String,
    val amount: BigDecimal,
    val type: TransactionType,
    val dateFormatted: String,
    val accountName: String,
    val categoryName: String,
    val rawId: String = ""
)

@Immutable
data class ScheduledPayment(
    val id: UUID = UUID.randomUUID(),
    val entityName: String,
    val dueDate: LocalDate,
    val amount: BigDecimal,
    val isIncome: Boolean = false,
    val rawId: String = id.toString(),
    val category: String = "SERVICIOS",
    val recurrence: String = "MONTHLY",
    val currency: String = "COP",
    val isPaid: Boolean = false,
    val paidDate: String? = null,
    val sourceAccountId: String? = null,
    val sourceAccountName: String? = null,
    val notes: String? = null,
    val reminderDaysBefore: Int = 3,
    val notificationEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Immutable
data class Goal(
    val id: UUID,
    val title: String,
    val targetAmount: BigDecimal,
    val currentAmount: BigDecimal,
    val targetDate: LocalDate? = null,
    val colorHex: String = "#10B981",
    val isCommitted: Boolean = false
) {
    val progressPercentage: Float
        get() = if (targetAmount > BigDecimal.ZERO) {
            (currentAmount.toFloat() / targetAmount.toFloat()).coerceIn(0f, 1f)
        } else 0f
}

@Immutable
data class CreditCard(
    val id: UUID,
    val cardName: String,
    val bankName: String,
    val quotaLimit: BigDecimal,
    val usedQuota: BigDecimal,
    val cutoffDateFormatted: String,
    val paymentDueDateFormatted: String,
    val colorHex: String = "#6366F1",
    val includeInNetWorth: Boolean = true
) {
    val availableQuota: BigDecimal get() = (quotaLimit - usedQuota).coerceAtLeast(BigDecimal.ZERO)
    val usagePercentage: Float
        get() = if (quotaLimit > BigDecimal.ZERO) {
            (usedQuota.toFloat() / quotaLimit.toFloat()).coerceIn(0f, 1f)
        } else 0f
}

@Immutable
data class SmartInsight(
    val id: UUID,
    val message: String,
    val severity: InsightSeverity
)

@Immutable
data class AiPromptState(
    val isProcessing: Boolean = false,
    val inputText: String = ""
)

// ==========================================
// UNIFIED MVI STATE & EVENTS
// ==========================================

@Immutable
data class DashboardUiState(
    val period: PeriodState = PeriodState(),
    val netWorth: NetWorthState = NetWorthState(),
    val healthScore: HealthScore = HealthScore(),
    val kpi: KpiState = KpiState(),
    val accounts: ImmutableList<Account> = persistentListOf(),
    val cashFlowEntries: ImmutableList<CashFlowEntry> = persistentListOf(),
    val categorySpends: ImmutableList<CategorySpend> = persistentListOf(),
    val recentTransactions: ImmutableList<Transaction> = persistentListOf(),
    val upcomingPayments: ImmutableList<ScheduledPayment> = persistentListOf(),
    val goals: ImmutableList<Goal> = persistentListOf(),
    val creditCards: ImmutableList<CreditCard> = persistentListOf(),
    val insights: ImmutableList<SmartInsight> = persistentListOf(),
    val aiPrompt: AiPromptState = AiPromptState(),
    val isPrivacyMode: Boolean = false,
    val isLoading: Boolean = false,
    val userName: String = "Andrés Felipe",
    val userEmail: String = "cdosuarez@gmail.com",
    val isSettingsOpen: Boolean = false,
    val baseCurrency: String = "COP",
    val isConvertedToUsd: Boolean = false,
    val displayCurrency: String = "COP",
    val usdExchangeRate: Double = 4142.50
)

sealed interface DashboardEvent {
    data class ChangePeriod(val month: YearMonth) : DashboardEvent
    data class InitiateTransfer(val accountId: UUID) : DashboardEvent
    data class SelectAccount(val accountId: UUID) : DashboardEvent
    data class SubmitAiQuery(val query: String) : DashboardEvent
    data class UpdateAiInput(val text: String) : DashboardEvent
    data object TogglePrivacyMode : DashboardEvent
    data object ToggleUsdConversion : DashboardEvent
    data object Refresh : DashboardEvent
    data object AddExpenseClick : DashboardEvent
    data object AddIncomeClick : DashboardEvent
    data object AddAccountClick : DashboardEvent
    data object AddGoalClick : DashboardEvent
    data object AddCreditCardClick : DashboardEvent
    data object OpenSettingsClick : DashboardEvent
    data object CloseSettingsClick : DashboardEvent
    data class ToggleAccountAllowNegative(val accountId: String, val allowNegative: Boolean) : DashboardEvent
    data class DeleteTransaction(
        val rawTxId: String,
        val onResult: ((com.example.domain.repository.DeleteTransactionResult) -> Unit)? = null
    ) : DashboardEvent
}
