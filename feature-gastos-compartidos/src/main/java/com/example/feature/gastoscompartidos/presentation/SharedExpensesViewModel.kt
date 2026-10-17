package com.example.feature.gastoscompartidos.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.result.Outcome
import com.example.core.text.TextNormalizer
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.SharedExpenseEntity
import com.example.data.local.entity.SharedExpensePayerEntity
import com.example.data.local.entity.SharedExpenseSplitEntity
import com.example.data.local.entity.SharedExpenseWithFullDetails
import com.example.data.local.entity.SharedGroupEntity
import com.example.data.local.entity.SharedParticipantEntity
import com.example.data.local.entity.SharedSettlementPaymentEntity
import com.example.domain.analytics.SharedExpenseFirebaseAiEngine
import com.example.domain.engine.ConsecutiveSequenceEngine
import com.example.domain.models.shared.BudgetImpactPolicy
import com.example.domain.models.shared.RealMoneyImpactMode
import com.example.domain.models.shared.ReimbursementAccountingTreatment
import com.example.domain.models.shared.RoundingRemainderTarget
import com.example.domain.models.shared.RoundingStepRule
import com.example.domain.models.shared.SettlementPaymentMethod
import com.example.domain.models.shared.SharedExpenseSettingsConfig
import com.example.domain.models.shared.SplitCalculationMethod
import com.example.domain.models.shared.TaxAndTipProrationRule
import com.example.domain.security.FirebaseApiProtectionGuard
import com.example.feature.gastoscompartidos.domain.engine.DebtSimplificationGraphEngine
import com.example.feature.gastoscompartidos.domain.engine.RealMoneyImpactEngine
import com.example.feature.gastoscompartidos.domain.engine.SharedSplitCalculatorEngine
import com.example.feature.gastoscompartidos.domain.ticket.SharedReceiptTicketEngine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

data class ParticipantBalanceSummaryUi(
    val participant: SharedParticipantEntity,
    val netBalanceWithUser: Double, // + me debe a mí / - yo le debo
    val pendingTicketsCount: Int,
    val oldestPendingDate: String?
)

data class SharedExpensesUiState(
    val selectedHubTabIndex: Int = 0,
    val searchQuery: String = "",
    val selectedSeriesFilter: String = "ALL", // "ALL" | "GC" | "TK" | "PG" | "LQ"
    // Posición Patrimonial Consolidada
    val totalReceivableNiif: Double = 0.0,
    val totalPayableNiif: Double = 0.0,
    val totalReservedInMentalHold: Double = 0.0,
    val netSharedPosition: Double = 0.0,
    // Listas Maestras
    val accounts: List<AccountEntity> = emptyList(),
    val participants: List<SharedParticipantEntity> = emptyList(),
    val participantBalances: List<ParticipantBalanceSummaryUi> = emptyList(),
    val groups: List<SharedGroupEntity> = emptyList(),
    val expensesWithDetails: List<SharedExpenseWithFullDetails> = emptyList(),
    val settlements: List<SharedSettlementPaymentEntity> = emptyList(),
    val graphSimplificationReport: DebtSimplificationGraphEngine.GraphSimplificationReport? = null,
    // Estado del Formulario de Creación / Simulación en Vivo
    val currentSequenceCounter: Long = 1L,
    val nextAutoExpenseConsecutive: String = "GC-${LocalDate.now().year}-0001",
    val nextAutoTicketConsecutive: String = "TK-${LocalDate.now().year}-0001",
    val nextPaymentVoucherPreview: String = "PG-${LocalDate.now().year}${String.format(Locale.US, "%02d", LocalDate.now().monthValue)}-0001",
    val manualConsecutiveOverride: String = "",
    val isManualConsecutiveActive: Boolean = false,
    val externalInvoiceNumberInput: String = "",
    val expenseTitleInput: String = "",
    val merchantNameInput: String = "",
    val merchantNitInput: String = "",
    val expenseDateIso: String = LocalDate.now().toString(),
    val dueDateForSettlementIso: String = LocalDate.now().plusDays(15).toString(),
    val selectedCategoryId: String = "cat_food",
    val selectedCategoryName: String = "Alimentación",
    val currencyCode: String = "COP",
    val subtotalInput: Double = 0.0,
    val ivaInput: Double = 0.0,
    val impoconsumoInput: Double = 0.0,
    val tipInput: Double = 0.0,
    val discountInput: Double = 0.0,
    val selectedPayerMode: String = "SELF_ONLY", // "SELF_ONLY" | "OTHER_PARTICIPANT" | "MULTI_PAYER"
    val selectedKashAccountId: String? = null,
    val creditCardInstallments: Int = 1,
    val passCreditCardInterestToGroup: Boolean = false,
    val selectedSplitMethod: SplitCalculationMethod = SplitCalculationMethod.EQUAL,
    val selectedTaxProrationRule: TaxAndTipProrationRule = TaxAndTipProrationRule.PROPORTIONAL_TO_SUBTOTAL,
    val selectedRoundingStep: RoundingStepRule = RoundingStepRule.ROUND_1,
    val selectedRemainderTarget: RoundingRemainderTarget = RoundingRemainderTarget.PAYER,
    val selectedRealMoneyMode: RealMoneyImpactMode = RealMoneyImpactMode.ACCRUAL_RECEIVABLE,
    val selectedBudgetPolicy: BudgetImpactPolicy = BudgetImpactPolicy.MY_NET_SHARE_ONLY,
    val autoReserveOwedInHold: Boolean = true,
    val splitParticipantsInput: List<SharedSplitCalculatorEngine.ParticipantSplitInput> = emptyList(),
    val ocrLineItemsInput: List<SharedSplitCalculatorEngine.OcrLineItemInput> = emptyList(),
    // Resultados en Tiempo Real para la UI
    val liveSplitCalculation: SharedSplitCalculatorEngine.SplitCalculationSummary? = null,
    val liveRealMoneySimulation: RealMoneyImpactEngine.RealMoneyImpactSimulation? = null,
    val liveValidationErrorMessage: String? = null,
    // Estado de Copiloto KashAI y Protección de API Firebase
    val aiNaturalPromptInput: String = "",
    val isAiProcessing: Boolean = false,
    val aiRecommendationBanner: SharedExpenseFirebaseAiEngine.AiAccountingModeRecommendation? = null,
    val apiProtectionStatus: FirebaseApiProtectionGuard.ApiProtectionStatus = FirebaseApiProtectionGuard.getProtectionTelemetry(),
    // Modales Activos y Ticket Seleccionado
    val activeDigitalTicketToView: SharedReceiptTicketEngine.SharedDigitalTicketModel? = null,
    val activeSettleParticipantId: String? = null,
    val isParticipantEditorVisible: Boolean = false,
    val editingParticipant: SharedParticipantEntity? = null,
    val settingsConfig: SharedExpenseSettingsConfig = SharedExpenseSettingsConfig()
)

sealed interface SharedExpensesSideEffect {
    data class ShowMessage(val message: String) : SharedExpensesSideEffect
    data class TicketExported(val ticketCode: String) : SharedExpensesSideEffect
}

