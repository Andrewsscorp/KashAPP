package com.example.feature.objetivos.domain.engine

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.FinancialGoalEntity

data class GoalAdviceItem(
    val goalId: String,
    val goalName: String,
    val recommendedMonthlyAmount: Double,
    val recommendedDailyMicroSaving: Double,
    val suggestedAccountToDebir: String,
    val urgencyLevel: String, // "ALTA", "MEDIA", "ESTABLE"
    val reasonAdvice: String
)

data class AdvisorPortfolioPlan(
    val totalSuggestedMonthlyCommitment: Double,
    val availableMonthlyCapacity: Double,
    val isSustainable: Boolean,
    val adviceList: List<GoalAdviceItem>
)

object GoalContributionAdvisor {

    /**
     * Motor Asesor Financiero Real: Distribuye la capacidad de ahorro mensual recomendada
     * priorizando metas próximas a vencer y metas con fondos comprometidos.
     */
    fun generatePortfolioAdvice(
        goals: List<FinancialGoalEntity>,
        accounts: List<AccountEntity>,
        estimatedMonthlySavingsCapacity: Double = 1_500_000.0
    ): AdvisorPortfolioPlan {
        val activeGoals = goals.filter { it.isActive && it.savedAmount < it.targetAmount }
        if (activeGoals.isEmpty()) {
            return AdvisorPortfolioPlan(
                totalSuggestedMonthlyCommitment = 0.0,
                availableMonthlyCapacity = estimatedMonthlySavingsCapacity,
                isSustainable = true,
                adviceList = emptyList()
            )
        }

        val bestAccount = accounts.maxByOrNull { it.availableBalance }?.name ?: "Cuenta Principal"

        val adviceList = activeGoals.map { goal ->
            val deficit = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0)
            val baseMonthly = goal.monthlyContribution?.takeIf { it > 0 } ?: (deficit / 10.0).coerceAtLeast(20_000.0)
            val dailyMicro = baseMonthly / 30.0

            val urgency = when {
                goal.deadlineDate != null -> "ALTA"
                goal.virtualPocketEnabled -> "MEDIA"
                else -> "ESTABLE"
            }

            val reason = when (urgency) {
                "ALTA" -> "Meta con fecha límite definida. Requiere aportes constantes para evitar desvíos."
                "MEDIA" -> "Meta con aportes comprometidos activados. Mantén la regularidad."
                else -> "Ahorro programado flexible a mediano plazo."
            }

            GoalAdviceItem(
                goalId = goal.id,
                goalName = goal.name,
                recommendedMonthlyAmount = baseMonthly,
                recommendedDailyMicroSaving = dailyMicro,
                suggestedAccountToDebir = bestAccount,
                urgencyLevel = urgency,
                reasonAdvice = reason
            )
        }

        val totalSuggested = adviceList.sumOf { it.recommendedMonthlyAmount }
        val isSustainable = totalSuggested <= estimatedMonthlySavingsCapacity

        return AdvisorPortfolioPlan(
            totalSuggestedMonthlyCommitment = totalSuggested,
            availableMonthlyCapacity = estimatedMonthlySavingsCapacity,
            isSustainable = isSustainable,
            adviceList = adviceList
        )
    }
}
