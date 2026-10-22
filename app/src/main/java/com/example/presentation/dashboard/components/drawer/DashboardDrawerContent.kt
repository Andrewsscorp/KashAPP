package com.example.presentation.dashboard.components.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.dashboard.model.DashboardUiState
import com.example.util.MoneyFormatter

@Composable
fun DashboardDrawerContent(
    uiState: DashboardUiState,
    onCloseDrawer: () -> Unit,
    onNavigateHome: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenCreateExpense: () -> Unit,
    onOpenCreateIncome: () -> Unit,
    onOpenCreateTransfer: () -> Unit,
    onOpenCardPurchase: (() -> Unit)? = null,
    onOpenBudgets: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenCreditCards: () -> Unit,
    onOpenCurrencyTrm: (() -> Unit)? = null,
    onOpenFixedAssets: (() -> Unit)? = null,
    onOpenReceiptScanner: (() -> Unit)? = null,
    onOpenInformes: (() -> Unit)? = null,
    onOpenDianTaxRadar: (() -> Unit)? = null,
    onOpenThirdParties: (() -> Unit)? = null,
    onOpenUpcomingPayments: (() -> Unit)? = null,
    onOpenSharedExpenses: (() -> Unit)? = null,
    onOpenAutoCapture: (() -> Unit)? = null,
    onOpenQuickCash: (() -> Unit)? = null,
    onOpenAuditVault: (() -> Unit)? = null,
    onOpenSettings: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier
            .width(320.dp)
            .fillMaxHeight(),
        drawerContainerColor = Color(0xFF0F172A),
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            // ==========================================
            // DRAWER HEADER: Gradient Profile Box
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF032B43),
                                Color(0xFF061E33),
                                Color(0xFF0F172A)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Brand Logo
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = com.example.R.drawable.logo_nuevo),
                                contentDescription = "KashAPP Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "KashAPP",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Enterprise Finance",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }

                        // Close button
                        IconButton(
                            onClick = onCloseDrawer,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .testTag("close_drawer_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar menú",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // User Profile Card
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0284C7),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "CS",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (uiState.userName.isNotBlank()) uiState.userName else "Carlos Suarez",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "PRO",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF34D399),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "cdosuarez@gmail.com",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Patrimonio: ${MoneyFormatter.format(uiState.netWorth.totalNetWorth, uiState.isPrivacyMode)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // DRAWER NAVIGATION ITEMS
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "PRINCIPAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 4.dp)
                )

                DrawerMenuItem(
                    icon = Icons.Default.Home,
                    label = "Tablero Principal",
                    badgeText = null,
                    iconColor = Color(0xFF38BDF8),
                    isSelected = true,
                    onClick = {
                        onCloseDrawer()
                        onNavigateHome()
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.AccountBalance,
                    label = "Mis Cuentas Bancarias",
                    badgeText = "${uiState.accounts.size}",
                    iconColor = Color(0xFF38BDF8),
                    isSelected = false,
                    onClick = {
                        onCloseDrawer()
                        onOpenAccounts()
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.SwapHoriz,
                    label = "Transferencias",
                    badgeText = "En vivo",
                    iconColor = Color(0xFF818CF8),
                    isSelected = false,
                    onClick = {
                        onCloseDrawer()
                        onOpenCreateTransfer()
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                    color = Color.White.copy(alpha = 0.08f)
                )

                Text(
                    text = "TRANSACCIONES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 4.dp)
                )

                DrawerMenuItem(
                    icon = Icons.Default.TrendingDown,
                    label = "Registrar Gasto",
                    badgeText = "- Gastos",
                    iconColor = Color(0xFFF87171),
                    isSelected = false,
                    onClick = {
                        onCloseDrawer()
                        onOpenCreateExpense()
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.TrendingUp,
                    label = "Registrar Ingreso",
                    badgeText = "+ Ingresos",
                    iconColor = Color(0xFF34D399),
                    isSelected = false,
                    onClick = {
                        onCloseDrawer()
                        onOpenCreateIncome()
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.CreditCard,
                    label = "Compra con Tarjeta",
                    badgeText = "Diferido",
                    iconColor = Color(0xFFA78BFA),
                    isSelected = false,
                    onClick = {
                        onCloseDrawer()
                        onOpenCardPurchase?.invoke()
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.ReceiptLong,
                    label = "Gastos Compartidos & Tickets",
                    badgeText = "NIIF / QR",
                    iconColor = Color(0xFF34D399),
                    isSelected = false,
                    onClick = {
                        onCloseDrawer()
                        onOpenSharedExpenses?.invoke()
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.NotificationsActive,
                    label = "Captura Automática Bancos",
                    badgeText = "Auto",
                    iconColor = Color(0xFF10B981),
                    isSelected = false,
                    onClick = {
                        onCloseDrawer()
                        onOpenAutoCapture?.invoke()
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.Payments,
                    label = "Gasto Rápido en Efectivo",
                    badgeText = "1 Toque",
                    iconColor = Color(0xFF38BDF8),
                    isSelected = false,
                    onClick = {
                        onCloseDrawer()
                        onOpenQuickCash?.invoke()
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                    color = Color.White.copy(alpha = 0.08f)
                )

                Text(
                    text = "PLANEACIÓN Y PASIVOS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 4.dp)
                )

                DrawerMenuItem(
                    icon = Icons.Default.PieChart,
                    label = "Presupuestos & Sobres",
                    badgeText = null,
                    iconColor = Color(0xFFFBBF24),
                    isSelected = false,
                    onClick = {
                        onCloseDrawer()
                        onOpenBudgets()
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.Flag,
                    label = "Metas de Ahorro",
                    badgeText = "${uiState.goals.size}",
                    iconColor = Color(0xFF34D399),
                    isSelected = false,
                    onClick = {
                        onCloseDrawer()
                        onOpenGoals()
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.CreditCard,
                    label = "Tarjetas de Crédito",
                    badgeText = "${uiState.creditCards.size}",
                    iconColor = Color(0xFFA78BFA),
                    isSelected = false,
                    onClick = {
                        onCloseDrawer()
                        onOpenCreditCards()
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.Payments,
                    label = "Próximos Pagos & Deudas",
                    badgeText = "Alertas",
                    iconColor = Color(0xFF38BDF8),
                    isSelected = false,
                    onClick = {
                        onCloseDrawer()
                        onOpenUpcomingPayments?.invoke()
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.Language,
                    label = "Moneda y TRM",
                    badgeText = "Frankfurter",
                    iconColor = Color(0xFF38BDF8),
                    isSelected = false,
                    onClick = {
                        onCloseDrawer()
                        onOpenCurrencyTrm?.invoke()
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    label = "Activos Fijos y Depreciación",
                    badgeText = "NIIF",
                    iconColor = Color(0xFF10B981),
                    isSelected = false,
                    onClick = {
                        onCloseDrawer()
                        onOpenFixedAssets?.invoke()
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.ContactPhone,
                    label = "Gestión de Terceros",
                    badgeText = "Directorio",
                    iconColor = Color(0xFF0284C7),
                    isSelected = false,
                    onClick = {
                        onCloseDrawer()
                        onOpenThirdParties?.invoke()
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.DocumentScanner,
                    label = "Escáner OCR Facturas",
                    badgeText = "IA/OCR",
                    iconColor = Color(0xFF38BDF8),
                    isSelected = false,
                    onClick = {
                        onCloseDrawer()
                        onOpenReceiptScanner?.invoke()
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.Assessment,
                    label = "Informes Financieros",
                    badgeText = "Central",
                    iconColor = Color(0xFFFBBF24),
                    isSelected = false,
                    onClick = {
                        onCloseDrawer()
                        onOpenInformes?.invoke()
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.AccountBalance,
                    label = "Impuestos DIAN & Global",
                    badgeText = "UVT / 4x1000",
                    iconColor = Color(0xFF38BDF8),
                    isSelected = false,
                    onClick = {
                        onCloseDrawer()
                        onOpenDianTaxRadar?.invoke()
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                    color = Color.White.copy(alpha = 0.08f)
                )

                DrawerMenuItem(
                    icon = Icons.Default.Security,
                    label = "Bóveda de Auditoría",
                    badgeText = "Inmutable",
                    iconColor = Color(0xFF38BDF8),
                    isSelected = false,
                    onClick = {
                        onCloseDrawer()
                        onOpenAuditVault?.invoke()
                    }
                )

                DrawerMenuItem(
                    icon = Icons.Default.Settings,
                    label = "Configuración y Seguridad",
                    badgeText = null,
                    iconColor = Color(0xFF94A3B8),
                    isSelected = false,
                    onClick = {
                        onCloseDrawer()
                        onOpenSettings()
                    }
                )

                DrawerMenuItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    label = "Cerrar Sesión",
                    badgeText = null,
                    iconColor = Color(0xFFEF4444),
                    isSelected = false,
                    onClick = {
                        onCloseDrawer()
                        onLogoutClick()
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // FOOTER: Version info
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "KashApp Enterprise v2.4.0 • Room ACID Offline",
                    fontSize = 10.sp,
                    color = Color(0xFF475569)
                )
            }
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    badgeText: String?,
    iconColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.2f) else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .testTag("drawer_item_${label.lowercase().replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                modifier = Modifier.weight(1f)
            )

            if (badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
