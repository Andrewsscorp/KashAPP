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
 * Dedicated form for Revalúo Técnico (IAS 16 Revaluation Model).
 * Includes: New Appraised Value ($), New Residual Value ($), Expert Appraiser Name,
 * Professional License Number, Appraisal PDF Attachment, Date, and Method Notes.
 */
@Composable
fun RevaluationMovementForm(
    asset: FixedAssetEntity,
    title: String,
    onTitleChange: (String) -> Unit,
    amount: Double?,
    onAmountChange: (Double?) -> Unit,
    newResidualValue: Double?,
    onNewResidualValueChange: (Double?) -> Unit,
    appraiserName: String,
    onAppraiserNameChange: (String) -> Unit,
    appraiserLicense: String,
    onAppraiserLicenseChange: (String) -> Unit,
    documentUri: String?,
    onDocumentUriChange: (String?) -> Unit,
    dateText: String,
    onDateChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color(0xFFA78BFA),
        unfocusedBorderColor = Color(0xFF132B4F),
        focusedLabelColor = Color(0xFFA78BFA),
        unfocusedLabelColor = Color(0xFF8EA5C4),
        cursorColor = Color(0xFFA78BFA)
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Concept
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Concepto del Revalúo") },
            placeholder = { Text("Ej: Avalúo técnico comercial anual", color = Color(0xFF64748B)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("revaluation_title_input"),
            colors = textFieldColors,
            shape = RoundedCornerShape(14.dp)
        )

        // New Appraised Value (Replaces depreciable base)
        CurrencyInputField(
            value = amount,
            onValueChange = onAmountChange,
            label = "Nuevo Valor Razonable / Avalúo Comercial",
            helperText = "Reemplaza el valor en libros actual. La diferencia genera Superávit por Revaluación.",
            currencyCode = asset.currency,
            testTag = "movement_amount_input"
        )

        // New Residual Value from appraisal
        CurrencyInputField(
            value = newResidualValue,
            onValueChange = onNewResidualValueChange,
            label = "Nuevo Valor Residual Estimado por el Perito",
            helperText = "Valor de salvamento actualizado según el dictamen técnico",
            currencyCode = asset.currency,
            showQuickPills = false,
            testTag = "revaluation_residual_input"
        )

        // Appraiser Name & License
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = appraiserName,
                onValueChange = onAppraiserNameChange,
                label = { Text("Perito Tasador / Firma") },
                singleLine = true,
                modifier = Modifier.weight(1.2f),
                colors = textFieldColors,
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = appraiserLicense,
                onValueChange = onAppraiserLicenseChange,
                label = { Text("N° Matrícula / RNA") },
                singleLine = true,
                modifier = Modifier.weight(0.8f),
                colors = textFieldColors,
                shape = RoundedCornerShape(14.dp)
            )
        }

        // Appraisal PDF Document Attachment
        DocumentAttachmentPicker(
            documentUri = documentUri,
            onDocumentChanged = onDocumentUriChange,
            label = "Dictamen del Peritaje Técnico (PDF Adjunto)",
            placeholderDocName = "informe_avaluo_${asset.assetCode}.pdf"
        )

        // Date of Appraisal
        IPhoneAdaptiveDatePickerField(
            value = dateText,
            onValueChange = onDateChange,
            label = "Fecha de Emisión del Avalúo",
            modifier = Modifier.fillMaxWidth()
        )

        // Methodology and Notes
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Metodología y Alcance del Dictamen") },
            placeholder = { Text("Ej: Enfoque de costo de reposición depreciado / Método de mercado", color = Color(0xFF64748B)) },
            maxLines = 3,
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )
    }
}
