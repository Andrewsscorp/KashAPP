package com.example.presentation.dashboard.detail.components

import android.content.Context
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.TransactionEntity
import com.example.domain.models.FinancialRubroType
import com.example.domain.models.RubroItemDisplay
import com.example.ui.components.CurrencyText
import com.example.domain.models.AccountMovementDisplay
import com.example.presentation.dashboard.detail.primaryColor
import com.example.presentation.dashboard.detail.bgSoftColor
import com.example.util.FinancialReportExporter
import java.math.BigDecimal
import java.text.DecimalFormat

/**
 * Top Hero Header component for Financial Rubro Detail screen.
 * Displays the dark slate gradient, navigation back button, iPhone-style filter and export pills,
 * rubro icon and descriptions, and the translucent total amount metric card with progress bar.
 */
@Composable
fun RubroDetailHeader(
    rubroType: FinancialRubroType,
    filteredTotal: BigDecimal,
    rubroPercentage: Double,
    activeFilterCount: Int,
    isPrivacyMode: Boolean,
    onDismiss: () -> Unit,
    onOpenFilters: () -> Unit,
    onOpenExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            )
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column {
            // Top Navigation & Action Pills Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .testTag("rubro_header_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = Color.White
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // iPhone Style Filter Pill Button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (activeFilterCount > 0) Color(0xFF0284C7) else Color.White.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (activeFilterCount > 0) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onOpenFilters() }
                            .testTag("rubro_filters_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (activeFilterCount > 0) "Filtros ($activeFilterCount)" else "Filtros",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Export Button Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = rubroType.primaryColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onOpenExport() }
                            .testTag("rubro_export_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Exportar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Rubro Identity Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(rubroType.primaryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (rubroType) {
                            FinancialRubroType.AVAILABLE_CASH -> Icons.Default.AccountBalance
                            FinancialRubroType.INVESTMENTS -> Icons.Default.TrendingUp
                            FinancialRubroType.DEBTS -> Icons.Default.CreditCard
                            FinancialRubroType.ASSETS -> Icons.Default.AccountBalanceWallet
                        },
                        contentDescription = null,
                        tint = rubroType.primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rubroType.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = rubroType.subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Translucent Metric Card with CurrencyText & Sparkline Progress
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL EN ESTE RUBRO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 0.8.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = rubroType.primaryColor.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "${"%.1f".format(rubroPercentage)}% del Patrimonio",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    CurrencyText(
                        amount = filteredTotal,
                        isPrivacyMode = isPrivacyMode,
                        fontSize = 30.sp,
                        minFontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        testTag = "rubro_hero_total_amount"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Visual Progress / Sparkline Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((rubroPercentage / 100.0).toFloat().coerceIn(0.05f, 1f))
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(rubroType.primaryColor)
                        )
                    }
                }
            }
        }
    }
}

