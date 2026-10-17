package com.example.feature.informes.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.feature.informes.presentation.InformesMainTab
import com.example.feature.informes.presentation.InformesUiState
import com.example.feature.informes.presentation.InformesViewModel
import com.example.feature.informes.domain.model.AdvancedProjectionsBundle
import com.example.feature.informes.ui.components.HorizontalScrollHint
import com.example.feature.informes.ui.dialogs.AddDocumentSupportDialog
import com.example.feature.informes.ui.dialogs.CalculationExplanationDialog
import com.example.feature.informes.ui.dialogs.PdfExportPreviewDialog
import com.example.feature.informes.ui.dialogs.PreviewDocumentSupportDialog
import com.example.feature.informes.ui.drilldown.CategoryDrillDownBottomSheet
import com.example.feature.informes.ui.export.ExportReportBottomSheet
import com.example.feature.informes.ui.filter.GlobalFilterBottomSheet
import com.example.feature.informes.ui.tabs.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformesScreen(
    viewModel: InformesViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(uiState.exportSuccessMessage) {
        uiState.exportSuccessMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearExportSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color(0xFF0F172A),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Regresar",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "Informes Financieros",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = uiState.filter.periodLabel,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Filter Trigger Button
                            IconButton(onClick = { viewModel.openFilterModal() }) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filtros",
                                    tint = Color(0xFF38BDF8)
                                )
                            }

                            // Export Button
                            IconButton(onClick = { viewModel.openExportModal() }) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Exportar",
                                    tint = Color(0xFF10B981)
                                )
                            }
                        }
                    }

                    // Horizontal hint for main tabs
                    HorizontalScrollHint(
                        text = "Desliza a la izquierda para ver todas las secciones",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )

                    // 6 Main Tabs (Resumen, Estados Financieros, Informes, Proyecciones y Gastos, Documentos y Soportes, Análisis Financiero)
                    ScrollableTabRow(
                        selectedTabIndex = uiState.activeTab.ordinal,
                        containerColor = Color(0xFF0F172A),
                        contentColor = Color(0xFF38BDF8),
                        edgePadding = 12.dp,
                        indicator = { tabPositions ->
                            val currentTabPos = tabPositions.getOrNull(uiState.activeTab.ordinal)
                            if (currentTabPos != null) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(currentTabPos),
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    ) {
                        InformesMainTab.values().forEach { tab ->
                            val isSelected = uiState.activeTab == tab
                            Tab(
                                selected = isSelected,
                                onClick = { viewModel.selectMainTab(tab) },
                                text = {
                                    Text(
                                        text = tab.label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0B132B),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.activeTab) {
                InformesMainTab.RESUMEN -> {
                    ResumenTab(
                        summary = uiState.summaryData,
                        filter = uiState.filter,
                        currency = uiState.filter.currency,
                        onCategoryClick = { viewModel.openDrillDown(it) },
                        onOpenFilterModal = { viewModel.openFilterModal() },
                        onShowCalculationTrace = { viewModel.showCalculationTrace(it) }
                    )
                }
                InformesMainTab.ESTADOS_FINANCIEROS -> {
                    EstadosFinancierosTab(
                        activeSubTab = uiState.activeStatementSubTab,
                        onSelectSubTab = { viewModel.selectStatementSubTab(it) },
                        balanceSheet = uiState.balanceSheetData,
                        incomeStatement = uiState.incomeStatementData,
                        cashFlow = uiState.cashFlowData,
                        equityStatement = uiState.equityStatementData,
                        currency = uiState.filter.currency,
                        onShowCalculationTrace = { viewModel.showCalculationTrace(it) }
                    )
                }
                InformesMainTab.INFORMES -> {
                    InformesEspecializadosTab(
                        activeReport = uiState.activeSpecializedReport,
                        onSelectReport = { viewModel.selectSpecializedReport(it) },
                        reportsBundle = uiState.specializedReportsData,
                        currency = uiState.filter.currency,
                        onShowCalculationTrace = { viewModel.showCalculationTrace(it) }
                    )
                }
                InformesMainTab.PROYECCIONES_GASTOS -> {
                    uiState.advancedProjections?.let { proj ->
                        ProyeccionesGastosTab(
                            projections = proj,
                            currency = uiState.filter.currency,
                            onShowCalculationTrace = { trace -> viewModel.showCalculationTrace(trace.metricKey) }
                        )
                    } ?: run {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF38BDF8))
                        }
                    }
                }
                InformesMainTab.DOCUMENTOS_SOPORTES -> {
                    DocumentosSoportesTab(
                        documents = uiState.documentsList,
                        currency = uiState.filter.currency,
                        searchQuery = uiState.documentSearchQuery,
                        onSearchQueryChange = { viewModel.setDocumentSearchQuery(it) },
                        selectedTypeFilter = uiState.documentSelectedTypeFilter,
                        onSelectTypeFilter = { viewModel.setDocumentTypeFilter(it) },
                        onOpenAddDocumentModal = { viewModel.openAddDocumentModal() },
                        onSelectPreviewDocument = { viewModel.setPreviewDocument(it) },
                        onDeleteDocument = { viewModel.deleteDocumentSupport(it) },
                        onUpdateStatus = { docId, st -> viewModel.updateDocumentStatus(docId, st) }
                    )
                }
                InformesMainTab.ANALISIS_FINANCIERO -> {
                    AnalisisFinancieroTab(
                        analysis = uiState.analysisData,
                        currency = uiState.filter.currency,
                        onShowCalculationTrace = { viewModel.showCalculationTrace(it) }
                    )
                }
            }
        }
    }

    // Modal: Global Filters
    if (uiState.isFilterModalOpen) {
        GlobalFilterBottomSheet(
            currentFilter = uiState.filter,
            onApplyFilter = { viewModel.updateFilter(it) },
            onDismiss = { viewModel.closeFilterModal() }
        )
    }

    // Modal: Category Drill-Down
    uiState.selectedDrillDownCategory?.let { category ->
        CategoryDrillDownBottomSheet(
            category = category,
            currency = uiState.filter.currency,
            onDismiss = { viewModel.closeDrillDown() }
        )
    }

    // Modal: Export Report
    if (uiState.isExportModalOpen) {
        ExportReportBottomSheet(
            filter = uiState.filter,
            onExport = { config, onDone ->
                viewModel.exportReport(config, onDone)
            },
            onGeneratePdf = { config ->
                viewModel.generatePdf(config)
            },
            onDismiss = { viewModel.closeExportModal() }
        )
    }

    // Modal: Add Document Support
    if (uiState.isAddDocumentModalOpen) {
        AddDocumentSupportDialog(
            onDismiss = { viewModel.closeAddDocumentModal() },
            onConfirm = { title, type, format, amount, entity, notes ->
                viewModel.addDocumentSupport(title, type, format, amount, entity, notes)
            }
        )
    }

    // Modal: Preview Document
    uiState.selectedPreviewDocument?.let { doc ->
        PreviewDocumentSupportDialog(
            doc = doc,
            currency = uiState.filter.currency,
            onDismiss = { viewModel.setPreviewDocument(null) }
        )
    }

    // Modal: Calculation Explanation Pop-up
    uiState.activeCalculationTrace?.let { trace ->
        CalculationExplanationDialog(
            trace = trace,
            onDismiss = { viewModel.closeCalculationTrace() }
        )
    }

    // Modal: PDF Export Preview Pop-up
    uiState.generatedPdfFileForPreview?.let { pdfFile ->
        PdfExportPreviewDialog(
            pdfFile = pdfFile,
            onDismiss = { viewModel.closePdfPreview() }
        )
    }
}
