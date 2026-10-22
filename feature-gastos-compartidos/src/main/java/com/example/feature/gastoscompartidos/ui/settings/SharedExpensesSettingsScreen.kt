package com.example.feature.gastoscompartidos.ui.settings

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import com.example.core.ui.components.IosStyleSwitch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.engine.ConsecutiveSequenceEngine
import com.example.domain.models.shared.SharedExpenseSettingsConfig
import com.example.domain.security.FirebaseApiProtectionGuard
import com.example.feature.gastoscompartidos.ui.theme.KashBlueTopBar
import com.example.feature.gastoscompartidos.ui.theme.KashStandardTextField
import com.example.feature.gastoscompartidos.ui.theme.SharedExpenseThemeTokens
import com.example.feature.gastoscompartidos.ui.theme.SharedExpenseThemeWrapper
import com.example.feature.gastoscompartidos.ui.theme.rememberSharedResponsiveLayoutInfo
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SharedExpensesSettingsScreen(
    config: SharedExpenseSettingsConfig,
    apiProtectionStatus: FirebaseApiProtectionGuard.ApiProtectionStatus,
    onUpdateConfig: (SharedExpenseSettingsConfig) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    val responsive = rememberSharedResponsiveLayoutInfo()
    val today = LocalDate.now()

    val livePreviewExpenseCode = ConsecutiveSequenceEngine.formatPattern(
        patternTemplate = config.expenseConsecutivePattern,
        prefix = "GC",
        sequenceNumber = 42L,
        referenceDate = today,
        groupCode = "APTO",
        userInitials = "CS"
    )

    val livePreviewTicketCode = ConsecutiveSequenceEngine.formatPattern(
        patternTemplate = config.receiptTicketPattern,
        prefix = "TK",
        sequenceNumber = 42L,
        referenceDate = today,
        groupCode = "APTO",
        userInitials = "CS"
    )

    val livePreviewVoucherCode = ConsecutiveSequenceEngine.formatPattern(
        patternTemplate = config.settlementVoucherPattern,
        prefix = "PG",
        sequenceNumber = 8L,
        referenceDate = today,
        groupCode = "APTO",
        userInitials = "CS"
    )

    SharedExpenseThemeWrapper {
        Scaffold(
            containerColor = SharedExpenseThemeTokens.KashLightBg,
            topBar = {
                KashBlueTopBar(
                    title = "Ajustes de Gastos Compartidos",
                    subtitle = "Series, Consecutivos, Recaudo y Privacidad",
                    onNavigateBack = onNavigateBack
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = responsive.maxContentWidth)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = responsive.horizontalPadding, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Panel de Series y Consecutivos Automáticos / Manuales
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, SharedExpenseThemeTokens.KashBorder),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SharedExpenseThemeTokens.KashSurfaceTint
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tag,
                                        contentDescription = null,
                                        tint = SharedExpenseThemeTokens.KashBlue,
                                        modifier = Modifier.padding(8.dp).size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "1. SERIES Y CONSECUTIVOS NIIF",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SharedExpenseThemeTokens.KashBlue
                                    )
                                    Text(
                                        text = "Estructura documental para gastos, tickets y comprobantes",
                                        fontSize = 11.sp,
                                        color = SharedExpenseThemeTokens.KashTextSecondary
                                    )
                                }
                            }

                            KashStandardTextField(
                                value = config.expenseConsecutivePattern,
                                onValueChange = { onUpdateConfig(config.copy(expenseConsecutivePattern = it)) },
                                label = "Plantilla Serie Gasto (GC)",
                                placeholder = "{PREFIX}-{YYYY}-{SEQ:4}"
                            )

                            // Token quick chips
                            Text(
                                text = "Toca para agregar comodines a la serie de gasto:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SharedExpenseThemeTokens.KashTextMuted
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("{PREFIX}", "{GROUP}", "{YYYY}", "{MM}", "{SEQ:4}", "{INIT}").forEach { token ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SharedExpenseThemeTokens.KashSurfaceTint,
                                        border = BorderStroke(1.dp, SharedExpenseThemeTokens.KashBlueBorder),
                                        modifier = Modifier.clickable {
                                            if (!config.expenseConsecutivePattern.contains(token)) {
                                                val updated = "${config.expenseConsecutivePattern.trimEnd('-')}-$token"
                                                onUpdateConfig(config.copy(expenseConsecutivePattern = updated))
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = "+ $token",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SharedExpenseThemeTokens.KashBlueDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            KashStandardTextField(
                                value = config.receiptTicketPattern,
                                onValueChange = { onUpdateConfig(config.copy(receiptTicketPattern = it)) },
                                label = "Plantilla Serie Ticket de Recibo (TK)",
                                placeholder = "{PREFIX}-{YYYY}-{SEQ:4}"
                            )

                            KashStandardTextField(
                                value = config.settlementVoucherPattern,
                                onValueChange = { onUpdateConfig(config.copy(settlementVoucherPattern = it)) },
                                label = "Plantilla Comprobante Abono / Pago (PG)",
                                placeholder = "{PREFIX}-{YYYY}{MM}-{SEQ:4}"
                            )

                            // Live preview card in KashApp blue
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SharedExpenseThemeTokens.KashDarkSlate,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "VISTA PREVIA EN VIVO",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SharedExpenseThemeTokens.KashCyanLight,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "• Gasto: $livePreviewExpenseCode",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "• Ticket Oficial: $livePreviewTicketCode",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF99F6E4)
                                    )
                                    Text(
                                        text = "• Comprobante Abono: $livePreviewVoucherCode",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFED7AA)
                                    )
                                }
                            }

                            // Switch allow manual consecutive override
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Permitir consecutivo o factura manual",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SharedExpenseThemeTokens.KashTextPrimary
                                    )
                                    Text(
                                        text = "Permite sobreescribir el consecutivo para digitar números de facturas electrónicas o tickets físicos.",
                                        fontSize = 11.sp,
                                        color = SharedExpenseThemeTokens.KashTextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                IosStyleSwitch(
                                    checked = config.allowManualConsecutiveOverride,
                                    onCheckedChange = { onUpdateConfig(config.copy(allowManualConsecutiveOverride = it)) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = SharedExpenseThemeTokens.KashBlue
                                    )
                                )
                            }
                        }
                    }

                    // 2. Privacidad en Tickets de Recibo y Datos de Cobro QR
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, SharedExpenseThemeTokens.KashBorder),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SharedExpenseThemeTokens.KashSurfaceTint
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = null,
                                        tint = SharedExpenseThemeTokens.KashBlue,
                                        modifier = Modifier.padding(8.dp).size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "2. PRIVACIDAD EN TICKETS Y RECAUDO QR",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SharedExpenseThemeTokens.KashBlue
                                    )
                                    Text(
                                        text = "Datos para imprimir en el QR del ticket y protección de saldos",
                                        fontSize = 11.sp,
                                        color = SharedExpenseThemeTokens.KashTextSecondary
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Enmascarar mis saldos bancarios en el Ticket",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SharedExpenseThemeTokens.KashTextPrimary
                                    )
                                    Text(
                                        text = "Oculta el saldo disponible de tu cuenta al exportar comprobantes PNG/PDF a WhatsApp o correo.",
                                        fontSize = 11.sp,
                                        color = SharedExpenseThemeTokens.KashTextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                IosStyleSwitch(
                                    checked = config.maskUserAccountBalancesOnTicket,
                                    onCheckedChange = { onUpdateConfig(config.copy(maskUserAccountBalancesOnTicket = it)) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = SharedExpenseThemeTokens.KashBlue
                                    )
                                )
                            }

                            KashStandardTextField(
                                value = config.defaultCollectionBankName,
                                onValueChange = { onUpdateConfig(config.copy(defaultCollectionBankName = it)) },
                                label = "Banco / Billetera de Recaudo",
                                placeholder = "Ej: Bancolombia / Nequi / DaviPlata"
                            )

                            KashStandardTextField(
                                value = config.defaultCollectionAccountOrKey,
                                onValueChange = { onUpdateConfig(config.copy(defaultCollectionAccountOrKey = it)) },
                                label = "Llave Bre-B / Celular / Número de Cuenta",
                                placeholder = "Ej: @andres.kash o 300 123 4567"
                            )
                        }
                    }

                    // 3. Panel de Seguridad de API Firebase AI & App Check Zero-Trust
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.5.dp, SharedExpenseThemeTokens.KashBlue.copy(alpha = 0.5f)),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.padding(8.dp).size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "3. SEGURIDAD ZERO-TRUST & FIREBASE AI",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SharedExpenseThemeTokens.KashNavy
                                    )
                                    Text(
                                        text = "Protección criptográfica y sanitización de datos financieros",
                                        fontSize = 11.sp,
                                        color = SharedExpenseThemeTokens.KashTextSecondary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SharedExpenseThemeTokens.KashSurfaceMuted,
                                border = BorderStroke(1.dp, SharedExpenseThemeTokens.KashBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "• Firebase App Check: ${if (apiProtectionStatus.isAppCheckInitialized) "ACTIVA (Play Integrity verificado)" else "MODO LOCAL SEGURO"}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SharedExpenseThemeTokens.SettledEmerald
                                    )
                                    Text(
                                        text = "• Anonimizador PII Zero-Knowledge: ACTIVO (Oculta números de cuenta, saldos y documentos antes de procesar con Gemini 2.5 Flash)",
                                        fontSize = 12.sp,
                                        color = SharedExpenseThemeTokens.KashTextPrimary
                                    )
                                    Text(
                                        text = "• Cuota Protegida Token Bucket: ${apiProtectionStatus.remainingMinuteQuota}/10 llamadas por minuto • ${apiProtectionStatus.remainingDailyQuota}/100 hoy",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SharedExpenseThemeTokens.KashTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Botón para aplicar y regresar
                    Button(
                        onClick = onNavigateBack,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SharedExpenseThemeTokens.KashBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Guardar y Regresar",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
