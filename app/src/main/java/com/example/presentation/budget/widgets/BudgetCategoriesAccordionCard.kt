package com.example.presentation.budget.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.presentation.budget.state.BudgetItemInput
import com.example.presentation.budget.state.BudgetFormState
import com.example.presentation.budget.state.BudgetKeyboardTarget
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import java.util.Locale

@Composable
fun BudgetCategoriesAccordionCard(
    state: BudgetFormState,
    onToggleExpand: () -> Unit,
    onAddCategory: (CategoryEntity) -> Unit,
    onRemoveCategory: (String) -> Unit,
    onOpenKeyboard: (BudgetKeyboardTarget) -> Unit,
    onOpenCreateCategoryDialog: () -> Unit,
    onQuickAddEnvelopeAmount: (String, Double) -> Unit,
    onAddGoal: ((com.example.data.local.entity.FinancialGoalEntity) -> Unit)? = null,
    onApply50_30_20: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isExpanded = state.expandedAccordions.contains("categories")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("budget_categories_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Accordion Header (collapsed by default)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = "Sobres de gasto",
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "2. Asignación por categorías (Sobres)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${state.items.size} categorías • $ ${String.format(Locale.GERMANY, "%,.0f", state.allocatedAmount)} asignados",
                            fontSize = 12.sp,
                            color = if (state.isOverbudget) Color(0xFFEF4444) else Color(0xFF94A3B8),
                            fontWeight = if (state.isOverbudget) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                    tint = Color(0xFF94A3B8)
                )
            }

            // Lazy Rendering
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Botón para crear nueva categoría
                    Button(
                        onClick = onOpenCreateCategoryDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_create_category_part3")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Crear nueva categoría",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Chips rápidos de categorías MDM existentes
                    Text(
                        text = "O agrega categorías existentes rápidamente:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.availableCategories.forEach { category ->
                            val isAlreadyAdded = state.items.any { it.categoryId == category.id }
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isAlreadyAdded) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF0F172A),
                                border = BorderStroke(1.dp, if (isAlreadyAdded) Color(0xFF10B981) else Color(0xFF334155)),
                                modifier = Modifier
                                    .clickable {
                                        if (!isAlreadyAdded) {
                                            onAddCategory(category)
                                        }
                                    }
                                    .testTag("chip_cat_${category.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isAlreadyAdded) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = category.name,
                                        fontSize = 12.sp,
                                        fontWeight = if (isAlreadyAdded) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isAlreadyAdded) Color(0xFF34D399) else Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }
                    }

                    if (state.availableGoals.isNotEmpty() && onAddGoal != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Vincular Metas como sobres de ahorro:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.availableGoals.forEach { goal ->
                                val virtualId = "goal_${goal.id}"
                                val isAdded = state.items.any { it.categoryId == virtualId }
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isAdded) Color(0xFF00C853).copy(alpha = 0.2f) else Color(0xFF0F172A),
                                    border = BorderStroke(1.dp, if (isAdded) Color(0xFF00E676) else Color(0xFF8B5CF6)),
                                    modifier = Modifier.clickable {
                                        if (!isAdded) onAddGoal(goal)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Savings,
                                            contentDescription = null,
                                            tint = if (isAdded) Color(0xFF00E676) else Color(0xFFA78BFA),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Meta: ${goal.name}",
                                            fontSize = 12.sp,
                                            fontWeight = if (isAdded) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isAdded) Color(0xFF00E676) else Color(0xFFE2E8F0)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (onApply50_30_20 != null && state.items.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onApply50_30_20,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)
                        ) {
                            Text("Sugerir distribución Regla 50 / 30 / 20", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Sobres de asignación (${state.items.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // No trae ninguna por defecto: si está vacío, muestra estado inicial limpio
                    if (state.items.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEDE9FE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PieChart,
                                        contentDescription = null,
                                        tint = Color(0xFF8B5CF6),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Sin categorías asignadas aún",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KashNavy
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Usa el botón \"Crear nueva categoría\" o selecciona una de las existentes para asignarle un sobre de dinero.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Envelopes List
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            state.items.forEach { item ->
                                BudgetItemEnvelopeRow(
                                    item = item,
                                    totalBudget = state.totalAmount,
                                    isKeyboardActive = state.activeKeyboardTarget is BudgetKeyboardTarget.Envelope &&
                                            (state.activeKeyboardTarget as BudgetKeyboardTarget.Envelope).envelopeId == item.id,
                                    onOpenKeyboard = { onOpenKeyboard(BudgetKeyboardTarget.Envelope(item.id, item.categoryName)) },
                                    onRemove = { onRemoveCategory(item.id) },
                                    onQuickAdd = { amount -> onQuickAddEnvelopeAmount(item.id, amount) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun BudgetItemEnvelopeRow(
    item: BudgetItemInput,
    totalBudget: Double,
    isKeyboardActive: Boolean,
    onOpenKeyboard: () -> Unit,
    onRemove: () -> Unit,
    onQuickAdd: (Double) -> Unit
) {
    val percentage = if (totalBudget > 0.0) {
        ((item.allocatedAmount / totalBudget) * 100.0).toFloat().coerceIn(0f, 100f)
    } else 0f

    val iconVector = getCategoryIconVector(item.categoryIcon)
    val parsedColor = try {
        Color(android.graphics.Color.parseColor(item.categoryColorHex))
    } catch (_: Exception) {
        Color(0xFF10B981)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isKeyboardActive) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
        border = BorderStroke(
            width = if (isKeyboardActive) 2.dp else 1.dp,
            color = if (isKeyboardActive) KashBlue else Color(0xFFE2E8F0)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("envelope_item_${item.categoryId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(parsedColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = item.categoryName,
                            tint = parsedColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = item.categoryName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashNavy
                        )
                        Text(
                            text = "${String.format(Locale.GERMANY, "%.1f", percentage)}% del presupuesto total",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar sobre",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Amount Box (triggers In-App Keyboard)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(1.dp, if (isKeyboardActive) KashBlue else Color(0xFFCBD5E1)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenKeyboard() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monto asignado:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$ ${String.format(Locale.GERMANY, "%,.0f", item.allocatedAmount)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.allocatedAmount > 0) KashNavy else Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = KashBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botones de incremento rápido para el sobre (+50k, +100k, +200k, +500k)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    50000.0 to "+$50k",
                    100000.0 to "+$100k",
                    200000.0 to "+$200k",
                    500000.0 to "+$500k"
                ).forEach { (amt, label) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onQuickAdd(amt) }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = KashBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getCategoryIconVector(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "restaurant", "food", "comida", "alimentacion" -> Icons.Default.Restaurant
        "directions_car", "transport", "transporte" -> Icons.Default.DirectionsCar
        "receipt_long", "services", "servicios" -> Icons.Default.ReceiptLong
        "home", "vivienda", "hogar" -> Icons.Default.Home
        "favorite", "health", "salud" -> Icons.Default.Favorite
        "movie", "entertainment", "entretenimiento" -> Icons.Default.Movie
        "school", "education", "educacion" -> Icons.Default.School
        "shopping_bag", "shopping", "compras" -> Icons.Default.ShoppingBag
        else -> Icons.Default.Category
    }
}
