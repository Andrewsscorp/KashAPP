package com.example.domain.model.fixedassets

import androidx.compose.ui.graphics.Color
import com.example.data.local.entity.AssetMovementEntity

enum class MaintenanceStatus(val label: String, val colorHex: String) {
    PENDING("Pendiente", "#F59E0B"),
    IN_PROGRESS("En Progreso", "#38BDF8"),
    COMPLETED("Finalizado", "#10B981");

    val color: Color
        get() = when (this) {
            PENDING -> Color(0xFFF59E0B)
            IN_PROGRESS -> Color(0xFF38BDF8)
            COMPLETED -> Color(0xFF10B981)
        }

    companion object {
        fun fromString(value: String?): MaintenanceStatus {
            if (value.isNullOrBlank()) return COMPLETED
            val v = value.uppercase().trim()
            return when {
                v.contains("PEND") || v == "PENDING" -> PENDING
                v.contains("PROG") || v.contains("CURSO") || v == "IN_PROGRESS" -> IN_PROGRESS
                else -> COMPLETED
            }
        }
    }
}

data class MaintenanceOrder(
    val id: String,
    val assetId: String,
    val assetCode: String,
    val assetName: String,
    val category: String,
    val title: String,
    val date: String,
    val amount: Double,
    val status: MaintenanceStatus,
    val maintenanceType: String, // Preventivo, Correctivo, Calibración, Inspección, Mejora
    val provider: String?,
    val notes: String?,
    val isCapitalizable: Boolean, // True = Mejora Capitalizable (NIC 16), False = Gasto Operacional
    val rawMovement: AssetMovementEntity
)
