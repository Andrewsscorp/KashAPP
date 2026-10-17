package com.example.feature.objetivos.ui.dialogs

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.AccountEntity
import com.example.feature.objetivos.domain.model.GoalDetailMetric
import java.text.DecimalFormat

@Composable
fun RegisterContributionDialog(
    goal: GoalDetailMetric,
    availableAccounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSaveContribution: (amount: Double, sourceAccountId: String?, isAllocation: Boolean, keepOpen: Boolean) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(goal.sourceAccountId ?: availableAccounts.firstOrNull()?.id ?: "") }
    var isAllocationModel by remember { mutableStateOf(false) }
    var showAccountDropdown by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val moneyFormatter = remember { DecimalFormat("#,##0") }

    val bgDark = Color(0xFF091424)
    val cardBg = Color(0xFF0B172B)
    val cardBorder = Color(0xFF132B4F)
    val primaryBlue = Color(0xFF007AFF)
    val accentCyan = Color(0xFF38BDF8)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF8EA5C4)

    val selectedAccount = availableAccounts.firstOrNull { it.id == selectedAccountId }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp)
                .testTag("register_contribution_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = bgDark,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header: Title + Close Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2563EB).copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = accentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Registrar aporte",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = goal.name,
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Monto input
                Text(
                    text = "Monto del aporte ($)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { char -> char.isDigit() || char == '.' }
                        errorMessage = null
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = accentCyan
                        )
                    },
                    singleLine = true,
                    placeholder = { Text("0", color = textSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedContainerColor = cardBg,
                        unfocusedContainerColor = cardBg,
                        focusedIndicatorColor = accentCyan,
                        unfocusedIndicatorColor = cardBorder,
                        cursorColor = accentCyan
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contribution_amount_input")
                )

                // Quick Shortcut Chips
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val shortcuts = listOf(50_000.0, 100_000.0, 200_000.0, goal.pendingAmount)
                    shortcuts.forEach { sc ->
                        if (sc > 0) {
                            Surface(
                                onClick = { amountText = sc.toInt().toString() },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0F2544),
                                border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (sc == goal.pendingAmount) "Resta" else "$${moneyFormatter.format(sc / 1000)}k",
                                    fontSize = 11.sp,
                                    color = accentCyan,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cuenta origen
                Text(
                    text = "Cuenta de débito",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        onClick = { showAccountDropdown = true },
                        shape = RoundedCornerShape(12.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = accentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = selectedAccount?.name ?: "Seleccionar cuenta",
                                        color = textPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (selectedAccount != null) {
                                        Text(
                                            text = "Disponible: $ ${moneyFormatter.format(selectedAccount.availableBalance)}",
                                            color = textSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = showAccountDropdown,
                        onDismissRequest = { showAccountDropdown = false }
                    ) {
                        availableAccounts.forEach { acc ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(acc.name, fontWeight = FontWeight.SemiBold)
                                        Text("Saldo: $ ${moneyFormatter.format(acc.availableBalance)}", fontSize = 11.sp)
                                    }
                                },
                                onClick = {
                                    selectedAccountId = acc.id
                                    showAccountDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Modelo de Aporte
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF061120),
                    border = BorderStroke(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAllocationModel = false }
                        ) {
                            RadioButton(
                                selected = !isAllocationModel,
                                onClick = { isAllocationModel = false },
                                colors = RadioButtonDefaults.colors(selectedColor = accentCyan)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Debitar de cuenta bancaria",
                                    color = textPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Descuenta el dinero del saldo disponible",
                                    color = textSecondary,
                                    fontSize = 10.5.sp
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAllocationModel = true }
                        ) {
                            RadioButton(
                                selected = isAllocationModel,
                                onClick = { isAllocationModel = true },
                                colors = RadioButtonDefaults.colors(selectedColor = accentCyan)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Asignación presupuestaria (Solo registro)",
                                    color = textPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Avanza la meta sin debitar la cuenta",
                                    color = textSecondary,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons with immediate response
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textSecondary),
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Cancelar", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val amount = amountText.toDoubleOrNull()
                            if (amount == null || amount <= 0.0) {
                                errorMessage = "Ingresa un monto válido."
                            } else {
                                onSaveContribution(amount, selectedAccountId, isAllocationModel, true)
                                amountText = ""
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentCyan),
                        border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                        modifier = Modifier.weight(1.2f).height(44.dp)
                    ) {
                        Text("+ Otro", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull()
                            if (amount == null || amount <= 0.0) {
                                errorMessage = "Ingresa un monto válido."
                            } else {
                                onSaveContribution(amount, selectedAccountId, isAllocationModel, false)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(44.dp)
                            .testTag("save_contribution_button")
                    ) {
                        Text("Guardar", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
