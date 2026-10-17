package com.example.presentation.budget.state

import com.example.data.local.entity.CategoryEntity
import java.util.UUID

sealed class BudgetKeyboardTarget {
    object Ceiling : BudgetKeyboardTarget()
    data class Envelope(val envelopeId: String, val categoryName: String) : BudgetKeyboardTarget()
}

data class BudgetItemInput(
    val id: String = UUID.randomUUID().toString(),
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String = "category",
    val categoryColorHex: String = "#10B981",
    val allocatedAmount: Double = 0.0
)

data class BudgetFormState(
    val name: String = "Presupuesto mensual",
    val period: String = "MONTHLY", // "MONTHLY", "WEEKLY", "CUSTOM"
    val startDate: String = "2026-10-01",
    val endDate: String = "2026-10-31",
    val description: String = "",
    val colorHex: String = "#10B981",
    val totalAmount: Double = 0.0,
    val items: List<BudgetItemInput> = emptyList(),
    val availableCategories: List<CategoryEntity> = emptyList(),
    val availableGoals: List<com.example.data.local.entity.FinancialGoalEntity> = emptyList(),
    val expandedAccordions: Set<String> = emptySet(),
    val activeKeyboardTarget: BudgetKeyboardTarget? = null,
    val numericInputBuffer: String = "",
    val allocatedAmount: Double = 0.0,
    val unassignedAmount: Double = 0.0,
    val progressPercentage: Float = 0f,
    val isOverbudget: Boolean = false,
    val isZeroBasedBalanced: Boolean = false,
    val ruleTitle: String = "Define tu techo financiero",
    val ruleMessage: String = "",
    val ruleBannerColorHex: String = "#0284C7",
    val isFormSubmittable: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val isCreateCategoryDialogOpen: Boolean = false,
    val duplicateCategoryError: String? = null
) {
    val nameCharacterCount: Int get() = name.length
    val descriptionCharacterCount: Int get() = description.length
}
