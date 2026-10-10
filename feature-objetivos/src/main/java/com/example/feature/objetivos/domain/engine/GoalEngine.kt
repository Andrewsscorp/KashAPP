package com.example.feature.objetivos.domain.engine

import com.example.data.local.entity.FinancialGoalEntity
import com.example.feature.objetivos.domain.model.GoalDetailMetric
import com.example.feature.objetivos.domain.model.GoalStatus
import com.example.feature.objetivos.domain.model.GoalSummaryOverview
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max

object GoalEngine {

    private val supportedDatePatterns = listOf(
        "dd/MM/yyyy",
        "yyyy-MM-dd",
        "d/M/yyyy",
        "dd-MM-yyyy"
    )

    fun parseDateSafely(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        val trimmed = dateStr.trim()
        for (pattern in supportedDatePatterns) {
            try {
                return LocalDate.parse(trimmed, DateTimeFormatter.ofPattern(pattern))
            } catch (_: Exception) {}
        }
        return null
    }

    fun computeSummary(
        goals: List<FinancialGoalEntity>,
        baseCurrencyCode: String = "COP",
        exchangeRateUpdated: String? = null
    ): GoalSummaryOverview {
        // According to specification:
        // El resumen financiero debe calcularse con los objetivos activos y sus aportes válidos.
        // Los objetivos completados, cancelados o archivados deben tratarse sin alterar artificialmente el progreso actual.
        val activeGoals = goals.filter { it.isActive && it.savedAmount < it.targetAmount }
        val completedGoals = goals.filter { it.isActive && it.savedAmount >= it.targetAmount }

        val totalTarget = activeGoals.sumOf { max(0.0, it.targetAmount) }
        val totalSaved = activeGoals.sumOf { max(0.0, it.savedAmount) }
        val totalPending = max(0.0, totalTarget - totalSaved)

        val globalProgress = if (totalTarget > 0.0) {
            ((totalSaved / totalTarget) * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }

        val atRiskCount = activeGoals.count { goal ->
            determineStatus(goal) == GoalStatus.AT_RISK
        }

        return GoalSummaryOverview(
            totalTarget = totalTarget,
            totalSaved = totalSaved,
            totalPending = totalPending,
            globalProgressPercentage = globalProgress,
            activeGoalsCount = activeGoals.size,
            completedGoalsCount = completedGoals.size,
            atRiskGoalsCount = atRiskCount,
            baseCurrencyCode = baseCurrencyCode,
            exchangeRateLastUpdated = exchangeRateUpdated
        )
    }

    fun determineStatus(goal: FinancialGoalEntity): GoalStatus {
        if (!goal.isActive) {
            return GoalStatus.ARCHIVED
        }
        if (goal.targetAmount > 0 && goal.savedAmount >= goal.targetAmount) {
            return GoalStatus.COMPLETED
        }

        val pct = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount) * 100.0 else 0.0
        if (pct >= 85.0) {
            return GoalStatus.NEAR_COMPLETION
        }

        if (goal.hasDeadline && !goal.deadlineDate.isNullOrBlank()) {
            val deadline = parseDateSafely(goal.deadlineDate)
            if (deadline != null) {
                val today = LocalDate.now()
                val daysLeft = ChronoUnit.DAYS.between(today, deadline)
                val startDate = parseDateSafely(goal.startDate) ?: today.minusMonths(1)
                val totalDays = max(1L, ChronoUnit.DAYS.between(startDate, deadline))
                val elapsedDays = max(0L, ChronoUnit.DAYS.between(startDate, today))
                val expectedPct = ((elapsedDays.toDouble() / totalDays.toDouble()) * 100.0).coerceIn(0.0, 100.0)

                // Overdue or significantly behind timeline
                if (daysLeft < 0 || (expectedPct > 30.0 && pct < (expectedPct * 0.6))) {
                    return GoalStatus.AT_RISK
                }
            }
        }

        return GoalStatus.IN_PROGRESS
    }

    fun mapToDetailMetric(goal: FinancialGoalEntity): GoalDetailMetric {
        val target = max(0.0, goal.targetAmount)
        val saved = max(0.0, goal.savedAmount)
        val pending = max(0.0, target - saved)
        val progressPct = if (target > 0.0) ((saved / target) * 100.0).coerceIn(0.0, 100.0) else 0.0

        val today = LocalDate.now()
        val deadline = parseDateSafely(goal.deadlineDate)
        val daysRemaining = if (goal.hasDeadline && deadline != null) {
            ChronoUnit.DAYS.between(today, deadline)
        } else null

        val isOverdue = daysRemaining != null && daysRemaining < 0

        // Accurate monthly savings needed
        val monthlyRequired = if (isOverdue || pending <= 0.0) {
            0.0
        } else if (goal.hasDeadline && deadline != null) {
            val months = max(1L, ChronoUnit.MONTHS.between(today, deadline))
            ceil(pending / months.toDouble())
        } else {
            ceil(pending / 12.0)
        }

        val status = determineStatus(goal)

        // Estimated completion date based on monthlyContribution or required savings
        val effectiveMonthlyRate = (goal.monthlyContribution ?: 0.0).takeIf { it > 0.0 } ?: (goal.savedAmount / 2.0).takeIf { it > 0.0 }
        val estimatedCompletionDate = if (pending <= 0.0) {
            "Meta alcanzada"
        } else if (effectiveMonthlyRate != null && effectiveMonthlyRate > 0.0) {
            val neededMonths = ceil(pending / effectiveMonthlyRate).toLong()
            today.plusMonths(neededMonths).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        } else {
            null
        }

        val nextMilestone = when {
            progressPct < 25.0 -> 25
            progressPct < 50.0 -> 50
            progressPct < 75.0 -> 75
            progressPct < 100.0 -> 100
            else -> 100
        }
        val nextMilestoneAmount = (target * (nextMilestone / 100.0))

        return GoalDetailMetric(
            id = goal.id,
            name = goal.name,
            categoryName = goal.categoryName.ifBlank { "Ahorro general" },
            priority = when {
                goal.targetAmount >= 10_000_000 -> "Alta"
                goal.targetAmount >= 3_000_000 -> "Media"
                else -> "Normal"
            },
            description = goal.description,
            colorHex = goal.colorHex.ifBlank { "#0284C7" },
            iconName = goal.iconName.ifBlank { "flag" },
            targetAmount = target,
            savedAmount = saved,
            pendingAmount = pending,
            progressPercentage = progressPct,
            hasDeadline = goal.hasDeadline,
            startDate = goal.startDate,
            deadlineDate = goal.deadlineDate,
            daysRemaining = daysRemaining,
            isOverdue = isOverdue,
            monthlyRequiredSaving = monthlyRequired,
            averageContribution = (goal.monthlyContribution ?: (if (saved > 0) saved / 3.0 else 0.0)),
            nextMilestonePercentage = nextMilestone,
            nextMilestoneAmount = nextMilestoneAmount,
            estimatedCompletionDate = estimatedCompletionDate,
            targetAccountId = goal.targetAccountId,
            targetAccountName = goal.targetAccountName,
            sourceAccountId = goal.sourceAccountId,
            sourceAccountName = goal.sourceAccountName,
            autoContribute = goal.autoContribute,
            monthlyContribution = goal.monthlyContribution,
            contributionDay = goal.contributionDay,
            contributionFrequency = goal.contributionFrequency,
            contributionMethod = goal.contributionMethod,
            virtualPocketEnabled = goal.virtualPocketEnabled,
            status = status,
            isActive = goal.isActive,
            createdAt = goal.createdAt
        )
    }
}
