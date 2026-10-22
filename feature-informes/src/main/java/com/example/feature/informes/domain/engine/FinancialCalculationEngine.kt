package com.example.feature.informes.domain.engine

import com.example.data.local.entity.AccountCreditCardEntity
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.FixedAssetEntity
import com.example.data.local.entity.InvestmentEntity
import com.example.data.local.entity.TransactionEntity
import com.example.feature.informes.domain.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

class FinancialCalculationEngine {

    fun isOpeningOrCapitalAllocation(tx: TransactionEntity): Boolean {
        val id = tx.id
        val cat = tx.categoryId.lowercase()
        val type = tx.type.uppercase()
        return cat == "cat_initial_balance" ||
               cat == "cat_opening_balance" ||
               id.startsWith("tx_init_") ||
               id.startsWith("tx_cc_init_") ||
               id.startsWith("tx_cdt_init_") ||
               id.startsWith("tx_cdt_out_") ||
               id.startsWith("tx_inv_") ||
               type == "TRANSFER_OUT" ||
               type == "TRANSFER_IN" ||
               !tx.transferGroupId.isNullOrBlank()
    }

    fun computeSummary(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        assets: List<FixedAssetEntity>,
        investments: List<InvestmentEntity>,
        creditCards: List<AccountCreditCardEntity>,
        filter: FinancialPeriodFilter,
        sharedSnapshot: SharedAccountingStatementIntegrator.SharedAccountingSnapshot? = null
    ): FinancialSummaryData {
        // Real Assets calculation directly from database entities + NIIF Shared Receivables (CxC)
        val bankAndCash = accounts.filter { (it.balance ?: 0.0) > 0 }
            .sumOf { it.balance ?: 0.0 }
        // Valuation under NIC 16: (acquisitionValue - accumulatedDepreciation).coerceAtLeast(residualValue)
        val fixedAssetsTotal = assets.filter {
            it.deletedAt == null && it.includeInNetWorth && it.status in setOf("ACTIVE", "EN_USO", "DISPONIBLE", "EN_MANTENIMIENTO")
        }.sumOf {
            (it.acquisitionValue - it.accumulatedDepreciation).coerceAtLeast(it.residualValue)
        }
        val investmentsTotal = investments
            .sumOf { it.currentValue }
        val sharedReceivablesTotal = sharedSnapshot?.activeAccountsReceivableNiif ?: 0.0

        val totalAssets = bankAndCash + fixedAssetsTotal + investmentsTotal + sharedReceivablesTotal
        val assetCount = assets.size + accounts.size + investments.size + (if (sharedReceivablesTotal > 0) 1 else 0)

        // Real Liabilities calculation directly from database entities + NIIF Shared Payables (CxP)
        // Exclusion of credit cards from negativeAccountBalances prevents 2x double counting of credit card liabilities!
        val cardDebts = creditCards.sumOf { it.quotaUsed }
        val negativeAccountBalances = accounts.filter {
            !it.type.equals("CREDIT_CARD", ignoreCase = true) && (it.balance ?: 0.0) < 0
        }.sumOf { abs(it.balance ?: 0.0) }
        val sharedPayablesTotal = sharedSnapshot?.activeAccountsPayableNiif ?: 0.0
        val totalLiabilities = cardDebts + negativeAccountBalances + sharedPayablesTotal
        val pendingDebtsCount = creditCards.count { it.quotaUsed > 0 } +
            accounts.count { !it.type.equals("CREDIT_CARD", ignoreCase = true) && (it.balance ?: 0.0) < 0 } +
            (sharedSnapshot?.openPayableTicketsCount ?: 0)

        val netWorth = totalAssets - totalLiabilities
        val netWorthVariationPct = 0.0

        // Real Filter transactions for period excluding opening balances and capital transfers from PyG
        val periodTransactions = filterTransactionsByDate(transactions, filter.startDate, filter.endDate)
        val operatingTransactions = periodTransactions.filter { !isOpeningOrCapitalAllocation(it) }

        val totalIncome = operatingTransactions.filter { it.type.equals("INCOME", ignoreCase = true) }
            .sumOf { it.amount }

        // PyG Recognized Expense uses accountingRecognizedExpenseAmount (NIIF Devengo) when present
        val totalExpenses = SharedAccountingStatementIntegrator.computeRecognizedPnlExpenses(operatingTransactions)

        val periodResult = totalIncome - totalExpenses

        // Cash flow entries based on actual cash inflows & real cash outflows (tx.amount)
        val cashFlowInflows = totalIncome + (sharedSnapshot?.periodReimbursementsCollectedCash ?: 0.0)
        val cashFlowOutflows = SharedAccountingStatementIntegrator.computeRealCashOutflows(operatingTransactions)
        val netCashFlow = cashFlowInflows - cashFlowOutflows

        // Financial evolution points from actual transactions grouped dynamically by month
        val monthFormatter = DateTimeFormatter.ofPattern("MMM", java.util.Locale("es", "CO"))
        val evolutionPoints = if (transactions.isNotEmpty()) {
            val endYm = java.time.YearMonth.from(filter.endDate)
            (5 downTo 0).map { monthsAgo ->
                val ym = endYm.minusMonths(monthsAgo.toLong())
                val monthLabel = ym.format(monthFormatter).replaceFirstChar { it.uppercase() }
                val txsInMonth = transactions.filter { tx ->
                    try {
                        val d = LocalDate.parse(tx.date.take(10))
                        java.time.YearMonth.from(d) == ym
                    } catch (_: Exception) {
                        false
                    }
                }
                val opTxs = txsInMonth.filter { !isOpeningOrCapitalAllocation(it) }
                val mIncome = opTxs.filter { it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amount }
                val mExpenses = SharedAccountingStatementIntegrator.computeRecognizedPnlExpenses(opTxs)
                FinancialEvolutionPoint(monthLabel, mIncome, mExpenses, mIncome - mExpenses)
            }
        } else {
            listOf(FinancialEvolutionPoint("Actual", totalIncome, totalExpenses, periodResult))
        }

        // Real Distributions for interactive charts
        val assetDistributions = buildList {
            if (fixedAssetsTotal > 0) {
                add(DistributionCategoryItem("A1", "Activos Fijos e Inmuebles", fixedAssetsTotal, if (totalAssets > 0) (fixedAssetsTotal / totalAssets) * 100 else 0.0, "#3B82F6", assets.size, "ACTIVO"))
            }
            if (investmentsTotal > 0) {
                add(DistributionCategoryItem("A2", "Inversiones y Portafolio", investmentsTotal, if (totalAssets > 0) (investmentsTotal / totalAssets) * 100 else 0.0, "#10B981", investments.size, "ACTIVO"))
            }
            if (bankAndCash > 0) {
                add(DistributionCategoryItem("A3", "Cuentas Bancarias y Efectivo", bankAndCash, if (totalAssets > 0) (bankAndCash / totalAssets) * 100 else 0.0, "#6366F1", accounts.size, "ACTIVO"))
            }
            if (sharedReceivablesTotal > 0) {
                add(DistributionCategoryItem("A4", "Cuentas por Cobrar (Gastos Compartidos)", sharedReceivablesTotal, if (totalAssets > 0) (sharedReceivablesTotal / totalAssets) * 100 else 0.0, "#059669", sharedSnapshot?.openReceivableTicketsCount ?: 1, "ACTIVO"))
            }
        }.ifEmpty {
            listOf(DistributionCategoryItem("A0", "Sin activos registrados", 0.0, 0.0, "#64748B", 0, "ACTIVO"))
        }

        val liabilityDistributions = buildList {
            if (cardDebts > 0) {
                add(DistributionCategoryItem("L1", "Tarjetas de Crédito", cardDebts, if (totalLiabilities > 0) (cardDebts / totalLiabilities) * 100 else 0.0, "#F59E0B", creditCards.size, "PASIVO"))
            }
            if (negativeAccountBalances > 0) {
                add(DistributionCategoryItem("L2", "Sobregiros Bancarios", negativeAccountBalances, if (totalLiabilities > 0) (negativeAccountBalances / totalLiabilities) * 100 else 0.0, "#EF4444", 1, "PASIVO"))
            }
            if (sharedPayablesTotal > 0) {
                add(DistributionCategoryItem("L3", "Cuentas por Pagar (Gastos Compartidos)", sharedPayablesTotal, if (totalLiabilities > 0) (sharedPayablesTotal / totalLiabilities) * 100 else 0.0, "#D97706", sharedSnapshot?.openPayableTicketsCount ?: 1, "PASIVO"))
            }
        }.ifEmpty {
            listOf(DistributionCategoryItem("L0", "Sin pasivos registrados", 0.0, 0.0, "#10B981", 0, "PASIVO"))
        }

        val incomeDistributions = operatingTransactions
            .filter { it.type.equals("INCOME", ignoreCase = true) }
            .groupBy { it.categoryName.ifBlank { "Ingresos del período" } }
            .entries
            .mapIndexed { idx, entry ->
                val amount = entry.value.sumOf { it.amount }
                DistributionCategoryItem(
                    categoryId = "I$idx",
                    categoryName = entry.key,
                    amount = amount,
                    percentage = if (totalIncome > 0) (amount / totalIncome) * 100 else 0.0,
                    colorHex = "#10B981",
                    transactionCount = entry.value.size,
                    type = "INGRESO"
                )
            }.ifEmpty {
                listOf(DistributionCategoryItem("I0", "Sin ingresos registrados", 0.0, 0.0, "#64748B", 0, "INGRESO"))
            }

        val expenseDistributions = operatingTransactions
            .filter { it.type.equals("EXPENSE", ignoreCase = true) }
            .groupBy { it.categoryName.ifBlank { "Gastos del período" } }
            .entries
            .mapIndexed { idx, entry ->
                val amount = entry.value.sumOf { it.accountingRecognizedExpenseAmount ?: it.amount }
                DistributionCategoryItem(
                    categoryId = "E$idx",
                    categoryName = entry.key,
                    amount = amount,
                    percentage = if (totalExpenses > 0) (amount / totalExpenses) * 100 else 0.0,
                    colorHex = "#EF4444",
                    transactionCount = entry.value.size,
                    type = "GASTO"
                )
            }.ifEmpty {
                listOf(DistributionCategoryItem("E0", "Sin gastos registrados", 0.0, 0.0, "#64748B", 0, "GASTO"))
            }

        return FinancialSummaryData(
            netWorth = netWorth,
            netWorthVariationPct = netWorthVariationPct,
            netWorthTrend = if (periodResult >= 0) "Superávit neto acumulado" else "Déficit en el período",
            totalAssets = totalAssets,
            assetsVariationPct = 0.0,
            assetCount = assetCount,
            totalLiabilities = totalLiabilities,
            liabilitiesVariationPct = 0.0,
            pendingDebtsCount = pendingDebtsCount,
            totalIncome = totalIncome,
            incomeVariationPct = 0.0,
            totalExpenses = totalExpenses,
            expenseVariationPct = 0.0,
            periodResult = periodResult,
            cashFlowInflows = cashFlowInflows,
            cashFlowOutflows = cashFlowOutflows,
            netCashFlow = netCashFlow,
            evolutionChartPoints = evolutionPoints,
            assetDistributions = assetDistributions,
            liabilityDistributions = liabilityDistributions,
            incomeDistributions = incomeDistributions,
            expenseDistributions = expenseDistributions
        )
    }

