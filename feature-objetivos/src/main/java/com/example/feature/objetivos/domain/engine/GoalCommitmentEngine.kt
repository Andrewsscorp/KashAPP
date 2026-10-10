package com.example.feature.objetivos.domain.engine

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.FinancialGoalEntity
import com.example.feature.objetivos.domain.model.GoalDetailMetric

/**
 * Motor de Lógica Real de Fondos Comprometidos y Mental Accounting.
 * Regla: No bloquea la totalidad del objetivo, solo los aportes efectivamente realizados.
 * Mantiene la visibilidad total en Dashboard pero audita qué porción está comprometida.
 */
data class AccountCommitmentOverview(
    val accountId: String,
    val accountName: String,
    val totalBalance: Double,
    val committedAmount: Double,
    val freeAvailableBalance: Double,
    val commitmentRatio: Double // 0.0 to 100.0%
)

data class GlobalCommitmentSummary(
    val totalCommittedFunds: Double,
    val totalAvailableLiquidity: Double,
    val globalCommitmentRatio: Double,
    val committedGoalsCount: Int,
    val accountBreakdowns: List<AccountCommitmentOverview>
)

object GoalCommitmentEngine {

    /**
     * Calcula los fondos comprometidos por cada objetivo individual.
     * Solo los aportes hechos (savedAmount) quedan comprometidos si virtualPocketEnabled es true.
     */
    fun calculateGoalCommittedAmount(goal: FinancialGoalEntity): Double {
        return if (goal.virtualPocketEnabled && goal.isActive) {
            goal.savedAmount.coerceAtLeast(0.0)
        } else {
            0.0
        }
    }

    /**
     * Calcula el resumen global de fondos comprometidos agrupado por cuentas bancarias/efectivo.
     */
    fun computeGlobalCommitmentSummary(
        goals: List<FinancialGoalEntity>,
        accounts: List<AccountEntity>
    ): GlobalCommitmentSummary {
        val activeGoals = goals.filter { it.isActive && it.virtualPocketEnabled }
        val totalCommitted = activeGoals.sumOf { it.savedAmount.coerceAtLeast(0.0) }

        // Agrupación por cuenta objetivo/fuente
        val committedByAccount = mutableMapOf<String, Double>()
        activeGoals.forEach { g ->
            val accId = g.targetAccountId.ifBlank { "acc_default" }
            val current = committedByAccount.getOrDefault(accId, 0.0)
            committedByAccount[accId] = current + g.savedAmount.coerceAtLeast(0.0)
        }

        val accountBreakdowns = accounts.map { acc ->
            val committed = committedByAccount.getOrDefault(acc.id, 0.0)
            val free = (acc.availableBalance - committed).coerceAtLeast(0.0)
            val ratio = if (acc.balance > 0) {
                ((committed / acc.balance) * 100.0).coerceIn(0.0, 100.0)
            } else 0.0

            AccountCommitmentOverview(
                accountId = acc.id,
                accountName = acc.name,
                totalBalance = acc.balance,
                committedAmount = committed,
                freeAvailableBalance = free,
                commitmentRatio = ratio
            )
        }

        val totalLiquidity = accounts.sumOf { it.balance.coerceAtLeast(0.0) }
        val globalRatio = if (totalLiquidity > 0) {
            ((totalCommitted / totalLiquidity) * 100.0).coerceIn(0.0, 100.0)
        } else 0.0

        return GlobalCommitmentSummary(
            totalCommittedFunds = totalCommitted,
            totalAvailableLiquidity = totalLiquidity,
            globalCommitmentRatio = globalRatio,
            committedGoalsCount = activeGoals.size,
            accountBreakdowns = accountBreakdowns
        )
    }

    /**
     * Valida si un nuevo aporte puede ser comprometido sin desfinanciar el saldo disponible real.
     */
    fun canCommitContribution(
        account: AccountEntity?,
        additionalAmount: Double
    ): Boolean {
        if (account == null) return true
        return account.availableBalance >= additionalAmount
    }
}
