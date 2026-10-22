package com.example.presentation.fixedassets.components.movement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import com.example.core.ui.components.IosStyleSwitch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FixedAssetEntity
import com.example.presentation.fixedassets.create.components.IPhoneAdaptiveDatePickerField

/**
 * Dedicated form for Mejora / Adición (Capitalizable).
 * Includes: Concept, Capitalized Amount ($), Useful life increment (months/years),
 * Optional new residual value ($), Date, Contractor/Vendor, and NIIF policy override.
 */
@Composable
fun ImprovementMovementForm(
    asset: FixedAssetEntity,
    title: String,
    onTitleChange: (String) -> Unit,
    amount: Double?,
    onAmountChange: (Double?) -> Unit,
    usefulLifeIncrementMonths: Int,
    onUsefulLifeIncrementChange: (Int) -> Unit,
    newResidualValue: Double?,
    onNewResidualValueChange: (Double?) -> Unit,
    dateText: String,
    onDateChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    provider: String,
    onProviderChange: (String) -> Unit,
    invoiceNumber: String,
    onInvoiceNumberChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var allowOverrideByCapitalization by remember { mutableStateOf(true) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color(0xFF38BDF8),
        unfocusedBorderColor = Color(0xFF132B4F),
        focusedLabelColor = Color(0xFF38BDF8),
        unfocusedLabelColor = Color(0xFF8EA5C4),
        cursorColor = Color(0xFF38BDF8)
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Concept
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Concepto de la Mejora") },
            placeholder = { Text("Ej: Repotenciación de motor / Ampliación", color = Color(0xFF64748B)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("improvement_title_input"),
            colors = textFieldColors,
            shape = RoundedCornerShape(14.dp)
        )

        // Capitalized Amount (Currency format)
        CurrencyInputField(
            value = amount,
            onValueChange = onAmountChange,
            label = "Monto de la Inversión (Capitalizable)",
            helperText = "Se sumará directamente al valor en libros (Nueva base depreciable)",
            currencyCode = asset.currency,
            testTag = "movement_amount_input"
        )

        // Useful life increment
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "AUMENTO DE VIDA ÚTIL TÉCNICA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8EA5C4),
                letterSpacing = 0.5.sp
            )

            // If asset policy had locked life changes, display NIIF exception banner
            if (!asset.allowLifeChange) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F1E38),
                    border = BorderStroke(1.dp, Color(0xFF1D4ED8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Excepción NIIF: Repotenciación",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF93C5FD)
                                )
                                Text(
                                    text = "Las mejoras de capital alteran legalmente la vida útil del activo repotenciado.",
                                    fontSize = 10.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }

                        IosStyleSwitch(
                            checked = allowOverrideByCapitalization,
                            onCheckedChange = { allowOverrideByCapitalization = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF0284C7)
                            )
                        )
                    }
                }
            }

            val isLifeInputEnabled = asset.allowLifeChange || allowOverrideByCapitalization

            OutlinedTextField(
                value = if (usefulLifeIncrementMonths == 0) "" else usefulLifeIncrementMonths.toString(),
                onValueChange = { input ->
                    if (isLifeInputEnabled) {
                        val digits = input.filter { it.isDigit() }
                        onUsefulLifeIncrementChange(digits.toIntOrNull() ?: 0)
                    }
                },
                enabled = isLifeInputEnabled,
                label = { Text("Meses adicionales de vida útil") },
                placeholder = { Text("Ej: 12 (1 año), 24 (2 años)", color = Color(0xFF64748B)) },
                supportingText = {
                    Text(
                        text = if (usefulLifeIncrementMonths > 0) {
                            "+${usefulLifeIncrementMonths / 12} años y ${usefulLifeIncrementMonths % 12} meses adicionales"
                        } else {
                            "Si la mejora no extiende la vida útil, dejar en 0"
                        },
                        color = Color(0xFF8EA5C4)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = RoundedCornerShape(14.dp)
            )

            // Quick increment chips for months
            if (isLifeInputEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    UsefulLifeChip(label = "+6 meses", onClick = { onUsefulLifeIncrementChange(6) })
                    UsefulLifeChip(label = "+12 meses (1 año)", onClick = { onUsefulLifeIncrementChange(12) })
                    UsefulLifeChip(label = "+24 meses (2 años)", onClick = { onUsefulLifeIncrementChange(24) })
                    UsefulLifeChip(label = "+36 meses (3 años)", onClick = { onUsefulLifeIncrementChange(36) })
                    UsefulLifeChip(label = "+60 meses (5 años)", onClick = { onUsefulLifeIncrementChange(60) })
                }
            }
        }

        // Optional New Residual Value
        CurrencyInputField(
            value = newResidualValue,
            onValueChange = onNewResidualValueChange,
            label = "Nuevo Valor Residual / Salvamento (Opcional)",
            placeholder = "Opcional: solo si el salvamento cambió",
            helperText = "Si el activo repotenciado tendrá mayor valor al final de su vida",
            currencyCode = asset.currency,
            showQuickPills = false
        )

        // Date Picker
        IPhoneAdaptiveDatePickerField(
            value = dateText,
            onValueChange = onDateChange,
            label = "Fecha de la Mejora (Corte Contable)",
            modifier = Modifier.fillMaxWidth()
        )

        // Vendor and Invoice
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = provider,
                onValueChange = onProviderChange,
                label = { Text("Contratista / Proveedor") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = textFieldColors,
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = invoiceNumber,
                onValueChange = onInvoiceNumberChange,
                label = { Text("N° Factura") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = textFieldColors,
                shape = RoundedCornerShape(14.dp)
            )
        }

        // Observations
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Observaciones Técnicas y Garantía de la Mejora") },
            maxLines = 3,
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
private fun UsefulLifeChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0C2444),
        border = BorderStroke(1.dp, Color(0xFF1D4ED8)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF60A5FA),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
