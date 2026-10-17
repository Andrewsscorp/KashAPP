package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.time.LocalDate

@Composable
fun IPhoneAdaptiveDatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "AAAA-MM-DD",
    title: String = label,
    isDarkTheme: Boolean = true
) {
    com.example.presentation.fixedassets.create.components.IPhoneAdaptiveDatePickerField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        placeholder = placeholder,
        title = title,
        isDarkTheme = isDarkTheme
    )
}

@Composable
fun AdaptiveWheelDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    title: String = "Seleccionar Fecha"
) {
    com.example.presentation.fixedassets.create.components.AdaptiveWheelDatePickerDialog(
        initialDate = initialDate,
        onDismiss = onDismiss,
        onDateSelected = onDateSelected,
        title = title
    )
}
