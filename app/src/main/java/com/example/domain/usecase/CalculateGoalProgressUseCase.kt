package com.example.domain.usecase

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.roundToLong

/**
 * High-precision mathematical projection engine for Financial Goals and Mental Accounting.
 * 
 * Computes:
 * 1. Remaining deficit = targetAmount - savedAmount
 * 2. Progress percentage = (savedAmount / targetAmount) * 100
 * 3. Timeline months delta
 * 4. Auto-calculated monthly contribution
 * 5. Rule Engine health evaluation (Valid vs Warning)
 */
class CalculateGoalProgressUseCase {

    data class GoalProjectionResult(
        val remainingDeficit: Double,
        val progressPercentage: Int,
        val remainingMonths: Int,
        val recommendedMonthlyContribution: Double,
        val isRuleEngineValid: Boolean,
        val ruleEngineTitle: String,
        val ruleEngineMessage: String
    )

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    operator fun invoke(
        targetAmount: Double,
        savedAmount: Double,
        startDateStr: String?,
        deadlineDateStr: String?,
        hasDeadline: Boolean,
        userMonthlyContribution: Double? = null
    ): GoalProjectionResult {
        val deficit = maxOf(0.0, targetAmount - savedAmount)
        val progressPct = if (targetAmount > 0.0) {
            ((savedAmount / targetAmount) * 100.0).toInt().coerceIn(0, 100)
        } else 0

        val today = LocalDate.now()
        val startDate = parseDateSafely(startDateStr) ?: today
        val deadlineDate = parseDateSafely(deadlineDateStr)

        val monthsRemaining = if (hasDeadline && deadlineDate != null) {
            val fromDate = if (startDate.isAfter(today)) startDate else today
            if (deadlineDate.isAfter(fromDate)) {
                val totalMonths = ChronoUnit.MONTHS.between(fromDate, deadlineDate).toInt()
                maxOf(1, totalMonths)
            } else {
                1
            }
        } else {
            12 // Default 1 year timeline when no deadline is set
        }

        val autoContribution = if (monthsRemaining > 0 && deficit > 0) {
            ceil(deficit / monthsRemaining)
        } else {
            deficit
        }

        val actualContribution = userMonthlyContribution ?: autoContribution

        // Rule Engine Evaluation
        val totalExpectedSavings = actualContribution * monthsRemaining
        val isValid = if (deficit <= 0.0) {
            true
        } else if (!hasDeadline) {
            true
        } else {
            totalExpectedSavings >= (deficit - 100.0) // small rounding tolerance
        }

        val title = if (deficit <= 0.0) {
            "¡Meta completada!"
        } else if (isValid) {
            "¡Estás más cerca de tu meta!"
        } else {
            "Ajuste recomendado"
        }

        val message = if (deficit <= 0.0) {
            "Ya cuentas con el 100% de los fondos necesarios para este objetivo."
        } else if (isValid) {
            "Con un plan constante, puedes lograrlo."
        } else {
            "Tu plan de ahorro no cubre la meta a tiempo. Intenta aumentar tu aporte o extender la fecha."
        }

        return GoalProjectionResult(
            remainingDeficit = deficit,
            progressPercentage = progressPct,
            remainingMonths = monthsRemaining,
            recommendedMonthlyContribution = autoContribution,
            isRuleEngineValid = isValid,
            ruleEngineTitle = title,
            ruleEngineMessage = message
        )
    }

    private fun parseDateSafely(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            LocalDate.parse(dateStr.trim(), dateFormatter)
        } catch (_: Exception) {
            try {
                LocalDate.parse(dateStr.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (_: Exception) {
                null
            }
        }
    }
}
