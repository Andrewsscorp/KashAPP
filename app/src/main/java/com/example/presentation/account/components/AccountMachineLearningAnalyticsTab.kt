package com.example.presentation.account.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.analytics.FinancialMachineLearningEngine
import com.example.util.MoneyFormatter
import java.math.BigDecimal
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Enterprise Intelligent Analytics & Machine Learning Module for "Mis Cuentas Bancarias".
 * 
 * Implements REAL Machine Learning models:
 * 1. Holt-Winters Double Exponential Smoothing for 30-day balance trajectory forecasting.
 * 2. Monte Carlo Stochastic Simulation (500 iterations) assessing liquidity and runway survival.
 * 3. Inflow vs Outflow Cashflow canvas graphics with burn-rate tracking.
 * 4. Portfolio diversification & concentration risk canvas donut chart.
 * 5. Interactive "What-If" scenario predictive modeling engine.
 */
@Composable
fun AccountMachineLearningAnalyticsTab(
    accounts: List<AccountEntity>,
    allTransactions: List<TransactionEntity>,
    totalBalance: Double,
    modifier: Modifier = Modifier
) {
    var selectedAnalysisMode by remember { mutableIntStateOf(0) } // 0: ML Forecast, 1: Cashflow, 2: Diversification
    var selectedAccountId by remember { mutableStateOf<String?>(null) } // null = Consolidated portfolio
    var whatIfExpenseAdjustment by remember { mutableDoubleStateOf(0.0) } // e.g. -10.0%
    var whatIfExtraIncome by remember { mutableDoubleStateOf(0.0) } // e.g. +500.000

    // Selected account context or consolidated virtual account
    val activeAccount = remember(accounts, selectedAccountId, totalBalance) {
        if (selectedAccountId == null) {
            AccountEntity(
                id = "consolidated_all",
                userId = "default_user",
                name = "Portafolio Consolidado",
                type = "TOTAL",
                balance = totalBalance,
                currency = "COP",
                bankName = "Todos los Bancos",
                customColorHex = "#007BFF"
            )
        } else {
            accounts.find { it.id == selectedAccountId } ?: AccountEntity(
                id = "fallback",
                userId = "default_user",
                name = "Cuenta",
                type = "SAVINGS",
                balance = 0.0,
                currency = "COP",
                bankName = "Banco",
                customColorHex = "#0284C7"
            )
        }
    }

    val activeTransactions = remember(allTransactions, selectedAccountId) {
        if (selectedAccountId == null) allTransactions
        else allTransactions.filter { it.accountId == selectedAccountId }
    }

    // Compute REAL Machine Learning analysis
    val mlSummary = remember(activeAccount, activeTransactions, whatIfExpenseAdjustment, whatIfExtraIncome) {
        FinancialMachineLearningEngine.analyzeAccount(
            account = activeAccount,
            transactions = activeTransactions,
            expenseAdjustmentPct = whatIfExpenseAdjustment,
            extraMonthlyIncome = whatIfExtraIncome
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth > 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ==========================================
            // HEADER & ACCOUNT SELECTOR CHIPS
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "ANALÍTICA Y MACHINE LEARNING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = "Modelos predictivos en tiempo real",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                        Text(
                            text = "ML v2.4 Activo",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34D399)
                        )
                    }
                }
            }

            // Account selection pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { selectedAccountId = null },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedAccountId == null) Color(0xFF007BFF) else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (selectedAccountId == null) Color(0xFF38BDF8) else Color(0xFF334155))
                ) {
                    Text(
                        text = "Consolidado Total (${accounts.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }

                accounts.forEach { acc ->
                    val isSelected = (acc.id == selectedAccountId)
                    Surface(
                        onClick = { selectedAccountId = acc.id },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFF007BFF) else Color(0xFF1E293B),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155))
                    ) {
                        Text(
                            text = "${acc.name} (${acc.bankName})",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ==========================================
            // ANALYSIS MODE SEGMENTED CONTROL
            // ==========================================
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf(
                        Triple(0, "Proyección ML", Icons.Default.AutoGraph),
                        Triple(1, "Flujo de Caja", Icons.Default.BarChart),
                        Triple(2, "Diversificación", Icons.Default.PieChart)
                    )

                    tabs.forEach { (index, title, icon) ->
                        val isSelected = (selectedAnalysisMode == index)
                        Surface(
                            onClick = { selectedAnalysisMode = index },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF1E293B) else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // ACTIVE ANALYSIS CONTENT
            // ==========================================
            when (selectedAnalysisMode) {
                0 -> {
                    // MODE 0: MACHINE LEARNING FORECAST (HOLT-WINTERS & MONTE CARLO)
                    MlForecastSection(
                        mlSummary = mlSummary,
                        account = activeAccount,
                        whatIfExpenseAdjustment = whatIfExpenseAdjustment,
                        whatIfExtraIncome = whatIfExtraIncome,
                        onExpenseAdjChange = { whatIfExpenseAdjustment = it },
                        onExtraIncomeChange = { whatIfExtraIncome = it }
                    )
                }
                1 -> {
                    // MODE 1: CASHFLOW & BURN RATE ANALYSIS
                    CashFlowAnalysisSection(
                        mlSummary = mlSummary,
                        account = activeAccount,
                        transactions = activeTransactions
                    )
                }
                2 -> {
                    // MODE 2: ASSET ALLOCATION & RISK DIVERSIFICATION
                    PortfolioDiversificationSection(
                        accounts = accounts,
                        totalBalance = totalBalance
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==========================================================
// SUB-SECTION 1: MACHINE LEARNING FORECAST & MONTE CARLO
// ==========================================================
@Composable
private fun MlForecastSection(
    mlSummary: FinancialMachineLearningEngine.AccountAnalyticsSummary,
    account: AccountEntity,
    whatIfExpenseAdjustment: Double,
    whatIfExtraIncome: Double,
    onExpenseAdjChange: (Double) -> Unit,
    onExtraIncomeChange: (Double) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Main Visual Canvas Forecast Chart
        Card(
            modifier = Modifier.fillMaxWidth().testTag("ml_forecast_chart_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Trayectoria Predictiva a 30 Días",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Suavizamiento Exponencial Doble (Nivel + Tendencia)",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Banda 90% Confianza",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real Canvas Curve Chart
                ForecastCanvasChart(
                    points = mlSummary.forecastPoints,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Chart Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF38BDF8)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Histórico", fontSize = 10.sp, color = Color(0xFFCBD5E1))

                    Spacer(modifier = Modifier.width(14.dp))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Proyección ML", fontSize = 10.sp, color = Color(0xFFCBD5E1))

                    Spacer(modifier = Modifier.width(14.dp))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF8B5CF6).copy(alpha = 0.6f)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Intervalo Estocástico", fontSize = 10.sp, color = Color(0xFFCBD5E1))
                }
            }
        }

        // ==========================================
        // MONTE CARLO STOCHASTIC SIMULATION RESULTS
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(18.dp))
                        Text(
                            text = "Simulación Monte Carlo (500 Ensayos)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    val riskColor = when (mlSummary.monteCarlo.riskClassification) {
                        "BAJO", "SOLVENTE" -> Color(0xFF10B981)
                        "MODERADO" -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = riskColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, riskColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Riesgo ${mlSummary.monteCarlo.riskClassification}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = riskColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4-box metrics grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricBox(
                        title = "Prob. Solvencia 30D",
                        value = "${String.format(Locale.US, "%.1f", mlSummary.monteCarlo.probabilityOfSolvency30Days)}%",
                        color = if (mlSummary.monteCarlo.probabilityOfSolvency30Days >= 80.0) Color(0xFF10B981) else Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Autonomía (Runway)",
                        value = if (mlSummary.monteCarlo.runwayDays >= 365) "> 1 Año" else "${mlSummary.monteCarlo.runwayDays} días",
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricBox(
                        title = "Saldo Mediano 30D",
                        value = MoneyFormatter.format(BigDecimal.valueOf(mlSummary.monteCarlo.medianBalance30Days), false, account.currency),
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Escenario Crítico (P10)",
                        value = MoneyFormatter.format(BigDecimal.valueOf(mlSummary.monteCarlo.pessimisticPercentile10), false, account.currency),
                        color = if (mlSummary.monteCarlo.pessimisticPercentile10 < 0) Color(0xFFEF4444) else Color(0xFFFBBF24),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Smart Recommendation Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                        )
                        Column {
                            Text("Diagnóstico Inteligente", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = mlSummary.smartRecommendation,
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // INTERACTIVE "WHAT-IF" SIMULATOR CONTROLS
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF007BFF).copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                        Text(
                            text = "Simulador '¿Qué pasaría si...?'",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (whatIfExpenseAdjustment != 0.0 || whatIfExtraIncome != 0.0) {
                        Surface(
                            onClick = {
                                onExpenseAdjChange(0.0)
                                onExtraIncomeChange(0.0)
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF334155)
                        ) {
                            Text(
                                text = "Restablecer",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Ajuste hipotético de gastos mensuales:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val expenseOptions = listOf(
                        -20.0 to "-20%",
                        -10.0 to "-10%",
                        0.0 to "Normal (0%)",
                        15.0 to "+15%"
                    )

                    expenseOptions.forEach { (pct, label) ->
                        val isSelected = whatIfExpenseAdjustment == pct
                        Surface(
                            onClick = { onExpenseAdjChange(pct) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF007BFF) else Color(0xFF0F172A),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Inyección de ingreso adicional recurrente:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val incomeOptions = listOf(
                        0.0 to "$ 0",
                        500000.0 to "+500k",
                        1000000.0 to "+1M",
                        2500000.0 to "+2.5M"
                    )

                    incomeOptions.forEach { (amt, label) ->
                        val isSelected = whatIfExtraIncome == amt
                        Surface(
                            onClick = { onExtraIncomeChange(amt) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF10B981) else Color(0xFF0F172A),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF34D399) else Color(0xFF334155)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// SUB-SECTION 2: CASHFLOW & BURN RATE SECTION
// ==========================================================
@Composable
private fun CashFlowAnalysisSection(
    mlSummary: FinancialMachineLearningEngine.AccountAnalyticsSummary,
    account: AccountEntity,
    transactions: List<TransactionEntity>
) {
    val totalIn = mlSummary.totalInflow
    val totalOut = mlSummary.totalOutflow
    val netCash = mlSummary.netCashFlow
    val burnRate = mlSummary.averageDailyBurnRate

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Cashflow Bar Comparison Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Comparativa de Flujo de Fondos",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Ingresos Entrantes vs Gastos Salientes",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(16.dp))

                CashFlowBarChart(
                    totalInflow = totalIn,
                    totalOutflow = totalOut,
                    currency = account.currency,
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricBox(
                        title = "Tasa de Quema Diaria",
                        value = MoneyFormatter.format(BigDecimal.valueOf(burnRate), false, account.currency),
                        color = Color(0xFFF87171),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Flujo Neto Consolidado",
                        value = MoneyFormatter.format(BigDecimal.valueOf(netCash), false, account.currency),
                        color = if (netCash >= 0) Color(0xFF34D399) else Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Volatility & Stability Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Índice de Volatilidad del Gasto",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${String.format(Locale.US, "%.1f", mlSummary.volatilityIndex)}%",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF38BDF8)
                        )
                        Text(
                            text = "Nivel de dispersión: ${mlSummary.volatilityTier}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text(
                            text = "${transactions.size} transacciones",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { (mlSummary.volatilityIndex / 100.0).toFloat().coerceIn(0.05f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = if (mlSummary.volatilityIndex > 60.0) Color(0xFFEF4444) else if (mlSummary.volatilityIndex > 30.0) Color(0xFFF59E0B) else Color(0xFF10B981),
                    trackColor = Color(0xFF0F172A)
                )
            }
        }
    }
}

// ==========================================================
// SUB-SECTION 3: PORTFOLIO DIVERSIFICATION DONUT
// ==========================================================
@Composable
private fun PortfolioDiversificationSection(
    accounts: List<AccountEntity>,
    totalBalance: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Distribución y Diversificación de Activos",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Participación porcentual por entidad bancaria",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Donut Chart
            PortfolioDonutCanvas(
                accounts = accounts,
                totalBalance = totalBalance,
                modifier = Modifier.fillMaxWidth().height(190.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bank items detail list
            accounts.forEach { acc ->
                val percentage = if (totalBalance > 0) ((acc.balance / totalBalance) * 100.0).coerceIn(0.0, 100.0) else 0.0
                val color = try {
                    Color(android.graphics.Color.parseColor(acc.customColorHex.ifBlank { "#0284C7" }))
                } catch (_: Exception) {
                    Color(0xFF0284C7)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                        Column {
                            Text(acc.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${acc.bankName} • ${acc.type}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = MoneyFormatter.format(BigDecimal.valueOf(acc.balance), false, acc.currency),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.1f", percentage)}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = color
                        )
                    }
                }
            }
        }
    }
}

// ==========================================================
// CUSTOM CANVAS GRAPHICS
// ==========================================================
@Composable
private fun ForecastCanvasChart(
    points: List<FinancialMachineLearningEngine.ForecastPoint>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height
        val padding = 20f

        val minVal = points.minOfOrNull { it.confidenceLower } ?: 0.0
        val maxVal = points.maxOfOrNull { it.confidenceUpper } ?: 1.0
        val range = (maxVal - minVal).coerceAtLeast(1.0)

        fun toY(v: Double): Float {
            val norm = (v - minVal) / range
            return (h - padding) - (norm.toFloat() * (h - 2 * padding))
        }

        fun toX(i: Int): Float {
            return padding + (i.toFloat() / (points.size - 1).coerceAtLeast(1)) * (w - 2 * padding)
        }

        // Draw soft horizontal gridlines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = padding + i * ((h - 2 * padding) / gridLines)
            drawLine(
                color = Color(0xFF334155).copy(alpha = 0.5f),
                start = Offset(padding, y),
                end = Offset(w - padding, y),
                strokeWidth = 1f
            )
        }

        // 1. Shaded Confidence Interval Envelope (Upper to Lower)
        val upperPath = Path()
        val lowerPath = Path()
        val projectedIndices = points.indices.filter { points[it].isProjected }

        if (projectedIndices.isNotEmpty()) {
            val firstIdx = projectedIndices.first()
            upperPath.moveTo(toX(firstIdx), toY(points[firstIdx].confidenceUpper))
            for (idx in projectedIndices) {
                upperPath.lineTo(toX(idx), toY(points[idx].confidenceUpper))
            }
            for (idx in projectedIndices.reversed()) {
                upperPath.lineTo(toX(idx), toY(points[idx].confidenceLower))
            }
            upperPath.close()

            drawPath(
                path = upperPath,
                color = Color(0xFF8B5CF6).copy(alpha = 0.15f)
            )
        }

        // 2. Trajectory Line (Historical = solid sky blue, Projected = dashed emerald)
        val histPath = Path()
        val projPath = Path()
        var histStarted = false
        var projStarted = false

        points.forEachIndexed { i, p ->
            val x = toX(i)
            val y = toY(p.projectedBalance)

            if (!p.isProjected) {
                if (!histStarted) {
                    histPath.moveTo(x, y)
                    histStarted = true
                } else {
                    histPath.lineTo(x, y)
                }
            } else {
                if (!projStarted) {
                    // Connect with last historical point
                    val prevIdx = (i - 1).coerceAtLeast(0)
                    projPath.moveTo(toX(prevIdx), toY(points[prevIdx].projectedBalance))
                    projPath.lineTo(x, y)
                    projStarted = true
                } else {
                    projPath.lineTo(x, y)
                }
            }
        }

        if (histStarted) {
            drawPath(
                path = histPath,
                color = Color(0xFF38BDF8),
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )
        }

        if (projStarted) {
            drawPath(
                path = projPath,
                color = Color(0xFF10B981),
                style = Stroke(
                    width = 4f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )
        }

        // Draw key milestone markers
        points.forEachIndexed { i, p ->
            if (i == 0 || i == points.size / 2 || i == points.size - 1) {
                val x = toX(i)
                val y = toY(p.projectedBalance)
                drawCircle(
                    color = if (p.isProjected) Color(0xFF10B981) else Color(0xFF38BDF8),
                    radius = 5f,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

@Composable
private fun CashFlowBarChart(
    totalInflow: Double,
    totalOutflow: Double,
    currency: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barWidth = (w * 0.28f).coerceAtMost(90f)
        val maxVal = maxOf(totalInflow, totalOutflow, 1.0)

        val inHeight = (totalInflow / maxVal * (h - 40f)).toFloat().coerceAtLeast(8f)
        val outHeight = (totalOutflow / maxVal * (h - 40f)).toFloat().coerceAtLeast(8f)

        // Baseline
        val baseY = h - 20f
        drawLine(
            color = Color(0xFF334155),
            start = Offset(20f, baseY),
            end = Offset(w - 20f, baseY),
            strokeWidth = 2f
        )

        // Bar 1: Inflow (Green)
        val inX = (w * 0.28f) - (barWidth / 2f)
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF34D399), Color(0xFF059669))),
            topLeft = Offset(inX, baseY - inHeight),
            size = Size(barWidth, inHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
        )

        // Bar 2: Outflow (Rose/Red)
        val outX = (w * 0.72f) - (barWidth / 2f)
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFF87171), Color(0xFFDC2626))),
            topLeft = Offset(outX, baseY - outHeight),
            size = Size(barWidth, outHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
        )
    }
}

@Composable
private fun PortfolioDonutCanvas(
    accounts: List<AccountEntity>,
    totalBalance: Double,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = (minOf(size.width, size.height) / 2f) - 20f
        val strokeWidth = 32f

        if (totalBalance <= 0 || accounts.isEmpty()) {
            drawCircle(
                color = Color(0xFF334155),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )
            return@Canvas
        }

        var startAngle = -90f
        accounts.forEach { acc ->
            val sweep = ((acc.balance / totalBalance) * 360.0).toFloat().coerceAtLeast(0f)
            val color = try {
                Color(android.graphics.Color.parseColor(acc.customColorHex.ifBlank { "#0284C7" }))
            } catch (_: Exception) {
                Color(0xFF0284C7)
            }

            if (sweep > 0.5f) {
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep - 1.5f, // subtle gap
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            startAngle += sweep
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
