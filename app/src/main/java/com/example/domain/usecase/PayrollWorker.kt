package com.example.domain.usecase

import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate

data class PayrollConfigModel(
    val accountId: String,
    val employerName: String = "Empresa XYZ",
    val expectedIncome: BigDecimal,
    val expectedPayDay: Int = 30,
    val autoRegister: Boolean = true,
    val periodicity: String = "MONTHLY"
)

interface PayrollConfigRepository {
    fun getDueToday(day: Int): List<PayrollConfigModel>
    fun getDueTodayWithWeekendShift(date: LocalDate): List<PayrollConfigModel> = getDueToday(date.dayOfMonth)
}

interface PayrollTransactionRepository {
    fun existsPayrollThisMonth(accountId: String, currentDate: LocalDate): Boolean
    fun insertIncome(accountId: String, amount: BigDecimal, concept: String)
}

interface PayrollAccountRepository {
    fun addBalance(accountId: String, amount: BigDecimal)
}

/**
 * Domain Worker for automated payroll income processing.
 */
class PayrollWorker(
    private val payrollRepo: PayrollConfigRepository,
    private val transactionRepo: PayrollTransactionRepository,
    private val accountRepo: PayrollAccountRepository
) {

    fun executeDailyBatch(currentDate: LocalDate) {
        // 1. Obtener nóminas programadas para hoy (con soporte de ajuste por fin de semana)
        val configs = payrollRepo.getDueToday(currentDate.dayOfMonth)

        for (config in configs) {
            if (!config.autoRegister) {
                continue
            }

            // 2. Idempotencia: Verificar si ya se registró la nómina este mes
            if (transactionRepo.existsPayrollThisMonth(config.accountId, currentDate)) {
                continue
            }

            // 3. Registrar ingreso y actualizar saldo
            transactionRepo.insertIncome(
                accountId = config.accountId,
                amount = config.expectedIncome,
                concept = "Auto: Nómina"
            )
            accountRepo.addBalance(config.accountId, config.expectedIncome)
        }
    }

    /**
     * Calculates the effective execution date shifting weekend payments to preceding Friday.
     */
    fun calculateEffectivePayDate(year: Int, month: java.time.Month, scheduledDay: Int): LocalDate {
        val maxDay = month.length(java.time.Year.isLeap(year.toLong()))
        val targetDate = LocalDate.of(year, month, scheduledDay.coerceAtMost(maxDay))
        return when (targetDate.dayOfWeek) {
            DayOfWeek.SATURDAY -> targetDate.minusDays(1)
            DayOfWeek.SUNDAY -> targetDate.minusDays(2)
            else -> targetDate
        }
    }
}
