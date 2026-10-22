package com.example.feature.informes.data.repository

import android.content.Context
import com.example.data.local.dao.FinancialDao
import com.example.data.local.dao.FixedAssetDao
import com.example.feature.informes.data.document.DocumentSupportManager
import com.example.feature.informes.data.export.FinancialReportExportEngine
import com.example.feature.informes.data.export.PdfReportGenerator
import com.example.feature.informes.domain.engine.CalculationTraceEngine
import com.example.feature.informes.domain.engine.FinancialCalculationEngine
import com.example.feature.informes.domain.engine.PredictiveExpenseEngine
import com.example.feature.informes.domain.model.*
import com.example.feature.informes.domain.repository.FinancialReportsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

class FinancialReportsRepositoryImpl(
    private val context: Context,
    private val financialDao: FinancialDao,
    private val fixedAssetDao: FixedAssetDao,
    private val sharedExpenseDao: com.example.data.local.dao.SharedExpenseDao? = null,
    private val calculationEngine: FinancialCalculationEngine = FinancialCalculationEngine(),
    private val predictiveEngine: PredictiveExpenseEngine = PredictiveExpenseEngine(),
    private val traceEngine: CalculationTraceEngine = CalculationTraceEngine(),
    private val documentSupportManager: DocumentSupportManager = DocumentSupportManager(),
    private val exportEngine: FinancialReportExportEngine = FinancialReportExportEngine(),
    private val pdfGenerator: PdfReportGenerator = PdfReportGenerator(context)
) : FinancialReportsRepository {

    private fun observeSharedSnapshot(userId: String): Flow<com.example.feature.informes.domain.engine.SharedAccountingStatementIntegrator.SharedAccountingSnapshot> {
        val dao = sharedExpenseDao
            ?: return kotlinx.coroutines.flow.flowOf(com.example.feature.informes.domain.engine.SharedAccountingStatementIntegrator.SharedAccountingSnapshot())
        return combine(
            dao.observeTotalAccountsReceivableNiif(userId),
            dao.observeTotalAccountsPayableNiif(userId),
            dao.observeTotalMentalHoldReserved(userId),
            dao.observeSharedExpensesWithDetails(userId)
        ) { rec, pay, hold, details ->
            val activeCodes = details
                .filter { it.expense.status != "VOIDED" && it.expense.status != "SETTLED" }
                .map { it.expense.consecutiveCode }
            val openRecCount = details.count { d ->
                d.expense.status != "VOIDED" && d.splits.any { it.participantId != "SELF_USER" && it.remainingPendingAmount > 0.01 }
            }
            val openPayCount = details.count { d ->
                d.expense.status != "VOIDED" && d.splits.any { it.participantId == "SELF_USER" && it.remainingPendingAmount > 0.01 }
            }
            com.example.feature.informes.domain.engine.SharedAccountingStatementIntegrator.SharedAccountingSnapshot(
                activeAccountsReceivableNiif = rec,
                activeAccountsPayableNiif = pay,
                totalMentalHoldReserved = hold,
                openReceivableTicketsCount = openRecCount,
                openPayableTicketsCount = openPayCount,
                traceConsecutiveCodes = activeCodes
            )
        }
    }

    override fun observeSummary(
        userId: String,
        filter: FinancialPeriodFilter
    ): Flow<FinancialSummaryData> {
        val baseFlow = combine(
            financialDao.getAccountsFlow(userId),
            financialDao.getTransactionsFlow(userId),
            fixedAssetDao.observeAssets(userId),
            financialDao.getInvestmentsFlow(userId),
            financialDao.getAccountCreditCardsByUserFlow(userId)
        ) { accounts, transactions, assets, investments, creditCards ->
             Quintuple(accounts, transactions, assets, investments, creditCards)
        }
        return combine(baseFlow, observeSharedSnapshot(userId)) { q, sharedSnap ->
            calculationEngine.computeSummary(
                accounts = q.first,
                transactions = q.second,
                assets = q.third,
                investments = q.fourth,
                creditCards = q.fifth,
                filter = filter,
                sharedSnapshot = sharedSnap
            )
        }
    }

    override fun observeBalanceSheet(
        userId: String,
        filter: FinancialPeriodFilter
    ): Flow<BalanceSheetData> {
        val baseFlow = combine(
            observeSummary(userId, filter),
            financialDao.getAccountsFlow(userId),
            fixedAssetDao.observeAssets(userId),
            financialDao.getInvestmentsFlow(userId),
            financialDao.getAccountCreditCardsByUserFlow(userId)
        ) { summary, accounts, assets, investments, creditCards ->
            Quintuple(summary, accounts, assets, investments, creditCards)
        }
        return combine(baseFlow, observeSharedSnapshot(userId)) { q, sharedSnap ->
            calculationEngine.computeBalanceSheet(
                summary = q.first,
                accounts = q.second,
                assets = q.third,
                investments = q.fourth,
                creditCards = q.fifth,
                sharedSnapshot = sharedSnap
            )
        }
    }

    private data class Quintuple<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )

    override fun observeIncomeStatement(
        userId: String,
        filter: FinancialPeriodFilter
    ): Flow<IncomeStatementData> {
        return observeSummary(userId, filter).map { summary ->
            calculationEngine.computeIncomeStatement(summary)
        }
    }

    override fun observeCashFlowStatement(
        userId: String,
        filter: FinancialPeriodFilter
    ): Flow<CashFlowStatementData> {
        return combine(
            observeSummary(userId, filter),
            financialDao.getAccountsFlow(userId),
            financialDao.getTransactionsFlow(userId)
        ) { summary, accounts, transactions ->
            val periodTx = calculationEngine.filterTransactionsByDate(transactions, filter.startDate, filter.endDate)
            calculationEngine.computeCashFlow(summary, accounts, periodTx)
        }
    }

    override fun observeEquityStatement(
        userId: String,
        filter: FinancialPeriodFilter
    ): Flow<EquityStatementData> {
        return combine(
            observeSummary(userId, filter),
            observeBalanceSheet(userId, filter),
            financialDao.getTransactionsFlow(userId)
        ) { summary, balanceSheet, transactions ->
            val periodTx = calculationEngine.filterTransactionsByDate(transactions, filter.startDate, filter.endDate)
            calculationEngine.computeEquityStatement(summary, balanceSheet, periodTx)
        }
    }

    override fun observeSpecializedReports(
        userId: String,
        filter: FinancialPeriodFilter
    ): Flow<SpecializedReportsBundle> {
        val baseFlow = combine(
            financialDao.getAccountsFlow(userId),
            fixedAssetDao.observeAssets(userId),
            financialDao.getInvestmentsFlow(userId),
            financialDao.getAccountCreditCardsByUserFlow(userId),
            observeIncomeStatement(userId, filter)
        ) { accounts, assets, investments, creditCards, incomeStatement ->
            Quintuple(accounts, assets, investments, creditCards, incomeStatement)
        }
        return combine(baseFlow, financialDao.getTransactionsFlow(userId)) { q, transactions ->
            val periodTx = calculationEngine.filterTransactionsByDate(transactions, filter.startDate, filter.endDate)
            calculationEngine.computeSpecializedReports(
                accounts = q.first,
                assets = q.second,
                investments = q.third,
                creditCards = q.fourth,
                incomeStatement = q.fifth,
                periodTransactions = periodTx
            )
        }
    }

    override fun observeFinancialAnalysis(
        userId: String,
        filter: FinancialPeriodFilter
    ): Flow<FinancialAnalysisData> {
        return combine(
            observeSummary(userId, filter),
            observeBalanceSheet(userId, filter)
        ) { summary, balanceSheet ->
            calculationEngine.computeFinancialAnalysis(summary, balanceSheet)
        }
    }

    override fun observeAdvancedProjections(
        userId: String,
        filter: FinancialPeriodFilter
    ): Flow<AdvancedProjectionsBundle> {
        return combine(
            financialDao.getAccountsFlow(userId),
            financialDao.getTransactionsFlow(userId),
            observeSummary(userId, filter)
        ) { accounts, transactions, summary ->
            val periodTx = calculationEngine.filterTransactionsByDate(transactions, filter.startDate, filter.endDate)
            predictiveEngine.computeAdvancedProjections(
                accounts = accounts,
                transactions = periodTx,
                currentNetWorth = summary.netWorth,
                currentIncome = summary.totalIncome,
                currentExpenses = summary.totalExpenses
            )
        }
    }

    override fun observeDocumentSupports(userId: String): Flow<List<DocumentSupportItem>> {
        return documentSupportManager.observeDocuments(userId)
    }

    override suspend fun addDocumentSupport(doc: DocumentSupportItem) {
        documentSupportManager.addDocument(doc)
    }

    override suspend fun deleteDocumentSupport(docId: String) {
        documentSupportManager.deleteDocument(docId)
    }

    override suspend fun updateDocumentStatus(docId: String, status: SupportVerificationStatus) {
        documentSupportManager.updateStatus(docId, status)
    }

    override suspend fun getCalculationTrace(
        metricKey: String,
        userId: String,
        filter: FinancialPeriodFilter
    ): CalculationTraceDetail {
        val accounts = financialDao.getAccounts(userId)
        val transactions = financialDao.getTransactions(userId)
        val assets = fixedAssetDao.getAssets(userId)
        val investments = financialDao.getInvestments(userId)
        val creditCards = financialDao.getAccountCreditCardsByUser(userId)
        val sharedSnapshot = observeSharedSnapshot(userId).first()
        val periodTx = calculationEngine.filterTransactionsByDate(transactions, filter.startDate, filter.endDate)

        val summary = calculationEngine.computeSummary(accounts, transactions, assets, investments, creditCards, filter, sharedSnapshot)
        val balanceSheet = calculationEngine.computeBalanceSheet(summary, accounts, assets, investments, creditCards, sharedSnapshot)
        val incomeStatement = calculationEngine.computeIncomeStatement(summary)
        val cashFlow = calculationEngine.computeCashFlow(summary, accounts, periodTx)
        val analysis = calculationEngine.computeFinancialAnalysis(summary, balanceSheet)

        return traceEngine.getTraceForMetric(metricKey, summary, balanceSheet, incomeStatement, cashFlow, analysis, sharedSnapshot)
    }

    override suspend fun exportReport(
        config: ExportReportConfig,
        filter: FinancialPeriodFilter,
        userId: String
    ): String {
        val accounts = financialDao.getAccounts(userId)
        val transactions = financialDao.getTransactions(userId)
        val assets = fixedAssetDao.getAssets(userId)
        val investments = financialDao.getInvestments(userId)
        val creditCards = financialDao.getAccountCreditCardsByUser(userId)
        val sharedSnapshot = observeSharedSnapshot(userId).first()
        val periodTx = calculationEngine.filterTransactionsByDate(transactions, filter.startDate, filter.endDate)

        val summary = calculationEngine.computeSummary(accounts, transactions, assets, investments, creditCards, filter, sharedSnapshot)
        val balanceSheet = calculationEngine.computeBalanceSheet(summary, accounts, assets, investments, creditCards, sharedSnapshot)
        val incomeStatement = calculationEngine.computeIncomeStatement(summary)
        val cashFlow = calculationEngine.computeCashFlow(summary, accounts, periodTx)
        val equityStatement = calculationEngine.computeEquityStatement(summary, balanceSheet, periodTx)
        val specialized = calculationEngine.computeSpecializedReports(accounts, assets, investments, creditCards, incomeStatement, periodTx)
        val analysis = calculationEngine.computeFinancialAnalysis(summary, balanceSheet)

        return if (config.format == ExportReportFormat.EXCEL_CSV) {
            exportEngine.generateCsv(summary, balanceSheet, incomeStatement, specialized)
        } else {
            exportEngine.generateReportText(config, filter, summary, balanceSheet, incomeStatement, cashFlow, equityStatement, specialized, analysis)
        }
    }

    override suspend fun generatePdfReport(
        config: ExportReportConfig,
        filter: FinancialPeriodFilter,
        userId: String
    ): File {
        val accounts = financialDao.getAccounts(userId)
        val transactions = financialDao.getTransactions(userId)
        val assets = fixedAssetDao.getAssets(userId)
        val investments = financialDao.getInvestments(userId)
        val creditCards = financialDao.getAccountCreditCardsByUser(userId)
        val sharedSnapshot = observeSharedSnapshot(userId).first()
        val periodTx = calculationEngine.filterTransactionsByDate(transactions, filter.startDate, filter.endDate)

        val summary = calculationEngine.computeSummary(accounts, transactions, assets, investments, creditCards, filter, sharedSnapshot)
        val balanceSheet = calculationEngine.computeBalanceSheet(summary, accounts, assets, investments, creditCards, sharedSnapshot)
        val incomeStatement = calculationEngine.computeIncomeStatement(summary)
        val cashFlow = calculationEngine.computeCashFlow(summary, accounts, periodTx)
        val analysis = calculationEngine.computeFinancialAnalysis(summary, balanceSheet)

        return pdfGenerator.generateExecutivePdf(config, filter, summary, balanceSheet, incomeStatement, cashFlow, analysis)
    }
}
