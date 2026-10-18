package com.example.presentation.budget.widgets
 
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.core.ui.components.KashNumericKeyboard

/**
 * Enterprise In-App Numeric Keyboard for Budgeting and Category Envelopes.
 * Delegated to the shared, responsive [KashNumericKeyboard] component in :core-ui.
 */
@Composable
fun BudgetNumericKeyboard(
    visible: Boolean,
    title: String,
    currentValue: Double,
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
        currentValue = currentValue,
        onDigitClick = onDigitClick,
        onQuickAdd = onQuickAdd,
        onBackspace = onBackspace,
        onClear = onClear,
        onDone = onDone,
        modifier = modifier,
        keyboardTestTag = "budget_numeric_keyboard"
    )
}
