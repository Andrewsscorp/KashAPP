package com.example.feature.objetivos.domain.engine

import com.example.data.local.entity.FinancialGoalEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.pow

enum class ProjectionScenario(val label: String, val factor: Double, val description: String) {
    CONSERVATIVE("Conservador", 0.8, "Ahorro al 80% del ritmo actual."),
    MODERATE("Moderado", 1.0, "Manteniendo el ritmo actual planificado."),
    ACCELERATED("Acelerado", 1.25, "Esfuerzo extra del +25% mensual.")
}

data class ProjectedScenarioResult(
    val scenario: ProjectionScenario,
    val monthlyPledge: Double,
    val estimatedCompletionDate: String,
    val monthsRequired: Double,
    val isAheadOfDeadline: Boolean,
    val inflationAdjustedDeficit: Double
)

data class GoalProjectionAnalysis(
    val goalId: String,
    val goalName: String,
    val targetAmount: Double,
    val currentSavedAmount: Double,
    val remainingDeficit: Double,
    val scenarios: List<ProjectedScenarioResult>,
    val compoundYieldBenefit: Double
)

object GoalProjectionEngine {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Calcula escenarios de proyección real para una meta específica.
     */
    fun analyzeGoal(
        goal: FinancialGoalEntity,
        annualInflationRate: Double = 0.045,
        yieldRateAnnual: Double = 0.08
    ): GoalProjectionAnalysis {
        val target = goal.targetAmount
        val saved = goal.savedAmount
        val deficit = (target - saved).coerceAtLeast(0.0)

        val mContrib = goal.monthlyContribution
        val dDate = goal.deadlineDate

        val baseMonthly = when {
            mContrib != null && mContrib > 0.0 -> mContrib
            dDate != null -> {
                val months = calculateMonthsToDate(dDate)
                if (months > 0.0) deficit / months else deficit
            }
            else -> deficit / 12.0
        }.coerceAtLeast(10_000.0)

        val scenarios = ProjectionScenario.entries.map { scenario ->
            val monthlyPledge = baseMonthly * scenario.factor
            val monthsNeeded = if (monthlyPledge > 0) deficit / monthlyPledge else 999.0
            val estimatedDate = LocalDate.now().plusDays((monthsNeeded * 30.4375).toLong())
            val dateStr = estimatedDate.format(dateFormatter)

            val isAhead = if (dDate != null) {
                try {
                    val deadline = LocalDate.parse(dDate.take(10), dateFormatter)
                    estimatedDate.isBefore(deadline) || estimatedDate.isEqual(deadline)
                } catch (_: Exception) {
                    true
                }
            } else true

            val years = monthsNeeded / 12.0
            val inflationAdjusted = deficit * (1.0 + annualInflationRate).pow(years)

            ProjectedScenarioResult(
                scenario = scenario,
                monthlyPledge = monthlyPledge,
                estimatedCompletionDate = dateStr,
                monthsRequired = monthsNeeded,
                isAheadOfDeadline = isAhead,
                inflationAdjustedDeficit = inflationAdjusted
            )
        }

        val avgMonths = scenarios.firstOrNull { it.scenario == ProjectionScenario.MODERATE }?.monthsRequired ?: 12.0
        val monthlyRate = (1.0 + yieldRateAnnual).pow(1.0 / 12.0) - 1.0
        var accumulatedYield = 0.0
        var currentPot = saved
        for (m in 1..avgMonths.toInt().coerceAtMost(60)) {
            currentPot += baseMonthly
            val gain = currentPot * monthlyRate
            accumulatedYield += gain
            currentPot += gain
        }

        return GoalProjectionAnalysis(
            goalId = goal.id,
            goalName = goal.name,
            targetAmount = target,
            currentSavedAmount = saved,
            remainingDeficit = deficit,
            scenarios = scenarios,
            compoundYieldBenefit = accumulatedYield
        )
    }

    private fun calculateMonthsToDate(dateStr: String): Double {
        return try {
            val target = LocalDate.parse(dateStr.take(10), dateFormatter)
            val now = LocalDate.now()
            val days = ChronoUnit.DAYS.between(now, target).coerceAtLeast(1)
            days / 30.4375
        } catch (_: Exception) {
            12.0
        }
    }
}
