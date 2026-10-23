package com.example.feature.notificaciones.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.NotificationEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class NotificationFilterTab(val label: String) {
    ALL("Todas"),
    CRITICAL("Urgentes"),
    TAX_DIAN("DIAN & 4x1000"),
    CARDS_LIQUIDITY("Tarjetas & Liquidez"),
    FIREBASE_CLOUD("Nube & Bancos")
}

@Composable
fun NotificationCenterScreen(
    notifications: List<NotificationEntity>,
    unreadCount: Int,
    onNavigateBack: () -> Unit,
    onOpenDeepLink: (NotificationEntity) -> Unit,
    onMarkAsRead: (String) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onSnoozeTwoHours: (String) -> Unit,
    onArchive: (String) -> Unit,
    onRunProactiveScan: () -> Unit,
    onSendFirebasePush: (title: String, body: String, category: String, severity: String, route: String, isBroadcast: Boolean) -> Unit,
    statusMessage: String? = null
) {
    var selectedTab by remember { mutableStateOf(NotificationFilterTab.ALL) }
    var showFirebaseStudioModal by remember { mutableStateOf(false) }

    val criticalCount = remember(notifications) {
        notifications.count { !it.isRead && (it.severity == "CRITICAL" || it.severity == "WARNING") }
    }
    val dianCount = remember(notifications) {
        notifications.count { it.category == "TAX_DIAN" || it.category == "BANK_GMF" }
    }
    val cloudCount = remember(notifications) {
        notifications.count {
            it.sourceOrigin.contains("FCM") ||
                it.sourceOrigin.contains("FIRESTORE") ||
                it.category == "FIREBASE_CLOUD" ||
                it.category == "AUTO_CAPTURE"
        }
    }

    val filteredList = remember(notifications, selectedTab) {
        when (selectedTab) {
            NotificationFilterTab.ALL -> notifications
            NotificationFilterTab.CRITICAL -> notifications.filter { it.severity == "CRITICAL" || it.severity == "WARNING" }
            NotificationFilterTab.TAX_DIAN -> notifications.filter { it.category == "TAX_DIAN" || it.category == "BANK_GMF" }
            NotificationFilterTab.CARDS_LIQUIDITY -> notifications.filter { it.category == "CREDIT_CARD" || it.category == "LIQUIDITY_AI" }
            NotificationFilterTab.FIREBASE_CLOUD -> notifications.filter {
                it.sourceOrigin.contains("FCM") ||
                    it.sourceOrigin.contains("FIRESTORE") ||
                    it.category == "FIREBASE_CLOUD" ||
                    it.category == "AUTO_CAPTURE" ||
                    it.category == "SHARED_EXPENSE"
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF2F2F7), // Fondo auténtico iOS Grouped Background
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF03172B), Color(0xFF0A2E52), Color(0xFF144272))
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Volver",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Centro de Notificaciones",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (unreadCount > 0) {
                                        "$unreadCount alerta(s) sin leer • Aislamiento seguro por cuenta"
                                    } else {
                                        "Todo al día • Sincronizado con Firebase & DIAN"
                                    },
                                    fontSize = 11.sp,
                                    color = Color(0xFFBAE6FD),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = onRunProactiveScan,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Escanear módulos",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = onMarkAllAsRead,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Marcar todas como leídas",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Tira de KPIs estilo iPhone Widgets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IosSummaryMetricPill(
                            label = "Urgentes",
                            count = criticalCount,
                            accentColor = Color(0xFFFF453A),
                            modifier = Modifier.weight(1f)
                        )
                        IosSummaryMetricPill(
                            label = "DIAN / 4x1000",
                            count = dianCount,
                            accentColor = Color(0xFFFF9F0A),
                            modifier = Modifier.weight(1f)
                        )
                        IosSummaryMetricPill(
                            label = "Cloud & Bancos",
                            count = cloudCount,
                            accentColor = Color(0xFF0A84FF),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            val isCompact = maxWidth < 360.dp
            val horizontalPad = if (isCompact) 12.dp else 16.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .padding(horizontal = horizontalPad, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tarjeta estilo iPhone para abrir el Laboratorio Push Firebase
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, Color(0xFFE5E5EA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF007AFF).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = Color(0xFF007AFF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Firebase Cloud & Push Studio",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C1C1E)
                                )
                                Text(
                                    text = "Simula alertas remotas seguras para tu cuenta o país.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF8E8E93),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            onClick = { showFirebaseStudioModal = true },
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF007AFF)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Probar Push",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                if (!statusMessage.isNullOrBlank()) {
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF007AFF).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = statusMessage,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E3A8A)
                            )
                        }
                    }
                }

                // Segmented Filter Pills estilo iOS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NotificationFilterTab.entries.forEach { tab ->
                        val selected = tab == selectedTab
                        val bgColor by animateColorAsState(
                            targetValue = if (selected) Color(0xFF007AFF) else Color.White,
                            label = "tabBg"
                        )
                        val txtColor by animateColorAsState(
                            targetValue = if (selected) Color.White else Color(0xFF3A3A3C),
                            label = "tabTxt"
                        )
                        Surface(
                            color = bgColor,
                            shape = RoundedCornerShape(50),
                            shadowElevation = if (selected) 2.dp else 0.dp,
                            border = BorderStroke(
                                1.dp,
                                if (selected) Color(0xFF007AFF) else Color(0xFFD1D1D6)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { selectedTab = tab }
                        ) {
                            Text(
                                text = tab.label,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = txtColor,
                                modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                if (filteredList.isEmpty()) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5E5EA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 34.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34C759).copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFF34C759),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = "Sin alertas pendientes en esta vista",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1C1E)
                            )
                            Text(
                                text = "Tus topes DIAN, cupos exentos de 4x1000, tarjetas de crédito y liquidez de esta cuenta están bajo control.",
                                fontSize = 12.sp,
                                color = Color(0xFF8E8E93),
                                lineHeight = 17.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredList, key = { it.id }) { item ->
                            NotificationItemCard(
                                item = item,
                                onOpenAction = {
                                    onMarkAsRead(item.id)
                                    onOpenDeepLink(item)
                                },
                                onSnoozeTwoHours = { onSnoozeTwoHours(item.id) },
                                onArchive = { onArchive(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFirebaseStudioModal) {
        FirebasePushStudioDialog(
            onDismiss = { showFirebaseStudioModal = false },
            onSend = { title, body, cat, sev, route, isBroadcast ->
                showFirebaseStudioModal = false
                onSendFirebasePush(title, body, cat, sev, route, isBroadcast)
            }
        )
    }
}

@Composable
private fun IosSummaryMetricPill(
    label: String,
    count: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = count.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFCBD5E1),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
        }
    }
}

