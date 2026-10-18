package com.example.presentation.expense.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.presentation.expense.model.CardPaymentVoucher
import com.example.presentation.expense.model.formatMoneyDisplay
import com.example.util.CreditCardPaymentEngine

/**
 * Premium iOS-Style Modal Pop-Up displaying the detailed transaction receipt (Comprobante de Pago / Abono a Tarjeta)
 * Shows complete breakdown to Capital, Intereses, Cuota de manejo, and Interest Rates (EA / EM).
 */
@Composable
fun CreditCardWaterfallVoucherDialog(
    voucher: CardPaymentVoucher?,
    onDismiss: () -> Unit
) {
    if (voucher == null) return
    CardPaymentVoucherDialog(voucher = voucher, onDismiss = onDismiss)
}

@Composable
fun CardPaymentVoucherDialog(
    voucher: CardPaymentVoucher,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(26.dp))
                .testTag("card_payment_voucher_dialog"),
            color = Color.White,
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success Badge
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDCFCE7)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16A34A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Éxito",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "¡Pago Aplicado con Éxito!",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Comprobante de Transacción Financiera",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0284C7),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Amount Main Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.5.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "MONTO TOTAL ABONADO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatMoneyDisplay(voucher.amount, voucher.currency),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF15803D)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ----------------------------------------------------
                // FINANCIAL BREAKDOWN CARD (DESGLOSE CAPITAL E INTERESES)
                // ----------------------------------------------------
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DESGLOSE DE APLICACIÓN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569),
                                letterSpacing = 0.5.sp
                            )
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE0F2FE)) {
                                Text(
                                    text = "Tasa: ${"%.2f".format(voucher.rateEa)}% E.A. (${"%.2f".format(voucher.rateEm)}% E.M.)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0369A1),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        // Row 1: Abono a Capital
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF16A34A)))
                                Text("Abono Real a Capital:", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155))
                            }
                            Text(
                                text = formatMoneyDisplay(voucher.principalAmortized, voucher.currency),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                        }

                        // Row 2: Intereses Corrientes
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFF97316)))
                                Text("Intereses Corrientes del Mes:", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155))
                            }
                            Text(
                                text = formatMoneyDisplay(voucher.currentInterestPaid, voucher.currency),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEA580C)
                            )
                        }

                        // Row 3: Cuota de Manejo (if any)
                        if (voucher.handlingFeePaid > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF64748B)))
                                    Text("Cuota de Manejo / Admin:", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155))
                                }
                                Text(
                                    text = formatMoneyDisplay(voucher.handlingFeePaid, voucher.currency),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        // Row 4: Intereses de Mora (if any)
                        if (voucher.lateInterestPaid > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFDC2626)))
                                    Text("Intereses de Mora:", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF991B1B))
                                }
                                Text(
                                    text = formatMoneyDisplay(voucher.lateInterestPaid, voucher.currency),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        // Visual Proportional Bar
                        val totalApp = (voucher.principalAmortized + voucher.currentInterestPaid + voucher.handlingFeePaid + voucher.lateInterestPaid).coerceAtLeast(1.0)
                        val pPct = (voucher.principalAmortized / totalApp).toFloat().coerceIn(0.01f, 1f)
                        val fPct = ((voucher.currentInterestPaid + voucher.handlingFeePaid + voucher.lateInterestPaid) / totalApp).toFloat().coerceIn(0f, 1f)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                if (pPct > 0) {
                                    Box(modifier = Modifier.fillMaxHeight().weight(pPct).background(Color(0xFF16A34A)))
                                }
                                if (fPct > 0) {
                                    Box(modifier = Modifier.fillMaxHeight().weight(fPct).background(Color(0xFFF97316)))
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${"%.1f".format(pPct * 100)}% Abono a Capital", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                            Text("${"%.1f".format(fPct * 100)}% Intereses y Cargos", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9A3412))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Transaction Details Table
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tarjeta Destino:", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text("${voucher.cardName} (${voucher.cardMask})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cuenta Origen Débito:", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(voucher.sourceAccountName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Modalidad / Estrategia:", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(voucher.strategyName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Nueva Deuda Tarjeta:", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(formatMoneyDisplay(voucher.newDebt, voucher.currency), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cupo Liberado:", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text("+ ${formatMoneyDisplay(voucher.quotaRestored, voucher.currency)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        }

                        if (voucher.projectedImpact.isNotBlank()) {
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Impacto Proyectado:", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text(voucher.projectedImpact, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF059669), modifier = Modifier.weight(1f, fill = false), textAlign = TextAlign.End)
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("No. Transacción / Aut:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text("${voucher.transactionId} • ${voucher.authCode}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Fecha y Hora:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text("${voucher.dateFormatted} • ${voucher.timeFormatted}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dismiss_voucher_btn")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Finalizar y Volver", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * Interactive Modal Bottom Sheet to choose Extra Capital Payment Strategy
 * when user enters an amount greater than the minimum monthly quota.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardStrategySelectionBottomSheet(
    visible: Boolean,
    excessAmount: Double,
    currentStrategy: String,
    selectedCurrencyCode: String,
    impactProjection: CreditCardPaymentEngine.ExtraCapitalImpactProjection,
    onSelectStrategy: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text(
                            text = "Abono Extraordinario a Capital",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Excedente a capital: + ${formatMoneyDisplay(excessAmount, selectedCurrencyCode)}",
                            fontSize = 12.sp,
                            color = Color(0xFF059669),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF64748B))
                }
            }

            Text(
                text = "Ingresaste un valor superior a la cuota del mes. Elige cómo deseas aplicar este excedente para optimizar tu deuda:",
                fontSize = 12.sp,
                color = Color(0xFF475569),
                lineHeight = 16.sp
            )

            // Strategy 1: Reducción de Tiempo / Plazo
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectStrategy("TIME_REDUCTION") },
                shape = RoundedCornerShape(16.dp),
                color = if (currentStrategy == "TIME_REDUCTION") Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                border = BorderStroke(1.5.dp, if (currentStrategy == "TIME_REDUCTION") Color(0xFF0284C7) else Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (currentStrategy == "TIME_REDUCTION") Color(0xFF0284C7) else Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = if (currentStrategy == "TIME_REDUCTION") Color.White else Color(0xFF64748B), modifier = Modifier.size(16.dp))
                            }
                            Text("Disminuir Tiempo / Plazo", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (currentStrategy == "TIME_REDUCTION") Color(0xFF0284C7) else Color(0xFF0F172A))
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFDBEAFE)) {
                            Text("Recomendado", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF), modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                    Text(
                        text = "Abona de atrás hacia adelante cancelando las últimas cuotas y ahorrando meses enteros de intereses.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Strategy 2: Reducción de Cuota Mensual
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectStrategy("QUOTA_REDUCTION") },
                shape = RoundedCornerShape(16.dp),
                color = if (currentStrategy == "QUOTA_REDUCTION") Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                border = BorderStroke(1.5.dp, if (currentStrategy == "QUOTA_REDUCTION") Color(0xFF0284C7) else Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (currentStrategy == "QUOTA_REDUCTION") Color(0xFF0284C7) else Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = if (currentStrategy == "QUOTA_REDUCTION") Color.White else Color(0xFF64748B), modifier = Modifier.size(16.dp))
                            }
                            Text("Disminuir Valor de Cuota", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (currentStrategy == "QUOTA_REDUCTION") Color(0xFF0284C7) else Color(0xFF0F172A))
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFEF3C7)) {
                            Text("Mayor Liquidez", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E), modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                    Text(
                        text = "Mantiene el mismo plazo pero reduce la cuota a pagar cada mes, dándote más holgura financiera.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Strategy 3: Método Avalancha (Mayor Tasa EA)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectStrategy("AVALANCHE") },
                shape = RoundedCornerShape(16.dp),
                color = if (currentStrategy == "AVALANCHE") Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                border = BorderStroke(1.5.dp, if (currentStrategy == "AVALANCHE") Color(0xFF0284C7) else Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (currentStrategy == "AVALANCHE") Color(0xFF0284C7) else Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = if (currentStrategy == "AVALANCHE") Color.White else Color(0xFF64748B), modifier = Modifier.size(16.dp))
                            }
                            Text("Método Avalancha (Mayor Tasa)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (currentStrategy == "AVALANCHE") Color(0xFF0284C7) else Color(0xFF0F172A))
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFDCFCE7)) {
                            Text("Máximo Ahorro", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534), modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                    Text(
                        text = "Abona a las compras con mayor tasa de interés vigente (avances primero), reduciendo el costo financiero total.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Strategy 4: Método Bola de Nieve (Menor Saldo)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectStrategy("SNOWBALL") },
                shape = RoundedCornerShape(16.dp),
                color = if (currentStrategy == "SNOWBALL") Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                border = BorderStroke(1.5.dp, if (currentStrategy == "SNOWBALL") Color(0xFF0284C7) else Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (currentStrategy == "SNOWBALL") Color(0xFF0284C7) else Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = if (currentStrategy == "SNOWBALL") Color.White else Color(0xFF64748B), modifier = Modifier.size(16.dp))
                            }
                            Text("Método Bola de Nieve (Menor Saldo)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (currentStrategy == "SNOWBALL") Color(0xFF0284C7) else Color(0xFF0F172A))
                        }
                    }
                    Text(
                        text = "Cancela primero las compras de menor saldo pendiente, liquidando compromisos más rápido.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Live Impact Projection Analysis
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("ANÁLISIS DE IMPACTO PROYECTADO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)
                    Text(text = impactProjection.explanation, fontSize = 11.sp, color = Color(0xFF334155), lineHeight = 15.sp)

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (currentStrategy == "QUOTA_REDUCTION") {
                            Column {
                                Text("Nueva cuota mensual:", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text(formatMoneyDisplay(impactProjection.newEstimatedMonthlyFee, selectedCurrencyCode), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Ahorro mensual:", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("- ${formatMoneyDisplay(impactProjection.feeReductionAmount, selectedCurrencyCode)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            }
                        } else {
                            Column {
                                Text("Plazo recortado:", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("-${impactProjection.monthsSaved} meses / cuotas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Ahorro en intereses:", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("~ ${formatMoneyDisplay(impactProjection.estimatedInterestSaved, selectedCurrencyCode)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            }
                        }
                    }
                }
            }

            // Buttons
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("confirm_strategy_and_pay_btn")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Aceptar y Procesar Abono", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
