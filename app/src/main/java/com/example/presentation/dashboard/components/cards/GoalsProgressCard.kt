package com.example.presentation.dashboard.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.domain.models.GoalProgressDto
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import java.util.Locale

@Composable
fun GoalsProgressCard(
    goal: GoalProgressDto?,
    isPrivacyModeActive: Boolean,
    onViewAllClick: () -> Unit,
    onNewGoalClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("goals_progress_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mis objetivos",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = KashNavy
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (onNewGoalClick != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onNewGoalClick() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("add_goal_header_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Nuevo objetivo",
                                tint = KashBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Nuevo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = KashBlue
                            )
                        }
                    }

                    Text(
                        text = "Ver todos →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = KashBlue,
                        modifier = Modifier
                            .clickable { onViewAllClick() }
                            .testTag("view_all_goals_link")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (goal != null) {
                // Dynamically resolve icon, background and tint
                val (iconVector, iconBg, iconTint) = when {
                    goal.iconName.contains("school", ignoreCase = true) ||
                            goal.title.contains("Maestría", ignoreCase = true) ||
                            goal.category.contains("Educación", ignoreCase = true) -> {
                        Triple(Icons.Default.School, Color(0xFFEDE9FE), Color(0xFF8B5CF6)) // Birrete morado
                    }
                    goal.iconName.contains("car", ignoreCase = true) || goal.title.contains("Carro", ignoreCase = true) -> {
                        Triple(Icons.Default.DirectionsCar, Color(0xFFFEF3C7), Color(0xFFD97706))
                    }
                    goal.iconName.contains("home", ignoreCase = true) || goal.title.contains("Casa", ignoreCase = true) -> {
                        Triple(Icons.Default.Home, Color(0xFFDCFCE7), Color(0xFF16A34A))
                    }
                    goal.iconName.contains("flight", ignoreCase = true) || goal.title.contains("Viaje", ignoreCase = true) -> {
                        Triple(Icons.Default.Flight, Color(0xFFE0F2FE), Color(0xFF0284C7))
                    }
                    goal.iconName.contains("laptop", ignoreCase = true) || goal.title.contains("Laptop", ignoreCase = true) -> {
                        Triple(Icons.Default.LaptopMac, Color(0xFF0F172A), Color(0xFF38BDF8))
                    }
                    else -> {
                        Triple(Icons.Default.Savings, Color(0xFFEDE9FE), Color(0xFF8B5CF6))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { if (onNewGoalClick != null) onNewGoalClick() else onViewAllClick() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Visual Icon Badge (e.g. Birrete morado)
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = goal.title,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            // Infinite Marquee for long goal names
                            Text(
                                text = goal.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = KashNavy,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            val currStr = if (isPrivacyModeActive) "$ •••••" else "$ " + String.format(Locale.GERMANY, "%,.0f", goal.currentAmount)
                            val targetStr = if (isPrivacyModeActive) "$ •••••" else "$ " + String.format(Locale.GERMANY, "%,.0f", goal.targetAmount)

                            Text(
                                text = "$currStr / $targetStr",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { (goal.percentage / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = iconTint,
                                trackColor = Color(0xFFE2E8F0)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Detalle",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${goal.percentage}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconTint
                        )
                    }
                }
            } else {
                // Empty state with quick add CTA
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Aún no tienes objetivos de ahorro activos.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onNewGoalClick?.invoke() ?: onViewAllClick() },
                        colors = ButtonDefaults.buttonColors(containerColor = KashNavy),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Crear mi primer objetivo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
