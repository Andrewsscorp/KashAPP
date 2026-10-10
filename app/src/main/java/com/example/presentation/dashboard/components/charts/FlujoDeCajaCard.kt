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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.dashboard.model.CashFlowEntry
import com.example.util.MoneyFormatter
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal

/**
 * "Flujo de caja" Card (Parte 3, Image 2 middle).
 * Deep navy card with dual smooth Bézier wave lines (Cyan for Income, Coral for Expenses).
 */
@Composable
fun FlujoDeCajaCard(
    cashFlowEntries: ImmutableList<CashFlowEntry>,
    savingsAmount: BigDecimal,
    isPrivacyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF051E38)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("flujo_de_caja_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF041C33),
                            Color(0xFF072B4B),
                            Color(0xFF0A3A64)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            // Header: "Flujo de caja" + Pill badge "↗ +$ 2,15 M"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Flujo de caja",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ingresos vs. gastos",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Cyan Pill Badge
                Surface(
                    color = Color(0xFF0D3349),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00D2FF).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF00D2FF),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isPrivacyMode) "••••" else "+${MoneyFormatter.formatCompact(savingsAmount, isPrivacyMode)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00D2FF)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Smooth Dual Bézier Lines Canvas
            DualWaveLinesCanvas(entries = cashFlowEntries)

            Spacer(modifier = Modifier.height(8.dp))

            // X-Axis Labels: "1 Sep", "15 Sep", "30 Sep"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "1 Sep", fontSize = 11.sp, color = Color(0xFF64748B))
                Text(text = "15 Sep", fontSize = 11.sp, color = Color(0xFF64748B))
                Text(text = "30 Sep", fontSize = 11.sp, color = Color(0xFF64748B))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend: ● Ingresos (Cyan)   ● Gastos (Coral)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00D2FF))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Ingresos", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFB7185))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Gastos", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                }
            }
        }
    }
}

@Composable
private fun DualWaveLinesCanvas(entries: List<CashFlowEntry>) {
    // Generate wave points or default realistic curves
    val incomePoints = listOf(0.15f, 0.35f, 0.45f, 0.65f, 0.85f, 0.95f)
    val expensePoints = listOf(0.10f, 0.25f, 0.38f, 0.50f, 0.70f, 0.88f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        val w = size.width
        val h = size.height

        // Subtle horizontal guide lines
        val gridColor = Color.White.copy(alpha = 0.06f)
        drawLine(gridColor, Offset(0f, h * 0.25f), Offset(w, h * 0.25f), strokeWidth = 1.dp.toPx())
        drawLine(gridColor, Offset(0f, h * 0.6f), Offset(w, h * 0.6f), strokeWidth = 1.dp.toPx())

        // 1. Draw Income Wave (Cyan)
        val incomePath = createWavePath(incomePoints, w, h)
        val incomeFill = Path().apply {
            addPath(incomePath)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        drawPath(
            path = incomeFill,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF00D2FF).copy(alpha = 0.15f), Color.Transparent),
                startY = 0f,
                endY = h
            )
        )

        drawPath(
            path = incomePath,
            color = Color(0xFF00D2FF),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 2. Draw Expense Wave (Coral)
        val expensePath = createWavePath(expensePoints, w, h)
        drawPath(
            path = expensePath,
            color = Color(0xFFFB7185),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // End Cyan Dot with White Glow
        val lastX = w
        val lastY = h - (incomePoints.last() * (h - 16.dp.toPx()) + 8.dp.toPx())
        drawCircle(color = Color(0xFF00D2FF).copy(alpha = 0.4f), radius = 7.dp.toPx(), center = Offset(lastX, lastY))
        drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = Offset(lastX, lastY))
    }
}

private fun createWavePath(points: List<Float>, width: Float, height: Float): Path {
    val path = Path()
    if (points.isEmpty()) return path

    val stepX = width / (points.size - 1)
    val coords = points.mapIndexed { idx, value ->
        Offset(idx * stepX, height - (value * (height - 20.dp.value) + 10.dp.value))
    }

    path.moveTo(coords.first().x, coords.first().y)
    for (i in 0 until coords.size - 1) {
        val p1 = coords[i]
        val p2 = coords[i + 1]
        val cx1 = p1.x + (p2.x - p1.x) / 2
        val cy1 = p1.y
        val cx2 = p1.x + (p2.x - p1.x) / 2
        val cy2 = p2.y
        path.cubicTo(cx1, cy1, cx2, cy2, p2.x, p2.y)
    }
    return path
}
