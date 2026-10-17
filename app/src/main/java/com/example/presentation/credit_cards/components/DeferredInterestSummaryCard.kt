package com.example.presentation.credit_cards.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.credit_cards.model.formatCardMoney
import com.example.ui.theme.KashBlue

/**
 * Financial Analysis and Deferred Interest Simulation Card.
 * Displays responsive 2x2 metric grid (Capital, Monthly Fee, Total Interest, Total Debt),
 * and a visual proportion bar comparing Capital vs. Interest charges.
 */
@Composable
fun DeferredInterestSummaryCard(
    numericAmount: Double,
    installmentsCount: Int,
    interestRateEa: Double,
    monthlyPaymentEstimated: Double,
    totalInterestCalculated: Double,
    totalDebtToPayCalculated: Double,
    selectedCurrency: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Icon + Title + Rate Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = KashBlue, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ANÁLISIS FINANCIERO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (installmentsCount == 1) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                ) {
                    Text(
                        text = if (installmentsCount == 1) "0% Interés" else "$interestRateEa% E.A.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (installmentsCount == 1) Color(0xFF166534) else Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFE2E8F0))

            // Responsive 2x2 Metric Grid with individual container cells
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: Capital & Cuota Mensual
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cell 1: Capital
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Capital Compra",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val capStr = formatCardMoney(numericAmount, selectedCurrency)
                            Text(
                                text = capStr,
                                fontSize = if (capStr.length > 15) 12.sp else 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Cell 2: Cuota Mensual
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Cuota ($installmentsCount)",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val monthlyStr = "${formatCardMoney(monthlyPaymentEstimated, selectedCurrency)}/mes"
                            Text(
                                text = monthlyStr,
                                fontSize = if (monthlyStr.length > 15) 12.sp else 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = KashBlue,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Row 2: Intereses & Deuda Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cell 3: Intereses
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Total Intereses",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val intStr = if (installmentsCount == 1) "$ 0 (0%)" else "+ ${formatCardMoney(totalInterestCalculated, selectedCurrency)}"
                            Text(
                                text = intStr,
                                fontSize = if (intStr.length > 15) 12.sp else 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (installmentsCount == 1) Color(0xFF16A34A) else Color(0xFFD97706),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Cell 4: Deuda Total
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "TOTAL A PAGAR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E40AF),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val totStr = formatCardMoney(totalDebtToPayCalculated, selectedCurrency)
                            Text(
                                text = totStr,
                                fontSize = if (totStr.length > 15) 13.sp else 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E3A8A),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Visual Proportion Bar (Capital vs. Interest)
            if (installmentsCount > 1 && numericAmount > 0 && totalDebtToPayCalculated > 0) {
                val capitalRatio = (numericAmount / totalDebtToPayCalculated).toFloat().coerceIn(0.1f, 1f)
                val interestRatio = (totalInterestCalculated / totalDebtToPayCalculated).toFloat().coerceIn(0f, 0.9f)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(capitalRatio)
                                .fillMaxHeight()
                                .background(KashBlue)
                        )
                        if (interestRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(interestRatio)
                                    .fillMaxHeight()
                                    .background(Color(0xFFF59E0B))
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(KashBlue))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Capital (${(capitalRatio * 100).toInt()}%)", fontSize = 10.sp, color = Color(0xFF475569))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Intereses (${(interestRatio * 100).toInt()}%)", fontSize = 10.sp, color = Color(0xFF475569))
                        }
                    }
                }
            }
        }
    }
}
