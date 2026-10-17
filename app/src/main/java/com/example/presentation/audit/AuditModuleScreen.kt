package com.example.presentation.audit

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Enterprise Immutable Audit Vault Screen ("Bóveda de Auditoría Forense").
 * Strictly immutable, append-only cryptographic ledger with 100% real local data,
 * interactive SHA-256 verification, distribution charts, official PDF reports and SIEM JSON exports.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditModuleScreen(
    viewModel: AuditViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle PDF Export and Share intent
    LaunchedEffect(uiState.exportPdfResult) {
        uiState.exportPdfResult?.let { result ->
            if (result.success && result.fileUri != null) {
                try {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, result.fileUri)
                        putExtra(Intent.EXTRA_SUBJECT, "KashApp - Certificado Oficial de Auditoría Forense")
                        putExtra(Intent.EXTRA_TEXT, "Adjunto Certificado Oficial de Auditoría y Trazabilidad Forense emitido por KashApp Enterprise.")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Compartir Informe de Auditoría PDF"))
                } catch (e: Exception) {
                    Toast.makeText(context, "No se pudo abrir visor de PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else if (!result.success && result.message.isNotBlank()) {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            }
            viewModel.clearPdfResult()
        }
    }

    // Handle SIEM JSON Export and Share intent
    LaunchedEffect(uiState.exportSiemResult) {
        uiState.exportSiemResult?.let { result ->
            if (result.success && result.fileUri != null) {
                try {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, result.fileUri)
                        putExtra(Intent.EXTRA_SUBJECT, "KashApp - Archivo de Auditoría SIEM (JSON)")
                        putExtra(Intent.EXTRA_TEXT, "Adjunto archivo de auditoría forense para ingesta SIEM (SOC-2 / ISO 27001 / NIIF NIC 16).")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Compartir Registro SIEM (JSON)"))
                } catch (e: Exception) {
                    Toast.makeText(context, "No se pudo compartir archivo SIEM: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else if (!result.success && result.message.isNotBlank()) {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            }
            viewModel.clearSiemResult()
        }
    }

    LaunchedEffect(uiState.exportSuccessMessage) {
        uiState.exportSuccessMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearExportMessage()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B132B))
            .testTag("audit_module_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ==========================================
            // HEADER TIER 1: TITLE & TOP NAVIGATION (FULL WIDTH)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .testTag("audit_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Bóveda de Auditoría",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(9.dp)
                                )
                                Text(
                                    text = "SOC-2 INMUTABLE",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Cadena de custodia criptográfica append-only • SQLCipher",
                        fontSize = 10.5.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Counter pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text(
                        text = "${uiState.filteredLogs.size}/${uiState.totalCount}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // ==========================================
            // HEADER TIER 2: FORENSIC ACTION TOOLBAR (RESPONSIVE SCROLLABLE)
            // ==========================================
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Verify Integrity Button
                item {
                    AuditActionButton(
                        icon = Icons.Default.Shield,
                        label = if (uiState.isVerifyingIntegrity) "Verificando..." else "Verificar Cripto",
                        color = Color(0xFF38BDF8),
                        isLoading = uiState.isVerifyingIntegrity,
                        testTag = "audit_verify_btn",
                        onClick = { viewModel.verifyVaultIntegrity() }
                    )
                }

                // 2. Export PDF Official Report
                item {
                    AuditActionButton(
                        icon = Icons.Default.FileDownload,
                        label = "Exportar PDF",
                        color = Color.White,
                        containerColor = Color(0xFF007BFF),
                        testTag = "audit_export_pdf_btn",
                        onClick = { viewModel.exportToPdf(context) }
                    )
                }

                // 3. Export SIEM JSON
                item {
                    AuditActionButton(
                        icon = Icons.Default.DataObject,
                        label = "SIEM (JSON)",
                        color = Color(0xFFFBBF24),
                        containerColor = Color(0xFF1E293B),
                        borderColor = Color(0xFFFBBF24).copy(alpha = 0.5f),
                        testTag = "audit_export_siem_btn",
                        onClick = { viewModel.exportToSiemJson(context) }
                    )
                }

                // 4. Copy Text Certificate
                item {
                    AuditActionButton(
                        icon = Icons.Default.ContentCopy,
                        label = "Copiar Certificado",
                        color = Color(0xFFCBD5E1),
                        testTag = "audit_copy_btn",
                        onClick = {
                            val report = viewModel.generateExportCertificate()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("KashApp Audit Report", report))
                            Toast.makeText(context, "Certificado copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // ==========================================
            // INTEGRITY VERIFICATION RESULT BANNER
            // ==========================================
            AnimatedVisibility(
                visible = uiState.integrityVerificationResult != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.integrityVerificationResult?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF052E16),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = msg,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFD1FAE5),
                                    lineHeight = 15.sp
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearIntegrityResult() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = Color(0xFF6EE7B7),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // KPI COUNTERS ROW
            // ==========================================
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AuditCounterCard(
                        title = "TOTAL",
                        count = uiState.totalCount,
                        color = Color(0xFF38BDF8)
                    )
                }
                item {
                    AuditCounterCard(
                        title = "SEGURIDAD",
                        count = uiState.securityCount,
                        color = Color(0xFFEF4444)
                    )
                }
                item {
                    AuditCounterCard(
                        title = "CUENTAS",
                        count = uiState.accountsCount,
                        color = Color(0xFF0284C7)
                    )
                }
                item {
                    AuditCounterCard(
                        title = "ACTIVOS",
                        count = uiState.assetsCount,
                        color = Color(0xFF8B5CF6)
                    )
                }
                item {
                    AuditCounterCard(
                        title = "FINANZAS",
                        count = uiState.transactionsCount,
                        color = Color(0xFF10B981)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // VISUAL CATEGORY DISTRIBUTION BAR
            // ==========================================
            if (uiState.totalCount > 0) {
                val secWeight = (uiState.securityCount.toFloat() / uiState.totalCount).coerceAtLeast(0.02f)
                val accWeight = (uiState.accountsCount.toFloat() / uiState.totalCount).coerceAtLeast(0.02f)
                val astWeight = (uiState.assetsCount.toFloat() / uiState.totalCount).coerceAtLeast(0.02f)
                val txWeight = (uiState.transactionsCount.toFloat() / uiState.totalCount).coerceAtLeast(0.02f)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        Box(modifier = Modifier.weight(secWeight).fillMaxHeight().background(Color(0xFFEF4444)))
                        Box(modifier = Modifier.weight(accWeight).fillMaxHeight().background(Color(0xFF0284C7)))
                        Box(modifier = Modifier.weight(astWeight).fillMaxHeight().background(Color(0xFF8B5CF6)))
                        Box(modifier = Modifier.weight(txWeight).fillMaxHeight().background(Color(0xFF10B981)))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ==========================================
            // SEARCH BAR
            // ==========================================
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = {
                    Text("Buscar evento, detalle o hash SHA-256...", color = Color(0xFF64748B), fontSize = 12.sp)
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(17.dp))
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Limpiar", tint = Color(0xFF94A3B8), modifier = Modifier.size(15.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF007BFF),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // TIME & SEVERITY FILTER CHIPS ROW
            // ==========================================
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Time Filters
                items(AuditTimeFilter.values()) { timeFilter ->
                    val isSelected = uiState.selectedTimeFilter == timeFilter
                    Surface(
                        onClick = { viewModel.onTimeFilterSelect(timeFilter) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155))
                    ) {
                        Text(
                            text = timeFilter.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                item {
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color(0xFF334155)))
                }

                // Severity Filters
                items(AuditSeverityFilter.values()) { sevFilter ->
                    val isSelected = uiState.selectedSeverityFilter == sevFilter
                    Surface(
                        onClick = { viewModel.onSeverityFilterSelect(sevFilter) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF334155) else Color(0xFF1E293B),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF94A3B8) else Color(0xFF334155))
                    ) {
                        Text(
                            text = sevFilter.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ==========================================
            // CATEGORY FILTER CHIPS
            // ==========================================
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(AuditCategory.values()) { category ->
                    val isSelected = uiState.selectedCategory == category
                    val catColor = Color(category.colorHex)
                    Surface(
                        onClick = { viewModel.onCategorySelect(category) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) catColor.copy(alpha = 0.25f) else Color(0xFF1E293B),
                        border = BorderStroke(1.dp, if (isSelected) catColor else Color(0xFF334155))
                    ) {
                        Text(
                            text = category.label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) catColor else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // AUDIT LOGS LIST (CRASH-PROOF KEYS & CLEAN INSETS)
            // ==========================================
            if (uiState.filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty()) "No hay eventos que coincidan con la búsqueda"
                            else "No hay eventos registrados con los filtros seleccionados",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    itemsIndexed(
                        items = uiState.filteredLogs,
                        key = { index, item -> "${item.category.name}_${item.id}_${item.timestamp}_$index" }
                    ) { _, entry ->
                        AuditCardItem(
                            entry = entry,
                            onClick = { viewModel.onSelectEntryForDetail(entry) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.navigationBarsPadding())
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // ==========================================
    // BOTTOM SHEET: EVENT DETAIL MODAL
    // ==========================================
    uiState.selectedEntryForDetail?.let { entry ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.onSelectEntryForDetail(null) },
            sheetState = sheetState,
            containerColor = Color(0xFF1E293B),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 4.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF475569))
                )
            }
        ) {
            AuditDetailBottomSheetContent(
                entry = entry,
                onDismiss = { viewModel.onSelectEntryForDetail(null) }
            )
        }
    }
}

@Composable
private fun AuditActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    containerColor: Color = Color(0xFF1E293B),
    borderColor: Color = Color(0xFF334155),
    isLoading: Boolean = false,
    testTag: String = "",
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(13.dp),
                    color = color,
                    strokeWidth = 1.5.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
private fun AuditCounterCard(
    title: String,
    count: Int,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = count.toString(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
private fun AuditCardItem(
    entry: AuditUiEntry,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("audit_item_${entry.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp)
        ) {
            // Top Row: Category tag + Severity pill + Relative time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(entry.category.colorHex).copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color(entry.category.colorHex).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = entry.category.label,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(entry.category.colorHex),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = entry.severity.color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = entry.severity.name,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = entry.severity.color,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = entry.relativeTime,
                    fontSize = 10.5.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Title
            Text(
                text = entry.eventTitle,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Details preview
            Text(
                text = entry.details.replace("\n", " "),
                fontSize = 11.5.sp,
                color = Color(0xFFCBD5E1),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(9.dp))

            // Bottom row: Origin / Auth method + SHA256 short hash
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = entry.authMethodOrOrigin,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                Text(
                    text = "sha256:${entry.hashSha256.take(8)}...",
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF38BDF8)
                )
            }
        }
    }
}

@Composable
private fun AuditDetailBottomSheetContent(
    entry: AuditUiEntry,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
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
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(entry.category.colorHex).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(entry.category.colorHex).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = entry.category.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(entry.category.colorHex),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = entry.severity.color.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = entry.severity.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = entry.severity.color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = entry.eventTitle,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = entry.formattedDateTime,
            fontSize = 12.sp,
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = Color(0xFF334155))
        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Detalle del Evento",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF94A3B8)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = entry.details,
            fontSize = 13.5.sp,
            color = Color(0xFFE2E8F0),
            lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Origen / Método de Autorización",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF94A3B8)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = entry.authMethodOrOrigin,
            fontSize = 13.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Cryptographic Hash Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Firma Digital SHA-256 (Inmutable)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("SHA256 Hash", entry.hashSha256))
                            Toast.makeText(context, "Hash copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copiar Hash", tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entry.hashSha256,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF))
        ) {
            Text("Entendido", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
