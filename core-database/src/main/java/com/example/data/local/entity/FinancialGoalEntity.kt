package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a Financial Goal / Mental Accounting Virtual Pocket.
 * 
 * Supports:
 * - Dynamic deficit calculation
 * - Virtual Pocket (funds freezing in account's available_balance)
 * - Auto-contribution schedule
 * - Categorization, custom institution colors and icons
 */
@Entity(
    tableName = "financial_goals",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["targetAccountId"]),
        Index(value = ["isActive"])
    ]
)
data class FinancialGoalEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val categoryId: String? = null,
    val categoryName: String = "Educación",
    val description: String? = null,
    val colorHex: String = "#8B5CF6",
    val iconName: String = "school",
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val hasDeadline: Boolean = true,
    val startDate: String? = null,
    val deadlineDate: String? = null,
    val targetAccountId: String,
    val targetAccountName: String,
    val sourceAccountId: String? = null,
    val sourceAccountName: String? = null,
    val autoContribute: Boolean = false,
    val monthlyContribution: Double? = null,
    val contributionDay: Int? = 15,
    val contributionFrequency: String = "Mensual",
    val contributionMethod: String = "Transferencia",
    val virtualPocketEnabled: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
