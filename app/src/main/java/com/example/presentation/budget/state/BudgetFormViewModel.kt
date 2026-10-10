package com.example.presentation.budget.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.BudgetItemEntity
import com.example.data.local.entity.CategoryEntity
import com.example.domain.repository.FinancialRepository
import com.example.domain.usecase.CalculateBudgetSummaryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class BudgetFormViewModel(
    private val financialRepository: FinancialRepository,
    private val calculateSummaryUseCase: CalculateBudgetSummaryUseCase = CalculateBudgetSummaryUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetFormState())
    val uiState: StateFlow<BudgetFormState> = _uiState.asStateFlow()

    init {
        setupDefaultDates()
        loadCategoriesAndInitMockData()
    }

    private fun setupDefaultDates() {
        val today = LocalDate.now()
        val firstDayOfMonth = today.withDayOfMonth(1)
        val lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth())
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        _uiState.update {
            it.copy(
                startDate = firstDayOfMonth.format(formatter),
                endDate = lastDayOfMonth.format(formatter)
            )
        }
    }

    private fun loadCategoriesAndInitMockData() {
        viewModelScope.launch {
            financialRepository.getActiveCategories("EXPENSE").collect { categories ->
                _uiState.update { currentState ->
                    currentState.copy(
                        availableCategories = categories
                    )
                }
                recalculate()
            }
        }
    }

    fun onQuickAddCeiling(amountToAdd: Double) {
        _uiState.update { state ->
            val newTotal = state.totalAmount + amountToAdd
            val buffer = if (state.activeKeyboardTarget is BudgetKeyboardTarget.Ceiling) newTotal.toLong().toString() else state.numericInputBuffer
            state.copy(
                totalAmount = newTotal,
                numericInputBuffer = buffer
            )
        }
        recalculate()
    }

    fun onNameChanged(newName: String) {
        val sanitized = newName.take(50)
        _uiState.update { it.copy(name = sanitized) }
        recalculate()
    }

    fun onPeriodChanged(newPeriod: String) {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val (start, end) = when (newPeriod) {
            "WEEKLY" -> {
                val weekStart = today
                val weekEnd = today.plusDays(7)
                Pair(weekStart.format(formatter), weekEnd.format(formatter))
            }
            "MONTHLY" -> {
                val monthStart = today.withDayOfMonth(1)
                val monthEnd = today.withDayOfMonth(today.lengthOfMonth())
                Pair(monthStart.format(formatter), monthEnd.format(formatter))
            }
            else -> {
                Pair(_uiState.value.startDate, _uiState.value.endDate)
            }
        }

        _uiState.update {
            it.copy(
                period = newPeriod,
                startDate = start,
                endDate = end
            )
        }
    }

    fun onStartDateChanged(date: String) {
        _uiState.update { it.copy(startDate = date) }
    }

    fun onEndDateChanged(date: String) {
        _uiState.update { it.copy(endDate = date) }
    }

    fun onDescriptionChanged(newDesc: String) {
        val sanitized = newDesc.take(200)
        _uiState.update { it.copy(description = sanitized) }
    }

    fun onColorChanged(colorHex: String) {
        _uiState.update { it.copy(colorHex = colorHex) }
    }

    fun toggleAccordion(section: String) {
        _uiState.update { state ->
            val set = state.expandedAccordions.toMutableSet()
            if (set.contains(section)) {
                set.remove(section)
            } else {
                set.add(section)
            }
            state.copy(expandedAccordions = set)
        }
    }

    fun openKeyboard(target: BudgetKeyboardTarget) {
        val initialAmount = when (target) {
            is BudgetKeyboardTarget.Ceiling -> _uiState.value.totalAmount
            is BudgetKeyboardTarget.Envelope -> {
                _uiState.value.items.find { it.id == target.envelopeId }?.allocatedAmount ?: 0.0
            }
        }
        val buffer = if (initialAmount > 0.0) initialAmount.toLong().toString() else ""
        _uiState.update {
            it.copy(
                activeKeyboardTarget = target,
                numericInputBuffer = buffer
            )
        }
    }

    fun closeKeyboard() {
        _uiState.update {
            it.copy(
                activeKeyboardTarget = null,
                numericInputBuffer = ""
            )
        }
    }

    fun onDigitClick(digit: String) {
        val target = _uiState.value.activeKeyboardTarget ?: return
        val currentBuffer = _uiState.value.numericInputBuffer
        if (currentBuffer.length >= 12) return // Prevent overflow

        val newBuffer = if (currentBuffer == "0" && digit != "00") digit else currentBuffer + digit
        val parsedValue = newBuffer.toDoubleOrNull() ?: 0.0
        applyTargetValue(target, parsedValue, newBuffer)
    }

    fun onQuickAdd(amountToAdd: Double) {
        val target = _uiState.value.activeKeyboardTarget ?: return
        val currentValue = when (target) {
            is BudgetKeyboardTarget.Ceiling -> _uiState.value.totalAmount
            is BudgetKeyboardTarget.Envelope -> {
                _uiState.value.items.find { it.id == target.envelopeId }?.allocatedAmount ?: 0.0
            }
        }
        val newValue = currentValue + amountToAdd
        val newBuffer = newValue.toLong().toString()
        applyTargetValue(target, newValue, newBuffer)
    }

    fun onBackspace() {
        val target = _uiState.value.activeKeyboardTarget ?: return
        val currentBuffer = _uiState.value.numericInputBuffer
        val newBuffer = if (currentBuffer.isNotEmpty()) currentBuffer.dropLast(1) else ""
        val parsedValue = newBuffer.toDoubleOrNull() ?: 0.0
        applyTargetValue(target, parsedValue, newBuffer)
    }

    fun onClear() {
        val target = _uiState.value.activeKeyboardTarget ?: return
        applyTargetValue(target, 0.0, "")
    }

    private fun applyTargetValue(target: BudgetKeyboardTarget, value: Double, buffer: String) {
        _uiState.update { state ->
            when (target) {
                is BudgetKeyboardTarget.Ceiling -> {
                    state.copy(
                        totalAmount = value,
                        numericInputBuffer = buffer
                    )
                }
                is BudgetKeyboardTarget.Envelope -> {
                    val updatedItems = state.items.map { item ->
                        if (item.id == target.envelopeId) {
                            item.copy(allocatedAmount = value)
                        } else {
                            item
                        }
                    }
                    state.copy(
                        items = updatedItems,
                        numericInputBuffer = buffer
                    )
                }
            }
        }
        recalculate()
    }

    fun addCategoryEnvelope(category: CategoryEntity, defaultAmount: Double = 0.0) {
        _uiState.update { state ->
            if (state.items.any { it.categoryId == category.id }) {
                state
            } else {
                val newItem = BudgetItemInput(
                    categoryId = category.id,
                    categoryName = category.name,
                    categoryIcon = category.icon,
                    categoryColorHex = category.colorHex,
                    allocatedAmount = defaultAmount
                )
                state.copy(items = state.items + newItem)
            }
        }
        recalculate()
    }

    fun onQuickAddEnvelopeAmount(envelopeId: String, amountToAdd: Double) {
        _uiState.update { state ->
            val updatedItems = state.items.map { item ->
                if (item.id == envelopeId) {
                    val newAmount = item.allocatedAmount + amountToAdd
                    item.copy(allocatedAmount = newAmount)
                } else {
                    item
                }
            }
            state.copy(items = updatedItems)
        }
        recalculate()
    }

    fun removeCategoryEnvelope(envelopeId: String) {
        _uiState.update { state ->
            if (state.activeKeyboardTarget is BudgetKeyboardTarget.Envelope &&
                (state.activeKeyboardTarget as BudgetKeyboardTarget.Envelope).envelopeId == envelopeId
            ) {
                state.copy(
                    items = state.items.filterNot { it.id == envelopeId },
                    activeKeyboardTarget = null,
                    numericInputBuffer = ""
                )
            } else {
                state.copy(items = state.items.filterNot { it.id == envelopeId })
            }
        }
        recalculate()
    }

    fun openCreateCategoryDialog(open: Boolean) {
        _uiState.update { it.copy(isCreateCategoryDialogOpen = open) }
    }

    fun createCustomCategory(name: String, colorHex: String, icon: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newCategory = financialRepository.createCategory(
                name = name.trim(),
                colorHex = colorHex,
                icon = icon,
                userId = "user_default",
                type = "EXPENSE"
            )
            addCategoryEnvelope(newCategory, 0.0)
            _uiState.update { it.copy(isCreateCategoryDialogOpen = false) }
        }
    }

    private fun recalculate() {
        val state = _uiState.value
        val summary = calculateSummaryUseCase(
            name = state.name,
            totalAmount = state.totalAmount,
            envelopeAmounts = state.items.map { it.allocatedAmount }
        )

        _uiState.update {
            it.copy(
                allocatedAmount = summary.allocatedAmount,
                unassignedAmount = summary.unassignedAmount,
                progressPercentage = summary.progressPercentage,
                isOverbudget = summary.isOverbudget,
                isZeroBasedBalanced = summary.isZeroBasedBalanced,
                ruleTitle = summary.ruleTitle,
                ruleMessage = summary.ruleMessage,
                ruleBannerColorHex = summary.ruleBannerColorHex,
                isFormSubmittable = summary.isFormSubmittable
            )
        }
    }

    fun saveBudget() {
        val state = _uiState.value
        if (state.isSaving) return

        val validation = com.example.domain.validation.FormValidator.validateBudget(
            name = state.name,
            totalAmount = state.totalAmount,
            envelopesTotal = state.allocatedAmount
        )
        if (validation is com.example.domain.validation.FormValidator.ValidationResult.Invalid) {
            _uiState.update { it.copy(errorMessage = validation.firstError) }
            return
        }

        val cleanName = com.example.domain.validation.FormValidator.sanitizeConcept(state.name)
        val cleanDescription = com.example.domain.validation.FormValidator.sanitizeDescription(state.description)
        val cleanColorHex = com.example.domain.validation.FormValidator.sanitizeColorHex(state.colorHex, "#10B981")

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val budgetId = UUID.randomUUID().toString()
            val budget = BudgetEntity(
                id = budgetId,
                userId = "user_default",
                name = cleanName,
                period = state.period,
                startDate = state.startDate,
                endDate = state.endDate,
                totalAmount = state.totalAmount,
                description = cleanDescription.takeIf { it.isNotBlank() },
                colorHex = cleanColorHex,
                isActive = true
            )

            val items = state.items.map { item ->
                BudgetItemEntity(
                    id = UUID.randomUUID().toString(),
                    budgetId = budgetId,
                    categoryId = item.categoryId,
                    categoryName = com.example.domain.validation.FormValidator.sanitizeConcept(item.categoryName),
                    categoryIcon = item.categoryIcon,
                    categoryColorHex = com.example.domain.validation.FormValidator.sanitizeColorHex(item.categoryColorHex),
                    allocatedAmount = item.allocatedAmount,
                    spentAmount = 0.0
                )
            }

            val result = financialRepository.createBudget(budget, items)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                },
                onFailure = { ex ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = ex.message ?: "Error al guardar el presupuesto.") }
                }
            )
        }
    }

    companion object {
        fun provideFactory(financialRepository: FinancialRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BudgetFormViewModel(financialRepository) as T
                }
            }
    }
}
