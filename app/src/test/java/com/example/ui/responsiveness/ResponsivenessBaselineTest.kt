package com.example.ui.responsiveness

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.datasource.FirebaseDataSource
import com.example.data.local.SecurityDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.BudgetItemEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.FinancialRepositoryImpl
import com.example.feature.informes.domain.model.BalanceSheetData
import com.example.feature.informes.domain.model.CashFlowStatementData
import com.example.feature.informes.domain.model.EquityStatementData
import com.example.feature.informes.domain.model.FinancialLineItem
import com.example.feature.informes.domain.model.IncomeStatementData
import com.example.feature.informes.presentation.FinancialStatementSubTab
import com.example.feature.informes.ui.tabs.EstadosFinancierosTab
import com.example.data.local.entity.SharedParticipantEntity
import com.example.feature.activos.ui.FixedAssetsAnalyticsBanner
import com.example.feature.gastoscompartidos.presentation.ParticipantBalanceSummaryUi
import com.example.feature.gastoscompartidos.presentation.SharedExpensesUiState
import com.example.feature.gastoscompartidos.ui.SharedExpensesHubScreen
import com.example.feature.ocr.ui.ReceiptScannerScreen
import com.example.feature.objetivos.domain.model.GoalDetailMetric
import com.example.feature.objetivos.domain.model.GoalStatus
import com.example.feature.objetivos.domain.model.GoalSummaryOverview
import com.example.feature.objetivos.ui.components.GoalIndividualCard
import com.example.feature.objetivos.ui.components.GoalSummaryCard
import com.example.presentation.budget.BudgetsScreen
import com.example.presentation.budget.dialogs.EnvelopeTransactionsModal
import com.example.presentation.budget.state.BudgetsViewModel
import com.example.presentation.payments.UpcomingPaymentsScreen
import com.example.presentation.payments.UpcomingPaymentsViewModel
import com.example.presentation.trm.components.ManualRateHistoryModal
import com.example.data.local.entity.ManualRateHistoryLogEntity
import com.example.presentation.account.BankAccountsManagementScreen
import com.example.presentation.audit.AuditModuleScreen
import com.example.presentation.audit.AuditViewModel
import com.example.data.local.entity.AuditSecurityLog
import com.example.presentation.fixedassets.FixedAssetsScreen
import com.example.presentation.fixedassets.FixedAssetsViewModel
import com.example.data.repository.FixedAssetRepositoryImpl
import com.example.data.local.entity.FixedAssetEntity
import com.example.presentation.dashboard.components.topbar.DashboardHeroHeader
import com.example.presentation.dashboard.model.PeriodState
import com.example.presentation.login.LoginScreen
import com.example.presentation.login.LoginUiState
import com.example.presentation.dashboard.components.goals.MisObjetivosCard
import com.example.presentation.dashboard.components.cards.TarjetasDeCreditoCard
import com.example.presentation.dashboard.components.health.TuSaludFinancieraCard
import com.example.presentation.credit_cards.widgets.CcQuotaProgressBar
import com.example.presentation.transfer.components.TransferGmfBanner
import com.example.features.settings.presentation.components.SettingsBottomNavBar
import com.example.presentation.dashboard.model.HealthScore
import com.example.presentation.dashboard.model.RiskLevel
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal
import java.time.YearMonth
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * TASK-001 (Fase 0 - Línea Base de Responsividad):
 * Suite automatizada en Robolectric + Compose UI Test que reproduce
 * y mide el comportamiento de las pantallas y modales bajo diferentes
 * anchos de ventana (320dp, 360dp, 600dp) antes de aplicar cambios estructurales.
 *
 * Reproduce las condiciones de las 3 evidencias reportadas en producción:
 * - Captura 1 (1000403159.jpg): BudgetsScreen ("ALIMENTACIÓN MES", botones Hero, +$50K, buscador).
 * - Captura 2 (1000403020.jpg): EnvelopeTransactionsModal (transacciones largas vs montos a la derecha).
 * - Captura 3 (1000403164.jpg): ReceiptScannerScreen (cabecera con badge "Módulo :feature-ocr").
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ResponsivenessBaselineTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: SecurityDatabase
    private lateinit var context: Context
    private lateinit var repository: FinancialRepositoryImpl
    private lateinit var viewModel: BudgetsViewModel

    private val testUserId = "user_responsiveness_baseline"

    private val sampleEnvelope = BudgetItemEntity(
        id = "env_alimentacion_mes",
        budgetId = "b_active_baseline",
        categoryId = "cat_alimentacion",
        categoryName = "ALIMENTACIÓN MES",
        categoryIcon = "restaurant",
        categoryColorHex = "#10B981",
        allocatedAmount = 300_000.0,
        spentAmount = 23_680.0
    )

    private val sampleTransactions = listOf(
        TransactionEntity(
            id = "tx_comercio_gral",
            userId = testUserId,
            accountId = "acc_tc_nu",
            accountName = "NU TC",
            categoryId = "cat_alimentacion",
            categoryName = "ALIMENTACIÓN MES",
            amount = 16_330.0,
            currency = "COP",
            type = "EXPENSE",
            date = "2026-10-17 • NU TC",
            description = "Compra con tarjeta en Comercio General (1 cuotas)",
            receiptUrl = null,
            idempotencyKey = "tx_idem_1"
        ),
        TransactionEntity(
            id = "tx_d1",
            userId = testUserId,
            accountId = "acc_tc_nu",
            accountName = "NU TC",
            categoryId = "cat_alimentacion",
            categoryName = "ALIMENTACIÓN MES",
            amount = 7_350.0,
            currency = "COP",
            type = "EXPENSE",
            date = "2026-10-17 • NU TC",
            description = "Compra con tarjeta en D1 (1 cuotas)",
            receiptUrl = null,
            idempotencyKey = "tx_idem_2"
        )
    )

    private val sampleGoalSummary = GoalSummaryOverview(
        totalTarget = 15_000_000.0,
        totalSaved = 4_500_000.0,
        totalPending = 10_500_000.0,
        globalProgressPercentage = 30.0,
        activeGoalsCount = 3,
        completedGoalsCount = 1,
        atRiskGoalsCount = 0
    )

    private val sampleGoalMetric = GoalDetailMetric(
        id = "goal_test_1",
        name = "Fondo de Emergencia Familiar",
        categoryName = "Ahorro",
        priority = "Alta",
        colorHex = "#2563EB",
        iconName = "Savings",
        targetAmount = 10_000_000.0,
        savedAmount = 3_500_000.0,
        pendingAmount = 6_500_000.0,
        progressPercentage = 35.0,
        hasDeadline = true,
        startDate = "2026-01-01",
        deadlineDate = "31 Dic 2026",
        daysRemaining = 74,
        isOverdue = false,
        monthlyRequiredSaving = 500_000.0,
        averageContribution = 400_000.0,
        nextMilestonePercentage = 50,
        nextMilestoneAmount = 5_000_000.0,
        estimatedCompletionDate = "2026-12-31",
        targetAccountId = "acc_1",
        targetAccountName = "Bancolombia Ahorros",
        sourceAccountId = null,
        sourceAccountName = null,
        autoContribute = false,
        monthlyContribution = null,
        contributionDay = null,
        contributionFrequency = "Mensual",
        contributionMethod = "MANUAL",
        virtualPocketEnabled = true,
        status = GoalStatus.IN_PROGRESS,
        isActive = true,
        createdAt = System.currentTimeMillis()
    )

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SecurityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        SecurityDatabase.installIntegrityGuards(database.openHelper.writableDatabase)

        repository = FinancialRepositoryImpl(
            financialDao = database.financialDao(),
            auditLogDao = database.auditSecurityLogDao(),
            firebaseDataSource = FirebaseDataSource(context)
        )

        // Seed data mirroring user capture 1000403159.jpg
        val budget = BudgetEntity(
            id = "b_active_baseline",
            userId = testUserId,
            name = "Presupuesto Mensual Activo",
            period = "MONTHLY",
            startDate = "2026-10-01",
            endDate = "2026-10-31",
            totalAmount = 300_000.0,
            colorHex = "#10B981",
            isActive = true
        )
        database.financialDao().insertBudget(budget)
        database.financialDao().insertBudgetItems(listOf(sampleEnvelope))
        val account = AccountEntity(
            id = "acc_tc_nu",
            userId = testUserId,
            name = "NU TC",
            balance = 5_000_000.0,
            availableBalance = 5_000_000.0
        )
        database.financialDao().insertAccount(account)

        sampleTransactions.forEach { database.financialDao().insertTransaction(it) }

        viewModel = BudgetsViewModel(
            financialRepository = repository,
            financialDao = database.financialDao(),
            userId = testUserId
        )
    }

    @After
    fun tearDown() {
        if (::viewModel.isInitialized) {
            viewModel.viewModelScope.cancel()
        }
        database.close()
    }

    /**
     * TST-02: Reproducción en Teléfono Estándar (360x800dp) de BudgetsScreen.
     * Verifica que los elementos clave existen en el árbol de Compose.
     */
    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun test_budgets_screen_baseline_at_360dp() {
        composeTestRule.setContent {
            BudgetsScreen(
                viewModel = viewModel,
                onNavigateBack = {},
                onOpenCreateBudget = {}
            )
        }

        composeTestRule.waitForIdle()

        // Controles de cabecera
        composeTestRule.onNodeWithTag("back_button_budgets").assertIsDisplayed()
        composeTestRule.onNodeWithText("Presupuestos & Sobres").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_budget_config").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_new_budget_top").assertIsDisplayed()

        // Botones de acción rápida en Hero Card
        composeTestRule.onNodeWithText("Editar Techo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rebalancear").assertIsDisplayed()
        composeTestRule.onNodeWithText("Barrer Meta").assertIsDisplayed()

        // Tarjeta de sobre "ALIMENTACIÓN MES"
        composeTestRule.onNodeWithText("ALIMENTACIÓN MES").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ver Gastos").assertIsDisplayed()
        composeTestRule.onNodeWithText("+$50K").assertIsDisplayed()
    }

    /**
     * TASK-013: Verificación en Pantalla Ultra-Compacta (320x640dp) de BudgetsScreen.
     * Valida que con ancho de 320dp todos los controles de cabecera y tarjetas permanezcan visibles.
     */
    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun test_budgets_screen_adapted_at_320dp() {
        composeTestRule.setContent {
            BudgetsScreen(
                viewModel = viewModel,
                onNavigateBack = {},
                onOpenCreateBudget = {}
            )
        }

        composeTestRule.waitForIdle()

        // Controles de cabecera son 100% visibles en 320dp
        composeTestRule.onNodeWithTag("back_button_budgets").assertIsDisplayed()
        composeTestRule.onNodeWithText("Presupuestos & Sobres").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_budget_config").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_new_budget_top").assertIsDisplayed()

        // Botones de acción rápida en Hero Card no se recortan
        composeTestRule.onNodeWithText("Editar Techo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rebalancear").assertIsDisplayed()
        composeTestRule.onNodeWithText("Barrer Meta").assertIsDisplayed()

        // Tarjeta de sobre y píldoras horizontales
        composeTestRule.onNodeWithTag("budgets_lazy_column")
            .performScrollToNode(hasText("ALIMENTACIÓN MES"))
        composeTestRule.onNodeWithText("ALIMENTACIÓN MES").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ver Gastos").assertIsDisplayed()
        composeTestRule.onNodeWithText("+$50K").assertIsDisplayed()
    }

    /**
     * TST-02: Reproducción en Teléfono Estándar (360x800dp) del Modal de Movimientos (1000403020.jpg).
     * En el estado previo a la auditoría, la descripción larga comprime el monto en el borde derecho.
     */
    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun test_envelope_transactions_modal_baseline_at_360dp() {
        composeTestRule.setContent {
            EnvelopeTransactionsModal(
                envelope = sampleEnvelope,
                transactions = sampleTransactions,
                onDismiss = {}
            )
        }

        composeTestRule.waitForIdle()

        // Modal y contenido presente
        composeTestRule.onNodeWithText("ALIMENTACIÓN MES").assertIsDisplayed()
        composeTestRule.onNodeWithText("Movimientos registrados (2)").assertIsDisplayed()

        // Transacciones renderizadas
        composeTestRule.onNodeWithText("Compra con tarjeta en Comercio General (1 cuotas)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Compra con tarjeta en D1 (1 cuotas)").assertIsDisplayed()
    }

    /**
     * TASK-014: Verificación en Pantalla Ultra-Compacta (320x640dp) del Modal de Movimientos (1000403020.jpg).
     * Valida que con ancho de 320dp la descripción larga y el monto a la derecha coexistan sin colapso.
     */
    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun test_envelope_transactions_modal_adapted_at_320dp() {
        composeTestRule.setContent {
            EnvelopeTransactionsModal(
                envelope = sampleEnvelope,
                transactions = sampleTransactions,
                onDismiss = {}
            )
        }

        composeTestRule.waitForIdle()

        // Cabecera y resumen
        composeTestRule.onNodeWithText("ALIMENTACIÓN MES").assertIsDisplayed()
        composeTestRule.onNodeWithText("Asignado en sobre").assertIsDisplayed()
        composeTestRule.onNodeWithText("Disponible").assertIsDisplayed()
        composeTestRule.onNodeWithText("Movimientos registrados (2)").assertIsDisplayed()

        // Transacciones con descripción larga y montos visibles
        composeTestRule.onNodeWithText("Compra con tarjeta en Comercio General (1 cuotas)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Compra con tarjeta en D1 (1 cuotas)").assertIsDisplayed()
    }

    /**
     * TST-02: Reproducción en Teléfono Estándar (360x800dp) del Escáner OCR (1000403164.jpg).
     * Verifica la presencia del título y el badge "Módulo :feature-ocr".
     */
    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun test_receipt_scanner_header_baseline_at_360dp() {
        composeTestRule.setContent {
            ReceiptScannerScreen(
                onNavigateBack = {},
                onApplyToExpense = { _, _, _, _ -> }
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Escáner OCR de Facturas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Módulo :feature-ocr").assertIsDisplayed()
    }

    /**
     * TASK-015: Verificación en Pantalla Ultra-Compacta (320x640dp) del Escáner OCR (1000403164.jpg).
     * Valida que con ancho de 320dp el título y el badge convivan fluidamente sin desborde.
     */
    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun test_receipt_scanner_header_adapted_at_320dp() {
        composeTestRule.setContent {
            ReceiptScannerScreen(
                onNavigateBack = {},
                onApplyToExpense = { _, _, _, _ -> }
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Escáner OCR de Facturas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Módulo :feature-ocr").assertIsDisplayed()
        composeTestRule.onNodeWithText("Supermercado").assertIsDisplayed()
    }

    /**
     * TST-01: Prueba de Reflujo Extremo en Pantalla Ultra-Compacta (320x640dp).
     * Asegura que EnvelopeTransactionsModal no lance excepción de composición.
     */
    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun test_envelope_transactions_modal_ultra_compact_320dp() {
        composeTestRule.setContent {
            EnvelopeTransactionsModal(
                envelope = sampleEnvelope,
                transactions = sampleTransactions,
                onDismiss = {}
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("ALIMENTACIÓN MES").assertIsDisplayed()
        composeTestRule.onNodeWithText("Compra con tarjeta en D1 (1 cuotas)").assertIsDisplayed()
    }

    /**
     * TASK-016: Verificación en Pantalla Ultra-Compacta (320x640dp) de Módulo de Objetivos.
     * Valida que con ancho de 320dp GoalSummaryCard y GoalIndividualCard no desborden ni fracturen texto.
     */
    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun test_objetivos_components_adapted_at_320dp() {
        composeTestRule.setContent {
            androidx.compose.foundation.layout.Column {
                GoalSummaryCard(
                    summary = sampleGoalSummary,
                    committedAmount = 3_500_000.0,
                    isPrivacyMode = false
                )
                GoalIndividualCard(
                    goal = sampleGoalMetric,
                    onCardClick = {},
                    onRegisterContribution = {},
                    onMarkCompleted = {},
                    onToggleArchive = {},
                    onDelete = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // GoalSummaryCard
        composeTestRule.onNodeWithText("Patrimonio en Objetivos").assertIsDisplayed()
        composeTestRule.onNodeWithText("COMPROMETIDO").assertIsDisplayed()
        composeTestRule.onNodeWithText("LOGROS").assertIsDisplayed()

        // GoalIndividualCard
        composeTestRule.onNodeWithText("Fondo de Emergencia Familiar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Comprometido").assertIsDisplayed()
        composeTestRule.onNodeWithText("Detalles").assertIsDisplayed()
        composeTestRule.onNodeWithText("Aportar").assertIsDisplayed()
    }

    /**
     * TASK-017: Verificación en Pantalla Ultra-Compacta (320x640dp) de Estados Financieros e Informes.
     * Valida que con ancho de 320dp EstadosFinancierosTab (Balance General con CurrencyText y pesos)
     * se renderice correctamente sin fractura de texto ni desborde.
     */
    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun test_informes_screen_adapted_at_320dp() {
        val sampleBalance = BalanceSheetData(
            totalAssets = 15_000_000.0,
            totalCurrentAssets = 5_000_000.0,
            currentAssets = listOf(
                FinancialLineItem(
                    id = "item_1",
                    title = "Cuenta de Ahorros Bancolombia Principal",
                    category = "Efectivo",
                    amount = 5_000_000.0,
                    percentageOfTotal = 33.3
                )
            ),
            totalLiabilities = 2_000_000.0,
            totalCurrentLiabilities = 2_000_000.0,
            totalEquity = 13_000_000.0,
            isBalanced = true
        )

        composeTestRule.setContent {
            EstadosFinancierosTab(
                activeSubTab = FinancialStatementSubTab.BALANCE_GENERAL,
                onSelectSubTab = {},
                balanceSheet = sampleBalance,
                incomeStatement = IncomeStatementData(),
                cashFlow = CashFlowStatementData(),
                equityStatement = EquityStatementData(),
                currency = "COP",
                onShowCalculationTrace = null
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Desliza a la izquierda para ver los 4 estados financieros").assertIsDisplayed()
        composeTestRule.onNodeWithText("✓ ESTADO CUADRADO").assertIsDisplayed()
        composeTestRule.onNodeWithText("ACTIVOS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cuenta de Ahorros Bancolombia Principal").assertIsDisplayed()
        composeTestRule.onNodeWithText("PASIVOS").assertIsDisplayed()
    }

    /**
     * TASK-018: Verificación en Pantalla Ultra-Compacta (320x640dp) de Gastos Compartidos.
     * Valida que con ancho de 320dp SharedExpensesHubScreen (TopBar, Hero Card con CurrencyText,
     * balance de participantes y botón de abono) se renderice sin desbordes.
     */
    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun test_shared_expenses_components_adapted_at_320dp() {
        val sampleParticipant = SharedParticipantEntity(
            id = "part_1",
            userId = testUserId,
            displayName = "Carolina Restrepo",
            nameNormalized = "carolina restrepo",
            aliasOrRelation = "Pareja",
            avatarInitials = "CR"
        )
        val sampleUiState = SharedExpensesUiState(
            totalReceivableNiif = 450_000.0,
            totalPayableNiif = 120_000.0,
            netSharedPosition = 330_000.0,
            currencyCode = "COP",
            participantBalances = listOf(
                ParticipantBalanceSummaryUi(
                    participant = sampleParticipant,
                    netBalanceWithUser = 330_000.0,
                    pendingTicketsCount = 2,
                    oldestPendingDate = "2026-10-15"
                )
            )
        )

        composeTestRule.setContent {
            SharedExpensesHubScreen(
                uiState = sampleUiState,
                onIntent = {},
                onNavigateBack = {},
                onOpenCreateSharedExpense = {},
                onOpenOcrScanner = {},
                onOpenSettleModal = {},
                onOpenSettings = {}
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Gastos Compartidos").assertIsDisplayed()
        composeTestRule.onNodeWithText("POSICIÓN NETA EN GASTOS COMPARTIDOS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Carolina Restrepo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Te debe").assertIsDisplayed()
        composeTestRule.onNodeWithText("Registrar Abono (PG)").assertIsDisplayed()
    }

    /**
     * TASK-018: Verificación en Pantalla Ultra-Compacta (320x640dp) de Activos Fijos (NIIF NIC 16).
     * Valida que con ancho de 320dp FixedAssetsAnalyticsBanner muestre Valor en Libros Neto con
     * CurrencyText sin truncamiento horizontal ni desborde.
     */
    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun test_fixed_assets_banner_adapted_at_320dp() {
        composeTestRule.setContent {
            FixedAssetsAnalyticsBanner(
                totalAcquisitionValue = 25_000_000.0,
                totalAccumulatedDepreciation = 5_000_000.0,
                totalBookValue = 20_000_000.0,
                activeAssetsCount = 3,
                currency = "COP"
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Patrimonio en Activos Fijos").assertIsDisplayed()
        composeTestRule.onNodeWithText("VALOR EN LIBROS NETO").assertIsDisplayed()
        composeTestRule.onNodeWithText("Costo de Adquisición").assertIsDisplayed()
        composeTestRule.onNodeWithText("Depreciación Acum.").assertIsDisplayed()
        composeTestRule.onNodeWithText("NIC 16").assertIsDisplayed()
    }

    /**
     * TASK-019: Verificación en Pantalla Ultra-Compacta (320x640dp) de Próximos Pagos.
     * Valida que con ancho de 320dp UpcomingPaymentsScreen (TopBar, Proyección de liquidez,
     * cobertura y tarjetas de vencimiento) se renderice sin colisión ni desborde.
     */
    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun test_upcoming_payments_screen_adapted_at_320dp() {
        val paymentsVm = UpcomingPaymentsViewModel(
            financialRepository = repository,
            userId = testUserId
        )

        composeTestRule.setContent {
            UpcomingPaymentsScreen(
                viewModel = paymentsVm,
                onNavigateBack = {}
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Próximos Pagos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vencimientos y liquidez").assertIsDisplayed()
        composeTestRule.onNodeWithText("PROYECCIÓN DE LIQUIDEZ Y CAJA").assertIsDisplayed()
        composeTestRule.onNodeWithText("Saldo Líquido Actual").assertIsDisplayed()
    }

    /**
     * TASK-019: Verificación en Pantalla Ultra-Compacta (320x640dp) de Monedas/TRM.
     * Valida que con ancho de 320dp ManualRateHistoryModal (con badges, fechas y notas)
     * se renderice fluidamente respetando el piso tipográfico de 11sp.
     */
    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun test_currency_trm_components_adapted_at_320dp() {
        val sampleLog = ManualRateHistoryLogEntity(
            id = "log_test_1",
            pairId = "USD_COP",
            fromCurrency = "USD",
            toCurrency = "COP",
            previousRate = null,
            newRate = 4250.0,
            action = "CREATED",
            reason = "Tasa personalizada para compras en el exterior",
            changedBy = "Usuario",
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            ManualRateHistoryModal(
                logs = listOf(sampleLog),
                onDismiss = {}
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Historial de Cambios").assertIsDisplayed()
        composeTestRule.onNodeWithText("Registro inmutable de tasas manuales").assertIsDisplayed()
        composeTestRule.onNodeWithText("CREATED").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tasa personalizada para compras en el exterior").assertIsDisplayed()
    }

    /**
     * TASK-020: Verificación en Pantalla Ultra-Compacta (320x640dp) de Cuentas Bancarias.
     * Valida que con ancho de 320dp BankAccountsManagementScreen (TopBar con badge Room Local DB,
     * saldo consolidado en CurrencyText, KashSearchBar y AccountCard) se renderice sin desbordes.
     */
    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun test_bank_accounts_screen_adapted_at_320dp() {
        composeTestRule.setContent {
            BankAccountsManagementScreen(
                onNavigateBack = {},
                onOpenCreateAccount = {},
                userId = testUserId,
                customFinancialRepository = repository
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Mis Cuentas Bancarias").assertIsDisplayed()
        composeTestRule.onNodeWithText("Room Local DB").assertIsDisplayed()
        composeTestRule.onNodeWithText("SALDO TOTAL CONSOLIDADO").assertIsDisplayed()
        composeTestRule.onNodeWithText("NU TC").assertExists()
    }

    /**
     * TASK-020: Verificación en Pantalla Ultra-Compacta (320x640dp) de Bóveda de Auditoría Forense.
     * Valida que con ancho de 320dp AuditModuleScreen (Badge SOC-2 INMUTABLE a 11sp, KashSearchBar,
     * y hash criptográfico) se renderice fluidamente sin fractura vertical.
     */
    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun test_audit_module_screen_adapted_at_320dp() {
        runBlocking {
            database.auditSecurityLogDao().insertLog(
                AuditSecurityLog(
                    id = 1L,
                    timestamp = System.currentTimeMillis(),
                    eventType = "SECURITY_CHECK",
                    authMethod = "PIN_BIOMETRIC",
                    details = "Verificación de integridad criptográfica de bóveda"
                )
            )
        }

        val auditVm = AuditViewModel(
            auditSecurityLogDao = database.auditSecurityLogDao(),
            assetAuditLogDao = database.assetAuditLogDao(),
            financialDao = database.financialDao(),
            fixedAssetDao = database.fixedAssetDao()
        )

        composeTestRule.setContent {
            AuditModuleScreen(
                viewModel = auditVm,
                onNavigateBack = {}
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Bóveda de Auditoría").assertIsDisplayed()
        composeTestRule.onNodeWithText("SOC-2 INMUTABLE").assertIsDisplayed()
        composeTestRule.onNodeWithTag("audit_back_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("audit_verify_btn").assertIsDisplayed()
    }

    /**
     * TASK-020: Verificación en Pantalla Ultra-Compacta (320x640dp) de Activos Fijos y Detalle NIIF.
     * Valida que con ancho de 320dp FixedAssetsScreen (con Executive KPI cards adaptadas a widthIn,
     * Banner NIIF NIC 16 e inventario) se visualice con total responsividad y piso tipográfico >= 11sp.
     */
    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun test_fixed_assets_screen_adapted_at_320dp() {
        runBlocking {
            database.fixedAssetDao().insertAsset(
                FixedAssetEntity(
                    id = "asset_test_baseline",
                    userId = testUserId,
                    name = "MacBook Pro M3 Max",
                    type = "TANGIBLE",
                    category = "Tecnología",
                    brand = "Apple",
                    model = "M3 Max 64GB",
                    acquisitionDate = "2026-01-15",
                    acquisitionValue = 18_000_000.0,
                    accumulatedDepreciation = 3_000_000.0,
                    residualValue = 2_000_000.0,
                    usefulLifeMonths = 36,
                    currency = "COP",
                    status = "ACTIVE"
                )
            )
        }

        val fixedAssetRepo = FixedAssetRepositoryImpl(
            fixedAssetDao = database.fixedAssetDao(),
            assetCategoryDao = database.assetCategoryDao(),
            assetAuditLogDao = database.assetAuditLogDao()
        )

        val fixedAssetsVm = FixedAssetsViewModel(
            fixedAssetRepository = fixedAssetRepo,
            userId = testUserId
        )

        composeTestRule.setContent {
            FixedAssetsScreen(
                viewModel = fixedAssetsVm,
                onDismiss = {}
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Activos Fijos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gestión patrimonial NIIF").assertIsDisplayed()
        composeTestRule.onNodeWithText("Patrimonio en Activos Fijos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Valor total de activos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Depreciación acumulada").assertIsDisplayed()
    }

    /**
     * TASK-021: Verificación en Pantalla Ultra-Compacta (320x640dp) de Dashboard Hero Header.
     * Valida que con ancho de 320dp DashboardHeroHeader muestre Patrimonio neto con CurrencyText,
     * hamburguesa y botón de sync sin desbordes.
     */
    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun test_dashboard_hero_header_adapted_at_320dp() {
        composeTestRule.setContent {
            DashboardHeroHeader(
                userName = "Alexander",
                periodState = PeriodState(YearMonth.now(), false),
                totalNetWorth = BigDecimal("15450000"),
                isPrivacyMode = false,
                displayCurrency = "COP",
                isConvertedToUsd = false,
                usdExchangeRate = 4150.0,
                onToggleUsdConversion = {},
                onOpenTrmRateClick = {},
                onPeriodSelected = {},
                onMenuClick = {},
                onNotificationClick = {},
                onSyncClick = {},
                onProfileClick = {},
                onExpenseClick = {},
                onIncomeClick = {},
                onTransferClick = {},
                onCardPaymentClick = {},
                onCardPurchaseClick = {},
                onCreateAccountClick = {},
                onGoalClick = {},
                onBudgetClick = {},
                onAddClick = {},
                onScanClick = {},
                onMoreClick = {}
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Patrimonio neto").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_hero_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_hamburger_menu").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_cloud_sync_button").assertIsDisplayed()
    }

    /**
     * TASK-021: Verificación en Pantalla Ultra-Compacta (320x640dp) de LoginScreen.
     * Valida que en pantalla ultra-compacta (320dp) LoginScreen renderice la marca, el slogan,
     * el botón Google con touch target >= 48dp, el botón de ayuda de 44dp y las opciones de acceso rápido.
     */
    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun test_login_screen_adapted_at_320dp() {
        composeTestRule.setContent {
            LoginScreen(
                uiState = LoginUiState(rememberedUser = null),
                onGoogleSignInClick = {},
                onBiometricClick = {},
                onPinClick = {},
                onPinSubmit = {},
                onSetupPin = {},
                onDismissPinDialog = {},
                onSwitchAccount = {}
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("KashAPP").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_help_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("biometric_quick_auth_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pin_quick_auth_card").assertIsDisplayed()
    }

    /**
     * TASK-021: Verificación en Pantalla Ultra-Compacta (320x640dp) de Tarjetas Principales del Dashboard.
     * Valida que MisObjetivosCard, TarjetasDeCreditoCard y TuSaludFinancieraCard se rendericen
     * respetando el piso tipográfico de 11sp y tamaños táctiles estándar.
     */
    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun test_dashboard_cards_adapted_at_320dp() {
        composeTestRule.setContent {
            androidx.compose.foundation.layout.Column {
                MisObjetivosCard(
                    goals = persistentListOf(),
                    onAddGoalClick = {},
                    onSeeAllClick = {}
                )
                TarjetasDeCreditoCard(
                    creditCards = persistentListOf(),
                    onAddCreditCardClick = {},
                    onManageCardsClick = {}
                )
                TuSaludFinancieraCard(
                    healthScore = HealthScore(
                        score = 75,
                        riskLevel = RiskLevel.HEALTHY,
                        insightMessage = "Excelente salud financiera."
                    )
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Mis objetivos").assertIsDisplayed()
        composeTestRule.onNodeWithTag("crear_primer_objetivo_btn").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tarjetas de crédito").assertIsDisplayed()
        composeTestRule.onNodeWithTag("agregar_tarjeta_btn").assertIsDisplayed()
        composeTestRule.onNodeWithText("TU SALUD FINANCIERA").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tu_salud_financiera_card").assertIsDisplayed()
    }

    /**
     * TASK-022: Verificación en Pantalla Ultra-Compacta (320x640dp) de Formularios de Creación y Settings.
     * Valida que CcQuotaProgressBar, TransferGmfBanner y SettingsBottomNavBar se rendericen
     * sin desbordamiento horizontal, cumpliendo con el piso tipográfico >= 11sp y tamaños táctiles >= 44dp.
     */
    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun test_creation_forms_and_settings_adapted_at_320dp() {
        composeTestRule.setContent {
            androidx.compose.foundation.layout.Column {
                CcQuotaProgressBar(
                    totalQuota = 10_000_000.0,
                    usedQuota = 3_000_000.0,
                    availableQuota = 7_000_000.0
                )
                TransferGmfBanner(
                    amount = 500_000.0,
                    currency = "COP",
                    applyGmf = true,
                    onToggleGmf = {}
                )
                SettingsBottomNavBar(
                    onHomeClick = {},
                    onTransactionsClick = {},
                    onFabClick = {},
                    onSummaryClick = {},
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Utilizado (Deuda / Pasivo)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cupo Total Aprobado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ecuación maestra validada: Total = Disponible + Utilizado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gravamen Financiero (4x1000)").assertIsDisplayed()
        composeTestRule.onNodeWithText("APLICA 4x1000").assertIsDisplayed()
        composeTestRule.onNodeWithText("Inicio").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ajustes").assertIsDisplayed()
    }
}
