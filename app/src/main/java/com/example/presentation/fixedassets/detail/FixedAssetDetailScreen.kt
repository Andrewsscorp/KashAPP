package com.example.presentation.fixedassets.detail

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LaptopChromebook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AssetMovementEntity
import com.example.data.local.entity.FixedAssetEntity
import com.example.domain.model.fixedassets.AssetCategory
import com.example.domain.model.fixedassets.AssetFinancialCalculations
import com.example.domain.model.fixedassets.AssetStatus
import com.example.presentation.fixedassets.components.AdvancedDepreciationOptionsIPhoneSheet
import com.example.presentation.fixedassets.components.AdvancedDepreciationSettings
import com.example.presentation.fixedassets.components.toAdvancedSettings
import com.example.presentation.fixedassets.components.RegisterMovementIPhoneSheet
import com.example.presentation.fixedassets.create.components.FullScreenImagePreviewDialog
import com.example.presentation.fixedassets.export.FixedAssetPdfExporter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Tune
import com.example.domain.model.fixedassets.AssetMovementType
import com.example.domain.model.fixedassets.DepreciationMethod
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.CircularProgressIndicator
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.fixedassets.statemachine.AssetState
import com.example.domain.model.fixedassets.statemachine.AssetStateMachine
import com.example.presentation.fixedassets.components.statemachine.AssetStateBadge
import com.example.presentation.fixedassets.components.statemachine.AssetStateExplainerSheet
import com.example.presentation.fixedassets.components.statemachine.ChangeAssetStateModal
import android.content.Intent
import java.text.DecimalFormat

