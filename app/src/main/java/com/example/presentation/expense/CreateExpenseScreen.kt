package com.example.presentation.expense

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.domain.repository.FinancialRepository
import com.example.presentation.account.CreateAccountScreen
import com.example.presentation.account.components.CurrencyPickerModal
import com.example.presentation.category.CategoryCreationDialog
import com.example.presentation.category.resolveCategoryIcon
import com.example.presentation.expense.components.ExpenseAccountSelector
import com.example.presentation.expense.components.ExpenseAmountHeader
import com.example.presentation.expense.components.ExpenseCategoryGrid
import com.example.presentation.expense.components.ExpenseRecurrenceOptions
import com.example.presentation.expense.dialogs.CreditCardStrategySelectionBottomSheet
import com.example.presentation.expense.dialogs.CreditCardWaterfallVoucherDialog
import com.example.presentation.expense.dialogs.ExpenseCalculatorBottomSheet
import com.example.presentation.expense.dialogs.OverdraftWarningDialog
import com.example.presentation.expense.model.CardPaymentVoucher
import com.example.presentation.expense.model.DefaultExpenseCategories
import com.example.presentation.expense.model.ExpenseAccount
import com.example.presentation.expense.model.ExpenseCategory
import com.example.presentation.expense.model.ExpenseFormState
import com.example.presentation.expense.model.buildDisplayCategories
import com.example.presentation.expense.model.formatMoneyDisplay
import com.example.presentation.expense.model.getDefaultCreditCardAccounts
import com.example.util.CreditCardPaymentEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

// Re-exports for backward compatibility with other screens
typealias ExpenseCategory = com.example.presentation.expense.model.ExpenseCategory
typealias ExpenseAccount = com.example.presentation.expense.model.ExpenseAccount
typealias CurrencyInfo = com.example.presentation.expense.model.CurrencyInfo
typealias CardPaymentVoucher = com.example.presentation.expense.model.CardPaymentVoucher
typealias CurrencyDotVisualTransformation = com.example.presentation.expense.model.CurrencyDotVisualTransformation
val SupportedCurrencies = com.example.presentation.expense.model.SupportedCurrencies
fun formatLiveNumberString(rawDigits: String): String = com.example.presentation.expense.model.formatLiveNumberString(rawDigits)
fun formatMoneyDisplay(amount: Double, currencyCode: String): String = com.example.presentation.expense.model.formatMoneyDisplay(amount, currencyCode)

