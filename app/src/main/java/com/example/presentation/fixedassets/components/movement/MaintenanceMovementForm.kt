package com.example.presentation.fixedassets.components.movement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.FixedAssetEntity
import com.example.presentation.fixedassets.create.components.IPhoneAdaptiveDatePickerField

/**
 * Dedicated form for Mantenimiento (100% Operational Expense).
 * Includes: Service concept, Expense Amount ($), Cost Center / Accounting Account,
 * Service Provider / Workshop, Work Order / Invoice Number, Date, and Tasks notes.
 */
@Composable
fun MaintenanceMovementForm(
    asset: FixedAssetEntity,
    title: String,
    onTitleChange: (String) -> Unit,
    amount: Double?,
    onAmountChange: (Double?) -> Unit,
    costCenter: String,
    onCostCenterChange: (String) -> Unit,
    accountingAccount: String,
    onAccountingAccountChange: (String) -> Unit,
    provider: String,
    onProviderChange: (String) -> Unit,
    workOrderOrInvoice: String,
    onWorkOrderOrInvoiceChange: (String) -> Unit,
    dateText: String,
    onDateChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color(0xFFF59E0B),
        unfocusedBorderColor = Color(0xFF132B4F),
        focusedLabelColor = Color(0xFFF59E0B),
        unfocusedLabelColor = Color(0xFF8EA5C4),
        cursorColor = Color(0xFFF59E0B)
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Concept
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Tipo de Mantenimiento / Servicio") },
            placeholder = { Text("Ej: Mantenimiento preventivo anual / Cambio de filtros", color = Color(0xFF64748B)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("maintenance_title_input"),
            colors = textFieldColors,
            shape = RoundedCornerShape(14.dp)
        )

        // Expense Amount (Currency format)
        CurrencyInputField(
            value = amount,
            onValueChange = onAmountChange,
            label = "Costo del Mantenimiento (Gasto Operacional)",
            helperText = "100% gasto a resultados. No incrementa el costo del activo ni altera la depreciación.",
            currencyCode = asset.currency,
            testTag = "movement_amount_input"
        )

        // ERP Cost Center & Accounting Account
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = costCenter,
                onValueChange = onCostCenterChange,
                label = { Text("Centro de Costo (ERP)") },
                placeholder = { Text("Ej: CC-102 Planta", color = Color(0xFF64748B)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = textFieldColors,
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = accountingAccount,
                onValueChange = onAccountingAccountChange,
                label = { Text("Cuenta Contable") },
                placeholder = { Text("Ej: 5145 Mantenimientos", color = Color(0xFF64748B)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = textFieldColors,
                shape = RoundedCornerShape(14.dp)
            )
        }

        // Workshop Provider and Work Order / Invoice
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = provider,
                onValueChange = onProviderChange,
                label = { Text("Taller / Proveedor") },
                placeholder = { Text("Ej: Taller Autorizado", color = Color(0xFF64748B)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = textFieldColors,
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = workOrderOrInvoice,
                onValueChange = onWorkOrderOrInvoiceChange,
                label = { Text("N° Orden / Factura") },
                placeholder = { Text("Ej: OT-8841", color = Color(0xFF64748B)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = textFieldColors,
                shape = RoundedCornerShape(14.dp)
            )
        }

        // Date of Service
        IPhoneAdaptiveDatePickerField(
            value = dateText,
            onValueChange = onDateChange,
            label = "Fecha de Realización del Servicio",
            modifier = Modifier.fillMaxWidth()
        )

        // Detailed Notes / Tasks executed
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Detalle de Repuestos y Trabajos Ejecutados") },
            placeholder = { Text("Repuestos sustituidos, horas de servicio y pruebas de conformidad técnica", color = Color(0xFF64748B)) },
            maxLines = 3,
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )
    }
}
