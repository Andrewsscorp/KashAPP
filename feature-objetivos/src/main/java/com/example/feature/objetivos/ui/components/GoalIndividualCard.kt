package com.example.feature.objetivos.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.components.CurrencyText
import com.example.core.ui.components.KashCompactButton
import com.example.core.ui.components.KashCompactOutlinedButton
import com.example.feature.objetivos.domain.model.GoalDetailMetric
import com.example.feature.objetivos.domain.model.GoalStatus
import java.text.DecimalFormat

@Composable
fun GoalIndividualCard(
    goal: GoalDetailMetric,
    onCardClick: () -> Unit,
    onRegisterContribution: () -> Unit,
    onMarkCompleted: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
    isPrivacyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val moneyFormatter = remember { DecimalFormat("#,##0") }

    val cardBg = Color(0xFF0B172B)
    val cardBorder = Color(0xFF132B4F)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF8EA5C4)
    val accentCyan = Color(0xFF38BDF8)
    val primaryBlue = Color(0xFF007AFF)

    val (statusLabel, statusColor, statusBg) = when (goal.status) {
        GoalStatus.COMPLETED -> Triple("Completado", Color(0xFF00E676), Color(0xFF00C853).copy(alpha = 0.15f))
        GoalStatus.NEAR_COMPLETION -> Triple("Próximo a cumplir", Color(0xFF38BDF8), Color(0xFF0284C7).copy(alpha = 0.2f))
        GoalStatus.AT_RISK -> Triple("En riesgo", Color(0xFFEF4444), Color(0xFFEF4444).copy(alpha = 0.2f))
        GoalStatus.PAUSED -> Triple("Pausado", Color(0xFFF59E0B), Color(0xFFF59E0B).copy(alpha = 0.2f))
        GoalStatus.ARCHIVED -> Triple("Archivado", Color(0xFF64748B), Color(0xFF64748B).copy(alpha = 0.2f))
        GoalStatus.IN_PROGRESS -> Triple("En progreso", Color(0xFF38BDF8), Color(0xFF1E3A8A).copy(alpha = 0.35f))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("goal_item_${goal.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Icon + Title + Category + Badges + Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF2563EB).copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = accentCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = goal.name,
                                color = textPrimary,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${goal.categoryName} • ${goal.targetAccountName}",
                                color = textSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Badge Comprometido (si virtualPocketEnabled)
                    if (goal.virtualPocketEnabled) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0F2544),
                            border = BorderStroke(1.dp, Color(0xFF1E3A8A))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "Comprometido",
                                    color = Color(0xFF00E676),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusBg,
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = statusLabel,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    // 3-dot menu
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opciones",
                                tint = textSecondary
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Ver detalles") },
                                leadingIcon = { Icon(Icons.Default.TrendingUp, null) },
                                onClick = {
                                    showMenu = false
                                    onCardClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Registrar aporte") },
                                leadingIcon = { Icon(Icons.Default.Savings, null) },
                                onClick = {
                                    showMenu = false
                                    onRegisterContribution()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (goal.status == GoalStatus.COMPLETED) "Meta cumplida" else "Marcar como cumplido") },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, null) },
                                onClick = {
                                    showMenu = false
                                    onMarkCompleted()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (goal.isActive) "Archivar" else "Desarchivar") },
                                leadingIcon = { Icon(Icons.Default.Archive, null) },
                                onClick = {
                                    showMenu = false
                                    onToggleArchive()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar", color = Color(0xFFEF4444)) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444)) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar and amounts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.Bottom
                ) {
                    CurrencyText(
                        amountDouble = goal.savedAmount,
                        isPrivacyMode = isPrivacyMode,
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " / ",
                        color = textSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    CurrencyText(
                        amountDouble = goal.targetAmount,
                        isPrivacyMode = isPrivacyMode,
                        color = textSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${String.format("%.1f", goal.progressPercentage)}%",
                    color = accentCyan,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { (goal.progressPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = accentCyan,
                trackColor = Color(0xFF132B4F),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Deadline info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when {
                            goal.isOverdue -> "Vencido (${goal.deadlineDate})"
                            goal.daysRemaining != null -> "${goal.daysRemaining} días restantes"
                            !goal.deadlineDate.isNullOrBlank() -> goal.deadlineDate
                            else -> "Sin plazo definido"
                        },
                        color = if (goal.isOverdue) Color(0xFFEF4444) else textSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (goal.pendingAmount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Faltan ",
                            color = accentCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        CurrencyText(
                            amountDouble = goal.pendingAmount,
                            isPrivacyMode = isPrivacyMode,
                            color = accentCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Text(
                        text = "¡Meta alcanzada!",
                        color = Color(0xFF00E676),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Direct responsive action buttons (No delay, instantaneous response!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KashCompactOutlinedButton(
                    text = "Detalles",
                    onClick = onCardClick,
                    modifier = Modifier.weight(1f),
                    minHeight = 38.dp,
                    contentColor = accentCyan,
                    borderColor = Color(0xFF1E3A8A)
                )

                KashCompactButton(
                    text = "Aportar",
                    icon = Icons.Default.Savings,
                    onClick = onRegisterContribution,
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("btn_aportar_${goal.id}"),
                    minHeight = 38.dp,
                    containerColor = primaryBlue
                )
            }
        }
    }
}
