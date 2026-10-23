package com.example.presentation.dashboard.components.topbar

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.example.features.settings.data.UserProfileSecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.dashboard.model.PeriodState
import com.example.util.MoneyFormatter
import com.example.core.ui.components.CurrencyText
import java.math.BigDecimal
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrackChanges

/**
 * Pixel-Perfect Hero Header matching the exact design specification from image.png:
 * - Logo + Brand "KashAPP" + Notification bell with unread dot + Avatar bubble (MG)
 * - Greeting: "Hola, [UserName]" in cursive script + Subtitle "Tu panorama financiero, en un vistazo."
 * - Period dropdown pill (e.g. "Septiembre ▼")
 * - Translucent "Patrimonio neto" card with "$ 0 COP", trend percentage and "Actualizado hoy" timestamp
 * - Horizontal Scrollable Carousel of Quick action round buttons: Gasto, Ingreso, Transferir, Pagar TC, Compra TC, Cuenta, Metas, Presupuesto, Escanear, Más
 */
@Composable
fun DashboardHeroHeader(
    userName: String,
    periodState: PeriodState,
    totalNetWorth: BigDecimal,
    isPrivacyMode: Boolean,
    onPeriodSelected: (YearMonth) -> Unit,
    onMenuClick: () -> Unit = {},
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    onExpenseClick: () -> Unit = {},
    onIncomeClick: () -> Unit = {},
    onTransferClick: () -> Unit = {},
    onCardPaymentClick: () -> Unit = {},
    onCardPurchaseClick: () -> Unit = {},
    onCreateAccountClick: () -> Unit = {},
    onGoalClick: () -> Unit = {},
    onBudgetClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onScanClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    displayCurrency: String = "COP",
    isConvertedToUsd: Boolean = false,
    usdExchangeRate: Double = 4142.50,
    onToggleUsdConversion: () -> Unit = {},
    onOpenTrmRateClick: () -> Unit = {},
    onSyncClick: () -> Unit = {},
    unreadNotificationCount: Int = 0,
    highestAlertSeverity: String = "INFO",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val avatarTrigger by UserProfileSecurityManager.avatarUpdateTrigger.collectAsState()
    var avatarBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(avatarTrigger, userName) {
        val path = UserProfileSecurityManager.resolveAvatarPath(context)
        avatarBitmap = if (!path.isNullOrBlank() && File(path).exists()) {
            withContext(Dispatchers.IO) {
                try {
                    BitmapFactory.decodeFile(path)
                } catch (_: Throwable) {
                    null
                }
            }
        } else {
            null
        }
    }

    val colombianLocale = remember { Locale("es", "CO") }
    var isPeriodDropdownOpen by remember { mutableStateOf(false) }

    val monthName = periodState.currentMonth.month.getDisplayName(TextStyle.FULL, colombianLocale)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(colombianLocale) else it.toString() }

    val formattedTime = remember {
        val now = LocalTime.now()
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale("es", "CO"))
        now.format(timeFormatter).lowercase()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF03172B),
                        Color(0xFF072948),
                        Color(0xFF0B3A62)
                    )
                )
            )
            .testTag("dashboard_hero_header")
    ) {
        // Subtle decorative concentric wave arcs
        Canvas(
            modifier = Modifier.matchParentSize()
        ) {
            val w = size.width
            val h = size.height

            drawArc(
                color = Color(0xFF38BDF8).copy(alpha = 0.07f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.35f, h * 0.35f),
                size = androidx.compose.ui.geometry.Size(w * 0.9f, h * 0.9f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 14.dp)
        ) {
            // ==========================================
            // TOP BAR: Hamburger Menu + Brand Logo + Notification + Avatar
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hamburger Menu + Logo & KashAPP Brand
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .testTag("dashboard_hamburger_menu")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menú Principal",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Image(
                        painter = painterResource(id = com.example.R.drawable.logo_nuevo),
                        contentDescription = "KashAPP Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .shadow(4.dp, RoundedCornerShape(10.dp))
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "KashAPP",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }

                // Right Actions: Cloud Sync + Notification Bell + Avatar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cloud Sync Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable { onSyncClick() }
                            .testTag("dashboard_cloud_sync_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sincronizar con la nube",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Notification Bell with dynamic unread count badge & severity color
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable { onNotificationClick() }
                            .testTag("dashboard_notification_bell"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Centro de Notificaciones",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        if (unreadNotificationCount > 0) {
                            val badgeColor = when (highestAlertSeverity) {
                                "CRITICAL" -> Color(0xFFFF3B30)
                                "WARNING" -> Color(0xFFFF9500)
                                else -> Color(0xFF0A84FF)
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 3.dp, end = 2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(badgeColor)
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadNotificationCount > 99) "99+" else unreadNotificationCount.toString(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Avatar Circle (Real Photo or Initials)
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { onProfileClick() }
                            .testTag("dashboard_avatar_bubble")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val bmp = avatarBitmap
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Foto de perfil de $userName",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                val initials = userName.split(" ")
                                    .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
                                    .take(2)
                                    .joinToString("")
                                    .ifEmpty { if (userName.length >= 2) userName.take(2).uppercase() else "KA" }
                                Text(
                                    text = initials,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // GREETING + PERIOD SELECTOR ROW
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hola, ${userName.ifBlank { "María" }}",
                        fontSize = 28.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        lineHeight = 32.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Tu panorama financiero, en un vistazo.",
                        fontSize = 13.sp,
                        color = Color(0xFFCBD5E1).copy(alpha = 0.9f)
                    )
                }

                // Right side: Month Selector Pill + USD Conversion Toggle underneath
                Column(horizontalAlignment = Alignment.End) {
                    Box {
                        Surface(
                            color = Color(0xFF1E3A5A).copy(alpha = 0.8f),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { isPeriodDropdownOpen = true }
                                .testTag("dashboard_period_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = monthName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Cambiar mes",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isPeriodDropdownOpen,
                            onDismissRequest = { isPeriodDropdownOpen = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            val currentYearMonth = YearMonth.now()
                            (0..5).forEach { offset ->
                                val targetMonth = currentYearMonth.minusMonths(offset.toLong())
                                val targetName = targetMonth.month.getDisplayName(TextStyle.FULL, colombianLocale)
                                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(colombianLocale) else it.toString() }
                                val isSelected = targetMonth == periodState.currentMonth

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (targetMonth == currentYearMonth) "$targetName (Actual)" else "$targetName ${targetMonth.year}",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFF007BFF) else Color(0xFF0F172A)
                                        )
                                    },
                                    onClick = {
                                        isPeriodDropdownOpen = false
                                        onPeriodSelected(targetMonth)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // USD Conversion button below date selector
                    Surface(
                        onClick = onToggleUsdConversion,
                        shape = RoundedCornerShape(12.dp),
                        color = if (isConvertedToUsd) Color(0xFF0284C7) else Color.White.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isConvertedToUsd) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("dashboard_toggle_usd_pill_under_date")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Cambiar divisa",
                                tint = if (isConvertedToUsd) Color.White else Color(0xFF38BDF8),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isConvertedToUsd) "🇺🇸 USD Activo" else "⇄ Ver en USD",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isConvertedToUsd) Color.White else Color(0xFF38BDF8)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // TRANSLUCENT NET WORTH HERO CARD
            // ==========================================
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0C2B4B).copy(alpha = 0.65f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFF38BDF8).copy(alpha = 0.22f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("net_worth_hero_card_translucent")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Card Top: Clean "Patrimonio neto" + Trend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Patrimonio neto",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "0,0% este mes",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Big Amount: Fluid font size + Interactive Currency Button right beside the currency name
                    val rawNetWorthStr = if (isPrivacyMode) "••••••" else MoneyFormatter.format(totalNetWorth, isPrivacyMode, displayCurrency)
                    val dynamicHeroFontSize = when {
                        rawNetWorthStr.length > 17 -> 22.sp
                        rawNetWorthStr.length > 14 -> 25.sp
                        rawNetWorthStr.length > 11 -> 28.sp
                        else -> 32.sp
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isPrivacyMode) {
                            Text(
                                text = "••••••",
                                fontSize = dynamicHeroFontSize,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        } else {
                            CurrencyText(
                                amount = totalNetWorth,
                                currencyCode = displayCurrency,
                                fontSize = dynamicHeroFontSize,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Currency Badge + Interactive Swap button right beside the currency name
                        Surface(
                            onClick = onToggleUsdConversion,
                            shape = RoundedCornerShape(8.dp),
                            color = if (isConvertedToUsd) Color(0xFF0284C7) else Color.White.copy(alpha = 0.14f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isConvertedToUsd) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .padding(bottom = 3.dp)
                                .testTag("dashboard_toggle_usd_beside_currency")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (displayCurrency == "USD") "USD 🇺🇸" else "$displayCurrency 🇨🇴",
                                    fontSize = (dynamicHeroFontSize.value * 0.42f).coerceAtLeast(11f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isConvertedToUsd) Color.White else Color(0xFF38BDF8)
                                )
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Cambiar divisa",
                                    tint = if (isConvertedToUsd) Color.White else Color(0xFF38BDF8),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (isConvertedToUsd) "Volver" else "USD",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isConvertedToUsd) Color.White.copy(alpha = 0.9f) else Color.White
                                )
                            }
                        }
                    }

                    // Converted View Notice
                    if (isConvertedToUsd) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0284C7).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tasa: 1 USD = ${MoneyFormatter.format(BigDecimal.valueOf(usdExchangeRate), false, "COP")} · Vista convertida (Saldos reales intactos)",
                                fontSize = 11.sp,
                                color = Color(0xFFBAE6FD),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    onClick = onOpenTrmRateClick,
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Ver TRM",
                                        fontSize = 11.sp,
                                        color = Color(0xFFBAE6FD),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    onClick = onToggleUsdConversion,
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF38BDF8).copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = "Restaurar",
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Updated timestamp
                    Text(
                        text = "Actualizado hoy · $formattedTime",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // HORIZONTAL CAROUSEL OF QUICK ACTIONS (IN BLUE HERO)
            // ==========================================
            val heroActions = remember(
                onExpenseClick,
                onIncomeClick,
                onTransferClick,
                onCardPaymentClick,
                onCardPurchaseClick,
                onCreateAccountClick,
                onGoalClick,
                onBudgetClick,
                onScanClick,
                onMoreClick
            ) {
                listOf(
                    HeroActionItem("act_expense", "Gasto", Icons.Default.ArrowOutward, Color(0xFFFCA5A5), Color(0xFF3B1720), Color(0xFFF87171).copy(alpha = 0.35f), "quick_action_expense", onExpenseClick),
                    HeroActionItem("act_income", "Ingreso", Icons.Default.ArrowDownward, Color(0xFF86EFAC), Color(0xFF133829), Color(0xFF4ADE80).copy(alpha = 0.35f), "quick_action_income", onIncomeClick),
                    HeroActionItem("act_transfer", "Transferir", Icons.Default.SwapHoriz, Color(0xFF7DD3FC), Color(0xFF163A5E), Color(0xFF38BDF8).copy(alpha = 0.35f), "quick_action_transfer", onTransferClick),
                    HeroActionItem("act_card_payment", "Pagar TC", Icons.Default.CreditCard, Color(0xFFD8B4FE), Color(0xFF311E4E), Color(0xFFC084FC).copy(alpha = 0.35f), "quick_action_card_payment", onCardPaymentClick),
                    HeroActionItem("act_card_purchase", "Compra TC", Icons.Default.ShoppingBag, Color(0xFFFDE68A), Color(0xFF382813), Color(0xFFFBBF24).copy(alpha = 0.35f), "quick_action_card_purchase", onCardPurchaseClick),
                    HeroActionItem("act_account", "Cuenta", Icons.Default.AccountBalance, Color(0xFF6EE7B7), Color(0xFF15392D), Color(0xFF34D399).copy(alpha = 0.35f), "quick_action_account", onCreateAccountClick),
                    HeroActionItem("act_goal", "Metas", Icons.Default.TrackChanges, Color(0xFF67E8F9), Color(0xFF143B45), Color(0xFF22D3EE).copy(alpha = 0.35f), "quick_action_goal", onGoalClick),
                    HeroActionItem("act_budget", "Presupuesto", Icons.Default.PieChart, Color(0xFFFDA4AF), Color(0xFF3A1926), Color(0xFFFB7185).copy(alpha = 0.35f), "quick_action_budget", onBudgetClick),
                    HeroActionItem("act_scan", "Escanear", Icons.Default.CropFree, Color.White, Color(0xFF1D456E).copy(alpha = 0.65f), Color.White.copy(alpha = 0.15f), "quick_action_scan", onScanClick),
                    HeroActionItem("act_more", "Más", Icons.Default.MoreHoriz, Color.White, Color(0xFF1D456E).copy(alpha = 0.65f), Color.White.copy(alpha = 0.15f), "quick_action_more", onMoreClick)
                )
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_actions_horizontal_carousel"),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(heroActions, key = { it.id }) { action ->
                    QuickActionRoundButton(
                        label = action.label,
                        icon = action.icon,
                        tint = action.iconTint,
                        bg = action.bgTint,
                        border = action.borderTint,
                        onClick = action.onClick,
                        testTag = action.testTag
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

private data class HeroActionItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val iconTint: Color,
    val bgTint: Color,
    val borderTint: Color,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
private fun QuickActionRoundButton(
    label: String,
    icon: ImageVector,
    tint: Color = Color.White,
    bg: Color = Color(0xFF1D456E).copy(alpha = 0.65f),
    border: Color = Color.White.copy(alpha = 0.15f),
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(bg)
                .border(1.dp, border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}
