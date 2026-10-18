package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Enterprise entity representing custom or system roles and commercial categories for third parties.
 * Supports dynamic addition of commercial roles from UI (e.g. Proveedores, Concesionarios, Distribuidores, etc.).
 */
@Entity(
    tableName = "third_party_categories",
    indices = [
        Index("userId"),
        Index("code")
    ]
)
data class ThirdPartyCategoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "user_default",
    val name: String,
    val code: String,
    val iconName: String = "Business",
    val colorHex: String = "#0284C7",
    val isSystem: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
