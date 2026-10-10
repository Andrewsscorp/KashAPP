package com.example.feature.informes.domain.model

enum class ExportReportFormat(val label: String, val extension: String, val mimeType: String) {
    EXCEL_CSV("Planilla Excel / CSV (.csv)", "csv", "text/csv"),
    PDF("Documento PDF Ejecutivo (.pdf)", "pdf", "application/pdf")
}

data class ExportReportConfig(
    val format: ExportReportFormat = ExportReportFormat.EXCEL_CSV,
    val includeSummary: Boolean = true,
    val includeFinancialStatements: Boolean = true,
    val includeSpecializedReports: Boolean = true,
    val includeFinancialAnalysis: Boolean = true,
    val includeDocumentCatalog: Boolean = false,
    val reportTitle: String = "Informe Financiero Central - KashApp",
    val authorOrCompany: String = "Usuario KashApp",
    val customNotes: String = "Informe generado con filtros contables y consolidación de cuentas, activos e inversiones."
)
