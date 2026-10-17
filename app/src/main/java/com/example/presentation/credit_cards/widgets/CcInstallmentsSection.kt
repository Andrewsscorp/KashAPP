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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
private val BlueCalendar = Color(0xFF0284C7)
private val BlueBg = Color(0xFFEFF6FF)

/**
 * Accordion 4: Plazos y Cuotas
 * Handles installment plans, French amortization system, pending installments calculation,
 * and monthly installment payment projections.
 */
@Composable
fun CcInstallmentsSection(
    state: CreditCardFormState,
    onToggleExpand: () -> Unit,
    onTotalInstallmentsChanged: (Int) -> Unit,
    onPaidInstallmentsChanged: (Int) -> Unit,
    onInstallmentValueChanged: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cc_installments_section"),
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
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = BlueCalendar,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Plazos y Cuotas",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate
                    )
                }

                Icon(
                    imageVector = if (state.isInstallmentsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF64748B)
                )
            }

            AnimatedVisibility(
                visible = state.isInstallmentsExpanded,
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    NumberStepperField(
                                        label = "Número total *",
                                        value = state.totalInstallments,
                                        modifier = Modifier.weight(1f),
                                        onValueChange = onTotalInstallmentsChanged
                                    )
                                    NumberStepperField(
                                        label = "Cuotas pagadas *",
                                        value = state.paidInstallments,
                                        modifier = Modifier.weight(1f),
                                        onValueChange = onPaidInstallmentsChanged
                                    )
                                    NumberStepperField(
                                        label = "Pendientes *",
                                        value = state.pendingInstallments,
                                        modifier = Modifier.weight(1f),
                                        onValueChange = {}
                                    )
                                }

                                CurrencyInputField(
                                    label = "Valor de la cuota *",
                                    amount = state.installmentValue,
                                    onAmountChange = onInstallmentValueChanged
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FormSelectorField(
                                        label = "Frecuencia *",
                                        value = state.frequency,
                                        modifier = Modifier.weight(1f),
                                        onClick = {}
                                    )
                                    FormInputField(
                                        label = "Próxima cuota *",
                                        value = state.nextInstallmentDate,
                                        trailingIcon = Icons.Default.CalendarToday,
                                        modifier = Modifier.weight(1f),
                                        onValueChange = {}
                                    )
                                }
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
                                    NumberStepperField(
                                        label = "Número total de cuotas *",
                                        value = state.totalInstallments,
                                        modifier = Modifier.weight(1f),
                                        onValueChange = onTotalInstallmentsChanged
                                    )
                                    NumberStepperField(
                                        label = "Cuotas pagadas *",
                                        value = state.paidInstallments,
                                        modifier = Modifier.weight(1f),
                                        onValueChange = onPaidInstallmentsChanged
                                    )
                                    NumberStepperField(
                                        label = "Cuotas pendientes *",
                                        value = state.pendingInstallments,
                                        modifier = Modifier.weight(1f),
                                        onValueChange = {}
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CurrencyInputField(
                                        label = "Valor de la cuota *",
                                        amount = state.installmentValue,
                                        modifier = Modifier.weight(1f),
                                        onAmountChange = onInstallmentValueChanged
                                    )
                                    FormSelectorField(
                                        label = "Frecuencia *",
                                        value = state.frequency,
                                        modifier = Modifier.weight(1f),
                                        onClick = {}
                                    )
                                    FormInputField(
                                        label = "Próxima cuota *",
                                        value = state.nextInstallmentDate,
                                        trailingIcon = Icons.Default.CalendarToday,
                                        modifier = Modifier.weight(1f),
                                        onValueChange = {}
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sistema de amortización
                    FormSelectorField(
                        label = "Sistema de amortización",
                        value = state.amortizationSystem,
                        onClick = {}
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}