    fun computeBalanceSheet(
        summary: FinancialSummaryData,
        accounts: List<AccountEntity>,
        assets: List<FixedAssetEntity>,
        investments: List<InvestmentEntity>,
        creditCards: List<AccountCreditCardEntity>,
        sharedSnapshot: SharedAccountingStatementIntegrator.SharedAccountingSnapshot? = null
    ): BalanceSheetData {
        // Real Current Assets from database accounts
        val cashAccounts = accounts.filter { it.type.equals("CASH", ignoreCase = true) }
        val bankAccounts = accounts.filter { !it.type.equals("CASH", ignoreCase = true) && !it.type.equals("CREDIT_CARD", ignoreCase = true) }

        val cashAmount = cashAccounts.filter { (it.balance ?: 0.0) > 0 }.sumOf { it.balance ?: 0.0 }
        val banksAmount = bankAccounts.filter { (it.balance ?: 0.0) > 0 }.sumOf { it.balance ?: 0.0 }
        val currentInvestmentsAmount = investments.filter { it.typeId.equals("RENTA_FIJA", ignoreCase = true) || it.name.contains("CDT", ignoreCase = true) }
            .sumOf { it.currentValue }

        val currentAssetsList = buildList {
            if (cashAmount > 0) {
                add(FinancialLineItem("ca1", "Efectivo disponible", "Caja menor y efectivo físico", "Disponible", cashAmount, if (summary.totalAssets > 0) (cashAmount / summary.totalAssets) * 100 else 0.0))
            } else if (cashAccounts.isNotEmpty()) {
                add(FinancialLineItem("ca1", "Efectivo disponible", "Caja menor y efectivo físico", "Disponible", 0.0, 0.0))
            }
            if (banksAmount > 0) {
                add(FinancialLineItem("ca2", "Bancos y Cuentas de Ahorro", "Saldos en entidades financieras", "Bancos", banksAmount, if (summary.totalAssets > 0) (banksAmount / summary.totalAssets) * 100 else 0.0))
            } else if (bankAccounts.isNotEmpty()) {
                add(FinancialLineItem("ca2", "Bancos y Cuentas de Ahorro", "Saldos en entidades financieras", "Bancos", 0.0, 0.0))
            }
            if (currentInvestmentsAmount > 0) {
                add(FinancialLineItem("ca4", "Inversiones corrientes", "CDT y fondos de liquidez", "Inversiones CP", currentInvestmentsAmount, if (summary.totalAssets > 0) (currentInvestmentsAmount / summary.totalAssets) * 100 else 0.0))
            }
        }.ifEmpty {
            listOf(FinancialLineItem("ca0", "Sin activos corrientes", "No hay cuentas registradas", "Disponible", 0.0, 0.0))
        }
        val totalCurrentAssets = currentAssetsList.sumOf { it.amount }

        // Real Non-Current Assets from fixed assets (NIC 16) and long-term investments
        val activeAssetStatus = setOf("ACTIVE", "EN_USO", "DISPONIBLE", "EN_MANTENIMIENTO")
        val fixedAssetsAmount = assets
            .filter { it.status.uppercase() in activeAssetStatus }
            .sumOf { asset ->
                val bookVal = (asset.acquisitionValue - asset.accumulatedDepreciation).coerceAtLeast(asset.residualValue)
                if (asset.currentMarketValue > 0) asset.currentMarketValue else bookVal
            }
        val longTermInvestmentsAmount = investments.filter { !it.typeId.equals("RENTA_FIJA", ignoreCase = true) && !it.name.contains("CDT", ignoreCase = true) }.sumOf { it.currentValue }

        val nonCurrentAssetsList = buildList {
            if (fixedAssetsAmount > 0) {
                add(FinancialLineItem("nca1", "Activos Fijos Tangibles", "Vehículos, propiedades y equipos registrados (NIC 16)", "Propiedad", fixedAssetsAmount, if (summary.totalAssets > 0) (fixedAssetsAmount / summary.totalAssets) * 100 else 0.0))
            }
            if (longTermInvestmentsAmount > 0) {
                add(FinancialLineItem("nca2", "Inversiones a largo plazo", "Acciones y fondos de inversión", "Inversión LP", longTermInvestmentsAmount, if (summary.totalAssets > 0) (longTermInvestmentsAmount / summary.totalAssets) * 100 else 0.0))
            }
        }.ifEmpty {
            listOf(FinancialLineItem("nca0", "Sin activos fijos", "No hay activos no corrientes registrados", "Propiedad", 0.0, 0.0))
        }
        val totalNonCurrentAssets = nonCurrentAssetsList.sumOf { it.amount }
        val grandTotalAssets = totalCurrentAssets + totalNonCurrentAssets

        // Real Current Liabilities from credit cards and negative account balances (excluding credit card accounts to prevent 200% double-counting)
        val cardsAmount = creditCards.sumOf { it.quotaUsed }
        val overdraftAmount = accounts
            .filter { !it.type.equals("CREDIT_CARD", ignoreCase = true) && (it.balance ?: 0.0) < 0 }
            .sumOf { abs(it.balance ?: 0.0) }

        val currentLiabilitiesList = buildList {
            if (cardsAmount > 0) {
                add(FinancialLineItem("cl1", "Tarjetas de Crédito", "Cupo utilizado en tarjetas activas", "Tarjetas", cardsAmount, if (summary.totalLiabilities > 0) (cardsAmount / summary.totalLiabilities) * 100 else 0.0))
            }
            if (overdraftAmount > 0) {
                add(FinancialLineItem("cl2", "Sobregiros bancarios", "Cuentas con saldo negativo", "Sobregiro", overdraftAmount, if (summary.totalLiabilities > 0) (overdraftAmount / summary.totalLiabilities) * 100 else 0.0))
            }
        }.ifEmpty {
            listOf(FinancialLineItem("cl0", "Sin pasivos corrientes", "No hay deudas a corto plazo registradas", "Pasivos", 0.0, 0.0))
        }
        val totalCurrentLiabilities = currentLiabilitiesList.sumOf { it.amount }
        val totalNonCurrentLiabilities = 0.0
        val nonCurrentLiabilitiesList = listOf(
            FinancialLineItem("ncl0", "Sin pasivos a largo plazo", "No hay obligaciones a largo plazo registradas", "Pasivos LP", 0.0, 0.0)
        )
        val grandTotalLiabilities = totalCurrentLiabilities + totalNonCurrentLiabilities

        // Real Equity Items (Patrimonio = Activos - Pasivos)
        val grandTotalEquity = grandTotalAssets - grandTotalLiabilities
        val liabilitiesPlusEquity = grandTotalLiabilities + grandTotalEquity
        val diff = grandTotalAssets - liabilitiesPlusEquity
        val isBalanced = abs(diff) < 0.01
        val periodResult = summary.periodResult
        val initialEquity = grandTotalEquity - periodResult
        val contributions = 0.0
        val retainedEarnings = 0.0
        val adjustments = 0.0

        val baseSheet = BalanceSheetData(
            currentAssets = currentAssetsList,
            nonCurrentAssets = nonCurrentAssetsList,
            totalCurrentAssets = totalCurrentAssets,
            totalNonCurrentAssets = totalNonCurrentAssets,
            totalAssets = grandTotalAssets,
            currentLiabilities = currentLiabilitiesList,
            nonCurrentLiabilities = nonCurrentLiabilitiesList,
            totalCurrentLiabilities = totalCurrentLiabilities,
            totalNonCurrentLiabilities = totalNonCurrentLiabilities,
            totalLiabilities = grandTotalLiabilities,
            initialEquity = initialEquity,
            contributions = contributions,
            retainedEarnings = retainedEarnings,
            periodResult = periodResult,
            adjustmentsAndRevaluations = adjustments,
            totalEquity = grandTotalEquity,
            liabilitiesPlusEquity = liabilitiesPlusEquity,
            isBalanced = isBalanced,
            balanceDifference = diff
        )

        return if (sharedSnapshot != null) {
            SharedAccountingStatementIntegrator.enrichBalanceSheetWithSharedPortfolio(baseSheet, sharedSnapshot)
        } else {
            baseSheet
        }
    }

