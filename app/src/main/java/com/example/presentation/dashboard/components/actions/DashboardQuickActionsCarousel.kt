package com.example.presentation.dashboard.components.actions

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Data item representing a quick dashboard action in the horizontal carousel
 */
data class DashboardActionItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconTint: Color,
    val iconBgColor: Color,
    val borderGradient: List<Color>,
    val testTag: String,
    val onClick: () -> Unit
)

/**
 * Modern iOS / Fintech Horizontal Quick Actions Carousel for KashApp Dashboard.
 * Allows instant access to:
 * - Nuevo Gasto
 * - Nuevo Ingreso
 * - Transferir
 * - Pagar Tarjeta (Cuota / Abono a capital)
 * - Nueva Compra TC
 * - Crear Cuenta
 * - Crear Meta de Ahorro
 * - Crear Presupuesto
 */
@Composable
fun DashboardQuickActionsCarousel(
    onOpenExpense: () -> Unit,
    onOpenIncome: () -> Unit,
    onOpenTransfer: () -> Unit,
    onOpenCardPayment: () -> Unit,
    onOpenCardPurchase: () -> Unit,
    onOpenCreateAccount: () -> Unit,
    onOpenGoal: () -> Unit,
    onOpenBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = remember(onOpenExpense, onOpenIncome, onOpenTransfer, onOpenCardPayment, onOpenCardPurchase, onOpenCreateAccount, onOpenGoal, onOpenBudget) {
        listOf(
            DashboardActionItem(
                id = "action_expense",
                title = "Gasto",
                subtitle = "Registrar",
                icon = Icons.Default.ArrowOutward,
                iconTint = Color(0xFFDC2626),
                iconBgColor = Color(0xFFFEE2E2),
                borderGradient = listOf(Color(0xFFFCA5A5), Color(0xFFFECACA)),
                testTag = "dash_carousel_expense_btn",
                onClick = onOpenExpense
            ),
            DashboardActionItem(
                id = "action_income",
                title = "Ingreso",
                subtitle = "Recibir",
                icon = Icons.Default.ArrowDownward,
                iconTint = Color(0xFF16A34A),
                iconBgColor = Color(0xFFDCFCE7),
                borderGradient = listOf(Color(0xFF86EFAC), Color(0xFFBBF7D0)),
                testTag = "dash_carousel_income_btn",
                onClick = onOpenIncome
            ),
            DashboardActionItem(
                id = "action_transfer",
                title = "Transferir",
                subtitle = "Entre cuentas",
                icon = Icons.Default.SwapHoriz,
                iconTint = Color(0xFF0284C7),
                iconBgColor = Color(0xFFE0F2FE),
                borderGradient = listOf(Color(0xFF7DD3FC), Color(0xFFBAE6FD)),
                testTag = "dash_carousel_transfer_btn",
                onClick = onOpenTransfer
            ),
            DashboardActionItem(
                id = "action_card_payment",
                title = "Pagar Tarjeta",
                subtitle = "Cuota / Abono",
                icon = Icons.Default.CreditCard,
                iconTint = Color(0xFF7E22CE),
                iconBgColor = Color(0xFFF3E8FF),
                borderGradient = listOf(Color(0xFFD8B4FE), Color(0xFFE9D5FF)),
                testTag = "dash_carousel_card_payment_btn",
                onClick = onOpenCardPayment
            ),
            DashboardActionItem(
                id = "action_card_purchase",
                title = "Compra TC",
                subtitle = "Diferir cuotas",
                icon = Icons.Default.ShoppingBag,
                iconTint = Color(0xFFD97706),
                iconBgColor = Color(0xFFFEF3C7),
                borderGradient = listOf(Color(0xFFFDE68A), Color(0xFFFEF08A)),
                testTag = "dash_carousel_card_purchase_btn",
                onClick = onOpenCardPurchase
            ),
            DashboardActionItem(
                id = "action_account",
                title = "Nueva Cuenta",
                subtitle = "Ahorros/CDT",
                icon = Icons.Default.AccountBalance,
                iconTint = Color(0xFF059669),
                iconBgColor = Color(0xFFECFDF5),
                borderGradient = listOf(Color(0xFF6EE7B7), Color(0xFFA7F3D0)),
                testTag = "dash_carousel_account_btn",
                onClick = onOpenCreateAccount
            ),
            DashboardActionItem(
                id = "action_goal",
                title = "Nueva Meta",
                subtitle = "Plan de ahorro",
                icon = Icons.Default.TrackChanges,
                iconTint = Color(0xFF0891B2),
                iconBgColor = Color(0xFFCFFAFE),
                borderGradient = listOf(Color(0xFF67E8F9), Color(0xFFA5F3FC)),
                testTag = "dash_carousel_goal_btn",
                onClick = onOpenGoal
            ),
            DashboardActionItem(
                id = "action_budget",
                title = "Presupuesto",
                subtitle = "Límites del mes",
                icon = Icons.Default.PieChart,
                iconTint = Color(0xFFE11D48),
                iconBgColor = Color(0xFFFFE4E6),
                borderGradient = listOf(Color(0xFFFDA4AF), Color(0xFFFECDD3)),
                testTag = "dash_carousel_budget_btn",
                onClick = onOpenBudget
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_quick_actions_carousel"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACCIONES RÁPIDAS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 0.6.sp
            )
            Text(
                text = "Desliza para ver más →",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8)
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(actions, key = { it.id }) { action ->
                QuickActionCarouselCard(action = action)
            }
        }
    }
}

@Composable
private fun QuickActionCarouselCard(action: DashboardActionItem) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1.0f, label = "action_scale")

    Surface(
        modifier = Modifier
            .width(116.dp)
            .height(106.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .clickable { action.onClick() }
            .testTag(action.testTag),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            // Icon Bubble
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(action.iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.title,
                    tint = action.iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = action.title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = action.subtitle,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
