package com.example.presentation.fixedassets.components.statemachine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.fixedassets.statemachine.AssetState
import com.example.domain.model.fixedassets.statemachine.AssetStateMachine
import com.example.presentation.fixedassets.components.IPhoneActionModal

/**
 * Explanatory Apple-style modal sheet detailing the financial, programming,
 * and interface rules of an Asset Lifecycle State, matching the advanced config aesthetic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetStateExplainerSheet(
    state: AssetState,
    onDismiss: () -> Unit,
    onChangeStateRequested: (() -> Unit)? = null
) {
    IPhoneActionModal(
        title = "Guía Contable del Estado",
        subtitle = state.label,
        icon = Icons.Default.HelpOutline,
        iconTint = state.color,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero State Badge Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF071426),
                border = BorderStroke(1.dp, state.color.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(state.color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(state.color)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Código de Sistema: ${state.code}",
                            fontSize = 11.sp,
                            color = Color(0xFF8EA5C4)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = state.color.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, state.color)
                    ) {
                        Text(
                            text = if (state.allowsDepreciation) "Depreciable" else "Depr. Pausada",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = state.color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 1. Fundamento Financiero NIIF / IFRS
            ExplainerSectionCard(
                title = "Fundamento Financiero (NIIF / NIC 16)",
                description = state.financialRationale,
                icon = Icons.Default.AccountBalance,
                accentColor = Color(0xFF38BDF8)
            )

            // 2. Motor de Cálculo y CRON Job
            ExplainerSectionCard(
                title = "Comportamiento del Motor Contable (CRON)",
                description = state.calculationEngineBehavior,
                icon = Icons.Default.Calculate,
                accentColor = if (state.allowsDepreciation) Color(0xFF10B981) else Color(0xFFEF4444)
            )

            // 3. Restricciones de Interfaz y Bloqueos
            ExplainerSectionCard(
                title = "Restricciones de Interfaz (UI Guardrails)",
                description = state.uiRestrictionsDescription,
                icon = Icons.Default.Lock,
                accentColor = Color(0xFFF59E0B)
            )

            // 4. Disparador / Trigger
            ExplainerSectionCard(
                title = "Criterio de Activación (Trigger)",
                description = state.triggerDescription,
                icon = Icons.Default.Timer,
                accentColor = Color(0xFFA78BFA)
            )

            // 5. Matriz de Transiciones Permitidas
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF091629),
                border = BorderStroke(1.dp, Color(0xFF163259)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Transiciones Lógicas Permitidas",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0)
                        )
                    }

                    if (state.allowedTargetStates.isEmpty()) {
                        Text(
                            text = "Estado Terminal: No permite transiciones hacia ningún otro estado. Es irreversible por interfaz.",
                            fontSize = 11.sp,
                            color = Color(0xFFEF4444)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            state.allowedTargetStates.forEach { targetCode ->
                                val target = AssetStateMachine.resolveState(targetCode)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0F223D),
                                    border = BorderStroke(1.dp, target.color.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "➔ ${target.shortBadgeLabel}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = target.color,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action button if not terminal and transition requested
            if (onChangeStateRequested != null && !state.isTerminal) {
                Button(
                    onClick = {
                        onDismiss()
                        onChangeStateRequested()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Cambiar Estado del Activo", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ExplainerSectionCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF091629),
        border = BorderStroke(1.dp, Color(0xFF163259)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                text = description,
                fontSize = 11.5.sp,
                color = Color(0xFFCBD5E1),
                lineHeight = 16.sp
            )
        }
    }
}