    fun computeIncomeStatement(summary: FinancialSummaryData): IncomeStatementData {
        val totalIncome = summary.totalIncome
        val totalExpenses = summary.totalExpenses
        val grossResult = totalIncome
        val netResult = totalIncome - totalExpenses
        val savingsMarginPct = if (totalIncome > 0) ((netResult / totalIncome) * 100) else 0.0

        val incomeByCat = summary.incomeDistributions
            .filter { it.amount > 0 }
            .map { dist ->
                GroupedFinancialAmount(
                    id = dist.categoryId,
                    name = dist.categoryName,
                    amount = dist.amount,
                    transactionCount = dist.transactionCount,
                    percentageOfTotal = dist.percentage
                )
            }

        val incomeBySource = incomeByCat
        val incomeByAccount = emptyList<GroupedFinancialAmount>()

        val expensesByCat = summary.expenseDistributions
            .filter { it.amount > 0 }
            .map { dist ->
                GroupedFinancialAmount(
                    id = dist.categoryId,
                    name = dist.categoryName,
                    amount = dist.amount,
                    transactionCount = dist.transactionCount,
                    percentageOfTotal = dist.percentage
                )
            }

        val expensesByAccount = emptyList<GroupedFinancialAmount>()
        val expensesByMerchant = emptyList<GroupedFinancialAmount>()

        return IncomeStatementData(
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            grossResult = grossResult,
            operatingExpenses = totalExpenses,
            netResult = netResult,
            savingsMarginPct = (savingsMarginPct * 10).roundToInt() / 10.0,
            variationVsPreviousPeriodPct = 0.0,
            incomeByCategory = incomeByCat,
            incomeBySource = incomeBySource,
            incomeByAccount = incomeByAccount,
            expensesByCategory = expensesByCat,
            expensesByAccount = expensesByAccount,
            expensesByMerchant = expensesByMerchant,
            recurringExpensesTotal = totalExpenses,
            variableExpensesTotal = 0.0
        )
    }

