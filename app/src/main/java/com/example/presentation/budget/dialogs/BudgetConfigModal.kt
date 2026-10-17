package com.example.presentation.budget.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.presentation.budget.state.BudgetConfiguration
import com.example.presentation.budget.state.BudgetCycleType
import com.example.presentation.budget.state.BudgetDistributionRule
import com.example.presentation.budget.state.BudgetingStyle

@Composable
fun BudgetConfigModal(
    currentConfig: BudgetConfiguration,
    onDismiss: () -> Unit,
    onSaveConfig: (BudgetConfiguration) -> Unit
) {
    var selectedRule by remember { mutableStateOf(currentConfig.distributionRule) }
    var customNeeds by remember { mutableIntStateOf(currentConfig.customNeedsPct) }
    var customWants by remember { mutableIntStateOf(currentConfig.customWantsPct) }
    var customSavings by remember { mutableIntStateOf(currentConfig.customSavingsPct) }

    var warningThreshold by remember { mutableIntStateOf(currentConfig.warningThresholdPct) }
    var budgetingStyle by remember { mutableStateOf(currentConfig.budgetingStyle) }
    var cycleType by remember { mutableStateOf(currentConfig.cycleType) }

    var notifyOnThreshold by remember { mutableStateOf(currentConfig.notifyOnThreshold) }
    var notifyOnOverdraft by remember { mutableStateOf(currentConfig.notifyOnOverdraft) }
    var notifyOnBurnRateRisk by remember { mutableStateOf(currentConfig.notifyOnBurnRateRisk) }

    val bgDark = Color(0xFF0F172A)
    val cardBg = Color(0xFF1E293B)
    val cardBorder = Color(0xFF334155)
    val primaryBlue = Color(0xFF0284C7)
    val accentCyan = Color(0xFF38BDF8)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF94A3B8)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = bgDark,
            border = BorderStroke(1.dp, cardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(primaryBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = accentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Configuración de Presupuesto",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "Reglas, alertas de consumo y estilos",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = cardBorder)
                Spacer(modifier = Modifier.height(16.dp))

                // 1. REGLA DE DISTRIBUCIÓN
                Text(
                    text = "REGLA DE DISTRIBUCIÓN DE GASTOS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentCyan,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                BudgetDistributionRule.entries.forEach { rule ->
                    val isSelected = selectedRule == rule
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) primaryBlue.copy(alpha = 0.15f) else cardBg
                        ),
                        border = BorderStroke(1.dp, if (isSelected) primaryBlue else cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedRule = rule }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rule.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else textSecondary
                                )
                                Text(
                                    text = if (rule != BudgetDistributionRule.CUSTOM) {
                                        "${rule.needs}% Necesidades · ${rule.wants}% Deseos · ${rule.savings}% Ahorro/Metas"
                                    } else {
                                        "Personaliza tus propios porcentajes con sliders"
                                    },
                                    fontSize = 11.sp,
                                    color = if (isSelected) accentCyan else textSecondary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .border(
                                        2.dp,
                                        if (isSelected) accentCyan else textSecondary.copy(alpha = 0.5f),
                                        CircleShape
                                    )
                                    .background(if (isSelected) accentCyan else Color.Transparent)
                            )
                        }
                    }
                }

                // If CUSTOM, show sliders
                if (selectedRule == BudgetDistributionRule.CUSTOM) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            val totalSum = customNeeds + customWants + customSavings
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Ajuste Porcentual", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text(
                                    text = "Total: $totalSum%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalSum == 100) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Necesidades: $customNeeds%", fontSize = 11.sp, color = textSecondary)
                            Slider(
                                value = customNeeds.toFloat(),
                                onValueChange = { customNeeds = it.toInt() },
                                valueRange = 10f..80f,
                                steps = 13,
                                colors = SliderDefaults.colors(thumbColor = accentCyan, activeTrackColor = accentCyan)
                            )

                            Text("Deseos & Ocio: $customWants%", fontSize = 11.sp, color = textSecondary)
                            Slider(
                                value = customWants.toFloat(),
                                onValueChange = { customWants = it.toInt() },
                                valueRange = 5f..60f,
                                steps = 10,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFFF59E0B), activeTrackColor = Color(0xFFF59E0B))
                            )

                            Text("Ahorro & Metas: $customSavings%", fontSize = 11.sp, color = textSecondary)
                            Slider(
                                value = customSavings.toFloat(),
                                onValueChange = { customSavings = it.toInt() },
                                valueRange = 5f..50f,
                                steps = 8,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF10B981), activeTrackColor = Color(0xFF10B981))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. UMBRAL DE ALERTA DE CONSUMO
                Text(
                    text = "UMBRAL DE ALERTA DE CONSUMO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentCyan,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Señalizar sobre cuando el gasto supere este porcentaje del tope",
                    fontSize = 11.sp,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(70, 80, 85, 90).forEach { threshold ->
                        val isSelected = warningThreshold == threshold
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) primaryBlue else cardBg,
                            border = BorderStroke(1.dp, if (isSelected) accentCyan else cardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { warningThreshold = threshold }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$threshold%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 3. ESTILO DE PRESUPUESTACIÓN
                Text(
                    text = "ESTILO DE PRESUPUESTACIÓN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentCyan,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                BudgetingStyle.entries.forEach { style ->
                    val isSelected = budgetingStyle == style
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) primaryBlue.copy(alpha = 0.15f) else cardBg
                        ),
                        border = BorderStroke(1.dp, if (isSelected) primaryBlue else cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { budgetingStyle = style }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = style.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else textSecondary
                            )
                            Text(
                                text = style.description,
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 4. CICLO DE PRESUPUESTO
                Text(
                    text = "CICLO Y PERÍODO DEL PRESUPUESTO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentCyan,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BudgetCycleType.entries.forEach { cType ->
                        val isSelected = cycleType == cType
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) primaryBlue else cardBg,
                            border = BorderStroke(1.dp, if (isSelected) accentCyan else cardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { cycleType = cType }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (cType) {
                                        BudgetCycleType.MONTHLY_CALENDAR -> "Mensual"
                                        BudgetCycleType.BIWEEKLY -> "Quincenal"
                                        BudgetCycleType.CUSTOM_PAYDAY -> "Nómina"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 5. NOTIFICACIONES & ALERTAS
                Text(
                    text = "NOTIFICACIONES & ALERTAS ACTIVAS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentCyan,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Alerta al llegar al $warningThreshold% del sobre", fontSize = 12.sp, color = textPrimary)
                        Text("Notifica preventivamente antes de sobregirarse", fontSize = 10.sp, color = textSecondary)
                    }
                    Switch(
                        checked = notifyOnThreshold,
                        onCheckedChange = { notifyOnThreshold = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = primaryBlue)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Alerta de sobregiro (100%+)", fontSize = 12.sp, color = textPrimary)
                        Text("Alerta crítica si una categoría supera el monto asignado", fontSize = 10.sp, color = textSecondary)
                    }
                    Switch(
                        checked = notifyOnOverdraft,
                        onCheckedChange = { notifyOnOverdraft = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFEF4444))
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Alerta de Ritmo de Gasto (Burn Rate)", fontSize = 12.sp, color = textPrimary)
                        Text("Advierte si la proyección diaria gastará todo antes de fin de mes", fontSize = 10.sp, color = textSecondary)
                    }
                    Switch(
                        checked = notifyOnBurnRateRisk,
                        onCheckedChange = { notifyOnBurnRateRisk = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFF59E0B))
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, cardBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", color = textSecondary)
                    }

                    Button(
                        onClick = {
                            val newConfig = BudgetConfiguration(
                                distributionRule = selectedRule,
                                customNeedsPct = customNeeds,
                                customWantsPct = customWants,
                                customSavingsPct = customSavings,
                                warningThresholdPct = warningThreshold,
                                budgetingStyle = budgetingStyle,
                                cycleType = cycleType,
                                notifyOnThreshold = notifyOnThreshold,
                                notifyOnOverdraft = notifyOnOverdraft,
                                notifyOnBurnRateRisk = notifyOnBurnRateRisk
                            )
                            onSaveConfig(newConfig)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guardar Cambios", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
