package com.example.domain.models

/**
 * Domain DTO for security audit logs.
 */
data class AuditLogEntry(
    val id: Long,
    val timestamp: Long,
    val eventType: String,
    val authMethod: String,
    val details: String
)
