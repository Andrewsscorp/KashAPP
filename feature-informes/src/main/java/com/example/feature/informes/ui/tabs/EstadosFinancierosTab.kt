package com.example.feature.informes.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.informes.domain.model.*
import com.example.feature.informes.presentation.FinancialStatementSubTab
import com.example.feature.informes.ui.components.HorizontalScrollHint
import com.example.feature.informes.ui.components.InformesFormatters

@Composable
fun EstadosFinancierosTab(
    activeSubTab: FinancialStatementSubTab,
    onSelectSubTab: (FinancialStatementSubTab) -> Unit,
    balanceSheet: BalanceSheetData,
    incomeStatement: IncomeStatementData,
    cashFlow: CashFlowStatementData,
    equityStatement: EquityStatementData,
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
        // Horizontal scroll hint for tabs
        HorizontalScrollHint(text = "Desliza a la izquierda para ver los 4 estados financieros")

        // Sub-Tab Switcher for the 4 financial statements
        ScrollableTabRow(
            selectedTabIndex = activeSubTab.ordinal,
            containerColor = Color(0xFF1E293B),
            contentColor = Color(0xFF38BDF8),
            edgePadding = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            FinancialStatementSubTab.values().forEach { sub ->
                Tab(
                    selected = activeSubTab == sub,
                    onClick = { onSelectSubTab(sub) },
                    text = {
                        Text(
                            text = sub.label,
                            fontSize = 12.sp,
                            fontWeight = if (activeSubTab == sub) FontWeight.Bold else FontWeight.Normal,
                            color = if (activeSubTab == sub) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                        )
                    }
                )
            }
        }

        when (activeSubTab) {
            FinancialStatementSubTab.BALANCE_GENERAL -> {
                BalanceGeneralView(balanceSheet, currency, onShowCalculationTrace)
            }
            FinancialStatementSubTab.ESTADO_RESULTADOS -> {
                EstadoResultadosView(incomeStatement, currency, onShowCalculationTrace)
            }
            FinancialStatementSubTab.FLUJO_EFECTIVO -> {
                FlujoEfectivoView(cashFlow, currency, onShowCalculationTrace)
            }
            FinancialStatementSubTab.CAMBIOS_PATRIMONIO -> {
                CambiosPatrimonioView(equityStatement, currency, onShowCalculationTrace)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun BalanceGeneralView(
    balance: BalanceSheetData,
    currency: String,
    onShowCalculationTrace: ((String) -> Unit)? = null
) {
    // Visual Indicator: Estado Cuadrado o Diferencia
    Surface(
        onClick = { onShowCalculationTrace?.invoke("BALANCE_SHEET_EQUILIBRIUM") },
        shape = RoundedCornerShape(14.dp),
        color = if (balance.isBalanced) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFF7F1D1D).copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (balance.isBalanced) Color(0xFF10B981) else Color(0xFFEF4444)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (balance.isBalanced) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (balance.isBalanced) Color(0xFF34D399) else Color(0xFFF87171),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (balance.isBalanced) "✓ ESTADO CUADRADO" else "⚠ DIFERENCIA DETECTADA",
                    color = if (balance.isBalanced) Color(0xFF34D399) else Color(0xFFF87171),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (balance.isBalanced) {
                        "Activos ($%,.0f) = Pasivos ($%,.0f) + Patrimonio ($%,.0f)".format(
                            balance.totalAssets,
                            balance.totalLiabilities,
                            balance.totalEquity
                        )
                    } else {
                        "Diferencia contable: $%,.2f".format(balance.balanceDifference)
                    },
                    color = Color(0xFFE2E8F0),
                    fontSize = 11.sp
                )
            }
        }
    }

    // ACTIVOS
    StatementSectionCard(
        title = "ACTIVOS",
        total = balance.totalAssets,
        colorAccent = Color(0xFF3B82F6),
        currency = currency
    ) {
        StatementSubHeader("Activos Corrientes", balance.totalCurrentAssets, currency)
        balance.currentAssets.forEach {
            StatementItemRow(it.title, it.category, it.amount, it.percentageOfTotal, currency)
        }

        Spacer(modifier = Modifier.height(10.dp))
        StatementSubHeader("Activos No Corrientes", balance.totalNonCurrentAssets, currency)
        balance.nonCurrentAssets.forEach {
            StatementItemRow(it.title, it.category, it.amount, it.percentageOfTotal, currency)
        }
    }

    // PASIVOS
    StatementSectionCard(
        title = "PASIVOS",
        total = balance.totalLiabilities,
        colorAccent = Color(0xFFF59E0B),
        currency = currency
    ) {
        StatementSubHeader("Pasivos Corrientes", balance.totalCurrentLiabilities, currency)
        balance.currentLiabilities.forEach {
            StatementItemRow(it.title, it.category, it.amount, it.percentageOfTotal, currency)
        }

        Spacer(modifier = Modifier.height(10.dp))
        StatementSubHeader("Pasivos No Corrientes", balance.totalNonCurrentLiabilities, currency)
        balance.nonCurrentLiabilities.forEach {
            StatementItemRow(it.title, it.category, it.amount, it.percentageOfTotal, currency)
        }
    }

    // PATRIMONIO
    StatementSectionCard(
        title = "PATRIMONIO",
        total = balance.totalEquity,
        colorAccent = Color(0xFF10B981),
        currency = currency
    ) {
        StatementItemRow("Patrimonio Inicial", "Base", balance.initialEquity, null, currency)
        StatementItemRow("Aportes de Capital", "Aportes", balance.contributions, null, currency)
        StatementItemRow("Resultados Acumulados", "Retenido", balance.retainedEarnings, null, currency)
        StatementItemRow("Resultado del Período", "Superávit", balance.periodResult, null, currency)
        StatementItemRow("Ajustes y Revaluaciones", "NIIF", balance.adjustmentsAndRevaluations, null, currency)
    }
}

