package com.example.presentation.goals.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.core.ui.components.KashNumericKeyboard

/**
 * Proprietary KashApp In-App Numeric Keyboard for Goals & Budgeting.
 * Delegated to the shared, responsive [KashNumericKeyboard] component in :core-ui.
 */
@Composable
fun GoalNumericKeyboard(
    visible: Boolean,
    title: String,
    onDigitClick: (String) -> Unit,
    onQuickAdd: (Double) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    KashNumericKeyboard(
        visible = visible,
        title = title,
        onDigitClick = onDigitClick,
        onQuickAdd = onQuickAdd,
        onBackspace = onBackspace,
        onClear = onClear,
        onDone = onDone,
        modifier = modifier,
        quickAmounts = listOf(
            100_000.0 to "+100k",
            500_000.0 to "+500k",
            1_000_000.0 to "+1M",
            5_000_000.0 to "+5M"
        ),
        keyboardTestTag = "goal_numeric_keyboard"
    )
}
