package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an individual bank account, digital wallet, or cash source.
 * Extended with financial calculation parameters, bank FK, and encrypted metadata.
 */
@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["userId"])
    ]
)
data class AccountEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val balance: Double = 0.0,
    val currency: String = "COP",
    val type: String = "SAVINGS", // "SAVINGS", "CHECKING", "PAYROLL", "CDT", "WALLET", "CREDIT_CARD", "CASH"
    val bankName: String = "Bancolombia",
    val isFavorite: Boolean = false,
    val accountNumberEncrypted: String? = null,
    val accountNumberMasked: String? = null,
    val openingDate: String? = null,
    val kashappDate: String = "2026-09-26",
    val generatesInterest: Boolean = false,
    val interestRateEa: Double? = null,
    val calcFrequency: String? = null, // "Diaria", "Mensual"
    val payoutFrequency: String? = null, // "Mensual", "Al vencimiento"
    val compoundInterest: Boolean = false,
    val purposes: String = "Ahorro", // comma-separated purposes (Ahorro, Inversión, etc.)
    val customColorHex: String = "#0284C7",
    val iconName: String = "ic_bank_default",
    val minimumBalance: Double = 0.0, // Encaje bancario: reduce Dinero Disponible, conserva Patrimonio Neto
    val monthlyFee: Double = 0.0, // Cuota de manejo mensual automática
    val availableBalance: Double = balance, // Saldo disponible (Liquidez inmediata)
    val holdBalance: Double = 0.0, // Saldo retenido / bloqueado (Canjes, autorizaciones)
    val country: String = "CO", // Código ISO País (CO, US, MX, ES)
    val cityName: String? = null, // Ciudad de apertura o radicación de la cuenta (ej. Bogotá, Miami)
    val favoriteRouting: String = "GENERAL", // "GENERAL", "EXPENSES", "INCOME"
    val accountOwner: String? = null, // Titular de la cuenta (ej. Andrés Felipe)
    val alias: String? = null, // Alias compacto para vistas rápidas
    val hasOverdraft: Boolean = false, // Cupo de sobregiro habilitado
    val overdraftLimit: Double = 0.0, // Monto del sobregiro
    val overdraftRateEa: Double = 0.0, // Tasa efectiva anual del sobregiro
    val feeChargeDay: Int = 30, // Día del mes para cobro de cuota de manejo
    val syncProvider: String? = null, // "BELVO", "PROMETEO", "PLAID"
    val syncInstitutionId: String? = null,
    val syncAccessTokenEncrypted: String? = null,
    val lastSyncTimestamp: Long? = null,
    val supportingDocumentUri: String? = null,
    val supportingDocumentType: String? = null,
    val favoriteForIncomes: Boolean = false, // Ruteo Dual: Destino por defecto al recibir nómina o ingresos
    val favoriteForExpenses: Boolean = false, // Ruteo Dual: Origen por defecto para gastos o compras
    val employerName: String? = null, // Nombre de la empresa / empleador (ej. Empresa XYZ)
    val isTaxExempt: Boolean = false // Exención fiscal legal (4x1000 GMF en Colombia)
)
