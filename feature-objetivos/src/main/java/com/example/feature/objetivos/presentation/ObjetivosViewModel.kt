package com.example.feature.objetivos.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.dao.FinancialDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.FinancialGoalEntity
import com.example.feature.objetivos.domain.engine.AdvisorPortfolioPlan
import com.example.feature.objetivos.domain.engine.GlobalCommitmentSummary
import com.example.feature.objetivos.domain.engine.GoalCommitmentEngine
import com.example.feature.objetivos.domain.engine.GoalContributionAdvisor
import com.example.feature.objetivos.domain.engine.GoalEngine
import com.example.feature.objetivos.domain.engine.GoalMilestoneEngine
import com.example.feature.objetivos.domain.engine.GoalProjectionAnalysis
import com.example.feature.objetivos.domain.engine.GoalProjectionEngine
import com.example.feature.objetivos.domain.engine.GoalVelocityMetrics
import com.example.feature.objetivos.domain.model.GoalDetailMetric
import com.example.feature.objetivos.domain.model.GoalFilterTab
import com.example.feature.objetivos.domain.model.GoalSortOption
import com.example.feature.objetivos.domain.model.GoalStatus
import com.example.feature.objetivos.domain.model.GoalSummaryOverview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.feature.objetivos.sync.DataSyncManager

data class ObjetivosUiState(
    val isLoading: Boolean = false,
    val selectedTopTab: Int = 0, // 0: Resumen, 1: Objetivos, 2: Comprometidos, 3: Proyecciones, 4: Hitos y Asesor
    val summary: GoalSummaryOverview = GoalSummaryOverview(0.0, 0.0, 0.0, 0.0, 0, 0, 0),
    val allGoals: List<GoalDetailMetric> = emptyList(),
    val filteredGoals: List<GoalDetailMetric> = emptyList(),
    val availableAccounts: List<AccountEntity> = emptyList(),
    val availableCategories: List<String> = emptyList(),
    val selectedFilterTab: GoalFilterTab = GoalFilterTab.ALL,
    val searchQuery: String = "",
    val selectedSortOption: GoalSortOption = GoalSortOption.DEADLINE,
    val selectedCategoryFilter: String? = null,
    val selectedAccountFilter: String? = null,
    val activeGoalForDetail: GoalDetailMetric? = null,
    val showContributionDialogForGoal: GoalDetailMetric? = null,
    val isPrivacyMode: Boolean = false,
    val notificationMessage: String? = null,
    val errorMessage: String? = null,
    // Real Domain Engines Data
    val commitmentSummary: GlobalCommitmentSummary = GlobalCommitmentSummary(0.0, 0.0, 0.0, 0, emptyList()),
    val projections: List<GoalProjectionAnalysis> = emptyList(),
    val milestoneMetrics: List<GoalVelocityMetrics> = emptyList(),
    val advisorPlan: AdvisorPortfolioPlan = AdvisorPortfolioPlan(0.0, 0.0, true, emptyList())
)

