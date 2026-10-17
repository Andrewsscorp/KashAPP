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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal

/**
 * 7. ExpensesCategoryDonutCard.kt: Interactive Donut chart with trigonometric arc angles.
 */
@Composable
fun ExpensesCategoryDonutCard(
    categorySpends: ImmutableList<CategorySpend>,
    isPrivacyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val totalExpense = categorySpends.fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("expenses_category_donut_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Distribución de Gastos",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (categorySpends.isEmpty() || totalExpense <= BigDecimal.ZERO) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sin gastos registrados en este periodo",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donut Chart with Center Text
                    Box(
                        modifier = Modifier.size(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DonutCanvas(
                            spends = categorySpends,
                            total = totalExpense
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = MoneyFormatter.formatCompact(totalExpense, isPrivacyMode),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Category Legend List
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categorySpends.take(4).forEach { spend ->
                            val color = try {
                                Color(android.graphics.Color.parseColor(spend.hexColor))
                            } catch (_: Exception) {
                                Color(0xFF0284C7)
                            }
                            val percentage = if (totalExpense > BigDecimal.ZERO) {
                                (spend.amount.toDouble() / totalExpense.toDouble()) * 100
                            } else 0.0

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = spend.categoryName,
                                        fontSize = 12.sp,
                                        color = Color(0xFF1E293B),
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "%.0f%%".format(percentage),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DonutCanvas(
    spends: List<CategorySpend>,
    total: BigDecimal
) {
    Canvas(modifier = Modifier.size(120.dp)) {
        val strokeWidth = 14.dp.toPx()
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
        var currentAngle = -90f

        val totalDouble = total.toDouble()

        spends.forEach { spend ->
            val sweep = ((spend.amount.toDouble() / totalDouble) * 360f).toFloat()
            val color = try {
                Color(android.graphics.Color.parseColor(spend.hexColor))
            } catch (_: Exception) {
                Color(0xFF0284C7)
            }

            drawArc(
                color = color,
                startAngle = currentAngle,
                sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            currentAngle += sweep
        }
    }
}
