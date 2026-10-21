package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Immutable append-only audit trail for authentication and security events.
 */
@Entity(tableName = "audit_security_logs")
data class AuditSecurityLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "event_type")
    val eventType: String,

    @ColumnInfo(name = "auth_method")
    val authMethod: String,

    @ColumnInfo(name = "details")
    val details: String,

    @ColumnInfo(name = "userId")
    val userId: String = ""
)
