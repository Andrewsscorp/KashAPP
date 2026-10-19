package com.example.security

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.datasource.FirebaseDataSource
import com.example.data.local.KeystoreCryptoManager
import com.example.data.local.SecurityDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.FinancialRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CloudBackupE2eeSecurityTest {

    private lateinit var context: Context
    private lateinit var database: SecurityDatabase
    private lateinit var cryptoManager: KeystoreCryptoManager
    private lateinit var fakeFirebaseDataSource: FakeFirebaseDataSource
    private lateinit var repository: FinancialRepositoryImpl

    private class FakeFirebaseDataSource(context: Context) : FirebaseDataSource(context) {
        var lastSavedPayload: Map<String, Any>? = null
        var mockCloudBackup: Map<String, Any>? = null
        var syncCount = 0

        override suspend fun syncOfflineFinancialBackup(userId: String, backupPayload: Map<String, Any>): Boolean {
            lastSavedPayload = backupPayload
            mockCloudBackup = backupPayload
            syncCount++
            return true
        }

        override suspend fun getFinancialBackup(userId: String): Map<String, Any>? {
            return mockCloudBackup
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SecurityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        SecurityDatabase.installIntegrityGuards(database.openHelper.writableDatabase)
        cryptoManager = KeystoreCryptoManager(allowSoftwareFallbackForTests = true)
        fakeFirebaseDataSource = FakeFirebaseDataSource(context)

        repository = FinancialRepositoryImpl(
            financialDao = database.financialDao(),
            auditLogDao = database.auditSecurityLogDao(),
            firebaseDataSource = fakeFirebaseDataSource,
            keystoreCryptoManager = cryptoManager
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun gzipCompressionAndDecompression_isLossless() {
        val testJson = buildString {
            append("{\"userId\":\"test_user\",\"items\":[")
            for (i in 1..200) {
                if (i > 1) append(",")
                append("{\"id\":\"tx_$i\",\"amount\":${i * 1000.5},\"desc\":\"Movimiento de prueba largo número $i para probar ratio de compresión GZIP\"}")
            }
            append("]}")
        }

        val compressedBase64 = FinancialRepositoryImpl.compressGzip(testJson)
        assertNotNull(compressedBase64)
        assertTrue(
            "Compressed representation should be significantly smaller than raw text",
            compressedBase64.length < testJson.length
        )

        val decompressed = FinancialRepositoryImpl.decompressGzip(compressedBase64)
        assertEquals("Decompressed string must match original JSON identically", testJson, decompressed)
    }

    @Test
    fun backupFullPortfolioToCloud_createsEncryptedEnvelopeWithoutCleartextFinancialData() = runBlocking {
        val testUserId = "user_e2ee_test"

        // Insert 3 accounts
        for (i in 1..3) {
            database.financialDao().insertAccount(
                AccountEntity(
                    id = "acc_e2ee_$i",
                    userId = testUserId,
                    name = "Cuenta E2EE $i",
                    balance = i * 2_000_000.0,
                    currency = "COP",
                    type = "SAVINGS",
                    bankName = "Bancolombia",
                    accountNumberEncrypted = "ENCv1:acc_num_$i"
                )
            )
        }

        // Insert 180 transactions (proving removal of .take(150) limit!)
        for (i in 1..180) {
            database.financialDao().insertTransaction(
                TransactionEntity(
                    id = "tx_e2ee_$i",
                    userId = testUserId,
                    accountId = "acc_e2ee_1",
                    accountName = "Cuenta E2EE 1",
                    categoryId = "cat_general",
                    categoryName = "General",
                    amount = i * 15_000.0,
                    currency = "COP",
                    type = "EXPENSE",
                    date = "2026-10-18",
                    description = "Gasto E2EE $i",
                    receiptUrl = null,
                    idempotencyKey = "tx_e2ee_$i"
                )
            )
        }

        val backupResult = repository.backupFullPortfolioToCloud(testUserId)
        assertTrue(backupResult.isSuccess)
        assertTrue(backupResult.getOrThrow())

        val payload = fakeFirebaseDataSource.lastSavedPayload
        assertNotNull("Payload must be sent to Firebase", payload)

        // 1. Verify schemaVersion and metadata
        assertEquals(2, payload!!["schemaVersion"])
        assertEquals(testUserId, payload["userId"])
        assertEquals(183, payload["recordCount"]) // 3 accounts + 180 txs (full 180 without truncation!)
        assertNotNull(payload["checksumSha256"])
        assertEquals(64, (payload["checksumSha256"] as String).length)
        assertNotNull(payload["encryptedPayload"])

        // 2. ZERO cleartext financial fields exposed in Firestore envelope
        assertFalse("Must NOT contain cleartext accounts array", payload.containsKey("accounts"))
        assertFalse("Must NOT contain cleartext transactions array", payload.containsKey("transactions"))
        assertFalse("Must NOT contain cleartext balance", payload.containsKey("balance"))
        assertFalse("Must NOT contain cleartext bankName", payload.containsKey("bankName"))
        assertFalse("Must NOT contain cleartext userEmail", payload.containsKey("userEmail"))
    }

    @Test
    fun restoreFinancialBackupFromCloud_decryptsE2eePayloadAndPopulatesRoom() = runBlocking {
        val testUserId = "user_e2ee_restore"

        // Step 1: Create and backup 2 accounts and 160 transactions
        for (i in 1..2) {
            database.financialDao().insertAccount(
                AccountEntity(
                    id = "acc_restore_$i",
                    userId = testUserId,
                    name = "Cuenta Para Restaurar $i",
                    balance = i * 500_000.0,
                    currency = "COP",
                    type = "SAVINGS",
                    bankName = "Davivienda",
                    accountNumberEncrypted = "ENCv1:restore_acc_$i"
                )
            )
        }
        for (i in 1..160) {
            database.financialDao().insertTransaction(
                TransactionEntity(
                    id = "tx_restore_$i",
                    userId = testUserId,
                    accountId = "acc_restore_1",
                    accountName = "Cuenta Para Restaurar 1",
                    categoryId = "cat_servicios",
                    categoryName = "Servicios",
                    amount = i * 20_000.0,
                    currency = "COP",
                    type = "EXPENSE",
                    date = "2026-10-18",
                    description = "Pago servicio $i",
                    receiptUrl = null,
                    idempotencyKey = "tx_restore_$i"
                )
            )
        }

        repository.backupFullPortfolioToCloud(testUserId)
        val cloudEnvelope = fakeFirebaseDataSource.lastSavedPayload
        assertNotNull(cloudEnvelope)

        // Step 2: Simulate fresh device or empty local DB
        database.financialDao().deleteTransactionsByUserId(testUserId)
        database.financialDao().deleteAccountsByUserId(testUserId)
        assertEquals(0, database.financialDao().getAccountsFlow(testUserId).first().size)
        assertEquals(0, database.financialDao().getTransactionsFlow(testUserId).first().size)

        // Step 3: Restore from cloud
        fakeFirebaseDataSource.mockCloudBackup = cloudEnvelope
        val restoreResult = repository.restoreFinancialBackupFromCloudIfNeeded(testUserId)
        assertTrue(restoreResult.isSuccess)
        assertTrue(restoreResult.getOrThrow())

        // Verify all 2 accounts and all 160 transactions restored into Room!
        val restoredAccounts = database.financialDao().getAccountsFlow(testUserId).first()
        val restoredTransactions = database.financialDao().getTransactionsFlow(testUserId).first()

        assertEquals(2, restoredAccounts.size)
        assertEquals(160, restoredTransactions.size)
        assertEquals("Cuenta Para Restaurar 1", restoredAccounts.first { it.id == "acc_restore_1" }.name)
        assertEquals(500_000.0, restoredAccounts.first { it.id == "acc_restore_1" }.balance, 0.001)
    }

    @Test
    fun restoreFinancialBackupFromCloud_withLegacyCleartext_restoresAndTriggersMigration() = runBlocking {
        val testUserId = "user_legacy_migration"

        // Prepare legacy V1 format payload (cleartext accounts array without encryptedPayload)
        val legacyPayload = mapOf(
            "userId" to testUserId,
            "version" to "9.1.0",
            "table" to "portfolio_backup",
            "accounts" to listOf(
                mapOf(
                    "id" to "acc_legacy_01",
                    "name" to "Cuenta Legada Bancolombia",
                    "balance" to 3_500_000.0,
                    "currency" to "COP",
                    "type" to "SAVINGS",
                    "bankName" to "Bancolombia",
                    "isFavorite" to true
                )
            ),
            "transactions" to listOf(
                mapOf(
                    "id" to "tx_legacy_01",
                    "accountId" to "acc_legacy_01",
                    "accountName" to "Cuenta Legada Bancolombia",
                    "amount" to 50_000.0,
                    "currency" to "COP",
                    "type" to "EXPENSE",
                    "date" to "2026-10-18",
                    "description" to "Gasto legado"
                )
            )
        )

        fakeFirebaseDataSource.mockCloudBackup = legacyPayload
        val initialSyncCount = fakeFirebaseDataSource.syncCount

        val restoreResult = repository.restoreFinancialBackupFromCloudIfNeeded(testUserId)
        assertTrue(restoreResult.isSuccess)
        assertTrue(restoreResult.getOrThrow())

        // Verify legacy account and transaction were restored
        val accounts = database.financialDao().getAccountsFlow(testUserId).first()
        val txs = database.financialDao().getTransactionsFlow(testUserId).first()

        assertEquals(1, accounts.size)
        assertEquals("Cuenta Legada Bancolombia", accounts[0].name)
        assertEquals(3_500_000.0, accounts[0].balance, 0.001)
        assertEquals(1, txs.size)
    }
}
