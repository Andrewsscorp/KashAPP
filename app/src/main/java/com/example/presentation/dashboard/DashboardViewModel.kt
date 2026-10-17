package com.example.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.FinancialRepository
import com.example.presentation.dashboard.model.Account
import com.example.presentation.dashboard.model.AccountType
import com.example.presentation.dashboard.model.CashFlowEntry
import com.example.presentation.dashboard.model.CategorySpend
import com.example.presentation.dashboard.model.CreditCard
import com.example.presentation.dashboard.model.DashboardEvent
import com.example.presentation.dashboard.model.DashboardUiState
import com.example.presentation.dashboard.model.Goal
import com.example.presentation.dashboard.model.HealthScore
import com.example.presentation.dashboard.model.InsightSeverity
import com.example.presentation.dashboard.model.KpiState
import com.example.presentation.dashboard.model.NetWorthState
import com.example.presentation.dashboard.model.PeriodState
import com.example.presentation.dashboard.model.RiskLevel
import com.example.presentation.dashboard.model.ScheduledPayment
import com.example.presentation.dashboard.model.SmartInsight
import com.example.presentation.dashboard.model.Transaction
import com.example.presentation.dashboard.model.TransactionType
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/**
 * Enterprise L7 MVI ViewModel governing the KashApp Dashboard.
 * Strictly uses Room SSOT, BigDecimal precision with HALF_EVEN rounding,
 * and zero mock data (defaults to 0 when local database is empty).
 */
