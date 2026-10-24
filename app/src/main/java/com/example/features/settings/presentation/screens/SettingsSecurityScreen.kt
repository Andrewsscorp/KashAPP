package com.example.features.settings.presentation.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.KashAppApplication
import com.example.core.ui.components.IosStyleSwitch
import com.example.domain.models.SecurityConfig
import com.example.domain.repository.AuthRepository
import com.example.features.settings.security.SecurityCenterCoordinator
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Pantalla Empresarial 100% Real de "Seguridad" (Estética iPhone Cupertino).
 * - Biometría real de hardware (`BIOMETRIC_STRONG` + `CryptoObject` de Android Keystore)
 * - Bloqueo en vivo de capturas y grabación de pantalla (`WindowManager.LayoutParams.FLAG_SECURE`)
 * - Cifrado Local SQLCipher 4.6.0 AES-256 y Frase de Paso Maestra (`PBKDF2-HMAC-SHA256` 310.000 iteraciones OWASP)
 * - Gestión de PIN (`setupPin` / `changePin`) con `PBKDF2-HMAC-SHA256` y bloqueo anti-fuerza bruta
 * - Sesiones reales del dispositivo físico (`Build.MANUFACTURER` + `Build.MODEL` + `UserSessionDao` + Firestore)
 * - Bitácora criptográfica en vivo (`AuditSecurityLogDao`)
 */
