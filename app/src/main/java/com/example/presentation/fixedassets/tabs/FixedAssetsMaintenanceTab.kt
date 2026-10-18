package com.example.presentation.fixedassets.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FixedAssetEntity
import com.example.domain.model.fixedassets.MaintenanceOrder
import com.example.domain.model.fixedassets.MaintenanceStatus
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedAssetsMaintenanceTab(
    maintenanceOrders: List<MaintenanceOrder>,
    assets: List<FixedAssetEntity>,
    onCreateMaintenanceOrder: (
        assetId: String,
        title: String,
        amount: Double,
        provider: String,
        notes: String,
        type: String,
        isCapitalizable: Boolean,
        initialStatus: MaintenanceStatus
    ) -> Unit,
    onUpdateOrderStatus: (
        movementId: String,
        assetId: String,
        newStatus: MaintenanceStatus,
        resolutionNotes: String
    ) -> Unit,
    onSelectAssetForDetail: (FixedAssetEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val moneyFmt = remember { DecimalFormat("$ #,##0") }
    var selectedStatusFilter by remember { mutableStateOf<MaintenanceStatus?>(null) } // null = Todos
    var showCreateOrderSheet by remember { mutableStateOf(false) }
    var selectedOrderForDetail by remember { mutableStateOf<MaintenanceOrder?>(null) }

    // Metrics
    val totalExpense = maintenanceOrders.filter { !it.isCapitalizable }.sumOf { it.amount }
    val totalCapitalized = maintenanceOrders.filter { it.isCapitalizable }.sumOf { it.amount }
    val inProgressCount = maintenanceOrders.count { it.status == MaintenanceStatus.IN_PROGRESS }
    val pendingCount = maintenanceOrders.count { it.status == MaintenanceStatus.PENDING }

    // Filtered list
    val filteredOrders = remember(maintenanceOrders, selectedStatusFilter) {
        if (selectedStatusFilter == null) maintenanceOrders
        else maintenanceOrders.filter { it.status == selectedStatusFilter }
    }

    val cardBg = Color(0xFF0B172B)
    val cardBorder = Color(0xFF132B4F)
    val primaryBlue = Color(0xFF007AFF)
    val accentCyan = Color(0xFF38BDF8)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF8EA5C4)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. BANNER METRICAS DE MANTENIMIENTO
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
                            Icon(Icons.Default.Build, contentDescription = null, tint = accentCyan, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                text = "Gestión de Mantenimientos",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "Historial físico y órdenes de servicio",
                                fontSize = 11.5.sp,
                                color = textSecondary
                            )
                        }
                    }

                    Button(
                        onClick = { showCreateOrderSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_crear_orden_mantenimiento")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("+ Orden", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Cuadrícula de Métricas Propias
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF071222),
                        border = BorderStroke(1.dp, Color(0xFF163259)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Gasto Operacional", fontSize = 11.sp, color = textSecondary)
                            Text(
                                text = moneyFmt.format(totalExpense),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text("No altera patrimonio", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF071222),
                        border = BorderStroke(1.dp, Color(0xFF163259)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Órdenes en Taller", fontSize = 11.sp, color = textSecondary)
                            Text(
                                text = "$inProgressCount en curso",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (inProgressCount > 0) Color(0xFF38BDF8) else Color(0xFF10B981)
                            )
                            Text("$pendingCount pendientes", fontSize = 11.sp, color = Color(0xFFF59E0B))
                        }
                    }
                }
            }
        }

        // 2. KANBAN / STATUS FILTER TABS (Todos, Pendiente, En Progreso, Finalizado)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                Pair(null, "Todos (${maintenanceOrders.size})"),
                Pair(MaintenanceStatus.PENDING, "Pendientes ($pendingCount)"),
                Pair(MaintenanceStatus.IN_PROGRESS, "En Curso ($inProgressCount)"),
                Pair(MaintenanceStatus.COMPLETED, "Finalizados (${maintenanceOrders.count { it.status == MaintenanceStatus.COMPLETED }})")
            )

            tabs.forEach { (status, label) ->
                val isSelected = selectedStatusFilter == status
                val pillColor = when (status) {
                    MaintenanceStatus.PENDING -> Color(0xFFF59E0B)
                    MaintenanceStatus.IN_PROGRESS -> Color(0xFF38BDF8)
                    MaintenanceStatus.COMPLETED -> Color(0xFF10B981)
                    null -> primaryBlue
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) pillColor.copy(alpha = 0.2f) else Color(0xFF091629),
                    border = BorderStroke(1.dp, if (isSelected) pillColor else Color(0xFF163259)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedStatusFilter = status }
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) pillColor else textSecondary,
                        modifier = Modifier.padding(vertical = 7.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }

        // 3. LISTADO DE ÓRDENES DE TRABAJO
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredOrders.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Construction, contentDescription = null, tint = textSecondary, modifier = Modifier.size(34.dp))
                        Text("No hay órdenes en este estado", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text("Registra una nueva orden de servicio para cualquier activo.", fontSize = 11.sp, color = textSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { showCreateOrderSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Crear Orden de Servicio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                filteredOrders.forEach { order ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedOrderForDetail = order }
                            .testTag("maintenance_order_${order.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Cabecera de la Orden: Título + Status Pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = order.title,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = order.assetName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = accentCyan
                                        )
                                        Text(text = "•", fontSize = 11.sp, color = Color(0xFF475569))
                                        Text(text = order.assetCode, fontSize = 11.5.sp, color = textSecondary)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = order.status.color.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, order.status.color.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(order.status.color))
                                        Text(
                                            text = order.status.label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = order.status.color
                                        )
                                    }
                                }
                            }

                            // Datos: Fecha, Proveedor, Tipo y Costo
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = "${order.maintenanceType}  •  ${order.date}",
                                        fontSize = 11.sp,
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!order.provider.isNullOrBlank()) {
                                        Text(
                                            text = "Taller: ${order.provider}",
                                            fontSize = 11.sp,
                                            color = Color(0xFFCBD5E1),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = moneyFmt.format(order.amount),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (order.isCapitalizable) "Mejora NIC 16" else "Gasto Operacional",
                                        fontSize = 11.sp,
                                        color = if (order.isCapitalizable) Color(0xFF10B981) else Color(0xFF94A3B8)
                                    )
                                }
                            }

                            // Barra de Transición de Estados (Domain Event: Iniciar Mantenimiento / Finalizar)
                            HorizontalDivider(color = Color(0xFF102644), thickness = 0.5.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Impacto contable
                                Text(
                                    text = if (order.isCapitalizable) "✓ Incrementa valor del activo" else "Gasto en Estado de Resultados",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.weight(1f, fill = false),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Acciones de transición
                                when (order.status) {
                                    MaintenanceStatus.PENDING -> {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF0284C7),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    // Domain Event: Al pasar a En Progreso, actualiza automáticamente el activo a EN_MANTENIMIENTO
                                                    onUpdateOrderStatus(order.id, order.assetId, MaintenanceStatus.IN_PROGRESS, "Mantenimiento iniciado en taller")
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                                Text("Iniciar (Taller)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                    MaintenanceStatus.IN_PROGRESS -> {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF059669),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    // Domain Event: Finaliza y retorna el activo a EN_USO
                                                    onUpdateOrderStatus(order.id, order.assetId, MaintenanceStatus.COMPLETED, "Mantenimiento completado satisfactoriamente")
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.Done, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                                Text("Finalizar y Dar de Alta", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                    MaintenanceStatus.COMPLETED -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                            Text("Completado", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 4. MODAL PARA CREAR ORDEN DE SERVICIO / MANTENIMIENTO
    if (showCreateOrderSheet) {
        var selectedAsset by remember { mutableStateOf(assets.firstOrNull()) }
        var isAssetDropdownExpanded by remember { mutableStateOf(false) }
        var orderTitle by remember { mutableStateOf("") }
        var orderCostText by remember { mutableStateOf("") }
        var providerText by remember { mutableStateOf("") }
        var notesText by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf("Preventivo") }
        var isCapitalizable by remember { mutableStateOf(false) }
        var initialStatus by remember { mutableStateOf(MaintenanceStatus.PENDING) }

        ModalBottomSheet(
            onDismissRequest = { showCreateOrderSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF091424),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 38.dp, height = 4.dp)
                        .background(Color(0xFF334A6E), RoundedCornerShape(2.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Nueva Orden de Trabajo",
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Vincular mantenimiento a equipo existente",
                            fontSize = 11.5.sp,
                            color = textSecondary
                        )
                    }
                    IconButton(onClick = { showCreateOrderSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = textSecondary)
                    }
                }

                // 1. Selector de Activo Fijo (Entre los 12 equipos existentes)
                Text("Equipo a intervenir:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                ExposedDropdownMenuBox(
                    expanded = isAssetDropdownExpanded,
                    onExpandedChange = { isAssetDropdownExpanded = !isAssetDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAsset?.let { "${it.name} (${it.assetCode})" } ?: "Seleccionar activo...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAssetDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = cardBg,
                            unfocusedContainerColor = cardBg,
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = cardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = isAssetDropdownExpanded,
                        onDismissRequest = { isAssetDropdownExpanded = false },
                        modifier = Modifier.background(Color(0xFF091424))
                    ) {
                        assets.forEach { a ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(a.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${a.assetCode} • ${a.category}", color = textSecondary, fontSize = 11.sp)
                                    }
                                },
                                onClick = {
                                    selectedAsset = a
                                    isAssetDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // 2. Concepto / Título
                OutlinedTextField(
                    value = orderTitle,
                    onValueChange = { orderTitle = it },
                    label = { Text("Concepto o Diagnóstico", color = textSecondary) },
                    placeholder = { Text("Ej. Cambio de aceite y filtros, calibración", color = Color(0xFF64748B)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardBg,
                        unfocusedContainerColor = cardBg,
                        focusedBorderColor = primaryBlue,
                        unfocusedBorderColor = cardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // 3. Tipo de Mantenimiento
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Preventivo", "Correctivo", "Calibración", "Inspección").forEach { t ->
                        val isSel = selectedType == t
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) primaryBlue else Color(0xFF071222),
                            border = BorderStroke(1.dp, if (isSel) primaryBlue else Color(0xFF163259)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedType = t }
                        ) {
                            Text(
                                text = t,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = Color.White,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // 4. Costo y Proveedor
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = orderCostText,
                        onValueChange = { orderCostText = it.filter { c -> c.isDigit() } },
                        label = { Text("Costo ($ COP)", color = textSecondary) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = cardBg,
                            unfocusedContainerColor = cardBg,
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = cardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = providerText,
                        onValueChange = { providerText = it },
                        label = { Text("Taller / Proveedor", color = textSecondary) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = cardBg,
                            unfocusedContainerColor = cardBg,
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = cardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // 5. Toggle de Mejora Capitalizable (NIC 16)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF071424),
                    border = BorderStroke(1.dp, Color(0xFF132F52)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("¿Es Mejora Capitalizable? (NIC 16)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                text = if (isCapitalizable)
                                    "Aumentará el valor contable del activo en libros."
                                else
                                    "Se contabiliza como Gasto Operacional sin alterar patrimonio.",
                                fontSize = 11.sp,
                                color = if (isCapitalizable) Color(0xFF10B981) else textSecondary
                            )
                        }
                        Switch(
                            checked = isCapitalizable,
                            onCheckedChange = { isCapitalizable = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981)
                            )
                        )
                    }
                }

                // 6. Estado Inicial: Pendiente vs Iniciar de Inmediato
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isPending = initialStatus == MaintenanceStatus.PENDING
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isPending) Color(0xFF1A2A44) else Color(0xFF071222),
                        border = BorderStroke(1.dp, if (isPending) Color(0xFFF59E0B) else Color(0xFF163259)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { initialStatus = MaintenanceStatus.PENDING }
                    ) {
                        Text(
                            text = "Crear como Pendiente",
                            fontSize = 11.5.sp,
                            color = if (isPending) Color(0xFFF59E0B) else textSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    val isInProgress = initialStatus == MaintenanceStatus.IN_PROGRESS
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isInProgress) Color(0xFF132A4A) else Color(0xFF071222),
                        border = BorderStroke(1.dp, if (isInProgress) Color(0xFF38BDF8) else Color(0xFF163259)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { initialStatus = MaintenanceStatus.IN_PROGRESS }
                    ) {
                        Text(
                            text = "Ingresar a Taller Ahora",
                            fontSize = 11.5.sp,
                            color = if (isInProgress) Color(0xFF38BDF8) else textSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // Botón Guardar
                Button(
                    onClick = {
                        val assetId = selectedAsset?.id ?: return@Button
                        val amount = orderCostText.toDoubleOrNull() ?: 0.0
                        onCreateMaintenanceOrder(
                            assetId,
                            orderTitle,
                            amount,
                            providerText,
                            notesText,
                            selectedType,
                            isCapitalizable,
                            initialStatus
                        )
                        showCreateOrderSheet = false
                    },
                    enabled = selectedAsset != null && orderTitle.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Registrar Orden de Trabajo", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
