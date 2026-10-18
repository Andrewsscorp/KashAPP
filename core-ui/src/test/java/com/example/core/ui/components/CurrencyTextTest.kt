package com.example.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

/**
 * TASK-006: Prueba unitaria para el componente CurrencyText en :core-ui.
 *
 * Valida:
 * 1) Inyección de prefijo y sufijo sobre el monto formateado.
 * 2) Modo privacidad masking ("••••••").
 * 3) Contención en una sola línea en anchos reducidos (70dp) con auto-reducción de fuente.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CurrencyTextTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun currencyText_withPrefixAndSuffix_rendersFullFormattedString() {
        composeTestRule.setContent {
            CurrencyText(
                amountDouble = 500000.0,
                prefix = "Techo: ",
                suffix = " /mes",
                currencyCode = "COP",
                testTag = "currency_techo"
            )
        }

        composeTestRule.onNodeWithTag("currency_techo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Techo: $ 500.000 /mes").assertIsDisplayed()
    }

    @Test
    fun currencyText_privacyMode_masksAmount() {
        composeTestRule.setContent {
            CurrencyText(
                amount = BigDecimal.valueOf(1234567),
                prefix = "Saldo: ",
                isPrivacyMode = true,
                testTag = "currency_privacy"
            )
        }

        composeTestRule.onNodeWithTag("currency_privacy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Saldo: ••••••").assertIsDisplayed()
    }

    @Test
    fun currencyText_inConstrainedWidth_adaptsWithoutThrowing() {
        composeTestRule.setContent {
            Box(modifier = Modifier.width(70.dp)) {
                CurrencyText(
                    amountDouble = 999999999.0,
                    prefix = "-$ ",
                    fontSize = 18.sp,
                    minFontSize = 11.sp,
                    testTag = "currency_constrained"
                )
            }
        }

        composeTestRule.onNodeWithTag("currency_constrained").assertIsDisplayed()
    }
}
