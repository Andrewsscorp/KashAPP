package com.example.features.settings.presentation.screens

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
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy

/**
 * Security & Protection Screen (PIN, Biometrics, SQLCipher AES-256 HSM, and Session Management).
 */
@Composable
fun SettingsSecurityScreen(
    biometricsEnabled: Boolean,
    onToggleBiometrics: (Boolean) -> Unit,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showChangePinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var isPinSet by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Hardware y Biometría", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KashNavy)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bloqueo Biométrico", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = KashNavy)
                        Text("Solicitar huella o reconocimiento facial al abrir la app", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = biometricsEnabled,
                        onCheckedChange = {
                            onToggleBiometrics(it)
                            onShowMessage(if (it) "Bloqueo biométrico activado" else "Bloqueo biométrico desactivado")
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                Column {
                    Text("Cifrado Local de Base de Datos", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = KashNavy)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("SQLCipher AES-256 bits activo en reposo con Android Keystore Hardware Security Module (HSM).", fontSize = 12.sp, color = Color(0xFF059669))
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                Text("Claves de Acceso y PIN", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = KashNavy)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            newPinInput = ""
                            confirmPinInput = ""
                            pinError = null
                            showChangePinDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cambiar PIN (4 dígitos)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active sessions card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Dispositivos y Sesiones Activas", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KashNavy)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = null, tint = KashBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Google Pixel 8 Pro (Este dispositivo)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = KashNavy)
                        Text("Bogotá, Colombia • Sesión actual activa", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(6.dp)) {
                        Text("ACTIVA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }

                HorizontalDivider(color = Color(0xFFF8FAFC))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Devices, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("MacBook Pro 16\" • KashApp Web", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = KashNavy)
                        Text("Medellín, Colombia • Hace 2 horas", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = { onShowMessage("Todas las demás sesiones fueron cerradas con éxito") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar todas las demás sesiones", color = Color(0xFFDC2626), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Actualizar PIN de Seguridad", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ingresa tu nuevo PIN numérico de 4 dígitos:", fontSize = 13.sp, color = Color(0xFF64748B))

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPinInput = it },
                        label = { Text("Nuevo PIN (4 dígitos)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPinInput = it },
                        label = { Text("Confirmar PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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

                    if (pinError != null) {
                        Text(text = pinError!!, color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinInput.length != 4) {
                            pinError = "El PIN debe tener exactamente 4 dígitos."
                        } else if (newPinInput != confirmPinInput) {
                            pinError = "Los PINs no coinciden."
                        } else {
                            showChangePinDialog = false
                            onShowMessage("¡PIN actualizado con éxito!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KashBlue)
                ) {
                    Text("Guardar PIN", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
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
