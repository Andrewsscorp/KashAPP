package com.example.presentation.investments.cdt

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AccountCdtEntity
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BankEntity
import com.example.data.local.entity.CurrencyEntity
import com.example.domain.repository.FinancialRepository
import com.example.domain.usecase.CalculateCdtYieldUseCase
import com.example.domain.usecase.CreateCdtUseCase
import com.example.presentation.account.components.AddBankEntityScreen
import com.example.presentation.account.components.AddCurrencyModal
import com.example.presentation.investments.cdt.components.CdtAlertsCard
import com.example.presentation.investments.cdt.components.CdtCapitalCard
import com.example.presentation.investments.cdt.components.CdtColorIconCard
import com.example.presentation.investments.cdt.components.CdtKeypadTarget
import com.example.presentation.investments.cdt.components.CdtMaturityActionCard
import com.example.presentation.investments.cdt.components.CdtNumericKeyboardDialog
import com.example.presentation.investments.cdt.components.CdtProfitabilityCard
import com.example.presentation.investments.cdt.components.CdtTermCard
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * Screen "Nuevo CDT" (Certificado de Depósito a Término).
 * Full compliance with Clean Architecture, responsive UI rules,
 * in-app keypad masks, and real-time yield calculation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCdtScreen(
    onCdtCreated: (AccountEntity, AccountCdtEntity) -> Unit,
    onDismiss: () -> Unit,
    financialRepository: FinancialRepository? = null,
    modifier: Modifier = Modifier
) {
    BackHandler { onDismiss() }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Use cases
    val calculateYieldUseCase = remember { CalculateCdtYieldUseCase() }
    val createCdtUseCase = remember(financialRepository) {
        financialRepository?.let { CreateCdtUseCase(it, calculateYieldUseCase) }
    }

    // Database state
    val dbAccounts by (financialRepository?.getAccounts("user_default") ?: flowOf(emptyList()))
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val dbBanks by (financialRepository?.getBanks("user_default") ?: flowOf(emptyList()))
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val dbCurrencies by (financialRepository?.getCurrencies("user_default") ?: flowOf(emptyList()))
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Form State (Clean starting state)
    var cdtName by remember { mutableStateOf("") }
    var selectedBank by remember { mutableStateOf("Bancolombia") }
    var selectedClearingCode by remember { mutableStateOf("001 Bancolombia") }
    var certificateNumber by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("COP") }
    var selectedCurrencyLabel by remember { mutableStateOf("COP - Peso colombiano") }

    // Capital & Rate (Clean empty inputs)
    var rawInitialAmount by remember { mutableStateOf("") }
    var rawInterestRate by remember { mutableStateOf("") }
    var rateType by remember { mutableStateOf("EA - Efectiva anual") }
    var payoutFrequency by remember { mutableStateOf("Al vencimiento") }
    var compounding by remember { mutableStateOf(true) }

    // Term & Dates
    var issueDate by remember { mutableStateOf(LocalDate.now()) }
    var termDays by remember { mutableIntStateOf(180) }

    // Maturity Action & Destination
    var actionAtMaturity by remember { mutableStateOf("ASK") }
    var selectedDestinationAccountId by remember { mutableStateOf<String?>(null) }
    var selectedSourceAccountId by remember { mutableStateOf<String?>(null) }
    var allowsEarlyWithdrawal by remember { mutableStateOf(false) }

    // Alerts
    var hasAlert by remember { mutableStateOf(true) }
    var alertDaysBefore by remember { mutableIntStateOf(15) }

    // Color & Icon
    var selectedColorHex by remember { mutableStateOf("#0284C7") }
    var selectedIconName by remember { mutableStateOf("ic_bank_default") }

    // Dialogs & Modals
    var showBankPickerModal by remember { mutableStateOf(false) }
    var showAddBankScreen by remember { mutableStateOf(false) }
    var showCurrencyPickerModal by remember { mutableStateOf(false) }
    var showAddCurrencyModal by remember { mutableStateOf(false) }
    var showDestinationAccountModal by remember { mutableStateOf(false) }
    var showSourceAccountModal by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCertNumberDialog by remember { mutableStateOf(false) }
    var activeKeypadTarget by remember { mutableStateOf<CdtKeypadTarget?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Reactive Financial Yield & Maturity calculation
    val calculation = remember(rawInitialAmount, rawInterestRate, termDays, issueDate) {
        val amount = rawInitialAmount.toDoubleOrNull() ?: 10000000.0
        val rate = rawInterestRate.replace(',', '.').toDoubleOrNull() ?: 10.5
        calculateYieldUseCase(
            initialAmount = amount,
            interestRateEa = rate,
            termDays = termDays,
            issueDate = issueDate
        )
    }

    // Destination account label
    val destinationAccount = remember(selectedDestinationAccountId, dbAccounts) {
        dbAccounts.find { it.id == selectedDestinationAccountId }
    }
    val destinationAccountLabel = destinationAccount?.let { "${it.name} •••• ${it.accountNumberMasked?.takeLast(4) ?: "0000"}" }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("create_cdt_screen"),
        containerColor = Color(0xFFF8FAFC),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Screen Header: Back button, Title + Subtitle (with typo fixed), Cancel button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF))
                            .testTag("cdt_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = KashBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Nuevo CDT",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashNavy
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        // Visual QA: Typo 'depósitoo' corrected to 'depósito'
                        Text(
                            text = "Registra un nuevo certificado de depósito a término para hacer crecer tu dinero.",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 15.sp,
                            maxLines = 2
                        )
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("cdt_cancel_btn")
                ) {
                    Text(
                        text = "✕ Cancelar",
                        color = KashBlue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        },
        bottomBar = {
            // Bottom Action Bar: "Guardar CDT"
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = {
                            if (isSaving) return@Button
                            val initialAmountVal = rawInitialAmount.toDoubleOrNull() ?: 0.0
                            val rateVal = rawInterestRate.replace(',', '.').toDoubleOrNull() ?: 0.0

                            val validation = com.example.domain.validation.FormValidator.validateCdt(
                                name = cdtName.ifBlank { "CDT $selectedBank $termDays días" },
                                bankName = selectedBank,
                                initialAmount = initialAmountVal,
                                interestRateEa = rateVal,
                                termDays = termDays
                            )
                            if (validation is com.example.domain.validation.FormValidator.ValidationResult.Invalid) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(validation.firstError)
                                }
                                return@Button
                            }

                            isSaving = true
                            coroutineScope.launch {
                                if (createCdtUseCase != null) {
                                    val result = createCdtUseCase(
                                        userId = "user_default",
                                        name = cdtName,
                                        bankName = selectedBank,
                                        currency = selectedCurrency,
                                        certificateNumber = certificateNumber,
                                        initialAmount = initialAmountVal,
                                        interestRateEa = rateVal,
                                        rateType = rateType,
                                        payoutFrequency = payoutFrequency,
                                        compounding = compounding,
                                        issueDate = issueDate,
                                        termDays = termDays,
                                        actionAtMaturity = actionAtMaturity,
                                        destinationAccountId = selectedDestinationAccountId,
                                        sourceAccountId = selectedSourceAccountId,
                                        allowsEarlyWithdrawal = allowsEarlyWithdrawal,
                                        hasAlert = hasAlert,
                                        alertDaysBefore = alertDaysBefore,
                                        customColorHex = selectedColorHex,
                                        iconName = selectedIconName
                                    )
                                    result.onSuccess { (createdAccount, createdCdt) ->
                                        onCdtCreated(createdAccount, createdCdt)
                                    }.onFailure { ex ->
                                        isSaving = false
                                        snackbarHostState.showSnackbar("Error al crear CDT: ${ex.message}")
                                    }
                                } else {
                                    // Fallback when repository is not passed directly
                                    val fallbackAccount = AccountEntity(
                                        id = java.util.UUID.randomUUID().toString(),
                                        userId = "user_default",
                                        name = cdtName,
                                        balance = initialAmountVal,
                                        availableBalance = 0.0,
                                        holdBalance = initialAmountVal,
                                        currency = selectedCurrency,
                                        type = "CDT",
                                        bankName = selectedBank,
                                        interestRateEa = rateVal,
                                        customColorHex = selectedColorHex,
                                        iconName = selectedIconName
                                    )
                                    val fallbackCdt = AccountCdtEntity(
                                        accountId = fallbackAccount.id,
                                        certificateNumber = certificateNumber,
                                        interestRateEa = rateVal,
                                        rateType = rateType,
                                        issueDate = issueDate.toString(),
                                        termDays = termDays,
                                        maturityDate = calculation.maturityDate.toString(),
                                        actionAtMaturity = actionAtMaturity,
                                        initialAmount = initialAmountVal,
                                        estimatedYield = calculation.estimatedYield
                                    )
                                    onCdtCreated(fallbackAccount, fallbackCdt)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("cdt_save_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                        enabled = !isSaving
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSaving) "Guardando CDT..." else "Guardar CDT",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ==========================================
            // CARD 1: INFORMACIÓN DEL CDT
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0F2FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = KashBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Información del CDT",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashNavy
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Field 1: Nombre del CDT * (with Marquee if overflow)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Nombre del CDT *",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEFF6FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = KashBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                BasicTextField(
                                    value = cdtName,
                                    onValueChange = { cdtName = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KashNavy
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .basicMarquee()
                                        .testTag("cdt_name_input"),
                                    decorationBox = { innerTextField ->
                                        if (cdtName.isEmpty()) {
                                            Text(
                                                text = "Ej. CDT Bancolombia 180 días",
                                                fontSize = 14.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Field 2: Entidad financiera * (with Marquee if overflow)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showBankPickerModal = true }
                            .testTag("cdt_bank_selector"),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Entidad financiera *",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEFF6FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = KashBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = selectedBank,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KashNavy,
                                        modifier = Modifier.basicMarquee()
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row: Código de la entidad * | Número de certificado
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Código de la entidad *
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Código de la entidad *",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEFF6FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Badge,
                                            contentDescription = null,
                                            tint = KashBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = selectedClearingCode,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KashNavy,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Número de certificado
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showCertNumberDialog = true }
                                .testTag("cdt_cert_number_btn"),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Número de certificado",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEFF6FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CreditCard,
                                            contentDescription = null,
                                            tint = KashBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = certificateNumber,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KashNavy,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Moneda *
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showCurrencyPickerModal = true }
                            .testTag("cdt_currency_btn"),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Moneda *",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEFF6FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AttachMoney,
                                            contentDescription = null,
                                            tint = KashBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = selectedCurrencyLabel,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KashNavy
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // CARD 2: CAPITAL INVERTIDO (Auto-Layout)
            // ==========================================
            CdtCapitalCard(
                initialAmount = calculation.initialAmount,
                currentValue = calculation.totalEstimatedValue,
                estimatedYield = calculation.estimatedYield,
                currencyCode = selectedCurrency,
                onRequestEditAmount = { activeKeypadTarget = CdtKeypadTarget.AMOUNT }
            )

            // ==========================================
            // CARD 3: RENTABILIDAD
            // ==========================================
            CdtProfitabilityCard(
                interestRateFormatted = rawInterestRate,
                rateType = rateType,
                onRateTypeSelected = { rateType = it },
                payoutFrequency = payoutFrequency,
                onPayoutFrequencySelected = { payoutFrequency = it },
                compounding = compounding,
                onCompoundingChanged = { compounding = it },
                onRequestEditRate = { activeKeypadTarget = CdtKeypadTarget.RATE }
            )

            // ==========================================
            // CARD 4: PLAZO (Reactive Maturity Date)
            // ==========================================
            CdtTermCard(
                issueDate = issueDate,
                termDays = termDays,
                maturityDate = calculation.maturityDate,
                onIssueDateClick = { showDatePicker = true },
                onTermDaysSelected = { termDays = it }
            )

            // ==========================================
            // CARD 5: ¿QUÉ DESEAS HACER AL VENCIMIENTO?
            // ==========================================
            CdtMaturityActionCard(
                actionAtMaturity = actionAtMaturity,
                onActionSelected = { actionAtMaturity = it },
                destinationAccountName = destinationAccountLabel,
                onSelectDestinationAccount = { showDestinationAccountModal = true },
                allowsEarlyWithdrawal = allowsEarlyWithdrawal,
                onEarlyWithdrawalChanged = { allowsEarlyWithdrawal = it }
            )

            // ==========================================
            // CARD 6: ALERTAS
            // ==========================================
            CdtAlertsCard(
                hasAlert = hasAlert,
                onHasAlertChanged = { hasAlert = it },
                alertDaysBefore = alertDaysBefore,
                onAlertDaysChanged = { alertDaysBefore = it }
            )

            // ==========================================
            // CARD 7: COLOR E ICONO
            // ==========================================
            CdtColorIconCard(
                selectedColorHex = selectedColorHex,
                onColorSelected = { selectedColorHex = it },
                selectedIconName = selectedIconName,
                onIconSelected = { selectedIconName = it }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ==========================================
    // IN-APP NUMERIC KEYBOARD MODAL
    // ==========================================
    activeKeypadTarget?.let { target ->
        val rawVal = when (target) {
            CdtKeypadTarget.AMOUNT -> rawInitialAmount
            CdtKeypadTarget.RATE -> rawInterestRate
        }
        CdtNumericKeyboardDialog(
            target = target,
            currentRawValue = rawVal,
            onValueChange = { newVal ->
                when (target) {
                    CdtKeypadTarget.AMOUNT -> rawInitialAmount = newVal
                    CdtKeypadTarget.RATE -> rawInterestRate = newVal
                }
            },
            onDismiss = { activeKeypadTarget = null }
        )
    }

    // ==========================================
    // DATE PICKER DIALOG
    // ==========================================
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = issueDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            issueDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Aceptar", color = KashBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Color.White)
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ==========================================
    // CERTIFICATE NUMBER EDIT DIALOG
    // ==========================================
    if (showCertNumberDialog) {
        var tempCertNumber by remember { mutableStateOf(certificateNumber) }
        AlertDialog(
            onDismissRequest = { showCertNumberDialog = false },
            title = {
                Text("Número de certificado", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = KashNavy)
            },
            text = {
                OutlinedTextField(
                    value = tempCertNumber,
                    onValueChange = { tempCertNumber = it },
                    label = { Text("Número de título valor") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KashBlue,
                        focusedLabelColor = KashBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        certificateNumber = tempCertNumber.trim().ifEmpty { "•••• 4827" }
                        showCertNumberDialog = false
                    }
                ) {
                    Text("Guardar", color = KashBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCertNumberDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color.White
        )
    }

    // ==========================================
    // BANK ENTITY SELECTION MODAL
    // ==========================================
    if (showBankPickerModal) {
        val displayBanks = remember(dbBanks) {
            if (dbBanks.isNotEmpty()) dbBanks else listOf(
                BankEntity(id = "b1", userId = "user_local", name = "Bancolombia", clearingCode = "001", countryCode = "CO", countryName = "Colombia"),
                BankEntity(id = "b2", userId = "user_local", name = "Davivienda", clearingCode = "051", countryCode = "CO", countryName = "Colombia"),
                BankEntity(id = "b3", userId = "user_local", name = "BBVA", clearingCode = "013", countryCode = "CO", countryName = "Colombia"),
                BankEntity(id = "b4", userId = "user_local", name = "Banco de Bogotá", clearingCode = "002", countryCode = "CO", countryName = "Colombia"),
                BankEntity(id = "b5", userId = "user_local", name = "Scotiabank Colpatria", clearingCode = "019", countryCode = "CO", countryName = "Colombia")
            )
        }

        AlertDialog(
            onDismissRequest = { showBankPickerModal = false },
            title = {
                Text("Selecciona una entidad financiera", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = KashNavy)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    displayBanks.forEach { bank ->
                        val isSelected = selectedBank == bank.name
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedBank = bank.name
                                    selectedClearingCode = "${bank.clearingCode} ${bank.name}"
                                    cdtName = "CDT ${bank.name} $termDays días"
                                    showBankPickerModal = false
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFFEFF6FF) else Color.White,
                            border = BorderStroke(1.dp, if (isSelected) KashBlue else Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE0F2FE)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = KashBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = bank.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KashNavy)
                                        Text(text = "Código ACH: ${bank.clearingCode} • ${bank.countryName}", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                }
                                if (isSelected) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = KashBlue, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            showBankPickerModal = false
                            showAddBankScreen = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+ Crear nueva entidad bancaria", color = KashBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBankPickerModal = false }) {
                    Text("Cerrar", color = Color.Gray)
                }
            },
            containerColor = Color.White
        )
    }

    // Modal: Add Bank Entity
    if (showAddBankScreen) {
        AddBankEntityScreen(
            preloadedBanks = dbBanks,
            financialRepository = financialRepository,
            userId = "user_default",
            onDismiss = { showAddBankScreen = false },
            onBankCreated = { createdBank ->
                selectedBank = createdBank.name
                selectedClearingCode = "${createdBank.clearingCode} ${createdBank.name}"
                cdtName = "CDT ${createdBank.name} $termDays días"
                showAddBankScreen = false
            }
        )
    }

    // ==========================================
    // CURRENCY PICKER MODAL & CUSTOM CURRENCY
    // ==========================================
    if (showCurrencyPickerModal) {
        val displayCurrencies = remember(dbCurrencies) {
            if (dbCurrencies.isNotEmpty()) dbCurrencies else listOf(
                CurrencyEntity(id = "cur_cop", code = "COP", name = "Peso colombiano", symbol = "$", manualExchangeRate = 1.0),
                CurrencyEntity(id = "cur_usd", code = "USD", name = "Dólar estadounidense", symbol = "$", manualExchangeRate = 4150.0),
                CurrencyEntity(id = "cur_eur", code = "EUR", name = "Euro", symbol = "€", manualExchangeRate = 4500.0)
            )
        }

        AlertDialog(
            onDismissRequest = { showCurrencyPickerModal = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Selecciona la moneda", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KashNavy)
                    IconButton(onClick = { showCurrencyPickerModal = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        displayCurrencies.forEach { curr ->
                            val isSel = selectedCurrency == curr.code
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedCurrency = curr.code
                                        selectedCurrencyLabel = "${curr.code} - ${curr.name}"
                                        showCurrencyPickerModal = false
                                    },
                                color = if (isSel) Color(0xFFEFF6FF) else Color.White,
                                border = BorderStroke(1.dp, if (isSel) KashBlue else Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = curr.symbol, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KashBlue)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "${curr.code} - ${curr.name}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = KashNavy)
                                        if (curr.manualExchangeRate > 1.0) {
                                            Text(
                                                text = "TRM: $ ${String.format(java.util.Locale.GERMANY, "%,.2f", curr.manualExchangeRate)} COP",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (financialRepository != null) {
                        OutlinedButton(
                            onClick = {
                                showCurrencyPickerModal = false
                                showAddCurrencyModal = true
                            },
                            modifier = Modifier.fillMaxWidth().testTag("cdt_add_custom_currency_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = KashBlue)
                        ) {
                            Text("+ Nueva divisa personalizada", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = Color.White
        )
    }

    if (showAddCurrencyModal && financialRepository != null) {
        AddCurrencyModal(
            financialRepository = financialRepository,
            userId = "user_default",
            onCurrencyCreated = { newCurr ->
                selectedCurrency = newCurr.code
                selectedCurrencyLabel = "${newCurr.code} - ${newCurr.name}"
                showAddCurrencyModal = false
            },
            onDismiss = { showAddCurrencyModal = false }
        )
    }

    // ==========================================
    // DESTINATION ACCOUNT MODAL
    // ==========================================
    if (showDestinationAccountModal) {
        val nonCdtAccounts = remember(dbAccounts) {
            dbAccounts.filter { it.type != "CDT" }
        }

        AlertDialog(
            onDismissRequest = { showDestinationAccountModal = false },
            title = {
                Text("Selecciona cuenta de destino", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = KashNavy)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (nonCdtAccounts.isEmpty()) {
                        Text(
                            text = "No tienes cuentas creadas para transferir al vencimiento.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        nonCdtAccounts.forEach { acc ->
                            val isSelected = selectedDestinationAccountId == acc.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedDestinationAccountId = acc.id
                                        showDestinationAccountModal = false
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFFEFF6FF) else Color.White,
                                border = BorderStroke(1.dp, if (isSelected) KashBlue else Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = acc.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KashNavy)
                                        Text(
                                            text = "${acc.bankName} • Saldo: $ ${String.format(Locale.GERMANY, "%,.0f", acc.balance)}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = KashBlue, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDestinationAccountModal = false }) {
                    Text("Cerrar", color = Color.Gray)
                }
            },
            containerColor = Color.White
        )
    }
}
