package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.datasource.FirebaseDataSource
import com.example.data.local.SecurityDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserSessionEntity
import com.example.data.repository.FinancialRepositoryImpl
import com.example.presentation.dashboard.DashboardViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class UserDataRestorationAndDashboardSyncTest {

    private lateinit var database: SecurityDatabase
    private lateinit var financialRepository: FinancialRepositoryImpl
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SecurityDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val mockFirebaseDataSource = object : FirebaseDataSource(context) {
            override suspend fun getFinancialBackup(userId: String): Map<String, Any>? = null
        }

        financialRepository = FinancialRepositoryImpl(
            financialDao = database.financialDao(),
            auditLogDao = database.auditSecurityLogDao(),
            firebaseDataSource = mockFirebaseDataSource
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testMigrateLegacyUserDefaultDataRestoresStrandedAccounts() = runBlocking {
        val dao = database.financialDao()
        val targetUserId = "andres_firebase_uid_123"

        // 1. Arrange: Existing user accounts were saved under 'user_default'
        val legacyAccount = AccountEntity(
            id = "acc_bancolombia_real",
            userId = "user_default",
            name = "Bancolombia Ahorros",
            balance = 4_500_000.0,
            availableBalance = 4_500_000.0,
            type = "SAVINGS"
        )
        dao.insertAccount(legacyAccount)

        val legacyTx = TransactionEntity(
            id = "tx_salario_01",
            userId = "user_default",
            accountId = legacyAccount.id,
            accountName = legacyAccount.name,
            categoryId = "cat_salario",
            categoryName = "Salario",
            amount = 4_500_000.0,
            currency = "COP",
            type = "INCOME",
            date = "2026-10-20",
            description = "Pago nómina",
            receiptUrl = null,
            idempotencyKey = "idem_tx_01"
        )
        dao.insertTransaction(legacyTx)

        // Target user currently sees 0 accounts
        val beforeMigration = dao.getAccounts(targetUserId)
        assertTrue(beforeMigration.isEmpty())

        // 2. Act: Run migration
        val result = financialRepository.migrateLegacyUserDefaultDataIfNeeded(targetUserId)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrDefault(0) > 0)

        // 3. Assert: Accounts and transactions are now associated with targetUserId
        val afterMigration = dao.getAccounts(targetUserId)
        assertEquals(1, afterMigration.size)
        assertEquals("Bancolombia Ahorros", afterMigration.first().name)
        assertEquals(4_500_000.0, afterMigration.first().balance, 0.001)

        val txsAfter = dao.getTransactions(targetUserId)
        assertEquals(1, txsAfter.size)
        assertEquals("tx_salario_01", txsAfter.first().id)
    }

    @Test
    fun testMigrateLegacyDataDoesNotOverwriteExistingTargetAccounts() = runBlocking {
        val dao = database.financialDao()
        val targetUserId = "andres_firebase_uid_456"

        // Target user already has their own account
        dao.insertAccount(
            AccountEntity(
                id = "acc_existing_user",
                userId = targetUserId,
                name = "Cuenta Propia",
                balance = 1_000_000.0,
                type = "SAVINGS"
            )
        )

        // 'user_default' also has an account
        dao.insertAccount(
            AccountEntity(
                id = "acc_legacy_different",
                userId = "user_default",
                name = "Cuenta Vieja",
                balance = 500_000.0,
                type = "SAVINGS"
            )
        )

        val result = financialRepository.migrateLegacyUserDefaultDataIfNeeded(targetUserId)
        assertEquals(0, result.getOrDefault(0))

        val accounts = dao.getAccounts(targetUserId)
        assertEquals(1, accounts.size)
        assertEquals("acc_existing_user", accounts.first().id)
    }

    @Test
    fun testDashboardViewModelDisplaysUserNameAndCalculatesNetWorth() = runBlocking {
        val sessionDao = database.userSessionDao()
        val dao = database.financialDao()
        val testUserId = "user_test_dash_789"

        sessionDao.insertOrUpdateSession(
            UserSessionEntity(
                userId = testUserId,
                displayName = "Andrés Felipe Suárez",
                email = "cdosuarez@gmail.com",
                isActive = true
            )
        )

        dao.insertAccount(
            AccountEntity(
                id = "acc_nequi_01",
                userId = testUserId,
                name = "Nequi",
                balance = 2_800_000.0,
                type = "SAVINGS"
            )
        )

        val viewModel = DashboardViewModel(
            financialRepository = financialRepository,
            userSessionDao = sessionDao,
            userId = testUserId,
            initialUserName = "Andrés Felipe Suárez",
            initialUserEmail = "cdosuarez@gmail.com"
        )

        // Assert initial state is Andrés Felipe Suárez and NOT 'Usuario'
        val state = viewModel.uiState.value
        assertEquals("Andrés Felipe Suárez", state.userName)
        assertEquals("cdosuarez@gmail.com", state.userEmail)

        // Let the flow emit accounts
        val accounts = financialRepository.getAccounts(testUserId).first()
        assertEquals(1, accounts.size)
        assertEquals("Nequi", accounts.first().name)
    }
}
