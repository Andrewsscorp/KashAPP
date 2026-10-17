package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing transaction tags/labels.
 */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val colorHex: String = "#0284C7",
    val usageCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)
