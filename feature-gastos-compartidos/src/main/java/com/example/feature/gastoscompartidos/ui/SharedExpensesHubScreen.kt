package com.example.feature.gastoscompartidos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SharedExpenseWithFullDetails
import com.example.data.local.entity.SharedGroupEntity
import com.example.data.local.entity.SharedParticipantEntity
import com.example.domain.models.shared.RealMoneyImpactMode
import com.example.domain.models.shared.SplitCalculationMethod
import com.example.feature.gastoscompartidos.domain.engine.DebtSimplificationGraphEngine
import com.example.feature.gastoscompartidos.domain.ticket.SharedReceiptTicketEngine
import com.example.feature.gastoscompartidos.presentation.ParticipantBalanceSummaryUi
import com.example.feature.gastoscompartidos.presentation.SharedExpensesIntent
import com.example.feature.gastoscompartidos.presentation.SharedExpensesUiState
import com.example.feature.gastoscompartidos.ui.theme.KashBlueTopBar
import com.example.feature.gastoscompartidos.ui.theme.KashStandardTextField
import com.example.feature.gastoscompartidos.ui.theme.SharedExpenseThemeTokens
import com.example.feature.gastoscompartidos.ui.theme.SharedExpenseThemeWrapper
import com.example.feature.gastoscompartidos.ui.theme.SharedResponsiveLayoutInfo
import com.example.feature.gastoscompartidos.ui.theme.rememberSharedResponsiveLayoutInfo
import kotlin.math.abs

