package com.example.presentation.budget.widgets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.budget.state.BudgetFormState
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import java.util.Locale

/**
 * Real-time Mathematical Summary Card & Zero-Based Budgeting Engine.
 * 
 * Displays:
 * - Total (A): Main ceiling
 * - Asignado (B): Sum of all envelope allocations
 * - Sin asignar (C): A - B
 * - Progress bar: (B / A) * 100
 * - Overbudget blocking & Zero-based balance certification
 */
@Composable
fun BudgetSummaryCard(
    state: BudgetFormState,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (state.progressPercentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 350),
        label = "budget_progress_anim"
    )

    val progressColor by animateColorAsState(
        targetValue = when {
            state.isOverbudget -> Color(0xFFEF4444)
            state.isZeroBasedBalanced -> Color(0xFF10B981)
            else -> KashBlue
        },
        animationSpec = tween(durationMillis = 300),
        label = "budget_color_anim"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("budget_summary_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(
            1.dp,
            if (state.isOverbudget) Color(0xFFEF4444) else Color(0xFF334155)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Card Title & Percentage Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Resumen Matemático del Presupuesto",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (state.isOverbudget) Color(0xFFEF4444).copy(alpha = 0.2f) else if (state.isZeroBasedBalanced) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF007BFF).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (state.isOverbudget) Color(0xFFEF4444) else if (state.isZeroBasedBalanced) Color(0xFF10B981) else Color(0xFF38BDF8))
                ) {
                    Text(
                        text = "${String.format(Locale.GERMANY, "%.1f", state.progressPercentage)}% Asignado",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (state.isOverbudget) Color(0xFFF87171) else if (state.isZeroBasedBalanced) Color(0xFF34D399) else Color(0xFF38BDF8),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3-Column Metrics (Total A, Asignado B, Sin Asignar C)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Column 1: Total (A)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Techo Total (A)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$ ${String.format(Locale.GERMANY, "%,.0f", state.totalAmount)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                // Column 2: Asignado (B)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Asignado (B)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$ ${String.format(Locale.GERMANY, "%,.0f", state.allocatedAmount)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = progressColor
                    )
                }

                // Column 3: Sin asignar (C)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Sin asignar (C)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (state.unassignedAmount < 0) {
                            "-$ ${String.format(Locale.GERMANY, "%,.0f", Math.abs(state.unassignedAmount))}"
                        } else {
                            "$ ${String.format(Locale.GERMANY, "%,.0f", state.unassignedAmount)}"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (state.isOverbudget) Color(0xFFEF4444) else if (state.isZeroBasedBalanced) Color(0xFF10B981) else Color(0xFF38BDF8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Smooth Animated Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .testTag("budget_progress_bar"),
                color = progressColor,
                trackColor = Color(0xFF0F172A),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Zero-Based Budgeting & Overbudget Rule Banner
            val bannerBorder = if (state.isOverbudget) Color(0xFFEF4444).copy(alpha = 0.5f) else if (state.isZeroBasedBalanced) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF007BFF).copy(alpha = 0.4f)

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, bannerBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (state.isOverbudget) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = progressColor,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = state.ruleTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isOverbudget) Color(0xFF991B1B) else if (state.isZeroBasedBalanced) Color(0xFF166534) else Color(0xFF075985)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = state.ruleMessage,
                            fontSize = 12.sp,
                            color = if (state.isOverbudget) Color(0xFFB91C1C) else if (state.isZeroBasedBalanced) Color(0xFF15803D) else Color(0xFF0369A1),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
