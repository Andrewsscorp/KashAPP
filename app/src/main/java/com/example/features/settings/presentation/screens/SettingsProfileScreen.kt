package com.example.features.settings.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KashBlue

/**
 * User Profile Screen (Personal Data, Avatar, Security Credentials & 2FA).
 */
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
    var editName by remember { mutableStateOf(name) }
    var editEmail by remember { mutableStateOf(email) }
    var editPhone by remember { mutableStateOf(phone) }
    var editTimezone by remember { mutableStateOf("Bogotá (UTC-5) • Colombia") }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    // Dialog edit states
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditEmailDialog by remember { mutableStateOf(false) }
    var showEditPhoneDialog by remember { mutableStateOf(false) }
    var showEditTimezoneDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showTwoFactorDialog by remember { mutableStateOf(false) }
    var is2faActive by remember { mutableStateOf(true) }

    // Temporary input buffers for dialogs
    var tempInputName by remember { mutableStateOf(editName) }
    var tempInputEmail by remember { mutableStateOf(editEmail) }
    var tempInputPhone by remember { mutableStateOf(editPhone) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Hero Avatar Card
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
                                Color(0xFF1E40AF)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar con insignia de cámara
                    Box(modifier = Modifier.size(76.dp)) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFDBEAFE),
                            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.8f)),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .size(68.dp)
                                .clickable { feedbackMessage = "Foto de perfil actualizada" }
                        ) {
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
                                    text = editName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "AF" },
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .size(26.dp)
                                .align(Alignment.BottomEnd)
                                .clickable { feedbackMessage = "Abriendo selector de foto de perfil..." }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Cambiar foto",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = editName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val effectiveUid = userIdDisplay.ifBlank { "kash-user-default" }
                                val clip = ClipData.newPlainText("ID", effectiveUid)
                                clipboard.setPrimaryClip(clip)
                                feedbackMessage = "UID copiado: $effectiveUid"
                            }
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
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = "Copiar ID",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            color = if (isEmailVerified) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isEmailVerified) "Cuenta Verificada ($authProvider)" else "Verificación Pendiente",
                                    color = if (isEmailVerified) Color(0xFF15803D) else Color(0xFFB45309),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = if (isEmailVerified) Icons.Default.Check else Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (isEmailVerified) Color(0xFF15803D) else Color(0xFFB45309),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 1: Contact & Security Data
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
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ProfileRowItem(
                        icon = Icons.Default.PersonOutline,
                        iconTint = Color(0xFF3B82F6),
                        iconBg = Color(0xFFEFF6FF),
                        caption = "NOMBRE COMPLETO",
                        value = editName,
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
                        iconTint = Color(0xFF3B82F6),
                        iconBg = Color(0xFFEFF6FF),
                        caption = "CORREO ELECTRÓNICO",
                        value = editEmail,
                        badgeText = "OTP",
                        showVerifiedCheck = true,
                        onClick = {
                            tempInputEmail = editEmail
                            showEditEmailDialog = true
                        },
                        testTag = "profile_row_email"
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                    ProfileRowItem(
                        icon = Icons.Default.PhoneAndroid,
                        iconTint = Color(0xFF3B82F6),
                        iconBg = Color(0xFFEFF6FF),
                        caption = "NÚMERO MÓVIL ASOCIADO",
                        value = editPhone,
                        badgeText = null,
                        showVerifiedCheck = false,
                        onClick = {
                            tempInputPhone = editPhone
                            showEditPhoneDialog = true
                        },
                        testTag = "profile_row_phone"
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                    ProfileRowItem(
                        icon = Icons.Default.Home,
                        iconTint = Color(0xFF3B82F6),
                        iconBg = Color(0xFFEFF6FF),
                        caption = "ZONA HORARIA Y JURISDICCIÓN",
                        value = editTimezone,
                        badgeText = null,
                        showVerifiedCheck = false,
                        onClick = { showEditTimezoneDialog = true },
                        testTag = "profile_row_timezone"
                    )
                }
            }
        }

        // Section 2: Account Preferences
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
                shape = RoundedCornerShape(20.dp),
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
                            .padding(horizontal = 16.dp, vertical = 14.dp)
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
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Cambiar contraseña",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTwoFactorDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("profile_row_2fa"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Autenticación en dos pasos (2FA)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (is2faActive) "Activado" else "Desactivado",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (is2faActive) Color(0xFF16A34A) else Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Feedback Banner
        feedbackMessage?.let { msg ->
            Surface(
                color = Color(0xFFDCFCE7),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = msg, color = Color(0xFF166534), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { feedbackMessage = null }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF166534), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Dialogs
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text(text = "Editar nombre completo", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ingresa tu nombre y apellido tal como figuran en tu documento:", fontSize = 12.sp, color = Color(0xFF64748B))
                    OutlinedTextField(
                        value = tempInputName,
                        onValueChange = { tempInputName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
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
                        if (tempInputName.isNotBlank()) {
                            editName = tempInputName.trim()
                            onSaveProfile(editName, editEmail, editPhone)
                            feedbackMessage = "Nombre actualizado correctamente"
                            showEditNameDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                    shape = RoundedCornerShape(10.dp)
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
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showEditEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEditEmailDialog = false },
            title = { Text(text = "Editar correo electrónico", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enviaremos un código OTP de seguridad para validar el cambio:", fontSize = 12.sp, color = Color(0xFF64748B))
                    OutlinedTextField(
                        value = tempInputEmail,
                        onValueChange = { tempInputEmail = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
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
                        if (tempInputEmail.contains("@") && tempInputEmail.contains(".")) {
                            editEmail = tempInputEmail.trim()
                            onSaveProfile(editName, editEmail, editPhone)
                            feedbackMessage = "Correo electrónico actualizado correctamente"
                            showEditEmailDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                    shape = RoundedCornerShape(10.dp)
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
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showEditPhoneDialog) {
        AlertDialog(
            onDismissRequest = { showEditPhoneDialog = false },
            title = { Text(text = "Editar número móvil", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Número asociado a notificaciones y alertas:", fontSize = 12.sp, color = Color(0xFF64748B))
                    OutlinedTextField(
                        value = tempInputPhone,
                        onValueChange = { tempInputPhone = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
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
                        if (tempInputPhone.isNotBlank()) {
                            editPhone = tempInputPhone.trim()
                            onSaveProfile(editName, editEmail, editPhone)
                            feedbackMessage = "Número móvil actualizado"
                            showEditPhoneDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPhoneDialog = false }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showEditTimezoneDialog) {
        val timezones = listOf(
            "Bogotá (UTC-5) • Colombia",
            "Ciudad de México (UTC-6) • México",
            "Buenos Aires (UTC-3) • Argentina",
            "Madrid (UTC+1) • España",
            "Miami / New York (UTC-5) • EE.UU."
        )

        AlertDialog(
            onDismissRequest = { showEditTimezoneDialog = false },
            title = { Text(text = "Seleccionar Zona Horaria", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    timezones.forEach { tz ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (editTimezone == tz) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (editTimezone == tz) KashBlue else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editTimezone = tz
                                    feedbackMessage = "Zona horaria configurada en $tz"
                                    showEditTimezoneDialog = false
                                }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = tz,
                                    fontSize = 13.sp,
                                    fontWeight = if (editTimezone == tz) FontWeight.Bold else FontWeight.Normal,
                                    color = if (editTimezone == tz) KashBlue else Color(0xFF0F172A),
                                    modifier = Modifier.weight(1f)
                                )
                                if (editTimezone == tz) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = KashBlue, modifier = Modifier.size(16.dp))
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
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text(text = "Cambiar Contraseña", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Crea una contraseña segura de mínimo 8 caracteres con letras y números:", fontSize = 12.sp, color = Color(0xFF64748B))

                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Contraseña actual") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Nueva contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirmar nueva contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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

                    passwordError?.let { err ->
                        Text(text = err, color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword.length < 6) {
                            passwordError = "La contraseña debe tener al menos 6 caracteres"
                        } else if (newPassword != confirmPassword) {
                            passwordError = "Las contraseñas no coinciden"
                        } else {
                            feedbackMessage = "¡Contraseña actualizada con éxito!"
                            showChangePasswordDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Actualizar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showTwoFactorDialog) {
        AlertDialog(
            onDismissRequest = { showTwoFactorDialog = false },
            title = {
                Text(
                    text = "Autenticación en dos pasos (2FA)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Agrega una capa extra de seguridad para transferencias y retiros.", fontSize = 13.sp, color = Color(0xFF64748B))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Activar 2FA obligatorio", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                        Switch(
                            checked = is2faActive,
                            onCheckedChange = { is2faActive = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                        )
                    }

                    if (is2faActive) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Método activo: Google Authenticator / SMS OTP", fontSize = 11.sp, color = Color(0xFF166534), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        feedbackMessage = if (is2faActive) "2FA activado correctamente" else "2FA desactivado"
                        showTwoFactorDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Listo", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTwoFactorDialog = false }) {
                    Text("Cerrar", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
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
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFDBEAFE)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1D4ED8),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
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
                    color = Color(0xFF0F172A)
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
