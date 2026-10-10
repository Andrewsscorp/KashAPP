package com.example.presentation.dashboard.components.kpis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.dashboard.model.KpiState
import com.example.util.MoneyFormatter
import java.math.BigDecimal

/**
 * 4. KpiMetricsGrid.kt: 2-column grid showing Income, Expenses, Savings, and Budget ceiling.
 */
@Composable
fun KpiMetricsGrid(
    kpiState: KpiState,
    isPrivacyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isOverBudget = kpiState.budgetLimit > BigDecimal.ZERO && kpiState.expenses > kpiState.budgetLimit
    val budgetProgress = if (kpiState.budgetLimit > BigDecimal.ZERO) {
        (kpiState.expenses.toFloat() / kpiState.budgetLimit.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Ingresos & Gastos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(
                title = "Ingresos",
                amount = kpiState.income,
                icon = Icons.Default.ArrowDownward,
                iconTint = Color(0xFF10B981),
                iconBg = Color(0xFFDCFCE7),
                isPrivacyMode = isPrivacyMode,
                modifier = Modifier.weight(1f)
            )

            KpiCard(
                title = "Gastos",
                amount = kpiState.expenses,
                icon = Icons.Default.ArrowUpward,
                iconTint = Color(0xFFEF4444),
                iconBg = Color(0xFFFEE2E2),
                isPrivacyMode = isPrivacyMode,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Ahorro & Presupuesto
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(
                title = "Ahorro Neto",
                amount = kpiState.savings,
                icon = Icons.Default.Savings,
                iconTint = Color(0xFF0284C7),
                iconBg = Color(0xFFE0F2FE),
                isPrivacyMode = isPrivacyMode,
                modifier = Modifier.weight(1f)
            )

            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_card_budget"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Presupuesto",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isOverBudget) Color(0xFFFEE2E2) else Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = if (isOverBudget) Color(0xFFEF4444) else Color(0xFF64748B),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = MoneyFormatter.format(kpiState.budgetLimit, isPrivacyMode),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { budgetProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isOverBudget) Color(0xFFEF4444) else Color(0xFF0284C7),
                        trackColor = Color(0xFFE2E8F0),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    amount: BigDecimal,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    isPrivacyMode: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("kpi_card_${title.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = MoneyFormatter.format(amount, isPrivacyMode),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }
    }
}
