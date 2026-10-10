package com.example.presentation.fixedassets.components.statemachine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.data.local.entity.FixedAssetEntity
import com.example.domain.model.fixedassets.statemachine.AssetState
import com.example.domain.model.fixedassets.statemachine.AssetStateMachine
import com.example.presentation.fixedassets.components.IPhoneActionModal
import com.example.presentation.fixedassets.components.movement.CurrencyInputField
import com.example.presentation.fixedassets.create.components.IPhoneAdaptiveDatePickerField
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Interactive Apple modal for transitioning asset lifecycle state with
 * guardrails enforcement, mandatory justification, and help dialogs for each state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeAssetStateModal(
    asset: FixedAssetEntity,
    onStateTransitionConfirmed: (targetStateCode: String, reasonNotes: String) -> Unit,
    onDismiss: () -> Unit,
    onInspectStateHelp: (AssetState) -> Unit
) {
    val currentState = remember(asset.status) { AssetStateMachine.resolveState(asset.status) }

    val allowedTargetStates = remember(currentState) {
        currentState.allowedTargetStates.map { AssetStateMachine.resolveState(it) }
    }

    var selectedTargetState by remember(allowedTargetStates) {
        mutableStateOf(allowedTargetStates.firstOrNull() ?: currentState)
    }

    var reasonNotes by remember { mutableStateOf("") }
    var effectiveDate by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))) }
    var extraDetail by remember { mutableStateOf("") }
    var saleOrScrapValue by remember { mutableStateOf<Double?>(null) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = selectedTargetState.color,
        unfocusedBorderColor = Color(0xFF132B4F),
        focusedLabelColor = selectedTargetState.color,
        unfocusedLabelColor = Color(0xFF8EA5C4),
        cursorColor = selectedTargetState.color
    )

    IPhoneActionModal(
        title = "Transición de Estado del Activo",
        subtitle = "${asset.name} • Estado actual: ${currentState.shortBadgeLabel}",
        icon = Icons.Default.SwapHoriz,
        iconTint = Color(0xFF38BDF8),
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Current State & Allowed Targets
            Text(
                text = "SELECCIONAR NUEVO ESTADO DESTINO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8EA5C4),
                letterSpacing = 0.5.sp
            )

            if (allowedTargetStates.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2D0A0A),
                    border = BorderStroke(1.dp, Color(0xFFDC2626)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "El activo se encuentra en estado terminal '${currentState.label}'. No se permiten más transiciones por interfaz.",
                        fontSize = 12.sp,
                        color = Color(0xFFFCA5A5),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allowedTargetStates.forEach { target ->
                        val isSelected = selectedTargetState == target
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) target.color.copy(alpha = 0.25f) else Color(0xFF0B172B),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) target.color else Color(0xFF132B4F)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedTargetState = target }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(target.color)
                                )
                                Text(
                                    text = target.shortBadgeLabel,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                )
                                // Help icon (?) for each option
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .clickable { onInspectStateHelp(target) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HelpOutline,
                                        contentDescription = "Ver ayuda de ${target.label}",
                                        tint = if (isSelected) target.color else Color(0xFF64748B),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Warning or impact banner for selected target state
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF071426),
                border = BorderStroke(1.dp, selectedTargetState.color.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = selectedTargetState.color,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Efecto Contable: ${selectedTargetState.label}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = selectedTargetState.color
                        )
                    }
                    Text(
                        text = selectedTargetState.financialRationale,
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            // Reason / Justification input (mandatory)
            OutlinedTextField(
                value = reasonNotes,
                onValueChange = { reasonNotes = it },
                label = { Text("Motivo / Justificación Técnica de la Transición") },
                placeholder = { Text("Ej: Traslado a taller por fallo mecánico / Asignación a nuevo custodio", color = Color(0xFF64748B)) },
                maxLines = 3,
                colors = textFieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transition_reason_input")
            )

            // Contextual field depending on target
            when (selectedTargetState.code) {
                "EN_COMODATO" -> {
                    OutlinedTextField(
                        value = extraDetail,
                        onValueChange = { extraDetail = it },
                        label = { Text("Tercero / Cliente Beneficiario (Comodato)") },
                        placeholder = { Text("Ej: Empresa Cliente S.A.S. - NIT 900.123.456", color = Color(0xFF64748B)) },
                        singleLine = true,
                        colors = textFieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                "EN_MANTENIMIENTO" -> {
                    OutlinedTextField(
                        value = extraDetail,
                        onValueChange = { extraDetail = it },
                        label = { Text("N° Orden de Trabajo / Taller Autorizado") },
                        placeholder = { Text("Ej: OT-7721 / Taller Central", color = Color(0xFF64748B)) },
                        singleLine = true,
                        colors = textFieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                "MANTENIDO_PARA_LA_VENTA" -> {
                    CurrencyInputField(
                        value = saleOrScrapValue,
                        onValueChange = { saleOrScrapValue = it },
                        label = "Precio de Venta Esperado (NIIF 5)",
                        currencyCode = asset.currency,
                        showQuickPills = false
                    )
                }
                "DADO_DE_BAJA" -> {
                    OutlinedTextField(
                        value = extraDetail,
                        onValueChange = { extraDetail = it },
                        label = { Text("N° Acta de Desincorporación / Resolución") },
                        placeholder = { Text("Ej: ACTA-BAJA-2026-88", color = Color(0xFF64748B)) },
                        singleLine = true,
                        colors = textFieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Effective Date
            IPhoneAdaptiveDatePickerField(
                value = effectiveDate,
                onValueChange = { effectiveDate = it },
                label = "Fecha Efectiva de la Transición",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Confirm Button
            Button(
                onClick = {
                    val fullNotes = buildString {
                        append(reasonNotes.trim())
                        if (extraDetail.isNotBlank()) {
                            append(" | Detalle: $extraDetail")
                        }
                        if (saleOrScrapValue != null) {
                            append(" | Valor estimado: $saleOrScrapValue")
                        }
                        append(" | Fecha: $effectiveDate")
                    }
                    onStateTransitionConfirmed(selectedTargetState.code, fullNotes)
                },
                enabled = allowedTargetStates.isNotEmpty() && reasonNotes.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = selectedTargetState.color),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_state_transition_button")
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirmar: ${AssetStateMachine.getActionVerbForTarget(selectedTargetState)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
