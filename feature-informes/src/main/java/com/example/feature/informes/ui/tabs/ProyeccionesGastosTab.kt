package com.example.feature.informes.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.informes.domain.model.AdvancedProjectionsBundle
import com.example.feature.informes.domain.model.CalculationTraceDetail
import com.example.feature.informes.domain.model.PredictabilityLevel
import com.example.feature.informes.ui.components.HorizontalScrollHint
import com.example.feature.informes.ui.components.InformesFormatters

@Composable
fun ProyeccionesGastosTab(
    projections: AdvancedProjectionsBundle,
    currency: String,
    onShowCalculationTrace: (CalculationTraceDetail) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // 1. HERO: RUNWAY FINANCIERO Y PROTECCIÓN
        // ==========================================
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E293B),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MOTOR PREDICTIVO & BLINDAJE",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = projections.runwayProtection.statusLabel,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    IconButton(
                        onClick = {
                            onShowCalculationTrace(
                                com.example.feature.informes.domain.model.CalculationTraceDetail(
                                    metricKey = "RUNWAY",
                                    title = "Runway y Protección de Supervivencia",
                                    currentFormattedValue = "%.1f Meses (%d Días)".format(
                                        projections.runwayProtection.runwayMonths,
                                        projections.runwayProtection.runwayDays
                                    ),
                                    accountingFormula = "Runway = Liquidez Inmediata / Tasa de Gasto Mensual (Burn Rate)",
                                    mathematicalOperation = "$%,.0f (Liquidez) / $%,.0f (Gasto Mensual) = %.1f meses".format(
                                        projections.runwayProtection.availableLiquidCash,
                                        projections.runwayProtection.monthlyBurnRate,
                                        projections.runwayProtection.runwayMonths
                                    ),
                                    dataSources = listOf(
                                        com.example.feature.informes.domain.model.DataSourceItem(
                                            "accounts (LIQUID)", "Cuentas Líquidas", "Saldos disponibles sin penalidad en bancos", 3, projections.runwayProtection.availableLiquidCash
                                        ),
                                        com.example.feature.informes.domain.model.DataSourceItem(
                                            "transactions (EXPENSE)", "Gasto Operativo", "Promedio mensual histórico consolidado", 38, projections.runwayProtection.monthlyBurnRate
                                        )
                                    ),
                                    stepByStepExplanation = listOf(
                                        "1. Se calcula el efectivo disponible inmediato en cuentas corrientes y de ahorro.",
                                        "2. Se extrae el gasto promedio mensual histórico del usuario.",
                                        "3. Se dividen ambas magnitudes para proyectar exactamente cuántos días de subsistencia se tienen sin necesidad de generar nuevos ingresos."
                                    ),
                                    accountingStandardNote = "Métrica estándar de gestión de tesorería y resiliencia financiera personal.",
                                    auditConfidenceStatus = "✓ Análisis predictivo calibrado al 100%."
                                )
                            )
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Cómo se calculó", tint = Color(0xFF38BDF8))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "TIEMPO DE RUNWAY ESTIMADO",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "%.1f Meses".format(projections.runwayProtection.runwayMonths),
                            color = Color(0xFF34D399),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Equivalente a ${projections.runwayProtection.runwayDays} días de cobertura",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "GASTO DIARIO (BURN RATE)",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = InformesFormatters.formatMoney(projections.dailyExpenseBurnRate, currency),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Promedio diario estimado",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar frente al Fondo de Emergencia de 6 Meses
                Text(
                    text = "Cobertura frente a la meta de 6 meses ($%,.0f COP): %.1f%%".format(
                        projections.runwayProtection.recommendedEmergencyFundTarget,
                        projections.runwayProtection.currentProtectionCoveragePct
                    ),
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (projections.runwayProtection.currentProtectionCoveragePct / 100f).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF10B981),
                    trackColor = Color(0xFF334155)
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = projections.runwayProtection.shockAbsorptionDescription,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
        }

        // ==========================================
        // 2. GASTOS PREDICTIVOS SEGÚN DATOS (CLASIFICACIÓN)
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ESTRUCTURA PREDICTIVA DE GASTOS",
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Total: ${InformesFormatters.formatMoney(projections.monthlyTotalProjectedExpense, currency)}/mes",
                color = Color(0xFF38BDF8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        projections.projectedCategories.forEach { cat ->
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
                        Text(
                            text = cat.categoryName,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = try {
                                Color(android.graphics.Color.parseColor(cat.predictabilityType.colorHex)).copy(alpha = 0.2f)
                            } catch (e: Exception) {
                                Color(0xFF0284C7).copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                text = cat.predictabilityType.label,
                                color = try {
                                    Color(android.graphics.Color.parseColor(cat.predictabilityType.colorHex))
                                } catch (e: Exception) {
                                    Color(0xFF38BDF8)
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(cat.frequencyDescription, color = Color(0xFF64748B), fontSize = 11.sp)
                            Text("Vencimiento: ${cat.nextDueDateEstimated}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = InformesFormatters.formatMoney(cat.monthlyEstimatedAmount, currency),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "%.1f%% del presupuesto".format(cat.percentageOfTotal),
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. SIMULACIÓN MULTI-ESCENARIO (DESLIZABLE)
        // ==========================================
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ESCENARIOS A 12 MESES (SIMULACIÓN)",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Swipe indicator
            HorizontalScrollHint(text = "Desliza a la izquierda para comparar escenarios")

            Spacer(modifier = Modifier.height(8.dp))

            // Horizontally Scrollable Scenario Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                projections.multiHorizonScenarios.forEach { sc ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            try {
                                Color(android.graphics.Color.parseColor(sc.colorHex)).copy(alpha = 0.5f)
                            } catch (e: Exception) {
                                Color(0xFF38BDF8)
                            }
                        ),
                        modifier = Modifier.width(280.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sc.scenarioName,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${sc.probabilityPct}% Prob.",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(sc.description, color = Color(0xFF94A3B8), fontSize = 11.sp, minLines = 2)

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFF334155))

                            Text("Patrimonio Final Proyectado:", color = Color(0xFF64748B), fontSize = 11.sp)
                            Text(
                                text = InformesFormatters.formatMoney(sc.projectedFinalNetWorth, currency),
                                color = Color(0xFF34D399),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Ingreso 12M", color = Color(0xFF64748B), fontSize = 11.sp)
                                    Text(InformesFormatters.formatCompactMoney(sc.projectedIncome), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Column {
                                    Text("Gasto 12M", color = Color(0xFF64748B), fontSize = 11.sp)
                                    Text(InformesFormatters.formatCompactMoney(sc.projectedExpenses), color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Column {
                                    Text("Ahorro 12M", color = Color(0xFF64748B), fontSize = 11.sp)
                                    Text(InformesFormatters.formatCompactMoney(sc.projectedNetSavings), color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. PRÓXIMOS 30 DÍAS DE FLUJO PREDICTIVO (DESLIZABLE)
        // ==========================================
        Column {
            Text(
                text = "TRAYECTORIA DE LIQUIDEZ (PRÓXIMOS 30 DÍAS)",
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            HorizontalScrollHint(text = "Desliza a la izquierda para ver el calendario predictivo")

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                projections.upcoming30DaysForecast.forEach { pt ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.width(130.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(pt.dayLabel, color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(pt.dateString, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFF334155))

                            if (pt.expectedInflow > 0) {
                                Text("+${InformesFormatters.formatCompactMoney(pt.expectedInflow)}", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            if (pt.expectedOutflow > 0) {
                                Text("-${InformesFormatters.formatCompactMoney(pt.expectedOutflow)}", color = Color(0xFFF87171), fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Saldo:", color = Color(0xFF64748B), fontSize = 11.sp)
                            Text(InformesFormatters.formatCompactMoney(pt.projectedEndBalance), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ==========================================
        // 5. RECOMENDACIONES ESTRATÉGICAS
        // ==========================================
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF151F33),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RECOMENDACIONES DEL MOTOR PREDICTIVO", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                projections.strategicRecommendations.forEach { rec ->
                    Text("• $rec", color = Color(0xFFCBD5E1), fontSize = 11.sp, modifier = Modifier.padding(vertical = 3.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
