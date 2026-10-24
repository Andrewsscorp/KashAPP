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
import androidx.compose.foundation.layout.widthIn
import com.example.core.ui.adaptive.rememberKashAdaptiveInfo
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import com.example.core.ui.components.IosStyleSnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.core.ui.components.IosStyleConfirmDialog
import com.example.core.ui.components.IosStyleSnackbarHost
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
import com.example.features.settings.presentation.screens.SettingsFixedAssetsSubScreen
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
    val adaptive = rememberKashAdaptiveInfo()

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
        snackbarHost = { IosStyleSnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            if (uiState.currentSubRoute != SettingsSubRoute.AUDITORIA) {
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
            }
        },
        bottomBar = {
            if (uiState.currentSubRoute != SettingsSubRoute.AUDITORIA) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.fillMaxWidth().widthIn(max = adaptive.maxContentWidth)) {
                        SettingsBottomNavBar(
                            onHomeClick = onNavigateToHome,
                            onTransactionsClick = onNavigateToTransactions,
                            onFabClick = onFabAddClick,
                            onSummaryClick = onNavigateToSummary,
                            onSettingsClick = { viewModel.navigateBackToMain() }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = adaptive.maxContentWidth)
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
                            autoNoAffectDashboardForAssets = uiState.preferences.autoNoAffectDashboardForAssets,
                            excludeAllFixedAssetsFromDashboard = uiState.preferences.excludeAllFixedAssetsFromDashboard,
                            onToggleAutoNoAffectInventory = { viewModel.toggleAutoNoAffectInventory(it) },
                            onApplyNoAffectInventoryToAll = { viewModel.applyNoAffectInventoryToAllAssets(it) },
                            onToggleAutoNoAffectDashboard = { viewModel.toggleAutoNoAffectDashboard(it) },
                            onToggleExcludeAllFixedAssetsFromDashboard = { viewModel.toggleExcludeAllFixedAssetsFromDashboard(it) },
                            onApplyNoAffectDashboardToAll = { viewModel.applyNoAffectDashboardToAllAssets(it) },
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
                            userIdDisplay = uiState.userIdDisplay,
                            authProvider = uiState.authProvider,
                            isEmailVerified = uiState.isEmailVerified,
                            memberSince = uiState.memberSince,
                            onSaveProfile = { newName, newEmail, newPhone ->
                                viewModel.updateProfile(newName, newEmail, newPhone)
                            },
                            onBack = { viewModel.navigateBackToMain() }
                        )
                    }
                    SettingsSubRoute.SEGURIDAD -> {
                        val ctx = LocalContext.current
                        val appContainer = (ctx.applicationContext as KashAppApplication).appContainer
                        SettingsSecurityScreen(
                            userId = viewModel.userId,
                            biometricsEnabled = uiState.preferences.biometricsEnabled,
                            onToggleBiometrics = { viewModel.toggleBiometrics(it) },
                            allowScreenshots = uiState.preferences.allowScreenshots,
                            onToggleAllowScreenshots = { viewModel.toggleAllowScreenshots(it) },
                            authRepository = appContainer.authRepository,
                            onShowMessage = { viewModel.showStatusMessage(it) },
                            onChangePin = { current, new -> viewModel.changePin(current, new) },
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
                                userId = viewModel.userId
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
                            key = "currency_trm_vm_${viewModel.userId}",
                            factory = CurrencyTrmViewModelFactory(
                                exchangeRateRepository = appContainer.exchangeRateRepository,
                                settingsRepository = appContainer.settingsRepository,
                                testSuite = appContainer.exchangeRateTestSuite,
                                userId = viewModel.userId
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
                            onBack = { viewModel.navigateBackToMain() },
                            userId = viewModel.userId
                        )
                    }
                    SettingsSubRoute.TIENDA -> {
                        SettingsMerchantsSubScreen(
                            onShowMessage = { viewModel.showStatusMessage(it) },
                            onBack = { viewModel.navigateBackToMain() },
                            onNavigateToTerceros = { viewModel.navigateToSubRoute(SettingsSubRoute.TERCEROS) }
                        )
                    }
                    SettingsSubRoute.TERCEROS -> {
                        val ctx = LocalContext.current
                        val appContainer = (ctx.applicationContext as KashAppApplication).appContainer
                        val thirdPartyViewModel: com.example.presentation.thirdparty.ThirdPartyViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                            factory = com.example.presentation.thirdparty.ThirdPartyViewModel.Factory(
                                thirdPartyRepository = appContainer.thirdPartyRepository,
                                fixedAssetDao = appContainer.securityDatabase.fixedAssetDao(),
                                userId = viewModel.userId
                            )
                        )
                        com.example.features.settings.presentation.screens.SettingsThirdPartiesSubScreen(
                            viewModel = thirdPartyViewModel,
                            onBack = { viewModel.navigateBackToMain() },
                            onShowMessage = { viewModel.showStatusMessage(it) }
                        )
                    }
                    SettingsSubRoute.ACTIVOS_FIJOS -> {
                        SettingsFixedAssetsSubScreen(
                            autoNoAffectInventoryForAssets = uiState.preferences.autoNoAffectInventoryForAssets,
                            autoNoAffectDashboardForAssets = uiState.preferences.autoNoAffectDashboardForAssets,
                            excludeAllFixedAssetsFromDashboard = uiState.preferences.excludeAllFixedAssetsFromDashboard,
                            onToggleAutoNoAffectInventory = { viewModel.toggleAutoNoAffectInventory(it) },
                            onApplyNoAffectInventoryToAll = { viewModel.applyNoAffectInventoryToAllAssets(it) },
                            onToggleAutoNoAffectDashboard = { viewModel.toggleAutoNoAffectDashboard(it) },
                            onToggleExcludeAllFixedAssetsFromDashboard = { viewModel.toggleExcludeAllFixedAssetsFromDashboard(it) },
                            onApplyNoAffectDashboardToAll = { viewModel.applyNoAffectDashboardToAllAssets(it) },
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
                            viewModel = viewModel,
                            onShowMessage = { viewModel.showStatusMessage(it) },
                            onBack = { viewModel.navigateBackToMain() }
                        )
                    }
                    SettingsSubRoute.COPIA_SEGURIDAD -> {
                        SettingsBackupScreen(
                            viewModel = viewModel,
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
}

    // Modal: Logout Confirmation Dialog (Estilo iPhone / iOS)
    if (uiState.showLogoutConfirmationDialog) {
        IosStyleConfirmDialog(
            title = "¿Cerrar sesión de KashApp?",
            message = "Tus datos locales están cifrados con Room ACID y protegidos. Para ingresar nuevamente deberás autenticarte.",
            confirmText = "Cerrar sesión",
            dismissText = "Cancelar",
            onConfirm = { viewModel.executeLogout(onLogoutSuccess) },
            onDismiss = { viewModel.showLogoutDialog(false) },
            icon = Icons.AutoMirrored.Filled.Logout,
            iconTint = Color(0xFFEF4444),
            iconBackground = Color(0xFFFEF2F2),
            isDestructive = true
        )
    }

    // Modal: Google Play Standard Account Deletion Dialog (Estilo iPhone / iOS)
    if (uiState.showDeleteAccountConfirmationDialog) {
        IosStyleConfirmDialog(
            title = "¿Eliminar cuenta definitivamente?",
            message = "Conforme a los estándares de Google Play y privacidad de datos, esta acción es permanente e irreversible.\n\n• Se eliminarán tus datos en Firebase.\n• Se borrarán tus cuentas y movimientos locales.\n• Tu cuenta será eliminada permanentemente.",
            confirmText = "Eliminar cuenta",
            dismissText = "Cancelar",
            onConfirm = { viewModel.executeDeleteAccount(onAccountDeletedSuccess) },
            onDismiss = { if (!uiState.isDeletingAccount) viewModel.showDeleteAccountDialog(false) },
            icon = Icons.Default.DeleteForever,
            iconTint = Color(0xFFDC2626),
            iconBackground = Color(0xFFFEE2E2),
            isDestructive = true,
            isLoading = uiState.isDeletingAccount
        )
    }
}

@Composable
private fun SettingsTopBar(
    title: String,
    subtitle: String?,
    onBackClick: () -> Unit
) {
    val adaptive = rememberKashAdaptiveInfo()
    Surface(
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = adaptive.maxContentWidth)
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
                            .size(44.dp)
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
}