@Composable
fun SharedExpensesHubScreen(
    uiState: SharedExpensesUiState,
    onIntent: (SharedExpensesIntent) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenCreateSharedExpense: () -> Unit,
    onOpenOcrScanner: () -> Unit,
    onOpenSettleModal: (participantId: String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val responsive = rememberSharedResponsiveLayoutInfo()
    val tabs = listOf(
        "Resumen & Saldos",
        "Grupos & Grafos",
        "Tickets & Consecutivos",
        "Directorio & Cobro"
    )

    SharedExpenseThemeWrapper {
        Scaffold(
            containerColor = SharedExpenseThemeTokens.KashLightBg,
            topBar = {
                KashBlueTopBar(
                    title = "Gastos Compartidos",
                    subtitle = "Contabilidad NIIF • Consecutivos GC / TK / PG / LQ",
                    onNavigateBack = onNavigateBack,
                    actions = {
                        IconButton(
                            onClick = onOpenOcrScanner,
                            modifier = Modifier.testTag("hub_open_ocr_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = "Escanear Factura OCR",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("hub_open_settings_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configuración Gastos Compartidos",
                                tint = Color.White
                            )
                        }
                    }
                )
            },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenCreateSharedExpense,
                containerColor = SharedExpenseThemeTokens.KashBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("fab_create_shared_expense")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (responsive.isUltraCompactWidth) {
                            "Dividir (${uiState.nextAutoExpenseConsecutive})"
                        } else {
                            "Dividir Gasto (${uiState.nextAutoExpenseConsecutive})"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = responsive.maxContentWidth)
            ) {
                // 1. Hero Card Patrimonial
                SharedNetPositionHeroCard(
                    netPosition = uiState.netSharedPosition,
                    totalReceivable = uiState.totalReceivableNiif,
                    totalPayable = uiState.totalPayableNiif,
                    totalMentalHold = uiState.totalReservedInMentalHold,
                    currency = uiState.currencyCode,
                    responsive = responsive
                )

                // 2. Pestañas de Navegación del Módulo
                ScrollableTabRow(
                    selectedTabIndex = uiState.selectedHubTabIndex,
                    containerColor = Color.White,
                    contentColor = SharedExpenseThemeTokens.KashBlue,
                    edgePadding = responsive.horizontalPadding
                ) {
                    tabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = uiState.selectedHubTabIndex == idx,
                            onClick = { onIntent(SharedExpensesIntent.SelectHubTab(idx)) },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (uiState.selectedHubTabIndex == idx) FontWeight.ExtraBold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }

                // 3. Contenido Dinámico según Pestaña (4 Pestañas Responsivas)
                when (uiState.selectedHubTabIndex) {
                    0 -> ParticipantBalancesTabContent(
                        balances = uiState.participantBalances,
                        currency = uiState.currencyCode,
                        responsive = responsive,
                        onSettleClick = onOpenSettleModal
                    )
                    1 -> GroupsAndGraphsTabContent(
                        groups = uiState.groups,
                        simplificationReport = uiState.graphSimplificationReport,
                        currency = uiState.currencyCode,
                        responsive = responsive,
                        onCreateGroup = { name, code, type ->
                            onIntent(SharedExpensesIntent.CreateSharedGroup(name, code, type))
                        }
                    )
                    2 -> TicketsAndConsecutivesTabContent(
                        expenses = uiState.expensesWithDetails,
                        selectedSeries = uiState.selectedSeriesFilter,
                        responsive = responsive,
                        onSelectSeries = { onIntent(SharedExpensesIntent.SelectSeriesFilter(it)) },
                        onViewTicket = { exp ->
                            val ticketModel = SharedReceiptTicketEngine.buildExpenseBreakdownTicket(
                                expenseConsecutive = exp.expense.consecutiveCode,
                                ticketConsecutive = exp.expense.receiptTicketCode,
                                externalInvoiceNumber = exp.expense.externalInvoiceNumber,
                                groupName = null,
                                groupCode = null,
                                title = exp.expense.title,
                                merchantName = exp.expense.merchantNameSnapshot,
                                merchantNit = exp.expense.merchantNit,
                                categoryName = exp.expense.categoryNameSnapshot,
                                dateFormatted = exp.expense.expenseDate,
                                timeFormatted = exp.expense.expenseTime,
                                currency = exp.expense.currency,
                                appliedExchangeRate = exp.expense.appliedExchangeRate,
                                splitMethod = SplitCalculationMethod.EQUAL,
                                realMoneyMode = RealMoneyImpactMode.ACCRUAL_RECEIVABLE,
                                subtotalAmount = exp.expense.subtotalAmount,
                                taxIvaAmount = exp.expense.taxIvaAmount,
                                taxConsumptionAmount = exp.expense.taxConsumptionAmount,
                                tipAmount = exp.expense.tipAmount,
                                discountAmount = exp.expense.discountAmount,
                                gmfAndFinancialAmount = exp.expense.gmfAmount,
                                totalAmount = exp.expense.totalAmount,
                                itemizedRows = emptyList(),
                                participantRows = emptyList(),
                                collectorName = "Titular KashApp",
                                collectorBankName = uiState.settingsConfig.defaultCollectionBankName,
                                collectorAccountOrKey = uiState.settingsConfig.defaultCollectionAccountOrKey,
                                collectorCustomQrUri = null,
                                sourceAccountName = null,
                                sourceAccountBalanceAfter = null,
                                recognizedPnlExpense = exp.expense.myConsumptionShareAmount,
                                receivableOrPayableDelta = exp.expense.myNetPositionDelta,
                                maskPrivateBalancesInExport = uiState.settingsConfig.maskUserAccountBalancesOnTicket
                            )
                            onIntent(SharedExpensesIntent.OpenDigitalTicketPreview(ticketModel))
                        }
                    )
                    3 -> DirectoryAndCollectionTabContent(
                        participants = uiState.participants,
                        currency = uiState.currencyCode,
                        responsive = responsive,
                        onAddOrEditParticipant = { participant ->
                            onIntent(SharedExpensesIntent.OpenParticipantEditor(participant))
                        }
                    )
                }
            }
        }
    }
}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SharedNetPositionHeroCard(
    netPosition: Double,
    totalReceivable: Double,
    totalPayable: Double,
    totalMentalHold: Double,
    currency: String,
    responsive: SharedResponsiveLayoutInfo = rememberSharedResponsiveLayoutInfo()
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SharedExpenseThemeTokens.KashNavy,
                        SharedExpenseThemeTokens.KashDarkSlate
                    )
                )
            )
            .padding(horizontal = responsive.horizontalPadding, vertical = 16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "POSICIÓN NETA EN GASTOS COMPARTIDOS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SharedExpenseThemeTokens.KashTextMuted,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = SharedReceiptTicketEngine.formatMoney(netPosition, currency),
                        fontSize = if (responsive.isUltraCompactWidth) 22.sp else 26.sp,
                        fontWeight = FontWeight.Black,
                        color = if (netPosition >= 0.0) Color(0xFF34D399) else Color(0xFFFB7185),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (totalMentalHold > 0.0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SharedExpenseThemeTokens.MentalHoldViolet.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, SharedExpenseThemeTokens.MentalHoldVioletBorder.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFC4B5FD),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Protegido en holdBalance",
                                    fontSize = 9.sp,
                                    color = Color(0xFFDDD6FE)
                                )
                                Text(
                                    text = SharedReceiptTicketEngine.formatMoney(totalMentalHold, currency),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            if (responsive.isUltraCompactWidth) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReceivablePayableMetricBox(
                        title = "Por Cobrar (Activo CxC NIIF)",
                        valueText = "+ ${SharedReceiptTicketEngine.formatMoney(totalReceivable, currency)}",
                        titleColor = Color(0xFF99F6E4),
                        borderColor = Color(0xFF2DD4BF).copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    )
                    ReceivablePayableMetricBox(
                        title = "Por Pagar (Pasivo CxP NIIF)",
                        valueText = "- ${SharedReceiptTicketEngine.formatMoney(totalPayable, currency)}",
                        titleColor = Color(0xFFFECDD3),
                        borderColor = Color(0xFFFB7185).copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReceivablePayableMetricBox(
                        title = "Por Cobrar (Activo CxC NIIF)",
                        valueText = "+ ${SharedReceiptTicketEngine.formatMoney(totalReceivable, currency)}",
                        titleColor = Color(0xFF99F6E4),
                        borderColor = Color(0xFF2DD4BF).copy(alpha = 0.35f),
                        modifier = Modifier.weight(1f)
                    )
                    ReceivablePayableMetricBox(
                        title = "Por Pagar (Pasivo CxP NIIF)",
                        valueText = "- ${SharedReceiptTicketEngine.formatMoney(totalPayable, currency)}",
                        titleColor = Color(0xFFFECDD3),
                        borderColor = Color(0xFFFB7185).copy(alpha = 0.35f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceivablePayableMetricBox(
    title: String,
    valueText: String,
    titleColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = titleColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = valueText,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ParticipantBalancesTabContent(
    balances: List<ParticipantBalanceSummaryUi>,
    currency: String,
    responsive: SharedResponsiveLayoutInfo,
    onSettleClick: (String) -> Unit
) {
    if (responsive.gridColumnsCount > 1) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(responsive.gridColumnsCount),
            modifier = Modifier
                .fillMaxSize()
                .padding(responsive.horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(balances, key = { it.participant.id }) { item ->
                ParticipantBalanceCard(item = item, currency = currency, onSettleClick = onSettleClick)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(responsive.horizontalPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(balances, key = { it.participant.id }) { item ->
                ParticipantBalanceCard(item = item, currency = currency, onSettleClick = onSettleClick)
            }
        }
    }
}

@Composable
private fun ParticipantBalanceCard(
    item: ParticipantBalanceSummaryUi,
    currency: String,
    onSettleClick: (String) -> Unit
) {
    val owesMe = item.netBalanceWithUser >= 0.0
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SharedExpenseThemeTokens.KashBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SharedExpenseThemeTokens.KashBlue.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.participant.avatarInitials,
                            fontWeight = FontWeight.ExtraBold,
                            color = SharedExpenseThemeTokens.KashBlue
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.participant.displayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SharedExpenseThemeTokens.KashTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.pendingTicketsCount} tickets abiertos • ${item.participant.preferredPaymentMethod ?: "Transferencia"}",
                            fontSize = 11.sp,
                            color = SharedExpenseThemeTokens.KashTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (owesMe) "Te debe" else "Le debes",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (owesMe) SharedExpenseThemeTokens.ReceivableTeal else SharedExpenseThemeTokens.PayableRose
                    )
                    Text(
                        text = SharedReceiptTicketEngine.formatMoney(abs(item.netBalanceWithUser), currency),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = if (owesMe) SharedExpenseThemeTokens.ReceivableTeal else SharedExpenseThemeTokens.PayableRose
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SharedExpenseThemeTokens.KashBorder)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { onSettleClick(item.participant.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (owesMe) SharedExpenseThemeTokens.ReceivableTeal else SharedExpenseThemeTokens.KashNavy
                    )
                ) {
                    Text(
                        text = if (owesMe) "Registrar Abono (PG)" else "Liquidar Deuda (PG)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupsAndGraphsTabContent(
    groups: List<SharedGroupEntity>,
    simplificationReport: DebtSimplificationGraphEngine.GraphSimplificationReport?,
    currency: String,
    responsive: SharedResponsiveLayoutInfo,
    onCreateGroup: (name: String, code: String, type: String) -> Unit
) {
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(responsive.horizontalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GRUPOS DE GASTOS COMPARTIDOS (${groups.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SharedExpenseThemeTokens.KashBlue,
                letterSpacing = 0.5.sp
            )
            OutlinedButton(
                onClick = { showCreateGroupDialog = true },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = SharedExpenseThemeTokens.KashBlue)
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ Grupo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SharedExpenseThemeTokens.KashBlue)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (simplificationReport != null && simplificationReport.optimizedInstructions.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SharedExpenseThemeTokens.ReceivableTealBg,
                        border = BorderStroke(1.dp, SharedExpenseThemeTokens.ReceivableTealBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountTree,
                                    contentDescription = null,
                                    tint = SharedExpenseThemeTokens.ReceivableTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SIMPLIFICACIÓN DE DEUDAS MIN-CASH-FLOW",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SharedExpenseThemeTokens.ReceivableTeal
                                )
                            }
                            Text(
                                text = "De ${simplificationReport.originalTransferCount} transferencias cruzadas a solo ${simplificationReport.simplifiedTransferCount} pagos directos (${simplificationReport.transactionsSavedCount} ahorrados):",
                                fontSize = 12.sp,
                                color = SharedExpenseThemeTokens.KashTextPrimary
                            )
                            simplificationReport.optimizedInstructions.forEach { instr ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${instr.fromParticipantName} ➔ ${instr.toParticipantName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SharedExpenseThemeTokens.KashNavy,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = SharedReceiptTicketEngine.formatMoney(instr.amount, currency),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SharedExpenseThemeTokens.ReceivableTeal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(groups, key = { it.id }) { group ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, SharedExpenseThemeTokens.KashBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${group.name} (${group.groupCode})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SharedExpenseThemeTokens.KashTextPrimary
                            )
                            Text(
                                text = "Tipo: ${group.groupType} • Simplificación de Grafos: ${if (group.simplifyDebtsEnabled) "Activa" else "Inactiva"}",
                                fontSize = 11.sp,
                                color = SharedExpenseThemeTokens.KashTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        var groupName by remember { mutableStateOf("") }
        var groupCode by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf("Hogar") }
        val types = listOf("Hogar", "Viaje", "Pareja", "Oficina", "Proyecto", "Evento")

        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = {
                Text(
                    text = "Nuevo Grupo Compartido",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SharedExpenseThemeTokens.KashNavy
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    KashStandardTextField(
                        value = groupName,
                        onValueChange = {
                            groupName = it
                            if (groupCode.isBlank()) {
                                groupCode = it.take(6).uppercase()
                            }
                        },
                        label = "Nombre del Grupo",
                        placeholder = "Ej: Apartamento 402, Viaje Cancún..."
                    )
                    KashStandardTextField(
                        value = groupCode,
                        onValueChange = { groupCode = it.uppercase() },
                        label = "Código Corto del Grupo (Serie)",
                        placeholder = "Ej: HOGAR o CANCUN"
                    )
                    Text(
                        text = "Tipo de Grupo:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SharedExpenseThemeTokens.KashTextSecondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(types) { t ->
                            val isSel = t == selectedType
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) SharedExpenseThemeTokens.KashBlue else SharedExpenseThemeTokens.KashSurfaceMuted,
                                border = BorderStroke(1.dp, if (isSel) SharedExpenseThemeTokens.KashBlue else SharedExpenseThemeTokens.KashBorder),
                                modifier = Modifier.clickable { selectedType = t }
                            ) {
                                Text(
                                    text = t,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else SharedExpenseThemeTokens.KashTextPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupName.isNotBlank() && groupCode.isNotBlank()) {
                            onCreateGroup(groupName.trim(), groupCode.trim(), selectedType)
                            showCreateGroupDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SharedExpenseThemeTokens.KashBlue),
                    enabled = groupName.isNotBlank() && groupCode.isNotBlank()
                ) {
                    Text("Crear Grupo", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) {
                    Text("Cancelar", color = SharedExpenseThemeTokens.KashNavy)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun TicketsAndConsecutivesTabContent(
    expenses: List<SharedExpenseWithFullDetails>,
    selectedSeries: String,
    responsive: SharedResponsiveLayoutInfo,
    onSelectSeries: (String) -> Unit,
    onViewTicket: (SharedExpenseWithFullDetails) -> Unit
) {
    val seriesFilters = listOf(
        "ALL" to "Todas las Series",
        "GC" to "GC • Gastos",
        "TK" to "TK • Tickets Recibo",
        "PG" to "PG • Abonos",
        "LQ" to "LQ • Actas Grupo"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = responsive.horizontalPadding, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(seriesFilters) { (code, label) ->
                val isSelected = selectedSeries == code
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) SharedExpenseThemeTokens.KashNavy else Color.White,
                    border = BorderStroke(1.dp, if (isSelected) SharedExpenseThemeTokens.KashNavy else SharedExpenseThemeTokens.KashBorder),
                    modifier = Modifier.clickable { onSelectSeries(code) }
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else SharedExpenseThemeTokens.KashTextSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = responsive.horizontalPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(expenses, key = { it.expense.id }) { item ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, SharedExpenseThemeTokens.KashBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewTicket(item) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SharedExpenseThemeTokens.KashBlue.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = item.expense.consecutiveCode,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SharedExpenseThemeTokens.KashBlue,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SharedExpenseThemeTokens.ReceivableTealBg
                                ) {
                                    Text(
                                        text = item.expense.receiptTicketCode,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SharedExpenseThemeTokens.ReceivableTeal,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.expense.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SharedExpenseThemeTokens.KashTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.expense.expenseDate} • Sello SHA-256: #${item.expense.ticketIntegrityHash.take(8)}",
                                fontSize = 11.sp,
                                color = SharedExpenseThemeTokens.KashTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = SharedReceiptTicketEngine.formatMoney(item.expense.totalAmount, item.expense.currency),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = SharedExpenseThemeTokens.KashTextPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "Ver Ticket",
                                tint = SharedExpenseThemeTokens.KashBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectoryAndCollectionTabContent(
    participants: List<SharedParticipantEntity>,
    currency: String,
    responsive: SharedResponsiveLayoutInfo,
    onAddOrEditParticipant: (SharedParticipantEntity?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(responsive.horizontalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DIRECTORIO DE TERCEROS CONTABLES (${participants.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SharedExpenseThemeTokens.KashTextSecondary
            )
            OutlinedButton(
                onClick = { onAddOrEditParticipant(null) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("directory_add_participant_btn")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("+ Nuevo Tercero", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(participants, key = { it.id }) { p ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, SharedExpenseThemeTokens.KashBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddOrEditParticipant(p) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SharedExpenseThemeTokens.KashBlue.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = p.avatarInitials,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SharedExpenseThemeTokens.KashBlue
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = p.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SharedExpenseThemeTokens.KashTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${p.preferredPaymentMethod ?: "BRE_B"} • ${p.accountNumberOrKey ?: "Sin llave registrada"}",
                                    fontSize = 11.sp,
                                    color = SharedExpenseThemeTokens.KashTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        p.creditTrustLimit?.let { limit ->
                            Text(
                                text = "Tope: ${SharedReceiptTicketEngine.formatMoney(limit, currency)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SharedExpenseThemeTokens.ReceivableTeal
                            )
                        }
                    }
                }
            }
        }
    }
}
