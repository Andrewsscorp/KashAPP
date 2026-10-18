package com.example.feature.objetivos.ui

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.components.KashCompactButton
import com.example.feature.objetivos.domain.engine.GoalVelocityMetrics
import com.example.feature.objetivos.domain.engine.ProjectedScenarioResult
import com.example.feature.objetivos.presentation.ObjetivosViewModel
import com.example.feature.objetivos.ui.components.GoalFilterBar
import com.example.feature.objetivos.ui.components.GoalIndividualCard
import com.example.feature.objetivos.ui.components.GoalSummaryCard
import com.example.feature.objetivos.ui.detail.GoalDetailScreen
import com.example.feature.objetivos.ui.dialogs.RegisterContributionDialog
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjetivosScreen(
    viewModel: ObjetivosViewModel,
    onNavigateBack: () -> Unit,
    onOpenCreateGoalForm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTopMenu by remember { mutableStateOf(false) }
    val moneyFormatter = remember { DecimalFormat("#,##0") }

    // Exact Activos Dark Navy Palette
    val bgDark = Color(0xFF07101E)
    val cardBg = Color(0xFF0B172B)
    val cardBorder = Color(0xFF132B4F)
    val primaryBlue = Color(0xFF007AFF)
    val accentCyan = Color(0xFF38BDF8)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF8EA5C4)

    val topTabs = listOf("Resumen", "Objetivos", "Comprometidos", "Proyecciones", "Hitos & Asesor")

    LaunchedEffect(state.notificationMessage, state.errorMessage) {
        val msg = state.notificationMessage ?: state.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    // Sub-Screen: Goal Detail Screen
    if (state.activeGoalForDetail != null) {
        androidx.activity.compose.BackHandler {
            viewModel.closeGoalDetail()
        }
        GoalDetailScreen(
            goal = state.activeGoalForDetail!!,
            onBack = { viewModel.closeGoalDetail() },
            onRegisterContribution = {
                viewModel.openContributionDialog(state.activeGoalForDetail!!)
            },
            onMarkCompleted = {
                viewModel.markGoalAsCompleted(state.activeGoalForDetail!!.id)
            },
            onToggleArchive = {
                viewModel.toggleGoalArchive(
                    goalId = state.activeGoalForDetail!!.id,
                    archive = state.activeGoalForDetail!!.isActive
                )
            },
            onDelete = {
                viewModel.deleteGoal(state.activeGoalForDetail!!.id)
            },
            isPrivacyMode = state.isPrivacyMode
        )
        return
    }

    // Sub-Dialog: Register Contribution
    if (state.showContributionDialogForGoal != null) {
        RegisterContributionDialog(
            goal = state.showContributionDialogForGoal!!,
            availableAccounts = state.availableAccounts,
            onDismiss = { viewModel.closeContributionDialog() },
            onSaveContribution = { amount, accountId, isAllocation, keepOpen ->
                viewModel.registerContribution(
                    goalId = state.showContributionDialogForGoal!!.id,
                    amount = amount,
                    sourceAccountId = accountId,
                    isBudgetaryAllocation = isAllocation,
                    keepOpenForAnother = keepOpen
                )
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
                    // Top Bar Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 14.dp),
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
                                    .size(38.dp)
                                    .testTag("back_button_objetivos")
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
                                    text = "Mis objetivos",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Mental Accounting y Fondos Comprometidos",
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
                            // Privacy Mode toggle
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

                            // Professional "+ Nuevo objetivo" Button (Instant Click)
                            KashCompactButton(
                                text = "Nuevo",
                                icon = Icons.Default.Add,
                                onClick = onOpenCreateGoalForm,
                                minHeight = 36.dp,
                                modifier = Modifier.testTag("add_goal_top_button")
                            )

                            // Overflow menu
                            Box {
                                IconButton(
                                    onClick = { showTopMenu = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Más",
                                        tint = textSecondary
                                    )
                                }

                                DropdownMenu(
                                    expanded = showTopMenu,
                                    onDismissRequest = { showTopMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Recargar datos") },
                                        leadingIcon = { Icon(Icons.Default.Refresh, null) },
                                        onClick = {
                                            showTopMenu = false
                                            viewModel.loadData()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Crear objetivo") },
                                        leadingIcon = { Icon(Icons.Default.Add, null) },
                                        onClick = {
                                            showTopMenu = false
                                            onOpenCreateGoalForm()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Top Tab Navigation Bar (Activos Navy Style)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        topTabs.forEachIndexed { index, title ->
                            val isSelected = state.selectedTopTab == index
                            Surface(
                                onClick = {
                                    try {
                                        viewModel.selectTopTab(index)
                                    } catch (e: Exception) {
                                        android.util.Log.e("ObjetivosScreen", "Error selecting tab $index", e)
                                    }
                                },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = bgDark,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("objetivos_lazy_column"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // TAB 0: RESUMEN
            if (state.selectedTopTab == 0) {
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    GoalSummaryCard(
                        summary = state.summary,
                        committedAmount = state.commitmentSummary.totalCommittedFunds,
                        isPrivacyMode = state.isPrivacyMode
                    )
                }

                item {
                    // Quick section header: Metas Principales + Ver todas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Metas Destacadas",
                            color = textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ver lista completa →",
                            color = accentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { viewModel.selectTopTab(1) }
                                .padding(4.dp)
                        )
                    }
                }

                if (state.allGoals.isEmpty()) {
                    item {
                        EmptyGoalsCard(onAddGoal = onOpenCreateGoalForm)
                    }
                } else {
                    itemsIndexed(state.allGoals.take(4), key = { idx, g -> "g0_${g.id}_$idx" }) { _, goal ->
                        GoalIndividualCard(
                            goal = goal,
                            onCardClick = { try { viewModel.openGoalDetail(goal) } catch (e: Exception) { android.util.Log.e("ObjetivosScreen", "Error opening goal detail", e) } },
                            onRegisterContribution = { try { viewModel.openContributionDialog(goal) } catch (e: Exception) { android.util.Log.e("ObjetivosScreen", "Error opening contribution dialog", e) } },
                            onMarkCompleted = { try { viewModel.markGoalAsCompleted(goal.id) } catch (e: Exception) { android.util.Log.e("ObjetivosScreen", "Error marking completed", e) } },
                            onToggleArchive = { try { viewModel.toggleGoalArchive(goal.id, goal.isActive) } catch (e: Exception) { android.util.Log.e("ObjetivosScreen", "Error toggling archive", e) } },
                            onDelete = { try { viewModel.deleteGoal(goal.id) } catch (e: Exception) { android.util.Log.e("ObjetivosScreen", "Error deleting goal", e) } },
                            isPrivacyMode = state.isPrivacyMode
                        )
                    }
                }
            }

            // TAB 1: OBJETIVOS (LISTA COMPLETA & FILTROS)
            else if (state.selectedTopTab == 1) {
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    GoalFilterBar(
                        selectedTab = state.selectedFilterTab,
                        onTabSelected = { viewModel.onFilterTabSelected(it) },
                        searchQuery = state.searchQuery,
                        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                        selectedSort = state.selectedSortOption,
                        onSortSelected = { viewModel.onSortOptionSelected(it) },
                        availableCategories = state.availableCategories,
                        selectedCategory = state.selectedCategoryFilter,
                        onCategorySelected = { viewModel.onCategoryFilterSelected(it) },
                        availableAccounts = state.availableAccounts,
                        selectedAccount = state.selectedAccountFilter,
                        onAccountSelected = { viewModel.onAccountFilterSelected(it) }
                    )
                }

                if (state.filteredGoals.isEmpty()) {
                    item {
                        EmptyGoalsCard(onAddGoal = onOpenCreateGoalForm)
                    }
                } else {
                    itemsIndexed(state.filteredGoals, key = { idx, g -> "g1_${g.id}_$idx" }) { _, goal ->
                        GoalIndividualCard(
                            goal = goal,
                            onCardClick = { try { viewModel.openGoalDetail(goal) } catch (e: Exception) { android.util.Log.e("ObjetivosScreen", "Error opening goal detail", e) } },
                            onRegisterContribution = { try { viewModel.openContributionDialog(goal) } catch (e: Exception) { android.util.Log.e("ObjetivosScreen", "Error opening contribution dialog", e) } },
                            onMarkCompleted = { try { viewModel.markGoalAsCompleted(goal.id) } catch (e: Exception) { android.util.Log.e("ObjetivosScreen", "Error marking completed", e) } },
                            onToggleArchive = { try { viewModel.toggleGoalArchive(goal.id, goal.isActive) } catch (e: Exception) { android.util.Log.e("ObjetivosScreen", "Error toggling archive", e) } },
                            onDelete = { try { viewModel.deleteGoal(goal.id) } catch (e: Exception) { android.util.Log.e("ObjetivosScreen", "Error deleting goal", e) } },
                            isPrivacyMode = state.isPrivacyMode
                        )
                    }
                }
            }

            // TAB 2: COMPROMETIDOS (Lógica Real: GoalCommitmentEngine)
            else if (state.selectedTopTab == 2) {
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    CommittedFundsHeaderCard(
                        totalCommitted = state.commitmentSummary.totalCommittedFunds,
                        totalLiquidity = state.commitmentSummary.totalAvailableLiquidity,
                        ratio = state.commitmentSummary.globalCommitmentRatio,
                        committedGoalsCount = state.commitmentSummary.committedGoalsCount,
                        isPrivacyMode = state.isPrivacyMode
                    )
                }

                item {
                    Text(
                        text = "Desglose por Cuentas",
                        color = textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                itemsIndexed(state.commitmentSummary.accountBreakdowns, key = { idx, acc -> "acc_${acc.accountId}_$idx" }) { _, acc ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(acc.accountName, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF0F2544),
                                    border = BorderStroke(1.dp, Color(0xFF1E3A8A))
                                ) {
                                    Text(
                                        text = "${String.format("%.1f", acc.commitmentRatio)}% Comprometido",
                                        color = accentCyan,
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
                                    Text("Aportes Comprometidos", color = textSecondary, fontSize = 11.sp)
                                    Text(
                                        text = if (state.isPrivacyMode) "$ ••••" else "$ ${moneyFormatter.format(acc.committedAmount)}",
                                        color = Color(0xFF00E676),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text("Disponible Libre", color = textSecondary, fontSize = 11.sp)
                                    Text(
                                        text = if (state.isPrivacyMode) "$ ••••" else "$ ${moneyFormatter.format(acc.freeAvailableBalance)}",
                                        color = accentCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text("Saldo Total", color = textSecondary, fontSize = 11.sp)
                                    Text(
                                        text = if (state.isPrivacyMode) "$ ••••" else "$ ${moneyFormatter.format(acc.totalBalance)}",
                                        color = textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // TAB 3: PROYECCIONES (Lógica Real: GoalProjectionEngine)
            else if (state.selectedTopTab == 3) {
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Simulación y Proyecciones Financieras",
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cálculo real con inflación estimada (4.5%) y rendimiento compuesto de ahorro.",
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                }

                if (state.projections.isEmpty()) {
                    item {
                        EmptyGoalsCard(onAddGoal = onOpenCreateGoalForm)
                    }
                } else {
                    itemsIndexed(state.projections, key = { idx, proj -> "proj_${proj.goalId}_$idx" }) { _, proj ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(proj.goalName, color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Déficit por ahorrar: $ ${moneyFormatter.format(proj.remainingDeficit)}",
                                    color = accentCyan,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                proj.scenarios.forEach { sc ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF061120),
                                        border = BorderStroke(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(sc.scenario.label, color = textPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                                Text(sc.scenario.description, color = textSecondary, fontSize = 10.5.sp)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("$ ${moneyFormatter.format(sc.monthlyPledge)}/mes", color = accentCyan, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                                Text("Meta: ${sc.estimatedCompletionDate}", color = Color(0xFF00E676), fontSize = 10.5.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 4: HITOS & ASESOR (Lógica Real: GoalMilestoneEngine & GoalContributionAdvisor)
            else if (state.selectedTopTab == 4) {
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Asesor y Distribución de Ahorro",
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Recomendaciones algorítmicas de micro-ahorro según liquidez, periodicidad y urgencia.",
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                }

                val adviceList = state.advisorPlan?.adviceList.orEmpty()
                if (adviceList.isEmpty()) {
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
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No hay metas activas que requieran asesoría",
                                    color = textSecondary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        try { onOpenCreateGoalForm() } catch (e: Exception) {
                                            android.util.Log.e("ObjetivosScreen", "Error opening goal form", e)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Crear objetivo financiero", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    // Advisor Portfolio Plan with unique composite key and error handling
                    itemsIndexed(adviceList, key = { idx, adv -> "adv_${adv.goalId.ifEmpty { "id" }}_$idx" }) { idx, adv ->
                        val matchingGoal = state.allGoals.firstOrNull { g -> g.id == adv.goalId }
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        matchingGoal?.let { viewModel.openGoalDetail(it) }
                                    } catch (e: Exception) {
                                        android.util.Log.e("ObjetivosScreen", "Error opening goal detail from advisor", e)
                                    }
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = adv.goalName.ifBlank { "Objetivo" },
                                        color = textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (adv.urgencyLevel == "ALTA") Color(0xFF7F1D1D) else Color(0xFF0F2544),
                                        border = BorderStroke(1.dp, if (adv.urgencyLevel == "ALTA") Color(0xFFEF4444) else Color(0xFF1E3A8A))
                                    ) {
                                        Text(
                                            text = "Urgencia ${adv.urgencyLevel.ifBlank { "MEDIA" }}",
                                            color = if (adv.urgencyLevel == "ALTA") Color(0xFFFCA5A5) else accentCyan,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(adv.reasonAdvice.ifBlank { "Ahorro programado flexible." }, color = textSecondary, fontSize = 11.5.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        val monthlyFormatted = try { moneyFormatter.format(adv.recommendedMonthlyAmount) } catch (_: Exception) { "0" }
                                        val dailyFormatted = try { moneyFormatter.format(adv.recommendedDailyMicroSaving) } catch (_: Exception) { "0" }
                                        Text("Aporte mensual: $ $monthlyFormatted", color = accentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("Micro-ahorro: $ $dailyFormatted/día", color = Color(0xFF00E676), fontSize = 11.5.sp)
                                    }
                                    if (matchingGoal != null) {
                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    viewModel.openContributionDialog(matchingGoal)
                                                } catch (e: Exception) {
                                                    android.util.Log.e("ObjetivosScreen", "Error opening contribution dialog", e)
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, accentCyan),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentCyan),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.heightIn(min = 40.dp)
                                        ) {
                                            Text("Aportar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Progreso de Hitos (25%, 50%, 75%, 100%)",
                        color = textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val milestoneList = state.milestoneMetrics.orEmpty()
                if (milestoneList.isEmpty()) {
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
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Sin hitos registrados para objetivos activos",
                                    color = textSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    // Milestone Metrics with unique composite key and error handling
                    itemsIndexed(milestoneList, key = { idx, m -> "ms_${m.goalId.ifEmpty { "id" }}_$idx" }) { idx, m ->
                        val matchingGoal = state.allGoals.firstOrNull { g -> g.id == m.goalId }
                        val velocityColor = when (m.velocityStatus) {
                            "ADELANTADO" -> Color(0xFF00E676)
                            "REZAGADO" -> Color(0xFFEF4444)
                            "COMPLETADO" -> Color(0xFF38BDF8)
                            else -> Color(0xFFFBBF24)
                        }

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        matchingGoal?.let { viewModel.openGoalDetail(it) }
                                    } catch (e: Exception) {
                                        android.util.Log.e("ObjetivosScreen", "Error opening goal detail from milestone", e)
                                    }
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(m.goalName.ifBlank { "Objetivo" }, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "Ritmo: ${m.velocityStatus.ifBlank { "EN_RITMO" }} • ${m.daysElapsed} días transcurridos",
                                            color = velocityColor,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    val formattedPct = try {
                                        String.format(java.util.Locale.US, "%.1f", m.progressPercentage)
                                    } catch (e: Exception) {
                                        "0.0"
                                    }
                                    Text("$formattedPct%", color = accentCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    (m.milestones.orEmpty()).forEach { ms ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (ms.isAchieved) Color(0xFF00C853).copy(alpha = 0.2f) else Color(0xFF061120),
                                            border = BorderStroke(1.dp, if (ms.isAchieved) Color(0xFF00E676) else Color(0xFF1E3A8A)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("${ms.percentage}%", color = if (ms.isAchieved) Color(0xFF00E676) else textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Text(if (ms.isAchieved) "Logrado" else "Pendiente", color = if (ms.isAchieved) Color(0xFF00E676) else textSecondary, fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
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
private fun EmptyGoalsCard(onAddGoal: () -> Unit) {
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
                Icon(
                    imageVector = Icons.Default.Savings,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Aún no tienes objetivos registrados",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Crea una meta y compromete aportes para protegerlos contra gastos.",
                color = Color(0xFF8EA5C4),
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddGoal,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Crear mi primer objetivo", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CommittedFundsHeaderCard(
    totalCommitted: Double,
    totalLiquidity: Double,
    ratio: Double,
    committedGoalsCount: Int,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF2563EB).copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Fondos Comprometidos", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("$committedGoalsCount objetivos con aportes protegidos", color = Color(0xFF8EA5C4), fontSize = 11.5.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF00C853).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "${String.format("%.1f", ratio)}% del saldo",
                        color = Color(0xFF00E676),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF061120),
                border = BorderStroke(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "TOTAL APORTES COMPROMETIDOS",
                        color = Color(0xFF38BDF8),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isPrivacyMode) "$ ••••••••" else "$ ${moneyFormatter.format(totalCommitted)}",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Solo los aportes efectivamente realizados quedan comprometidos. Todo sigue visible en el Dashboard.",
                        color = Color(0xFF8EA5C4),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
