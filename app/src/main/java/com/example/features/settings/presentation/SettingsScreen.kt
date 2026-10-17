package com.example.features.settings.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.KashAppApplication
import com.example.features.settings.domain.model.SettingsSubRoute
import com.example.features.settings.presentation.components.SettingsBottomNavBar
import com.example.features.settings.presentation.components.SettingsMainContent
import com.example.features.settings.presentation.screens.SettingsAboutSubScreen
import com.example.features.settings.presentation.screens.SettingsAccountsSubScreen
import com.example.features.settings.presentation.screens.SettingsAppearanceSubScreen
import com.example.features.settings.presentation.screens.SettingsBackupScreen
import com.example.features.settings.presentation.screens.SettingsCategoriesSubScreen
import com.example.features.settings.presentation.screens.SettingsCurrencyScreen
import com.example.features.settings.presentation.screens.SettingsDashboardLayoutSubScreen
import com.example.features.settings.presentation.screens.SettingsHelpSubScreen
import com.example.features.settings.presentation.screens.SettingsMerchantsSubScreen
import com.example.features.settings.presentation.screens.SettingsNotificationsScreen
import com.example.features.settings.presentation.screens.SettingsProfileScreen
import com.example.features.settings.presentation.screens.SettingsSecurityScreen
import com.example.features.settings.presentation.screens.getSubRouteTitle
import com.example.presentation.trm.CurrencyTrmScreen
import com.example.presentation.trm.CurrencyTrmViewModel
import com.example.presentation.trm.CurrencyTrmViewModelFactory
import com.example.ui.theme.KashNavy