@Composable
fun FixedAssetDetailScreen(
    asset: FixedAssetEntity,
    movements: List<AssetMovementEntity>,
    onEditAsset: (FixedAssetEntity) -> Unit,
    onUpdateAsset: (FixedAssetEntity) -> Unit = {},
    onChangeAssetState: ((FixedAssetEntity, String, String, AssetMovementEntity) -> Unit)? = null,
    onDeleteAsset: (String) -> Unit,
    onRecordMovement: (AssetMovementEntity) -> Unit,
    onRunIndividualDepreciation: (FixedAssetEntity) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    val moneyFmt = DecimalFormat("$ #,##0")
    var currentAsset by remember(asset) { mutableStateOf(asset) }
    androidx.compose.runtime.LaunchedEffect(asset) { currentAsset = asset }
    var movementInitialType by remember { mutableStateOf(AssetMovementType.IMPROVEMENT) }

    val category = AssetCategory.fromString(currentAsset.category)
    val status = AssetStatus.fromString(currentAsset.status)

    val bookValue = AssetFinancialCalculations.calculateCurrentBookValue(
        currentAsset.acquisitionValue, currentAsset.accumulatedDepreciation, currentAsset.residualValue
    )
    val remainingMonths = AssetFinancialCalculations.calculateRemainingUsefulLifeMonths(
        currentAsset.acquisitionDate, currentAsset.usefulLifeMonths
    )
    val remainingYears = (remainingMonths / 12).coerceAtLeast(1)

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Resumen", "Depreciación", "Mantenimientos", "Movimientos", "Documentos", "Más")

    var showAdvancedDepreciationSheet by remember { mutableStateOf(false) }
    var showRegisterMovementSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showFullScreenPhotoDialog by remember { mutableStateOf(false) }
    var showChangeStateModal by remember { mutableStateOf(false) }
    var showStateExplainerSheet by remember { mutableStateOf(false) }
    var stateToExplain by remember { mutableStateOf<AssetState?>(null) }
    var showFinalizeMaintenanceDialog by remember { mutableStateOf(false) }
    var showInitiateMaintenanceDialog by remember { mutableStateOf(false) }
    var maintenanceConceptInput by remember { mutableStateOf("Mantenimiento preventivo general") }
    var maintenanceResolutionNotes by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val savedFile = java.io.File(context.filesDir, "asset_photo_${currentAsset.id}_${System.currentTimeMillis()}.jpg")
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        savedFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    val updated = currentAsset.copy(imageUrl = savedFile.absolutePath, updatedAt = System.currentTimeMillis())
                    currentAsset = updated
                    onUpdateAsset(updated)
                    Toast.makeText(context, "Fotografía guardada exitosamente", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al guardar imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    val parsedImageModel = remember(currentAsset.imageUrl) {
        val raw = currentAsset.imageUrl?.trim()
        when {
            raw.isNullOrBlank() -> null
            raw.startsWith("content://") || raw.startsWith("file://") || raw.startsWith("http://") || raw.startsWith("https://") -> android.net.Uri.parse(raw)
            raw.startsWith("/") -> java.io.File(raw)
            else -> java.io.File(context.filesDir, raw).takeIf { it.exists() } ?: android.net.Uri.parse(raw)
        }
    }

    // Colors matching Image 2
    val bgDark = Color(0xFF07101E)
    val cardBg = Color(0xFF0B172B)
    val cardBorder = Color(0xFF132B4F)
    val primaryBlue = Color(0xFF007AFF)
    val accentCyan = Color(0xFF38BDF8)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF8EA5C4)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // TOP APP BAR
        Surface(
            color = Color(0xFF091424),
            border = BorderStroke(1.dp, Color(0xFF102644)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Detalle del activo",
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Información completa del activo fijo",
                            fontSize = 11.5.sp,
                            color = textSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onEditAsset(asset) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Más opciones",
                                tint = textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            modifier = Modifier.background(Color(0xFF0C1F38))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Configuración de depreciación", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Settings, null, tint = primaryBlue) },
                                onClick = {
                                    showOverflowMenu = false
                                    showAdvancedDepreciationSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Registrar mantenimiento", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Build, null, tint = Color(0xFFF59E0B)) },
                                onClick = {
                                    showOverflowMenu = false
                                    showRegisterMovementSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar activo", color = Color(0xFFEF4444)) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444)) },
                                onClick = {
                                    showOverflowMenu = false
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // BARRA DE ACCIONES VISIBLES (Opciones que estaban en los tres puntos ahora en botones visibles)
        Surface(
            color = Color(0xFF091424),
            border = BorderStroke(1.dp, Color(0xFF102644)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            val isDisposed = currentAsset.status == "DADO_DE_BAJA"
            val isInMaintenance = currentAsset.status == "EN_MANTENIMIENTO"

            if (isDisposed) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF261014),
                    border = BorderStroke(1.dp, Color(0xFF7F1D1D)),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(14.dp))
                        Text("Activo Retirado / Solo Lectura NIIF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFCA5A5))
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0E223D),
                    border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onEditAsset(currentAsset) }
                        .testTag("detail_btn_editar")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                        Text("Editar", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0E223D),
                    border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showAdvancedDepreciationSheet = true }
                        .testTag("detail_btn_config_depreciacion")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = primaryBlue, modifier = Modifier.size(14.dp))
                        Text("Config. Depreciación", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }

                // Mutating Maintenance Button
                if (isInMaintenance) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF064E3B),
                        border = BorderStroke(1.dp, Color(0xFF059669)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showFinalizeMaintenanceDialog = true }
                            .testTag("detail_btn_finalizar_mantenimiento")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(14.dp))
                            Text("Finalizar Mantenimiento", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0E223D),
                        border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showInitiateMaintenanceDialog = true }
                            .testTag("detail_btn_mantenimiento")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                            Text("Mantenimiento", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }

                // State Transition Button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0E223D),
                    border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showChangeStateModal = true }
                        .testTag("detail_btn_cambiar_estado")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                        Text("Cambiar Estado", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF2D1515),
                    border = BorderStroke(1.dp, Color(0xFF7F1D1D)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showDeleteConfirmDialog = true }
                        .testTag("detail_btn_eliminar")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(14.dp))
                        Text("Baja / Retiro", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFCA5A5))
                    }
                }
            }
        }
    }

    // SCROLLABLE CONTENT
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Read-only audit banner when asset is disposed
        if (currentAsset.status == "DADO_DE_BAJA") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF261014),
                border = BorderStroke(1.dp, Color(0xFF7F1D1D)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(26.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ACTIVO DADO DE BAJA • MODO AUDITORÍA",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCA5A5)
                        )
                        Text(
                            text = "Este activo ha finalizado su ciclo contable bajo NIIF. No admite nuevos cálculos de depreciación ni adición de movimientos.",
                            fontSize = 11.sp,
                            color = Color(0xFFF87171)
                        )
                    }
                }
            }
        }

        // HERO ASSET CARD (Totalmente responsivo y adaptativo)
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Fila Superior Responsiva: Código patrimonial + Categoría y Estado interactivo
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    if (maxWidth < 360.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0E2849),
                                    border = BorderStroke(1.dp, Color(0xFF1E3A8A))
                                ) {
                                    Text(
                                        text = currentAsset.assetCode,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentCyan,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0C1F38)
                                ) {
                                    Text(
                                        text = currentAsset.category,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textSecondary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            AssetStateBadge(
                                asset = currentAsset,
                                onHelpClick = {
                                    stateToExplain = AssetStateMachine.resolveState(currentAsset.status)
                                    showStateExplainerSheet = true
                                },
                                onClick = {
                                    showChangeStateModal = true
                                }
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0E2849),
                                    border = BorderStroke(1.dp, Color(0xFF1E3A8A))
                                ) {
                                    Text(
                                        text = currentAsset.assetCode,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentCyan,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0C1F38)
                                ) {
                                    Text(
                                        text = currentAsset.category,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textSecondary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            AssetStateBadge(
                                asset = currentAsset,
                                onHelpClick = {
                                    stateToExplain = AssetStateMachine.resolveState(currentAsset.status)
                                    showStateExplainerSheet = true
                                },
                                onClick = {
                                    showChangeStateModal = true
                                }
                            )
                        }
                    }
                }

                // Sección Visual: Banner fotográfico responsivo con relación 16:9
                if (parsedImageModel != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF060E1A))
                                .border(BorderStroke(1.dp, Color(0xFF163259)), RoundedCornerShape(16.dp))
                                .clickable { showFullScreenPhotoDialog = true }
                        ) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(parsedImageModel)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = currentAsset.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                loading = {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = accentCyan, strokeWidth = 2.dp)
                                    }
                                },
                                error = {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        AssetHardwareThumbnail(category = category, modifier = Modifier.size(52.dp))
                                    }
                                }
                            )

                            // Overlay sutil para legibilidad
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color(0x66000000), Color(0xCC07101E)),
                                            startY = 70f
                                        )
                                    )
                            )

                            // Botón para ampliar en la esquina inferior derecha
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xCC000000),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = "Ampliar foto", tint = Color.White, modifier = Modifier.size(14.dp))
                                    Text("Ampliar", fontSize = 10.5.sp, color = Color.White)
                                }
                            }

                            // Botón para cambiar foto en la esquina superior izquierda
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xCC000000),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = accentCyan, modifier = Modifier.size(12.dp))
                                    Text("Cambiar foto", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Información descriptiva bajo la foto
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = currentAsset.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )

                            Text(
                                text = "${currentAsset.brand ?: "Equipo"}  •  ${currentAsset.model ?: "Estándar"}",
                                fontSize = 12.5.sp,
                                color = textSecondary
                            )

                            if (!currentAsset.location.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = currentAsset.location!!,
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF08223F),
                                border = BorderStroke(1.dp, Color(0xFF0E4A7A)),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "En libros: ${moneyFmt.format(bookValue)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Layout adaptativo cuando no hay foto subida
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val thumbSize = if (maxWidth < 360.dp) 64.dp else 78.dp
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(thumbSize)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF060E1A))
                                    .border(BorderStroke(1.dp, Color(0xFF163259)), RoundedCornerShape(16.dp))
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                AssetHardwareThumbnail(category = category, modifier = Modifier.size(44.dp))
                                Text(
                                    text = "+ Foto",
                                    fontSize = 9.5.sp,
                                    color = accentCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = currentAsset.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = "${currentAsset.brand ?: "Equipo"}  •  ${currentAsset.model ?: "Estándar"}",
                                fontSize = 12.sp,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (!currentAsset.location.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = currentAsset.location!!,
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF08223F),
                                border = BorderStroke(1.dp, Color(0xFF0E4A7A)),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "En libros: ${moneyFmt.format(bookValue)}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

            // 4 METRICS CARDS ROW (Scroll horizontal para cifras monetarias legibles)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricMiniCard(
                    title = "Valor de adquisición",
                    value = moneyFmt.format(asset.acquisitionValue),
                    icon = Icons.Default.Inventory2,
                    modifier = Modifier.width(160.dp)
                )
                MetricMiniCard(
                    title = "Depreciación acumulada",
                    value = moneyFmt.format(asset.accumulatedDepreciation),
                    icon = Icons.Default.TrendingDown,
                    modifier = Modifier.width(160.dp)
                )
                MetricMiniCard(
                    title = "Valor en libros",
                    value = moneyFmt.format(bookValue),
                    icon = Icons.Default.Description,
                    modifier = Modifier.width(160.dp)
                )
                MetricMiniCard(
                    title = "Vida útil restante",
                    value = "$remainingYears años",
                    icon = Icons.Default.AutoMode,
                    modifier = Modifier.width(160.dp)
                )
            }

            // HORIZONTAL TABS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { index, tabName ->
                    val isSelected = selectedTabIndex == index
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) primaryBlue else Color(0xFF0B172B),
                        border = BorderStroke(1.dp, if (isSelected) primaryBlue else cardBorder),
                        modifier = Modifier.clickable { selectedTabIndex = index }
                    ) {
                        Text(
                            text = tabName,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else textSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // TAB 0: RESUMEN CONTENT
            if (selectedTabIndex == 0) {
                // Card "Resumen general" / "Estado actual del activo"
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0E2849)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = accentCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Resumen general",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "Estado actual del activo",
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                }
                            }

                            // Button "Ver foto" / "Adjuntar foto"
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0E223D),
                                border = BorderStroke(1.dp, Color(0xFF1A3E6D)),
                                modifier = Modifier.clickable {
                                    if (parsedImageModel != null) {
                                        showFullScreenPhotoDialog = true
                                    } else {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = accentCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (parsedImageModel != null) "Ver foto" else "+ Adjuntar foto",
                                        fontSize = 11.5.sp,
                                        color = accentCyan,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Fotografía de Inventario (Responsivo, destacado, no recortado)
                        if (parsedImageModel != null) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF040A14),
                                border = BorderStroke(1.dp, Color(0xFF163259)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 190.dp, max = 260.dp)
                                            .background(Color(0xFF020712))
                                            .clickable { showFullScreenPhotoDialog = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        SubcomposeAsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(parsedImageModel)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = currentAsset.name,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize().padding(6.dp),
                                            loading = {
                                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = accentCyan, strokeWidth = 2.dp)
                                                }
                                            },
                                            error = {
                                                Column(
                                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(Icons.Default.BrokenImage, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(32.dp))
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text("No se pudo cargar la imagen guardada", fontSize = 11.sp, color = textSecondary)
                                                    Text("Toca 'Cambiar foto' para actualizar", fontSize = 11.sp, color = accentCyan, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        )

                                        // Badge Superior
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xCC000000),
                                            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                                Text("Evidencia Verificada", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                            }
                                        }

                                        // Badge Inferior Ampliar
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xCC000000),
                                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.ZoomIn, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                                Text("Tocar para ampliar", fontSize = 10.sp, color = Color.White)
                                            }
                                        }
                                    }

                                    // Action bar debajo de la foto
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF071222))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Fotografía de inventario patrimonial",
                                            fontSize = 11.sp,
                                            color = textSecondary
                                        )
                                        TextButton(
                                            onClick = {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = accentCyan, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Cambiar foto", fontSize = 11.sp, color = accentCyan, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Empty state card with direct upload button
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF071222),
                                border = BorderStroke(1.dp, Color(0xFF163259)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    AssetHardwareThumbnail(category = category, modifier = Modifier.size(54.dp))
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Sin fotografía adjunta", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Sube una foto del activo para respaldo patrimonial y auditorías.", fontSize = 11.sp, color = textSecondary)
                                    }
                                    Button(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Foto", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }

                        // Technical specs grid (Full width, responsive, unclipped)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SpecItem("Nombre del Activo", currentAsset.name)
                            SpecItem("Código Patrimonial", currentAsset.assetCode, badgeText = currentAsset.assetCode)
                            SpecItem("Categoría Contable", currentAsset.category)
                            SpecItem("Marca", currentAsset.brand?.ifBlank { null } ?: "No especificada")
                            SpecItem("Modelo", currentAsset.model?.ifBlank { null } ?: "No especificado")
                            SpecItem("Número de Serie", currentAsset.serialNumber?.ifBlank { null } ?: "Sin serial")
                            SpecItem("Proveedor Emisor", currentAsset.supplier?.ifBlank { null } ?: "No especificado")
                            SpecItem("Ubicación Registrada", currentAsset.location?.ifBlank { null } ?: "Sede Principal")
                            SpecItem("Fecha de Adquisición", currentAsset.acquisitionDate)
                            SpecItem("Moneda Oficial", currentAsset.currency)
                            SpecItem("Valor de Adquisición", "${moneyFmt.format(currentAsset.acquisitionValue)} ${currentAsset.currency}", isHighlight = true)
                            SpecItem("Valor Residual (Salvamento)", "${moneyFmt.format(currentAsset.residualValue)} ${currentAsset.currency}")
                            SpecItem("Valor Neto en Libros", "${moneyFmt.format(bookValue)} ${currentAsset.currency}", isSuccess = true)
                        }
                    }
                }

                // GRÁFICO 1: EVOLUCIÓN DEL VALOR (Ancho completo responsivo)
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.TrendingUp, null, tint = primaryBlue, modifier = Modifier.size(16.dp))
                            Column {
                                Text("Evolución del valor", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text("Valor en libros vs. Amortización acumulada", fontSize = 10.5.sp, color = textSecondary)
                            }
                        }

                        // Canvas line chart dinámico
                        ValueEvolutionLineChart(
                            acquisitionCost = asset.acquisitionValue,
                            currentBookValue = bookValue,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                        )

                        // Chart Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(primaryBlue))
                                Text("Valor en libros (${moneyFmt.format(bookValue)})", fontSize = 10.sp, color = textSecondary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF38BDF8)))
                                Text("Depreciación (${moneyFmt.format(asset.accumulatedDepreciation)})", fontSize = 10.sp, color = textSecondary)
                            }
                        }
                    }
                }

                // GRÁFICO 2: DISTRIBUCIÓN DEL VALOR 100% DINÁMICO (Valor en libros vs Depreciación)
                val acqVal = asset.acquisitionValue.coerceAtLeast(1.0)
                val depRatioPct = ((asset.accumulatedDepreciation / acqVal) * 100).toInt().coerceIn(0, 100)
                val bookRatioPct = (100 - depRatioPct).coerceIn(0, 100)
                val bookSweepAngle = (bookRatioPct / 100f) * 360f

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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.AutoMode, null, tint = accentCyan, modifier = Modifier.size(16.dp))
                            Column {
                                Text("Distribución del valor del activo", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text("Porcentaje contable NIIF actual", fontSize = 10.5.sp, color = textSecondary)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Donut chart dinámico
                            Box(
                                modifier = Modifier.size(90.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(80.dp)) {
                                    drawCircle(
                                        color = Color(0xFF0F2646),
                                        style = Stroke(width = 11.dp.toPx())
                                    )
                                    // Arco de valor en libros
                                    drawArc(
                                        color = primaryBlue,
                                        startAngle = -90f,
                                        sweepAngle = bookSweepAngle,
                                        useCenter = false,
                                        style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                    // Arco de depreciación acumulada
                                    if (depRatioPct > 0) {
                                        drawArc(
                                            color = Color(0xFF38BDF8),
                                            startAngle = -90f + bookSweepAngle,
                                            sweepAngle = 360f - bookSweepAngle,
                                            useCenter = false,
                                            style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$bookRatioPct%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                    Text("Libros", fontSize = 8.sp, color = textSecondary)
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Leyenda con porcentajes reales del activo
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LegendRow("Valor en libros", "$bookRatioPct% (${moneyFmt.format(bookValue)})", primaryBlue)
                                LegendRow("Dep. acumulada", "$depRatioPct% (${moneyFmt.format(asset.accumulatedDepreciation)})", Color(0xFF38BDF8))
                                LegendRow("Base adquisición", "100% (${moneyFmt.format(asset.acquisitionValue)})", Color(0xFF94A3B8))
                            }
                        }
                    }
                }

                // Card "Últimos movimientos" (Real desde la base de datos o síntesis fiel del activo)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0E2849)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoMode,
                                        contentDescription = null,
                                        tint = accentCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Últimos movimientos",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "Historial de transacciones y cambios",
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.clickable { selectedTabIndex = 3 },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ver todos",
                                    fontSize = 12.sp,
                                    color = primaryBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = primaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Movimientos reales desde la base de datos
                        val displayMovements = if (movements.isNotEmpty()) {
                            movements
                        } else {
                            listOfNotNull(
                                AssetMovementEntity(
                                    id = "mov_init_${currentAsset.id}",
                                    assetId = currentAsset.id,
                                    userId = currentAsset.userId,
                                    type = "ACQUISITION",
                                    title = "Compra y Capitalización Inicial",
                                    date = currentAsset.acquisitionDate,
                                    amount = currentAsset.acquisitionValue,
                                    notes = "Adquisición y capitalización inicial en libros contables NIIF",
                                    financialImpact = "Capitalizable (+ Base depreciable)"
                                ),
                                if (currentAsset.accumulatedDepreciation > 0) {
                                    AssetMovementEntity(
                                        id = "mov_dep_${currentAsset.id}",
                                        assetId = currentAsset.id,
                                        userId = currentAsset.userId,
                                        type = "DEPRECIATION",
                                        title = "Depreciación Acumulada Contable",
                                        date = currentAsset.lastDepreciationDate ?: currentAsset.acquisitionDate,
                                        amount = currentAsset.accumulatedDepreciation,
                                        notes = "Amortización contable histórica bajo NIIF / NIC 16",
                                        financialImpact = "Gasto del Periodo (P&L)"
                                    )
                                } else null,
                                if (currentAsset.status == "MAINTENANCE" || currentAsset.status == "EN_MANTENIMIENTO") {
                                    AssetMovementEntity(
                                        id = "mov_maint_${currentAsset.id}",
                                        assetId = currentAsset.id,
                                        userId = currentAsset.userId,
                                        type = "MAINTENANCE",
                                        title = "Ingreso a Taller / Mantenimiento",
                                        date = java.time.LocalDate.now().toString(),
                                        amount = 0.0,
                                        notes = "Activo en proceso de mantenimiento correctivo/preventivo.",
                                        financialImpact = "Gasto Operacional (100% P&L)"
                                    )
                                } else null
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            displayMovements.take(3).forEach { mov ->
                                MovementRowItem(mov = mov, moneyFmt = moneyFmt)
                            }
                        }
                    }
                }
            } else if (selectedTabIndex == 1) {
                // Tab Depreciación
                DepreciationTabContent(
                    asset = currentAsset,
                    moneyFmt = moneyFmt,
                    onOpenAdvancedSettings = { showAdvancedDepreciationSheet = true },
                    onRegisterImpairment = {
                        movementInitialType = AssetMovementType.IMPAIRMENT
                        showRegisterMovementSheet = true
                    },
                    onRegisterRevaluation = {
                        movementInitialType = AssetMovementType.REVALUATION
                        showRegisterMovementSheet = true
                    },
                    onRunDepreciation = { onRunIndividualDepreciation(currentAsset) }
                )
            } else if (selectedTabIndex == 2) {
                // Tab Mantenimientos
                MaintenanceTabContent(
                    movements = movements.filter { it.type == "MAINTENANCE" },
                    moneyFmt = moneyFmt,
                    onAddMaintenance = { showRegisterMovementSheet = true }
                )
            } else if (selectedTabIndex == 3) {
                // Tab Movimientos
                MovementsTabContent(
                    movements = movements,
                    asset = currentAsset,
                    moneyFmt = moneyFmt,
                    onAddMovement = { showRegisterMovementSheet = true }
                )
            } else if (selectedTabIndex == 4) {
                // Tab Documentos
                DocumentsTabContent(
                    asset = currentAsset,
                    moneyFmt = moneyFmt,
                    onOpenPhoto = { showFullScreenPhotoDialog = true },
                    onAttachPhoto = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onExportPdf = {
                        val result = FixedAssetPdfExporter.exportToPdf(
                            context = context,
                            assets = listOf(currentAsset),
                            filterDesc = "Ficha Técnica: ${currentAsset.name} (${currentAsset.assetCode})"
                        )
                        Toast.makeText(context, "Reporte PDF generado exitosamente", Toast.LENGTH_SHORT).show()
                        result.fileUri?.let { uri ->
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Ver reporte PDF"))
                            } catch (_: Exception) {}
                        }
                    },
                    onEditAsset = { onEditAsset(currentAsset) }
                )
            } else {
                // Tab Más
                MoreOptionsTabContent(
                    asset = currentAsset,
                    onOpenAdvancedSettings = { showAdvancedDepreciationSheet = true },
                    onRegisterMaintenance = { showRegisterMovementSheet = true },
                    onExportPdf = {
                        val result = FixedAssetPdfExporter.exportToPdf(
                            context = context,
                            assets = listOf(currentAsset),
                            filterDesc = "Ficha Técnica: ${currentAsset.name} (${currentAsset.assetCode})"
                        )
                        Toast.makeText(context, "Reporte PDF generado exitosamente", Toast.LENGTH_SHORT).show()
                        result.fileUri?.let { uri ->
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Ver reporte PDF"))
                            } catch (_: Exception) {}
                        }
                    },
                    onEditAsset = { onEditAsset(currentAsset) },
                    onDeleteAsset = { showDeleteConfirmDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // BOTTOM ACTION BAR (Matches Image 2)
        Surface(
            color = Color(0xFF091424),
            border = BorderStroke(1.dp, Color(0xFF102644)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Generar reporte
                OutlinedButton(
                    onClick = {
                        val result = FixedAssetPdfExporter.exportToPdf(
                            context = context,
                            assets = listOf(asset),
                            filterDesc = "Ficha Técnica: ${asset.name} (${asset.assetCode})"
                        )
                        Toast.makeText(context, "Reporte PDF generado exitosamente", Toast.LENGTH_SHORT).show()
                        result.fileUri?.let { uri ->
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Ver reporte PDF"))
                            } catch (_: Exception) {}
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E3A63)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.weight(1.1f).height(46.dp)
                ) {
                    Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Generar reporte", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Ver documentos
                OutlinedButton(
                    onClick = {
                        selectedTabIndex = 4 // Documentos
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E3A63)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.weight(1.1f).height(46.dp)
                ) {
                    Icon(Icons.Default.Description, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver documentos", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Guardar / Editar
                Button(
                    onClick = {
                        onEditAsset(asset)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Guardar", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    // MODAL 1: ADVANCED DEPRECIATION OPTIONS (MATCHING IMAGE 1)
    if (showAdvancedDepreciationSheet) {
        AdvancedDepreciationOptionsIPhoneSheet(
            asset = currentAsset,
            initialSettings = currentAsset.toAdvancedSettings(),
            onSaveSettings = { settings ->
                val newMethodCode = when (settings.method) {
                    "Suma de dígitos" -> "DIGITS_SUM"
                    "Saldos decrecientes" -> "DECLINING_BALANCE"
                    else -> "STRAIGHT_LINE"
                }
                val updated = currentAsset.copy(
                    depreciationMethod = newMethodCode,
                    depreciableBase = settings.depreciableBase,
                    startConvention = settings.startConvention,
                    frequency = settings.frequency,
                    rounding = settings.rounding,
                    impairmentTracking = settings.impairmentTracking,
                    revaluationEnabled = settings.revaluationEnabled,
                    allowLifeChange = settings.allowLifeChange,
                    allowResidualChange = settings.allowResidualChange,
                    allowMethodChange = settings.allowMethodChange,
                    updatedAt = System.currentTimeMillis()
                )
                currentAsset = updated
                onUpdateAsset(updated)
                Toast.makeText(context, "Configuración avanzada aplicada a ${updated.name}", Toast.LENGTH_SHORT).show()
                showAdvancedDepreciationSheet = false
            },
            onDismiss = { showAdvancedDepreciationSheet = false }
        )
    }

    // MODAL 2: REGISTER MOVEMENT
    if (showRegisterMovementSheet) {
        RegisterMovementIPhoneSheet(
            asset = currentAsset,
            initialType = movementInitialType,
            onSaveMovement = { movement ->
                onRecordMovement(movement)
                showRegisterMovementSheet = false
            },
            onDismiss = { showRegisterMovementSheet = false }
        )
    }

    // STATE MACHINE TRANSITION MODAL
    if (showChangeStateModal) {
        ChangeAssetStateModal(
            asset = currentAsset,
            onStateTransitionConfirmed = { targetStateCode, reasonNotes ->
                val updated = currentAsset.copy(
                    status = targetStateCode,
                    stateNotes = reasonNotes,
                    updatedAt = System.currentTimeMillis()
                )
                currentAsset = updated

                val targetState = AssetStateMachine.resolveState(targetStateCode)
                val movType = when (targetStateCode) {
                    "EN_MANTENIMIENTO" -> AssetMovementType.MAINTENANCE.name
                    "DADO_DE_BAJA" -> AssetMovementType.DISPOSAL.name
                    "MANTENIDO_PARA_LA_VENTA" -> AssetMovementType.SALE.name
                    else -> AssetMovementType.STATUS_CHANGE.name
                }
                val impact = when (targetStateCode) {
                    "EN_USO" -> "Operación Activa • Depreciación Normal NIIF"
                    "EN_MANTENIMIENTO" -> "Gasto Operacional • Salvaguardas NIIF"
                    "DISPONIBLE" -> "En Bodega • Gasto Administrativo"
                    "DADO_DE_BAJA" -> "Retiro Definitivo • Pérdida por Retiro NIIF"
                    "EN_CONSTRUCCION" -> "Obra en Curso • Capitalización Costos"
                    "EN_TRANSITO" -> "En Tránsito • Depreciación Congelada"
                    "MANTENIDO_PARA_LA_VENTA" -> "NIIF 5 • Activo Corriente"
                    "EN_COMODATO" -> "En Préstamo / Tercero • Deprecia en Libros"
                    "EN_SINIESTRO" -> "Investigación Seguros • En Limbo"
                    else -> "Transición de Estado NIIF"
                }

                val transitionMov = AssetMovementEntity(
                    id = "mov_${java.util.UUID.randomUUID()}",
                    assetId = currentAsset.id,
                    userId = currentAsset.userId,
                    type = movType,
                    title = "Transición: ${targetState.shortBadgeLabel} [${targetState.code}]",
                    date = java.time.LocalDate.now().toString(),
                    amount = 0.0,
                    notes = "Transición de estado a ${targetState.label}. $reasonNotes",
                    disposalReason = if (targetStateCode == "DADO_DE_BAJA") reasonNotes else null,
                    financialImpact = impact
                )

                if (onChangeAssetState != null) {
                    onChangeAssetState(currentAsset, targetStateCode, reasonNotes, transitionMov)
                } else {
                    onUpdateAsset(updated)
                    onRecordMovement(transitionMov)
                }

                showChangeStateModal = false
                Toast.makeText(context, "Estado actualizado a ${targetState.shortBadgeLabel}", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showChangeStateModal = false },
            onInspectStateHelp = { state ->
                stateToExplain = state
                showStateExplainerSheet = true
            }
        )
    }

    // STATE EXPLAINER BOTTOM SHEET
    if (showStateExplainerSheet && stateToExplain != null) {
        AssetStateExplainerSheet(
            state = stateToExplain!!,
            onDismiss = { showStateExplainerSheet = false }
        )
    }

    // FINALIZE MAINTENANCE DIALOG
    if (showFinalizeMaintenanceDialog) {
        AlertDialog(
            onDismissRequest = { showFinalizeMaintenanceDialog = false },
            containerColor = Color(0xFF0C1F38),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF34D399), modifier = Modifier.size(24.dp))
                    Text("Finalizar Mantenimiento", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "El activo ${currentAsset.name} regresará a estado 'EN_USO' (Operativo). Se desbloquearán las opciones de mejoras y revalúos bajo NIIF.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.5.sp
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = maintenanceResolutionNotes,
                        onValueChange = { maintenanceResolutionNotes = it },
                        label = { Text("Notas de conclusión / Trabajo realizado") },
                        placeholder = { Text("Ej: Mantenimiento concluido con éxito, cambio de repuestos.") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF34D399),
                            unfocusedBorderColor = Color(0xFF1E3A8A)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = currentAsset.copy(
                            status = "EN_USO",
                            updatedAt = System.currentTimeMillis()
                        )
                        currentAsset = updated
                        onUpdateAsset(updated)

                        val logMov = AssetMovementEntity(
                            id = "mov_${java.util.UUID.randomUUID()}",
                            assetId = currentAsset.id,
                            userId = currentAsset.userId,
                            type = AssetMovementType.MAINTENANCE.name,
                            title = "Conclusión de mantenimiento",
                            date = java.time.LocalDate.now().toString(),
                            amount = 0.0,
                            notes = "Mantenimiento finalizado exitosamente. Retorna a EN_USO. ${maintenanceResolutionNotes.trim()}",
                            financialImpact = "Reincorporado a Operación Activa"
                        )
                        onRecordMovement(logMov)

                        showFinalizeMaintenanceDialog = false
                        Toast.makeText(context, "Mantenimiento finalizado. Activo en estado EN_USO.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Text("Reincorporar a Uso", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinalizeMaintenanceDialog = false }) {
                    Text("Cancelar", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // INITIATE MAINTENANCE DIALOG
    if (showInitiateMaintenanceDialog) {
        AlertDialog(
            onDismissRequest = { showInitiateMaintenanceDialog = false },
            containerColor = Color(0xFF0C1F38),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Build, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(24.dp))
                    Text("Gestión de Mantenimiento", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Selecciona la acción para ${currentAsset.name}:",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0A182E),
                        border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showInitiateMaintenanceDialog = false
                                movementInitialType = AssetMovementType.MAINTENANCE
                                showRegisterMovementSheet = true
                            }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Build, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                            Column {
                                Text("Registrar Gasto de Mantenimiento", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Registra facturas de talleres o repuestos (Gasto contable directo).", fontSize = 11.sp, color = textSecondary)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF26190B),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val updated = currentAsset.copy(status = "EN_MANTENIMIENTO", updatedAt = System.currentTimeMillis())
                                currentAsset = updated
                                onUpdateAsset(updated)

                                val logMov = AssetMovementEntity(
                                    id = "mov_${java.util.UUID.randomUUID()}",
                                    assetId = currentAsset.id,
                                    userId = currentAsset.userId,
                                    type = AssetMovementType.MAINTENANCE.name,
                                    title = "Ingreso a taller / mantenimiento",
                                    date = java.time.LocalDate.now().toString(),
                                    amount = 0.0,
                                    notes = "Transición de estado: Activo ingresado a mantenimiento preventivo/correctivo.",
                                    financialImpact = "Gasto Operacional (100% P&L)"
                                )
                                onRecordMovement(logMov)

                                showInitiateMaintenanceDialog = false
                                Toast.makeText(context, "Activo pasado a estado EN_MANTENIMIENTO", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.SwapHoriz, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(22.dp))
                            Column {
                                Text("Enviar a Taller (Cambiar a EN_MANTENIMIENTO)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDE68A))
                                Text("Muta el estado a 'En Mantenimiento' y activa salvaguardas NIIF.", fontSize = 11.sp, color = Color(0xFFFCD34D))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showInitiateMaintenanceDialog = false }) {
                    Text("Cerrar", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // DELETE / DISPOSAL CONFIRMATION DIALOG
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Delete, null, tint = Color(0xFFF87171), modifier = Modifier.size(24.dp))
                    Text("Baja o Retiro del Activo", color = Color.White, fontSize = 16.5.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "¿Cómo deseas proceder con ${currentAsset.name} (${currentAsset.assetCode})?",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    )
                    Text(
                        "• Dar de Baja (Recomendado NIIF): Conserva el historial para auditoría contable, reconoce la pérdida por retiro y pasa el activo a modo solo lectura.\n• Eliminar definitivamente: Borra físicamente todo registro de la base de datos.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp
                    )
                }
            },
            containerColor = Color(0xFF0F172A),
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val updated = currentAsset.copy(
                                status = "DADO_DE_BAJA",
                                updatedAt = System.currentTimeMillis()
                            )
                            currentAsset = updated
                            onUpdateAsset(updated)

                            val disposalMov = AssetMovementEntity(
                                id = "mov_${java.util.UUID.randomUUID()}",
                                assetId = currentAsset.id,
                                userId = currentAsset.userId,
                                type = AssetMovementType.DISPOSAL.name,
                                title = "Baja contable por retiro patrimonial",
                                date = java.time.LocalDate.now().toString(),
                                amount = 0.0,
                                notes = "Activo dado de baja por acción de usuario bajo normas NIIF.",
                                disposalReason = "Retiro patrimonial",
                                financialImpact = "Pérdida por Retiro de Activos (NIIF)"
                            )
                            onRecordMovement(disposalMov)
                            showDeleteConfirmDialog = false
                            Toast.makeText(context, "Activo dado de baja exitosamente (NIIF)", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dar de Baja (NIIF)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            onDeleteAsset(currentAsset.id)
                            showDeleteConfirmDialog = false
                            Toast.makeText(context, "Activo eliminado definitivamente del sistema", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        border = BorderStroke(1.dp, Color(0xFF7F1D1D)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Eliminar Definitivamente", fontSize = 12.5.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // FULL SCREEN PHOTO PREVIEW DIALOG
    if (showFullScreenPhotoDialog && parsedImageModel != null) {
        Dialog(
            onDismissRequest = { showFullScreenPhotoDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF0030712))
                    .clickable { showFullScreenPhotoDialog = false }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(parsedImageModel)
                        .crossfade(true)
                        .build(),
                    contentDescription = currentAsset.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    loading = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accentCyan, strokeWidth = 2.dp)
                        }
                    }
                )
                IconButton(
                    onClick = { showFullScreenPhotoDialog = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color(0x99000000), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }
        }
    }
}
}

@Composable
private fun MetricMiniCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0B172B),
        border = BorderStroke(1.dp, Color(0xFF132B4F)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0E2849)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = title,
                fontSize = 9.5.sp,
                color = Color(0xFF8EA5C4),
                maxLines = 1
            )
            Text(
                text = value,
                fontSize = if (value.length > 14) 11.sp else 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                softWrap = true
            )
        }
    }
}

@Composable
private fun SpecItem(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    isSuccess: Boolean = false,
    badgeText: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF8EA5C4),
            modifier = Modifier.weight(0.42f, fill = false)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            modifier = Modifier.weight(0.58f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            if (badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0E2849),
                    border = BorderStroke(1.dp, Color(0xFF163259))
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = value,
                fontSize = 12.5.sp,
                fontWeight = if (isHighlight || isSuccess) FontWeight.Bold else FontWeight.SemiBold,
                color = when {
                    isHighlight -> Color(0xFF38BDF8)
                    isSuccess -> Color(0xFF34D399)
                    else -> Color.White
                },
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                softWrap = true
            )
        }
    }
}

@Composable
private fun MovementRowItem(
    mov: AssetMovementEntity,
    moneyFmt: DecimalFormat
) {
    val movType = AssetMovementType.fromString(mov.type)
    val (badgeIcon, badgeColor, badgeBg) = when (movType) {
        AssetMovementType.ACQUISITION -> Triple(Icons.Default.Add, Color(0xFF10B981), Color(0xFF064E3B))
        AssetMovementType.DEPRECIATION, AssetMovementType.AMORTIZATION -> Triple(Icons.Default.TrendingDown, Color(0xFF38BDF8), Color(0xFF0C3866))
        AssetMovementType.IMPROVEMENT -> Triple(Icons.Default.TrendingUp, Color(0xFF0284C7), Color(0xFF0C2444))
        AssetMovementType.REVALUATION -> Triple(Icons.Default.TrendingUp, Color(0xFF8B5CF6), Color(0xFF2E1065))
        AssetMovementType.IMPAIRMENT -> Triple(Icons.Default.TrendingDown, Color(0xFFEF4444), Color(0xFF450A0A))
        AssetMovementType.MAINTENANCE -> Triple(Icons.Default.Build, Color(0xFFF59E0B), Color(0xFF451A03))
        AssetMovementType.DISPOSAL -> Triple(Icons.Default.Delete, Color(0xFF94A3B8), Color(0xFF1E293B))
        AssetMovementType.SALE -> Triple(Icons.Default.Inventory2, Color(0xFF10B981), Color(0xFF064E3B))
        AssetMovementType.STATUS_CHANGE -> Triple(Icons.Default.SwapHoriz, Color(0xFF38BDF8), Color(0xFF0E2849))
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF071426),
        border = BorderStroke(1.dp, Color(0xFF132B4F)),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(badgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column {
                        Text(
                            text = mov.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = mov.date,
                            fontSize = 11.sp,
                            color = Color(0xFF8EA5C4)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (mov.amount > 0) {
                        Text(
                            text = moneyFmt.format(mov.amount),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    } else {
                        Text(
                            text = "Sin costo",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                    }
                    val impact = mov.financialImpact
                    if (!impact.isNullOrBlank()) {
                        Text(
                            text = impact,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // Rich traceability tags
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (mov.usefulLifeIncrementMonths > 0) {
                    MovementDetailChip(label = "+${mov.usefulLifeIncrementMonths}m vida útil", color = Color(0xFF34D399))
                }
                val residual = mov.newResidualValue
                if (residual != null) {
                    MovementDetailChip(label = "Residual: $ ${moneyFmt.format(residual)}", color = Color(0xFF60A5FA))
                }
                val vendor = mov.providerOrVendor
                if (!vendor.isNullOrBlank()) {
                    MovementDetailChip(label = "Tercero: $vendor", color = Color(0xFFFBBF24))
                }
                val doc = mov.invoiceOrDocNumber
                if (!doc.isNullOrBlank()) {
                    MovementDetailChip(label = "Doc: $doc", color = Color(0xFFA78BFA))
                }
                val cc = mov.costCenter
                if (!cc.isNullOrBlank()) {
                    MovementDetailChip(label = "CC: $cc", color = Color(0xFF38BDF8))
                }
                val account = mov.accountingAccount
                if (!account.isNullOrBlank()) {
                    MovementDetailChip(label = "Cuenta: $account", color = Color(0xFF94A3B8))
                }
                val disposal = mov.disposalReason
                if (!disposal.isNullOrBlank()) {
                    MovementDetailChip(label = "Baja: $disposal", color = Color(0xFFEF4444))
                }
                val impairment = mov.impairmentReason
                if (!impairment.isNullOrBlank()) {
                    MovementDetailChip(label = "Causa: $impairment", color = Color(0xFFEF4444))
                }
                val docUri = mov.documentUri
                if (!docUri.isNullOrBlank()) {
                    MovementDetailChip(label = "PDF Soporte", color = Color(0xFF34D399))
                }
            }

            val notesText = mov.notes
            if (!notesText.isNullOrBlank()) {
                Text(
                    text = notesText,
                    fontSize = 11.5.sp,
                    color = Color(0xFFCBD5E1)
                )
            }
        }
    }
}

@Composable
private fun MovementDetailChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF0F1E38),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun LegendRow(label: String, percentage: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(text = label, fontSize = 9.sp, color = Color(0xFF8EA5C4))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = percentage, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun ValueEvolutionLineChart(
    acquisitionCost: Double,
    currentBookValue: Double,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Subtle guide lines
        for (i in 0..4) {
            val y = height * (i / 4f)
            drawLine(
                color = Color(0xFF132B4F),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        // Dynamic points based on real acquisition value and current book value
        val depRatio = if (acquisitionCost > 0) {
            (1.0 - (currentBookValue / acquisitionCost)).coerceIn(0.0, 1.0).toFloat()
        } else 0.5f

        val startY = height * 0.18f
        val currentY = (startY + (height * 0.65f * depRatio)).coerceAtMost(height * 0.85f)

        val points = listOf(
            Offset(width * 0.05f, startY),
            Offset(width * 0.23f, startY + (currentY - startY) * 0.25f),
            Offset(width * 0.41f, startY + (currentY - startY) * 0.55f),
            Offset(width * 0.60f, currentY),
            Offset(width * 0.78f, (currentY + (height * 0.85f - currentY) * 0.45f).coerceAtMost(height * 0.88f)),
            Offset(width * 0.95f, (height * 0.88f))
        )

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }

        // Draw glowing line
        drawPath(
            path = path,
            color = Color(0xFF007AFF),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw points with white center
        points.forEach { pt ->
            drawCircle(color = Color(0xFF007AFF), radius = 5.dp.toPx(), center = pt)
            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pt)
        }
    }
}

@Composable
fun AssetHardwareThumbnail(
    category: AssetCategory,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (category) {
            AssetCategory.TECHNOLOGY -> {
                // Sleek MacBook / Laptop drawing
                // Screen bezel
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))),
                    topLeft = Offset(w * 0.18f, h * 0.15f),
                    size = Size(w * 0.64f, h * 0.50f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
                // Inner blue screen glow
                drawRoundRect(
                    brush = Brush.radialGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF0B172B))),
                    topLeft = Offset(w * 0.22f, h * 0.19f),
                    size = Size(w * 0.56f, h * 0.42f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
                // Laptop base
                drawRoundRect(
                    color = Color(0xFF64748B),
                    topLeft = Offset(w * 0.10f, h * 0.65f),
                    size = Size(w * 0.80f, h * 0.08f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
            }
            AssetCategory.VEHICLES -> {
                // Sleek Car / Truck drawing
                drawRoundRect(
                    color = Color(0xFF0284C7),
                    topLeft = Offset(w * 0.15f, h * 0.35f),
                    size = Size(w * 0.70f, h * 0.30f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )
                // Wheels
                drawCircle(color = Color(0xFF0F172A), radius = w * 0.12f, center = Offset(w * 0.30f, h * 0.68f))
                drawCircle(color = Color(0xFF0F172A), radius = w * 0.12f, center = Offset(w * 0.70f, h * 0.68f))
            }
            else -> {
                // General asset icon shape
                drawCircle(
                    color = Color(0xFF1E3A63),
                    radius = w * 0.38f,
                    center = Offset(w * 0.5f, h * 0.5f)
                )
                drawCircle(
                    color = Color(0xFF007AFF),
                    radius = w * 0.22f,
                    center = Offset(w * 0.5f, h * 0.5f)
                )
            }
        }
    }
}

@Composable
private fun DepreciationTabContent(
    asset: FixedAssetEntity,
    moneyFmt: DecimalFormat,
    onOpenAdvancedSettings: () -> Unit,
    onRegisterImpairment: () -> Unit,
    onRegisterRevaluation: () -> Unit,
    onRunDepreciation: () -> Unit
) {
    val methodEnum = DepreciationMethod.fromString(asset.depreciationMethod)
    val methodStrategyLabel = when (methodEnum) {
        DepreciationMethod.STRAIGHT_LINE -> "Línea Recta (Constante)"
        DepreciationMethod.DIGITS_SUM -> "Suma de Dígitos (Acelerada)"
        DepreciationMethod.DECLINING_BALANCE -> "Saldos Decrecientes (Acelerada)"
    }
    val methodFormula = when (methodEnum) {
        DepreciationMethod.STRAIGHT_LINE -> "(Base - Valor Residual) / Vida Útil"
        DepreciationMethod.DIGITS_SUM -> "(Meses Restantes / Suma Dígitos) × Base"
        DepreciationMethod.DECLINING_BALANCE -> "Tasa Periódica × Saldo Remanente en Libros"
    }

    val effectiveCost = when {
        asset.depreciableBase.contains("reposición", ignoreCase = true) || asset.depreciableBase.contains("reposicion", ignoreCase = true) -> asset.currentMarketValue
        asset.depreciableBase.contains("neto", ignoreCase = true) || asset.depreciableBase.contains("ajustado", ignoreCase = true) -> kotlin.math.max(0.0, asset.acquisitionValue - asset.accumulatedDepreciation)
        else -> asset.acquisitionValue
    }
    val effectiveBase = kotlin.math.max(0.0, effectiveCost - asset.residualValue)
    val effectiveStartDate = AssetFinancialCalculations.resolveEffectiveStartDate(asset.acquisitionDate, asset.startConvention)

    val schedule = remember(asset) {
        AssetFinancialCalculations.generateScheduleAdvanced(
            acquisitionValue = asset.acquisitionValue,
            residualValue = asset.residualValue,
            usefulLifeMonths = asset.usefulLifeMonths,
            method = methodEnum,
            startDateStr = asset.acquisitionDate,
            depreciableBaseMode = asset.depreciableBase,
            startConvention = asset.startConvention,
            frequency = asset.frequency,
            rounding = asset.rounding,
            currentMarketValue = asset.currentMarketValue,
            accumulatedDepreciation = asset.accumulatedDepreciation
        )
    }

    val primaryBlue = Color(0xFF007AFF)
    val accentCyan = Color(0xFF38BDF8)
    val cardBg = Color(0xFF0B172B)
    val cardBorder = Color(0xFF132B4F)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // CARD 1: TRATAMIENTO Y POLÍTICA DE DEPRECIACIÓN
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Tratamiento de Depreciación", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Curva de desgaste y política contable", fontSize = 11.5.sp, color = Color(0xFF8EA5C4))
                    }
                    Button(
                        onClick = onOpenAdvancedSettings,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E2849)),
                        border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = accentCyan, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Opciones avanzadas", fontSize = 12.sp, color = accentCyan, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Strategy Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF09162A),
                    border = BorderStroke(1.dp, Color(0xFF1E3558)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(primaryBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoMode, contentDescription = null, tint = primaryBlue, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(methodStrategyLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Fórmula: $methodFormula", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }

                // 2x2 Grid with Calculation Options
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        SpecParamMiniCard(
                            label = "Base de cálculo",
                            value = asset.depreciableBase,
                            subValue = moneyFmt.format(effectiveCost)
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SpecParamMiniCard(
                            label = "Convención inicio",
                            value = asset.startConvention,
                            subValue = "Desde $effectiveStartDate"
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        SpecParamMiniCard(
                            label = "Frecuencia",
                            value = asset.frequency,
                            subValue = "${schedule.size} periodos"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SpecParamMiniCard(
                            label = "Redondeo",
                            value = asset.rounding,
                            subValue = "Ajuste contable"
                        )
                    }
                }

                SpecItem("Base amortizable neta", moneyFmt.format(effectiveBase))
                SpecItem("Valor residual estimado", moneyFmt.format(asset.residualValue))
                SpecItem("Vida útil", "${asset.usefulLifeMonths} meses (${asset.usefulLifeMonths / 12} años)")
                SpecItem("Última ejecución contable", asset.lastDepreciationDate ?: "Sin ejecuciones previas")
            }
        }

        // CARD 2: ACCIONES CONTABLES (DETERIORO, REVALUACIÓN, EJECUTAR)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Acciones Contables", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Registra novedades contables según las políticas activas.", fontSize = 11.5.sp, color = Color(0xFF8EA5C4))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (asset.impairmentTracking) {
                        Button(
                            onClick = onRegisterImpairment,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                            border = BorderStroke(1.dp, Color(0xFFB91C1C)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("⚠️ Deterioro", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    if (asset.revaluationEnabled) {
                        Button(
                            onClick = onRegisterRevaluation,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF065F46)),
                            border = BorderStroke(1.dp, Color(0xFF059669)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📈 Revaluación", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Button(
                        onClick = onRunDepreciation,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("▶️ Amortizar", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                if (!asset.impairmentTracking || !asset.revaluationEnabled) {
                    val hints = buildList {
                        if (!asset.impairmentTracking) add("Seguimiento de deterioro inactivo")
                        if (!asset.revaluationEnabled) add("Modelo de revaluación inactivo")
                    }.joinToString(" • ")
                    Text(
                        text = "Políticas restringidas: $hints (Actívalas en Opciones avanzadas).",
                        fontSize = 10.5.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        // CARD 3: CRONOGRAMA DE AMORTIZACIÓN PROYECTADO
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Cronograma de Depreciación", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${schedule.size} periodos proyectados • Frecuencia ${asset.frequency}", fontSize = 11.5.sp, color = Color(0xFF8EA5C4))
                    }
                }

                if (schedule.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay proyección aplicable para la base o vida útil actual.", color = Color(0xFF8EA5C4), fontSize = 12.sp)
                    }
                } else {
                    // Header de la tabla
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF071426),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Periodo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), modifier = Modifier.weight(1.2f))
                            Text("Cuota", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), modifier = Modifier.weight(1f))
                            Text("Dep. Acum.", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), modifier = Modifier.weight(1f))
                            Text("En Libros", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), modifier = Modifier.weight(1.1f))
                        }
                    }

                    // Schedule rows
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        schedule.take(24).forEachIndexed { idx, entry ->
                            val isEven = idx % 2 == 0
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isEven) Color(0xFF0C1D36) else Color(0xFF081527),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(entry.periodLabel, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White, modifier = Modifier.weight(1.2f))
                                    Text(moneyFmt.format(entry.monthlyDepreciation), fontSize = 11.sp, color = accentCyan, modifier = Modifier.weight(1f))
                                    Text(moneyFmt.format(entry.accumulatedDepreciation), fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.weight(1f))
                                    Text(moneyFmt.format(entry.remainingBookValue), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF34D399), modifier = Modifier.weight(1.1f))
                                }
                            }
                        }
                        if (schedule.size > 24) {
                            Text(
                                text = "... y ${schedule.size - 24} periodos subsiguientes calculados hasta el fin de la vida útil.",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecParamMiniCard(label: String, value: String, subValue: String? = null) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF071426),
        border = BorderStroke(1.dp, Color(0xFF132B4F)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontSize = 10.sp, color = Color(0xFF8EA5C4))
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
            if (subValue != null) {
                Text(subValue, fontSize = 10.5.sp, color = Color(0xFF38BDF8))
            }
        }
    }
}

