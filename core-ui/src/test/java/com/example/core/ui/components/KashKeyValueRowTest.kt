package com.example.core.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * TASK-009: Pruebas unitarias para KashKeyValueRow, KashTransactionItemRow
 * y KashResponsiveDialog en :core-ui.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KashKeyValueRowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun transactionItemRow_inConstrainedWidth320dp_rendersDescriptionAndAmountIntact() {
        val longDescription = "Compra de suministros empresariales y alimentos para el evento corporativo de fin de mes"

        composeTestRule.setContent {
            Box(modifier = Modifier.width(320.dp)) {
                KashTransactionItemRow(
                    title = "Alimentación y Eventos",
                    subtitle = longDescription,
                    date = "18 Oct 2026",
                    amountDouble = 16330.0,
                    prefix = "- ",
                    isExpense = true,
                    currencyCode = "COP",
                    testTag = "row_long_tx"
                )
            }
        }

        // Verify the row is displayed
        composeTestRule.onNodeWithTag("row_long_tx").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alimentación y Eventos").assertIsDisplayed()
        composeTestRule.onNodeWithText(longDescription).assertIsDisplayed()

        // Amount must be formatted cleanly in one single line
        composeTestRule.onNodeWithText("- $ 16.330").assertIsDisplayed()
    }

    @Test
    fun keyValueRow_rendersKeyAndValueCorrectly() {
        composeTestRule.setContent {
            Box(modifier = Modifier.width(320.dp)) {
                KashKeyValueRow(
                    keyText = "Categoría Seleccionada",
                    valueText = "Alimentación Mes",
                    testTag = "kv_row_test"
                )
            }
        }

        composeTestRule.onNodeWithTag("kv_row_test").assertIsDisplayed()
        composeTestRule.onNodeWithText("Categoría Seleccionada").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alimentación Mes").assertIsDisplayed()
    }

    @Test
    fun responsiveDialog_rendersTitleAndContent() {
        composeTestRule.setContent {
            KashResponsiveDialog(
                onDismissRequest = {},
                title = "Detalle del Sobre",
                subtitle = "Transacciones del período",
                testTag = "test_dialog"
            ) {
                KashKeyValueRow(
                    keyText = "Presupuesto Asignado",
                    valueText = "$ 300.000"
                )
            }
        }

        composeTestRule.onNodeWithTag("test_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Detalle del Sobre").assertIsDisplayed()
        composeTestRule.onNodeWithText("Presupuesto Asignado").assertIsDisplayed()
    }
}
