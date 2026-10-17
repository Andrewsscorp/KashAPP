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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.dashboard.model.CashFlowEntry
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal

/**
 * 6. CashFlowChartCard.kt: Grouped twin bars Canvas with P95 auto-scaling and outlier truncation.
 */
@Composable
fun CashFlowChartCard(
    cashFlowEntries: ImmutableList<CashFlowEntry>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cash_flow_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header & Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Flujo de Efectivo",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendItem(label = "Ingresos", color = Color(0xFF10B981))
                    LegendItem(label = "Gastos", color = Color(0xFFEF4444))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (cashFlowEntries.isEmpty()) {
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
                            text = "Aún no hay movimientos en este periodo",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                CashFlowTwinBarsCanvas(entries = cashFlowEntries)
            }
        }
    }
}

@Composable
private fun CashFlowTwinBarsCanvas(entries: List<CashFlowEntry>) {
    // Calculate P95 scaling
    val allValues = entries.flatMap { listOf(it.totalIncome.toDouble(), it.totalExpense.toDouble()) }.filter { it > 0.0 }.sorted()
    val p95 = if (allValues.isNotEmpty()) {
        val p95Index = (allValues.size * 0.95).toInt().coerceAtMost(allValues.size - 1)
        allValues[p95Index].coerceAtLeast(100.0)
    } else 100.0

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val width = size.width
        val height = size.height - 20.dp.toPx() // Reserve 20dp for labels
        val groupCount = entries.size
        val groupWidth = width / groupCount
        val barWidth = (groupWidth * 0.32f).coerceAtMost(16.dp.toPx())

        entries.forEachIndexed { index, entry ->
            val groupStartX = index * groupWidth + (groupWidth - (barWidth * 2 + 4.dp.toPx())) / 2
            val incomeStartX = groupStartX
            val expenseStartX = groupStartX + barWidth + 4.dp.toPx()

            val incomeVal = entry.totalIncome.toDouble()
            val expenseVal = entry.totalExpense.toDouble()

            val incomeRatio = (incomeVal / p95).coerceIn(0.0, 1.0).toFloat()
            val expenseRatio = (expenseVal / p95).coerceIn(0.0, 1.0).toFloat()

            val isIncomeOutlier = incomeVal > p95
            val isExpenseOutlier = expenseVal > p95

            val incomeHeight = (incomeRatio * height).coerceAtLeast(if (incomeVal > 0) 4.dp.toPx() else 0f)
            val expenseHeight = (expenseRatio * height).coerceAtLeast(if (expenseVal > 0) 4.dp.toPx() else 0f)

            // Draw Income Bar
            if (incomeHeight > 0) {
                drawRoundRect(
                    brush = if (isIncomeOutlier) Brush.verticalGradient(
                        colors = listOf(Color(0xFF059669), Color(0xFF10B981).copy(alpha = 0.6f))
                    ) else Brush.verticalGradient(listOf(Color(0xFF10B981), Color(0xFF10B981))),
                    topLeft = Offset(incomeStartX, height - incomeHeight),
                    size = Size(barWidth, incomeHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }

            // Draw Expense Bar
            if (expenseHeight > 0) {
                drawRoundRect(
                    brush = if (isExpenseOutlier) Brush.verticalGradient(
                        colors = listOf(Color(0xFFDC2626), Color(0xFFEF4444).copy(alpha = 0.6f))
                    ) else Brush.verticalGradient(listOf(Color(0xFFEF4444), Color(0xFFEF4444))),
                    topLeft = Offset(expenseStartX, height - expenseHeight),
                    size = Size(barWidth, expenseHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }
    }

    // Date Labels Row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        entries.forEach { entry ->
            Text(
                text = entry.dateLabel,
                fontSize = 10.sp,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, shape = RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp, color = Color(0xFF64748B))
    }
}
