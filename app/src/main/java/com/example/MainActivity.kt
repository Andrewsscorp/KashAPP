package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Scaffold
import com.example.core.ui.components.IosStyleSnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.presentation.dashboard.DashboardScreen
import com.example.presentation.dashboard.DashboardViewModel
import com.example.presentation.dashboard.DashboardViewModelFactory
import com.example.presentation.login.LoginScreen
import com.example.presentation.login.LoginViewModel
import android.view.WindowManager
import com.example.presentation.login.LoginViewModelFactory
import com.example.ui.theme.KashAppTheme
import com.example.util.SharedExportSecurityUtil

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // HALL-07 / GATE-07 (REM-P1-04): Prevent screen captures and task preview leakage
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        SharedExportSecurityUtil.purgeStaleExports(applicationContext)
        enableEdgeToEdge()

        try {
            val appContainer = (application as KashAppApplication).appContainer

            setContent {
                KashAppTheme {
                    MainAppContent(appContainer)
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "Fatal initialization error in MainActivity: ${t.message}", t)
            setContent {
                KashAppTheme {
                    AppStartupRecoveryScreen(
                        errorMessage = t.localizedMessage ?: "Error al inicializar la aplicación.",
                        onRetry = {
                            recreate()
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun MainAppContent(appContainer: com.example.di.AppContainer) {
        val viewModel: LoginViewModel = viewModel(
            factory = LoginViewModelFactory(
                authenticateUserUseCase = appContainer.authenticateUserUseCase,
                authRepository = appContainer.authRepository,
                googleAuthDataSource = appContainer.googleAuthDataSource,
                biometricDataSource = appContainer.biometricDataSource
            )
        )

        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val activeUserId = uiState.authenticatedUser?.userId ?: "user_default"
        val userPreferences by remember(activeUserId) {
            appContainer.settingsRepository.observePreferences(activeUserId)
        }.collectAsStateWithLifecycle(initialValue = com.example.features.settings.domain.model.UserPreferencesDomain(userId = activeUserId))

        LaunchedEffect(userPreferences.allowScreenshots) {
            if (userPreferences.allowScreenshots) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            }
        }

        LaunchedEffect(uiState.statusMessage) {
            uiState.statusMessage?.let { msg ->
                snackbarHostState.showSnackbar(msg)
                viewModel.clearStatusMessage()
            }
        }

        // Automatic Fast Unlock on Startup: prompt for fingerprint when configured
        LaunchedEffect(uiState.autoPromptBiometricTrigger) {
            if (uiState.autoPromptBiometricTrigger > 0L && uiState.authenticatedUser == null && !uiState.isLockedOut) {
                viewModel.consumeAutoPromptBiometric()
                viewModel.onBiometricClicked(this@MainActivity)
            }
        }

        val vaultError = com.example.data.local.RoomLocalDb.vaultUnlockErrorMessage
        if (vaultError != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { com.example.data.local.RoomLocalDb.clearVaultUnlockError() },
                icon = {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Alerta de bóveda",
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.error
                    )
                },
                title = {
                    androidx.compose.material3.Text("Bóveda de Seguridad Cifrada")
                },
                text = {
                    androidx.compose.material3.Text(vaultError)
                },
                confirmButton = {
                    androidx.compose.material3.Button(
                        onClick = {
                            com.example.data.local.RoomLocalDb.clearVaultUnlockError()
                        }
                    ) {
                        androidx.compose.material3.Text("Aceptar")
                    }
                }
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { IosStyleSnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            val user = uiState.authenticatedUser
            if (user != null) {
                BackHandler {
                    viewModel.lockSession()
                    moveTaskToBack(true)
                }
                val dashboardViewModel: DashboardViewModel = viewModel(
                    key = user.userId,
                    factory = DashboardViewModelFactory(
                        financialRepository = appContainer.financialRepository,
                        exchangeRateRepository = appContainer.exchangeRateRepository,
                        settingsRepository = appContainer.settingsRepository,
                        fixedAssetRepository = appContainer.fixedAssetRepository,
                        userSessionDao = appContainer.securityDatabase.userSessionDao(),
                        userId = user.userId,
                        initialUserName = user.displayName?.takeIf { it.isNotBlank() } ?: "Andrés Felipe",
                        initialUserEmail = user.email?.takeIf { it.isNotBlank() } ?: "cdosuarez@gmail.com"
                    )
                )
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onLogoutClick = { viewModel.onLogout() },
                    modifier = Modifier.padding(innerPadding)
                )
            } else {
                LoginScreen(
                    uiState = uiState,
                    onGoogleSignInClick = {
                        viewModel.onGoogleSignInClicked(this@MainActivity)
                    },
                    onBiometricClick = {
                        viewModel.onBiometricClicked(this@MainActivity)
                    },
                    onPinClick = {
                        viewModel.onPinClicked()
                    },
                    onPinSubmit = { pin ->
                        viewModel.onPinSubmitted(pin)
                    },
                    onSetupPin = { pin ->
                        viewModel.setupNewPin(pin)
                    },
                    onDismissPinDialog = {
                        viewModel.dismissPinDialog()
                    },
                    onSwitchAccount = {
                        viewModel.onSwitchAccount()
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    @Composable
    private fun AppStartupRecoveryScreen(
        errorMessage: String,
        onRetry: () -> Unit
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.Card(
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Shield,
                        contentDescription = "Seguridad",
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    androidx.compose.material3.Text(
                        text = "KashApp - Recuperación",
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    androidx.compose.material3.Text(
                        text = "Se ha detectado una actualización del sistema. Presiona el botón para restaurar y abrir la aplicación con seguridad.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.material3.Button(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        androidx.compose.material3.Text("Abrir KashApp")
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // HALL-07 / GATE-07 (REM-P1-04): Purge remaining unencrypted shared exports on activity exit
        SharedExportSecurityUtil.purgeAllExports(applicationContext)
    }
}
