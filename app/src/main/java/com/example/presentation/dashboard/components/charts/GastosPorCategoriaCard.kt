package com.example.presentation.dashboard.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.dashboard.model.CategorySpend
import com.example.util.MoneyFormatter
import com.example.ui.components.CurrencyText
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal

/**
 * "Gastos por categoría" Card (Parte 3, Image 2 bottom).
 * Donut ring chart with top category percentage in center and category breakdown on the right.
 */
@Composable
fun GastosPorCategoriaCard(
    categorySpends: ImmutableList<CategorySpend>,
    totalExpense: BigDecimal,
    isPrivacyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val effectiveTotal = if (totalExpense > BigDecimal.ZERO) totalExpense
    else categorySpends.fold(BigDecimal.ZERO) { acc, c -> acc + c.amount }

    // Fallback or dynamic items
    val displayCategories = if (categorySpends.isNotEmpty()) {
        categorySpends.take(4)
    } else {
        listOf(
            CategorySpend(java.util.UUID.randomUUID(), "Vivienda", BigDecimal.ZERO, "#007BFF"),
            CategorySpend(java.util.UUID.randomUUID(), "Alimentación", BigDecimal.ZERO, "#00D2FF"),
            CategorySpend(java.util.UUID.randomUUID(), "Transporte", BigDecimal.ZERO, "#8B5CF6"),
            CategorySpend(java.util.UUID.randomUUID(), "Otros", BigDecimal.ZERO, "#F59E0B")
        )
    }

    val topCategory = displayCategories.firstOrNull()
    val topPercentage = if (effectiveTotal > BigDecimal.ZERO && topCategory != null) {
        ((topCategory.amount.toDouble() / effectiveTotal.toDouble()) * 100).toInt()
    } else 0

    val adaptive = com.example.core.ui.adaptive.rememberKashAdaptiveInfo()
    val cardPadding = if (adaptive.isCompact) 14.dp else 20.dp
    val donutSize = if (adaptive.isUltraCompact) 104.dp else 124.dp

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("gastos_por_categoria_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding)
        ) {
            // Header: "Gastos por categoría" + "$ 2.850.000" (Defensive layout, no line break before decimals)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "Gastos por categoría",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Total del mes",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                CurrencyText(
                    amount = effectiveTotal,
                    isPrivacyMode = isPrivacyMode,
                    fontSize = 20.sp,
                    minFontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    testTag = "gastos_por_categoria_total"
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Body: Donut Ring on Left + Breakdown List on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut Ring Canvas with Center Percentage (Strictly bounded within inner circle radius)
                Box(
                    modifier = Modifier.size(donutSize),
                    contentAlignment = Alignment.Center
                ) {
                    CategoryDonutRingCanvas(
                        categories = displayCategories,
                        total = effectiveTotal,
                        sizeDp = if (adaptive.isUltraCompact) 100.dp else 120.dp
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .widthIn(max = if (adaptive.isUltraCompact) 66.dp else 76.dp)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = if (topPercentage > 0) "$topPercentage%" else "0%",
                            fontSize = if (adaptive.isUltraCompact) 17.sp else 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1
                        )
                        Text(
                            text = topCategory?.categoryName?.lowercase() ?: "general",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.width(if (adaptive.isUltraCompact) 10.dp else 14.dp))

                // Breakdown List with priority width and responsive numbers
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    displayCategories.forEachIndexed { index, cat ->
                        val dotColor = when (index) {
                            0 -> Color(0xFF007BFF) // Blue
                            1 -> Color(0xFF00D2FF) // Cyan
                            2 -> Color(0xFF8B5CF6) // Purple
                            else -> Color(0xFFF59E0B) // Orange
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat.categoryName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF334155),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            CurrencyText(
                                amount = cat.amount,
                                isPrivacyMode = isPrivacyMode,
                                fontSize = 13.sp,
                                minFontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDonutRingCanvas(
    categories: List<CategorySpend>,
    total: BigDecimal,
    sizeDp: androidx.compose.ui.unit.Dp = 120.dp
) {
    Canvas(modifier = Modifier.size(sizeDp)) {
        val strokeWidth = 14.dp.toPx()
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

        val totalDouble = total.toDouble()

        if (totalDouble <= 0.0) {
            // Draw 4 default segments
            val colors = listOf(Color(0xFF007BFF), Color(0xFF00D2FF), Color(0xFF8B5CF6), Color(0xFFF59E0B))
            var startAngle = -90f
            colors.forEach { color ->
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = 86f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += 90f
            }
        } else {
            var startAngle = -90f
            categories.forEachIndexed { index, cat ->
                val ratio = (cat.amount.toDouble() / totalDouble).toFloat()
                val sweep = (ratio * 360f).coerceAtLeast(4f)
                val color = when (index) {
                    0 -> Color(0xFF007BFF)
                    1 -> Color(0xFF00D2FF)
                    2 -> Color(0xFF8B5CF6)
                    else -> Color(0xFFF59E0B)
                }

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = (sweep - 4f).coerceAtLeast(2f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += sweep
            }
        }
    }
}
