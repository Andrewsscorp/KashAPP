package com.example.presentation.goals.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
fun GoalSavingPlanCard(
    state: GoalFormState,
    onToggleExpand: () -> Unit,
    onOpenKeyboard: (String) -> Unit,
    onFrequencyChanged: (String) -> Unit,
    onAutoContributeChanged: (Boolean) -> Unit,
    onVirtualPocketChanged: (Boolean) -> Unit,
    onTargetAccountChanged: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    val formattedContribution = "$ " + String.format(Locale.GERMANY, "%,.0f", state.monthlyContribution)
    val formattedSaved = "$ " + String.format(Locale.GERMANY, "%,.0f", state.savedAmount)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("goal_saving_plan_card"),
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
                            .background(Color(0xFFDCFCE7)), // Soft Emerald
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "4. Plan de ahorro",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashNavy
                        )
                        Text(
                            text = "$formattedContribution / ${state.frequency.lowercase()}",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Icon(
                    imageVector = if (state.isSavingPlanExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (state.isSavingPlanExpanded) "Colapsar" else "Expandir",
                    tint = Color(0xFF64748B)
                )
            }

            AnimatedVisibility(visible = state.isSavingPlanExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    // Aporte mensual auto-calculado
                    Text(
                        text = "Aporte mensual sugerido",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenKeyboard("MONTHLY_CONTRIBUTION") }
                            .testTag("field_monthly_contribution"),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFF93C5FD))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = formattedContribution,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KashBlue
                                )
                                Text(
                                    text = "Calculado para cubrir el déficit de forma equitativa",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDBEAFE)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = KashBlue,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Auto",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KashBlue
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Frecuencia Chips
                    Text(
                        text = "Frecuencia de ahorro",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Semanal", "Quincenal", "Mensual").forEach { freq ->
                            val isSelected = state.frequency.equals(freq, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onFrequencyChanged(freq) },
                                label = { Text(freq, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = KashNavy,
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Switch: Aporte automático (Cron Job de KashApp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aporte automático",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KashNavy
                            )
                            Text(
                                text = "Programa un débito automático los días ${state.contributionDay} de cada mes",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Switch(
                            checked = state.autoContribute,
                            onCheckedChange = { onAutoContributeChanged(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = KashBlue
                            ),
                            modifier = Modifier.testTag("switch_auto_contribute")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Switch: Separar dinero automáticamente (Bolsillo Virtual / Mental Accounting)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0FDF4))
                            .border(BorderStroke(1.dp, Color(0xFFBBF7D0)), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDCFCE7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Bloquear aportes hechos",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                                Text(
                                    text = "Bloquea solo los aportes realizados ($formattedSaved), no la meta total. Se sigue mostrando todo en el Dashboard pero este monto queda comprometido.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF166534),
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Switch(
                            checked = state.virtualPocketEnabled,
                            onCheckedChange = { onVirtualPocketChanged(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF16A34A)
                            ),
                            modifier = Modifier.testTag("switch_virtual_pocket")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Cuenta para ahorrar (Destino)
                    Text(
                        text = "Cuenta para ahorrar (Destino)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { accountDropdownExpanded = true }
                                .testTag("target_account_picker"),
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
                                        text = state.targetAccountName.ifBlank { "Bancolombia ••••• 1234" },
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
                            expanded = accountDropdownExpanded,
                            onDismissRequest = { accountDropdownExpanded = false }
                        ) {
                            if (state.availableAccounts.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Bancolombia ••••• 1234") },
                                    onClick = {
                                        onTargetAccountChanged("acc_bancolombia", "Bancolombia ••••• 1234")
                                        accountDropdownExpanded = false
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
                                            onTargetAccountChanged(acc.id, display)
                                            accountDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
