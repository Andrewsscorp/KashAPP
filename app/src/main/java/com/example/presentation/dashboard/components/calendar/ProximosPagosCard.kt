package com.example.presentation.dashboard.components.calendar

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.dashboard.model.ScheduledPayment
import com.example.util.MoneyFormatter
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal
import java.time.LocalDate

/**
 * "CALENDARIO — Próximos pagos" Section (Image 2 top).
 */
@Composable
fun ProximosPagosCard(
    payments: ImmutableList<ScheduledPayment>,
    onCalendarIconClick: () -> Unit = {},
    isPrivacyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CALENDARIO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF007BFF),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Próximos pagos",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }

            // Calendar Icon Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE0F2FE))
                    .clickable { onCalendarIconClick() }
                    .testTag("calendario_icon_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Calendario",
                    tint = Color(0xFF007BFF),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card Container
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCalendarIconClick() }
        ) {
            if (payments.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Sin pagos programados",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No tienes pagos ni cobros pendientes para los próximos días.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    payments.take(4).forEachIndexed { index, payment ->
                        PaymentRowItem(
                            payment = payment,
                            isPrivacyMode = isPrivacyMode
                        )

                        if (index < payments.take(4).size - 1) {
                            HorizontalDivider(
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.padding(start = 68.dp, end = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentRowItem(
    payment: ScheduledPayment,
    isPrivacyMode: Boolean
) {
    val (icon, iconBg, iconTint, subtext) = when {
        payment.isIncome -> Tuple4(Icons.Default.Payments, Color(0xFFFEF3C7), Color(0xFFD97706), "30 Sep · Ingreso")
        payment.entityName.contains("netflix", ignoreCase = true) -> Tuple4(Icons.Default.PlayArrow, Color(0xFFFEE2E2), Color(0xFFEF4444), "3 Oct · Entretenimiento")
        payment.entityName.contains("tarjeta", ignoreCase = true) -> Tuple4(Icons.Default.CreditCard, Color(0xFFF3E8FF), Color(0xFF8B5CF6), "8 Oct · Pago mínimo")
        payment.entityName.contains("arriendo", ignoreCase = true) || payment.entityName.contains("hogar", ignoreCase = true) ->
            Tuple4(Icons.Default.Home, Color(0xFFE0F2FE), Color(0xFF007BFF), "15 Oct · Vivienda")
        else -> Tuple4(Icons.Default.CalendarMonth, Color(0xFFE0F2FE), Color(0xFF007BFF), "Próximo vencimiento")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = payment.entityName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }

        Text(
            text = if (isPrivacyMode) "••••••" else if (payment.isIncome) "+$ ${MoneyFormatter.format(payment.amount, false).removePrefix("$").trim()}" else MoneyFormatter.format(payment.amount, false),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (payment.isIncome) Color(0xFF10B981) else Color(0xFF0F172A)
        )
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
