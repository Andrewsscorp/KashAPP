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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FixedAssetEntity
import com.example.domain.model.fixedassets.DepreciationBatch
import com.example.domain.model.fixedassets.DepreciationBatchItem
import com.example.domain.usecase.fixedassets.CalculateAssetDepreciationUseCase.AssetPendingDepreciation
import com.example.domain.usecase.fixedassets.GetPatrimonioResumenUseCase.PatrimonioResumen
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedAssetsDepreciationTab(
    patrimonioResumen: PatrimonioResumen,
    depreciationBatches: List<DepreciationBatch>,
    pendingDepreciationList: List<AssetPendingDepreciation>,
    assets: List<FixedAssetEntity>,
    isRunningDepreciation: Boolean,
    onRunDepreciationTrigger: () -> Unit,
    onRecalculateHistoricalTrigger: () -> Unit = {},
    onOpenProjections: () -> Unit,
    modifier: Modifier = Modifier
) {
    val moneyFmt = remember { DecimalFormat("$ #,##0") }
    var selectedBatchForDetail by remember { mutableStateOf<DepreciationBatch?>(null) }
    var showPendingPreview by remember { mutableStateOf(false) }

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
        // 1. BOTONES SUPERIORES DE ACCIÓN RÁPIDA: [🔄 Depreciación], [🧮 Recalcular Histórico] y [📈 Proyecciones]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Botón Trigger Manual de Depreciación
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0E2849),
                border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = !isRunningDepreciation) { onRunDepreciationTrigger() }
                    .testTag("btn_trigger_depreciacion_batch")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isRunningDepreciation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = accentCyan,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E3A8A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Autorenew, contentDescription = null, tint = accentCyan, modifier = Modifier.size(16.dp))
                        }
                    }
                    Column {
                        Text(
                            text = if (isRunningDepreciation) "Cierre..." else "Ejecutar Cierre",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = "Deprec. mes",
                            fontSize = 9.sp,
                            color = textSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            // Botón Recalcular Depreciación Histórica
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0F2B3E),
                border = BorderStroke(1.dp, Color(0xFF0284C7)),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = !isRunningDepreciation) { onRecalculateHistoricalTrigger() }
                    .testTag("btn_recalculate_historical_depreciation")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0369A1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Column {
                        Text(
                            text = "Recalcular",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = "Histórico compra",
                            fontSize = 9.sp,
                            color = textSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            // Botón Proyecciones Financieras
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF18153A),
                border = BorderStroke(1.dp, Color(0xFF3B2E6E)),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenProjections() }
                    .testTag("btn_proyecciones_depreciacion")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF382968)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(16.dp))
                    }
                    Column {
                        Text(
                            text = "Proyecciones",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = "Plurianual",
                            fontSize = 9.sp,
                            color = Color(0xFFC4B5FD),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 2. TARJETA DE INTEGRIDAD CONTABLE (NIC 16)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
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
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F243E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = accentCyan, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(
                                text = "Ecuación de Integridad Contable",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "NIC 16 • Depreciación acumulada y valor en libros",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Auditado NIIF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                // Balance principal
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "VALOR EN LIBROS NETO",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentCyan,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = moneyFmt.format(patrimonioResumen.totalNetBookValue),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                // Ecuación Contable: Costo Adquisición - Depreciación Acum = Valor Neto
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF071222),
                        border = BorderStroke(1.dp, Color(0xFF163259)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Costo de Adquisición", fontSize = 10.5.sp, color = textSecondary)
                            Text(
                                text = moneyFmt.format(patrimonioResumen.totalAcquisitionCost),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF071222),
                        border = BorderStroke(1.dp, Color(0xFF163259)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Depreciación Acum.", fontSize = 10.5.sp, color = textSecondary)
                            Text(
                                text = "- ${moneyFmt.format(patrimonioResumen.totalAccumulatedDepreciation)}",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF87171)
                            )
                        }
                    }
                }

                // Barra de Porcentajes Dinámicos
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Amortizado: ${patrimonioResumen.amortizedPercent}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentCyan
                        )
                        Text(
                            text = "Remanente: ${patrimonioResumen.remainingPercent}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryBlue
                        )
                    }

                    val progressFraction = (patrimonioResumen.amortizedPercent / 100.0).toFloat().coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = accentCyan,
                        trackColor = Color(0xFF162B48)
                    )
                }

                // Cuota proyectada del mes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cuota proyectada para el próximo cierre:", fontSize = 11.sp, color = textSecondary)
                    Text(
                        text = "${moneyFmt.format(patrimonioResumen.monthlyDepreciationQuota)} / mes",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // 3. SECCIÓN DE ACTIVOS PENDIENTES DE CIERRE (PREVIEW)
        if (pendingDepreciationList.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF071526),
                border = BorderStroke(1.dp, Color(0xFF13325B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F3B66)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = accentCyan, modifier = Modifier.size(14.dp))
                            }
                            Text(
                                text = "${pendingDepreciationList.size} activos listos para cierre",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = if (showPendingPreview) "Ocultar" else "Ver lista",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentCyan,
                            modifier = Modifier
                                .clickable { showPendingPreview = !showPendingPreview }
                                .padding(4.dp)
                        )
                    }

                    AnimatedVisibility(visible = showPendingPreview) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pendingDepreciationList.forEach { pending ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0B172B),
                                    border = BorderStroke(1.dp, Color(0xFF163259)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(pending.asset.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(
                                                "${pending.asset.assetCode} • ${pending.asset.depreciationMethod}",
                                                fontSize = 10.sp,
                                                color = textSecondary
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                moneyFmt.format(pending.totalPendingAmount),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = accentCyan
                                            )
                                            Text("Cuota calculada", fontSize = 9.5.sp, color = textSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. HISTORIAL DE EJECUCIONES (BATCHES CRONOLÓGICOS)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0E2849)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = accentCyan, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        text = "Historial de Cierres Contables (${depreciationBatches.size})",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }

                Text(
                    text = "Toca para ver asientos",
                    fontSize = 10.5.sp,
                    color = textSecondary
                )
            }

            if (depreciationBatches.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.TrendingDown, contentDescription = null, tint = textSecondary, modifier = Modifier.size(32.dp))
                        Text("No se han registrado cierres aún", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text("Presiona 'Ejecutar Cierre' arriba para correr el primer lote automático.", fontSize = 11.sp, color = textSecondary)
                    }
                }
            } else {
                depreciationBatches.forEach { batch ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedBatchForDetail = batch }
                            .testTag("batch_item_${batch.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
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
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = batch.title,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(text = batch.date, fontSize = 11.sp, color = textSecondary)
                                        Text(text = "•", fontSize = 10.sp, color = Color(0xFF475569))
                                        Text(text = "${batch.assetsCount} activos", fontSize = 11.sp, color = accentCyan)
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "- ${moneyFmt.format(batch.totalAmount)}",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF87171)
                                    )
                                    Text(text = "Depreciado", fontSize = 10.sp, color = textSecondary)
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Ver asientos",
                                    tint = textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 5. DETALLE DE ASIENTOS CONTABLES POR LOTE (BOTTOM SHEET MODAL)
    if (selectedBatchForDetail != null) {
        val batch = selectedBatchForDetail!!
        ModalBottomSheet(
            onDismissRequest = { selectedBatchForDetail = null },
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
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Detalle de Asientos Contables",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${batch.title} • ${batch.date}",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                    IconButton(onClick = { selectedBatchForDetail = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Resumen del Lote
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F223D),
                    border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Cuotas del Periodo", fontSize = 11.sp, color = textSecondary)
                            Text(
                                text = moneyFmt.format(batch.totalAmount),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentCyan
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${batch.items.size} Asientos NIIF",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Desglose activo por activo:", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(batch.items) { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0B172B),
                            border = BorderStroke(1.dp, Color(0xFF142C4E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.assetName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(
                                            "${item.assetCode} • ${item.category} • ${item.depreciationMethod}",
                                            fontSize = 10.5.sp,
                                            color = textSecondary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "- ${moneyFmt.format(item.amount)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF87171)
                                        )
                                        Text("Cuota del mes", fontSize = 9.5.sp, color = textSecondary)
                                    }
                                }

                                HorizontalDivider(color = Color(0xFF132A4A), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Acumulada: ${moneyFmt.format(item.newAccumulated)}",
                                        fontSize = 10.5.sp,
                                        color = textSecondary
                                    )
                                    Text(
                                        text = "Valor en libros: ${moneyFmt.format(item.bookValueAfter)}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accentCyan
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
