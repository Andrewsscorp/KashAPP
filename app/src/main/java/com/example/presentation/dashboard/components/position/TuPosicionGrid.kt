package com.example.presentation.dashboard.components.position

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.ReceiptLong
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.dashboard.model.NetWorthState
import com.example.util.MoneyFormatter
import com.example.ui.components.CurrencyText
import java.math.BigDecimal

/**
 * "TU POSICIÓN — Así está tu dinero" (Parte 2, Image 1).
 * 2x2 Grid breakdown with high contrast, elegant typography, and individual item status.
 */
@Composable
fun TuPosicionGrid(
    netWorthState: NetWorthState,
    isPrivacyMode: Boolean = false,
    currencyCode: String = "COP",
    onViewDetailClick: () -> Unit = {},
    onCashClick: () -> Unit = {},
    onInvestmentsClick: () -> Unit = {},
    onDebtsClick: () -> Unit = {},
    onAssetsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header: "TU POSICIÓN" + "Así está tu dinero" + "Ver detalle →"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = "TU POSICIÓN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF007BFF),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Así está tu dinero",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier
                    .clickable { onViewDetailClick() }
                    .padding(vertical = 6.dp, horizontal = 4.dp)
                    .testTag("posicion_ver_detalle"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Ver detalle",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF007BFF)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF007BFF),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2x2 Grid Layout
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Row 1: Dinero disponible & Inversiones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PosicionCard(
                    title = "Dinero disponible",
                    amount = netWorthState.availableCash,
                    subtitle = "0,0% vs. agosto",
                    icon = Icons.Default.AccountBalanceWallet,
                    iconTint = Color(0xFF0284C7),
                    iconBg = Color(0xFFE0F2FE),
                    isPrivacyMode = isPrivacyMode,
                    currencyCode = currencyCode,
                    onClick = onCashClick,
                    modifier = Modifier.weight(1f),
                    testTag = "posicion_dinero_disponible"
                )

                PosicionCard(
                    title = "Inversiones",
                    amount = netWorthState.investments,
                    subtitle = if (netWorthState.investments > BigDecimal.ZERO) "En rendimiento" else "Sin productos activos",
                    icon = Icons.Default.TrendingUp,
                    iconTint = Color(0xFF8B5CF6),
                    iconBg = Color(0xFFF3E8FF),
                    isPrivacyMode = isPrivacyMode,
                    currencyCode = currencyCode,
                    onClick = onInvestmentsClick,
                    modifier = Modifier.weight(1f),
                    testTag = "posicion_inversiones"
                )
            }

            // Row 2: Deudas & Activos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PosicionCard(
                    title = "Deudas",
                    amount = netWorthState.liabilities,
                    subtitle = if (netWorthState.liabilities > BigDecimal.ZERO) "Pagos pendientes" else "Todo al día",
                    icon = Icons.Default.ReceiptLong,
                    iconTint = Color(0xFFEF4444),
                    iconBg = Color(0xFFFEE2E2),
                    amountColor = if (netWorthState.liabilities > BigDecimal.ZERO) Color(0xFFEF4444) else Color(0xFFEF4444),
                    isPrivacyMode = isPrivacyMode,
                    currencyCode = currencyCode,
                    onClick = onDebtsClick,
                    modifier = Modifier.weight(1f),
                    testTag = "posicion_deudas"
                )

                PosicionCard(
                    title = "Activos",
                    amount = netWorthState.assets,
                    subtitle = if (netWorthState.assets > BigDecimal.ZERO) "Bienes valorizados" else "Sin activos registrados",
                    icon = Icons.Default.AccountBalance,
                    iconTint = Color(0xFF0D9488),
                    iconBg = Color(0xFFCCFBF1),
                    isPrivacyMode = isPrivacyMode,
                    currencyCode = currencyCode,
                    onClick = onAssetsClick,
                    modifier = Modifier.weight(1f),
                    testTag = "posicion_activos"
                )
            }
        }
    }
}

@Composable
private fun PosicionCard(
    title: String,
    amount: BigDecimal,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    amountColor: Color = Color(0xFF0F172A),
    isPrivacyMode: Boolean = false,
    currencyCode: String = "COP",
    testTag: String = ""
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Icon Badge + ↗ Arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.NorthEast,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Label
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Amount: Centralized CurrencyText with auto-shrink & ellipsis
            CurrencyText(
                amount = amount,
                currencyCode = currencyCode,
                showCurrencySuffix = (currencyCode != "COP"),
                isPrivacyMode = isPrivacyMode,
                fontSize = 20.sp,
                minFontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor,
                testTag = "${testTag}_amount"
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
