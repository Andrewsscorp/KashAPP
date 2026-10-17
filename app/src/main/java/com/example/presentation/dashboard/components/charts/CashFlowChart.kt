package com.example.presentation.dashboard.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.CashFlowTimeline
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashCyan
import com.example.ui.theme.KashNavy

@Composable
fun CashFlowChart(
    cashFlow: CashFlowTimeline,
    onGranularityChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cash_flow_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header Row: Title, Legend, Dropdown & Filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Flujo de caja",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = KashNavy
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Legend: Blue = Ingresos, Cyan = Gastos
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .border(1.dp, KashBlue, CircleShape)
                                .padding(1.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .padding(end = 4.dp)
                        ) {
                            Canvas(modifier = Modifier.size(6.dp)) {
                                drawCircle(color = KashBlue)
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Ingresos", fontSize = 11.sp, color = Color(0xFF64748B))

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                        ) {
                            Canvas(modifier = Modifier.size(6.dp)) {
                                drawCircle(color = KashCyan)
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Gastos", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Granularity Dropdown (Mes, Semana, Día)
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                .clickable { menuExpanded = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("granularity_dropdown"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cashFlow.granularity,
                                fontSize = 12.sp,
                                color = KashNavy,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Cambiar",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            listOf("Día", "Semana", "Mes").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        menuExpanded = false
                                        onGranularityChange(option)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { /* filter transfers */ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filtrar",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Chart with Side-by-Side Bars & Net Polygon Line
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .testTag("grouped_bars_canvas")
            ) {
                val w = size.width
                val h = size.height
                val bottomPadding = 24.dp.toPx()
                val leftPadding = 34.dp.toPx()
                val chartHeight = h - bottomPadding
                val chartWidth = w - leftPadding

                val maxVal = 8000000.0 // 8M max

                // Draw Y-Axis Horizontal Grid Lines (2M, 4M, 6M, 8M)
                val gridLevels = listOf(2000000.0, 4000000.0, 6000000.0, 8000000.0)
                gridLevels.forEach { level ->
                    val y = chartHeight - (level / maxVal * chartHeight).toFloat()
                    drawLine(
                        color = Color(0xFFF1F5F9),
                        start = Offset(leftPadding, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val points = cashFlow.points
                if (points.isNotEmpty()) {
                    val groupCount = points.size
                    val groupWidth = chartWidth / groupCount
                    val barWidth = 10.dp.toPx()

                    val netLinePath = Path()

                    points.forEachIndexed { index, point ->
                        val groupCenterX = leftPadding + (index * groupWidth) + (groupWidth / 2f)

                        // 1. Blue Bar (Income)
                        val incomeHeight = ((point.income / maxVal) * chartHeight).toFloat()
                        val incomeTop = chartHeight - incomeHeight
                        drawRoundRect(
                            color = KashBlue,
                            topLeft = Offset(groupCenterX - barWidth - 1.dp.toPx(), incomeTop),
                            size = Size(barWidth, incomeHeight),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )

                        // 2. Cyan Bar (Expense)
                        val expenseHeight = ((point.expense / maxVal) * chartHeight).toFloat()
                        val expenseTop = chartHeight - expenseHeight
                        drawRoundRect(
                            color = KashCyan,
                            topLeft = Offset(groupCenterX + 1.dp.toPx(), expenseTop),
                            size = Size(barWidth, expenseHeight),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )

                        // 3. Net Line point
                        val netY = chartHeight - ((point.netBalance / maxVal) * chartHeight).toFloat()
                        if (index == 0) {
                            netLinePath.moveTo(groupCenterX, netY)
                        } else {
                            netLinePath.lineTo(groupCenterX, netY)
                        }

                        // Dot at net balance
                        drawCircle(
                            color = Color(0xFF38BDF8),
                            radius = 3.dp.toPx(),
                            center = Offset(groupCenterX, netY)
                        )
                    }

                    // Draw net balance polygonal line
                    drawPath(
                        path = netLinePath,
                        color = Color(0xFF0284C7).copy(alpha = 0.5f),
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // X-Axis Labels Row (1 Sep, 7 Sep, 14 Sep, 21 Sep, 28 Sep)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                cashFlow.points.forEach { pt ->
                    Text(
                        text = pt.label,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}