@Composable
private fun MaintenanceTabContent(
    movements: List<AssetMovementEntity>,
    moneyFmt: DecimalFormat,
    onAddMaintenance: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0B172B),
        border = BorderStroke(1.dp, Color(0xFF132B4F)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bitácora de Mantenimientos", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Button(
                    onClick = onAddMaintenance,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+ Registrar", fontSize = 11.sp, color = Color.White)
                }
            }

            if (movements.isEmpty()) {
                Text("No hay mantenimientos registrados aún.", fontSize = 12.sp, color = Color(0xFF8EA5C4))
            } else {
                movements.forEach { mov ->
                    MovementRowItem(mov = mov, moneyFmt = moneyFmt)
                }
            }
        }
    }
}

@Composable
private fun MovementsTabContent(
    movements: List<AssetMovementEntity>,
    asset: FixedAssetEntity,
    moneyFmt: DecimalFormat,
    onAddMovement: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("TODOS") }
    val filters = listOf(
        "TODOS",
        "ADQUISICIÓN",
        "DEPRECIACIÓN",
        "MANTENIMIENTO",
        "MEJORAS",
        "REVALÚOS",
        "DETERIORO",
        "BAJAS",
        "ESTADOS"
    )

    val displayList = remember(movements, selectedFilter, asset) {
        val base = if (movements.isNotEmpty()) {
            movements
        } else {
            listOfNotNull(
                AssetMovementEntity(
                    id = "mov_init_${asset.id}",
                    assetId = asset.id,
                    userId = asset.userId,
                    type = "ACQUISITION",
                    title = "Compra y Capitalización Inicial",
                    date = asset.acquisitionDate,
                    amount = asset.acquisitionValue,
                    notes = "Registro inicial del activo en libros contables NIIF",
                    financialImpact = "Capitalizable (+ Base depreciable)"
                ),
                if (asset.accumulatedDepreciation > 0) {
                    AssetMovementEntity(
                        id = "mov_dep_${asset.id}",
                        assetId = asset.id,
                        userId = asset.userId,
                        type = "DEPRECIATION",
                        title = "Depreciación Acumulada Contable",
                        date = asset.lastDepreciationDate ?: asset.acquisitionDate,
                        amount = asset.accumulatedDepreciation,
                        notes = "Amortización contable acumulada bajo NIIF / NIC 16",
                        financialImpact = "Gasto del Periodo (P&L)"
                    )
                } else null,
                if (asset.status == "MAINTENANCE" || asset.status == "EN_MANTENIMIENTO") {
                    AssetMovementEntity(
                        id = "mov_maint_${asset.id}",
                        assetId = asset.id,
                        userId = asset.userId,
                        type = "MAINTENANCE",
                        title = "Ingreso a Taller / Mantenimiento",
                        date = java.time.LocalDate.now().toString(),
                        amount = 0.0,
                        notes = "Activo en proceso de mantenimiento correctivo/preventivo.",
                        financialImpact = "Gasto Operacional (100% P&L)"
                    )
                } else null,
                if (asset.status == "WRITTEN_OFF" || asset.status == "DADO_DE_BAJA") {
                    AssetMovementEntity(
                        id = "mov_disp_${asset.id}",
                        assetId = asset.id,
                        userId = asset.userId,
                        type = "DISPOSAL",
                        title = "Baja contable por retiro patrimonial",
                        date = asset.lastDepreciationDate ?: asset.acquisitionDate,
                        amount = 0.0,
                        notes = "Activo desincorporado de libros bajo normas contables NIIF.",
                        financialImpact = "Pérdida por Retiro de Activos (NIIF)"
                    )
                } else null
            )
        }

        when (selectedFilter) {
            "TODOS" -> base
            "ADQUISICIÓN" -> base.filter { it.type.equals("ACQUISITION", ignoreCase = true) }
            "DEPRECIACIÓN" -> base.filter { 
                it.type.equals("DEPRECIATION", ignoreCase = true) || it.type.equals("AMORTIZATION", ignoreCase = true) 
            }
            "MANTENIMIENTO" -> base.filter { it.type.equals("MAINTENANCE", ignoreCase = true) }
            "MEJORAS" -> base.filter { it.type.equals("IMPROVEMENT", ignoreCase = true) }
            "REVALÚOS" -> base.filter { it.type.equals("REVALUATION", ignoreCase = true) }
            "DETERIORO" -> base.filter { it.type.equals("IMPAIRMENT", ignoreCase = true) }
            "BAJAS" -> base.filter { 
                it.type.equals("DISPOSAL", ignoreCase = true) || it.type.equals("SALE", ignoreCase = true) 
            }
            "ESTADOS" -> base.filter { it.type.equals("STATUS_CHANGE", ignoreCase = true) }
            else -> base
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0B172B),
        border = BorderStroke(1.dp, Color(0xFF132B4F)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Historial de Movimientos", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${displayList.size} registros contables y operativos", fontSize = 11.sp, color = Color(0xFF8EA5C4))
                }
                Button(
                    onClick = onAddMovement,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Registrar", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filters.forEach { f ->
                    val isSel = f == selectedFilter
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) Color(0xFF007AFF) else Color(0xFF071222),
                        border = BorderStroke(1.dp, if (isSel) Color(0xFF007AFF) else Color(0xFF163259)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedFilter = f }
                    ) {
                        Text(
                            text = f,
                            fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color.White else Color(0xFF8EA5C4),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (displayList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No se encontraron movimientos para este filtro.", fontSize = 12.sp, color = Color(0xFF8EA5C4))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    displayList.forEach { mov ->
                        MovementRowItem(mov = mov, moneyFmt = moneyFmt)
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentsTabContent(
    asset: FixedAssetEntity,
    moneyFmt: DecimalFormat,
    onOpenPhoto: () -> Unit,
    onAttachPhoto: () -> Unit,
    onExportPdf: () -> Unit,
    onEditAsset: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Fotografía de Inventario Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0B172B),
            border = BorderStroke(1.dp, Color(0xFF132B4F)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fotografía de Inventario", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    if (!asset.imageUrl.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF064E3B),
                            border = BorderStroke(1.dp, Color(0xFF059669))
                        ) {
                            Text(
                                text = "Evidencia Verificada",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (!asset.imageUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF040A14))
                            .border(BorderStroke(1.dp, Color(0xFF163259)), RoundedCornerShape(14.dp))
                            .clickable { onOpenPhoto() }
                    ) {
                        AsyncImage(
                            model = asset.imageUrl,
                            contentDescription = asset.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xCC000000),
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                        ) {
                            Text("Tocar para ampliar", fontSize = 10.sp, color = Color.White, modifier = Modifier.padding(6.dp))
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF071222),
                        border = BorderStroke(1.dp, Color(0xFF163259)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Sin fotografía adjunta", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text("Sube una foto del activo editando el registro para auditorías patrimoniales.", fontSize = 11.5.sp, color = Color(0xFF8EA5C4), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Button(
                                onClick = onAttachPhoto,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("+ Adjuntar Foto", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // 2. Factura de Compra y Comprobantes Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0B172B),
            border = BorderStroke(1.dp, Color(0xFF132B4F)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Comprobante de Compra y Proveedor", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                SpecItem("Proveedor / Emisor", asset.supplier ?: "No especificado")
                SpecItem("Fecha de Factura", asset.acquisitionDate)
                SpecItem("Valor de Adquisición", "${moneyFmt.format(asset.acquisitionValue)} ${asset.currency}")
                SpecItem("Ubicación Registrada", asset.location ?: "Sede Principal")
                SpecItem("Clasificación Contable", asset.category)
            }
        }

        // 3. Ficha Técnica y Garantía Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0B172B),
            border = BorderStroke(1.dp, Color(0xFF132B4F)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Ficha Técnica y Amparo", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                SpecItem("Marca", asset.brand ?: "Genérica")
                SpecItem("Modelo / Año", asset.model ?: "Estándar")
                SpecItem("Identificador Serie/Placa", asset.serialNumber ?: asset.licensePlate ?: "N/A")
                SpecItem("Póliza de Seguro", "Amparado bajo Póliza Multirriesgo NIIF")
                SpecItem("Vigencia Garantía", asset.warrantyExpiryDate ?: "No registrada")
            }
        }

        // Botón exportar PDF expediente
        Button(
            onClick = onExportPdf,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(16.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Exportar Expediente Completo (PDF)", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun MoreOptionsTabContent(
    asset: FixedAssetEntity,
    onOpenAdvancedSettings: () -> Unit,
    onRegisterMaintenance: () -> Unit,
    onExportPdf: () -> Unit,
    onEditAsset: () -> Unit,
    onDeleteAsset: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0B172B),
        border = BorderStroke(1.dp, Color(0xFF132B4F)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Gestión Avanzada del Activo", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)

            MoreOptionRow(
                title = "Configuración NIIF y Depreciación",
                subtitle = "Métodos contables, bases, deterioro y vidas útiles",
                icon = Icons.Default.Settings,
                iconColor = Color(0xFF007AFF),
                onClick = onOpenAdvancedSettings
            )

            MoreOptionRow(
                title = "Registrar Mantenimiento o Mejora",
                subtitle = "Adicionar costos o registrar mantenimientos preventivos",
                icon = Icons.Default.Build,
                iconColor = Color(0xFFF59E0B),
                onClick = onRegisterMaintenance
            )

            MoreOptionRow(
                title = "Generar Reporte Contable (PDF)",
                subtitle = "Descargar informe formal con gráficos y especificaciones",
                icon = Icons.Default.FileDownload,
                iconColor = Color(0xFF10B981),
                onClick = onExportPdf
            )

            MoreOptionRow(
                title = "Editar Datos y Fotografía del Activo",
                subtitle = "Modificar nombre, valores, categoría o imagen",
                icon = Icons.Default.Edit,
                iconColor = Color(0xFF38BDF8),
                onClick = onEditAsset
            )

            MoreOptionRow(
                title = "Eliminar Activo Definitivamente",
                subtitle = "Borrar el activo y su historial de la base de datos",
                icon = Icons.Default.Delete,
                iconColor = Color(0xFFEF4444),
                onClick = onDeleteAsset
            )
        }
    }
}

@Composable
private fun MoreOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF071222),
        border = BorderStroke(1.dp, Color(0xFF163259)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text(subtitle, fontSize = 11.sp, color = Color(0xFF8EA5C4))
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color(0xFF8EA5C4), modifier = Modifier.size(18.dp))
        }
    }
}