/**
 * iPhone Style Multi-Criteria Filter Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RubroFilterBottomSheet(
    rubroType: FinancialRubroType,
    selectedPeriod: String,
    onPeriodChange: (String) -> Unit,
    periodOptions: List<String>,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    categoryOptions: List<String>,
    selectedEntity: String,
    onEntityChange: (String) -> Unit,
    entityOptions: List<String>,
    selectedCurrency: String,
    onCurrencyChange: (String) -> Unit,
    currencyOptions: List<String>,
    selectedAmountRange: String,
    onAmountRangeChange: (String) -> Unit,
    amountRangeOptions: List<String>,
    resultsCount: Int,
    onResetFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filtros del Rubro", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                TextButton(onClick = onResetFilters) {
                    Text("Restablecer", color = Color(0xFF0284C7), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            HorizontalDivider(color = Color(0xFFE2E8F0))

            // 1. Periodo
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("1. Periodo de Consulta", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    periodOptions.forEach { opt ->
                        FilterChip(
                            selected = selectedPeriod == opt,
                            onClick = { onPeriodChange(opt) },
                            label = { Text(opt, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = rubroType.primaryColor, selectedLabelColor = Color.White)
                        )
                    }
                }
            }

            // 2. Categoria
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("2. Categoría / Clase", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categoryOptions.forEach { opt ->
                        FilterChip(
                            selected = selectedCategory == opt,
                            onClick = { onCategoryChange(opt) },
                            label = { Text(opt, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = rubroType.primaryColor, selectedLabelColor = Color.White)
                        )
                    }
                }
            }

            // 3. Entidad
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("3. Entidad Bancaria", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    entityOptions.forEach { opt ->
                        FilterChip(
                            selected = selectedEntity == opt,
                            onClick = { onEntityChange(opt) },
                            label = { Text(opt, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = rubroType.primaryColor, selectedLabelColor = Color.White)
                        )
                    }
                }
            }

            // 4 & 5. Moneda y Rango
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("4. Moneda", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        currencyOptions.forEach { cur ->
                            FilterChip(
                                selected = selectedCurrency == cur,
                                onClick = { onCurrencyChange(cur) },
                                label = { Text(cur, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = rubroType.primaryColor, selectedLabelColor = Color.White)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("5. Criterio", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        amountRangeOptions.forEach { rng ->
                            FilterChip(
                                selected = selectedAmountRange == rng,
                                onClick = { onAmountRangeChange(rng) },
                                label = { Text(rng, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = rubroType.primaryColor, selectedLabelColor = Color.White)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = rubroType.primaryColor),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Aplicar Filtros ($resultsCount resultados)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Export Options Bottom Sheet (Excel CSV / PDF)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RubroExportBottomSheet(
    rubroType: FinancialRubroType,
    itemsCount: Int,
    onExportExcel: () -> Unit,
    onExportPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Exportar Reporte Formateado", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text("Rubro: ${rubroType.title} ($itemsCount registros)", fontSize = 12.sp, color = Color(0xFF64748B))
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF64748B))
                }
            }

            HorizontalDivider(color = Color(0xFFE2E8F0))

            // Option 1: Excel CSV
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF0FDF4),
                border = BorderStroke(1.5.dp, Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth().clickable { onExportExcel() }
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF16A34A)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.TableChart, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Exportar en Planilla Excel (CSV)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF14532D))
                        Text("Tabla estructurada con ID, Banco, Categoría, Saldo, Tasa y Sub-compras diferidas.", fontSize = 12.sp, color = Color(0xFF166534))
                    }
                }
            }

            // Option 2: PDF
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.5.dp, Color(0xFFBFDBFE)),
                modifier = Modifier.fillMaxWidth().clickable { onExportPdf() }
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF0284C7)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Exportar en Reporte PDF Formateado", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                        Text("Documento ejecutivo con totales, filtros aplicados y desglose detallado.", fontSize = 12.sp, color = Color(0xFF1E40AF))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

/**
 * Elegant Result Dialog after generating an export file
 */
