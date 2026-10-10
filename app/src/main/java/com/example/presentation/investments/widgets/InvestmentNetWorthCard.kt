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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.investments.state.InvestmentFormState
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import java.util.Locale

@Composable
fun InvestmentNetWorthCard(
    state: InvestmentFormState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onIncludeInNetWorthChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val trmRate = 4150.0 // Default TRM for USD to COP representation
    val effectiveCopValue = if (state.currencyCode == "USD") {
        state.netWorthValue * trmRate
    } else {
        state.netWorthValue
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("investment_net_worth_card"),
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
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = "Patrimonio",
                            tint = KashBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "5. Patrimonio Neto y Activo Global",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashNavy
                        )
                        Text(
                            text = if (state.includeInNetWorth) {
                                "Incluido en patrimonio: $ ${String.format(Locale.GERMANY, "%,.0f", effectiveCopValue)} COP"
                            } else "Excluido del cálculo de patrimonio",
                            fontSize = 12.sp,
                            color = if (state.includeInNetWorth) KashBlue else Color(0xFF64748B)
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
                    // Switch: Incluir en patrimonio
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
                                text = "Incluir en el cálculo de patrimonio neto",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = KashNavy
                            )
                            Text(
                                text = "Suma el valor actual de este activo a la tarjeta Hero del Dashboard",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Switch(
                            checked = state.includeInNetWorth,
                            onCheckedChange = onIncludeInNetWorthChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = KashBlue
                            )
                        )
                    }

                    if (state.includeInNetWorth) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // Read-Only Valor en Patrimonio
                        Text(
                            text = "Valor computable en patrimonio (Read-Only)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KashNavy
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Valorización activa",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "Sincronizado",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A)
                                    )
                                }

                                Text(
                                    text = "$ ${String.format(Locale.GERMANY, "%,.0f", state.netWorthValue)} ${state.currencyCode}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = KashNavy
                                )

                                if (state.currencyCode == "USD") {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "≈ $ ${String.format(Locale.GERMANY, "%,.0f", effectiveCopValue)} COP (TRM Ref. $ 4.150)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KashBlue
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
