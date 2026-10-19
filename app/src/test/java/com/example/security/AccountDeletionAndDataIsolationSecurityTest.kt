package com.example.security

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.datasource.FirebaseDataSource
import com.example.data.datasource.GoogleDriveManager
import com.example.data.local.KeystoreCryptoManager
import com.example.data.local.SecurityDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AssetAuditLogEntity
import com.example.data.local.entity.AssetMovementEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.BudgetItemEntity
import com.example.data.local.entity.FinancialGoalEntity
import com.example.data.local.entity.FixedAssetEntity
import com.example.data.local.entity.IncomeSourceEntity
import com.example.data.local.entity.InvestmentEntity
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.OutboxOperationEntity
import com.example.data.local.entity.ScheduledPaymentEntity
import com.example.data.local.entity.SharedExpenseEntity
import com.example.data.local.entity.SharedExpenseLineItemEntity
import com.example.data.local.entity.SharedExpensePayerEntity
import com.example.data.local.entity.SharedExpenseSplitEntity
import com.example.data.local.entity.SharedGroupEntity
import com.example.data.local.entity.SharedGroupMemberEntity
import com.example.data.local.entity.SharedParticipantEntity
import com.example.data.local.entity.SharedSequenceConfigEntity
import com.example.data.local.entity.SharedSettlementPaymentEntity
import com.example.data.local.entity.ThirdPartyCategoryEntity
import com.example.data.local.entity.ThirdPartyEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserSessionEntity
import com.example.data.repository.AuthRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
class AccountDeletionAndDataIsolationSecurityTest {

    private lateinit var context: Context
    private lateinit var database: SecurityDatabase
    private lateinit var cryptoManager: KeystoreCryptoManager
    private lateinit var authRepository: AuthRepositoryImpl

    private val testDispatcher = UnconfinedTestDispatcher()
    private val userA = "user_alpha_101"
    private val userB = "user_bravo_202"

    private class MockFirebaseDataSource(context: Context) : FirebaseDataSource(context) {
        var cloudDeletedUser: String? = null
        override suspend fun deleteCloudAccountAndData(userId: String): Result<Unit> {
            cloudDeletedUser = userId
            return Result.success(Unit)
        }
    }

    private fun createTestTx(
        id: String,
        userId: String,
        accountId: String,
        amount: Double = 50.0
    ): TransactionEntity {
        return TransactionEntity(
            id = id,
            userId = userId,
            accountId = accountId,
            accountName = "Cuenta Test",
            categoryId = "cat_general",
            categoryName = "General",
            amount = amount,
            currency = "COP",
            type = "EXPENSE",
            date = "2026-10-18",
            description = "Test Tx",
            receiptUrl = null,
            idempotencyKey = "tx_key_$id"
        )
    }

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
        cryptoManager = KeystoreCryptoManager(allowSoftwareFallbackForTests = true)

        val fakeFirebase = MockFirebaseDataSource(context)
        val fakeDrive = GoogleDriveManager(context)

