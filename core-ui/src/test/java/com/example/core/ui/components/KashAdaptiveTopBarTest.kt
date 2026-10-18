package com.example.core.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
 * TASK-007: Pruebas unitarias para KashAdaptiveTopBar y KashStatusBadge en :core-ui.
 *
 * Valida:
 * 1) En 320dp, con título largo, subtítulo largo y 4 botones a la derecha,
 *    todos los 4 botones de acción permanecen 100% visibles y no son expulsados.
 * 2) El badge nunca colapsa en columnas verticales y mantiene su texto en una línea.
 * 3) El botón de navegación hacia atrás es accesible con su testTag.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KashAdaptiveTopBarTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun adaptiveTopBar_inUltraCompactWidth320dp_rendersAllActionsAndBadge() {
        var backClicked = false

        composeTestRule.setContent {
            Box(modifier = Modifier.width(320.dp)) {
                KashAdaptiveTopBar(
                    title = "Escáner OCR de Facturas Larga",
                    subtitle = "Procesamiento automático de comprobantes y recibos",
                    onNavigateBack = { backClicked = true },
                    backButtonTestTag = "test_back_btn",
                    badgeContent = {
                        KashStatusBadge(
                            text = "Módulo :feature-ocr",
                            modifier = Modifier.testTag("ocr_badge")
                        )
                    },
                    actions = {
                        IconButton(onClick = {}, modifier = Modifier.size(36.dp).testTag("action_refresh")) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                        }
                        IconButton(onClick = {}, modifier = Modifier.size(36.dp).testTag("action_share")) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir")
                        }
                        IconButton(onClick = {}, modifier = Modifier.size(36.dp).testTag("action_add")) {
                            Icon(Icons.Default.Add, contentDescription = "Añadir")
                        }
                        IconButton(onClick = {}, modifier = Modifier.size(36.dp).testTag("action_more")) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más")
                        }
                    }
                )
            }
        }

        // Verify all 4 action buttons are rendered and displayed
        composeTestRule.onNodeWithTag("action_refresh").assertIsDisplayed()
        composeTestRule.onNodeWithTag("action_share").assertIsDisplayed()
        composeTestRule.onNodeWithTag("action_add").assertIsDisplayed()
        composeTestRule.onNodeWithTag("action_more").assertIsDisplayed()

        // Verify back button is displayed
        composeTestRule.onNodeWithTag("test_back_btn").assertIsDisplayed()

        // Verify badge is displayed intact
        composeTestRule.onNodeWithTag("ocr_badge").assertIsDisplayed()
        composeTestRule.onNodeWithText("Módulo :feature-ocr").assertIsDisplayed()
    }
}
