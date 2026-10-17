package com.example.presentation.dashboard.components.tables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.TransactionItemDto
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashCyan
import com.example.ui.theme.KashNavy
import java.util.Locale

@Composable
fun RecentTransactionsList(
    transactions: List<TransactionItemDto>,
    isPrivacyModeActive: Boolean,
    onTransactionClick: (TransactionItemDto) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filterType by remember { mutableStateOf("Todos") }
    var filterMenuExpanded by remember { mutableStateOf(false) }

    val filteredList = when (filterType) {
        "Solo Gastos" -> transactions.filter { !it.isIncome && !it.isTransfer }
        "Solo Ingresos" -> transactions.filter { it.isIncome && !it.isTransfer }
        "Solo Transferencias" -> transactions.filter { it.isTransfer }
        else -> transactions
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recent_transactions_table_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header Row: Title, Ver todos, Search, Filter Dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Últimos movimientos",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = KashNavy
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ver todos →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = KashBlue,
                        modifier = Modifier
                            .clickable { onViewAllClick() }
                            .padding(end = 8.dp)
                            .testTag("view_all_transactions_link")
                    )

                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Filter Dropdown: Todos, Solo Gastos, Solo Ingresos
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                .clickable { filterMenuExpanded = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("transactions_filter_dropdown"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = filterType,
                                fontSize = 11.sp,
                                color = KashNavy,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Filtro",
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = filterMenuExpanded,
                            onDismissRequest = { filterMenuExpanded = false }
                        ) {
                            listOf("Todos", "Solo Gastos", "Solo Ingresos", "Solo Transferencias").forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt, fontSize = 12.sp) },
                                    onClick = {
                                        filterType = opt
                                        filterMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isCompactPhone = maxWidth < 520.dp

                if (isCompactPhone) {
                    // Mobile-Optimized List View (Adaptive for all phone widths)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredList.forEach { tx ->
                            val amountStr = if (isPrivacyModeActive) {
                                "$ •••••"
                            } else {
                                val formatted = String.format(Locale.GERMANY, "%,.0f", tx.amount)
                                when {
                                    tx.isTransfer -> "⇄ $formatted"
                                    tx.isIncome -> "+$ $formatted"
                                    else -> "-$ $formatted"
                                }
                            }
                            val amountColor = when {
                                tx.isTransfer -> Color(0xFF0284C7)
                                tx.isIncome -> Color(0xFF10B981)
                                else -> Color(0xFFEF4444)
                            }
                            val dotColor = when {
                                tx.isTransfer -> Color(0xFF0284C7)
                                tx.isIncome -> Color(0xFF10B981)
                                else -> Color(0xFFF97316)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onTransactionClick(tx) }
                                    .background(Color(0xFFF8FAFC))
                                    .padding(12.dp)
                                    .testTag("tx_row_${tx.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(dotColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(
                                            text = tx.description,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = KashNavy,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${tx.dateFormatted} • ${tx.category} • ${tx.account}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B),
                                            maxLines = 1
                                        )
                                    }
                                }

                                Text(
                                    text = amountStr,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = amountColor,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Expanded Table View (for wide screens / tablets)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Table Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Fecha", fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.weight(1.1f))
                            Text(text = "Descripción", fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.weight(1.3f))
                            Text(text = "Categoría", fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.weight(1.2f))
                            Text(text = "Cuenta", fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.weight(1.3f))
                            Text(text = "Importe", fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.weight(1.2f))
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                        filteredList.forEach { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onTransactionClick(tx) }
                                    .padding(vertical = 10.dp, horizontal = 2.dp)
                                    .testTag("tx_row_${tx.id}"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tx.dateFormatted,
                                    fontSize = 12.sp,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.weight(1.1f)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (tx.isIncome) Color(0xFF10B981) else Color(0xFF0284C7))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tx.description,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KashNavy
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, Color(0xFF94A3B8), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = tx.category,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Text(
                                    text = tx.account,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.weight(1.3f)
                                )

                                val amountStr = if (isPrivacyModeActive) {
                                    "$ •••••"
                                } else {
                                    val formatted = String.format(Locale.GERMANY, "%,.0f", tx.amount)
                                    when {
                                        tx.isTransfer -> "⇄ $formatted"
                                        tx.isIncome -> "+$ $formatted"
                                        else -> "-$ $formatted"
                                    }
                                }
                                Text(
                                    text = amountStr,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        tx.isTransfer -> Color(0xFF0284C7)
                                        tx.isIncome -> Color(0xFF10B981)
                                        else -> Color(0xFFEF4444)
                                    },
                                    modifier = Modifier.weight(1.2f)
                                )
                            }
                            HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 0.8.dp)
                        }
                    }
                }
            }
        }
    }
}
