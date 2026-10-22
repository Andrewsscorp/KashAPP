package com.example.presentation.account

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import com.example.core.ui.components.IosStyleSnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import com.example.core.ui.components.IosStyleSwitch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.KashAppApplication
import com.example.data.local.SecureDocumentManager
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountPayrollConfigEntity
import com.example.domain.repository.FinancialRepository
import com.example.presentation.account.components.CurrencyItemInfo
import com.example.presentation.account.components.CurrencyPickerModal
import com.example.presentation.account.components.LiveCurrencyConversionPreview
import com.example.presentation.account.components.WORLD_CURRENCIES_CATALOG
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import com.example.core.ui.adaptive.rememberKashAdaptiveInfo
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/**
 * VisualTransformation that formats numbers into thousand dot-separated currency values (e.g. 5000000 -> 5.000.000)
 */
private class AccountCurrencyDotVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        if (original.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val isNeg = original.startsWith("-")
        val unsignedOriginal = if (isNeg) original.substring(1) else original
        val hasDot = unsignedOriginal.contains(".")
        val parts = unsignedOriginal.split(".")
        val intStr = parts[0]
        val decStr = if (parts.size > 1) parts[1] else null

        val symbols = DecimalFormatSymbols(Locale("es", "CO")).apply {
            groupingSeparator = '.'
        }
        val formatter = DecimalFormat("#,###", symbols)
        val formattedInt = if (intStr.isNotEmpty()) {
            val num = intStr.toLongOrNull() ?: 0L
            val formatted = formatter.format(num)
            if (isNeg) "-$formatted" else formatted
        } else {
            if (isNeg) "-" else ""
        }

        val transformed = when {
            decStr != null -> "$formattedInt,$decStr"
            hasDot -> "$formattedInt,"
            else -> formattedInt
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val sub = original.take(offset)
                val subIsNeg = sub.startsWith("-")
                val subUnsigned = if (subIsNeg) sub.substring(1) else sub
                val subParts = subUnsigned.split(".")
                val subInt = subParts[0]
                val subDec = if (subParts.size > 1) subParts[1] else null
                val formattedSubInt = if (subInt.isNotEmpty()) {
                    val n = subInt.toLongOrNull() ?: 0L
                    val formatted = formatter.format(n)
                    if (subIsNeg) "-$formatted" else formatted
                } else {
                    if (subIsNeg) "-" else ""
                }
                val res = when {
                    subDec != null -> "$formattedSubInt,$subDec"
                    subUnsigned.contains(".") -> "$formattedSubInt,"
                    else -> formattedSubInt
                }
                return res.length.coerceAtMost(transformed.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val sub = transformed.take(offset)
                val digitsBefore = sub.count { it.isDigit() || it == ',' || it == '.' || it == '-' }
                return digitsBefore.coerceAtMost(original.length)
            }
        }

        return TransformedText(AnnotatedString(transformed), offsetMapping)
    }
}

/**
 * Standard field colors guaranteeing crisp dark black text (#0F172A) on all backgrounds.
 */
@Composable
private fun standardFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(0xFF0F172A),
    unfocusedTextColor = Color(0xFF0F172A),
    focusedContainerColor = Color(0xFFF8FAFC),
    unfocusedContainerColor = Color(0xFFF8FAFC),
    focusedBorderColor = Color(0xFF0284C7),
    unfocusedBorderColor = Color(0xFFCBD5E1),
    focusedLabelColor = Color(0xFF0284C7),
    unfocusedLabelColor = Color(0xFF64748B),
    cursorColor = Color(0xFF0284C7)
)

