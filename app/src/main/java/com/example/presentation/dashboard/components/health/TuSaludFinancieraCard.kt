package com.example.presentation.dashboard.components.health

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
 * "TU SALUD FINANCIERA" Card (Parte 2, Image 1 bottom).
 * Circular Donut Ring Gauge with center score, score status title, insight message, and action link.
 */
@Composable
fun TuSaludFinancieraCard(
    healthScore: HealthScore,
    onViewRecommendationsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val statusTitle = when (healthScore.riskLevel) {
        RiskLevel.HEALTHY -> if (healthScore.score >= 80) "Excelente" else "Saludable"
        RiskLevel.WARNING -> "Moderado"
        RiskLevel.CRITICAL -> "Crítico"
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("tu_salud_financiera_card")
    ) {
        val adaptive = com.example.core.ui.adaptive.rememberKashAdaptiveInfo()
        val cardPadding = if (adaptive.isCompact) 14.dp else 20.dp
        val donutBoxSize = if (adaptive.isUltraCompact) 88.dp else 110.dp
        val donutCanvasSize = if (adaptive.isUltraCompact) 80.dp else 100.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Donut Ring Gauge
            Box(
                modifier = Modifier.size(donutBoxSize),
                contentAlignment = Alignment.Center
            ) {
                CircularHealthDonutCanvas(score = healthScore.score, sizeDp = donutCanvasSize)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${healthScore.score}",
                        fontSize = if (adaptive.isUltraCompact) 22.sp else 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "/ 100",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.width(if (adaptive.isUltraCompact) 12.dp else 18.dp))

            // Right Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TU SALUD FINANCIERA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = statusTitle,
                    fontSize = if (adaptive.isUltraCompact) 18.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = healthScore.insightMessage.ifBlank { "Vas por buen camino. Ajustar tus gastos variables puede elevar tu puntaje." },
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .clickable { onViewRecommendationsClick() }
                        .padding(vertical = 6.dp, horizontal = 2.dp)
                        .testTag("ver_recomendaciones_link"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Ver recomendaciones",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF007BFF)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF007BFF),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CircularHealthDonutCanvas(score: Int, sizeDp: androidx.compose.ui.unit.Dp = 100.dp) {
    val clampedScore = score.coerceIn(0, 100)
    val sweepAngle = (clampedScore / 100f) * 360f

    Canvas(modifier = Modifier.size(sizeDp)) {
        val strokeWidth = 12.dp.toPx()
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

        // Background Track (Soft light-blue/gray ring)
        drawArc(
            color = Color(0xFFE0F2FE).copy(alpha = 0.6f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Active Cyan Ring Arc (Glow gradient)
        if (sweepAngle > 0f) {
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to Color(0xFF00D2FF),
                    0.5f to Color(0xFF0284C7),
                    1.0f to Color(0xFF00D2FF)
                ),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}
