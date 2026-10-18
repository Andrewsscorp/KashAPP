package com.example.feature.activos.ui

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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.components.CurrencyText
import java.text.DecimalFormat

/**
 * Modern, executive and fully responsive analytics banner for Fixed Assets net worth.
 * Complies with NIIF/NIC 16 standard presentation with fluid adaptive typography and metrics.
 */
@Composable
fun FixedAssetsAnalyticsBanner(
    totalAcquisitionValue: Double,
    totalAccumulatedDepreciation: Double,
    totalBookValue: Double,
    activeAssetsCount: Int,
    currency: String = "COP",
    modifier: Modifier = Modifier
) {
    val moneyFormatter = remember { DecimalFormat("#,##0") }
    val depPercentage = if (totalAcquisitionValue > 0) {
        ((totalAccumulatedDepreciation / totalAcquisitionValue) * 100).coerceIn(0.0, 100.0)
    } else 0.0
    val remainingPercentage = (100.0 - depPercentage).coerceIn(0.0, 100.0)

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
            modifier = Modifier.fillMaxWidth(),
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
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Patrimonio en Activos Fijos",
                                color = Color.White,
                                fontSize = if (isNarrow) 14.5.sp else 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$activeAssetsCount activos • Valuación NIIF",
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
                                text = "NIC 16",
                                color = Color(0xFF00E676),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Hero Metric Box: Net Book Value (Valor en Libros Neto)
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
                                text = "VALOR EN LIBROS NETO",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        CurrencyText(
                            amountDouble = totalBookValue,
                            currencyCode = currency,
                            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium.copy(
                                fontSize = if (isNarrow) 22.sp else 25.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Impacto directo al patrimonio neto en el Dashboard",
                            color = Color(0xFF64748B),
                            fontSize = 11.5.sp
                        )
                    }
                }

                // Balanced 2-Column Metrics: Acquisition Cost & Accumulated Depreciation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF081426),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Costo de Adquisición",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            CurrencyText(
                                amountDouble = totalAcquisitionValue,
                                currencyCode = currency,
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                                    fontSize = if (isNarrow) 13.5.sp else 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF1F5F9)
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF081426),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Depreciación Acum.",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            CurrencyText(
                                amountDouble = -totalAccumulatedDepreciation,
                                currencyCode = currency,
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                                    fontSize = if (isNarrow) 13.5.sp else 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF87171)
                                )
                            )
                        }
                    }
                }

                // Progress Bar: Accounting Amortization
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Amortizado: ${String.format("%.1f", depPercentage)}%",
                            color = Color(0xFF8EA5C4),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Remanente: ${String.format("%.1f", remainingPercentage)}%",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (depPercentage / 100f).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF38BDF8),
                        trackColor = Color(0xFF1E293B)
                    )
                }
            }
        }
    }
}
