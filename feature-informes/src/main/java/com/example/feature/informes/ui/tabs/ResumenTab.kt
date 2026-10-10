package com.example.feature.informes.ui.tabs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.informes.domain.model.DistributionCategoryItem
import com.example.feature.informes.domain.model.FinancialPeriodFilter
import com.example.feature.informes.domain.model.FinancialSummaryData
import com.example.feature.informes.ui.components.InformesFormatters

@Composable
fun ResumenTab(
    summary: FinancialSummaryData,
    filter: FinancialPeriodFilter,
    currency: String,
    onCategoryClick: (DistributionCategoryItem) -> Unit,
    onOpenFilterModal: () -> Unit,
    onShowCalculationTrace: ((String) -> Unit)? = null
) {
    var selectedEvolutionMetric by remember { mutableStateOf("RESULTADO") } // INGRESOS, GASTOS, RESULTADO

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // 3.1 INDICADOR PRINCIPAL: HERO PATRIMONIO NETO
        // ==========================================
        Surface(
            onClick = { onShowCalculationTrace?.invoke("NET_WORTH") },
            shape = RoundedCornerShape(18.dp),
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF1E3A8A), Color(0xFF0F172A), Color(0xFF064E3B))
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PATRIMONIO NETO",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        // Período seleccionado pill
                        Surface(
                            onClick = onOpenFilterModal,
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = filter.periodLabel,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = InformesFormatters.formatMoney(summary.netWorth, currency),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = InformesFormatters.formatVariation(summary.netWorthVariationPct),
                                    color = Color(0xFF34D399),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "vs. período anterior • Tendencia positiva",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Activos ($%,.0f) − Pasivos ($%,.0f)".format(summary.totalAssets, summary.totalLiabilities),
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            onClick = { onShowCalculationTrace?.invoke("NET_WORTH") },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF38BDF8).copy(alpha = 0.16f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = "Ver cálculo",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Ver fórmula",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3.2 INDICADORES PRINCIPALES (6 TARJETAS EN GRID)
        // ==========================================
        Text(
            text = "INDICADORES DEL PERÍODO",
            color = Color(0xFF64748B),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Activos
            KpiMetricCard(
                title = "Activos",
                amount = summary.totalAssets,
                variationPct = summary.assetsVariationPct,
                detail = "${summary.assetCount} activos registrados",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = Color(0xFF3B82F6),
                currency = currency,
                onClick = { onShowCalculationTrace?.invoke("TOTAL_ASSETS") },
                modifier = Modifier.weight(1f)
            )

            // Pasivos
            KpiMetricCard(
                title = "Pasivos",
                amount = summary.totalLiabilities,
                variationPct = summary.liabilitiesVariationPct,
                detail = "${summary.pendingDebtsCount} deudas pendientes",
                icon = Icons.Default.CreditCard,
                accentColor = Color(0xFFF59E0B),
                currency = currency,
                onClick = { onShowCalculationTrace?.invoke("TOTAL_LIABILITIES") },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Ingresos
            KpiMetricCard(
                title = "Ingresos",
                amount = summary.totalIncome,
                variationPct = summary.incomeVariationPct,
                detail = "Total del período",
                icon = Icons.Default.TrendingUp,
                accentColor = Color(0xFF10B981),
                currency = currency,
                onClick = { onShowCalculationTrace?.invoke("INCOME") },
                modifier = Modifier.weight(1f)
            )

            // Gastos
            KpiMetricCard(
                title = "Gastos",
                amount = summary.totalExpenses,
                variationPct = summary.expenseVariationPct,
                detail = "Total del período",
                icon = Icons.Default.TrendingDown,
                accentColor = Color(0xFFEF4444),
                currency = currency,
                onClick = { onShowCalculationTrace?.invoke("EXPENSES") },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Resultado del período (Ingresos - Gastos)
            KpiMetricCard(
                title = "Resultado",
                amount = summary.periodResult,
                variationPct = 12.4,
                detail = "Ingresos − Gastos (Superávit)",
                icon = Icons.Default.Savings,
                accentColor = Color(0xFF8B5CF6),
                currency = currency,
                onClick = { onShowCalculationTrace?.invoke("PERIOD_RESULT") },
                modifier = Modifier.weight(1f)
            )

            // Flujo de Efectivo
            KpiMetricCard(
                title = "Flujo Neto",
                amount = summary.netCashFlow,
                variationPct = 8.1,
                detail = "Entradas ($%,.0f) − Salidas".format(summary.cashFlowInflows),
                icon = Icons.Default.SwapHoriz,
                accentColor = Color(0xFF06B6D4),
                currency = currency,
                onClick = { onShowCalculationTrace?.invoke("CASH_FLOW") },
                modifier = Modifier.weight(1f)
            )
        }

        // ==========================================
        // 5. GRÁFICO: EVOLUCIÓN FINANCIERA INTERACTIVA
        // ==========================================
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "EVOLUCIÓN FINANCIERA",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Ingresos, Gastos y Resultado",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Metric toggles
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("INGRESOS" to Color(0xFF10B981), "GASTOS" to Color(0xFFEF4444), "RESULTADO" to Color(0xFF38BDF8)).forEach { (key, color) ->
                            val isSel = selectedEvolutionMetric == key
                            Surface(
                                onClick = { selectedEvolutionMetric = key },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) color.copy(alpha = 0.25f) else Color.Transparent
                            ) {
                                Text(
                                    text = key.take(3),
                                    color = if (isSel) color else Color(0xFF64748B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Bar Chart canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    val points = summary.evolutionChartPoints
                    val maxVal = points.maxOfOrNull {
                        when (selectedEvolutionMetric) {
                            "INGRESOS" -> it.income
                            "GASTOS" -> it.expenses
                            else -> it.result
                        }
                    }?.coerceAtLeast(1.0) ?: 10_000_000.0

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        points.forEach { pt ->
                            val ptVal = when (selectedEvolutionMetric) {
                                "INGRESOS" -> pt.income
                                "GASTOS" -> pt.expenses
                                else -> pt.result
                            }
                            val barHeightRatio = (ptVal / maxVal).coerceIn(0.1, 1.0).toFloat()

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = InformesFormatters.formatCompactMoney(ptVal),
                                    color = Color(0xFF94A3B8),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .fillMaxHeight(barHeightRatio * 0.75f)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(
                                            when (selectedEvolutionMetric) {
                                                "INGRESOS" -> Color(0xFF10B981)
                                                "GASTOS" -> Color(0xFFEF4444)
                                                else -> Color(0xFF38BDF8)
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = pt.periodLabel,
                                    color = Color(0xFF64748B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 5. GRÁFICOS DE DISTRIBUCIÓN INTERACTIVOS
        // ==========================================
        Text(
            text = "DISTRIBUCIÓN PATRIMONIAL Y FLUJOS (Toca para ver detalle)",
            color = Color(0xFF64748B),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )

        // Distribution Section 1: Activos por Categoría
        DistributionCard(
            title = "Activos por Categoría",
            subtitle = "Composición del patrimonio bruto",
            items = summary.assetDistributions,
            currency = currency,
            onCategoryClick = onCategoryClick
        )

        // Distribution Section 2: Pasivos por Tipo
        DistributionCard(
            title = "Pasivos por Tipo",
            subtitle = "Obligaciones y deudas financieras",
            items = summary.liabilityDistributions,
            currency = currency,
            onCategoryClick = onCategoryClick
        )

        // Distribution Section 3: Ingresos por Categoría
        DistributionCard(
            title = "Ingresos por Categoría",
            subtitle = "Fuentes de ingreso del período",
            items = summary.incomeDistributions,
            currency = currency,
            onCategoryClick = onCategoryClick
        )

        // Distribution Section 4: Gastos por Categoría
        DistributionCard(
            title = "Gastos por Categoría",
            subtitle = "Destino del gasto mensual",
            items = summary.expenseDistributions,
            currency = currency,
            onCategoryClick = onCategoryClick
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    amount: Double,
    variationPct: Double,
    detail: String,
    icon: ImageVector,
    accentColor: Color,
    currency: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E293B),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = InformesFormatters.formatMoney(amount, currency),
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = InformesFormatters.formatVariation(variationPct),
                        color = if (variationPct >= 0) Color(0xFF34D399) else Color(0xFFF87171),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = detail,
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                if (onClick != null) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Ver cálculo",
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFF38BDF8)
                    )
                }
            }
        }
    }
}

@Composable
private fun DistributionCard(
    title: String,
    subtitle: String,
    items: List<DistributionCategoryItem>,
    currency: String,
    onCategoryClick: (DistributionCategoryItem) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E293B),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = Color(0xFF64748B),
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-segment Horizontal Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF334155))
            ) {
                items.forEach { cat ->
                    val weight = (cat.percentage.toFloat() / 100f).coerceIn(0.01f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(weight)
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(cat.colorHex))
                                } catch (e: Exception) {
                                    Color(0xFF38BDF8)
                                }
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category rows clickable for drill-down
            items.forEach { item ->
                Surface(
                    onClick = { onCategoryClick(item) },
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        try {
                                            Color(android.graphics.Color.parseColor(item.colorHex))
                                        } catch (e: Exception) {
                                            Color(0xFF38BDF8)
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.categoryName,
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = InformesFormatters.formatMoney(item.amount, currency),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(%.1f%%)".format(item.percentage),
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
