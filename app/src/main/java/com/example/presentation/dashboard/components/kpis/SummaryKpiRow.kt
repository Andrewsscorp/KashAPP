package com.example.presentation.dashboard.components.kpis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.MonthlyKpiSummary
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashCyan
import com.example.ui.theme.KashNavy
import com.example.ui.theme.KashSuccess
import java.util.Locale

@Composable
fun SummaryKpiRow(
    kpi: MonthlyKpiSummary,
    isPrivacyModeActive: Boolean,
    onNavigateBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    fun formatCurrency(amount: Double): String {
        return if (isPrivacyModeActive) {
            "$ ••••••••"
        } else {
            "$ " + String.format(Locale.GERMANY, "%,.0f", amount)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag("summary_kpi_row")
    ) {
        val isTablet = maxWidth >= 700.dp
        val isNarrowPhone = maxWidth < 360.dp
        val cardPadding = if (isNarrowPhone) 10.dp else 14.dp
        val cardSpacing = if (isNarrowPhone) 8.dp else 12.dp

        if (!isTablet) {
            // 2x2 Grid for Mobile / Compact / Foldables
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(cardSpacing)
            ) {
                // Row 1: Ingresos + Gastos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(cardSpacing)
                ) {
                    KpiMetricCard(
                        title = "Ingresos",
                        amount = formatCurrency(kpi.incomeTotal),
                        variation = "↑ ${kpi.incomeVariation}%",
                        icon = Icons.Default.ArrowUpward,
                        iconTint = Color(0xFF10B981),
                        iconBg = Color(0xFFD1FAE5),
                        isNarrow = isNarrowPhone,
                        modifier = Modifier.weight(1f)
                    )

                    KpiMetricCard(
                        title = "Gastos",
                        amount = formatCurrency(kpi.expenseTotal),
                        variation = "↑ ${kpi.expenseVariation}%",
                        icon = Icons.Default.ArrowDownward,
                        iconTint = Color(0xFFEF4444),
                        iconBg = Color(0xFFFEE2E2),
                        isNarrow = isNarrowPhone,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Ahorro + Presupuesto
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(cardSpacing)
                ) {
                    KpiMetricCard(
                        title = "Ahorro",
                        amount = formatCurrency(kpi.savingsTotal),
                        variation = "↑ ${kpi.savingsVariation}%",
                        icon = Icons.Default.Savings,
                        iconTint = KashCyan,
                        iconBg = Color(0xFFE0F2FE),
                        isNarrow = isNarrowPhone,
                        modifier = Modifier.weight(1f)
                    )

                    BudgetKpiCard(
                        kpi = kpi,
                        formatCurrency = ::formatCurrency,
                        onNavigateBudget = onNavigateBudget,
                        isNarrow = isNarrowPhone,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            // 1x4 Row for wide screens and tablets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiMetricCard(
                    title = "Ingresos",
                    amount = formatCurrency(kpi.incomeTotal),
                    variation = "↑ ${kpi.incomeVariation}%",
                    icon = Icons.Default.ArrowUpward,
                    iconTint = Color(0xFF10B981),
                    iconBg = Color(0xFFD1FAE5),
                    isNarrow = false,
                    modifier = Modifier.weight(1f)
                )

                KpiMetricCard(
                    title = "Gastos",
                    amount = formatCurrency(kpi.expenseTotal),
                    variation = "↑ ${kpi.expenseVariation}%",
                    icon = Icons.Default.ArrowDownward,
                    iconTint = Color(0xFFEF4444),
                    iconBg = Color(0xFFFEE2E2),
                    isNarrow = false,
                    modifier = Modifier.weight(1f)
                )

                KpiMetricCard(
                    title = "Ahorro",
                    amount = formatCurrency(kpi.savingsTotal),
                    variation = "↑ ${kpi.savingsVariation}%",
                    icon = Icons.Default.Savings,
                    iconTint = KashCyan,
                    iconBg = Color(0xFFE0F2FE),
                    isNarrow = false,
                    modifier = Modifier.weight(1f)
                )

                BudgetKpiCard(
                    kpi = kpi,
                    formatCurrency = ::formatCurrency,
                    onNavigateBudget = onNavigateBudget,
                    isNarrow = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    amount: String,
    variation: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    isNarrow: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("kpi_card_$title"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isNarrow) 10.dp else 14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isNarrow) 24.dp else 28.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(if (isNarrow) 14.dp else 16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(if (isNarrow) 6.dp else 8.dp))
                Text(
                    text = title,
                    fontSize = if (isNarrow) 11.sp else 13.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(if (isNarrow) 6.dp else 10.dp))

            Text(
                text = amount,
                fontSize = if (isNarrow) 14.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                color = KashNavy,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(if (isNarrow) 2.dp else 4.dp))

            Text(
                text = variation,
                fontSize = if (isNarrow) 10.sp else 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (title == "Gastos") Color(0xFFEF4444) else KashSuccess,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BudgetKpiCard(
    kpi: MonthlyKpiSummary,
    formatCurrency: (Double) -> String,
    onNavigateBudget: () -> Unit,
    isNarrow: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .testTag("kpi_card_presupuesto")
            .clickable { onNavigateBudget() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isNarrow) 10.dp else 14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isNarrow) 24.dp else 28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEDE9FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrackChanges,
                            contentDescription = "Presupuesto",
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(if (isNarrow) 14.dp else 16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(if (isNarrow) 6.dp else 8.dp))
                    Text(
                        text = "Presupuesto",
                        fontSize = if (isNarrow) 11.sp else 13.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Ver presupuesto",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(if (isNarrow) 16.dp else 18.dp)
                )
            }

            Spacer(modifier = Modifier.height(if (isNarrow) 6.dp else 10.dp))

            Text(
                text = "${kpi.budgetPercentage}%",
                fontSize = if (isNarrow) 14.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                color = KashNavy,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(if (isNarrow) 4.dp else 8.dp))

            val progressColor = when {
                kpi.budgetPercentage > 90 -> Color(0xFFEF4444)
                kpi.budgetPercentage >= 75 -> Color(0xFFF59E0B)
                else -> KashBlue
            }

            LinearProgressIndicator(
                progress = { (kpi.budgetPercentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isNarrow) 4.dp else 6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = progressColor,
                trackColor = Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(if (isNarrow) 4.dp else 6.dp))

            Text(
                text = "${formatCurrency(kpi.budgetSpent)} / ${formatCurrency(kpi.budgetTotal)}",
                fontSize = if (isNarrow) 9.sp else 10.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Start,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
