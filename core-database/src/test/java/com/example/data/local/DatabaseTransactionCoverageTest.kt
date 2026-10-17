package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.AccountCreditCardEntity
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.FinancialGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransferGroupEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DatabaseTransactionCoverageTest {

    private lateinit var database: SecurityDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SecurityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        SecurityDatabase.installIntegrityGuards(database.openHelper.writableDatabase)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun accountQueries_areScopedAndUpsertPreservesChildren() = runBlocking {
        val dao = database.financialDao()
        val accountA = account("a", "user-a", 1_000.0)
        dao.insertAccounts(listOf(accountA, account("b", "user-b", 2_000.0)))
        dao.insertAccountCreditCard(AccountCreditCardEntity(accountId = accountA.id))
        dao.insertAccount(accountA.copy(name = "Updated", balance = 900.0))

        assertEquals(listOf("a"), dao.getAccounts("user-a").map { it.id })
        assertEquals("Updated", dao.getAccountById("a")?.name)
        assertNotNull(dao.getAccountCreditCard("a"))
    }

    @Test
    fun expenseTransaction_updatesBalanceAndIsAtomic() = runBlocking {
        val dao = database.financialDao()
        dao.insertAccount(account("cash", "user-a", 1_000.0))
        dao.executeExpenseTransaction(transaction("expense", "cash", "user-a", 250.0), "user-a", affectsBudget = false)
        assertEquals(750.0, dao.getAccountById("cash")?.balance ?: 0.0, 0.001)
        assertEquals(1, dao.getTransactions("user-a").size)

        var failed = false
        try {
            dao.executeExpenseTransaction(transaction("invalid", "missing", "user-a", 10.0), "user-a", affectsBudget = false)
        } catch (_: RuntimeException) {
            failed = true
        }
        assertTrue(failed)
        assertEquals(1, dao.getTransactions("user-a").size)
    }

    @Test
    fun incomeAndTransferTransactions_keepDoubleEntryBalances() = runBlocking {
        val dao = database.financialDao()
        dao.insertAccounts(listOf(account("source", "user-a", 1_000.0), account("target", "user-a", 100.0)))
        dao.executeIncomeTransaction(transaction("income", "target", "user-a", 200.0, "INCOME"), "user-a")
        dao.executeTransferTransaction(
            TransferGroupEntity("group-1", "user-a", "move", "source", "target", 300.0, "COP"),
            transaction("out", "source", "user-a", 300.0, "TRANSFER_OUT", "group-1"),
            transaction("in", "target", "user-a", 300.0, "TRANSFER_IN", "group-1"),
            "user-a"
        )
        assertEquals(700.0, dao.getAccountById("source")?.balance ?: 0.0, 0.001)
        assertEquals(600.0, dao.getAccountById("target")?.balance ?: 0.0, 0.001)
        assertEquals(3, dao.getTransactions("user-a").size)
    }

    @Test
    fun goalVirtualPocket_requiresExistingAccountAndFreezesFunds() = runBlocking {
        val dao = database.financialDao()
        dao.insertAccount(account("goal-account", "user-a", 1_000.0).copy(availableBalance = 800.0))
        dao.executeGoalCreationWithVirtualPocket(
            FinancialGoalEntity(
                id = "goal-1",
                userId = "user-a",
                name = "Emergency",
                targetAmount = 500.0,
                savedAmount = 0.0,
                targetAccountId = "goal-account",
                targetAccountName = "Savings",
                virtualPocketEnabled = true
            ),
            freezeAmount = 200.0
        )
        val saved = dao.getGoalById("goal-1")
        assertNotNull(saved)
        assertEquals(600.0, dao.getAccountById("goal-account")?.availableBalance ?: 0.0, 0.001)
        assertEquals(200.0, dao.getAccountById("goal-account")?.holdBalance ?: 0.0, 0.001)
    }

    @Test
    fun directOrphanTransactionInsert_isRejectedByIntegrityGuard() = runBlocking {
        var failed = false
        try {
            database.financialDao().insertTransaction(transaction("orphan", "missing", "user-a", 1.0))
        } catch (_: RuntimeException) {
            failed = true
        }
        assertTrue(failed)
    }

    private fun account(id: String, userId: String, balance: Double) = AccountEntity(
        id = id,
        userId = userId,
        name = "Account $id",
        balance = balance,
        availableBalance = balance
    )

    private fun transaction(
        id: String,
        accountId: String,
        userId: String,
        amount: Double,
        type: String = "EXPENSE",
        transferGroupId: String? = null
    ) = TransactionEntity(
        id = id,
        userId = userId,
        accountId = accountId,
        accountName = "Account",
        categoryId = "category",
        categoryName = "Category",
        amount = amount,
        currency = "COP",
        type = type,
        date = "2026-10-12",
        description = "Test",
        receiptUrl = null,
        idempotencyKey = "key-$id",
        transferGroupId = transferGroupId
    )
}
