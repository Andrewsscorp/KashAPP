package com.example.presentation.fixedassets.components.movement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FixedAssetEntity
import com.example.presentation.fixedassets.create.components.IPhoneAdaptiveDatePickerField

/**
 * Dedicated form for Asset Disposal / Derecognition (IAS 16).
 * Includes: Disposal motive, Recovery / Scrap sale value ($), Disposal Resolution / Police Report #,
 * Disposal Document Attachment, Date, and derecognition notice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisposalMovementForm(
    asset: FixedAssetEntity,
    title: String,
    onTitleChange: (String) -> Unit,
    amount: Double?,
    onAmountChange: (Double?) -> Unit,
    disposalReason: String,
    onDisposalReasonChange: (String) -> Unit,
    actNumber: String,
    onActNumberChange: (String) -> Unit,
    documentUri: String?,
    onDocumentUriChange: (String?) -> Unit,
    dateText: String,
    onDateChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    isSale: Boolean = false,
    modifier: Modifier = Modifier
) {
    var expandedReasonDropdown by remember { mutableStateOf(false) }

    val motives = remember(isSale) {
        if (isSale) {
            listOf(
                "Venta a terceros por renovación de flota",
                "Venta de salvamento a aseguradora",
                "Subasta pública de activos",
                "Cesión comercial a valor residual"
            )
        } else {
            listOf(
                "Chatarrización y desmantelamiento",
                "Obsolescencia tecnológica irreparable",
                "Hurto o robo comprobado (Denuncia legal)",
                "Destrucción por siniestro o incendio",
                "Donación a entidad sin ánimo de lucro",
                "Desincorporación por fin de vida útil"
            )
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = if (isSale) Color(0xFF10B981) else Color(0xFF94A3B8),
        unfocusedBorderColor = Color(0xFF132B4F),
        focusedLabelColor = if (isSale) Color(0xFF10B981) else Color(0xFF94A3B8),
        unfocusedLabelColor = Color(0xFF8EA5C4),
        cursorColor = if (isSale) Color(0xFF10B981) else Color(0xFF94A3B8)
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Warning Banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF201600),
            border = BorderStroke(1.dp, Color(0xFFB45309)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isSale) {
                        "Atención: La venta marcará el activo como 'Vendido' y detendrá definitivamente los ciclos futuros de depreciación."
                    } else {
                        "Atención: La baja desincorporará el activo de la contabilidad patrimonial ('Dado de baja'). Esta acción detiene la depreciación."
                    },
                    fontSize = 11.5.sp,
                    color = Color(0xFFFEF3C7)
                )
            }
        }

        // Title
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text(if (isSale) "Concepto de la Venta" else "Concepto de la Baja Definitiva") },
            placeholder = { Text("Ej: Desincorporación por chatarrización", color = Color(0xFF64748B)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("disposal_title_input"),
            colors = textFieldColors,
            shape = RoundedCornerShape(14.dp)
        )

        // Dropdown Motive
        ExposedDropdownMenuBox(
            expanded = expandedReasonDropdown,
            onExpandedChange = { expandedReasonDropdown = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = disposalReason.ifBlank { motives.first() },
                onValueChange = {},
                readOnly = true,
                label = { Text("Motivo Específico de Disposición") },
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
                motives.forEach { m ->
                    DropdownMenuItem(
                        text = { Text(m, fontSize = 13.sp) },
                        onClick = {
                            onDisposalReasonChange(m)
                            expandedReasonDropdown = false
                        }
                    )
                }
            }
        }

        // Sale or scrap salvage price (Currency format)
        CurrencyInputField(
            value = amount,
            onValueChange = onAmountChange,
            label = if (isSale) "Precio de Venta Cobrado" else "Valor Recuperado / Chatarra (Si aplica)",
            placeholder = "0 si no hubo valor de recuperación",
            helperText = if (isSale) "Genera utilidad o pérdida en venta según valor en libros" else "Monto ingresado por reciclaje, subasta o seguro",
            currencyCode = asset.currency,
            showQuickPills = false,
            testTag = "movement_amount_input"
        )

        // Act / Police report / Resolution number
        OutlinedTextField(
            value = actNumber,
            onValueChange = onActNumberChange,
            label = { Text("N° Acta de Baja / Resolución / Factura de Venta") },
            placeholder = { Text("Ej: ACTA-BAJA-2026-04 o DEN-9921", color = Color(0xFF64748B)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors,
            shape = RoundedCornerShape(14.dp)
        )

        // Disposal document attachment
        DocumentAttachmentPicker(
            documentUri = documentUri,
            onDocumentChanged = onDocumentUriChange,
            label = "Acta de Desincorporación Firmada (PDF / Foto)",
            placeholderDocName = "acta_baja_${asset.assetCode}.pdf"
        )

        // Date of disposal
        IPhoneAdaptiveDatePickerField(
            value = dateText,
            onValueChange = onDateChange,
            label = "Fecha de Baja / Salida Definitiva",
            modifier = Modifier.fillMaxWidth()
        )

        // Notes
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Observaciones del Retiro y Destino Final") },
            placeholder = { Text("Responsable de la entrega, empresa recicladora o detalles del siniestro", color = Color(0xFF64748B)) },
            maxLines = 3,
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )
    }
}
