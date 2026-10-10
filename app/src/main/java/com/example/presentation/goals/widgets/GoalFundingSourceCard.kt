package com.example.presentation.goals.widgets

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.goals.state.GoalFormState
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import java.util.Locale

@Composable
fun GoalFundingSourceCard(
    state: GoalFormState,
    onToggleExpand: () -> Unit,
    onSourceAccountChanged: (String, String) -> Unit,
    onContributionMethodChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var sourceDropdownExpanded by remember { mutableStateOf(false) }
    val isSameAccount = state.sourceAccountId.isNotBlank() && state.sourceAccountId == state.targetAccountId

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("goal_funding_source_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Accordion
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
                            .background(Color(0xFFEDE9FE)), // Soft Violet
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "5. Fuente del objetivo",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashNavy
                        )
                        Text(
                            text = "Aporte desde ${state.sourceAccountName.ifBlank { "Bancolombia ••••• 1234" }}",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Icon(
                    imageVector = if (state.isFundingSourceExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (state.isFundingSourceExpanded) "Colapsar" else "Expandir",
                    tint = Color(0xFF64748B)
                )
            }

            AnimatedVisibility(visible = state.isFundingSourceExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = "Cuenta de origen (De dónde saldrá el aporte)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { sourceDropdownExpanded = true }
                                .testTag("source_account_picker"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            color = Color(0xFFF8FAFC)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = KashNavy,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = state.sourceAccountName.ifBlank { state.targetAccountName.ifBlank { "Bancolombia ••••• 1234" } },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = KashNavy
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = sourceDropdownExpanded,
                            onDismissRequest = { sourceDropdownExpanded = false }
                        ) {
                            if (state.availableAccounts.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Bancolombia ••••• 1234") },
                                    onClick = {
                                        onSourceAccountChanged("acc_bancolombia", "Bancolombia ••••• 1234")
                                        sourceDropdownExpanded = false
                                    }
                                )
                            } else {
                                state.availableAccounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(acc.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(
                                                    "${acc.accountNumberMasked ?: ""} • Saldo: $ ${String.format(Locale.GERMANY, "%,.0f", acc.availableBalance)}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        },
                                        onClick = {
                                            val display = "${acc.name} ${acc.accountNumberMasked ?: ""}".trim()
                                            onSourceAccountChanged(acc.id, display)
                                            sourceDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Notice if Source and Target are same account (Virtual Pocket)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF5F3FF),
                        border = BorderStroke(1.dp, Color(0xFFDDD6FE))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Bolsillo Virtual Interno: Al ser la misma cuenta destino, KashApp separa los fondos sin mover dinero fuera de tu banco ni comisiones.",
                                fontSize = 11.sp,
                                color = Color(0xFF6D28D9),
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Método de aporte
                    Text(
                        text = "Método de aporte",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Transferencia", "Débito automático", "Nómina").forEach { method ->
                            val isSelected = state.contributionMethod.equals(method, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onContributionMethodChanged(method) },
                                label = { Text(method, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF8B5CF6),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFF1F5F9),
                                    labelColor = Color(0xFF64748B)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                border = null,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
