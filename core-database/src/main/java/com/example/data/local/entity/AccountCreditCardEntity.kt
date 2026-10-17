package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity representing Credit Card (Revolving Debt / Liability) metadata (1:1 with AccountEntity).
 * Defines revolving credit lines, billing cycles, cut-off and due dates,
 * purchase vs advance interest rates, grace periods, French amortization, and auto-debit rules.
 */
@Entity(
    tableName = "account_credit_cards",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["account_id"], unique = true)
    ]
)
data class AccountCreditCardEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "account_id")
    val accountId: String,

    @ColumnInfo(name = "franchise")
    val franchise: String = "VISA", // "VISA", "MASTERCARD", "AMEX", "DINERS"

    @ColumnInfo(name = "category")
    val category: String = "PLATINUM", // "CLASSIC", "GOLD", "PLATINUM", "SIGNATURE", "BLACK", "INFINITE"

    @ColumnInfo(name = "quota_total")
    val quotaTotal: Double = 12_000_000.0,

    @ColumnInfo(name = "quota_available")
    val quotaAvailable: Double = 3_550_000.0,

    @ColumnInfo(name = "quota_used")
    val quotaUsed: Double = 8_450_000.0,

    @ColumnInfo(name = "quota_retained")
    val quotaRetained: Double = 0.0,

    @ColumnInfo(name = "cut_off_day")
    val cutOffDay: Int = 25, // Día de corte (1 al 31)

    @ColumnInfo(name = "payment_due_day")
    val paymentDueDay: Int = 10, // Límite de pago (1 al 31)

    @ColumnInfo(name = "interest_rate_ea")
    val interestRateEa: Double = 18.50, // Tasa general EA (%)

    @ColumnInfo(name = "purchases_rate_ea")
    val purchasesRateEa: Double = 22.00, // Tasa para consumos EA (%)

    @ColumnInfo(name = "advances_rate_ea")
    val advancesRateEa: Double = 28.00, // Tasa para avances en efectivo EA (%) (Diaria sin gracia)

    @ColumnInfo(name = "has_grace_period")
    val hasGracePeriod: Boolean = true, // Periodo de gracia para compras a 1 cuota

    @ColumnInfo(name = "late_interest_rate_monthly")
    val lateInterestRateMonthly: Double = 2.50, // Interés de mora mensual (%)

    @ColumnInfo(name = "current_interest_amount")
    val currentInterestAmount: Double = 250_000.0,

    @ColumnInfo(name = "interest_cutoff_date")
    val interestCutoffDate: String = "2026-09-25",

    @ColumnInfo(name = "payment_strategy")
    val paymentStrategy: String = "MINIMUM_PAYMENT", // "TOTAL_PAYMENT", "MINIMUM_PAYMENT"

    @ColumnInfo(name = "minimum_payment_amount")
    val minimumPaymentAmount: Double = 300_000.0,

    @ColumnInfo(name = "auto_pay_enabled")
    val autoPayEnabled: Boolean = true,

    @ColumnInfo(name = "auto_pay_source_account_id")
    val autoPaySourceAccountId: String? = null,

    @ColumnInfo(name = "auto_pay_source_account_name")
    val autoPaySourceAccountName: String? = "Cuenta de ahorros - Bancolombia",

    @ColumnInfo(name = "custom_pay_day")
    val customPayDay: Int = 15,

    @ColumnInfo(name = "scheduled_pay_enabled")
    val scheduledPayEnabled: Boolean = true,

    @ColumnInfo(name = "scheduled_pay_date")
    val scheduledPayDate: String = "2026-10-15",

    @ColumnInfo(name = "extra_payments_enabled")
    val extraPaymentsEnabled: Boolean = true,

    @ColumnInfo(name = "extra_payment_reduces")
    val extraPaymentReduces: String = "BOTH", // "TERM", "QUOTA", "BOTH"

    @ColumnInfo(name = "total_installments")
    val totalInstallments: Int = 48,

    @ColumnInfo(name = "paid_installments")
    val paidInstallments: Int = 12,

    @ColumnInfo(name = "pending_installments")
    val pendingInstallments: Int = 36,

    @ColumnInfo(name = "installment_value")
    val installmentValue: Double = 485_000.0,

    @ColumnInfo(name = "amortization_system")
    val amortizationSystem: String = "FRENCH", // "FRENCH" (cuotas iguales), "GERMAN", "FIXED_PRINCIPAL"

    @ColumnInfo(name = "next_installment_date")
    val nextInstallmentDate: String = "2026-10-15",

    @ColumnInfo(name = "include_in_net_worth")
    val includeInNetWorth: Boolean = true, // Pasivo: Resta en el patrimonio neto

    @ColumnInfo(name = "finances_asset")
    val financesAsset: Boolean = false,

    @ColumnInfo(name = "related_asset_name")
    val relatedAssetName: String? = null,

    @ColumnInfo(name = "related_asset_value")
    val relatedAssetValue: Double = 0.0,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false
)