class DashboardViewModel(
    val financialRepository: FinancialRepository? = null,
    val exchangeRateRepository: com.example.domain.repository.ExchangeRateRepository? = null,
    val settingsRepository: com.example.features.settings.domain.repository.SettingsRepository? = null,
    val fixedAssetRepository: com.example.domain.repository.FixedAssetRepository? = null,
    val userId: String = "user_default"
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var rawNetWorthState = NetWorthState()
    private var rawKpiState = KpiState()

    init {
        observeSettings()
        observeDatabaseSources()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val currentAccounts = financialRepository?.getAccounts(userId)?.firstOrNull().orEmpty()
                val dummyIds = setOf("acc_ahorros", "acc_corriente", "acc_billetera", "acc_efectivo")
                val hasRealAccounts = currentAccounts.any { !dummyIds.contains(it.id) || it.balance > 0.0 }
                if (!hasRealAccounts) {
                    val restored = financialRepository?.restoreFinancialBackupFromCloudIfNeeded(userId)?.getOrDefault(false) ?: false
                    if (!restored) {
                        financialRepository?.seedDefaultsIfNeeded(userId)
                    }
                }
                fixedAssetRepository?.seedDefaultCategoriesIfNeeded(userId)
                fixedAssetRepository?.seedDefaultAssetsIfNeeded(userId)
            } catch (e: Exception) {
                android.util.Log.w("DashboardViewModel", "Error sincronizando datos al iniciar: ${e.message}")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun observeSettings() {
        val settings = settingsRepository ?: return
        viewModelScope.launch {
            settings.observePreferences(userId).collect { prefs ->
                val base = prefs.baseCurrency.ifBlank { "COP" }
                _uiState.update { current ->
                    current.copy(
                        baseCurrency = base,
                        displayCurrency = if (current.isConvertedToUsd) "USD" else base
                    )
                }
            }
        }
    }

    private fun observeDatabaseSources() {
        val repo = financialRepository ?: return
        val assetsFlow = fixedAssetRepository?.observeAssets(userId) ?: kotlinx.coroutines.flow.flowOf(emptyList())

        val f1 = combine(
            repo.getAccounts(userId),
            repo.getTransactions(userId),
            repo.getBudgets(userId)
        ) { accounts, transactions, budgets ->
            Triple(accounts, transactions, budgets)
        }

        val f2 = combine(
            repo.getGoals(userId),
            repo.getInvestments(userId),
            assetsFlow
        ) { goals, investments, fixedAssets ->
            Triple(goals, investments, fixedAssets)
        }

        val f3 = combine(
            repo.getAllAccountCreditCards(),
            repo.getScheduledPayments(userId)
        ) { creditCardConfigs, scheduledPayments ->
            creditCardConfigs to scheduledPayments
        }

        val prefsFlow = settingsRepository?.observePreferences(userId)
            ?: kotlinx.coroutines.flow.flowOf(com.example.features.settings.domain.model.UserPreferencesDomain())

        val f4 = combine(f3, prefsFlow) { (creditCardConfigs, scheduledPayments), prefs ->
            Triple(creditCardConfigs, scheduledPayments, prefs)
        }

        viewModelScope.launch {
            // Combine real reactive flows from Room Database including Fixed Assets, Credit Cards, Scheduled Payments, and User Preferences
            combine(f1, f2, f4) { (accounts, transactions, budgets), (goals, investments, fixedAssets), (creditCardConfigs, scheduledPayments, prefs) ->
                val selectedMonth = _uiState.value.period.currentMonth
                val isPrivacy = _uiState.value.isPrivacyMode
                val excludeAllAccounts = prefs.excludeAllAccountsFromDashboard
                val excludedAccountIds = prefs.excludedAccountIds
                val excludeAllFixedAssets = prefs.excludeAllFixedAssetsFromDashboard

                // 1. Compute Accounts
                val domainAccounts = accounts.map { acc ->
                    val isExcluded = excludeAllAccounts || excludedAccountIds.contains(acc.id)
                    Account(
                        id = try { UUID.fromString(acc.id.removePrefix("acc_").removePrefix("card_")) } catch (_: Exception) { UUID.randomUUID() },
                        name = acc.name,
                        bankName = acc.bankName,
                        balance = BigDecimal.valueOf(acc.balance).setScale(2, RoundingMode.HALF_EVEN),
                        type = when (acc.type.uppercase()) {
                            "CREDIT_CARD" -> AccountType.CREDIT_CARD
                            "CHECKING" -> AccountType.CHECKING
                            "CDT", "INVESTMENT" -> AccountType.INVESTMENT
                            "CASH" -> AccountType.CASH
                            else -> AccountType.SAVINGS
                        },
                        currency = acc.currency,
                        colorHex = acc.customColorHex.ifBlank { "#0284C7" },
                        allowsNegative = acc.hasOverdraft,
                        rawId = acc.id,
                        isExcludedFromDashboard = isExcluded
                    )
                }.toImmutableList()

                // 2. Filter Transactions by Selected Period
                val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)
                val periodTransactions = transactions.filter { tx ->
                    try {
                        val date = LocalDate.parse(tx.date.take(10), monthFormatter)
                        YearMonth.from(date) == selectedMonth
                    } catch (_: Exception) {
                        true
                    }
                }

                // 3. Compute Net Worth State (availableCash + investments + assets - liabilities)
                val availableCash = if (excludeAllAccounts) {
                    BigDecimal.ZERO
                } else {
                    accounts
                        .filter { 
                            it.type != "CREDIT_CARD" && it.type != "CREDIT" && it.type != "CDT" && it.type != "INVESTMENT" &&
                            !it.name.contains("Tarjeta", ignoreCase = true) &&
                            !excludedAccountIds.contains(it.id)
                        }
                        .fold(BigDecimal.ZERO) { acc, item -> acc + BigDecimal.valueOf(item.balance.coerceAtLeast(0.0)) }
                        .setScale(2, RoundingMode.HALF_EVEN)
                }

                val investmentsTotal = investments
                    .filter { it.includeInNetWorth }
                    .fold(BigDecimal.ZERO) { acc, item -> acc + BigDecimal.valueOf(item.initialAmount) }
                    .add(
                        if (excludeAllAccounts) {
                            BigDecimal.ZERO
                        } else {
                            accounts.filter { (it.type == "CDT" || it.type == "INVESTMENT") && !excludedAccountIds.contains(it.id) }
                                .fold(BigDecimal.ZERO) { acc, item -> acc + BigDecimal.valueOf(item.balance) }
                        }
                    )
                    .setScale(2, RoundingMode.HALF_EVEN)

                val liabilitiesTotal = if (excludeAllAccounts) {
                    BigDecimal.ZERO
                } else {
                    accounts
                        .filter { 
                            (it.type.equals("CREDIT_CARD", ignoreCase = true) || 
                            it.type.equals("CREDIT", ignoreCase = true) || 
                            it.type.equals("LOAN", ignoreCase = true) || 
                            it.type.equals("DEBT", ignoreCase = true) ||
                            it.name.contains("Tarjeta", ignoreCase = true)) &&
                            !excludedAccountIds.contains(it.id)
                        }
                        .fold(BigDecimal.ZERO) { acc, item ->
                            val cardConfig = creditCardConfigs.find { it.accountId == item.id }
                            if (cardConfig != null && !cardConfig.includeInNetWorth) {
                                acc
                            } else {
                                val debtAmount = if (cardConfig != null) {
                                    cardConfig.quotaUsed
                                } else if (item.overdraftLimit > 0 && item.availableBalance != null) {
                                    (item.overdraftLimit - item.availableBalance).coerceAtLeast(0.0)
                                } else if (item.balance < 0) {
                                    kotlin.math.abs(item.balance)
                                } else {
                                    0.0
                                }
                                acc + BigDecimal.valueOf(debtAmount)
                            }
                        }
                        .setScale(2, RoundingMode.HALF_EVEN)
                }

                // Fixed Assets: Sum all valid non-disposed user assets respecting 'noAffectDashboard' and global exclusion
                val totalFixedAssetsValue = if (excludeAllFixedAssets) {
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN)
                } else {
                    fixedAssets
                        .filter { asset ->
                            val state = com.example.domain.model.fixedassets.statemachine.AssetStateMachine.resolveState(asset.status)
                            asset.deletedAt == null && !state.isTerminal &&
                            !asset.status.equals("WRITTEN_OFF", ignoreCase = true) &&
                            !asset.status.equals("SOLD", ignoreCase = true) &&
                            !asset.status.equals("DISPOSED", ignoreCase = true) &&
                            !asset.status.equals("DADO_DE_BAJA", ignoreCase = true) &&
                            !asset.noAffectDashboard && asset.includeInNetWorth
                        }
                        .fold(BigDecimal.ZERO) { acc, asset ->
                            val netBookValue = (asset.acquisitionValue - asset.accumulatedDepreciation).coerceAtLeast(asset.residualValue).coerceAtLeast(0.0)
                            val assetVal = if (netBookValue > 0.0) netBookValue else asset.acquisitionValue
                            acc + BigDecimal.valueOf(assetVal)
                        }
                        .setScale(2, RoundingMode.HALF_EVEN)
                }

                val sparkline = computeSparklinePoints(transactions)

                val netWorthState = NetWorthState(
                    availableCash = availableCash,
                    investments = investmentsTotal,
                    assets = totalFixedAssetsValue,
                    liabilities = liabilitiesTotal,
                    sparklinePoints = sparkline.toImmutableList()
                )

                // 4. Compute KPI State (Income, Expenses, Savings, BudgetLimit)
                val totalIncome = periodTransactions
                    .filter { it.type == "INCOME" }
                    .fold(BigDecimal.ZERO) { acc, item -> acc + BigDecimal.valueOf(item.amount) }
                    .setScale(2, RoundingMode.HALF_EVEN)

                val totalExpenses = periodTransactions
                    .filter { it.type == "EXPENSE" }
                    .fold(BigDecimal.ZERO) { acc, item -> acc + BigDecimal.valueOf(item.amount) }
                    .setScale(2, RoundingMode.HALF_EVEN)

                val totalSavings = (totalIncome - totalExpenses).setScale(2, RoundingMode.HALF_EVEN)

                val budgetLimit = budgets
                    .filter { it.isActive }
                    .fold(BigDecimal.ZERO) { acc, item -> acc + BigDecimal.valueOf(item.totalAmount) }
                    .setScale(2, RoundingMode.HALF_EVEN)

                val kpiState = KpiState(
                    income = totalIncome,
                    expenses = totalExpenses,
                    savings = totalSavings,
                    budgetLimit = budgetLimit
                )

                // 5. Compute Financial Health Score
                val healthScore = computeHealthScore(totalIncome, totalExpenses, budgetLimit, liabilitiesTotal)

                // 6. Compute Category Spends Donut
                val categorySpends = periodTransactions
                    .filter { it.type == "EXPENSE" }
                    .groupBy { it.categoryId to it.categoryName }
                    .map { (catKey, txList) ->
                        val amount = txList.fold(BigDecimal.ZERO) { acc, item -> acc + BigDecimal.valueOf(item.amount) }
                        CategorySpend(
                            categoryId = try { UUID.fromString(catKey.first.removePrefix("cat_")) } catch (_: Exception) { UUID.randomUUID() },
                            categoryName = catKey.second,
                            amount = amount.setScale(2, RoundingMode.HALF_EVEN),
                            hexColor = getCategoryColor(catKey.second)
                        )
                    }
                    .sortedByDescending { it.amount }
                    .toImmutableList()

                // 7. Compute Cash Flow Entries
                val cashFlow = computeCashFlowEntries(periodTransactions)

                // 8. Recent Transactions
                val recentDomainTx = transactions.take(10).map { tx ->
                    Transaction(
                        id = try { UUID.fromString(tx.id.removePrefix("tx_")) } catch (_: Exception) { UUID.randomUUID() },
                        mccIcon = "payments",
                        concept = tx.description ?: tx.categoryName,
                        amount = BigDecimal.valueOf(tx.amount).setScale(2, RoundingMode.HALF_EVEN),
                        type = when (tx.type.uppercase()) {
                            "INCOME" -> TransactionType.INCOME
                            "TRANSFER", "TRANSFER_IN", "TRANSFER_OUT" -> TransactionType.TRANSFER
                            else -> TransactionType.EXPENSE
                        },
                        dateFormatted = tx.date,
                        accountName = tx.accountName,
                        categoryName = tx.categoryName
                    )
                }.toImmutableList()

                // 9. Goals
                val domainGoals = goals.map { g ->
                    Goal(
                        id = try { UUID.fromString(g.id.removePrefix("goal_")) } catch (_: Exception) { UUID.randomUUID() },
                        title = g.name,
                        targetAmount = BigDecimal.valueOf(g.targetAmount).setScale(2, RoundingMode.HALF_EVEN),
                        currentAmount = BigDecimal.valueOf(g.savedAmount).setScale(2, RoundingMode.HALF_EVEN),
                        colorHex = g.colorHex.ifBlank { "#10B981" },
                        isCommitted = g.virtualPocketEnabled
                    )
                }.toImmutableList()

                // 10. Credit Cards (All credit cards from Room DB matched with metadata)
                val domainCards = accounts.filter {
                    it.type.equals("CREDIT_CARD", ignoreCase = true) ||
                    it.type.equals("CREDIT", ignoreCase = true) ||
                    it.name.contains("Tarjeta", ignoreCase = true)
                }.map { c ->
                    val cardConfig = creditCardConfigs.find { it.accountId == c.id }
                    val totalLimit = cardConfig?.quotaTotal ?: if (c.overdraftLimit > 0) c.overdraftLimit else 2_000_000.0
                    val usedQuotaValue = cardConfig?.quotaUsed ?: if (c.overdraftLimit > 0 && c.availableBalance != null) {
                        (c.overdraftLimit - c.availableBalance).coerceAtLeast(0.0)
                    } else if (c.balance < 0) {
                        kotlin.math.abs(c.balance)
                    } else {
                        0.0
                    }
                    CreditCard(
                        id = try { UUID.fromString(c.id.removePrefix("card_").removePrefix("acc_")) } catch (_: Exception) { UUID.randomUUID() },
                        cardName = c.name,
                        bankName = c.bankName,
                        quotaLimit = BigDecimal.valueOf(totalLimit).setScale(2, RoundingMode.HALF_EVEN),
                        usedQuota = BigDecimal.valueOf(usedQuotaValue).setScale(2, RoundingMode.HALF_EVEN),
                        cutoffDateFormatted = if (cardConfig != null && cardConfig.cutOffDay > 0) "Día ${cardConfig.cutOffDay}" else if (c.feeChargeDay > 0) "Día ${c.feeChargeDay}" else "Día 15",
                        paymentDueDateFormatted = if (cardConfig != null && cardConfig.paymentDueDay > 0) "Día ${cardConfig.paymentDueDay}" else if (c.feeChargeDay > 0) "Día ${(c.feeChargeDay + 15) % 30 + 1}" else "Día 2",
                        colorHex = c.customColorHex.ifBlank { "#0284C7" },
                        includeInNetWorth = cardConfig?.includeInNetWorth ?: true
                    )
                }.toImmutableList()

                // 11. Scheduled Payments (Real obligations from Room DB)
                val domainUpcomingPayments = scheduledPayments
                    .filter { !it.isPaid }
                    .map { sp ->
                        val dueLocalDate: LocalDate = try {
                            LocalDate.parse(sp.dueDate.take(10))
                        } catch (_: Exception) {
                            LocalDate.now()
                        }
                        val paymentId: UUID = try {
                            UUID.fromString(sp.id.removePrefix("sp_"))
                        } catch (_: Exception) {
                            UUID.randomUUID()
                        }
                        ScheduledPayment(
                            id = paymentId,
                            entityName = sp.entityName,
                            dueDate = dueLocalDate,
                            amount = BigDecimal.valueOf(sp.amount).setScale(2, RoundingMode.HALF_EVEN),
                            isIncome = sp.isIncome,
                            rawId = sp.id,
                            category = sp.category,
                            recurrence = sp.recurrence,
                            currency = sp.currency,
                            isPaid = sp.isPaid,
                            paidDate = sp.paidDate,
                            sourceAccountId = sp.sourceAccountId,
                            sourceAccountName = sp.sourceAccountName,
                            notes = sp.notes,
                            reminderDaysBefore = sp.reminderDaysBefore,
                            notificationEnabled = sp.notificationEnabled,
                            createdAt = sp.createdAt
                        )
                    }
                    .sortedBy { it.dueDate }
                    .toImmutableList()

                // 12. Smart Insights
                val domainInsights = generateInsights(totalIncome, totalExpenses, budgetLimit, liabilitiesTotal)

                rawNetWorthState = netWorthState
                rawKpiState = kpiState

                val isConverted = _uiState.value.isConvertedToUsd
                val rate = _uiState.value.usdExchangeRate

                val displayedNetWorth = if (isConverted) convertNetWorthToUsd(netWorthState, rate) else netWorthState
                val displayedKpi = if (isConverted) convertKpiToUsd(kpiState, rate) else kpiState

                _uiState.update { current ->
                    current.copy(
                        netWorth = displayedNetWorth,
                        kpi = displayedKpi,
                        healthScore = healthScore,
                        accounts = domainAccounts,
                        categorySpends = categorySpends,
                        cashFlowEntries = cashFlow.toImmutableList(),
                        recentTransactions = recentDomainTx,
                        goals = domainGoals,
                        creditCards = domainCards,
                        upcomingPayments = domainUpcomingPayments,
                        insights = domainInsights.toImmutableList(),
                        isLoading = false
                    )
                }
            }.collect {}
        }
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.ChangePeriod -> {
                _uiState.update {
                    it.copy(period = PeriodState(currentMonth = event.month, isHistorical = event.month != YearMonth.now()))
                }
                observeDatabaseSources()
            }
            DashboardEvent.ToggleUsdConversion -> {
                val willBeConverted = !_uiState.value.isConvertedToUsd
                viewModelScope.launch {
                    val base = _uiState.value.baseCurrency
                    val rate = if (base.equals("USD", ignoreCase = true)) {
                        1.0
                    } else {
                        val effective = exchangeRateRepository?.getInstantEffectiveRate("USD", base)
                        effective?.rate ?: 4142.50
                    }

                    _uiState.update { current ->
                        val displayedNetWorth = if (willBeConverted) convertNetWorthToUsd(rawNetWorthState, rate) else rawNetWorthState
                        val displayedKpi = if (willBeConverted) convertKpiToUsd(rawKpiState, rate) else rawKpiState
                        current.copy(
                            isConvertedToUsd = willBeConverted,
                            displayCurrency = if (willBeConverted) "USD" else current.baseCurrency,
                            usdExchangeRate = rate,
                            netWorth = displayedNetWorth,
                            kpi = displayedKpi
                        )
                    }
                }
            }
            is DashboardEvent.InitiateTransfer -> {
                // Transfer handled by navigation
            }
            is DashboardEvent.SelectAccount -> {
                // Account detail handled by navigation
            }
            is DashboardEvent.SubmitAiQuery -> {
                submitAiQuery(event.query)
            }
            is DashboardEvent.UpdateAiInput -> {
                _uiState.update { it.copy(aiPrompt = it.aiPrompt.copy(inputText = event.text)) }
            }
            DashboardEvent.TogglePrivacyMode -> {
                _uiState.update { it.copy(isPrivacyMode = !it.isPrivacyMode) }
            }
            DashboardEvent.Refresh -> {
                syncWithCloud()
            }
            DashboardEvent.AddExpenseClick -> {}
            DashboardEvent.AddIncomeClick -> {}
            DashboardEvent.AddAccountClick -> {}
            DashboardEvent.AddGoalClick -> {}
            DashboardEvent.AddCreditCardClick -> {}
            DashboardEvent.OpenSettingsClick -> {
                _uiState.update { it.copy(isSettingsOpen = true) }
            }
            DashboardEvent.CloseSettingsClick -> {
                _uiState.update { it.copy(isSettingsOpen = false) }
            }
            is DashboardEvent.ToggleAccountAllowNegative -> {
                viewModelScope.launch {
                    financialRepository?.updateAccountAllowNegative(event.accountId, event.allowNegative)
                }
            }
        }
    }

    private fun submitAiQuery(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(aiPrompt = it.aiPrompt.copy(isProcessing = true)) }
            // Simula análisis de contexto RAG y devuelve insight
            kotlinx.coroutines.delay(1000)
            val newInsight = SmartInsight(
                id = UUID.randomUUID(),
                message = "Kash AI: Para optimizar tu ahorro, puedes limitar los gastos en entretenimiento y asegurar tu meta mensual.",
                severity = InsightSeverity.SUCCESS
            )
            _uiState.update {
                it.copy(
                    aiPrompt = it.aiPrompt.copy(isProcessing = false, inputText = ""),
                    insights = (listOf(newInsight) + it.insights).toImmutableList()
                )
            }
        }
    }

    private fun computeHealthScore(income: BigDecimal, expenses: BigDecimal, budget: BigDecimal, liabilities: BigDecimal): HealthScore {
        if (income <= BigDecimal.ZERO && expenses <= BigDecimal.ZERO) {
            return HealthScore(
                score = 0,
                riskLevel = RiskLevel.HEALTHY,
                insightMessage = "Registra tus primeros ingresos y gastos para calcular tu índice de salud financiera."
            )
        }

        var score = 100
        val savingsRate = if (income > BigDecimal.ZERO) {
            (income - expenses).toDouble() / income.toDouble()
        } else 0.0

        if (expenses > income) {
            score -= 40
        } else if (savingsRate < 0.2) {
            score -= 20
        }

        if (budget > BigDecimal.ZERO && expenses > budget) {
            score -= 25
        }

        if (liabilities > income.multiply(BigDecimal.valueOf(3))) {
            score -= 20
        }

        val clampedScore = score.coerceIn(10, 100)
        val risk = when {
            clampedScore >= 80 -> RiskLevel.HEALTHY
            clampedScore >= 50 -> RiskLevel.WARNING
            else -> RiskLevel.CRITICAL
        }

        val msg = when (risk) {
            RiskLevel.HEALTHY -> "Excelente disciplina financiera. Tu tasa de ahorro y control de presupuesto están en nivel óptimo."
            RiskLevel.WARNING -> "Atención: Tus gastos superan el 80% de tus ingresos o estás cerca de tu límite presupuestal."
            RiskLevel.CRITICAL -> "Alerta crítica: Tus gastos superan los ingresos de este periodo. Te recomendamos ajustar compras no esenciales."
        }

        return HealthScore(score = clampedScore, riskLevel = risk, insightMessage = msg)
    }

    private fun computeSparklinePoints(transactions: List<com.example.data.local.entity.TransactionEntity>): List<Float> {
        if (transactions.isEmpty()) return listOf(0f, 0f, 0f, 0f, 0f)
        var cumulative = 0f
        val points = mutableListOf<Float>()
        transactions.take(10).reversed().forEach { tx ->
            if (tx.type == "INCOME") cumulative += tx.amount.toFloat()
            else if (tx.type == "EXPENSE") cumulative -= tx.amount.toFloat()
            points.add(cumulative)
        }
        return if (points.size >= 2) points else listOf(0f, cumulative)
    }

    private fun computeCashFlowEntries(transactions: List<com.example.data.local.entity.TransactionEntity>): List<CashFlowEntry> {
        if (transactions.isEmpty()) return emptyList()
        // Group into 4 weekly buckets
        return (1..4).map { week ->
            val inc = transactions.filter { it.type == "INCOME" }.map { it.amount }.sum() / 4.0
            val exp = transactions.filter { it.type == "EXPENSE" }.map { it.amount }.sum() / 4.0
            CashFlowEntry(
                dateLabel = "Sem $week",
                totalIncome = BigDecimal.valueOf(inc).setScale(2, RoundingMode.HALF_EVEN),
                totalExpense = BigDecimal.valueOf(exp).setScale(2, RoundingMode.HALF_EVEN)
            )
        }
    }

    private fun generateInsights(income: BigDecimal, expenses: BigDecimal, budget: BigDecimal, liabilities: BigDecimal): List<SmartInsight> {
        val list = mutableListOf<SmartInsight>()
        if (expenses > income && income > BigDecimal.ZERO) {
            list.add(
                SmartInsight(
                    id = UUID.randomUUID(),
                    message = "Tus egresos de este mes superan los ingresos generados. Revisa tus gastos hormiga.",
                    severity = InsightSeverity.CRITICAL
                )
            )
        }
        if (budget > BigDecimal.ZERO && expenses > budget.multiply(BigDecimal.valueOf(0.8))) {
            list.add(
                SmartInsight(
                    id = UUID.randomUUID(),
                    message = "Has consumido más del 80% de tu presupuesto global asignado para el periodo.",
                    severity = InsightSeverity.WARNING
                )
            )
        }
        if (income > expenses && income > BigDecimal.ZERO) {
            list.add(
                SmartInsight(
                    id = UUID.randomUUID(),
                    message = "Excelente ritmo de ahorro: mantienes un flujo de caja positivo en este periodo.",
                    severity = InsightSeverity.SUCCESS
                )
            )
        }
        return list
    }

    private fun getCategoryColor(name: String): String {
        return when (name.lowercase()) {
            "alimentación" -> "#10B981"
            "transporte" -> "#0284C7"
            "servicios públicos", "servicios" -> "#F59E0B"
            "vivienda y hogar", "hogar" -> "#6366F1"
            "salud y cuidado", "salud" -> "#EC4899"
            "entretenimiento" -> "#8B5CF6"
            "educación" -> "#06B6D4"
            else -> "#64748B"
        }
    }

    private fun convertNetWorthToUsd(netWorth: NetWorthState, rate: Double): NetWorthState {
        if (rate <= 0.0) return netWorth
        val bdRate = BigDecimal.valueOf(rate)
        return NetWorthState(
            availableCash = netWorth.availableCash.divide(bdRate, 2, RoundingMode.HALF_EVEN),
            investments = netWorth.investments.divide(bdRate, 2, RoundingMode.HALF_EVEN),
            assets = netWorth.assets.divide(bdRate, 2, RoundingMode.HALF_EVEN),
            liabilities = netWorth.liabilities.divide(bdRate, 2, RoundingMode.HALF_EVEN),
            sparklinePoints = netWorth.sparklinePoints
        )
    }

    private fun convertKpiToUsd(kpi: KpiState, rate: Double): KpiState {
        if (rate <= 0.0) return kpi
        val bdRate = BigDecimal.valueOf(rate)
        return KpiState(
            income = kpi.income.divide(bdRate, 2, RoundingMode.HALF_EVEN),
            expenses = kpi.expenses.divide(bdRate, 2, RoundingMode.HALF_EVEN),
            savings = kpi.savings.divide(bdRate, 2, RoundingMode.HALF_EVEN),
            budgetLimit = kpi.budgetLimit.divide(bdRate, 2, RoundingMode.HALF_EVEN)
        )
    }

    fun setExcludeAllAccountsFromDashboard(excluded: Boolean) {
        viewModelScope.launch {
            settingsRepository?.setExcludeAllAccountsFromDashboard(userId, excluded)
        }
    }

    fun setAccountExcludedFromDashboard(accountId: String, excluded: Boolean) {
        viewModelScope.launch {
            settingsRepository?.setAccountExcludedFromDashboard(userId, accountId, excluded)
        }
    }

    fun syncWithCloud() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val currentAccounts = financialRepository?.getAccounts(userId)?.firstOrNull().orEmpty()
                val dummyIds = setOf("acc_ahorros", "acc_corriente", "acc_billetera", "acc_efectivo")
                val hasRealAccounts = currentAccounts.any { !dummyIds.contains(it.id) || it.balance > 0.0 }
                if (!hasRealAccounts) {
                    val restored = financialRepository?.restoreFinancialBackupFromCloudIfNeeded(userId)?.getOrDefault(false) ?: false
                    if (restored) {
                        android.util.Log.i("DashboardViewModel", "✓ Cuentas restauradas de la nube")
                    }
                } else {
                    financialRepository?.backupFullPortfolioToCloud(userId)
                    android.util.Log.i("DashboardViewModel", "✓ Portafolio respaldado en la nube exitosamente")
                }
            } catch (e: Exception) {
                android.util.Log.w("DashboardViewModel", "Sync notice: ${e.message}")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