sealed interface SharedExpensesIntent {
    data class SelectHubTab(val index: Int) : SharedExpensesIntent
    data class UpdateSearchQuery(val query: String) : SharedExpensesIntent
    data class SelectSeriesFilter(val series: String) : SharedExpensesIntent
    data class UpdateExpenseTitle(val title: String) : SharedExpensesIntent
    data class UpdateMerchantInfo(val merchantName: String, val nit: String = "") : SharedExpensesIntent
    data class UpdateExternalInvoiceNumber(val invoiceNumber: String) : SharedExpensesIntent
    data class UpdateExpenseDate(val dateIso: String) : SharedExpensesIntent
    data class UpdateDueDateForSettlement(val dateIso: String) : SharedExpensesIntent
    data class UpdateCurrency(val currencyCode: String) : SharedExpensesIntent
    data class UpdateCategory(val categoryId: String, val categoryName: String) : SharedExpensesIntent
    data class UpdateAmounts(
        val subtotal: Double,
        val iva: Double = 0.0,
        val impoconsumo: Double = 0.0,
        val tip: Double = 0.0,
        val discount: Double = 0.0
    ) : SharedExpensesIntent
    data class SelectPayerMode(val payerMode: String, val kashAccountId: String? = null) : SharedExpensesIntent
    data class SelectSplitMethod(val method: SplitCalculationMethod) : SharedExpensesIntent
    data class SelectRealMoneyImpactMode(val mode: RealMoneyImpactMode) : SharedExpensesIntent
    data class SelectBudgetPolicy(val policy: BudgetImpactPolicy) : SharedExpensesIntent
    data class ToggleMentalHoldReserve(val enabled: Boolean) : SharedExpensesIntent
    data class OverrideConsecutiveCode(val manualCode: String, val isManual: Boolean) : SharedExpensesIntent
    data class ToggleParticipantInclusion(val participantId: String) : SharedExpensesIntent
    data class UpdateParticipantSplitParameter(
        val participantId: String,
        val percentage: Double? = null,
        val shares: Double? = null,
        val exactAmount: Double? = null,
        val adjustment: Double? = null,
        val monthlyIncome: Double? = null
    ) : SharedExpensesIntent
    data class AssignOcrItemConsumer(val itemId: String, val participantId: String) : SharedExpensesIntent
    data class AssignOcrItemToAllConsumers(val itemId: String) : SharedExpensesIntent
    data class ExpandMultiUnitOcrItem(val itemId: String) : SharedExpensesIntent
    data class UpsertParticipantInDirectory(val participant: SharedParticipantEntity) : SharedExpensesIntent
    data class CreateSharedGroup(
        val name: String,
        val groupCode: String,
        val groupType: String
    ) : SharedExpensesIntent
    data class OpenParticipantEditor(val participant: SharedParticipantEntity? = null) : SharedExpensesIntent
    data object DismissParticipantEditor : SharedExpensesIntent
    data class OpenSettleDebtModal(val participantId: String) : SharedExpensesIntent
    data object DismissSettleDebtModal : SharedExpensesIntent
    data class ConfirmDebtSettlement(
        val participantId: String,
        val amount: Double,
        val method: SettlementPaymentMethod,
        val treatment: ReimbursementAccountingTreatment,
        val kashAccountId: String?,
        val referenceCode: String?,
        val paymentDateIso: String = LocalDate.now().toString()
    ) : SharedExpensesIntent
    data class RunKashAiNaturalLanguageSplit(val prompt: String) : SharedExpensesIntent
    data object SubmitAndEmitSharedExpenseTicket : SharedExpensesIntent
    data class OpenDigitalTicketPreview(val ticket: SharedReceiptTicketEngine.SharedDigitalTicketModel) : SharedExpensesIntent
    data object DismissDigitalTicketPreview : SharedExpensesIntent
    data class UpdateSettingsConfig(val newConfig: SharedExpenseSettingsConfig) : SharedExpensesIntent
}

