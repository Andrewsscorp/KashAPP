package com.example.feature.informes.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.informes.domain.model.*
import com.example.feature.informes.ui.components.InformesFormatters

@Composable
fun AnalisisFinancieroTab(
    analysis: FinancialAnalysisData,
    currency: String,
    onShowCalculationTrace: ((String) -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // HEALTH SCORE & SMART DIAGNOSIS HERO
        // ==========================================
        Surface(
            onClick = { onShowCalculationTrace?.invoke("NET_WORTH") },
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DIAGNÓSTICO FINANCIERO INTEGRAL",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = analysis.overallHealthLabel,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Score Circle
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${analysis.overallHealthScore}",
                            color = Color(0xFF34D399),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Tu estructura financiera presenta altos niveles de solvencia a corto plazo y solvencia patrimonial equilibrada, con bajo costo de endeudamiento.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }

        // ==========================================
        // RATIOS FINANCIEROS (SEMÁFORO DE SALUD)
        // ==========================================
        Text(
            text = "RATIOS E INDICADORES FINANCIEROS (TOCA PARA VER CÁLCULO)",
            color = Color(0xFF64748B),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        analysis.ratios.forEach { ratio ->
            Surface(
                onClick = { onShowCalculationTrace?.invoke(ratio.id) },
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(ratio.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = "Ver cálculo de ${ratio.name}",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text("Categoría: ${ratio.category} • Fórmula: ${ratio.formula}", color = Color(0xFF64748B), fontSize = 11.sp)
                        }

                        // Status badge (Semáforo)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (ratio.status) {
                                RatioHealthStatus.EXCELLENT -> Color(0xFF064E3B)
                                RatioHealthStatus.HEALTHY -> Color(0xFF065F46)
                                RatioHealthStatus.WARNING -> Color(0xFF78350F)
                                RatioHealthStatus.CRITICAL -> Color(0xFF7F1D1D)
                            }
                        ) {
                            Text(
                                text = ratio.status.label,
                                color = when (ratio.status) {
                                    RatioHealthStatus.EXCELLENT, RatioHealthStatus.HEALTHY -> Color(0xFF34D399)
                                    RatioHealthStatus.WARNING -> Color(0xFFFBBF24)
                                    RatioHealthStatus.CRITICAL -> Color(0xFFF87171)
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("VALOR CALCULADO", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(ratio.formattedValue, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("BENCHMARK SEGURO", color = Color(0xFF64748B), fontSize = 11.sp)
                            Text(ratio.benchmark, color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF334155))

                    Text(ratio.interpretation, color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("💡 ${ratio.recommendation}", color = Color(0xFF38BDF8), fontSize = 11.sp)
                }
            }
        }

        // ==========================================
        // SMART ALERTS & ACCIONES RECOMENDADAS
        // ==========================================
        Text(
            text = "ALERTAS Y DIAGNÓSTICO INTELIGENTE",
            color = Color(0xFF64748B),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        analysis.smartAlerts.forEach { alert ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Icon(
                        imageVector = when (alert.severity) {
                            AlertSeverityLevel.INFO -> Icons.Default.Info
                            AlertSeverityLevel.WARNING -> Icons.Default.Warning
                            AlertSeverityLevel.CRITICAL -> Icons.Default.Error
                        },
                        contentDescription = null,
                        tint = when (alert.severity) {
                            AlertSeverityLevel.INFO -> Color(0xFF38BDF8)
                            AlertSeverityLevel.WARNING -> Color(0xFFF59E0B)
                            AlertSeverityLevel.CRITICAL -> Color(0xFFEF4444)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(alert.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(alert.description, color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF0F172A)) {
                            Text(
                                text = "Acción: ${alert.actionableRecommendation}",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // PROYECCIONES Y TENDENCIAS FUTURAS
        // ==========================================
        Text(
            text = "PROYECCIONES Y TENDENCIAS PATRIMONIALES",
            color = Color(0xFF64748B),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        analysis.projections.forEach { proj ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(proj.horizonLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Confianza: ${proj.confidencePct}%",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Patrimonio Proyectado", color = Color(0xFF64748B), fontSize = 11.sp)
                            Text(
                                text = InformesFormatters.formatMoney(proj.projectedNetWorth, currency),
                                color = Color(0xFF34D399),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Ahorro Acumulado", color = Color(0xFF64748B), fontSize = 11.sp)
                            Text(
                                text = InformesFormatters.formatMoney(proj.projectedSavings, currency),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(proj.narrativeSummary, color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
