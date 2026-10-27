package com.example.feature.reconciliation.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AccountEntity
import com.example.feature.reconciliation.model.ReconciliationStatus
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ReconciliationDialog(
    account: AccountEntity,
    viewModel: ReconciliationViewModel,
    onDismiss: () -> Unit,
    onViewHistory: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 2
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Conciliar Cuenta",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onViewHistory
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Historial",
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Historial")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header card with current account details
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Saldo Actual en KashApp: ${currencyFormat.format(account.balance)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Input for Real / Statement Balance
                OutlinedTextField(
                    value = uiState.realBalanceInput,
                    onValueChange = { viewModel.onRealBalanceChanged(it) },
                    label = { Text("Saldo Real del Extracto / Efectivo") },
                    placeholder = { Text("Ej: 1500000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Input for Cutoff Date
                OutlinedTextField(
                    value = uiState.reconciliationDate,
                    onValueChange = { viewModel.onDateChanged(it) },
                    label = { Text("Fecha de Corte (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Input for Notes
                OutlinedTextField(
                    value = uiState.notesInput,
                    onValueChange = { viewModel.onNotesChanged(it) },
                    label = { Text("Notas de Auditoría (Opcional)") },
                    placeholder = { Text("Ej: Cuadre con extracto bancario de Octubre") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // Live Discrepancy Calculation Banner
                uiState.calculatedDiscrepancy?.let { disc ->
                    val (bgColor, borderColor, icon, statusText) = when (disc.status) {
                        ReconciliationStatus.BALANCED -> Quadruple(
                            Color(0xFFE8F5E9),
                            Color(0xFF4CAF50),
                            Icons.Default.CheckCircle,
                            "La cuenta está 100% cuadrada. No requiere movimiento de ajuste."
                        )
                        ReconciliationStatus.ADJUSTED_SURPLUS -> Quadruple(
                            Color(0xFFE3F2FD),
                            Color(0xFF2196F3),
                            Icons.Default.Info,
                            "Superávit: El saldo real es mayor (+${currencyFormat.format(disc.discrepancyAmount)}). Se creará un movimiento de ajuste."
                        )
                        ReconciliationStatus.ADJUSTED_DEFICIT -> Quadruple(
                            Color(0xFFFFF3E0),
                            Color(0xFFFF9800),
                            Icons.Default.Warning,
                            "Déficit: El saldo real es menor (${currencyFormat.format(disc.discrepancyAmount)}). Se creará un gasto de ajuste."
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = borderColor,
                                modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                            )
                            Column {
                                Text(
                                    text = disc.status.label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = statusText,
                                    fontSize = 12.sp,
                                    color = Color(0xFF334155)
                                )
                            }
                        }
                    }
                }

                // Error feedback
                AnimatedVisibility(visible = uiState.errorMessage != null) {
                    uiState.errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.submitReconciliation {
                        onDismiss()
                    }
                },
                enabled = !uiState.isExecuting,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (uiState.isExecuting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .width(16.dp)
                            .height(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Confirmar y Conciliar")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !uiState.isExecuting
            ) {
                Text("Cancelar")
            }
        }
    )
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
