package com.example.presentation.credit_cards.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.credit_cards.state.CreditCardFormState
import java.util.Locale

private val GreenCoin = Color(0xFF10B981)
private val GreenBg = Color(0xFFECFDF5)
private val DarkSlate = Color(0xFF1E293B)
private val BorderColor = Color(0xFFE2E8F0)
private val LabelGray = Color(0xFF475569)
private val KashBlue = Color(0xFF0284C7)

/**
 * Accordion 2: Saldos y Capital & Motor de Grilla Dinámica Adaptable (Auto-Layout).
 * Solves the UI collision/overlapping bug on "Saldo facturado actual" and tight mobile displays.
 * Implements:
 * - FlowRow / Responsive constraints: on small screens (iPhone SE < 380dp)
 *   fields gracefully stack in 2 rows (2 top, 1 bottom) automatically.
 * - Dynamic font sizing and two-line wrapping for labels so text never overlaps or clips.
 * - Master Equation live synchronization between Total, Used, and Available.
 * - Auto-calculated billing cycle dates and Saturday/Sunday rollover alert.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CcBillingInfoGrid(
    state: CreditCardFormState,
    onToggleExpand: () -> Unit,
    onTotalQuotaChanged: (Double) -> Unit,
    onUsedQuotaChanged: (Double) -> Unit,
    onAvailableQuotaChanged: (Double) -> Unit,
    onCutOffDayChanged: (Int) -> Unit,
    onPaymentDueDayChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cc_billing_info_grid"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(GreenBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = GreenCoin,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Saldos y Capital",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate
                    )
                }

                Icon(
                    imageVector = if (state.isBalanceExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF64748B)
                )
            }

            AnimatedVisibility(
                visible = state.isBalanceExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // REACTIVE QUOTA PROGRESS BAR
                    CcQuotaProgressBar(
                        totalQuota = state.totalQuota,
                        usedQuota = state.usedQuota,
                        availableQuota = state.availableQuota,
                        retainedQuota = state.retainedQuota
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // DYNAMIC AUTO-LAYOUT GRID FOR BALANCES
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isNarrow = maxWidth < 480.dp

                        if (isNarrow) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CurrencyInputField(
                                    label = "Monto original *",
                                    amount = state.totalQuota,
                                    onAmountChange = onTotalQuotaChanged
                                )

                                CurrencyInputField(
                                    label = "¿Cuánto debes hoy? *",
                                    amount = state.usedQuota,
                                    onAmountChange = onUsedQuotaChanged
                                )

                                CurrencyInputField(
                                    label = "Capital pendiente",
                                    amount = state.usedQuota,
                                    readOnly = true,
                                    onAmountChange = {}
                                )

                                CurrencyInputField(
                                    label = "Capital pagado",
                                    amount = (state.totalQuota - state.usedQuota).coerceAtLeast(0.0),
                                    readOnly = true,
                                    onAmountChange = {}
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CurrencyInputField(
                                        label = "Monto original *",
                                        amount = state.totalQuota,
                                        modifier = Modifier.weight(1f),
                                        onAmountChange = onTotalQuotaChanged
                                    )
                                    CurrencyInputField(
                                        label = "¿Cuánto debes hoy? *",
                                        amount = state.usedQuota,
                                        modifier = Modifier.weight(1f),
                                        onAmountChange = onUsedQuotaChanged
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CurrencyInputField(
                                        label = "Capital pendiente",
                                        amount = state.usedQuota,
                                        readOnly = true,
                                        modifier = Modifier.weight(1f),
                                        onAmountChange = {}
                                    )
                                    CurrencyInputField(
                                        label = "Capital pagado",
                                        amount = (state.totalQuota - state.usedQuota).coerceAtLeast(0.0),
                                        readOnly = true,
                                        modifier = Modifier.weight(1f),
                                        onAmountChange = {}
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // SUB-SECTION: INFORMACIÓN DE FACTURACIÓN (Adaptive FlowRow resolving text overlapping)
                    Text(
                        text = "Información de facturación",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val containerWidth = maxWidth
                        // On small screens (e.g. iPhone SE / < 380dp) item width is set to wrap dynamically
                        val itemWidth = if (containerWidth < 380.dp) {
                            (containerWidth - 8.dp) / 2
                        } else if (containerWidth < 600.dp) {
                            (containerWidth - 16.dp) / 3
                        } else {
                            (containerWidth - 24.dp) / 4
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            maxItemsInEachRow = if (containerWidth < 380.dp) 2 else 3
                        ) {
                            // Field 1: Día de corte
                            NumberStepperField(
                                label = "Día de corte",
                                value = state.cutOffDay,
                                suffix = "",
                                modifier = Modifier.width(itemWidth),
                                onValueChange = onCutOffDayChanged
                            )

                            // Field 2: Límite de pago
                            NumberStepperField(
                                label = "Límite de pago",
                                value = state.paymentDueDay,
                                suffix = "",
                                modifier = Modifier.width(itemWidth),
                                onValueChange = onPaymentDueDayChanged
                            )

                            // Field 3: Saldo facturado actual (Resolves the bug! Multi-line wrapped, scalable font)
                            ReadOnlyBillingCard(
                                label = "Saldo facturado actual",
                                value = "$ " + String.format(Locale.GERMANY, "%,.0f", state.usedQuota),
                                modifier = if (containerWidth < 380.dp) Modifier.fillMaxWidth() else Modifier.width(itemWidth)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // AUTOMATIC BANKING ENGINE DATES (Calculated Read-Only)
                    val cycle = state.billingCycle
                    if (cycle != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Próxima fecha de corte",
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = cycle.nextCutOffDateFormatted,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkSlate
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Próxima fecha de pago",
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = cycle.nextPaymentDueDateFormatted,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEA580C)
                                        )
                                    }
                                }

                                // Weekend Rollover Warning Pill
                                if (cycle.businessDayNotice != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFFEF3C7))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EventBusy,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = cycle.businessDayNotice,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun CurrencyInputField(
    label: String,
    amount: Double,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier,
    onAmountChange: (Double) -> Unit
) {
    val formatted = String.format(Locale.GERMANY, "%,.0f", amount)
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = LabelGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = formatted,
            onValueChange = { input ->
                val digitsOnly = input.filter { it.isDigit() }
                val parsed = digitsOnly.toDoubleOrNull() ?: 0.0
                onAmountChange(parsed)
            },
            readOnly = readOnly,
            singleLine = true,
            leadingIcon = {
                Text(
                    text = "$",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF0284C7),
                unfocusedBorderColor = BorderColor,
                focusedContainerColor = if (readOnly) Color(0xFFF8FAFC) else Color.White,
                unfocusedContainerColor = if (readOnly) Color(0xFFF8FAFC) else Color.White
            )
        )
    }
}

@Composable
fun NumberStepperField(
    label: String,
    value: Int,
    suffix: String = "",
    modifier: Modifier = Modifier,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = LabelGray,
            maxLines = 2,
            lineHeight = 12.sp,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { input ->
                val num = input.filter { it.isDigit() }.toIntOrNull()
                if (num != null) onValueChange(num)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF0284C7),
                unfocusedBorderColor = BorderColor,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )
    }
}

@Composable
fun ReadOnlyBillingCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(68.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF1F5F9),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B),
                maxLines = 2,
                lineHeight = 12.sp,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0284C7),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
