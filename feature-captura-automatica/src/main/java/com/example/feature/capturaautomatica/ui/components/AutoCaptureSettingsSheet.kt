package com.example.feature.capturaautomatica.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.capturaautomatica.domain.model.SupportedColombianBank

/**
 * Settings and diagnostics sheet for Colombian bank auto-capture with Ley 1581 consent
 * and one-click notification simulation test triggers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCaptureSettingsSheet(
    onSimulate: (bankName: String) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF121417),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Ajustes de Captura Automática",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Privacidad y Permisos del Sistema",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8)
                            )
                        )
                    }
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ley 1581 Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F29))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lectura 100% en el Dispositivo",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Protección Ley 1581 de Habeas Data. KashApp procesa las alertas localmente. Los códigos de seguridad y OTPs son destruidos de inmediato en memoria.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFCBD5E1),
                                    lineHeight = 16.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF10B981),
                                checkedTrackColor = Color(0xFF065F46)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Permission settings button
            Button(
                onClick = {
                    try {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gestionar Permiso en Ajustes de Android", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Spacer(modifier = Modifier.height(20.dp))

            // Supported banks list (22 entidades colombianas)
            Text(
                text = "Entidades Financieras y Billeteras (${SupportedColombianBank.entries.size})",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Text(
                text = "Bancos, billeteras digitales, tarjetas de crédito, cooperativas y SMS transaccionales de Colombia.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF94A3B8)
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Grid flexible de bancos soportados con acento de color
            SupportedColombianBank.entries.chunked(3).forEach { rowBanks ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowBanks.forEach { bank ->
                        val color = try {
                            Color(android.graphics.Color.parseColor(bank.brandColorHex))
                        } catch (e: Exception) {
                            Color(0xFF10B981)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1A1F29),
                            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = bank.displayName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        fontSize = 10.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    // Rellenar espacios vacíos si la última fila tiene menos de 3 elementos
                    repeat(3 - rowBanks.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Interactive Simulator
            Text(
                text = "Simulador de Notificaciones Bancarias",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF34D399)
                )
            )
            Text(
                text = "Genera un evento idéntico a una notificación push real para probar la captura en 1 toque.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF94A3B8)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            listOf(
                "nequi bre-b" to "⚡ Simular Bre-B Nequi ($5 COP)",
                "Bancolombia" to "💳 Simular compra Bancolombia ($48.500)",
                "Nequi" to "💜 Simular pago Nequi ($22.000)",
                "Nu Colombia" to "🟣 Simular compra Nu ($42.000)",
                "DaviPlata" to "🔴 Simular compra DaviPlata ($45.000)",
                "Banco de Bogota" to "🔵 Simular compra Banco de Bogotá ($89.000)",
                "Falabella" to "🟢 Simular compra CMR Falabella ($120.000)",
                "Dale" to "🟡 Simular Bre-B Dale! ($25.000)",
                "RappiPay" to "🟠 Simular pago RappiPay ($35.000)"
            ).forEach { (key, label) ->
                OutlinedButton(
                    onClick = {
                        onSimulate(key)
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = label, fontSize = 13.sp)
                }
            }
        }
    }
}
