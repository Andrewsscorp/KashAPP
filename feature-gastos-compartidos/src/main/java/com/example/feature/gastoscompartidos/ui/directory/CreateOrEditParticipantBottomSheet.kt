package com.example.feature.gastoscompartidos.ui.directory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.text.TextNormalizer
import com.example.data.local.entity.SharedParticipantEntity
import com.example.domain.models.shared.SettlementPaymentMethod
import com.example.feature.gastoscompartidos.ui.theme.KashFormattedCurrencyField
import com.example.feature.gastoscompartidos.ui.theme.KashStandardTextField
import com.example.feature.gastoscompartidos.ui.theme.SharedExpenseThemeTokens
import com.example.feature.gastoscompartidos.ui.theme.SharedExpenseThemeWrapper
import com.example.feature.gastoscompartidos.ui.theme.rememberSharedResponsiveLayoutInfo
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateOrEditParticipantBottomSheet(
    visible: Boolean,
    initialParticipant: SharedParticipantEntity? = null,
    prefilledName: String = "",
    onPickFromPhoneContacts: () -> Unit,
    onAttachCustomQrImage: () -> Unit,
    onSaveParticipant: (SharedParticipantEntity) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val responsive = rememberSharedResponsiveLayoutInfo()

    var displayName by remember(initialParticipant, prefilledName) {
        mutableStateOf(initialParticipant?.displayName ?: prefilledName)
    }
    var aliasOrNit by remember(initialParticipant) {
        mutableStateOf(initialParticipant?.aliasOrRelation.orEmpty())
    }
    var phoneOrWhatsApp by remember(initialParticipant) {
        mutableStateOf(initialParticipant?.phoneOrWhatsApp.orEmpty())
    }
    var selectedPaymentMethod by remember(initialParticipant) {
        mutableStateOf(initialParticipant?.preferredPaymentMethod ?: SettlementPaymentMethod.BRE_B.code)
    }
    var bankName by remember(initialParticipant) {
        mutableStateOf(initialParticipant?.bankName ?: "Bancolombia / Nequi")
    }
    var accountOrKey by remember(initialParticipant) {
        mutableStateOf(initialParticipant?.accountNumberOrKey.orEmpty())
    }
    var creditTrustLimit by remember(initialParticipant) {
        mutableDoubleStateOf(initialParticipant?.creditTrustLimit ?: 0.0)
    }
    var monthlyIncome by remember(initialParticipant) {
        mutableDoubleStateOf(initialParticipant?.monthlyIncomeEstimate ?: 0.0)
    }
    var selectedAvatarColor by remember(initialParticipant) {
        mutableStateOf(initialParticipant?.avatarColorHex ?: "#0284C7")
    }

    val avatarColors = listOf("#0284C7", "#0D9488", "#7C3AED", "#E11D48", "#10B981", "#D97706")
    val computedInitials = remember(displayName) {
        val parts = displayName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "TR"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        SharedExpenseThemeWrapper {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = responsive.horizontalPadding, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cabecera y botón para importar de la agenda del teléfono
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(selectedAvatarColor))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = computedInitials,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (initialParticipant == null) "Crear Tercero / Participante" else "Editar Tercero Contable",
                                fontSize = if (responsive.isUltraCompactWidth) 15.sp else 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SharedExpenseThemeTokens.KashNavy
                            )
                            Text(
                                text = "Directorio NIIF • Normalización NFKD Anti-Duplicados",
                                fontSize = 11.sp,
                                color = SharedExpenseThemeTokens.KashTextSecondary
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onPickFromPhoneContacts,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContactPhone, contentDescription = null, modifier = Modifier.size(15.dp), tint = SharedExpenseThemeTokens.KashBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Contactos", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SharedExpenseThemeTokens.KashBlue)
                    }
                }

                // Selector de Color de Avatar
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(avatarColors) { hex ->
                        val isSelected = hex == selectedAvatarColor
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 30.dp else 24.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .clickable { selectedAvatarColor = hex }
                        )
                    }
                }

                // 1. Datos de Identificación del Tercero
                KashStandardTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = "Nombre Completo o Razón Social *",
                    placeholder = "Ej: Carlos Ruiz o Ana Gómez",
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("participant_name_input")
                )

                if (responsive.isUltraCompactWidth) {
                    KashStandardTextField(
                        value = aliasOrNit,
                        onValueChange = { aliasOrNit = it },
                        label = "Cédula / NIT / Relación",
                        placeholder = "Ej: 1.018.492.310 o Socio",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    KashStandardTextField(
                        value = phoneOrWhatsApp,
                        onValueChange = { phoneOrWhatsApp = it },
                        label = "WhatsApp (Para Enviar Tickets)",
                        placeholder = "+57 300...",
                        keyboardType = KeyboardType.Phone,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KashStandardTextField(
                            value = aliasOrNit,
                            onValueChange = { aliasOrNit = it },
                            label = "Cédula / NIT / Relación",
                            placeholder = "Ej: 1.018.492.310",
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        KashStandardTextField(
                            value = phoneOrWhatsApp,
                            onValueChange = { phoneOrWhatsApp = it },
                            label = "WhatsApp",
                            placeholder = "+57 300...",
                            keyboardType = KeyboardType.Phone,
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 2. Medio de Pago / Llave para Reembolsos y Cobros
                Text(
                    text = "MEDIO DE PAGO PREFERIDO / LLAVE DE RECAUDO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SharedExpenseThemeTokens.KashBlue
                )

                val paymentMethods = listOf(
                    SettlementPaymentMethod.BRE_B,
                    SettlementPaymentMethod.NEQUI,
                    SettlementPaymentMethod.DAVIPLATA,
                    SettlementPaymentMethod.BANK_TRANSFER,
                    SettlementPaymentMethod.CASH
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(paymentMethods) { method ->
                        val isSelected = selectedPaymentMethod == method.code
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SharedExpenseThemeTokens.ReceivableTeal else SharedExpenseThemeTokens.KashSurfaceMuted,
                            border = BorderStroke(1.dp, if (isSelected) SharedExpenseThemeTokens.ReceivableTeal else SharedExpenseThemeTokens.KashBorder),
                            modifier = Modifier.clickable { selectedPaymentMethod = method.code }
                        ) {
                            Text(
                                text = method.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else SharedExpenseThemeTokens.KashTextPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        KashStandardTextField(
                            value = accountOrKey,
                            onValueChange = { accountOrKey = it },
                            label = "Número de Cuenta, Celular o Llave Bre-B",
                            placeholder = "Ej: @camilo.breb o 300 412 8890",
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedButton(
                        onClick = onAttachCustomQrImage,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(54.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp), tint = SharedExpenseThemeTokens.KashBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("QR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SharedExpenseThemeTokens.KashBlue)
                    }
                }

                // 3. Reglas Financieras: Cupo de Confianza Crediticia e Ingreso Mensual
                Text(
                    text = "CONTROL DE RIESGO Y EQUIDAD PROPORCIONAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SharedExpenseThemeTokens.KashBlue
                )

                if (responsive.isUltraCompactWidth) {
                    KashFormattedCurrencyField(
                        value = creditTrustLimit,
                        onValueChange = { creditTrustLimit = it },
                        label = "Tope Confianza CxC (Opcional)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    KashFormattedCurrencyField(
                        value = monthlyIncome,
                        onValueChange = { monthlyIncome = it },
                        label = "Ingreso Mensual (Modo ⚖️ Equidad)",
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            KashFormattedCurrencyField(
                                value = creditTrustLimit,
                                onValueChange = { creditTrustLimit = it },
                                label = "Tope Confianza CxC",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            KashFormattedCurrencyField(
                                value = monthlyIncome,
                                onValueChange = { monthlyIncome = it },
                                label = "Ingreso Mensual",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Botón de Guardado con Normalización NFKD
                Button(
                    onClick = {
                        val trimmedName = displayName.trim()
                        if (trimmedName.length >= 2) {
                            val entity = SharedParticipantEntity(
                                id = initialParticipant?.id ?: "part_${UUID.randomUUID().toString().take(8)}",
                                userId = initialParticipant?.userId ?: "user_default",
                                isSelf = false,
                                displayName = trimmedName,
                                nameNormalized = TextNormalizer.normalizeForUniqueness(trimmedName),
                                aliasOrRelation = aliasOrNit.trim().ifBlank { null },
                                phoneOrWhatsApp = phoneOrWhatsApp.trim().ifBlank { null },
                                preferredPaymentMethod = selectedPaymentMethod,
                                bankName = bankName.trim().ifBlank { null },
                                accountNumberOrKey = accountOrKey.trim().ifBlank { null },
                                monthlyIncomeEstimate = monthlyIncome,
                                avatarColorHex = selectedAvatarColor,
                                avatarInitials = computedInitials,
                                creditTrustLimit = if (creditTrustLimit > 0) creditTrustLimit else null
                            )
                            onSaveParticipant(entity)
                            onDismiss()
                        }
                    },
                    enabled = displayName.trim().length >= 2,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SharedExpenseThemeTokens.KashNavy),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_participant_btn")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Guardar Tercero en Directorio",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
