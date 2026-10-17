package com.example.presentation.investments

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentation.investments.state.InvestmentFormState
import com.example.presentation.investments.state.InvestmentFormViewModel
import com.example.presentation.investments.state.InvestmentKeyboardTarget
import com.example.presentation.investments.widgets.CreateCustomInstitutionDialog
import com.example.presentation.investments.widgets.InvestmentAccountsCard
import com.example.presentation.investments.widgets.InvestmentBasicInfoCard
import com.example.presentation.investments.widgets.InvestmentCapitalCard
import com.example.presentation.investments.widgets.InvestmentCustomizationCard
import com.example.presentation.investments.widgets.InvestmentDocumentsCard
import com.example.presentation.investments.widgets.InvestmentNetWorthCard
import com.example.presentation.investments.widgets.InvestmentNumericKeyboard
import com.example.presentation.investments.widgets.InvestmentYieldCard
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvestmentScreen(
    viewModel: InvestmentFormViewModel,
    onNavigateBack: () -> Unit,
    onInvestmentCreatedSuccessfully: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Accordions collapsed by default as required by the architectural rules
    val expandedSections = remember {
        mutableStateMapOf(
            "basic" to false,
            "capital" to false,
            "yield" to false,
            "accounts" to false,
            "networth" to false,
            "documents" to false,
            "customization" to false
        )
    }

    BackHandler {
        if (state.activeKeyboardTarget != null) {
            viewModel.closeKeyboard()
        } else {
            onNavigateBack()
        }
    }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            onInvestmentCreatedSuccessfully()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .testTag("create_investment_screen")
    ) {
        Scaffold(
            topBar = {
                Surface(
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.testTag("back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Regresar",
                                    tint = KashNavy
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Crear inversión",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KashNavy
                                )
                                Text(
                                    text = "Activos, acciones, cripto, bienes raíces y préstamos",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        // Live Financial Summary in Sticky Bottom Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Invertido: $ ${String.format(Locale.GERMANY, "%,.0f", state.initialAmount)} ${state.currencyCode}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "Actual: $ ${String.format(Locale.GERMANY, "%,.0f", if (state.currentValue > 0.0) state.currentValue else state.initialAmount)} ${state.currencyCode}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KashNavy
                                )
                            }

                            state.roiResult?.let { roi ->
                                val isGain = roi.capitalGainLoss >= 0.0
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isGain) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                ) {
                                    Text(
                                        text = "ROI: ${if (isGain) "+" else ""}${String.format(Locale.US, "%.1f", roi.roiPercentage)}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isGain) Color(0xFF15803D) else Color(0xFFB91C1C),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { viewModel.saveInvestment() },
                            enabled = state.isFormSubmittable && !state.isSaving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KashBlue,
                                disabledContainerColor = Color(0xFFCBD5E1)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_investment_button")
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Guardando inversión...",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = "Guardar inversión",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            },
            containerColor = Color(0xFFF8FAFC)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .widthIn(max = 700.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Información básica (con caja de valor directo tras el nombre)
                InvestmentBasicInfoCard(
                    state = state,
                    isExpanded = expandedSections["basic"] == true,
                    onToggleExpand = { expandedSections["basic"] = !(expandedSections["basic"] ?: false) },
                    onNameChanged = { viewModel.onNameChanged(it) },
                    onTypeSelected = { id, name, icon -> viewModel.onTypeSelected(id, name, icon) },
                    onInstitutionSelected = { id, name -> viewModel.onInstitutionSelected(id, name) },
                    onOpenCreateInstitution = { viewModel.openCreateInstitutionDialog(true) },
                    onCurrencySelected = { viewModel.onCurrencySelected(it) },
                    onAcquisitionDateSelected = { viewModel.onAcquisitionDateSelected(it) },
                    onStatusSelected = { viewModel.onStatusSelected(it) },
                    onOpenKeyboard = { target -> viewModel.openKeyboard(target) },
                    onQuickAdd = { amt -> viewModel.onQuickAdd(amt) }
                )

                // 2. Capital: El Motor Matemático (Monto inicial, Valor actual, Aportes, Retiros, Plusvalía)
                InvestmentCapitalCard(
                    state = state,
                    isExpanded = expandedSections["capital"] == true,
                    onToggleExpand = { expandedSections["capital"] = !(expandedSections["capital"] ?: false) },
                    onOpenKeyboard = { target -> viewModel.openKeyboard(target) }
                )

                // 3. Rendimiento e Intereses (Tasa %, Periodicidad, Próximo pago)
                InvestmentYieldCard(
                    state = state,
                    isExpanded = expandedSections["yield"] == true,
                    onToggleExpand = { expandedSections["yield"] = !(expandedSections["yield"] ?: false) },
                    onGeneratesYieldChanged = { viewModel.onGeneratesYieldChanged(it) },
                    onRateTypeSelected = { viewModel.onRateTypeSelected(it) },
                    onYieldPeriodicitySelected = { viewModel.onYieldPeriodicitySelected(it) },
                    onValueUpdateDateSelected = { viewModel.onValueUpdateDateSelected(it) },
                    onOpenKeyboard = { target -> viewModel.openKeyboard(target) }
                )

                // 4. Cuenta Asociada (Partida Doble: Origen y Rendimientos)
                InvestmentAccountsCard(
                    state = state,
                    isExpanded = expandedSections["accounts"] == true,
                    onToggleExpand = { expandedSections["accounts"] = !(expandedSections["accounts"] ?: false) },
                    onOriginAccountSelected = { id, name -> viewModel.onOriginAccountSelected(id, name) },
                    onYieldAccountSelected = { id, name -> viewModel.onYieldAccountSelected(id, name) }
                )

                // 5. Patrimonio Neto (Suma al activo global con regla TRM)
                InvestmentNetWorthCard(
                    state = state,
                    isExpanded = expandedSections["networth"] == true,
                    onToggleExpand = { expandedSections["networth"] = !(expandedSections["networth"] ?: false) },
                    onIncludeInNetWorthChanged = { viewModel.onIncludeInNetWorthChanged(it) }
                )

                // 6. Documentos y Comprobantes (Límite 10 MB, PDF, JPG, PNG)
                InvestmentDocumentsCard(
                    state = state,
                    isExpanded = expandedSections["documents"] == true,
                    onToggleExpand = { expandedSections["documents"] = !(expandedSections["documents"] ?: false) },
                    onDocumentSelected = { uri, name, size -> viewModel.onDocumentSelected(uri, name, size) },
                    onDocumentCategorySelected = { viewModel.onDocumentCategorySelected(it) }
                )

                // 7. Personalización y Notas (Favorito, Color, Tags, Notas)
                InvestmentCustomizationCard(
                    state = state,
                    isExpanded = expandedSections["customization"] == true,
                    onToggleExpand = { expandedSections["customization"] = !(expandedSections["customization"] ?: false) },
                    onFavoriteChanged = { viewModel.onFavoriteChanged(it) },
                    onColorSelected = { viewModel.onColorSelected(it) },
                    onTagsChanged = { viewModel.onTagsChanged(it) },
                    onNotesChanged = { viewModel.onNotesChanged(it) }
                )

                Spacer(modifier = Modifier.height(90.dp))
            }
        }

        // Dim overlay when keyboard is open
        AnimatedVisibility(
            visible = state.activeKeyboardTarget != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { viewModel.closeKeyboard() }
            )
        }

        // Floating In-App Numeric Keyboard with Masks
        AnimatedVisibility(
            visible = state.activeKeyboardTarget != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val target = state.activeKeyboardTarget
            val title = when (target) {
                is InvestmentKeyboardTarget.InitialAmount -> "Monto inicial invertido"
                is InvestmentKeyboardTarget.CurrentValue -> "Valor actual (Mark-to-market)"
                is InvestmentKeyboardTarget.Contributions -> "Aportes adicionales de capital"
                is InvestmentKeyboardTarget.Withdrawals -> "Retiros de capital realizados"
                is InvestmentKeyboardTarget.YieldRate -> "Tasa de rendimiento (%)"
                is InvestmentKeyboardTarget.NetWorthValue -> "Valor en patrimonio neto"
                null -> "Ingresar monto"
            }
            val currentVal = when (target) {
                is InvestmentKeyboardTarget.InitialAmount -> state.initialAmount
                is InvestmentKeyboardTarget.CurrentValue -> state.currentValue
                is InvestmentKeyboardTarget.Contributions -> state.contributions
                is InvestmentKeyboardTarget.Withdrawals -> state.withdrawals
                is InvestmentKeyboardTarget.YieldRate -> state.yieldRate
                is InvestmentKeyboardTarget.NetWorthValue -> state.netWorthValue
                null -> 0.0
            }
            val isPercentage = target is InvestmentKeyboardTarget.YieldRate

            InvestmentNumericKeyboard(
                visible = true,
                title = title,
                currentValue = currentVal,
                isPercentage = isPercentage,
                onDigitClick = { viewModel.onDigitClick(it) },
                onQuickAdd = { viewModel.onQuickAdd(it) },
                onBackspace = { viewModel.onBackspace() },
                onClear = { viewModel.onClear() },
                onDone = { viewModel.closeKeyboard() }
            )
        }

        // Custom Institution Modal
        if (state.isCreateInstitutionDialogOpen) {
            CreateCustomInstitutionDialog(
                onDismiss = { viewModel.openCreateInstitutionDialog(false) },
                onSaveInstitution = { name, code ->
                    viewModel.createCustomInstitution(name, code)
                }
            )
        }
    }
}
