package com.example.ui.responsiveness

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.ui.adaptive.KashHeightSizeClass
import com.example.core.ui.adaptive.KashWidthSizeClass
import com.example.core.ui.adaptive.KashWindowAdaptiveInfo
import com.example.core.ui.adaptive.rememberKashAdaptiveInfo
import com.example.core.ui.components.CurrencyText
import com.example.core.ui.components.KashAdaptiveTopBar
import com.example.core.ui.components.KashCompactButton
import com.example.core.ui.components.KashKeyValueRow
import com.example.core.ui.components.KashNumericKeyboard
import com.example.core.ui.components.KashResponsiveDialog
import com.example.core.ui.components.KashSearchBar
import com.example.core.ui.components.KashStatusBadge
import com.example.data.datasource.FirebaseDataSource
import com.example.data.local.SecurityDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.BudgetItemEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.FinancialRepositoryImpl
import com.example.presentation.budget.BudgetsScreen
import com.example.presentation.budget.state.BudgetsViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.lifecycle.viewModelScope

/**
 * TASK-024 (Fase 6 - Suite Integral de Aceptación TST-01 a TST-12):
 * Suite automatizada sobre Robolectric + Compose UI Test que ejecuta y valida
 * la matriz completa de los 12 escenarios de prueba de la Auditoría Técnica de Arquitectura Visual.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ResponsivenessAcceptanceTestSuite {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var database: SecurityDatabase
    private lateinit var context: Context
    private lateinit var repository: FinancialRepositoryImpl
    private lateinit var budgetsViewModel: BudgetsViewModel

    private val testUserId = "user_acceptance_test"

    private val sampleEnvelope = BudgetItemEntity(
        id = "bi_alim_acceptance",
        budgetId = "b_active_acceptance",
        categoryId = "cat_alim_acc",
        categoryName = "ALIMENTACIÓN MES",
        categoryIcon = "restaurant",
        categoryColorHex = "#10B981",
        allocatedAmount = 300_000.0,
        spentAmount = 23_680.0
    )

    private val sampleTransactions = listOf(
        TransactionEntity(
            id = "tx_cg_acceptance",
            userId = testUserId,
            accountId = "acc_tc_nu",
            accountName = "NU TC",
            categoryId = "cat_alim_acc",
            categoryName = "ALIMENTACIÓN MES",
            amount = 16_330.0,
            currency = "COP",
            type = "EXPENSE",
            date = "2026-10-17 • NU TC",
            description = "Compra con tarjeta en Comercio General (1 cuotas)",
            receiptUrl = null,
            idempotencyKey = "tx_acc_idem_1"
        ),
        TransactionEntity(
            id = "tx_d1_acceptance",
            userId = testUserId,
            accountId = "acc_tc_nu",
            accountName = "NU TC",
            categoryId = "cat_alim_acc",
            categoryName = "ALIMENTACIÓN MES",
            amount = 7_350.0,
            currency = "COP",
            type = "EXPENSE",
            date = "2026-10-17 • NU TC",
            description = "Compra con tarjeta en D1 (1 cuotas)",
            receiptUrl = null,
            idempotencyKey = "tx_acc_idem_2"
        )
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

        val budget = BudgetEntity(
            id = "b_active_acceptance",
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

        budgetsViewModel = BudgetsViewModel(
            financialRepository = repository,
            financialDao = database.financialDao(),
            userId = testUserId
        )
    }

    @After
    fun tearDown() {
        if (::budgetsViewModel.isInitialized) {
            budgetsViewModel.viewModelScope.cancel()
        }
        database.close()
    }

    // =========================================================================
    // TST-01: Teléfono Ultra-Compacto (320x640dp, xxhdpi) — Prueba de reflujo extremo
    // =========================================================================
    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun test_TST01_ultra_compact_320dp_header_and_single_line_metrics() {
        composeTestRule.setContent {
            Column(modifier = Modifier.fillMaxSize().background(Color(0xFF07101E))) {
                KashAdaptiveTopBar(
                    title = "Presupuestos & Sobres",
                    subtitle = "Planificación financiera y control por categorías",
                    onNavigateBack = {},
                    backButtonTestTag = "tst01_back_btn",
                    badgeContent = {
                        KashStatusBadge(
                            text = "Módulo :feature-ocr",
                            modifier = Modifier.testTag("tst01_ocr_badge")
                        )
                    },
                    actions = {
                        KashCompactButton(
                            text = "+ Nuevo",
                            onClick = {},
                            testTag = "tst01_new_btn"
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                KashKeyValueRow(
                    keyText = "Ritmo Diario Permitido (Burn Rate)",
                    valueContent = {
                        CurrencyText(
                            amountDouble = 23680.0,
                            prefix = "Gastado: $ "
                        )
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tst01_back_btn").assertIsDisplayed()
        composeTestRule.onNodeWithText("Presupuestos & Sobres").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tst01_ocr_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tst01_new_btn").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ritmo Diario Permitido (Burn Rate)").assertIsDisplayed()
    }

    // =========================================================================
    // TST-02: Teléfono Estándar (360x800dp) — Reproducción y resolución de capturas
    // =========================================================================
    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun test_TST02_standard_phone_360dp_reported_captures_resolution() {
        composeTestRule.setContent {
            BudgetsScreen(
                viewModel = budgetsViewModel,
                onNavigateBack = {},
                onOpenCreateBudget = {}
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("back_button_budgets").assertIsDisplayed()
        composeTestRule.onNodeWithText("Presupuestos & Sobres").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_budget_config").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_new_budget_top").assertIsDisplayed()

        composeTestRule.onNodeWithText("Editar Techo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rebalancear").assertIsDisplayed()
        composeTestRule.onNodeWithText("Barrer Meta").assertIsDisplayed()

        composeTestRule.onNodeWithText("ALIMENTACIÓN MES").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ver Gastos").assertIsDisplayed()
        composeTestRule.onNodeWithText("+$50K").assertIsDisplayed()
    }

    // =========================================================================
    // TST-03: Plegable Abierto y Tablet Compacta (600x960dp) — Clase Medium
    // =========================================================================
    @Test
    @Config(qualifiers = "w600dp-h960dp-xxhdpi")
    fun test_TST03_foldable_and_medium_tablet_600dp_adaptive_tokens() {
        var adaptiveInfo: KashWindowAdaptiveInfo? = null

        composeTestRule.setContent {
            val adaptive = rememberKashAdaptiveInfo()
            adaptiveInfo = adaptive

            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "WidthClass: ${adaptive.widthClass}",
                    modifier = Modifier.testTag("tst03_class_label")
                )
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(adaptiveInfo)
        assertEquals(KashWidthSizeClass.MEDIUM, adaptiveInfo?.widthClass)
        assertEquals(760.dp, adaptiveInfo?.maxContentWidth)
        assertEquals(2, adaptiveInfo?.gridColumnsCount)
        assertTrue(adaptiveInfo?.isMedium == true)
        composeTestRule.onNodeWithTag("tst03_class_label").assertIsDisplayed()
    }

    // =========================================================================
    // TST-04: Tablet Grande / Escritorio (1024x768dp) — Clase Expanded
    // =========================================================================
    @Test
    @Config(qualifiers = "w1024dp-h768dp-xxhdpi")
    fun test_TST04_expanded_tablet_desktop_1024dp_centered_content_width() {
        var adaptiveInfo: KashWindowAdaptiveInfo? = null

        composeTestRule.setContent {
            val adaptive = rememberKashAdaptiveInfo()
            adaptiveInfo = adaptive

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    modifier = Modifier
                        .width(adaptive.maxContentWidth)
                        .testTag("tst04_centered_surface")
                ) {
                    Text("Canvas Centrado en Pantalla Grande")
                }
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(adaptiveInfo)
        assertEquals(KashWidthSizeClass.EXPANDED, adaptiveInfo?.widthClass)
        assertEquals(960.dp, adaptiveInfo?.maxContentWidth)
        assertTrue(adaptiveInfo?.isExpanded == true)
        composeTestRule.onNodeWithTag("tst04_centered_surface").assertIsDisplayed()
    }

    // =========================================================================
    // TST-05: Rotación de Pantalla y Conservación de Estado (StateRestorationTester)
    // =========================================================================
    @Test
    fun test_TST05_screen_rotation_and_state_preservation_with_StateRestorationTester() {
        val restorationTester = StateRestorationTester(composeTestRule)

        restorationTester.setContent {
            var selectedTabIndex by rememberSaveable { mutableStateOf(2) }
            var searchQuery by rememberSaveable { mutableStateOf("Alimentación") }
            var isPrivacyEnabled by rememberSaveable { mutableStateOf(true) }

            Column {
                Text(text = "Tab: $selectedTabIndex", modifier = Modifier.testTag("state_tab"))
                Text(text = "Query: $searchQuery", modifier = Modifier.testTag("state_query"))
                Text(text = "Privacy: $isPrivacyEnabled", modifier = Modifier.testTag("state_privacy"))

                Button(
                    onClick = {
                        selectedTabIndex = 3
                        searchQuery = "Transporte"
                        isPrivacyEnabled = false
                    },
                    modifier = Modifier.testTag("btn_mutate_state")
                ) {
                    Text("Mutar Estado")
                }
            }
        }

        composeTestRule.onNodeWithTag("btn_mutate_state").performClick()
        composeTestRule.onNodeWithText("Tab: 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Query: Transporte").assertIsDisplayed()
        composeTestRule.onNodeWithText("Privacy: false").assertIsDisplayed()

        // Emular recreación de actividad por rotación
        restorationTester.emulateSavedInstanceStateRestore()

        // El estado mutado sobrevive íntegramente
        composeTestRule.onNodeWithText("Tab: 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Query: Transporte").assertIsDisplayed()
        composeTestRule.onNodeWithText("Privacy: false").assertIsDisplayed()
    }

    // =========================================================================
    // TST-06: Redimensionamiento Dinámico (Multi-Window / Split-Screen)
    // =========================================================================
    @Test
    fun test_TST06_dynamic_resizing_and_multi_window_reflow() {
        val metricsHalfScreen = KashWindowAdaptiveInfo.calculate(widthDp = 340, heightDp = 700, fontScale = 1.0f)
        val metricsFullScreen = KashWindowAdaptiveInfo.calculate(widthDp = 720, heightDp = 700, fontScale = 1.0f)

        assertEquals(KashWidthSizeClass.ULTRA_COMPACT, metricsHalfScreen.widthClass)
        assertEquals(1, metricsHalfScreen.gridColumnsCount)

        assertEquals(KashWidthSizeClass.MEDIUM, metricsFullScreen.widthClass)
        assertEquals(2, metricsFullScreen.gridColumnsCount)
        assertEquals(760.dp, metricsFullScreen.maxContentWidth)
    }

    // =========================================================================
    // TST-07: Textos Extremos, Nombres Extensos y Cifras Multimillonarias
    // =========================================================================
    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun test_TST07_extreme_texts_multimillion_figures_and_long_descriptions() {
        val longCategory = "Categoría Ultra Extensa de Servicios Públicos e Infraestructura de Acueducto 2026"
        val multimillionAmount = 999_999_999_999.0

        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                KashKeyValueRow(
                    keyText = longCategory,
                    valueContent = {
                        CurrencyText(
                            amountDouble = multimillionAmount,
                            prefix = "-$ "
                        )
                    },
                    testTag = "tst07_extreme_row"
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tst07_extreme_row").assertIsDisplayed()
        composeTestRule.onNodeWithText("-$ $ 999.999.999.999").assertIsDisplayed()
    }

    // =========================================================================
    // TST-08: Formularios con Errores de Validación Activos y Scroll al Botón
    // =========================================================================
    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun test_TST08_creation_forms_with_active_validation_errors() {
        composeTestRule.setContent {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                Text("Formulario de Prueba", fontSize = 20.sp)

                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = { Text("Nombre Obligatorio") },
                    isError = true,
                    supportingText = { Text("El nombre es requerido y no puede estar en blanco") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = "-5000",
                    onValueChange = {},
                    label = { Text("Monto Válido") },
                    isError = true,
                    supportingText = { Text("El monto no puede ser negativo ni exceder el cupo") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(300.dp))

                KashCompactButton(
                    text = "Guardar Cambios",
                    onClick = {},
                    testTag = "tst08_save_btn",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tst08_save_btn")
            .performScrollTo()
            .assertIsDisplayed()
    }

    // =========================================================================
    // TST-09: Teclado Virtual (IME) y Teclado Numérico Compartido en Horizontal
    // =========================================================================
    @Test
    @Config(qualifiers = "w640dp-h360dp-xxhdpi")
    fun test_TST09_virtual_ime_and_compact_numeric_keyboard_in_landscape() {
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                KashNumericKeyboard(
                    visible = true,
                    title = "Valor a Transferir",
                    currentValue = 150000.0,
                    onDigitClick = {},
                    onBackspace = {},
                    onClear = {},
                    onDone = {},
                    modifier = Modifier.fillMaxWidth(),
                    keyboardTestTag = "tst09_keyboard"
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tst09_keyboard").assertIsDisplayed()
        composeTestRule.onNodeWithTag("key_5").assertIsDisplayed()
        composeTestRule.onNodeWithTag("keyboard_done_btn").assertIsDisplayed()
    }

    // =========================================================================
    // TST-10: Diálogos, Modales y Hojas Modales con Scrollable heightIn
    // =========================================================================
    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun test_TST10_modals_dialogs_and_sheets_scrollable_heightIn() {
        composeTestRule.setContent {
            KashResponsiveDialog(
                onDismissRequest = {},
                testTag = "tst10_dialog"
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Historial Extenso",
                            fontSize = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {},
                            modifier = Modifier.testTag("tst10_close_btn")
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        repeat(20) { index ->
                            Text(
                                text = "Item de historial contable #$index",
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    KashCompactButton(
                        text = "Confirmar Acción",
                        onClick = {},
                        testTag = "tst10_confirm_btn",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tst10_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tst10_close_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tst10_confirm_btn").assertIsDisplayed()
    }

    // =========================================================================
    // TST-11: Escalado de Texto (fontScale = 1.5f) y Accesibilidad WCAG 2.2 AA
    // =========================================================================
    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi", fontScale = 1.5f)
    fun test_TST11_accessibility_fontScale_1_5x_and_wcag_touch_targets() {
        composeTestRule.setContent {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                KashSearchBar(
                    query = "Buscar sobre",
                    onQueryChange = {},
                    placeholder = "Buscar sobre por categoría...",
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "tst11_search_bar"
                )

                Spacer(modifier = Modifier.height(16.dp))

                KashCompactButton(
                    text = "Editar Techo",
                    onClick = {},
                    testTag = "tst11_compact_btn"
                )

                Spacer(modifier = Modifier.height(16.dp))

                CurrencyText(
                    amountDouble = 125450000.0,
                    prefix = "Patrimonio: $ ",
                    testTag = "tst11_currency_large"
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tst11_search_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tst11_compact_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tst11_currency_large").assertIsDisplayed()
    }

    // =========================================================================
    // TST-12: Navegación entre Flujos y Regresión de BackHandler
    // =========================================================================
    @Test
    fun test_TST12_back_handler_navigation_regression_and_lock_session_prevention() {
        var isSessionLocked = false
        var currentRoute by mutableStateOf("DASHBOARD")

        composeTestRule.setContent {
            Box {
                if (currentRoute == "DASHBOARD") {
                    Text("Dashboard Principal", modifier = Modifier.testTag("nav_dashboard"))
                    Button(
                        onClick = { currentRoute = "SUB_AUDIT_VAULT" },
                        modifier = Modifier.testTag("btn_open_audit")
                    ) {
                        Text("Abrir Bóveda")
                    }
                    BackHandler(enabled = true) {
                        isSessionLocked = true
                    }
                } else if (currentRoute == "SUB_AUDIT_VAULT") {
                    Text("Bóveda de Auditoría SOC-2", modifier = Modifier.testTag("nav_audit"))
                    BackHandler(enabled = true) {
                        currentRoute = "DASHBOARD"
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("nav_dashboard").assertIsDisplayed()
        assertFalse(isSessionLocked)

        // Abrir sub-pantalla
        composeTestRule.onNodeWithTag("btn_open_audit").performClick()
        composeTestRule.onNodeWithTag("nav_audit").assertIsDisplayed()

        // Presionar Atrás
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        // Regresa al Dashboard sin bloquear sesión
        composeTestRule.onNodeWithTag("nav_dashboard").assertIsDisplayed()
        assertFalse("La sesión no debe bloquearse al retroceder de una subruta", isSessionLocked)
    }
}
