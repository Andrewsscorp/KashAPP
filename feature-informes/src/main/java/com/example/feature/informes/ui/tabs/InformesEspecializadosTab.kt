package com.example.feature.informes.ui.tabs

import androidx.compose.foundation.background
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
import com.example.feature.informes.domain.model.SpecializedReportType
import com.example.feature.informes.domain.model.SpecializedReportsBundle
import com.example.feature.informes.ui.components.HorizontalScrollHint
import com.example.feature.informes.ui.components.InformesFormatters

@Composable
fun InformesEspecializadosTab(
    activeReport: SpecializedReportType,
    onSelectReport: (SpecializedReportType) -> Unit,
    reportsBundle: SpecializedReportsBundle,
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
        // Horizontal scroll hint for specialized reports selector
        HorizontalScrollHint(text = "Desliza a la izquierda para ver los 7 informes especializados")

        // Horizontal Chip Selector for the 7 specialized reports
        ScrollableTabRow(
            selectedTabIndex = activeReport.ordinal,
            containerColor = Color(0xFF1E293B),
            contentColor = Color(0xFF38BDF8),
            edgePadding = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            SpecializedReportType.values().forEach { rType ->
                Tab(
                    selected = activeReport == rType,
                    onClick = { onSelectReport(rType) },
                    text = {
                        Text(
                            text = rType.label,
                            fontSize = 12.sp,
                            fontWeight = if (activeReport == rType) FontWeight.Bold else FontWeight.Normal,
                            color = if (activeReport == rType) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                        )
                    }
                )
            }
        }

        when (activeReport) {
            SpecializedReportType.INCOME -> {
                IncomeReportSection(reportsBundle, currency, onShowCalculationTrace)
            }
            SpecializedReportType.EXPENSES -> {
                ExpensesReportSection(reportsBundle, currency, onShowCalculationTrace)
            }
            SpecializedReportType.ACCOUNTS -> {
                AccountsReportSection(reportsBundle, currency, onShowCalculationTrace)
            }
            SpecializedReportType.CREDIT_CARDS -> {
                CreditCardsReportSection(reportsBundle, currency, onShowCalculationTrace)
            }
            SpecializedReportType.DEBTS -> {
                DebtsReportSection(reportsBundle, currency, onShowCalculationTrace)
            }
            SpecializedReportType.FIXED_ASSETS -> {
                FixedAssetsReportSection(reportsBundle, currency, onShowCalculationTrace)
            }
            SpecializedReportType.INVESTMENTS -> {
                InvestmentsReportSection(reportsBundle, currency, onShowCalculationTrace)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun IncomeReportSection(
    bundle: SpecializedReportsBundle,
    currency: String,
    onShowCalculationTrace: ((String) -> Unit)? = null
) {
    Surface(
        onClick = { onShowCalculationTrace?.invoke("INCOME") },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF10B981).copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("INFORME DE INGRESOS", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            CalculationHintLabel()
        }
    }

    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF1E293B), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Ingresos por Categoría y Fuente", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            bundle.incomeByCategoryList.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(item.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("${item.transactionCount} entradas registradas", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(InformesFormatters.formatMoney(item.amount, currency), color = Color(0xFF34D399), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("(%.1f%%)".format(item.percentageOfTotal), color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpensesReportSection(
    bundle: SpecializedReportsBundle,
    currency: String,
    onShowCalculationTrace: ((String) -> Unit)? = null
) {
    Surface(
        onClick = { onShowCalculationTrace?.invoke("EXPENSES") },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFEF4444).copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("INFORME DE GASTOS", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            CalculationHintLabel()
        }
    }

    // Recurrentes vs Variables
    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF1E293B), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Gastos Recurrentes (Fijos)", color = Color(0xFFF59E0B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            bundle.recurringExpensesList.forEach {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(it.name, color = Color(0xFFE2E8F0), fontSize = 12.sp)
                    Text(InformesFormatters.formatMoney(it.amount, currency), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFF334155))

            Text("Gastos Variables (Discrecionales)", color = Color(0xFFEC4899), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            bundle.variableExpensesList.forEach {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(it.name, color = Color(0xFFE2E8F0), fontSize = 12.sp)
                    Text(InformesFormatters.formatMoney(it.amount, currency), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun AccountsReportSection(
    bundle: SpecializedReportsBundle,
    currency: String,
    onShowCalculationTrace: ((String) -> Unit)? = null
) {
    Surface(
        onClick = { onShowCalculationTrace?.invoke("CASH_FLOW") },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF38BDF8).copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("INFORME DE CUENTAS BANCARIAS", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            CalculationHintLabel()
        }
    }

    bundle.accountReports.forEach { acc ->
        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF1E293B), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(acc.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(InformesFormatters.formatMoney(acc.currentBalance, currency), color = Color(0xFF38BDF8), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Text(acc.bankName, color = Color(0xFF94A3B8), fontSize = 11.sp)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF334155))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Saldo Inicial", color = Color(0xFF64748B), fontSize = 10.sp)
                        Text(InformesFormatters.formatMoney(acc.initialBalance, currency), color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    }
                    Column {
                        Text("Entradas", color = Color(0xFF64748B), fontSize = 10.sp)
                        Text(InformesFormatters.formatMoney(acc.totalInflows, currency), color = Color(0xFF34D399), fontSize = 12.sp)
                    }
                    Column {
                        Text("Salidas", color = Color(0xFF64748B), fontSize = 10.sp)
                        Text(InformesFormatters.formatMoney(acc.totalOutflows, currency), color = Color(0xFFF87171), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreditCardsReportSection(
    bundle: SpecializedReportsBundle,
    currency: String,
    onShowCalculationTrace: ((String) -> Unit)? = null
) {
    Surface(
        onClick = { onShowCalculationTrace?.invoke("TOTAL_LIABILITIES") },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFA78BFA).copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("INFORME DE TARJETAS DE CRÉDITO", color = Color(0xFFA78BFA), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            CalculationHintLabel()
        }
    }

    bundle.creditCardReports.forEach { card ->
        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF1E293B), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(card.cardName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("%.1f%% Usado".format(card.utilizationPct), color = if (card.utilizationPct > 50) Color(0xFFF59E0B) else Color(0xFF34D399), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(card.bankName, color = Color(0xFF94A3B8), fontSize = 11.sp)

                Spacer(modifier = Modifier.height(8.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { (card.utilizationPct / 100f).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = if (card.utilizationPct > 50) Color(0xFFF59E0B) else Color(0xFFA78BFA),
                    trackColor = Color(0xFF334155)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Cupo Total", color = Color(0xFF64748B), fontSize = 10.sp)
                        Text(InformesFormatters.formatMoney(card.quotaTotal, currency), color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    }
                    Column {
                        Text("Utilizado", color = Color(0xFF64748B), fontSize = 10.sp)
                        Text(InformesFormatters.formatMoney(card.quotaUsed, currency), color = Color(0xFFF87171), fontSize = 11.sp)
                    }
                    Column {
                        Text("Disponible", color = Color(0xFF64748B), fontSize = 10.sp)
                        Text(InformesFormatters.formatMoney(card.quotaAvailable, currency), color = Color(0xFF34D399), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtsReportSection(
    bundle: SpecializedReportsBundle,
    currency: String,
    onShowCalculationTrace: ((String) -> Unit)? = null
) {
    Surface(
        onClick = { onShowCalculationTrace?.invoke("TOTAL_LIABILITIES") },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("INFORME DE DEUDAS Y PASIVOS", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            CalculationHintLabel()
        }
    }

    bundle.debtReports.forEach { debt ->
        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF1E293B), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(debt.creditorName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(debt.status, color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text("Tipo: ${debt.debtType} • Vencimiento: ${debt.nextDueDate}", color = Color(0xFF94A3B8), fontSize = 11.sp)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF334155))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Saldo Inicial", color = Color(0xFF64748B), fontSize = 10.sp)
                        Text(InformesFormatters.formatMoney(debt.initialBalance, currency), color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    }
                    Column {
                        Text("Amortizado", color = Color(0xFF64748B), fontSize = 10.sp)
                        Text(InformesFormatters.formatMoney(debt.amortizedCapital, currency), color = Color(0xFF34D399), fontSize = 11.sp)
                    }
                    Column {
                        Text("Saldo Pendiente", color = Color(0xFF64748B), fontSize = 10.sp)
                        Text(InformesFormatters.formatMoney(debt.currentBalance, currency), color = Color(0xFFF59E0B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FixedAssetsReportSection(
    bundle: SpecializedReportsBundle,
    currency: String,
    onShowCalculationTrace: ((String) -> Unit)? = null
) {
    Surface(
        onClick = { onShowCalculationTrace?.invoke("TOTAL_ASSETS") },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF10B981).copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("INFORME DE ACTIVOS FIJOS TANGIBLES", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            CalculationHintLabel()
        }
    }

    bundle.assetReports.forEach { asset ->
        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF1E293B), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(asset.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(asset.status, color = Color(0xFF38BDF8), fontSize = 11.sp)
                }
                Text("Código: ${asset.assetCode} • Categoría: ${asset.categoryName}", color = Color(0xFF94A3B8), fontSize = 11.sp)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF334155))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Costo Adquisición", color = Color(0xFF64748B), fontSize = 10.sp)
                        Text(InformesFormatters.formatMoney(asset.acquisitionCost, currency), color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    }
                    Column {
                        Text("Deprec. Acumulada", color = Color(0xFF64748B), fontSize = 10.sp)
                        Text(InformesFormatters.formatMoney(asset.accumulatedDepreciation, currency), color = Color(0xFFF87171), fontSize = 11.sp)
                    }
                    Column {
                        Text("Valor en Libros", color = Color(0xFF64748B), fontSize = 10.sp)
                        Text(InformesFormatters.formatMoney(asset.bookValue, currency), color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun InvestmentsReportSection(
    bundle: SpecializedReportsBundle,
    currency: String,
    onShowCalculationTrace: ((String) -> Unit)? = null
) {
    Surface(
        onClick = { onShowCalculationTrace?.invoke("TOTAL_ASSETS") },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF38BDF8).copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("INFORME DE INVERSIONES Y RENDIMIENTOS", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            CalculationHintLabel()
        }
    }

    bundle.investmentReports.forEach { inv ->
        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF1E293B), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(inv.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("+%.1f%% Retorno".format(inv.returnPercentage), color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text("Plataforma: ${inv.entityOrPlatform}", color = Color(0xFF94A3B8), fontSize = 11.sp)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF334155))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Capital Invertido", color = Color(0xFF64748B), fontSize = 10.sp)
                        Text(InformesFormatters.formatMoney(inv.investedCapital, currency), color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    }
                    Column {
                        Text("Rendimiento Neto", color = Color(0xFF64748B), fontSize = 10.sp)
                        Text(InformesFormatters.formatMoney(inv.generatedYields, currency), color = Color(0xFF34D399), fontSize = 11.sp)
                    }
                    Column {
                        Text("Valor Actual", color = Color(0xFF64748B), fontSize = 10.sp)
                        Text(InformesFormatters.formatMoney(inv.currentValue, currency), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalculationHintLabel() {
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
