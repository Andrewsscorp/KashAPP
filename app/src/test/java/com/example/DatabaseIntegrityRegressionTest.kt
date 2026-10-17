package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SecurityDatabase
import com.example.data.local.entity.AccountCreditCardEntity
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
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
class DatabaseIntegrityRegressionTest {

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
    fun accounts_are_isolatedByUserId() = runBlocking {
        val dao = database.financialDao()
        dao.insertAccounts(
            listOf(
                AccountEntity(id = "account-a", userId = "user-a", name = "Cuenta A"),
                AccountEntity(id = "account-b", userId = "user-b", name = "Cuenta B")
            )
        )

        val accountsForA = dao.getAccounts("user-a")

        assertEquals(listOf("account-a"), accountsForA.map { it.id })
    }

    @Test
    fun expense_with_missing_account_rolls_back_inserted_transaction() = runBlocking {
        val dao = database.financialDao()
        val transaction = validTransaction(accountId = "missing-account", userId = "user-a")

        var failed = false
        try {
            dao.executeExpenseTransaction(transaction, userId = "user-a")
        } catch (_: RuntimeException) {
            failed = true
        }

        assertTrue(failed)
        assertTrue(dao.getTransactions("user-a").isEmpty())
    }

    @Test
    fun direct_transaction_insert_with_missing_account_is_rejected() = runBlocking {
        val dao = database.financialDao()
        var failed = false

        try {
            dao.insertTransaction(validTransaction(accountId = "missing-account", userId = "user-a"))
        } catch (_: RuntimeException) {
            failed = true
        }

        assertTrue(failed)
        assertTrue(dao.getTransactions("user-a").isEmpty())
    }

    @Test
    fun account_upsert_preserves_credit_card_child() = runBlocking {
        val dao = database.financialDao()
        val account = AccountEntity(id = "card-account", userId = "user-a", name = "Tarjeta")
        dao.insertAccount(account)
        dao.insertAccountCreditCard(AccountCreditCardEntity(accountId = account.id))

        dao.insertAccount(account.copy(name = "Tarjeta actualizada", balance = -250.0))

        assertEquals("Tarjeta actualizada", dao.getAccountById(account.id)?.name)
        assertNotNull(dao.getAccountCreditCard(account.id))
    }

    @Test
    fun account_with_history_cannot_be_deleted() = runBlocking {
        val dao = database.financialDao()
        val account = AccountEntity(id = "history-account", userId = "user-a", name = "Histórica")
        dao.insertAccount(account)
        dao.insertTransaction(validTransaction(accountId = account.id, userId = account.userId))

        var failed = false
        try {
            dao.deleteAccountById(account.id)
        } catch (_: RuntimeException) {
            failed = true
        }

        assertTrue(failed)
        assertNotNull(dao.getAccountById(account.id))
    }

    private fun validTransaction(accountId: String, userId: String) = TransactionEntity(
        id = "transaction-1",
        userId = userId,
        accountId = accountId,
        accountName = "Cuenta",
        categoryId = "category-1",
        categoryName = "Gasto",
        amount = 100.0,
        currency = "COP",
        type = "EXPENSE",
        date = "2026-10-12",
        description = "Prueba",
        receiptUrl = null,
        idempotencyKey = "idempotency-1"
    )
}
