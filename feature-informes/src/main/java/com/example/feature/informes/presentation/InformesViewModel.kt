package com.example.feature.informes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.informes.domain.model.*
import com.example.feature.informes.domain.repository.FinancialReportsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class InformesViewModel(
    private val repository: FinancialReportsRepository,
    private val userId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(InformesUiState(isLoading = true))
    val uiState: StateFlow<InformesUiState> = _uiState.asStateFlow()
    private var dataCollectionJob: kotlinx.coroutines.Job? = null

    init {
        loadAllData()
    }

    fun selectMainTab(tab: InformesMainTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun selectStatementSubTab(subTab: FinancialStatementSubTab) {
        _uiState.update { it.copy(activeStatementSubTab = subTab) }
    }

    fun selectSpecializedReport(type: SpecializedReportType) {
        _uiState.update { it.copy(activeSpecializedReport = type) }
    }

    fun openFilterModal() {
        _uiState.update { it.copy(isFilterModalOpen = true) }
    }

    fun closeFilterModal() {
        _uiState.update { it.copy(isFilterModalOpen = false) }
    }

    fun openExportModal() {
        _uiState.update { it.copy(isExportModalOpen = true) }
    }

    fun closeExportModal() {
        _uiState.update { it.copy(isExportModalOpen = false) }
    }

    fun openDrillDown(category: DistributionCategoryItem) {
        _uiState.update { it.copy(selectedDrillDownCategory = category) }
    }

    fun closeDrillDown() {
        _uiState.update { it.copy(selectedDrillDownCategory = null) }
    }

    fun openAddDocumentModal() {
        _uiState.update { it.copy(isAddDocumentModalOpen = true) }
    }

    fun closeAddDocumentModal() {
        _uiState.update { it.copy(isAddDocumentModalOpen = false) }
    }

    fun setPreviewDocument(doc: DocumentSupportItem?) {
        _uiState.update { it.copy(selectedPreviewDocument = doc) }
    }

    fun setDocumentSearchQuery(query: String) {
        _uiState.update { it.copy(documentSearchQuery = query) }
    }

    fun setDocumentTypeFilter(type: DocumentSupportType?) {
        _uiState.update { it.copy(documentSelectedTypeFilter = type) }
    }

    fun updateFilter(newFilter: FinancialPeriodFilter) {
        _uiState.update { it.copy(filter = newFilter, isFilterModalOpen = false, isLoading = true) }
        loadAllData()
    }

    fun setPeriodPreset(preset: PeriodPreset) {
        val now = LocalDate.now()
        val (start, end) = when (preset) {
            PeriodPreset.THIS_MONTH -> Pair(now.withDayOfMonth(1), now)
            PeriodPreset.LAST_MONTH -> {
                val prev = now.minusMonths(1)
                Pair(prev.withDayOfMonth(1), prev.withDayOfMonth(prev.lengthOfMonth()))
            }
            PeriodPreset.LAST_3_MONTHS -> Pair(now.minusMonths(3).withDayOfMonth(1), now)
            PeriodPreset.LAST_6_MONTHS -> Pair(now.minusMonths(6).withDayOfMonth(1), now)
            PeriodPreset.THIS_YEAR -> Pair(now.withDayOfYear(1), now)
            PeriodPreset.LAST_YEAR -> {
                val prevYear = now.minusYears(1)
                Pair(prevYear.withDayOfYear(1), prevYear.withDayOfYear(prevYear.lengthOfYear()))
            }
            PeriodPreset.CUSTOM -> Pair(_uiState.value.filter.startDate, _uiState.value.filter.endDate)
        }
        val updated = _uiState.value.filter.copy(preset = preset, startDate = start, endDate = end)
        updateFilter(updated)
    }

    fun addDocumentSupport(
        title: String,
        type: DocumentSupportType,
        format: DocumentFileFormat,
        amount: Double?,
        entityName: String,
        notes: String
    ) {
        viewModelScope.launch {
            val doc = DocumentSupportItem(
                id = "doc-${System.currentTimeMillis()}",
                userId = userId,
                title = title,
                type = type,
                fileFormat = format,
                date = LocalDate.now().toString(),
                relatedAmount = amount,
                relatedEntityType = type.label,
                relatedEntityName = entityName.ifBlank { "General" },
                tags = listOf(type.label, format.extension),
                status = SupportVerificationStatus.VERIFICADO,
                notes = notes.ifBlank { "Documento registrado y validado en el centro documental." }
            )
            repository.addDocumentSupport(doc)
            closeAddDocumentModal()
        }
    }

    fun deleteDocumentSupport(docId: String) {
        viewModelScope.launch {
            repository.deleteDocumentSupport(docId)
        }
    }

    fun updateDocumentStatus(docId: String, status: SupportVerificationStatus) {
        viewModelScope.launch {
            repository.updateDocumentStatus(docId, status)
        }
    }

    fun showCalculationTrace(metricKey: String) {
        viewModelScope.launch {
            try {
                val trace = repository.getCalculationTrace(metricKey, userId, _uiState.value.filter)
                _uiState.update { it.copy(activeCalculationTrace = trace) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error obteniendo cálculo: ${e.message}") }
            }
        }
    }

    fun closeCalculationTrace() {
        _uiState.update { it.copy(activeCalculationTrace = null) }
    }

    fun generatePdf(config: ExportReportConfig) {
        viewModelScope.launch {
            try {
                val pdfFile = repository.generatePdfReport(config, _uiState.value.filter, userId)
                _uiState.update {
                    it.copy(
                        isExportModalOpen = false,
                        generatedPdfFileForPreview = pdfFile,
                        exportSuccessMessage = "PDF generado exitosamente (${pdfFile.name})"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error generando PDF: ${e.message}") }
            }
        }
    }

    fun closePdfPreview() {
        _uiState.update { it.copy(generatedPdfFileForPreview = null) }
    }

    fun exportReport(config: ExportReportConfig, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val exportedText = repository.exportReport(config, _uiState.value.filter, userId)
                _uiState.update {
                    it.copy(
                        isExportModalOpen = false,
                        exportSuccessMessage = "Reporte exportado exitosamente en formato ${config.format.label}"
                    )
                }
                onComplete(exportedText)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al exportar reporte: ${e.message}") }
            }
        }
    }

    fun clearExportSuccessMessage() {
        _uiState.update { it.copy(exportSuccessMessage = null) }
    }

    private fun loadAllData() {
        val currentFilter = _uiState.value.filter
        dataCollectionJob?.cancel()
        dataCollectionJob = viewModelScope.launch {
            launch {
                repository.observeSummary(userId, currentFilter).collectLatest { summary ->
                    _uiState.update { it.copy(summaryData = summary, isLoading = false) }
                }
            }
            launch {
                repository.observeBalanceSheet(userId, currentFilter).collectLatest { balance ->
                    _uiState.update { it.copy(balanceSheetData = balance) }
                }
            }
            launch {
                repository.observeIncomeStatement(userId, currentFilter).collectLatest { inc ->
                    _uiState.update { it.copy(incomeStatementData = inc) }
                }
            }
            launch {
                repository.observeCashFlowStatement(userId, currentFilter).collectLatest { cf ->
                    _uiState.update { it.copy(cashFlowData = cf) }
                }
            }
            launch {
                repository.observeEquityStatement(userId, currentFilter).collectLatest { eq ->
                    _uiState.update { it.copy(equityStatementData = eq) }
                }
            }
            launch {
                repository.observeSpecializedReports(userId, currentFilter).collectLatest { spec ->
                    _uiState.update { it.copy(specializedReportsData = spec) }
                }
            }
            launch {
                repository.observeFinancialAnalysis(userId, currentFilter).collectLatest { analysis ->
                    _uiState.update { it.copy(analysisData = analysis) }
                }
            }
            launch {
                repository.observeAdvancedProjections(userId, currentFilter).collectLatest { proj ->
                    _uiState.update { it.copy(advancedProjections = proj) }
                }
            }
            launch {
                repository.observeDocumentSupports(userId).collectLatest { docs ->
                    _uiState.update { it.copy(documentsList = docs) }
                }
            }
        }
    }
}
