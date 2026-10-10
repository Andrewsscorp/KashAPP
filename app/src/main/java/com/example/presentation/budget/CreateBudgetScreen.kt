package com.example.presentation.budget

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentation.budget.state.BudgetFormViewModel
import com.example.presentation.budget.state.BudgetKeyboardTarget
import com.example.presentation.budget.widgets.BudgetBasicInfoCard
import com.example.presentation.budget.widgets.BudgetCategoriesAccordionCard
import com.example.presentation.budget.widgets.BudgetCeilingCard
import com.example.presentation.budget.widgets.BudgetNumericKeyboard
import com.example.presentation.budget.widgets.BudgetSummaryCard
import com.example.presentation.budget.widgets.CreateCustomCategoryDialog
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBudgetScreen(
    viewModel: BudgetFormViewModel,
    onNavigateBack: () -> Unit,
    onBudgetCreatedSuccessfully: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler {
        if (state.activeKeyboardTarget != null) {
            viewModel.closeKeyboard()
        } else {
            onNavigateBack()
        }
    }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            onBudgetCreatedSuccessfully()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .testTag("create_budget_screen")
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Presupuestos y Sobres",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Planificación financiera y control por categorías",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Regresar",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0F172A),
                        titleContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF0F172A),
                    shadowElevation = 16.dp,
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        // Quick dynamic progress summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Techo: $ ${String.format(Locale.GERMANY, "%,.0f", state.totalAmount)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = if (state.isOverbudget) {
                                    "Sobreasignado: $ ${String.format(Locale.GERMANY, "%,.0f", Math.abs(state.unassignedAmount))}"
                                } else {
                                    "Sin asignar: $ ${String.format(Locale.GERMANY, "%,.0f", state.unassignedAmount)}"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isOverbudget) Color(0xFFEF4444) else if (state.isZeroBasedBalanced) Color(0xFF10B981) else Color(0xFF38BDF8)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.saveBudget() },
                            enabled = state.isFormSubmittable,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF007BFF),
                                disabledContainerColor = Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_budget_button")
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Guardando presupuesto...",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = if (state.isOverbudget) "Sobreasignado (Corregir montos)" else "Guardar presupuesto",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            },
            containerColor = Color(0xFF0F172A)
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
                // 1. Configuración del presupuesto (Información básica y Monto total)
                BudgetBasicInfoCard(
                    state = state,
                    onToggleExpand = { viewModel.toggleAccordion("basic") },
                    onNameChanged = { viewModel.onNameChanged(it) },
                    onOpenKeyboard = { target -> viewModel.openKeyboard(target) },
                    onQuickAddCeiling = { amt -> viewModel.onQuickAddCeiling(amt) },
                    onPeriodChanged = { viewModel.onPeriodChanged(it) },
                    onStartDateChanged = { viewModel.onStartDateChanged(it) },
                    onEndDateChanged = { viewModel.onEndDateChanged(it) },
                    onDescriptionChanged = { viewModel.onDescriptionChanged(it) },
                    onColorChanged = { viewModel.onColorChanged(it) }
                )

                // 2. Asignación por Categorías (Sobres de Gasto, Botón Crear Categoría, MDM chips)
                BudgetCategoriesAccordionCard(
                    state = state,
                    onToggleExpand = { viewModel.toggleAccordion("categories") },
                    onAddCategory = { category -> viewModel.addCategoryEnvelope(category, 0.0) },
                    onRemoveCategory = { envelopeId -> viewModel.removeCategoryEnvelope(envelopeId) },
                    onOpenKeyboard = { target -> viewModel.openKeyboard(target) },
                    onOpenCreateCategoryDialog = { viewModel.openCreateCategoryDialog(true) },
                    onQuickAddEnvelopeAmount = { envelopeId, amt ->
                        viewModel.onQuickAddEnvelopeAmount(envelopeId, amt)
                    }
                )

                // 4. Tarjeta Resumen Matemático en Tiempo Real & Zero-Based Budgeting Banner
                BudgetSummaryCard(state = state)

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Dim overlay when keyboard is active
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

        // Floating In-App Numeric Keyboard
        AnimatedVisibility(
            visible = state.activeKeyboardTarget != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val target = state.activeKeyboardTarget
            val title = when (target) {
                is BudgetKeyboardTarget.Ceiling -> "Monto total del presupuesto"
                is BudgetKeyboardTarget.Envelope -> "Monto para ${target.categoryName}"
                null -> "Ingresar monto"
            }
            val currentVal = when (target) {
                is BudgetKeyboardTarget.Ceiling -> state.totalAmount
                is BudgetKeyboardTarget.Envelope -> {
                    state.items.find { it.id == target.envelopeId }?.allocatedAmount ?: 0.0
                }
                null -> 0.0
            }

            BudgetNumericKeyboard(
                visible = true,
                title = title,
                currentValue = currentVal,
                onDigitClick = { viewModel.onDigitClick(it) },
                onQuickAdd = { viewModel.onQuickAdd(it) },
                onBackspace = { viewModel.onBackspace() },
                onClear = { viewModel.onClear() },
                onDone = { viewModel.closeKeyboard() }
            )
        }

        // Custom Category Creation Dialog
        if (state.isCreateCategoryDialogOpen) {
            CreateCustomCategoryDialog(
                onDismiss = { viewModel.openCreateCategoryDialog(false) },
                onSaveCategory = { name, colorHex, icon ->
                    viewModel.createCustomCategory(name, colorHex, icon)
                }
            )
        }
    }
}
