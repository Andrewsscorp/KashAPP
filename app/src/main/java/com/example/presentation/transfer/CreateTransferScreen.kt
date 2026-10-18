package com.example.presentation.transfer

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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.AccountEntity
import com.example.domain.repository.FinancialRepository
import com.example.presentation.account.CreateAccountScreen
import com.example.presentation.transfer.components.TransferAccountPickerModal
import com.example.presentation.transfer.components.TransferAccountsBridge
import com.example.presentation.transfer.components.TransferGmfBanner
import com.example.presentation.transfer.dialogs.DiscardTransferDialog
import com.example.presentation.transfer.dialogs.OverdraftBlockedIphoneDialog
import com.example.presentation.transfer.dialogs.TransferAddTagDialog
import com.example.presentation.transfer.dialogs.TransferCurrencyPickerDialog
import com.example.presentation.transfer.dialogs.TransferQuickRoutesModal
import com.example.presentation.transfer.dialogs.TransferSuccessIphonePopup
import com.example.presentation.transfer.dialogs.TransferSuccessVoucherDialog
import com.example.presentation.transfer.model.QuickTransferRoute
import com.example.presentation.transfer.model.TransferFormState
import com.example.presentation.transfer.model.TransferSuccessSummary
import com.example.presentation.transfer.model.formatTransferMoney
import com.example.presentation.transfer.model.formatTransferNumberOnly
import com.example.core.ui.adaptive.rememberKashAdaptiveInfo
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

// Backward compatibility exports
typealias QuickTransferRoute = QuickTransferRoute
typealias TransferSuccessSummary = TransferSuccessSummary