class SharedExpensesViewModel(
    private val aiEngine: SharedExpenseFirebaseAiEngine = SharedExpenseFirebaseAiEngine(),
    private val sharedExpenseDao: com.example.data.local.dao.SharedExpenseDao? = null,
    private val financialDao: com.example.data.local.dao.FinancialDao? = null,
    private val userId: String = "user_default"
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharedExpensesUiState())
    val uiState: StateFlow<SharedExpensesUiState> = _uiState.asStateFlow()

    private val _sideEffects = MutableSharedFlow<SharedExpensesSideEffect>(extraBufferCapacity = 8)
    val sideEffects: SharedFlow<SharedExpensesSideEffect> = _sideEffects.asSharedFlow()

    init {
        seedInitialParticipantsIfNeeded()
        if (sharedExpenseDao != null) {
            viewModelScope.launch {
                sharedExpenseDao.observeActiveParticipants(userId).collect { list ->
                    if (list.isEmpty()) {
                        val defaults = buildDefaultParticipants(userId)
                        sharedExpenseDao.upsertParticipants(defaults)
                    } else {
                        _uiState.update { state ->
                            val splitInputs = mergeParticipantsIntoSplitInputs(list, state.splitParticipantsInput, state.subtotalInput)
                            state.copy(participants = list, splitParticipantsInput = splitInputs)
                        }
                        recalculateLiveSimulation()
                        rebuildBalancesAndGraph()
                    }
                }
            }
            viewModelScope.launch {
                sharedExpenseDao.observeGroups(userId).collect { groups ->
                    if (groups.isEmpty()) {
                        val defaultGroup = SharedGroupEntity(
                            id = "grp_default_apto",
                            userId = userId,
                            name = "Hogar & Gastos Compartidos",
                            nameNormalized = "hogar & gastos compartidos",
                            groupCode = "HOGAR",
                            groupType = "HOME",
                            baseCurrency = "COP",
                            simplifyDebtsEnabled = true
                        )
                        sharedExpenseDao.upsertGroup(defaultGroup)
                    } else {
                        _uiState.update { it.copy(groups = groups) }
                    }
                }
            }
            viewModelScope.launch {
                sharedExpenseDao.observeSharedExpensesWithDetails(userId).collect { expenses ->
                    val nextSeq = (expenses.size + 1).toLong()
                    val year = LocalDate.now().year
                    _uiState.update {
                        it.copy(
                            expensesWithDetails = expenses,
                            currentSequenceCounter = nextSeq,
                            nextAutoExpenseConsecutive = String.format(Locale.US, "GC-%d-%04d", year, nextSeq),
                            nextAutoTicketConsecutive = String.format(Locale.US, "TK-%d-%04d", year, nextSeq)
                        )
                    }
                    rebuildBalancesAndGraph()
                }
            }
            viewModelScope.launch {
                sharedExpenseDao.observeTotalAccountsReceivableNiif(userId).collect { rec ->
                    _uiState.update {
                        it.copy(
                            totalReceivableNiif = rec,
                            netSharedPosition = rec - it.totalPayableNiif
                        )
                    }
                }
            }
            viewModelScope.launch {
                sharedExpenseDao.observeTotalAccountsPayableNiif(userId).collect { pay ->
                    _uiState.update {
                        it.copy(
                            totalPayableNiif = pay,
                            netSharedPosition = it.totalReceivableNiif - pay
                        )
                    }
                }
            }
            viewModelScope.launch {
                sharedExpenseDao.observeTotalMentalHoldReserved(userId).collect { hold ->
                    _uiState.update { it.copy(totalReservedInMentalHold = hold) }
                }
            }
        }
        if (financialDao != null) {
            viewModelScope.launch {
                financialDao.getAccountsFlow(userId).collect { accounts ->
                    _uiState.update { state ->
                        state.copy(
                            accounts = accounts,
                            selectedKashAccountId = state.selectedKashAccountId ?: accounts.firstOrNull()?.id
                        )
                    }
                    recalculateLiveSimulation()
                }
            }
        }
    }

    private fun seedInitialParticipantsIfNeeded() {
        val defaults = buildDefaultParticipants(userId)
        val defaultGroup = SharedGroupEntity(
            id = "grp_default_apto",
            userId = userId,
            name = "Hogar & Gastos Compartidos",
            nameNormalized = "hogar & gastos compartidos",
            groupCode = "HOGAR",
            groupType = "HOME",
            baseCurrency = "COP",
            simplifyDebtsEnabled = true
        )
        val initialOcrItems = listOf(
            SharedSplitCalculatorEngine.OcrLineItemInput(
                itemId = "item_1",
                description = "Plato Fuerte / Entrada Compartida",
                quantity = 2.0,
                unitPrice = 45000.0,
                lineTotalPrice = 90000.0,
                assignedParticipantIds = defaults.map { it.id }
            ),
            SharedSplitCalculatorEngine.OcrLineItemInput(
                itemId = "item_2",
                description = "Bebidas & Postres",
                quantity = 3.0,
                unitPrice = 20000.0,
                lineTotalPrice = 60000.0,
                assignedParticipantIds = defaults.map { it.id }
            )
        )
        _uiState.update { state ->
            val splitInputs = mergeParticipantsIntoSplitInputs(defaults, emptyList(), 150000.0)
            state.copy(
                participants = defaults,
                groups = listOf(defaultGroup),
                splitParticipantsInput = splitInputs,
                ocrLineItemsInput = initialOcrItems
            )
        }
    }

    private fun buildDefaultParticipants(ownerUserId: String): List<SharedParticipantEntity> = listOf(
        SharedParticipantEntity(
            id = "SELF_USER",
            userId = ownerUserId,
            isSelf = true,
            displayName = "Yo (Titular KashApp)",
            nameNormalized = "YO TITULAR KASHAPP",
            aliasOrRelation = "Titular",
            preferredPaymentMethod = SettlementPaymentMethod.BRE_B.code,
            bankName = "Bancolombia / Bre-B",
            accountNumberOrKey = "@titular.kash",
            monthlyIncomeEstimate = 6_500_000.0,
            avatarColorHex = "#0284C7",
            avatarInitials = "YO"
        ),
        SharedParticipantEntity(
            id = "part_ana",
            userId = ownerUserId,
            isSelf = false,
            displayName = "Ana María Gómez",
            nameNormalized = "ANA MARIA GOMEZ",
            aliasOrRelation = "Socia / Hogar",
            phoneOrWhatsApp = "+57 300 412 8890",
            preferredPaymentMethod = SettlementPaymentMethod.NEQUI.code,
            bankName = "Nequi",
            accountNumberOrKey = "300 412 8890",
            monthlyIncomeEstimate = 4_800_000.0,
            avatarColorHex = "#0D9488",
            avatarInitials = "AG",
            creditTrustLimit = 800_000.0
        ),
        SharedParticipantEntity(
            id = "part_carlos",
            userId = ownerUserId,
            isSelf = false,
            displayName = "Carlos Andrés Ruiz",
            nameNormalized = "CARLOS ANDRES RUIZ",
            aliasOrRelation = "Compañero",
            phoneOrWhatsApp = "+57 311 590 2341",
            preferredPaymentMethod = SettlementPaymentMethod.BRE_B.code,
            bankName = "Bancolombia",
            accountNumberOrKey = "@carlos.breb",
            monthlyIncomeEstimate = 4_200_000.0,
            avatarColorHex = "#7C3AED",
            avatarInitials = "CR",
            creditTrustLimit = 600_000.0
        )
    )

    private fun mergeParticipantsIntoSplitInputs(
        participants: List<SharedParticipantEntity>,
        existingInputs: List<SharedSplitCalculatorEngine.ParticipantSplitInput>,
        currentTotal: Double
    ): List<SharedSplitCalculatorEngine.ParticipantSplitInput> {
        val count = participants.size.coerceAtLeast(1)
        val defaultPct = 100.0 / count
        return participants.map { p ->
            val existing = existingInputs.find { it.participantId == p.id }
            SharedSplitCalculatorEngine.ParticipantSplitInput(
                participantId = p.id,
                displayName = p.displayName,
                isSelf = p.isSelf,
                isIncluded = existing?.isIncluded ?: true,
                paidAmountAtRegister = if (p.isSelf) currentTotal else 0.0,
                percentageInput = existing?.percentageInput ?: defaultPct,
                sharesUnitsInput = existing?.sharesUnitsInput ?: 1.0,
                exactAmountInput = existing?.exactAmountInput ?: (currentTotal / count),
                adjustmentAmountInput = existing?.adjustmentAmountInput ?: 0.0,
                monthlyIncomeInput = existing?.monthlyIncomeInput?.takeIf { it > 0.0 } ?: p.monthlyIncomeEstimate.takeIf { it > 0.0 } ?: 3_500_000.0,
                creditTrustLimit = p.creditTrustLimit
            )
        }
    }

    fun onIntent(intent: SharedExpensesIntent) {
        when (intent) {
            is SharedExpensesIntent.SelectHubTab ->
                _uiState.update { it.copy(selectedHubTabIndex = intent.index) }

            is SharedExpensesIntent.UpdateSearchQuery ->
                _uiState.update { it.copy(searchQuery = intent.query) }

            is SharedExpensesIntent.SelectSeriesFilter ->
                _uiState.update { it.copy(selectedSeriesFilter = intent.series) }

            is SharedExpensesIntent.UpdateExpenseTitle ->
                _uiState.update { it.copy(expenseTitleInput = intent.title) }

            is SharedExpensesIntent.UpdateMerchantInfo ->
                _uiState.update {
                    it.copy(
                        merchantNameInput = intent.merchantName,
                        merchantNitInput = intent.nit
                    )
                }

            is SharedExpensesIntent.UpdateExternalInvoiceNumber ->
                _uiState.update { it.copy(externalInvoiceNumberInput = intent.invoiceNumber) }

            is SharedExpensesIntent.UpdateExpenseDate ->
                _uiState.update { it.copy(expenseDateIso = intent.dateIso) }

            is SharedExpensesIntent.UpdateDueDateForSettlement ->
                _uiState.update { it.copy(dueDateForSettlementIso = intent.dateIso) }

            is SharedExpensesIntent.UpdateCurrency -> {
                _uiState.update { it.copy(currencyCode = intent.currencyCode) }
                recalculateLiveSimulation()
            }

            is SharedExpensesIntent.UpdateCategory ->
                _uiState.update {
                    it.copy(
                        selectedCategoryId = intent.categoryId,
                        selectedCategoryName = intent.categoryName
                    )
                }

            is SharedExpensesIntent.UpdateAmounts -> {
                _uiState.update { state ->
                    val grossTotal = (intent.subtotal + intent.iva + intent.impoconsumo + intent.tip - intent.discount).coerceAtLeast(0.0)
                    val updatedParticipants = state.splitParticipantsInput.map { p ->
                        val paid = when (state.selectedPayerMode) {
                            "SELF_ONLY" -> if (p.isSelf) grossTotal else 0.0
                            "OTHER_PARTICIPANT" -> if (!p.isSelf && p.participantId == state.splitParticipantsInput.firstOrNull { !it.isSelf }?.participantId) grossTotal else 0.0
                            else -> if (p.isSelf) grossTotal * 0.5 else if (p.participantId == state.splitParticipantsInput.firstOrNull { !it.isSelf }?.participantId) grossTotal * 0.5 else 0.0
                        }
                        p.copy(paidAmountAtRegister = paid)
                    }
                    state.copy(
                        subtotalInput = intent.subtotal,
                        ivaInput = intent.iva,
                        impoconsumoInput = intent.impoconsumo,
                        tipInput = intent.tip,
                        discountInput = intent.discount,
                        splitParticipantsInput = updatedParticipants
                    )
                }
                recalculateLiveSimulation()
            }

            is SharedExpensesIntent.SelectPayerMode -> {
                _uiState.update { state ->
                    val grossTotal = (state.subtotalInput + state.ivaInput + state.impoconsumoInput + state.tipInput - state.discountInput).coerceAtLeast(0.0)
                    val updatedParticipants = state.splitParticipantsInput.map { p ->
                        val paid = when (intent.payerMode) {
                            "SELF_ONLY" -> if (p.isSelf) grossTotal else 0.0
                            "OTHER_PARTICIPANT" -> if (!p.isSelf && p.participantId == state.splitParticipantsInput.firstOrNull { !it.isSelf }?.participantId) grossTotal else 0.0
                            else -> if (p.isSelf) grossTotal * 0.5 else if (p.participantId == state.splitParticipantsInput.firstOrNull { !it.isSelf }?.participantId) grossTotal * 0.5 else 0.0
                        }
                        p.copy(paidAmountAtRegister = paid)
                    }
                    state.copy(
                        selectedPayerMode = intent.payerMode,
                        selectedKashAccountId = intent.kashAccountId ?: state.selectedKashAccountId,
                        splitParticipantsInput = updatedParticipants
                    )
                }
                recalculateLiveSimulation()
            }

            is SharedExpensesIntent.SelectSplitMethod -> {
                _uiState.update { state ->
                    val grossTotal = (state.subtotalInput + state.ivaInput + state.impoconsumoInput + state.tipInput - state.discountInput).coerceAtLeast(0.0)
                    val includedCount = state.splitParticipantsInput.count { it.isIncluded }.coerceAtLeast(1)
                    val updatedParticipants = state.splitParticipantsInput.mapIndexed { idx, p ->
                        if (!p.isIncluded) p
                        else p.copy(
                            percentageInput = if (idx == 0) 100.0 - (100.0 / includedCount).toInt() * (includedCount - 1) else (100.0 / includedCount).toInt().toDouble(),
                            exactAmountInput = if (idx == 0) grossTotal - (grossTotal / includedCount).toLong() * (includedCount - 1) else (grossTotal / includedCount).toLong().toDouble()
                        )
                    }
                    state.copy(
                        selectedSplitMethod = intent.method,
                        splitParticipantsInput = updatedParticipants
                    )
                }
                recalculateLiveSimulation()
            }

            is SharedExpensesIntent.SelectRealMoneyImpactMode -> {
                _uiState.update { it.copy(selectedRealMoneyMode = intent.mode) }
                recalculateLiveSimulation()
            }

            is SharedExpensesIntent.SelectBudgetPolicy -> {
                _uiState.update { it.copy(selectedBudgetPolicy = intent.policy) }
                recalculateLiveSimulation()
            }

            is SharedExpensesIntent.ToggleMentalHoldReserve -> {
                _uiState.update { it.copy(autoReserveOwedInHold = intent.enabled) }
                recalculateLiveSimulation()
            }

            is SharedExpensesIntent.OverrideConsecutiveCode -> {
                _uiState.update {
                    it.copy(
                        manualConsecutiveOverride = intent.manualCode.uppercase().trim(),
                        isManualConsecutiveActive = intent.isManual && intent.manualCode.isNotBlank()
                    )
                }
            }

            is SharedExpensesIntent.ToggleParticipantInclusion -> {
                _uiState.update { state ->
                    val updated = state.splitParticipantsInput.map { p ->
                        if (p.participantId == intent.participantId) {
                            p.copy(isIncluded = !p.isIncluded)
                        } else p
                    }
                    // Ensure at least 1 participant stays included
                    val safeUpdated = if (updated.none { it.isIncluded }) state.splitParticipantsInput else updated
                    state.copy(splitParticipantsInput = safeUpdated)
                }
                recalculateLiveSimulation()
            }

            is SharedExpensesIntent.UpdateParticipantSplitParameter -> {
                _uiState.update { state ->
                    val updated = state.splitParticipantsInput.map { p ->
                        if (p.participantId == intent.participantId) {
                            p.copy(
                                percentageInput = intent.percentage ?: p.percentageInput,
                                sharesUnitsInput = intent.shares ?: p.sharesUnitsInput,
                                exactAmountInput = intent.exactAmount ?: p.exactAmountInput,
                                adjustmentAmountInput = intent.adjustment ?: p.adjustmentAmountInput,
                                monthlyIncomeInput = intent.monthlyIncome ?: p.monthlyIncomeInput
                            )
                        } else p
                    }
                    state.copy(splitParticipantsInput = updated)
                }
                recalculateLiveSimulation()
            }

            is SharedExpensesIntent.AssignOcrItemConsumer -> {
                _uiState.update { state ->
                    val updatedItems = state.ocrLineItemsInput.map { item ->
                        if (item.itemId == intent.itemId) {
                            val current = item.assignedParticipantIds.toMutableList()
                            if (current.contains(intent.participantId)) {
                                current.remove(intent.participantId)
                            } else {
                                current.add(intent.participantId)
                            }
                            item.copy(assignedParticipantIds = current)
                        } else item
                    }
                    val newSubtotal = updatedItems.sumOf { it.lineTotalPrice }
                    state.copy(
                        ocrLineItemsInput = updatedItems,
                        subtotalInput = if (newSubtotal > 0.0) newSubtotal else state.subtotalInput
                    )
                }
                recalculateLiveSimulation()
            }

            is SharedExpensesIntent.AssignOcrItemToAllConsumers -> {
                _uiState.update { state ->
                    val allIds = state.participants.map { it.id }.ifEmpty {
                        state.splitParticipantsInput.map { it.participantId }
                    }
                    val updatedItems = state.ocrLineItemsInput.map { item ->
                        if (item.itemId == intent.itemId) {
                            item.copy(assignedParticipantIds = allIds)
                        } else item
                    }
                    state.copy(ocrLineItemsInput = updatedItems)
                }
                recalculateLiveSimulation()
            }

            is SharedExpensesIntent.ExpandMultiUnitOcrItem -> {
                _uiState.update { state ->
                    val expanded = mutableListOf<SharedSplitCalculatorEngine.OcrLineItemInput>()
                    for (item in state.ocrLineItemsInput) {
                        val qtyInt = item.quantity.toInt()
                        if (item.itemId == intent.itemId && qtyInt in 2..12) {
                            val unitPrice = item.lineTotalPrice / qtyInt
                            for (u in 1..qtyInt) {
                                expanded.add(
                                    item.copy(
                                        itemId = "${item.itemId}_u$u",
                                        description = "${item.description} ($u/$qtyInt)",
                                        quantity = 1.0,
                                        unitPrice = unitPrice,
                                        lineTotalPrice = unitPrice
                                    )
                                )
                            }
                        } else {
                            expanded.add(item)
                        }
                    }
                    state.copy(ocrLineItemsInput = expanded)
                }
                recalculateLiveSimulation()
            }

            is SharedExpensesIntent.UpsertParticipantInDirectory -> {
                viewModelScope.launch {
                    sharedExpenseDao?.upsertParticipant(intent.participant)
                }
                _uiState.update { state ->
                    val updatedParticipants = state.participants
                        .filterNot { it.id == intent.participant.id } + intent.participant
                    val updatedSplitInputs = mergeParticipantsIntoSplitInputs(
                        updatedParticipants,
                        state.splitParticipantsInput,
                        state.subtotalInput
                    )
                    state.copy(
                        participants = updatedParticipants,
                        splitParticipantsInput = updatedSplitInputs,
                        isParticipantEditorVisible = false,
                        editingParticipant = null
                    )
                }
                recalculateLiveSimulation()
                rebuildBalancesAndGraph()
            }

            is SharedExpensesIntent.CreateSharedGroup -> {
                val cleanName = intent.name.trim()
                if (cleanName.isNotEmpty()) {
                    val cleanCode = intent.groupCode.trim().uppercase().take(8).ifBlank { "GRP" }
                    val newGroup = SharedGroupEntity(
                        id = "grp_${UUID.randomUUID().toString().take(8)}",
                        userId = userId,
                        name = cleanName,
                        nameNormalized = cleanName.lowercase(),
                        groupCode = cleanCode,
                        groupType = intent.groupType,
                        baseCurrency = _uiState.value.currencyCode,
                        simplifyDebtsEnabled = true
                    )
                    viewModelScope.launch {
                        sharedExpenseDao?.upsertGroup(newGroup)
                    }
                    _uiState.update { state ->
                        state.copy(groups = state.groups + newGroup)
                    }
                }
            }

            is SharedExpensesIntent.OpenParticipantEditor ->
                _uiState.update {
                    it.copy(
                        isParticipantEditorVisible = true,
                        editingParticipant = intent.participant
                    )
                }

            is SharedExpensesIntent.DismissParticipantEditor ->
                _uiState.update {
                    it.copy(
                        isParticipantEditorVisible = false,
                        editingParticipant = null
                    )
                }

            is SharedExpensesIntent.OpenSettleDebtModal ->
                _uiState.update { it.copy(activeSettleParticipantId = intent.participantId) }

            is SharedExpensesIntent.DismissSettleDebtModal ->
                _uiState.update { it.copy(activeSettleParticipantId = null) }

            is SharedExpensesIntent.ConfirmDebtSettlement -> {
                handleConfirmDebtSettlement(intent)
            }

            is SharedExpensesIntent.RunKashAiNaturalLanguageSplit -> {
                executeAiNaturalLanguageSplit(intent.prompt)
            }

            is SharedExpensesIntent.SubmitAndEmitSharedExpenseTicket -> {
                submitAndEmitSharedExpense()
            }

            is SharedExpensesIntent.OpenDigitalTicketPreview ->
                _uiState.update { it.copy(activeDigitalTicketToView = intent.ticket) }

            is SharedExpensesIntent.DismissDigitalTicketPreview ->
                _uiState.update { it.copy(activeDigitalTicketToView = null) }

            is SharedExpensesIntent.UpdateSettingsConfig -> {
                val cfg = intent.newConfig
                val previewExp = ConsecutiveSequenceEngine.formatPattern(
                    patternTemplate = cfg.expenseConsecutivePattern,
                    prefix = "GC",
                    sequenceNumber = _uiState.value.currentSequenceCounter,
                    referenceDate = LocalDate.now(),
                    groupCode = "HOGAR",
                    userInitials = "YO"
                )
                val previewTk = ConsecutiveSequenceEngine.formatPattern(
                    patternTemplate = cfg.receiptTicketPattern,
                    prefix = "TK",
                    sequenceNumber = _uiState.value.currentSequenceCounter,
                    referenceDate = LocalDate.now(),
                    groupCode = "HOGAR",
                    userInitials = "YO"
                )
                _uiState.update {
                    it.copy(
                        settingsConfig = cfg,
                        nextAutoExpenseConsecutive = previewExp,
                        nextAutoTicketConsecutive = previewTk
                    )
                }
            }
        }
    }

    private fun submitAndEmitSharedExpense() {
        val state = _uiState.value
        val effectiveSubtotal = if (state.subtotalInput > 0.0) state.subtotalInput else 120_000.0
        val grossTotal = (effectiveSubtotal + state.ivaInput + state.impoconsumoInput + state.tipInput - state.discountInput).coerceAtLeast(1.0)

        // Force live calculation with effectiveSubtotal
        val participantsWithPaid = state.splitParticipantsInput.map { p ->
            val paid = when (state.selectedPayerMode) {
                "SELF_ONLY" -> if (p.isSelf) grossTotal else 0.0
                "OTHER_PARTICIPANT" -> if (!p.isSelf && p.participantId == state.splitParticipantsInput.firstOrNull { !it.isSelf }?.participantId) grossTotal else 0.0
                else -> if (p.isSelf) grossTotal * 0.5 else if (p.participantId == state.splitParticipantsInput.firstOrNull { !it.isSelf }?.participantId) grossTotal * 0.5 else 0.0
            }
            p.copy(paidAmountAtRegister = paid)
        }

        val splitReq = SharedSplitCalculatorEngine.SplitCalculationRequest(
            subtotalAmount = effectiveSubtotal,
            taxIvaAmount = state.ivaInput,
            taxConsumptionAmount = state.impoconsumoInput,
            tipAmount = state.tipInput,
            discountAmount = state.discountInput,
            splitMethod = state.selectedSplitMethod,
            taxAndTipProrationRule = state.selectedTaxProrationRule,
            roundingStepRule = state.selectedRoundingStep,
            roundingRemainderTarget = state.selectedRemainderTarget,
            participants = participantsWithPaid,
            ocrLineItems = state.ocrLineItemsInput,
            consecutiveSequenceNumber = state.currentSequenceCounter
        )

        val splitSummary = (SharedSplitCalculatorEngine.calculateSplits(splitReq) as? Outcome.Success)?.value ?: return
        val expCode = if (state.isManualConsecutiveActive && state.manualConsecutiveOverride.isNotBlank()) {
            state.manualConsecutiveOverride
        } else {
            state.nextAutoExpenseConsecutive
        }
        val tkCode = state.nextAutoTicketConsecutive
        val titleText = state.expenseTitleInput.trim().ifBlank { "Gasto Compartido (${state.selectedCategoryName})" }
        val merchantText = state.merchantNameInput.trim().ifBlank { "Establecimiento Comercial" }
        val timeFormatted = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
        val sharesSig = splitSummary.participantSplits.joinToString(";") { "${it.participantId}:${it.totalOwedShare}" }
        val integrityHash = ConsecutiveSequenceEngine.computeTicketIntegrityHash(
            consecutiveCode = expCode,
            receiptTicketCode = tkCode,
            totalAmount = splitSummary.effectiveTotalAmount,
            currency = state.currencyCode,
            expenseDate = state.expenseDateIso,
            participantSharesSignature = sharesSig
        )

        val mySplit = splitSummary.participantSplits.find { it.isSelf || it.participantId == "SELF_USER" }
        val myShare = mySplit?.totalOwedShare ?: 0.0
        val myPaid = mySplit?.paidByThisParticipant ?: 0.0
        val myNetDelta = myPaid - myShare
        val expenseId = "sexp_${UUID.randomUUID().toString().take(8)}"

        val expenseEntity = SharedExpenseEntity(
            id = expenseId,
            userId = userId,
            groupId = state.groups.firstOrNull()?.id,
            consecutiveCode = expCode,
            consecutiveNormalized = TextNormalizer.normalizeForUniqueness(expCode),
            isManualConsecutive = state.isManualConsecutiveActive,
            externalInvoiceNumber = state.externalInvoiceNumberInput.ifBlank { null },
            receiptTicketCode = tkCode,
            ticketIntegrityHash = integrityHash,
            title = titleText,
            description = titleText,
            merchantNameSnapshot = merchantText,
            merchantNit = state.merchantNitInput.ifBlank { null },
            categoryId = state.selectedCategoryId,
            categoryNameSnapshot = state.selectedCategoryName,
            subtotalAmount = effectiveSubtotal,
            taxIvaAmount = state.ivaInput,
            taxConsumptionAmount = state.impoconsumoInput,
            tipAmount = state.tipInput,
            discountAmount = state.discountInput,
            gmfAmount = 0.0,
            totalAmount = splitSummary.effectiveTotalAmount,
            currency = state.currencyCode,
            appliedExchangeRate = 1.0,
            splitMethod = state.selectedSplitMethod.code,
            taxAndTipProrationRule = state.selectedTaxProrationRule.code,
            roundingRule = state.selectedRoundingStep.code,
            roundingRemainderTarget = state.selectedRemainderTarget.code,
            realMoneyImpactMode = state.selectedRealMoneyMode.code,
            budgetImpactPolicy = state.selectedBudgetPolicy.code,
            holdReservedAmount = if (myNetDelta < 0.0 && state.autoReserveOwedInHold) abs(myNetDelta) else 0.0,
            myPaidAmount = myPaid,
            myConsumptionShareAmount = myShare,
            myNetPositionDelta = myNetDelta,
            expenseDate = state.expenseDateIso,
            expenseTime = timeFormatted,
            dueDateForSettlement = state.dueDateForSettlementIso,
            status = "PENDING_SETTLEMENT",
            idempotencyKey = "idem_$expenseId"
        )

        val splitEntities = splitSummary.participantSplits.filter { it.isIncluded }.map { s ->
            SharedExpenseSplitEntity(
                id = "ssplit_${UUID.randomUUID().toString().take(8)}",
                sharedExpenseId = expenseId,
                participantId = s.participantId,
                isIncluded = s.isIncluded,
                rawInputWeightOrPct = s.rawWeightOrPercentage,
                baseSubtotalShare = s.baseSubtotalShare,
                allocatedTaxAmount = s.allocatedTaxAmount,
                allocatedTipAmount = s.allocatedTipAmount,
                allocatedDiscountAmount = s.allocatedDiscountAmount,
                allocatedGmfAmount = s.allocatedGmfAndFinancialAmount,
                roundingAdjustment = s.roundingAdjustment,
                totalOwedShare = s.totalOwedShare,
                paidByThisParticipant = s.paidByThisParticipant,
                netBalanceForExpense = s.netBalanceForExpense,
                amountAlreadySettled = 0.0,
                remainingPendingAmount = s.remainingPendingAmount,
                settlementStatus = s.initialSettlementStatus.name
            )
        }

        val payerEntities = participantsWithPaid.filter { it.paidAmountAtRegister > 0.0 }.map { p ->
            SharedExpensePayerEntity(
                id = "spayer_${UUID.randomUUID().toString().take(8)}",
                sharedExpenseId = expenseId,
                participantId = p.participantId,
                kashAccountId = if (p.isSelf) state.selectedKashAccountId else null,
                kashAccountNameSnapshot = if (p.isSelf) state.accounts.find { it.id == state.selectedKashAccountId }?.name else null,
                paidAmount = p.paidAmountAtRegister,
                gmfGeneratedAmount = 0.0
            )
        }

        val fullDetails = SharedExpenseWithFullDetails(
            expense = expenseEntity,
            payers = payerEntities,
            splits = splitEntities,
            lineItems = emptyList(),
            settlements = emptyList()
        )

        viewModelScope.launch {
            runCatching {
                sharedExpenseDao?.insertCompleteSharedExpenseTransactional(
                    updatedExpenseSeqConfig = null,
                    updatedTicketSeqConfig = null,
                    expense = expenseEntity,
                    payers = payerEntities,
                    splits = splitEntities,
                    lineItems = emptyList()
                )
            }
        }

        val selectedAcc = state.accounts.find { it.id == state.selectedKashAccountId } ?: state.accounts.firstOrNull()
        val ticketParticipantRows = splitSummary.participantSplits.filter { it.isIncluded }.map { s ->
            val statusBadge = when {
                s.paidByThisParticipant > s.totalOwedShare -> "PAGÓ EN CAJA"
                abs(s.netBalanceForExpense) < 0.01 -> "PAZ Y SALVO"
                else -> "PENDIENTE"
            }
            SharedReceiptTicketEngine.TicketParticipantRow(
                participantId = s.participantId,
                displayName = s.displayName,
                avatarInitials = s.displayName.take(2).uppercase(),
                paidInRegisterFormatted = SharedReceiptTicketEngine.formatMoney(s.paidByThisParticipant, state.currencyCode),
                subtotalConsumedFormatted = SharedReceiptTicketEngine.formatMoney(s.baseSubtotalShare, state.currencyCode),
                taxesAndTipFormatted = SharedReceiptTicketEngine.formatMoney(s.allocatedTaxAmount + s.allocatedTipAmount, state.currencyCode),
                totalOwedShareFormatted = SharedReceiptTicketEngine.formatMoney(s.totalOwedShare, state.currencyCode),
                netBalanceFormatted = SharedReceiptTicketEngine.formatMoney(s.netBalanceForExpense, state.currencyCode),
                remainingPendingFormatted = SharedReceiptTicketEngine.formatMoney(s.remainingPendingAmount, state.currencyCode),
                statusBadgeText = statusBadge,
                statusColorHex = if (s.netBalanceForExpense >= 0.0) "#10B981" else "#D97706",
                isCreditor = s.netBalanceForExpense > 0.0,
                isSettled = s.remainingPendingAmount <= 0.01
            )
        }

        val generatedTicket = SharedReceiptTicketEngine.buildExpenseBreakdownTicket(
            expenseConsecutive = expCode,
            ticketConsecutive = tkCode,
            externalInvoiceNumber = state.externalInvoiceNumberInput.ifBlank { null },
            groupName = state.groups.firstOrNull()?.name ?: "Hogar & Gastos Compartidos",
            groupCode = state.groups.firstOrNull()?.groupCode ?: "HOGAR",
            title = titleText,
            merchantName = merchantText,
            merchantNit = state.merchantNitInput.ifBlank { null },
            categoryName = state.selectedCategoryName,
            dateFormatted = state.expenseDateIso,
            timeFormatted = timeFormatted,
            currency = state.currencyCode,
            appliedExchangeRate = 1.0,
            splitMethod = state.selectedSplitMethod,
            realMoneyMode = state.selectedRealMoneyMode,
            subtotalAmount = effectiveSubtotal,
            taxIvaAmount = state.ivaInput,
            taxConsumptionAmount = state.impoconsumoInput,
            tipAmount = state.tipInput,
            discountAmount = state.discountInput,
            gmfAndFinancialAmount = 0.0,
            totalAmount = splitSummary.effectiveTotalAmount,
            itemizedRows = emptyList(),
            participantRows = ticketParticipantRows,
            collectorName = "Titular KashApp",
            collectorBankName = state.settingsConfig.defaultCollectionBankName,
            collectorAccountOrKey = state.settingsConfig.defaultCollectionAccountOrKey,
            collectorCustomQrUri = null,
            sourceAccountName = selectedAcc?.name,
            sourceAccountBalanceAfter = selectedAcc?.availableBalance?.let { it - myPaid },
            recognizedPnlExpense = myShare,
            receivableOrPayableDelta = myNetDelta,
            maskPrivateBalancesInExport = state.settingsConfig.maskUserAccountBalancesOnTicket
        )

        val nextSeq = state.currentSequenceCounter + 1L
        val year = LocalDate.now().year
        _uiState.update { current ->
            val updatedList = listOf(fullDetails) + current.expensesWithDetails
            current.copy(
                expensesWithDetails = updatedList,
                currentSequenceCounter = nextSeq,
                nextAutoExpenseConsecutive = String.format(Locale.US, "GC-%d-%04d", year, nextSeq),
                nextAutoTicketConsecutive = String.format(Locale.US, "TK-%d-%04d", year, nextSeq),
                expenseTitleInput = "",
                merchantNameInput = "",
                merchantNitInput = "",
                externalInvoiceNumberInput = "",
                manualConsecutiveOverride = "",
                isManualConsecutiveActive = false,
                activeDigitalTicketToView = generatedTicket
            )
        }
        rebuildBalancesAndGraph()
    }

    private fun handleConfirmDebtSettlement(intent: SharedExpensesIntent.ConfirmDebtSettlement) {
        val state = _uiState.value
        val counterparty = state.participants.find { it.id == intent.participantId } ?: return
        val currentBalItem = state.participantBalances.find { it.participant.id == intent.participantId }
        val prevDebtAbs = abs(currentBalItem?.netBalanceWithUser ?: intent.amount)
        val owesMe = (currentBalItem?.netBalanceWithUser ?: 1.0) >= 0.0
        val remainingAbs = (prevDebtAbs - intent.amount).coerceAtLeast(0.0)
        val voucherCode = state.nextPaymentVoucherPreview
        val timeFormatted = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))

        val paymentTicket = SharedReceiptTicketEngine.buildPaymentVoucherTicket(
            voucherConsecutive = voucherCode,
            relatedExpenseConsecutive = state.expensesWithDetails.firstOrNull()?.expense?.consecutiveCode ?: "GC-2026-0001",
            expenseTitle = state.expensesWithDetails.firstOrNull()?.expense?.title ?: "Liquidación de Gastos",
            groupName = state.groups.firstOrNull()?.name,
            fromDebtorName = if (owesMe) counterparty.displayName else "Yo (Titular KashApp)",
            toCreditorName = if (owesMe) "Yo (Titular KashApp)" else counterparty.displayName,
            settlementMethodLabel = intent.method.label,
            previousPendingAmount = prevDebtAbs,
            amountPaidNow = intent.amount,
            newRemainingAmount = remainingAbs,
            currency = state.currencyCode,
            paymentDate = intent.paymentDateIso,
            paymentTime = timeFormatted,
            externalBankReference = intent.referenceCode
        )

        val paymentEntity = SharedSettlementPaymentEntity(
            id = "spay_${UUID.randomUUID().toString().take(8)}",
            userId = userId,
            voucherConsecutiveCode = voucherCode,
            sharedExpenseId = state.expensesWithDetails.firstOrNull()?.expense?.id,
            fromParticipantId = if (owesMe) counterparty.id else "SELF_USER",
            toParticipantId = if (owesMe) "SELF_USER" else counterparty.id,
            amount = intent.amount,
            currency = state.currencyCode,
            appliedExchangeRate = 1.0,
            settlementMethod = intent.method.code,
            reimbursementTreatmentApplied = intent.treatment.name,
            kashAccountId = intent.kashAccountId,
            externalReferenceCode = intent.referenceCode,
            ticketIntegrityHash = paymentTicket.sha256IntegrityHash,
            paymentDate = intent.paymentDateIso,
            paymentTime = timeFormatted,
            idempotencyKey = "idem_pay_${System.currentTimeMillis()}"
        )

        viewModelScope.launch {
            runCatching {
                sharedExpenseDao?.insertSettlementPayment(paymentEntity)
            }
        }

        _uiState.update { current ->
            val updatedBalances = current.participantBalances.map { bal ->
                if (bal.participant.id == intent.participantId) {
                    val sign = if (bal.netBalanceWithUser >= 0.0) 1.0 else -1.0
                    bal.copy(
                        netBalanceWithUser = sign * remainingAbs,
                        pendingTicketsCount = if (remainingAbs < 0.01) 0 else bal.pendingTicketsCount
                    )
                } else bal
            }
            val newRec = updatedBalances.filter { it.netBalanceWithUser > 0.0 }.sumOf { it.netBalanceWithUser }
            val newPay = updatedBalances.filter { it.netBalanceWithUser < 0.0 }.sumOf { abs(it.netBalanceWithUser) }
            current.copy(
                participantBalances = updatedBalances,
                settlements = listOf(paymentEntity) + current.settlements,
                totalReceivableNiif = newRec,
                totalPayableNiif = newPay,
                netSharedPosition = newRec - newPay,
                activeSettleParticipantId = null,
                activeDigitalTicketToView = paymentTicket
            )
        }
    }

    private fun rebuildBalancesAndGraph() {
        val state = _uiState.value
        val otherParticipants = state.participants.filter { !it.isSelf && it.id != "SELF_USER" }
        val balances = otherParticipants.map { p ->
            var netWithUser = 0.0
            var openTickets = 0
            var oldestDate: String? = null
            for (expDetail in state.expensesWithDetails) {
                if (expDetail.expense.status == "VOIDED") continue
                val splitForP = expDetail.splits.find { it.participantId == p.id }
                val mySplit = expDetail.splits.find { it.participantId == "SELF_USER" }
                if (splitForP != null && splitForP.remainingPendingAmount > 0.01 && (mySplit?.paidByThisParticipant ?: 0.0) > 0.0) {
                    netWithUser += splitForP.remainingPendingAmount
                    openTickets++
                    oldestDate = expDetail.expense.expenseDate
                } else if (mySplit != null && mySplit.remainingPendingAmount > 0.01 && (splitForP?.paidByThisParticipant ?: 0.0) > 0.0) {
                    netWithUser -= mySplit.remainingPendingAmount
                    openTickets++
                    oldestDate = expDetail.expense.expenseDate
                }
            }
            ParticipantBalanceSummaryUi(
                participant = p,
                netBalanceWithUser = netWithUser,
                pendingTicketsCount = openTickets,
                oldestPendingDate = oldestDate
            )
        }

        val totalRec = balances.filter { it.netBalanceWithUser > 0.0 }.sumOf { it.netBalanceWithUser }
        val totalPay = balances.filter { it.netBalanceWithUser < 0.0 }.sumOf { abs(it.netBalanceWithUser) }

        val rawEdges = mutableListOf<DebtSimplificationGraphEngine.RawDirectedDebtEdge>()
        state.expensesWithDetails.forEach { exp ->
            val payer = exp.payers.firstOrNull()
            val payerName = state.participants.find { it.id == payer?.participantId }?.displayName ?: "Pagador"
            exp.splits.forEach { split ->
                if (split.participantId != payer?.participantId && split.totalOwedShare > 0.0) {
                    val debtorName = state.participants.find { it.id == split.participantId }?.displayName ?: "Deudor"
                    rawEdges.add(
                        DebtSimplificationGraphEngine.RawDirectedDebtEdge(
                            fromDebtorId = split.participantId,
                            fromDebtorName = debtorName,
                            toCreditorId = payer?.participantId ?: "SELF_USER",
                            toCreditorName = payerName,
                            amount = split.totalOwedShare,
                            sourceConsecutiveCode = exp.expense.consecutiveCode
                        )
                    )
                }
            }
        }
        val graphReport = DebtSimplificationGraphEngine.simplifyGroupDebts(rawEdges)

        _uiState.update {
            it.copy(
                participantBalances = balances,
                totalReceivableNiif = if (it.expensesWithDetails.isNotEmpty()) totalRec else it.totalReceivableNiif,
                totalPayableNiif = if (it.expensesWithDetails.isNotEmpty()) totalPay else it.totalPayableNiif,
                netSharedPosition = if (it.expensesWithDetails.isNotEmpty()) (totalRec - totalPay) else it.netSharedPosition,
                graphSimplificationReport = graphReport
            )
        }
    }

    private fun executeAiNaturalLanguageSplit(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAiProcessing = true, aiNaturalPromptInput = prompt) }
            val state = _uiState.value
            val names = state.participants.map { it.displayName }
            val available = state.accounts.firstOrNull()?.availableBalance ?: 1_500_000.0

            val parsed = aiEngine.parseNaturalLanguageExpensePrompt(
                userPrompt = prompt,
                participantNames = names,
                userAvailableLiquidity = available
            )

            val newSubtotal = (parsed.detectedTotalAmount - parsed.detectedTipAmount).coerceAtLeast(0.0)
            val grossTotal = parsed.detectedTotalAmount.coerceAtLeast(0.0)

            _uiState.update { current ->
                val updatedParticipants = current.splitParticipantsInput.map { p ->
                    val paid = if (p.isSelf) grossTotal else 0.0
                    val adj = parsed.participantAdjustmentsOrAmounts[p.displayName] ?: 0.0
                    p.copy(
                        paidAmountAtRegister = paid,
                        adjustmentAmountInput = adj,
                        exactAmountInput = if (parsed.suggestedSplitMethod == SplitCalculationMethod.EXACT_AMOUNT && adj > 0.0) adj else p.exactAmountInput
                    )
                }
                current.copy(
                    isAiProcessing = false,
                    expenseTitleInput = parsed.detectedTitle,
                    subtotalInput = newSubtotal,
                    tipInput = parsed.detectedTipAmount,
                    selectedCategoryName = parsed.suggestedCategoryName,
                    selectedSplitMethod = parsed.suggestedSplitMethod,
                    selectedRealMoneyMode = parsed.suggestedRealMoneyMode,
                    splitParticipantsInput = updatedParticipants,
                    apiProtectionStatus = FirebaseApiProtectionGuard.getProtectionTelemetry()
                )
            }
            recalculateLiveSimulation()
        }
    }

    private fun recalculateLiveSimulation() {
        val state = _uiState.value
        if (state.subtotalInput <= 0.0 || state.splitParticipantsInput.isEmpty()) return

        val splitReq = SharedSplitCalculatorEngine.SplitCalculationRequest(
            subtotalAmount = state.subtotalInput,
            taxIvaAmount = state.ivaInput,
            taxConsumptionAmount = state.impoconsumoInput,
            tipAmount = state.tipInput,
            discountAmount = state.discountInput,
            splitMethod = state.selectedSplitMethod,
            taxAndTipProrationRule = state.selectedTaxProrationRule,
            roundingStepRule = state.selectedRoundingStep,
            roundingRemainderTarget = state.selectedRemainderTarget,
            participants = state.splitParticipantsInput,
            ocrLineItems = state.ocrLineItemsInput,
            consecutiveSequenceNumber = state.currentSequenceCounter
        )

        when (val splitOut = SharedSplitCalculatorEngine.calculateSplits(splitReq)) {
            is Outcome.Failure -> {
                _uiState.update { it.copy(liveValidationErrorMessage = "Verifica que los porcentajes o montos cuadren con el total de la factura.") }
            }
            is Outcome.Success -> {
                val summary = splitOut.value
                val mySplit = summary.participantSplits.find { it.isSelf || it.participantId == "SELF_USER" }
                val myShare = mySplit?.totalOwedShare ?: 0.0
                val myPaid = mySplit?.paidByThisParticipant ?: 0.0
                val selectedAcc = state.accounts.find { it.id == state.selectedKashAccountId } ?: state.accounts.firstOrNull()

                val impactOut = RealMoneyImpactEngine.simulateExpenseImpact(
                    mode = state.selectedRealMoneyMode,
                    budgetPolicy = state.selectedBudgetPolicy,
                    myPaidPrincipalAmount = myPaid,
                    myGmf4x1000Amount = 0.0,
                    myConsumptionShareAmount = myShare,
                    sourcePaymentAccount = selectedAcc,
                    holdReserveAccount = selectedAcc,
                    autoReserveOwedInHold = state.autoReserveOwedInHold
                )

                val aiRec = aiEngine.recommendOptimalRealMoneyMode(
                    expenseTotal = summary.effectiveTotalAmount,
                    myPaidAmount = myPaid,
                    myConsumptionShare = myShare,
                    accountAvailableBalance = selectedAcc?.availableBalance ?: 1_000_000.0,
                    categoryRemainingBudget = 500_000.0
                )

                _uiState.update {
                    it.copy(
                        liveSplitCalculation = summary,
                        liveRealMoneySimulation = (impactOut as? Outcome.Success)?.value,
                        aiRecommendationBanner = aiRec,
                        liveValidationErrorMessage = null
                    )
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            aiEngine: SharedExpenseFirebaseAiEngine = SharedExpenseFirebaseAiEngine(),
            sharedExpenseDao: com.example.data.local.dao.SharedExpenseDao? = null,
            financialDao: com.example.data.local.dao.FinancialDao? = null,
            userId: String = "user_default"
        ): androidx.lifecycle.ViewModelProvider.Factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SharedExpensesViewModel(
                    aiEngine = aiEngine,
                    sharedExpenseDao = sharedExpenseDao,
                    financialDao = financialDao,
                    userId = userId
                ) as T
            }
        }
    }
}
