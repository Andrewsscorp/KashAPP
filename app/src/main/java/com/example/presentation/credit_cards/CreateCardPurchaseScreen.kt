package com.example.presentation.credit_cards

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.repository.FinancialRepository
import com.example.presentation.account.components.CurrencyPickerModal
import com.example.presentation.category.CategoryCreationDialog
import com.example.presentation.category.resolveCategoryIcon
import com.example.presentation.credit_cards.components.CreditCardPickerSheet
import com.example.presentation.credit_cards.components.CreditCardSelectorCard
import com.example.presentation.credit_cards.components.DeferredInterestSummaryCard
import com.example.presentation.credit_cards.components.InstallmentsSliderPicker
import com.example.presentation.credit_cards.model.CardPurchaseFormState
import com.example.presentation.credit_cards.model.CardPurchaseVoucher
import com.example.presentation.credit_cards.model.CreditCardDisplayItem
import com.example.presentation.credit_cards.model.formatCardMoney
import com.example.presentation.expense.ExpenseCategory
import com.example.ui.theme.KashBlue
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

// Backward compatibility exports
typealias CreditCardDisplayItem = CreditCardDisplayItem
typealias CardPurchaseVoucher = CardPurchaseVoucher

/**
 * Screen for recording credit card purchases ("Compra con Tarjeta").
 * Reduced to ~200 lines coordinating layout, financial engine, and modal lifecycles.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateCardPurchaseScreen(
    onDismiss: () -> Unit,
    onPurchaseSuccess: (
        cardId: String,
        cardName: String,
        amount: Double,
        currency: String,
        installments: Int,
        merchant: String,
        category: String,
        dateFormatted: String
    ) -> Unit,
    financialRepository: FinancialRepository? = null,
    userId: String = "user_default",
    onNavigateToAddCard: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BackHandler { onDismiss() }

    val prefs = remember { context.getSharedPreferences("kashapp_preferences", Context.MODE_PRIVATE) }
    val lastUsedCurrency = remember { prefs.getString("last_used_currency", "COP") ?: "COP" }

    // 1. Fetch Room Accounts and Credit Card Configurations
    val roomAccountsState = financialRepository?.getAccounts(userId)?.collectAsState(initial = emptyList())
    val roomAccounts = roomAccountsState?.value ?: emptyList()
    val cardConfigsState = financialRepository?.getAllAccountCreditCards()?.collectAsState(initial = emptyList())
    val cardConfigs = cardConfigsState?.value ?: emptyList()
    val cardConfigsByAccountId = remember(cardConfigs) { cardConfigs.associateBy { it.accountId } }

    val defaultCreditCards = remember {
        listOf(
            CreditCardDisplayItem("cc_bancolombia_visa", "Bancolombia Visa Signature", "Bancolombia", "•••• 8912", 8000000.0, 4850000.0, 15, 2, 24.5, "Visa Signature", "#0284C7"),
            CreditCardDisplayItem("cc_davivienda_mc", "Davivienda Mastercard Black", "Davivienda", "•••• 3401", 12000000.0, 9200000.0, 20, 5, 25.2, "Mastercard Black", "#DC2626"),
            CreditCardDisplayItem("cc_nu_gold", "Nu Colombia Gold", "Nu", "•••• 6710", 3500000.0, 1850000.0, 10, 25, 23.9, "Mastercard Gold", "#7C3AED")
        )
    }

    val creditCardList = remember(roomAccounts, cardConfigsByAccountId) {
        val filteredRoomCards = roomAccounts.filter { it.type == "CREDIT" || it.type == "CREDIT_CARD" || it.name.contains("Tarjeta", ignoreCase = true) || it.name.contains("Crédito", ignoreCase = true) }
        if (filteredRoomCards.isNotEmpty()) {
            filteredRoomCards.map { acc ->
                val cfg = cardConfigsByAccountId[acc.id]
                CreditCardDisplayItem(
                    id = acc.id,
                    name = acc.name,
                    bankName = if (acc.name.contains("Nu", ignoreCase = true)) "Nu Bank" else if (acc.name.contains("Davivienda", ignoreCase = true)) "Davivienda" else acc.bankName,
                    mask = acc.accountNumberMasked ?: "•••• ${acc.id.takeLast(4)}",
                    quotaLimit = cfg?.quotaTotal ?: (acc.balance * 2.0).coerceAtLeast(5000000.0),
                    availableQuota = cfg?.quotaAvailable ?: acc.balance.coerceAtLeast(0.0),
                    cutoffDay = cfg?.cutOffDay ?: 15,
                    paymentDueDay = cfg?.paymentDueDay ?: 2,
                    interestRateEa = cfg?.purchasesRateEa ?: 24.8,
                    cardBrand = if (acc.name.contains("Mastercard", ignoreCase = true)) "Mastercard" else "Visa",
                    colorHex = acc.customColorHex,
                    allowsOverdraft = acc.hasOverdraft
                )
            }
        } else defaultCreditCards
    }

    // Default categories fallback
    val defaultCategories = remember {
        listOf(
            ExpenseCategory("cat_rest", "Restaurantes", Icons.Default.Restaurant, Color(0xFFEA580C), Color(0xFFFFF7ED)),
            ExpenseCategory("cat_shop", "Compras", Icons.Default.ShoppingBag, Color(0xFF2563EB), Color(0xFFEFF6FF)),
            ExpenseCategory("cat_sup", "Supermercado", Icons.Default.Storefront, Color(0xFF16A34A), Color(0xFFDCFCE7)),
            ExpenseCategory("cat_ent", "Entretenimiento", Icons.Default.Movie, Color(0xFF7C3AED), Color(0xFFF5F3FF)),
            ExpenseCategory("cat_tra", "Transporte", Icons.Default.DirectionsCar, Color(0xFFD97706), Color(0xFFFEF3C7)),
            ExpenseCategory("cat_via", "Viajes", Icons.Default.Flight, Color(0xFF0284C7), Color(0xFFE0F2FE)),
            ExpenseCategory("cat_hog", "Hogar", Icons.Default.Home, Color(0xFF0D9488), Color(0xFFCCFBF1)),
            ExpenseCategory("cat_edu", "Educación", Icons.Default.School, Color(0xFF9333EA), Color(0xFFFAF5FF))
        )
    }

    val roomCategoriesState = financialRepository?.getCategories()?.collectAsState(initial = emptyList())
    val displayCategories = remember(roomCategoriesState?.value) {
        val roomCats = roomCategoriesState?.value ?: emptyList()
        val customMapped = roomCats.filter { it.type == "EXPENSE" }.map { c ->
            val icon = resolveCategoryIcon(c.icon, c.name)
            val color = try { Color(android.graphics.Color.parseColor(c.colorHex)) } catch (_: Exception) { Color(0xFF0284C7) }
            ExpenseCategory(id = c.id, name = c.name, icon = icon, color = color, bgColor = color.copy(alpha = 0.15f))
        }
        (defaultCategories + customMapped).distinctBy { it.name.trim().lowercase() }
    }

    // Form State
    var formState by remember {
        mutableStateOf(
            CardPurchaseFormState(
                selectedCurrency = lastUsedCurrency,
                selectedCard = creditCardList.firstOrNull(),
                selectedCategory = displayCategories.firstOrNull() ?: defaultCategories[0]
            )
        )
    }

    // Modals
    var showCardPickerSheet by remember { mutableStateOf(false) }
    var showCurrencyPickerSheet by remember { mutableStateOf(false) }
    var showCategoryPickerSheet by remember { mutableStateOf(false) }
    var showCategoryCreationDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showQuotaBlockedPopup by remember { mutableStateOf(false) }
    var showSuccessVoucherPopup by remember { mutableStateOf(false) }
    var successVoucherData by remember { mutableStateOf<CardPurchaseVoucher?>(null) }
    var categorySheetSearchQuery by remember { mutableStateOf("") }

    val receiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            formState = formState.copy(selectedReceiptUri = uri)
        }
    }

    val quickMerchants = listOf("Amazon", "MercadoLibre", "Éxito", "Rappi", "Uber", "Netflix", "Spotify", "Zara", "Avianca", "Homecenter")
    val spanishDateFormatter = remember { DateTimeFormatter.ofPattern("d 'de' MMMM, yyyy", Locale("es", "ES")) }

    val onPerformSavePurchase: () -> Unit = {
        val card = formState.selectedCard
        val amount = formState.numericAmount
        if (amount <= 0) {
            formState = formState.copy(errorMessage = "Ingresa un monto válido para la compra")
        } else if (card == null) {
            formState = formState.copy(errorMessage = "Debes seleccionar una tarjeta de crédito")
        } else if (!card.allowsOverdraft && amount > card.availableQuota) {
            showQuotaBlockedPopup = true
        } else if (!formState.isSubmitting) {
            formState = formState.copy(isSubmitting = true, errorMessage = null)
            scope.launch {
                try {
                    val finalMerchant = formState.merchantNameInput.trim().ifBlank { "Comercio General" }
                    val finalCategory = formState.selectedCategory?.name ?: "Compras"
                    val nowTime = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a", Locale("es", "CO")))
                    val authCode = "AUTH-${(100000..999999).random()}"

                    if (financialRepository != null) {
                        val result = financialRepository.recordExpense(
                            userId = userId,
                            accountId = card.id,
                            accountName = card.name,
                            categoryId = formState.selectedCategory?.id ?: "cat_shop",
                            categoryName = finalCategory,
                            amount = amount,
                            currency = formState.selectedCurrency,
                            dateFormatted = formState.selectedDate.toString(),
                            description = "Compra con tarjeta en $finalMerchant (${formState.installmentsCount} cuotas)",
                            receiptUrl = formState.selectedReceiptUri?.toString(),
                            isRecurring = false,
                            saveAsTemplate = false,
                            tags = formState.tagsList
                        )
                        if (result.isSuccess) {
                            successVoucherData = CardPurchaseVoucher(
                                transactionId = result.getOrNull()?.id ?: "TRX-${UUID.randomUUID().toString().take(8).uppercase()}",
                                authCode = authCode,
                                cardName = card.name,
                                cardMask = card.mask,
                                merchant = finalMerchant,
                                amount = amount,
                                currency = formState.selectedCurrency,
                                installments = formState.installmentsCount,
                                monthlyPayment = formState.monthlyPaymentEstimated,
                                dateFormatted = formState.selectedDate.format(spanishDateFormatter),
                                timeFormatted = nowTime,
                                remainingQuota = (card.availableQuota - amount).coerceAtLeast(0.0),
                                category = finalCategory
                            )
                            showSuccessVoucherPopup = true
                            onPurchaseSuccess(card.id, card.name, amount, formState.selectedCurrency, formState.installmentsCount, finalMerchant, finalCategory, formState.selectedDate.toString())
                        } else {
                            formState = formState.copy(errorMessage = result.exceptionOrNull()?.message ?: "Error al registrar compra")
                        }
                    } else {
                        onPurchaseSuccess(card.id, card.name, amount, formState.selectedCurrency, formState.installmentsCount, finalMerchant, finalCategory, formState.selectedDate.toString())
                        onDismiss()
                    }
                } catch (e: Exception) {
                    formState = formState.copy(errorMessage = e.message ?: "Error al procesar compra")
                } finally {
                    formState = formState.copy(isSubmitting = false)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP HEADER CON GRADIENTE PREMIUM
            Surface(modifier = Modifier.fillMaxWidth(), color = KashBlue) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xFF0369A1), KashBlue, Color(0xFF0EA5E9))))
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).testTag("back_button_card_purchase")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White, modifier = Modifier.size(20.dp))
                            }

                            Text("Compra con Tarjeta", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (formState.isFormValid) Color.White else Color.White.copy(alpha = 0.2f),
                                shadowElevation = if (formState.isFormValid) 4.dp else 0.dp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable(enabled = formState.isFormValid) { onPerformSavePurchase() }
                                    .testTag("top_bar_save_purchase_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = if (formState.isFormValid) KashBlue else Color.White, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = if (formState.isSubmitting) "Guardando..." else "Guardar",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (formState.isFormValid) KashBlue else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // MAIN SCROLLABLE FORM
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (formState.errorMessage != null) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFEF2F2), border = BorderStroke(1.dp, Color(0xFFFECACA)), modifier = Modifier.fillMaxWidth()) {
                        Text(formState.errorMessage ?: "", fontSize = 12.sp, color = Color(0xFFDC2626), modifier = Modifier.padding(12.dp))
                    }
                }

                // 1. AMOUNT CARD (HERO DE VALORES ARRIBA)
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("IMPORTE DE LA COMPRA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.8.sp)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF0F9FF),
                                border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                                modifier = Modifier.clickable { showCurrencyPickerSheet = true }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = formState.selectedCurrency, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = KashBlue)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = KashBlue, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        // Input field
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("$ ", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            BasicTextField(
                                value = formState.rawAmountDigits,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() || it == '.' }
                                    if (filtered.count { it == '.' } <= 1 && filtered.length <= 15) {
                                        formState = formState.copy(rawAmountDigits = filtered)
                                    }
                                },
                                textStyle = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A)),
                                cursorBrush = SolidColor(KashBlue),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("card_purchase_amount_input"),
                                decorationBox = { inner ->
                                    if (formState.rawAmountDigits.isEmpty()) Text("0", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF94A3B8))
                                    inner()
                                }
                            )
                            if (formState.rawAmountDigits.isNotEmpty()) {
                                IconButton(onClick = { formState = formState.copy(rawAmountDigits = "") }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Quick Chips
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(50_000L to "$50k", 100_000L to "$100k", 500_000L to "$500k", 1_000_000L to "$1M").forEach { (inc, lbl) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF1F5F9),
                                    modifier = Modifier.weight(1f).clickable {
                                        val cur = formState.rawAmountDigits.toDoubleOrNull() ?: 0.0
                                        formState = formState.copy(rawAmountDigits = (cur + inc).toLong().toString())
                                    }
                                ) {
                                    Text(text = "+$lbl", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 6.dp))
                                }
                            }
                        }
                    }
                }

                // 2. CREDIT CARD SELECTOR CARD
                CreditCardSelectorCard(
                    selectedCard = formState.selectedCard,
                    numericAmount = formState.numericAmount,
                    onOpenCardPicker = { showCardPickerSheet = true }
                )

                // 3. INSTALLMENTS PICKER & DEFERRED INTEREST SUMMARY CARD
                InstallmentsSliderPicker(
                    installmentsCount = formState.installmentsCount,
                    onInstallmentsChanged = { formState = formState.copy(installmentsCount = it) }
                )

                DeferredInterestSummaryCard(
                    numericAmount = formState.numericAmount,
                    installmentsCount = formState.installmentsCount,
                    interestRateEa = formState.rateEa,
                    monthlyPaymentEstimated = formState.monthlyPaymentEstimated,
                    totalInterestCalculated = formState.totalInterestCalculated,
                    totalDebtToPayCalculated = formState.totalDebtToPayCalculated,
                    selectedCurrency = formState.selectedCurrency
                )

                // 4. MERCHANT & CATEGORY CARD
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("ESTABLECIMIENTO Y CATEGORÍA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.8.sp)

                        // Merchant input
                        OutlinedTextField(
                            value = formState.merchantNameInput,
                            onValueChange = { formState = formState.copy(merchantNameInput = it) },
                            placeholder = { Text("Nombre del comercio (ej. Amazon, Éxito)...", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = KashBlue) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Quick merchants chips
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            quickMerchants.take(6).forEach { m ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (formState.merchantNameInput.equals(m, ignoreCase = true)) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (formState.merchantNameInput.equals(m, ignoreCase = true)) KashBlue else Color(0xFFE2E8F0)),
                                    modifier = Modifier.clickable { formState = formState.copy(merchantNameInput = m) }
                                ) {
                                    Text(m, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (formState.merchantNameInput.equals(m, ignoreCase = true)) KashBlue else Color(0xFF334155), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }

                        // Category selector trigger
                        val currentCategory = formState.selectedCategory
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth().clickable { showCategoryPickerSheet = true }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (currentCategory != null) {
                                    Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(currentCategory.bgColor), contentAlignment = Alignment.Center) {
                                        Icon(currentCategory.icon, contentDescription = null, tint = currentCategory.color, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Categoría", fontSize = 10.sp, color = Color(0xFF64748B))
                                        Text(currentCategory.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }
                                } else {
                                    Icon(Icons.Default.Category, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Seleccionar categoría", fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.weight(1f))
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF94A3B8))
                            }
                        }
                    }
                }

                // 5. FECHA, NOTAS Y COMPROBANTE
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("FECHA Y DETALLES ADICIONALES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.8.sp)

                        // Date Picker Row
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth().clickable { showDatePickerDialog = true }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = KashBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Fecha de la compra", fontSize = 10.sp, color = Color(0xFF64748B))
                                    Text(formState.selectedDate.format(spanishDateFormatter), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF94A3B8))
                            }
                        }

                        // Notes TextField
                        OutlinedTextField(
                            value = formState.notesText,
                            onValueChange = { formState = formState.copy(notesText = it) },
                            placeholder = { Text("Nota opcional o descripción de la compra...", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Attach receipt
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth().clickable { receiptPickerLauncher.launch(arrayOf("image/*", "application/pdf")) }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (formState.selectedReceiptUri != null) Icons.Default.CheckCircle else Icons.Default.PhotoCamera, contentDescription = null, tint = KashBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (formState.selectedReceiptUri != null) "Comprobante adjuntado" else "Adjuntar comprobante o voucher",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // BOTTOM ACTION BUTTON
                Button(
                    onClick = onPerformSavePurchase,
                    enabled = formState.isFormValid,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KashBlue, disabledContainerColor = Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_card_purchase_bottom_btn")
                ) {
                    if (formState.isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Registrando compra...", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Text(
                            text = if (formState.numericAmount > 0) "Registrar compra (${formatCardMoney(formState.numericAmount, formState.selectedCurrency)})" else "Registrar compra",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // MODAL DIALOGS
    if (showCardPickerSheet) {
        CreditCardPickerSheet(
            creditCardList = creditCardList,
            selectedCard = formState.selectedCard,
            onSelectCard = { card ->
                formState = formState.copy(selectedCard = card)
                showCardPickerSheet = false
            },
            onNavigateToAddCard = {
                showCardPickerSheet = false
                onNavigateToAddCard?.invoke()
            },
            onDismiss = { showCardPickerSheet = false }
        )
    }

    if (showCurrencyPickerSheet) {
        CurrencyPickerModal(
            selectedCurrencyCode = formState.selectedCurrency,
            onCurrencySelected = { code ->
                formState = formState.copy(selectedCurrency = code)
                prefs.edit().putString("last_used_currency", code).apply()
                showCurrencyPickerSheet = false
            },
            onDismiss = { showCurrencyPickerSheet = false },
            financialRepository = financialRepository
        )
    }

    if (showCategoryPickerSheet) {
        val filteredCats = remember(displayCategories, categorySheetSearchQuery) {
            if (categorySheetSearchQuery.isBlank()) displayCategories
            else displayCategories.filter { it.name.contains(categorySheetSearchQuery, ignoreCase = true) }
        }

        ModalBottomSheet(
            onDismissRequest = {
                categorySheetSearchQuery = ""
                showCategoryPickerSheet = false
            },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Seleccionar Categoría", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text("Sincronizadas con tus gastos y presupuesto.", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.clickable { showCategoryCreationDialog = true }
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = KashBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nueva", color = KashBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = categorySheetSearchQuery,
                    onValueChange = { categorySheetSearchQuery = it },
                    placeholder = { Text("Buscar categoría...", color = Color(0xFF94A3B8)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = KashBlue) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredCats.forEach { cat ->
                        val isSelected = formState.selectedCategory?.id == cat.id
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isSelected) KashBlue else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth().clickable {
                                formState = formState.copy(selectedCategory = cat)
                                categorySheetSearchQuery = ""
                                showCategoryPickerSheet = false
                            }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(cat.bgColor), contentAlignment = Alignment.Center) {
                                    Icon(cat.icon, contentDescription = null, tint = cat.color, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(cat.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF0F172A), modifier = Modifier.weight(1f))
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KashBlue, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCategoryCreationDialog) {
        CategoryCreationDialog(
            categoryType = "EXPENSE",
            onDismiss = { showCategoryCreationDialog = false },
            onCategoryCreated = { newName, iconKey, colorHex ->
                scope.launch {
                    val created = financialRepository?.createCategory(name = newName, icon = iconKey, colorHex = colorHex, type = "EXPENSE")
                    val icon = resolveCategoryIcon(created?.icon ?: iconKey, newName)
                    val parsedColor = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (_: Exception) { Color(0xFF0284C7) }
                    val newCatModel = ExpenseCategory(
                        id = created?.id ?: "cat_${System.currentTimeMillis()}",
                        name = newName,
                        icon = icon,
                        color = parsedColor,
                        bgColor = parsedColor.copy(alpha = 0.15f)
                    )
                    formState = formState.copy(selectedCategory = newCatModel)
                    showCategoryCreationDialog = false
                    showCategoryPickerSheet = false
                }
            }
        )
    }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = formState.selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        formState = formState.copy(selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())
                    }
                    showDatePickerDialog = false
                }) {
                    Text("Aceptar", fontWeight = FontWeight.Bold, color = KashBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) { Text("Cancelar", color = Color(0xFF64748B)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showQuotaBlockedPopup) {
        val card = formState.selectedCard
        val diff = formState.numericAmount - (card?.availableQuota ?: 0.0)

        AlertDialog(
            onDismissRequest = { showQuotaBlockedPopup = false },
            icon = {
                Surface(shape = CircleShape, color = Color(0xFFFEE2E2), modifier = Modifier.size(56.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(28.dp))
                    }
                }
            },
            title = {
                Text("Cupo Insuficiente en Tarjeta", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF0F172A), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("La transacción no pudo completarse porque excede el cupo disponible asignado a tu tarjeta de crédito.", fontSize = 13.sp, color = Color(0xFF475569), textAlign = TextAlign.Center, lineHeight = 17.sp)
                    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF8FAFC), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tarjeta:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(card?.name ?: "Tarjeta", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Cupo disponible:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(formatCardMoney(card?.availableQuota ?: 0.0, "COP"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Monto compra:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(formatCardMoney(formState.numericAmount, formState.selectedCurrency), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            }
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Faltante:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
                                Text(formatCardMoney(diff, "COP"), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB91C1C))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showQuotaBlockedPopup = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Entendido", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showSuccessVoucherPopup && successVoucherData != null) {
        val voucher = successVoucherData!!
        AlertDialog(
            onDismissRequest = {
                showSuccessVoucherPopup = false
                onDismiss()
            },
            icon = {
                Surface(shape = CircleShape, color = Color(0xFFDCFCE7), modifier = Modifier.size(60.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(34.dp))
                    }
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("¡Compra Aprobada!", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF0F172A))
                    Text("Voucher de Transacción Electrónica", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF8FAFC), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Monto:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(formatCardMoney(voucher.amount, voucher.currency), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Comercio:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(voucher.merchant, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tarjeta:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text("${voucher.cardName} (${voucher.cardMask})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Diferido:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text("${voucher.installments} cuota(s)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KashBlue)
                            }
                            if (voucher.installments > 1) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Cuota mensual:", fontSize = 12.sp, color = Color(0xFF64748B))
                                    Text(formatCardMoney(voucher.monthlyPayment, voucher.currency), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                }
                            }
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Autorización:", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text(voucher.authCode, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Fecha y Hora:", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("${voucher.dateFormatted} • ${voucher.timeFormatted}", fontSize = 11.sp, color = Color(0xFF475569))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Nuevo cupo disponible:", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text(formatCardMoney(voucher.remainingQuota, "COP"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessVoucherPopup = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Volver al Tablero", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