/**
 * Enterprise Settings Master Coordinator Screen (~180 lines).
 * Delegates modularized logic, components, and dialogs to dedicated feature screens.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToSummary: () -> Unit,
    onFabAddClick: () -> Unit,
    onLogoutSuccess: () -> Unit,
    onAccountDeletedSuccess: () -> Unit = onLogoutSuccess,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    BackHandler {
        if (uiState.currentSubRoute != SettingsSubRoute.MAIN) {
            viewModel.navigateBackToMain()
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("kashapp_settings_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            SettingsTopBar(
                title = if (uiState.currentSubRoute == SettingsSubRoute.MAIN) "Ajustes" else getSubRouteTitle(uiState.currentSubRoute),
                subtitle = if (uiState.currentSubRoute == SettingsSubRoute.MAIN) "Personaliza tu experiencia en KashApp" else null,
                onBackClick = {
                    if (uiState.currentSubRoute != SettingsSubRoute.MAIN) {
                        viewModel.navigateBackToMain()
                    } else {
                        onNavigateBack()
                    }
                }
            )
        },
        bottomBar = {
            SettingsBottomNavBar(
                onHomeClick = onNavigateToHome,
                onTransactionsClick = onNavigateToTransactions,
                onFabClick = onFabAddClick,
                onSummaryClick = onNavigateToSummary,
                onSettingsClick = { viewModel.navigateBackToMain() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = uiState.currentSubRoute,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "settings_subroute_transition"
            ) { subRoute ->
                when (subRoute) {
                    SettingsSubRoute.MAIN -> {
                        SettingsMainContent(
                            userName = uiState.userName,
                            userEmail = uiState.userEmail,
                            autoNoAffectInventoryForAssets = uiState.preferences.autoNoAffectInventoryForAssets,
                            onToggleAutoNoAffectInventory = { viewModel.toggleAutoNoAffectInventory(it) },
                            onApplyNoAffectInventoryToAll = { viewModel.applyNoAffectInventoryToAllAssets(it) },
                            onNavigateTo = { viewModel.navigateToSubRoute(it) },
                            onRequestLogout = { viewModel.showLogoutDialog(true) },
                            onRequestDeleteAccount = { viewModel.showDeleteAccountDialog(true) }
                        )
                    }
                    SettingsSubRoute.MI_PERFIL -> {
                        SettingsProfileScreen(
                            name = uiState.userName,
                            email = uiState.userEmail,
                            phone = uiState.userPhone,
                            onSaveProfile = { newName, newEmail, newPhone ->
                                viewModel.updateProfile(newName, newEmail, newPhone)
                            },
                            onBack = { viewModel.navigateBackToMain() }
                        )
                    }
                    SettingsSubRoute.SEGURIDAD -> {
                        SettingsSecurityScreen(
                            biometricsEnabled = uiState.preferences.biometricsEnabled,
                            onToggleBiometrics = { viewModel.toggleBiometrics(it) },
                            onShowMessage = { viewModel.showStatusMessage(it) },
                            onBack = { viewModel.navigateBackToMain() }
                        )
                    }
                    SettingsSubRoute.AUDITORIA -> {
                        val ctx = LocalContext.current
                        val appContainer = (ctx.applicationContext as KashAppApplication).appContainer
                        val auditViewModel: com.example.presentation.audit.AuditViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                            factory = com.example.presentation.audit.AuditViewModelFactory(
                                auditSecurityLogDao = appContainer.securityDatabase.auditSecurityLogDao(),
                                assetAuditLogDao = appContainer.securityDatabase.assetAuditLogDao(),
                                financialDao = appContainer.securityDatabase.financialDao(),
                                fixedAssetDao = appContainer.securityDatabase.fixedAssetDao(),
                                userId = uiState.userEmail.ifBlank { "user_default" }
                            )
                        )
                        com.example.presentation.audit.AuditModuleScreen(
                            viewModel = auditViewModel,
                            onNavigateBack = { viewModel.navigateBackToMain() }
                        )
                    }
                    SettingsSubRoute.NOTIFICACIONES -> {
                        SettingsNotificationsScreen(
                            budgetAlerts = uiState.preferences.budgetAlertsEnabled,
                            paymentReminders = uiState.preferences.paymentRemindersEnabled,
                            weeklySummary = uiState.preferences.weeklySummaryEnabled,
                            onToggleBudgetAlerts = { viewModel.toggleBudgetAlerts(it) },
                            onTogglePaymentReminders = { viewModel.togglePaymentReminders(it) },
                            onShowMessage = { viewModel.showStatusMessage(it) },
                            onBack = { viewModel.navigateBackToMain() }
                        )
                    }
                    SettingsSubRoute.MONEDA_TRM -> {
                        val context = LocalContext.current
                        val appContainer = (context.applicationContext as KashAppApplication).appContainer
                        val trmViewModel: CurrencyTrmViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                            factory = CurrencyTrmViewModelFactory(
                                exchangeRateRepository = appContainer.exchangeRateRepository,
                                settingsRepository = appContainer.settingsRepository,
                                testSuite = appContainer.exchangeRateTestSuite
                            )
                        )
                        CurrencyTrmScreen(
                            viewModel = trmViewModel,
                            onBack = { viewModel.navigateBackToMain() }
                        )
                    }
                    SettingsSubRoute.APARIENCIA -> {
                        SettingsAppearanceSubScreen(
                            currentTheme = uiState.preferences.theme,
                            privacyMode = uiState.preferences.privacyModeEnabled,
                            onSelectTheme = { viewModel.setTheme(it) },
                            onTogglePrivacyMode = { viewModel.togglePrivacyMode() },
                            onBack = { viewModel.navigateBackToMain() }
                        )
                    }
                    SettingsSubRoute.CATEGORIAS -> {
                        SettingsCategoriesSubScreen(
                            onShowMessage = { viewModel.showStatusMessage(it) },
                            onBack = { viewModel.navigateBackToMain() }
                        )
                    }
                    SettingsSubRoute.CUENTAS_ENTIDADES -> {
                        SettingsAccountsSubScreen(
                            onShowMessage = { viewModel.showStatusMessage(it) },
                            onBack = { viewModel.navigateBackToMain() }
                        )
                    }
                    SettingsSubRoute.TIENDA -> {
                        SettingsMerchantsSubScreen(
                            onShowMessage = { viewModel.showStatusMessage(it) },
                            onBack = { viewModel.navigateBackToMain() }
                        )
                    }
                    SettingsSubRoute.PERSONALIZAR_INICIO -> {
                        SettingsDashboardLayoutSubScreen(
                            onShowMessage = { viewModel.showStatusMessage(it) },
                            onBack = { viewModel.navigateBackToMain() }
                        )
                    }
                    SettingsSubRoute.EXPORTAR_DATOS -> {
                        SettingsBackupScreen(
                            onShowMessage = { viewModel.showStatusMessage(it) },
                            onBack = { viewModel.navigateBackToMain() }
                        )
                    }
                    SettingsSubRoute.COPIA_SEGURIDAD -> {
                        SettingsBackupScreen(
                            onShowMessage = { viewModel.showStatusMessage(it) },
                            onBack = { viewModel.navigateBackToMain() }
                        )
                    }
                    SettingsSubRoute.AYUDA_SOPORTE -> {
                        SettingsHelpSubScreen(
                            onShowMessage = { viewModel.showStatusMessage(it) },
                            onBack = { viewModel.navigateBackToMain() }
                        )
                    }
                    SettingsSubRoute.ACERCA_DE -> {
                        SettingsAboutSubScreen(
                            onBack = { viewModel.navigateBackToMain() }
                        )
                    }
                }
            }
        }
    }

    // Modal: Logout Confirmation Dialog
    if (uiState.showLogoutConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showLogoutDialog(false) },
            title = {
                Text(
                    text = "¿Cerrar sesión de KashApp?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = KashNavy
                )
            },
            text = {
                Text(
                    text = "Tus datos locales están cifrados con Room ACID y protegidos. Para ingresar nuevamente deberás autenticarte.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.executeLogout(onLogoutSuccess) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cerrar sesión", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showLogoutDialog(false) }) {
                    Text("Cancelar", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Modal: Google Play Standard Account Deletion Dialog
    if (uiState.showDeleteAccountConfirmationDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isDeletingAccount) viewModel.showDeleteAccountDialog(false)
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEE2E2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Eliminar cuenta",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "¿Eliminar cuenta definitivamente?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFFDC2626)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Conforme a los estándares de Google Play y privacidad de datos, esta acción es permanente e irreversible.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KashNavy
                    )
                    Text(
                        text = "Al confirmar:\n• Se eliminarán tus datos financieros en la nube de Firebase.\n• Se borrarán tus cuentas, presupuestos y movimientos de este dispositivo.\n• Tu cuenta de autenticación será eliminada permanentemente.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.executeDeleteAccount(onAccountDeletedSuccess) },
                    enabled = !uiState.isDeletingAccount,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isDeletingAccount) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Eliminando cuenta...", color = Color.White)
                    } else {
                        Text("Eliminar definitivamente", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.showDeleteAccountDialog(false) },
                    enabled = !uiState.isDeletingAccount
                ) {
                    Text("Cancelar", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun SettingsTopBar(
    title: String,
    subtitle: String?,
    onBackClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 1.dp,
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
                    .padding(start = 12.dp, top = 16.dp, end = 12.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
                    .testTag("settings_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = KashNavy,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = KashNavy
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}
}
