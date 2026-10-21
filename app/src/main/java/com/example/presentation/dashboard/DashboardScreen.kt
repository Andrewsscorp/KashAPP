package com.example.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.KashAppApplication
import com.example.core.ui.adaptive.rememberKashAdaptiveInfo
import com.example.data.local.entity.AccountEntity
import com.example.features.settings.presentation.SettingsScreen
import com.example.features.settings.presentation.SettingsViewModel
import com.example.presentation.account.CreateAccountScreen
import com.example.presentation.account.components.SelectAccountTypeBottomSheet
import com.example.presentation.budget.BudgetsScreen
import com.example.presentation.budget.CreateBudgetScreen
import com.example.presentation.budget.state.BudgetFormViewModel
import com.example.presentation.credit_cards.AddCreditCardScreen
import com.example.presentation.credit_cards.CreateCardPurchaseScreen
import com.example.presentation.credit_cards.state.CreditCardFormViewModel
import com.example.presentation.dashboard.components.accounts.MisCuentasBancariasCard
import com.example.presentation.dashboard.components.activity.UltimosMovimientosCard
import com.example.presentation.dashboard.components.ai.PreguntaKashAiCard
import com.example.presentation.dashboard.components.alerts.AlertasYNotificacionesCard
import com.example.presentation.dashboard.components.bottombar.DashboardBottomNavBar
import com.example.presentation.dashboard.components.calendar.ProximosPagosCard
import com.example.presentation.dashboard.components.cards.TarjetasDeCreditoCard
import com.example.presentation.dashboard.components.charts.FlujoDeCajaCard
import com.example.presentation.dashboard.components.charts.GastosPorCategoriaCard
import com.example.presentation.dashboard.components.dialogs.QuickAddModal
import com.example.presentation.dashboard.components.goals.MisObjetivosCard
import com.example.presentation.dashboard.components.health.TuSaludFinancieraCard
import com.example.presentation.dashboard.components.position.TuPosicionGrid
import com.example.presentation.dashboard.components.pulse.ElPulsoDeTuMesCard
import com.example.presentation.dashboard.components.topbar.DashboardHeroHeader
import com.example.presentation.dashboard.detail.FinancialRubroDetailScreen
import com.example.presentation.dashboard.components.dialogs.TransactionDetailsBottomSheet
import com.example.presentation.dashboard.components.dialogs.ScheduledPaymentDetailsBottomSheet
import com.example.domain.models.FinancialRubroType
import com.example.presentation.dashboard.model.DashboardEvent
import com.example.presentation.expense.CreateExpenseScreen
import com.example.presentation.goals.CreateGoalScreen
import com.example.presentation.goals.state.GoalFormViewModel
import com.example.presentation.income.CreateIncomeScreen
import com.example.presentation.trm.CurrencyTrmScreen
import com.example.presentation.trm.CurrencyTrmViewModel
import com.example.presentation.trm.CurrencyTrmViewModelFactory
import com.example.presentation.transfer.CreateTransferScreen
import com.example.presentation.fixedassets.FixedAssetsScreen
import com.example.presentation.fixedassets.FixedAssetsViewModel

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.presentation.dashboard.components.drawer.DashboardDrawerContent
import com.example.feature.capturaautomatica.ui.AutoCaptureViewModel
import com.example.feature.capturaautomatica.ui.components.AutoCaptureInboxBanner
import com.example.feature.capturaautomatica.ui.components.AutoCaptureInboxSheet
import com.example.feature.capturaautomatica.ui.components.AutoCaptureSettingsSheet
import com.example.feature.capturaautomatica.ui.components.QuickCashEntryDialog
import com.example.feature.capturaautomatica.service.AutoCaptureNotificationHelper
import com.example.feature.capturaautomatica.service.QuickCashTileService