@Composable
fun SettingsSecurityScreen(
    userId: String = "user_default",
    biometricsEnabled: Boolean,
    onToggleBiometrics: (Boolean) -> Unit,
    onShowMessage: (String) -> Unit = {},
    onBack: () -> Unit,
    allowScreenshots: Boolean = false,
    onToggleAllowScreenshots: (Boolean) -> Unit = {},
    authRepository: AuthRepository? = null,
    onChangePin: (suspend (currentPin: String, newPin: String) -> Result<Unit>)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val coordinator = remember(context, authRepository, userId) {
        val appContainer = (context.applicationContext as? KashAppApplication)?.appContainer
        SecurityCenterCoordinator(
            context = context,
            authRepository = authRepository ?: appContainer?.authRepository,
            settingsRepository = appContainer?.settingsRepository,
            userSessionDao = appContainer?.securityDatabase?.userSessionDao(),
            auditSecurityLogDao = appContainer?.securityDatabase?.auditSecurityLogDao()
        )
    }

    val securityConfig by coordinator.observeSecurityConfig().collectAsState(
        initial = SecurityConfig(
            hasPinSet = false,
            isBiometricEnabled = biometricsEnabled,
            failedAttempts = 0,
            maxAllowedAttempts = 3,
            isLockedOut = false,
            lockoutRemainingSeconds = 0L
        )
    )
    val activeSessions by coordinator.observeRealActiveSessions(userId).collectAsState(initial = emptyList())
    val recentAuditLogs by coordinator.observeRecentSecurityLogs(userId, limit = 6).collectAsState(initial = emptyList())

    var encryptionInfo by remember { mutableStateOf(coordinator.getSqlCipherEncryptionInfo()) }
    val hardwareBiometricSupported = remember { coordinator.isBiometricHardwareAvailable() }

    // Estados de modales
    var showPinDialog by remember { mutableStateOf(false) }
    var showVaultPassphraseDialog by remember { mutableStateOf(false) }

    // Campos de PIN
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }
    var pinDialogError by remember { mutableStateOf<String?>(null) }

    // Campos de Frase de Paso Maestra SQLCipher
    var masterPassphraseInput by remember { mutableStateOf("") }
    var masterPassphraseConfirm by remember { mutableStateOf("") }
    var passphraseDialogError by remember { mutableStateOf<String?>(null) }

    var bannerFeedback by remember { mutableStateOf<String?>(null) }
    var isBannerError by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        coordinator.syncCurrentDeviceHeartbeatToFirestore(userId)
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "CO")) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        bannerFeedback?.let { msg ->
            Surface(
                color = if (isBannerError) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isBannerError) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isBannerError) Color(0xFFDC2626) else Color(0xFF15803D),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = msg,
                        color = if (isBannerError) Color(0xFF991B1B) else Color(0xFF166534),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { bannerFeedback = null }, modifier = Modifier.size(20.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = if (isBannerError) Color(0xFF991B1B) else Color(0xFF166534),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // =====================================================================
        // TARJETA 1: HARDWARE, BIOMETRÍA, CAPTURAS, SQLCIPHER Y PIN PBKDF2
        // =====================================================================
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Hardware y Biometría",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = KashNavy
                )

                // 1. Bloqueo Biométrico Real con verificación en hardware
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bloqueo Biométrico",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = KashNavy
                        )
                        Text(
                            text = if (hardwareBiometricSupported) {
                                "Solicitar huella o reconocimiento facial (BiometricPrompt) al abrir la app"
                            } else {
                                "Hardware biométrico no configurado en este dispositivo Android"
                            },
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IosStyleSwitch(
                        checked = biometricsEnabled,
                        enabled = hardwareBiometricSupported,
                        onCheckedChange = { targetState ->
                            val fragAct = context.findActivity() as? FragmentActivity
                            if (fragAct != null) {
                                coordinator.toggleBiometricLockWithHardwarePrompt(
                                    activity = fragAct,
                                    userId = userId,
                                    targetEnabled = targetState,
                                    onSuccess = { confirmedState ->
                                        coroutineScope.launch {
                                            coordinator.persistBiometricSetting(userId, confirmedState)
                                            onToggleBiometrics(confirmedState)
                                            isBannerError = false
                                            val msg = if (confirmedState) {
                                                "Huella verificada: Bloqueo biométrico activado en Room y Keystore."
                                            } else {
                                                "Bloqueo biométrico desactivado tras confirmar identidad."
                                            }
                                            bannerFeedback = msg
                                            onShowMessage(msg)
                                        }
                                    },
                                    onError = { err ->
                                        isBannerError = true
                                        bannerFeedback = err
                                    }
                                )
                            } else {
                                coroutineScope.launch {
                                    coordinator.persistBiometricSetting(userId, targetState)
                                    onToggleBiometrics(targetState)
                                    onShowMessage(if (targetState) "Bloqueo biométrico activado" else "Bloqueo biométrico desactivado")
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue),
                        modifier = Modifier.testTag("security_switch_biometric")
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // 2. Permitir Capturas de Pantalla con aplicación inmediata de FLAG_SECURE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Permitir Capturas de Pantalla",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = KashNavy
                        )
                        Text(
                            text = if (allowScreenshots) {
                                "Activado: FLAG_SECURE deshabilitado en la ventana actual"
                            } else {
                                "Protegido (Recomendado): Android bloquea capturas y grabación de pantalla (FLAG_SECURE)"
                            },
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IosStyleSwitch(
                        checked = allowScreenshots,
                        onCheckedChange = { allowed ->
                            val act = context.findActivity()
                            coroutineScope.launch {
                                coordinator.applyScreenshotPolicy(act, userId, allowed)
                                onToggleAllowScreenshots(allowed)
                                isBannerError = false
                                val msg = if (allowed) {
                                    "Capturas de pantalla habilitadas en esta sesión."
                                } else {
                                    "Protección FLAG_SECURE activada: capturas y grabación bloqueadas."
                                }
                                bannerFeedback = msg
                                onShowMessage(msg)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue),
                        modifier = Modifier.testTag("security_switch_screenshots")
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // 3. Cifrado Local de Base de Datos (Estado real desde PassphraseManager)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Cifrado Local de Base de Datos",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = KashNavy
                    )
                    Text(
                        text = "${encryptionInfo.cipherEngine} • ${encryptionInfo.keyDerivation}",
                        fontSize = 12.sp,
                        color = Color(0xFF059669),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = encryptionInfo.statusDescription,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedButton(
                        onClick = {
                            masterPassphraseInput = ""
                            masterPassphraseConfirm = ""
                            passphraseDialogError = null
                            showVaultPassphraseDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, KashBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("security_btn_vault_passphrase")
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = KashBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (encryptionInfo.hasUserPassphrase) {
                                "Rotar o Restablecer Frase Maestra SQLCipher"
                            } else {
                                "Configurar Frase de Paso Maestra (PBKDF2 310k iter)"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashBlue
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // 4. Claves de Acceso y PIN (Detecta si ya existe PIN o si debe crearse por primera vez)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Claves de Acceso y PIN (PBKDF2-HMAC-SHA256)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = KashNavy
                    )
                    if (securityConfig.isLockedOut) {
                        Text(
                            text = "⚠️ PIN bloqueado temporalmente por intentos fallidos (${securityConfig.lockoutRemainingSeconds}s restantes).",
                            fontSize = 11.sp,
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = if (securityConfig.hasPinSet) {
                                "PIN activo y protegido con derivación criptográfica PBKDF2 y sal aleatoria."
                            } else {
                                "Aún no has creado un PIN de desbloqueo rápido para este dispositivo."
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Button(
                        onClick = {
                            currentPinInput = ""
                            newPinInput = ""
                            confirmPinInput = ""
                            pinDialogError = null
                            showPinDialog = true
                        },
                        enabled = !securityConfig.isLockedOut,
                        colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("security_btn_change_pin")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (securityConfig.hasPinSet) {
                                "Cambiar PIN de Acceso (4-8 dígitos)"
                            } else {
                                "Crear PIN de Acceso (4-8 dígitos)"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // =====================================================================
        // TARJETA 2: DISPOSITIVOS Y SESIONES ACTIVAS (100% REALES, SIN MACBOOK FALSO)
        // =====================================================================
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Dispositivo Actual y Sesiones Activas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = KashNavy
                )

                activeSessions.forEachIndexed { index, session ->
                    if (index > 0) {
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0F2FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = KashBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.deviceModelLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = KashNavy
                            )
                            Text(
                                text = "${session.osVersionLabel} • ${session.authProviderLabel}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "Última actividad: ${dateFormatter.format(Date(session.lastActiveEpochMs))}",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        if (session.isCurrentDevice) {
                            Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(50)) {
                                Text(
                                    text = "ACTIVA",
                                    color = Color(0xFF15803D),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            coordinator.revokeAllOtherSessions(userId)
                                .onSuccess { revoked ->
                                    isBannerError = false
                                    val msg = if (revoked > 0) {
                                        "Se revocaron $revoked sesiones remotas en Firestore y Room."
                                    } else {
                                        "Bóveda verificada: únicamente ${coordinator.currentHardwareModelName} tiene sesión activa."
                                    }
                                    bannerFeedback = msg
                                    onShowMessage(msg)
                                }
                                .onFailure { err ->
                                    isBannerError = true
                                    bannerFeedback = err.message ?: "Error al revocar sesiones secundarias."
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFEE2E2),
                        contentColor = Color(0xFFDC2626)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("security_btn_revoke_sessions")
                ) {
                    Text(
                        text = "Revocar sesiones secundarias en Room y Firebase",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }

        // =====================================================================
        // TARJETA 3: BITÁCORA EN VIVO DE AUDITORÍA DE SEGURIDAD (AuditSecurityLogDao)
        // =====================================================================
        if (recentAuditLogs.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = KashBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Registro de Auditoría Criptográfica (Room)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = KashNavy
                        )
                    }
                    recentAuditLogs.forEach { log ->
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = log.eventType,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KashBlue
                                )
                                Text(
                                    text = dateFormatter.format(Date(log.timestamp)),
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Text(
                                text = log.details,
                                fontSize = 11.sp,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(64.dp))
    }

    // =========================================================================
    // MODAL REAL 1: CREAR O CAMBIAR PIN (PBKDF2WithHmacSHA256)
    // =========================================================================
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = KashBlue)
                    Text(
                        text = if (securityConfig.hasPinSet) "Cambiar PIN de Seguridad" else "Crear PIN de Seguridad",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Tu PIN se almacena únicamente como hash derivado con PBKDF2-HMAC-SHA256 y sal aleatoria en Room:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    if (securityConfig.hasPinSet) {
                        OutlinedTextField(
                            value = currentPinInput,
                            onValueChange = {
                                if (it.length <= 8 && it.all(Char::isDigit)) {
                                    currentPinInput = it
                                    pinDialogError = null
                                }
                            },
                            label = { Text("PIN Actual") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { pinVisible = !pinVisible }) {
                                    Icon(
                                        imageVector = if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Mostrar u ocultar PIN"
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 16.sp, fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = KashBlue,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                cursorColor = KashBlue
                            )
                        )
                    }

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = {
                            if (it.length <= 8 && it.all(Char::isDigit)) {
                                newPinInput = it
                                pinDialogError = null
                            }
                        },
                        label = { Text("Nuevo PIN (4 a 8 dígitos)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = KashBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            cursorColor = KashBlue
                        )
                    )

                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = {
                            if (it.length <= 8 && it.all(Char::isDigit)) {
                                confirmPinInput = it
                                pinDialogError = null
                            }
                        },
                        label = { Text("Confirmar Nuevo PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = KashBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            cursorColor = KashBlue
                        )
                    )

                    pinDialogError?.let { err ->
                        Text(
                            text = err,
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            coordinator.createOrChangeSecurityPin(
                                hasPinAlreadySet = securityConfig.hasPinSet,
                                currentPin = currentPinInput,
                                newPin = newPinInput,
                                confirmPin = confirmPinInput
                            ).onSuccess { msg ->
                                isBannerError = false
                                bannerFeedback = msg
                                onShowMessage(msg)
                                showPinDialog = false
                            }.onFailure { err ->
                                pinDialogError = err.message ?: "Error al guardar el PIN"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Guardar PIN", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // =========================================================================
    // MODAL REAL 2: CONFIGURAR / ROTAR FRASE DE PASO MAESTRA SQLCIPHER (PassphraseManager)
    // =========================================================================
    if (showVaultPassphraseDialog) {
        val strength = remember(masterPassphraseInput) {
            coordinator.evaluateMasterPassphraseStrength(masterPassphraseInput)
        }
        AlertDialog(
            onDismissRequest = { showVaultPassphraseDialog = false },
            title = {
                Text(
                    text = "Frase Maestra SQLCipher AES-256",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Deriva una llave de 256 bits con PBKDF2-HMAC-SHA256 (310.000 iteraciones OWASP) envuelta por el hardware Android Keystore:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    OutlinedTextField(
                        value = masterPassphraseInput,
                        onValueChange = {
                            masterPassphraseInput = it
                            passphraseDialogError = null
                        },
                        label = { Text("Nueva frase de paso maestra (mín. 6 caracteres)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Medium),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = KashBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            cursorColor = KashBlue
                        )
                    )
                    if (masterPassphraseInput.isNotBlank()) {
                        LinearProgressIndicator(
                            progress = { (strength.score / 100f).coerceIn(0f, 1f) },
                            color = if (strength.score >= 60) Color(0xFF10B981) else Color(0xFFD97706),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Text(
                            text = "Fortaleza: ${strength.label} (${strength.score}/100)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (strength.score >= 60) Color(0xFF15803D) else Color(0xFFB45309)
                        )
                    }
                    OutlinedTextField(
                        value = masterPassphraseConfirm,
                        onValueChange = {
                            masterPassphraseConfirm = it
                            passphraseDialogError = null
                        },
                        label = { Text("Confirmar frase de paso maestra") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Medium),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = KashBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            cursorColor = KashBlue
                        )
                    )

                    if (encryptionInfo.hasUserPassphrase) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    coordinator.resetToHardwareKeystorePassphrase(userId)
                                        .onSuccess { updatedInfo ->
                                            encryptionInfo = updatedInfo
                                            isBannerError = false
                                            bannerFeedback = "Bóveda SQLCipher restaurada a la semilla maestra del hardware Android Keystore."
                                            showVaultPassphraseDialog = false
                                        }
                                }
                            }
                        ) {
                            Text(
                                text = "Restaurar llave automática de Hardware Keystore",
                                fontSize = 11.sp,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    passphraseDialogError?.let { err ->
                        Text(
                            text = err,
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (masterPassphraseInput != masterPassphraseConfirm) {
                            passphraseDialogError = "La frase maestra y su confirmación no coinciden."
                            return@Button
                        }
                        coroutineScope.launch {
                            coordinator.configureCustomMasterPassphrase(userId, masterPassphraseInput)
                                .onSuccess { updatedInfo ->
                                    encryptionInfo = updatedInfo
                                    isBannerError = false
                                    bannerFeedback = "Frase maestra derivada con 310.000 iteraciones y protegida en Android Keystore."
                                    showVaultPassphraseDialog = false
                                }
                                .onFailure { err ->
                                    passphraseDialogError = err.message
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Blindar Base de Datos", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVaultPassphraseDialog = false }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

/**
 * Backward compatibility alias for SettingsSecuritySubScreen
 */
@Composable
fun SettingsSecuritySubScreen(
    biometricsEnabled: Boolean,
    onToggleBiometrics: (Boolean) -> Unit,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsSecurityScreen(
        biometricsEnabled = biometricsEnabled,
        onToggleBiometrics = onToggleBiometrics,
        onShowMessage = onShowMessage,
        onBack = onBack,
        modifier = modifier
    )
}
