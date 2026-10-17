package com.example.presentation.dashboard.components.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.dashboard.model.ScheduledPayment
import com.example.util.MoneyFormatter
import kotlinx.collections.immutable.ImmutableList
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * 9. UpcomingPaymentsCard.kt: Predictive upcoming bills and subscriptions with urgent badges.
 */
@Composable
fun UpcomingPaymentsCard(
    payments: ImmutableList<ScheduledPayment>,
    onPaymentClick: (ScheduledPayment) -> Unit = {},
    isPrivacyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("upcoming_payments_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Próximos Pagos & Vencimientos",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (payments.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "No tienes pagos programados próximos a vencer",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val today = LocalDate.now()
                val dateFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es", "CO"))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    payments.take(4).forEachIndexed { index, payment ->
                        val daysUntilDue = ChronoUnit.DAYS.between(today, payment.dueDate)
                        val isUrgent = daysUntilDue in 0..3

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPaymentClick(payment) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = payment.entityName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
                                    )
                                    if (isUrgent) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = Color(0xFFFEE2E2),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = if (daysUntilDue == 0L) "Vence hoy" else "Vence en $daysUntilDue d",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFDC2626),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = payment.dueDate.format(dateFormatter),
                                    fontSize = 11.sp,
                                    color = if (isUrgent) Color(0xFFDC2626) else Color(0xFF64748B)
                                )
                            }

                            Text(
                                text = MoneyFormatter.format(payment.amount, isPrivacyMode),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (payment.isIncome) Color(0xFF10B981) else Color(0xFF0F172A)
                            )
                        }

                        if (index < payments.take(4).size - 1) {
                            HorizontalDivider(color = Color(0xFFF8FAFC))
                        }
                    }
                }
            }
        }
    }
}
