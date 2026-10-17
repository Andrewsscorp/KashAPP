package com.example.presentation.dashboard.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.CreditCardSummaryDto
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import java.util.Locale

/**
 * Highly responsive Credit Card component.
 * Uses BoxWithConstraints to automatically adapt to any screen size (Compact Phone, Foldable, Tablet, Desktop)
 * providing proportional typography, layout reflow, and elegant virtual card visuals.
 */
@Composable
fun CreditCardPreviewCard(
    card: CreditCardSummaryDto?,
    isPrivacyModeActive: Boolean,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("credit_card_preview_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val containerWidth = maxWidth
            val isVeryNarrow = containerWidth < 330.dp
            val isExpanded = containerWidth >= 480.dp

            Column(modifier = Modifier.fillMaxWidth()) {
                // Header: Title & Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = KashBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tarjetas de crédito",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = KashNavy,
                                fontSize = if (isVeryNarrow) 14.sp else 16.sp
                            )
                        )
                    }

                    Text(
                        text = "Ver todas →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = KashBlue,
                        modifier = Modifier
                            .clickable { onViewAllClick() }
                            .padding(vertical = 4.dp)
                            .testTag("view_all_cards_link")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (card != null) {
                    val usedFormatted = if (isPrivacyModeActive) "$ •••••" else "$ " + String.format(Locale.GERMANY, "%,.0f", card.usedLimit)
                    val totalFormatted = if (isPrivacyModeActive) "$ •••••" else "$ " + String.format(Locale.GERMANY, "%,.0f", card.totalLimit)
                    val availableLimit = maxOf(0.0, card.totalLimit - card.usedLimit)
                    val availableFormatted = if (isPrivacyModeActive) "$ •••••" else "$ " + String.format(Locale.GERMANY, "%,.0f", availableLimit)

                    val cardColor = Color(card.cardColorHex)
                    val cardGradient = Brush.linearGradient(
                        listOf(cardColor, cardColor.copy(alpha = 0.85f), Color(0xFF1E293B))
                    )

                    if (isExpanded) {
                        // Wide / Tablet Layout: Proportional Virtual Card on the Left + Full Metrics on the Right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Realistic ISO 7810 Virtual Card
                            Box(
                                modifier = Modifier
                                    .width(160.dp)
                                    .aspectRatio(1.586f)
                                    .shadow(6.dp, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardGradient)
                                    .padding(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = card.cardName,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Nfc,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    // Chip
                                    Box(
                                        modifier = Modifier
                                            .size(width = 20.dp, height = 14.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFFFBBF24))
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Text(
                                            text = "•••• 4289",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "12/28",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 8.sp
                                        )
                                    }
                                }
                            }

                            // Metrics Column
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Cupo utilizado",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = usedFormatted,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = KashNavy
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (card.usagePercentage > 70) Color(0xFFFEE2E2) else Color(0xFFEFF6FF)
                                    ) {
                                        Text(
                                            text = "${card.usagePercentage}% uso",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (card.usagePercentage > 70) Color(0xFFDC2626) else KashBlue,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { (card.usagePercentage / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (card.usagePercentage > 70) Color(0xFFDC2626) else cardColor,
                                    trackColor = Color(0xFFE2E8F0)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Disponible: $availableFormatted",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF10B981)
                                    )
                                    Text(
                                        text = "Corte: ${card.cutoffDate} • Pago: ${card.paymentDate}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    } else {
                        // Phone / Compact Layout: Fluid and wrap-safe
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mini Virtual Card
                            Box(
                                modifier = Modifier
                                    .size(width = if (isVeryNarrow) 52.dp else 64.dp, height = if (isVeryNarrow) 34.dp else 40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cardGradient)
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = card.cardName,
                                        color = Color.White,
                                        fontSize = if (isVeryNarrow) 8.sp else 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(width = 11.dp, height = 7.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFFFBBF24))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                if (isVeryNarrow) {
                                    // Stacked amounts on narrow screens so text never clips
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = usedFormatted,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = KashNavy
                                        )
                                        Text(
                                            text = "${card.usagePercentage}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (card.usagePercentage > 70) Color(0xFFDC2626) else KashBlue
                                        )
                                    }
                                    Text(
                                        text = "de $totalFormatted",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$usedFormatted / $totalFormatted",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = KashNavy,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${card.usagePercentage}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (card.usagePercentage > 70) Color(0xFFDC2626) else KashBlue
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                LinearProgressIndicator(
                                    progress = { (card.usagePercentage / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(2.5.dp)),
                                    color = if (card.usagePercentage > 70) Color(0xFFDC2626) else cardColor,
                                    trackColor = Color(0xFFE2E8F0)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Corte: ${card.cutoffDate} • Pago: ${card.paymentDate}",
                                        fontSize = if (isVeryNarrow) 9.sp else 10.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No tienes tarjetas de crédito o deudas registradas.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onViewAllClick,
                            colors = ButtonDefaults.buttonColors(containerColor = KashNavy),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Registrar tarjeta o deuda", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
