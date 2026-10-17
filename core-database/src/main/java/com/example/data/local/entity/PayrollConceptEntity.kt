package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing habitual salary/payroll concepts (e.g. Salario Ordinario, Quincena,
 * Honorarios, Bonificación, Prima, etc.).
 * Allows users to choose from standard concepts or create and persist custom ones in SQLite/Room.
 */
@Entity(tableName = "payroll_concepts")
data class PayrollConceptEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
