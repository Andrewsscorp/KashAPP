package com.example.presentation.dashboard.components.topbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.TransactionItemDto
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopAppBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearchOpen: Boolean,
    searchResults: List<TransactionItemDto>,
    onSelectSearchResult: (TransactionItemDto) -> Unit,
    unreadNotificationsCount: Int,
    onNotificationsClick: () -> Unit,
    userName: String = "Andrés Felipe",
    onLogoutClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onSqlCipherSecurityClick: () -> Unit = {},
    onOpenNavDrawer: (() -> Unit)? = null,
    selectedPeriod: YearMonth = YearMonth.now(),
    onPeriodSelected: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCalendarDialog by remember { mutableStateOf(false) }
    var profileMenuExpanded by remember { mutableStateOf(false) }
    var isMobileSearchExpanded by remember { mutableStateOf(false) }

    val monthName = selectedPeriod.month.getDisplayName(TextStyle.FULL, Locale("es", "CO"))
        .replaceFirstChar { it.uppercase() }
    val isCurrentMonth = selectedPeriod == YearMonth.now()
    val periodLabel = if (isCurrentMonth) "$monthName ${selectedPeriod.year} (Actual)" else "$monthName ${selectedPeriod.year}"

    // Material 3 Native Calendar Picker Dialog
    if (showCalendarDialog) {
        val initialEpochMillis = remember(selectedPeriod) {
            selectedPeriod.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialEpochMillis
        )

        DatePickerDialog(
            onDismissRequest = { showCalendarDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCalendarDialog = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedLocalDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            onPeriodSelected(selectedLocalDate.year, selectedLocalDate.monthValue)
                        }
                    },
                    modifier = Modifier.testTag("calendar_confirm_button")
                ) {
                    Text("Filtrar Mes", fontWeight = FontWeight.Bold, color = KashBlue)
                }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            val now = YearMonth.now()
                            showCalendarDialog = false
                            onPeriodSelected(now.year, now.monthValue)
                        },
                        modifier = Modifier.testTag("calendar_today_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = "Mes actual",
                            modifier = Modifier.size(16.dp),
                            tint = KashNavy
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mes actual", color = KashNavy, fontSize = 12.sp)
                    }
                    TextButton(onClick = { showCalendarDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Color.White)
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "Seleccionar Período Financiero",
                        modifier = Modifier.padding(start = 24.dp, top = 20.dp, end = 24.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KashNavy
                    )
                },
                headline = {
                    Text(
                        text = "Elige cualquier fecha del mes a consultar",
                        modifier = Modifier.padding(start = 24.dp, bottom = 12.dp, end = 24.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            )
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_top_app_bar"),
        color = Color.Transparent
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompact = maxWidth < 540.dp
            val isVeryNarrow = maxWidth < 380.dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Primary Top Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Section: Drawer Menu + Avatar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        if (onOpenNavDrawer != null) {
                            IconButton(
                                onClick = onOpenNavDrawer,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("mobile_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menú",
                                    tint = KashNavy,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        // User Profile Chip (Avatar + Name)
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { profileMenuExpanded = true }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("user_profile_chip"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(KashNavy),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "AF",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (!isVeryNarrow) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = userName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KashNavy,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Opciones",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = profileMenuExpanded,
                                onDismissRequest = { profileMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Plan: Premium KashApp") },
                                    onClick = { profileMenuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Firebase: kashapp-a9d56 (Conectado)") },
                                    onClick = { profileMenuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Google Drive: appDataFolder (Activo)") },
                                    onClick = { profileMenuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = KashBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Cifrado SQLCipher (AES-256)", color = KashNavy)
                                        }
                                    },
                                    onClick = {
                                        profileMenuExpanded = false
                                        onSqlCipherSecurityClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = null,
                                                tint = KashBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Ajustes y Configuración", color = KashNavy)
                                        }
                                    },
                                    onClick = {
                                        profileMenuExpanded = false
                                        onSettingsClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Cerrar Sesión", color = Color(0xFFEF4444)) },
                                    onClick = {
                                        profileMenuExpanded = false
                                        onLogoutClick()
                                    }
                                )
                            }
                        }
                    }

                    // Middle Section (Desktop / Tablet inline search bar)
                    if (!isCompact) {
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .padding(horizontal = 12.dp)
                        ) {
                            SearchInputField(
                                searchQuery = searchQuery,
                                onSearchQueryChange = onSearchQueryChange,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Right Actions: Search toggle (on mobile), Notification Bell, Period Calendar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        // On compact devices, show Search icon toggle button
                        if (isCompact) {
                            IconButton(
                                onClick = { isMobileSearchExpanded = !isMobileSearchExpanded },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("mobile_search_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isMobileSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = "Buscar",
                                    tint = if (isMobileSearchExpanded) KashBlue else Color(0xFF475569),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // SQLCipher Security Button
                        IconButton(
                            onClick = onSqlCipherSecurityClick,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("top_bar_sqlcipher_security_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Seguridad SQLCipher",
                                tint = KashBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Notifications Bell with numeric badge
                        BadgedBox(
                            badge = {
                                if (unreadNotificationsCount > 0) {
                                    Badge(
                                        containerColor = Color(0xFFEF4444),
                                        contentColor = Color.White
                                    ) {
                                        Text(text = "$unreadNotificationsCount")
                                    }
                                }
                            },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            IconButton(
                                onClick = onNotificationsClick,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("notifications_bell_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notificaciones",
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // On wide screens, period calendar button is in the top row
                        if (!isCompact) {
                            Spacer(modifier = Modifier.width(8.dp))
                            PeriodCalendarButton(
                                periodLabel = periodLabel,
                                onClick = { showCalendarDialog = true }
                            )
                        }
                    }
                }

                // Mobile Search Bar (Collapsible / Full width)
                AnimatedVisibility(
                    visible = isCompact && isMobileSearchExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        SearchInputField(
                            searchQuery = searchQuery,
                            onSearchQueryChange = onSearchQueryChange,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Floating Search Results Layer
                if (isSearchOpen && searchResults.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("search_results_overlay"),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 8.dp,
                        color = Color.White
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "Resultados encontrados:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            searchResults.take(4).forEach { tx ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onSelectSearchResult(tx) }
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.description,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = KashNavy,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${tx.category} • ${tx.account}",
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = if (tx.isIncome) "+$ ${String.format(Locale.GERMANY, "%,.0f", tx.amount)}" else "-$ ${String.format(Locale.GERMANY, "%,.0f", tx.amount)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (tx.isIncome) Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Subheader Row: Greeting on left, Period Calendar Button on right
                if (isCompact) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "¡Hola, Andrés!",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = KashNavy
                                ),
                                fontSize = 20.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Tu futuro financiero en tus manos.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF64748B)
                                ),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        PeriodCalendarButton(
                            periodLabel = periodLabel,
                            onClick = { showCalendarDialog = true }
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "¡Hola, Andrés!",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = KashNavy
                                ),
                                fontSize = 24.sp
                            )
                            Text(
                                text = "Tu futuro financiero está en tus manos.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF64748B)
                                ),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchInputField(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF1F5F9))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Buscar",
            tint = Color(0xFF64748B),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        BasicTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .weight(1f)
                .testTag("global_search_input"),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = KashNavy,
                fontSize = 14.sp
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(KashBlue),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Buscar movimientos...",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                innerTextField()
            }
        )
        if (searchQuery.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Limpiar",
                tint = Color.Gray,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onSearchQueryChange("") }
            )
        }
    }
}

@Composable
private fun PeriodCalendarButton(
    periodLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag("period_selector_calendar_button"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = "Abrir Calendario",
            tint = KashBlue,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = periodLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = KashNavy,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Abrir calendario",
            tint = Color.Gray,
            modifier = Modifier.size(15.dp)
        )
    }
}
