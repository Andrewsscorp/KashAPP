package com.example.feature.capturaautomatica.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.local.entity.PendingAutoCaptureEntity
import com.example.feature.capturaautomatica.domain.model.AutoCaptureAccountOption
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modal review sheet for pending bank notification captures.
 * Allows reviewing detected financial movements, picking the destination bank account, and 1-tap confirmation or dismissal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCaptureInboxSheet(
    pendingCaptures: List<PendingAutoCaptureEntity>,
    availableAccounts: List<AutoCaptureAccountOption> = emptyList(),
    onConfirmCapture: (captureId: String, customAccountId: String?, customAccountName: String?) -> Unit,
    onDismissCapture: (captureId: String) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF121417),
        dragHandle = null
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
                Column {
                    Text(
                        text = "Movimientos Detectados",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Lectura 100% en dispositivo • Anti-OTP (Ley 1581)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8)
                        )
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (pendingCaptures.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "¡Bandeja al día!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "No tienes notificaciones pendientes de registrar",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8)
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(pendingCaptures, key = { it.id }) { capture ->
                        PendingCaptureCard(
                            capture = capture,
                            availableAccounts = availableAccounts,
                            onConfirm = { accId, accName ->
                                onConfirmCapture(capture.id, accId, accName)
                            },
                            onDismiss = { onDismissCapture(capture.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Backward compatibility overload for callers who do not pass availableAccounts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCaptureInboxSheet(
    pendingCaptures: List<PendingAutoCaptureEntity>,
    onConfirmCapture: (captureId: String) -> Unit,
    onDismissCapture: (captureId: String) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    AutoCaptureInboxSheet(
        pendingCaptures = pendingCaptures,
        availableAccounts = emptyList(),
        onConfirmCapture = { id, _, _ -> onConfirmCapture(id) },
        onDismissCapture = onDismissCapture,
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    )
}

@Composable
fun PendingCaptureCard(
    capture: PendingAutoCaptureEntity,
    availableAccounts: List<AutoCaptureAccountOption>,
    onConfirm: (selectedAccountId: String?, selectedAccountName: String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val localeCo = Locale.Builder().setLanguage("es").setRegion("CO").build()
    val formattedAmount = try {
        val format = NumberFormat.getCurrencyInstance(localeCo)
        format.maximumFractionDigits = 0
        format.format(capture.amount)
    } catch (e: Exception) {
        "$${capture.amount.toLong()}"
    }

    val dateStr = try {
        SimpleDateFormat("dd MMM, hh:mm a", localeCo).format(Date(capture.notificationTimestamp))
    } catch (e: Exception) {
        ""
    }

    val isIncome = capture.transactionType == "INCOME"

    // Resolver cuenta inicial
    var selectedAccount by remember(capture.id, availableAccounts) {
        mutableStateOf(
            availableAccounts.find { it.id == capture.matchedAccountId }
                ?: availableAccounts.find {
                    it.bankName.contains(capture.detectedBankName, ignoreCase = true) ||
                        it.name.contains(capture.detectedBankName, ignoreCase = true)
                }
                ?: availableAccounts.firstOrNull()
        )
    }

    var isAccountSelectorOpen by remember { mutableStateOf(false) }

    // Color temático del banco
    val bankAccentColor = when {
        capture.detectedBankName.contains("Nequi", ignoreCase = true) -> Color(0xFFE00070)
        capture.detectedBankName.contains("Bancolombia", ignoreCase = true) -> Color(0xFFFDDA24)
        capture.detectedBankName.contains("Nu", ignoreCase = true) -> Color(0xFF820AD1)
        capture.detectedBankName.contains("Davivienda", ignoreCase = true) ||
            capture.detectedBankName.contains("DaviPlata", ignoreCase = true) -> Color(0xFFED1C24)
        capture.detectedBankName.contains("BBVA", ignoreCase = true) -> Color(0xFF004481)
        capture.detectedBankName.contains("Lulo", ignoreCase = true) -> Color(0xFF00E5FF)
        capture.detectedBankName.contains("Bogot", ignoreCase = true) -> Color(0xFF004884)
        capture.detectedBankName.contains("Falabella", ignoreCase = true) -> Color(0xFF00843D)
        capture.detectedBankName.contains("Colpatria", ignoreCase = true) -> Color(0xFFE31837)
        capture.detectedBankName.contains("Dale", ignoreCase = true) -> Color(0xFFFFCC00)
        capture.detectedBankName.contains("Rappi", ignoreCase = true) -> Color(0xFFFF441F)
        else -> Color(0xFF10B981)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E232B)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Bank badge, date, amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(bankAccentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                capture.detectedBankName.contains("Nequi", ignoreCase = true) ||
                                    capture.detectedBankName.contains("DaviPlata", ignoreCase = true) ||
                                    capture.detectedBankName.contains("Dale", ignoreCase = true) ||
                                    capture.detectedBankName.contains("Movii", ignoreCase = true) -> Icons.Default.Wallet
                                capture.lastFourDigits != null -> Icons.Default.CreditCard
                                else -> Icons.Default.AccountBalance
                            },
                            contentDescription = null,
                            tint = bankAccentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = capture.detectedBankName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF94A3B8)
                            )
                        )
                    }
                }

                // Amount
                Text(
                    text = (if (isIncome) "+ " else "- ") + formattedAmount,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isIncome) Color(0xFF34D399) else Color(0xFFF87171)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Merchant / Details
            Text(
                text = capture.merchantOrCounterparty,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )

            if (!capture.lastFourDigits.isNullOrBlank()) {
                Text(
                    text = "Tarjeta: •••• ${capture.lastFourDigits}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF94A3B8)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Account Selector & Category Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Bank / Account Selector Pill
                Surface(
                    onClick = { isAccountSelectorOpen = true },
                    color = Color(0xFF242C38),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF3B4657))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedAccount?.name
                                ?: capture.matchedAccountName
                                ?: "Elegir cuenta destino",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Cambiar cuenta",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Category Tag
                Surface(
                    color = Color(0xFF29303D),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "🏷️ " + (capture.matchedCategoryName ?: "General"),
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFCBD5E1)),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Symmetrical, no clipped text, robust layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Descartar (No cortado, padding controlado, maxLines=1)
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF4444)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Descartar",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFEF4444)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Descartar",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }

                // Botón Confirmar
                Button(
                    onClick = {
                        onConfirm(selectedAccount?.id, selectedAccount?.name)
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirmar",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Confirmar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }

    // Modal de Selección de Cuenta Bancaria Destino
    if (isAccountSelectorOpen) {
        AlertDialog(
            onDismissRequest = { isAccountSelectorOpen = false },
            title = {
                Text(
                    text = "Seleccionar Cuenta Destino",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            },
            text = {
                if (availableAccounts.isEmpty()) {
                    Text(
                        text = "No tienes cuentas registradas en KashApp todavía. Se usará la cuenta sugerida por el banco.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8))
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableAccounts, key = { it.id }) { acc ->
                            val isSelected = selectedAccount?.id == acc.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedAccount = acc
                                        isAccountSelectorOpen = false
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF064E3B) else Color(0xFF1F242D),
                                border = if (isSelected) BorderStroke(1.dp, Color(0xFF10B981)) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                selectedAccount = acc
                                                isAccountSelectorOpen = false
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = Color(0xFF10B981),
                                                unselectedColor = Color(0xFF64748B)
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = acc.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            )
                                            Text(
                                                text = "${acc.bankName} • ${acc.type}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF94A3B8)
                                                )
                                            )
                                        }
                                    }
                                    Text(
                                        text = "$${acc.balance.toLong()}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF38BDF8)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isAccountSelectorOpen = false }) {
                    Text("Cerrar", color = Color(0xFF10B981))
                }
            },
            containerColor = Color(0xFF181C24),
            shape = RoundedCornerShape(20.dp)
        )
    }
}
