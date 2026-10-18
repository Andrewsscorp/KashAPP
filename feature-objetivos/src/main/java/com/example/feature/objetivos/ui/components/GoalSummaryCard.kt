package com.example.feature.objetivos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.components.CurrencyText
import com.example.feature.objetivos.domain.model.GoalSummaryOverview
import java.text.DecimalFormat

/**
 * Banner Ejecutivo de Objetivos Financieros con la misma paleta y estilo de Activos.
 * Gradiente Dark Navy, métricas de Fondos Comprometidos y avance en tiempo real.
 */
@Composable
fun GoalSummaryCard(
    summary: GoalSummaryOverview,
    committedAmount: Double = 0.0,
    isPrivacyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val moneyFormatter = remember { DecimalFormat("#,##0") }
    val progressPct = (summary.globalProgressPercentage / 100.0).toFloat().coerceIn(0f, 1f)

    val gradientBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F2544),
                Color(0xFF09162A)
            )
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isNarrow = maxWidth < 360.dp

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("goal_summary_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradientBrush)
                    .padding(if (isNarrow) 14.dp else 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header: Icon + Title + Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFF2563EB).copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Patrimonio en Objetivos",
                                color = Color.White,
                                fontSize = if (isNarrow) 14.5.sp else 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${summary.activeGoalsCount} objetivos activos • Mental Accounting",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF00C853).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${String.format("%.1f", summary.globalProgressPercentage)}%",
                                color = Color(0xFF00E676),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Hero Metric Box: Total Ahorrado Acumulado
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF061120),
                    border = BorderStroke(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "CAPITAL TOTAL AHORRADO",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        CurrencyText(
                            amountDouble = summary.totalSaved,
                            isPrivacyMode = isPrivacyMode,
                            color = Color.White,
                            fontSize = if (isNarrow) 22.sp else 25.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { progressPct },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF132B4F),
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Meta: ",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.5.sp
                                )
                                CurrencyText(
                                    amountDouble = summary.totalTarget,
                                    isPrivacyMode = isPrivacyMode,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.5.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Resta: ",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                CurrencyText(
                                    amountDouble = summary.totalPending,
                                    isPrivacyMode = isPrivacyMode,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Balanced 2-Column Metrics: Fondos Comprometidos & Metas Cumplidas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Col 1: Fondos Comprometidos (Solo aportes hechos)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF061120),
                        border = BorderStroke(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.35f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "COMPROMETIDO",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.4.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            CurrencyText(
                                amountDouble = committedAmount.ifZero(summary.totalSaved),
                                isPrivacyMode = isPrivacyMode,
                                color = Color.White,
                                fontSize = if (isNarrow) 14.sp else 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Solo aportes hechos",
                                color = Color(0xFF00E676),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Col 2: Metas cumplidas
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF061120),
                        border = BorderStroke(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.35f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Adjust,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "LOGROS",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.4.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${summary.completedGoalsCount} logradas",
                                color = Color.White,
                                fontSize = if (isNarrow) 14.sp else 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${summary.activeGoalsCount} en ejecución",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Double.ifZero(fallback: Double): Double = if (this > 0.0) this else fallback