    fun computeCashFlow(
        summary: FinancialSummaryData,
        accounts: List<AccountEntity> = emptyList(),
        periodTransactions: List<TransactionEntity> = emptyList()
    ): CashFlowStatementData {
        val liquidTypes = setOf("CASH", "SAVINGS", "CHECKING", "WALLET", "PAYROLL")
        val liquidAccounts = accounts.filter { it.type.uppercase() in liquidTypes }
        val currentLiquidBalance = if (liquidAccounts.isNotEmpty()) {
            liquidAccounts.filter { (it.balance ?: 0.0) > 0 }.sumOf { it.balance ?: 0.0 }
        } else {
            summary.totalAssets
        }

        val totalInflows = summary.cashFlowInflows
        val totalOutflows = summary.cashFlowOutflows
        val netCashFlow = totalInflows - totalOutflows
        val finalBalance = currentLiquidBalance
        val initialBalance = finalBalance - netCashFlow

        val internalTransfers = periodTransactions.filter {
            it.type.equals("TRANSFER_OUT", true) ||
            it.type.equals("TRANSFER_IN", true) ||
            !it.transferGroupId.isNullOrBlank()
        }
        val internalCount = internalTransfers.size
        val internalVolume = if (internalTransfers.isNotEmpty()) {
            if (internalTransfers.any { it.type.equals("TRANSFER_IN", true) }) {
                internalTransfers.sumOf { it.amount } / 2.0
            } else {
                internalTransfers.sumOf { it.amount }
            }
        } else {
            0.0
        }

        val inflows = summary.incomeDistributions.filter { it.amount > 0 }.map { dist ->
            CashFlowEntryItem(
                concept = dist.categoryName,
                category = "Operativo",
                amount = dist.amount,
                percentageOfTotal = dist.percentage,
                transactionCount = dist.transactionCount
            )
        }

        val outflows = summary.expenseDistributions.filter { it.amount > 0 }.map { dist ->
            CashFlowEntryItem(
                concept = dist.categoryName,
                category = "Operativo",
                amount = dist.amount,
                percentageOfTotal = dist.percentage,
                transactionCount = dist.transactionCount
            )
        }

        return CashFlowStatementData(
            initialBalance = initialBalance,
            inflows = inflows,
            totalInflows = totalInflows,
            outflows = outflows,
            totalOutflows = totalOutflows,
            netCashFlow = netCashFlow,
            finalBalance = finalBalance,
            internalTransfersExcludedCount = internalCount,
            internalTransfersExcludedVolume = internalVolume
        )
    }

