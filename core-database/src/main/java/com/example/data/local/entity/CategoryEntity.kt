package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing expense and income classification categories.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val userId: String? = null, // null for system default categories
    val name: String,
    val icon: String = "category",
    val colorHex: String = "#EF4444",
    val type: String = "EXPENSE", // "EXPENSE", "INCOME", "BOTH"
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
