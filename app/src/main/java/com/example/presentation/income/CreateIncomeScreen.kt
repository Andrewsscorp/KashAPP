package com.example.presentation.income

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.domain.repository.FinancialRepository
import com.example.presentation.account.CreateAccountScreen
import com.example.presentation.account.components.CurrencyPickerModal
import com.example.presentation.category.CategoryCreationDialog
import com.example.presentation.category.resolveCategoryIcon
import com.example.presentation.expense.CurrencyDotVisualTransformation
import com.example.presentation.expense.ExpenseCategory
import com.example.presentation.expense.formatMoneyDisplay
import com.example.presentation.income.components.IncomeDestinationAccount
import com.example.presentation.income.components.IncomeSourceSelector
import com.example.presentation.income.dialogs.IncomeCalculatorSheet
import com.example.presentation.income.model.DefaultIncomeCategories
import com.example.presentation.income.model.IncomeFormState
import com.example.presentation.income.model.buildIncomeDisplayCategories
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/**
 * Enterprise "Nuevo ingreso" Master Screen (~240 lines).
 * Coordinates amount header, account destination, source/category selection, and optional details.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateIncomeScreen(
    onDismiss: () -> Unit,
    onSaveIncome: (
        amount: Double,
        currency: String,
        category: String,
        source: String,
        account: String,
        date: String,
        description: String,
        isRecurring: Boolean,
        frequency: String,
        saveAsTemplate: Boolean,
        tags: List<String>,
        receiptUri: Uri?,
        idempotencyKey: String
    ) -> Unit,
    modifier: Modifier = Modifier,
    initialAmount: Double? = null,
    financialRepository: FinancialRepository? = null,
    userId: String = "user_default"
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val amountFocusRequester = remember { FocusRequester() }

    BackHandler { onDismiss() }

    val prefs = remember { context.getSharedPreferences("kashapp_preferences", Context.MODE_PRIVATE) }
    val lastUsedCurrency = remember { prefs.getString("last_used_currency", "COP") ?: "COP" }

    var formState by remember {
        mutableStateOf(
            IncomeFormState(
                rawAmountInput = if (initialAmount != null && initialAmount > 0) initialAmount.toString() else "",
                currencyCode = lastUsedCurrency
            )
        )
    }

    val roomAccountsState = financialRepository?.getAccounts(userId)?.collectAsState(initial = emptyList())
    val roomAccounts = roomAccountsState?.value ?: emptyList()
    var selectedAccountEntity by remember { mutableStateOf<AccountEntity?>(null) }

    LaunchedEffect(roomAccounts) {
        if (selectedAccountEntity == null && roomAccounts.isNotEmpty()) {
            selectedAccountEntity = roomAccounts.first()
        }
    }

    val roomCategoriesState = financialRepository?.getCategories()?.collectAsState(initial = emptyList())
    val displayCategories = remember(roomCategoriesState?.value) {
        buildIncomeDisplayCategories(roomCategoriesState?.value ?: emptyList())
    }
    var selectedCategory by remember {
        mutableStateOf(displayCategories.firstOrNull() ?: DefaultIncomeCategories[0])
    }

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
    var showSuccessToast by remember { mutableStateOf(false) }
    var successToastMessage by remember { mutableStateOf("") }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                val path = uri.path.orEmpty()
                val isPdf = path.endsWith(".pdf", ignoreCase = true) || uri.toString().contains("pdf", ignoreCase = true)
                formState = formState.copy(
                    receiptUri = uri,
                    isPdfDocument = isPdf,
                    receiptFileName = if (isPdf) "Comprobante.pdf" else "Comprobante.jpg"
                )
            }
        }
    )

    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = formState.selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    val spanishDateFormatter = remember { DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es", "ES")) }
    val dateDisplayString = formState.selectedDate.format(spanishDateFormatter)

    val onPerformSaveIncome: () -> Unit = {
        val amount = formState.amountDouble
        if (!formState.isSubmitting && amount > 0.0) {
            formState = formState.copy(isSubmitting = true)
            val finalConcept = if (formState.sourceText.isNotBlank()) formState.sourceText else selectedCategory.name
            val targetAccount = selectedAccountEntity ?: roomAccounts.firstOrNull()

            coroutineScope.launch {
                try {
                    if (targetAccount != null && financialRepository != null) {
                        val result = financialRepository.recordIncome(
                            userId = userId,
                            accountId = targetAccount.id,
                            accountName = targetAccount.name,
                            categoryId = selectedCategory.id,
                            categoryName = selectedCategory.name,
                            sourceId = null,
                            sourceName = finalConcept,
                            amount = amount,
                            currency = formState.currencyCode,
                            dateFormatted = formState.selectedDate.toString(),
                            description = finalConcept,
                            receiptUrl = formState.receiptUri?.toString(),
                            isRecurring = formState.isRecurring,
                            recurrenceFrequency = formState.recurrenceFrequency,
                            saveAsTemplate = formState.isSavedAsFrequent,
                            tags = formState.tagsList
                        )

                        if (result.isSuccess) {
                            onSaveIncome(
                                amount, formState.currencyCode, selectedCategory.name, finalConcept,
                                targetAccount.name, dateDisplayString, finalConcept, formState.isRecurring,
                                formState.recurrenceFrequency, formState.isSavedAsFrequent, formState.tagsList,
                                formState.receiptUri, UUID.randomUUID().toString()
                            )
                            successToastMessage = "Ingreso de ${formatMoneyDisplay(amount, formState.currencyCode)} registrado con éxito"
                            showSuccessToast = true
                            delay(800)
                            onDismiss()
                        }
                    }
                } finally {
                    formState = formState.copy(isSubmitting = false)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .background(Color(0xFFF8FAFC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            // Header Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF064E3B), Color(0xFF022C22))
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x26FFFFFF))
                                .testTag("back_button_income")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (formState.isAmountValid && !formState.isSubmitting) Color(0xFF10B981) else Color(0x33FFFFFF),
                            shadowElevation = if (formState.isAmountValid) 4.dp else 0.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(enabled = formState.isAmountValid && !formState.isSubmitting) { onPerformSaveIncome() }
                                .testTag("top_bar_save_income_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = if (formState.isAmountValid) Color.White else Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                Text(
                                    text = if (formState.isSubmitting) "Guardando..." else "Guardar",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (formState.isAmountValid) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Text(text = "Nuevo ingreso", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.5).sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Registra salarios, honorarios o rendimientos y aumenta tu patrimonio.", fontSize = 13.sp, color = Color(0xFFA7F3D0))
                }
            }

            // Body Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Amount Header Card
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Importe del ingreso", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFEFF6FF))
                                        .clickable { showCalculatorSheet = true }
                                        .testTag("open_calculator_income_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Calculate, contentDescription = "Calculadora", tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFEFF6FF),
                                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                    modifier = Modifier.clickable { showCurrencySheet = true }.testTag("currency_selector_income_badge")
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = formState.currencyCode, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                                .clickable {
                                    amountFocusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            color = Color(0xFFF8FAFC)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(text = "$", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                Spacer(modifier = Modifier.width(8.dp))
                                BasicTextField(
                                    value = formState.rawAmountInput,
                                    onValueChange = { input ->
                                        val filtered = input.filter { it.isDigit() || it == '.' }
                                        if (filtered.count { it == '.' } <= 1 && filtered.length <= 15) {
                                            formState = formState.copy(rawAmountInput = filtered)
                                        }
                                    },
                                    visualTransformation = CurrencyDotVisualTransformation(),
                                    textStyle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)),
                                    cursorBrush = SolidColor(Color(0xFF10B981)),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        if (formState.rawAmountInput.isEmpty()) {
                                            Text(text = "0", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                        }
                                        innerTextField()
                                    },
                                    modifier = Modifier.weight(1f).focusRequester(amountFocusRequester).testTag("importe_income_input_field")
                                )
                                if (formState.rawAmountInput.isNotEmpty()) {
                                    IconButton(onClick = { formState = formState.copy(rawAmountInput = "") }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        if (formState.amountDouble > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Equivalente: ${formatMoneyDisplay(formState.amountDouble, formState.currencyCode)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF10B981))
                        }
                    }
                }

                // 2. Destination Account & Impact
                IncomeDestinationAccount(
                    selectedAccount = selectedAccountEntity,
                    allAccounts = roomAccounts,
                    amountDouble = formState.amountDouble,
                    currencyCode = formState.currencyCode,
                    onSelectAccount = { selectedAccountEntity = it },
                    onCreateAccountClick = { type -> createAccountTypeCode = type; showCreateAccountModal = true },
                    onQuickSaveClick = onPerformSaveIncome
                )

                // 3. Source & Category Selector
                IncomeSourceSelector(
                    selectedCategory = selectedCategory,
                    allCategories = displayCategories,
                    sourceText = formState.sourceText,
                    onSourceChange = { formState = formState.copy(sourceText = it) },
                    onCategorySelected = { selectedCategory = it },
                    onOpenCreateCategory = { showNewCategoryDialog = true }
                )

                // 4. Date & Recurrence
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    modifier = Modifier.fillMaxWidth().testTag("fecha_income_selector_card")
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        com.example.ui.components.IPhoneAdaptiveDatePickerField(
                            value = formState.selectedDate.toString(),
                            onValueChange = { inputStr ->
                                try {
                                    if (inputStr.length >= 10) {
                                        formState = formState.copy(selectedDate = LocalDate.parse(inputStr.take(10)))
                                    }
                                } catch (_: Exception) {}
                            },
                            label = "Fecha del ingreso",
                            title = "Fecha del Ingreso",
                            placeholder = "AAAA-MM-DD",
                            isDarkTheme = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFDCFCE7)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Ingreso recurrente", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Text(text = "Se repetirá periódicamente según tu preferencia", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                        Switch(
                            checked = formState.isRecurring,
                            onCheckedChange = { formState = formState.copy(isRecurring = it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF10B981))
                        )
                    }
                }

                // 5. Optional Details Section
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { formState = formState.copy(showMoreDetails = !formState.showMoreDetails) }.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Más detalles (opcional)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    Icon(imageVector = if (formState.showMoreDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                }

                AnimatedVisibility(visible = formState.showMoreDetails, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(text = "Nota adicional", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = formState.additionalNotes,
                                    onValueChange = { formState = formState.copy(additionalNotes = it) },
                                    placeholder = { Text("Añade contexto o cualquier detalle...", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFDCFCE7)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.StarOutline, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Guardar como ingreso frecuente", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    Text(text = "Salvar para reusar con 1 toque", fontSize = 11.sp, color = Color(0xFF64748B))
                                }
                                Switch(
                                    checked = formState.isSavedAsFrequent,
                                    onCheckedChange = { formState = formState.copy(isSavedAsFrequent = it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF10B981))
                                )
                            }
                        }

                        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocalOffer, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = "Etiquetas", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                                    }
                                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFECFDF5), border = BorderStroke(1.dp, Color(0xFFA7F3D0)), modifier = Modifier.clickable { showTagInputDialog = true }) {
                                        Text(text = "+ Añadir", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                                    }
                                }
                                if (formState.tagsList.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        formState.tagsList.forEach { tag ->
                                            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFECFDF5), border = BorderStroke(1.dp, Color(0xFF6EE7B7))) {
                                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Text("#$tag", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(14.dp).clickable { formState = formState.copy(tagsList = formState.tagsList - tag) })
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (formState.receiptUri != null) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.5.dp, if (formState.receiptUri != null) Color(0xFF86EFAC) else Color(0xFFCBD5E1)),
                                modifier = Modifier.fillMaxWidth().clickable { documentPickerLauncher.launch(arrayOf("image/*", "application/pdf")) }
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = if (formState.receiptUri != null) Icons.Default.CheckCircle else Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = formState.receiptFileName ?: "Agregar comprobante", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        Text(text = if (formState.receiptUri != null) "Toca para cambiar comprobante" else "Foto o PDF del volante de pago", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = onPerformSaveIncome,
                        enabled = formState.isAmountValid && !formState.isSubmitting,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), disabledContainerColor = Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 440.dp)
                            .height(52.dp)
                            .testTag("registrar_ingreso_btn")
                    ) {
                        if (formState.isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Guardando...", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color.White)
                                Text(text = "Registrar ingreso", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Floating Success Toast
        AnimatedVisibility(visible = showSuccessToast, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)) {
            Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF0F172A), shadowElevation = 10.dp) {
                Row(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    Text(text = successToastMessage, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    // Modal Dialogs
    IncomeCalculatorSheet(
        visible = showCalculatorSheet,
        initialValue = formState.rawAmountInput,
        currencyCode = formState.currencyCode,
        onConfirm = { formState = formState.copy(rawAmountInput = it) },
        onDismiss = { showCalculatorSheet = false }
    )

    if (showCurrencySheet) {
        CurrencyPickerModal(
            selectedCurrencyCode = formState.currencyCode,
            onCurrencySelected = { code ->
                formState = formState.copy(currencyCode = code)
                prefs.edit().putString("last_used_currency", code).apply()
                showCurrencySheet = false
            },
            onDismiss = { showCurrencySheet = false },
            financialRepository = financialRepository,
            userId = userId
        )
    }

    if (showDatePicker) {
        com.example.ui.components.AdaptiveWheelDatePickerDialog(
            initialDate = formState.selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { newDate ->
                formState = formState.copy(selectedDate = newDate)
                showDatePicker = false
            },
            title = "Fecha del Ingreso"
        )
    }

    if (showTagInputDialog) {
        AlertDialog(
            onDismissRequest = { showTagInputDialog = false },
            title = { Text("Nueva Etiqueta", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    placeholder = { Text("Ej. Sueldo, Freelance, Bono...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newTagInput.trim().replace("#", "")
                        if (clean.isNotBlank()) {
                            if (!formState.tagsList.contains(clean)) {
                                formState = formState.copy(tagsList = formState.tagsList + clean)
                            }
                            financialRepository?.let { repo ->
                                coroutineScope.launch { repo.insertTag(clean) }
                            }
                        }
                        newTagInput = ""
                        showTagInputDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) { Text("Agregar") }
            },
            dismissButton = { TextButton(onClick = { showTagInputDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showNewCategoryDialog) {
        CategoryCreationDialog(
            categoryType = "INCOME",
            onDismiss = { showNewCategoryDialog = false },
            onCategoryCreated = { name, iconKey, colorHex ->
                val icon = resolveCategoryIcon(iconKey, name)
                val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (_: Exception) { Color(0xFF10B981) }
                financialRepository?.let { repo ->
                    coroutineScope.launch {
                        val created = repo.createCategory(name = name, colorHex = colorHex, icon = iconKey, userId = userId, type = "INCOME")
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
            onAccountCreated = { newAcc, _ ->
                selectedAccountEntity = newAcc
                showCreateAccountModal = false
            },
            onDismiss = { showCreateAccountModal = false },
            financialRepository = financialRepository
        )
    }
}
