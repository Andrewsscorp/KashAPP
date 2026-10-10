package com.example.presentation.dashboard.components.cards

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.UpcomingBillDto
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import java.util.Locale

@Composable
fun UpcomingBillsCard(
    bills: List<UpcomingBillDto>,
    isPrivacyModeActive: Boolean,
    onBillClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("upcoming_bills_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header Row: Title, Ver todos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Próximos pagos",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = KashNavy
                    )
                )

                Text(
                    text = "Ver todos →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = KashBlue,
                    modifier = Modifier
                        .clickable { onViewAllClick() }
                        .testTag("view_all_bills_link")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (bills.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    bills.forEach { bill ->
                        val (iconVector, iconBg, iconTint) = when (bill.iconType) {
                            "netflix" -> Triple(Icons.Default.Movie, Color(0xFFFEE2E2), Color(0xFFEF4444))
                            "card" -> Triple(Icons.Default.CreditCard, Color(0xFFE0F2FE), Color(0xFF0284C7))
                            "home" -> Triple(Icons.Default.Home, Color(0xFFEDE9FE), Color(0xFF8B5CF6))
                            else -> Triple(Icons.Default.Payments, Color(0xFFFEF3C7), Color(0xFFF59E0B))
                        }

                        // Warning color if due in <= 3 days
                        val dateTextColor = if (bill.daysRemaining <= 3) Color(0xFFEA580C) else Color(0xFF64748B)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onBillClick(bill.id) }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                                .testTag("bill_item_${bill.id}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(iconBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = bill.title,
                                        tint = iconTint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bill.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KashNavy,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (bill.isPaid) "Pagada ✓" else "${bill.dueDateFormatted}${if (bill.daysRemaining <= 3) " (Vence pronto)" else ""}",
                                        fontSize = 11.sp,
                                        color = if (bill.isPaid) Color(0xFF10B981) else dateTextColor,
                                        fontWeight = if (bill.daysRemaining <= 3) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }

                            val formattedAmount = if (isPrivacyModeActive) {
                                "$ •••••"
                            } else {
                                val sign = if (bill.isIncome) "+$ " else "$ "
                                sign + String.format(Locale.GERMANY, "%,.0f", bill.amount)
                            }

                            Text(
                                text = formattedAmount,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (bill.isIncome) Color(0xFF10B981) else KashNavy
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "No tienes pagos pendientes ni obligaciones registradas.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}
