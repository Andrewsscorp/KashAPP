package com.example.presentation.income.model

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.local.entity.CategoryEntity
import com.example.presentation.category.resolveCategoryIcon
import com.example.presentation.expense.ExpenseCategory
import java.time.LocalDate

/**
 * Immutable UI State for Create Income Form
 */
data class IncomeFormState(
    val rawAmountInput: String = "",
    val currencyCode: String = "COP",
    val sourceText: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val isRecurring: Boolean = false,
    val recurrenceFrequency: String = "Mensual",
    val showMoreDetails: Boolean = true,
    val additionalNotes: String = "",
    val isSavedAsFrequent: Boolean = false,
    val tagsList: List<String> = emptyList(),
    val receiptUri: Uri? = null,
    val receiptFileName: String? = null,
    val isPdfDocument: Boolean = false,
    val isSubmitting: Boolean = false
) {
    val amountDouble: Double
        get() = rawAmountInput.toDoubleOrNull() ?: 0.0

    val isAmountValid: Boolean
        get() = amountDouble > 0.0
}

/**
 * Default Income Categories Fallback
 */
val DefaultIncomeCategories = listOf(
    ExpenseCategory("cat_inc_salario", "Sueldo y Nómina", Icons.Default.Work, Color(0xFF10B981), Color(0xFFDCFCE7)),
    ExpenseCategory("cat_inc_honorarios", "Honorarios y Servicios", Icons.Default.AccountBalanceWallet, Color(0xFF0284C7), Color(0xFFE0F2FE)),
    ExpenseCategory("cat_inc_ventas", "Ventas y Negocio", Icons.Default.Store, Color(0xFFF59E0B), Color(0xFFFEF3C7)),
    ExpenseCategory("cat_inc_rendimientos", "Rendimientos e Inversiones", Icons.Default.TrendingUp, Color(0xFF8B5CF6), Color(0xFFF3E8FF)),
    ExpenseCategory("cat_inc_otros", "Otros ingresos", Icons.Default.Payments, Color(0xFF64748B), Color(0xFFE2E8F0))
)

/**
 * Merge room categories with default income categories
 */
fun buildIncomeDisplayCategories(roomCategories: List<CategoryEntity>): List<ExpenseCategory> {
    val validRoomCats = roomCategories.filter { (it.type == "INCOME" || it.type == "BOTH") && it.name.trim().isNotBlank() }
    val customCats = validRoomCats.map { entity ->
        val icon = resolveCategoryIcon(entity.icon, entity.name)
        val color = try {
            if (entity.colorHex.isNotBlank() && entity.colorHex != "#FFFFFF") {
                Color(android.graphics.Color.parseColor(entity.colorHex))
            } else Color(0xFF10B981)
        } catch (_: Exception) {
            Color(0xFF10B981)
        }
        val bgColor = color.copy(alpha = 0.15f)
        ExpenseCategory(entity.id, entity.name.trim(), icon, color, bgColor)
    }
    val customNames = customCats.map { it.name.lowercase().trim() }.toSet()
    val filteredDefaults = DefaultIncomeCategories.filter { it.name.lowercase().trim() !in customNames }
    return filteredDefaults + customCats
}
