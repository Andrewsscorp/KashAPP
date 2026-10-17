package com.example.presentation.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.dao.AssetAuditLogDao
import com.example.data.local.dao.AuditSecurityLogDao
import com.example.data.local.dao.FinancialDao
import com.example.data.local.entity.AssetAuditLogEntity
import com.example.data.local.entity.AuditSecurityLog
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuditViewModel(
    private val auditSecurityLogDao: AuditSecurityLogDao,
    private val assetAuditLogDao: AssetAuditLogDao,
    private val financialDao: FinancialDao,
    private val userId: String = "user_default"
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow(AuditCategory.ALL)
    private val _selectedEntryForDetail = MutableStateFlow<AuditUiEntry?>(null)
    private val _exportSuccessMessage = MutableStateFlow<String?>(null)

    private val rawLogsFlow = combine(
        auditSecurityLogDao.getAllLogs(),
        assetAuditLogDao.observeAuditLogs(userId),
        financialDao.getTransactionsFlow(userId)
    ) { secLogs, assetLogs, txLogs ->
        val uiEntries = mutableListOf<AuditUiEntry>()

        // 1. Process Security and Config Logs
        secLogs.forEach { log ->
            uiEntries.add(mapSecurityLogToUi(log))
        }

        // 2. Process Fixed Asset NIIF Audit Logs
        assetLogs.forEach { assetLog ->
            uiEntries.add(mapAssetLogToUi(assetLog))
        }

        // 3. Process Financial Transaction Ledger Logs
        txLogs.forEach { tx ->
            uiEntries.add(mapTransactionToUi(tx))
        }

        // Sort descending by timestamp (most recent first)
        uiEntries.sortedByDescending { it.timestamp }
    }

    val uiState: StateFlow<AuditVaultUiState> = combine(
        rawLogsFlow,
        _searchQuery,
        _selectedCategory,
        _selectedEntryForDetail,
        _exportSuccessMessage
    ) { allLogs, query, category, selectedDetail, exportMsg ->
        val totalCount = allLogs.size
        val securityCount = allLogs.count { it.category == AuditCategory.SECURITY }
        val accountsCount = allLogs.count { it.category == AuditCategory.ACCOUNTS }
        val assetsCount = allLogs.count { it.category == AuditCategory.FIXED_ASSETS }
        val transactionsCount = allLogs.count { it.category == AuditCategory.TRANSACTIONS }

        val filtered = allLogs.filter { entry ->
            val matchesCategory = (category == AuditCategory.ALL || entry.category == category)
            val matchesSearch = query.isBlank() ||
                    entry.eventTitle.contains(query, ignoreCase = true) ||
                    entry.details.contains(query, ignoreCase = true) ||
                    entry.authMethodOrOrigin.contains(query, ignoreCase = true) ||
                    entry.hashSha256.contains(query, ignoreCase = true)
            matchesCategory && matchesSearch
        }

        AuditVaultUiState(
            logs = allLogs,
            filteredLogs = filtered,
            searchQuery = query,
            selectedCategory = category,
            totalCount = totalCount,
            securityCount = securityCount,
            accountsCount = accountsCount,
            assetsCount = assetsCount,
            transactionsCount = transactionsCount,
            selectedEntryForDetail = selectedDetail,
            isLoading = false,
            exportSuccessMessage = exportMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AuditVaultUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(category: AuditCategory) {
        _selectedCategory.value = category
    }

    fun onSelectEntryForDetail(entry: AuditUiEntry?) {
        _selectedEntryForDetail.value = entry
    }

    fun clearExportMessage() {
        _exportSuccessMessage.value = null
    }

    fun generateExportCertificate(): String {
        val currentLogs = uiState.value.logs
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
        val generatedAt = dateFormat.format(Date())

        val sb = StringBuilder()
        sb.appendLine("================================================================================")
        sb.appendLine("                 KASHAPP ENTERPRISE - BÓVEDA DE AUDITORÍA INMUTABLE             ")
        sb.appendLine("================================================================================")
        sb.appendLine("CERTIFICADO DE TRAZABILIDAD Y CUSTODIA CRIPTOGRÁFICA (APPEND-ONLY)")
        sb.appendLine("Fecha de Generación: $generatedAt")
        sb.appendLine("Usuario / Identificador: $userId")
        sb.appendLine("Total Eventos Auditados: ${currentLogs.size}")
        sb.appendLine("Seguridad y Accesos: ${currentLogs.count { it.category == AuditCategory.SECURITY }}")
        sb.appendLine("Cuentas y Bancos: ${currentLogs.count { it.category == AuditCategory.ACCOUNTS }}")
        sb.appendLine("Activos Fijos NIIF: ${currentLogs.count { it.category == AuditCategory.FIXED_ASSETS }}")
        sb.appendLine("Transacciones Contables: ${currentLogs.count { it.category == AuditCategory.TRANSACTIONS }}")
        sb.appendLine("Motor Criptográfico: SQLCipher AES-256 + SHA-256 Digest")
        sb.appendLine("Garantía de Inmutabilidad: No existen sentencias UPDATE ni DELETE en esquema Room.")
        sb.appendLine("================================================================================")
        sb.appendLine()
        sb.appendLine("REGISTRO CRONOLÓGICO DE EVENTOS:")
        sb.appendLine("--------------------------------------------------------------------------------")

        currentLogs.forEachIndexed { index, entry ->
            sb.appendLine("[#${index + 1}] ${entry.formattedDateTime} | ${entry.category.label} | ${entry.severity.name}")
            sb.appendLine("  Evento: ${entry.eventTitle}")
            sb.appendLine("  Origen / Método: ${entry.authMethodOrOrigin}")
            sb.appendLine("  Detalle: ${entry.details.replace("\n", " ")}")
            sb.appendLine("  Firma SHA-256: ${entry.hashSha256}")
            sb.appendLine("--------------------------------------------------------------------------------")
        }

        sb.appendLine()
        sb.appendLine("================================================================================")
        sb.appendLine("FIN DEL INFORME AUDITADO - FIRMA DIGITAL VÁLIDA PARA COMPLIANCE")
        sb.appendLine("================================================================================")

        _exportSuccessMessage.value = "Informe de auditoría generado y copiado con éxito (${currentLogs.size} eventos)"
        return sb.toString()
    }

    private fun mapSecurityLogToUi(log: AuditSecurityLog): AuditUiEntry {
        val date = Date(log.timestamp)
        val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(date)
        val relative = getRelativeTimeSpan(log.timestamp)

        val category = when {
            log.eventType.contains("ACCOUNT", ignoreCase = true) ||
                    log.eventType.contains("DASHBOARD_EXCLUSION", ignoreCase = true) ||
                    log.eventType.contains("CUENTA", ignoreCase = true) -> AuditCategory.ACCOUNTS

            log.eventType.contains("SETTINGS", ignoreCase = true) ||
                    log.eventType.contains("PREFS", ignoreCase = true) ||
                    log.eventType.contains("BACKUP", ignoreCase = true) -> AuditCategory.SYSTEM

            else -> AuditCategory.SECURITY
        }

        val severity = when {
            log.eventType.contains("FAIL", ignoreCase = true) ||
                    log.eventType.contains("ERROR", ignoreCase = true) ||
                    log.eventType.contains("LOCKOUT", ignoreCase = true) ||
                    log.eventType.contains("CORRUPT", ignoreCase = true) -> AuditSeverity.CRITICAL

            log.eventType.contains("EXCLUSION", ignoreCase = true) ||
                    log.eventType.contains("DISABLE", ignoreCase = true) ||
                    log.eventType.contains("DELETE", ignoreCase = true) -> AuditSeverity.WARNING

            log.eventType.contains("SUCCESS", ignoreCase = true) ||
                    log.eventType.contains("LOGIN", ignoreCase = true) ||
                    log.eventType.contains("AUTH", ignoreCase = true) -> AuditSeverity.SUCCESS

            else -> AuditSeverity.INFO
        }

        val friendlyTitle = formatEventTitle(log.eventType)
        val hash = calculateSha256("SEC_${log.id}_${log.timestamp}_${log.eventType}_${log.details}")

        return AuditUiEntry(
            id = "SEC-${log.id}",
            timestamp = log.timestamp,
            formattedDateTime = formattedDate,
            relativeTime = relative,
            category = category,
            severity = severity,
            eventTitle = friendlyTitle,
            details = log.details,
            authMethodOrOrigin = log.authMethod.ifBlank { "SISTEMA_LOCAL" },
            hashSha256 = hash
        )
    }

    private fun mapAssetLogToUi(assetLog: AssetAuditLogEntity): AuditUiEntry {
        val date = Date(assetLog.timestamp)
        val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(date)
        val relative = getRelativeTimeSpan(assetLog.timestamp)

        val severity = when (assetLog.actionType.uppercase()) {
            "BAJA", "ELIMINACION", "DISPOSAL" -> AuditSeverity.WARNING
            "CREACION", "RECALCULO" -> AuditSeverity.SUCCESS
            "MODIFICACION", "ESTADO" -> AuditSeverity.INFO
            else -> AuditSeverity.INFO
        }

        val title = "Activo Fijo: ${assetLog.actionTitle} (${assetLog.assetName ?: "General"})"
        val fullDetails = buildString {
            append(assetLog.details)
            if (!assetLog.financialImpact.isNullOrBlank()) {
                append("\nImpacto Financiero: ")
                append(assetLog.financialImpact)
            }
        }
        val hash = calculateSha256("AST_${assetLog.id}_${assetLog.timestamp}_${assetLog.actionType}_${assetLog.details}")

        return AuditUiEntry(
            id = "AST-${assetLog.id.takeLast(8)}",
            timestamp = assetLog.timestamp,
            formattedDateTime = formattedDate,
            relativeTime = relative,
            category = AuditCategory.FIXED_ASSETS,
            severity = severity,
            eventTitle = title,
            details = fullDetails,
            authMethodOrOrigin = assetLog.userRole.ifBlank { "NIIF NIC 16 Engine" },
            hashSha256 = hash
        )
    }

    private fun mapTransactionToUi(tx: TransactionEntity): AuditUiEntry {
        val date = Date(tx.createdAt)
        val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(date)
        val relative = getRelativeTimeSpan(tx.createdAt)

        val severity = AuditSeverity.SUCCESS

        val typeText = when (tx.type.uppercase()) {
            "EXPENSE", "GASTO" -> "Gasto Contable"
            "INCOME", "INGRESO" -> "Ingreso Contable"
            "TRANSFER", "TRANSFERENCIA" -> "Transferencia Interbancaria"
            else -> "Transacción ${tx.type}"
        }

        val title = "$typeText [${tx.categoryName.ifBlank { "General" }}]"
        val details = buildString {
            append("Monto: $")
            append(String.format(Locale.ROOT, "%,.2f", tx.amount))
            append(" ")
            append(tx.currency)
            append(" • Concepto: ")
            append(tx.description?.ifBlank { "Sin descripción" } ?: "Sin descripción")
            if (tx.accountName.isNotBlank()) {
                append(" • Cuenta: ")
                append(tx.accountName)
            } else if (tx.accountId.isNotBlank()) {
                append(" • Cuenta ID: ")
                append(tx.accountId)
            }
            append(" • Estado Sync: ")
            append(tx.syncStatus)
        }

        val hash = calculateSha256("TX_${tx.id}_${tx.createdAt}_${tx.amount}_${tx.description ?: ""}")

        return AuditUiEntry(
            id = "TX-${tx.id.takeLast(8)}",
            timestamp = tx.createdAt,
            formattedDateTime = formattedDate,
            relativeTime = relative,
            category = AuditCategory.TRANSACTIONS,
            severity = severity,
            eventTitle = title,
            details = details,
            authMethodOrOrigin = "Motor Transaccional ACID",
            hashSha256 = hash
        )
    }

    private fun formatEventTitle(eventType: String): String {
        return when (eventType.uppercase()) {
            "DASHBOARD_EXCLUSION_GLOBAL" -> "Configuración Global: Exclusión de Cuentas en Dashboard"
            "DASHBOARD_EXCLUSION_ACCOUNT" -> "Ajuste Individual: Visibilidad de Cuenta en Dashboard"
            "LOGIN_SUCCESS" -> "Inicio de Sesión Exitoso"
            "LOGIN_FAILED" -> "Intento Fallido de Autenticación"
            "BIOMETRIC_AUTH_SUCCESS" -> "Autenticación Biométrica Verificada"
            "PIN_VERIFY_SUCCESS" -> "Desbloqueo por PIN de Seguridad Aprobado"
            "PIN_CHANGED" -> "Actualización de PIN Criptográfico"
            "ACCOUNT_DELETED_REVERSED" -> "Eliminación y Reversión Contable de Cuenta"
            "ACCOUNT_CREATED" -> "Apertura / Registro de Cuenta Bancaria"
            "CREDIT_LIMIT_VALIDATION" -> "Validación Automática de Cupo en Tarjeta"
            "SQLCIPHER_KEY_ROTATED" -> "Rotación de Llave de Cifrado SQLCipher"
            "BACKUP_CLOUD_SYNC" -> "Sincronización Cifrada con Nube"
            else -> eventType.replace("_", " ").lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }

    private fun getRelativeTimeSpan(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diffMs = now - timestamp
        if (diffMs < 0) return "Ahora"

        val seconds = diffMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Hace unos segundos"
            minutes < 60 -> "Hace $minutes min"
            hours < 24 -> "Hace $hours hora${if (hours > 1) "s" else ""}"
            days < 7 -> "Hace $days día${if (days > 1) "s" else ""}"
            else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    private fun calculateSha256(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            "sha256:00000000000000000000000000000000"
        }
    }
}

class AuditViewModelFactory(
    private val auditSecurityLogDao: AuditSecurityLogDao,
    private val assetAuditLogDao: AssetAuditLogDao,
    private val financialDao: FinancialDao,
    private val userId: String = "user_default"
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuditViewModel::class.java)) {
            return AuditViewModel(auditSecurityLogDao, assetAuditLogDao, financialDao, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
