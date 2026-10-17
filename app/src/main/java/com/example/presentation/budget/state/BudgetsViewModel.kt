package com.example.presentation.budget.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.dao.FinancialDao
import com.example.data.local.entity.BudgetItemEntity
import com.example.data.local.entity.BudgetWithItems
import com.example.data.local.entity.FinancialGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.repository.FinancialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class BudgetConfiguration(
    val distributionRule: BudgetDistributionRule = BudgetDistributionRule.RULE_50_30_20,
    val customNeedsPct: Int = 50,
    val customWantsPct: Int = 30,
    val customSavingsPct: Int = 20,
    val warningThresholdPct: Int = 80,
    val budgetingStyle: BudgetingStyle = BudgetingStyle.ENVELOPES,
    val cycleType: BudgetCycleType = BudgetCycleType.MONTHLY_CALENDAR,
    val customPayDay: Int = 1,
    val notifyOnThreshold: Boolean = true,
    val notifyOnOverdraft: Boolean = true,
    val notifyOnBurnRateRisk: Boolean = true
)

enum class BudgetDistributionRule(val title: String, val needs: Int, val wants: Int, val savings: Int) {
    RULE_50_30_20("Regla 50 / 30 / 20 (Clásica)", 50, 30, 20),
    RULE_70_20_10("Regla 70 / 20 / 10 (Operativa)", 70, 20, 10),
    RULE_60_20_20("Regla 60 / 20 / 20 (Ahorro Firme)", 60, 20, 20),
    CUSTOM("Regla Personalizada", 50, 30, 20)
}

enum class BudgetingStyle(val title: String, val description: String) {
    ENVELOPES("Sobres Tradicionales", "Límites estrictos por sobre con rebalanceo manual"),
    FLEXIBLE("Flexible con Compensación", "Permite sobregiros compensados con superávit"),
    ZERO_BASED("Base Cero (Zero-Based)", "Cada peso ingresado se asigna a un sobre o meta")
}

enum class BudgetCycleType(val title: String) {
    MONTHLY_CALENDAR("Mensual (Día 1 al 30/31)"),
    BIWEEKLY("Quincenal (Día 1-15 y 16-30)"),
    CUSTOM_PAYDAY("Día de Nómina Personalizado")
}

data class FiftyThirtyTwentyBreakdown(
    val needsAllocated: Double = 0.0,
    val needsSpent: Double = 0.0,
    val needsPct: Double = 0.0,
    val targetNeedsPct: Double = 50.0,
    val wantsAllocated: Double = 0.0,
    val wantsSpent: Double = 0.0,
    val wantsPct: Double = 0.0,
    val targetWantsPct: Double = 30.0,
    val savingsAllocated: Double = 0.0,
    val savingsSpent: Double = 0.0,
    val savingsPct: Double = 0.0,
    val targetSavingsPct: Double = 20.0,
    val statusLabel: String = "Balance Equilibrado",
    val adviceMessage: String = "Distribución saludable alineada a la regla de presupuesto activa."
)

data class BurnRateAnalysis(
    val daysTotal: Long = 30,
    val daysElapsed: Long = 1,
    val daysRemaining: Long = 29,
    val allowedDailySpending: Double = 0.0,
    val actualDailySpending: Double = 0.0,
    val projectedSpentTotal: Double = 0.0,
    val projectedDifference: Double = 0.0, // Positivo: superávit, Negativo: sobregiro proyectado
    val burnStatus: String = "DENTRO_DEL_RITMO" // "DENTRO_DEL_RITMO", "EN_RIESGO", "SOBREGIRADO"
)

data class BudgetsUiState(
    val isLoading: Boolean = false,
    val selectedTab: Int = 0, // 0: Presupuesto Activo & Sobres, 1: Metas & Distribución, 2: Rebalanceo & Alertas, 3: Historial
    val budgets: List<BudgetWithItems> = emptyList(),
    val activeBudgetId: String? = null,
    val activeBudgetWithItems: BudgetWithItems? = null,
    val activeGoals: List<FinancialGoalEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val fiftyThirtyTwenty: FiftyThirtyTwentyBreakdown = FiftyThirtyTwentyBreakdown(),
    val burnRate: BurnRateAnalysis = BurnRateAnalysis(),
    val configuration: BudgetConfiguration = BudgetConfiguration(),
    val categorySearchQuery: String = "",
    val envelopeFilterStatus: String = "TODOS", // "TODOS", "NORMAL", "ALERTA", "SOBREGIRADO"
    val isPrivacyMode: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    // Dialogs
    val envelopeForTransactions: BudgetItemEntity? = null,
    val showRebalanceDialog: Boolean = false,
    val showSweepSurplusDialog: Boolean = false,
    val showEditCeilingDialog: Boolean = false,
    val showConfigDialog: Boolean = false
)

