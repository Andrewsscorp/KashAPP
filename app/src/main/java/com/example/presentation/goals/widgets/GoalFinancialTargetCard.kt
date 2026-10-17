package com.example.presentation.goals.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Payments
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
import com.example.presentation.goals.state.GoalFormState
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import java.util.Locale

@Composable
fun GoalFinancialTargetCard(
    state: GoalFormState,
    onToggleExpand: () -> Unit,
    onOpenKeyboard: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val progressRatio = (state.progressPercentage / 100f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "GoalProgressAnimation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("goal_financial_target_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Payments icon inside blue circular container + Title + Chevron
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2FE)), // Light Blue
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = KashBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Meta financiera",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashNavy
                        )
                        Text(
                            text = "Define cuánto necesitas y cuánto tienes actualmente.",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Icon(
                    imageVector = if (state.isFinancialTargetExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF64748B)
                )
            }

            AnimatedVisibility(visible = state.isFinancialTargetExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Row with 3 inputs: ¿Cuánto necesitas? *, ¿Cuánto tienes ahorrado? *, ¿Cuánto falta?
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. ¿Cuánto necesitas? * (Keyboard Trigger)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "¿Cuánto necesitas? *",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KashNavy
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            NumericDisplayField(
                                amount = state.targetAmount,
                                isHighlighted = state.activeKeyboardTarget == "TARGET_AMOUNT",
                                onClick = { onOpenKeyboard("TARGET_AMOUNT") },
                                testTag = "target_amount_trigger"
                            )
                        }

                        // 2. ¿Cuánto tienes ahorrado? * (Keyboard Trigger)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "¿Cuánto tienes ahorrado? *",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KashNavy
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            NumericDisplayField(
                                amount = state.savedAmount,
                                isHighlighted = state.activeKeyboardTarget == "SAVED_AMOUNT",
                                onClick = { onOpenKeyboard("SAVED_AMOUNT") },
                                testTag = "saved_amount_trigger"
                            )
                        }

                        // 3. ¿Cuánto falta? (Read-only reactive container)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "¿Cuánto falta?",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KashBlue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            ReadOnlyDeficitField(
                                amount = state.remainingAmount,
                                testTag = "remaining_amount_display"
                            )
                        }
                    }

                    // Progress Bar Row (Animated bar + percentage label)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Custom track and animated indicator
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(KashBlue)
                            )
                        }

                        Text(
                            text = "${state.progressPercentage}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashNavy,
                            modifier = Modifier.testTag("goal_progress_percent_label")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NumericDisplayField(
    amount: Double,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val formatted = String.format(Locale.US, "%,.0f", amount).replace(",", ".")
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        color = if (isHighlighted) Color(0xFFEFF6FF) else Color.White,
        border = BorderStroke(1.dp, if (isHighlighted) KashBlue else Color(0xFFCBD5E1))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = KashBlue
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = formatted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = KashNavy
            )
        }
    }
}

@Composable
private fun ReadOnlyDeficitField(
    amount: Double,
    testTag: String
) {
    val formatted = String.format(Locale.US, "%,.0f", amount).replace(",", ".")
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF0F9FF), // Light Sky / Blue read-only
        border = BorderStroke(1.dp, Color(0xFFBAE6FD))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = KashBlue
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = formatted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = KashBlue
            )
        }
    }
}