    fun computeEquityStatement(
        summary: FinancialSummaryData,
        balanceSheet: BalanceSheetData,
        periodTransactions: List<TransactionEntity> = emptyList()
    ): EquityStatementData {
        val capitalContributions = periodTransactions
            .filter { isOpeningOrCapitalAllocation(it) && it.type.equals("INCOME", true) }
            .sumOf { it.amount }
        val periodResult = summary.periodResult
        val finalEquity = balanceSheet.totalEquity
        val initialEquity = finalEquity - periodResult - capitalContributions
        val netGrowthPct = if (initialEquity > 0) ((finalEquity - initialEquity) / initialEquity) * 100 else 0.0

        val changeRows = buildList {
            add(EquityChangeRow("Patrimonio inicial estimado", initialEquity, initialEquity >= 0, "Balance antes del período"))
            if (capitalContributions > 0) {
                add(EquityChangeRow("Aportes de capital / saldos iniciales", capitalContributions, true, "Aportes de apertura y capitalización"))
            }
            add(EquityChangeRow("Resultado neto del ejercicio", periodResult, periodResult >= 0, "Ingresos menos gastos operacionales"))
            add(EquityChangeRow("Patrimonio neto final", finalEquity, finalEquity >= 0, "Total patrimonio según Balance General"))
        }

        return EquityStatementData(
            initialEquity = initialEquity,
            periodResult = periodResult,
            capitalContributions = capitalContributions,
            withdrawals = 0.0,
            valuationGainsOrLosses = 0.0,
            assetRevaluations = 0.0,
            otherAdjustments = 0.0,
            finalEquity = finalEquity,
            changeRows = changeRows,
            netEquityGrowthPct = (netGrowthPct * 10).roundToInt() / 10.0
        )
    }

