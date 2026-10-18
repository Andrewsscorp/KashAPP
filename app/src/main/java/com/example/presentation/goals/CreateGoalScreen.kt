package com.example.presentation.goals

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentation.goals.state.GoalFormViewModel
import com.example.core.ui.adaptive.rememberKashAdaptiveInfo
import com.example.presentation.goals.widgets.GoalBasicInfoCard
import com.example.presentation.goals.widgets.GoalFinancialTargetCard
import com.example.presentation.goals.widgets.GoalFundingSourceCard
import com.example.presentation.goals.widgets.GoalNumericKeyboard
import com.example.presentation.goals.widgets.GoalSavingPlanCard
import com.example.presentation.goals.widgets.GoalTimelineCard
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalScreen(
    viewModel: GoalFormViewModel,
    onNavigateBack: () -> Unit,
    onGoalCreatedSuccessfully: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val adaptive = rememberKashAdaptiveInfo()

    BackHandler {
        if (state.activeKeyboardTarget != null) {
            viewModel.closeKeyboard()
        } else {
            onNavigateBack()
        }
    }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            onGoalCreatedSuccessfully()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .testTag("create_goal_screen")
    ) {
        Scaffold(
            topBar = {
                Surface(
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = adaptive.maxContentWidth)
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
                                    modifier = Modifier.size(44.dp).testTag("back_button")
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
                                        text = "Crear objetivo",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KashNavy
                                    )
                                    Text(
                                        text = "Planificación y Mental Accounting",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
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
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = adaptive.maxContentWidth)
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
                                    text = "Meta: $ ${String.format(Locale.GERMANY, "%,.0f", state.targetAmount)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "Faltan: $ ${String.format(Locale.GERMANY, "%,.0f", state.remainingAmount)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KashBlue
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { viewModel.saveGoal() },
                                enabled = state.isFormSubmittable,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = KashBlue,
                                    disabledContainerColor = Color(0xFFCBD5E1)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("save_goal_button")
                            ) {
                                if (state.isSaving) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Guardando objetivo...",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                } else {
                                    Text(
                                        text = "Guardar objetivo",
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
            containerColor = Color(0xFFF8FAFC)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .widthIn(max = adaptive.maxContentWidth),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                // 1. Información Básica
                GoalBasicInfoCard(
                    state = state,
                    onToggleExpand = { viewModel.toggleAccordion("basic") },
                    onNameChanged = { viewModel.onNameChanged(it) },
                    onCategoryChanged = { viewModel.onCategoryChanged(it) },
                    onDescriptionChanged = { viewModel.onDescriptionChanged(it) },
                    onColorChanged = { viewModel.onColorChanged(it) },
                    onIconChanged = { viewModel.onIconChanged(it) },
                    onOpenKeyboard = { target -> viewModel.openKeyboard(target) }
                )

                // 2. Meta Financiera (with Live Reactive Deficit & Progress Bar)
                GoalFinancialTargetCard(
                    state = state,
                    onToggleExpand = { viewModel.toggleAccordion("financial") },
                    onOpenKeyboard = { target -> viewModel.openKeyboard(target) }
                )

                // 3. Fecha Objetivo (Timeline & Months Calculation)
                GoalTimelineCard(
                    state = state,
                    onToggleExpand = { viewModel.toggleAccordion("timeline") },
                    onHasDeadlineChanged = { viewModel.onHasDeadlineChanged(it) },
                    onDeadlineDateChanged = { viewModel.onDeadlineDateChanged(it) },
                    onStartDateChanged = { viewModel.onStartDateChanged(it) }
                )

                // 4. Plan de Ahorro (Auto-calculated contribution, Virtual Pocket Switch & Target Account)
                GoalSavingPlanCard(
                    state = state,
                    onToggleExpand = { viewModel.toggleAccordion("saving") },
                    onOpenKeyboard = { target -> viewModel.openKeyboard(target) },
                    onFrequencyChanged = { viewModel.onFrequencyChanged(it) },
                    onAutoContributeChanged = { viewModel.onAutoContributeChanged(it) },
                    onVirtualPocketChanged = { viewModel.onVirtualPocketChanged(it) },
                    onTargetAccountChanged = { id, name -> viewModel.onTargetAccountChanged(id, name) }
                )

                // 5. Fuente del Objetivo (Source account & Contribution method)
                GoalFundingSourceCard(
                    state = state,
                    onToggleExpand = { viewModel.toggleAccordion("funding") },
                    onSourceAccountChanged = { id, name -> viewModel.onSourceAccountChanged(id, name) },
                    onContributionMethodChanged = { viewModel.onContributionMethodChanged(it) }
                )

                // 6. Rule Engine Smart Notification Banner
                RuleEngineValidationBanner(
                    isValid = state.isRuleEngineValid,
                    title = state.ruleEngineTitle,
                    message = state.ruleEngineMessage
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

        // Overlay & In-App Numeric Keyboard
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
                    ) {
                        viewModel.closeKeyboard()
                    }
            )
        }

        AnimatedVisibility(
            visible = state.activeKeyboardTarget != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val title = when (state.activeKeyboardTarget) {
                "TARGET_AMOUNT" -> "¿Cuánto necesitas?"
                "SAVED_AMOUNT" -> "¿Cuánto tienes ahorrado?"
                "MONTHLY_CONTRIBUTION" -> "Aporte mensual personalizado"
                else -> "Monto"
            }

            GoalNumericKeyboard(
                visible = true,
                title = title,
                onDigitClick = { digit -> viewModel.onKeyboardDigit(digit) },
                onQuickAdd = { amount -> viewModel.onKeyboardQuickAdd(amount) },
                onBackspace = { viewModel.onKeyboardBackspace() },
                onClear = { viewModel.onKeyboardClear() },
                onDone = { viewModel.closeKeyboard() },
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }
}

/**
 * Proactive Financial Advisor Rule Engine Validation Box.
 * Evaluates whether user contribution + timeline covers target amount on time.
 */
@Composable
private fun RuleEngineValidationBanner(
    isValid: Boolean,
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isValid) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
    val borderColor = if (isValid) Color(0xFF86EFAC) else Color(0xFFFCD34D)
    val iconTint = if (isValid) Color(0xFF16A34A) else Color(0xFFD97706)
    val titleColor = if (isValid) Color(0xFF15803D) else Color(0xFFB45309)
    val descColor = if (isValid) Color(0xFF166534) else Color(0xFF92400E)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rule_engine_banner"),
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isValid) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message,
                    fontSize = 12.sp,
                    color = descColor,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