/**
 * Enterprise "Nuevo gasto" Screen (Coordinated Modular Architecture).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExpenseScreen(
    onDismiss: () -> Unit,
    onSaveExpense: (
        amount: Double, currency: String, category: ExpenseCategory, account: ExpenseAccount,
        dateFormatted: String, description: String, isRecurring: Boolean, recurrenceFrequency: String,
        saveAsTemplate: Boolean, tags: List<String>, receiptUri: Uri?, idempotencyKey: String
    ) -> Unit,
    modifier: Modifier = Modifier,
    initialAmount: Double? = null,
    financialRepository: FinancialRepository? = null,
    userId: String = "user_default"
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val amountFocusRequester = remember { FocusRequester() }
    val prefs = remember { context.getSharedPreferences("kashapp_preferences", Context.MODE_PRIVATE) }
    val lastUsedCurrency = remember { prefs.getString("last_used_currency", "COP") ?: "COP" }

    BackHandler { onDismiss() }

    var formState by remember {
        mutableStateOf(
            ExpenseFormState(
                rawAmountInput = if (initialAmount != null && initialAmount > 0) initialAmount.toString() else "",
                currencyCode = lastUsedCurrency
            )
        )
    }

    val roomAccountsState = financialRepository?.getAccounts(userId)?.collectAsState(initial = emptyList())
    val allRoomAccounts = roomAccountsState?.value ?: emptyList()
    val roomAccounts = remember(allRoomAccounts) {
        allRoomAccounts.filter {
            !it.type.equals("CREDIT", ignoreCase = true) && !it.type.equals("CREDIT_CARD", ignoreCase = true) &&
            !it.name.contains("Tarjeta de Crédito", ignoreCase = true) && !it.name.contains("Credit Card", ignoreCase = true)
        }
    }
    val creditCardAccounts = remember(allRoomAccounts) {
        val filtered = allRoomAccounts.filter {
            it.type.equals("CREDIT", ignoreCase = true) || it.type.equals("CREDIT_CARD", ignoreCase = true) ||
            it.name.contains("Tarjeta", ignoreCase = true) || it.name.contains("Visa", ignoreCase = true) || it.name.contains("Mastercard", ignoreCase = true)
        }
        if (filtered.isNotEmpty()) filtered else getDefaultCreditCardAccounts(userId)
    }

    var selectedAccountEntity by remember { mutableStateOf<AccountEntity?>(null) }
    var selectedCreditCardToPay by remember(creditCardAccounts) { mutableStateOf<AccountEntity?>(creditCardAccounts.firstOrNull()) }

    LaunchedEffect(roomAccounts) {
        if (selectedAccountEntity == null && roomAccounts.isNotEmpty()) selectedAccountEntity = roomAccounts.first()
    }

    val allCreditCardsFlow = remember(financialRepository) { financialRepository?.getAllAccountCreditCards() }
    val allCreditCardConfigsState = allCreditCardsFlow?.collectAsState(initial = emptyList())
    val selectedCreditCardConfig = remember(selectedCreditCardToPay, allCreditCardConfigsState?.value) {
        selectedCreditCardToPay?.let { card -> allCreditCardConfigsState?.value?.find { it.accountId == card.id } }
    }
    val minPaymentBreakdown = remember(selectedCreditCardToPay, selectedCreditCardConfig) {
        selectedCreditCardToPay?.let { card -> CreditCardPaymentEngine.calculateMinimumPayment(cardAccount = card, cardConfig = selectedCreditCardConfig) }
    }
    val defaultMonthlyQuota = minPaymentBreakdown?.totalSuggestedPayment ?: 350000.0

    val roomCategoriesState = financialRepository?.getCategories()?.collectAsState(initial = emptyList())
    val displayCategories = remember(roomCategoriesState?.value) { buildDisplayCategories(roomCategoriesState?.value ?: emptyList()) }
    var selectedCategory by remember { mutableStateOf(displayCategories.firstOrNull() ?: DefaultExpenseCategories[0]) }

    val roomTagsState = financialRepository?.getTags()?.collectAsState(initial = emptyList())

    // Dialog & Flow States
    var showCurrencySheet by remember { mutableStateOf(false) }
    var showCalculatorSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTagInputDialog by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var showCreateAccountModal by remember { mutableStateOf(false) }
    var createAccountTypeCode by remember { mutableStateOf("SAVINGS") }
    var showStrategySelectionModal by remember { mutableStateOf(false) }
    var showSuccessToast by remember { mutableStateOf(false) }
    var successToastMessage by remember { mutableStateOf("") }
    var showOverdraftBlockedPopup by remember { mutableStateOf(false) }
    var paymentVoucherData by remember { mutableStateOf<CardPaymentVoucher?>(null) }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                val isPdf = uri.path.orEmpty().endsWith(".pdf", ignoreCase = true) || uri.toString().contains("pdf", ignoreCase = true)
                formState = formState.copy(receiptUri = uri, isPdfDocument = isPdf, receiptFileName = if (isPdf) "Comprobante.pdf" else "Comprobante.jpg")
            }
        }
    )
    val dateState = rememberDatePickerState(initialSelectedDateMillis = formState.selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    val dateDisplayString = formState.selectedDate.format(remember { DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es", "ES")) })

    val onPerformSaveExpense: () -> Unit = {
        val amount = formState.amountDouble
        if (!formState.isSubmitting && amount > 0.0) {
            val targetAccount = selectedAccountEntity ?: roomAccounts.firstOrNull()
            if (formState.isCreditCardPaymentMode && (targetAccount == null || targetAccount.id == selectedCreditCardToPay?.id || targetAccount.type.contains("CREDIT", true))) {
                showOverdraftBlockedPopup = true
            } else if (targetAccount != null && !targetAccount.hasOverdraft && ((targetAccount.availableBalance ?: targetAccount.balance) < amount)) {
                showOverdraftBlockedPopup = true
            } else if (formState.isCreditCardPaymentMode && amount > defaultMonthlyQuota && !formState.strategyConfirmed) {
                showStrategySelectionModal = true
            } else {
                formState = formState.copy(isSubmitting = true)
                val breakdown = minPaymentBreakdown ?: CreditCardPaymentEngine.calculateMinimumPayment(
                    cardAccount = selectedCreditCardToPay ?: AccountEntity(id = "cc_default", userId = userId, name = "Tarjeta de Crédito", type = "CREDIT_CARD", balance = -3150000.0, availableBalance = 4850000.0, currency = formState.currencyCode),
                    cardConfig = selectedCreditCardConfig
                )
                val waterfall = CreditCardPaymentEngine.applyPaymentWaterfall(paymentAmount = amount, minBreakdown = breakdown)
                val strategyLabel = when (formState.cardPaymentStrategy) {
                    "AVALANCHE" -> "Método Avalancha (Mayor Tasa EA)"
                    "SNOWBALL" -> "Método Bola de Nieve (Menor Saldo)"
                    "TIME_REDUCTION" -> "Reducción de Tiempo"
                    "QUOTA_REDUCTION" -> "Reducción de Valor de Cuota"
                    else -> "Pago Cuota Mensual"
                }
                val finalConcept = if (formState.isCreditCardPaymentMode) "Pago ${selectedCreditCardToPay?.name ?: "Tarjeta"} - $strategyLabel" else if (formState.conceptText.isNotBlank()) formState.conceptText else selectedCategory.name

                coroutineScope.launch {
                    try {
                        if (targetAccount != null && financialRepository != null) {
                            if (formState.isCreditCardPaymentMode && selectedCreditCardToPay != null) {
                                financialRepository.recordTransfer(
                                    userId = userId, sourceAccountId = targetAccount.id, sourceAccountName = targetAccount.name,
                                    destinationAccountId = selectedCreditCardToPay!!.id, destinationAccountName = selectedCreditCardToPay!!.name,
                                    amount = amount, currency = formState.currencyCode, dateFormatted = formState.selectedDate.toString(),
                                    concept = finalConcept, note = "Estrategia: $strategyLabel", receiptUrl = formState.receiptUri?.toString(),
                                    saveAsTemplate = false, tags = listOf("Pago Tarjeta", "Abono Capital")
                                )
                                paymentVoucherData = CardPaymentVoucher(
                                    transactionId = "TX-${UUID.randomUUID().toString().take(8).uppercase()}",
                                    authCode = "AUT-${(100000..999999).random()}", cardName = selectedCreditCardToPay!!.name,
                                    cardMask = selectedCreditCardToPay!!.accountNumberMasked ?: "•••• 8912", sourceAccountName = targetAccount.name,
                                    amount = amount, currency = formState.currencyCode, strategyName = strategyLabel,
                                    previousDebt = kotlin.math.abs(selectedCreditCardToPay!!.balance).coerceAtLeast(amount),
                                    newDebt = waterfall.remainingDebt, quotaRestored = waterfall.paidPrincipal, dateFormatted = dateDisplayString,
                                    timeFormatted = "Hoy, ${java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))}",
                                    principalAmortized = waterfall.paidPrincipal, currentInterestPaid = waterfall.paidCurrentInterest,
                                    handlingFeePaid = waterfall.paidHandlingFee, lateInterestPaid = waterfall.paidLateInterest,
                                    rateEa = breakdown.rateEa, rateEm = breakdown.rateEm, projectedImpact = "Abono aplicado exitosamente."
                                )
                            } else {
                                val result = financialRepository.recordExpense(
                                    userId = userId, accountId = targetAccount.id, accountName = targetAccount.name,
                                    categoryId = selectedCategory.id, categoryName = selectedCategory.name, amount = amount,
                                    currency = formState.currencyCode, dateFormatted = formState.selectedDate.toString(),
                                    description = finalConcept, receiptUrl = formState.receiptUri?.toString(),
                                    isRecurring = formState.isRecurring, saveAsTemplate = formState.isSavedAsFrequent, tags = formState.tagsList
                                )
                                if (result.isSuccess) {
                                    val accountModel = ExpenseAccount(id = targetAccount.id, name = targetAccount.name, balance = targetAccount.balance - amount, icon = Icons.Default.AccountBalance, currency = formState.currencyCode, mask = targetAccount.accountNumberMasked ?: "•••• 4208")
                                    onSaveExpense(amount, formState.currencyCode, selectedCategory, accountModel, dateDisplayString, finalConcept, formState.isRecurring, formState.recurrenceFrequency, formState.isSavedAsFrequent, formState.tagsList, formState.receiptUri, UUID.randomUUID().toString())
                                    successToastMessage = "Gasto de ${formatMoneyDisplay(amount, formState.currencyCode)} registrado con éxito"
                                    showSuccessToast = true
                                    delay(800)
                                    onDismiss()
                                }
                            }
                        }
                    } finally {
                        formState = formState.copy(isSubmitting = false)
                    }
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().navigationBarsPadding().background(Color(0xFFF8FAFC))) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()) {
            // Header Bar
            Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF032B43), Color(0xFF021727)))).statusBarsPadding().padding(horizontal = 20.dp, vertical = 18.dp)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0x26FFFFFF)).testTag("back_button_expense")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (formState.isAmountValid && !formState.isSubmitting) Color(0xFFEF4444) else Color(0x33FFFFFF),
                            shadowElevation = if (formState.isAmountValid) 4.dp else 0.dp,
                            modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable(enabled = formState.isAmountValid && !formState.isSubmitting) { onPerformSaveExpense() }.testTag("top_bar_save_expense_btn")
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = if (formState.isAmountValid) Color.White else Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                Text(text = if (formState.isSubmitting) "Guardando..." else "Guardar", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (formState.isAmountValid) Color.White else Color(0xFF94A3B8))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(text = if (formState.isCreditCardPaymentMode) "Gasto / Pago de Tarjeta" else "Nuevo gasto", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.5).sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = if (formState.isCreditCardPaymentMode) "Paga la cuota o abona a capital para reducir tiempo o cuota." else "Registra el movimiento y mantén tus finanzas al día.", fontSize = 13.sp, color = Color(0xFF94A3B8))
                }
            }

            // Body Content
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Segmented Switch
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFE2E8F0), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                        Surface(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { formState = formState.copy(isCreditCardPaymentMode = false) }.testTag("segment_gasto_comun"), color = if (!formState.isCreditCardPaymentMode) Color.White else Color.Transparent, shadowElevation = if (!formState.isCreditCardPaymentMode) 2.dp else 0.dp) {
                            Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = if (!formState.isCreditCardPaymentMode) Color(0xFF0F172A) else Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                    Text("Gasto Común", fontSize = 13.sp, fontWeight = if (!formState.isCreditCardPaymentMode) FontWeight.Bold else FontWeight.Medium, color = if (!formState.isCreditCardPaymentMode) Color(0xFF0F172A) else Color(0xFF64748B))
                                }
                            }
                        }
                        Surface(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { formState = formState.copy(isCreditCardPaymentMode = true) }.testTag("segment_gasto_tarjeta"), color = if (formState.isCreditCardPaymentMode) Color(0xFF0284C7) else Color.Transparent, shadowElevation = if (formState.isCreditCardPaymentMode) 2.dp else 0.dp) {
                            Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = if (formState.isCreditCardPaymentMode) Color.White else Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                    Text("Gasto de Tarjeta", fontSize = 13.sp, fontWeight = if (formState.isCreditCardPaymentMode) FontWeight.Bold else FontWeight.Medium, color = if (formState.isCreditCardPaymentMode) Color.White else Color(0xFF64748B))
                                }
                            }
                        }
                    }
                }

                // 1. Amount Header
                ExpenseAmountHeader(
                    rawAmountInput = formState.rawAmountInput,
                    onAmountChange = { formState = formState.copy(rawAmountInput = it) },
                    selectedCurrencyCode = formState.currencyCode,
                    onOpenCurrencyPicker = { showCurrencySheet = true },
                    onOpenCalculator = { showCalculatorSheet = true },
                    amountFocusRequester = amountFocusRequester,
                    isCreditCardPaymentMode = formState.isCreditCardPaymentMode,
                    defaultMonthlyQuota = defaultMonthlyQuota,
                    onUseMonthlyQuota = { formState = formState.copy(rawAmountInput = defaultMonthlyQuota.toLong().toString(), cardPaymentStrategy = "MONTHLY_FEE", strategyConfirmed = false) }
                )

                // 2. Account Selector & Impact
                ExpenseAccountSelector(
                    selectedAccount = selectedAccountEntity,
                    allAccounts = roomAccounts,
                    amountDouble = formState.amountDouble,
                    selectedCurrencyCode = formState.currencyCode,
                    onSelectAccount = { selectedAccountEntity = it },
                    onCreateAccountClick = { type -> createAccountTypeCode = type; showCreateAccountModal = true }
                )

                // 3. Category Grid
                ExpenseCategoryGrid(
                    selectedCategory = selectedCategory,
                    categories = displayCategories,
                    onCategorySelected = { selectedCategory = it },
                    onOpenCreateCategory = { showNewCategoryDialog = true }
                )

                // 4. Recurrence, Concept & Optional Details
                ExpenseRecurrenceOptions(
                    selectedDate = formState.selectedDate,
                    onOpenDatePicker = { showDatePicker = true },
                    isRecurring = formState.isRecurring,
                    onRecurringChange = { formState = formState.copy(isRecurring = it) },
                    recurrenceFrequency = formState.recurrenceFrequency,
                    onFrequencyChange = { formState = formState.copy(recurrenceFrequency = it) },
                    conceptText = formState.conceptText,
                    onConceptChange = { formState = formState.copy(conceptText = it) },
                    additionalNotes = formState.additionalNotes,
                    onNotesChange = { formState = formState.copy(additionalNotes = it) },
                    isSavedAsFrequent = formState.isSavedAsFrequent,
                    onFrequentChange = { formState = formState.copy(isSavedAsFrequent = it) },
                    selectedTags = formState.tagsList,
                    allRoomTags = roomTagsState?.value ?: emptyList(),
                    onToggleTag = { tag ->
                        val updated = if (formState.tagsList.contains(tag)) formState.tagsList - tag else formState.tagsList + tag
                        formState = formState.copy(tagsList = updated)
                    },
                    onOpenAddTagDialog = { showTagInputDialog = true },
                    receiptUri = formState.receiptUri,
                    receiptFileName = formState.receiptFileName,
                    isPdfDocument = formState.isPdfDocument,
                    onPickDocument = { documentPickerLauncher.launch(arrayOf("image/*", "application/pdf")) },
                    onRemoveDocument = { formState = formState.copy(receiptUri = null, receiptFileName = null, isPdfDocument = false) }
                )
                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // Floating Success Notification Toast
        AnimatedVisibility(visible = showSuccessToast, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)) {
            Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF0F172A), shadowElevation = 10.dp) {
                Row(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    Text(text = successToastMessage, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    // Dialogs & Sheets
    ExpenseCalculatorBottomSheet(
        visible = showCalculatorSheet, initialValue = formState.rawAmountInput, currencyCode = formState.currencyCode, currencySymbol = "$",
        onConfirm = { result -> formState = formState.copy(rawAmountInput = result) }, onDismiss = { showCalculatorSheet = false }
    )
    if (showCurrencySheet) {
        CurrencyPickerModal(
            selectedCurrencyCode = formState.currencyCode,
            onCurrencySelected = { code -> formState = formState.copy(currencyCode = code); prefs.edit().putString("last_used_currency", code).apply(); showCurrencySheet = false },
            onDismiss = { showCurrencySheet = false }, financialRepository = financialRepository, userId = userId
        )
    }
    OverdraftWarningDialog(
        visible = showOverdraftBlockedPopup, accountName = selectedAccountEntity?.name ?: "Cuenta bancaria",
        availableBalance = selectedAccountEntity?.availableBalance ?: selectedAccountEntity?.balance ?: 0.0,
        attemptedAmount = formState.amountDouble, currency = formState.currencyCode, onDismiss = { showOverdraftBlockedPopup = false }
    )
    CreditCardWaterfallVoucherDialog(voucher = paymentVoucherData, onDismiss = { paymentVoucherData = null; onDismiss() })
    CreditCardStrategySelectionBottomSheet(
        visible = showStrategySelectionModal, excessAmount = (formState.amountDouble - defaultMonthlyQuota).coerceAtLeast(0.0),
        currentStrategy = formState.cardPaymentStrategy, selectedCurrencyCode = formState.currencyCode,
        impactProjection = CreditCardPaymentEngine.calculateExtraCapitalImpact(
            extraAmount = (formState.amountDouble - defaultMonthlyQuota).coerceAtLeast(0.0), strategy = formState.cardPaymentStrategy,
            minBreakdown = minPaymentBreakdown ?: CreditCardPaymentEngine.calculateMinimumPayment(cardAccount = selectedCreditCardToPay ?: AccountEntity(id = "cc_default", userId = userId, name = "Tarjeta de Crédito", type = "CREDIT_CARD", balance = -3150000.0, availableBalance = 4850000.0, currency = formState.currencyCode), cardConfig = selectedCreditCardConfig)
        ),
        onSelectStrategy = { formState = formState.copy(cardPaymentStrategy = it) },
        onConfirm = { formState = formState.copy(strategyConfirmed = true); showStrategySelectionModal = false; onPerformSaveExpense() },
        onDismiss = { showStrategySelectionModal = false }
    )
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dateState.selectedDateMillis?.let { millis -> formState = formState.copy(selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()) }; showDatePicker = false }) { Text("Seleccionar", fontWeight = FontWeight.Bold, color = Color(0xFF0284C7)) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar", color = Color(0xFF64748B)) } },
            colors = DatePickerDefaults.colors(containerColor = Color.White)
        ) { DatePicker(state = dateState) }
    }
    if (showTagInputDialog) {
        AlertDialog(
            onDismissRequest = { showTagInputDialog = false },
            title = { Text("Nueva Etiqueta", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = { OutlinedTextField(value = newTagInput, onValueChange = { newTagInput = it }, placeholder = { Text("Ej. ViajeCartagena, Supermercado...") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { Button(onClick = { val clean = newTagInput.trim().replace("#", ""); if (clean.isNotBlank()) { if (!formState.tagsList.contains(clean)) formState = formState.copy(tagsList = formState.tagsList + clean); financialRepository?.let { repo -> coroutineScope.launch { repo.insertTag(clean) } } }; newTagInput = ""; showTagInputDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))) { Text("Agregar") } },
            dismissButton = { TextButton(onClick = { showTagInputDialog = false }) { Text("Cancelar") } },
            containerColor = Color.White
        )
    }
    if (showNewCategoryDialog) {
        CategoryCreationDialog(
            categoryType = "EXPENSE", onDismiss = { showNewCategoryDialog = false },
            onCategoryCreated = { name, iconKey, colorHex ->
                val icon = resolveCategoryIcon(iconKey, name)
                val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (_: Exception) { Color(0xFF0284C7) }
                financialRepository?.let { repo ->
                    coroutineScope.launch {
                        val created = repo.createCategory(name = name, colorHex = colorHex, icon = iconKey, userId = userId, type = "EXPENSE")
                        selectedCategory = ExpenseCategory(id = created.id, name = created.name, icon = icon, color = color, bgColor = color.copy(alpha = 0.15f))
                    }
                }
                showNewCategoryDialog = false
            }
        )
    }
    if (showCreateAccountModal) {
        CreateAccountScreen(
            initialTypeCode = createAccountTypeCode,
            onAccountCreated = { newAcc, _ -> selectedAccountEntity = newAcc; showCreateAccountModal = false },
            onDismiss = { showCreateAccountModal = false }, financialRepository = financialRepository
        )
    }
}
