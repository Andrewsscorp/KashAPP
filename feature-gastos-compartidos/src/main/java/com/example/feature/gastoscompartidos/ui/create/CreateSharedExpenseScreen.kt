package com.example.feature.gastoscompartidos.ui.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import com.example.core.ui.components.IosStyleSwitch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.components.CurrencyText
import com.example.data.local.entity.AccountEntity
import com.example.domain.models.shared.BudgetImpactPolicy
import com.example.domain.models.shared.RealMoneyImpactMode
import com.example.domain.models.shared.SplitCalculationMethod
import com.example.feature.gastoscompartidos.domain.engine.RealMoneyImpactEngine
import com.example.feature.gastoscompartidos.domain.ticket.SharedReceiptTicketEngine
import com.example.feature.gastoscompartidos.presentation.SharedExpensesIntent
import com.example.feature.gastoscompartidos.presentation.SharedExpensesUiState
import com.example.feature.gastoscompartidos.ui.theme.KashBlueTopBar
import com.example.feature.gastoscompartidos.ui.theme.KashFormattedCurrencyField
import com.example.feature.gastoscompartidos.ui.theme.KashFormattedDatePickerField
import com.example.feature.gastoscompartidos.ui.theme.KashStandardTextField
import com.example.feature.gastoscompartidos.ui.theme.SharedExpenseThemeTokens
import com.example.feature.gastoscompartidos.ui.theme.SharedExpenseThemeWrapper
import com.example.feature.gastoscompartidos.ui.theme.rememberSharedResponsiveLayoutInfo