/**
 * Screen for creating an internal transfer between user accounts ("Nueva transferencia").
 * Refactored to act strictly as the coordinator of layout, state, and modal lifecycles (~220 lines).
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun CreateTransferScreen(
    onNavigateBack: () -> Unit,
    financialRepository: FinancialRepository?,
    userId: String = "user_default",
    onTransferSuccess: (
        sourceAccountId: String,
        sourceAccountName: String,
        destinationAccountId: String,
        destinationAccountName: String,
        amount: Double,
        currency: String,
        dateFormatted: String,
        concept: String?,
        note: String?,
        receiptUrl: String?,
        saveAsTemplate: Boolean,
        tags: List<String>,
        isDestinationLiability: Boolean
    ) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val adaptive = rememberKashAdaptiveInfo()

    // Live Accounts from Local Room DB
    val accountsList by (financialRepository?.getAccounts(userId)?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList<AccountEntity>()) })

    var formState by remember { mutableStateOf(TransferFormState()) }

    // Dialog & Flow States
    var showSuccessSummaryPopup by remember { mutableStateOf(false) }
    var transferSummaryData by remember { mutableStateOf<TransferSuccessSummary?>(null) }
    var showOverdraftBlockedPopup by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showSourcePicker by remember { mutableStateOf(false) }
    var showDestinationPicker by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showQuickRoutesDialog by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var showNewAccountDialog by remember { mutableStateOf(false) }

    // Receipt File Picker Launcher
    val receiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val type = context.contentResolver.getType(uri) ?: ""
            val isAllowed = type.startsWith("image/", ignoreCase = true) ||
                type.equals("application/pdf", ignoreCase = true) ||
                uri.toString().contains("pdf", ignoreCase = true)
            if (isAllowed) {
                formState = formState.copy(selectedReceiptUri = uri, errorMessage = null)
            } else {
                formState = formState.copy(errorMessage = "Formato no permitido. Solo se admite imagen o PDF.")
            }
        }
    }

    // Default Account Preselection
    LaunchedEffect(accountsList) {
        if (accountsList.isNotEmpty()) {
            if (formState.selectedSourceAccount == null) {
                val defaultSource = accountsList.firstOrNull { it.type == "CHECKING" || it.name.contains("Corriente", ignoreCase = true) || it.name.contains("Principal", ignoreCase = true) }
                    ?: accountsList.firstOrNull()
                formState = formState.copy(selectedSourceAccount = defaultSource)
            }
            if (formState.selectedDestinationAccount == null) {
                val defaultDest = accountsList.firstOrNull { it.id != formState.selectedSourceAccount?.id && (it.type == "SAVINGS" || it.name.contains("Ahorro", ignoreCase = true) || it.name.contains("Metas", ignoreCase = true)) }
                    ?: accountsList.firstOrNull { it.id != formState.selectedSourceAccount?.id }
                formState = formState.copy(selectedDestinationAccount = defaultDest)
            }
        }
    }

    val spanishDateFormatter = remember { DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es", "ES")) }
    val shortDateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "ES")) }

    val handleBackAttempt = {
        if (formState.numericAmount > 0 || formState.noteText.isNotBlank() || formState.conceptText.isNotBlank()) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }
    BackHandler { handleBackAttempt() }

    val quickRoutes = listOf(
        QuickTransferRoute("Corriente → Ahorros", "Cuenta Corriente", "Cuenta de Ahorros", "Ahorro mensual"),
        QuickTransferRoute("Ahorros → Tarjeta", "Cuenta de Ahorros", "Tarjeta de Crédito", "Pago de tarjeta"),
        QuickTransferRoute("Billetera → Ahorros", "Efectivo", "Cuenta de Ahorros", "Consignación"),
        QuickTransferRoute("Corriente → Inversiones", "Cuenta Corriente", "Fondo de Inversión", "Inversión")
    )

    val onPerformTransfer: () -> Unit = {
        val src = formState.selectedSourceAccount
        val dst = formState.selectedDestinationAccount
        val amount = formState.numericAmount
        if (src == null || dst == null) {
            formState = formState.copy(errorMessage = "Debes seleccionar cuenta origen y destino")
        } else if (src.id == dst.id) {
            formState = formState.copy(errorMessage = "La cuenta de origen y destino deben ser distintas")
        } else {
            val availBalance = src.availableBalance ?: src.balance
            if (!src.hasOverdraft && (availBalance < formState.totalDeduction)) {
                showOverdraftBlockedPopup = true
            } else if (amount > 0.0 && !formState.isSubmitting) {
                val cleanConcept = formState.conceptText.trim().ifBlank { null }
                val cleanNote = formState.noteText.trim().ifBlank { null }
                val cleanTags = formState.tagsList.map { it.trim().lowercase() }.filter { it.isNotBlank() }

                formState = formState.copy(isSubmitting = true, errorMessage = null)
                scope.launch {
                    try {
                        if (financialRepository != null) {
                            val result = financialRepository.recordTransfer(
                                userId = userId,
                                sourceAccountId = src.id,
                                sourceAccountName = src.name,
                                destinationAccountId = dst.id,
                                destinationAccountName = dst.name,
                                amount = amount,
                                currency = formState.selectedCurrency,
                                dateFormatted = formState.selectedDate.toString(),
                                concept = cleanConcept,
                                note = cleanNote,
                                receiptUrl = formState.selectedReceiptUri?.toString(),
                                saveAsTemplate = formState.saveAsTemplate,
                                tags = cleanTags
                            )

                            if (result.isSuccess) {
                                val nowTime = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a", Locale("es", "CO")))
                                transferSummaryData = TransferSuccessSummary(
                                    transactionId = result.getOrNull()?.first?.id ?: "TRX-${UUID.randomUUID().toString().take(8).uppercase()}",
                                    sourceAccountName = src.name,
                                    sourceAccountBalanceAfter = (src.balance - formState.totalDeduction).coerceAtLeast(0.0),
                                    destinationAccountName = dst.name,
                                    destinationAccountBalanceAfter = dst.balance + amount,
                                    amount = amount,
                                    currency = formState.selectedCurrency,
                                    dateFormatted = formState.selectedDate.format(shortDateFormatter),
                                    timeFormatted = nowTime,
                                    concept = cleanConcept ?: "Transferencia entre cuentas",
                                    gmfAmount = formState.gmfAmount
                                )
                                showSuccessSummaryPopup = true
                            } else {
                                formState = formState.copy(errorMessage = result.exceptionOrNull()?.message ?: "Error al registrar la transferencia")
                            }
                        } else {
                            val isLiability = dst.type == "CREDIT" || dst.type == "LOAN" || dst.name.contains("Tarjeta", ignoreCase = true)
                            onTransferSuccess(
                                src.id, src.name, dst.id, dst.name, amount, formState.selectedCurrency,
                                formState.selectedDate.toString(), cleanConcept, cleanNote,
                                formState.selectedReceiptUri?.toString(), formState.saveAsTemplate, cleanTags, isLiability
                            )
                        }
                    } catch (e: Exception) {
                        formState = formState.copy(errorMessage = e.message ?: "Error al procesar transferencia")
                    } finally {
                        formState = formState.copy(isSubmitting = false)
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = adaptive.maxContentWidth)
        ) {
            // TOP HEADER CON GRADIENTE PREMIUM
            Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF0284C7)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xFF0369A1), Color(0xFF0284C7), Color(0xFF0EA5E9))))
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
                                onClick = { handleBackAttempt() },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .testTag("transfer_back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White, modifier = Modifier.size(20.dp))
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (formState.isFormValid) Color(0xFF0284C7) else Color.White.copy(alpha = 0.2f),
                                shadowElevation = if (formState.isFormValid) 4.dp else 0.dp,
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 44.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable(enabled = formState.isFormValid) { onPerformTransfer() }
                                    .testTag("top_bar_transfer_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = if (formState.isSubmitting) "Transfiriendo..." else "Transferir",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(text = "Nueva transferencia", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(text = "Mueve dinero entre tus cuentas con acreditación instantánea.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }

            // SCROLLABLE FORM BODY
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. AMOUNT CARD
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, if (formState.isOverdraft) Color(0xFFFCA5A5) else Color(0xFFE2E8F0), RoundedCornerShape(20.dp)),
                    color = Color.White,
                    shadowElevation = 1.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE0F2FE)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = "Importe de la transferencia", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF0F9FF),
                                border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                                modifier = Modifier.clickable { showCurrencyPicker = true }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = formState.selectedCurrency, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0284C7))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth().clickable { formState = formState.copy(isNumpadVisible = !formState.isNumpadVisible) }, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "$ ", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = if (formState.isOverdraft) Color(0xFFDC2626) else Color(0xFF0F172A))
                            Text(
                                text = if (formState.numericAmount == 0.0) "0.00" else formatTransferNumberOnly(formState.numericAmount),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (formState.isOverdraft) Color(0xFFDC2626) else Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 2. QUICK INCREMENTS & NUMERIC KEYPAD
                Surface(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp)),
                    color = Color.White,
                    shadowElevation = 1.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(100L to "$100", 500L to "$500", 1_000L to "$1,000").forEach { (inc, lbl) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF0F9FF))
                                        .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(8.dp))
                                        .clickable {
                                            val cur = formState.rawDigits.toLongOrNull() ?: 0L
                                            formState = formState.copy(rawDigits = (cur + inc).toString())
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = lbl, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0284C7))
                                    .clickable {
                                        val sBal = formState.sourceBalance
                                        if (sBal > 0) {
                                            formState = formState.copy(rawDigits = if (sBal % 1.0 == 0.0) sBal.toLong().toString() else sBal.toString())
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "Máx.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        if (formState.isNumpadVisible) {
                            val keypadKeys = listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9"),
                                listOf("00", "0", "⌫")
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                keypadKeys.forEach { row ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        row.forEach { key ->
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        when (key) {
                                                            "⌫" -> if (formState.rawDigits.isNotEmpty()) formState = formState.copy(rawDigits = formState.rawDigits.dropLast(1))
                                                            else -> if (formState.rawDigits.length < 12) formState = formState.copy(rawDigits = formState.rawDigits + key)
                                                        }
                                                    },
                                                color = if (key == "⌫") Color(0xFFF1F5F9) else Color(0xFFF8FAFC),
                                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    if (key == "⌫") {
                                                        Icon(Icons.Default.Backspace, contentDescription = "Borrar", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                                    } else {
                                                        Text(text = key, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. TRANSFER ACCOUNTS BRIDGE
                TransferAccountsBridge(
                    sourceAccount = formState.selectedSourceAccount,
                    destinationAccount = formState.selectedDestinationAccount,
                    numericAmount = formState.numericAmount,
                    selectedCurrency = formState.selectedCurrency,
                    newSourceBalance = formState.newSourceBalance,
                    newDestBalance = formState.newDestBalance,
                    isOverdraft = formState.isOverdraft,
                    onSourceClick = { showSourcePicker = true },
                    onDestinationClick = { showDestinationPicker = true },
                    onSwapClick = {
                        val temp = formState.selectedSourceAccount
                        formState = formState.copy(
                            selectedSourceAccount = formState.selectedDestinationAccount,
                            selectedDestinationAccount = temp
                        )
                    }
                )

                // 4. GMF BANNER & BREAKDOWN (4x1000)
                TransferGmfBanner(
                    amount = formState.numericAmount,
                    currency = formState.selectedCurrency,
                    applyGmf = formState.applyGmf,
                    onToggleGmf = { formState = formState.copy(applyGmf = it) }
                )

                // 5. FECHA CARD (iPhone Wheel Picker + Escritura Directa)
                Surface(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp)),
                    color = Color.White
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        com.example.ui.components.IPhoneAdaptiveDatePickerField(
                            value = formState.selectedDate.toString(),
                            onValueChange = { str ->
                                try {
                                    if (str.length >= 10) {
                                        formState = formState.copy(selectedDate = LocalDate.parse(str.take(10)))
                                    }
                                } catch (_: Exception) {}
                            },
                            label = "Fecha de transferencia",
                            title = "Fecha de Transferencia",
                            placeholder = "AAAA-MM-DD",
                            isDarkTheme = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 6. CONCEPTO CARD
                Surface(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp)), color = Color.White) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE0F2FE)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "Concepto", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "(Opcional)", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }
                                Text(text = "Identificador para la transferencia.", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF8FAFC), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                            BasicTextField(
                                value = formState.conceptText,
                                onValueChange = { formState = formState.copy(conceptText = it) },
                                textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A)),
                                cursorBrush = SolidColor(Color(0xFF0284C7)),
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                decorationBox = { inner ->
                                    if (formState.conceptText.isEmpty()) Text("Ej. Ahorro mensual o pago cuota", fontSize = 13.sp, color = Color(0xFF94A3B8))
                                    inner()
                                }
                            )
                        }
                    }
                }

                // 7. MÁS DETALLES (OPCIONAL) COLLAPSIBLE ACCORDION
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth().clickable { formState = formState.copy(isMoreDetailsExpanded = !formState.isMoreDetailsExpanded) }
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Más detalles (opcional)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                        }
                        Icon(imageVector = if (formState.isMoreDetailsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                    }
                }

                AnimatedVisibility(visible = formState.isMoreDetailsExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Nota adicional
                        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(text = "Nota adicional", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF8FAFC), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                                    BasicTextField(
                                        value = formState.noteText,
                                        onValueChange = { formState = formState.copy(noteText = it) },
                                        textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF0F172A)),
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        decorationBox = { inner ->
                                            if (formState.noteText.isEmpty()) Text("Detalles útiles sobre la transferencia...", fontSize = 13.sp, color = Color(0xFF94A3B8))
                                            inner()
                                        }
                                    )
                                }
                            }
                        }

                        // Rutas rápidas
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth().clickable { showQuickRoutesDialog = true }
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE0F2FE)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Rutas rápidas frecuentes", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                    Text(text = "Transfiere con un solo toque desde tus favoritos.", fontSize = 11.sp, color = Color(0xFF64748B))
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                            }
                        }

                        // Guardar como plantilla
                        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE0F2FE)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Guardar como plantilla frecuente", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    Text(text = "Aparecerá en tus accesos directos", fontSize = 11.sp, color = Color(0xFF64748B))
                                }
                                Switch(
                                    checked = formState.saveAsTemplate,
                                    onCheckedChange = { formState = formState.copy(saveAsTemplate = it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0284C7))
                                )
                            }
                        }

                        // Etiquetas
                        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "Etiquetas", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF0F9FF), border = BorderStroke(1.dp, Color(0xFFBAE6FD)), modifier = Modifier.clickable { showAddTagDialog = true }) {
                                        Text(text = "+ Añadir", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                                if (formState.tagsList.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        formState.tagsList.forEach { tag ->
                                            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFE0F2FE)) {
                                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = "#$tag", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.SemiBold)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(12.dp).clickable { formState = formState.copy(tagsList = formState.tagsList - tag) })
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Comprobante
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth().clickable { receiptPickerLauncher.launch(arrayOf("image/*", "application/pdf")) }
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE0F2FE)), contentAlignment = Alignment.Center) {
                                    Icon(if (formState.selectedReceiptUri != null) Icons.Default.CheckCircle else Icons.Default.PhotoCamera, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = if (formState.selectedReceiptUri != null) "Comprobante adjuntado" else "Adjuntar comprobante", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    Text(text = if (formState.selectedReceiptUri != null) "Toca para reemplazar archivo" else "Foto o PDF del soporte bancario", fontSize = 11.sp, color = Color(0xFF64748B))
                                }
                            }
                        }
                    }
                }

                // Error Message Banner
                if (formState.errorMessage != null) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFEF2F2), border = BorderStroke(1.dp, Color(0xFFFECACA)), modifier = Modifier.fillMaxWidth()) {
                        Text(text = formState.errorMessage ?: "", fontSize = 12.sp, color = Color(0xFFDC2626), modifier = Modifier.padding(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 8. ACTION BUTTON "TRANSFERIR AHORA"
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = onPerformTransfer,
                        enabled = formState.isFormValid,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7), disabledContainerColor = Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 440.dp)
                            .height(52.dp)
                            .testTag("confirm_transfer_btn")
                    ) {
                        if (formState.isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Transfiriendo...", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        } else {
                            Text(
                                text = if (formState.numericAmount > 0) "Transferir ${formatTransferMoney(formState.numericAmount, formState.selectedCurrency)}" else "Transferir ahora",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // MODAL DIALOGS
    if (showSourcePicker) {
        TransferAccountPickerModal(
            title = "Cuenta de origen",
            accounts = accountsList,
            selectedAccount = formState.selectedSourceAccount,
            disabledAccountId = formState.selectedDestinationAccount?.id,
            onSelectAccount = {
                formState = formState.copy(selectedSourceAccount = it)
                showSourcePicker = false
            },
            onCreateAccountClick = {
                showSourcePicker = false
                showNewAccountDialog = true
            },
            onDismiss = { showSourcePicker = false }
        )
    }

    if (showDestinationPicker) {
        TransferAccountPickerModal(
            title = "Cuenta de destino",
            accounts = accountsList,
            selectedAccount = formState.selectedDestinationAccount,
            disabledAccountId = formState.selectedSourceAccount?.id,
            onSelectAccount = {
                formState = formState.copy(selectedDestinationAccount = it)
                showDestinationPicker = false
            },
            onCreateAccountClick = {
                showDestinationPicker = false
                showNewAccountDialog = true
            },
            onDismiss = { showDestinationPicker = false }
        )
    }

    if (showQuickRoutesDialog) {
        TransferQuickRoutesModal(
            routes = quickRoutes,
            accounts = accountsList,
            onSelectRoute = { route ->
                val src = accountsList.firstOrNull { it.name.contains(route.sourceName, ignoreCase = true) }
                val dst = accountsList.firstOrNull { it.name.contains(route.destinationName, ignoreCase = true) }
                formState = formState.copy(
                    selectedSourceAccount = src ?: formState.selectedSourceAccount,
                    selectedDestinationAccount = dst ?: formState.selectedDestinationAccount,
                    conceptText = route.defaultConcept
                )
                showQuickRoutesDialog = false
            },
            onDismiss = { showQuickRoutesDialog = false }
        )
    }

    if (showSuccessSummaryPopup && transferSummaryData != null) {
        TransferSuccessVoucherDialog(
            summary = transferSummaryData!!,
            onDismiss = {
                showSuccessSummaryPopup = false
                onNavigateBack()
            },
            onShare = { /* Sharing voucher intent */ }
        )
    }

    if (showOverdraftBlockedPopup) {
        OverdraftBlockedIphoneDialog(
            accountName = formState.selectedSourceAccount?.name ?: "",
            availableBalance = formState.selectedSourceAccount?.availableBalance ?: formState.sourceBalance,
            attemptedAmount = formState.totalDeduction,
            currency = formState.selectedCurrency,
            onDismiss = { showOverdraftBlockedPopup = false }
        )
    }

    if (showDiscardDialog) {
        DiscardTransferDialog(
            onConfirmDiscard = {
                showDiscardDialog = false
                onNavigateBack()
            },
            onDismiss = { showDiscardDialog = false }
        )
    }

    if (showCurrencyPicker) {
        TransferCurrencyPickerDialog(
            selectedCurrency = formState.selectedCurrency,
            onCurrencySelected = { code ->
                formState = formState.copy(selectedCurrency = code)
                showCurrencyPicker = false
            },
            onDismiss = { showCurrencyPicker = false }
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
            title = "Fecha de Transferencia"
        )
    }

    if (showAddTagDialog) {
        TransferAddTagDialog(
            onAddTag = { tag ->
                val clean = tag.trim().replace("#", "")
                if (clean.isNotBlank() && !formState.tagsList.contains(clean)) {
                    formState = formState.copy(tagsList = formState.tagsList + clean)
                    scope.launch { financialRepository?.insertTag(clean) }
                }
                showAddTagDialog = false
            },
            onDismiss = { showAddTagDialog = false }
        )
    }

    if (showNewAccountDialog) {
        CreateAccountScreen(
            onAccountCreated = { newAcc, _ ->
                formState = formState.copy(selectedDestinationAccount = newAcc)
                showNewAccountDialog = false
            },
            onDismiss = { showNewAccountDialog = false },
            financialRepository = financialRepository
        )
    }
}
