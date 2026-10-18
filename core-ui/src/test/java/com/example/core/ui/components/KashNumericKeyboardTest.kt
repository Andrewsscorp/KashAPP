package com.example.core.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * TASK-010: Pruebas unitarias para KashNumericKeyboard en :core-ui.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KashNumericKeyboardTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun numericKeyboard_digitClicksAndActions_triggerCallbacks() {
        var clickedDigit = ""
        var backspaceClicked = false
        var clearClicked = false
        var doneClicked = false

        composeTestRule.setContent {
            Box(modifier = Modifier.width(360.dp)) {
                KashNumericKeyboard(
                    visible = true,
                    title = "Asignar Presupuesto",
                    currentValue = 150000.0,
                    onDigitClick = { clickedDigit = it },
                    onBackspace = { backspaceClicked = true },
                    onClear = { clearClicked = true },
                    onDone = { doneClicked = true },
                    keyboardTestTag = "test_keyboard"
                )
            }
        }

        // Verify keyboard and header are displayed
        composeTestRule.onNodeWithTag("test_keyboard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Asignar Presupuesto").assertIsDisplayed()

        // Click key '5'
        composeTestRule.onNodeWithTag("key_5").performClick()
        assertEquals("5", clickedDigit)

        // Click DEL
        composeTestRule.onNodeWithTag("key_DEL").performClick()
        assertTrue(backspaceClicked)

        // Click Clear
        composeTestRule.onNodeWithTag("keyboard_clear_btn").performClick()
        assertTrue(clearClicked)

        // Click Done
        composeTestRule.onNodeWithTag("keyboard_done_btn").performClick()
        assertTrue(doneClicked)
    }

    @Test
    fun numericKeyboard_percentageMode_displaysDotKey() {
        var clickedKey = ""

        composeTestRule.setContent {
            Box(modifier = Modifier.width(320.dp)) {
                KashNumericKeyboard(
                    visible = true,
                    title = "Tasa de Interés",
                    currentValue = 12.5,
                    isPercentage = true,
                    onDigitClick = { clickedKey = it },
                    onBackspace = {},
                    onClear = {},
                    onDone = {}
                )
            }
        }

        // Dot key must be present in percentage mode
        composeTestRule.onNodeWithTag("key_.").assertIsDisplayed().performClick()
        assertEquals(".", clickedKey)
    }

    @Test
    @Config(qualifiers = "w800dp-h360dp")
    fun numericKeyboard_inLandscapeCompactHeight_rendersWithoutCrashing() {
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxWidth()) {
                KashNumericKeyboard(
                    visible = true,
                    title = "Monto de Meta",
                    currentValue = 500000.0,
                    onDigitClick = {},
                    onBackspace = {},
                    onClear = {},
                    onDone = {},
                    keyboardTestTag = "landscape_keyboard"
                )
            }
        }

        composeTestRule.onNodeWithTag("landscape_keyboard").assertIsDisplayed()
        composeTestRule.onNodeWithTag("key_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("key_0").assertIsDisplayed()
    }
}
