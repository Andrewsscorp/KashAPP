package com.example.presentation.fixedassets.components.movement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Reusable Currency Input Field with real-time currency formatting ($ X.XXX.XXX)
 * and quick-increment amount pills for professional financial UX.
 */
@Composable
fun CurrencyInputField(
    value: Double?,
    onValueChange: (Double?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "0",
    helperText: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    currencySymbol: String = "$",
    currencyCode: String = "COP",
    showQuickPills: Boolean = true,
    testTag: String = "currency_input_field"
) {
    val formatter = remember {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        DecimalFormat("#,##0", symbols)
    }

    val displayString = remember(value) {
        if (value == null || value == 0.0) "" else formatter.format(value)
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color(0xFF38BDF8),
        unfocusedBorderColor = Color(0xFF132B4F),
        focusedLabelColor = Color(0xFF38BDF8),
        unfocusedLabelColor = Color(0xFF8EA5C4),
        cursorColor = Color(0xFF38BDF8),
        errorBorderColor = Color(0xFFEF4444),
        errorLabelColor = Color(0xFFEF4444)
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = displayString,
            onValueChange = { rawInput ->
                val cleanDigits = rawInput.filter { it.isDigit() }
                if (cleanDigits.isBlank()) {
                    onValueChange(null)
                } else {
                    val parsed = cleanDigits.toDoubleOrNull()
                    onValueChange(parsed)
                }
            },
            label = { Text("$label ($currencySymbol $currencyCode)") },
            placeholder = { Text(placeholder, color = Color(0xFF64748B)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.AttachMoney,
                    contentDescription = null,
                    tint = if (value != null && value > 0) Color(0xFF10B981) else Color(0xFF8EA5C4)
                )
            },
            trailingIcon = {
                if (value != null && value > 0) {
                    IconButton(onClick = { onValueChange(null) }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Limpiar monto",
                            tint = Color(0xFF8EA5C4)
                        )
                    }
                }
            },
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = textFieldColors,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )

        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                fontSize = 11.sp,
                color = Color(0xFFEF4444),
                modifier = Modifier.padding(start = 4.dp)
            )
        } else if (!helperText.isNullOrBlank()) {
            Text(
                text = helperText,
                fontSize = 11.sp,
                color = Color(0xFF8EA5C4),
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // Quick amount chips (+500K, +1M, +5M, +10M)
        if (showQuickPills) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickAmountChip(label = "+500K", onClick = {
                    val current = value ?: 0.0
                    onValueChange(current + 500_000.0)
                })
                QuickAmountChip(label = "+1M", onClick = {
                    val current = value ?: 0.0
                    onValueChange(current + 1_000_000.0)
                })
                QuickAmountChip(label = "+5M", onClick = {
                    val current = value ?: 0.0
                    onValueChange(current + 5_000_000.0)
                })
                QuickAmountChip(label = "+10M", onClick = {
                    val current = value ?: 0.0
                    onValueChange(current + 10_000_000.0)
                })
            }
        }
    }
}

@Composable
private fun QuickAmountChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0F223D),
        border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF93C5FD),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
