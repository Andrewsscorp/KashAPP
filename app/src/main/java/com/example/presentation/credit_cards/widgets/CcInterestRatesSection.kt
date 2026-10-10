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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.credit_cards.state.CreditCardFormState
import java.util.Locale

private val DarkSlate = Color(0xFF1E293B)
private val BorderColor = Color(0xFFE2E8F0)
private val LabelGray = Color(0xFF475569)
private val KashBlue = Color(0xFF0284C7)
private val BlueBg = Color(0xFFEFF6FF)

/**
 * Accordion 3: Intereses
 * Implements bank rates, purchases rate with grace period ($0 projected interest on 1 installment),
 * and cash advance interest without grace period (instant daily interest).
 */
@Composable
fun CcInterestRatesSection(
    state: CreditCardFormState,
    onToggleExpand: () -> Unit,
    onGeneratesInterestChanged: (Boolean) -> Unit,
    onInterestRateEaChanged: (Double) -> Unit,
    onPurchasesRateEaChanged: (Double) -> Unit,
    onAdvancesRateEaChanged: (Double) -> Unit,
    onGracePeriodChanged: (Boolean) -> Unit,
    onLateInterestMonthlyChanged: (Double) -> Unit,
    onCurrentInterestAmountChanged: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cc_interest_rates_section"),
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
                            imageVector = Icons.Default.Percent,
                            contentDescription = null,
                            tint = KashBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Intereses",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate
                    )
                }

                Icon(
                    imageVector = if (state.isInterestExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF64748B)
                )
            }

            AnimatedVisibility(
                visible = state.isInterestExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Row 1: ¿Genera intereses? Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "¿Genera intereses? *",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkSlate
                            )
                            Text(
                                text = "Aplica tasas de financiamiento sobre saldos diferidos",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Switch(
                            checked = state.generatesInterest,
                            onCheckedChange = onGeneratesInterestChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = KashBlue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (state.generatesInterest) {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val isNarrow = maxWidth < 480.dp

                            if (isNarrow) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RateInputField(
                                        label = "Tasa de interés *",
                                        value = state.interestRateEa,
                                        suffix = "% E.A.",
                                        onValueChange = onInterestRateEaChanged
                                    )

                                    RateInputField(
                                        label = "Tasa de compras (opcional)",
                                        value = state.purchasesRateEa,
                                        suffix = "% E.A.",
                                        onValueChange = onPurchasesRateEaChanged
                                    )

                                    RateInputField(
                                        label = "Tasa de avances (opcional)",
                                        value = state.advancesRateEa,
                                        suffix = "% E.A.",
                                        onValueChange = onAdvancesRateEaChanged
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RateInputField(
                                        label = "Tasa de interés *",
                                        value = state.interestRateEa,
                                        suffix = "% E.A.",
                                        modifier = Modifier.weight(1f),
                                        onValueChange = onInterestRateEaChanged
                                    )
                                    RateInputField(
                                        label = "Tasa de compras (opcional)",
                                        value = state.purchasesRateEa,
                                        suffix = "% E.A.",
                                        modifier = Modifier.weight(1f),
                                        onValueChange = onPurchasesRateEaChanged
                                    )
                                    RateInputField(
                                        label = "Tasa de avances (opcional)",
                                        value = state.advancesRateEa,
                                        suffix = "% E.A.",
                                        modifier = Modifier.weight(1f),
                                        onValueChange = onAdvancesRateEaChanged
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Switch: ¿Tiene período de gracia?
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "¿Tiene período de gracia?",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DarkSlate
                                )
                                Text(
                                    text = "0% interés en compras canceladas a 1 cuota",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Switch(
                                checked = state.hasGracePeriod,
                                onCheckedChange = onGracePeriodChanged,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = KashBlue
                                )
                            )
                        }

                        // Banking Core Rules Callouts
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEFF6FF))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = KashBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tasa de compras (22% EA): Si pagas a 1 cuota antes de la fecha límite, genera $0 de interés proyectado.",
                                fontSize = 10.sp,
                                color = Color(0xFF0369A1)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEF2F2))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tasa de avances (28% EA): Los retiros en cajero no tienen periodo de gracia y liquidan interés diario desde el segundo 1.",
                                fontSize = 10.sp,
                                color = Color(0xFF991B1B)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Interés de mora & Interés corriente
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RateInputField(
                                label = "Interés de mora (opcional)",
                                value = state.lateInterestRateMonthly,
                                suffix = "% Mensual",
                                modifier = Modifier.weight(1f),
                                onValueChange = onLateInterestMonthlyChanged
                            )
                            CurrencyInputField(
                                label = "Interés corriente (opcional)",
                                amount = state.currentInterestAmount,
                                modifier = Modifier.weight(1f),
                                onAmountChange = onCurrentInterestAmountChanged
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun RateInputField(
    label: String,
    value: Double,
    suffix: String = "%",
    modifier: Modifier = Modifier,
    onValueChange: (Double) -> Unit
) {
    val formatted = String.format(Locale.US, "%.2f", value)
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
                val cleaned = input.replace(",", ".").filter { it.isDigit() || it == '.' }
                val parsed = cleaned.toDoubleOrNull() ?: 0.0
                onValueChange(parsed)
            },
            singleLine = true,
            trailingIcon = {
                Text(
                    text = suffix,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(end = 8.dp)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
