package com.example.presentation.dashboard.detail

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountCreditCardEntity
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.FixedAssetEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.repository.FinancialRepository
import com.example.presentation.dashboard.detail.components.AccountMovementsBottomSheet
import com.example.presentation.dashboard.detail.components.AssetsRubroContent
import com.example.presentation.dashboard.detail.components.CashRubroContent
import com.example.presentation.dashboard.detail.components.CreditCardsRubroContent
import com.example.presentation.dashboard.detail.components.InvestmentsRubroContent
import com.example.presentation.dashboard.detail.components.RubroDetailHeader
import com.example.presentation.dashboard.detail.components.RubroExportBottomSheet
import com.example.presentation.dashboard.detail.components.RubroExportSuccessDialog
import com.example.presentation.dashboard.detail.components.RubroFilterBottomSheet
import com.example.presentation.dashboard.model.NetWorthState
import com.example.util.MoneyFormatter
import com.example.domain.models.FinancialRubroType
import com.example.domain.models.CardSubPurchaseItem
import com.example.domain.models.AccountMovementDisplay
import com.example.domain.models.RubroItemDisplay
import com.example.util.FinancialReportExporter
import java.math.BigDecimal

/**
 * Enterprise Coordinator Screen for Financial Rubros (~150 lines).
 * Coordinates data observation and switches dynamically between Cash, Investments, Debts, and Assets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialRubroDetailScreen(
    rubroType: FinancialRubroType,
    netWorthState: NetWorthState,
    financialRepository: FinancialRepository?,
    fixedAssetRepository: com.example.domain.repository.FixedAssetRepository? = null,
    userId: String = "user_default",
    isPrivacyMode: Boolean = false,
    onOpenFixedAssetsModule: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler { onDismiss() }

    // Room DB Observations
    val accountsState = financialRepository?.getAccounts(userId)?.collectAsState(initial = emptyList())
    val roomAccounts = accountsState?.value ?: emptyList()

    val cardConfigsState = financialRepository?.getAccountCreditCards(userId)?.collectAsState(initial = emptyList())
    val cardConfigs = cardConfigsState?.value ?: emptyList()
    val cardConfigsByAccountId = remember(cardConfigs) { cardConfigs.associateBy { it.accountId } }

    val transactionsState = financialRepository?.getTransactions(userId)?.collectAsState(initial = emptyList())
    val roomTransactions = transactionsState?.value ?: emptyList()

    val fixedAssetsState = fixedAssetRepository?.observeAssets(userId)?.collectAsState(initial = emptyList())
    val roomFixedAssets = fixedAssetsState?.value ?: emptyList()

    // 1. Build raw items through dedicated data mapper
    val rawItems = remember(rubroType, roomAccounts, cardConfigsByAccountId, netWorthState, roomTransactions, roomFixedAssets) {
        buildRawRubroItems(rubroType, roomAccounts, cardConfigsByAccountId, roomTransactions, netWorthState, roomFixedAssets)
    }

    // 2. Interactive Filter States
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedPeriodFilter by remember { mutableStateOf("Este Mes") }
    val periodOptions = listOf("Este Mes", "Último Trimestre", "Año Actual", "Todo")

    var selectedCategoryFilter by remember { mutableStateOf("Todas") }
    val categoryOptions = remember(rawItems) { listOf("Todas") + rawItems.map { it.category }.distinct() }

    var selectedEntityFilter by remember { mutableStateOf("Todas") }
    val entityOptions = remember(rawItems) { listOf("Todas") + rawItems.map { it.institution }.distinct() }

    var selectedCurrencyFilter by remember { mutableStateOf("Todas") }
    val currencyOptions = listOf("Todas", "COP", "USD", "EUR")

    var selectedAmountRangeFilter by remember { mutableStateOf("Todos") }
    val amountRangeOptions = listOf("Todos", "> $ 500.000", "> $ 2.000.000", "Con Tasa / Rendimiento")
    var searchQuery by remember { mutableStateOf("") }

    val activeFilterCount = (if (selectedPeriodFilter != "Este Mes") 1 else 0) +
            (if (selectedCategoryFilter != "Todas") 1 else 0) +
            (if (selectedEntityFilter != "Todas") 1 else 0) +
            (if (selectedCurrencyFilter != "Todas") 1 else 0) +
            (if (selectedAmountRangeFilter != "Todos") 1 else 0)

    // 3. Modals and Export States
    var showExportModal by remember { mutableStateOf(false) }
    var exportSuccessToast by remember { mutableStateOf<String?>(null) }
    var selectedAccountForDetail by remember { mutableStateOf<RubroItemDisplay?>(null) }
    var latestExportResult by remember { mutableStateOf<FinancialReportExporter.ExportResult?>(null) }
    var latestExportMimeType by remember { mutableStateOf("application/pdf") }
    var latestExportTitle by remember { mutableStateOf("Reporte Financiero") }
    var showExportSuccessDialog by remember { mutableStateOf(false) }

    // 4. Reactive Calculations
    val filteredItems = remember(rawItems, selectedPeriodFilter, selectedCategoryFilter, selectedEntityFilter, selectedCurrencyFilter, selectedAmountRangeFilter, searchQuery) {
        rawItems.filter { item ->
            val matchCategory = selectedCategoryFilter == "Todas" || item.category.equals(selectedCategoryFilter, ignoreCase = true)
            val matchEntity = selectedEntityFilter == "Todas" || item.institution.equals(selectedEntityFilter, ignoreCase = true)
            val matchCurrency = selectedCurrencyFilter == "Todas" || item.currency.equals(selectedCurrencyFilter, ignoreCase = true)
            val matchQuery = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.institution.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true)
            val matchRange = when (selectedAmountRangeFilter) {
                "> $ 500.000" -> item.amount >= BigDecimal(500000)
                "> $ 2.000.000" -> item.amount >= BigDecimal(2000000)
                "Con Tasa / Rendimiento" -> (item.rateEa ?: 0.0) > 0
                else -> true
            }
            matchCategory && matchEntity && matchCurrency && matchQuery && matchRange
        }
    }

    val filteredTotal = remember(filteredItems) {
        filteredItems.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.amount) }
    }
    val netWorthVal = netWorthState.totalNetWorth.toDouble().coerceAtLeast(1.0)
    val rubroPercentage = ((filteredTotal.toDouble() / netWorthVal) * 100).coerceIn(0.0, 100.0)

    val categoryBreakdown = remember(filteredItems, filteredTotal) {
        val totalDouble = filteredTotal.toDouble()
        if (totalDouble <= 0.0) emptyList()
        else {
            filteredItems.groupBy { it.category }.map { (cat, items) ->
                val sum = items.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.amount) }
                val percent = (sum.toDouble() / totalDouble) * 100.0
                Triple(cat, sum, percent)
            }.sortedByDescending { it.third }
        }
    }

    fun onExportExcel() {
        val filterDesc = "Periodo: $selectedPeriodFilter, Cat: $selectedCategoryFilter, Entidad: $selectedEntityFilter"
        val totalFormatted = MoneyFormatter.format(filteredTotal, false)
        val res = FinancialReportExporter.exportToExcelCsv(context, rubroType, totalFormatted, filteredItems, filterDesc)
        latestExportResult = res
        latestExportMimeType = "text/csv"
        latestExportTitle = "Planilla Excel - ${rubroType.title}"
        showExportSuccessDialog = true
        exportSuccessToast = res.message
        showExportModal = false
    }

    fun onExportPdf() {
        val filterDesc = "Periodo: $selectedPeriodFilter, Cat: $selectedCategoryFilter, Entidad: $selectedEntityFilter"
        val totalFormatted = MoneyFormatter.format(filteredTotal, false)
        val res = FinancialReportExporter.exportToPdf(context, rubroType, totalFormatted, rubroPercentage, filteredItems, filterDesc)
        latestExportResult = res
        latestExportMimeType = "application/pdf"
        latestExportTitle = "Informe PDF - ${rubroType.title}"
        showExportSuccessDialog = true
        exportSuccessToast = res.message
        showExportModal = false
    }

    // 5. MAIN COORDINATOR UI
    Surface(
        color = Color(0xFFF8FAFC),
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("financial_rubro_detail_screen_${rubroType.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header Hero with Amount, Sparkline, and Filters
            RubroDetailHeader(
                rubroType = rubroType,
                filteredTotal = filteredTotal,
                rubroPercentage = rubroPercentage,
                activeFilterCount = activeFilterCount,
                isPrivacyMode = isPrivacyMode,
                onDismiss = onDismiss,
                onOpenFilters = { showFilterSheet = true },
                onOpenExport = { showExportModal = true }
            )

            // Optional Notification Toast
            if (exportSuccessToast != null) {
                Surface(color = Color(0xFF10B981), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = exportSuccessToast ?: "", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { exportSuccessToast = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Body Container with Search Bar and Rubro Switch
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text("Buscar cuenta, entidad o categoría...", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                }
                                inner()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // DYNAMIC VIEW SWITCH ACCORDING TO RUBRO TYPE
                when (rubroType) {
                    FinancialRubroType.AVAILABLE_CASH -> CashRubroContent(
                        items = filteredItems,
                        categoryBreakdown = categoryBreakdown,
                        isPrivacyMode = isPrivacyMode,
                        onSelectAccountForDetail = { selectedAccountForDetail = it }
                    )
                    FinancialRubroType.INVESTMENTS -> InvestmentsRubroContent(
                        items = filteredItems,
                        categoryBreakdown = categoryBreakdown,
                        isPrivacyMode = isPrivacyMode,
                        onSelectAccountForDetail = { selectedAccountForDetail = it }
                    )
                    FinancialRubroType.DEBTS -> CreditCardsRubroContent(
                        items = filteredItems,
                        categoryBreakdown = categoryBreakdown,
                        isPrivacyMode = isPrivacyMode,
                        onSelectAccountForDetail = { selectedAccountForDetail = it }
                    )
                    FinancialRubroType.ASSETS -> AssetsRubroContent(
                        items = filteredItems,
                        categoryBreakdown = categoryBreakdown,
                        isPrivacyMode = isPrivacyMode,
                        onSelectAccountForDetail = { selectedAccountForDetail = it },
                        onOpenFixedAssetsModule = onOpenFixedAssetsModule
                    )
                }
            }
        }
    }

    // Modal: Multi-Criteria Filter Sheet
    if (showFilterSheet) {
        RubroFilterBottomSheet(
            rubroType = rubroType,
            selectedPeriod = selectedPeriodFilter,
            onPeriodChange = { selectedPeriodFilter = it },
            periodOptions = periodOptions,
            selectedCategory = selectedCategoryFilter,
            onCategoryChange = { selectedCategoryFilter = it },
            categoryOptions = categoryOptions,
            selectedEntity = selectedEntityFilter,
            onEntityChange = { selectedEntityFilter = it },
            entityOptions = entityOptions,
            selectedCurrency = selectedCurrencyFilter,
            onCurrencyChange = { selectedCurrencyFilter = it },
            currencyOptions = currencyOptions,
            selectedAmountRange = selectedAmountRangeFilter,
            onAmountRangeChange = { selectedAmountRangeFilter = it },
            amountRangeOptions = amountRangeOptions,
            resultsCount = filteredItems.size,
            onResetFilters = {
                selectedPeriodFilter = "Este Mes"
                selectedCategoryFilter = "Todas"
                selectedEntityFilter = "Todas"
                selectedCurrencyFilter = "Todas"
                selectedAmountRangeFilter = "Todos"
                searchQuery = ""
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    // Modal: Export Options
    if (showExportModal) {
        RubroExportBottomSheet(
            rubroType = rubroType,
            itemsCount = filteredItems.size,
            onExportExcel = { onExportExcel() },
            onExportPdf = { onExportPdf() },
            onDismiss = { showExportModal = false }
        )
    }

    // Modal: Export Success Dialog
    if (showExportSuccessDialog && latestExportResult != null) {
        RubroExportSuccessDialog(
            exportResult = latestExportResult!!,
            mimeType = latestExportMimeType,
            title = latestExportTitle,
            context = context,
            onDismiss = { showExportSuccessDialog = false }
        )
    }

    // Modal: Account Movements with % of Total
    if (selectedAccountForDetail != null) {
        AccountMovementsBottomSheet(
            targetAccount = selectedAccountForDetail!!,
            roomTransactions = roomTransactions,
            context = context,
            onExportSuccess = { res, mime, title ->
                latestExportResult = res
                latestExportMimeType = mime
                latestExportTitle = title
                showExportSuccessDialog = true
                selectedAccountForDetail = null
            },
            onDismiss = { selectedAccountForDetail = null }
        )
    }
}

/**
 * Maps raw Room entities into domain RubroItemDisplay instances for the selected Rubro.
 */
