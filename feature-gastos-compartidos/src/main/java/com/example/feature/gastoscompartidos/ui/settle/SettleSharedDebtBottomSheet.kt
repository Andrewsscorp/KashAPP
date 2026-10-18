package com.example.feature.gastoscompartidos.ui.settle

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.SharedParticipantEntity
import com.example.domain.models.shared.ReimbursementAccountingTreatment
import com.example.domain.models.shared.SettlementPaymentMethod
import com.example.feature.gastoscompartidos.domain.ticket.SharedReceiptTicketEngine
import com.example.feature.gastoscompartidos.ui.theme.KashFormattedCurrencyField
import com.example.feature.gastoscompartidos.ui.theme.KashStandardTextField
import com.example.feature.gastoscompartidos.ui.theme.SharedExpenseThemeTokens
import com.example.feature.gastoscompartidos.ui.theme.SharedExpenseThemeWrapper
import com.example.feature.gastoscompartidos.ui.theme.rememberSharedResponsiveLayoutInfo
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleSharedDebtBottomSheet(
    visible: Boolean,
    participant: SharedParticipantEntity?,
    netBalanceWithUser: Double,
    currency: String,
    accounts: List<AccountEntity>,
    onConfirmSettlement: (
        amount: Double,
        method: SettlementPaymentMethod,
        treatment: ReimbursementAccountingTreatment,
        kashAccountId: String?,
        externalReference: String?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible || participant == null) return
    val responsive = rememberSharedResponsiveLayoutInfo()
    val pendingAbs = abs(netBalanceWithUser)
    val owesMe = netBalanceWithUser >= 0.0

    var settlementAmount by remember(pendingAbs) {
        mutableDoubleStateOf(pendingAbs)
    }
    var selectedMethod by remember {
        mutableStateOf(SettlementPaymentMethod.BRE_B)
    }
    var selectedTreatment by remember {
        mutableStateOf(ReimbursementAccountingTreatment.OFFSET_RECEIVABLE_ASSET)
    }
    var selectedAccountId by remember(accounts) {
        mutableStateOf(accounts.firstOrNull()?.id)
    }
    var referenceText by remember { mutableStateOf("") }

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
                    .padding(horizontal = responsive.horizontalPadding, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (owesMe) {
                        "💳 Registrar Abono de ${participant.displayName} (PG)"
                    } else {
                        "💳 Liquidar Deuda a ${participant.displayName} (PG)"
                    },
                    fontSize = if (responsive.isUltraCompactWidth) 16.sp else 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SharedExpenseThemeTokens.KashNavy
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (owesMe) SharedExpenseThemeTokens.ReceivableTealBg else SharedExpenseThemeTokens.PayableRoseBg,
                    border = BorderStroke(
                        1.dp,
                        if (owesMe) SharedExpenseThemeTokens.ReceivableTealBorder else SharedExpenseThemeTokens.PayableRoseBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (owesMe) "Saldo Pendiente por Cobrar (FIFO)" else "Saldo Pendiente por Pagar (FIFO)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (owesMe) SharedExpenseThemeTokens.ReceivableTeal else SharedExpenseThemeTokens.PayableRose
                            )
                            Text(
                                text = if (owesMe) {
                                    "Cancela automáticamente desde el ticket más antiguo."
                                } else {
                                    "Libera automáticamente tu reserva holdBalance y debita tu cuenta."
                                },
                                fontSize = 11.sp,
                                color = SharedExpenseThemeTokens.KashTextSecondary
                            )
                        }
                        Text(
                            text = SharedReceiptTicketEngine.formatMoney(pendingAbs, currency),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = if (owesMe) SharedExpenseThemeTokens.ReceivableTeal else SharedExpenseThemeTokens.PayableRose
                        )
                    }
                }

                // Campo formateado de Moneda con chips rápidos (100%, 50%, 25%)
                KashFormattedCurrencyField(
                    value = settlementAmount,
                    onValueChange = { settlementAmount = it },
                    label = "Monto a Liquidar / Abonar ($currency)",
                    currencySymbol = currency,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settle_amount_input")
                )

                // Chips de porcentaje de liquidación
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "100% Total" to 1.0,
                        "50% Mitad" to 0.5,
                        "25% Cuarto" to 0.25
                    ).forEach { (label, ratio) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SharedExpenseThemeTokens.KashSurfaceMuted,
                            border = BorderStroke(1.dp, SharedExpenseThemeTokens.KashBorder),
                            modifier = Modifier.clickable {
                                settlementAmount = (pendingAbs * ratio)
                            }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SharedExpenseThemeTokens.KashBlue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "MEDIO DE PAGO UTILIZADO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SharedExpenseThemeTokens.KashBlue
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SettlementPaymentMethod.entries) { method ->
                        val isSelected = method == selectedMethod
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SharedExpenseThemeTokens.KashNavy else SharedExpenseThemeTokens.KashSurfaceMuted,
                            border = BorderStroke(1.dp, if (isSelected) SharedExpenseThemeTokens.KashNavy else SharedExpenseThemeTokens.KashBorder),
                            modifier = Modifier.clickable { selectedMethod = method }
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

                if (owesMe) {
                    Text(
                        text = "TRATAMIENTO CONTABLE DEL REEMBOLSO (NIIF)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SharedExpenseThemeTokens.KashBlue
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ReimbursementAccountingTreatment.entries) { tr ->
                            val isSelected = tr == selectedTreatment
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) SharedExpenseThemeTokens.ReceivableTeal else SharedExpenseThemeTokens.KashSurfaceMuted,
                                border = BorderStroke(1.dp, if (isSelected) SharedExpenseThemeTokens.ReceivableTeal else SharedExpenseThemeTokens.KashBorder),
                                modifier = Modifier.clickable { selectedTreatment = tr }
                            ) {
                                Text(
                                    text = tr.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else SharedExpenseThemeTokens.KashTextPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                if (accounts.isNotEmpty()) {
                    Text(
                        text = if (owesMe) "CUENTA DESTINO EN KASHAPP" else "CUENTA ORIGEN EN KASHAPP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SharedExpenseThemeTokens.KashBlue
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(accounts, key = { it.id }) { acc ->
                            val isSelected = acc.id == selectedAccountId
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) SharedExpenseThemeTokens.KashBlue else Color.White,
                                border = BorderStroke(1.dp, if (isSelected) SharedExpenseThemeTokens.KashBlue else SharedExpenseThemeTokens.KashBorder),
                                modifier = Modifier.clickable { selectedAccountId = acc.id }
                            ) {
                                Text(
                                    text = acc.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else SharedExpenseThemeTokens.KashTextPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                KashStandardTextField(
                    value = referenceText,
                    onValueChange = { referenceText = it },
                    label = "Referencia / Comprobante Bancario (Opcional)",
                    placeholder = "Ej: M9182736 o Bre-B",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (settlementAmount > 0.0) {
                            onConfirmSettlement(
                                settlementAmount,
                                selectedMethod,
                                selectedTreatment,
                                selectedAccountId,
                                referenceText.trim().ifBlank { null }
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SharedExpenseThemeTokens.SettledEmerald),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_settle_debt_btn")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Emitir Recibo de Pago Oficial (PG)",
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
