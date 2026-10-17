package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Relational entity 1:1 with AccountEntity for payroll accounts.
 * Manages income projections, employer info, automation triggers, and banking exemptions.
 */
@Entity(
    tableName = "account_payroll_configs",
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
data class AccountPayrollConfigEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "account_id")
    val accountId: String,
    @ColumnInfo(name = "employer_name")
    val employerName: String,
    @ColumnInfo(name = "default_concept")
    val defaultConcept: String = "Nómina",
    @ColumnInfo(name = "expected_income")
    val expectedIncome: Double = 4500000.0,
    @ColumnInfo(name = "periodicity")
    val periodicity: String = "MONTHLY", // 'MONTHLY', 'BIWEEKLY', 'WEEKLY'
    @ColumnInfo(name = "expected_pay_day")
    val expectedPayDay: Int = 30, // Day of month (1 - 31)
    @ColumnInfo(name = "auto_register")
    val autoRegister: Boolean = true,
    @ColumnInfo(name = "exemption_management_fee")
    val exemptionManagementFee: Boolean = true,
    @ColumnInfo(name = "exemption_tax_4x1000")
    val exemptionTax4x1000: Boolean = false,
    @ColumnInfo(name = "free_atm_withdrawals")
    val freeAtmWithdrawals: Boolean = true,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
