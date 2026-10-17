package com.example.presentation.dashboard

import com.example.data.local.DatabaseEncryptionInfo
import com.example.domain.models.CashFlowTimeline
import com.example.domain.models.CategoryExpenseShare
import com.example.domain.models.CreditCardSummaryDto
import com.example.domain.models.GoalProgressDto
import com.example.domain.models.MonthlyKpiSummary
import com.example.domain.models.NetWorthSummary
import com.example.domain.models.SystemAlertDto
import com.example.domain.models.TransactionItemDto
import com.example.domain.models.UpcomingBillDto
import java.time.YearMonth

/**
 * Immutable UI State representing the complete KashApp Financial Dashboard.
 */
data class DashboardUiState(
    val isLoading: Boolean = false,
    val selectedPeriod: YearMonth = YearMonth.now(), // Dynamic current date
    val isPrivacyModeActive: Boolean = false,
    val accounts: List<com.example.data.local.entity.AccountEntity> = emptyList(),
    val netWorth: NetWorthSummary = NetWorthSummary.EMPTY,
    val healthScore: Int = 78,
    val healthScoreStatus: String = "En buen camino",
    val healthScoreAdvice: String = "Mejora tu ahorro mensual para llegar a tus objetivos más rápido.",
    val kpiMetrics: MonthlyKpiSummary = MonthlyKpiSummary.EMPTY,
    val cashFlowData: CashFlowTimeline = CashFlowTimeline.EMPTY,
    val categoryDistribution: List<CategoryExpenseShare> = emptyList(),
    val recentTransactions: List<TransactionItemDto> = emptyList(),
    val upcomingBills: List<UpcomingBillDto> = emptyList(),
    val topGoal: GoalProgressDto? = null,
    val creditCardPreview: CreditCardSummaryDto? = null,
    val investmentsList: List<com.example.data.local.entity.InvestmentEntity> = emptyList(),
    val budgetsList: List<com.example.data.local.entity.BudgetWithItems> = emptyList(),
    val activeAlerts: List<SystemAlertDto> = emptyList(),
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false,
    val searchResults: List<TransactionItemDto> = emptyList(),
    val unreadNotificationsCount: Int = 3,
    val isNotificationsSheetOpen: Boolean = false,
    val isQuickAddModalOpen: Boolean = false,
    val isCreateExpenseOpen: Boolean = false,
    val isCreateIncomeOpen: Boolean = false,
    val isCreateTransferOpen: Boolean = false,
    val isSelectAccountTypeOpen: Boolean = false,
    val isCreateAccountOpen: Boolean = false,
    val selectedAccountTypeCode: String = "SAVINGS",
    val isCreateGoalOpen: Boolean = false,
    val isCreateBudgetOpen: Boolean = false,
    val isCreateInvestmentOpen: Boolean = false,
    val isKashAIModalOpen: Boolean = false,
    val kashAIPromptText: String = "",
    val selectedTransactionForReceipt: TransactionItemDto? = null,
    val selectedGranularity: String = "Mes",
    val currentRoute: String = "kashapp://dashboard",
    val isSqlCipherSecurityDialogOpen: Boolean = false,
    val databaseEncryptionInfo: DatabaseEncryptionInfo = DatabaseEncryptionInfo(),
    val isSettingsOpen: Boolean = false,
    val userName: String = "Andrés Felipe",
    val userEmail: String = "cdosuarez@gmail.com",
    val defaultCurrency: String = "COP",
    val notificationsCardsAlert: Boolean = true,
    val notificationsBudgetAlert: Boolean = true,
    val notificationsPayrollAlert: Boolean = true,
    val biometricLockEnabled: Boolean = false
)