@Composable
private fun EstadoResultadosView(
    inc: IncomeStatementData,
    currency: String,
    onShowCalculationTrace: ((String) -> Unit)? = null
) {
    // Executive Summary of Results
    Surface(
        onClick = { onShowCalculationTrace?.invoke("PERIOD_RESULT") },
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
                Text(
                    text = "RESULTADO DEL EJERCICIO",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Ver cálculo",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "¿Cómo se calculó?",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = InformesFormatters.formatMoney(inc.netResult, currency),
                color = if (inc.netResult >= 0) Color(0xFF34D399) else Color(0xFFF87171),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Margen de Ahorro", color = Color(0xFF64748B), fontSize = 11.sp)
                    Text("%.1f%%".format(inc.savingsMarginPct), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Variación vs Anterior", color = Color(0xFF64748B), fontSize = 11.sp)
                    Text("+%.1f%%".format(inc.variationVsPreviousPeriodPct), color = Color(0xFF34D399), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Estructura Fijo/Var", color = Color(0xFF64748B), fontSize = 11.sp)
                    Text("68% / 32%", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Ingresos Agrupados
    StatementSectionCard("INGRESOS POR CATEGORÍA", inc.totalIncome, Color(0xFF10B981), currency) {
        inc.incomeByCategory.forEach {
            StatementItemRow(it.name, "${it.transactionCount} movs", it.amount, it.percentageOfTotal, currency)
        }
    }

    StatementSectionCard("INGRESOS POR FUENTE", inc.totalIncome, Color(0xFF06B6D4), currency) {
        inc.incomeBySource.forEach {
            StatementItemRow(it.name, null, it.amount, it.percentageOfTotal, currency)
        }
    }

    // Gastos Agrupados
    StatementSectionCard("GASTOS POR CATEGORÍA", inc.totalExpenses, Color(0xFFEF4444), currency) {
        inc.expensesByCategory.forEach {
            StatementItemRow(it.name, "${it.transactionCount} movs", it.amount, it.percentageOfTotal, currency)
        }
    }

    StatementSectionCard("GASTOS POR COMERCIO", inc.totalExpenses, Color(0xFFF59E0B), currency) {
        inc.expensesByMerchant.forEach {
            StatementItemRow(it.name, null, it.amount, it.percentageOfTotal, currency)
        }
    }
}

@Composable
private fun FlujoEfectivoView(
    cf: CashFlowStatementData,
    currency: String,
    onShowCalculationTrace: ((String) -> Unit)? = null
) {
    // Flow formula card
    Surface(
        onClick = { onShowCalculationTrace?.invoke("CASH_FLOW") },
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
                Text(
                    text = "ESTADO DE FLUJO DE EFECTIVO (MÉTODO DIRECTO)",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Ver cálculo",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ver cálculo",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            FlowStepRow("Saldo Inicial de Caja & Bancos", cf.initialBalance, false, currency)
            FlowStepRow("(+) Total Entradas de Efectivo", cf.totalInflows, true, currency)
            FlowStepRow("(-) Total Salidas de Efectivo", cf.totalOutflows, false, currency)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF334155))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("(=) SALDO FINAL DISPONIBLE", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(InformesFormatters.formatMoney(cf.finalBalance, currency), color = Color(0xFF34D399), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0F172A)
            ) {
                Text(
                    text = "✓ Las transferencias internas (${cf.internalTransfersExcludedCount} ops por $%,.0f) fueron aisladas para no duplicar el flujo.".format(cf.internalTransfersExcludedVolume),
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }

    // Entradas Detalladas
    StatementSectionCard("ENTRADAS DE EFECTIVO", cf.totalInflows, Color(0xFF10B981), currency) {
        cf.inflows.forEach {
            StatementItemRow(it.concept, it.category, it.amount, it.percentageOfTotal, currency)
        }
    }

    // Salidas Detalladas
    StatementSectionCard("SALIDAS DE EFECTIVO", cf.totalOutflows, Color(0xFFEF4444), currency) {
        cf.outflows.forEach {
            StatementItemRow(it.concept, it.category, it.amount, it.percentageOfTotal, currency)
        }
    }
}

@Composable
private fun CambiosPatrimonioView(
    eq: EquityStatementData,
    currency: String,
    onShowCalculationTrace: ((String) -> Unit)? = null
) {
    Surface(
        onClick = { onShowCalculationTrace?.invoke("NET_WORTH") },
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
                Text(
                    text = "EVOLUCIÓN DEL PATRIMONIO NETO",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Ver fórmula",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ver fórmula",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = InformesFormatters.formatMoney(eq.finalEquity, currency),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Crecimiento patrimonial neto: +%.1f%% en el período".format(eq.netEquityGrowthPct),
                color = Color(0xFF34D399),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    StatementSectionCard("MOVIMIENTOS DE CAPITAL Y RESERVAS", eq.finalEquity, Color(0xFF8B5CF6), currency) {
        eq.changeRows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(row.concept, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    row.note?.let { Text(it, color = Color(0xFF64748B), fontSize = 10.sp) }
                }
                Text(
                    text = InformesFormatters.formatMoney(row.amount, currency),
                    color = if (row.isPositive) Color(0xFF34D399) else Color(0xFFF87171),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatementSectionCard(
    title: String,
    total: Double,
    colorAccent: Color,
    currency: String,
    content: @Composable ColumnScope.() -> Unit
) {
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
                Text(
                    text = title,
                    color = colorAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = InformesFormatters.formatMoney(total, currency),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF334155))
            content()
        }
    }
}

@Composable
private fun StatementSubHeader(title: String, amount: Double, currency: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(InformesFormatters.formatMoney(amount, currency), color = Color(0xFFCBD5E1), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatementItemRow(
    title: String,
    subtitle: String?,
    amount: Double,
    pct: Double?,
    currency: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color(0xFFE2E8F0), fontSize = 12.sp)
            subtitle?.let { Text(it, color = Color(0xFF64748B), fontSize = 10.sp) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = InformesFormatters.formatMoney(amount, currency),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            pct?.let {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "(%.1f%%)".format(it),
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun FlowStepRow(label: String, amount: Double, isPositive: Boolean, currency: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFFCBD5E1), fontSize = 12.sp)
        Text(
            text = InformesFormatters.formatMoney(amount, currency),
            color = if (isPositive) Color(0xFF34D399) else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