/**
 * Premium Apple Wallet Style Screen for creating and managing Financial Accounts
 * (Savings, Checking, Payroll, Digital Wallets, CDTs/Investments).
 * All text fields render in crisp black, currency inputs live format with dot separators,
 * and the currency selector connects to the complete 164 ISO currencies catalog + custom currency creation.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateAccountScreen(
    initialTypeCode: String = "SAVINGS",
    callerFlow: String = "dashboard",
    onAccountCreated: (account: AccountEntity, payrollConfig: AccountPayrollConfigEntity?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    financialRepository: FinancialRepository? = null,
    userId: String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val appContainer = remember { (context.applicationContext as KashAppApplication).appContainer }
    val adaptive = rememberKashAdaptiveInfo()
    val standardFieldColors = standardFieldColors()

    var isSaving by remember { mutableStateOf(false) }
    var accountErrorMessage by remember { mutableStateOf<String?>(null) }

    // Account Type
    var selectedTypeCode by remember { mutableStateOf(initialTypeCode) }
    val isPayroll = selectedTypeCode == "PAYROLL"

    // 1. Account Name
    var accountName by remember {
        mutableStateOf(
            when (initialTypeCode) {
                "PAYROLL" -> "Nómina Principal"
                "CHECKING" -> "Cuenta Corriente Principal"
                "CDT" -> "Mi CDT Inversión"
                "WALLET" -> "Billetera Digital"
                else -> "Cuenta de Ahorros Principal"
            }
        )
    }

    // 2. Bank & Entities
    val dbBanks by appContainer.financialRepository.getBanks(userId).collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedBank by remember { mutableStateOf("Bancolombia") }
    var showBankPickerModal by remember { mutableStateOf(false) }
    var bankSearchQuery by remember { mutableStateOf("") }

    // Currency (World 164 Catalog + DB Currencies + Creation)
    var selectedCurrency by remember { mutableStateOf("COP") }
    var showCurrencyPickerModal by remember { mutableStateOf(false) }

    val selectedCurrencyInfo = remember(selectedCurrency) {
        WORLD_CURRENCIES_CATALOG.find { it.code.equals(selectedCurrency, ignoreCase = true) }
            ?: CurrencyItemInfo(selectedCurrency, "Divisa Seleccionada", "$", "🌐")
    }

    // 3. Primary Balance Input & Calculations
    var rawBalanceInput by remember { mutableStateOf("") }
    var rawAvailableBalanceInput by remember { mutableStateOf("") }
    var rawHoldBalanceInput by remember { mutableStateOf("0") }

    val parsedBalanceResult = remember(rawBalanceInput, selectedTypeCode) {
        com.example.domain.currency.CurrencyConversionEngine.parseMonetaryInput(
            rawInput = rawBalanceInput,
            allowNegative = (selectedTypeCode == "CHECKING")
        )
    }
    val parsedBalance = remember(parsedBalanceResult) { parsedBalanceResult.amountOrZero }
    val parsedAvailableBalance = remember(rawAvailableBalanceInput, parsedBalance, selectedTypeCode) {
        if (rawAvailableBalanceInput.isBlank()) {
            parsedBalance
        } else {
            com.example.domain.currency.CurrencyConversionEngine.parseMonetaryInput(
                rawInput = rawAvailableBalanceInput,
                allowNegative = (selectedTypeCode == "CHECKING")
            ).amountOrZero
        }
    }
    val parsedHoldBalance = remember(rawHoldBalanceInput) {
        com.example.domain.currency.CurrencyConversionEngine.parseMonetaryInput(
            rawInput = rawHoldBalanceInput,
            allowNegative = false
        ).amountOrZero
    }

    // 4. Account Number & Security
    var rawAccountNumber by remember { mutableStateOf("") }
    var isAccountNumberVisible by remember { mutableStateOf(false) }
    var accountOwner by remember { mutableStateOf("") }
    var accountAlias by remember { mutableStateOf("") }
    var openingDate by remember { mutableStateOf(LocalDate.now()) }

    // 5. Rendimientos & Tasas
    var generatesInterest by remember { mutableStateOf(false) }
    var interestRateEaInput by remember { mutableStateOf("10.5") }
    var calcFrequency by remember { mutableStateOf("Diaria") }
    var payoutFrequency by remember { mutableStateOf("Mensual") }
    var compoundInterest by remember { mutableStateOf(true) }

    // 6. Sobregiro (Checking)
    var hasOverdraft by remember { mutableStateOf(false) }
    var overdraftLimitInput by remember { mutableStateOf("") }
    var overdraftRateInput by remember { mutableStateOf("24.5") }

    // 7. Payroll Specific Settings & Tax Configuration Link
    var employerName by remember { mutableStateOf("") }
    var habitualConcept by remember { mutableStateOf("Nómina") }
    var rawExpectedIncome by remember { mutableStateOf("") }
    val parsedExpectedIncome = remember(rawExpectedIncome) {
        com.example.domain.currency.CurrencyConversionEngine.parseMonetaryInput(rawExpectedIncome).amountOrZero
    }
    var periodicity by remember { mutableStateOf("Mensual") }
    var expectedPayDay by remember { mutableStateOf(30) }
    var autoRegisterPayroll by remember { mutableStateOf(false) }
    var exemptionManagementFee by remember { mutableStateOf(true) }
    var exemptionTax4x1000 by remember { mutableStateOf(false) }
    var freeAtmWithdrawals by remember { mutableStateOf(true) }
    val taxProfile by remember(userId) {
        appContainer.dianTaxRepository.observeTaxConfiguration(userId)
    }.collectAsStateWithLifecycle()
    var showTaxConfigDialogInCreateAccount by remember { mutableStateOf(false) }

    // 8. Favoritos & Enrutamiento
    var isFavorite by remember { mutableStateOf(false) }
    var favoriteForIncomes by remember { mutableStateOf(false) }
    var favoriteForExpenses by remember { mutableStateOf(false) }

    // 9. Personalización Visual & Media
    var selectedColorHex by remember { mutableStateOf("#0284C7") }
    var additionalNotes by remember { mutableStateOf("") }
    var attachedDocumentUri by remember { mutableStateOf<Uri?>(null) }
    var attachedDocumentType by remember { mutableStateOf<String?>(null) }
    val secureDocManager = remember { SecureDocumentManager(context) }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val saveRes = secureDocManager.saveDocumentSafely(uri, userId = userId)
            if (saveRes.isSuccess) {
                val saved = saveRes.getOrThrow()
                attachedDocumentUri = Uri.parse(saved.localUri)
                attachedDocumentType = if (saved.mimeType.contains("pdf", ignoreCase = true)) "PDF" else "IMAGE"
            }
        }
    }

    val colorPalette = listOf(
        "#0284C7" to "Azul Zafiro",
        "#8B5CF6" to "Morado Real",
        "#10B981" to "Esmeralda",
        "#0D9488" to "Teal Petróleo",
        "#F59E0B" to "Ámbar Dorado",
        "#E11D48" to "Rojo Rubí",
        "#0F172A" to "Negro Carbón",
        "#4F46E5" to "Índigo Profundo"
    )

    val popularBanks = listOf("Bancolombia", "Nequi", "Davivienda", "Nu Colombia", "BBVA", "Daviplata", "Scotiabank", "Banco de Bogotá")

    val typeOptions = listOf(
        Triple("SAVINGS", "Ahorros", Icons.Default.AccountBalanceWallet),
        Triple("CHECKING", "Corriente", Icons.Default.CreditCard),
        Triple("PAYROLL", "Nómina", Icons.Default.BusinessCenter),
        Triple("WALLET", "Billetera", Icons.Default.Payments),
        Triple("CDT", "Inversión", Icons.Default.TrendingUp)
    )

    fun formatMoneyDisplay(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale("es", "CO")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val formatter = DecimalFormat("#,##0", symbols)
        return "$ ${formatter.format(amount)} $selectedCurrency"
    }

    BackHandler { onDismiss() }

    // Save Account Function
    fun performSaveAccount() {
        if (accountName.isBlank()) {
            val errorMsg = "Ingresa un nombre para la cuenta"
            accountErrorMessage = errorMsg
            coroutineScope.launch {
                snackbarHostState.showSnackbar(errorMsg)
            }
            return
        }
        if (parsedBalanceResult is com.example.domain.currency.ParsedMonetaryInput.Invalid) {
            val errorMsg = parsedBalanceResult.reason
            accountErrorMessage = errorMsg
            coroutineScope.launch {
                snackbarHostState.showSnackbar(errorMsg)
            }
            return
        }
        accountErrorMessage = null
        isSaving = true

        val cleanName = accountName.trim()
        val cleanBank = selectedBank.trim()
        val cleanCurrency = selectedCurrency.trim()
        val cleanAccountNumber = rawAccountNumber.trim()
        val cleanEmployer = employerName.trim().ifEmpty { null }
        val cleanConcept = habitualConcept.trim().ifEmpty { "Nómina" }
        val cleanAlias = accountAlias.trim().ifEmpty { null }
        val cleanOwner = accountOwner.trim().ifEmpty { null }
        val rateEaClean = interestRateEaInput.replace("%", "").replace(",", ".").trim().toDoubleOrNull()

        val createdAccount = AccountEntity(
            id = "acc_${UUID.randomUUID()}",
            userId = userId,
            name = cleanName,
            balance = parsedBalance,
            currency = cleanCurrency,
            type = selectedTypeCode,
            bankName = cleanBank,
            isFavorite = isFavorite || favoriteForIncomes || favoriteForExpenses,
            favoriteForIncomes = favoriteForIncomes,
            favoriteForExpenses = favoriteForExpenses,
            employerName = cleanEmployer,
            accountNumberEncrypted = cleanAccountNumber.ifBlank { null },
            accountNumberMasked = if (cleanAccountNumber.length > 4) "•••• ${cleanAccountNumber.takeLast(4)}" else cleanAccountNumber.ifBlank { null },
            openingDate = openingDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            kashappDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            generatesInterest = generatesInterest,
            interestRateEa = if (generatesInterest) rateEaClean else null,
            calcFrequency = if (generatesInterest) calcFrequency else null,
            payoutFrequency = if (generatesInterest) payoutFrequency else null,
            compoundInterest = compoundInterest,
            purposes = if (isPayroll) "Nómina,Ingresos" else "Ahorro",
            customColorHex = selectedColorHex,
            iconName = if (isPayroll) "business_center" else "account_balance",
            minimumBalance = 0.0,
            monthlyFee = 0.0,
            feeChargeDay = 30,
            supportingDocumentUri = attachedDocumentUri?.toString(),
            supportingDocumentType = attachedDocumentType,
            availableBalance = parsedAvailableBalance,
            holdBalance = parsedHoldBalance,
            country = "CO",
            favoriteRouting = if (favoriteForIncomes && !favoriteForExpenses) "INCOME" else if (!favoriteForIncomes && favoriteForExpenses) "EXPENSES" else "GENERAL",
            accountOwner = cleanOwner,
            alias = cleanAlias,
            hasOverdraft = hasOverdraft,
            overdraftLimit = com.example.domain.currency.CurrencyConversionEngine.parseUserInputAmount(overdraftLimitInput),
            overdraftRateEa = overdraftRateInput.replace("%", "").replace(",", ".").trim().toDoubleOrNull() ?: 0.0,
            isTaxExempt = exemptionTax4x1000
        )

        val payrollConfig = if (isPayroll) {
            AccountPayrollConfigEntity(
                id = "paycfg_${UUID.randomUUID()}",
                accountId = createdAccount.id,
                employerName = cleanEmployer ?: "Empresa Principal",
                defaultConcept = cleanConcept,
                expectedIncome = parsedExpectedIncome,
                periodicity = when (periodicity) {
                    "Quincenal" -> "BIWEEKLY"
                    "Semanal" -> "WEEKLY"
                    else -> "MONTHLY"
                },
                expectedPayDay = expectedPayDay,
                autoRegister = autoRegisterPayroll,
                exemptionManagementFee = exemptionManagementFee,
                exemptionTax4x1000 = exemptionTax4x1000,
                freeAtmWithdrawals = freeAtmWithdrawals
            )
        } else null

        coroutineScope.launch {
            try {
                val repo = financialRepository ?: appContainer.financialRepository
                val saved = repo.createAccount(
                    name = createdAccount.name,
                    type = createdAccount.type,
                    balance = createdAccount.balance,
                    currency = createdAccount.currency,
                    userId = createdAccount.userId,
                    bankName = createdAccount.bankName,
                    isFavorite = createdAccount.isFavorite,
                    accountNumber = cleanAccountNumber.ifBlank { null },
                    openingDate = createdAccount.openingDate,
                    kashappDate = createdAccount.kashappDate,
                    generatesInterest = createdAccount.generatesInterest,
                    interestRateEa = createdAccount.interestRateEa,
                    calcFrequency = createdAccount.calcFrequency,
                    payoutFrequency = createdAccount.payoutFrequency,
                    compoundInterest = createdAccount.compoundInterest,
                    purposes = createdAccount.purposes,
                    customColorHex = createdAccount.customColorHex,
                    iconName = createdAccount.iconName,
                    minimumBalance = createdAccount.minimumBalance,
                    monthlyFee = createdAccount.monthlyFee,
                    feeChargeDay = createdAccount.feeChargeDay,
                    availableBalance = createdAccount.availableBalance,
                    holdBalance = createdAccount.holdBalance,
                    country = createdAccount.country,
                    favoriteRouting = createdAccount.favoriteRouting,
                    favoriteForIncomes = createdAccount.favoriteForIncomes,
                    favoriteForExpenses = createdAccount.favoriteForExpenses,
                    employerName = createdAccount.employerName,
                    accountOwner = createdAccount.accountOwner,
                    alias = createdAccount.alias,
                    hasOverdraft = createdAccount.hasOverdraft,
                    overdraftLimit = createdAccount.overdraftLimit,
                    overdraftRateEa = createdAccount.overdraftRateEa,
                    isTaxExempt = createdAccount.isTaxExempt,
                    payrollConfig = payrollConfig
                )
                isSaving = false
                onAccountCreated(saved, payrollConfig)
            } catch (e: Exception) {
                isSaving = false
                val errorMsg = "Error al crear la cuenta: ${e.localizedMessage ?: "Error en base de datos"}"
                accountErrorMessage = errorMsg
                snackbarHostState.showSnackbar(errorMsg)
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("create_account_screen"),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0284C7)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0369A1),
                                    Color(0xFF0284C7),
                                    Color(0xFF1E40AF)
                                )
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = adaptive.maxContentWidth),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Back Button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 44.dp)
                                    .clickable { onDismiss() }
                                    .padding(4.dp)
                                    .testTag("back_button")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Volver",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Volver",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Center Title
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isPayroll) "Cuenta Nómina" else "Nueva Cuenta",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.2).sp
                                )
                                Text(
                                    text = "Gestión de liquidez y ahorros",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                )
                            }

                            // Top Guardar Pill Action Button
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (accountName.isNotBlank()) Color.White else Color.White.copy(alpha = 0.3f),
                                shadowElevation = if (accountName.isNotBlank()) 3.dp else 0.dp,
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 44.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable(enabled = !isSaving) { performSaveAccount() }
                                    .testTag("top_bar_save_account_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isSaving) {
                                        CircularProgressIndicator(
                                            color = KashNavy,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (accountName.isNotBlank()) KashNavy else Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = "Guardar",
                                        color = if (accountName.isNotBlank()) KashNavy else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp),
                color = Color.White
            ) {
                val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = adaptive.maxContentWidth)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .padding(bottom = bottomPadding)
                    ) {
                        Button(
                            onClick = { performSaveAccount() },
                            enabled = !isSaving && accountName.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("save_account_button_bottom"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Guardando cuenta...", color = Color.White, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isPayroll) "Guardar Cuenta de Nómina" else "Guardar Cuenta Bancaria",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { IosStyleSnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = adaptive.maxContentWidth)
                    .imePadding()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp)
            ) {
            // ==========================================
            // 1. APPLE WALLET LIVE VIRTUAL CARD PREVIEW
            // ==========================================
            item {
                val cardColor = try {
                    Color(android.graphics.Color.parseColor(selectedColorHex))
                } catch (_: Exception) {
                    Color(0xFF0284C7)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .testTag("virtual_account_card_hero"),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        cardColor,
                                        cardColor.copy(alpha = 0.85f),
                                        Color(0xFF0F172A)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top Row: Bank name + Account Type badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = selectedBank,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Surface(
                                    color = Color.White.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = when (selectedTypeCode) {
                                            "PAYROLL" -> "NÓMINA"
                                            "CHECKING" -> "CORRIENTE"
                                            "WALLET" -> "BILLETERA"
                                            "CDT" -> "CDT / INVERSIÓN"
                                            else -> "AHORROS"
                                        },
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Middle: Account Name & Masked digits
                            Column {
                                Text(
                                    text = accountName.ifBlank { "Nombre de la cuenta" },
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val maskedNumber = if (rawAccountNumber.length >= 4) "•••• ${rawAccountNumber.takeLast(4)}" else "•••• 0000"
                                Text(
                                    text = maskedNumber,
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Bottom Row: Live Balance + Interest rate badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = "SALDO DISPONIBLE",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = formatMoneyDisplay(parsedBalance),
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                if (generatesInterest) {
                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.9f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${interestRateEaInput.ifBlank { "0" }}% E.A.",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 2. PRIMARY BALANCE INPUT (WITH DOT THOUSAND FORMATTING)
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SALDO INICIAL / DISPONIBLE *",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )

                            // Currency pill selector
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier.clickable { showCurrencyPickerModal = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(selectedCurrencyInfo.flag, fontSize = 14.sp)
                                    Text(
                                        text = selectedCurrency,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0284C7)
                                    )
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        // Oversized formatted balance input with live thousand dot separators and crisp black text
                        val isBalanceInvalid = parsedBalanceResult is com.example.domain.currency.ParsedMonetaryInput.Invalid
                        OutlinedTextField(
                            value = rawBalanceInput,
                            onValueChange = { input ->
                                val clean = input.filter { it.isDigit() || it == '.' || it == ',' || (selectedTypeCode == "CHECKING" && it == '-') }
                                rawBalanceInput = clean
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("account_balance_input"),
                            isError = isBalanceInvalid,
                            placeholder = { Text("0", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8)) },
                            leadingIcon = {
                                Text(
                                    text = "$",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBalanceInvalid) Color(0xFFDC2626) else Color(0xFF0284C7),
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            },
                            trailingIcon = {
                                Text(
                                    text = selectedCurrency,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            supportingText = {
                                if (isBalanceInvalid) {
                                    Text(
                                        text = (parsedBalanceResult as com.example.domain.currency.ParsedMonetaryInput.Invalid).reason,
                                        color = Color(0xFFDC2626),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else if (rawBalanceInput.isBlank() || parsedBalanceResult is com.example.domain.currency.ParsedMonetaryInput.Empty) {
                                    Text(
                                        text = "Saldo inicial: $0 (Por defecto)",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp
                                    )
                                }
                            },
                            visualTransformation = AccountCurrencyDotVisualTransformation(),
                            textStyle = TextStyle(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBalanceInvalid) Color(0xFFDC2626) else Color(0xFF0F172A)
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = if (selectedTypeCode == "CHECKING") KeyboardType.Text else KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            colors = standardFieldColors
                        )

                        // Quick Preset Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                500000.0 to "+$500K",
                                1000000.0 to "+$1M",
                                2000000.0 to "+$2M",
                                5000000.0 to "+$5M"
                            ).forEach { (amt, label) ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFEFF6FF),
                                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                    modifier = Modifier.clickable {
                                        val current = com.example.domain.currency.CurrencyConversionEngine.parseUserInputAmount(rawBalanceInput)
                                        rawBalanceInput = (current + amt).toLong().toString()
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0284C7),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            if (rawBalanceInput.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFFEF2F2),
                                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                    modifier = Modifier.clickable { rawBalanceInput = "" }
                                ) {
                                    Text(
                                        text = "Limpiar",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Live Currency Conversion Preview
                        val enteredBal = com.example.domain.currency.CurrencyConversionEngine.parseUserInputAmount(rawBalanceInput)
                        Spacer(modifier = Modifier.height(12.dp))
                        LiveCurrencyConversionPreview(
                            amount = enteredBal,
                            inputCurrency = selectedCurrency,
                            userId = userId
                        )
                    }
                }
            }

            // ==========================================
            // 3. TIPO DE CUENTA & ENTIDAD BANCARIA
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "TIPO DE CUENTA & BANCO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )

                        // Type Pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            typeOptions.forEach { (typeCode, label, icon) ->
                                val isSelected = selectedTypeCode == typeCode
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFF0284C7) else Color(0xFFF1F5F9),
                                    modifier = Modifier.clickable {
                                        selectedTypeCode = typeCode
                                        accountName = when (typeCode) {
                                            "PAYROLL" -> "Nómina Principal"
                                            "CHECKING" -> "Cuenta Corriente"
                                            "WALLET" -> "Billetera Digital"
                                            "CDT" -> "CDT Inversión"
                                            else -> "Cuenta de Ahorros"
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else Color(0xFF475569),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFF334155)
                                        )
                                    }
                                }
                            }
                        }

                        // Account Name Field (Explicit black text #0F172A)
                        OutlinedTextField(
                            value = accountName,
                            onValueChange = { accountName = it },
                            label = { Text("Nombre de la cuenta *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("account_name_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF0284C7)) },
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            ),
                            colors = standardFieldColors
                        )

                        // Bank Selector Chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Banco / Entidad Financiera *", fontSize = 12.sp, color = Color(0xFF64748B))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                popularBanks.forEach { bank ->
                                    val isSelected = selectedBank.equals(bank, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedBank = bank },
                                        label = { Text(bank, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF0284C7),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }

                                FilterChip(
                                    selected = false,
                                    onClick = { showBankPickerModal = true },
                                    label = { Text("Más bancos...", fontSize = 11.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                            }
                        }

                        // Moneda & Selector Button
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCurrencyPickerModal = true }
                                .testTag("open_currency_picker_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(selectedCurrencyInfo.flag, fontSize = 22.sp)
                                    Column {
                                        Text("Moneda de la cuenta", fontSize = 11.sp, color = Color(0xFF64748B))
                                        Text(
                                            text = "${selectedCurrencyInfo.code} - ${selectedCurrencyInfo.name} (${selectedCurrencyInfo.symbol})",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                }

                                Surface(
                                    color = Color(0xFF0284C7),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Text("Cambiar", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 4. DATOS BANCARIOS & SEGURIDAD
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "IDENTIFICACIÓN & SEGURIDAD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )

                        // Account Number
                        OutlinedTextField(
                            value = rawAccountNumber,
                            onValueChange = { rawAccountNumber = it },
                            label = { Text("Número de cuenta / Celular (Opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = if (isAccountNumberVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF64748B)) },
                            trailingIcon = {
                                IconButton(onClick = { isAccountNumberVisible = !isAccountNumberVisible }) {
                                    Icon(
                                        imageVector = if (isAccountNumberVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Ver número",
                                        tint = Color(0xFF64748B)
                                    )
                                }
                            },
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A)),
                            colors = standardFieldColors
                        )

                        // Titular & Alias
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = accountOwner,
                                onValueChange = { accountOwner = it },
                                label = { Text("Titular", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A)),
                                colors = standardFieldColors
                            )
                            OutlinedTextField(
                                value = accountAlias,
                                onValueChange = { accountAlias = it },
                                label = { Text("Alias / Nota", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A)),
                                colors = standardFieldColors
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 5. RENDIMIENTOS, INTERESES & SOBREGIRO
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "RENDIMIENTOS & TASAS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )

                        // Generates Interest Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Genera Intereses / Rendimientos", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                Text("Aplica tasa E.A. sobre el saldo", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                            IosStyleSwitch(
                                checked = generatesInterest,
                                onCheckedChange = { generatesInterest = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF10B981))
                            )
                        }

                        if (generatesInterest) {
                            OutlinedTextField(
                                value = interestRateEaInput,
                                onValueChange = { interestRateEaInput = it },
                                label = { Text("Tasa Efectiva Anual (E.A. %)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = { Text("% E.A.", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), modifier = Modifier.padding(end = 12.dp)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)),
                                colors = standardFieldColors
                            )
                        }

                        if (selectedTypeCode == "CHECKING") {
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Línea de Sobregiro", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                    Text("Cupo extra autorizado por el banco", fontSize = 11.sp, color = Color(0xFF64748B))
                                }
                                IosStyleSwitch(
                                    checked = hasOverdraft,
                                    onCheckedChange = { hasOverdraft = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0284C7))
                                )
                            }

                            if (hasOverdraft) {
                                OutlinedTextField(
                                    value = overdraftLimitInput,
                                    onValueChange = { overdraftLimitInput = it.filter { c -> c.isDigit() || c == '.' } },
                                    label = { Text("Cupo de sobregiro ($)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    visualTransformation = AccountCurrencyDotVisualTransformation(),
                                    textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A)),
                                    colors = standardFieldColors
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 6. INFORMACIÓN DE NÓMINA (SI ES NÓMINA)
            // ==========================================
            if (isPayroll) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "PARÁMETROS DE NÓMINA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7),
                                letterSpacing = 0.5.sp
                            )

                            OutlinedTextField(
                                value = employerName,
                                onValueChange = { employerName = it },
                                label = { Text("Empresa / Empleador") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A)),
                                colors = standardFieldColors
                            )

                            OutlinedTextField(
                                value = rawExpectedIncome,
                                onValueChange = { rawExpectedIncome = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Ingreso mensual esperado ($)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = AccountCurrencyDotVisualTransformation(),
                                textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)),
                                colors = standardFieldColors
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Exenta de 4x1000", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                IosStyleSwitch(checked = exemptionTax4x1000, onCheckedChange = { exemptionTax4x1000 = it })
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 6B. VINCULACIÓN CON MÓDULO TRIBUTARIO (DIAN / INTERNACIONAL)
            // ==========================================
            item {
                com.example.feature.impuestosdian.ui.AccountCreationTaxLinkCard(
                    profile = taxProfile,
                    accountType = selectedTypeCode,
                    isTaxExempt = exemptionTax4x1000,
                    onTaxExemptChange = { exemptionTax4x1000 = it },
                    onOpenTaxConfig = { showTaxConfigDialogInCreateAccount = true }
                )
            }

            // ==========================================
            // 7. PERSONALIZACIÓN DE COLOR & SOPORTE
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "COLOR DE LA TARJETA VIRTUAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            colorPalette.forEach { (hex, name) ->
                                val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                                val c = Color(android.graphics.Color.parseColor(hex))
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) Color(0xFF0F172A) else Color.Transparent, CircleShape)
                                        .clickable { selectedColorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        // Document support
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Adjuntar Extracto / Certificación", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                Text(
                                    text = if (attachedDocumentUri != null) "Documento adjuntado ($attachedDocumentType)" else "Opcional (PDF o Imagen)",
                                    fontSize = 11.sp,
                                    color = if (attachedDocumentUri != null) Color(0xFF10B981) else Color(0xFF64748B)
                                )
                            }

                            Button(
                                onClick = { documentPickerLauncher.launch(arrayOf("image/*", "application/pdf")) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (attachedDocumentUri != null) "Cambiar" else "Adjuntar", color = Color(0xFF0284C7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

    // ==========================================
    // MODAL: CONFIGURACIÓN TRIBUTARIA VINCULADA
    // ==========================================
    if (showTaxConfigDialogInCreateAccount) {
        com.example.feature.impuestosdian.ui.TaxConfigurationDialog(
            initialProfile = taxProfile,
            onDismiss = { showTaxConfigDialogInCreateAccount = false },
            onSave = { updated ->
                showTaxConfigDialogInCreateAccount = false
                coroutineScope.launch {
                    appContainer.dianTaxRepository.updateTaxConfiguration(updated)
                }
            }
        )
    }

    // ==========================================
    // MODAL: UNIVERSAL CURRENCY PICKER (164 WORLD CURRENCIES + CREATION)
    // ==========================================
    if (showCurrencyPickerModal) {
        CurrencyPickerModal(
            selectedCurrencyCode = selectedCurrency,
            onCurrencySelected = { newCurrency ->
                selectedCurrency = newCurrency
                showCurrencyPickerModal = false
            },
            onDismiss = { showCurrencyPickerModal = false },
            financialRepository = financialRepository ?: appContainer.financialRepository
        )
    }

    // ==========================================
    // MODAL: BANK PICKER BOTTOM SHEET
    // ==========================================
    if (showBankPickerModal) {
        ModalBottomSheet(
            onDismissRequest = { showBankPickerModal = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Selecciona tu Entidad Bancaria",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                OutlinedTextField(
                    value = bankSearchQuery,
                    onValueChange = { bankSearchQuery = it },
                    placeholder = { Text("Buscar banco...", color = Color(0xFF94A3B8)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF0284C7)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF0F172A)),
                    colors = standardFieldColors
                )

                val allBanks = remember(dbBanks, bankSearchQuery) {
                    val names = if (dbBanks.isNotEmpty()) dbBanks.map { it.name } else popularBanks + listOf("Banco Agrario", "Itaú", "Banco Caja Social", "Lulo Bank", "Ualá", "RappiPay")
                    if (bankSearchQuery.isBlank()) names.distinct() else names.filter { it.contains(bankSearchQuery, ignoreCase = true) }.distinct()
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(allBanks.size) { idx ->
                        val b = allBanks[idx]
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedBank.equals(b, ignoreCase = true)) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedBank = b
                                    showBankPickerModal = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF0284C7))
                                    Text(b, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                }
                                if (selectedBank.equals(b, ignoreCase = true)) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0284C7))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
