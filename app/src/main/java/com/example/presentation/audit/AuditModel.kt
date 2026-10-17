package com.example.presentation.audit

import androidx.compose.ui.graphics.Color

/**
 * Enterprise Audit Vault Models.
 * Strictly immutable data structures for recording all system, security, and financial operations.
 */
enum class AuditCategory(val label: String, val colorHex: Long) {
    ALL("Todos", 0xFF38BDF8),
    SECURITY("Seguridad & Acceso", 0xFFEF4444),
    ACCOUNTS("Cuentas & Bancos", 0xFF0284C7),
    TRANSACTIONS("Transacciones", 0xFF10B981),
    FIXED_ASSETS("Activos Fijos", 0xFF8B5CF6),
    SYSTEM("Sistema & Ajustes", 0xFFF59E0B)
}

enum class AuditSeverity(val label: String, val color: Color) {
    INFO("Informativo", Color(0xFF38BDF8)),
    SUCCESS("Exitoso", Color(0xFF10B981)),
    WARNING("Advertencia", Color(0xFFFBBF24)),
    CRITICAL("Crítico", Color(0xFFEF4444))
}

enum class AuditTimeFilter(val label: String) {
    ALL("Todo"),
    TODAY("Hoy"),
    LAST_7_DAYS("7 Días"),
    THIS_MONTH("Este Mes")
}

enum class AuditSeverityFilter(val label: String) {
    ALL("Todas"),
    CRITICAL("Críticas"),
    WARNING("Alertas"),
    SUCCESS("Exitosas"),
    INFO("Informativas")
}

data class AuditUiEntry(
    val id: String,
    val timestamp: Long,
    val formattedDateTime: String,
    val relativeTime: String,
    val category: AuditCategory,
    val severity: AuditSeverity,
    val eventTitle: String,
    val details: String,
    val authMethodOrOrigin: String,
    val hashSha256: String,
    val isImmutable: Boolean = true
)

data class AuditVaultUiState(
    val logs: List<AuditUiEntry> = emptyList(),
    val filteredLogs: List<AuditUiEntry> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: AuditCategory = AuditCategory.ALL,
    val selectedTimeFilter: AuditTimeFilter = AuditTimeFilter.ALL,
    val selectedSeverityFilter: AuditSeverityFilter = AuditSeverityFilter.ALL,
    val totalCount: Int = 0,
    val securityCount: Int = 0,
    val accountsCount: Int = 0,
    val assetsCount: Int = 0,
    val transactionsCount: Int = 0,
    val selectedEntryForDetail: AuditUiEntry? = null,
    val isLoading: Boolean = false,
    val isVerifyingIntegrity: Boolean = false,
    val integrityVerificationResult: String? = null,
    val exportSuccessMessage: String? = null,
    val exportPdfResult: com.example.util.FinancialReportExporter.ExportResult? = null
)
