package com.example.feature.objetivos

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.FinancialGoalEntity
import com.example.feature.objetivos.domain.engine.GoalCommitmentEngine
import com.example.feature.objetivos.domain.engine.GoalEngine
import com.example.feature.objetivos.domain.engine.GoalMilestoneEngine
import com.example.feature.objetivos.domain.engine.GoalProjectionEngine
import com.example.feature.objetivos.domain.engine.ProjectionScenario
import com.example.feature.objetivos.domain.model.GoalStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalEnginesCoverageTest {

    @Test
    fun goalEngine_parsesSupportedDatesAndRejectsInvalidDates() {
        assertEquals(LocalDate.of(2026, 10, 12), GoalEngine.parseDateSafely("12/10/2026"))
        assertEquals(LocalDate.of(2026, 10, 12), GoalEngine.parseDateSafely("2026-10-12"))
        assertEquals(LocalDate.of(2026, 10, 12), GoalEngine.parseDateSafely("1/10/2026")?.withDayOfMonth(12))
        assertEquals(LocalDate.of(2026, 10, 12), GoalEngine.parseDateSafely("12-10-2026"))
        assertEquals(null, GoalEngine.parseDateSafely("invalid"))
        assertEquals(null, GoalEngine.parseDateSafely(null))
    }

    @Test
    fun goalEngine_computesStatusesSummaryAndDetailMetrics() {
        val active = goal(id = "active", target = 1_000.0, saved = 250.0)
        val completed = goal(id = "done", target = 1_000.0, saved = 1_000.0)
        val archived = goal(id = "archived", target = 1_000.0, saved = 100.0, isActive = false)
        assertEquals(GoalStatus.IN_PROGRESS, GoalEngine.determineStatus(active))
        assertEquals(GoalStatus.COMPLETED, GoalEngine.determineStatus(completed))
        assertEquals(GoalStatus.ARCHIVED, GoalEngine.determineStatus(archived))

        val summary = GoalEngine.computeSummary(listOf(active, completed, archived), "COP", "today")
        assertEquals(1, summary.activeGoalsCount)
        assertEquals(1, summary.completedGoalsCount)
        assertEquals(1_000.0, summary.totalTarget, 0.001)
        assertEquals(250.0, summary.totalSaved, 0.001)
        assertEquals(750.0, summary.totalPending, 0.001)
        assertEquals(25.0, summary.globalProgressPercentage, 0.001)
        assertEquals("today", summary.exchangeRateLastUpdated)

        val metric = GoalEngine.mapToDetailMetric(goal(target = 10_000_000.0, saved = 2_500_000.0, monthlyContribution = 500_000.0))
        assertEquals(25.0, metric.progressPercentage, 0.001)
        assertEquals(7_500_000.0, metric.pendingAmount, 0.001)
        assertEquals(50, metric.nextMilestonePercentage)
        assertEquals("Alta", metric.priority)
        assertNotNull(metric.estimatedCompletionDate)
    }

    @Test
    fun goalProjectionEngine_generatesAllScenariosAndCompoundBenefit() {
        val analysis = GoalProjectionEngine.analyzeGoal(
            goal(target = 12_000_000.0, saved = 2_000_000.0, monthlyContribution = 1_000_000.0, deadlineDate = "2099-12-31"),
            annualInflationRate = 0.05,
            yieldRateAnnual = 0.08
        )
        assertEquals(10_000_000.0, analysis.remainingDeficit, 0.001)
        assertEquals(3, analysis.scenarios.size)
        assertEquals(ProjectionScenario.CONSERVATIVE, analysis.scenarios.first().scenario)
        assertTrue(analysis.scenarios[2].monthlyPledge > analysis.scenarios[0].monthlyPledge)
        assertTrue(analysis.compoundYieldBenefit > 0.0)
        assertTrue(analysis.scenarios.all { it.estimatedCompletionDate.isNotBlank() })
    }

    @Test
    fun goalMilestoneEngine_tracksProgressAndVelocity() {
        val metrics = GoalMilestoneEngine.evaluateMilestones(
            goal(target = 1_000.0, saved = 600.0, startDate = "2026-01-01", deadlineDate = "2026-12-31")
        )
        assertEquals(60.0, metrics.progressPercentage, 0.001)
        assertEquals(4, metrics.milestones.size)
        assertTrue(metrics.milestones[0].isAchieved)
        assertTrue(metrics.milestones[1].isAchieved)
        assertFalse(metrics.milestones[3].isAchieved)
        assertEquals(75, metrics.nextMilestonePercentage)
        assertEquals(150.0, metrics.amountToNextMilestone, 0.001)
        assertTrue(metrics.velocityStatus in setOf("ADELANTADO", "EN_RITMO", "REZAGADO"))
    }

    @Test
    fun goalCommitmentEngine_groupsActiveVirtualFundsAndValidatesLiquidity() {
        val account = AccountEntity(
            id = "account-1",
            userId = "user-1",
            name = "Ahorros",
            balance = 1_000.0,
            availableBalance = 800.0
        )
        val committed = goal(id = "goal-1", target = 1_000.0, saved = 300.0, targetAccountId = account.id, virtualPocketEnabled = true)
        val inactive = goal(id = "goal-2", target = 1_000.0, saved = 500.0, targetAccountId = account.id, virtualPocketEnabled = true, isActive = false)
        assertEquals(300.0, GoalCommitmentEngine.calculateGoalCommittedAmount(committed), 0.001)
        assertEquals(0.0, GoalCommitmentEngine.calculateGoalCommittedAmount(inactive), 0.001)
        assertTrue(GoalCommitmentEngine.canCommitContribution(account, 800.0))
        assertFalse(GoalCommitmentEngine.canCommitContribution(account, 801.0))
        assertTrue(GoalCommitmentEngine.canCommitContribution(null, 10_000.0))

        val summary = GoalCommitmentEngine.computeGlobalCommitmentSummary(listOf(committed, inactive), listOf(account))
        assertEquals(300.0, summary.totalCommittedFunds, 0.001)
        assertEquals(1, summary.committedGoalsCount)
        assertEquals(30.0, summary.globalCommitmentRatio, 0.001)
        assertEquals(500.0, summary.accountBreakdowns.single().freeAvailableBalance, 0.001)
    }

    private fun goal(
        id: String = "goal",
        target: Double,
        saved: Double,
        targetAccountId: String = "account-1",
        virtualPocketEnabled: Boolean = false,
        isActive: Boolean = true,
        startDate: String? = null,
        deadlineDate: String? = null,
        monthlyContribution: Double? = null
    ) = FinancialGoalEntity(
        id = id,
        userId = "user-1",
        name = "Meta $id",
        targetAmount = target,
        savedAmount = saved,
        targetAccountId = targetAccountId,
        targetAccountName = "Cuenta",
        virtualPocketEnabled = virtualPocketEnabled,
        isActive = isActive,
        startDate = startDate,
        deadlineDate = deadlineDate,
        monthlyContribution = monthlyContribution
    )
}
