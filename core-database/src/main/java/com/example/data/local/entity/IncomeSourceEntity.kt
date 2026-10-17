package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an income source (e.g. "Salario", "Venta de auto", "Honorarios"),
 * owned by a specific user with logical activation flag.
 */
@Entity(
    tableName = "income_sources",
    indices = [Index(value = ["userId"])]
)
data class IncomeSourceEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
