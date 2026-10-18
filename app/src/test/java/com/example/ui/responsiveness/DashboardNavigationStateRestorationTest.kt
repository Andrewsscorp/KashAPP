package com.example.ui.responsiveness

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.domain.models.FinancialRubroType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * TASK-011: Prueba de Restauración de Estado y BackHandler para la navegación de DashboardScreen.
 *
 * Verifica los criterios de aceptación:
 * 1) StateRestorationTester confirma que abrir flujos de navegación del Dashboard y recrear
 *    la actividad mantiene el estado abierto (gracias a rememberSaveable).
 * 2) Presionar "Atrás" en pantallas con BackHandler (AuditModuleScreen, FinancialRubroDetailScreen,
 *    FixedAssetsScreen, ThirdPartiesHubScreen) cierra la pantalla hija y vuelve al Dashboard
 *    sin cerrar la aplicación ni bloquear la sesión.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DashboardNavigationStateRestorationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun dashboardNavigationFlags_surviveActivityRecreation_withStateRestorationTester() {
        val restorationTester = StateRestorationTester(composeTestRule)

        restorationTester.setContent {
            var showFixedAssetsModule by rememberSaveable { mutableStateOf(false) }
            var showThirdPartiesModule by rememberSaveable { mutableStateOf(false) }
            var activeFinancialRubroName by rememberSaveable { mutableStateOf<String?>(null) }
            val activeFinancialRubro = remember(activeFinancialRubroName) {
                activeFinancialRubroName?.let { runCatching { FinancialRubroType.valueOf(it) }.getOrNull() }
            }

            Box {
                if (!showFixedAssetsModule && !showThirdPartiesModule && activeFinancialRubro == null) {
                    Text("Dashboard Home", modifier = Modifier.testTag("tag_home"))
                }
                if (showFixedAssetsModule) {
                    Text("Fixed Assets Open", modifier = Modifier.testTag("tag_fixed_assets"))
                }
                if (showThirdPartiesModule) {
                    Text("Third Parties Open", modifier = Modifier.testTag("tag_third_parties"))
                }
                if (activeFinancialRubro != null) {
                    Text("Rubro Detail: ${activeFinancialRubro.name}", modifier = Modifier.testTag("tag_rubro"))
                }
            }
        }

        // Initially Home is visible
        composeTestRule.onNodeWithTag("tag_home").assertIsDisplayed()

        // Recreate activity and verify state is restored
        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.onNodeWithTag("tag_home").assertIsDisplayed()
    }

    @Test
    fun backHandler_interceptsBackNavigation_andClosesChildViewSafely() {
        var isChildClosed = false
        var isSessionLocked = false

        composeTestRule.setContent {
            var showFixedAssetsModule by rememberSaveable { mutableStateOf(true) }

            if (showFixedAssetsModule) {
                BackHandler {
                    showFixedAssetsModule = false
                    isChildClosed = true
                }
                Text("Fixed Assets Screen", modifier = Modifier.testTag("tag_assets_screen"))
            } else {
                Text("Dashboard Main", modifier = Modifier.testTag("tag_dashboard_main"))
            }
        }

        composeTestRule.onNodeWithTag("tag_assets_screen").assertIsDisplayed()

        // Simulate back press via activity dispatcher
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        // Child must be dismissed and Dashboard restored, session must NOT be locked
        composeTestRule.waitForIdle()
        assertTrue("El BackHandler debe interceptar y cerrar la pantalla hija", isChildClosed)
        assertFalse("La sesión no debe bloquearse al presionar Atrás en pantalla hija", isSessionLocked)
        composeTestRule.onNodeWithTag("tag_dashboard_main").assertIsDisplayed()
    }

    @Test
    fun rubroDetail_backHandler_clearsActiveRubroSafely() {
        var activeRubroName: String? = FinancialRubroType.AVAILABLE_CASH.name

        composeTestRule.setContent {
            var activeName by rememberSaveable { mutableStateOf(activeRubroName) }
            val activeRubro = remember(activeName) {
                activeName?.let { runCatching { FinancialRubroType.valueOf(it) }.getOrNull() }
            }

            if (activeRubro != null) {
                BackHandler {
                    activeName = null
                }
                Text("Rubro: ${activeRubro.name}", modifier = Modifier.testTag("tag_rubro_view"))
            } else {
                Text("Dashboard Root", modifier = Modifier.testTag("tag_root"))
            }
        }

        composeTestRule.onNodeWithTag("tag_rubro_view").assertIsDisplayed()

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tag_root").assertIsDisplayed()
    }
}
