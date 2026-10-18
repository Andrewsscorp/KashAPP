package com.example.presentation.fixedassets.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AssetAuditLogEntity

@Composable
fun FixedAssetsAuditTab(
    auditLogs: List<AssetAuditLogEntity>,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("TODOS") }

    val cardBg = Color(0xFF0B172B)
    val cardBorder = Color(0xFF132B4F)
    val primaryBlue = Color(0xFF007AFF)
    val accentCyan = Color(0xFF38BDF8)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF8EA5C4)

    // Filtrar registros
    val filteredLogs = remember(auditLogs, selectedFilter, searchQuery) {
        auditLogs.filter { log ->
            val matchesFilter = when (selectedFilter) {
                "TODOS" -> true
                "CREACION" -> log.actionType.equals("CREACION", ignoreCase = true)
                "MODIFICACION" -> log.actionType.equals("MODIFICACION", ignoreCase = true)
                "DEPRECIACION" -> log.actionType.equals("DEPRECIACION", ignoreCase = true)
                "MANTENIMIENTO" -> log.actionType.equals("MANTENIMIENTO", ignoreCase = true)
                "BAJA" -> log.actionType.equals("BAJA", ignoreCase = true)
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    log.actionTitle.contains(searchQuery, ignoreCase = true) ||
                    (log.assetName ?: "").contains(searchQuery, ignoreCase = true) ||
                    log.details.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    val totalCount = auditLogs.size
    val creacionCount = auditLogs.count { it.actionType.equals("CREACION", ignoreCase = true) }
    val depCount = auditLogs.count { it.actionType.equals("DEPRECIACION", ignoreCase = true) }
    val bajaCount = auditLogs.count { it.actionType.equals("BAJA", ignoreCase = true) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. BANNER AUDITORÍA GENERAL
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0E2849)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = accentCyan, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text(
                                text = "Bitácora Oficial de Auditoría",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "Trazabilidad inmutable bajo NIC 16 / NIIF",
                                fontSize = 11.5.sp,
                                color = textSecondary
                            )
                        }
                    }

                    // Acciones de Exportación
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E3A8A),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onExportPdf() }
                                .testTag("btn_exportar_auditoria_pdf")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Text("PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF065F46),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onExportExcel() }
                                .testTag("btn_exportar_auditoria_excel")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Text("Excel", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // Mini KPIs de Auditoría
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AuditMetricCard(
                        title = "Total Eventos",
                        value = "$totalCount",
                        color = accentCyan,
                        modifier = Modifier.weight(1f)
                    )
                    AuditMetricCard(
                        title = "Altas de Activo",
                        value = "$creacionCount",
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    AuditMetricCard(
                        title = "Cierres Deprec.",
                        value = "$depCount",
                        color = Color(0xFFA78BFA),
                        modifier = Modifier.weight(1f)
                    )
                    AuditMetricCard(
                        title = "Bajas / Retiros",
                        value = "$bajaCount",
                        color = Color(0xFFF87171),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. BUSCADOR
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar en bitácora por activo, detalle...", color = textSecondary, fontSize = 12.5.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = textSecondary, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryBlue,
                    unfocusedBorderColor = cardBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF071221),
                    unfocusedContainerColor = Color(0xFF071221)
                )
            )
        }

        // 3. CHIPS DE FILTRO HORIZONTALES
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filterOptions = listOf(
                "TODOS" to "Todos",
                "CREACION" to "Altas",
                "MODIFICACION" to "Modificaciones",
                "DEPRECIACION" to "Depreciación",
                "MANTENIMIENTO" to "Mantenimiento",
                "BAJA" to "Bajas"
            )

            filterOptions.forEach { (code, label) ->
                val isSel = selectedFilter == code
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSel) primaryBlue else Color(0xFF08172C),
                    border = BorderStroke(1.dp, if (isSel) primaryBlue else cardBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedFilter = code }
                ) {
                    Text(
                        text = label,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) Color.White else textSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // 4. LISTADO DE EVENTOS DE AUDITORÍA
        if (filteredLogs.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = textSecondary, modifier = Modifier.size(36.dp))
                    Text("No hay registros en la bitácora", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text("Todas las altas, cambios de estado y depreciaciones se registrarán aquí automáticamente.", fontSize = 11.5.sp, color = textSecondary, textAlign = TextAlign.Center)
                }
            }
        } else {
            filteredLogs.forEach { log ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    AuditLogRowCard(
                        log = log,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun AuditMetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF061120),
        border = BorderStroke(1.dp, Color(0xFF10284A)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
            Text(title, fontSize = 11.sp, color = Color(0xFF94A3B8), maxLines = 1)
        }
    }
}

@Composable
private fun AuditLogRowCard(
    log: AssetAuditLogEntity,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val (badgeBg, badgeText, badgeIcon) = when (log.actionType) {
        "CREACION" -> Triple(Color(0xFF0284C7).copy(alpha = 0.2f), Color(0xFF38BDF8), Icons.Default.AddCircle)
        "MODIFICACION" -> Triple(Color(0xFF3B82F6).copy(alpha = 0.2f), Color(0xFF60A5FA), Icons.Default.Edit)
        "CAMBIO_ESTADO" -> Triple(Color(0xFF8B5CF6).copy(alpha = 0.2f), Color(0xFFA78BFA), Icons.Default.History)
        "DEPRECIACION" -> Triple(Color(0xFF10B981).copy(alpha = 0.2f), Color(0xFF34D399), Icons.Default.Autorenew)
        "MANTENIMIENTO" -> Triple(Color(0xFFF59E0B).copy(alpha = 0.2f), Color(0xFFFBBF24), Icons.Default.Build)
        "BAJA" -> Triple(Color(0xFFEF4444).copy(alpha = 0.2f), Color(0xFFF87171), Icons.Default.Delete)
        else -> Triple(Color(0xFF64748B).copy(alpha = 0.2f), Color(0xFF94A3B8), Icons.AutoMirrored.Filled.ReceiptLong)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg,
                    border = BorderStroke(1.dp, badgeText.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(badgeIcon, contentDescription = null, tint = badgeText, modifier = Modifier.size(12.dp))
                        Text(log.actionType, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeText)
                    }
                }

                Text(log.formattedDate, fontSize = 11.sp, color = textSecondary)
            }

            Text(
                text = log.actionTitle,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Text(
                text = log.details,
                fontSize = 12.sp,
                color = textSecondary,
                lineHeight = 16.sp
            )

            val impact = log.financialImpact
            if (!impact.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF071527),
                    border = BorderStroke(1.dp, Color(0xFF132B4F)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Impacto:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                        Text(impact, fontSize = 11.sp, color = Color(0xFFE2E8F0))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Responsable: ${log.userRole}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                if (!log.assetName.isNullOrBlank()) {
                    Text(
                        text = "Activo: ${log.assetName}",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
