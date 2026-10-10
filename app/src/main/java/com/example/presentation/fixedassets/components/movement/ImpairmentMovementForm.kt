package com.example.presentation.fixedassets.components.movement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FixedAssetEntity
import com.example.presentation.fixedassets.create.components.IPhoneAdaptiveDatePickerField

/**
 * Dedicated form for Impairment loss recognition (NIC 36).
 * Includes: Impairment cause dropdown, Loss amount ($), Recoverable amount ($),
 * Technical justification notes, and Date.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImpairmentMovementForm(
    asset: FixedAssetEntity,
    title: String,
    onTitleChange: (String) -> Unit,
    amount: Double?,
    onAmountChange: (Double?) -> Unit,
    recoverableAmount: Double?,
    onRecoverableAmountChange: (Double?) -> Unit,
    impairmentReason: String,
    onImpairmentReasonChange: (String) -> Unit,
    dateText: String,
    onDateChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedReasonDropdown by remember { mutableStateOf(false) }

    val causes = remember {
        listOf(
            "Daño físico o accidente no asegurable",
            "Obsolescencia tecnológica acelerada",
            "Caída abrupta del valor de mercado",
            "Cese de línea de producción / Desuso",
            "Siniestro operativo parcial",
            "Cambio regulatorio desfavorable"
        )
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color(0xFFEF4444),
        unfocusedBorderColor = Color(0xFF132B4F),
        focusedLabelColor = Color(0xFFEF4444),
        unfocusedLabelColor = Color(0xFF8EA5C4),
        cursorColor = Color(0xFFEF4444)
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Concept
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Concepto de Deterioro") },
            placeholder = { Text("Ej: Pérdida por obsolescencia técnica", color = Color(0xFF64748B)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("impairment_title_input"),
            colors = textFieldColors,
            shape = RoundedCornerShape(14.dp)
        )

        // Dropdown: Impairment Cause
        ExposedDropdownMenuBox(
            expanded = expandedReasonDropdown,
            onExpandedChange = { expandedReasonDropdown = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = impairmentReason.ifBlank { causes.first() },
                onValueChange = {},
                readOnly = true,
                label = { Text("Causa Principal del Deterioro (NIC 36)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedReasonDropdown) },
                colors = textFieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expandedReasonDropdown,
                onDismissRequest = { expandedReasonDropdown = false }
            ) {
                causes.forEach { cause ->
                    DropdownMenuItem(
                        text = { Text(cause, fontSize = 13.sp) },
                        onClick = {
                            onImpairmentReasonChange(cause)
                            expandedReasonDropdown = false
                        }
                    )
                }
            }
        }

        // Loss Amount (Currency format)
        CurrencyInputField(
            value = amount,
            onValueChange = onAmountChange,
            label = "Importe del Deterioro (Pérdida Contable)",
            helperText = "Reduce el valor en libros y ajusta prospectivamente las cuotas de amortización",
            currencyCode = asset.currency,
            testTag = "movement_amount_input"
        )

        // Estimated Recoverable Amount (NIC 36)
        CurrencyInputField(
            value = recoverableAmount,
            onValueChange = onRecoverableAmountChange,
            label = "Importe Recuperable Estimado (Mayor entre VR y VU)",
            placeholder = "Opcional: techo de recuperación del activo",
            helperText = "Valor razonable menos costos de venta vs. valor en uso",
            currencyCode = asset.currency,
            showQuickPills = false,
            testTag = "recoverable_amount_input"
        )

        // Date of test
        IPhoneAdaptiveDatePickerField(
            value = dateText,
            onValueChange = onDateChange,
            label = "Fecha de la Prueba de Deterioro",
            modifier = Modifier.fillMaxWidth()
        )

        // Technical Justification Notes
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Justificación Técnica y Dictamen de Deterioro") },
            placeholder = { Text("Resultados de la inspección, pruebas de esfuerzo o dictamen pericial de daño", color = Color(0xFF64748B)) },
            maxLines = 3,
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )
    }
}
