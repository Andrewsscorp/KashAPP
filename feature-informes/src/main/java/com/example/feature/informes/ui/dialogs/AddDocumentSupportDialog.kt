package com.example.feature.informes.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.feature.informes.domain.model.DocumentFileFormat
import com.example.feature.informes.domain.model.DocumentSupportType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDocumentSupportDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        type: DocumentSupportType,
        format: DocumentFileFormat,
        amount: Double?,
        entityName: String,
        notes: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(DocumentSupportType.FACTURA) }
    var selectedFormat by remember { mutableStateOf(DocumentFileFormat.PDF) }
    var amountText by remember { mutableStateOf("") }
    var entityName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isTypeMenuExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subir Soporte Documental",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF94A3B8))
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título o Descripción del Documento") },
                    placeholder = { Text("Ej: Factura Notaría Escritura Pública", color = Color(0xFF64748B)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFF38BDF8),
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    )
                )

                // Document Type Selector
                ExposedDropdownMenuBox(
                    expanded = isTypeMenuExpanded,
                    onExpandedChange = { isTypeMenuExpanded = !isTypeMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedType.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Documento") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTypeMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFF38BDF8),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = isTypeMenuExpanded,
                        onDismissRequest = { isTypeMenuExpanded = false },
                        containerColor = Color(0xFF1E293B)
                    ) {
                        DocumentSupportType.values().forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.label, color = Color.White) },
                                onClick = {
                                    selectedType = t
                                    isTypeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Format Selector (PDF, IMG, XLSX)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DocumentFileFormat.values().forEach { fmt ->
                        val isSel = selectedFormat == fmt
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedFormat = fmt },
                            label = { Text(fmt.extension, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0284C7),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Related Entity / Movement
                OutlinedTextField(
                    value = entityName,
                    onValueChange = { entityName = it },
                    label = { Text("Vínculo (Cuenta, Activo o Deuda)") },
                    placeholder = { Text("Ej: Apartamento 402 / Mazda 3", color = Color(0xFF64748B)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFF38BDF8),
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    )
                )

                // Related Amount (Optional)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Monto Asociado (Opcional)") },
                    placeholder = { Text("0", color = Color(0xFF64748B)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFF38BDF8),
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    )
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas y Observaciones") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFF38BDF8),
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    )
                )

                // Confirm Button
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onConfirm(
                                title.trim(),
                                selectedType,
                                selectedFormat,
                                amountText.toDoubleOrNull(),
                                entityName.trim(),
                                notes.trim()
                            )
                        }
                    },
                    enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Guardar Soporte", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
