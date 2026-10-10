package com.example.feature.objetivos.domain.model

enum class GoalStatus(val label: String) {
    IN_PROGRESS("En progreso"),
    AT_RISK("En riesgo"),
    NEAR_COMPLETION("Próximo a cumplir"),
    COMPLETED("Completado"),
    PAUSED("Pausado"),
    ARCHIVED("Archivado")
}

enum class GoalFilterTab(val label: String) {
    ALL("Todos"),
    IN_PROGRESS("En progreso"),
    NEAR_COMPLETION("Próximos a cumplir"),
    AT_RISK("En riesgo"),
    COMPLETED("Completados"),
    ARCHIVED("Archivados")
}

enum class GoalSortOption(val label: String) {
    DEADLINE("Fecha límite"),
    PROGRESS("Progreso (%)"),
    REMAINING_AMOUNT("Monto pendiente"),
    PRIORITY("Prioridad"),
    RECENT("Más recientes")
}

data class GoalSummaryOverview(
    val totalTarget: Double,
    val totalSaved: Double,
    val totalPending: Double,
    val globalProgressPercentage: Double,
    val activeGoalsCount: Int,
    val completedGoalsCount: Int,
    val atRiskGoalsCount: Int,
    val baseCurrencyCode: String = "COP",
    val exchangeRateLastUpdated: String? = null
)

data class GoalDetailMetric(
    val id: String,
    val name: String,
    val categoryName: String,
    val priority: String = "Media",
    val description: String? = null,
    val colorHex: String,
    val iconName: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val pendingAmount: Double,
    val progressPercentage: Double,
    val hasDeadline: Boolean,
    val startDate: String?,
    val deadlineDate: String?,
    val daysRemaining: Long?,
    val isOverdue: Boolean,
    val monthlyRequiredSaving: Double,
    val averageContribution: Double,
    val nextMilestonePercentage: Int,
    val nextMilestoneAmount: Double,
    val estimatedCompletionDate: String?,
    val targetAccountId: String,
    val targetAccountName: String,
    val sourceAccountId: String?,
    val sourceAccountName: String?,
    val autoContribute: Boolean,
    val monthlyContribution: Double?,
    val contributionDay: Int?,
    val contributionFrequency: String,
    val contributionMethod: String,
    val virtualPocketEnabled: Boolean,
    val status: GoalStatus,
    val isActive: Boolean,
    val createdAt: Long
)