class ObjetivosViewModel(
    private val financialDao: FinancialDao,
    val userId: String = "user_default",
    val dataSyncManager: DataSyncManager = DataSyncManager(financialDao, userId)
) : ViewModel() {

    private val _uiState = MutableStateFlow(ObjetivosUiState())
    val uiState: StateFlow<ObjetivosUiState> = _uiState.asStateFlow()

    private var cachedRawGoals: List<FinancialGoalEntity> = emptyList()
    private var cachedAccounts: List<AccountEntity> = emptyList()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                // Observe real-time combined sync snapshot from Room database via DataSyncManager
                dataSyncManager.observeSyncSnapshot().collect { snapshot ->
                    cachedRawGoals = snapshot.goals
                    cachedAccounts = snapshot.accounts

                    val mappedGoals = snapshot.goals.map { GoalEngine.mapToDetailMetric(it) }
                    val summary = GoalEngine.computeSummary(snapshot.goals)
                    val categories = snapshot.goals.map { it.categoryName }.distinct().sorted()

                    _uiState.update { state ->
                        val updatedDetail = state.activeGoalForDetail?.let { detail ->
                            mappedGoals.firstOrNull { it.id == detail.id }
                        }
                        state.copy(
                            isLoading = false,
                            summary = summary,
                            allGoals = mappedGoals,
                            availableAccounts = snapshot.accounts,
                            availableCategories = categories,
                            activeGoalForDetail = updatedDetail
                        )
                    }
                    applyFiltersAndSort()
                    recomputeDomainEngines()
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = ex.message) }
            }
        }
    }

    fun syncFromDashboard() {
        dataSyncManager.notifyExternalMutation("DASHBOARD")
    }

    private fun recomputeDomainEngines() {
        try {
            val goals = cachedRawGoals
            val accounts = cachedAccounts

            // 1. Motor de Fondos Comprometidos (Solo aportes hechos)
            val commitmentSummary = try {
                GoalCommitmentEngine.computeGlobalCommitmentSummary(goals, accounts)
            } catch (e: Exception) {
                GlobalCommitmentSummary(0.0, 0.0, 0.0, 0, emptyList())
            }

            // 2. Motor de Proyecciones Reales
            val projections = goals.filter { it.isActive }.mapNotNull { goal ->
                try {
                    GoalProjectionEngine.analyzeGoal(goal)
                } catch (e: Exception) {
                    null
                }
            }

            // 3. Motor de Hitos y Velocidad (25%, 50%, 75%, 100%)
            val milestones = goals.filter { it.isActive }.mapNotNull { goal ->
                try {
                    GoalMilestoneEngine.evaluateMilestones(goal)
                } catch (e: Exception) {
                    null
                }
            }

            // 4. Asesor de Ahorros y Mental Accounting
            val advisor = try {
                GoalContributionAdvisor.generatePortfolioAdvice(goals, accounts)
            } catch (e: Exception) {
                AdvisorPortfolioPlan(0.0, 0.0, true, emptyList())
            }

            _uiState.update {
                it.copy(
                    commitmentSummary = commitmentSummary,
                    projections = projections,
                    milestoneMetrics = milestones,
                    advisorPlan = advisor
                )
            }
        } catch (ex: Exception) {
            android.util.Log.e("ObjetivosViewModel", "Error recomputing domain engines", ex)
        }
    }

    fun selectTopTab(tabIndex: Int) {
        try {
            val safeTab = tabIndex.coerceIn(0, 4)
            _uiState.update { it.copy(selectedTopTab = safeTab) }
        } catch (ex: Exception) {
            android.util.Log.e("ObjetivosViewModel", "Error selecting tab", ex)
        }
    }

    fun onFilterTabSelected(tab: GoalFilterTab) {
        _uiState.update { it.copy(selectedFilterTab = tab) }
        applyFiltersAndSort()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFiltersAndSort()
    }

    fun onSortOptionSelected(sort: GoalSortOption) {
        _uiState.update { it.copy(selectedSortOption = sort) }
        applyFiltersAndSort()
    }

    fun onCategoryFilterSelected(category: String?) {
        _uiState.update { it.copy(selectedCategoryFilter = category) }
        applyFiltersAndSort()
    }

    fun onAccountFilterSelected(accountId: String?) {
        _uiState.update { it.copy(selectedAccountFilter = accountId) }
        applyFiltersAndSort()
    }

    fun togglePrivacyMode() {
        _uiState.update { it.copy(isPrivacyMode = !it.isPrivacyMode) }
    }

    fun openGoalDetail(goal: GoalDetailMetric) {
        _uiState.update { it.copy(activeGoalForDetail = goal) }
    }

    fun closeGoalDetail() {
        _uiState.update { it.copy(activeGoalForDetail = null) }
    }

    fun openContributionDialog(goal: GoalDetailMetric) {
        _uiState.update { it.copy(showContributionDialogForGoal = goal) }
    }

    fun closeContributionDialog() {
        _uiState.update { it.copy(showContributionDialogForGoal = null) }
    }

    fun clearNotification() {
        _uiState.update { it.copy(notificationMessage = null, errorMessage = null) }
    }

    private fun applyFiltersAndSort() {
        val state = _uiState.value
        var list = state.allGoals

        // 1. Tab filter
        list = when (state.selectedFilterTab) {
            GoalFilterTab.ALL -> list
            GoalFilterTab.IN_PROGRESS -> list.filter { it.status == GoalStatus.IN_PROGRESS }
            GoalFilterTab.NEAR_COMPLETION -> list.filter { it.status == GoalStatus.NEAR_COMPLETION }
            GoalFilterTab.AT_RISK -> list.filter { it.status == GoalStatus.AT_RISK }
            GoalFilterTab.COMPLETED -> list.filter { it.status == GoalStatus.COMPLETED }
            GoalFilterTab.ARCHIVED -> list.filter { it.status == GoalStatus.ARCHIVED || !it.isActive }
        }

        // 2. Search filter
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                it.categoryName.lowercase().contains(q) ||
                (it.description?.lowercase()?.contains(q) == true)
            }
        }

        // 3. Category filter
        if (state.selectedCategoryFilter != null) {
            list = list.filter { it.categoryName == state.selectedCategoryFilter }
        }

        // 4. Account filter
        if (state.selectedAccountFilter != null) {
            list = list.filter { it.targetAccountId == state.selectedAccountFilter || it.sourceAccountId == state.selectedAccountFilter }
        }

        // 5. Sorting
        list = when (state.selectedSortOption) {
            GoalSortOption.DEADLINE -> list.sortedWith(compareBy(nullsLast()) { it.deadlineDate })
            GoalSortOption.PROGRESS -> list.sortedByDescending { it.progressPercentage }
            GoalSortOption.REMAINING_AMOUNT -> list.sortedByDescending { it.pendingAmount }
            GoalSortOption.PRIORITY -> list.sortedByDescending {
                when (it.priority) {
                    "Alta" -> 3
                    "Media" -> 2
                    else -> 1
                }
            }
            GoalSortOption.RECENT -> list.sortedByDescending { it.createdAt }
        }

        _uiState.update { it.copy(filteredGoals = list) }
    }

    fun registerContribution(
        goalId: String,
        amount: Double,
        sourceAccountId: String?,
        isBudgetaryAllocation: Boolean,
        keepOpenForAnother: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                if (amount <= 0.0) {
                    _uiState.update { it.copy(errorMessage = "El monto del aporte debe ser mayor a 0.") }
                    return@launch
                }

                val goal = cachedRawGoals.firstOrNull { it.id == goalId }
                    ?: return@launch

                // Deduct from source account if not purely budgetary allocation
                if (!isBudgetaryAllocation && !sourceAccountId.isNullOrBlank()) {
                    val acc = financialDao.getAccountById(sourceAccountId)
                    if (acc != null && acc.availableBalance >= amount) {
                        financialDao.deductAccountBalance(acc.id, amount)
                    }
                }

                // Add to goal saved amount
                financialDao.contributeToGoal(goalId, amount)

                // If virtual pocket enabled, freeze the made contribution in target account
                if (goal.virtualPocketEnabled) {
                    financialDao.freezeAccountVirtualPocket(goal.targetAccountId, amount)
                }

                val updatedGoal = financialDao.getGoalById(goalId)
                if (updatedGoal != null) {
                    val mapped = GoalEngine.mapToDetailMetric(updatedGoal)
                    _uiState.update { state ->
                        state.copy(
                            notificationMessage = "¡Aporte de $${"%,.0f".format(amount)} registrado exitosamente!",
                            activeGoalForDetail = if (state.activeGoalForDetail?.id == goalId) mapped else state.activeGoalForDetail,
                            showContributionDialogForGoal = if (keepOpenForAnother) state.showContributionDialogForGoal else null
                        )
                    }
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al registrar aporte: ${ex.message}") }
            }
        }
    }

    fun markGoalAsCompleted(goalId: String) {
        viewModelScope.launch {
            try {
                val goal = cachedRawGoals.firstOrNull { it.id == goalId } ?: return@launch
                val updated = goal.copy(savedAmount = goal.targetAmount)
                financialDao.updateGoal(updated)
                _uiState.update { it.copy(notificationMessage = "¡Objetivo cumplido al 100%!") }
            } catch (ex: Exception) {
                _uiState.update { it.copy(errorMessage = ex.message) }
            }
        }
    }

    fun toggleGoalArchive(goalId: String, archive: Boolean) {
        viewModelScope.launch {
            try {
                financialDao.setGoalActive(goalId, !archive)
                _uiState.update {
                    it.copy(
                        notificationMessage = if (archive) "Objetivo archivado." else "Objetivo restaurado.",
                        activeGoalForDetail = null
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(errorMessage = ex.message) }
            }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            try {
                financialDao.deleteGoalById(goalId)
                _uiState.update {
                    it.copy(
                        notificationMessage = "Objetivo eliminado correctamente.",
                        activeGoalForDetail = null
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(errorMessage = ex.message) }
            }
        }
    }
}
