package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity representing an individual CDT investment configuration (1:1 with AccountEntity).
 * Stores financial rates, compounding rules, maturity instructions, alerts, and yield projection.
 */
@Entity(
    tableName = "account_cdts",
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
data class AccountCdtEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "account_id")
    val accountId: String,
    @ColumnInfo(name = "certificate_number")
    val certificateNumber: String? = null,
    @ColumnInfo(name = "interest_rate_ea")
    val interestRateEa: Double = 10.5,
    @ColumnInfo(name = "rate_type")
    val rateType: String = "EA", // 'EA', 'NOMINAL_MV', 'NOMINAL_TV'
    @ColumnInfo(name = "payout_frequency")
    val payoutFrequency: String = "AT_MATURITY", // 'AT_MATURITY', 'MONTHLY'
    @ColumnInfo(name = "compounding")
    val compounding: Boolean = true, // ¿Capitaliza? (Sí/No)
    @ColumnInfo(name = "issue_date")
    val issueDate: String, // "YYYY-MM-DD"
    @ColumnInfo(name = "term_days")
    val termDays: Int = 180,
    @ColumnInfo(name = "maturity_date")
    val maturityDate: String, // "YYYY-MM-DD"
    @ColumnInfo(name = "action_at_maturity")
    val actionAtMaturity: String = "ASK", // 'ASK', 'AUTO_RENEW', 'TRANSFER'
    @ColumnInfo(name = "destination_account_id")
    val destinationAccountId: String? = null,
    @ColumnInfo(name = "source_account_id")
    val sourceAccountId: String? = null,
    @ColumnInfo(name = "allows_early_withdrawal")
    val allowsEarlyWithdrawal: Boolean = false,
    @ColumnInfo(name = "alert_days_before")
    val alertDaysBefore: Int = 15,
    @ColumnInfo(name = "has_alert")
    val hasAlert: Boolean = true,
    @ColumnInfo(name = "initial_amount")
    val initialAmount: Double = 10000000.0,
    @ColumnInfo(name = "estimated_yield")
    val estimatedYield: Double = 505044.0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
