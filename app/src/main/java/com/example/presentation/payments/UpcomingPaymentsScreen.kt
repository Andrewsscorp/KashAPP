package com.example.presentation.payments

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import com.example.core.ui.components.IosStyleSnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import com.example.core.ui.components.IosStyleSwitch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AccountEntity
import com.example.presentation.payments.notification.PaymentNotificationConfig
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingPaymentsScreen(
    viewModel: UpcomingPaymentsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    var showSortMenu by remember { mutableStateOf(false) }

    BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(state.notificationMessage, state.errorMessage) {
        val msg = state.notificationMessage ?: state.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    // Colors
    val bgDark = Color(0xFF0F172A)
    val cardBg = Color(0xFF1E293B)
    val cardBorder = Color(0xFF334155)
    val primaryBlue = Color(0xFF0284C7)
    val accentCyan = Color(0xFF38BDF8)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF94A3B8)

    Scaffold(
        topBar = {
            Surface(
                color = bgDark,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Próximos Pagos",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Vencimientos y liquidez",
                                fontSize = 11.sp,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Copiar Resumen
                        IconButton(
                            onClick = {
                                val text = viewModel.getSummaryShareText()
                                clipboardManager.setText(AnnotatedString(text))
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Resumen de pagos copiado al portapapeles")
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar Resumen",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.toggleConfigDialog(true) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(primaryBlue.copy(alpha = 0.2f))
                                .testTag("payment_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configuración Alertas",
                                tint = accentCyan,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.toggleCreatePaymentModal(true) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(primaryBlue)
                                .testTag("add_scheduled_payment_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Nuevo Pago",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            androidx.compose.material3.ExtendedFloatingActionButton(
                onClick = { viewModel.toggleCreatePaymentModal(true) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nuevo Pago",
                        tint = Color.White
                    )
                },
                text = {
                    Text(
                        text = "Nuevo Pago",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                },
                containerColor = primaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("fab_add_scheduled_payment")
            )
        },
        snackbarHost = { IosStyleSnackbarHost(snackbarHostState) },
        containerColor = bgDark,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
                // KPI Header Overview with Cash Flow Liquidity Analysis
                PaymentKpisHeader(
                    totalPending = state.totalPendingAmount,
                    totalCreditCards = state.totalCreditCardDebt,
                    totalPaid = state.totalPaidThisMonth,
                    criticalCount = state.criticalAlertsCount,
                    liquidBalance = state.totalLiquidBalance,
                    remainingBalance = state.projectedRemainingBalance,
                    coveragePercent = state.coveragePercent,
                    isPrivacyMode = state.isPrivacyMode
                )
            }

            item {
                // Filter Tabs Bar
                PaymentFilterTabsRow(
                    selectedTab = state.filterTab,
                    onTabSelected = { viewModel.setFilterTab(it) }
                )
            }

            item {
                // Search & Filter Toolbar
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Buscar pago o factura...", color = textSecondary, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = accentCyan, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = textSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = cardBg,
                                unfocusedContainerColor = cardBg,
                                focusedBorderColor = primaryBlue,
                                unfocusedBorderColor = cardBorder,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )

                        // Sort Menu Button
                        Box {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = cardBg,
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier
                                    .height(48.dp)
                                    .clickable { showSortMenu = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Sort, contentDescription = null, tint = accentCyan, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(state.sortOrder.label, color = textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                PaymentSortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.label) },
                                        onClick = {
                                            viewModel.setSortOrder(order)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Horizon Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PaymentHorizonFilter.entries.forEach { horizon ->
                            val isSelected = state.horizonFilter == horizon
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) primaryBlue else cardBg,
                                border = BorderStroke(1.dp, if (isSelected) accentCyan else cardBorder),
                                modifier = Modifier.clickable { viewModel.setHorizonFilter(horizon) }
                            ) {
                                Text(
                                    text = horizon.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else textSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (state.displayItems.isEmpty()) {
                item {
                    EmptyPaymentsCard(onAddPayment = { viewModel.toggleCreatePaymentModal(true) })
                }
            } else {
                itemsIndexed(state.displayItems, key = { idx, item -> "payment_${item.id}_$idx" }) { _, item ->
                    PaymentItemCard(
                        item = item,
                        isPrivacyMode = state.isPrivacyMode,
                        onCardClick = { viewModel.openDetailModal(item) },
                        onPayClick = { viewModel.openPaymentModal(item) },
                        onSimulateClick = { viewModel.openCcSimulator(item) },
                        onEditClick = { viewModel.openEditPaymentModal(item) },
                        onTogglePaid = { viewModel.togglePaymentPaidStatus(item) },
                        onDeleteClick = { viewModel.deleteScheduledPayment(item.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Dialog: Configuración de Alertas y Notificaciones con WorkManager
    if (state.showConfigDialog) {
        val currentContext = LocalContext.current
        NotificationConfigDialog(
            config = PaymentNotificationConfig(
                enabled = state.notificationsEnabled,
                advanceNoticeDays = state.advanceNoticeDays,
                notifyOnDueDate = state.notifyOnDueDate,
                notifyDailyIfOverdue = state.notifyDailyIfOverdue,
                notifyCreditCards = state.notifyCreditCards,
                notifyLoansAndDebts = state.notifyLoansAndDebts,
                notifyServices = state.notifyServices,
                preferredHour = state.preferredHour,
                lastRunTimestamp = state.lastNotifRunTime
            ),
            onDismiss = { viewModel.toggleConfigDialog(false) },
            onSave = { updatedConfig ->
                viewModel.saveFullNotificationConfig(currentContext, updatedConfig)
            },
            onTestNotification = {
                viewModel.triggerTestNotification(currentContext)
            },
            onRunScanNow = {
                viewModel.triggerImmediateScan(currentContext)
            }
        )
    }

    // Modal: Crear Nuevo Próximo Pago
    if (state.showCreatePaymentModal) {
        CreateScheduledPaymentModal(
            availableAccounts = state.availableAccounts,
            onDismiss = { viewModel.toggleCreatePaymentModal(false) },
            onSave = { name, cat, amt, due, rec, accId, accName, notes, remDays ->
                viewModel.createScheduledPayment(name, cat, amt, due, rec, accId, accName, notes, remDays)
            }
        )
    }

    // Modal: Editar Pago Programado
    state.itemForEditing?.let { editItem ->
        EditScheduledPaymentModal(
            item = editItem,
            onDismiss = { viewModel.closeEditPaymentModal() },
            onSave = { id, name, cat, amt, due, rec, notes, remDays ->
                viewModel.updateScheduledPayment(id, name, cat, amt, due, rec, notes, remDays)
            }
        )
    }

    // Modal: Simulador de Tarjetas de Crédito y Cuotas
    if (state.showCcSimulatorDialog && state.selectedCcForSimulation != null) {
        CreditCardSimulatorModal(
            cardItem = state.selectedCcForSimulation!!,
            availableAccounts = state.availableAccounts,
            onDismiss = { viewModel.closeCcSimulator() },
            onPayWithAccount = { item, accId, accName, amt ->
                viewModel.executeDirectPayment(item, accId, accName, amt, "Pago Tarjeta de Crédito")
            }
        )
    }

    // Dialog: Ejecutar Pago en Cuenta Directo
    state.selectedItemForPayment?.let { payItem ->
        ExecutePaymentDialog(
            item = payItem,
            availableAccounts = state.availableAccounts,
            onDismiss = { viewModel.closePaymentModal() },
            onConfirmPayment = { item, accId, accName, amt, catName ->
                viewModel.executeDirectPayment(item, accId, accName, amt, catName)
            }
        )
    }

    // Modal: Detalle Completo de Deuda / Pago
    state.selectedItemForDetail?.let { detailItem ->
        PaymentDetailModal(
            item = detailItem,
            availableAccounts = state.availableAccounts,
            onDismiss = { viewModel.closeDetailModal() },
            onPayClick = {
                viewModel.closeDetailModal()
                viewModel.openPaymentModal(detailItem)
            },
            onSimulateClick = {
                viewModel.closeDetailModal()
                viewModel.openCcSimulator(detailItem)
            },
            onEditClick = {
                viewModel.closeDetailModal()
                viewModel.openEditPaymentModal(detailItem)
            },
            onDeleteClick = {
                viewModel.closeDetailModal()
                viewModel.deleteScheduledPayment(detailItem.id)
            }
        )
    }
}

@Composable
private fun PaymentKpisHeader(
    totalPending: Double,
    totalCreditCards: Double,
    totalPaid: Double,
    criticalCount: Int,
    liquidBalance: Double,
    remainingBalance: Double,
    coveragePercent: Double,
    isPrivacyMode: Boolean
) {
    val moneyFormatter = remember { DecimalFormat("#,##0") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Main Pending Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFF0284C7).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("PRÓXIMOS A VENCER", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Text("Total pendiente de pago", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (criticalCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$criticalCount Críticos",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isPrivacyMode) "$ ••••••••" else "$ ${moneyFormatter.format(totalPending)} COP",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Deuda en Tarjetas", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(
                            text = if (isPrivacyMode) "$ ••••" else "$ ${moneyFormatter.format(totalCreditCards)}",
                            color = Color(0xFFA78BFA),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text("Total Pagado Este Mes", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(
                            text = if (isPrivacyMode) "$ ••••" else "$ ${moneyFormatter.format(totalPaid)}",
                            color = Color(0xFF34D399),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Cash Flow Liquidity Analysis Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PROYECCIÓN DE LIQUIDEZ Y CAJA",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (remainingBalance >= 0) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, if (remainingBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444))
                    ) {
                        Text(
                            text = if (remainingBalance >= 0) "Cobertura ${coveragePercent.toInt()}%" else "Riesgo de Déficit",
                            color = if (remainingBalance >= 0) Color(0xFF34D399) else Color(0xFFFCA5A5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Saldo Líquido Actual", color = Color(0xFF64748B), fontSize = 11.sp)
                        Text(
                            text = if (isPrivacyMode) "$ ••••" else "$ ${moneyFormatter.format(liquidBalance)}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Remanente Post-Pagos", color = Color(0xFF64748B), fontSize = 11.sp)
                        Text(
                            text = if (isPrivacyMode) "$ ••••" else "$ ${moneyFormatter.format(remainingBalance)}",
                            color = if (remainingBalance >= 0) Color(0xFF38BDF8) else Color(0xFFEF4444),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentFilterTabsRow(
    selectedTab: PaymentFilterTab,
    onTabSelected: (PaymentFilterTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PaymentFilterTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            Surface(
                onClick = { onTabSelected(tab) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B),
                border = if (isSelected) null else BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Text(
                    text = tab.label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PaymentItemCard(
    item: UnifiedPaymentItem,
    isPrivacyMode: Boolean,
    onCardClick: () -> Unit,
    onPayClick: () -> Unit,
    onSimulateClick: () -> Unit,
    onEditClick: () -> Unit,
    onTogglePaid: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val moneyFormatter = remember { DecimalFormat("#,##0") }

    val (icon, iconBg, iconTint) = when (item.category) {
        "TARJETA_CREDITO" -> Triple(Icons.Default.CreditCard, Color(0xFF8B5CF6).copy(alpha = 0.2f), Color(0xFFA78BFA))
        "SERVICIOS" -> Triple(Icons.Default.Receipt, Color(0xFF0284C7).copy(alpha = 0.2f), Color(0xFF38BDF8))
        "SUSCRIPCION" -> Triple(Icons.Default.PlayArrow, Color(0xFFEF4444).copy(alpha = 0.2f), Color(0xFFFCA5A5))
        "DEUDA_PRESTAMO" -> Triple(Icons.Default.Payments, Color(0xFFF59E0B).copy(alpha = 0.2f), Color(0xFFFBBF24))
        "ARRIENDO" -> Triple(Icons.Default.Home, Color(0xFF10B981).copy(alpha = 0.2f), Color(0xFF34D399))
        else -> Triple(Icons.Default.CalendarMonth, Color(0xFF0284C7).copy(alpha = 0.2f), Color(0xFF38BDF8))
    }

    val (badgeText, badgeBg, badgeTextTint) = when (item.urgencyStatus) {
        UrgencyStatus.OVERDUE -> Triple("VENCIDO", Color(0xFF7F1D1D), Color(0xFFFCA5A5))
        UrgencyStatus.CRITICAL -> Triple("CRÍTICO", Color(0xFF7C2D12), Color(0xFFFDBA74))
        UrgencyStatus.UPCOMING -> Triple("PRÓXIMO", Color(0xFF713F12), Color(0xFFFDE047))
        UrgencyStatus.ON_TIME -> Triple("AL DÍA", Color(0xFF064E3B), Color(0xFF6EE7B7))
        UrgencyStatus.PAID -> Triple("PAGADO", Color(0xFF065F46), Color(0xFF34D399))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, if (item.urgencyStatus == UrgencyStatus.OVERDUE) Color(0xFFEF4444) else Color(0xFF334155)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val dueText = if (item.daysUntilDue < 0) "Venció hace ${-item.daysUntilDue} días" else if (item.daysUntilDue == 0L) "Vence hoy" else "Vence en ${item.daysUntilDue} días (${item.dueDate})"
                        Text(
                            text = if (item.isPaid) "Pagado el ${item.paidDate ?: item.dueDate}" else dueText,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.5.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg,
                    border = BorderStroke(1.dp, badgeTextTint.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = badgeText,
                        color = badgeTextTint,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val label = if (item.isCreditCard) "Pago mínimo sugerido" else "Monto a pagar"
                    Text(label, color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Text(
                        text = if (isPrivacyMode) "$ ••••••" else "$ ${moneyFormatter.format(item.amount)} ${item.currency}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (item.secondaryAmount != null) {
                        Text(
                            text = "Deuda Total: $ ${moneyFormatter.format(item.secondaryAmount)}",
                            color = Color(0xFFA78BFA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (item.isCreditCard) {
                        Button(
                            onClick = onSimulateClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simular", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!item.isPaid) {
                        Button(
                            onClick = onPayClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Pagar", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onTogglePaid,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pagado", color = Color(0xFF34D399), fontSize = 11.5.sp)
                        }
                    }

                    if (!item.isCreditCard) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF38BDF8), modifier = Modifier.size(17.dp))
                        }

                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFEF4444), modifier = Modifier.size(17.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPaymentsCard(onAddPayment: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0xFF0284C7).copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("No tienes pagos pendientes registrados", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Agrega servicios, arriendos, cuotas o créditos para recibir alertas antes del vencimiento.", color = Color(0xFF94A3B8), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddPayment,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Agregar Próximo Pago", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NotificationConfigDialog(
    config: PaymentNotificationConfig,
    onDismiss: () -> Unit,
    onSave: (PaymentNotificationConfig) -> Unit,
    onTestNotification: () -> Unit,
    onRunScanNow: () -> Unit
) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(config.enabled) }
    var daysBefore by remember { mutableIntStateOf(config.advanceNoticeDays) }
    var onDueDate by remember { mutableStateOf(config.notifyOnDueDate) }
    var dailyIfOverdue by remember { mutableStateOf(config.notifyDailyIfOverdue) }
    var notifyCreditCards by remember { mutableStateOf(config.notifyCreditCards) }
    var notifyLoansDebts by remember { mutableStateOf(config.notifyLoansAndDebts) }
    var notifyServices by remember { mutableStateOf(config.notifyServices) }
    var preferredHour by remember { mutableIntStateOf(config.preferredHour) }
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            enabled = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Servicio de Alertas WorkManager",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Notificaciones programadas de vencimientos",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Permission Check Banner (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF7C2D12).copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, Color(0xFFEA580C)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "⚠️ Permiso de notificaciones requerido",
                                color = Color(0xFFFDBA74),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Android requiere autorización para que WorkManager pueda mostrar alertas en la barra de estado.",
                                color = Color(0xFFFED7AA),
                                fontSize = 10.5.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Conceder Permiso Ahora", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Master Toggle: WorkManager Service
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
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
                                text = "Monitoreo en Segundo Plano",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "WorkManager ejecuta revisiones periódicas automáticas",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.5.sp
                            )
                        }
                        IosStyleSwitch(
                            checked = enabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    enabled = isChecked
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF0284C7)
                            )
                        )
                    }
                }

                if (enabled) {
                    // Anticipation Days Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Anticipación de la alerta:",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1, 2, 3, 5, 7).forEach { day ->
                                val isSel = daysBefore == day
                                Surface(
                                    onClick = { daysBefore = day },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) Color(0xFF0284C7) else Color(0xFF0F172A),
                                    border = BorderStroke(1.dp, if (isSel) Color(0xFF38BDF8) else Color(0xFF334155)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "${day}d antes",
                                            color = if (isSel) Color.White else Color(0xFF94A3B8),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF334155))

                    // Categories to Alert
                    Text(
                        text = "Obligaciones a alertar:",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // 1. Tarjetas de crédito
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("💳 Tarjetas de Crédito", color = Color.White, fontSize = 12.sp)
                            Text("Fechas límite de pago y montos mínimos", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        IosStyleSwitch(
                            checked = notifyCreditCards,
                            onCheckedChange = { notifyCreditCards = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF8B5CF6))
                        )
                    }

                    // 2. Deudas & Préstamos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🏦 Préstamos & Deudas Bancarias", color = Color.White, fontSize = 12.sp)
                            Text("Cuotas fijas y amortizaciones periódicas", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        IosStyleSwitch(
                            checked = notifyLoansDebts,
                            onCheckedChange = { notifyLoansDebts = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0284C7))
                        )
                    }

                    // 3. Servicios Públicos & Suscripciones
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("💡 Servicios, Arriendos & Suscripciones", color = Color.White, fontSize = 12.sp)
                            Text("Facturas mensuales y pagos fijos", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        IosStyleSwitch(
                            checked = notifyServices,
                            onCheckedChange = { notifyServices = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF10B981))
                        )
                    }

                    HorizontalDivider(color = Color(0xFF334155))

                    // Specific Notification Moments
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🚨 Alerta el día de vencimiento", color = Color.White, fontSize = 12.sp)
                            Text("Aviso prioritario en la fecha exacta de pago", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        IosStyleSwitch(
                            checked = onDueDate,
                            onCheckedChange = { onDueDate = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0284C7))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("⚠️ Alerta diaria si está en mora", color = Color.White, fontSize = 12.sp)
                            Text("Notificar a diario hasta marcar como pagado", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        IosStyleSwitch(
                            checked = dailyIfOverdue,
                            onCheckedChange = { dailyIfOverdue = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFEF4444))
                        )
                    }

                    // Preferred Hour Selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Hora preferida de entrega:",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                8 to "08:00 AM",
                                9 to "09:00 AM",
                                12 to "12:00 PM",
                                18 to "06:00 PM"
                            ).forEach { (hr, label) ->
                                val isSel = preferredHour == hr
                                Surface(
                                    onClick = { preferredHour = hr },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) Color(0xFF38BDF8) else Color(0xFF0F172A),
                                    border = BorderStroke(1.dp, if (isSel) Color.White else Color(0xFF334155)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSel) Color(0xFF0F172A) else Color(0xFF94A3B8),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Test Action & Immediate Scan Buttons
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Prueba en vivo del servicio:",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onTestNotification,
                                    border = BorderStroke(1.dp, Color(0xFF0284C7)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("🔔 Probar Alerta", color = Color(0xFF38BDF8), fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = onRunScanNow,
                                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("⚡ Escanear Ahora", color = Color(0xFF34D399), fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // WorkManager Explanation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0284C7).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Gestionado por WorkManager: opera con ahorro de batería y persiste tras reiniciar el dispositivo.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = PaymentNotificationConfig(
                        enabled = enabled,
                        advanceNoticeDays = daysBefore,
                        notifyOnDueDate = onDueDate,
                        notifyDailyIfOverdue = dailyIfOverdue,
                        notifyCreditCards = notifyCreditCards,
                        notifyLoansAndDebts = notifyLoansDebts,
                        notifyServices = notifyServices,
                        preferredHour = preferredHour,
                        lastRunTimestamp = config.lastRunTimestamp
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Text("Guardar Configuración", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, border = BorderStroke(1.dp, Color(0xFF334155))) {
                Text("Cancelar", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        }
    )
}

@Composable
private fun CreateScheduledPaymentModal(
    availableAccounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        category: String,
        amount: Double,
        dueDate: String,
        recurrence: String,
        sourceAccountId: String?,
        sourceAccountName: String?,
        notes: String?,
        reminderDays: Int
    ) -> Unit
) {
    var entityName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("SERVICIOS") }
    var amountInput by remember { mutableStateOf("") }
    var dueDateInput by remember { mutableStateOf(LocalDate.now().plusDays(5).toString()) }
    var selectedRecurrence by remember { mutableStateOf("MONTHLY") }
    var selectedAccount by remember { mutableStateOf<AccountEntity?>(availableAccounts.firstOrNull()) }
    var notesInput by remember { mutableStateOf("") }
    var reminderDays by remember { mutableIntStateOf(3) }

    val moneyFormatter = remember { DecimalFormat("#,##0") }
    val parsedAmount = amountInput.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Text("Agregar Próximo Pago", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = entityName,
                    onValueChange = { entityName = it },
                    label = { Text("Nombre de factura / entidad", color = Color(0xFF94A3B8)) },
                    placeholder = { Text("Ej: Arriendo, Netflix, EPM, Préstamo", color = Color(0xFF64748B)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF38BDF8), unfocusedBorderColor = Color(0xFF334155), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Categoría:", color = Color(0xFF94A3B8), fontSize = 11.5.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "SERVICIOS" to "Servicios",
                        "SUSCRIPCION" to "Suscripción",
                        "DEUDA_PRESTAMO" to "Deuda/Préstamo",
                        "ARRIENDO" to "Arriendo",
                        "OTRO" to "Otro"
                    ).forEach { (catKey, label) ->
                        val isSel = selectedCategory == catKey
                        Surface(
                            onClick = { selectedCategory = catKey },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) Color(0xFF0284C7) else Color(0xFF0F172A),
                            border = BorderStroke(1.dp, if (isSel) Color(0xFF38BDF8) else Color(0xFF334155))
                        ) {
                            Text(label, color = if (isSel) Color.White else Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Monto a pagar ($)", color = Color(0xFF94A3B8)) },
                    placeholder = { Text("Ej: 250000", color = Color(0xFF64748B)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF38BDF8), unfocusedBorderColor = Color(0xFF334155), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                if (parsedAmount > 0) {
                    Text("Monto formateado: $ ${moneyFormatter.format(parsedAmount)} COP", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                com.example.ui.components.IPhoneAdaptiveDatePickerField(
                    value = dueDateInput,
                    onValueChange = { dueDateInput = it },
                    label = "Fecha límite de pago",
                    title = "Fecha Límite de Pago",
                    placeholder = "AAAA-MM-DD",
                    isDarkTheme = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Notas / Referencia", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF38BDF8), unfocusedBorderColor = Color(0xFF334155), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (entityName.isNotBlank() && parsedAmount > 0) {
                        onSave(
                            entityName,
                            selectedCategory,
                            parsedAmount,
                            dueDateInput,
                            selectedRecurrence,
                            selectedAccount?.id,
                            selectedAccount?.name,
                            notesInput,
                            reminderDays
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                enabled = entityName.isNotBlank() && parsedAmount > 0
            ) {
                Text("Guardar Pago", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, border = BorderStroke(1.dp, Color(0xFF334155))) {
                Text("Cancelar", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        }
    )
}

@Composable
private fun EditScheduledPaymentModal(
    item: UnifiedPaymentItem,
    onDismiss: () -> Unit,
    onSave: (
        id: String,
        name: String,
        category: String,
        amount: Double,
        dueDate: String,
        recurrence: String,
        notes: String?,
        reminderDays: Int
    ) -> Unit
) {
    var entityName by remember { mutableStateOf(item.title) }
    var selectedCategory by remember { mutableStateOf(item.category) }
    var amountInput by remember { mutableStateOf(if (item.amount % 1.0 == 0.0) item.amount.toLong().toString() else item.amount.toString()) }
    var dueDateInput by remember { mutableStateOf(item.dueDate) }
    var selectedRecurrence by remember { mutableStateOf(item.recurrence) }
    var notesInput by remember { mutableStateOf(item.notes ?: "") }
    var reminderDays by remember { mutableIntStateOf(item.reminderDaysBefore) }

    val moneyFormatter = remember { DecimalFormat("#,##0") }
    val parsedAmount = amountInput.toDoubleOrNull() ?: item.amount

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Text("Editar Pago Programado", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = entityName,
                    onValueChange = { entityName = it },
                    label = { Text("Nombre de factura", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF38BDF8), unfocusedBorderColor = Color(0xFF334155), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Monto a pagar ($)", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF38BDF8), unfocusedBorderColor = Color(0xFF334155), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                if (parsedAmount > 0) {
                    Text("Formateado: $ ${moneyFormatter.format(parsedAmount)} COP", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                com.example.ui.components.IPhoneAdaptiveDatePickerField(
                    value = dueDateInput,
                    onValueChange = { dueDateInput = it },
                    label = "Fecha límite de pago",
                    title = "Fecha Límite de Pago",
                    placeholder = "AAAA-MM-DD",
                    isDarkTheme = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Notas", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF38BDF8), unfocusedBorderColor = Color(0xFF334155), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (entityName.isNotBlank() && parsedAmount > 0) {
                        onSave(
                            item.id,
                            entityName,
                            selectedCategory,
                            parsedAmount,
                            dueDateInput,
                            selectedRecurrence,
                            notesInput,
                            reminderDays
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Text("Guardar Cambios", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, border = BorderStroke(1.dp, Color(0xFF334155))) {
                Text("Cancelar", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        }
    )
}

@Composable
private fun CreditCardSimulatorModal(
    cardItem: UnifiedPaymentItem,
    availableAccounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onPayWithAccount: (item: UnifiedPaymentItem, accountId: String, accountName: String, amount: Double) -> Unit
) {
    val totalDebt = cardItem.secondaryAmount ?: cardItem.amount
    val minPayment = cardItem.amount
    var simulationMode by remember { mutableIntStateOf(0) } // 0: Pago Mínimo, 1: Pago Total, 2: Personalizado
    var customAmountInput by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf(availableAccounts.firstOrNull()) }

    val moneyFormatter = remember { DecimalFormat("#,##0") }

    val simPayment = when (simulationMode) {
        0 -> minPayment
        1 -> totalDebt
        else -> (customAmountInput.toDoubleOrNull() ?: minPayment).coerceIn(minPayment, totalDebt)
    }

    val remainingDebt = (totalDebt - simPayment).coerceAtLeast(0.0)
    val estimatedInterestSaved = (simPayment * 0.023) // ~2.3% monthly average interest saved

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFFA78BFA))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simulador de Pago de Tarjeta", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(cardItem.title, color = Color(0xFF38BDF8), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Deuda Total en Extracto", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text("$ ${moneyFormatter.format(totalDebt)}", color = Color(0xFFA78BFA), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Pago Mínimo", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text("$ ${moneyFormatter.format(minPayment)}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = Color(0xFF334155))

                Text("Selecciona modalidad de pago simulado:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Mínimo" to 0, "Total" to 1, "Abono" to 2).forEach { (label, mode) ->
                        val isSel = simulationMode == mode
                        Surface(
                            onClick = { simulationMode = mode },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) Color(0xFF8B5CF6) else Color(0xFF0F172A),
                            border = BorderStroke(1.dp, if (isSel) Color(0xFFA78BFA) else Color(0xFF334155)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(label, color = if (isSel) Color.White else Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (simulationMode == 2) {
                    OutlinedTextField(
                        value = customAmountInput,
                        onValueChange = { customAmountInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Abono deseado ($)", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("Entre ${moneyFormatter.format(minPayment)} y ${moneyFormatter.format(totalDebt)}", color = Color(0xFF64748B)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFA78BFA), unfocusedBorderColor = Color(0xFF334155), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Simulation Result Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Abono a realizar:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Text("$ ${moneyFormatter.format(simPayment)}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Deuda remanente:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Text("$ ${moneyFormatter.format(remainingDebt)}", color = if (remainingDebt == 0.0) Color(0xFF10B981) else Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ahorro estimado en intereses:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Text("+$ ${moneyFormatter.format(estimatedInterestSaved)} / mes", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val acc = selectedAccount ?: availableAccounts.firstOrNull()
                    if (acc != null && simPayment > 0) {
                        onPayWithAccount(cardItem, acc.id, acc.name, simPayment)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
            ) {
                Text("Pagar $ ${moneyFormatter.format(simPayment)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, border = BorderStroke(1.dp, Color(0xFF334155))) {
                Text("Cerrar", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExecutePaymentDialog(
    item: UnifiedPaymentItem,
    availableAccounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirmPayment: (item: UnifiedPaymentItem, accountId: String, accountName: String, amount: Double, categoryName: String) -> Unit
) {
    var selectedAccount by remember { mutableStateOf(availableAccounts.firstOrNull()) }
    var payAmountInput by remember { mutableStateOf(item.amount.toString()) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var isFullPayment by remember { mutableStateOf(true) }

    val moneyFormatter = remember { DecimalFormat("#,##0") }
    val parsedPayAmount = payAmountInput.toDoubleOrNull() ?: item.amount
    val currentAccountBalance = selectedAccount?.availableBalance ?: 0.0
    val balanceAfterPayment = currentAccountBalance - parsedPayAmount
    val isInsufficientBalance = selectedAccount != null && balanceAfterPayment < 0

    val totalDebtAmount = item.secondaryAmount ?: item.amount
    val remainingDebt = (totalDebtAmount - parsedPayAmount).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Column {
                Text("Registrar Pago / Abono a Deuda", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = item.title,
                    color = Color(0xFF38BDF8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Selector de Modalidad (Pago Total vs Abono Parcial)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isFullPayment) Color(0xFF0284C7) else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    isFullPayment = true
                                    payAmountInput = item.amount.toString()
                                }
                        ) {
                            Text(
                                text = "Cuota Completa\n($ ${moneyFormatter.format(item.amount)})",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (!isFullPayment) Color(0xFF0284C7) else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    isFullPayment = false
                                }
                        ) {
                            Text(
                                text = "Abono Parcial / Extra\n(Monto Libre)",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Selector de Cuenta de Débito leída desde Base de Datos
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Saldrá de la cuenta (Base de Datos):", color = Color(0xFF94A3B8), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)

                    ExposedDropdownMenuBox(
                        expanded = isDropdownExpanded,
                        onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedAccount?.let { "${it.name} ($ ${moneyFormatter.format(it.availableBalance)} COP)" } ?: "Selecciona cuenta de origen",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            availableAccounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(acc.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                            Text("Saldo en BD: $ ${moneyFormatter.format(acc.availableBalance)} ${acc.currency}", fontSize = 11.sp, color = Color(0xFF34D399))
                                        }
                                    },
                                    onClick = {
                                        selectedAccount = acc
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Campo de valor a abonar/pagar
                OutlinedTextField(
                    value = payAmountInput,
                    onValueChange = { payAmountInput = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Monto a pagar / abonar ($)", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Resumen de Impacto Financiero
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, if (isInsufficientBalance) Color(0xFFEF4444) else Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("RESUMEN DEL DÉBITO", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Saldrá de la cuenta:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(selectedAccount?.name ?: "Sin seleccionar", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Saldo actual en cuenta:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("$ ${moneyFormatter.format(currentAccountBalance)} ${item.currency}", color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Saldo resultante en BD:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(
                                text = "$ ${moneyFormatter.format(balanceAfterPayment)} ${item.currency}",
                                color = if (isInsufficientBalance) Color(0xFFEF4444) else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (totalDebtAmount > 0) {
                            HorizontalDivider(color = Color(0xFF1E293B))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Deuda remanente estimada:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Text("$ ${moneyFormatter.format(remainingDebt)} ${item.currency}", color = Color(0xFFA78BFA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (isInsufficientBalance) {
                            Text(
                                text = "⚠️ El monto supera el saldo disponible de la cuenta.",
                                color = Color(0xFFFCA5A5),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedAccount != null && parsedPayAmount > 0) {
                        onConfirmPayment(
                            item,
                            selectedAccount!!.id,
                            selectedAccount!!.name,
                            parsedPayAmount,
                            if (item.isCreditCard) "Pago de Tarjeta de Crédito" else "Pago de Facturas y Deudas"
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                enabled = selectedAccount != null && parsedPayAmount > 0
            ) {
                Text("Confirmar Pago / Debitar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, border = BorderStroke(1.dp, Color(0xFF334155))) {
                Text("Cancelar", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentDetailModal(
    item: UnifiedPaymentItem,
    availableAccounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onPayClick: () -> Unit,
    onSimulateClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val moneyFormatter = remember { DecimalFormat("#,##0") }
    var selectedDebitAccount by remember { mutableStateOf(availableAccounts.firstOrNull()) }
    var isAccountDropdownExpanded by remember { mutableStateOf(false) }

    val (badgeText, badgeBg, badgeTextTint) = when (item.urgencyStatus) {
        UrgencyStatus.OVERDUE -> Triple("VENCIDO", Color(0xFF7F1D1D), Color(0xFFFCA5A5))
        UrgencyStatus.CRITICAL -> Triple("CRÍTICO", Color(0xFF7C2D12), Color(0xFFFDBA74))
        UrgencyStatus.UPCOMING -> Triple("PRÓXIMO", Color(0xFF713F12), Color(0xFFFDE047))
        UrgencyStatus.ON_TIME -> Triple("AL DÍA", Color(0xFF064E3B), Color(0xFF6EE7B7))
        UrgencyStatus.PAID -> Triple("PAGADO", Color(0xFF065F46), Color(0xFF34D399))
    }

    val accBalance = selectedDebitAccount?.availableBalance ?: 0.0
    val projectedBalance = accBalance - item.amount

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.isCreditCard) Icons.Default.CreditCard else Icons.Default.Payments,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.category.replace("_", " "),
                            color = Color(0xFF94A3B8),
                            fontSize = 11.5.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg,
                    border = BorderStroke(1.dp, badgeTextTint.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = badgeText,
                        color = badgeTextTint,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 1: Detalle Financiero de la Deuda
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("DETALLE FINANCIERO Y OBLIGACIÓN", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (item.isCreditCard) "Pago mínimo / Cuota sugerida:" else "Monto cuota a pagar:",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$ ${moneyFormatter.format(item.amount)} ${item.currency}",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                softWrap = false,
                                maxLines = 1
                            )
                        }

                        if (item.secondaryAmount != null) {
                            HorizontalDivider(color = Color(0xFF1E293B))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Deuda total / Extracto completo:",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$ ${moneyFormatter.format(item.secondaryAmount)} ${item.currency}",
                                    color = Color(0xFFA78BFA),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Card 2: Calendario y Frecuencia
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("CALENDARIO Y VENCIMIENTO", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Fecha límite de pago:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(item.dueDate, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        val dueDesc = if (item.daysUntilDue < 0) "Vencido hace ${-item.daysUntilDue} días" else if (item.daysUntilDue == 0L) "Vence hoy" else "Faltan ${item.daysUntilDue} días"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Estado de días:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(dueDesc, color = if (item.daysUntilDue < 0) Color(0xFFEF4444) else Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (item.cutOffDate != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Día de corte de tarjeta:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Text(item.cutOffDate, color = Color(0xFFCBD5E1), fontSize = 12.sp)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Frecuencia de cobro:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(if (item.recurrence == "MONTHLY") "Mensual" else item.recurrence, color = Color(0xFFCBD5E1), fontSize = 12.sp)
                        }

                        if (!item.notes.isNull_or_empty()) {
                            HorizontalDivider(color = Color(0xFF1E293B))
                            Text("Notas / Referencia:", color = Color(0xFF94A3B8), fontSize = 11.5.sp)
                            Text(item.notes ?: "", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                // Card 3: Cuenta de Débito (Selección directa leyendo de la Base de Datos)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CUENTA DE DÉBITO (BASE DE DATOS)", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }

                        if (availableAccounts.isNotEmpty()) {
                            Text("Saldrá de la cuenta seleccionada:", color = Color(0xFF94A3B8), fontSize = 11.5.sp)

                            ExposedDropdownMenuBox(
                                expanded = isAccountDropdownExpanded,
                                onExpandedChange = { isAccountDropdownExpanded = !isAccountDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedDebitAccount?.let { "${it.name} ($ ${moneyFormatter.format(it.availableBalance)} COP)" } ?: "Selecciona cuenta",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAccountDropdownExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )

                                ExposedDropdownMenu(
                                    expanded = isAccountDropdownExpanded,
                                    onDismissRequest = { isAccountDropdownExpanded = false }
                                ) {
                                    availableAccounts.forEach { acc ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(acc.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                    Text("Saldo en BD: $ ${moneyFormatter.format(acc.availableBalance)} ${acc.currency}", fontSize = 11.sp, color = Color(0xFF34D399))
                                                }
                                            },
                                            onClick = {
                                                selectedDebitAccount = acc
                                                isAccountDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            selectedDebitAccount?.let { acc ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Saldo disponible en BD:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    Text("$ ${moneyFormatter.format(acc.availableBalance)} COP", color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Saldo estimado tras el pago:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    Text(
                                        text = "$ ${moneyFormatter.format(projectedBalance)} COP",
                                        color = if (projectedBalance < 0) Color(0xFFEF4444) else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Text("Sin cuentas bancarias registradas en la base de datos.", color = Color(0xFFFCA5A5), fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!item.isPaid) {
                    Button(
                        onClick = onPayClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (item.isCreditCard) "💳 Registrar Pago / Abono Tarjeta" else "💳 Registrar Pago / Abono Cuota",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.isCreditCard) {
                        OutlinedButton(
                            onClick = onSimulateClick,
                            border = BorderStroke(1.dp, Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Simular", color = Color(0xFFA78BFA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!item.isCreditCard) {
                        OutlinedButton(
                            onClick = onEditClick,
                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Editar", color = Color(0xFF38BDF8), fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onDeleteClick,
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Eliminar", color = Color(0xFFEF4444), fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        }
    )
}

private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()


