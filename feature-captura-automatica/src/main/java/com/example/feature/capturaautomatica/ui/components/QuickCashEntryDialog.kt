package com.example.feature.capturaautomatica.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.ui.components.KashNumericKeyboard

/**
 * Quick dialog triggered via the Android Quick Settings Tile to record cash expenses instantly.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickCashEntryDialog(
    onDismissRequest: () -> Unit,
    onSaveCashExpense: (amount: Double, description: String, categoryName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var rawAmountStr by remember { mutableStateOf("") }
    var amountValue by remember { mutableDoubleStateOf(0.0) }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Restaurantes") }

    val presetCategories = listOf("Restaurantes", "Transporte", "Mercado", "Servicios", "Varios")
    val presetConcepts = listOf("Almuerzo", "Café", "Taxi / Bus", "Panadería", "Parqueadero", "Tienda de Barrio")

    val quickCashAmounts = listOf(
        2000.0 to "+$2k",
        5000.0 to "+$5k",
        10000.0 to "+$10k",
        20000.0 to "+$20k",
        50000.0 to "+$50k"
    )

    fun updateAmount(newRaw: String) {
        val sanitized = newRaw.filter { it.isDigit() }
        rawAmountStr = sanitized
        amountValue = sanitized.toDoubleOrNull() ?: 0.0
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF13171F)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Gasto Rápido en Efectivo",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Cuenta: Efectivo / Billetera",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF34D399)
                                )
                            )
                        }
                    }

                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Concept chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetConcepts.forEach { concept ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (description == concept) Color(0xFF10B981) else Color(0xFF1E232B),
                            modifier = Modifier.clickable {
                                description = concept
                                when (concept) {
                                    "Almuerzo", "Café", "Panadería" -> selectedCategory = "Restaurantes"
                                    "Taxi / Bus", "Parqueadero" -> selectedCategory = "Transporte"
                                    "Tienda de Barrio" -> selectedCategory = "Mercado"
                                }
                            }
                        ) {
                            Text(
                                text = concept,
                                color = if (description == concept) Color.Black else Color(0xFFCBD5E1),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (description == concept) FontWeight.Bold else FontWeight.Normal
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Custom description textfield
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Detalle o comercio (ej: Frutería)", color = Color(0xFF64748B), fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF1A1F29),
                        unfocusedContainerColor = Color(0xFF1A1F29)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetCategories.forEach { cat ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedCategory == cat) Color(0xFF0284C7) else Color(0xFF1E232B),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedCategory = cat }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    color = if (selectedCategory == cat) Color.White else Color(0xFF94A3B8),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 10.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // KashNumericKeyboard embedded directly
                KashNumericKeyboard(
                    visible = true,
                    title = "Monto en Efectivo",
                    currentValue = amountValue,
                    onDigitClick = { digit ->
                        if (rawAmountStr.length < 10) {
                            updateAmount(rawAmountStr + digit)
                        }
                    },
                    onBackspace = {
                        if (rawAmountStr.isNotEmpty()) {
                            updateAmount(rawAmountStr.dropLast(1))
                        }
                    },
                    onClear = {
                        updateAmount("")
                    },
                    onDone = {
                        if (amountValue > 0.0) {
                            onSaveCashExpense(
                                amountValue,
                                description.ifBlank { "Gasto rápido en efectivo" },
                                selectedCategory
                            )
                        }
                    },
                    onQuickAdd = { delta ->
                        updateAmount((amountValue + delta).toLong().toString())
                    },
                    quickAmounts = quickCashAmounts
                )
            }
        }
    }
}
