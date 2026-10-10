package com.example.presentation.goals.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.FinancialGoalEntity
import com.example.domain.repository.FinancialRepository
import com.example.domain.usecase.CalculateGoalProgressUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class GoalFormViewModel(
    private val financialRepository: FinancialRepository,
    private val calculateGoalProgressUseCase: CalculateGoalProgressUseCase = CalculateGoalProgressUseCase(),
    val userId: String = "user_default"
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalFormState())
    val uiState: StateFlow<GoalFormState> = _uiState.asStateFlow()

    init {
        // Load actual accounts from Room database
        viewModelScope.launch {
            financialRepository.getAccounts(userId).collect { accounts ->
                _uiState.update { state ->
                    val defaultTarget = accounts.firstOrNull { it.name.contains("Bancolombia", ignoreCase = true) }
                        ?: accounts.firstOrNull()

                    val targetId = state.targetAccountId.ifBlank { defaultTarget?.id ?: "" }
                    val targetName = if (state.targetAccountName == "Bancolombia ••••• 1234" && defaultTarget != null) {
                        "${defaultTarget.name} ${defaultTarget.accountNumberMasked ?: ""}"
                    } else state.targetAccountName

                    val sourceId = state.sourceAccountId.ifBlank { targetId }
                    val sourceName = if (state.sourceAccountName == "Bancolombia ••••• 1234" && defaultTarget != null) {
                        targetName
                    } else state.sourceAccountName

                    state.copy(
                        availableAccounts = accounts,
                        targetAccountId = targetId,
                        targetAccountName = targetName,
                        sourceAccountId = sourceId,
                        sourceAccountName = sourceName
                    )
                }
            }
        }
        recalculateEquation()
    }

    fun onNameChanged(newName: String) {
        val trimmed = newName.take(50)
        _uiState.update { it.copy(name = trimmed) }
    }

    fun onCategoryChanged(newCategory: String) {
        _uiState.update { it.copy(categoryName = newCategory) }
    }

    fun onDescriptionChanged(newDesc: String) {
        val trimmed = newDesc.take(200)
        _uiState.update { it.copy(description = trimmed) }
    }

    fun onColorChanged(newColorHex: String) {
        _uiState.update { it.copy(selectedColorHex = newColorHex) }
    }

    fun onIconChanged(newIconName: String) {
        _uiState.update { it.copy(selectedIconName = newIconName) }
    }

    fun onTargetAmountChanged(amount: Double) {
        _uiState.update { it.copy(targetAmount = maxOf(0.0, amount)) }
        recalculateEquation()
    }

    fun onSavedAmountChanged(amount: Double) {
        _uiState.update { it.copy(savedAmount = maxOf(0.0, amount)) }
        recalculateEquation()
    }

    fun onHasDeadlineChanged(has: Boolean) {
        _uiState.update { it.copy(hasDeadline = has) }
        recalculateEquation()
    }

    fun onDeadlineDateChanged(date: String) {
        _uiState.update { it.copy(deadlineDate = date) }
        recalculateEquation()
    }

    fun onStartDateChanged(date: String) {
        _uiState.update { it.copy(startDate = date) }
        recalculateEquation()
    }

    fun onMonthlyContributionChanged(contribution: Double) {
        _uiState.update { it.copy(monthlyContribution = maxOf(0.0, contribution)) }
        recalculateEquation(preserveContribution = true)
    }

    fun onFrequencyChanged(freq: String) {
        _uiState.update { it.copy(frequency = freq) }
    }

    fun onTargetAccountChanged(id: String, name: String) {
        _uiState.update { it.copy(targetAccountId = id, targetAccountName = name) }
    }

    fun onSourceAccountChanged(id: String, name: String) {
        _uiState.update { it.copy(sourceAccountId = id, sourceAccountName = name) }
    }

    fun onAutoContributeChanged(enabled: Boolean) {
        _uiState.update { it.copy(autoContribute = enabled) }
    }

    fun onVirtualPocketChanged(enabled: Boolean) {
        _uiState.update { it.copy(virtualPocketEnabled = enabled) }
    }

    fun onContributionMethodChanged(method: String) {
        _uiState.update { it.copy(contributionMethod = method) }
    }

    fun toggleAccordion(section: String) {
        _uiState.update { state ->
            when (section) {
                "basic" -> state.copy(isBasicInfoExpanded = !state.isBasicInfoExpanded)
                "financial" -> state.copy(isFinancialTargetExpanded = !state.isFinancialTargetExpanded)
                "timeline" -> state.copy(isTimelineExpanded = !state.isTimelineExpanded)
                "saving" -> state.copy(isSavingPlanExpanded = !state.isSavingPlanExpanded)
                "funding" -> state.copy(isFundingSourceExpanded = !state.isFundingSourceExpanded)
                else -> state
            }
        }
    }

    // In-App Keyboard Handlers
    fun openKeyboard(target: String) {
        _uiState.update { it.copy(activeKeyboardTarget = target) }
    }

    fun closeKeyboard() {
        _uiState.update { it.copy(activeKeyboardTarget = null) }
    }

    fun onKeyboardDigit(digit: String) {
        val s = _uiState.value
        when (s.activeKeyboardTarget) {
            "TARGET_AMOUNT" -> {
                val currentStr = s.targetAmount.toLong().toString()
                val nextStr = if (currentStr == "0") digit else currentStr + digit
                onTargetAmountChanged(nextStr.toDoubleOrNull() ?: 0.0)
            }
            "SAVED_AMOUNT" -> {
                val currentStr = s.savedAmount.toLong().toString()
                val nextStr = if (currentStr == "0") digit else currentStr + digit
                onSavedAmountChanged(nextStr.toDoubleOrNull() ?: 0.0)
            }
            "MONTHLY_CONTRIBUTION" -> {
                val currentStr = s.monthlyContribution.toLong().toString()
                val nextStr = if (currentStr == "0") digit else currentStr + digit
                onMonthlyContributionChanged(nextStr.toDoubleOrNull() ?: 0.0)
            }
        }
    }

    fun onKeyboardQuickAdd(amount: Double) {
        val s = _uiState.value
        when (s.activeKeyboardTarget) {
            "TARGET_AMOUNT" -> onTargetAmountChanged(s.targetAmount + amount)
            "SAVED_AMOUNT" -> onSavedAmountChanged(s.savedAmount + amount)
            "MONTHLY_CONTRIBUTION" -> onMonthlyContributionChanged(s.monthlyContribution + amount)
        }
    }

    fun onKeyboardBackspace() {
        val s = _uiState.value
        when (s.activeKeyboardTarget) {
            "TARGET_AMOUNT" -> {
                val currentStr = s.targetAmount.toLong().toString()
                val nextStr = if (currentStr.length > 1) currentStr.dropLast(1) else "0"
                onTargetAmountChanged(nextStr.toDoubleOrNull() ?: 0.0)
            }
            "SAVED_AMOUNT" -> {
                val currentStr = s.savedAmount.toLong().toString()
                val nextStr = if (currentStr.length > 1) currentStr.dropLast(1) else "0"
                onSavedAmountChanged(nextStr.toDoubleOrNull() ?: 0.0)
            }
            "MONTHLY_CONTRIBUTION" -> {
                val currentStr = s.monthlyContribution.toLong().toString()
                val nextStr = if (currentStr.length > 1) currentStr.dropLast(1) else "0"
                onMonthlyContributionChanged(nextStr.toDoubleOrNull() ?: 0.0)
            }
        }
    }

    fun onKeyboardClear() {
        when (_uiState.value.activeKeyboardTarget) {
            "TARGET_AMOUNT" -> onTargetAmountChanged(0.0)
            "SAVED_AMOUNT" -> onSavedAmountChanged(0.0)
            "MONTHLY_CONTRIBUTION" -> onMonthlyContributionChanged(0.0)
        }
    }

    private fun recalculateEquation(preserveContribution: Boolean = false) {
        val s = _uiState.value
        val projection = calculateGoalProgressUseCase(
            targetAmount = s.targetAmount,
            savedAmount = s.savedAmount,
            startDateStr = s.startDate,
            deadlineDateStr = s.deadlineDate,
            hasDeadline = s.hasDeadline,
            userMonthlyContribution = if (preserveContribution) s.monthlyContribution else null
        )

        _uiState.update {
            it.copy(
                remainingAmount = projection.remainingDeficit,
                progressPercentage = projection.progressPercentage,
                remainingMonths = projection.remainingMonths,
                monthlyContribution = if (preserveContribution) it.monthlyContribution else projection.recommendedMonthlyContribution,
                isRuleEngineValid = projection.isRuleEngineValid,
                ruleEngineTitle = projection.ruleEngineTitle,
                ruleEngineMessage = projection.ruleEngineMessage
            )
        }
    }

    fun saveGoal(targetUserId: String? = null) {
        val effectiveUserId = targetUserId ?: this.userId
        val s = _uiState.value
        if (!s.isFormSubmittable) return

        val validation = com.example.domain.validation.FormValidator.validateGoal(
            name = s.name,
            targetAmount = s.targetAmount,
            categoryName = s.categoryName
        )
        if (validation is com.example.domain.validation.FormValidator.ValidationResult.Invalid) {
            _uiState.update { it.copy(errorMessage = validation.firstError) }
            return
        }

        val cleanName = com.example.domain.validation.FormValidator.sanitizeConcept(s.name)
        val cleanDescription = com.example.domain.validation.FormValidator.sanitizeDescription(s.description)
        val cleanColorHex = com.example.domain.validation.FormValidator.sanitizeColorHex(s.selectedColorHex, "#8B5CF6")
        val cleanCategoryName = com.example.domain.validation.FormValidator.sanitizeConcept(s.categoryName)

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val goalId = "goal_${UUID.randomUUID().toString().take(12)}"
            val goalEntity = FinancialGoalEntity(
                id = goalId,
                userId = effectiveUserId,
                name = cleanName,
                categoryId = s.categoryId,
                categoryName = cleanCategoryName,
                description = cleanDescription.ifBlank { null },
                colorHex = cleanColorHex,
                iconName = s.selectedIconName,
                targetAmount = s.targetAmount,
                savedAmount = s.savedAmount,
                hasDeadline = s.hasDeadline,
                startDate = s.startDate,
                deadlineDate = s.deadlineDate,
                targetAccountId = s.targetAccountId.ifBlank { "acc_default" },
                targetAccountName = s.targetAccountName,
                sourceAccountId = s.sourceAccountId.ifBlank { s.targetAccountId.ifBlank { "acc_default" } },
                sourceAccountName = s.sourceAccountName,
                autoContribute = s.autoContribute,
                monthlyContribution = s.monthlyContribution,
                contributionDay = s.contributionDay,
                contributionFrequency = s.frequency,
                contributionMethod = s.contributionMethod,
                virtualPocketEnabled = s.virtualPocketEnabled,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )

            // When virtualPocketEnabled is TRUE, the savedAmount is frozen from availableBalance
            val freezeAmount = if (s.virtualPocketEnabled) s.savedAmount else 0.0

            val result = financialRepository.createGoal(goalEntity, freezeAmount)
            result.onSuccess {
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            }.onFailure { ex ->
                _uiState.update { it.copy(isSaving = false, errorMessage = ex.message ?: "Error al guardar el objetivo") }
            }
        }
    }

    companion object {
        fun provideFactory(financialRepository: FinancialRepository, userId: String = "user_default"): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GoalFormViewModel(financialRepository, userId = userId) as T
                }
            }
    }
}
