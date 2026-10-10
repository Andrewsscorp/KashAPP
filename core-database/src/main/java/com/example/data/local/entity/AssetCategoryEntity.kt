package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a dedicated fixed asset category with custom colors and icons.
 */
@Entity(
    tableName = "asset_categories",
    indices = [
        Index("userId"),
        Index("name")
    ]
)
data class AssetCategoryEntity(
    @PrimaryKey
    val id: String,
    val userId: String = "user_default",
    val name: String,
    val code: String = "",
    val iconName: String = "category",
    val colorHex: String = "#0284C7",
    val isSystem: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
