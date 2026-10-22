package com.example.presentation.dashboard.components.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import com.example.core.ui.components.IosStyleSwitch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashCyan
import com.example.ui.theme.KashNavy

/**
 * Production-ready Settings & Configuration Modal (Ventana de Ajustes) for KashApp.
 * Provides user management, currency defaults, privacy mode toggles, SQLCipher security,
 * cloud backup status, notification preferences, and local Room cache diagnostics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsModal(
    onDismiss: () -> Unit,
    userName: String,
    userEmail: String,
    defaultCurrency: String,
    isPrivacyModeActive: Boolean,
    notificationsCardsAlert: Boolean,
    notificationsBudgetAlert: Boolean,
    notificationsPayrollAlert: Boolean,
    biometricLockEnabled: Boolean,
    accountsCount: Int,
    transactionsCount: Int,
    onSaveProfile: (name: String, email: String) -> Unit,
    onCurrencySelected: (String) -> Unit,
    onTogglePrivacyMode: () -> Unit,
    onToggleCardAlerts: (Boolean) -> Unit,
    onToggleBudgetAlerts: (Boolean) -> Unit,
    onTogglePayrollAlerts: (Boolean) -> Unit,
    onToggleBiometricLock: (Boolean) -> Unit,
    onOpenSqlCipherSecurity: () -> Unit,
    onBackupNow: () -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var editableName by remember { mutableStateOf(userName) }
    var editableEmail by remember { mutableStateOf(userEmail) }
    var isEditingProfile by remember { mutableStateOf(false) }
    var backupStatusMessage by remember { mutableStateOf<String?>(null) }
    var cacheClearedMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF8FAFC),
        dragHandle = null,
        modifier = modifier.testTag("settings_modal_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            // Header Bar
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(KashBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Ajustes",
                                tint = KashBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Ajustes y Configuración",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = KashNavy
                            )
                            Text(
                                text = "Preferencias, seguridad y sincronización",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .testTag("close_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar ajustes",
                            tint = KashNavy,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status feedbacks
                if (backupStatusMessage != null) {
                    StatusNoticeBox(
                        message = backupStatusMessage ?: "",
                        isSuccess = true,
                        onDismiss = { backupStatusMessage = null }
                    )
                }
                if (cacheClearedMessage != null) {
                    StatusNoticeBox(
                        message = cacheClearedMessage ?: "",
                        isSuccess = true,
                        onDismiss = { cacheClearedMessage = null }
                    )
                }

                // 1. Perfil & Plan Premium
                SettingsSectionCard(title = "Cuenta y Perfil", icon = Icons.Default.AccountCircle) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(KashNavy, KashBlue)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = editableName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "KA" },
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = editableName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = KashNavy
                            )
                            Text(
                                text = editableEmail,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFEF3C7))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Plan Premium KashApp ✨",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (isEditingProfile) {
                                    onSaveProfile(editableName, editableEmail)
                                }
                                isEditingProfile = !isEditingProfile
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEditingProfile) KashBlue else Color(0xFFE2E8F0),
                                contentColor = if (isEditingProfile) Color.White else KashNavy
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("edit_profile_toggle_btn")
                        ) {
                            Text(
                                text = if (isEditingProfile) "Listo" else "Editar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    AnimatedVisibility(visible = isEditingProfile) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = editableName,
                                onValueChange = { editableName = it },
                                label = { Text("Nombre completo") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_name_input"),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = KashBlue,
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )
                            OutlinedTextField(
                                value = editableEmail,
                                onValueChange = { editableEmail = it },
                                label = { Text("Correo electrónico") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_email_input"),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = KashBlue,
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )
                        }
                    }
                }

                // 2. Preferencias Financieras
                SettingsSectionCard(title = "Preferencias Financieras", icon = Icons.Default.AttachMoney) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Moneda Principal de la Cuenta",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KashNavy
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val currencies = listOf("COP" to "Peso ($ COP)", "USD" to "Dólar ($ USD)", "EUR" to "Euro (€ EUR)")
                            currencies.forEach { (code, label) ->
                                val isSelected = defaultCurrency.equals(code, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onCurrencySelected(code) },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = KashBlue,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("currency_chip_$code")
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // Switch: Modo Privacidad
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPrivacyModeActive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = KashBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Modo Privacidad",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KashNavy
                                    )
                                    Text(
                                        text = "Oculta saldos monetarios en el panel principal (••••)",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                            IosStyleSwitch(
                                checked = isPrivacyModeActive,
                                onCheckedChange = { onTogglePrivacyMode() },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue),
                                modifier = Modifier.testTag("settings_privacy_mode_switch")
                            )
                        }
                    }
                }

                // 3. Seguridad de Datos & Blindaje
                SettingsSectionCard(title = "Seguridad & Cifrado Local", icon = Icons.Default.Shield) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // SQLCipher Status Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF0FDF4))
                                .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(12.dp))
                                .clickable { onOpenSqlCipherSecurity() }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Cifrado SQLCipher AES-256",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                    Text(
                                        text = "Base de datos Room cifrada en reposo",
                                        fontSize = 11.sp,
                                        color = Color(0xFF166534)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "Gestionar",
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Biometrics Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = KashNavy,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Bloqueo Biométrico / Huella",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KashNavy
                                    )
                                    Text(
                                        text = "Solicitar autenticación al abrir KashApp",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                            IosStyleSwitch(
                                checked = biometricLockEnabled,
                                onCheckedChange = onToggleBiometricLock,
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue),
                                modifier = Modifier.testTag("settings_biometric_switch")
                            )
                        }
                    }
                }

                // 4. Sincronización & Nube
                SettingsSectionCard(title = "Nube y Copias de Seguridad", icon = Icons.Default.Backup) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Google Drive & Firebase Sync",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KashNavy
                                )
                                Text(
                                    text = "Sincronización híbrida offline-first activa",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onBackupNow()
                                backupStatusMessage = "Copia de respaldo generada y guardada con éxito en Google Drive ✓"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("backup_now_button")
                        ) {
                            Icon(imageVector = Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Generar Copia de Seguridad Inmediata", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 5. Notificaciones & Alertas
                SettingsSectionCard(title = "Alertas y Notificaciones", icon = Icons.Default.Notifications) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NotificationToggleRow(
                            title = "Vencimiento de Tarjetas",
                            subtitle = "Avisar 3 días antes de la fecha límite de pago",
                            checked = notificationsCardsAlert,
                            onCheckedChange = onToggleCardAlerts,
                            testTag = "alert_cards_switch"
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        NotificationToggleRow(
                            title = "Control de Presupuesto",
                            subtitle = "Notificar cuando un sobre supere el 80% del límite",
                            checked = notificationsBudgetAlert,
                            onCheckedChange = onToggleBudgetAlerts,
                            testTag = "alert_budget_switch"
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        NotificationToggleRow(
                            title = "Registro de Nómina",
                            subtitle = "Recordatorio para conciliar abonos de sueldo",
                            checked = notificationsPayrollAlert,
                            onCheckedChange = onTogglePayrollAlerts,
                            testTag = "alert_payroll_switch"
                        )
                    }
                }

                // 6. Almacenamiento Local & Diagnóstico
                SettingsSectionCard(title = "Almacenamiento Local", icon = Icons.Default.Storage) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Cuentas registradas:", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text(text = "$accountsCount cuentas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KashNavy)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Movimientos en SQLite:", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text(text = "$transactionsCount transacciones", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KashNavy)
                        }

                        Button(
                            onClick = {
                                onClearCache()
                                cacheClearedMessage = "Caché de consultas y vistas previas temporal limpiada ✓"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF1F5F9),
                                contentColor = KashNavy
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("clear_cache_btn")
                        ) {
                            Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Limpiar Caché Temporal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // 7. Información y Versión
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "KashApp Finanzas Enterprise v2.4.0",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "Arquitectura Limpia con Jetpack Compose y SQLCipher",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            // Bottom CTA
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = {
                            if (isEditingProfile) {
                                onSaveProfile(editableName, editableEmail)
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_settings_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Guardar y Cerrar Ajustes",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = KashBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = KashNavy
                )
            }
            content()
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = KashNavy
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }
        IosStyleSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun StatusNoticeBox(
    message: String,
    isSuccess: Boolean,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSuccess) Color(0xFFDCFCE7) else Color(0xFFFEE2E2))
            .border(1.dp, if (isSuccess) Color(0xFF86EFAC) else Color(0xFFFCA5A5), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSuccess) Color(0xFF166534) else Color(0xFF991B1B),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = if (isSuccess) Color(0xFF166534) else Color(0xFF991B1B),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
