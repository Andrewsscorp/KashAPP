package com.example.presentation.fixedassets

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FixedAssetEntity
import com.example.domain.model.fixedassets.AssetCategory
import com.example.domain.model.fixedassets.AssetFinancialCalculations
import com.example.domain.model.fixedassets.AssetStatus
import com.example.domain.model.fixedassets.statemachine.AssetStateMachine
import com.example.presentation.dashboard.detail.components.RubroExportSuccessDialog
import com.example.presentation.fixedassets.components.FixedAssetExportIPhoneSheet
import com.example.presentation.fixedassets.components.FixedAssetFilterIPhoneSheet
import com.example.presentation.fixedassets.components.RunDepreciationIPhoneSheet
import com.example.presentation.fixedassets.create.CreateFixedAssetScreen
import com.example.presentation.fixedassets.create.state.FixedAssetFormState
import com.example.presentation.fixedassets.detail.AssetHardwareThumbnail
import com.example.presentation.fixedassets.detail.FixedAssetDetailScreen
import com.example.presentation.fixedassets.projections.FixedAssetProjectionsScreen
import com.example.presentation.fixedassets.tabs.FixedAssetsInventoryTab
import com.example.presentation.fixedassets.tabs.FixedAssetsDepreciationTab
import com.example.presentation.fixedassets.tabs.FixedAssetsMaintenanceTab
import com.example.presentation.fixedassets.tabs.FixedAssetsAuditTab
import com.example.presentation.fixedassets.notification.FixedAssetNotificationHelper
import com.example.domain.model.fixedassets.MaintenanceStatus
import com.example.feature.activos.ui.FixedAssetsAnalyticsBanner
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Módulo de Activos Fijos Tangibles: Responsivo, Dinámico y Empresarial.
 * Sin datos mockeados en el código, con visualizaciones calculadas en tiempo real.
 */
