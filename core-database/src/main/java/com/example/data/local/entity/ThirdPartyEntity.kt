package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Enterprise entity representing third parties (Terceros, Proveedores, Concesionarios, Clientes, Tiendas y Comercios).
 * Enforces anti-duplicate policy based on the unique tuple (userId, normalizedName, idNumber).
 */
@Entity(
    tableName = "third_parties",
    indices = [
        Index(value = ["userId", "normalizedName", "idNumber"], unique = true),
        Index(value = ["userId", "category"]),
        Index(value = ["userId", "isFavorite"]),
        Index(value = ["userId", "isActive"]),
        Index(value = ["idNumber"])
    ]
)
data class ThirdPartyEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "user_default",
    val name: String,
    val normalizedName: String,
    val personType: String = "NATURAL", // "NATURAL" o "JURIDICA"
    val idType: String = "CC",          // "CC", "NIT", "CE", "PASAPORTE", "RUT", "OTRO"
    val idNumber: String,
    val verificationDigit: String? = null,
    val commercialName: String? = null,
    val category: String = "PROVEEDOR", // "PROVEEDOR", "CONCESIONARIO", "TIENDA_COMERCIO", "CLIENTE", "ACREEDOR", "EMPLEADO", "OTRO"
    val primaryPhone: String? = null,
    val secondaryPhone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val city: String? = null,
    val paymentTermsDays: Int = 0,
    val defaultBank: String? = null,
    val bankAccountType: String? = null,
    val bankAccountNumber: String? = null,
    val iconName: String = "Person",
    val colorHex: String = "#0284C7",
    val notes: String? = null,
    val isFavorite: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val displayCode: String
        get() = "TER-" + (kotlin.math.abs(id.hashCode()) % 100000).toString().padStart(5, '0')

    val displayName: String
        get() = commercialName?.takeIf { it.isNotBlank() } ?: name

    val fullDocument: String
        get() = if (verificationDigit.isNullOrBlank()) {
            "$idType $idNumber"
        } else {
            "$idType $idNumber-$verificationDigit"
        }

    companion object {
        fun normalizeName(rawName: String): String {
            return rawName.trim().lowercase().replace(Regex("\\s+"), " ")
        }

        fun normalizeId(rawId: String): String {
            return rawId.trim().replace(Regex("[^A-Za-z0-9]"), "")
        }
    }
}
