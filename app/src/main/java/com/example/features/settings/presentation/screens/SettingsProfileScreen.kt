package com.example.features.settings.presentation.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.KashAppApplication
import com.example.core.ui.components.IosStyleSwitch
import com.example.features.settings.data.ProfileJurisdictionOption
import com.example.features.settings.data.UserProfileSecurityManager
import com.example.features.settings.security.TotpTwoFactorAuthenticator
import com.example.ui.theme.KashBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Pantalla Empresarial Real de "Mi Perfil" (Datos Personales, Foto de Perfil,
 * Jurisdicción Fiscal DIAN/Internacional, Credenciales Firebase Auth y 2FA TOTP RFC 6238).
 * Diseño iOS Cupertino 100% responsivo con aislamiento multi-usuario por UID.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsProfileScreen(
    name: String,
    email: String,
    phone: String,
    userIdDisplay: String = "",
    authProvider: String = "Firebase Auth Seguro",
    isEmailVerified: Boolean = true,
    memberSince: String = "2026",
    onSaveProfile: (String, String, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val profileManager = remember(context) {
        val appContainer = (context.applicationContext as? KashAppApplication)?.appContainer
        UserProfileSecurityManager(
            context = context,
            userSessionDao = appContainer?.securityDatabase?.userSessionDao(),
            auditSecurityLogDao = appContainer?.securityDatabase?.auditSecurityLogDao(),
            settingsRepository = appContainer?.settingsRepository
        )
    }

    val effectiveUserId = remember(userIdDisplay) {
        userIdDisplay.ifBlank { "user_default" }
    }

    // Estados reales hidratados desde FirebaseAuth + Room + Keystore + TaxConfigurationStore
    var editName by remember(name) { mutableStateOf(name) }
    var editEmail by remember(email) { mutableStateOf(email) }
    var editPhone by remember(phone) { mutableStateOf(phone) }
    var isPhoneVerified by remember { mutableStateOf(false) }
    var emailVerifiedState by remember(isEmailVerified) { mutableStateOf(isEmailVerified) }
    var avatarLocalPath by remember { mutableStateOf<String?>(null) }
    var avatarBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var editTimezone by remember { mutableStateOf("Bogotá (UTC-5) • Colombia (DIAN)") }
    var activeJurisdictionCode by remember { mutableStateOf("CO_DIAN") }
    var activeBaseCurrency by remember { mutableStateOf("COP") }
    var is2faActive by remember { mutableStateOf(false) }
    var hasPasswordProvider by remember { mutableStateOf(true) }
    var hasGoogleProvider by remember { mutableStateOf(false) }

    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isErrorFeedback by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    // Hidratación inicial y cuando cambia el usuario activo (aislamiento multi-usuario)
    LaunchedEffect(effectiveUserId, name, email, phone) {
        val state = profileManager.loadProfileState(
            userId = effectiveUserId,
            fallbackName = name,
            fallbackEmail = email,
            fallbackPhone = phone
        )
        editName = state.displayName
        editEmail = state.email
        editPhone = state.phone
        isPhoneVerified = state.isPhoneVerified
        emailVerifiedState = state.isEmailVerified
        avatarLocalPath = state.avatarLocalPath
        editTimezone = state.timezoneLabel
        activeJurisdictionCode = state.jurisdictionCode
        activeBaseCurrency = state.baseCurrency
        is2faActive = state.isTwoFactorEnabled
        hasPasswordProvider = state.hasPasswordProvider
        hasGoogleProvider = state.hasGoogleProvider
    }

    // Decodificar Bitmap del avatar local cuando cambia la ruta
    LaunchedEffect(avatarLocalPath) {
        val path = avatarLocalPath
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

    // Selector real de Galería de Android (Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { selectedUri: Uri? ->
        if (selectedUri != null) {
            isProcessing = true
            scope.launch {
                val res = profileManager.updateProfileAvatar(effectiveUserId, selectedUri)
                isProcessing = false
                res.onSuccess { savedPath ->
                    avatarLocalPath = savedPath
                    isErrorFeedback = false
                    feedbackMessage = "Foto de perfil actualizada y sincronizada"
                }.onFailure { err ->
                    isErrorFeedback = true
                    feedbackMessage = err.message ?: "No se pudo procesar la foto de perfil."
                }
            }
        }
    }

    // Estados de apertura de modales
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditEmailDialog by remember { mutableStateOf(false) }
    var showEditPhoneDialog by remember { mutableStateOf(false) }
    var showEditTimezoneDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showTwoFactorDialog by remember { mutableStateOf(false) }

    // Buffers para edición de Nombre y Correo
    var tempInputName by remember { mutableStateOf("") }
    var tempInputEmail by remember { mutableStateOf("") }
    var emailCurrentPassword by remember { mutableStateOf("") }
    var emailDialogError by remember { mutableStateOf<String?>(null) }

    // Buffers para verificación SMS OTP de Número Móvil
    var tempInputPhone by remember { mutableStateOf("") }
    var phoneVerificationStep by remember { mutableStateOf(1) } // 1 = Ingresar teléfono, 2 = Ingresar OTP 6 dígitos
    var pendingVerificationId by remember { mutableStateOf("") }
    var pendingNormalizedPhone by remember { mutableStateOf("") }
    var phoneOtpInput by remember { mutableStateOf("") }
    var fallbackOtpHint by remember { mutableStateOf<String?>(null) }
    var phoneDialogError by remember { mutableStateOf<String?>(null) }

    // Buffers para Zona Horaria y Jurisdicción Fiscal
    var syncCurrencyOnJurisdictionChange by remember { mutableStateOf(true) }

    // Buffers para Cambio / Vinculación de Contraseña
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // Buffers para 2FA TOTP RFC 6238
    var totpSecretBase32 by remember { mutableStateOf("") }
    var totpOtpAuthUri by remember { mutableStateOf("") }
    var totpRecoveryCodes by remember { mutableStateOf<List<String>>(emptyList()) }
    var totpVerificationInput by remember { mutableStateOf("") }
    var totpDisableInput by remember { mutableStateOf("") }
    var totpDialogError by remember { mutableStateOf<String?>(null) }
    var totpRemainingSeconds by remember { mutableStateOf(30) }

    // Reloj en vivo para la ventana de 30s cuando el modal 2FA está abierto
    LaunchedEffect(showTwoFactorDialog) {
        while (showTwoFactorDialog) {
            totpRemainingSeconds = TotpTwoFactorAuthenticator.getRemainingSecondsInWindow()
            delay(1000L)
        }
    }

    // Formateador limpio del proveedor de autenticación (evita el texto roto "firebase_google")
    val cleanAuthProviderBadge = remember(authProvider, hasGoogleProvider) {
        when {
            authProvider.contains("google", ignoreCase = true) || hasGoogleProvider -> "Google"
            authProvider.contains("password", ignoreCase = true) -> "Email"
            authProvider.contains("firebase", ignoreCase = true) -> "Firebase"
            else -> "Verificada"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // =========================================================================
        // 1. HERO AVATAR CARD (Estilo iPhone Cupertino + Photo Picker Real)
        // =========================================================================
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF2563EB),
                                Color(0xFF1D4ED8),
                                Color(0xFF1E3A8A)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar con imagen real o iniciales + botón de cámara
                    Box(modifier = Modifier.size(78.dp)) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color(0xFFDBEAFE),
                            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.85f)),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .size(70.dp)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("profile_avatar_button")
                        ) {
                            val bmp = avatarBitmap
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Foto de perfil de $editName",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = editName.split(" ")
                                            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
                                            .take(2)
                                            .joinToString("")
                                            .ifEmpty { "KA" },
                                        fontSize = 25.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.BottomEnd)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Cambiar foto de perfil",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = editName.ifBlank { "Titular KashApp" },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val effectiveUid = userIdDisplay.ifBlank { "kash-user-default" }
                                    val clip = ClipData.newPlainText("UID KashApp", effectiveUid)
                                    clipboard.setPrimaryClip(clip)
                                    isErrorFeedback = false
                                    feedbackMessage = "UID copiado al portapapeles: $effectiveUid"
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            val displayUidText = if (userIdDisplay.length > 16) {
                                "UID: ${userIdDisplay.take(8)}...${userIdDisplay.takeLast(4)}"
                            } else if (userIdDisplay.isNotBlank()) {
                                "UID: $userIdDisplay"
                            } else {
                                "UID: Cuenta Local KashApp"
                            }
                            Text(
                                text = displayUidText,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.88f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = "Copiar UID",
                                tint = Color.White.copy(alpha = 0.88f),
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Píldoras estilo iOS Cupertino en 1 sola línea (sin partirse en 2 renglones)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = if (emailVerifiedState) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (emailVerifiedState) Icons.Default.CheckCircle else Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (emailVerifiedState) Color(0xFF15803D) else Color(0xFFB45309),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (emailVerifiedState) "Verificada • $cleanAuthProviderBadge" else "Verificación Pendiente",
                                        color = if (emailVerifiedState) Color(0xFF15803D) else Color(0xFFB45309),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "$activeBaseCurrency • $memberSince",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 2. SECCIÓN: DATOS DE CONTACTO Y JURISDICCIÓN FISCAL
        // =========================================================================
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "DATOS DE CONTACTO Y SEGURIDAD",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ProfileRowItem(
                        icon = Icons.Default.PersonOutline,
                        iconTint = Color(0xFF2563EB),
                        iconBg = Color(0xFFEFF6FF),
                        caption = "NOMBRE COMPLETO",
                        value = editName.ifBlank { "Sin nombre configurado" },
                        badgeText = null,
                        showVerifiedCheck = false,
                        onClick = {
                            tempInputName = editName
                            showEditNameDialog = true
                        },
                        testTag = "profile_row_nombre"
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                    ProfileRowItem(
                        icon = Icons.Default.Email,
                        iconTint = Color(0xFF2563EB),
                        iconBg = Color(0xFFEFF6FF),
                        caption = "CORREO ELECTRÓNICO",
                        value = editEmail.ifBlank { "Sin correo asociado" },
                        badgeText = "OTP",
                        showVerifiedCheck = emailVerifiedState,
                        onClick = {
                            tempInputEmail = editEmail
                            emailCurrentPassword = ""
                            emailDialogError = null
                            showEditEmailDialog = true
                        },
                        testTag = "profile_row_email"
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                    ProfileRowItem(
                        icon = Icons.Default.PhoneAndroid,
                        iconTint = Color(0xFF2563EB),
                        iconBg = Color(0xFFEFF6FF),
                        caption = "NÚMERO MÓVIL ASOCIADO",
                        value = editPhone.ifBlank { "Vincular número móvil (+57)" },
                        badgeText = if (isPhoneVerified && editPhone.isNotBlank()) "SMS OTP" else "Pendiente",
                        showVerifiedCheck = isPhoneVerified && editPhone.isNotBlank(),
                        onClick = {
                            tempInputPhone = editPhone.ifBlank { "+57" }
                            phoneVerificationStep = 1
                            phoneOtpInput = ""
                            fallbackOtpHint = null
                            phoneDialogError = null
                            showEditPhoneDialog = true
                        },
                        testTag = "profile_row_phone"
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                    ProfileRowItem(
                        icon = Icons.Default.Home,
                        iconTint = Color(0xFF2563EB),
                        iconBg = Color(0xFFEFF6FF),
                        caption = "ZONA HORARIA Y JURISDICCIÓN",
                        value = editTimezone,
                        badgeText = activeBaseCurrency,
                        showVerifiedCheck = true,
                        onClick = { showEditTimezoneDialog = true },
                        testTag = "profile_row_timezone"
                    )
                }
            }
        }

        // =========================================================================
        // 3. SECCIÓN: PREFERENCIAS DE CUENTA Y 2FA TOTP
        // =========================================================================
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "PREFERENCIAS DE CUENTA",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                                passwordError = null
                                showChangePasswordDialog = true
                            }
                            .padding(horizontal = 16.dp, vertical = 15.dp)
                            .testTag("profile_row_change_password"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF334155),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (hasPasswordProvider) "Cambiar contraseña" else "Crear contraseña de respaldo",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = if (hasPasswordProvider) {
                                    "Protegida con Firebase Auth + Hash local"
                                } else {
                                    "Vincula una clave directa a tu cuenta Google"
                                },
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val (secret, uri, codes) = profileManager.prepareTwoFactorEnrollment(
                                    userId = effectiveUserId,
                                    userEmail = editEmail
                                )
                                totpSecretBase32 = secret
                                totpOtpAuthUri = uri
                                totpRecoveryCodes = codes
                                totpVerificationInput = ""
                                totpDisableInput = ""
                                totpDialogError = null
                                showTwoFactorDialog = true
                            }
                            .padding(horizontal = 16.dp, vertical = 15.dp)
                            .testTag("profile_row_2fa"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (is2faActive) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (is2faActive) Color(0xFF16A34A) else Color(0xFF475569),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Autenticación en dos pasos (2FA)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = if (is2faActive) "TOTP RFC 6238 • Keystore AES-256" else "Google Authenticator / Authy",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (is2faActive) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = if (is2faActive) "Activado" else "Desactivado",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (is2faActive) Color(0xFF15803D) else Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Indicador de procesamiento en curso
        if (isProcessing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = KashBlue
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sincronizando con Firebase Auth y bóveda segura...",
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Banner de Feedback (Éxito o Error)
        feedbackMessage?.let { msg ->
            val bg = if (isErrorFeedback) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
            val fg = if (isErrorFeedback) Color(0xFF991B1B) else Color(0xFF166534)
            val icon = if (isErrorFeedback) Icons.Default.WarningAmber else Icons.Default.CheckCircle

            Surface(
                color = bg,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = msg,
                        color = fg,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { feedbackMessage = null },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = fg,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Espaciado inferior generoso para evitar solapamiento con la barra inferior o botón flotante
        Spacer(modifier = Modifier.height(64.dp))
    }

    // =========================================================================
    // MODAL 1: EDITAR NOMBRE COMPLETO REAL
    // =========================================================================
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(
                    text = "Editar nombre completo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tu nombre se actualizará en Firebase Auth, Cloud Firestore, Room y en los certificados PDF tributarios:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    OutlinedTextField(
                        value = tempInputName,
                        onValueChange = { tempInputName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        textStyle = TextStyle(
                            color = Color(0xFF0F172A),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
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
            },
            confirmButton = {
                Button(
                    onClick = {
                        val candidate = tempInputName.trim()
                        if (candidate.length >= 3) {
                            showEditNameDialog = false
                            isProcessing = true
                            scope.launch {
                                val result = profileManager.updateDisplayName(effectiveUserId, candidate)
                                isProcessing = false
                                result.onSuccess { updated ->
                                    editName = updated
                                    onSaveProfile(editName, editEmail, editPhone)
                                    isErrorFeedback = false
                                    feedbackMessage = "Nombre titular sincronizado en Firebase y bóveda local"
                                }.onFailure { err ->
                                    isErrorFeedback = true
                                    feedbackMessage = err.message ?: "Error al actualizar el nombre."
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // =========================================================================
    // MODAL 2: EDITAR CORREO ELECTRÓNICO CON VERIFICACIÓN FIREBASE
    // =========================================================================
    if (showEditEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEditEmailDialog = false },
            title = {
                Text(
                    text = "Actualizar correo electrónico",
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
                        text = "Enviaremos un enlace/código oficial de verificación de Firebase Auth al nuevo correo antes de efectuar el cambio:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    OutlinedTextField(
                        value = tempInputEmail,
                        onValueChange = {
                            tempInputEmail = it
                            emailDialogError = null
                        },
                        label = { Text("Nuevo correo electrónico") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        textStyle = TextStyle(
                            color = Color(0xFF0F172A),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
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

                    if (hasPasswordProvider) {
                        OutlinedTextField(
                            value = emailCurrentPassword,
                            onValueChange = {
                                emailCurrentPassword = it
                                emailDialogError = null
                            },
                            label = { Text("Contraseña actual (para reautenticar)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            textStyle = TextStyle(
                                color = Color(0xFF0F172A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
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

                    emailDialogError?.let { err ->
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
                        scope.launch {
                            isProcessing = true
                            val result = profileManager.requestEmailUpdateWithVerification(
                                userId = effectiveUserId,
                                newEmail = tempInputEmail,
                                currentPasswordForReauth = emailCurrentPassword.takeIf { it.isNotBlank() }
                            )
                            isProcessing = false
                            result.onSuccess { msg ->
                                editEmail = tempInputEmail.trim().lowercase()
                                onSaveProfile(editName, editEmail, editPhone)
                                isErrorFeedback = false
                                feedbackMessage = msg
                                showEditEmailDialog = false
                            }.onFailure { err ->
                                emailDialogError = err.message ?: "No se pudo verificar el cambio de correo."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Guardar y verificar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditEmailDialog = false }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // =========================================================================
    // MODAL 3: NÚMERO MÓVIL CON VERIFICACIÓN SMS OTP DE 2 PASOS
    // =========================================================================
    if (showEditPhoneDialog) {
        AlertDialog(
            onDismissRequest = { showEditPhoneDialog = false },
            title = {
                Text(
                    text = if (phoneVerificationStep == 1) "Vincular número móvil" else "Confirmar código SMS OTP",
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
                    if (phoneVerificationStep == 1) {
                        Text(
                            text = "Ingresa tu número celular en formato internacional (ej. +573001234567). Enviaremos un código OTP de 6 dígitos:",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                        OutlinedTextField(
                            value = tempInputPhone,
                            onValueChange = {
                                tempInputPhone = it
                                phoneDialogError = null
                            },
                            label = { Text("Número móvil E.164") },
                            placeholder = { Text("+573001234567") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            textStyle = TextStyle(
                                color = Color(0xFF0F172A),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
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
                    } else {
                        Text(
                            text = "Ingresa el código de verificación de 6 dígitos enviado a $pendingNormalizedPhone:",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )

                        fallbackOtpHint?.let { localOtp ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Código OTP de seguridad generado para este dispositivo:",
                                        fontSize = 11.sp,
                                        color = Color(0xFF1E40AF)
                                    )
                                    Text(
                                        text = localOtp,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF1D4ED8),
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = phoneOtpInput,
                            onValueChange = {
                                if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                                    phoneOtpInput = it
                                    phoneDialogError = null
                                }
                            },
                            label = { Text("Código OTP (6 dígitos)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            textStyle = TextStyle(
                                color = Color(0xFF0F172A),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 4.sp
                            ),
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

                    phoneDialogError?.let { err ->
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
                if (phoneVerificationStep == 1) {
                    Button(
                        onClick = {
                            val activity = context as? Activity
                            profileManager.startPhoneSmsVerification(
                                activity = activity,
                                rawPhone = tempInputPhone,
                                onCodeSent = { verificationId, normalizedPhone, fallbackOtp ->
                                    pendingVerificationId = verificationId
                                    pendingNormalizedPhone = normalizedPhone
                                    fallbackOtpHint = fallbackOtp
                                    phoneVerificationStep = 2
                                },
                                onInstantVerified = { normalizedPhone ->
                                    scope.launch {
                                        editPhone = normalizedPhone
                                        isPhoneVerified = true
                                        onSaveProfile(editName, editEmail, editPhone)
                                        isErrorFeedback = false
                                        feedbackMessage = "Número móvil $normalizedPhone verificado automáticamente"
                                        showEditPhoneDialog = false
                                    }
                                },
                                onError = { err ->
                                    phoneDialogError = err
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Enviar SMS OTP", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                isProcessing = true
                                val res = profileManager.confirmPhoneOtpAndSave(
                                    userId = effectiveUserId,
                                    normalizedPhone = pendingNormalizedPhone,
                                    verificationId = pendingVerificationId,
                                    smsCode = phoneOtpInput
                                )
                                isProcessing = false
                                res.onSuccess { verifiedPhone ->
                                    editPhone = verifiedPhone
                                    isPhoneVerified = true
                                    onSaveProfile(editName, editEmail, editPhone)
                                    isErrorFeedback = false
                                    feedbackMessage = "Número móvil $verifiedPhone verificado con éxito"
                                    showEditPhoneDialog = false
                                }.onFailure { err ->
                                    phoneDialogError = err.message ?: "Código OTP inválido."
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirmar código", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (phoneVerificationStep == 2) {
                            phoneVerificationStep = 1
                        } else {
                            showEditPhoneDialog = false
                        }
                    }
                ) {
                    Text(
                        text = if (phoneVerificationStep == 2) "Atrás" else "Cancelar",
                        color = Color(0xFF64748B)
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // =========================================================================
    // MODAL 4: ZONA HORARIA Y JURISDICCIÓN FISCAL (SINCRONIZADA CON RADAR DIAN)
    // =========================================================================
    if (showEditTimezoneDialog) {
        AlertDialog(
            onDismissRequest = { showEditTimezoneDialog = false },
            title = {
                Text(
                    text = "Zona Horaria y Jurisdicción Fiscal",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Configura tu residencia fiscal para sincronizar automáticamente el Radar Tributario (UVT DIAN / SAT / ARCA / IRS) y tus cierres contables:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sincronizar moneda base automáticamente",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(1f)
                        )
                        IosStyleSwitch(
                            checked = syncCurrencyOnJurisdictionChange,
                            onCheckedChange = { syncCurrencyOnJurisdictionChange = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = KashBlue
                            )
                        )
                    }

                    UserProfileSecurityManager.SUPPORTED_JURISDICTIONS.forEach { option: ProfileJurisdictionOption ->
                        val isSelected = editTimezone == option.displayLabel ||
                            activeJurisdictionCode == option.jurisdiction.code

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) KashBlue else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showEditTimezoneDialog = false
                                    scope.launch {
                                        val res = profileManager.updateTimezoneAndJurisdiction(
                                            userId = effectiveUserId,
                                            option = option,
                                            syncBaseCurrency = syncCurrencyOnJurisdictionChange
                                        )
                                        res.onSuccess { updated ->
                                            editTimezone = updated.displayLabel
                                            activeJurisdictionCode = updated.jurisdiction.code
                                            if (syncCurrencyOnJurisdictionChange) {
                                                activeBaseCurrency = updated.defaultCurrency
                                            }
                                            isErrorFeedback = false
                                            feedbackMessage = "Jurisdicción sincronizada: ${updated.displayLabel} (${updated.jurisdiction.authorityAcronym})"
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option.flagEmoji,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.displayLabel,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) KashBlue else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Moneda: ${option.defaultCurrency} • Unidad: ${option.jurisdiction.taxUnitName} (${option.jurisdiction.authorityAcronym})",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = KashBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showEditTimezoneDialog = false }) {
                    Text("Cerrar", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // =========================================================================
    // MODAL 5: CAMBIAR O VINCULAR CONTRASEÑA REAL (FIREBASE AUTH + KEYSTORE)
    // =========================================================================
    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = {
                Text(
                    text = if (hasPasswordProvider) "Cambiar Contraseña" else "Vincular Contraseña a tu Cuenta",
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
                        text = if (hasPasswordProvider) {
                            "Reautenticaremos tu sesión en Firebase Auth. Tu nueva clave requiere mínimo 8 caracteres, mayúscula, minúscula y número:"
                        } else {
                            "Iniciaste sesión con Google. Crea una contraseña segura para poder ingresar también con tu correo y clave directa:"
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    if (hasPasswordProvider) {
                        OutlinedTextField(
                            value = currentPassword,
                            onValueChange = {
                                currentPassword = it
                                passwordError = null
                            },
                            label = { Text("Contraseña actual") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            textStyle = TextStyle(
                                color = Color(0xFF0F172A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
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
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            passwordError = null
                        },
                        label = { Text("Nueva contraseña (mín. 8 caracteres)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        textStyle = TextStyle(
                            color = Color(0xFF0F172A),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
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
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            passwordError = null
                        },
                        label = { Text("Confirmar nueva contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        textStyle = TextStyle(
                            color = Color(0xFF0F172A),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
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

                    passwordError?.let { err ->
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
                        scope.launch {
                            isProcessing = true
                            val result = profileManager.changeOrLinkAccountPassword(
                                userId = effectiveUserId,
                                userEmail = editEmail,
                                currentPassword = currentPassword,
                                newPassword = newPassword,
                                confirmPassword = confirmPassword
                            )
                            isProcessing = false
                            result.onSuccess { msg ->
                                hasPasswordProvider = true
                                isErrorFeedback = false
                                feedbackMessage = msg
                                showChangePasswordDialog = false
                            }.onFailure { err ->
                                passwordError = err.message ?: "No se pudo actualizar la contraseña."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (hasPasswordProvider) "Actualizar" else "Vincular clave",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // =========================================================================
    // MODAL 6: AUTENTICACIÓN EN DOS PASOS (2FA TOTP RFC 6238 REAL)
    // =========================================================================
    if (showTwoFactorDialog) {
        AlertDialog(
            onDismissRequest = { showTwoFactorDialog = false },
            title = {
                Text(
                    text = "Autenticación 2FA (TOTP RFC 6238)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!is2faActive) {
                        Text(
                            text = "Vincula tu cuenta con Google Authenticator, Authy o Microsoft Authenticator para proteger retiros, exportaciones y cambios sensibles:",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )

                        // Tarjeta con la clave secreta Base32 + botón de copiar
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "CLAVE SECRETA BASE32 (160 BITS)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = totpSecretBase32.chunked(4).joinToString(" "),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(
                                                ClipData.newPlainText("Secreto 2FA KashApp", totpSecretBase32)
                                            )
                                            totpDialogError = null
                                            isErrorFeedback = false
                                            feedbackMessage = "Clave secreta Base32 copiada al portapapeles"
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copiar secreto Base32",
                                            tint = KashBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Botón para abrir directamente Google Authenticator / Authy mediante URI otpauth://
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(totpOtpAuthUri))
                                    context.startActivity(intent)
                                } catch (_: Throwable) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(
                                        ClipData.newPlainText("Secreto 2FA KashApp", totpSecretBase32)
                                    )
                                    totpDialogError = "No se detectó app autenticadora instalada. Se copió la clave secreta al portapapeles."
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, KashBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = KashBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Abrir en Google Authenticator / Authy",
                                color = KashBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            text = "Paso 2: Ingresa el código de 6 dígitos generado por tu app autenticadora (expira en ${totpRemainingSeconds}s):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )

                        OutlinedTextField(
                            value = totpVerificationInput,
                            onValueChange = {
                                if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                                    totpVerificationInput = it
                                    totpDialogError = null
                                }
                            },
                            label = { Text("Código TOTP de 6 dígitos") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            textStyle = TextStyle(
                                color = Color(0xFF0F172A),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 4.sp
                            ),
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
                    } else {
                        // Estado 2FA ya activo: muestra estado, códigos de emergencia y opción de desactivar con verificación
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "2FA TOTP Activo • Semilla cifrada con AES-256-GCM en Android Keystore.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF166534),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (totpRecoveryCodes.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Key,
                                                contentDescription = null,
                                                tint = Color(0xFF475569),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "CÓDIGOS DE EMERGENCIA",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF475569)
                                            )
                                        }
                                        TextButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(
                                                    ClipData.newPlainText(
                                                        "Códigos Recuperación KashApp",
                                                        totpRecoveryCodes.joinToString("\n")
                                                    )
                                                )
                                                isErrorFeedback = false
                                                feedbackMessage = "Códigos de emergencia copiados"
                                            }
                                        ) {
                                            Text("Copiar todos", fontSize = 11.sp, color = KashBlue, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(
                                        text = totpRecoveryCodes.chunked(2).joinToString("\n") { it.joinToString("   •   ") },
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        Text(
                            text = "Para desactivar el 2FA, ingresa tu código actual de 6 dígitos o uno de tus códigos de emergencia:",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )

                        OutlinedTextField(
                            value = totpDisableInput,
                            onValueChange = {
                                totpDisableInput = it
                                totpDialogError = null
                            },
                            label = { Text("Código 2FA o código XXXX-XXXX") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            textStyle = TextStyle(
                                color = Color(0xFF0F172A),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFFDC2626),
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                cursorColor = Color(0xFFDC2626)
                            )
                        )
                    }

                    totpDialogError?.let { err ->
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
                if (!is2faActive) {
                    Button(
                        onClick = {
                            scope.launch {
                                val res = profileManager.enableTwoFactorTotp(
                                    userId = effectiveUserId,
                                    secretBase32 = totpSecretBase32,
                                    verificationCode6Digits = totpVerificationInput,
                                    recoveryCodes = totpRecoveryCodes
                                )
                                res.onSuccess { savedCodes ->
                                    is2faActive = true
                                    totpRecoveryCodes = savedCodes
                                    isErrorFeedback = false
                                    feedbackMessage = "Autenticación 2FA TOTP activada y cifrada en Keystore"
                                    showTwoFactorDialog = false
                                }.onFailure { err ->
                                    totpDialogError = err.message ?: "Código de 6 dígitos inválido."
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Verificar y Activar 2FA", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                val res = profileManager.disableTwoFactorTotp(
                                    userId = effectiveUserId,
                                    verificationOrRecoveryCode = totpDisableInput
                                )
                                res.onSuccess {
                                    is2faActive = false
                                    isErrorFeedback = false
                                    feedbackMessage = "Autenticación 2FA desactivada"
                                    showTwoFactorDialog = false
                                }.onFailure { err ->
                                    totpDialogError = err.message ?: "Código inválido."
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Desactivar 2FA", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showTwoFactorDialog = false }) {
                    Text("Cerrar", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

/**
 * Reusable Row Component in Profile details
 */
@Composable
fun ProfileRowItem(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    caption: String,
    value: String,
    badgeText: String?,
    showVerifiedCheck: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = caption,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = caption,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )

                if (badgeText != null) {
                    val isPendingBadge = badgeText.equals("Pendiente", ignoreCase = true)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isPendingBadge) Color(0xFFFEF3C7) else Color(0xFFDBEAFE)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPendingBadge) Color(0xFFB45309) else Color(0xFF1D4ED8),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (showVerifiedCheck) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verificado",
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Backward compatibility alias for SettingsProfileSubScreen
 */
@Composable
fun SettingsProfileSubScreen(
    name: String,
    email: String,
    phone: String,
    onSaveProfile: (String, String, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsProfileScreen(
        name = name,
        email = email,
        phone = phone,
        onSaveProfile = onSaveProfile,
        onBack = onBack,
        modifier = modifier
    )
}