        authRepository = AuthRepositoryImpl(
            localAuthConfigDao = database.localAuthConfigDao(),
            auditSecurityLogDao = database.auditSecurityLogDao(),
            firebaseDataSource = fakeFirebase,
            googleDriveManager = fakeDrive,
            userSessionDao = database.userSessionDao(),
            financialDao = database.financialDao(),
            fixedAssetDao = database.fixedAssetDao(),
            sharedExpenseDao = database.sharedExpenseDao(),
            thirdPartyDao = database.thirdPartyDao(),
            thirdPartyCategoryDao = database.thirdPartyCategoryDao(),
            merchantDao = database.merchantDao(),
            outboxDao = database.outboxDao(),
            assetAuditLogDao = database.assetAuditLogDao(),
            assetCategoryDao = database.assetCategoryDao(),
            securityDatabase = database,
            context = context,
            keystoreCryptoManager = cryptoManager
        )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `enforces strict multi-tenant isolation across all DAOs`() = runTest(testDispatcher) {
        val finDao = database.financialDao()
        val assetDao = database.fixedAssetDao()
        val sharedDao = database.sharedExpenseDao()
        val tpDao = database.thirdPartyDao()

        // Seed data for User A
        finDao.insertAccount(AccountEntity(id = "acc_a", userId = userA, name = "Cuenta A", balance = 1000.0))
        finDao.insertTransaction(createTestTx(id = "tx_a", userId = userA, accountId = "acc_a", amount = 50.0))
        assetDao.insertAsset(
            FixedAssetEntity(
                id = "ast_a",
                userId = userA,
                name = "Laptop A",
                type = "TANGIBLE",
                category = "Tecnología",
                acquisitionDate = "2026-01-01",
                acquisitionValue = 1200.0
            )
        )
        assetDao.insertMovement(
            AssetMovementEntity(
                id = "mov_a",
                userId = userA,
                assetId = "ast_a",
                type = "DEPRECIATION",
                title = "Depreciación",
                amount = 100.0,
                date = "2026-10-18"
            )
        )
        sharedDao.upsertParticipant(
            SharedParticipantEntity(
                id = "part_a",
                userId = userA,
                displayName = "Participante A",
                nameNormalized = "PARTICIPANTE A"
            )
        )
        tpDao.insertThirdParty(
            ThirdPartyEntity(
                id = "tp_a",
                userId = userA,
                name = "Proveedor A",
                normalizedName = "PROVEEDOR A",
                idNumber = "123456789"
            )
        )

        // Seed data for User B
        finDao.insertAccount(AccountEntity(id = "acc_b", userId = userB, name = "Cuenta B", balance = 2000.0))
        finDao.insertTransaction(createTestTx(id = "tx_b", userId = userB, accountId = "acc_b", amount = 150.0))
        assetDao.insertAsset(
            FixedAssetEntity(
                id = "ast_b",
                userId = userB,
                name = "Vehiculo B",
                type = "TANGIBLE",
                category = "Vehículos",
                acquisitionDate = "2026-01-01",
                acquisitionValue = 35000.0
            )
        )
        assetDao.insertMovement(
            AssetMovementEntity(
                id = "mov_b",
                userId = userB,
                assetId = "ast_b",
                type = "DEPRECIATION",
                title = "Depreciación",
                amount = 500.0,
                date = "2026-10-18"
            )
        )
        sharedDao.upsertParticipant(
            SharedParticipantEntity(
                id = "part_b",
                userId = userB,
                displayName = "Participante B",
                nameNormalized = "PARTICIPANTE B"
            )
        )
        tpDao.insertThirdParty(
            ThirdPartyEntity(
                id = "tp_b",
                userId = userB,
                name = "Proveedor B",
                normalizedName = "PROVEEDOR B",
                idNumber = "987654321"
            )
        )

        // Verify User A only sees User A records
        val accountsA = finDao.getAccountsFlow(userA).first()
        assertEquals(1, accountsA.size)
        assertEquals("acc_a", accountsA[0].id)

        val txsA = finDao.getTransactionsFlow(userA).first()
        assertEquals(1, txsA.size)
        assertEquals("tx_a", txsA[0].id)

        val assetsA = assetDao.observeAssets(userA).first()
        assertEquals(1, assetsA.size)
        assertEquals("ast_a", assetsA[0].id)

        val movsA = assetDao.observeAllMovements(userA).first()
        assertEquals(1, movsA.size)
        assertEquals("mov_a", movsA[0].id)

        val partsA = sharedDao.observeActiveParticipants(userA).first()
        assertEquals(1, partsA.size)
        assertEquals("part_a", partsA[0].id)

        val tpsA = tpDao.getActiveThirdPartiesFlow(userA).first()
        assertEquals(1, tpsA.size)
        assertEquals("tp_a", tpsA[0].id)

        // Verify User B only sees User B records
        val accountsB = finDao.getAccountsFlow(userB).first()
        assertEquals(1, accountsB.size)
        assertEquals("acc_b", accountsB[0].id)

        val txsB = finDao.getTransactionsFlow(userB).first()
        assertEquals(1, txsB.size)
        assertEquals("tx_b", txsB[0].id)

        val assetsB = assetDao.observeAssets(userB).first()
        assertEquals(1, assetsB.size)
        assertEquals("ast_b", assetsB[0].id)

        // Verify empty userId query returns empty list (no leakage to unauthenticated caller)
        val accountsEmpty = finDao.getAccountsFlow("").first()
        assertTrue("Empty userId must return 0 accounts", accountsEmpty.isEmpty())

        val txsEmpty = finDao.getTransactionsFlow("").first()
        assertTrue("Empty userId must return 0 transactions", txsEmpty.isEmpty())

        val assetsEmpty = assetDao.observeAssets("").first()
        assertTrue("Empty userId must return 0 fixed assets", assetsEmpty.isEmpty())
    }

