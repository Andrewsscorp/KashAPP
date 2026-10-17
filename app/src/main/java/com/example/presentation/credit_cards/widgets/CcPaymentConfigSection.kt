package com.example.presentation.credit_cards.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import com.example.presentation.credit_cards.state.CreditCardFormState

private val DarkSlate = Color(0xFF1E293B)
private val KashBlue = Color(0xFF0284C7)
private val BlueBg = Color(0xFFEFF6FF)

/**
 * Accordion 5: Configuración de Pagos & Automatización KashApp.
 * Handles automatic debits, auto-transfers from source accounts (e.g. Payroll/Savings),
 * minimum payment rules, scheduled payments, and extra payment debt reduction strategies.
 */
@Composable
fun CcPaymentConfigSection(
    state: CreditCardFormState,
    onToggleExpand: () -> Unit,
    onPaymentMethodChanged: (String) -> Unit,
    onSourceAccountChanged: (String, String) -> Unit,
    onRegularPaymentDayChanged: (Int) -> Unit,
    onMinimumPaymentEnabledChanged: (Boolean) -> Unit,
    onMinimumPaymentAmountChanged: (Double) -> Unit,
    onScheduledPaymentEnabledChanged: (Boolean) -> Unit,
    onScheduledPaymentDateChanged: (String) -> Unit,
    onExtraPaymentsEnabledChanged: (Boolean) -> Unit,
    onExtraPaymentReducesChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var sourceAccountMenuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cc_payment_config_section"),
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
                            .background(BlueBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = null,
                            tint = KashBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Configuración de Pagos",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate
                    )
                }

                Icon(
                    imageVector = if (state.isPaymentConfigExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF64748B)
                )
            }

            AnimatedVisibility(
                visible = state.isPaymentConfigExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isNarrow = maxWidth < 480.dp

                        if (isNarrow) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FormSelectorField(
                                    label = "Método de pago *",
                                    value = state.paymentMethod,
                                    onClick = {}
                                )

                                Box {
                                    FormSelectorField(
                                        label = "Cuenta desde la cual se paga *",
                                        value = state.sourceAccount,
                                        leadingIcon = Icons.Default.AccountBalance,
                                        onClick = { sourceAccountMenuOpen = true }
                                    )
                                    DropdownMenu(
                                        expanded = sourceAccountMenuOpen,
                                        onDismissRequest = { sourceAccountMenuOpen = false }
                                    ) {
                                        state.availableSourceAccounts.forEach { (id, name) ->
                                            DropdownMenuItem(
                                                text = { Text(name) },
                                                onClick = {
                                                    onSourceAccountChanged(id, name)
                                                    sourceAccountMenuOpen = false
                                                }
                                            )
                                        }
                                    }
                                }

                                NumberStepperField(
                                    label = "Día habitual de pago *",
                                    value = state.regularPaymentDay,
                                    onValueChange = onRegularPaymentDayChanged
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FormSelectorField(
                                    label = "Método de pago *",
                                    value = state.paymentMethod,
                                    modifier = Modifier.weight(1f),
                                    onClick = {}
                                )

                                Box(modifier = Modifier.weight(1.2f)) {
                                    FormSelectorField(
                                        label = "Cuenta desde la cual se paga *",
                                        value = state.sourceAccount,
                                        leadingIcon = Icons.Default.AccountBalance,
                                        onClick = { sourceAccountMenuOpen = true }
                                    )
                                    DropdownMenu(
                                        expanded = sourceAccountMenuOpen,
                                        onDismissRequest = { sourceAccountMenuOpen = false }
                                    ) {
                                        state.availableSourceAccounts.forEach { (id, name) ->
                                            DropdownMenuItem(
                                                text = { Text(name) },
                                                onClick = {
                                                    onSourceAccountChanged(id, name)
                                                    sourceAccountMenuOpen = false
                                                }
                                            )
                                        }
                                    }
                                }

                                NumberStepperField(
                                    label = "Día habitual *",
                                    value = state.regularPaymentDay,
                                    modifier = Modifier.weight(0.8f),
                                    onValueChange = onRegularPaymentDayChanged
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Pago mínimo Switch & Valor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Pago mínimo:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = DarkSlate
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = state.minimumPaymentEnabled,
                                onCheckedChange = onMinimumPaymentEnabledChanged,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = KashBlue
                                )
                            )
                        }

                        if (state.minimumPaymentEnabled) {
                            CurrencyInputField(
                                label = "Valor mínimo",
                                amount = state.minimumPaymentAmount,
                                modifier = Modifier.width(160.dp),
                                onAmountChange = onMinimumPaymentAmountChanged
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Pago programado Switch & Fecha
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Pago programado:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = DarkSlate
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = state.scheduledPaymentEnabled,
                                onCheckedChange = onScheduledPaymentEnabledChanged,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = KashBlue
                                )
                            )
                        }

                        if (state.scheduledPaymentEnabled) {
                            FormInputField(
                                label = "Fecha de pago programado",
                                value = state.scheduledPaymentDate,
                                trailingIcon = Icons.Default.CalendarToday,
                                modifier = Modifier.width(160.dp),
                                onValueChange = onScheduledPaymentDateChanged
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Abonos extraordinarios & Qué reduce
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Abonos extraordinarios:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = DarkSlate
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = state.extraPaymentsEnabled,
                                onCheckedChange = onExtraPaymentsEnabledChanged,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = KashBlue
                                )
                            )
                        }

                        if (state.extraPaymentsEnabled) {
                            FormSelectorField(
                                label = "¿Qué reduce el pago?",
                                value = state.extraPaymentReduces,
                                modifier = Modifier.width(160.dp),
                                onClick = {}
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Automation notice
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF0FDF4))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Automatización activa: KashApp programará el débito desde ${state.sourceAccount} para saldar la cuota en tu fecha de corte.",
                            fontSize = 10.sp,
                            color = Color(0xFF15803D)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}
