package com.example.feature.informes.presentation

import com.example.feature.informes.domain.model.*
import java.io.File

enum class InformesMainTab(val label: String, val iconName: String) {
    RESUMEN("Resumen", "Dashboard"),
    ESTADOS_FINANCIEROS("Estados Financieros", "AccountBalance"),
    INFORMES("Informes", "Assessment"),
    PROYECCIONES_GASTOS("Proyecciones y Gastos", "TrendingUp"),
    DOCUMENTOS_SOPORTES("Documentos y Soportes", "Description"),
    ANALISIS_FINANCIERO("Análisis Financiero", "Insights")
}

enum class FinancialStatementSubTab(val label: String) {
    BALANCE_GENERAL("Situación Financiera"),
    ESTADO_RESULTADOS("Resultados"),
    FLUJO_EFECTIVO("Flujo de Efectivo"),
    CAMBIOS_PATRIMONIO("Cambios en Patrimonio")
}

data class InformesUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val activeTab: InformesMainTab = InformesMainTab.RESUMEN,
    val filter: FinancialPeriodFilter = FinancialPeriodFilter(),
    val activeStatementSubTab: FinancialStatementSubTab = FinancialStatementSubTab.BALANCE_GENERAL,
    val activeSpecializedReport: SpecializedReportType = SpecializedReportType.INCOME,

    // Data Models
    val summaryData: FinancialSummaryData = FinancialSummaryData(),
    val balanceSheetData: BalanceSheetData = BalanceSheetData(),
    val incomeStatementData: IncomeStatementData = IncomeStatementData(),
    val cashFlowData: CashFlowStatementData = CashFlowStatementData(),
    val equityStatementData: EquityStatementData = EquityStatementData(),
    val specializedReportsData: SpecializedReportsBundle = SpecializedReportsBundle(),
    val analysisData: FinancialAnalysisData = FinancialAnalysisData(),
    val advancedProjections: AdvancedProjectionsBundle? = null,
    val documentsList: List<DocumentSupportItem> = emptyList(),

    // UI Dialog & Drill-down states
    val isFilterModalOpen: Boolean = false,
    val isExportModalOpen: Boolean = false,
    val isAddDocumentModalOpen: Boolean = false,
    val selectedDrillDownCategory: DistributionCategoryItem? = null,
    val selectedPreviewDocument: DocumentSupportItem? = null,
    val activeCalculationTrace: CalculationTraceDetail? = null,
    val generatedPdfFileForPreview: File? = null,
    val documentSearchQuery: String = "",
    val documentSelectedTypeFilter: DocumentSupportType? = null,
    val exportSuccessMessage: String? = null
)
