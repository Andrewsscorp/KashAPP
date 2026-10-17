package com.example.presentation.budget.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.BudgetItemEntity
import java.text.DecimalFormat

@Composable
fun RebalanceEnvelopesModal(
    availableEnvelopes: List<BudgetItemEntity>,
    onDismiss: () -> Unit,
    onConfirmRebalance: (sourceId: String, targetId: String, amount: Double) -> Unit
) {
    var sourceEnvelope by remember { mutableStateOf(availableEnvelopes.firstOrNull()) }
    var targetEnvelope by remember { mutableStateOf(availableEnvelopes.getOrNull(1) ?: availableEnvelopes.firstOrNull()) }
    var amountInput by remember { mutableStateOf("") }
    var showSourceDropdown by remember { mutableStateOf(false) }
    var showTargetDropdown by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val moneyFormatter = remember { DecimalFormat("#,##0") }
    val cardBg = Color(0xFF0B172B)
    val cardBorder = Color(0xFF132B4F)
    val primaryBlue = Color(0xFF007AFF)
    val accentCyan = Color(0xFF38BDF8)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF8EA5C4)

    val sourceAvailable = (sourceEnvelope?.allocatedAmount ?: 0.0) - (sourceEnvelope?.spentAmount ?: 0.0)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
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
                                .size(36.dp)
                                .background(primaryBlue.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = accentCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Rebalancear Sobres", color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Mover fondos sin alterar el techo global", color = textSecondary, fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Source Envelope Picker
                Text("Sobre Origen (Se le restará):", color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Box {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF061120),
                        border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSourceDropdown = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sourceEnvelope?.categoryName ?: "Seleccionar sobre", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Disp: $ ${moneyFormatter.format(sourceAvailable.coerceAtLeast(0.0))}", color = accentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    DropdownMenu(
                        expanded = showSourceDropdown,
                        onDismissRequest = { showSourceDropdown = false }
                    ) {
                        availableEnvelopes.forEach { env ->
                            val disp = (env.allocatedAmount - env.spentAmount).coerceAtLeast(0.0)
                            DropdownMenuItem(
                                text = { Text("${env.categoryName} (Disp: $ ${moneyFormatter.format(disp)})") },
                                onClick = {
                                    sourceEnvelope = env
                                    showSourceDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Target Envelope Picker
                Text("Sobre Destino (Se le sumará):", color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Box {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF061120),
                        border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTargetDropdown = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(targetEnvelope?.categoryName ?: "Seleccionar sobre", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Actual: $ ${moneyFormatter.format(targetEnvelope?.allocatedAmount ?: 0.0)}", color = Color(0xFF00E676), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    DropdownMenu(
                        expanded = showTargetDropdown,
                        onDismissRequest = { showTargetDropdown = false }
                    ) {
                        availableEnvelopes.filter { it.id != sourceEnvelope?.id }.forEach { env ->
                            DropdownMenuItem(
                                text = { Text("${env.categoryName} (Asignado: $ ${moneyFormatter.format(env.allocatedAmount)})") },
                                onClick = {
                                    targetEnvelope = env
                                    showTargetDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Amount Field
                Text("Monto a Transferir ($ COP):", color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { input ->
                        val clean = input.filter { it.isDigit() }
                        amountInput = clean
                        errorMessage = null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("Ej: 50000", color = textSecondary.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryBlue,
                        unfocusedBorderColor = Color(0xFF1E3A8A),
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedContainerColor = Color(0xFF061120),
                        unfocusedContainerColor = Color(0xFF061120)
                    ),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage!!, color = Color(0xFFEF4444), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Button
                Button(
                    onClick = {
                        val parsed = amountInput.toDoubleOrNull() ?: 0.0
                        if (sourceEnvelope == null || targetEnvelope == null) {
                            errorMessage = "Selecciona sobre origen y sobre destino"
                            return@Button
                        }
                        if (sourceEnvelope?.id == targetEnvelope?.id) {
                            errorMessage = "El sobre de origen y destino no pueden ser el mismo"
                            return@Button
                        }
                        if (parsed <= 0) {
                            errorMessage = "Ingresa un monto válido superior a cero"
                            return@Button
                        }
                        if (parsed > sourceAvailable && sourceAvailable > 0) {
                            errorMessage = "El sobre origen solo tiene $ ${moneyFormatter.format(sourceAvailable)} disponible"
                            return@Button
                        }
                        onConfirmRebalance(sourceEnvelope!!.id, targetEnvelope!!.id, parsed)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("Ejecutar Rebalanceo", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