class BudgetsViewModel(
    private val financialRepository: FinancialRepository,
    private val financialDao: FinancialDao,
    val userId: String = "user_default"
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetsUiState(isLoading = true))
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                financialRepository.getAllBudgetsWithItems(userId),
                financialDao.getGoalsFlow(userId),
                financialDao.getTransactionsFlow(userId)
            ) { budgets: List<BudgetWithItems>, goals: List<FinancialGoalEntity>, txs: List<TransactionEntity> ->
                Triple(budgets, goals, txs)
            }.collect { (budgets, goals, txs) ->
                _uiState.update { current ->
                    val isNewBudgetAdded = budgets.size > current.budgets.size
                    val chosenBudgetId = if (isNewBudgetAdded) {
                        budgets.firstOrNull()?.budget?.id
                    } else {
                        current.activeBudgetId ?: budgets.firstOrNull()?.budget?.id
                    }
                    val activeBudget = budgets.firstOrNull { it.budget.id == chosenBudgetId } ?: budgets.firstOrNull()

                    val fifty = compute50_30_20(activeBudget, goals)
                    val burn = computeBurnRate(activeBudget)

                    current.copy(
                        isLoading = false,
                        budgets = budgets,
                        activeBudgetId = activeBudget?.budget?.id,
                        activeBudgetWithItems = activeBudget,
                        activeGoals = goals,
                        transactions = txs,
                        fiftyThirtyTwenty = fifty,
                        burnRate = burn
                    )
                }
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun selectActiveBudget(budgetId: String) {
        val budget = _uiState.value.budgets.firstOrNull { it.budget.id == budgetId }
        val fifty = compute50_30_20(budget, _uiState.value.activeGoals)
        val burn = computeBurnRate(budget)
        _uiState.update {
            it.copy(
                activeBudgetId = budgetId,
                activeBudgetWithItems = budget,
                fiftyThirtyTwenty = fifty,
                burnRate = burn
            )
        }
    }

    fun togglePrivacyMode() {
        _uiState.update { it.copy(isPrivacyMode = !it.isPrivacyMode) }
    }

    fun openTransactionsForEnvelope(envelope: BudgetItemEntity) {
        _uiState.update { it.copy(envelopeForTransactions = envelope) }
    }

    fun closeEnvelopeTransactions() {
        _uiState.update { it.copy(envelopeForTransactions = null) }
    }

    fun openRebalanceDialog() {
        _uiState.update { it.copy(showRebalanceDialog = true) }
    }

    fun closeRebalanceDialog() {
        _uiState.update { it.copy(showRebalanceDialog = false) }
    }

    fun openSweepSurplusDialog() {
        _uiState.update { it.copy(showSweepSurplusDialog = true) }
    }

    fun closeSweepSurplusDialog() {
        _uiState.update { it.copy(showSweepSurplusDialog = false) }
    }

    fun openEditCeilingDialog() {
        _uiState.update { it.copy(showEditCeilingDialog = true) }
    }

    fun closeEditCeilingDialog() {
        _uiState.update { it.copy(showEditCeilingDialog = false) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
    }

    fun executeRebalance(sourceItemId: String, targetItemId: String, amount: Double) {
        viewModelScope.launch {
            val result = financialRepository.rebalanceEnvelopes(sourceItemId, targetItemId, amount)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            showRebalanceDialog = false,
                            statusMessage = "Sobres rebalanceados con éxito ($ $amount transferidos)"
                        )
                    }
                },
                onFailure = { ex ->
                    _uiState.update { it.copy(errorMessage = ex.message ?: "Error al rebalancear sobres") }
                }
            )
        }
    }

    fun updateEnvelopeAllocation(itemId: String, newAmount: Double) {
        viewModelScope.launch {
            val result = financialRepository.updateBudgetItemAllocated(itemId, newAmount)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(statusMessage = "Monto de sobre actualizado") }
                },
                onFailure = { ex ->
                    _uiState.update { it.copy(errorMessage = ex.message ?: "Error al actualizar sobre") }
                }
            )
        }
    }

    fun updateCeilingTotal(newTotal: Double) {
        val currentBudgetId = _uiState.value.activeBudgetId ?: return
        viewModelScope.launch {
            val result = financialRepository.updateBudgetTotalAmount(currentBudgetId, newTotal)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            showEditCeilingDialog = false,
                            statusMessage = "Techo presupuestal actualizado a $ $newTotal"
                        )
                    }
                },
                onFailure = { ex ->
                    _uiState.update { it.copy(errorMessage = ex.message ?: "Error al modificar techo") }
                }
            )
        }
    }

    fun sweepSurplusToGoal(goalId: String, amount: Double) {
        viewModelScope.launch {
            val result = financialRepository.sweepSurplusToGoal(goalId, amount, userId)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            showSweepSurplusDialog = false,
                            statusMessage = "¡Excedente de $ $amount transferido exitosamente a la meta!"
                        )
                    }
                },
                onFailure = { ex ->
                    _uiState.update { it.copy(errorMessage = ex.message ?: "Error al barrer excedente") }
                }
            )
        }
    }

    fun contributeDirectlyToGoal(goalId: String, amount: Double) {
        viewModelScope.launch {
            val result = financialRepository.sweepSurplusToGoal(goalId, amount, userId)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            statusMessage = "Aporte presupuestado de $ $amount sumado a la meta financiera"
                        )
                    }
                },
                onFailure = { ex ->
                    _uiState.update { it.copy(errorMessage = ex.message ?: "Error al aportar a la meta") }
                }
            )
        }
    }

    fun cloneBudgetForNextMonth(budgetId: String) {
        viewModelScope.launch {
            val result = financialRepository.cloneBudgetForNextMonth(budgetId, userId)
            result.fold(
                onSuccess = { cloned ->
                    _uiState.update {
                        it.copy(
                            statusMessage = "Presupuesto '${cloned.name}' creado para el próximo mes",
                            activeBudgetId = cloned.id
                        )
                    }
                },
                onFailure = { ex ->
                    _uiState.update { it.copy(errorMessage = ex.message ?: "Error al clonar presupuesto") }
                }
            )
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            val result = financialRepository.deleteBudget(budgetId)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            statusMessage = "Presupuesto eliminado",
                            activeBudgetId = null
                        )
                    }
                },
                onFailure = { ex ->
                    _uiState.update { it.copy(errorMessage = ex.message ?: "Error al eliminar presupuesto") }
                }
            )
        }
    }

    fun toggleConfigDialog(show: Boolean) {
        _uiState.update { it.copy(showConfigDialog = show) }
    }

    fun updateBudgetConfiguration(config: BudgetConfiguration) {
        _uiState.update { current ->
            val fifty = compute50_30_20(current.activeBudgetWithItems, current.activeGoals, config)
            val burn = computeBurnRate(current.activeBudgetWithItems, config)
            current.copy(
                configuration = config,
                showConfigDialog = false,
                fiftyThirtyTwenty = fifty,
                burnRate = burn,
                statusMessage = "Configuración de presupuesto actualizada."
            )
        }
    }

    fun setCategorySearchQuery(query: String) {
        _uiState.update { it.copy(categorySearchQuery = query) }
    }

    fun setEnvelopeFilterStatus(status: String) {
        _uiState.update { it.copy(envelopeFilterStatus = status) }
    }

    private fun compute50_30_20(
        budget: BudgetWithItems?,
        goals: List<FinancialGoalEntity>,
        config: BudgetConfiguration = _uiState.value.configuration
    ): FiftyThirtyTwentyBreakdown {
        val targetNeeds = if (config.distributionRule == BudgetDistributionRule.CUSTOM) config.customNeedsPct.toDouble() else config.distributionRule.needs.toDouble()
        val targetWants = if (config.distributionRule == BudgetDistributionRule.CUSTOM) config.customWantsPct.toDouble() else config.distributionRule.wants.toDouble()
        val targetSavings = if (config.distributionRule == BudgetDistributionRule.CUSTOM) config.customSavingsPct.toDouble() else config.distributionRule.savings.toDouble()

        if (budget == null || budget.budget.totalAmount <= 0.0) {
            return FiftyThirtyTwentyBreakdown(
                targetNeedsPct = targetNeeds,
                targetWantsPct = targetWants,
                targetSavingsPct = targetSavings
            )
        }

        val totalCeiling = budget.budget.totalAmount

        // Needs: Vivienda, Mercado, Servicios, Salud, Educación, Transporte
        val needsKeywords = listOf("vivienda", "arriendo", "mercado", "comida", "servicios", "salud", "transporte", "educacion", "gasolina")

        var needsAlloc = 0.0
        var needsSpent = 0.0
        var wantsAlloc = 0.0
        var wantsSpent = 0.0
        var savingsAlloc = 0.0
        var savingsSpent = 0.0

        budget.items.forEach { item ->
            val nameLower = item.categoryName.lowercase()
            when {
                nameLower.contains("meta") || nameLower.contains("ahorro") || nameLower.contains("inversion") -> {
                    savingsAlloc += item.allocatedAmount
                    savingsSpent += item.spentAmount
                }
                needsKeywords.any { nameLower.contains(it) } -> {
                    needsAlloc += item.allocatedAmount
                    needsSpent += item.spentAmount
                }
                else -> {
                    wantsAlloc += item.allocatedAmount
                    wantsSpent += item.spentAmount
                }
            }
        }

        val totalMonthlyGoalContributions = goals.filter { it.isActive }.sumOf { it.monthlyContribution ?: 0.0 }
        if (savingsAlloc == 0.0 && totalMonthlyGoalContributions > 0.0) {
            savingsAlloc = totalMonthlyGoalContributions.coerceAtMost(totalCeiling * (targetSavings / 100.0 * 1.5))
        }

        val needsPct = (needsAlloc / totalCeiling) * 100.0
        val wantsPct = (wantsAlloc / totalCeiling) * 100.0
        val savingsPct = (savingsAlloc / totalCeiling) * 100.0

        val (status, advice) = when {
            needsPct > (targetNeeds + 10.0) -> Pair("Alerta en Necesidades", "Tus gastos fijos (${"%.0f".format(needsPct)}%) superan la meta del ${"%.0f".format(targetNeeds)}%. Optimiza contratos o servicios.")
            wantsPct > (targetWants + 10.0) -> Pair("Exceso en Deseos", "Los gastos discrecionales (${"%.0f".format(wantsPct)}%) superan la meta del ${"%.0f".format(targetWants)}%. Modera salidas y ocio.")
            savingsPct < (targetSavings - 5.0) -> Pair("Ahorro por Debajo", "Tu ahorro (${"%.0f".format(savingsPct)}%) está por debajo de la meta del ${"%.0f".format(targetSavings)}%. Considera aportar a tus metas.")
            else -> Pair("Distribución Óptima (${config.distributionRule.title})", "Tu presupuesto cumple la meta planificada (${"%.0f".format(targetNeeds)}/${"%.0f".format(targetWants)}/${"%.0f".format(targetSavings)}).")
        }

        return FiftyThirtyTwentyBreakdown(
            needsAllocated = needsAlloc,
            needsSpent = needsSpent,
            needsPct = needsPct,
            targetNeedsPct = targetNeeds,
            wantsAllocated = wantsAlloc,
            wantsSpent = wantsSpent,
            wantsPct = wantsPct,
            targetWantsPct = targetWants,
            savingsAllocated = savingsAlloc,
            savingsSpent = savingsSpent,
            savingsPct = savingsPct,
            targetSavingsPct = targetSavings,
            statusLabel = status,
            adviceMessage = advice
        )
    }

    private fun computeBurnRate(
        budget: BudgetWithItems?,
        config: BudgetConfiguration = _uiState.value.configuration
    ): BurnRateAnalysis {
        if (budget == null) return BurnRateAnalysis()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()

        val start = try {
            LocalDate.parse(budget.budget.startDate.take(10), formatter)
        } catch (_: Exception) {
            today.withDayOfMonth(1)
        }

        val end = try {
            LocalDate.parse(budget.budget.endDate.take(10), formatter)
        } catch (_: Exception) {
            today.withDayOfMonth(today.lengthOfMonth())
        }

        val daysTotal = ChronoUnit.DAYS.between(start, end).coerceAtLeast(1) + 1
        val daysElapsed = ChronoUnit.DAYS.between(start, today).coerceIn(1, daysTotal)
        val daysRemaining = (daysTotal - daysElapsed).coerceAtLeast(1)

        val totalBudget = budget.budget.totalAmount
        val totalSpent = budget.totalSpent
        val remainingBudget = (totalBudget - totalSpent).coerceAtLeast(0.0)

        val allowedDaily = remainingBudget / daysRemaining.toDouble()
        val actualDaily = totalSpent / daysElapsed.toDouble()

        val projectedSpentTotal = actualDaily * daysTotal
        val projectedDiff = totalBudget - projectedSpentTotal
        val spentPct = if (totalBudget > 0) (totalSpent / totalBudget) * 100.0 else 0.0

        val status = when {
            totalSpent >= totalBudget -> "SOBREGIRADO"
            spentPct >= config.warningThresholdPct || projectedSpentTotal > totalBudget -> "EN_RIESGO"
            else -> "DENTRO_DEL_RITMO"
        }

        return BurnRateAnalysis(
            daysTotal = daysTotal,
            daysElapsed = daysElapsed,
            daysRemaining = daysRemaining,
            allowedDailySpending = allowedDaily,
            actualDailySpending = actualDaily,
            projectedSpentTotal = projectedSpentTotal,
            projectedDifference = projectedDiff,
            burnStatus = status
        )
    }

    companion object {
        fun provideFactory(
            financialRepository: FinancialRepository,
            financialDao: FinancialDao,
            userId: String = "user_default"
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BudgetsViewModel(financialRepository, financialDao, userId) as T
            }
        }
    }
}
