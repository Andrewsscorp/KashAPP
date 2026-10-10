package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a Budget Header (Presupuesto) in KashApp.
 * 
 * Supports:
 * - Ceiling amount (total_amount)
 * - Temporal bounds (startDate to endDate)
 * - Period cycles (MONTHLY, WEEKLY, CUSTOM)
 * - Category envelopes linking with zero-based budgeting enforcement
 */
@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["startDate", "endDate"]),
        Index(value = ["isActive"])
    ]
)
data class BudgetEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val period: String = "MONTHLY", // "MONTHLY", "WEEKLY", "CUSTOM"
    val startDate: String, // e.g. "2026-10-01"
    val endDate: String,   // e.g. "2026-10-31"
    val totalAmount: Double,
    val description: String? = null,
    val colorHex: String = "#10B981",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
