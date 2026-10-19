package com.example.security

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.datasource.FirebaseDataSource
import com.example.data.local.SecurityDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AuditSecurityLog
import com.example.data.local.entity.FinancialGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.FinancialRepositoryImpl
import com.example.presentation.audit.AuditCategory
import com.example.presentation.audit.AuditSeverity
import com.example.presentation.audit.AuditUiEntry
import com.example.presentation.audit.AuditViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ContributeToGoalAndAuditIntegritySecurityTest {

    private lateinit var context: Context
    private lateinit var database: SecurityDatabase
    private lateinit var repository: FinancialRepositoryImpl
    private val testDispatcher = UnconfinedTestDispatcher()

    private val testUserId = "user_sec_audit_88"

    private class MockFirebaseDataSource(context: Context) : FirebaseDataSource(context)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()

        database = Room.inMemoryDatabaseBuilder(context, SecurityDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()

        SecurityDatabase.installIntegrityGuards(database.openHelper.writableDatabase)

        val fakeFirebase = MockFirebaseDataSource(context)

        repository = FinancialRepositoryImpl(
            financialDao = database.financialDao(),
            auditLogDao = database.auditSecurityLogDao(),
            firebaseDataSource = fakeFirebase
        )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testContributeToGoal_atomicTransaction_debitsAccount_creditsGoal_andCreatesTransactionEntity() = runTest {
        // 1. Arrange: Create source account with initial balance $500,000 COP
        val account = AccountEntity(
            id = "acc_bancolombia_01",
            userId = testUserId,
            name = "Bancolombia Principal",
            type = "SAVINGS",
            balance = 500_000.0,
            availableBalance = 500_000.0,
            currency = "COP"
        )
        database.financialDao().insertAccount(account)

        // 2. Arrange: Create goal with target $1,000,000 COP and initial saved $100,000 COP
        val goal = FinancialGoalEntity(
            id = "goal_emergencias_01",
            userId = testUserId,
            name = "Fondo de Emergencias",
            targetAmount = 1_000_000.0,
            savedAmount = 100_000.0,
            deadlineDate = "2027-12-31",
            sourceAccountId = account.id,
            sourceAccountName = account.name,
            targetAccountId = account.id,
            targetAccountName = account.name,
            virtualPocketEnabled = false
        )
        database.financialDao().insertGoal(goal)

        val contributionAmount = 150_000.0

        // 3. Act: Contribute $150,000 COP
        val result = repository.contributeToGoal(
            goalId = goal.id,
            amount = contributionAmount,
            sourceAccountId = account.id
        )

        // 4. Assert: Operation succeeded
        assertTrue("Goal contribution should succeed", result.isSuccess)

        // 5. Assert: Account balance was debited atomically
        val updatedAccount = database.financialDao().getAccountById(account.id)
        assertNotNull(updatedAccount)
        assertEquals(350_000.0, updatedAccount!!.balance, 0.001)
        assertEquals(350_000.0, updatedAccount.availableBalance, 0.001)

        // 6. Assert: Goal savedAmount was credited atomically
        val updatedGoal = database.financialDao().getGoalById(goal.id)
        assertNotNull(updatedGoal)
        assertEquals(250_000.0, updatedGoal!!.savedAmount, 0.001)

        // 7. Assert: Immutable TransactionEntity was inserted in double-entry ledger
        val transactions = database.financialDao().getTransactions(testUserId)
        assertEquals("Exactly 1 transaction entity must be recorded for goal contribution", 1, transactions.size)

        val tx = transactions.first()
        assertEquals(account.id, tx.accountId)
        assertEquals(account.name, tx.accountName)
        assertEquals(contributionAmount, tx.amount, 0.001)
        assertEquals("EXPENSE", tx.type)
        assertEquals("cat_savings_goals", tx.categoryId)
        assertEquals("Ahorro / Metas", tx.categoryName)
        assertEquals("Aporte a meta: Fondo de Emergencias", tx.description)
        assertEquals("COP", tx.currency)
    }

    @Test
    fun testContributeToGoal_failsAndRollsBack_whenInsufficientBalance() = runTest {
        // 1. Arrange: Account with only $50,000 COP available
        val account = AccountEntity(
            id = "acc_nequi_02",
            userId = testUserId,
            name = "Nequi",
            type = "DIGITAL_WALLET",
            balance = 50_000.0,
            availableBalance = 50_000.0,
            currency = "COP"
        )
        database.financialDao().insertAccount(account)

        val goal = FinancialGoalEntity(
            id = "goal_viaje_02",
            userId = testUserId,
            name = "Viaje Japón",
            targetAmount = 5_000_000.0,
            savedAmount = 200_000.0,
            deadlineDate = "2027-06-30",
            sourceAccountId = account.id,
            sourceAccountName = account.name,
            targetAccountId = account.id,
            targetAccountName = account.name,
            virtualPocketEnabled = false
        )
        database.financialDao().insertGoal(goal)

        // 2. Act: Attempt to contribute $100,000 COP (more than available)
        val result = repository.contributeToGoal(
            goalId = goal.id,
            amount = 100_000.0,
            sourceAccountId = account.id
        )

        // 3. Assert: Operation failed with insufficient balance
        assertTrue("Contribution should fail due to insufficient funds", result.isFailure)

        // 4. Assert: Account balance was NOT changed (rollback)
        val accountAfter = database.financialDao().getAccountById(account.id)
        assertNotNull(accountAfter)
        assertEquals(50_000.0, accountAfter!!.balance, 0.001)

        // 5. Assert: Goal was NOT changed
        val goalAfter = database.financialDao().getGoalById(goal.id)
        assertNotNull(goalAfter)
        assertEquals(200_000.0, goalAfter!!.savedAmount, 0.001)

        // 6. Assert: No transaction was inserted
        val txs = database.financialDao().getTransactions(testUserId)
        assertTrue("No transaction ledger should be created on failure", txs.isEmpty())
    }

    @Test
    fun testAuditViewModel_verifyVaultIntegrity_detectsTamperingAgainstSqlite() = runTest {
        val auditSecurityDao = database.auditSecurityLogDao()
        val assetAuditDao = database.assetAuditLogDao()
        val financialDao = database.financialDao()
        val fixedAssetDao = database.fixedAssetDao()

        // 1. Insert genuine records
        val acc = AccountEntity(
            id = "acc_davivienda_10",
            userId = testUserId,
            name = "Davivienda",
            type = "CHECKING",
            balance = 1_200_000.0,
            availableBalance = 1_200_000.0,
            currency = "COP"
        )
        financialDao.insertAccount(acc)

        val tx = TransactionEntity(
            id = "tx_mercado_10",
            userId = testUserId,
            accountId = acc.id,
            accountName = acc.name,
            categoryId = "cat_food",
            categoryName = "Alimentación",
            amount = 85_000.0,
            currency = "COP",
            type = "EXPENSE",
            date = "2026-10-18",
            description = "Compra Supermercado",
            receiptUrl = null,
            idempotencyKey = "tx_idem_market_10",
            createdAt = 1700000000000L
        )
        financialDao.insertTransaction(tx)

        val secLog = AuditSecurityLog(
            id = 101L,
            timestamp = 1700000001000L,
            eventType = "LOGIN_SUCCESS",
            authMethod = "BIOMETRIC",
            details = "Usuario autenticado con StrongBox"
        )
        auditSecurityDao.insertLog(secLog)

        val viewModel = AuditViewModel(
            auditSecurityLogDao = auditSecurityDao,
            assetAuditLogDao = assetAuditDao,
            financialDao = financialDao,
            fixedAssetDao = fixedAssetDao,
            userId = testUserId
        )

        // 2. Run integrity verification on genuine database
        viewModel.verifyVaultIntegrity()

        val genuineResult = viewModel.uiState.first { it.integrityVerificationResult != null }.integrityVerificationResult
        assertNotNull(genuineResult)
        assertTrue(
            "Expected 100% integrity confirmation but got: $genuineResult",
            genuineResult!!.contains("Bóveda 100% Íntegra") && genuineResult.contains("verificados criptográficamente contra SQLite")
        )

        // 3. TAMPER WITH DATABASE: Directly modify transaction amount via raw SQLite execSQL (bypassing Room triggers)
        database.openHelper.writableDatabase.execSQL("UPDATE transactions SET amount = 999999.0 WHERE id = 'tx_mercado_10'")

        // 4. Run integrity verification again
        viewModel.clearIntegrityResult()
        viewModel.verifyVaultIntegrity()

        val tamperedResult = viewModel.uiState.first { it.integrityVerificationResult != null }.integrityVerificationResult
        assertNotNull(tamperedResult)
        assertTrue(
            "Expected tampering alert but got: $tamperedResult",
            tamperedResult!!.contains("Alerta de Integridad") && tamperedResult.contains("inconsistencias o firmas hash adulteradas")
        )
    }

    @Test
    fun testRoomSchemaExport_schema37FileGeneratedAndValid() {
        val schemaFile = listOf(
            File("../core-database/schemas/com.example.data.local.SecurityDatabase/37.json"),
            File("core-database/schemas/com.example.data.local.SecurityDatabase/37.json"),
            File("schemas/com.example.data.local.SecurityDatabase/37.json")
        ).firstOrNull { it.exists() }

        assertNotNull("Room schema file 37.json must be generated by KSP in core-database/schemas", schemaFile)
        assertTrue("Room schema file 37.json must exist", schemaFile!!.exists())
        assertTrue("Schema file must be non-empty", schemaFile.length() > 50_000)

        val content = schemaFile.readText(Charsets.UTF_8)
        assertTrue("Schema must record version 37", content.contains("\"version\": 37"))
        assertTrue("Schema must declare financial_goals table", content.contains("\"tableName\": \"financial_goals\""))
        assertTrue("Schema must declare transactions table", content.contains("\"tableName\": \"transactions\""))
        assertTrue("Schema must declare accounts table", content.contains("\"tableName\": \"accounts\""))
    }
}
