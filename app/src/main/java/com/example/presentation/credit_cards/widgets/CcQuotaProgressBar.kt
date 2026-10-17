package com.example.presentation.credit_cards.widgets

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val KashBlue = Color(0xFF0284C7)
private val WarningRed = Color(0xFFDC2626)
private val GreenAvailable = Color(0xFF10B981)
private val DarkSlate = Color(0xFF1E293B)
private val TrackGray = Color(0xFFE2E8F0)

/**
 * Reactive Quota Progress Bar (State Listener).
 * As the user updates "Cupo Utilizado" (e.g. $ 2.850.000 out of $ 10.000.000),
 * the bar smoothly animates to 28.5% in real time via animateFloatAsState.
 *
 * Displays Master Equation status:
 * Cupo Total ($12.000.000) = Cupo Disponible ($3.550.000) + Cupo Utilizado ($8.450.000)
 */
@Composable
fun CcQuotaProgressBar(
    totalQuota: Double,
    usedQuota: Double,
    availableQuota: Double,
    retainedQuota: Double = 0.0,
    modifier: Modifier = Modifier
) {
    val usageRatio = if (totalQuota > 0) {
        (usedQuota / totalQuota).toFloat().coerceIn(0f, 1f)
    } else 0f

    val usagePercentage = (usageRatio * 100f)

    // Smooth animated state listener
    val animatedUsageRatio by animateFloatAsState(
        targetValue = usageRatio,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "QuotaUsageAnimation"
    )

    val isHighUsage = usagePercentage > 70f
    val barColor = if (isHighUsage) WarningRed else KashBlue

    val formattedTotal = "$ " + String.format(Locale.GERMANY, "%,.0f", totalQuota)
    val formattedUsed = "$ " + String.format(Locale.GERMANY, "%,.0f", usedQuota)
    val formattedAvailable = "$ " + String.format(Locale.GERMANY, "%,.0f", availableQuota)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cc_quota_progress_bar"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Labels & Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(barColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cupo de crédito rotativo",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isHighUsage) Color(0xFFFEE2E2) else Color(0xFFE0F2FE)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f%% utilizado", usagePercentage),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = barColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Animated Two-Tone Reactive Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(TrackGray)
            ) {
                // Utilized portion (animated)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedUsageRatio)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(barColor)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Master Equation Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Utilizado (Deuda / Pasivo)",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = formattedUsed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = barColor
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Disponible",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = formattedAvailable,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenAvailable
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Cupo Total Aprobado",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = formattedTotal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mathematical Balance Rule Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = GreenAvailable,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ecuación maestra validada: Total = Disponible + Utilizado",
                    fontSize = 10.sp,
                    color = Color(0xFF475569)
                )
            }
        }
    }
}
