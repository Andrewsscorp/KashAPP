package com.example.presentation.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AccountCreditCardEntity
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.ScheduledPaymentEntity
import com.example.domain.repository.FinancialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class PaymentFilterTab(val label: String) {
    TODOS("Todos"),
    TARJETAS_CREDITO("Tarjetas de Crédito"),
    SERVICIOS_SUBSCRIPCIONES("Servicios & Subscripciones"),
    DEUDAS_PRESTAMOS("Deudas & Préstamos"),
    PAGADOS("Pagados")
}

enum class PaymentSortOrder(val label: String) {
    BY_DATE_ASC("Más próximo"),
    BY_AMOUNT_DESC("Mayor valor"),
    BY_AMOUNT_ASC("Menor valor"),
    BY_URGENCY("Más urgente")
}

enum class PaymentHorizonFilter(val label: String) {
    ALL("Todo"),
    NEXT_7_DAYS("Próximos 7 días"),
    NEXT_15_DAYS("Próximos 15 días"),
    THIS_MONTH("Este mes"),
    OVERDUE_ONLY("Solo vencidos")
}

data class UnifiedPaymentItem(
    val id: String,
    val title: String,
    val category: String, // "TARJETA_CREDITO", "SERVICIOS", "SUSCRIPCION", "DEUDA_PRESTAMO", "ARRIENDO", "OTRO"
    val amount: Double,
    val secondaryAmount: Double? = null, // e.g., Total Statement Balance vs Minimum Payment
    val currency: String = "COP",
    val dueDate: String, // YYYY-MM-DD
    val cutOffDate: String? = null, // for credit cards
    val isCreditCard: Boolean = false,
    val creditCardAccountId: String? = null,
    val isPaid: Boolean = false,
    val paidDate: String? = null,
    val recurrence: String = "MONTHLY",
    val notes: String? = null,
    val reminderDaysBefore: Int = 3,
    val daysUntilDue: Long = 0,
    val urgencyStatus: UrgencyStatus = UrgencyStatus.ON_TIME
)

enum class UrgencyStatus {
    OVERDUE, // Vencido (< 0 días)
    CRITICAL, // Crítico (0-3 días)
    UPCOMING, // Próximo (4-7 días)
    ON_TIME,  // Al día (> 7 días)
    PAID      // Ya pagado
}

data class UpcomingPaymentsUiState(
    val isLoading: Boolean = true,
    val filterTab: PaymentFilterTab = PaymentFilterTab.TODOS,
    val searchQuery: String = "",
    val sortOrder: PaymentSortOrder = PaymentSortOrder.BY_DATE_ASC,
    val horizonFilter: PaymentHorizonFilter = PaymentHorizonFilter.ALL,
    val rawScheduledPayments: List<ScheduledPaymentEntity> = emptyList(),
    val creditCards: List<Pair<AccountEntity, AccountCreditCardEntity>> = emptyList(),
    val availableAccounts: List<AccountEntity> = emptyList(),
    val displayItems: List<UnifiedPaymentItem> = emptyList(),
    // Configurable notification settings
    val notificationsEnabled: Boolean = true,
    val advanceNoticeDays: Int = 3,
    val notifyOnDueDate: Boolean = true,
    val notifyDailyIfOverdue: Boolean = true,
    val notifyCreditCards: Boolean = true,
    val notifyLoansAndDebts: Boolean = true,
    val notifyServices: Boolean = true,
    val preferredHour: Int = 9,
    val lastNotifRunTime: Long = 0L,
    val showConfigDialog: Boolean = false,
    val showCreatePaymentModal: Boolean = false,
    val showCcSimulatorDialog: Boolean = false,
    val selectedCcForSimulation: UnifiedPaymentItem? = null,
    val selectedItemForPayment: UnifiedPaymentItem? = null,
    val selectedItemForDetail: UnifiedPaymentItem? = null,
    val itemForEditing: UnifiedPaymentItem? = null,
    val isPrivacyMode: Boolean = false,
    val notificationMessage: String? = null,
    val errorMessage: String? = null,
    // KPIs & Cash Flow Projection
    val totalPendingAmount: Double = 0.0,
    val totalCreditCardDebt: Double = 0.0,
    val totalPaidThisMonth: Double = 0.0,
    val criticalAlertsCount: Int = 0,
    val totalLiquidBalance: Double = 0.0,
    val projectedRemainingBalance: Double = 0.0,
    val coveragePercent: Double = 100.0
)

