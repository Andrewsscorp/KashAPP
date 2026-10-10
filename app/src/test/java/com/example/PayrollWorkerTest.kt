package com.example

import com.example.domain.usecase.PayrollAccountRepository
import com.example.domain.usecase.PayrollConfigModel
import com.example.domain.usecase.PayrollConfigRepository
import com.example.domain.usecase.PayrollTransactionRepository
import com.example.domain.usecase.PayrollWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

class PayrollWorkerTest {

    private lateinit var fakePayrollRepo: FakePayrollConfigRepository
    private lateinit var fakeTransactionRepo: FakePayrollTransactionRepository
    private lateinit var fakeAccountRepo: FakePayrollAccountRepository
    private lateinit var worker: PayrollWorker

    @Before
    fun setup() {
        fakePayrollRepo = FakePayrollConfigRepository()
        fakeTransactionRepo = FakePayrollTransactionRepository()
        fakeAccountRepo = FakePayrollAccountRepository()
        worker = PayrollWorker(fakePayrollRepo, fakeTransactionRepo, fakeAccountRepo)
    }

    @Test
    fun test_payroll_worker_registers_income() {
        val testDate = LocalDate.of(2026, 9, 30)
        val expectedAmount = BigDecimal("4500000.00")
        val config = PayrollConfigModel(
            accountId = "acc-1",
            employerName = "Empresa XYZ",
            expectedIncome = expectedAmount,
            expectedPayDay = 30,
            autoRegister = true
        )

        fakePayrollRepo.configsByDay[30] = listOf(config)
        fakeTransactionRepo.payrollPaidThisMonth["acc-1"] = false

        // Act
        worker.executeDailyBatch(testDate)

        // Assert
        assertTrue(fakeTransactionRepo.insertedIncomes.any {
            it.accountId == "acc-1" && it.amount == expectedAmount && it.concept == "Auto: Nómina"
        })
        assertEquals(expectedAmount, fakeAccountRepo.balances["acc-1"])
    }

    @Test
    fun test_payroll_worker_prevents_duplicate_payments() {
        val testDate = LocalDate.of(2026, 9, 30)
        val expectedAmount = BigDecimal("4500000.00")
        val config = PayrollConfigModel(
            accountId = "acc-1",
            employerName = "Empresa XYZ",
            expectedIncome = expectedAmount,
            expectedPayDay = 30,
            autoRegister = true
        )

        fakePayrollRepo.configsByDay[30] = listOf(config)
        // Simulate payroll already paid this month
        fakeTransactionRepo.payrollPaidThisMonth["acc-1"] = true

        // Act
        worker.executeDailyBatch(testDate)

        // Assert: No new income was inserted and balance was not updated
        assertTrue(fakeTransactionRepo.insertedIncomes.isEmpty())
        assertFalse(fakeAccountRepo.balances.containsKey("acc-1"))
    }

    @Test
    fun test_payroll_worker_weekend_adjustment() {
        val effectiveDate = worker.calculateEffectivePayDate(2026, Month.OCTOBER, 31)
        assertEquals(DayOfWeek.FRIDAY, effectiveDate.dayOfWeek)
        assertEquals(30, effectiveDate.dayOfMonth)

        val effectiveSundayDate = worker.calculateEffectivePayDate(2026, Month.NOVEMBER, 15)
        assertEquals(DayOfWeek.FRIDAY, effectiveSundayDate.dayOfWeek)
        assertEquals(13, effectiveSundayDate.dayOfMonth)
    }

    // --- In-memory Fakes ---
    class FakePayrollConfigRepository : PayrollConfigRepository {
        val configsByDay = mutableMapOf<Int, List<PayrollConfigModel>>()
        override fun getDueToday(day: Int): List<PayrollConfigModel> = configsByDay[day] ?: emptyList()
    }

    class FakePayrollTransactionRepository : PayrollTransactionRepository {
        val payrollPaidThisMonth = mutableMapOf<String, Boolean>()
        data class IncomeRecord(val accountId: String, val amount: BigDecimal, val concept: String)
        val insertedIncomes = mutableListOf<IncomeRecord>()

        override fun existsPayrollThisMonth(accountId: String, currentDate: LocalDate): Boolean {
            return payrollPaidThisMonth[accountId] ?: false
        }

        override fun insertIncome(accountId: String, amount: BigDecimal, concept: String) {
            insertedIncomes.add(IncomeRecord(accountId, amount, concept))
        }
    }

    class FakePayrollAccountRepository : PayrollAccountRepository {
        val balances = mutableMapOf<String, BigDecimal>()

        override fun addBalance(accountId: String, amount: BigDecimal) {
            val current = balances[accountId] ?: BigDecimal.ZERO
            balances[accountId] = current.add(amount)
        }
    }
}