@Composable
private fun NotificationItemCard(
    item: NotificationEntity,
    onOpenAction: () -> Unit,
    onSnoozeTwoHours: () -> Unit,
    onArchive: () -> Unit
) {
    // Paleta de sistema iOS (Red, Orange, Green, Blue)
    val accentColor = when (item.severity) {
        "CRITICAL" -> Color(0xFFFF3B30)
        "WARNING" -> Color(0xFFFF9500)
        "SUCCESS" -> Color(0xFF34C759)
        else -> Color(0xFF007AFF)
    }

    val categoryFriendlyLabel = remember(item.category) {
        when (item.category) {
            "TAX_DIAN" -> "Radar DIAN"
            "BANK_GMF" -> "Control 4x1000"
            "CREDIT_CARD" -> "Tarjeta de Crédito"
            "LIQUIDITY_AI" -> "Liquidez 7 Días"
            "AUTO_CAPTURE" -> "Auto-Captura"
            "SHARED_EXPENSE" -> "Gasto Compartido"
            else -> "Firebase Cloud"
        }
    }

    val formattedTime = remember(item.createdAtEpochMs) {
        runCatching {
            SimpleDateFormat("d MMM · h:mm a", Locale("es", "CO")).format(Date(item.createdAtEpochMs))
        }.getOrDefault("Reciente")
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (item.isRead) Color.White.copy(alpha = 0.92f) else Color.White
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isRead) 0.dp else 2.dp),
        border = BorderStroke(
            width = if (item.isRead) 1.dp else 1.5.dp,
            color = if (item.isRead) Color(0xFFE5E5EA) else accentColor.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenAction() }
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = accentColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = categoryFriendlyLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = Color(0xFF8E8E93)
                    )
                }

                if (!item.isRead) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Text(
                            text = "Nueva",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
            }

            Text(
                text = item.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E),
                lineHeight = 20.sp
            )

            Text(
                text = item.body,
                fontSize = 13.sp,
                color = Color(0xFF3A3A3C),
                lineHeight = 18.sp
            )

            HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        onClick = onSnoozeTwoHours,
                        color = Color(0xFFF2F2F7),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF636366),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Posponer 2h",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF3A3A3C)
                            )
                        }
                    }

                    Surface(
                        onClick = onArchive,
                        color = Color(0xFFF2F2F7),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = null,
                                tint = Color(0xFF8E8E93),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Archivar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF636366)
                            )
                        }
                    }
                }

                Surface(
                    onClick = onOpenAction,
                    color = accentColor,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = item.actionLabel ?: "Abrir",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FirebasePushStudioDialog(
    onDismiss: () -> Unit,
    onSend: (title: String, body: String, category: String, severity: String, route: String, isBroadcast: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("🇨🇴 Alerta DIAN: Actualización UVT 2026") }
    var body by remember { mutableStateOf("Verifica tu cupo exento de 350 UVT y tus facturas electrónicas del mes.") }
    var category by remember { mutableStateOf("TAX_DIAN") }
    var severity by remember { mutableStateOf("WARNING") }
    var route by remember { mutableStateOf("RADAR_DIAN") }
    // Por defecto personal (false) para garantizar aislamiento total con la cuenta activa
    var isBroadcast by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFFF2F2F7),
            tonalElevation = 6.dp,
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 500.dp)
                .heightIn(max = 680.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Grabber iOS
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(38.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFC7C7CC))
                    )
                }

                Text(
                    text = "Laboratorio Push Firebase",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1C1C1E)
                )
                Text(
                    text = "Envía una alerta en tiempo real a tu bandeja cifrada y barra de estado de Android.",
                    fontSize = 12.sp,
                    color = Color(0xFF8E8E93)
                )

                // 1. PLANTILLAS MASIVAS (PARA TODOS LOS USUARIOS: MANTENIMIENTO, PROMOCIONES, INFORMACIÓN)
                Text(
                    text = "📢 PLANTILLAS MASIVAS (ENVIAR A TODOS)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF007AFF)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetChip("🛠️ Mantenimiento") {
                        title = "🛠️ Mantenimiento Programado KashApp"
                        body = "Optimizaremos nuestros servidores hoy de 11:00 PM a 11:30 PM. Tu bóveda local cifrada seguirá funcionando sin interrupciones."
                        category = "FIREBASE_CLOUD"
                        severity = "WARNING"
                        route = "DASHBOARD"
                        isBroadcast = true
                    }
                    PresetChip("🎁 Promoción") {
                        title = "🎁 Beneficio Especial KashApp Pro"
                        body = "Desbloquea reportes ilimitados Pre-Renta DIAN, auditoría patrimonial avanzada y respaldo multi-nube con descuento exclusivo."
                        category = "FIREBASE_CLOUD"
                        severity = "INFO"
                        route = "DASHBOARD"
                        isBroadcast = true
                    }
                    PresetChip("ℹ️ Información / Novedad") {
                        title = "✨ Nueva Actualización KashApp 9.0"
                        body = "Ya tienes disponible el nuevo Radar Tributario DIAN 2026, control de 4x1000 en tiempo real y bandeja de Auto-Captura."
                        category = "FIREBASE_CLOUD"
                        severity = "INFO"
                        route = "RADAR_DIAN"
                        isBroadcast = true
                    }
                    PresetChip("🛡️ Aviso Seguridad") {
                        title = "🛡️ Comunicado Oficial de Seguridad"
                        body = "Recuerda mantener activa tu huella biométrica. KashApp jamás te solicitará tu PIN o semilla criptográfica por correo o SMS."
                        category = "FIREBASE_CLOUD"
                        severity = "CRITICAL"
                        route = "DASHBOARD"
                        isBroadcast = true
                    }
                }

                // 2. PLANTILLAS FINANCIERAS PERSONALES (SOLO MI CUENTA)
                Text(
                    text = "👤 PLANTILLAS FINANCIERAS (MI CUENTA)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8E8E93)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetChip("🇨🇴 Tope DIAN") {
                        title = "🚨 Tope DIAN cerca del límite: Ingresos Brutos"
                        body = "Alcanzaste el 88% del tope de 1.400 UVT. Revisa tu simulación de Renta."
                        category = "TAX_DIAN"
                        severity = "WARNING"
                        route = "RADAR_DIAN"
                        isBroadcast = false
                    }
                    PresetChip("💸 4x1000 Excedido") {
                        title = "💸 Cupo de 350 UVT superado este mes"
                        body = "Tus retiros superaron el tope exento mensual en tu cuenta principal."
                        category = "BANK_GMF"
                        severity = "CRITICAL"
                        route = "RADAR_DIAN"
                        isBroadcast = false
                    }
                    PresetChip("💳 Pago Tarjeta") {
                        title = "💳 Fecha límite de pago en 2 días"
                        body = "Evita intereses moratorios realizando el pago oportuno de tu tarjeta."
                        category = "CREDIT_CARD"
                        severity = "CRITICAL"
                        route = "PAY_CREDIT_CARD"
                        isBroadcast = false
                    }
                    PresetChip("📲 Auto-Captura") {
                        title = "📲 Compra bancaria detectada"
                        body = "Nuevo movimiento detectado por confirmación en la bandeja de Auto-Captura."
                        category = "AUTO_CAPTURE"
                        severity = "INFO"
                        route = "AUTO_CAPTURE"
                        isBroadcast = false
                    }
                }

                // Campos dentro de tarjeta blanca estilo iOS Grouped List
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E5EA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Título de la alerta") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF007AFF),
                                unfocusedBorderColor = Color(0xFFD1D1D6)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = body,
                            onValueChange = { body = it },
                            label = { Text("Mensaje detallado") },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF007AFF),
                                unfocusedBorderColor = Color(0xFFD1D1D6)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Selector de Severidad estilo iOS
                        Text(
                            text = "Severidad",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF3A3A3C)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "CRITICAL" to Color(0xFFFF3B30),
                                "WARNING" to Color(0xFFFF9500),
                                "INFO" to Color(0xFF007AFF)
                            ).forEach { (sev, color) ->
                                val selected = severity == sev
                                Surface(
                                    onClick = { severity = sev },
                                    color = if (selected) color else color.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = sev,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) Color.White else color
                                        )
                                    }
                                }
                            }
                        }

                        // Selector de Ruta Deep-Link
                        Text(
                            text = "Módulo destino al tocar (Deep-Link)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF3A3A3C)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "RADAR_DIAN" to ("TAX_DIAN" to "Radar DIAN"),
                                "PAY_CREDIT_CARD" to ("CREDIT_CARD" to "Pagar TC"),
                                "SHARED_EXPENSES" to ("SHARED_EXPENSE" to "Compartidos"),
                                "AUTO_CAPTURE" to ("AUTO_CAPTURE" to "Auto-Captura")
                            ).forEach { (routeCode, catAndLabel) ->
                                val selected = route == routeCode
                                Surface(
                                    onClick = {
                                        route = routeCode
                                        category = catAndLabel.first
                                    },
                                    color = if (selected) Color(0xFF007AFF) else Color(0xFFF2F2F7),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = catAndLabel.second,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else Color(0xFF3A3A3C),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Switch estilo iPhone para elegir Personal vs Broadcast
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E5EA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBroadcast) Icons.Default.Warning else Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (isBroadcast) Color(0xFFFF9500) else Color(0xFF34C759),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isBroadcast) "Modo Broadcast (País)" else "Solo mi cuenta actual (Aislado)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C1C1E)
                                )
                            }
                            Text(
                                text = if (isBroadcast) {
                                    "Publica en el canal general de la jurisdicción activa."
                                } else {
                                    "Garantiza que la alerta solo se guarde en tu usuario activo."
                                },
                                fontSize = 11.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        IosCupertinoToggle(
                            checked = isBroadcast,
                            onCheckedChange = { isBroadcast = it }
                        )
                    }
                }

                // Botones de acción estilo iPhone
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFD1D1D6)),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = "Cancelar",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF3A3A3C)
                        )
                    }
                    Button(
                        onClick = { onSend(title, body, category, severity, route, isBroadcast) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = "Disparar Push",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.White,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, Color(0xFFD1D1D6))
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF007AFF),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun IosCupertinoToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF34C759) else Color(0xFFE5E5EA),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "iosTrack"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "iosThumb"
    )
    Box(
        modifier = Modifier
            .width(51.dp)
            .height(31.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(27.dp)
                .shadow(elevation = 3.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
