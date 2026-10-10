package com.example.presentation.dashboard.components.cards

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashCyan
import com.example.ui.theme.KashNavy
import com.example.ui.theme.KashSuccess

@Composable
fun HealthScoreCard(
    score: Int = 78,
    status: String = "En buen camino",
    advice: String = "Mejora tu ahorro mensual para llegar a tus objetivos más rápido.",
    onViewDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("health_score_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "Tu salud financiera",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Radial Gauge Canvas
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(90.dp)
                    .testTag("radial_gauge_box")
            ) {
                Canvas(modifier = Modifier.size(86.dp)) {
                    val strokeWidth = 8.dp.toPx()

                    // Background full track arc
                    drawArc(
                        color = Color(0xFFE2E8F0),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Active progress arc
                    val progressSweep = (score / 100f) * 270f
                    drawArc(
                        color = KashCyan,
                        startAngle = 135f,
                        sweepAngle = progressSweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = KashNavy
                        ),
                        fontSize = 20.sp
                    )
                    Text(
                        text = "⚡ /100",
                        fontSize = 9.sp,
                        color = KashSuccess,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = status,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = KashNavy
                ),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = advice,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF64748B),
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center
                ),
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Ver detalles →",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = KashBlue,
                modifier = Modifier
                    .clickable { onViewDetailsClick() }
                    .testTag("health_score_details_link")
            )
        }
    }
}