private fun buildRawRubroItems(
    rubroType: FinancialRubroType,
    roomAccounts: List<AccountEntity>,
    cardConfigsByAccountId: Map<String, AccountCreditCardEntity>,
    roomTransactions: List<TransactionEntity>,
    netWorthState: NetWorthState,
    roomFixedAssets: List<FixedAssetEntity> = emptyList()
): List<RubroItemDisplay> {
    return when (rubroType) {
        FinancialRubroType.AVAILABLE_CASH -> {
            val cashAccounts = roomAccounts.filter {
                !it.type.equals("CREDIT", ignoreCase = true) &&
                !it.type.equals("CREDIT_CARD", ignoreCase = true) &&
                !it.type.equals("INVESTMENT", ignoreCase = true) &&
                !it.type.equals("CDT", ignoreCase = true)
            }
            if (cashAccounts.isNotEmpty()) {
                cashAccounts.map { acc ->
                    val accBalDouble = acc.balance
                    val accTxs = roomTransactions.filter { it.accountId == acc.id }
                    val movs = if (accTxs.isNotEmpty()) {
                        accTxs.map { tx ->
                            val pct = if (accBalDouble > 0.0) (kotlin.math.abs(tx.amount) / accBalDouble) * 100.0 else 0.0
                            val txTitle = tx.description?.takeIf { it.isNotBlank() } ?: tx.categoryName ?: "Transacción"
                            AccountMovementDisplay(
                                id = tx.id,
                                title = txTitle,
                                category = tx.categoryName ?: "General",
                                dateFormatted = if (tx.date.length >= 10) tx.date.take(10) else tx.date,
                                amount = kotlin.math.abs(tx.amount),
                                percentageOfTotal = pct,
                                details = if (tx.type == "INCOME") "Depósito / Abono" else "Gasto / Débito"
                            )
                        }
                    } else {
                        emptyList()
                    }

                    val rate = acc.interestRateEa
                    RubroItemDisplay(
                        id = acc.id,
                        name = acc.name,
                        institution = acc.bankName,
                        category = when (acc.type.uppercase()) {
                            "SAVINGS" -> "Ahorros"
                            "CHECKING" -> "Corriente"
                            "PAYROLL" -> "Nómina"
                            "WALLET" -> "Billetera Digital"
                            else -> "Efectivo / Banco"
                        },
                        amount = BigDecimal.valueOf(acc.balance),
                        currency = acc.currency,
                        rateEa = if (rate != null && rate > 0) rate else null,
                        status = if (acc.balance >= 0) "Positivo" else "Sobregiro",
                        colorHex = acc.customColorHex.ifBlank { "#0284C7" },
                        accountMovements = movs
                    )
                }
            } else {
                val cash1 = netWorthState.availableCash.multiply(BigDecimal("0.65"))
                val cash2 = netWorthState.availableCash.multiply(BigDecimal("0.20"))
                val cash3 = netWorthState.availableCash.multiply(BigDecimal("0.15"))
                listOf(
                    RubroItemDisplay("1", "Cuenta Ahorros Principal", "Bancolombia", "Ahorros", cash1, "COP", 10.5, colorHex = "#0284C7"),
                    RubroItemDisplay("2", "Billetera Nequi", "Bancolombia", "Billetera Digital", cash2, "COP", null, colorHex = "#8B5CF6"),
                    RubroItemDisplay("3", "Caja Fuerte / Efectivo", "Efectivo", "Efectivo", cash3, "COP", null, colorHex = "#10B981")
                )
            }
        }
        FinancialRubroType.INVESTMENTS -> {
            val investmentAccounts = roomAccounts.filter {
                it.type.equals("INVESTMENT", ignoreCase = true) ||
                it.type.equals("CDT", ignoreCase = true) ||
                it.name.contains("CDT", ignoreCase = true) ||
                it.name.contains("Fondo", ignoreCase = true) ||
                it.name.contains("Acciones", ignoreCase = true)
            }
            if (investmentAccounts.isNotEmpty()) {
                investmentAccounts.map { acc ->
                    val rate = acc.interestRateEa
                    RubroItemDisplay(
                        id = acc.id,
                        name = acc.name,
                        institution = acc.bankName,
                        category = if (acc.name.contains("CDT", ignoreCase = true)) "CDT Renta Fija" else "Fondo de Inversión",
                        amount = BigDecimal.valueOf(acc.balance.coerceAtLeast(0.0)),
                        currency = acc.currency,
                        rateEa = if (rate != null && rate > 0) rate else 12.5,
                        status = "En Rendimiento",
                        colorHex = acc.customColorHex.ifBlank { "#8B5CF6" }
                    )
                }
            } else {
                listOf(
                    RubroItemDisplay("inv_1", "CDT Digital Bancolombia (180 días)", "Bancolombia", "CDT Renta Fija", netWorthState.investments.multiply(BigDecimal("0.70")), "COP", 12.2, status = "En Rendimiento", colorHex = "#8B5CF6"),
                    RubroItemDisplay("inv_2", "Fondo de Inversión Fiduciario", "Fiducuenta", "Fondo Colectivo", netWorthState.investments.multiply(BigDecimal("0.30")), "COP", 11.4, status = "Activo", colorHex = "#06B6D4")
                )
            }
        }
        FinancialRubroType.DEBTS -> {
            val creditAccounts = roomAccounts.filter {
                it.type.equals("CREDIT", ignoreCase = true) ||
                it.type.equals("CREDIT_CARD", ignoreCase = true) ||
                it.name.contains("Tarjeta", ignoreCase = true) ||
                it.name.contains("Visa", ignoreCase = true) ||
                it.name.contains("Mastercard", ignoreCase = true) ||
                it.name.contains("Amex", ignoreCase = true)
            }
            if (creditAccounts.isNotEmpty()) {
                creditAccounts.map { acc ->
                    val config = cardConfigsByAccountId[acc.id]
                    val rate = config?.purchasesRateEa?.takeIf { it > 0.0 }
                        ?: config?.interestRateEa?.takeIf { it > 0.0 }
                        ?: (if (acc.overdraftRateEa > 0) acc.overdraftRateEa else 24.5)
                    val used = config?.quotaUsed ?: kotlin.math.abs(acc.balance)
                    val limit = config?.quotaTotal?.takeIf { it > 0.0 }
                        ?: (if (acc.overdraftLimit > 0) acc.overdraftLimit else (if (used > 0.0) used else 0.0))
                    val available = config?.quotaAvailable ?: (limit - used).coerceAtLeast(0.0)

                    val accTxs = roomTransactions.filter {
                        it.accountId == acc.id && (it.type.equals("EXPENSE", ignoreCase = true) || it.type.equals("CREDIT_PURCHASE", ignoreCase = true))
                    }

                    val cardSubs = accTxs.map { tx ->
                        val originalAmount = kotlin.math.abs(tx.amount)
                        val installmentsMatch = Regex("(?i)(\\d+)\\s*cuotas?").find(tx.description ?: "")
                        val totalInstallments = installmentsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
                        val monthlyPayment = if (totalInstallments > 0) originalAmount / totalInstallments else originalAmount
                        val merchantMatch = Regex("(?i)en\\s+([^(]+)").find(tx.description ?: "")
                        val merchant = merchantMatch?.groupValues?.get(1)?.trim() ?: tx.description?.ifBlank { tx.categoryName } ?: tx.categoryName
                        val totalInterest = originalAmount * (rate / 100.0) * (totalInstallments / 12.0)

                        CardSubPurchaseItem(
                            id = tx.id,
                            cardId = acc.id,
                            cardName = acc.name,
                            merchant = merchant,
                            concept = tx.description ?: tx.categoryName,
                            originalAmount = originalAmount,
                            currentBalance = originalAmount,
                            totalInstallments = totalInstallments,
                            paidInstallments = 0,
                            rateEa = rate,
                            monthlyPayment = monthlyPayment,
                            totalInterestEstimated = totalInterest,
                            interestPaid = 0.0,
                            nextPaymentDueDate = "Día ${config?.paymentDueDay ?: 10} del mes",
                            category = tx.categoryName,
                            dateFormatted = tx.date
                        )
                    }

                    RubroItemDisplay(
                        id = acc.id, name = acc.name, institution = acc.bankName,
                        category = "Tarjeta de Crédito", amount = BigDecimal.valueOf(used),
                        currency = acc.currency, rateEa = rate, quotaLimit = limit,
                        availableQuota = available, status = "Al día",
                        colorHex = acc.customColorHex.ifBlank { "#EF4444" },
                        subPurchases = cardSubs
                    )
                }
            } else {
                emptyList()
            }
        }
        FinancialRubroType.ASSETS -> {
            val activeAssets = roomFixedAssets.filter { asset ->
                val state = com.example.domain.model.fixedassets.statemachine.AssetStateMachine.resolveState(asset.status)
                asset.deletedAt == null && !state.isTerminal &&
                !asset.status.equals("WRITTEN_OFF", ignoreCase = true) &&
                !asset.status.equals("SOLD", ignoreCase = true) &&
                !asset.status.equals("DISPOSED", ignoreCase = true) &&
                !asset.status.equals("DADO_DE_BAJA", ignoreCase = true)
            }
            if (activeAssets.isNotEmpty()) {
                activeAssets.map { asset ->
                    val netVal = (asset.acquisitionValue - asset.accumulatedDepreciation).coerceAtLeast(asset.residualValue)
                    RubroItemDisplay(
                        id = asset.id,
                        name = asset.name,
                        institution = "${if (asset.type.equals("TANGIBLE", ignoreCase = true)) "Tangible" else "Intangible"} • ${asset.category}",
                        category = asset.category,
                        amount = BigDecimal.valueOf(netVal),
                        currency = asset.currency,
                        status = if (asset.includeInNetWorth) "Afecta Patrimonio" else "Excluido de Patrimonio",
                        colorHex = asset.colorHex.ifBlank { "#10B981" }
                    )
                }
            } else if (roomFixedAssets.isNotEmpty()) {
                roomFixedAssets.filter { it.deletedAt == null }.map { asset ->
                    val netVal = (asset.acquisitionValue - asset.accumulatedDepreciation).coerceAtLeast(asset.residualValue)
                    RubroItemDisplay(
                        id = asset.id,
                        name = asset.name,
                        institution = "${asset.type} • ${asset.category}",
                        category = asset.category,
                        amount = BigDecimal.valueOf(netVal),
                        currency = asset.currency,
                        status = asset.status,
                        colorHex = asset.colorHex.ifBlank { "#94A3B8" }
                    )
                }
            } else {
                emptyList()
            }
        }
    }
}
