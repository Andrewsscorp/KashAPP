package com.example.presentation.investments.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.investments.state.InvestmentFormState
import com.example.presentation.investments.state.InvestmentKeyboardTarget
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import java.util.Locale

@Composable
fun InvestmentYieldCard(
    state: InvestmentFormState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onGeneratesYieldChanged: (Boolean) -> Unit,
    onRateTypeSelected: (String) -> Unit,
    onYieldPeriodicitySelected: (String) -> Unit,
    onValueUpdateDateSelected: (String) -> Unit,
    onOpenKeyboard: (InvestmentKeyboardTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    val isYieldActive = state.activeKeyboardTarget is InvestmentKeyboardTarget.YieldRate

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("investment_yield_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Accordion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Percent,
                            contentDescription = "Rendimiento e Intereses",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "3. Rendimiento e Intereses",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashNavy
                        )
                        Text(
                            text = if (state.generatesYield) {
                                "Genera rendimiento: ${String.format(Locale.US, "%.2f", state.yieldRate)}% ${state.rateType} (${when (state.yieldPeriodicity) {
                                    "MONTHLY" -> "Mensual"
                                    "QUARTERLY" -> "Trimestral"
                                    "SEMIANNUAL" -> "Semestral"
                                    "ANNUALLY" -> "Anual"
                                    else -> "Al Vencimiento"
                                }})"
                            } else "Sin tasa fija / Solo plusvalía de capital",
                            fontSize = 12.sp,
                            color = if (state.generatesYield) Color(0xFFD97706) else Color(0xFF64748B)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                    tint = Color(0xFF64748B)
                )
            }

            // Expanded Body
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Switch: ¿Genera rendimiento?
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "¿Genera rendimiento o intereses?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = KashNavy
                            )
                            Text(
                                text = "Activa para renta fija, dividendos periódicos o préstamos con interés",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Switch(
                            checked = state.generatesYield,
                            onCheckedChange = onGeneratesYieldChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = KashBlue
                            )
                        )
                    }

                    if (state.generatesYield) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // Tasa de rendimiento input
                        Text(
                            text = "Tasa de rendimiento (%)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KashNavy
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenKeyboard(InvestmentKeyboardTarget.YieldRate) },
                            color = if (isYieldActive) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                width = if (isYieldActive) 2.dp else 1.dp,
                                color = if (isYieldActive) KashBlue else Color(0xFFCBD5E1)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Tasa proyectada de ganancia",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.2f", state.yieldRate)} %",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = KashNavy
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar tasa",
                                    tint = if (isYieldActive) KashBlue else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Tipo de tasa
                        Text(
                            text = "Tipo de tasa",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KashNavy
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "EA" to "E.A. (Efectiva Anual)",
                                "NOMINAL" to "Nominal",
                                "MENSUAL" to "Mensual",
                                "VARIABLE" to "Variable / IBR"
                            ).forEach { (type, label) ->
                                val isSelected = state.rateType == type
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (isSelected) KashBlue else Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onRateTypeSelected(type) }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label.split(" ").first(),
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) KashBlue else Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Periodicidad de pago
                        Text(
                            text = "Periodicidad de rendimientos",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KashNavy
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "MONTHLY" to "Mensual",
                                "QUARTERLY" to "Trimestral",
                                "SEMIANNUAL" to "Semestral",
                                "ANNUALLY" to "Anual",
                                "MATURITY" to "Vencimiento"
                            ).forEach { (period, label) ->
                                val isSelected = state.yieldPeriodicity == period
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFFFEF3C7) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFD97706) else Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onYieldPeriodicitySelected(period) }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFFB45309) else Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Fecha de actualización / valoración
                        Text(
                            text = "Fecha de próxima liquidación",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KashNavy
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = state.valueUpdateDate,
                            onValueChange = onValueUpdateDateSelected,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Fecha",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = KashNavy,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = KashNavy,
                                unfocusedTextColor = KashNavy,
                                focusedBorderColor = Color(0xFFD97706),
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
