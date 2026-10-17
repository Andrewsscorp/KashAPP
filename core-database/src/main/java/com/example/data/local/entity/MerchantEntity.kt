package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing canonical commercial merchants and stores with automatic classification.
 */
@Entity(
    tableName = "merchants",
    indices = [
        Index(value = ["canonicalName"]),
        Index(value = ["alias"])
    ]
)
data class MerchantEntity(
    @PrimaryKey
    val id: String,
    val userId: String? = null,
    val canonicalName: String,
    val rawDescription: String = "",
    val alias: String = "",
    val defaultCategoryId: String? = null,
    val defaultCategoryName: String? = null,
    val classificationConfidence: Int = 90,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