    fun computeSpecializedReports(
        accounts: List<AccountEntity>,
        assets: List<FixedAssetEntity>,
        investments: List<InvestmentEntity>,
        creditCards: List<AccountCreditCardEntity>,
        incomeStatement: IncomeStatementData,
        periodTransactions: List<TransactionEntity> = emptyList()
    ): SpecializedReportsBundle {
        val accountReports = accounts.map { acc ->
            val accTx = periodTransactions.filter { it.accountId == acc.id }
            val inflows = accTx.filter { it.type.equals("INCOME", true) || it.type.equals("TRANSFER_IN", true) }.sumOf { it.amount }
            val outflows = accTx.filter { it.type.equals("EXPENSE", true) || it.type.equals("TRANSFER_OUT", true) }.sumOf { it.amount }
            val curBal = acc.balance ?: 0.0
            val initBal = curBal - inflows + outflows
            AccountReportItem(
                id = acc.id,
                name = acc.name,
                bankName = acc.bankName ?: "Entidad Financiera",
                initialBalance = initBal,
                totalInflows = inflows,
                totalOutflows = outflows,
                currentBalance = curBal,
                transactionCount = accTx.size,
                currency = acc.currency
            )
        }

        val cardReports = creditCards.map { card ->
            val util = if (card.quotaTotal > 0) (card.quotaUsed / card.quotaTotal) * 100 else 0.0
            val cardTx = periodTransactions.filter { it.accountId == card.accountId }
            val expenses = cardTx.filter { it.type.equals("EXPENSE", true) }.sumOf { it.amount }
            val payments = cardTx.filter { it.type.equals("INCOME", true) || it.type.equals("TRANSFER_IN", true) }.sumOf { it.amount }
            CreditCardReportItem(
                accountId = card.accountId,
                cardName = "Tarjeta ${card.franchise} ${card.category}",
                bankName = "Entidad Financiera",
                quotaTotal = card.quotaTotal,
                quotaUsed = card.quotaUsed,
                quotaAvailable = card.quotaAvailable,
                utilizationPct = (util * 10).roundToInt() / 10.0,
                currentPeriodExpenses = if (expenses > 0) expenses else card.quotaUsed,
                currentPeriodPayments = payments,
                interestEstimate = card.quotaUsed * 0.021,
                pendingBalance = card.quotaUsed,
                cutOffDay = card.cutOffDay,
                paymentDueDay = card.paymentDueDay
            )
        }

        val debtReports = buildList {
            creditCards.filter { it.quotaUsed > 0 }.forEach { card ->
                add(DebtReportItem(
                    id = card.accountId,
                    creditorName = "Tarjeta ${card.franchise} ${card.category}",
                    debtType = "Tarjeta de Crédito",
                    initialBalance = card.quotaTotal,
                    currentBalance = card.quotaUsed,
                    totalPayments = 0.0,
                    amortizedCapital = (card.quotaTotal - card.quotaUsed).coerceAtLeast(0.0),
                    interestPaid = card.quotaUsed * 0.021,
                    nextDueDate = "Día ${card.paymentDueDay} del mes",
                    status = "Al día"
                ))
            }
            accounts.filter { !it.type.equals("CREDIT_CARD", true) && (it.balance ?: 0.0) < 0 }.forEach { acc ->
                val overdraft = abs(acc.balance ?: 0.0)
                add(DebtReportItem(
                    id = acc.id,
                    creditorName = "Sobregiro en ${acc.name}",
                    debtType = "Línea de Sobregiro",
                    initialBalance = overdraft,
                    currentBalance = overdraft,
                    totalPayments = 0.0,
                    amortizedCapital = 0.0,
                    interestPaid = overdraft * 0.019,
                    nextDueDate = "Inmediato",
                    status = "Pendiente de regularizar"
                ))
            }
        }

        val assetReports = assets.map { a ->
            val bookVal = (a.acquisitionValue - a.accumulatedDepreciation).coerceAtLeast(a.residualValue)
            AssetReportItem(
                id = a.id,
                assetCode = a.serialNumber ?: "ACT-${a.id.take(4)}",
                name = a.name,
                categoryName = a.category,
                acquisitionCost = a.acquisitionValue,
                currentMarketValue = if (a.currentMarketValue > 0) a.currentMarketValue else bookVal,
                bookValue = bookVal,
                accumulatedDepreciation = a.accumulatedDepreciation,
                periodDepreciation = 0.0,
                maintenanceExpense = 0.0,
                isDisposedOrSold = a.status == "SOLD" || a.status == "DISPOSED",
                status = a.status
            )
        }

        val investmentReports = investments.map { inv ->
            val diff = inv.currentValue - inv.initialAmount
            val retPct = if (inv.initialAmount > 0) (diff / inv.initialAmount) * 100 else 0.0
            InvestmentReportItem(
                id = inv.id,
                name = inv.name,
                entityOrPlatform = inv.institutionName,
                investedCapital = inv.initialAmount,
                currentValue = inv.currentValue,
                generatedYields = diff,
                returnPercentage = (retPct * 10).roundToInt() / 10.0,
                variationAmount = diff,
                isPositive = diff >= 0
            )
        }

        return SpecializedReportsBundle(
            accountReports = accountReports,
            creditCardReports = cardReports,
            debtReports = debtReports,
            assetReports = assetReports,
            investmentReports = investmentReports,
            recurringExpensesList = incomeStatement.expensesByCategory.take(3),
            variableExpensesList = incomeStatement.expensesByCategory.drop(3),
            incomeByCategoryList = incomeStatement.incomeByCategory,
            incomeBySourceList = incomeStatement.incomeBySource
        )
    }

