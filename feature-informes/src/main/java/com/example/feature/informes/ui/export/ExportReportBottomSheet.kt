package com.example.feature.informes.ui.export

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.informes.domain.model.ExportReportConfig
import com.example.feature.informes.domain.model.ExportReportFormat
import com.example.feature.informes.domain.model.FinancialPeriodFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportReportBottomSheet(
    filter: FinancialPeriodFilter,
    onExport: (ExportReportConfig, onDone: (String) -> Unit) -> Unit,
    onGeneratePdf: (ExportReportConfig) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(ExportReportFormat.PDF) }
    var incSummary by remember { mutableStateOf(true) }
    var incStatements by remember { mutableStateOf(true) }
    var incSpecialized by remember { mutableStateOf(true) }
    var incAnalysis by remember { mutableStateOf(true) }
    var incDocuments by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("Informe Financiero Central - KashApp") }
    var isExporting by remember { mutableStateOf(false) }
    var exportPreviewText by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Exportar",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Generar y Exportar Informe",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Text(
                text = "Filtro activo: ${filter.periodLabel} • Moneda: ${filter.currency}",
                color = Color(0xFF38BDF8),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Format Selection
            Text(
                text = "FORMATO DE SALIDA",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportReportFormat.values().forEach { fmt ->
                    val isSel = selectedFormat == fmt
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedFormat = fmt },
                        label = { Text(fmt.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF10B981),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Sections to include
            Text(
                text = "SECCIONES A INCLUIR",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ExportCheckboxRow("1. Resumen Ejecutivo & Patrimonio", incSummary) { incSummary = it }
                ExportCheckboxRow("2. Estados Financieros Oficiales (4 estados)", incStatements) { incStatements = it }
                ExportCheckboxRow("3. Informes Especializados (7 informes)", incSpecialized) { incSpecialized = it }
                ExportCheckboxRow("4. Análisis Financiero & Ratios", incAnalysis) { incAnalysis = it }
                ExportCheckboxRow("5. Centro Documental y Soportes", incDocuments) { incDocuments = it }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título del Reporte") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedLabelColor = Color(0xFF38BDF8),
                    unfocusedLabelColor = Color(0xFF94A3B8)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            if (selectedFormat == ExportReportFormat.PDF) {
                Button(
                    onClick = {
                        isExporting = true
                        val config = ExportReportConfig(
                            format = ExportReportFormat.PDF,
                            includeSummary = incSummary,
                            includeFinancialStatements = incStatements,
                            includeSpecializedReports = incSpecialized,
                            includeFinancialAnalysis = incAnalysis,
                            includeDocumentCatalog = incDocuments,
                            reportTitle = title
                        )
                        onGeneratePdf(config)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generar PDF y Ver Vista Previa",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = {
                        isExporting = true
                        val config = ExportReportConfig(
                            format = selectedFormat,
                            includeSummary = incSummary,
                            includeFinancialStatements = incStatements,
                            includeSpecializedReports = incSpecialized,
                            includeFinancialAnalysis = incAnalysis,
                            includeDocumentCatalog = incDocuments,
                            reportTitle = title
                        )
                        onExport(config) { generatedText ->
                            isExporting = false
                            exportPreviewText = generatedText
                            // Open Android share intent
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, generatedText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Compartir Informe Financiero"))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isExporting) "Generando..." else "Generar y Compartir (.csv)",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF151F33),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF10B981),
                    uncheckedColor = Color(0xFF64748B)
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (checked) Color.White else Color(0xFF94A3B8),
                fontSize = 13.sp
            )
        }
    }
}
