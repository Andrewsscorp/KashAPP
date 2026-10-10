package com.example.presentation.dashboard.components.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.dashboard.model.InsightSeverity
import com.example.presentation.dashboard.model.SmartInsight
import kotlinx.collections.immutable.ImmutableList

/**
 * 11. InsightsFeedCard.kt: Anomaly detection and financial insights with custom Canvas bullet indicators.
 */
@Composable
fun InsightsFeedCard(
    insights: ImmutableList<SmartInsight>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("insights_feed_card"),
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
                text = "Alertas & Diagnóstico Inteligente",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (insights.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Todo marcha en orden. No se detectaron anomalías en tu flujo de caja.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    insights.forEachIndexed { index, insight ->
                        val dotColor = when (insight.severity) {
                            InsightSeverity.INFO -> Color(0xFF0284C7)
                            InsightSeverity.WARNING -> Color(0xFFF59E0B)
                            InsightSeverity.CRITICAL -> Color(0xFFEF4444)
                            InsightSeverity.SUCCESS -> Color(0xFF10B981)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(8.dp)
                            ) {
                                drawCircle(color = dotColor)
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = insight.message,
                                fontSize = 13.sp,
                                color = Color(0xFF334155),
                                lineHeight = 18.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (index < insights.size - 1) {
                            HorizontalDivider(color = Color(0xFFF8FAFC), modifier = Modifier.padding(start = 18.dp))
                        }
                    }
                }
            }
        }
    }
}