    @Test
    fun `deleteAccountPermanently deletes full financial graph without SQLite trigger violation and leaves zero orphans`() = runTest(testDispatcher) {
        val finDao = database.financialDao()
        val assetDao = database.fixedAssetDao()
        val assetAuditDao = database.assetAuditLogDao()
        val sharedDao = database.sharedExpenseDao()
        val tpDao = database.thirdPartyDao()
        val tpcDao = database.thirdPartyCategoryDao()
        val merchantDao = database.merchantDao()
        val outboxDao = database.outboxDao()
        val sessionDao = database.userSessionDao()

        // 1. Seed User A with complete interdependent relational graph
        finDao.insertAccount(AccountEntity(id = "acc_a1", userId = userA, name = "Ahorros A1", balance = 5000.0))
        finDao.insertTransaction(createTestTx(id = "tx_a1", userId = userA, accountId = "acc_a1", amount = 120.0))
        finDao.insertGoal(
            FinancialGoalEntity(
                id = "goal_a1",
                userId = userA,
                name = "Meta A",
                targetAccountId = "acc_a1",
                targetAccountName = "Ahorros A1",
                targetAmount = 10000.0
            )
        )
        finDao.insertInvestment(
            InvestmentEntity(
                id = "inv_a1",
                userId = userA,
                name = "CDT A",
                typeId = "RENTA_FIJA",
                typeName = "CDT",
                acquisitionDate = "2026-10-01",
                initialAmount = 2000.0,
                currentValue = 2000.0,
                originAccountId = "acc_a1",
                originAccountName = "Ahorros A1"
            )
        )
        finDao.insertScheduledPayment(
            ScheduledPaymentEntity(
                id = "sp_a1",
                userId = userA,
                sourceAccountId = "acc_a1",
                entityName = "Renta",
                amount = 1000.0,
                dueDate = "2026-10-30"
            )
        )
        
        finDao.insertBudget(
            BudgetEntity(
                id = "bud_a1",
                userId = userA,
                name = "Presupuesto Octubre",
                startDate = "2026-10-01",
                endDate = "2026-10-31",
                totalAmount = 3000.0
            )
        )
        finDao.insertBudgetItems(
            listOf(
                BudgetItemEntity(
                    id = "bi_a1",
                    budgetId = "bud_a1",
                    categoryId = "cat_1",
                    categoryName = "Alimentos",
                    allocatedAmount = 500.0
                )
            )
        )
        finDao.insertIncomeSource(IncomeSourceEntity(id = "is_a1", userId = userA, name = "Sueldo"))

        assetDao.insertAsset(
            FixedAssetEntity(
                id = "ast_a1",
                userId = userA,
                name = "Maquinaria",
                type = "TANGIBLE",
                category = "Maquinaria",
                acquisitionDate = "2026-01-01",
                acquisitionValue = 15000.0
            )
        )
        assetDao.insertMovement(
            AssetMovementEntity(
                id = "mov_a1",
                userId = userA,
                assetId = "ast_a1",
                type = "MAINTENANCE",
                title = "Mantenimiento Preventivo",
                amount = 250.0,
                date = "2026-10-18"
            )
        )
        assetAuditDao.insertAuditLog(
            AssetAuditLogEntity(
                id = "aal_a1",
                userId = userA,
                assetId = "ast_a1",
                actionType = "CREACION",
                actionTitle = "Alta",
                details = "Registro contable inicial",
                formattedDate = "2026-10-18 20:00:00"
            )
        )

        sharedDao.upsertParticipant(
            SharedParticipantEntity(
                id = "part_a1",
                userId = userA,
                displayName = "Compañero A",
                nameNormalized = "COMPANERO A"
            )
        )
        sharedDao.upsertGroup(
            SharedGroupEntity(
                id = "grp_a1",
                userId = userA,
                name = "Viaje A",
                nameNormalized = "VIAJE A",
                groupCode = "VIAJE"
            )
        )
        sharedDao.upsertGroupMembers(listOf(SharedGroupMemberEntity(groupId = "grp_a1", participantId = "part_a1")))
        sharedDao.upsertSequenceConfig(SharedSequenceConfigEntity(id = "seq_a1", userId = userA, sequenceType = "EXPENSE"))
        sharedDao.insertSharedExpenseHeader(
            SharedExpenseEntity(
                id = "exp_a1",
                userId = userA,
                consecutiveCode = "EXP-001",
                consecutiveNormalized = "EXP-001",
                receiptTicketCode = "TK-001",
                title = "Cena A",
                categoryId = "cat_rest",
                categoryNameSnapshot = "Restaurantes",
                expenseDate = "2026-10-18",
                expenseTime = "20:00:00",
                subtotalAmount = 80.0,
                totalAmount = 80.0,
                ticketIntegrityHash = "0123456789abcdef",
                idempotencyKey = "exp_key_a1"
            )
        )
        sharedDao.insertSharedExpensePayers(listOf(SharedExpensePayerEntity(id = "pyr_a1", sharedExpenseId = "exp_a1", participantId = "part_a1", paidAmount = 80.0)))
        sharedDao.insertSharedExpenseSplits(
            listOf(
                SharedExpenseSplitEntity(
                    id = "spl_a1",
                    sharedExpenseId = "exp_a1",
                    participantId = "part_a1",
                    baseSubtotalShare = 40.0,
                    totalOwedShare = 40.0,
                    paidByThisParticipant = 80.0,
                    netBalanceForExpense = 40.0,
                    remainingPendingAmount = 0.0
                )
            )
        )
        sharedDao.insertSharedExpenseLineItems(
            listOf(
                SharedExpenseLineItemEntity(
                    id = "li_a1",
                    sharedExpenseId = "exp_a1",
                    itemOrder = 1,
                    description = "Plato",
                    unitPrice = 80.0,
                    lineTotalPrice = 80.0,
                    assignedParticipantIdsCsv = "part_a1"
                )
            )
        )
        sharedDao.insertSettlementPayment(
            SharedSettlementPaymentEntity(
                id = "set_a1",
                userId = userA,
                voucherConsecutiveCode = "VOU-001",
                fromParticipantId = "part_a1",
                toParticipantId = "part_a1",
                amount = 40.0,
                settlementMethod = "CASH",
                ticketIntegrityHash = "abcdef0123456789",
                paymentDate = "2026-10-18",
                paymentTime = "21:00:00",
                idempotencyKey = "set_key_a1"
            )
        )

        tpDao.insertThirdParty(
            ThirdPartyEntity(
                id = "tp_a1",
                userId = userA,
                name = "Tercero A",
                normalizedName = "TERCERO A",
                idNumber = "11223344"
            )
        )
        tpcDao.insertCategory(ThirdPartyCategoryEntity(id = "tpc_a1", userId = userA, name = "Cat A", code = "CAT_A", isSystem = false))
        merchantDao.insertMerchant(MerchantEntity(id = "m_a1", userId = userA, canonicalName = "Tienda A"))
        outboxDao.insertOperation(OutboxOperationEntity(id = "oo_a1", userId = userA, entityType = "TRANSACTION", entityId = "tx_a1", operation = "CREATE", payloadJson = "{}", idempotencyKey = "key_a1"))
        sessionDao.insertOrUpdateSession(UserSessionEntity(userId = userA, displayName = "Usuario Alpha", email = "alpha@kashapp.com", authProvider = "PASSWORD"))

        // Create dummy local file
        val hiddenDir = File(context.filesDir, "hidden_docs")
        hiddenDir.mkdirs()
        val dummyDoc = File(hiddenDir, "doc_user_a.enc")
        dummyDoc.writeText("SECRET_DOCUMENT_USER_A")
        assertTrue(dummyDoc.exists())

        // 2. Seed User B records (control group)
        finDao.insertAccount(AccountEntity(id = "acc_b1", userId = userB, name = "Cuenta B1", balance = 9999.0))
        finDao.insertTransaction(createTestTx(id = "tx_b1", userId = userB, accountId = "acc_b1", amount = 300.0))
        sessionDao.insertOrUpdateSession(UserSessionEntity(userId = userB, displayName = "Usuario Bravo", email = "bravo@kashapp.com", authProvider = "GOOGLE"))

        // 3. Execute permanent account deletion for User A
        val deleteResult = authRepository.deleteAccountPermanently(userA)
        assertTrue("Account deletion must succeed without trigger errors: ${deleteResult.exceptionOrNull()?.message}", deleteResult.isSuccess)

        // 4. Verify User A has ZERO records left across all DAOs
        assertEquals(0, finDao.getAccounts(userA).size)
        assertEquals(0, finDao.getTransactions(userA).size)
        assertNull(finDao.getGoalById("goal_a1"))
        assertNull(finDao.getInvestmentById("inv_a1"))
        assertEquals(0, finDao.getPendingScheduledPayments(userA).size)
        assertNull(finDao.getBudgetById("bud_a1"))
        assertEquals(0, finDao.getIncomeSources(userA).size)

        assertEquals(0, assetDao.observeAssets(userA).first().size)
        assertEquals(0, assetDao.observeAllMovements(userA).first().size)
        assertEquals(0, assetAuditDao.getAllAuditLogs(userA).size)

        assertEquals(0, sharedDao.getActiveParticipants(userA).size)
        assertEquals(0, sharedDao.observeGroups(userA).first().size)
        assertEquals(0, sharedDao.observeAllSharedExpensesWithDetails(userA).first().size)
        assertEquals(0, sharedDao.observeSettlementPayments(userA).first().size)

        assertEquals(0, tpDao.getActiveThirdPartiesFlow(userA).first().size)
        assertEquals(0, tpcDao.getCategories(userA).filter { !it.isSystem }.size)
        assertEquals(0, outboxDao.observePendingCount().first())
        assertNull(sessionDao.getSessionByUserId(userA))

        // Verify local file was purged
        assertFalse(dummyDoc.exists())

        // 5. Verify User B was NOT touched at all
        val userBAccounts = finDao.getAccounts(userB)
        assertEquals(1, userBAccounts.size)
        assertEquals("acc_b1", userBAccounts[0].id)
        assertNotNull(sessionDao.getSessionByUserId(userB))
    }

    @Test
    fun `deleteAccountPermanently rejects blank userId`() = runTest(testDispatcher) {
        val result = authRepository.deleteAccountPermanently("   ")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
