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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.DatabaseEncryptionInfo
import com.example.data.local.PassphraseStrength
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashCyan
import com.example.ui.theme.KashNavy
import com.example.ui.theme.KashTextSecondary

@Composable
fun SqlCipherSecurityDialog(
    encryptionInfo: DatabaseEncryptionInfo,
    onSavePassphrase: (newPassphrase: String) -> Unit,
    onResetToHardwarePassphrase: () -> Unit,
    onDismiss: () -> Unit,
    onEvaluateStrength: (String) -> PassphraseStrength = { passphrase ->
        val score = when {
            passphrase.length >= 12 -> 90
            passphrase.length >= 8 -> 70
            passphrase.length >= 6 -> 45
            else -> 20
        }
        val label = when {
            score >= 80 -> "Muy Segura (Cifrado Militar)"
            score >= 60 -> "Fuerte"
            score >= 40 -> "Moderada"
            else -> "Débil"
        }
        PassphraseStrength(score, label, passphrase.length >= 6, if (passphrase.length < 6) "Mínimo 6 caracteres" else null)
    }
) {
    var newPassphrase by remember { mutableStateOf("") }
    var confirmPassphrase by remember { mutableStateOf("") }
    var isNewPassphraseVisible by remember { mutableStateOf(false) }
    var isConfirmPassphraseVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val strength = remember(newPassphrase) {
        onEvaluateStrength(newPassphrase)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .testTag("sqlcipher_security_dialog"),
            color = Color.White,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF0284C7), Color(0xFF06B6D4))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Cifrado SQLCipher",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Seguridad de Datos (SQLCipher)",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = KashNavy
                            )
                            Text(
                                text = "Cifrado en reposo AES-256 para SQLite",
                                fontSize = 12.sp,
                                color = KashTextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_sqlcipher_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Status Badge Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color(0xFF86EFAC), Color(0xFF4ADE80))))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Estado Cifrado",
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Base de Datos Cifrada en Reposo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF14532D)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Motor: ${encryptionInfo.cipherEngine}\n" +
                                    "• Derivación: ${encryptionInfo.keyDerivation}\n" +
                                    "• Modo de Clave: ${if (encryptionInfo.hasUserPassphrase) "Contraseña Personalizada de Usuario" else "Clave Maestra Hardware Keystore"}",
                            fontSize = 12.sp,
                            color = Color(0xFF166534),
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Configurar Contraseña Personalizada",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KashNavy
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Asegura todas tus cuentas, saldos, transacciones, CDTs y nómina con tu propia frase de paso. La base de datos Room será re-cifrada con SQLCipher.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // New Passphrase Field
                OutlinedTextField(
                    value = newPassphrase,
                    onValueChange = {
                        newPassphrase = it
                        errorMessage = null
                        successMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_new_database_passphrase"),
                    label = { Text("Nueva Contraseña de Base de Datos") },
                    placeholder = { Text("Mínimo 6 caracteres...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Key Icon",
                            tint = KashBlue
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { isNewPassphraseVisible = !isNewPassphraseVisible }) {
                            Icon(
                                imageVector = if (isNewPassphraseVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isNewPassphraseVisible) "Ocultar" else "Mostrar",
                                tint = Color(0xFF64748B)
                            )
                        }
                    },
                    visualTransformation = if (isNewPassphraseVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KashBlue,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    singleLine = true
                )

                // Passphrase Strength Meter
                if (newPassphrase.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val progress = strength.score / 100f
                        val strengthColor = when {
                            strength.score >= 80 -> Color(0xFF10B981) // Green
                            strength.score >= 60 -> Color(0xFF0284C7) // Blue
                            strength.score >= 40 -> Color(0xFFF59E0B) // Yellow/Amber
                            else -> Color(0xFFEF4444) // Red
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = strengthColor,
                            trackColor = Color(0xFFE2E8F0)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Nivel de seguridad:",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = strength.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = strengthColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Confirm Passphrase Field
                OutlinedTextField(
                    value = confirmPassphrase,
                    onValueChange = {
                        confirmPassphrase = it
                        errorMessage = null
                        successMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_confirm_database_passphrase"),
                    label = { Text("Confirmar Contraseña") },
                    placeholder = { Text("Repite la contraseña...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Confirm Lock",
                            tint = KashBlue
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { isConfirmPassphraseVisible = !isConfirmPassphraseVisible }) {
                            Icon(
                                imageVector = if (isConfirmPassphraseVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isConfirmPassphraseVisible) "Ocultar" else "Mostrar",
                                tint = Color(0xFF64748B)
                            )
                        }
                    },
                    visualTransformation = if (isConfirmPassphraseVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KashBlue,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    singleLine = true
                )

                // Error / Success feedback
                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let { err ->
                        Text(
                            text = "⚠ $err",
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = successMessage != null) {
                    successMessage?.let { msg ->
                        Text(
                            text = "✓ $msg",
                            color = Color(0xFF16A34A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (newPassphrase.length < 6) {
                                errorMessage = "La contraseña debe contener al menos 6 caracteres."
                                return@Button
                            }
                            if (newPassphrase != confirmPassphrase) {
                                errorMessage = "Las contraseñas no coinciden."
                                return@Button
                            }
                            errorMessage = null
                            onSavePassphrase(newPassphrase)
                            successMessage = "Base de datos Room asegurada con tu contraseña personalizada."
                            newPassphrase = ""
                            confirmPassphrase = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_save_database_passphrase"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KashBlue
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Guardar y Cifrar con SQLCipher",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    if (encryptionInfo.hasUserPassphrase) {
                        OutlinedButton(
                            onClick = {
                                onResetToHardwarePassphrase()
                                successMessage = "Restablecido a clave maestra de hardware Keystore."
                                errorMessage = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_reset_hardware_passphrase"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockReset,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Restablecer a Clave de Hardware",
                                fontSize = 13.sp,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        }
    }
}
