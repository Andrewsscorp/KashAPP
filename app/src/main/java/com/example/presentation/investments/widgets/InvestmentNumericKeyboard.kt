package com.example.presentation.investments.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.core.ui.components.KashNumericKeyboard

/**
 * Enterprise In-App Numeric Keyboard for Investments & CDTs.
 * Delegated to the shared, responsive [KashNumericKeyboard] component in :core-ui.
 */
@Composable
fun InvestmentNumericKeyboard(
    visible: Boolean,
    title: String,
    currentValue: Double,
    isPercentage: Boolean = false,
    onDigitClick: (String) -> Unit,
    onQuickAdd: (Double) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quickAmounts = if (isPercentage) {
        listOf(
            5.0 to "5%",
            10.0 to "10%",
            12.0 to "12%",
            15.0 to "15%",
            18.0 to "18%"
        )
    } else {
        listOf(
            100_000.0 to "+100k",
            500_000.0 to "+500k",
            1_000_000.0 to "+1M",
            2_000_000.0 to "+2M",
            5_000_000.0 to "+5M"
        )
    }

    KashNumericKeyboard(
        visible = visible,
        title = title,
        currentValue = currentValue,
        isPercentage = isPercentage,
        onDigitClick = onDigitClick,
        onQuickAdd = onQuickAdd,
        onBackspace = onBackspace,
        onClear = onClear,
        onDone = onDone,
        modifier = modifier,
        quickAmounts = quickAmounts,
        keyboardTestTag = "investment_numeric_keyboard"
    )
}
