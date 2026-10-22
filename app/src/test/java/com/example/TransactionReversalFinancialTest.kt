package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SecurityDatabase
import com.example.data.local.entity.AccountCreditCardEntity
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.BudgetItemEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransferGroupEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TransactionReversalFinancialTest {

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
    fun testExpenseReversalRestoresAccountBalanceAndBudgetSpent() = runBlocking {
        val dao = database.financialDao()
        val userId = "user_reversal_1"

        // 1. Initial State: Account with $1,000,000, Budget envelope with $0 spent
        val account = AccountEntity(
            id = "acc_ahorros_1",
            userId = userId,
            name = "Ahorros Bancolombia",
            balance = 1000000.0,
            availableBalance = 1000000.0
        )
        dao.insertAccount(account)

        val budget = BudgetEntity(
            id = "bgt_oct_2026",
            userId = userId,
            name = "Presupuesto Octubre",
            startDate = "2026-10-01",
            endDate = "2026-10-31",
            totalAmount = 500000.0
        )
        dao.insertBudget(budget)

        val budgetItem = BudgetItemEntity(
            id = "item_alimentacion",
            budgetId = budget.id,
            categoryId = "cat_food",
            categoryName = "Alimentación",
            allocatedAmount = 500000.0,
            spentAmount = 0.0
        )
        dao.insertBudgetItems(listOf(budgetItem))

        // 2. Execute Expense: $200,000 in Alimentación
        val expenseTx = TransactionEntity(
            id = "tx_exp_001",
            userId = userId,
            accountId = account.id,
            accountName = account.name,
            categoryId = budgetItem.categoryId,
            categoryName = budgetItem.categoryName,
            sourceId = null,
            sourceName = null,
            amount = 200000.0,
            currency = "COP",
            type = "EXPENSE",
            date = "2026-10-15",
            description = "Almuerzo",
            receiptUrl = null,
            idempotencyKey = "idem_001"
        )
        dao.executeExpenseTransaction(expenseTx, userId, affectsBudget = true)

        // Verify state after expense
        val accAfterExpense = dao.getAccountById(account.id)
        assertEquals(800000.0, accAfterExpense?.balance ?: 0.0, 0.001)

        val budgetItemsAfterExpense = dao.getBudgetItems(budget.id)
        assertEquals(200000.0, budgetItemsAfterExpense.first().spentAmount, 0.001)

        // 3. Reverse and Delete the Expense
        val deletedIds = dao.executeAtomicTransactionReversal(expenseTx.id, userId)
        assertEquals(listOf(expenseTx.id), deletedIds)

        // 4. Assert full restoration
        val accRestored = dao.getAccountById(account.id)
        assertEquals(1000000.0, accRestored?.balance ?: 0.0, 0.001)
        assertEquals(1000000.0, accRestored?.availableBalance ?: 0.0, 0.001)

        val budgetItemsRestored = dao.getBudgetItems(budget.id)
        assertEquals(0.0, budgetItemsRestored.first().spentAmount, 0.001)

        assertNull(dao.getTransactionById(expenseTx.id))
    }

    @Test
    fun testCreditCardExpenseReversalRestoresBalanceAndQuota() = runBlocking {
        val dao = database.financialDao()
        val userId = "user_reversal_2"

        // 1. Initial State: Credit card with limit $5,000,000, quotaUsed $1,000,000, balance -$1,000,000
        val cardAccount = AccountEntity(
            id = "acc_card_visa",
            userId = userId,
            name = "Visa Platinum",
            type = "CREDIT_CARD",
            balance = -1000000.0,
            availableBalance = 4000000.0
        )
        dao.insertAccount(cardAccount)
        dao.insertAccountCreditCard(
            AccountCreditCardEntity(
                accountId = cardAccount.id,
                quotaTotal = 5000000.0,
                quotaUsed = 1000000.0,
                quotaAvailable = 4000000.0
            )
        )

        // 2. Execute Purchase: $500,000
        val purchaseTx = TransactionEntity(
            id = "tx_card_buy_1",
            userId = userId,
            accountId = cardAccount.id,
            accountName = cardAccount.name,
            categoryId = "cat_tech",
            categoryName = "Tecnología",
            sourceId = null,
            sourceName = null,
            amount = 500000.0,
            currency = "COP",
            type = "EXPENSE",
            date = "2026-10-18",
            description = "Laptop",
            receiptUrl = null,
            idempotencyKey = "idem_buy_1"
        )
        dao.executeExpenseTransaction(purchaseTx, userId, affectsBudget = false)

        val cardConfigAfter = dao.getAccountCreditCard(cardAccount.id)
        assertEquals(1500000.0, cardConfigAfter?.quotaUsed ?: 0.0, 0.001)
        val accAfter = dao.getAccountById(cardAccount.id)
        assertEquals(-1500000.0, accAfter?.balance ?: 0.0, 0.001)

        // 3. Reversal
        val deletedIds = dao.executeAtomicTransactionReversal(purchaseTx.id, userId)
        assertEquals(listOf(purchaseTx.id), deletedIds)

        // 4. Assert Quota and Balance restored
        val cardConfigRestored = dao.getAccountCreditCard(cardAccount.id)
        assertEquals(1000000.0, cardConfigRestored?.quotaUsed ?: 0.0, 0.001)
        val accRestored = dao.getAccountById(cardAccount.id)
        assertEquals(-1000000.0, accRestored?.balance ?: 0.0, 0.001)
    }

    @Test
    fun testTransferReversalRestoresBothAccounts() = runBlocking {
        val dao = database.financialDao()
        val userId = "user_reversal_3"

        // Account A with $1,000,000, Account B with $500,000
        val accA = AccountEntity(id = "acc_source_a", userId = userId, name = "Cuenta A", balance = 1000000.0)
        val accB = AccountEntity(id = "acc_dest_b", userId = userId, name = "Cuenta B", balance = 500000.0)
        dao.insertAccount(accA)
        dao.insertAccount(accB)

        val transferGroup = TransferGroupEntity(
            id = "grp_transfer_123",
            userId = userId,
            concept = "Transferencia entre cuentas",
            sourceAccountId = accA.id,
            destinationAccountId = accB.id,
            amount = 300000.0,
            currency = "COP"
        )
        val txOut = TransactionEntity(
            id = "tx_out_123",
            userId = userId,
            accountId = accA.id,
            accountName = accA.name,
            categoryId = "cat_transfer",
            categoryName = "Transferencia",
            sourceId = null,
            sourceName = null,
            amount = 300000.0,
            currency = "COP",
            type = "TRANSFER_OUT",
            date = "2026-10-20",
            description = "Transferencia Salida",
            receiptUrl = null,
            transferGroupId = transferGroup.id,
            idempotencyKey = "idem_out_123"
        )
        val txIn = TransactionEntity(
            id = "tx_in_123",
            userId = userId,
            accountId = accB.id,
            accountName = accB.name,
            categoryId = "cat_transfer",
            categoryName = "Transferencia",
            sourceId = null,
            sourceName = null,
            amount = 300000.0,
            currency = "COP",
            type = "TRANSFER_IN",
            date = "2026-10-20",
            description = "Transferencia Entrada",
            receiptUrl = null,
            transferGroupId = transferGroup.id,
            idempotencyKey = "idem_in_123"
        )
        dao.executeTransferTransaction(transferGroup, txOut, txIn, userId)

        assertEquals(700000.0, dao.getAccountById(accA.id)?.balance ?: 0.0, 0.001)
        assertEquals(800000.0, dao.getAccountById(accB.id)?.balance ?: 0.0, 0.001)

        // Reverse by deleting tx_out_
        val deletedIds = dao.executeAtomicTransactionReversal(txOut.id, userId)
        assertTrue(deletedIds.contains(txOut.id))
        assertTrue(deletedIds.contains(txIn.id))

        assertEquals(1000000.0, dao.getAccountById(accA.id)?.balance ?: 0.0, 0.001)
        assertEquals(500000.0, dao.getAccountById(accB.id)?.balance ?: 0.0, 0.001)
        assertNull(dao.getTransactionById(txOut.id))
        assertNull(dao.getTransactionById(txIn.id))
    }

    @Test
    fun testTenantIsolationPreventsReversalOfOtherUserTransaction() = runBlocking {
        val dao = database.financialDao()
        val userA = "user_owner_a"
        val userB = "user_attacker_b"

        val accountA = AccountEntity(id = "acc_victim", userId = userA, name = "Cuenta Víctima", balance = 5000000.0)
        dao.insertAccount(accountA)

        val txA = TransactionEntity(
            id = "tx_secret_a",
            userId = userA,
            accountId = accountA.id,
            accountName = accountA.name,
            categoryId = "cat_test",
            categoryName = "Prueba",
            sourceId = null,
            sourceName = null,
            amount = 1000000.0,
            currency = "COP",
            type = "EXPENSE",
            date = "2026-10-20",
            description = "Gasto Secreto",
            receiptUrl = null,
            idempotencyKey = "idem_secret"
        )
        dao.insertTransaction(txA)

        // User B attempts to reverse User A's transaction
        val deletedByB = dao.executeAtomicTransactionReversal(txA.id, userB)
        assertTrue(deletedByB.isEmpty())

        // User A's transaction and balance remain intact
        assertNotNull(dao.getTransactionById(txA.id))
        assertEquals(5000000.0, dao.getAccountById(accountA.id)?.balance ?: 0.0, 0.001)
    }
}
