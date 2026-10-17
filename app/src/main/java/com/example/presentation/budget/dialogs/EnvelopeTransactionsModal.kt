package com.example.presentation.budget.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.BudgetItemEntity
import com.example.data.local.entity.TransactionEntity
import java.text.DecimalFormat

@Composable
fun EnvelopeTransactionsModal(
    envelope: BudgetItemEntity,
    transactions: List<TransactionEntity>,
    onDismiss: () -> Unit
) {
    val moneyFormatter = remember { DecimalFormat("#,##0") }
    val cardBg = Color(0xFF0B172B)
    val cardBorder = Color(0xFF132B4F)
    val primaryBlue = Color(0xFF007AFF)
    val accentCyan = Color(0xFF38BDF8)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF8EA5C4)

    // Filter transactions matching this envelope category
    val filteredTx = transactions.filter {
        (it.categoryId == envelope.categoryId || it.categoryName.equals(envelope.categoryName, ignoreCase = true)) &&
                it.type.equals("EXPENSE", ignoreCase = true)
    }.sortedByDescending { it.date }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
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
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = accentCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(envelope.categoryName, color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Consumo real: $ ${moneyFormatter.format(envelope.spentAmount)}", color = textSecondary, fontSize = 11.5.sp)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Summary Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF061120),
                    border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Asignado en sobre", color = textSecondary, fontSize = 11.sp)
                            Text("$ ${moneyFormatter.format(envelope.allocatedAmount)}", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Disponible", color = textSecondary, fontSize = 11.sp)
                            val remaining = envelope.allocatedAmount - envelope.spentAmount
                            Text(
                                "$ ${moneyFormatter.format(remaining)}",
                                color = if (remaining < 0) Color(0xFFEF4444) else Color(0xFF00E676),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Movimientos registrados (${filteredTx.size})", color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                if (filteredTx.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay gastos registrados en esta categoría", color = textSecondary, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredTx, key = { it.id }) { tx ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF061120),
                                border = BorderStroke(1.dp, Color(0xFF132B4F)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(tx.description ?: tx.categoryName, color = textPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                        Text("${tx.date} • ${tx.accountName}", color = textSecondary, fontSize = 10.5.sp)
                                    }
                                    Text("-$ ${moneyFormatter.format(tx.amount)}", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
