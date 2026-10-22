package com.example.presentation.budget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import com.example.core.ui.components.IosStyleSnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextOverflow
import com.example.core.ui.components.CurrencyText
import com.example.core.ui.components.KashAdaptiveActionRow
import com.example.core.ui.components.KashCompactButton
import com.example.core.ui.components.KashCompactOutlinedButton
import com.example.core.ui.components.KashSearchBar
import com.example.core.ui.components.KashStatusBadge
import com.example.data.local.entity.BudgetItemEntity
import com.example.data.local.entity.BudgetWithItems
import com.example.presentation.budget.dialogs.BudgetConfigModal
import com.example.presentation.budget.dialogs.EditCeilingModal
import com.example.presentation.budget.dialogs.EnvelopeTransactionsModal
import com.example.presentation.budget.dialogs.RebalanceEnvelopesModal
import com.example.presentation.budget.dialogs.SweepSurplusModal
import com.example.presentation.budget.state.BudgetsViewModel
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel,
    onNavigateBack: () -> Unit,
    onOpenCreateBudget: () -> Unit,
    onOpenGoalDetail: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTopMenu by rememberSaveable { mutableStateOf(false) }
    var showBudgetSelectorDropdown by rememberSaveable { mutableStateOf(false) }
    val moneyFormatter = remember { DecimalFormat("#,##0") }

    val bgDark = Color(0xFF07101E)
    val cardBg = Color(0xFF0B172B)
    val cardBorder = Color(0xFF132B4F)
    val primaryBlue = Color(0xFF007AFF)
    val accentCyan = Color(0xFF38BDF8)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF8EA5C4)

    val topTabs = listOf("Presupuesto Activo", "Metas & Regla 50/30/20", "Rebalanceo & Alertas", "Historial")

    LaunchedEffect(state.statusMessage, state.errorMessage) {
        val msg = state.statusMessage ?: state.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    // Modal: Envelope Transactions
    state.envelopeForTransactions?.let { envelope ->
        EnvelopeTransactionsModal(
            envelope = envelope,
            transactions = state.transactions,
            onDismiss = { viewModel.closeEnvelopeTransactions() }
        )
    }

    // Modal: Rebalance Envelopes
    if (state.showRebalanceDialog) {
        val activeEnvelopes = state.activeBudgetWithItems?.items ?: emptyList()
        RebalanceEnvelopesModal(
            availableEnvelopes = activeEnvelopes,
            onDismiss = { viewModel.closeRebalanceDialog() },
            onConfirmRebalance = { sourceId, targetId, amount ->
                viewModel.executeRebalance(sourceId, targetId, amount)
            }
        )
    }

    // Modal: Sweep Surplus to Goal
    if (state.showSweepSurplusDialog) {
        val totalCeiling = state.activeBudgetWithItems?.budget?.totalAmount ?: 0.0
        val totalSpent = state.activeBudgetWithItems?.totalSpent ?: 0.0
        val surplus = (totalCeiling - totalSpent).coerceAtLeast(0.0)
        SweepSurplusModal(
            availableSurplus = surplus,
            activeGoals = state.activeGoals,
            onDismiss = { viewModel.closeSweepSurplusDialog() },
            onConfirmSweep = { goalId, amount ->
                viewModel.sweepSurplusToGoal(goalId, amount)
            }
        )
    }

    // Modal: Edit Ceiling
    if (state.showEditCeilingDialog) {
        val currentCeiling = state.activeBudgetWithItems?.budget?.totalAmount ?: 0.0
        EditCeilingModal(
            currentCeiling = currentCeiling,
            onDismiss = { viewModel.closeEditCeilingDialog() },
            onConfirm = { newCeiling ->
                viewModel.updateCeilingTotal(newCeiling)
            }
        )
    }

    // Modal: Budget Configuration
    if (state.showConfigDialog) {
        BudgetConfigModal(
            currentConfig = state.configuration,
            onDismiss = { viewModel.toggleConfigDialog(false) },
            onSaveConfig = { newConfig ->
                viewModel.updateBudgetConfiguration(newConfig)
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color(0xFF091424),
                border = BorderStroke(1.dp, Color(0xFF102644)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("back_button_budgets")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Regresar",
                                    tint = textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Presupuestos & Sobres",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Planificación financiera y control por categorías",
                                    fontSize = 11.sp,
                                    color = textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Configuración de Presupuestos
                            IconButton(
                                onClick = { viewModel.toggleConfigDialog(true) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("btn_budget_config")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Configurar",
                                    tint = accentCyan,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Privacy Toggle
                            IconButton(
                                onClick = { viewModel.togglePrivacyMode() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.isPrivacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Privacidad",
                                    tint = textSecondary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Nuevo Presupuesto
                            Button(
                                onClick = onOpenCreateBudget,
                                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .heightIn(min = 36.dp)
                                    .testTag("btn_new_budget_top")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Nuevo",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            // Overflow Menu
                            Box {
                                IconButton(
                                    onClick = { showTopMenu = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = textSecondary)
                                }

                                DropdownMenu(
                                    expanded = showTopMenu,
                                    onDismissRequest = { showTopMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Configurar reglas y alertas") },
                                        leadingIcon = { Icon(Icons.Default.Tune, null) },
                                        onClick = {
                                            showTopMenu = false
                                            viewModel.toggleConfigDialog(true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Rebalancear sobres") },
                                        leadingIcon = { Icon(Icons.Default.SwapHoriz, null) },
                                        onClick = {
                                            showTopMenu = false
                                            viewModel.openRebalanceDialog()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Modificar techo global") },
                                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                                        onClick = {
                                            showTopMenu = false
                                            viewModel.openEditCeilingDialog()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Barrer excedente a meta") },
                                        leadingIcon = { Icon(Icons.Default.Savings, null) },
                                        onClick = {
                                            showTopMenu = false
                                            viewModel.openSweepSurplusDialog()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        topTabs.forEachIndexed { index, title ->
                            val isSelected = state.selectedTab == index
                            Surface(
                                onClick = { viewModel.selectTab(index) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) primaryBlue else Color.Transparent,
                                border = if (isSelected) null else BorderStroke(1.dp, Color(0xFF132B4F))
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else textSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { IosStyleSnackbarHost(snackbarHostState) },
        containerColor = bgDark,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("budgets_lazy_column")
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // TAB 0: PRESUPUESTO ACTIVO & SOBRES
            if (state.selectedTab == 0) {
                val activeBudget = state.activeBudgetWithItems
                if (activeBudget == null) {
                    item {
                        EmptyBudgetCard(onOpenCreateBudget = onOpenCreateBudget)
                    }
                } else {
                    item {
                        Spacer(modifier = Modifier.height(2.dp))
                        // Budget Selector Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    activeBudget.budget.name,
                                    color = textPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${activeBudget.budget.startDate} al ${activeBudget.budget.endDate}",
                                    color = textSecondary,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (state.budgets.size > 1) {
                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0F2544),
                                        border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                                        modifier = Modifier.clickable { showBudgetSelectorDropdown = true }
                                    ) {
                                        Text(
                                            "Cambiar presupuesto ▼",
                                            color = accentCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showBudgetSelectorDropdown,
                                        onDismissRequest = { showBudgetSelectorDropdown = false }
                                    ) {
                                        state.budgets.forEach { b ->
                                            DropdownMenuItem(
                                                text = { Text(b.budget.name) },
                                                onClick = {
                                                    viewModel.selectActiveBudget(b.budget.id)
                                                    showBudgetSelectorDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Main Execution Hero Card
                    item {
                        BudgetHeroExecutionCard(
                            budget = activeBudget,
                            burnRate = state.burnRate,
                            isPrivacyMode = state.isPrivacyMode,
                            onEditCeiling = { viewModel.openEditCeilingDialog() },
                            onRebalance = { viewModel.openRebalanceDialog() },
                            onSweep = { viewModel.openSweepSurplusDialog() }
                        )
                    }

                    // Burn Rate Diagnostic Card
                    item {
                        BurnRateSummaryCard(
                            burnRate = state.burnRate,
                            isPrivacyMode = state.isPrivacyMode
                        )
                    }

                    // Envelopes List Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sobres por Categoría (${activeBudget.items.size})",
                                color = textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                            Text(
                                text = "Rebalancear fondos →",
                                color = accentCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier
                                    .clickable { viewModel.openRebalanceDialog() }
                                    .padding(4.dp)
                            )
                        }
                    }

                    // Search & Filter Envelopes Bar
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            KashSearchBar(
                                query = state.categorySearchQuery,
                                onQueryChange = { viewModel.setCategorySearchQuery(it) },
                                placeholder = "Buscar sobre por categoría...",
                                containerColor = cardBg,
                                borderColor = cardBorder,
                                textColor = textPrimary,
                                placeholderColor = textSecondary,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Status Filter Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val chips = listOf(
                                    "TODOS" to "Todos (${activeBudget.items.size})",
                                    "NORMAL" to "En Meta",
                                    "ALERTA" to "Alerta (≥${state.configuration.warningThresholdPct}%)",
                                    "SOBREGIRADO" to "Sobregirados (100%+)"
                                )
                                chips.forEach { (key, label) ->
                                    val isSelected = state.envelopeFilterStatus == key
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) primaryBlue else cardBg,
                                        border = BorderStroke(1.dp, if (isSelected) accentCyan else cardBorder),
                                        modifier = Modifier.clickable { viewModel.setEnvelopeFilterStatus(key) }
                                    ) {
                                        Text(
                                            text = label,
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

                    val filteredEnvelopes = activeBudget.items.filter { env ->
                        val matchesQuery = state.categorySearchQuery.isBlank() || env.categoryName.contains(state.categorySearchQuery, ignoreCase = true)
                        val spentPct = if (env.allocatedAmount > 0) (env.spentAmount / env.allocatedAmount) * 100.0 else 0.0
                        val matchesStatus = when (state.envelopeFilterStatus) {
                            "NORMAL" -> spentPct < state.configuration.warningThresholdPct
                            "ALERTA" -> spentPct >= state.configuration.warningThresholdPct && spentPct < 100.0
                            "SOBREGIRADO" -> spentPct >= 100.0
                            else -> true
                        }
                        matchesQuery && matchesStatus
                    }

                    if (filteredEnvelopes.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No hay sobres que coincidan con el filtro actual",
                                        color = textSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    } else {
                        // Envelope Items
                        items(filteredEnvelopes, key = { it.id }) { envelope ->
                            EnvelopeItemCard(
                                envelope = envelope,
                                isPrivacyMode = state.isPrivacyMode,
                                onViewTransactions = { viewModel.openTransactionsForEnvelope(envelope) },
                                onRebalance = { viewModel.openRebalanceDialog() },
                                onQuickAdd = { amount ->
                                    viewModel.updateEnvelopeAllocation(envelope.id, envelope.allocatedAmount + amount)
                                }
                            )
                        }
                    }
                }
            }

            // TAB 1: METAS & REGLA DE DISTRIBUCIÓN
            else if (state.selectedTab == 1) {
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = "Distribución: ${state.configuration.distributionRule.title}",
                                color = textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Diagnóstico de gastos vs metas (${state.fiftyThirtyTwenty.targetNeedsPct.toInt()}/${state.fiftyThirtyTwenty.targetWantsPct.toInt()}/${state.fiftyThirtyTwenty.targetSavingsPct.toInt()})",
                                color = textSecondary,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        KashCompactOutlinedButton(
                            text = "Configurar",
                            onClick = { viewModel.toggleConfigDialog(true) },
                            borderColor = accentCyan,
                            containerColor = primaryBlue.copy(alpha = 0.2f),
                            contentColor = accentCyan,
                            fontSize = 11.sp
                        )
                    }
                }

                // 50/30/20 Rule Breakdown Card
                item {
                    FiftyThirtyTwentyCard(
                        fifty = state.fiftyThirtyTwenty,
                        isPrivacyMode = state.isPrivacyMode
                    )
                }

                // Surplus Sweep Action Card
                item {
                    SurplusSweepActionCard(
                        activeBudget = state.activeBudgetWithItems,
                        activeGoalsCount = state.activeGoals.size,
                        isPrivacyMode = state.isPrivacyMode,
                        onOpenSweep = { viewModel.openSweepSurplusDialog() }
                    )
                }

                // Goals Linked to Budget Envelopes
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Metas Financieras Vinculadas (${state.activeGoals.size})",
                            color = textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Aportar cuotas programadas",
                            color = textSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                }

                if (state.activeGoals.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Aún no tienes metas financieras registradas", color = textSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(state.activeGoals, key = { it.id }) { goal ->
                        GoalBudgetLinkCard(
                            goal = goal,
                            isPrivacyMode = state.isPrivacyMode,
                            onContribute = { amount ->
                                viewModel.contributeDirectlyToGoal(goal.id, amount)
                            }
                        )
                    }
                }
            }

            // TAB 2: REBALANCEO & ALERTAS
            else if (state.selectedTab == 2) {
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Rebalanceo de Sobres y Alertas", color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Mantén el techo bajo control transfiriendo fondos entre sobres.", color = textSecondary, fontSize = 12.sp)
                }

                item {
                    RebalanceActionBanner(
                        onOpenRebalance = { viewModel.openRebalanceDialog() }
                    )
                }

                // Overspent / At Risk Envelopes
                val activeItems = state.activeBudgetWithItems?.items ?: emptyList()
                val atRiskItems = activeItems.filter {
                    val pct = if (it.allocatedAmount > 0) (it.spentAmount / it.allocatedAmount) else 0.0
                    pct >= 0.8
                }

                item {
                    Text(
                        text = "Sobres en Alerta o Sobregirados (${atRiskItems.size})",
                        color = textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (atRiskItems.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("¡Todos tus sobres están bajo control!", color = textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                    Text("Ningún sobre supera el 80% de su límite asignado.", color = textSecondary, fontSize = 11.5.sp)
                                }
                            }
                        }
                    }
                } else {
                    items(atRiskItems, key = { "risk_${it.id}" }) { envelope ->
                        EnvelopeItemCard(
                            envelope = envelope,
                            isPrivacyMode = state.isPrivacyMode,
                            onViewTransactions = { viewModel.openTransactionsForEnvelope(envelope) },
                            onRebalance = { viewModel.openRebalanceDialog() },
                            onQuickAdd = { amount ->
                                viewModel.updateEnvelopeAllocation(envelope.id, envelope.allocatedAmount + amount)
                            }
                        )
                    }
                }
            }

            // TAB 3: HISTORIAL & GESTIÓN
            else if (state.selectedTab == 3) {
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Historial de Presupuestos",
                            color = textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        KashCompactButton(
                            text = "+ Crear",
                            onClick = onOpenCreateBudget,
                            containerColor = primaryBlue,
                            contentColor = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }

                if (state.budgets.isEmpty()) {
                    item {
                        EmptyBudgetCard(onOpenCreateBudget = onOpenCreateBudget)
                    }
                } else {
                    items(state.budgets, key = { it.budget.id }) { b ->
                        BudgetHistoryCard(
                            budgetWithItems = b,
                            isActive = b.budget.id == state.activeBudgetId,
                            isPrivacyMode = state.isPrivacyMode,
                            onSelect = { viewModel.selectActiveBudget(b.budget.id) },
                            onClone = { viewModel.cloneBudgetForNextMonth(b.budget.id) },
                            onDelete = { viewModel.deleteBudget(b.budget.id) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun BudgetHeroExecutionCard(
    budget: BudgetWithItems,
    burnRate: com.example.presentation.budget.state.BurnRateAnalysis,
    isPrivacyMode: Boolean,
    onEditCeiling: () -> Unit,
    onRebalance: () -> Unit,
    onSweep: () -> Unit
) {
    val moneyFormatter = remember { DecimalFormat("#,##0") }
    val totalCeiling = budget.budget.totalAmount
    val totalSpent = budget.totalSpent
    val remaining = totalCeiling - totalSpent
    val percentage = if (totalCeiling > 0) ((totalSpent / totalCeiling) * 100.0).coerceIn(0.0, 150.0) else 0.0

    val progressColor = when {
        percentage >= 100.0 -> Color(0xFFEF4444)
        percentage >= 85.0 -> Color(0xFFF59E0B)
        percentage >= 70.0 -> Color(0xFFFBBF24)
        else -> Color(0xFF00E676)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B172B)),
        border = BorderStroke(1.dp, Color(0xFF132B4F)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Icon + Title + Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF2563EB).copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PieChart, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Ejecución Global",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${burnRate.daysRemaining} días restantes del periodo",
                            color = Color(0xFF8EA5C4),
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = progressColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, progressColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", percentage)}% Usado",
                        color = progressColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Metric Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF061120),
                border = BorderStroke(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "DISPONIBLE PARA GASTAR",
                        color = Color(0xFF38BDF8),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    if (isPrivacyMode) {
                        Text(
                            text = "$ ••••••••",
                            color = if (remaining < 0) Color(0xFFEF4444) else Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                    } else {
                        CurrencyText(
                            amountDouble = remaining,
                            color = if (remaining < 0) Color(0xFFEF4444) else Color.White,
                            fontSize = 24.sp,
                            minFontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { (percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = progressColor,
                        trackColor = Color(0xFF132B4F),
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText(
                            amountDouble = totalSpent,
                            prefix = "Gastado: ",
                            color = Color(0xFF8EA5C4),
                            fontSize = 11.5.sp,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(end = 4.dp)
                        )
                        CurrencyText(
                            amountDouble = totalCeiling,
                            prefix = "Techo: ",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(start = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Tool Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KashCompactOutlinedButton(
                    text = "Editar Techo",
                    onClick = onEditCeiling,
                    borderColor = Color(0xFF1E3A8A),
                    contentColor = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
                KashCompactOutlinedButton(
                    text = "Rebalancear",
                    onClick = onRebalance,
                    borderColor = Color(0xFF1E3A8A),
                    contentColor = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
                KashCompactButton(
                    text = "Barrer Meta",
                    onClick = onSweep,
                    containerColor = Color(0xFF00C853),
                    contentColor = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BurnRateSummaryCard(
    burnRate: com.example.presentation.budget.state.BurnRateAnalysis,
    isPrivacyMode: Boolean
) {
    val moneyFormatter = remember { DecimalFormat("#,##0") }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B172B)),
        border = BorderStroke(1.dp, Color(0xFF132B4F)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Ritmo Diario Permitido (Burn Rate)",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (burnRate.burnStatus == "DENTRO_DEL_RITMO") Color(0xFF00C853).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (burnRate.burnStatus == "DENTRO_DEL_RITMO") Color(0xFF00E676) else Color(0xFFEF4444))
                ) {
                    Text(
                        text = if (burnRate.burnStatus == "DENTRO_DEL_RITMO") "En Ritmo" else "Riesgo Sobregiro",
                        color = if (burnRate.burnStatus == "DENTRO_DEL_RITMO") Color(0xFF00E676) else Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                ) {
                    Text(
                        "Límite diario recomendado",
                        color = Color(0xFF8EA5C4),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isPrivacyMode) {
                        Text(
                            text = "$ ••••",
                            color = Color(0xFF00E676),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    } else {
                        CurrencyText(
                            amountDouble = burnRate.allowedDailySpending,
                            suffix = "/día",
                            color = Color(0xFF00E676),
                            fontSize = 13.sp,
                            minFontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "Ritmo real consumido",
                        color = Color(0xFF8EA5C4),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isPrivacyMode) {
                        Text(
                            text = "$ ••••",
                            color = if (burnRate.actualDailySpending > burnRate.allowedDailySpending) Color(0xFFEF4444) else Color(0xFF38BDF8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    } else {
                        CurrencyText(
                            amountDouble = burnRate.actualDailySpending,
                            suffix = "/día",
                            color = if (burnRate.actualDailySpending > burnRate.allowedDailySpending) Color(0xFFEF4444) else Color(0xFF38BDF8),
                            fontSize = 13.sp,
                            minFontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EnvelopeItemCard(
    envelope: BudgetItemEntity,
    isPrivacyMode: Boolean,
    onViewTransactions: () -> Unit,
    onRebalance: () -> Unit,
    onQuickAdd: (Double) -> Unit
) {
    val moneyFormatter = remember { DecimalFormat("#,##0") }
    val allocated = envelope.allocatedAmount
    val spent = envelope.spentAmount
    val remaining = allocated - spent
    val pct = if (allocated > 0) ((spent / allocated) * 100.0).coerceIn(0.0, 150.0) else 0.0

    val barColor = when {
        pct >= 100.0 -> Color(0xFFEF4444)
        pct >= 80.0 -> Color(0xFFF59E0B)
        else -> Color(0xFF00E676)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B172B)),
        border = BorderStroke(1.dp, Color(0xFF132B4F)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF1E3A8A).copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Category, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            envelope.categoryName,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        CurrencyText(
                            amountDouble = Math.abs(remaining),
                            prefix = if (remaining < 0) "Sobregiro de " else "Quedan ",
                            color = if (remaining < 0) Color(0xFFEF4444) else Color(0xFF00E676),
                            fontSize = 11.5.sp,
                            minFontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = barColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, barColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", pct)}%",
                        color = barColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { (pct / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = barColor,
                trackColor = Color(0xFF132B4F),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    CurrencyText(
                        amountDouble = spent,
                        prefix = "Gastado: ",
                        color = Color(0xFF8EA5C4),
                        fontSize = 11.sp
                    )
                    CurrencyText(
                        amountDouble = allocated,
                        prefix = "• Asignado: ",
                        color = Color(0xFF8EA5C4),
                        fontSize = 11.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onViewTransactions,
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF061120),
                        border = BorderStroke(1.dp, Color(0xFF1E3A8A))
                    ) {
                        Text(
                            "Ver Gastos",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        onClick = { onQuickAdd(50_000.0) },
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF061120),
                        border = BorderStroke(1.dp, Color(0xFF1E3A8A))
                    ) {
                        Text(
                            "+$50K",
                            color = Color(0xFF00E676),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FiftyThirtyTwentyCard(
    fifty: com.example.presentation.budget.state.FiftyThirtyTwentyBreakdown,
    isPrivacyMode: Boolean
) {
    val moneyFormatter = remember { DecimalFormat("#,##0") }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B172B)),
        border = BorderStroke(1.dp, Color(0xFF132B4F)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Distribución Actual vs Ideal",
                    color = Color.White,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0F2544),
                    border = BorderStroke(1.dp, Color(0xFF1E3A8A))
                ) {
                    Text(
                        fifty.statusLabel,
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 50% Needs
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "50% Necesidades Básicas",
                        color = Color(0xFF8EA5C4),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                    )
                    Text(
                        "${String.format(java.util.Locale.US, "%.1f", fifty.needsPct)}% ($ ${moneyFormatter.format(fifty.needsAllocated)})",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (fifty.needsPct / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = if (fifty.needsPct > 60.0) Color(0xFFEF4444) else Color(0xFF38BDF8),
                    trackColor = Color(0xFF132B4F)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 30% Wants
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "30% Deseos y Estilo de Vida",
                        color = Color(0xFF8EA5C4),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                    )
                    Text(
                        "${String.format(java.util.Locale.US, "%.1f", fifty.wantsPct)}% ($ ${moneyFormatter.format(fifty.wantsAllocated)})",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (fifty.wantsPct / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = if (fifty.wantsPct > 35.0) Color(0xFFF59E0B) else Color(0xFFA78BFA),
                    trackColor = Color(0xFF132B4F)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 20% Savings
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "20% Metas de Ahorro e Inversión",
                        color = Color(0xFF8EA5C4),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                    )
                    Text(
                        "${String.format(java.util.Locale.US, "%.1f", fifty.savingsPct)}% ($ ${moneyFormatter.format(fifty.savingsAllocated)})",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (fifty.savingsPct / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF00E676),
                    trackColor = Color(0xFF132B4F)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF061120),
                border = BorderStroke(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = fifty.adviceMessage,
                    color = Color(0xFF8EA5C4),
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun SurplusSweepActionCard(
    activeBudget: BudgetWithItems?,
    activeGoalsCount: Int,
    isPrivacyMode: Boolean,
    onOpenSweep: () -> Unit
) {
    val moneyFormatter = remember { DecimalFormat("#,##0") }
    val totalCeiling = activeBudget?.budget?.totalAmount ?: 0.0
    val totalSpent = activeBudget?.totalSpent ?: 0.0
    val surplus = (totalCeiling - totalSpent).coerceAtLeast(0.0)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B172B)),
        border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF00C853).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Savings, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Barrer Excedente a Metas",
                            color = Color.White,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "$activeGoalsCount metas activas disponibles para abonar",
                            color = Color(0xFF8EA5C4),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                KashCompactButton(
                    text = "Barrer",
                    onClick = onOpenSweep,
                    containerColor = Color(0xFF00C853),
                    contentColor = Color.White,
                    fontSize = 11.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Tienes $ ${moneyFormatter.format(surplus)} de excedente presupuestal no gastado. Transfiérelo con un clic a tus objetivos.",
                color = Color(0xFF8EA5C4),
                fontSize = 11.5.sp
            )
        }
    }
}

@Composable
private fun GoalBudgetLinkCard(
    goal: com.example.data.local.entity.FinancialGoalEntity,
    isPrivacyMode: Boolean,
    onContribute: (Double) -> Unit
) {
    val moneyFormatter = remember { DecimalFormat("#,##0") }
    val pct = if (goal.targetAmount > 0) ((goal.savedAmount / goal.targetAmount) * 100.0).coerceIn(0.0, 100.0) else 0.0
    val monthlyPledge = goal.monthlyContribution ?: 50_000.0

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B172B)),
        border = BorderStroke(1.dp, Color(0xFF132B4F)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        goal.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Cuenta: ${goal.targetAccountName} • Cuota: $ ${moneyFormatter.format(monthlyPledge)}/mes",
                        color = Color(0xFF8EA5C4),
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0F2544),
                    border = BorderStroke(1.dp, Color(0xFF1E3A8A))
                ) {
                    Text(
                        "${String.format(java.util.Locale.US, "%.1f", pct)}%",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { (pct / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF38BDF8),
                trackColor = Color(0xFF132B4F)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ahorrado: $ ${moneyFormatter.format(goal.savedAmount)} / $ ${moneyFormatter.format(goal.targetAmount)}",
                    color = Color(0xFF8EA5C4),
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )

                KashCompactOutlinedButton(
                    text = "Aportar $ ${moneyFormatter.format(monthlyPledge)}",
                    onClick = { onContribute(monthlyPledge) },
                    borderColor = Color(0xFF00E676),
                    contentColor = Color(0xFF00E676),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun RebalanceActionBanner(onOpenRebalance: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B172B)),
        border = BorderStroke(1.dp, Color(0xFF132B4F)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF007AFF).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Rebalanceo Entre Sobres",
                            color = Color.White,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Mueve dinero sin romper tu presupuesto mensual",
                            color = Color(0xFF8EA5C4),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                KashCompactButton(
                    text = "Rebalancear",
                    onClick = onOpenRebalance,
                    containerColor = Color(0xFF007AFF),
                    contentColor = Color.White,
                    fontSize = 11.5.sp
                )
            }
        }
    }
}

@Composable
private fun BudgetHistoryCard(
    budgetWithItems: BudgetWithItems,
    isActive: Boolean,
    isPrivacyMode: Boolean,
    onSelect: () -> Unit,
    onClone: () -> Unit,
    onDelete: () -> Unit
) {
    val moneyFormatter = remember { DecimalFormat("#,##0") }
    val b = budgetWithItems.budget

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isActive) Color(0xFF0F2544) else Color(0xFF0B172B)),
        border = BorderStroke(1.dp, if (isActive) Color(0xFF007AFF) else Color(0xFF132B4F)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            b.name,
                            color = Color.White,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isActive) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF00C853).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFF00E676))
                            ) {
                                Text(
                                    "ACTIVO",
                                    color = Color(0xFF00E676),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        "${b.startDate} al ${b.endDate} • ${budgetWithItems.items.size} sobres",
                        color = Color(0xFF8EA5C4),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isPrivacyMode) {
                    Text(
                        text = "$ ••••••••",
                        color = Color(0xFF38BDF8),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                } else {
                    CurrencyText(
                        amountDouble = b.totalAmount,
                        color = Color(0xFF38BDF8),
                        fontSize = 14.sp,
                        minFontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isActive) {
                    KashCompactOutlinedButton(
                        text = "Activar",
                        onClick = onSelect,
                        borderColor = Color(0xFF1E3A8A),
                        contentColor = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                KashCompactOutlinedButton(
                    text = "Clonar mes sig.",
                    onClick = onClone,
                    icon = Icons.Default.ContentCopy,
                    borderColor = Color(0xFF1E3A8A),
                    contentColor = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyBudgetCard(onOpenCreateBudget: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B172B)),
        border = BorderStroke(1.dp, Color(0xFF132B4F)),
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
                    .size(54.dp)
                    .background(Color(0xFF2563EB).copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PieChart, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("No tienes ningún presupuesto registrado", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Crea un presupuesto mensual por sobres para controlar tus gastos y proteger tus metas.",
                color = Color(0xFF8EA5C4),
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onOpenCreateBudget,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Crear mi primer presupuesto", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
