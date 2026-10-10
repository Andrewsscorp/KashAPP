package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 1-to-Many relation between Budget header and its category envelopes.
 */
data class BudgetWithItems(
    @Embedded
    val budget: BudgetEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "budgetId"
    )
    val items: List<BudgetItemEntity>
) {
    val totalAllocated: Double
        get() = items.sumOf { it.allocatedAmount }

    val totalSpent: Double
        get() = items.sumOf { it.spentAmount }

    val remainingUnallocated: Double
        get() = budget.totalAmount - totalAllocated

    val isOverbudget: Boolean
        get() = totalAllocated > budget.totalAmount

    val allocatedPercentage: Float
        get() = if (budget.totalAmount > 0) {
            ((totalAllocated / budget.totalAmount) * 100.0).toFloat().coerceIn(0f, 200f)
        } else 0f
}