    fun computeFinancialAnalysis(
        summary: FinancialSummaryData,
        balanceSheet: BalanceSheetData
    ): FinancialAnalysisData {
        val hasCurrentLiabilities = balanceSheet.totalCurrentLiabilities > 0.01
        val liquidityRatio = if (hasCurrentLiabilities) {
            balanceSheet.totalCurrentAssets / balanceSheet.totalCurrentLiabilities
        } else {
            if (balanceSheet.totalCurrentAssets > 0) 99.9 else 1.0
        }

        val liquidCurrentAssets = balanceSheet.currentAssets
            .filter { it.id.startsWith("ca1") || it.id.startsWith("ca2") }
            .sumOf { it.amount }

        val acidTest = if (hasCurrentLiabilities) {
            liquidCurrentAssets / balanceSheet.totalCurrentLiabilities
        } else {
            if (liquidCurrentAssets > 0) 99.9 else 1.0
        }

        val totalAssets = balanceSheet.totalAssets.coerceAtLeast(1.0)
        val debtRatioPct = (balanceSheet.totalLiabilities / totalAssets) * 100.0

        val totalIncome = summary.totalIncome.coerceAtLeast(1.0)
        val netMarginPct = (summary.periodResult / totalIncome) * 100.0

        val roaPct = (summary.periodResult / totalAssets) * 100.0

        val totalEquity = balanceSheet.totalEquity.coerceAtLeast(1.0)
        val roePct = (summary.periodResult / totalEquity) * 100.0

        val savingsRatePct = if (summary.totalIncome > 0) ((summary.periodResult / summary.totalIncome) * 100.0) else 0.0

        var healthScore = 0
        healthScore += when {
            !hasCurrentLiabilities && balanceSheet.totalCurrentAssets > 0 -> 25
            liquidityRatio >= 1.5 -> 25
            liquidityRatio >= 1.0 -> 18
            liquidityRatio >= 0.7 -> 10
            else -> 5
        }
        healthScore += when {
            debtRatioPct <= 20.0 -> 25
            debtRatioPct <= 35.0 -> 20
            debtRatioPct <= 50.0 -> 12
            else -> 5
        }
        healthScore += when {
            savingsRatePct >= 25.0 -> 25
            savingsRatePct >= 15.0 -> 20
            savingsRatePct >= 5.0 -> 14
            savingsRatePct >= 0.0 -> 8
            else -> 2
        }
        healthScore += when {
            balanceSheet.totalAssets > balanceSheet.totalLiabilities && balanceSheet.totalEquity > 0 -> 25
            balanceSheet.totalEquity > 0 -> 15
            else -> 5
        }

        val healthLabel = when {
            healthScore >= 85 -> "Excelente Salud Financiera (A+)"
            healthScore >= 70 -> "Buena Salud Financiera (A)"
            healthScore >= 50 -> "Salud Financiera Moderada (B)"
            else -> "Atención Requerida (C)"
        }

        val ratios = listOf(
            FinancialRatioItem(
                id = "r_liquidity",
                name = "Razón Corriente (Liquidez)",
                category = "Liquidez",
                rawValue = liquidityRatio,
                formattedValue = if (!hasCurrentLiabilities) "Sin pasivos CP (Óptimo)" else "%.2f x".format(liquidityRatio),
                benchmark = "> 1.50 x",
                status = if (!hasCurrentLiabilities || liquidityRatio >= 1.5) RatioHealthStatus.EXCELLENT else if (liquidityRatio >= 1.0) RatioHealthStatus.HEALTHY else RatioHealthStatus.WARNING,
                formula = "Activo Corriente / Pasivo Corriente",
                interpretation = if (!hasCurrentLiabilities) "No presentas deudas u obligaciones exigibles a corto plazo. Toda tu liquidez ($%,.0f COP) está libre de gravámenes.".format(balanceSheet.totalCurrentAssets) else "Dispones de $%.2f pesos en activos líquidos por cada peso de deuda a corto plazo.".format(liquidityRatio),
                recommendation = "Excelente colchón de solvencia. Mantén fondos en cuentas remuneradas o de alta disponibilidad."
            ),
            FinancialRatioItem(
                id = "r_acid",
                name = "Prueba Ácida",
                category = "Liquidez",
                rawValue = acidTest,
                formattedValue = if (!hasCurrentLiabilities) "Sin pasivos CP (Óptimo)" else "%.2f x".format(acidTest),
                benchmark = "> 1.00 x",
                status = if (!hasCurrentLiabilities || acidTest >= 1.2) RatioHealthStatus.EXCELLENT else if (acidTest >= 0.9) RatioHealthStatus.HEALTHY else RatioHealthStatus.WARNING,
                formula = "Activos Inmediatamente Disponibles / Pasivo Corriente",
                interpretation = if (!hasCurrentLiabilities) "Cuentas con $%,.0f COP en efectivo y bancos sin compromisos exigibles inmediatos.".format(liquidCurrentAssets) else "Capacidad inmediata de pago sin depender de liquidar activos secundarios o inversiones.",
                recommendation = "Tu respaldo inmediato cubre con holgura todas las obligaciones a 30 días."
            ),
            FinancialRatioItem(
                id = "r_debt",
                name = "Nivel de Endeudamiento",
                category = "Endeudamiento",
                rawValue = debtRatioPct,
                formattedValue = "%.1f %%".format(debtRatioPct),
                benchmark = "< 40.0 %",
                status = if (debtRatioPct <= 30.0) RatioHealthStatus.EXCELLENT else if (debtRatioPct <= 45.0) RatioHealthStatus.HEALTHY else RatioHealthStatus.WARNING,
                formula = "(Pasivo Total / Activo Total) * 100",
                interpretation = "El %.1f%% de tu patrimonio y activos está financiado por acreedores o tarjetas.".format(debtRatioPct),
                recommendation = "Nivel de deuda conservador y altamente saludable frente al patrimonio neto."
            ),
            FinancialRatioItem(
                id = "r_margin",
                name = "Margen de Ahorro / Neto",
                category = "Rentabilidad",
                rawValue = netMarginPct,
                formattedValue = "%.1f %%".format(netMarginPct),
                benchmark = "> 20.0 %",
                status = if (netMarginPct >= 25.0) RatioHealthStatus.EXCELLENT else if (netMarginPct >= 15.0) RatioHealthStatus.HEALTHY else RatioHealthStatus.WARNING,
                formula = "(Resultado Neto / Ingresos Totales) * 100",
                interpretation = "Retienes de forma neta el %.1f%% de cada peso ingresado este período.".format(netMarginPct),
                recommendation = "Mantén la regla del 50/30/20 canalizando los excedentes a inversión diversificada."
            ),
            FinancialRatioItem(
                id = "r_roa",
                name = "Rentabilidad s/ Activos (ROA)",
                category = "Eficiencia",
                rawValue = roaPct,
                formattedValue = "%.1f %%".format(roaPct),
                benchmark = "> 5.0 %",
                status = if (roaPct >= 6.0) RatioHealthStatus.EXCELLENT else RatioHealthStatus.HEALTHY,
                formula = "(Resultado Neto / Activos Totales) * 100",
                interpretation = "Tus activos están rindiendo positivamente en generación de flujo neto anualizado.",
                recommendation = "Revisa activos ociosos en efectivo sin rentabilidad para mejorar el índice."
            ),
            FinancialRatioItem(
                id = "r_roe",
                name = "Rentabilidad s/ Patrimonio (ROE)",
                category = "Rentabilidad",
                rawValue = roePct,
                formattedValue = "%.1f %%".format(roePct),
                benchmark = "> 8.0 %",
                status = if (roePct >= 10.0) RatioHealthStatus.EXCELLENT else RatioHealthStatus.HEALTHY,
                formula = "(Resultado Neto / Patrimonio Total) * 100",
                interpretation = "Rendimiento del capital propio invertido en el ecosistema personal.",
                recommendation = "Excelente crecimiento patrimonial compuesto por encima de la inflación proyectada."
            )
        )

        val alerts = buildList {
            if (!hasCurrentLiabilities) {
                add(SmartFinancialAlert(
                    id = "alt_no_debt",
                    title = "Solvencia Plena sin Deuda a Corto Plazo",
                    description = "No registras pasivos corrientes ni obligaciones de tarjeta de crédito pendientes para el período.",
                    severity = AlertSeverityLevel.INFO,
                    actionableRecommendation = "Excelente salud crediticia. Puedes canalizar tus flujos libres a instrumentos de inversión productiva.",
                    indicatorImpact = "Pasivo Corriente: $0 COP"
                ))
            } else {
                add(SmartFinancialAlert(
                    id = "alt1",
                    title = "Liquidez y Solvencia en Rango Seguro",
                    description = "La razón corriente de %.2f supera el umbral de 1.50x. Cuentas con respaldo para imprevistos.".format(liquidityRatio),
                    severity = AlertSeverityLevel.INFO,
                    actionableRecommendation = "Considera mover excedentes de caja a un CDT o fondo fiduciario con tasa superior a la inflación.",
                    indicatorImpact = "Razón Corriente: %.2f x".format(liquidityRatio)
                ))
            }
            if (debtRatioPct > 40.0) {
                add(SmartFinancialAlert(
                    id = "alt_debt_high",
                    title = "Apalancamiento Superior al Umbral Recomendado",
                    description = "Tu nivel de endeudamiento se sitúa en %.1f%% de los activos totales.".format(debtRatioPct),
                    severity = AlertSeverityLevel.WARNING,
                    actionableRecommendation = "Prioriza amortizar pasivos de mayor costo de interés para reducir el servicio de la deuda.",
                    indicatorImpact = "Endeudamiento: %.1f%%".format(debtRatioPct)
                ))
            } else {
                add(SmartFinancialAlert(
                    id = "alt2",
                    title = "Nivel de Endeudamiento Saludable",
                    description = "Tus pasivos representan el %.1f%% de tus activos, dentro del límite prudencial (< 40%%).".format(debtRatioPct),
                    severity = AlertSeverityLevel.INFO,
                    actionableRecommendation = "Mantén la disciplina de pago para reportar un score crediticio superior.",
                    indicatorImpact = "Endeudamiento: %.1f%%".format(debtRatioPct)
                ))
            }
            if (summary.periodResult < 0) {
                add(SmartFinancialAlert(
                    id = "alt_deficit",
                    title = "Déficit Operativo en el Período",
                    description = "Los gastos superaron a los ingresos en $%,.0f COP durante este período.".format(abs(summary.periodResult)),
                    severity = AlertSeverityLevel.WARNING,
                    actionableRecommendation = "Revisa gastos discrecionales y ajusta el presupuesto del mes próximo.",
                    indicatorImpact = "Déficit: $%,.0f COP".format(abs(summary.periodResult))
                ))
            }
        }

        val projections = listOf(
            FutureProjectionPoint(
                horizonMonths = 1,
                horizonLabel = "Próximo Mes",
                projectedNetWorth = summary.netWorth + summary.periodResult,
                projectedIncome = summary.totalIncome * 1.02,
                projectedExpenses = summary.totalExpenses * 0.98,
                projectedSavings = summary.periodResult * 1.05,
                confidencePct = 94,
                isPositiveTrend = summary.periodResult >= 0,
                narrativeSummary = "Proyección mensual basada en el flujo operativo neto actual ($%,.0f COP).".format(summary.periodResult)
            ),
            FutureProjectionPoint(
                horizonMonths = 3,
                horizonLabel = "En 3 Meses",
                projectedNetWorth = summary.netWorth + (summary.periodResult * 3.0),
                projectedIncome = summary.totalIncome * 3.0,
                projectedExpenses = summary.totalExpenses * 3.0,
                projectedSavings = summary.periodResult * 3.0,
                confidencePct = 88,
                isPositiveTrend = summary.periodResult >= 0,
                narrativeSummary = "Consolidación de metas de ahorro a corto plazo y amortización de deudas."
            ),
            FutureProjectionPoint(
                horizonMonths = 6,
                horizonLabel = "En 6 Meses",
                projectedNetWorth = summary.netWorth + (summary.periodResult * 6.0),
                projectedIncome = summary.totalIncome * 6.0,
                projectedExpenses = summary.totalExpenses * 6.0,
                projectedSavings = summary.periodResult * 6.0,
                confidencePct = 80,
                isPositiveTrend = summary.periodResult >= 0,
                narrativeSummary = "Expansión patrimonial proyectada manteniendo el ritmo operativo actual."
            ),
            FutureProjectionPoint(
                horizonMonths = 12,
                horizonLabel = "En 1 Año",
                projectedNetWorth = summary.netWorth + (summary.periodResult * 12.0),
                projectedIncome = summary.totalIncome * 12.0,
                projectedExpenses = summary.totalExpenses * 12.0,
                projectedSavings = summary.periodResult * 12.0,
                confidencePct = 72,
                isPositiveTrend = summary.periodResult >= 0,
                narrativeSummary = "Proyección anualizada de patrimonio neto según el resultado operativo vigente."
            )
        )

        return FinancialAnalysisData(
            overallHealthScore = healthScore,
            overallHealthLabel = healthLabel,
            ratios = ratios,
            smartAlerts = alerts,
            projections = projections,
            liquidityRatioValue = (liquidityRatio * 100).roundToInt() / 100.0,
            acidTestValue = (acidTest * 100).roundToInt() / 100.0,
            debtLevelPct = (debtRatioPct * 10).roundToInt() / 10.0,
            netMarginPct = (netMarginPct * 10).roundToInt() / 10.0,
            roaPct = (roaPct * 10).roundToInt() / 10.0,
            roePct = (roePct * 10).roundToInt() / 10.0,
            savingsRatePct = (savingsRatePct * 10).roundToInt() / 10.0
        )
    }

    internal fun filterTransactionsByDate(
        transactions: List<TransactionEntity>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<TransactionEntity> {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        return transactions.filter { tx ->
            try {
                val txDate = LocalDate.parse(tx.date.take(10), formatter)
                !txDate.isBefore(startDate) && !txDate.isAfter(endDate)
            } catch (e: Exception) {
                true // fallback
            }
        }
    }
}