@Composable
fun CreateSharedExpenseScreen(
    uiState: SharedExpensesUiState,
    onIntent: (SharedExpensesIntent) -> Unit,
    onOpenOcrItemizedModal: () -> Unit,
    onSaveAndEmitTicket: () -> Unit,
    onDismiss: () -> Unit
) {
    val responsive = rememberSharedResponsiveLayoutInfo()
    var showConsecutiveModal by remember { mutableStateOf(false) }
    var naturalAiPrompt by remember { mutableStateOf(uiState.aiNaturalPromptInput) }
    var showTaxDetails by remember { mutableStateOf(uiState.ivaInput > 0 || uiState.impoconsumoInput > 0 || uiState.tipInput > 0 || uiState.discountInput > 0) }

    val activeExpenseConsecutive = if (uiState.isManualConsecutiveActive && uiState.manualConsecutiveOverride.isNotBlank()) {
        uiState.manualConsecutiveOverride
    } else {
        uiState.nextAutoExpenseConsecutive
    }

    val computedTotal = (uiState.subtotalInput + uiState.ivaInput + uiState.impoconsumoInput + uiState.tipInput - uiState.discountInput).coerceAtLeast(0.0)

    val expenseCategories = listOf(
        "cat_food" to "🍽️ Alimentación",
        "cat_home" to "🏠 Hogar & Servicios",
        "cat_travel" to "✈️ Viajes & Hotel",
        "cat_leisure" to "🎉 Ocio & Salidas",
        "cat_transport" to "🚗 Transporte & Gasolina",
        "cat_grocery" to "🛒 Supermercado",
        "cat_health" to "💊 Salud & Farmacia",
        "cat_other" to "📦 Otros Gastos"
    )

    SharedExpenseThemeWrapper {
        Scaffold(
            topBar = {
                KashBlueTopBar(
                    title = "Nuevo Gasto Compartido",
                    subtitle = "Serie $activeExpenseConsecutive • Ticket ${uiState.nextAutoTicketConsecutive}",
                    onNavigateBack = onDismiss
                )
            },
            containerColor = SharedExpenseThemeTokens.KashLightBg
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = responsive.maxContentWidth)
                        .verticalScroll(rememberScrollState())
                        .padding(responsive.horizontalPadding),
                    verticalArrangement = Arrangement.spacedBy(responsive.verticalSpacing)
                ) {
                    // 1. Cabecera Interactiva de Consecutivos Documentales (GC y TK)
                    SharedConsecutiveHeaderBar(
                        expenseConsecutive = activeExpenseConsecutive,
                        ticketConsecutive = uiState.nextAutoTicketConsecutive,
                        isManualMode = uiState.isManualConsecutiveActive,
                        onClickEditConsecutive = { showConsecutiveModal = true }
                    )

                    // 2. Información General del Gasto
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, SharedExpenseThemeTokens.KashBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = SharedExpenseThemeTokens.KashBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "INFORMACIÓN DEL GASTO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SharedExpenseThemeTokens.KashBlue,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            KashStandardTextField(
                                value = uiState.expenseTitleInput,
                                onValueChange = { onIntent(SharedExpensesIntent.UpdateExpenseTitle(it)) },
                                label = "Concepto / Descripción del Gasto",
                                placeholder = "Ej: Almuerzo de trabajo, Mercado semanal...",
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Selector de Categoría en chips horizontales
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Categoría del Gasto",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SharedExpenseThemeTokens.KashTextSecondary
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(expenseCategories) { (catId, catLabel) ->
                                        val isSelected = uiState.selectedCategoryId == catId
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) SharedExpenseThemeTokens.KashNavy else SharedExpenseThemeTokens.KashSurfaceMuted,
                                            border = BorderStroke(1.dp, if (isSelected) SharedExpenseThemeTokens.KashNavy else SharedExpenseThemeTokens.KashBorder),
                                            modifier = Modifier.clickable {
                                                onIntent(SharedExpensesIntent.UpdateCategory(catId, catLabel.substringAfter(" ")))
                                            }
                                        ) {
                                            Text(
                                                text = catLabel,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else SharedExpenseThemeTokens.KashTextPrimary,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Comercio / Establecimiento y NIT
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                KashStandardTextField(
                                    value = uiState.merchantNameInput,
                                    onValueChange = { onIntent(SharedExpensesIntent.UpdateMerchantInfo(it, uiState.merchantNitInput)) },
                                    label = "Establecimiento / Comercio",
                                    placeholder = "Ej: Crepes & Waffles",
                                    modifier = Modifier.weight(1.3f)
                                )
                                KashStandardTextField(
                                    value = uiState.merchantNitInput,
                                    onValueChange = { onIntent(SharedExpensesIntent.UpdateMerchantInfo(uiState.merchantNameInput, it)) },
                                    label = "NIT / RUT",
                                    placeholder = "860.000.000-1",
                                    modifier = Modifier.weight(0.9f)
                                )
                            }

                            // Factura Externa / POS
                            KashStandardTextField(
                                value = uiState.externalInvoiceNumberInput,
                                onValueChange = { onIntent(SharedExpensesIntent.UpdateExternalInvoiceNumber(it)) },
                                label = "N° Factura Externa / Ticket POS (Opcional)",
                                placeholder = "Ej: FE-9874 o POS-1123",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 3. Fechas del Gasto y Cobro
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, SharedExpenseThemeTokens.KashBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = SharedExpenseThemeTokens.KashBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "FECHAS DEL DOCUMENTO & VENCIMIENTO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SharedExpenseThemeTokens.KashBlue,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            KashFormattedDatePickerField(
                                label = "Fecha de Emisión del Gasto",
                                valueIso = uiState.expenseDateIso,
                                onDateSelected = { onIntent(SharedExpensesIntent.UpdateExpenseDate(it)) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            KashFormattedDatePickerField(
                                label = "Fecha Límite de Cobro / Vencimiento",
                                valueIso = uiState.dueDateForSettlementIso,
                                onDateSelected = { onIntent(SharedExpensesIntent.UpdateDueDateForSettlement(it)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 4. Montos, Impuestos y Gran Total
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, SharedExpenseThemeTokens.KashBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Money,
                                    contentDescription = null,
                                    tint = SharedExpenseThemeTokens.KashBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "VALORES MONETARIOS & CÁLCULO NIIF",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SharedExpenseThemeTokens.KashBlue,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // Subtotal con formateador numérico y chips de incremento rápido
                            KashFormattedCurrencyField(
                                value = uiState.subtotalInput,
                                onValueChange = { newSubtotal ->
                                    onIntent(
                                        SharedExpensesIntent.UpdateAmounts(
                                            subtotal = newSubtotal,
                                            iva = uiState.ivaInput,
                                            impoconsumo = uiState.impoconsumoInput,
                                            tip = uiState.tipInput,
                                            discount = uiState.discountInput
                                        )
                                    )
                                },
                                label = "Subtotal Base (Antes de Impuestos)",
                                currencySymbol = uiState.currencyCode,
                                quickPresets = listOf(20000L, 50000L, 100000L, 200000L, 500000L),
                                onQuickAdd = { delta ->
                                    onIntent(
                                        SharedExpensesIntent.UpdateAmounts(
                                            subtotal = uiState.subtotalInput + delta,
                                            iva = uiState.ivaInput,
                                            impoconsumo = uiState.impoconsumoInput,
                                            tip = uiState.tipInput,
                                            discount = uiState.discountInput
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Toggle para desplegar impuestos y propinas
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTaxDetails = !showTaxDetails }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (showTaxDetails) "▼ Ocultar Impuestos, Propina y Descuentos" else "▶ Agregar IVA 19%, Impoconsumo 8%, Propina 10%...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SharedExpenseThemeTokens.KashBlue
                                )
                            }

                            AnimatedVisibility(visible = showTaxDetails) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // IVA 19%
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            KashFormattedCurrencyField(
                                                value = uiState.ivaInput,
                                                onValueChange = {
                                                    onIntent(SharedExpensesIntent.UpdateAmounts(uiState.subtotalInput, it, uiState.impoconsumoInput, uiState.tipInput, uiState.discountInput))
                                                },
                                                label = "IVA (19% General)",
                                                currencySymbol = uiState.currencyCode
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val autoIva = (uiState.subtotalInput * 0.19)
                                                onIntent(SharedExpensesIntent.UpdateAmounts(uiState.subtotalInput, autoIva, uiState.impoconsumoInput, uiState.tipInput, uiState.discountInput))
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("19%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SharedExpenseThemeTokens.KashBlue)
                                        }
                                    }

                                    // Impoconsumo 8%
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            KashFormattedCurrencyField(
                                                value = uiState.impoconsumoInput,
                                                onValueChange = {
                                                    onIntent(SharedExpensesIntent.UpdateAmounts(uiState.subtotalInput, uiState.ivaInput, it, uiState.tipInput, uiState.discountInput))
                                                },
                                                label = "Impoconsumo (8% Restaurantes)",
                                                currencySymbol = uiState.currencyCode
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val autoImpo = (uiState.subtotalInput * 0.08)
                                                onIntent(SharedExpensesIntent.UpdateAmounts(uiState.subtotalInput, uiState.ivaInput, autoImpo, uiState.tipInput, uiState.discountInput))
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("8%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SharedExpenseThemeTokens.KashBlue)
                                        }
                                    }

                                    // Propina 10%
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            KashFormattedCurrencyField(
                                                value = uiState.tipInput,
                                                onValueChange = {
                                                    onIntent(SharedExpensesIntent.UpdateAmounts(uiState.subtotalInput, uiState.ivaInput, uiState.impoconsumoInput, it, uiState.discountInput))
                                                },
                                                label = "Propina Voluntaria (10%)",
                                                currencySymbol = uiState.currencyCode
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val autoTip = (uiState.subtotalInput * 0.10)
                                                onIntent(SharedExpensesIntent.UpdateAmounts(uiState.subtotalInput, uiState.ivaInput, uiState.impoconsumoInput, autoTip, uiState.discountInput))
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("10%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SharedExpenseThemeTokens.KashBlue)
                                        }
                                    }

                                    // Descuento
                                    KashFormattedCurrencyField(
                                        value = uiState.discountInput,
                                        onValueChange = {
                                            onIntent(SharedExpensesIntent.UpdateAmounts(uiState.subtotalInput, uiState.ivaInput, uiState.impoconsumoInput, uiState.tipInput, it))
                                        },
                                        label = "Descuento Comercial",
                                        currencySymbol = uiState.currencyCode,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Banner Resumen Gran Total
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SharedExpenseThemeTokens.KashNavy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "GRAN TOTAL A DIVIDIR",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SharedExpenseThemeTokens.KashCyanLight,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "Subtotal + Impuestos + Propina - Dcto",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    CurrencyText(
                                        amountDouble = computedTotal,
                                        currencyCode = uiState.currencyCode,
                                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 5. Copiloto KashAI (Lenguaje Natural)
                    KashAiNaturalSplitBanner(
                        promptText = naturalAiPrompt,
                        onPromptChange = { naturalAiPrompt = it },
                        isProcessing = uiState.isAiProcessing,
                        remainingQuota = uiState.apiProtectionStatus.remainingDailyQuota,
                        onExecuteAiSplit = {
                            onIntent(SharedExpensesIntent.RunKashAiNaturalLanguageSplit(naturalAiPrompt))
                        }
                    )

                    // 6. Selector de Pagadores y Cuenta Bancaria Real
                    SharedPayerSelectorCard(
                        selectedPayerMode = uiState.selectedPayerMode,
                        accounts = uiState.accounts,
                        selectedAccountId = uiState.selectedKashAccountId,
                        currency = uiState.currencyCode,
                        onSelectPayerMode = { mode, accId ->
                            onIntent(SharedExpensesIntent.SelectPayerMode(mode, accId))
                        },
                        onAddInlineParticipant = {
                            onIntent(SharedExpensesIntent.OpenParticipantEditor(null))
                        }
                    )

                    // 7. Selector de los 7 Algoritmos Matemáticos de División
                    SharedSplitMethodCarousel(
                        selectedMethod = uiState.selectedSplitMethod,
                        onSelectMethod = { method ->
                            onIntent(SharedExpensesIntent.SelectSplitMethod(method))
                            if (method == SplitCalculationMethod.ITEMIZED_OCR) {
                                onOpenOcrItemizedModal()
                            }
                        }
                    )

                    // 8. Participantes Activos y Cuotas de División
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, SharedExpenseThemeTokens.KashBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        tint = SharedExpenseThemeTokens.KashBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "REPARTO ENTRE PARTICIPANTES",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SharedExpenseThemeTokens.KashBlue,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Text(
                                    text = "${uiState.splitParticipantsInput.count { it.isIncluded }} de ${uiState.splitParticipantsInput.size} activos",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SharedExpenseThemeTokens.KashTextSecondary
                                )
                            }

                            if (uiState.selectedSplitMethod == SplitCalculationMethod.ITEMIZED_OCR) {
                                Button(
                                    onClick = onOpenOcrItemizedModal,
                                    colors = ButtonDefaults.buttonColors(containerColor = SharedExpenseThemeTokens.KashBlue),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Configurar Consumos Ítem por Ítem (OCR)", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            uiState.splitParticipantsInput.forEach { participantInput ->
                                val liveRow = uiState.liveSplitCalculation?.participantSplits?.find { it.participantId == participantInput.participantId }
                                val isIncluded = participantInput.isIncluded

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isIncluded) SharedExpenseThemeTokens.KashSurfaceMuted else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isIncluded) SharedExpenseThemeTokens.KashBorder else Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isIncluded,
                                                onCheckedChange = { onIntent(SharedExpensesIntent.ToggleParticipantInclusion(participantInput.participantId)) },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = SharedExpenseThemeTokens.KashBlue,
                                                    checkmarkColor = Color.White
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = participantInput.displayName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isIncluded) SharedExpenseThemeTokens.KashTextPrimary else SharedExpenseThemeTokens.KashTextMuted
                                                )
                                                if (isIncluded && liveRow != null) {
                                                    Text(
                                                        text = "Cuota: ${SharedReceiptTicketEngine.formatMoney(liveRow.totalOwedShare, uiState.currencyCode)} (${String.format(java.util.Locale.US, "%.1f", liveRow.rawWeightOrPercentage)}%)",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = SharedExpenseThemeTokens.KashBlue
                                                    )
                                                }
                                            }
                                        }

                                        // Campos de ajuste específicos según método
                                        if (isIncluded) {
                                            when (uiState.selectedSplitMethod) {
                                                SplitCalculationMethod.EXACT_AMOUNT -> {
                                                    Box(modifier = Modifier.width(130.dp)) {
                                                        KashFormattedCurrencyField(
                                                            value = participantInput.exactAmountInput,
                                                            onValueChange = {
                                                                onIntent(SharedExpensesIntent.UpdateParticipantSplitParameter(participantInput.participantId, exactAmount = it))
                                                            },
                                                            label = "Monto",
                                                            currencySymbol = uiState.currencyCode
                                                        )
                                                    }
                                                }
                                                SplitCalculationMethod.PERCENTAGE -> {
                                                    Box(modifier = Modifier.width(90.dp)) {
                                                        KashStandardTextField(
                                                            value = participantInput.percentageInput.toString(),
                                                            onValueChange = { str ->
                                                                val p = str.toDoubleOrNull() ?: 0.0
                                                                onIntent(SharedExpensesIntent.UpdateParticipantSplitParameter(participantInput.participantId, percentage = p))
                                                            },
                                                            label = "% Cuota",
                                                            keyboardType = KeyboardType.Decimal
                                                        )
                                                    }
                                                }
                                                SplitCalculationMethod.SHARES -> {
                                                    Box(modifier = Modifier.width(80.dp)) {
                                                        KashStandardTextField(
                                                            value = participantInput.sharesUnitsInput.toString(),
                                                            onValueChange = { str ->
                                                                val s = str.toDoubleOrNull() ?: 1.0
                                                                onIntent(SharedExpensesIntent.UpdateParticipantSplitParameter(participantInput.participantId, shares = s))
                                                            },
                                                            label = "Partes",
                                                            keyboardType = KeyboardType.Decimal
                                                        )
                                                    }
                                                }
                                                else -> { /* Equal, OCR, Income se calculan automáticamente */ }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 9. Simulador Interactivo en Vivo de "Impacto en Tu Dinero Real"
                    RealMoneyImpactSimulatorCard(
                        selectedMode = uiState.selectedRealMoneyMode,
                        selectedBudgetPolicy = uiState.selectedBudgetPolicy,
                        autoReserveOwedInHold = uiState.autoReserveOwedInHold,
                        simulation = uiState.liveRealMoneySimulation,
                        currency = uiState.currencyCode,
                        onSelectMode = { onIntent(SharedExpensesIntent.SelectRealMoneyImpactMode(it)) },
                        onSelectBudgetPolicy = { onIntent(SharedExpensesIntent.SelectBudgetPolicy(it)) },
                        onToggleHoldReserve = { onIntent(SharedExpensesIntent.ToggleMentalHoldReserve(it)) }
                    )

                    // 10. Botón Principal de Guardado Atómico y Emisión de Ticket TK-xxxx
                    Button(
                        onClick = {
                            onIntent(SharedExpensesIntent.SubmitAndEmitSharedExpenseTicket)
                            onSaveAndEmitTicket()
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SharedExpenseThemeTokens.KashNavy),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("save_and_emit_shared_ticket_btn")
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Guardar y Emitir Ticket ${uiState.nextAutoTicketConsecutive}",
                            fontSize = if (responsive.isUltraCompactWidth) 13.sp else 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (showConsecutiveModal) {
            ConsecutiveOverrideDialog(
                currentAutoCode = uiState.nextAutoExpenseConsecutive,
                initialManualCode = uiState.manualConsecutiveOverride,
                onConfirmManual = { manualCode ->
                    onIntent(SharedExpensesIntent.OverrideConsecutiveCode(manualCode, isManual = true))
                    showConsecutiveModal = false
                },
                onRestoreAutomatic = {
                    onIntent(SharedExpensesIntent.OverrideConsecutiveCode("", isManual = false))
                    showConsecutiveModal = false
                },
                onDismiss = { showConsecutiveModal = false }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SharedConsecutiveHeaderBar(
    expenseConsecutive: String,
    ticketConsecutive: String,
    isManualMode: Boolean,
    onClickEditConsecutive: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SharedExpenseThemeTokens.KashNavy,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickEditConsecutive() }
            .testTag("consecutive_header_bar")
    ) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SharedExpenseThemeTokens.KashBlue
                ) {
                    Text(
                        text = "📄 $expenseConsecutive (${if (isManualMode) "Manual" else "Auto"})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        maxLines = 1
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SharedExpenseThemeTokens.ReceivableTeal
                ) {
                    Text(
                        text = "🎟️ $ticketConsecutive",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        maxLines = 1
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Personalizar consecutivo",
                    tint = SharedExpenseThemeTokens.KashCyanLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Serie / Manual",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SharedExpenseThemeTokens.KashCyanLight
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KashAiNaturalSplitBanner(
    promptText: String,
    onPromptChange: (String) -> Unit,
    isProcessing: Boolean,
    remainingQuota: Int,
    onExecuteAiSplit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF0F9FF),
        border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = SharedExpenseThemeTokens.KashBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "✨ Dividir con KashAI (Lenguaje Natural)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SharedExpenseThemeTokens.KashNavy
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFDCFCE7)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "App Check • PII Anonimizado ($remainingQuota disp.)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534)
                        )
                    }
                }
            }

            KashStandardTextField(
                value = promptText,
                onValueChange = onPromptChange,
                label = "Instrucción de División en Lenguaje Natural",
                placeholder = "Ej: Cena de $200.000 con Ana y Carlos, Carlos pidió vino extra de $35.000...",
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onExecuteAiSplit,
                enabled = !isProcessing && promptText.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SharedExpenseThemeTokens.KashBlue),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = if (isProcessing) "Analizando con Firebase AI..." else "Autocompletar División",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun SharedPayerSelectorCard(
    selectedPayerMode: String,
    accounts: List<AccountEntity>,
    selectedAccountId: String?,
    currency: String,
    onSelectPayerMode: (mode: String, kashAccountId: String?) -> Unit,
    onAddInlineParticipant: () -> Unit
) {
    val payerModes = listOf(
        "SELF_ONLY" to "Pagué Yo (100%)",
        "OTHER_PARTICIPANT" to "Pagó Otro Tercero",
        "MULTI_PAYER" to "Multi-Pagador Mixto"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SharedExpenseThemeTokens.KashBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = SharedExpenseThemeTokens.KashBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "¿QUIÉN PAGÓ EN CAJA Y CON QUÉ CUENTA?",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SharedExpenseThemeTokens.KashBlue
                    )
                }

                OutlinedButton(
                    onClick = onAddInlineParticipant,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp), tint = SharedExpenseThemeTokens.KashBlue)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Tercero", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SharedExpenseThemeTokens.KashBlue)
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(payerModes) { (code, label) ->
                    val isSelected = selectedPayerMode == code
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) SharedExpenseThemeTokens.KashNavy else SharedExpenseThemeTokens.KashSurfaceMuted,
                        border = BorderStroke(1.dp, if (isSelected) SharedExpenseThemeTokens.KashNavy else SharedExpenseThemeTokens.KashBorder),
                        modifier = Modifier.clickable { onSelectPayerMode(code, selectedAccountId) }
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else SharedExpenseThemeTokens.KashTextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            if (accounts.isNotEmpty() && selectedPayerMode != "OTHER_PARTICIPANT") {
                Text(
                    text = "Cuenta de Débito Bancario en KashApp:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SharedExpenseThemeTokens.KashTextSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts, key = { it.id }) { acc ->
                        val isSelected = acc.id == (selectedAccountId ?: accounts.firstOrNull()?.id)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SharedExpenseThemeTokens.ReceivableTealBg else Color.White,
                            border = BorderStroke(1.dp, if (isSelected) SharedExpenseThemeTokens.ReceivableTeal else SharedExpenseThemeTokens.KashBorder),
                            modifier = Modifier.clickable { onSelectPayerMode(selectedPayerMode, acc.id) }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = "${acc.name} ${if (acc.isTaxExempt) "(Exenta 4x1000)" else "(4x1000 Activo)"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SharedExpenseThemeTokens.KashNavy
                                )
                                Text(
                                    text = "Disp: ${SharedReceiptTicketEngine.formatMoney(acc.availableBalance, currency)}",
                                    fontSize = 11.sp,
                                    color = SharedExpenseThemeTokens.KashTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RealMoneyImpactSimulatorCard(
    selectedMode: RealMoneyImpactMode,
    selectedBudgetPolicy: BudgetImpactPolicy,
    autoReserveOwedInHold: Boolean,
    simulation: RealMoneyImpactEngine.RealMoneyImpactSimulation?,
    currency: String,
    onSelectMode: (RealMoneyImpactMode) -> Unit,
    onSelectBudgetPolicy: (BudgetImpactPolicy) -> Unit,
    onToggleHoldReserve: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.5.dp, SharedExpenseThemeTokens.KashBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "CÓMO AFECTA ESTE GASTO A TU DINERO REAL",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SharedExpenseThemeTokens.KashBlue,
                letterSpacing = 0.5.sp
            )

            // Selector de los 4 Modos Contables
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RealMoneyImpactMode.entries) { mode ->
                    val isSelected = mode == selectedMode
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) SharedExpenseThemeTokens.KashNavy else SharedExpenseThemeTokens.KashSurfaceMuted,
                        border = BorderStroke(1.dp, if (isSelected) SharedExpenseThemeTokens.KashNavy else SharedExpenseThemeTokens.KashBorder),
                        modifier = Modifier.clickable { onSelectMode(mode) }
                    ) {
                        Text(
                            text = mode.shortBadge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else SharedExpenseThemeTokens.KashTextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            if (simulation != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SharedExpenseThemeTokens.KashSurfaceMuted,
                    border = BorderStroke(1.dp, SharedExpenseThemeTokens.KashBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SimulatorMetricLine(
                            label = "1. Sale hoy de tu cuenta bancaria:",
                            valueText = "- ${SharedReceiptTicketEngine.formatMoney(simulation.immediateAccountBalanceDebit + simulation.immediateGmf4x1000Debit, currency)}",
                            valueColor = SharedExpenseThemeTokens.PayableRose
                        )

                        SimulatorMetricLine(
                            label = "2. Tu Gasto Real (PyG y Presupuesto):",
                            valueText = SharedReceiptTicketEngine.formatMoney(simulation.recognizedExpenseInPnlToday, currency),
                            valueColor = SharedExpenseThemeTokens.KashTextPrimary
                        )

                        SimulatorMetricLine(
                            label = "3. Activo Cuenta por Cobrar (NIIF):",
                            valueText = "+ ${SharedReceiptTicketEngine.formatMoney(simulation.accountsReceivableAssetDelta, currency)}",
                            valueColor = SharedExpenseThemeTokens.ReceivableTeal
                        )

                        if (simulation.mentalHoldReservedDelta > 0.0) {
                            SimulatorMetricLine(
                                label = "4. Bloqueo en tu Dinero Libre (holdBalance):",
                                valueText = "🔒 ${SharedReceiptTicketEngine.formatMoney(simulation.mentalHoldReservedDelta, currency)}",
                                labelColor = SharedExpenseThemeTokens.MentalHoldViolet,
                                valueColor = SharedExpenseThemeTokens.MentalHoldViolet
                            )
                        }

                        HorizontalDivider(color = SharedExpenseThemeTokens.KashBorder)
                        Text(
                            text = simulation.detailedAccountingExplanation,
                            fontSize = 11.sp,
                            color = SharedExpenseThemeTokens.KashTextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Switch de Protección Mental Accounting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Proteger en holdBalance lo que deba pagar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SharedExpenseThemeTokens.KashTextPrimary
                    )
                    Text(
                        text = "Aparta el dinero de tu saldo libre cuando otra persona pague por ti.",
                        fontSize = 11.sp,
                        color = SharedExpenseThemeTokens.KashTextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IosStyleSwitch(
                    checked = autoReserveOwedInHold,
                    onCheckedChange = onToggleHoldReserve,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SharedExpenseThemeTokens.KashBlue
                    )
                )
            }
        }
    }
}

@Composable
private fun SimulatorMetricLine(
    label: String,
    valueText: String,
    labelColor: Color = SharedExpenseThemeTokens.KashTextSecondary,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = labelColor,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = valueText,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor
        )
    }
}

@Composable
fun SharedSplitMethodCarousel(
    selectedMethod: SplitCalculationMethod,
    onSelectMethod: (SplitCalculationMethod) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "MÉTODO MATEMÁTICO DE DIVISIÓN (7 ALGORITMOS)",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = SharedExpenseThemeTokens.KashTextSecondary
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(SplitCalculationMethod.entries) { method ->
                val isSelected = method == selectedMethod
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) SharedExpenseThemeTokens.KashBlue else Color.White,
                    border = BorderStroke(1.dp, if (isSelected) SharedExpenseThemeTokens.KashBlue else SharedExpenseThemeTokens.KashBorder),
                    modifier = Modifier.clickable { onSelectMethod(method) }
                ) {
                    Text(
                        text = method.shortLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else SharedExpenseThemeTokens.KashTextPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ConsecutiveOverrideDialog(
    currentAutoCode: String,
    initialManualCode: String,
    onConfirmManual: (String) -> Unit,
    onRestoreAutomatic: () -> Unit,
    onDismiss: () -> Unit
) {
    var inputCode by remember { mutableStateOf(initialManualCode.ifBlank { currentAutoCode }) }

    SharedExpenseThemeWrapper {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Configurar Consecutivo del Gasto",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SharedExpenseThemeTokens.KashNavy
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Consecutivo automático sugerido por la serie activa: $currentAutoCode. También puedes ingresar un código manual o el número de factura electrónica (FE/POS):",
                        fontSize = 12.sp,
                        color = SharedExpenseThemeTokens.KashTextSecondary
                    )
                    KashStandardTextField(
                        value = inputCode,
                        onValueChange = { inputCode = it.uppercase() },
                        label = "Código Consecutivo / Factura",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onConfirmManual(inputCode) },
                    colors = ButtonDefaults.buttonColors(containerColor = SharedExpenseThemeTokens.KashBlue)
                ) {
                    Text("Usar Código Manual", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onRestoreAutomatic) {
                    Text("Usar Automático ($currentAutoCode)", color = SharedExpenseThemeTokens.KashNavy)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