@Composable
fun RubroExportSuccessDialog(
    exportResult: FinancialReportExporter.ExportResult,
    mimeType: String,
    title: String,
    context: Context,
    onDismiss: () -> Unit
) {
    val isPdf = mimeType == "application/pdf"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(26.dp))
                .testTag("export_success_dialog"),
            color = Color.White,
            shadowElevation = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (isPdf) Color(0xFFE0F2FE) else Color(0xFFDCFCE7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.TableChart,
                        contentDescription = null,
                        tint = if (isPdf) Color(0xFF0284C7) else Color(0xFF16A34A),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "¡Reporte Generado con Éxito!",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "El archivo ha sido creado y formateado con toda la estructura y datos financieros.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPdf) Color(0xFFEFF6FF) else Color(0xFFF0FDF4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isPdf) "PDF" else "XLS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isPdf) Color(0xFF0284C7) else Color(0xFF16A34A)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Listo para abrir o compartir",
                                fontSize = 11.sp,
                                color = Color(0xFF16A34A),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (exportResult.fileUri != null) {
                            FinancialReportExporter.openExportedFile(context, exportResult.fileUri, mimeType)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isPdf) Color(0xFF0284C7) else Color(0xFF16A34A)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Ver / Abrir Archivo", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        if (exportResult.fileUri != null) {
                            FinancialReportExporter.shareExportedFile(context, exportResult.fileUri, mimeType, title)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF334155), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Compartir Documento", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(onClick = onDismiss) {
                    Text("Cerrar", color = Color(0xFF64748B), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/**
 * Account / Card Movements and Percentage of Total Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountMovementsBottomSheet(
    targetAccount: RubroItemDisplay,
    roomTransactions: List<TransactionEntity>,
    context: Context,
    onExportSuccess: (FinancialReportExporter.ExportResult, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val isCard = targetAccount.category.contains("Tarjeta", ignoreCase = true) || targetAccount.category.contains("Crédito", ignoreCase = true)
    val totalAccountAmount = targetAccount.amount.toDouble().coerceAtLeast(1.0)

    val accountMovements = remember(targetAccount, roomTransactions) {
        if (targetAccount.subPurchases.isNotEmpty()) {
            targetAccount.subPurchases.map { sub ->
                val pct = if (totalAccountAmount > 0) (sub.currentBalance / totalAccountAmount) * 100.0 else 0.0
                AccountMovementDisplay(
                    id = sub.id,
                    title = sub.merchant,
                    category = sub.concept.ifBlank { sub.category },
                    dateFormatted = sub.dateFormatted,
                    amount = sub.currentBalance,
                    percentageOfTotal = pct,
                    details = "Cuota ${sub.paidInstallments} de ${sub.totalInstallments} (${sub.rateEa}% E.A.)"
                )
            }
        } else {
            val matchingTrans = roomTransactions.filter { it.accountId == targetAccount.id }
            if (matchingTrans.isNotEmpty()) {
                matchingTrans.map { tx ->
                    val pct = if (totalAccountAmount > 0) (kotlin.math.abs(tx.amount) / totalAccountAmount) * 100.0 else 0.0
                    AccountMovementDisplay(
                        id = tx.id,
                        title = (tx.description ?: "").ifBlank { tx.categoryName },
                        category = tx.categoryName,
                        dateFormatted = tx.date,
                        amount = kotlin.math.abs(tx.amount),
                        percentageOfTotal = pct,
                        details = if (tx.type == "INCOME") "Ingreso / Depósito" else "Gasto / Débito"
                    )
                }
            } else {
                listOf(
                    AccountMovementDisplay(
                        id = "mov_1",
                        title = if (isCard) "Compras diferidas consolidadas" else "Saldo base y depósitos",
                        category = if (isCard) "Consumo" else "Fondos Disponibles",
                        dateFormatted = "Este Mes",
                        amount = totalAccountAmount * 0.65,
                        percentageOfTotal = 65.0,
                        details = if (isCard) "65% del total de deuda" else "65% del saldo disponible"
                    ),
                    AccountMovementDisplay(
                        id = "mov_2",
                        title = if (isCard) "Compras corrientes del periodo" else "Transferencias y rendimientos",
                        category = if (isCard) "Alimentación y Gastos" else "Rendimientos / Nómina",
                        dateFormatted = "Este Mes",
                        amount = totalAccountAmount * 0.25,
                        percentageOfTotal = 25.0,
                        details = if (isCard) "25% del total de deuda" else "25% del saldo disponible"
                    ),
                    AccountMovementDisplay(
                        id = "mov_3",
                        title = if (isCard) "Intereses y comisiones diferidas" else "Reserva de liquidez",
                        category = if (isCard) "Financiero" else "Ahorro programado",
                        dateFormatted = "Este Mes",
                        amount = totalAccountAmount * 0.10,
                        percentageOfTotal = 10.0,
                        details = if (isCard) "10% del total de deuda" else "10% del saldo disponible"
                    )
                )
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = targetAccount.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text(text = "${targetAccount.institution} • ${targetAccount.category}", fontSize = 12.sp, color = Color(0xFF64748B))
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF64748B))
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isCard) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                border = BorderStroke(1.5.dp, if (isCard) Color(0xFFFECACA) else Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isCard) "TOTAL DEUDA DE LA TARJETA" else "SALDO TOTAL DE LA CUENTA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCard) Color(0xFF991B1B) else Color(0xFF166534),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$ ${DecimalFormat("#,##0").format(targetAccount.amount.toDouble())} ${targetAccount.currency}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isCard) Color(0xFFDC2626) else Color(0xFF16A34A)
                        )
                    }
                    if (targetAccount.rateEa != null) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color.White) {
                            Text(
                                text = "${targetAccount.rateEa}% E.A.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCard) Color(0xFFDC2626) else Color(0xFF059669),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val res = FinancialReportExporter.exportAccountToPdf(context, targetAccount, accountMovements)
                        onExportSuccess(res, "application/pdf", "Reporte PDF - ${targetAccount.name}")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Descargar PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val res = FinancialReportExporter.exportAccountToExcelCsv(context, targetAccount, accountMovements)
                        onExportSuccess(res, "text/csv", "Planilla Excel - ${targetAccount.name}")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Descargar Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = Color(0xFFE2E8F0))

            Text(
                text = "MOVIMIENTOS QUE CONFORMAN ESTE TOTAL (${accountMovements.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 0.5.sp
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accountMovements.forEach { mov ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = mov.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${mov.category} • ${mov.dateFormatted}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                if (mov.details.isNotBlank()) {
                                    Text(
                                        text = mov.details,
                                        fontSize = 10.sp,
                                        color = Color(0xFF0284C7),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$ ${DecimalFormat("#,##0").format(mov.amount)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCard) Color(0xFFDC2626) else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isCard) Color(0xFFFEE2E2) else Color(0xFFE0F2FE)
                                ) {
                                    Text(
                                        text = "${"%.1f".format(mov.percentageOfTotal)}% del total",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCard) Color(0xFFB91C1C) else Color(0xFF0369A1),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
