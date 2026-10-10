package com.example.presentation.dashboard.components.cards

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.presentation.dashboard.model.NetWorthState
import com.example.util.MoneyFormatter
import com.example.ui.components.CurrencyText

/**
 * 2. NetWorthHeroCard.kt: Net Worth Engine with 2x2 breakdown and smooth Cubic Bézier Sparkline.
 */
@Composable
fun NetWorthHeroCard(
    netWorthState: NetWorthState,
    isPrivacyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("net_worth_hero_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Patrimonio Neto Total",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    CurrencyText(
                        amount = netWorthState.totalNetWorth,
                        isPrivacyMode = isPrivacyMode,
                        fontSize = 28.sp,
                        minFontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        testTag = "net_worth_total_amount"
                    )
                }

                // Sparkline
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .fillMaxWidth(0.35f)
                ) {
                    SparklineCanvas(points = netWorthState.sparklinePoints)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(16.dp))

            // 2x2 Breakdown Grid
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    NetWorthItem(
                        label = "Efectivo y Cuentas",
                        amount = netWorthState.availableCash,
                        color = Color(0xFF0284C7),
                        isPrivacyMode = isPrivacyMode,
                        modifier = Modifier.weight(1f)
                    )
                    NetWorthItem(
                        label = "Inversiones y CDTs",
                        amount = netWorthState.investments,
                        color = Color(0xFF8B5CF6),
                        isPrivacyMode = isPrivacyMode,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    NetWorthItem(
                        label = "Activos y Bienes",
                        amount = netWorthState.assets,
                        color = Color(0xFF10B981),
                        isPrivacyMode = isPrivacyMode,
                        modifier = Modifier.weight(1f)
                    )
                    NetWorthItem(
                        label = "Pasivos y Deudas",
                        amount = netWorthState.liabilities.negate(),
                        color = Color(0xFFEF4444),
                        isPrivacyMode = isPrivacyMode,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NetWorthItem(
    label: String,
    amount: java.math.BigDecimal,
    color: Color,
    isPrivacyMode: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .background(color, shape = RoundedCornerShape(2.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        CurrencyText(
            amount = amount,
            isPrivacyMode = isPrivacyMode,
            fontSize = 14.sp,
            minFontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E293B)
        )
    }
}

@Composable
private fun SparklineCanvas(points: List<Float>) {
    Canvas(modifier = Modifier.fillMaxWidth().height(44.dp)) {
        if (points.isEmpty()) return@Canvas
        val width = size.width
        val height = size.height
        val min = points.minOrNull() ?: 0f
        val max = points.maxOrNull() ?: 1f
        val range = if (max == min) 1f else (max - min)

        val normalized = points.map { 
            val norm = (it - min) / range
            height - (norm * (height - 8.dp.toPx()) + 4.dp.toPx())
        }

        val stepX = if (points.size > 1) width / (points.size - 1) else width

        val path = Path()
        val fillPath = Path()

        path.moveTo(0f, normalized.first())
        fillPath.moveTo(0f, height)
        fillPath.lineTo(0f, normalized.first())

        for (i in 0 until points.size - 1) {
            val x1 = i * stepX
            val y1 = normalized[i]
            val x2 = (i + 1) * stepX
            val y2 = normalized[i + 1]

            val cx1 = x1 + (x2 - x1) / 2
            val cy1 = y1
            val cx2 = x1 + (x2 - x1) / 2
            val cy2 = y2

            path.cubicTo(cx1, cy1, cx2, cy2, x2, y2)
            fillPath.cubicTo(cx1, cy1, cx2, cy2, x2, y2)
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        // Gradient Fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0284C7).copy(alpha = 0.25f), Color(0xFF0284C7).copy(alpha = 0.0f)),
                startY = 0f,
                endY = height
            )
        )

        // Line Stroke
        drawPath(
            path = path,
            color = Color(0xFF0284C7),
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
