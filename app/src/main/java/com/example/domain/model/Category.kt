package com.example.domain.model

import androidx.compose.ui.graphics.Color
import com.example.core.text.TextNormalizer

/**
 * Pure domain model for financial categorization (SSOT).
 * Uncoupled from database/Room frameworks.
 */
data class Category(
    val id: String,
    val name: String,
    val nameNormalized: String = TextNormalizer.normalize(name),
    val icon: String = "category",
    val colorHex: String = "#0284C7",
    val type: CategoryType = CategoryType.EXPENSE,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val userId: String? = null
) {
    val displayColor: Color
        get() = try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (_: Exception) {
            Color(0xFF0284C7)
        }
}

enum class CategoryType {
    EXPENSE,
    INCOME,
    BOTH;

    companion object {
        fun fromString(value: String): CategoryType = when (value.uppercase()) {
            "INCOME" -> INCOME
            "BOTH" -> BOTH
            else -> EXPENSE
        }
    }
}
