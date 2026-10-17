package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room Entity for custom or recurring upcoming payments, bills, subscriptions, and debt obligations.
 */
@Entity(tableName = "scheduled_payments")
data class ScheduledPaymentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "user_id")
    val userId: String = "user_default",

    @ColumnInfo(name = "entity_name")
    val entityName: String,

    @ColumnInfo(name = "category")
    val category: String = "SERVICIOS", // "TARJETA_CREDITO", "SERVICIOS", "SUSCRIPCION", "DEUDA_PRESTAMO", "ARRIENDO", "OTRO"

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "currency")
    val currency: String = "COP",

    @ColumnInfo(name = "due_date")
    val dueDate: String, // YYYY-MM-DD

    @ColumnInfo(name = "recurrence")
    val recurrence: String = "MONTHLY", // "ONCE", "MONTHLY", "BIWEEKLY", "ANNUAL"

    @ColumnInfo(name = "is_income")
    val isIncome: Boolean = false,

    @ColumnInfo(name = "is_paid")
    val isPaid: Boolean = false,

    @ColumnInfo(name = "paid_date")
    val paidDate: String? = null,

    @ColumnInfo(name = "source_account_id")
    val sourceAccountId: String? = null,

    @ColumnInfo(name = "source_account_name")
    val sourceAccountName: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "reminder_days_before")
    val reminderDaysBefore: Int = 3,

    @ColumnInfo(name = "notification_enabled")
    val notificationEnabled: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
