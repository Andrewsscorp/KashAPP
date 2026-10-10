package com.example.presentation.goals.state

/**
 * State representing the Goal Creation / Mental Accounting Virtual Pocket Form.
 */
data class GoalFormState(
    // Accordion Expansion states (all collapsed by default)
    val isBasicInfoExpanded: Boolean = false,
    val isFinancialTargetExpanded: Boolean = false,
    val isTimelineExpanded: Boolean = false,
    val isSavingPlanExpanded: Boolean = false,
    val isFundingSourceExpanded: Boolean = false,

    // 1. Información Básica
    val name: String = "",
    val categoryName: String = "Educación",
    val categoryId: String? = null,
    val description: String = "",
    val selectedIconName: String = "school",
    val selectedColorHex: String = "#8B5CF6", // Violet / Purple from design

    // 2. Meta Financiera
    val targetAmount: Double = 0.0,
    val savedAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val progressPercentage: Int = 0,

    // 3. Fecha Objetivo
    val hasDeadline: Boolean = true,
    val deadlineDate: String = "",
    val startDate: String = "",
    val remainingMonths: Int = 0,

    // 4. Plan de Ahorro
    val monthlyContribution: Double = 0.0,
    val frequency: String = "Mensual",
    val targetAccountId: String = "",
    val targetAccountName: String = "",
    val autoContribute: Boolean = false,
    val virtualPocketEnabled: Boolean = true,
    val contributionDay: Int = 15,

    // 5. Fuente del Objetivo
    val sourceAccountId: String = "",
    val sourceAccountName: String = "",
    val contributionMethod: String = "Transferencia",

    // Rule Engine Validation
    val isRuleEngineValid: Boolean = true,
    val ruleEngineTitle: String = "¡Estás más cerca de tu meta!",
    val ruleEngineMessage: String = "Con un plan constante, puedes lograrlo.",

    // In-App Keyboard
    val activeKeyboardTarget: String? = null, // "TARGET_AMOUNT", "SAVED_AMOUNT", "MONTHLY_CONTRIBUTION"

    // Accounts available from Room
    val availableAccounts: List<com.example.data.local.entity.AccountEntity> = emptyList(),

    // Async state
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
) {
    val nameLength: Int get() = name.length
    val descriptionLength: Int get() = description.length
    val isNameValid: Boolean get() = name.isNotBlank() && name.length <= 50
    val isTargetAmountValid: Boolean get() = targetAmount > 0.0
    val isFormSubmittable: Boolean get() = isNameValid && isTargetAmountValid && !isSaving
}
