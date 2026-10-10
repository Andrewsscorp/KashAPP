package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a Budget Envelope Item (Detalle del Presupuesto).
 * 
 * Supports:
 * - Allocated amount per category envelope
 * - Spent accumulator tracking real-time expenses within the budget date window
 * - Direct cascade deletion when budget header is deleted
 */
@Entity(
    tableName = "budget_items",
    foreignKeys = [
        ForeignKey(
            entity = BudgetEntity::class,
            parentColumns = ["id"],
            childColumns = ["budgetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["budgetId"]),
        Index(value = ["categoryId"])
    ]
)
data class BudgetItemEntity(
    @PrimaryKey
    val id: String,
    val budgetId: String,
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String = "category",
    val categoryColorHex: String = "#10B981",
    val allocatedAmount: Double,
    val spentAmount: Double = 0.0
)