class UpcomingPaymentsViewModel(
    private val financialRepository: FinancialRepository,
    val userId: String = "user_default"
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpcomingPaymentsUiState())
    val uiState: StateFlow<UpcomingPaymentsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                combine(
                    financialRepository.getScheduledPayments(userId),
                    financialRepository.getAccounts(userId)
                ) { scheduled, accounts ->
                    val cards = mutableListOf<Pair<AccountEntity, AccountCreditCardEntity>>()
                    for (acc in accounts) {
                        if (acc.type == "CREDIT_CARD" || acc.type == "CREDIT") {
                            val ccMeta = AccountCreditCardEntity(
                                accountId = acc.id,
                                quotaTotal = acc.balance.coerceAtLeast(0.0) + acc.availableBalance.coerceAtLeast(0.0),
                                quotaUsed = acc.balance.coerceAtLeast(0.0),
                                quotaAvailable = acc.availableBalance.coerceAtLeast(0.0),
                                minimumPaymentAmount = (acc.balance * 0.08).coerceAtLeast(150_000.0)
                            )
                            cards.add(Pair(acc, ccMeta))
                        }
                    }

                    val nonCreditAccounts = accounts.filter { it.type != "CREDIT_CARD" && it.type != "CREDIT" }
                    val unified = buildUnifiedList(scheduled, cards)

                    val totalPending = unified.filter { !it.isPaid }.sumOf { it.amount }
                    val totalCcDebt = cards.sumOf { it.first.balance }
                    val totalPaid = unified.filter { it.isPaid }.sumOf { it.amount }
                    val criticalCount = unified.count { !it.isPaid && (it.urgencyStatus == UrgencyStatus.CRITICAL || it.urgencyStatus == UrgencyStatus.OVERDUE) }

                    val liquidBalance = nonCreditAccounts.sumOf { it.balance.coerceAtLeast(0.0) }
                    val remaining = liquidBalance - totalPending
                    val coverage = if (totalPending > 0) (liquidBalance / totalPending) * 100.0 else 100.0

                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            rawScheduledPayments = scheduled,
                            creditCards = cards,
                            availableAccounts = nonCreditAccounts,
                            displayItems = filterAndSort(unified, state.filterTab, state.horizonFilter, state.searchQuery, state.sortOrder),
                            totalPendingAmount = totalPending,
                            totalCreditCardDebt = totalCcDebt,
                            totalPaidThisMonth = totalPaid,
                            criticalAlertsCount = criticalCount,
                            totalLiquidBalance = liquidBalance,
                            projectedRemainingBalance = remaining,
                            coveragePercent = coverage
                        )
                    }
                }.collectLatest {}
            } catch (ex: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = ex.message) }
            }
        }
    }

    private fun buildUnifiedList(
        scheduled: List<ScheduledPaymentEntity>,
        cards: List<Pair<AccountEntity, AccountCreditCardEntity>>
    ): List<UnifiedPaymentItem> {
        val today = LocalDate.now()
        val list = mutableListOf<UnifiedPaymentItem>()

        // 1. Map Credit Card obligations
        cards.forEach { (acc, cc) ->
            val dueDay = cc.paymentDueDay.coerceIn(1, 28)
            val cutDay = cc.cutOffDay.coerceIn(1, 28)

            val dueDate = try {
                val targetMonth = if (today.dayOfMonth > dueDay) today.monthValue + 1 else today.monthValue
                val targetYear = if (targetMonth > 12) today.year + 1 else today.year
                val safeMonth = if (targetMonth > 12) 1 else targetMonth
                LocalDate.of(targetYear, safeMonth, dueDay)
            } catch (_: Exception) {
                today.plusDays(7)
            }

            val cutDate = try {
                LocalDate.of(today.year, today.monthValue, cutDay).toString()
            } catch (_: Exception) {
                today.minusDays(5).toString()
            }

            val daysUntil = ChronoUnit.DAYS.between(today, dueDate)
            val urgency = when {
                daysUntil < 0 -> UrgencyStatus.OVERDUE
                daysUntil <= 3 -> UrgencyStatus.CRITICAL
                daysUntil <= 7 -> UrgencyStatus.UPCOMING
                else -> UrgencyStatus.ON_TIME
            }

            list.add(
                UnifiedPaymentItem(
                    id = "cc_${acc.id}",
                    title = "Tarjeta ${acc.name} (${cc.franchise})",
                    category = "TARJETA_CREDITO",
                    amount = cc.minimumPaymentAmount.coerceAtLeast(100_000.0),
                    secondaryAmount = acc.balance.coerceAtLeast(cc.minimumPaymentAmount),
                    currency = acc.currency,
                    dueDate = dueDate.toString(),
                    cutOffDate = cutDate,
                    isCreditCard = true,
                    creditCardAccountId = acc.id,
                    isPaid = acc.balance <= 0.0,
                    recurrence = "MONTHLY",
                    daysUntilDue = daysUntil,
                    urgencyStatus = if (acc.balance <= 0.0) UrgencyStatus.PAID else urgency
                )
            )
        }

        // 2. Map Custom Scheduled Payments
        scheduled.forEach { item ->
            val due = try {
                LocalDate.parse(item.dueDate.take(10))
            } catch (_: Exception) {
                today.plusDays(3)
            }
            val daysUntil = ChronoUnit.DAYS.between(today, due)
            val urgency = when {
                item.isPaid -> UrgencyStatus.PAID
                daysUntil < 0 -> UrgencyStatus.OVERDUE
                daysUntil <= 3 -> UrgencyStatus.CRITICAL
                daysUntil <= 7 -> UrgencyStatus.UPCOMING
                else -> UrgencyStatus.ON_TIME
            }

            list.add(
                UnifiedPaymentItem(
                    id = item.id,
                    title = item.entityName,
                    category = item.category,
                    amount = item.amount,
                    currency = item.currency,
                    dueDate = item.dueDate,
                    isCreditCard = false,
                    isPaid = item.isPaid,
                    paidDate = item.paidDate,
                    recurrence = item.recurrence,
                    notes = item.notes,
                    reminderDaysBefore = item.reminderDaysBefore,
                    daysUntilDue = daysUntil,
                    urgencyStatus = urgency
                )
            )
        }

        return list
    }

    private fun filterAndSort(
        items: List<UnifiedPaymentItem>,
        tab: PaymentFilterTab,
        horizon: PaymentHorizonFilter,
        query: String,
        sort: PaymentSortOrder
    ): List<UnifiedPaymentItem> {
        var filtered = when (tab) {
            PaymentFilterTab.TODOS -> items
            PaymentFilterTab.TARJETAS_CREDITO -> items.filter { it.isCreditCard || it.category == "TARJETA_CREDITO" }
            PaymentFilterTab.SERVICIOS_SUBSCRIPCIONES -> items.filter { it.category == "SERVICIOS" || it.category == "SUSCRIPCION" || it.category == "ARRIENDO" }
            PaymentFilterTab.DEUDAS_PRESTAMOS -> items.filter { it.category == "DEUDA_PRESTAMO" }
            PaymentFilterTab.PAGADOS -> items.filter { it.isPaid }
        }

        filtered = when (horizon) {
            PaymentHorizonFilter.ALL -> filtered
            PaymentHorizonFilter.NEXT_7_DAYS -> filtered.filter { !it.isPaid && it.daysUntilDue in 0..7 }
            PaymentHorizonFilter.NEXT_15_DAYS -> filtered.filter { !it.isPaid && it.daysUntilDue in 0..15 }
            PaymentHorizonFilter.THIS_MONTH -> filtered.filter { !it.isPaid && it.daysUntilDue in 0..30 }
            PaymentHorizonFilter.OVERDUE_ONLY -> filtered.filter { !it.isPaid && it.daysUntilDue < 0 }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filtered = filtered.filter {
                it.title.lowercase().contains(q) || it.category.lowercase().contains(q) || (it.notes?.lowercase()?.contains(q) == true)
            }
        }

        return when (sort) {
            PaymentSortOrder.BY_DATE_ASC -> filtered.sortedWith(compareBy<UnifiedPaymentItem> { it.isPaid }.thenBy { it.daysUntilDue })
            PaymentSortOrder.BY_AMOUNT_DESC -> filtered.sortedWith(compareBy<UnifiedPaymentItem> { it.isPaid }.thenByDescending { it.amount })
            PaymentSortOrder.BY_AMOUNT_ASC -> filtered.sortedWith(compareBy<UnifiedPaymentItem> { it.isPaid }.thenBy { it.amount })
            PaymentSortOrder.BY_URGENCY -> filtered.sortedWith(compareBy<UnifiedPaymentItem> { it.isPaid }.thenBy { it.urgencyStatus.ordinal })
        }
    }

    fun setFilterTab(tab: PaymentFilterTab) {
        _uiState.update { state ->
            val all = buildUnifiedList(state.rawScheduledPayments, state.creditCards)
            state.copy(
                filterTab = tab,
                displayItems = filterAndSort(all, tab, state.horizonFilter, state.searchQuery, state.sortOrder)
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val all = buildUnifiedList(state.rawScheduledPayments, state.creditCards)
            state.copy(
                searchQuery = query,
                displayItems = filterAndSort(all, state.filterTab, state.horizonFilter, query, state.sortOrder)
            )
        }
    }

    fun setHorizonFilter(horizon: PaymentHorizonFilter) {
        _uiState.update { state ->
            val all = buildUnifiedList(state.rawScheduledPayments, state.creditCards)
            state.copy(
                horizonFilter = horizon,
                displayItems = filterAndSort(all, state.filterTab, horizon, state.searchQuery, state.sortOrder)
            )
        }
    }

    fun setSortOrder(order: PaymentSortOrder) {
        _uiState.update { state ->
            val all = buildUnifiedList(state.rawScheduledPayments, state.creditCards)
            state.copy(
                sortOrder = order,
                displayItems = filterAndSort(all, state.filterTab, state.horizonFilter, state.searchQuery, order)
            )
        }
    }

    fun toggleConfigDialog(open: Boolean) {
        _uiState.update { it.copy(showConfigDialog = open) }
    }

    fun toggleCreatePaymentModal(open: Boolean) {
        _uiState.update { it.copy(showCreatePaymentModal = open) }
    }

    fun openDetailModal(item: UnifiedPaymentItem) {
        _uiState.update { it.copy(selectedItemForDetail = item) }
    }

    fun closeDetailModal() {
        _uiState.update { it.copy(selectedItemForDetail = null) }
    }

    fun openCcSimulator(item: UnifiedPaymentItem) {
        _uiState.update { it.copy(selectedCcForSimulation = item, showCcSimulatorDialog = true) }
    }

    fun closeCcSimulator() {
        _uiState.update { it.copy(selectedCcForSimulation = null, showCcSimulatorDialog = false) }
    }

    fun openEditPaymentModal(item: UnifiedPaymentItem) {
        _uiState.update { it.copy(itemForEditing = item) }
    }

    fun closeEditPaymentModal() {
        _uiState.update { it.copy(itemForEditing = null) }
    }

    fun loadNotificationPreferences(context: android.content.Context) {
        val prefs = com.example.presentation.payments.notification.UpcomingPaymentsNotificationPrefsManager(context).getConfig()
        _uiState.update {
            it.copy(
                notificationsEnabled = prefs.enabled,
                advanceNoticeDays = prefs.advanceNoticeDays,
                notifyOnDueDate = prefs.notifyOnDueDate,
                notifyDailyIfOverdue = prefs.notifyDailyIfOverdue,
                notifyCreditCards = prefs.notifyCreditCards,
                notifyLoansAndDebts = prefs.notifyLoansAndDebts,
                notifyServices = prefs.notifyServices,
                preferredHour = prefs.preferredHour,
                lastNotifRunTime = prefs.lastRunTimestamp
            )
        }
    }

    fun saveFullNotificationConfig(
        context: android.content.Context,
        config: com.example.presentation.payments.notification.PaymentNotificationConfig
    ) {
        val prefsManager = com.example.presentation.payments.notification.UpcomingPaymentsNotificationPrefsManager(context)
        prefsManager.saveConfig(config)

        // Reschedule or cancel WorkManager periodic alerts
        com.example.data.worker.UpcomingPaymentsWorkScheduler.schedulePeriodicAlerts(
            context = context,
            userId = userId,
            forceUpdate = true
        )

        _uiState.update {
            it.copy(
                notificationsEnabled = config.enabled,
                advanceNoticeDays = config.advanceNoticeDays,
                notifyOnDueDate = config.notifyOnDueDate,
                notifyDailyIfOverdue = config.notifyDailyIfOverdue,
                notifyCreditCards = config.notifyCreditCards,
                notifyLoansAndDebts = config.notifyLoansAndDebts,
                notifyServices = config.notifyServices,
                preferredHour = config.preferredHour,
                showConfigDialog = false,
                notificationMessage = if (config.enabled) {
                    "Alertas WorkManager configuradas: aviso ${config.advanceNoticeDays} días antes."
                } else {
                    "Alertas de pago desactivadas."
                }
            )
        }
    }

    fun triggerTestNotification(context: android.content.Context) {
        com.example.data.worker.UpcomingPaymentsWorkScheduler.triggerImmediateCheck(
            context = context,
            userId = userId,
            isTestNotification = true
        )
        _uiState.update {
            it.copy(notificationMessage = "Enviando alerta de prueba mediante WorkManager...")
        }
    }

    fun triggerImmediateScan(context: android.content.Context) {
        com.example.data.worker.UpcomingPaymentsWorkScheduler.triggerImmediateCheck(
            context = context,
            userId = userId,
            isTestNotification = false
        )
        _uiState.update {
            it.copy(notificationMessage = "Escaneo de vencimientos WorkManager en ejecución...")
        }
    }

    fun updateNotificationConfig(enabled: Boolean, daysBefore: Int, onDueDate: Boolean, dailyIfOverdue: Boolean) {
        _uiState.update {
            it.copy(
                notificationsEnabled = enabled,
                advanceNoticeDays = daysBefore,
                notifyOnDueDate = onDueDate,
                notifyDailyIfOverdue = dailyIfOverdue,
                showConfigDialog = false,
                notificationMessage = "Configuración de notificaciones guardada ($daysBefore días de anticipación)."
            )
        }
    }

    fun openPaymentModal(item: UnifiedPaymentItem) {
        _uiState.update { it.copy(selectedItemForPayment = item) }
    }

    fun closePaymentModal() {
        _uiState.update { it.copy(selectedItemForPayment = null) }
    }

    fun createScheduledPayment(
        entityName: String,
        category: String,
        amount: Double,
        dueDate: String,
        recurrence: String,
        sourceAccountId: String?,
        sourceAccountName: String?,
        notes: String?,
        reminderDaysBefore: Int
    ) {
        viewModelScope.launch {
            try {
                val entity = ScheduledPaymentEntity(
                    userId = userId,
                    entityName = entityName.trim(),
                    category = category,
                    amount = amount,
                    dueDate = dueDate,
                    recurrence = recurrence,
                    sourceAccountId = sourceAccountId,
                    sourceAccountName = sourceAccountName,
                    notes = notes?.trim(),
                    reminderDaysBefore = reminderDaysBefore,
                    notificationEnabled = true
                )
                financialRepository.createScheduledPayment(entity)
                _uiState.update {
                    it.copy(
                        showCreatePaymentModal = false,
                        notificationMessage = "Próximo pago '$entityName' registrado correctamente."
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al crear pago: ${ex.message}") }
            }
        }
    }

    fun updateScheduledPayment(
        id: String,
        entityName: String,
        category: String,
        amount: Double,
        dueDate: String,
        recurrence: String,
        notes: String?,
        reminderDaysBefore: Int
    ) {
        viewModelScope.launch {
            try {
                val entity = ScheduledPaymentEntity(
                    id = id,
                    userId = userId,
                    entityName = entityName.trim(),
                    category = category,
                    amount = amount,
                    dueDate = dueDate,
                    recurrence = recurrence,
                    notes = notes?.trim(),
                    reminderDaysBefore = reminderDaysBefore,
                    notificationEnabled = true
                )
                financialRepository.updateScheduledPayment(entity)
                _uiState.update {
                    it.copy(
                        itemForEditing = null,
                        notificationMessage = "Pago '$entityName' actualizado correctamente."
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al actualizar pago: ${ex.message}") }
            }
        }
    }

    fun togglePaymentPaidStatus(item: UnifiedPaymentItem) {
        viewModelScope.launch {
            try {
                if (item.isCreditCard) {
                    _uiState.update { it.copy(notificationMessage = "Para pagar la tarjeta, selecciona 'Registrar Pago en Cuenta'.") }
                    return@launch
                }
                val newPaidState = !item.isPaid
                val paidDate = if (newPaidState) LocalDate.now().toString() else null
                financialRepository.setScheduledPaymentPaid(item.id, newPaidState, paidDate)
                _uiState.update {
                    it.copy(notificationMessage = if (newPaidState) "Pago marcado como realizado." else "Pago marcado como pendiente.")
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(errorMessage = ex.message) }
            }
        }
    }

    fun executeDirectPayment(
        item: UnifiedPaymentItem,
        sourceAccountId: String,
        sourceAccountName: String,
        payAmount: Double,
        categoryName: String = "Pago de Servicios / Deudas"
    ) {
        viewModelScope.launch {
            try {
                val result = financialRepository.recordExpense(
                    userId = userId,
                    accountId = sourceAccountId,
                    accountName = sourceAccountName,
                    categoryId = "cat_payment",
                    categoryName = categoryName,
                    amount = payAmount,
                    currency = item.currency,
                    dateFormatted = LocalDate.now().toString(),
                    description = "Pago programado: ${item.title}",
                    receiptUrl = null,
                    isRecurring = false,
                    saveAsTemplate = false,
                    tags = listOf("proximo_pago", item.category.lowercase())
                )

                if (result.isSuccess) {
                    if (!item.isCreditCard) {
                        financialRepository.setScheduledPaymentPaid(item.id, true, LocalDate.now().toString())
                    }
                    _uiState.update {
                        it.copy(
                            selectedItemForPayment = null,
                            showCcSimulatorDialog = false,
                            notificationMessage = "Pago de $${"%,.0f".format(payAmount)} debitado exitosamente de $sourceAccountName."
                        )
                    }
                } else {
                    _uiState.update { it.copy(errorMessage = "No se pudo procesar el débito en la cuenta.") }
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al procesar pago: ${ex.message}") }
            }
        }
    }

    fun deleteScheduledPayment(id: String) {
        viewModelScope.launch {
            try {
                financialRepository.deleteScheduledPayment(id)
                _uiState.update { it.copy(notificationMessage = "Recordatorio de pago eliminado.") }
            } catch (ex: Exception) {
                _uiState.update { it.copy(errorMessage = ex.message) }
            }
        }
    }

    fun getSummaryShareText(): String {
        val state = _uiState.value
        val items = state.displayItems.filter { !it.isPaid }
        val sb = StringBuilder()
        sb.append("📋 *KashApp - Resumen de Próximos Pagos*\n")
        sb.append("💰 Total Pendiente: $ ${"%,.0f".format(state.totalPendingAmount)}\n")
        sb.append("💳 Deuda Tarjetas: $ ${"%,.0f".format(state.totalCreditCardDebt)}\n")
        sb.append("⚠️ Vencimientos Críticos: ${state.criticalAlertsCount}\n\n")
        sb.append("*Detalle de Obligaciones:*\n")
        items.take(10).forEach { item ->
            val status = when (item.urgencyStatus) {
                UrgencyStatus.OVERDUE -> "🔴 VENCIDO"
                UrgencyStatus.CRITICAL -> "🟠 CRÍTICO (${item.daysUntilDue}d)"
                UrgencyStatus.UPCOMING -> "🟡 PRÓXIMO (${item.daysUntilDue}d)"
                else -> "🟢 AL DÍA"
            }
            sb.append("• ${item.title}: $ ${"%,.0f".format(item.amount)} [Vence: ${item.dueDate}] - $status\n")
        }
        return sb.toString()
    }

    fun clearNotification() {
        _uiState.update { it.copy(notificationMessage = null, errorMessage = null) }
    }

    companion object {
        fun provideFactory(financialRepository: FinancialRepository, userId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return UpcomingPaymentsViewModel(financialRepository, userId) as T
                }
            }
    }
}
