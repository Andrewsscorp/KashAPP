package com.example.features.settings.presentation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.models.AuthMethod
import com.example.features.settings.domain.model.SettingsSubRoute
import com.example.features.settings.domain.model.UserPreferencesDomain
import com.example.features.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: UserPreferencesDomain = UserPreferencesDomain(),
    val currentSubRoute: SettingsSubRoute = SettingsSubRoute.MAIN,
    val isLoggingOut: Boolean = false,
    val showLogoutConfirmationDialog: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val showDeleteAccountConfirmationDialog: Boolean = false,
    val statusMessage: String? = null,
    val userName: String = "Usuario",
    val userEmail: String = "",
    val userPhone: String = "",
    val userIdDisplay: String = "",
    val photoUrl: String? = null,
    val authProvider: String = "Firebase Auth Seguro",
    val isEmailVerified: Boolean = false,
    val memberSince: String = "",
    val lastCloudBackupTimestamp: Long = 0L,
    val localAccountsCount: Int = 0,
    val localTransactionsCount: Int = 0,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val isExporting: Boolean = false,
    val showRestoreConfirmationDialog: Boolean = false
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val authRepository: com.example.domain.repository.AuthRepository? = null,
    private val financialRepository: com.example.domain.repository.FinancialRepository? = null,
    val userId: String = "user_default",
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observePreferences(userId).collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }
        viewModelScope.launch {
            loadRealUserProfile()
        }
        viewModelScope.launch(ioDispatcher) {
            refreshLocalCounts()
        }
        viewModelScope.launch(ioDispatcher) {
            financialRepository?.getAccounts(userId)?.collect { accounts ->
                _uiState.update { it.copy(localAccountsCount = accounts.size) }
            }
        }
        viewModelScope.launch(ioDispatcher) {
            financialRepository?.getTransactions(userId)?.collect { txs ->
                _uiState.update { it.copy(localTransactionsCount = txs.size) }
            }
        }
        viewModelScope.launch(ioDispatcher) {
            refreshCloudBackupMetadata()
        }
        viewModelScope.launch {
            authRepository?.getActiveSession()?.collect { session ->
                if (session != null) {
                    _uiState.update { current ->
                        current.copy(
                            userName = session.displayName?.takeIf { it.isNotBlank() } ?: current.userName,
                            userEmail = session.email?.takeIf { it.isNotBlank() } ?: current.userEmail,
                            userIdDisplay = session.userId,
                            photoUrl = session.photoUrl ?: current.photoUrl,
                            authProvider = session.authProvider,
                            isEmailVerified = session.isEmailVerified
                        )
                    }
                }
            }
        }
    }

    private fun loadRealUserProfile() {
        try {
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (firebaseUser != null) {
                val creationTime = firebaseUser.metadata?.creationTimestamp ?: 0L
                val creationFormatted = if (creationTime > 0) {
                    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(creationTime))
                } else "2026"

                val provider = firebaseUser.providerData.firstOrNull { it.providerId != "firebase" }?.providerId ?: "Firebase"
                val providerName = when (provider) {
                    "google.com" -> "Google Authentication"
                    "password" -> "Correo y Contraseña"
                    "phone" -> "Autenticación Telefónica"
                    else -> "Firebase Auth Seguro"
                }

                _uiState.update { current ->
                    current.copy(
                        userName = firebaseUser.displayName?.takeIf { it.isNotBlank() } ?: current.userName.ifBlank { "Usuario KashApp" },
                        userEmail = firebaseUser.email?.takeIf { it.isNotBlank() } ?: current.userEmail,
                        userPhone = firebaseUser.phoneNumber?.takeIf { it.isNotBlank() } ?: current.userPhone,
                        userIdDisplay = firebaseUser.uid,
                        photoUrl = firebaseUser.photoUrl?.toString() ?: current.photoUrl,
                        authProvider = providerName,
                        isEmailVerified = firebaseUser.isEmailVerified,
                        memberSince = creationFormatted
                    )
                }
            } else {
                viewModelScope.launch {
                    val localSession = authRepository?.getCurrentSession()
                    if (localSession != null) {
                        _uiState.update { current ->
                            current.copy(
                                userName = localSession.displayName?.takeIf { it.isNotBlank() } ?: "Usuario KashApp",
                                userEmail = localSession.email ?: "",
                                userIdDisplay = localSession.userId,
                                photoUrl = localSession.photoUrl,
                                authProvider = localSession.authProvider,
                                isEmailVerified = localSession.isEmailVerified
                            )
                        }
                    } else {
                        _uiState.update { current ->
                            current.copy(
                                userName = current.userName.ifBlank { "Usuario KashApp" },
                                userIdDisplay = "KASH-" + (kotlin.math.abs(userId.hashCode()) % 100000).toString().padStart(5, '0')
                            )
                        }
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    fun navigateToSubRoute(subRoute: SettingsSubRoute) {
        _uiState.update { it.copy(currentSubRoute = subRoute) }
    }

    fun navigateBackToMain() {
        _uiState.update { it.copy(currentSubRoute = SettingsSubRoute.MAIN) }
    }

    fun showLogoutDialog(show: Boolean) {
        _uiState.update { it.copy(showLogoutConfirmationDialog = show) }
    }

    fun togglePrivacyMode() {
        val current = _uiState.value.preferences.privacyModeEnabled
        viewModelScope.launch {
            repository.togglePrivacyMode(userId, !current)
        }
    }

    fun setBaseCurrency(currency: String) {
        viewModelScope.launch {
            repository.setBaseCurrency(userId, currency)
            _uiState.update { it.copy(statusMessage = "Moneda base cambiada a $currency") }
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            repository.setTheme(userId, theme)
        }
    }

    fun toggleBiometrics(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBiometricsEnabled(userId, enabled)
        }
    }

    fun toggleAllowScreenshots(allowed: Boolean) {
        viewModelScope.launch {
            repository.setAllowScreenshots(userId, allowed)
        }
    }

    fun toggleBudgetAlerts(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBudgetAlertsEnabled(userId, enabled)
        }
    }

    fun togglePaymentReminders(enabled: Boolean) {
        viewModelScope.launch {
            repository.setPaymentRemindersEnabled(userId, enabled)
        }
    }

    fun updateProfile(name: String, email: String, phone: String) {
        _uiState.update {
            it.copy(
                userName = name,
                userEmail = email,
                userPhone = phone,
                statusMessage = "Perfil actualizado con éxito"
            )
        }
    }

    fun showStatusMessage(message: String) {
        _uiState.update { it.copy(statusMessage = message) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun toggleAutoNoAffectInventory(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoNoAffectInventoryForAssets(userId, enabled)
            _uiState.update {
                it.copy(
                    statusMessage = if (enabled)
                        "Nuevos activos fijos se marcarán automáticamente como 'No afecta inventarios'"
                    else
                        "Nuevos activos fijos afectarán inventarios por defecto"
                )
            }
        }
    }

    fun applyNoAffectInventoryToAllAssets(noAffect: Boolean) {
        viewModelScope.launch {
            repository.applyNoAffectInventoryToAllAssets(userId, noAffect)
                .onSuccess { count ->
                    _uiState.update {
                        it.copy(
                            statusMessage = if (noAffect)
                                "Se aplicó 'No afecta inventarios' a $count activos fijos existentes"
                            else
                                "Se habilitó afectación de inventario para $count activos fijos"
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(statusMessage = "Error al actualizar activos: ${err.message}") }
                }
        }
    }

    fun toggleAutoNoAffectDashboard(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoNoAffectDashboardForAssets(userId, enabled)
            _uiState.update {
                it.copy(
                    statusMessage = if (enabled)
                        "Nuevos activos fijos se marcarán automáticamente como 'No afecta dashboard'"
                    else
                        "Nuevos activos fijos afectarán el dashboard por defecto"
                )
            }
        }
    }

    fun toggleExcludeAllFixedAssetsFromDashboard(excluded: Boolean) {
        viewModelScope.launch {
            repository.setExcludeAllFixedAssetsFromDashboard(userId, excluded)
            _uiState.update {
                it.copy(
                    statusMessage = if (excluded)
                        "Se excluyeron todos los activos fijos del Dashboard principal"
                    else
                        "Los activos fijos se incluyen en el Dashboard principal"
                )
            }
        }
    }

    fun applyNoAffectDashboardToAllAssets(noAffect: Boolean) {
        viewModelScope.launch {
            repository.applyNoAffectDashboardToAllAssets(userId, noAffect)
                .onSuccess { count ->
                    _uiState.update {
                        it.copy(
                            statusMessage = if (noAffect)
                                "Se aplicó 'No afecta dashboard' a $count activos fijos existentes"
                            else
                                "Se habilitó afectación al dashboard para $count activos fijos"
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(statusMessage = "Error al actualizar activos: ${err.message}") }
                }
        }
    }

    fun executeLogout(onLogoutSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoggingOut = true, showLogoutConfirmationDialog = false) }
        viewModelScope.launch {
            repository.executeProtocolLogout(userId)
            _uiState.update { it.copy(isLoggingOut = false) }
            onLogoutSuccess()
        }
    }

    suspend fun changePin(currentPin: String, newPin: String): Result<Unit> {
        val auth = authRepository ?: return Result.failure(IllegalStateException("Servicio de autenticación no disponible"))
        return auth.changePin(currentPin, newPin)
    }

    fun showDeleteAccountDialog(show: Boolean) {
        _uiState.update { it.copy(showDeleteAccountConfirmationDialog = show) }
    }

    fun executeDeleteAccount(onDeleteSuccess: () -> Unit) {
        _uiState.update { it.copy(isDeletingAccount = true, showDeleteAccountConfirmationDialog = false) }
        viewModelScope.launch {
            authRepository?.deleteAccountPermanently(userId)
            repository.executeProtocolLogout(userId)
            _uiState.update { it.copy(isDeletingAccount = false, statusMessage = "Cuenta eliminada permanentemente.") }
            onDeleteSuccess()
        }
    }

    fun refreshLocalCounts() {
        viewModelScope.launch(ioDispatcher) {
            try {
                val accounts = financialRepository?.getAccountsSnapshot(userId).orEmpty()
                val txs = financialRepository?.getTransactionsSnapshot(userId).orEmpty()
                _uiState.update {
                    it.copy(
                        localAccountsCount = accounts.size,
                        localTransactionsCount = txs.size
                    )
                }
            } catch (e: Exception) {
                Log.w("SettingsViewModel", "Error al refrescar conteos locales: ${e.message}")
            }
        }
    }

    fun refreshCloudBackupMetadata() {
        viewModelScope.launch(ioDispatcher) {
            try {
                val metadata = financialRepository?.getCloudBackupMetadata(userId)
                val ts = (metadata?.get("updatedAt") as? Number)?.toLong()
                    ?: (metadata?.get("lastSyncedAt") as? Number)?.toLong()
                    ?: 0L
                _uiState.update { it.copy(lastCloudBackupTimestamp = ts) }
            } catch (e: Exception) {
                Log.w("SettingsViewModel", "Error al refrescar metadatos de backup: ${e.message}")
            }
        }
    }

    fun executeCloudBackup() {
        val repo = financialRepository ?: run {
            _uiState.update { it.copy(statusMessage = "Repositorio financiero no disponible.") }
            return
        }
        _uiState.update { it.copy(isBackingUp = true) }
        viewModelScope.launch(ioDispatcher) {
            val result = repo.backupFullPortfolioToCloud(userId)
            _uiState.update { it.copy(isBackingUp = false) }
            result.onSuccess {
                val metadata = repo.getCloudBackupMetadata(userId)
                val ts = (metadata?.get("updatedAt") as? Number)?.toLong()
                    ?: (metadata?.get("lastSyncedAt") as? Number)?.toLong()
                    ?: System.currentTimeMillis()
                val accounts = repo.getAccountsSnapshot(userId)
                val txs = repo.getTransactionsSnapshot(userId)
                _uiState.update {
                    it.copy(
                        localAccountsCount = accounts.size,
                        localTransactionsCount = txs.size,
                        lastCloudBackupTimestamp = ts,
                        statusMessage = "¡Copia de seguridad en la nube (E2EE) creada exitosamente!"
                    )
                }
                try {
                    authRepository?.logSecurityEvent(
                        eventType = "CLOUD_BACKUP_EXECUTED",
                        method = AuthMethod.OTHER,
                        details = "Copia de seguridad en la nube (E2EE) creada exitosamente para el usuario $userId (${accounts.size} cuentas, ${txs.size} transacciones)."
                    )
                } catch (logEx: Exception) {
                    Log.w("SettingsViewModel", "No fue posible registrar log de auditoría para backup: ${logEx.message}")
                }
            }.onFailure { ex ->
                _uiState.update { it.copy(statusMessage = "Error al respaldar en la nube: ${ex.message ?: "Fallo de conexión"}") }
            }
        }
    }

    fun showRestoreConfirmation(show: Boolean) {
        _uiState.update { it.copy(showRestoreConfirmationDialog = show) }
    }

    fun executeCloudRestore() {
        val repo = financialRepository ?: run {
            _uiState.update { it.copy(statusMessage = "Repositorio financiero no disponible.", showRestoreConfirmationDialog = false) }
            return
        }
        _uiState.update { it.copy(isRestoring = true, showRestoreConfirmationDialog = false) }
        viewModelScope.launch(ioDispatcher) {
            val result = repo.restoreFullPortfolioFromCloud(userId)
            _uiState.update { it.copy(isRestoring = false) }
            result.onSuccess { restored ->
                if (restored) {
                    val metadata = repo.getCloudBackupMetadata(userId)
                    val ts = (metadata?.get("updatedAt") as? Number)?.toLong()
                        ?: (metadata?.get("lastSyncedAt") as? Number)?.toLong()
                        ?: System.currentTimeMillis()
                    val accounts = repo.getAccountsSnapshot(userId)
                    val txs = repo.getTransactionsSnapshot(userId)
                    _uiState.update {
                        it.copy(
                            localAccountsCount = accounts.size,
                            localTransactionsCount = txs.size,
                            lastCloudBackupTimestamp = ts,
                            statusMessage = "¡Copia de seguridad restaurada exitosamente desde la nube!"
                        )
                    }
                } else {
                    _uiState.update { it.copy(statusMessage = "No se encontró ningún respaldo en la nube para restaurar.") }
                }
            }.onFailure { ex ->
                _uiState.update { it.copy(statusMessage = "Error al restaurar desde la nube: ${ex.message ?: "Fallo de conexión"}") }
            }
        }
    }

    fun exportFinancialData(format: String, period: String, context: Context) {
        val repo = financialRepository ?: run {
            _uiState.update { it.copy(statusMessage = "Repositorio financiero no disponible.") }
            return
        }
        _uiState.update { it.copy(isExporting = true) }
        viewModelScope.launch(ioDispatcher) {
            try {
                val accounts = repo.getAccountsSnapshot(userId)
                val transactions = repo.getTransactionsSnapshot(userId)

                val now = java.time.LocalDate.now()
                val filteredTxs = when (period) {
                    "Este mes" -> {
                        val currentPrefix = "${now.year}-${now.monthValue.toString().padStart(2, '0')}"
                        transactions.filter { it.date.startsWith(currentPrefix) }
                    }
                    "Últimos 3 meses" -> {
                        val threeMonthsAgo = now.minusMonths(3)
                        transactions.filter {
                            try {
                                java.time.LocalDate.parse(it.date).isAfter(threeMonthsAgo)
                            } catch (_: Exception) { true }
                        }
                    }
                    "Año 2026" -> transactions.filter { it.date.startsWith("2026") }
                    else -> transactions
                }

                when (format.uppercase()) {
                    "CSV", "EXCEL (.XLSX)" -> {
                        val items = accounts.map { acc ->
                            val accTxs = filteredTxs.filter { it.accountId == acc.id }
                            val movements = accTxs.map { tx ->
                                com.example.domain.models.AccountMovementDisplay(
                                    id = tx.id,
                                    title = tx.description ?: tx.categoryName,
                                    category = tx.categoryName,
                                    dateFormatted = tx.date,
                                    amount = tx.amount,
                                    percentageOfTotal = 0.0,
                                    details = tx.type
                                )
                            }
                            com.example.domain.models.RubroItemDisplay(
                                id = acc.id,
                                name = acc.name,
                                institution = acc.bankName,
                                category = acc.type,
                                currency = acc.currency,
                                amount = java.math.BigDecimal.valueOf(acc.balance),
                                status = "Activa",
                                accountMovements = movements
                            )
                        }
                        val totalFormatted = "$ ${java.text.DecimalFormat("#,##0").format(accounts.sumOf { it.balance })}"
                        val result = com.example.util.FinancialReportExporter.exportToExcelCsv(
                            context = context,
                            rubroType = com.example.domain.models.FinancialRubroType.AVAILABLE_CASH,
                            totalAmountFormatted = totalFormatted,
                            items = items,
                            appliedFiltersDesc = "Período: $period"
                        )
                        _uiState.update { it.copy(isExporting = false, statusMessage = result.message) }
                        if (result.success) {
                            try {
                                authRepository?.logSecurityEvent(
                                    eventType = "DATA_EXPORT_PDF_CSV",
                                    method = AuthMethod.OTHER,
                                    details = "Exportación de datos financieros formato $format para período $period completada."
                                )
                            } catch (_: Exception) {}
                        }
                    }
                    "PDF" -> {
                        val items = accounts.map { acc ->
                            val accTxs = filteredTxs.filter { it.accountId == acc.id }
                            val movements = accTxs.map { tx ->
                                com.example.domain.models.AccountMovementDisplay(
                                    id = tx.id,
                                    title = tx.description ?: tx.categoryName,
                                    category = tx.categoryName,
                                    dateFormatted = tx.date,
                                    amount = tx.amount,
                                    percentageOfTotal = 0.0,
                                    details = tx.type
                                )
                            }
                            com.example.domain.models.RubroItemDisplay(
                                id = acc.id,
                                name = acc.name,
                                institution = acc.bankName,
                                category = acc.type,
                                currency = acc.currency,
                                amount = java.math.BigDecimal.valueOf(acc.balance),
                                status = "Activa",
                                accountMovements = movements
                            )
                        }
                        val totalFormatted = "$ ${java.text.DecimalFormat("#,##0").format(accounts.sumOf { it.balance })}"
                        val result = com.example.util.FinancialReportExporter.exportToPdf(
                            context = context,
                            rubroType = com.example.domain.models.FinancialRubroType.AVAILABLE_CASH,
                            totalAmountFormatted = totalFormatted,
                            percentageNetWorth = 100.0,
                            items = items,
                            appliedFiltersDesc = "Período: $period"
                        )
                        _uiState.update { it.copy(isExporting = false, statusMessage = result.message) }
                        if (result.success) {
                            try {
                                authRepository?.logSecurityEvent(
                                    eventType = "DATA_EXPORT_PDF_CSV",
                                    method = AuthMethod.OTHER,
                                    details = "Exportación de datos financieros formato PDF para período $period completada."
                                )
                            } catch (_: Exception) {}
                        }
                    }
                    "JSON" -> {
                        val json = org.json.JSONObject().apply {
                            put("appName", "KashApp")
                            put("version", "9.3.5")
                            put("userId", userId)
                            put("period", period)
                            put("exportedAt", System.currentTimeMillis())
                            put("accountsCount", accounts.size)
                            put("transactionsCount", filteredTxs.size)
                            val accArr = org.json.JSONArray()
                            accounts.forEach { acc ->
                                accArr.put(org.json.JSONObject().apply {
                                    put("id", acc.id)
                                    put("name", acc.name)
                                    put("balance", acc.balance)
                                    put("currency", acc.currency)
                                    put("bankName", acc.bankName)
                                    put("type", acc.type)
                                })
                            }
                            put("accounts", accArr)
                            val txArr = org.json.JSONArray()
                            filteredTxs.forEach { tx ->
                                txArr.put(org.json.JSONObject().apply {
                                    put("id", tx.id)
                                    put("accountId", tx.accountId)
                                    put("amount", tx.amount)
                                    put("currency", tx.currency)
                                    put("type", tx.type)
                                    put("date", tx.date)
                                    put("description", tx.description ?: "")
                                    put("categoryName", tx.categoryName)
                                })
                            }
                            put("transactions", txArr)
                        }
                        val fileName = "KashApp_Export_${System.currentTimeMillis()}.json"
                        val file = java.io.File(com.example.util.SharedExportSecurityUtil.getExportsDir(context), fileName)
                        file.writeText(json.toString(2))
                        val uri = try {
                            androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        } catch (_: Throwable) {
                            android.net.Uri.fromFile(file)
                        }
                        com.example.util.FinancialReportExporter.shareExportedFile(context, uri, "application/json", "Exportación JSON KashApp")
                        _uiState.update { it.copy(isExporting = false, statusMessage = "Archivo JSON generado y compartido exitosamente.") }
                        try {
                            authRepository?.logSecurityEvent(
                                eventType = "DATA_EXPORT_PDF_CSV",
                                method = AuthMethod.OTHER,
                                details = "Exportación de datos financieros formato JSON para período $period completada."
                            )
                        } catch (_: Exception) {}
                    }
                    else -> {
                        _uiState.update { it.copy(isExporting = false, statusMessage = "Formato no soportado.") }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, statusMessage = "Error al exportar reporte: ${e.message}") }
            }
        }
    }

    class Factory(
        private val repository: SettingsRepository,
        private val authRepository: com.example.domain.repository.AuthRepository? = null,
        private val financialRepository: com.example.domain.repository.FinancialRepository? = null,
        private val userId: String = "user_default"
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository, authRepository, financialRepository, userId) as T
        }
    }
}
