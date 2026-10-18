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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
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
import com.example.data.local.entity.FinancialGoalEntity
import java.text.DecimalFormat

import androidx.compose.ui.text.style.TextOverflow
import com.example.core.ui.components.CurrencyText
import com.example.core.ui.components.KashCompactButton

@Composable
fun SweepSurplusModal(
    availableSurplus: Double,
    activeGoals: List<FinancialGoalEntity>,
    onDismiss: () -> Unit,
    onConfirmSweep: (goalId: String, amount: Double) -> Unit
) {
    var selectedGoal by remember { mutableStateOf(activeGoals.firstOrNull()) }
    var amountInput by remember { mutableStateOf(availableSurplus.toLong().toString()) }
    var showGoalDropdown by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val moneyFormatter = remember { DecimalFormat("#,##0") }
    val cardBg = Color(0xFF0B172B)
    val cardBorder = Color(0xFF132B4F)
    val primaryBlue = Color(0xFF007AFF)
    val accentCyan = Color(0xFF38BDF8)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF8EA5C4)

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
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF00C853).copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Savings, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Barrer Excedente a Meta",
                                color = textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Convertir ahorro presupuestal en logro financiero",
                                color = textSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Surplus Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF061120),
                    border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("EXCEDENTE NO GASTADO DISPONIBLE", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        CurrencyText(
                            amountDouble = availableSurplus,
                            color = textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Goal Picker
                Text("Meta Financiera Destino:", color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                if (activeGoals.isEmpty()) {
                    Text("No tienes metas financieras activas registradas.", color = Color(0xFFEF4444), fontSize = 12.sp)
                } else {
                    Box {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF061120),
                            border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showGoalDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedGoal?.name ?: "Seleccionar meta",
                                    color = textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                )
                                val pct = selectedGoal?.let {
                                    if (it.targetAmount > 0) ((it.savedAmount / it.targetAmount) * 100.0) else 0.0
                                } ?: 0.0
                                Text(
                                    text = "${String.format("%.1f", pct)}% de avance",
                                    color = accentCyan,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showGoalDropdown,
                            onDismissRequest = { showGoalDropdown = false }
                        ) {
                            activeGoals.forEach { g ->
                                val missing = (g.targetAmount - g.savedAmount).coerceAtLeast(0.0)
                                DropdownMenuItem(
                                    text = { Text("${g.name} (Falta: $ ${moneyFormatter.format(missing)})") },
                                    onClick = {
                                        selectedGoal = g
                                        showGoalDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Amount Field
                Text("Monto a Barrer hacia la Meta:", color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { input ->
                        amountInput = input.filter { it.isDigit() }
                        errorMessage = null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                KashCompactButton(
                    text = "Barrer Fondos a la Meta",
                    icon = Icons.Default.TrendingUp,
                    onClick = {
                        val parsed = amountInput.toDoubleOrNull() ?: 0.0
                        if (selectedGoal == null) {
                            errorMessage = "Selecciona una meta financiera de destino"
                            return@KashCompactButton
                        }
                        if (parsed <= 0) {
                            errorMessage = "Ingresa un monto válido"
                            return@KashCompactButton
                        }
                        if (parsed > availableSurplus) {
                            errorMessage = "No puedes transferir más del excedente disponible ($ ${moneyFormatter.format(availableSurplus)})"
                            return@KashCompactButton
                        }
                        onConfirmSweep(selectedGoal!!.id, parsed)
                    },
                    enabled = activeGoals.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    minHeight = 44.dp,
                    containerColor = Color(0xFF00C853)
                )
            }
        }
    }
}
