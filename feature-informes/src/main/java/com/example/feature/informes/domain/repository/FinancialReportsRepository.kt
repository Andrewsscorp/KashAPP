package com.example.feature.informes.domain.repository

import com.example.feature.informes.domain.model.*
import kotlinx.coroutines.flow.Flow

interface FinancialReportsRepository {
    fun observeSummary(userId: String, filter: FinancialPeriodFilter): Flow<FinancialSummaryData>
    fun observeBalanceSheet(userId: String, filter: FinancialPeriodFilter): Flow<BalanceSheetData>
    fun observeIncomeStatement(userId: String, filter: FinancialPeriodFilter): Flow<IncomeStatementData>
    fun observeCashFlowStatement(userId: String, filter: FinancialPeriodFilter): Flow<CashFlowStatementData>
    fun observeEquityStatement(userId: String, filter: FinancialPeriodFilter): Flow<EquityStatementData>
    fun observeSpecializedReports(userId: String, filter: FinancialPeriodFilter): Flow<SpecializedReportsBundle>
    fun observeFinancialAnalysis(userId: String, filter: FinancialPeriodFilter): Flow<FinancialAnalysisData>

    fun observeAdvancedProjections(userId: String, filter: FinancialPeriodFilter): Flow<AdvancedProjectionsBundle>

    fun observeDocumentSupports(userId: String): Flow<List<DocumentSupportItem>>
    suspend fun addDocumentSupport(doc: DocumentSupportItem)
    suspend fun deleteDocumentSupport(docId: String)
    suspend fun updateDocumentStatus(docId: String, status: SupportVerificationStatus)

    suspend fun getCalculationTrace(metricKey: String, userId: String, filter: FinancialPeriodFilter): CalculationTraceDetail
    suspend fun exportReport(config: ExportReportConfig, filter: FinancialPeriodFilter, userId: String): String
    suspend fun generatePdfReport(config: ExportReportConfig, filter: FinancialPeriodFilter, userId: String): java.io.File
}
