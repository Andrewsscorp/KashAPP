package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Immutable append-only audit trail for all operations in the Fixed Assets module (NIIF / NIC 16).
 * Logs creation, edition, state changes, movements, depreciation runs, maintenance orders, and deletions.
 */
@Entity(
    tableName = "asset_audit_logs",
    indices = [
        Index("assetId"),
        Index("userId"),
        Index("actionType"),
        Index("timestamp")
    ]
)
data class AssetAuditLogEntity(
    @PrimaryKey
    val id: String,
    val userId: String = "user_default",
    val assetId: String? = null,
    val assetName: String? = null,
    val actionType: String, // "CREACION", "MODIFICACION", "CAMBIO_ESTADO", "DEPRECIACION", "MANTENIMIENTO", "BAJA", "EXPORTACION"
    val actionTitle: String,
    val details: String,
    val financialImpact: String? = null,
    val userRole: String = "Administrador Contable NIIF",
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDate: String // "YYYY-MM-DD HH:mm:ss"
)
