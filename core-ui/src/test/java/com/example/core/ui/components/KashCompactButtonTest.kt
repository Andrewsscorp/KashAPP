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
 * TASK-008: Pruebas unitarias para KashCompactButton, KashCompactOutlinedButton,
 * KashAdaptiveActionRow y KashSearchBar en :core-ui.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KashCompactButtonTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun compactButtons_insideAdaptiveActionRow_renderInConstrainedWidthWithoutClipping() {
        composeTestRule.setContent {
            Box(modifier = Modifier.width(328.dp)) {
                KashAdaptiveActionRow {
                    KashCompactOutlinedButton(
                        text = "Editar Techo",
                        onClick = {},
                        testTag = "btn_editar"
                    )
                    KashCompactOutlinedButton(
                        text = "Rebalancear",
                        onClick = {},
                        testTag = "btn_rebalancear"
                    )
                    KashCompactOutlinedButton(
                        text = "Barrer Meta",
                        onClick = {},
                        testTag = "btn_barrer"
                    )
                }
            }
        }

        // Verify all 3 buttons are rendered and visible
        composeTestRule.onNodeWithTag("btn_editar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Editar Techo").assertIsDisplayed()

        composeTestRule.onNodeWithTag("btn_rebalancear").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rebalancear").assertIsDisplayed()

        composeTestRule.onNodeWithTag("btn_barrer").assertIsDisplayed()
        composeTestRule.onNodeWithText("Barrer Meta").assertIsDisplayed()
    }

    @Test
    fun searchBar_rendersCorrectly_withPlaceholder() {
        composeTestRule.setContent {
            KashSearchBar(
                query = "",
                onQueryChange = {},
                placeholder = "Buscar sobre por categoría...",
                testTag = "search_bar_test"
            )
        }

        composeTestRule.onNodeWithTag("search_bar_test").assertIsDisplayed()
        composeTestRule.onNodeWithText("Buscar sobre por categoría...").assertIsDisplayed()
    }
}
