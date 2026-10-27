package com.example.feature.reconciliation.presentation

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.components.KashResponsiveDialog
import com.example.data.local.entity.AccountEntity
import java.text.NumberFormat
import java.util.Locale

/**
 * iPhone / iOS styled Responsive Reconciliation Audit History Modal.
 * Adapted for compact and small screens with frosted glass cards,
 * status badges, and full SOX/GAAP audit trail metadata.
 */
@Composable
fun ReconciliationHistoryDialog(
    account: AccountEntity,
    viewModel: ReconciliationViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
        }
    }

    val kashAppBlue = Color(0xFF0284C7)
    val kashAppLightBlue = Color(0xFF38BDF8)
    val glassBorderColor = Color(0xFF38BDF8).copy(alpha = 0.22f)

    KashResponsiveDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        borderColor = glassBorderColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            // Header: Title + Close Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(kashAppBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = kashAppLightBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Historial de Conciliación",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = account.name,
                            fontSize = 11.5.sp,
                            color = kashAppLightBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body: Content list or Empty/Loading state
            if (uiState.isLoadingHistory) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = kashAppLightBlue)
                }
            } else if (uiState.historyLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No hay registros de conciliación previos",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.historyLogs, key = { it.id }) { log ->
                        val (statusBadgeColor, statusLabel) = when (log.status) {
                            "BALANCED" -> Pair(Color(0xFF10B981), "Cuadrada (0 COP)")
                            "ADJUSTED_SURPLUS" -> Pair(Color(0xFF38BDF8), "Superávit (+)")
                            else -> Pair(Color(0xFFF59E0B), "Déficit (-)")
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1E293B).copy(alpha = 0.85f))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${log.reconciliationDate} • ${log.reconciliationTime}",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFCBD5E1)
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = statusBadgeColor.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, statusBadgeColor.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = statusLabel,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp,
                                            color = statusBadgeColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Anterior: ${currencyFormat.format(log.previousSystemBalance)}",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = "Real: ${currencyFormat.format(log.reconciledRealBalance)}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                if (kotlin.math.abs(log.discrepancyAmount) > 0.001) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Ajuste Contable:",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                        Text(
                                            text = (if (log.discrepancyAmount > 0) "+" else "") + currencyFormat.format(log.discrepancyAmount),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusBadgeColor
                                        )
                                    }
                                }

                                if (!log.notes.isNullOrBlank()) {
                                    Text(
                                        text = "Nota: ${log.notes}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                Text(
                                    text = "Auditado por: ${log.reconciledByUserName} (${log.deviceModel})",
                                    fontSize = 9.5.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom action: Cerrar
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = kashAppBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(
                    text = "Cerrar",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
