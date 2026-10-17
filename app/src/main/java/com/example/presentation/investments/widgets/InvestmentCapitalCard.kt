package com.example.presentation.investments.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import com.example.presentation.investments.state.InvestmentKeyboardTarget
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import java.util.Locale

@Composable
fun InvestmentCapitalCard(
    state: InvestmentFormState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenKeyboard: (InvestmentKeyboardTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    val roi = state.roiResult
    val isGain = (roi?.capitalGainLoss ?: 0.0) >= 0.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("investment_capital_card"),
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
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Capital y Motor Matemático",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "2. Capital: El Motor Matemático",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashNavy
                        )
                        Text(
                            text = "Valor actual: $ ${String.format(Locale.GERMANY, "%,.0f", state.currentValue)} • ROI: ${String.format(Locale.US, "%.1f", roi?.roiPercentage ?: 0.0)}%",
                            fontSize = 12.sp,
                            color = if (isGain) Color(0xFF16A34A) else Color(0xFFDC2626),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.basicMarquee()
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
                    // Real-time Capital Gains/Losses Hero Banner
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isGain) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, if (isGain) Color(0xFFBBF7D0) else Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isGain) "Plusvalía Neta (Capital Gain)" else "Minusvalía Neta (Capital Loss)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGain) Color(0xFF166534) else Color(0xFF991B1B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${if (isGain) "+" else ""}$ ${String.format(Locale.GERMANY, "%,.0f", roi?.capitalGainLoss ?: 0.0)} ${state.currencyCode}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isGain) Color(0xFF15803D) else Color(0xFFB91C1C)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isGain) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isGain) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        contentDescription = "Tendencia",
                                        tint = if (isGain) Color(0xFF16A34A) else Color(0xFFDC2626),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", roi?.roiPercentage ?: 0.0)}%",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isGain) Color(0xFF16A34A) else Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4 Interactive Capital Value Boxes (Grid 2x2 Auto-Layout)
                    // Row 1: Monto Inicial & Valor Actual
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CapitalValueTile(
                            title = "Monto inicial",
                            subtitle = "Costo base de compra",
                            amount = state.initialAmount,
                            currency = state.currencyCode,
                            isActive = state.activeKeyboardTarget is InvestmentKeyboardTarget.InitialAmount,
                            onClick = { onOpenKeyboard(InvestmentKeyboardTarget.InitialAmount) },
                            modifier = Modifier.weight(1f)
                        )

                        CapitalValueTile(
                            title = "Valor actual",
                            subtitle = "Mark-to-market",
                            amount = state.currentValue,
                            currency = state.currencyCode,
                            isActive = state.activeKeyboardTarget is InvestmentKeyboardTarget.CurrentValue,
                            highlightColor = if (isGain) Color(0xFF16A34A) else Color(0xFFDC2626),
                            onClick = { onOpenKeyboard(InvestmentKeyboardTarget.CurrentValue) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 2: Aportes & Retiros
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CapitalValueTile(
                            title = "Aportes extra",
                            subtitle = "Inyecciones históricas",
                            amount = state.contributions,
                            currency = state.currencyCode,
                            icon = Icons.Default.ArrowUpward,
                            iconTint = KashBlue,
                            isActive = state.activeKeyboardTarget is InvestmentKeyboardTarget.Contributions,
                            onClick = { onOpenKeyboard(InvestmentKeyboardTarget.Contributions) },
                            modifier = Modifier.weight(1f)
                        )

                        CapitalValueTile(
                            title = "Retiros capital",
                            subtitle = "Desinversiones previas",
                            amount = state.withdrawals,
                            currency = state.currencyCode,
                            icon = Icons.Default.ArrowDownward,
                            iconTint = Color(0xFFD97706),
                            isActive = state.activeKeyboardTarget is InvestmentKeyboardTarget.Withdrawals,
                            onClick = { onOpenKeyboard(InvestmentKeyboardTarget.Withdrawals) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun CapitalValueTile(
    title: String,
    subtitle: String,
    amount: Double,
    currency: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlightColor: Color = KashNavy,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Edit,
    iconTint: Color = Color(0xFF64748B)
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = if (isActive) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
        border = BorderStroke(
            width = if (isActive) 2.dp else 1.dp,
            color = if (isActive) KashBlue else Color(0xFFCBD5E1)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KashNavy
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isActive) KashBlue else iconTint,
                    modifier = Modifier.size(15.dp)
                )
            }

            Text(
                text = "$ ${String.format(Locale.GERMANY, "%,.0f", amount)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isActive) KashBlue else highlightColor,
                modifier = Modifier.basicMarquee()
            )

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}
