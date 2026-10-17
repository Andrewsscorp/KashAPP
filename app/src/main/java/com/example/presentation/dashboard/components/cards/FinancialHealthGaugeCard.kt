package com.example.presentation.dashboard.components.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.dashboard.model.HealthScore
import com.example.presentation.dashboard.model.RiskLevel

/**
 * 3. FinancialHealthGaugeCard.kt: Semicircular gauge with dynamic color interpolation.
 */
@Composable
fun FinancialHealthGaugeCard(
    healthScore: HealthScore,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("financial_health_gauge_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Salud Financiera",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    RiskBadge(riskLevel = healthScore.riskLevel)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = healthScore.insightMessage,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.size(16.dp))

            // 180-degree Gauge Canvas
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier.size(width = 110.dp, height = 70.dp)
            ) {
                HealthGaugeCanvas(score = healthScore.score)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Text(
                        text = "${healthScore.score}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "/100",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthGaugeCanvas(score: Int) {
    val sweepAngle = (score.coerceIn(0, 100) / 100f) * 180f

    Canvas(modifier = Modifier.size(width = 100.dp, height = 65.dp)) {
        val strokeWidth = 10.dp.toPx()
        val arcSize = Size(size.width - strokeWidth, (size.height * 2) - strokeWidth)
        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

        // Background Gray Track
        drawArc(
            color = Color(0xFFE2E8F0),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Active Arc with Gradient Interpolation
        if (sweepAngle > 0f) {
            drawArc(
                brush = Brush.sweepGradient(
                    0.5f to Color(0xFFEF4444),
                    0.75f to Color(0xFFF59E0B),
                    1.0f to Color(0xFF10B981)
                ),
                startAngle = 180f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun RiskBadge(riskLevel: RiskLevel) {
    val (bgColor, textColor, label) = when (riskLevel) {
        RiskLevel.HEALTHY -> Triple(Color(0xFFDCFCE7), Color(0xFF166534), "Saludable")
        RiskLevel.WARNING -> Triple(Color(0xFFFEF3C7), Color(0xFF92400E), "Alerta")
        RiskLevel.CRITICAL -> Triple(Color(0xFFFEE2E2), Color(0xFF991B1B), "Crítico")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