@Composable
fun FixedAssetsScreen(
    viewModel: FixedAssetsViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val moneyFmt = DecimalFormat("$ #,##0")

    BackHandler {
        when {
            uiState.selectedAssetForDetail != null -> viewModel.selectAssetForDetail(null)
            uiState.isCreatingAsset -> viewModel.closeCreateOrEditAsset()
            uiState.isViewingProjections -> viewModel.closeProjections()
            else -> onDismiss()
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    // Sub-pantallas
    if (uiState.isCreatingAsset) {
        val initialForm = uiState.assetToEdit?.let { FixedAssetFormState.fromEntity(it) } ?: FixedAssetFormState()
        CreateFixedAssetScreen(
            initialState = initialForm,
            availableCategories = uiState.categories,
            onCreateCategory = { name, code, colorHex, iconName ->
                viewModel.createCategory(name, code, colorHex, iconName)
            },
            onSaveAsset = { form ->
                val entity = form.toEntity(viewModel.userId)
                viewModel.saveAsset(entity)
                FixedAssetNotificationHelper.sendAssetCreatedNotification(context, entity.name, entity.acquisitionValue)
            },
            onDismiss = { viewModel.closeCreateOrEditAsset() }
        )
        return
    }

    if (uiState.isViewingProjections) {
        FixedAssetProjectionsScreen(
            assets = uiState.allAssets,
            projectAssetValuesUseCase = viewModel.projectAssetValuesUseCase,
            onDismiss = { viewModel.closeProjections() }
        )
        return
    }

    if (uiState.selectedAssetForDetail != null) {
        val asset = uiState.selectedAssetForDetail!!
        val movements by remember(asset.id) { viewModel.observeMovementsForAsset(asset.id) }.collectAsState(initial = emptyList())
        FixedAssetDetailScreen(
            asset = asset,
            movements = movements,
            onEditAsset = { viewModel.openEditAsset(it) },
            onUpdateAsset = { viewModel.saveAsset(it) },
            onChangeAssetState = { a, code, notes, mov -> viewModel.changeAssetState(a, code, notes, mov) },
            onDeleteAsset = { viewModel.deleteAsset(it) },
            onRecordMovement = { viewModel.recordMovement(it) },
            onRunIndividualDepreciation = { viewModel.toggleRunDepreciationSheet(true) },
            onDismiss = { viewModel.selectAssetForDetail(null) }
        )
        return
    }

    // Estado local de la vista
    var isSearchExpanded by remember { mutableStateOf(false) }
    var selectedTopTab by remember { mutableIntStateOf(0) }
    val topTabs = listOf("Resumen", "Activos", "Depreciación", "Mantenimientos", "Auditoría")
    var showTopOverflowMenu by remember { mutableStateOf(false) }
    var assetToDeleteForConfirm by remember { mutableStateOf<FixedAssetEntity?>(null) }

    // Paleta corporativa Dark Navy
    val bgDark = Color(0xFF07101E)
    val cardBg = Color(0xFF0B172B)
    val cardBorder = Color(0xFF132B4F)
    val primaryBlue = Color(0xFF007AFF)
    val accentCyan = Color(0xFF38BDF8)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF8EA5C4)

    // Métricas calculadas en tiempo real de la base de datos
    val totalAssets = uiState.allAssets.size
    val activeCount = uiState.allAssets.count {
        val s = AssetStateMachine.resolveState(it.status)
        s.code == "EN_USO" || s.code == "ACTIVE"
    }
    val maintenanceCount = uiState.allAssets.count {
        val s = AssetStateMachine.resolveState(it.status)
        s.code == "EN_MANTENIMIENTO" || s.code == "MAINTENANCE"
    }
    val writtenOffCount = uiState.allAssets.count {
        val s = AssetStateMachine.resolveState(it.status)
        s.isTerminal
    }

    val monthlyDepreciationTotal = uiState.allAssets.sumOf {
        AssetFinancialCalculations.calculateMonthlyDepreciation(
            acquisitionValue = it.acquisitionValue,
            residualValue = it.residualValue,
            usefulLifeMonths = it.usefulLifeMonths
        )
    }

    // Filtrado según la pestaña activa
    val displayedAssets = when (selectedTopTab) {
        1 -> uiState.filteredAssets // "Activos"
        2 -> uiState.filteredAssets.filter { it.hasDepreciation } // "Depreciación"
        3 -> uiState.allAssets.filter {
            val s = AssetStateMachine.resolveState(it.status)
            s.code == "EN_MANTENIMIENTO" || s.code == "MAINTENANCE"
        } // "Mantenimientos"
        else -> uiState.filteredAssets // "Resumen" y "Reportes"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. TOP APP BAR RESPONSIVO (Con botón "+ Nuevo activo" profesional, nunca bola redonda)
            Surface(
                color = Color(0xFF091424),
                border = BorderStroke(1.dp, Color(0xFF102644)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Título con botón volver
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Volver",
                                    tint = textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(Color(0xFF0E2849)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = accentCyan,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Text(
                                    text = "Activos Fijos",
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Gestión patrimonial NIIF",
                                    fontSize = 11.sp,
                                    color = textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Acciones a la derecha
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = { isSearchExpanded = !isSearchExpanded },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = "Buscar",
                                    tint = textSecondary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            Box {
                                IconButton(
                                    onClick = { showTopOverflowMenu = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Más opciones",
                                        tint = textSecondary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showTopOverflowMenu,
                                    onDismissRequest = { showTopOverflowMenu = false },
                                    modifier = Modifier.background(Color(0xFF0C1F38))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Correr depreciación contable", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.AutoMode, null, tint = accentCyan) },
                                        onClick = {
                                            showTopOverflowMenu = false
                                            viewModel.toggleRunDepreciationSheet(true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Proyecciones financieras multi-anual", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.TrendingUp, null, tint = Color(0xFF8B5CF6)) },
                                        onClick = {
                                            showTopOverflowMenu = false
                                            viewModel.openProjections()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Exportar inventario", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.FileDownload, null, tint = Color(0xFF10B981)) },
                                        onClick = {
                                            showTopOverflowMenu = false
                                            viewModel.toggleExportSheet(true)
                                        }
                                    )
                                }
                            }

                            // BOTÓN CREAR ACTIVO: Rectángulo redondeado elegante con texto "+ Nuevo" (NO bola redonda)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = primaryBlue,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.openCreateAsset() }
                                    .testTag("top_add_asset_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Nuevo",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Campo de búsqueda desplegable
                    AnimatedVisibility(visible = isSearchExpanded) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChange(it) },
                                placeholder = { Text("Buscar por nombre, placa, marca o código...", color = textSecondary) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("asset_search_input"),
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
                    }

                    // Pestañas horizontales (Tabs)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        topTabs.forEachIndexed { index, tab ->
                            val isSelected = selectedTopTab == index
                            Column(
                                modifier = Modifier
                                    .clickable { selectedTopTab = index }
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = tab,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else textSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .height(2.5.dp)
                                        .width(32.dp)
                                        .background(if (isSelected) primaryBlue else Color.Transparent, RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }

                    // BOTONES VISIBLES DE ACCIÓN RÁPIDA (Opciones que estaban en los tres puntos)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0E223D),
                            border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.toggleRunDepreciationSheet(true) }
                                .testTag("btn_correr_depreciacion")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Default.AutoMode, contentDescription = null, tint = accentCyan, modifier = Modifier.size(15.dp))
                                Text("Depreciación", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0E223D),
                            border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.openProjections() }
                                .testTag("btn_proyecciones_niif")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(15.dp))
                                Text("Proyecciones", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0E223D),
                            border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.toggleExportSheet(true) }
                                .testTag("btn_exportar_activos")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(15.dp))
                                Text("Exportar", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0E223D),
                            border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.toggleFilterSheet(true) }
                                .testTag("btn_filtros_activos")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Default.FilterList, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(15.dp))
                                Text("Filtros", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // 2. CONTENIDO PRINCIPAL SCROLLABLE
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                when (selectedTopTab) {
                    0 -> {
                        // 0: Resumen (Dashboard Agregado / Read-Model)
                        item {
                            FixedAssetsAnalyticsBanner(
                                totalAcquisitionValue = uiState.totalAcquisitionCost,
                                totalAccumulatedDepreciation = uiState.totalAccumulatedDepreciation,
                                totalBookValue = uiState.totalBookValue,
                                activeAssetsCount = totalAssets,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        // 3 TARJETAS KPI EJECUTIVAS RESPONSIVAS
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ExecutiveKpiCard(
                                    title = "Valor total de activos",
                                    amount = moneyFmt.format(uiState.totalAcquisitionCost),
                                    subtitle = "$totalAssets activos registrados",
                                    icon = Icons.Default.Inventory2,
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    accentCyan = accentCyan,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    modifier = Modifier.width(185.dp)
                                )

                                val depPct = if (uiState.totalAcquisitionCost > 0) {
                                    ((uiState.totalAccumulatedDepreciation / uiState.totalAcquisitionCost) * 100).toInt()
                                } else 0
                                ExecutiveKpiCard(
                                    title = "Depreciación acumulada",
                                    amount = moneyFmt.format(uiState.totalAccumulatedDepreciation),
                                    subtitle = "$depPct% del valor total",
                                    icon = Icons.Default.TrendingDown,
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    accentCyan = accentCyan,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    modifier = Modifier.width(185.dp)
                                )

                                ExecutiveKpiCard(
                                    title = "Depreciación del mes",
                                    amount = moneyFmt.format(monthlyDepreciationTotal),
                                    subtitle = "Promedio mensual NIIF",
                                    icon = Icons.Default.CalendarToday,
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    accentCyan = accentCyan,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    modifier = Modifier.width(185.dp)
                                )
                            }
                        }

                        // 4 PILLS DE ESTADO RESPONSIVAS
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatusFilterPill(
                                    title = "Total de activos",
                                    count = totalAssets,
                                    icon = Icons.Default.Inventory2,
                                    iconColor = accentCyan,
                                    isSelected = uiState.selectedStatusFilter == "Todos",
                                    onClick = { viewModel.resetFilters() },
                                    modifier = Modifier.widthIn(min = 120.dp)
                                )
                                StatusFilterPill(
                                    title = "En uso",
                                    count = activeCount,
                                    icon = Icons.Default.Security,
                                    iconColor = Color(0xFF10B981),
                                    isSelected = uiState.selectedStatusFilter == "ACTIVE",
                                    onClick = { viewModel.onStatusFilterChange("ACTIVE") },
                                    modifier = Modifier.widthIn(min = 95.dp)
                                )
                                StatusFilterPill(
                                    title = "En mantenimiento",
                                    count = maintenanceCount,
                                    icon = Icons.Default.Build,
                                    iconColor = Color(0xFF38BDF8),
                                    isSelected = uiState.selectedStatusFilter == "MAINTENANCE",
                                    onClick = { viewModel.onStatusFilterChange("MAINTENANCE") },
                                    modifier = Modifier.widthIn(min = 135.dp)
                                )
                                StatusFilterPill(
                                    title = "Dados de baja",
                                    count = writtenOffCount,
                                    icon = Icons.Default.Warning,
                                    iconColor = Color(0xFF94A3B8),
                                    isSelected = uiState.selectedStatusFilter == "WRITTEN_OFF",
                                    onClick = { viewModel.onStatusFilterChange("WRITTEN_OFF") },
                                    modifier = Modifier.widthIn(min = 120.dp)
                                )
                            }
                        }

                        // GRÁFICO 1: DISTRIBUCIÓN POR CATEGORÍA
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                DynamicCategoryDonutCard(
                                    assets = uiState.allAssets,
                                    moneyFmt = moneyFmt,
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary
                                )
                            }
                        }

                        // GRÁFICO 2: EVOLUCIÓN HISTÓRICA Y TENDENCIA
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                DynamicAssetValueEvolutionCard(
                                    totalAcquisition = uiState.totalAcquisitionCost,
                                    currentBookValue = uiState.totalBookValue,
                                    totalAccumulatedDepreciation = uiState.totalAccumulatedDepreciation,
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    primaryBlue = primaryBlue
                                )
                            }
                        }

                        // BOTÓN DE ACCESO DIRECTO AL MAESTRO DE INVENTARIO
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF0E2849),
                                border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedTopTab = 1 }
                                    .testTag("btn_ir_a_maestro_activos")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF163C6B)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = accentCyan, modifier = Modifier.size(20.dp))
                                        }
                                        Column {
                                            Text("Explorar Maestro de Inventario", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("Ver los $totalAssets equipos y filtros detallados", fontSize = 11.sp, color = textSecondary)
                                        }
                                    }
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = accentCyan)
                                }
                            }
                        }

                        // Muestra de todos los activos registrados
                        if (uiState.allAssets.isEmpty()) {
                            item {
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
                                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = accentCyan, modifier = Modifier.size(36.dp))
                                        Text("Sin activos registrados", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                        Text("Agrega tus vehículos, inmuebles o equipos pulsando '+ Nuevo'", fontSize = 12.sp, color = textSecondary)
                                    }
                                }
                            }
                        } else {
                            items(uiState.allAssets, key = { "resumen_${it.id}" }) { asset ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    ResponsiveDarkNavyAssetCard(
                                        asset = asset,
                                        moneyFmt = moneyFmt,
                                        onClick = { viewModel.selectAssetForDetail(asset) },
                                        onEdit = { viewModel.openEditAsset(asset) },
                                        onDelete = { assetToDeleteForConfirm = asset }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // 1: Pestaña "Activos" (El Maestro de Inventario)
                        item {
                            FixedAssetsInventoryTab(
                                assets = uiState.filteredAssets,
                                categories = uiState.categories,
                                searchQuery = uiState.searchQuery,
                                selectedCategory = uiState.selectedCategoryFilter,
                                selectedStatus = uiState.selectedStatusFilter,
                                onSearchChange = { viewModel.onSearchQueryChange(it) },
                                onCategoryChange = { viewModel.onCategoryFilterChange(it) },
                                onStatusChange = { viewModel.onStatusFilterChange(it) },
                                onResetFilters = { viewModel.resetFilters() },
                                onSelectAsset = { viewModel.selectAssetForDetail(it) },
                                onEditAsset = { viewModel.openEditAsset(it) },
                                onDeleteAsset = { assetToDeleteForConfirm = it },
                                onCreateAsset = { viewModel.openCreateAsset() }
                            )
                        }
                    }
                    2 -> {
                        // 2: Pestaña "Depreciación" (El Motor de Cálculo y Auditoría - NIC 16)
                        item {
                            FixedAssetsDepreciationTab(
                                patrimonioResumen = uiState.patrimonioResumen,
                                depreciationBatches = uiState.depreciationBatches,
                                pendingDepreciationList = uiState.pendingDepreciationList,
                                assets = uiState.allAssets,
                                isRunningDepreciation = uiState.isRunningDepreciation,
                                onRunDepreciationTrigger = {
                                    viewModel.executeDepreciationBatch()
                                    FixedAssetNotificationHelper.sendDepreciationReminder(context, uiState.allAssets.size, monthlyDepreciationTotal)
                                },
                                onRecalculateHistoricalTrigger = {
                                    viewModel.recalculateDepreciationForExistingAssets()
                                },
                                onOpenProjections = { viewModel.openProjections() }
                            )
                        }
                    }
                    3 -> {
                        // 3: Pestaña "Mantenimientos" (El Historial de Sucesos y Vida Física)
                        item {
                            FixedAssetsMaintenanceTab(
                                maintenanceOrders = uiState.maintenanceOrders,
                                assets = uiState.allAssets,
                                onCreateMaintenanceOrder = { assetId, title, amount, provider, notes, type, isCap, initStatus ->
                                    val asset = uiState.allAssets.find { it.id == assetId }
                                    viewModel.createMaintenanceOrder(assetId, title, amount, provider, notes, type, isCap, initStatus)
                                    FixedAssetNotificationHelper.sendMaintenanceAlert(context, asset?.name ?: "Activo Fijo", title)
                                },
                                onUpdateOrderStatus = { movId, assetId, newStatus, resNotes ->
                                    viewModel.updateMaintenanceOrderStatus(movId, assetId, newStatus, resNotes)
                                },
                                onSelectAssetForDetail = { viewModel.selectAssetForDetail(it) }
                            )
                        }
                    }
                    4 -> {
                        // 4: Pestaña "Auditoría" (Trazabilidad Total y Exportación Oficial NIC 16)
                        item {
                            FixedAssetsAuditTab(
                                auditLogs = uiState.auditLogs,
                                onExportPdf = { viewModel.exportAuditPdf(context) },
                                onExportExcel = { viewModel.exportAuditExcel(context) }
                            )
                        }
                    }
                }

                // Espacio para la barra flotante inferior
                item {
                    Spacer(modifier = Modifier.height(76.dp))
                }
            }
        }

        // 3. BARRA FLOTANTE INFERIOR
        Surface(
            color = Color(0xFF091424),
            border = BorderStroke(1.dp, Color(0xFF102644)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicador de activos
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.ViewList, null, tint = primaryBlue, modifier = Modifier.size(18.dp))
                    Text(
                        "${displayedAssets.size} activos",
                        fontSize = 12.5.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Acciones: Exportar, PDF y Crear
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0E223D),
                        border = BorderStroke(1.dp, Color(0xFF193B66)),
                        modifier = Modifier.clickable { viewModel.toggleExportSheet(true) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Text("Exportar", fontSize = 11.5.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0E223D),
                        border = BorderStroke(1.dp, Color(0xFF193B66)),
                        modifier = Modifier.clickable { viewModel.exportToPdf(context) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Description, null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Text("PDF", fontSize = 11.5.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = primaryBlue,
                        modifier = Modifier.clickable { viewModel.openCreateAsset() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text("Crear", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // iPhone Sheets Modales
    if (uiState.showRunDepreciationSheet) {
        RunDepreciationIPhoneSheet(
            pendingList = uiState.pendingDepreciationList,
            isLoading = uiState.isRunningDepreciation,
            onConfirmRun = { viewModel.runAutomaticDepreciation() },
            onDismiss = { viewModel.toggleRunDepreciationSheet(false) }
        )
    }

    if (uiState.showExportSheet) {
        FixedAssetExportIPhoneSheet(
            totalAssetsCount = uiState.filteredAssets.size,
            onExportPdf = { viewModel.exportToPdf(context) },
            onExportExcel = { viewModel.exportToExcel(context) },
            onDismiss = { viewModel.toggleExportSheet(false) }
        )
    }

    if (uiState.showFilterSheet) {
        FixedAssetFilterIPhoneSheet(
            selectedCategory = uiState.selectedCategoryFilter,
            selectedStatus = uiState.selectedStatusFilter,
            onCategoryChange = { viewModel.onCategoryFilterChange(it) },
            onStatusChange = { viewModel.onStatusFilterChange(it) },
            onResetFilters = { viewModel.resetFilters() },
            onDismiss = { viewModel.toggleFilterSheet(false) }
        )
    }

    if (uiState.showExportSuccessDialog && uiState.latestExportResult != null) {
        val res = uiState.latestExportResult!!
        RubroExportSuccessDialog(
            exportResult = res,
            mimeType = uiState.latestExportMimeType,
            title = "Reporte de Activos Fijos",
            context = context,
            onDismiss = { viewModel.dismissExportSuccessDialog() }
        )
    }

    if (assetToDeleteForConfirm != null) {
        val target = assetToDeleteForConfirm!!
        AlertDialog(
            onDismissRequest = { assetToDeleteForConfirm = null },
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(28.dp))
            },
            title = {
                Text(
                    text = "Eliminar Activo Definitivamente",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas eliminar permanentemente '${target.name}' (${target.assetCode})? Esta acción purgará el activo y su historial contable de la base de datos.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )
            },
            containerColor = Color(0xFF0F172A),
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAsset(target.id)
                        assetToDeleteForConfirm = null
                        android.widget.Toast.makeText(context, "Activo eliminado exitosamente", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Eliminar definitivamente", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { assetToDeleteForConfirm = null }) {
                    Text("Cancelar", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

/**
 * Tarjeta KPI Ejecutiva amplia que nunca corta cifras monetarias.
 */
@Composable
private fun ExecutiveKpiCard(
    title: String,
    amount: String,
    subtitle: String,
    icon: ImageVector,
    cardBg: Color,
    cardBorder: Color,
    accentCyan: Color,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0E2849)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentCyan, modifier = Modifier.size(17.dp))
            }
            Text(text = title, fontSize = 10.5.sp, color = textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = amount,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = subtitle, fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Gráfico Donut de Categorías 100% Dinámico:
 * Calcula las proporciones de cada categoría a partir de los activos reales en base de datos.
 */
@Composable
private fun DynamicCategoryDonutCard(
    assets: List<FixedAssetEntity>,
    moneyFmt: DecimalFormat,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val totalAcq = assets.sumOf { it.acquisitionValue }
    val categoryTotals = assets
        .groupBy { it.category }
        .mapValues { (_, list) -> list.sumOf { it.acquisitionValue } }
        .entries
        .sortedByDescending { it.value }

    val palette = listOf(
        Color(0xFF007AFF),
        Color(0xFF38BDF8),
        Color(0xFF6366F1),
        Color(0xFF8B5CF6),
        Color(0xFFEC4899),
        Color(0xFF10B981),
        Color(0xFFF59E0B)
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.AutoMode, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                Text("Valor de los activos por categoría", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            }

            if (assets.isEmpty() || totalAcq <= 0) {
                Text("Registra activos para visualizar la distribución contable.", fontSize = 11.5.sp, color = textSecondary)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Donut Chart con el total real al centro
                    Box(
                        modifier = Modifier.size(86.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(76.dp)) {
                            var startAngle = -90f
                            categoryTotals.forEachIndexed { idx, entry ->
                                val sweep = ((entry.value / totalAcq) * 360f).toFloat()
                                val color = palette[idx % palette.size]
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    style = Stroke(10.dp.toPx())
                                )
                                startAngle += sweep
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val shortTotal = if (totalAcq >= 1_000_000_000) {
                                String.format(Locale.US, "%.1fB", totalAcq / 1_000_000_000.0)
                            } else if (totalAcq >= 1_000_000) {
                                String.format(Locale.US, "%.1fM", totalAcq / 1_000_000.0)
                            } else {
                                String.format(Locale.US, "%.0fK", totalAcq / 1_000.0)
                            }
                            Text("$$shortTotal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Text("Total", fontSize = 8.sp, color = textSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Leyenda dinámica
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categoryTotals.take(5).forEachIndexed { idx, entry ->
                            val pct = ((entry.value / totalAcq) * 100).toInt()
                            val color = palette[idx % palette.size]
                            ChartCategoryLegend(entry.key, "$pct%", color)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Gráfico de Evolución del Valor 100% Dinámico:
 * Dibuja la curva real de amortización desde el valor histórico de adquisición al valor neto en libros.
 */
@Composable
private fun DynamicAssetValueEvolutionCard(
    totalAcquisition: Double,
    currentBookValue: Double,
    totalAccumulatedDepreciation: Double,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    primaryBlue: Color
) {
    val monthLabels = remember {
        val now = LocalDate.now()
        (5 downTo 0).map { offset ->
            now.minusMonths(offset.toLong())
                .month
                .getDisplayName(TextStyle.SHORT, Locale("es"))
                .replaceFirstChar { it.uppercase() }
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.TrendingUp, null, tint = primaryBlue, modifier = Modifier.size(16.dp))
                    Text("Evolución del valor de los activos", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                }
                Text("Tendencia", fontSize = 10.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold)
            }

            Canvas(modifier = Modifier.fillMaxWidth().height(90.dp)) {
                val w = size.width
                val h = size.height

                // Guías horizontales
                for (i in 0..3) {
                    val y = h * (i / 3f)
                    drawLine(Color(0xFF132B4F), Offset(0f, y), Offset(w, y), 1f)
                }

                // Cálculo dinámico de la curva:
                // Paso 0 inicia en valor de adquisición (h * 0.15f)
                // y decae suavemente hacia el valor en libros actual
                val depRatio = if (totalAcquisition > 0) {
                    (totalAccumulatedDepreciation / totalAcquisition).toFloat().coerceIn(0f, 0.75f)
                } else 0.25f

                val startY = h * 0.18f
                val endY = (startY + (h * 0.65f * depRatio)).coerceAtMost(h * 0.85f)

                val points = listOf(
                    Offset(w * 0.05f, startY),
                    Offset(w * 0.23f, startY + (endY - startY) * 0.20f),
                    Offset(w * 0.41f, startY + (endY - startY) * 0.45f),
                    Offset(w * 0.60f, startY + (endY - startY) * 0.70f),
                    Offset(w * 0.78f, startY + (endY - startY) * 0.90f),
                    Offset(w * 0.95f, endY)
                )

                // Relleno suave bajo la curva
                val fillPath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                    lineTo(points.last().x, h)
                    lineTo(points.first().x, h)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryBlue.copy(alpha = 0.25f), Color.Transparent),
                        startY = startY,
                        endY = h
                    )
                )

                // Línea principal
                val strokePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                drawPath(strokePath, primaryBlue, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))

                // Puntos circulares
                points.forEach { pt ->
                    drawCircle(primaryBlue, radius = 3.5.dp.toPx(), center = pt)
                    drawCircle(Color.White, radius = 1.5.dp.toPx(), center = pt)
                }
            }

            // Etiquetas de meses
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                monthLabels.forEach { m ->
                    Text(m, fontSize = 9.sp, color = textSecondary)
                }
            }
        }
    }
}

/**
 * Tarjeta de Activo Altamente Responsiva:
 * Diseñada en 2 niveles internos para que los códigos, marcas y cifras monetarias
 * NUNCA se corten ni se superpongan en ninguna pantalla.
 */
@Composable
internal fun ResponsiveDarkNavyAssetCard(
    asset: FixedAssetEntity,
    moneyFmt: DecimalFormat,
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val category = AssetCategory.fromString(asset.category)
    val state = AssetStateMachine.resolveState(asset.status)
    val bookValue = AssetFinancialCalculations.calculateCurrentBookValue(
        asset.acquisitionValue, asset.accumulatedDepreciation, asset.residualValue
    )

    val chipBg = state.color.copy(alpha = 0.16f)
    val chipBorder = state.color.copy(alpha = 0.45f)
    val chipDot = state.color
    val chipText = state.color

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0B172B),
        border = BorderStroke(1.dp, Color(0xFF132B4F)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .testTag("asset_item_${asset.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() },
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            // Nivel Superior: Miniatura + Nombre + Badges (Estado + Dashboard)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF060E1A))
                            .border(BorderStroke(1.dp, Color(0xFF163259)), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!asset.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = asset.imageUrl,
                                contentDescription = asset.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AssetHardwareThumbnail(category = category, modifier = Modifier.size(32.dp))
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = asset.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = asset.category, fontSize = 10.5.sp, color = Color(0xFF8EA5C4))
                            Text(text = "•", fontSize = 10.sp, color = Color(0xFF475569))
                            Text(text = asset.assetCode, fontSize = 10.5.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = chipBg,
                        border = BorderStroke(1.dp, chipBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(chipDot))
                            Text(text = state.shortBadgeLabel, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = chipText)
                        }
                    }

                    // Badge de Afectación al Dashboard / Patrimonio Total
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (asset.includeInNetWorth) Color(0xFF00C853).copy(alpha = 0.14f) else Color(0xFF1E293B),
                        border = BorderStroke(1.dp, if (asset.includeInNetWorth) Color(0xFF00E676).copy(alpha = 0.35f) else Color(0xFF475569).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = if (asset.includeInNetWorth) "✓ En Patrimonio" else "Excluido",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (asset.includeInNetWorth) Color(0xFF00E676) else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 6.5.dp, vertical = 2.5.dp)
                        )
                    }
                }
            }

            // Separador sutil
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF102644))
            )

            // Nivel Intermedio: Resumen Financiero Amplio y Responsivo (Nunca apretado)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "VALOR EN LIBROS", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8EA5C4))
                    Text(text = moneyFmt.format(bookValue), fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Black)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Compra: ${moneyFmt.format(asset.acquisitionValue)}",
                        fontSize = 10.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "Deprec.: -${moneyFmt.format(asset.accumulatedDepreciation)}",
                        fontSize = 10.5.sp,
                        color = Color(0xFFF87171),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            }

            // Nivel Inferior: Botones de Acción Visibles (Opciones que antes estaban ocultas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = Color(0xFF0E223D),
                    border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { onClick() }
                        .testTag("btn_detalle_${asset.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ver Detalle", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = Color(0xFF0E223D),
                    border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { onEdit() }
                        .testTag("btn_editar_${asset.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = Color(0xFF2D1515),
                    border = BorderStroke(1.dp, Color(0xFF7F1D1D)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { onDelete() }
                        .testTag("btn_eliminar_${asset.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Eliminar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFCA5A5))
                    }
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun StatusFilterPill(
    title: String,
    count: Int,
    icon: ImageVector,
    iconColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xFF0F2646) else Color(0xFF0B172B),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF007AFF) else Color(0xFF132B4F)),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(13.dp))
                Text(text = title, fontSize = 9.5.sp, color = Color(0xFF8EA5C4), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(text = "$count", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun FilterButtonPill(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0B172B),
        border = BorderStroke(1.dp, Color(0xFF132B4F)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
            Text(text = label, fontSize = 11.5.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FilterDropdownPill(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0B172B),
        border = BorderStroke(1.dp, Color(0xFF132B4F)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = label, fontSize = 11.5.sp, color = Color(0xFFCBD5E1))
            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF8EA5C4), modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun ChartCategoryLegend(name: String, percentage: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(text = name, fontSize = 9.sp, color = Color(0xFF8EA5C4), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(text = percentage, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
