package com.example.presentation.dashboard.components.alerts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.dashboard.model.InsightSeverity
import com.example.presentation.dashboard.model.SmartInsight
import kotlinx.collections.immutable.ImmutableList

/**
 * "Alertas y notificaciones" Card (Image 2 bottom).
 */
@Composable
fun AlertasYNotificacionesCard(
    insights: ImmutableList<SmartInsight>,
    onCardClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val defaultAlerts = listOf(
        Pair("Tus gastos en restaurantes aumentaron 24%", Color(0xFFF59E0B)), // Orange
        Pair("Tu presupuesto de entretenimiento está al 82%", Color(0xFFF59E0B)), // Orange
        Pair("Tu tarjeta vence en 3 días", Color(0xFFEF4444)), // Red
        Pair("Alcanzaste el 60% de tu objetivo de ahorro", Color(0xFF10B981)) // Green
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("alertas_y_notificaciones_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Yellow Bell + "Alertas y notificaciones" + "→"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Alertas y notificaciones",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bullet List
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (insights.isNotEmpty()) {
                    insights.take(4).forEach { ins ->
                        val bulletColor = when (ins.severity) {
                            InsightSeverity.CRITICAL -> Color(0xFFEF4444)
                            InsightSeverity.WARNING -> Color(0xFFF59E0B)
                            InsightSeverity.SUCCESS -> Color(0xFF10B981)
                            InsightSeverity.INFO -> Color(0xFF007BFF)
                        }
                        AlertBulletRow(message = ins.message, bulletColor = bulletColor)
                    }
                } else {
                    defaultAlerts.forEach { (msg, color) ->
                        AlertBulletRow(message = msg, bulletColor = color)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertBulletRow(message: String, bulletColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(6.dp)) {
            drawCircle(color = bulletColor)
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = message,
            fontSize = 13.sp,
            color = Color(0xFF334155),
            lineHeight = 18.sp
        )
    }
}