/**
 * Main Enterprise Dashboard Screen for KashApp.
 * Implements:
 * - Parte 1: Hero Header
 * - Parte 2: Tu Posición & Tu Salud Financiera
 * - Parte 3: El Pulso de tu mes, Flujo de caja & Gastos por categoría
 * - Continuación Imagen 1: Mis cuentas bancarias & ACTIVIDAD (Últimos movimientos)
 * - Continuación Imagen 2: CALENDARIO (Próximos pagos), Mis objetivos, Tarjetas de crédito & Alertas y notificaciones
 * - Continuación Imagen 3: Pregunta a KashAI (Asistente RAG interactivo)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onLogoutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val dashContext = LocalContext.current
    val appContainer = (dashContext.applicationContext as KashAppApplication).appContainer
    val adaptive = rememberKashAdaptiveInfo()

    // Navigation and sub-flow dialog states (persisted across orientation and process recreation)
    var showQuickAddModal by rememberSaveable { mutableStateOf(false) }
    var showCreateExpense by rememberSaveable { mutableStateOf(false) }
    var showCreateIncome by rememberSaveable { mutableStateOf(false) }
    var showCreateTransfer by rememberSaveable { mutableStateOf(false) }
    var showSelectAccountTypeModal by rememberSaveable { mutableStateOf(false) }
    var showBankAccountsManagementScreen by rememberSaveable { mutableStateOf(false) }
    var selectedAccountTypeCodeForCreation by rememberSaveable { mutableStateOf("SAVINGS") }
    var showCreateAccount by rememberSaveable { mutableStateOf(false) }
    var showCreateGoal by rememberSaveable { mutableStateOf(false) }
    var showCreateCreditCard by rememberSaveable { mutableStateOf(false) }
    var showCreateCardPurchase by rememberSaveable { mutableStateOf(false) }
    var showCreateBudget by rememberSaveable { mutableStateOf(false) }
    var showBudgetsModule by rememberSaveable { mutableStateOf(false) }
    var showCurrencyTrmScreen by rememberSaveable { mutableStateOf(false) }
    var showFixedAssetsModule by rememberSaveable { mutableStateOf(false) }
    var showThirdPartiesModule by rememberSaveable { mutableStateOf(false) }
    var showReceiptScannerScreen by rememberSaveable { mutableStateOf(false) }
    var showInformesModule by rememberSaveable { mutableStateOf(false) }
    var showObjetivosModule by rememberSaveable { mutableStateOf(false) }
    var showUpcomingPaymentsModule by rememberSaveable { mutableStateOf(false) }
    var showSharedExpensesModule by rememberSaveable { mutableStateOf(false) }
    var showCreateSharedExpenseScreen by rememberSaveable { mutableStateOf(false) }
    var showSharedExpensesSettings by rememberSaveable { mutableStateOf(false) }
    var activeFinancialRubroName by rememberSaveable { mutableStateOf<String?>(null) }
    val activeFinancialRubro = remember(activeFinancialRubroName) {
        activeFinancialRubroName?.let { name ->
            runCatching { FinancialRubroType.valueOf(name) }.getOrNull()
        }
    }
    var currentBottomNavRoute by rememberSaveable { mutableStateOf("kashapp://dashboard") }
    var showAuditVault by rememberSaveable { mutableStateOf(false) }
    var selectedTxIdForModal by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedTxForModal = remember(selectedTxIdForModal, uiState.recentTransactions) {
        selectedTxIdForModal?.let { id -> uiState.recentTransactions.find { it.id.toString() == id } }
    }
    var selectedPaymentIdForModal by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedPaymentForModal = remember(selectedPaymentIdForModal, uiState.upcomingPayments) {
        selectedPaymentIdForModal?.let { id -> uiState.upcomingPayments.find { it.id.toString() == id } }
    }

    val currentActivity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
    val autoCaptureViewModel: AutoCaptureViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = AutoCaptureViewModel.Factory(
            repository = appContainer.autoCaptureRepository,
            userId = viewModel.userId.ifBlank { "user_default" }
        )
    )
    val pendingAutoCaptures by autoCaptureViewModel.pendingCaptures.collectAsState()
    val pendingAutoCapturesCount by autoCaptureViewModel.pendingCount.collectAsState()
    var showAutoCaptureInbox by rememberSaveable { mutableStateOf(false) }
    var showAutoCaptureSettings by rememberSaveable { mutableStateOf(false) }
    var showQuickCashEntry by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (currentActivity?.intent?.getBooleanExtra("open_upcoming_payments", false) == true) {
            showUpcomingPaymentsModule = true
            currentActivity.intent?.removeExtra("open_upcoming_payments")
        }
        if (currentActivity?.intent?.getBooleanExtra(QuickCashTileService.EXTRA_OPEN_QUICK_CASH, false) == true) {
            showQuickCashEntry = true
            currentActivity.intent?.removeExtra(QuickCashTileService.EXTRA_OPEN_QUICK_CASH)
        }
        if (currentActivity?.intent?.action == AutoCaptureNotificationHelper.ACTION_OPEN_INBOX) {
            showAutoCaptureInbox = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DashboardDrawerContent(
                uiState = uiState,
                onCloseDrawer = {
                    coroutineScope.launch { drawerState.close() }
                },
                onNavigateHome = {
                    coroutineScope.launch {
                        drawerState.close()
                        scrollState.animateScrollTo(0)
                    }
                },
                onOpenAccounts = {
                    showBankAccountsManagementScreen = true
                },
                onOpenCreateExpense = {
                    showCreateExpense = true
                },
                onOpenCreateIncome = {
                    showCreateIncome = true
                },
                onOpenCreateTransfer = {
                    showCreateTransfer = true
                },
                onOpenCardPurchase = {
                    showCreateCardPurchase = true
                },
                onOpenBudgets = {
                    showBudgetsModule = true
                },
                onOpenGoals = {
                    showObjetivosModule = true
                },
                onOpenCreditCards = {
                    showCreateCreditCard = true
                },
                onOpenCurrencyTrm = {
                    showCurrencyTrmScreen = true
                },
                onOpenFixedAssets = {
                    showFixedAssetsModule = true
                },
                onOpenThirdParties = {
                    showThirdPartiesModule = true
                },
                onOpenReceiptScanner = {
                    showReceiptScannerScreen = true
                },
                onOpenInformes = {
                    showInformesModule = true
                },
                onOpenUpcomingPayments = {
                    showUpcomingPaymentsModule = true
                },
                onOpenSharedExpenses = {
                    showSharedExpensesModule = true
                },
                onOpenAutoCapture = {
                    showAutoCaptureSettings = true
                },
                onOpenQuickCash = {
                    showQuickCashEntry = true
                },
                onOpenAuditVault = {
                    showAuditVault = true
                },
                onOpenSettings = {
                    viewModel.onEvent(DashboardEvent.OpenSettingsClick)
                },
                onLogoutClick = {
                    onLogoutClick()
                }
            )
        }
    ) {
        Scaffold(
            bottomBar = {
                DashboardBottomNavBar(
                    currentRoute = currentBottomNavRoute,
                    onNavigate = { route ->
                        try {
                            currentBottomNavRoute = route
                            if (route.startsWith("kashapp://finances")) {
                                showInformesModule = true
                            } else if (route.startsWith("kashapp://settings")) {
                                viewModel.onEvent(DashboardEvent.OpenSettingsClick)
                            } else if (route.startsWith("kashapp://planning/objetivos")) {
                                showObjetivosModule = true
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("DashboardScreen", "Error navigating to route: $route", e)
                        }
                    },
                    onFabClick = { showQuickAddModal = true }
                )
            },
            containerColor = Color(0xFFF8FAFC),
            modifier = modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
                    .verticalScroll(scrollState)
            ) {
                // ==========================================
                // PARTE 1: HERO HEADER (image.png)
                // ==========================================
                DashboardHeroHeader(
                    userName = uiState.userName,
                    periodState = uiState.period,
                    totalNetWorth = uiState.netWorth.totalNetWorth,
                    isPrivacyMode = uiState.isPrivacyMode,
                    displayCurrency = uiState.displayCurrency,
                    isConvertedToUsd = uiState.isConvertedToUsd,
                    usdExchangeRate = uiState.usdExchangeRate,
                    onToggleUsdConversion = { viewModel.onEvent(DashboardEvent.ToggleUsdConversion) },
                    onOpenTrmRateClick = { showCurrencyTrmScreen = true },
                    onPeriodSelected = { viewModel.onEvent(DashboardEvent.ChangePeriod(it)) },
                    onMenuClick = {
                        coroutineScope.launch { drawerState.open() }
                    },
                    onNotificationClick = {
                        viewModel.onEvent(DashboardEvent.OpenSettingsClick)
                    },
                    onSyncClick = {
                        viewModel.onEvent(DashboardEvent.Refresh)
                    },
                    onProfileClick = {
                        viewModel.onEvent(DashboardEvent.OpenSettingsClick)
                    },
                    onExpenseClick = { showCreateExpense = true },
                    onIncomeClick = { showCreateIncome = true },
                    onTransferClick = { showCreateTransfer = true },
                    onCardPaymentClick = { showCreateExpense = true },
                    onCardPurchaseClick = { showCreateCardPurchase = true },
                    onCreateAccountClick = { showSelectAccountTypeModal = true },
                    onGoalClick = { showCreateGoal = true },
                    onBudgetClick = { showBudgetsModule = true },
                    onAddClick = { showQuickAddModal = true },
                    onScanClick = { showCreateExpense = true },
                    onMoreClick = { showQuickAddModal = true }
                )

            // ==========================================
            // DASHBOARD BODY (Parte 2, Parte 3 y Continuación de Imágenes)
            // ==========================================
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = androidx.compose.ui.Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = adaptive.maxContentWidth)
                        .padding(vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {

                // ==========================================
                // CAPTURA AUTOMÁTICA: BANNER DE MOVIMIENTOS DETECTADOS
                // ==========================================
                AutoCaptureInboxBanner(
                    pendingCount = pendingAutoCapturesCount,
                    onClickReview = { showAutoCaptureInbox = true }
                )

                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // PARTE 2: TU POSICIÓN — Así está tu dinero (2x2 Grid con apertura a análisis completo)
                    TuPosicionGrid(
                        netWorthState = uiState.netWorth,
                        isPrivacyMode = uiState.isPrivacyMode,
                        currencyCode = uiState.displayCurrency,
                        onViewDetailClick = { showInformesModule = true },
                        onCashClick = { activeFinancialRubroName = FinancialRubroType.AVAILABLE_CASH.name },
                        onInvestmentsClick = { activeFinancialRubroName = FinancialRubroType.INVESTMENTS.name },
                        onDebtsClick = { activeFinancialRubroName = FinancialRubroType.DEBTS.name },
                        onAssetsClick = { showFixedAssetsModule = true }
                    )
                }

                // PARTE 2: TU SALUD FINANCIERA (Gauge Donut 66/100)
                TuSaludFinancieraCard(
                    healthScore = uiState.healthScore,
                    onViewRecommendationsClick = {
                        viewModel.onEvent(DashboardEvent.SubmitAiQuery("¿Cómo puedo mejorar mi salud financiera?"))
                    }
                )

                // PARTE 3: SEPTIEMBRE / OCTUBRE — El pulso de tu mes (Ingresos, Gastos, Ahorro, Presupuesto)
                ElPulsoDeTuMesCard(
                    currentMonth = uiState.period.currentMonth,
                    kpiState = uiState.kpi,
                    isPrivacyMode = uiState.isPrivacyMode,
                    onBudgetClick = { showBudgetsModule = true }
                )

                // PARTE 3: Flujo de caja (Dark Blue Translucent Card with Dual Bézier Wave lines)
                FlujoDeCajaCard(
                    cashFlowEntries = uiState.cashFlowEntries,
                    savingsAmount = uiState.kpi.savings,
                    isPrivacyMode = uiState.isPrivacyMode
                )

                // PARTE 3: Gastos por categoría (Donut Ring Breakdown)
                GastosPorCategoriaCard(
                    categorySpends = uiState.categorySpends,
                    totalExpense = uiState.kpi.expenses,
                    isPrivacyMode = uiState.isPrivacyMode
                )

                // ==========================================
                // CONTINUACIÓN IMAGEN 1: Mis cuentas bancarias & ACTIVIDAD
                // ==========================================
                MisCuentasBancariasCard(
                    accounts = uiState.accounts,
                    onAccountClick = { viewModel.onEvent(DashboardEvent.SelectAccount(it)) },
                    onTransferClick = { showCreateTransfer = true },
                    onMoreAccountOptions = { viewModel.onEvent(DashboardEvent.SelectAccount(it)) },
                    onToggleAllowNegative = { accId, allowNeg ->
                        viewModel.onEvent(DashboardEvent.ToggleAccountAllowNegative(accId, allowNeg))
                    },
                    isPrivacyMode = uiState.isPrivacyMode
                )

                UltimosMovimientosCard(
                    transactions = uiState.recentTransactions,
                    onTransactionClick = { txId ->
                        selectedTxIdForModal = txId.toString()
                    },
                    onSeeAllClick = { showInformesModule = true },
                    isPrivacyMode = uiState.isPrivacyMode
                )

                // ==========================================
                // CONTINUACIÓN IMAGEN 2: Calendario, Objetivos, Tarjetas & Alertas
                // ==========================================
                ProximosPagosCard(
                    payments = uiState.upcomingPayments,
                    onCalendarIconClick = { showUpcomingPaymentsModule = true },
                    onPaymentClick = { payment -> selectedPaymentIdForModal = payment.id.toString() },
                    isPrivacyMode = uiState.isPrivacyMode
                )

                MisObjetivosCard(
                    goals = uiState.goals,
                    onAddGoalClick = { showCreateGoal = true },
                    onSeeAllClick = { showObjetivosModule = true },
                    isPrivacyMode = uiState.isPrivacyMode
                )

                TarjetasDeCreditoCard(
                    creditCards = uiState.creditCards,
                    onAddCreditCardClick = { showCreateCreditCard = true },
                    onManageCardsClick = { showCreateCreditCard = true },
                    onRecordPurchaseClick = { showCreateCardPurchase = true },
                    isPrivacyMode = uiState.isPrivacyMode
                )

                AlertasYNotificacionesCard(
                    insights = uiState.insights,
                    onCardClick = {
                        viewModel.onEvent(DashboardEvent.SubmitAiQuery("Explícame mis alertas financieras actuales"))
                    }
                )

                // ==========================================
                // CONTINUACIÓN IMAGEN 3: Pregunta a KashAI
                // ==========================================
                PreguntaKashAiCard(
                    aiState = uiState.aiPrompt,
                    onInputChange = { viewModel.onEvent(DashboardEvent.UpdateAiInput(it)) },
                    onSubmitQuery = { viewModel.onEvent(DashboardEvent.SubmitAiQuery(it)) }
                )

                Spacer(modifier = Modifier.height(72.dp))
            }
        }
        }
    }
    }

    // Modal: Quick Add Options Sheet
    if (showQuickAddModal) {
        QuickAddModal(
            onDismiss = { showQuickAddModal = false },
            onOpenCreateExpense = {
                showQuickAddModal = false
                showCreateExpense = true
            },
            onOpenCreateIncome = {
                showQuickAddModal = false
                showCreateIncome = true
            },
            onOpenCreateTransfer = {
                showQuickAddModal = false
                showCreateTransfer = true
            },
            onOpenCreateCardPurchase = {
                showQuickAddModal = false
                showCreateCardPurchase = true
            },
            onOpenSelectAccountType = {
                showQuickAddModal = false
                showSelectAccountTypeModal = true
            },
            onOpenCreateCreditCard = {
                showQuickAddModal = false
                showCreateCreditCard = true
            },
            onOpenCreateGoal = {
                showQuickAddModal = false
                showCreateGoal = true
            },
            onOpenCreateBudget = {
                showQuickAddModal = false
                showCreateBudget = true
            },
            onOpenReceiptScanner = {
                showQuickAddModal = false
                showReceiptScannerScreen = true
            },
            onOpenSharedExpenses = {
                showQuickAddModal = false
                showSharedExpensesModule = true
            },
            onAddTransaction = { _, _, _, _, _ ->
                showQuickAddModal = false
                viewModel.onEvent(DashboardEvent.Refresh)
            }
        )
    }

    // Modal: Settings / Control Plane
    if (uiState.isSettingsOpen) {
        val context = LocalContext.current
        val appContainer = (context.applicationContext as KashAppApplication).appContainer
        val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
            factory = SettingsViewModel.Factory(
                repository = appContainer.settingsRepository,
                authRepository = appContainer.authRepository,
                financialRepository = appContainer.financialRepository,
                userId = viewModel.userId
            )
        )
        SettingsScreen(
            viewModel = settingsViewModel,
            onNavigateBack = { viewModel.onEvent(DashboardEvent.CloseSettingsClick) },
            onNavigateToHome = { viewModel.onEvent(DashboardEvent.CloseSettingsClick) },
            onNavigateToTransactions = { viewModel.onEvent(DashboardEvent.CloseSettingsClick) },
            onNavigateToSummary = { viewModel.onEvent(DashboardEvent.CloseSettingsClick) },
            onFabAddClick = {
                viewModel.onEvent(DashboardEvent.CloseSettingsClick)
                showQuickAddModal = true
            },
            onLogoutSuccess = {
                viewModel.onEvent(DashboardEvent.CloseSettingsClick)
                onLogoutClick()
            },
            onAccountDeletedSuccess = {
                viewModel.onEvent(DashboardEvent.CloseSettingsClick)
                onLogoutClick()
            }
        )
    }

    // Flow: Create Expense
    if (showCreateExpense) {
        CreateExpenseScreen(
            onDismiss = { showCreateExpense = false },
            onSaveExpense = { _, _, _, _, _, _, _, _, _, _, _, _ ->
                showCreateExpense = false
                viewModel.onEvent(DashboardEvent.Refresh)
            },
            financialRepository = viewModel.financialRepository,
            userId = viewModel.userId,
            onOpenSharedExpenseSplitter = { _ ->
                showCreateExpense = false
                showSharedExpensesModule = true
                showCreateSharedExpenseScreen = true
            }
        )
    }

    // Flow: Create Income
    if (showCreateIncome) {
        CreateIncomeScreen(
            onDismiss = { showCreateIncome = false },
            onSaveIncome = { _, _, _, _, _, _, _, _, _, _, _, _, _ ->
                showCreateIncome = false
                viewModel.onEvent(DashboardEvent.Refresh)
            },
            financialRepository = viewModel.financialRepository,
            userId = viewModel.userId
        )
    }

    // Flow: Create Transfer
    if (showCreateTransfer && viewModel.financialRepository != null) {
        CreateTransferScreen(
            onNavigateBack = { showCreateTransfer = false },
            financialRepository = viewModel.financialRepository,
            userId = viewModel.userId,
            onTransferSuccess = { _, _, _, _, _, _, _, _, _, _, _, _, _ ->
                showCreateTransfer = false
                viewModel.onEvent(DashboardEvent.Refresh)
            }
        )
    }

    // Flow: Select Account Type Bottom Sheet (Popup inicial tipo de cuenta estilo iPhone)
    if (showSelectAccountTypeModal) {
        SelectAccountTypeBottomSheet(
            onDismiss = { showSelectAccountTypeModal = false },
            onOpenDetailedCreateAccount = { typeCode ->
                showSelectAccountTypeModal = false
                if (typeCode == "CREDIT_CARD") {
                    showCreateCreditCard = true
                } else {
                    selectedAccountTypeCodeForCreation = typeCode
                    showCreateAccount = true
                }
            },
            onCreateAccount = { name, typeCode, initialBal, curr ->
                showSelectAccountTypeModal = false
                if (typeCode == "CREDIT_CARD") {
                    showCreateCreditCard = true
                } else {
                    coroutineScope.launch {
                        viewModel.financialRepository?.createAccount(
                            name = name,
                            type = typeCode,
                            balance = initialBal,
                            currency = curr,
                            userId = viewModel.userId,
                            bankName = "Bancolombia",
                            isFavorite = true
                        )
                        viewModel.onEvent(DashboardEvent.Refresh)
                    }
                }
            }
        )
    }

    // Flow: Create Account (Formulario detallado con tipo pre-seleccionado)
    if (showCreateAccount) {
        CreateAccountScreen(
            initialTypeCode = selectedAccountTypeCodeForCreation,
            onDismiss = { showCreateAccount = false },
            onAccountCreated = { _, _ ->
                showCreateAccount = false
                viewModel.onEvent(DashboardEvent.Refresh)
            },
            financialRepository = viewModel.financialRepository,
            userId = viewModel.userId
        )
    }


    // Flow: Add Credit Card
    if (showCreateCreditCard && viewModel.financialRepository != null) {
        val context = LocalContext.current
        val appContainer = (context.applicationContext as KashAppApplication).appContainer
        val ccViewModel: CreditCardFormViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
            factory = CreditCardFormViewModel.provideFactory(
                createCreditCardUseCase = appContainer.createCreditCardUseCase,
                validateCreditLimitsUseCase = appContainer.validateCreditLimitsUseCase,
                calculateNextBillingDatesUseCase = appContainer.calculateNextBillingDatesUseCase,
                financialRepository = viewModel.financialRepository
            )
        )
        AddCreditCardScreen(
            viewModel = ccViewModel,
            onNavigateBack = { showCreateCreditCard = false },
            onCreditCardCreated = {
                showCreateCreditCard = false
                viewModel.onEvent(DashboardEvent.Refresh)
            },
            userId = viewModel.userId,
            financialRepository = viewModel.financialRepository
        )
    }

    // Screen: Módulo de Presupuestos & Sobres
    if (showBudgetsModule && viewModel.financialRepository != null) {
        val budgetsViewModel: com.example.presentation.budget.state.BudgetsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
            key = "budgets_vm_${viewModel.userId}",
            factory = com.example.presentation.budget.state.BudgetsViewModel.provideFactory(
                financialRepository = viewModel.financialRepository,
                financialDao = appContainer.financialDao,
                userId = viewModel.userId
            )
        )
        androidx.activity.compose.BackHandler {
            showBudgetsModule = false
        }
        BudgetsScreen(
            viewModel = budgetsViewModel,
            onNavigateBack = {
                showBudgetsModule = false
                viewModel.onEvent(DashboardEvent.Refresh)
            },
            onOpenCreateBudget = {
                showCreateBudget = true
            },
            onOpenGoalDetail = {
                showObjetivosModule = true
            }
        )
    }

    // Flow: Create Budget
    if (showCreateBudget && viewModel.financialRepository != null) {
        val budgetViewModel: BudgetFormViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
            key = "budget_form_vm_${viewModel.userId}",
            factory = BudgetFormViewModel.provideFactory(viewModel.financialRepository, viewModel.userId)
        )
        CreateBudgetScreen(
            viewModel = budgetViewModel,
            onNavigateBack = { showCreateBudget = false },
            onBudgetCreatedSuccessfully = {
                showCreateBudget = false
                viewModel.onEvent(DashboardEvent.Refresh)
            }
        )
    }

    // Flow: Create Card Purchase (Compra con Tarjeta)
    if (showCreateCardPurchase) {
        CreateCardPurchaseScreen(
            onDismiss = { showCreateCardPurchase = false },
            onPurchaseSuccess = { _, _, _, _, _, _, _, _ ->
                showCreateCardPurchase = false
                viewModel.onEvent(DashboardEvent.Refresh)
            },
            financialRepository = viewModel.financialRepository,
            userId = viewModel.userId,
            onNavigateToAddCard = {
                showCreateCardPurchase = false
                showCreateCreditCard = true
            }
        )
    }

    // Window / Screen: Financial Rubro Complete Analysis Detail View
    if (activeFinancialRubro != null) {
        BackHandler {
            activeFinancialRubroName = null
        }
        FinancialRubroDetailScreen(
            rubroType = activeFinancialRubro,
            netWorthState = uiState.netWorth,
            financialRepository = viewModel.financialRepository,
            fixedAssetRepository = viewModel.fixedAssetRepository,
            userId = viewModel.userId,
            isPrivacyMode = uiState.isPrivacyMode,
            onOpenFixedAssetsModule = {
                activeFinancialRubroName = null
                showFixedAssetsModule = true
            },
            onDismiss = { activeFinancialRubroName = null }
        )
    }

    // Screen: Módulo de Activos Fijos (Depreciación NIIF, Proyecciones y Exportación)
    if (showFixedAssetsModule) {
        BackHandler {
            showFixedAssetsModule = false
            viewModel.onEvent(DashboardEvent.Refresh)
        }
        val context = LocalContext.current
        val appContainer = (context.applicationContext as KashAppApplication).appContainer
        val fixedAssetsViewModel: FixedAssetsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
            factory = FixedAssetsViewModel.Factory(
                fixedAssetRepository = appContainer.fixedAssetRepository,
                calculateDepreciationUseCase = appContainer.calculateAssetDepreciationUseCase,
                projectAssetValuesUseCase = appContainer.projectAssetValuesUseCase,
                userId = viewModel.userId
            )
        )
        FixedAssetsScreen(
            viewModel = fixedAssetsViewModel,
            onDismiss = {
                showFixedAssetsModule = false
                viewModel.onEvent(DashboardEvent.Refresh)
            }
        )
    }

    // Screen: Módulo Central de Terceros (Directorio Empresarial, Operaciones, Contactos y PDF)
    if (showThirdPartiesModule) {
        BackHandler {
            showThirdPartiesModule = false
            viewModel.onEvent(DashboardEvent.Refresh)
        }
        val context = LocalContext.current
        val appContainer = (context.applicationContext as KashAppApplication).appContainer
        val thirdPartyViewModel: com.example.presentation.thirdparty.ThirdPartyViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
            factory = com.example.presentation.thirdparty.ThirdPartyViewModel.Factory(
                thirdPartyRepository = appContainer.thirdPartyRepository,
                fixedAssetDao = appContainer.securityDatabase.fixedAssetDao(),
                userId = viewModel.userId
            )
        )
        com.example.presentation.thirdparty.ThirdPartiesHubScreen(
            viewModel = thirdPartyViewModel,
            onDismiss = {
                showThirdPartiesModule = false
                viewModel.onEvent(DashboardEvent.Refresh)
            }
        )
    }

    // Screen: Moneda y TRM (Frankfurter API, +200 Divisas, Conversor, Histórico, Tasa Manual y Pruebas)
    if (showCurrencyTrmScreen) {
        val context = LocalContext.current
        val appContainer = (context.applicationContext as KashAppApplication).appContainer
        val trmViewModel: CurrencyTrmViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
            factory = CurrencyTrmViewModelFactory(
                exchangeRateRepository = appContainer.exchangeRateRepository,
                settingsRepository = appContainer.settingsRepository,
                testSuite = appContainer.exchangeRateTestSuite
            )
        )
        androidx.activity.compose.BackHandler {
            showCurrencyTrmScreen = false
        }
        CurrencyTrmScreen(
            viewModel = trmViewModel,
            onBack = {
                showCurrencyTrmScreen = false
                viewModel.onEvent(DashboardEvent.Refresh)
            }
        )
    }

    // Screen: Mis Cuentas Bancarias (Gestión, Modificación, Estadísticas y Movimientos)
    if (showBankAccountsManagementScreen) {
        androidx.activity.compose.BackHandler {
            showBankAccountsManagementScreen = false
        }
        com.example.presentation.account.BankAccountsManagementScreen(
            onNavigateBack = {
                showBankAccountsManagementScreen = false
                viewModel.onEvent(DashboardEvent.Refresh)
            },
            onOpenCreateAccount = {
                showSelectAccountTypeModal = true
            },
            userId = viewModel.userId
        )
    }

    // Screen: Escáner OCR de Facturas (:feature-ocr)
    if (showReceiptScannerScreen) {
        androidx.activity.compose.BackHandler {
            showReceiptScannerScreen = false
        }
        com.example.feature.ocr.ui.ReceiptScannerScreen(
            onNavigateBack = { showReceiptScannerScreen = false },
            onApplyToExpense = { _, _, _, _ ->
                showReceiptScannerScreen = false
                showCreateExpense = true
            }
        )
    }

    // Screen: Informes Financieros Central (:feature-informes)
    if (showInformesModule) {
        val context = LocalContext.current
        val appContainer = (context.applicationContext as KashAppApplication).appContainer
        val informesViewModel: com.example.feature.informes.presentation.InformesViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
            factory = com.example.feature.informes.presentation.InformesViewModelFactory(
                repository = appContainer.financialReportsRepository,
                userId = viewModel.userId
            )
        )
        androidx.activity.compose.BackHandler {
            showInformesModule = false
        }
        com.example.feature.informes.ui.InformesScreen(
            viewModel = informesViewModel,
            onNavigateBack = {
                showInformesModule = false
                viewModel.onEvent(DashboardEvent.Refresh)
            }
        )
    }

    // Screen: Módulo de Objetivos Financieros (:feature-objetivos)
    val objetivosViewModel: com.example.feature.objetivos.presentation.ObjetivosViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.example.feature.objetivos.presentation.ObjetivosViewModelFactory(
            financialDao = appContainer.financialDao,
            userId = viewModel.userId
        )
    )

    if (showObjetivosModule) {
        androidx.activity.compose.BackHandler {
            showObjetivosModule = false
        }
        com.example.feature.objetivos.ui.ObjetivosScreen(
            viewModel = objetivosViewModel,
            onNavigateBack = {
                showObjetivosModule = false
                viewModel.onEvent(DashboardEvent.Refresh)
            },
            onOpenCreateGoalForm = {
                showCreateGoal = true
            }
        )
    }

    // Flow: Create Goal (Placed after showObjetivosModule so it renders on top when opened from either Dashboard or ObjetivosScreen)
    if (showCreateGoal && viewModel.financialRepository != null) {
        val goalViewModel: GoalFormViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
            key = "goal_form_vm_${System.currentTimeMillis() / 60000}",
            factory = GoalFormViewModel.provideFactory(
                financialRepository = viewModel.financialRepository,
                userId = viewModel.userId
            )
        )
        CreateGoalScreen(
            viewModel = goalViewModel,
            onNavigateBack = { showCreateGoal = false },
            onGoalCreatedSuccessfully = {
                showCreateGoal = false
                viewModel.onEvent(DashboardEvent.Refresh)
                objetivosViewModel.syncFromDashboard()
            }
        )
    }

    // Screen: Próximos Pagos, Notificaciones y Tarjetas de Crédito
    if (showUpcomingPaymentsModule && viewModel.financialRepository != null) {
        val paymentsViewModel: com.example.presentation.payments.UpcomingPaymentsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
            factory = com.example.presentation.payments.UpcomingPaymentsViewModel.provideFactory(
                financialRepository = viewModel.financialRepository,
                userId = viewModel.userId
            )
        )
        androidx.activity.compose.BackHandler {
            showUpcomingPaymentsModule = false
        }
        com.example.presentation.payments.UpcomingPaymentsScreen(
            viewModel = paymentsViewModel,
            onNavigateBack = {
                showUpcomingPaymentsModule = false
                viewModel.onEvent(DashboardEvent.Refresh)
            }
        )
    }

    // Screen: Módulo de Gastos Compartidos, Consecutivos y Tickets (:feature-gastos-compartidos)
    if (showSharedExpensesModule) {
        val sharedVm: com.example.feature.gastoscompartidos.presentation.SharedExpensesViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
            key = "shared_expenses_vm_${viewModel.userId}",
            factory = com.example.feature.gastoscompartidos.presentation.SharedExpensesViewModel.provideFactory(
                aiEngine = appContainer.sharedExpenseFirebaseAiEngine,
                sharedExpenseDao = appContainer.sharedExpenseDao,
                financialDao = appContainer.financialDao,
                userId = viewModel.userId
            )
        )
        val sharedState by sharedVm.uiState.collectAsState()
        var showOcrItemizedModal by rememberSaveable { mutableStateOf(false) }

        androidx.activity.compose.BackHandler {
            when {
                showSharedExpensesSettings -> showSharedExpensesSettings = false
                showCreateSharedExpenseScreen -> showCreateSharedExpenseScreen = false
                else -> showSharedExpensesModule = false
            }
        }

        when {
            showSharedExpensesSettings -> {
                com.example.feature.gastoscompartidos.ui.settings.SharedExpensesSettingsScreen(
                    config = sharedState.settingsConfig,
                    apiProtectionStatus = sharedState.apiProtectionStatus,
                    onUpdateConfig = { newCfg ->
                        sharedVm.onIntent(
                            com.example.feature.gastoscompartidos.presentation.SharedExpensesIntent.UpdateSettingsConfig(newCfg)
                        )
                    },
                    onNavigateBack = { showSharedExpensesSettings = false }
                )
            }
            showCreateSharedExpenseScreen -> {
                com.example.feature.gastoscompartidos.ui.create.CreateSharedExpenseScreen(
                    uiState = sharedState,
                    onIntent = { sharedVm.onIntent(it) },
                    onOpenOcrItemizedModal = {
                        showOcrItemizedModal = true
                    },
                    onSaveAndEmitTicket = {
                        showCreateSharedExpenseScreen = false
                        viewModel.onEvent(DashboardEvent.Refresh)
                    },
                    onDismiss = {
                        showCreateSharedExpenseScreen = false
                    }
                )
            }
            else -> {
                com.example.feature.gastoscompartidos.ui.SharedExpensesHubScreen(
                    uiState = sharedState,
                    onIntent = { sharedVm.onIntent(it) },
                    onNavigateBack = {
                        showSharedExpensesModule = false
                        viewModel.onEvent(DashboardEvent.Refresh)
                    },
                    onOpenCreateSharedExpense = { showCreateSharedExpenseScreen = true },
                    onOpenOcrScanner = { showReceiptScannerScreen = true },
                    onOpenSettleModal = { participantId ->
                        sharedVm.onIntent(
                            com.example.feature.gastoscompartidos.presentation.SharedExpensesIntent.OpenSettleDebtModal(participantId)
                        )
                    },
                    onOpenSettings = { showSharedExpensesSettings = true }
                )
            }
        }

        com.example.feature.gastoscompartidos.ui.ocr.OcrItemizedSplitModal(
            visible = showOcrItemizedModal,
            merchantName = sharedState.merchantNameInput.ifBlank { "Factura Escaneada" },
            externalInvoiceNumber = sharedState.manualConsecutiveOverride.takeIf { it.isNotBlank() },
            currency = sharedState.currencyCode,
            lineItems = sharedState.ocrLineItemsInput,
            participants = sharedState.participants,
            onToggleParticipantOnItem = { itemId, participantId ->
                sharedVm.onIntent(
                    com.example.feature.gastoscompartidos.presentation.SharedExpensesIntent.AssignOcrItemConsumer(itemId, participantId)
                )
            },
            onAssignAllParticipantsOnItem = { itemId ->
                sharedVm.onIntent(
                    com.example.feature.gastoscompartidos.presentation.SharedExpensesIntent.AssignOcrItemToAllConsumers(itemId)
                )
            },
            onExpandMultiUnitItem = { itemId ->
                sharedVm.onIntent(
                    com.example.feature.gastoscompartidos.presentation.SharedExpensesIntent.ExpandMultiUnitOcrItem(itemId)
                )
            },
            onConfirmItemizedSplit = {
                showOcrItemizedModal = false
            },
            onDismiss = {
                showOcrItemizedModal = false
            }
        )

        val settleParticipant = sharedState.participants.find { it.id == sharedState.activeSettleParticipantId }
        val settleBalance = sharedState.participantBalances.find { it.participant.id == sharedState.activeSettleParticipantId }?.netBalanceWithUser
            ?: (sharedState.totalReceivableNiif - sharedState.totalPayableNiif)

        com.example.feature.gastoscompartidos.ui.settle.SettleSharedDebtBottomSheet(
            visible = settleParticipant != null,
            participant = settleParticipant,
            netBalanceWithUser = settleBalance,
            currency = sharedState.currencyCode,
            accounts = sharedState.accounts,
            onConfirmSettlement = { amount, method, treatment, accountId, refCode ->
                val partId = settleParticipant?.id ?: return@SettleSharedDebtBottomSheet
                sharedVm.onIntent(
                    com.example.feature.gastoscompartidos.presentation.SharedExpensesIntent.ConfirmDebtSettlement(
                        participantId = partId,
                        amount = amount,
                        method = method,
                        treatment = treatment,
                        kashAccountId = accountId,
                        referenceCode = refCode
                    )
                )
            },
            onDismiss = {
                sharedVm.onIntent(com.example.feature.gastoscompartidos.presentation.SharedExpensesIntent.DismissSettleDebtModal)
            }
        )

        com.example.feature.gastoscompartidos.ui.ticket.SharedReceiptTicketDialog(
            ticket = sharedState.activeDigitalTicketToView,
            onShareWhatsAppPng = { ticketModel ->
                val exported = appContainer.sharedTicketExporter.exportAndEncryptTicket(ticketModel, viewModel.userId)
                val msg = "Ticket KashApp ${ticketModel.primaryTicketConsecutive} • ${ticketModel.headlineTitle} (${ticketModel.grandTotalFormatted})\n${ticketModel.verificationFooterText}"
                val shareIntent = appContainer.sharedTicketExporter.createShareTicketIntent(
                    artifactFile = exported.pngFile,
                    mimeType = "image/png",
                    accompanyingMessage = msg
                )
                dashContext.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir Ticket ${ticketModel.primaryTicketConsecutive}"))
            },
            onExportPdf = { ticketModel ->
                val exported = appContainer.sharedTicketExporter.exportAndEncryptTicket(ticketModel, viewModel.userId)
                val msg = "Ticket KashApp ${ticketModel.primaryTicketConsecutive} • ${ticketModel.headlineTitle} (${ticketModel.grandTotalFormatted})\n${ticketModel.verificationFooterText}"
                val shareIntent = appContainer.sharedTicketExporter.createShareTicketIntent(
                    artifactFile = exported.pdfFile,
                    mimeType = "application/pdf",
                    accompanyingMessage = msg
                )
                dashContext.startActivity(android.content.Intent.createChooser(shareIntent, "Exportar PDF ${ticketModel.primaryTicketConsecutive}"))
            },
            onDismiss = {
                sharedVm.onIntent(com.example.feature.gastoscompartidos.presentation.SharedExpensesIntent.DismissDigitalTicketPreview)
            }
        )

        com.example.feature.gastoscompartidos.ui.directory.CreateOrEditParticipantBottomSheet(
            visible = sharedState.isParticipantEditorVisible,
            initialParticipant = sharedState.editingParticipant,
            onPickFromPhoneContacts = {},
            onAttachCustomQrImage = {},
            onSaveParticipant = { participant ->
                sharedVm.onIntent(
                    com.example.feature.gastoscompartidos.presentation.SharedExpensesIntent.UpsertParticipantInDirectory(
                        participant
                    )
                )
            },
            onDismiss = {
                sharedVm.onIntent(com.example.feature.gastoscompartidos.presentation.SharedExpensesIntent.DismissParticipantEditor)
            }
        )
    }

    if (selectedTxForModal != null) {
        TransactionDetailsBottomSheet(
            transaction = selectedTxForModal,
            onDismiss = { selectedTxIdForModal = null },
            onDelete = {
                selectedTxIdForModal = null
                viewModel.onEvent(DashboardEvent.Refresh)
            }
        )
    }

    if (selectedPaymentForModal != null) {
        ScheduledPaymentDetailsBottomSheet(
            payment = selectedPaymentForModal,
            onDismiss = { selectedPaymentIdForModal = null },
            onOpenUpcomingPayments = {
                selectedPaymentIdForModal = null
                showUpcomingPaymentsModule = true
            }
        )
    }

    if (showAuditVault) {
        BackHandler {
            showAuditVault = false
        }
        val auditViewModel: com.example.presentation.audit.AuditViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
            factory = com.example.presentation.audit.AuditViewModelFactory(
                auditSecurityLogDao = appContainer.securityDatabase.auditSecurityLogDao(),
                assetAuditLogDao = appContainer.securityDatabase.assetAuditLogDao(),
                financialDao = appContainer.securityDatabase.financialDao(),
                fixedAssetDao = appContainer.securityDatabase.fixedAssetDao(),
                userId = uiState.userName.ifBlank { "user_default" }
            )
        )
        com.example.presentation.audit.AuditModuleScreen(
            viewModel = auditViewModel,
            onNavigateBack = { showAuditVault = false }
        )
    }

    val roomCategories by appContainer.financialDao.getCategoriesFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val roomTags by appContainer.financialDao.getTagsFlow().collectAsStateWithLifecycle(initialValue = emptyList())

    if (showAutoCaptureInbox) {
        val accountOptions = remember(uiState.accounts) {
            uiState.accounts.map { acc ->
                com.example.feature.capturaautomatica.domain.model.AutoCaptureAccountOption(
                    id = acc.rawId.ifBlank { acc.id.toString() },
                    name = acc.name,
                    bankName = acc.bankName,
                    balance = acc.balance.toDouble(),
                    type = acc.type.name,
                    colorHex = acc.colorHex
                )
            }
        }
        val categoryOptions = remember(roomCategories) {
            roomCategories.filter { it.isActive }.map { cat ->
                com.example.feature.capturaautomatica.domain.model.AutoCaptureCategoryOption(
                    id = cat.id,
                    name = cat.name,
                    type = cat.type,
                    colorHex = cat.colorHex,
                    icon = cat.icon
                )
            }
        }
        val tagOptions = remember(roomTags) {
            roomTags.map { tag ->
                com.example.feature.capturaautomatica.domain.model.AutoCaptureTagOption(
                    id = tag.id,
                    name = tag.name,
                    colorHex = tag.colorHex
                )
            }
        }
        AutoCaptureInboxSheet(
            pendingCaptures = pendingAutoCaptures,
            availableAccounts = accountOptions,
            availableCategories = categoryOptions,
            availableTags = tagOptions,
            onConfirmCapture = { captureId, selectedAccountId, selectedAccountName, selectedCategoryId, selectedCategoryName, selectedTags ->
                autoCaptureViewModel.confirmCapture(
                    captureId = captureId,
                    customAccountId = selectedAccountId,
                    customAccountName = selectedAccountName,
                    customCategoryId = selectedCategoryId,
                    customCategoryName = selectedCategoryName,
                    customTags = selectedTags
                )
                viewModel.onEvent(DashboardEvent.Refresh)
            },
            onDraftChange = { captureId, accountId, accountName, categoryId, categoryName ->
                autoCaptureViewModel.updateDraftSelection(
                    captureId = captureId,
                    accountId = accountId,
                    accountName = accountName,
                    categoryId = categoryId,
                    categoryName = categoryName
                )
            },
            onDismissCapture = { captureId ->
                autoCaptureViewModel.dismissCapture(captureId)
            },
            onDismissRequest = { showAutoCaptureInbox = false }
        )
    }

    if (showAutoCaptureSettings) {
        AutoCaptureSettingsSheet(
            onSimulate = { bankName ->
                autoCaptureViewModel.simulateCapture(bankName)
            },
            onDismissRequest = { showAutoCaptureSettings = false }
        )
    }

    if (showQuickCashEntry) {
        QuickCashEntryDialog(
            onDismissRequest = { showQuickCashEntry = false },
            onSaveCashExpense = { amount, desc, catName ->
                showQuickCashEntry = false
                coroutineScope.launch {
                    val currentUserId = viewModel.userId.ifBlank { "user_default" }
                    val accounts = appContainer.financialDao.getAccounts(currentUserId)
                    val cashAccount = accounts.find { it.type == "CASH" || it.name.contains("Efectivo", ignoreCase = true) }
                        ?: accounts.firstOrNull()

                    val categories = appContainer.financialDao.getCategories()
                    val matchedCat = categories.find { it.name.contains(catName, ignoreCase = true) }
                        ?: categories.firstOrNull()

                    if (cashAccount != null) {
                        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date())
                        appContainer.recordExpenseUseCase(
                            userId = currentUserId,
                            accountId = cashAccount.id,
                            accountName = cashAccount.name,
                            categoryId = matchedCat?.id ?: "cat_default",
                            categoryName = matchedCat?.name ?: catName,
                            amount = amount,
                            currency = cashAccount.currency,
                            dateFormatted = dateStr,
                            description = desc,
                            receiptUrl = null,
                            isRecurring = false,
                            saveAsTemplate = false,
                            tags = listOf("Efectivo", "QuickCash"),
                            idempotencyKey = "cash_${java.util.UUID.randomUUID().toString().take(12)}"
                        )
                        viewModel.onEvent(DashboardEvent.Refresh)
                    }
                }
            }
        )
    }
}
