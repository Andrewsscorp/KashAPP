package com.example.presentation.dashboard.components.pulse

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.dashboard.model.KpiState
import com.example.util.MoneyFormatter
import java.math.BigDecimal
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * "El pulso de tu mes" Card (Parte 3, Image 2 top).
 * Unified 2x2 grid showing Ingresos, Gastos, Ahorro, and Presupuesto with a bottom progress bar.
 */
@Composable
fun ElPulsoDeTuMesCard(
    currentMonth: YearMonth,
    kpiState: KpiState,
    isPrivacyMode: Boolean = false,
    onBudgetClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colombianLocale = remember { Locale("es", "CO") }
    var selectedDaysFilter by remember { mutableStateOf("30 días") }
    var isFilterMenuOpen by remember { mutableStateOf(false) }

    val monthTag = currentMonth.month.getDisplayName(TextStyle.FULL, colombianLocale).uppercase(colombianLocale)

    val expenseToIncomePct = if (kpiState.income > BigDecimal.ZERO) {
        ((kpiState.expenses.toDouble() / kpiState.income.toDouble()) * 100).toInt().coerceIn(0, 100)
    } else 0

    val budgetUsagePct = if (kpiState.budgetLimit > BigDecimal.ZERO) {
        ((kpiState.expenses.toDouble() / kpiState.budgetLimit.toDouble()) * 100).toInt().coerceIn(0, 100)
    } else 0

    val freeBudgetAmount = (kpiState.budgetLimit - kpiState.expenses).coerceAtLeast(BigDecimal.ZERO)

    val budgetProgress = if (kpiState.budgetLimit > BigDecimal.ZERO) {
        (kpiState.expenses.toFloat() / kpiState.budgetLimit.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = monthTag,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF007BFF),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "El pulso de tu mes",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 30 días ▼ Filter Pill
            Box {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { isFilterMenuOpen = true }
                        .testTag("pulso_dias_filter")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = selectedDaysFilter,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = isFilterMenuOpen,
                    onDismissRequest = { isFilterMenuOpen = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    listOf("7 días", "15 días", "30 días", "Todo el mes").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, fontSize = 13.sp) },
                            onClick = {
                                selectedDaysFilter = option
                                isFilterMenuOpen = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Unified White Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // 2x2 Breakdown
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Row 1: Ingresos & Gastos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PulseMetricItem(
                            label = "Ingresos",
                            amount = MoneyFormatter.format(kpiState.income, isPrivacyMode),
                            subtext = "Programado",
                            subtextColor = Color(0xFF10B981),
                            icon = Icons.Default.SouthWest,
                            iconTint = Color(0xFF10B981),
                            iconBg = Color(0xFFDCFCE7),
                            modifier = Modifier.weight(1f)
                        )

                        PulseMetricItem(
                            label = "Gastos",
                            amount = MoneyFormatter.format(kpiState.expenses, isPrivacyMode),
                            subtext = "$expenseToIncomePct% del ingreso",
                            subtextColor = Color(0xFFEF4444),
                            icon = Icons.Default.ArrowOutward,
                            iconTint = Color(0xFFEF4444),
                            iconBg = Color(0xFFFEE2E2),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // Row 2: Ahorro & Presupuesto
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PulseMetricItem(
                            label = "Ahorro",
                            amount = MoneyFormatter.format(kpiState.savings, isPrivacyMode),
                            subtext = "Meta: 40%",
                            subtextColor = Color(0xFF007BFF),
                            icon = Icons.Default.Savings,
                            iconTint = Color(0xFF007BFF),
                            iconBg = Color(0xFFE0F2FE),
                            modifier = Modifier.weight(1f)
                        )

                        PulseMetricItem(
                            label = "Presupuesto",
                            amount = if (kpiState.budgetLimit > BigDecimal.ZERO) "$budgetUsagePct% usado" else "Sin límite",
                            subtext = if (kpiState.budgetLimit > BigDecimal.ZERO) "${MoneyFormatter.format(freeBudgetAmount, isPrivacyMode)} libres" else "Definir meta",
                            subtextColor = Color(0xFFD97706),
                            icon = Icons.Default.Speed,
                            iconTint = Color(0xFFD97706),
                            iconBg = Color(0xFFFEF3C7),
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = onBudgetClick != null) { onBudgetClick?.invoke() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Progress: "Presupuesto mensual" + "$ 2.850.000 / $ 3.750.000"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = onBudgetClick != null) { onBudgetClick?.invoke() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Presupuesto mensual →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF007BFF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${MoneyFormatter.format(kpiState.expenses, isPrivacyMode)} / ${MoneyFormatter.format(kpiState.budgetLimit, isPrivacyMode)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { budgetProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF007BFF),
                    trackColor = Color(0xFFF1F5F9),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun PulseMetricItem(
    label: String,
    amount: String,
    subtext: String,
    subtextColor: Color,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            val dynamicAmountFontSize = when {
                amount.length > 16 -> 13.sp
                amount.length > 12 -> 15.sp
                else -> 17.sp
            }
            Text(
                text = amount,
                fontSize = dynamicAmountFontSize,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = subtextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
