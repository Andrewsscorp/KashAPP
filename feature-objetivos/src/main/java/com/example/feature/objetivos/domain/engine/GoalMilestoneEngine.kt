package com.example.feature.objetivos.domain.engine

import com.example.data.local.entity.FinancialGoalEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class GoalMilestone(
    val percentage: Int, // 25, 50, 75, 100
    val targetAmount: Double,
    val isAchieved: Boolean,
    val description: String,
    val badgeIconName: String
)

data class GoalVelocityMetrics(
    val goalId: String,
    val goalName: String,
    val progressPercentage: Double,
    val milestones: List<GoalMilestone>,
    val nextMilestonePercentage: Int,
    val amountToNextMilestone: Double,
    val velocityStatus: String, // "ADELANTADO", "EN_RITMO", "REZAGADO", "COMPLETADO"
    val daysElapsed: Long,
    val expectedProgressPercentage: Double
)

object GoalMilestoneEngine {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Evalúa los hitos alcanzados y la velocidad de ahorro para un objetivo.
     */
    fun evaluateMilestones(goal: FinancialGoalEntity): GoalVelocityMetrics {
        val target = goal.targetAmount
        val saved = goal.savedAmount
        val currentPct = if (target > 0) ((saved / target) * 100.0).coerceIn(0.0, 100.0) else 0.0

        val milestonePercentages = listOf(25, 50, 75, 100)
        val milestoneList = milestonePercentages.map { pct ->
            val milestoneTarget = target * (pct / 100.0)
            val achieved = saved >= milestoneTarget
            val desc = when (pct) {
                25 -> "Primer cuarto completado. Base inicial consolidada."
                50 -> "Mitad del camino. El hábito de ahorro está firme."
                75 -> "Tramo final. La meta está al alcance."
                100 -> "¡Meta 100% alcanzada! Logro financiero cumplido."
                else -> "Hito financiero"
            }
            val icon = when (pct) {
                25 -> "flag"
                50 -> "emoji_events"
                75 -> "rocket_launch"
                100 -> "verified"
                else -> "star"
            }
            GoalMilestone(
                percentage = pct,
                targetAmount = milestoneTarget,
                isAchieved = achieved,
                description = desc,
                badgeIconName = icon
            )
        }

        val nextMilestone = milestoneList.firstOrNull { !it.isAchieved }
        val nextPct = nextMilestone?.percentage ?: 100
        val amountToNext = if (nextMilestone != null) {
            (nextMilestone.targetAmount - saved).coerceAtLeast(0.0)
        } else 0.0

        // Cálculo de velocidad vs cronograma
        var expectedPct = 0.0
        var daysElapsed = 0L
        var velocity = "EN_RITMO"

        val sDate = goal.startDate
        val dDate = goal.deadlineDate

        if (sDate != null && dDate != null) {
            try {
                val start = LocalDate.parse(sDate.take(10), dateFormatter)
                val deadline = LocalDate.parse(dDate.take(10), dateFormatter)
                val today = LocalDate.now()

                val totalDays = ChronoUnit.DAYS.between(start, deadline).coerceAtLeast(1)
                daysElapsed = ChronoUnit.DAYS.between(start, today).coerceAtLeast(0)

                expectedPct = ((daysElapsed.toDouble() / totalDays.toDouble()) * 100.0).coerceIn(0.0, 100.0)

                velocity = when {
                    currentPct >= 100.0 -> "COMPLETADO"
                    currentPct >= expectedPct + 5.0 -> "ADELANTADO"
                    currentPct <= expectedPct - 15.0 -> "REZAGADO"
                    else -> "EN_RITMO"
                }
            } catch (_: Exception) {
                velocity = if (currentPct >= 100.0) "COMPLETADO" else "EN_RITMO"
            }
        } else {
            velocity = if (currentPct >= 100.0) "COMPLETADO" else "EN_RITMO"
        }

        return GoalVelocityMetrics(
            goalId = goal.id,
            goalName = goal.name,
            progressPercentage = currentPct,
            milestones = milestoneList,
            nextMilestonePercentage = nextPct,
            amountToNextMilestone = amountToNext,
            velocityStatus = velocity,
            daysElapsed = daysElapsed,
            expectedProgressPercentage = expectedPct
        )
    }
}
