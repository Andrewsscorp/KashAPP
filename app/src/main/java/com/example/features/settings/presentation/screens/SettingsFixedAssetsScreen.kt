package com.example.features.settings.presentation.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy

/**
 * Dedicated Sub-Screen for Fixed Assets Settings ("Activos Fijos").
 * Contains all enterprise accounting rules, inventory exclusions, and net worth impact configurations.
 */
@Composable
fun SettingsFixedAssetsSubScreen(
    autoNoAffectInventoryForAssets: Boolean,
    autoNoAffectDashboardForAssets: Boolean,
    excludeAllFixedAssetsFromDashboard: Boolean,
    onToggleAutoNoAffectInventory: (Boolean) -> Unit,
    onApplyNoAffectInventoryToAll: (Boolean) -> Unit,
    onToggleAutoNoAffectDashboard: (Boolean) -> Unit,
    onToggleExcludeAllFixedAssetsFromDashboard: (Boolean) -> Unit,
    onApplyNoAffectDashboardToAll: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMassApplyDashboardDialog by remember { mutableStateOf(false) }
    var showMassApplyInventoryDialog by remember { mutableStateOf(false) }

    // Dialog: Regla Masiva de Patrimonio / Dashboard (En color azul claro)
    if (showMassApplyDashboardDialog) {
        AlertDialog(
            onDismissRequest = { showMassApplyDashboardDialog = false },
            containerColor = Color(0xFFF0F9FF), // Color azul claro requerido
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Afectación en Patrimonio Neto",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF0369A1)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Configura si los activos fijos deben sumar o no en el cálculo de tu Patrimonio Neto:",
                        fontSize = 13.sp,
                        color = Color(0xFF0F172A)
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFE0F2FE),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "• 'Excluir de Patrimonio': Los activos seguirán registrados en la app y con su valor en libros visible en la tarjeta 'Activos', pero NO inflarán el Patrimonio Neto principal.",
                                fontSize = 11.5.sp,
                                color = Color(0xFF0369A1),
                                lineHeight = 15.sp
                            )
                            Text(
                                text = "• 'Incluir en Patrimonio': El valor de los activos se sumará normalmente al cálculo global del Patrimonio Neto.",
                                fontSize = 11.5.sp,
                                color = Color(0xFF0369A1),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMassApplyDashboardDialog = false
                        onApplyNoAffectDashboardToAll(true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Excluir de Patrimonio", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { showMassApplyDashboardDialog = false }) {
                        Text("Cancelar", color = Color(0xFF64748B))
                    }
                    OutlinedButton(
                        onClick = {
                            showMassApplyDashboardDialog = false
                            onApplyNoAffectDashboardToAll(false)
                        },
                        border = BorderStroke(1.dp, Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Incluir en Patrimonio", color = Color(0xFF0284C7), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        )
    }

    // Dialog: Regla Masiva de Inventarios
    if (showMassApplyInventoryDialog) {
        AlertDialog(
            onDismissRequest = { showMassApplyInventoryDialog = false },
            containerColor = Color(0xFFF0FDF4),
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDCFCE7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Regla Masiva de Inventarios",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF166534)
                )
            },
            text = {
                Text(
                    text = "¿Deseas aplicar la regla 'No afecta inventarios' a todos los activos fijos registrados en el sistema?",
                    fontSize = 13.sp,
                    color = Color(0xFF14532D)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMassApplyInventoryDialog = false
                        onApplyNoAffectInventoryToAll(true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Aplicar a Todos", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMassApplyInventoryDialog = false }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // CARD 1: PATRIMONIO NETO Y DASHBOARD
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Patrimonio Neto y Dashboard", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = KashNavy)
                        Text("Controla qué activos suman al total del tablero", fontSize = 11.5.sp, color = Color(0xFF64748B))
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Switch 1: Excluir todos los activos del Patrimonio Neto
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Excluir todos los activos de Patrimonio", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (excludeAllFixedAssetsFromDashboard)
                                "Los activos están excluidos del Patrimonio Neto. Siguen visibles en la casilla de Activos con su valor en libros."
                            else
                                "Los activos fijos válidos suman normalmente al cálculo global del Patrimonio Neto.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 15.sp
                        )
                    }
                    Switch(
                        checked = excludeAllFixedAssetsFromDashboard,
                        onCheckedChange = { onToggleExcludeAllFixedAssetsFromDashboard(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF0284C7)
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Switch 2: Marcar 'No afecta dashboard' por defecto para nuevos activos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Pre-marcar 'No afecta patrimonio' en nuevos activos", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (autoNoAffectDashboardForAssets)
                                "Al crear nuevos activos, se marcarán automáticamente para no sumar al Patrimonio Neto."
                            else
                                "Los nuevos activos afectarán el Patrimonio Neto por defecto.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 15.sp
                        )
                    }
                    Switch(
                        checked = autoNoAffectDashboardForAssets,
                        onCheckedChange = { onToggleAutoNoAffectDashboard(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF0284C7)
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Button Tile: Aplicar a todos los existentes
                Surface(
                    onClick = { showMassApplyDashboardDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth().testTag("settings_btn_mass_dashboard_rules")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.TableChart, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                            Text("Aplicar regla masiva a activos existentes", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // CARD 2: INVENTARIOS Y NIIF
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Inventarios y Reglas NIIF NIC 16", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = KashNavy)
                        Text("Separación contable de bienes de uso propio e inventarios", fontSize = 11.5.sp, color = Color(0xFF64748B))
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Switch 3: No afecta inventarios por defecto
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Marcar 'No afecta inventarios' por defecto", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (autoNoAffectInventoryForAssets)
                                "Los nuevos activos vendrán con la opción de inventario desvinculada por defecto."
                            else
                                "Los nuevos activos vincularán inventarios por defecto.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 15.sp
                        )
                    }
                    Switch(
                        checked = autoNoAffectInventoryForAssets,
                        onCheckedChange = { onToggleAutoNoAffectInventory(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF16A34A)
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Button Tile: Aplicar regla de inventarios a todos
                Surface(
                    onClick = { showMassApplyInventoryDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth().testTag("settings_btn_mass_inventory_rules")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                            Text("Aplicar regla de inventarios a activos existentes", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // CARD 3: AUDITORÍA Y TRAZABILIDAD
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Trazabilidad Inmutable NIIF NIC 16", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF166534))
                    Text(
                        text = "Cada alta, baja, depreciación y cambio de regla en tus activos se registra criptográficamente con SHA-256 en la Bóveda de Auditoría Forense sin posibilidad de borrado contable.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF15803D),
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
