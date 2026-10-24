package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.datasource.FirebaseDataSource
import com.example.data.local.KeystoreCryptoManager
import com.example.data.local.SecurityDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.FinancialRepositoryImpl
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
class CloudBackupUninstallAndRestoreTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testCloudCryptoSurvivesDeviceUninstallAndKeystoreDestruction() {
        val userSeed = "firebase_uid_andres_777"
        val sensitiveData = """{"accounts":[{"id":"acc_1","name":"Bancolombia","balance":5500000.0}],"status":"ok"}"""

        // 1. Device A: Initial install generates backup with user seed
        val deviceACryptoManager = KeystoreCryptoManager(allowSoftwareFallbackForTests = true)
        val encryptedPayload = deviceACryptoManager.encryptCloudPayload(sensitiveData, userSeed)
        assertNotNull(encryptedPayload)
        assertNotEquals(sensitiveData, encryptedPayload)

        // 2. Simulate App Uninstall:
        // Android KeyStore hardware key is destroyed on uninstall.
        // A brand new KeystoreCryptoManager is instantiated on clean install.
        val deviceBCryptoManager = KeystoreCryptoManager(allowSoftwareFallbackForTests = true)

        // 3. Device B / Reinstalled App: Decrypt using user seed
        val decryptedData = deviceBCryptoManager.decryptCloudPayload(encryptedPayload, userSeed)
        assertNotNull("El respaldo debe poder descifrarse tras desinstalar y reinstalar", decryptedData)
        assertEquals(sensitiveData, decryptedData)
    }

    @Test
    fun testFullPortfolioCloudRestoreAfterAppReinstall() = runBlocking {
        val userId = "firebase_user_restoration_888"

        // Device A DB & Repository
        val dbDeviceA = Room.inMemoryDatabaseBuilder(context, SecurityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val inMemoryCloudStorage = mutableMapOf<String, Map<String, Any>>()

        val cloudDataSourceDeviceA = object : FirebaseDataSource(context) {
            override fun getCurrentUserId(): String = userId
            override suspend fun syncOfflineFinancialBackup(targetUserId: String, backupPayload: Map<String, Any>): Boolean {
                inMemoryCloudStorage[targetUserId] = backupPayload
                return true
            }
            override suspend fun getFinancialBackup(targetUserId: String): Map<String, Any>? {
                return inMemoryCloudStorage[targetUserId]
            }
        }

        val cryptoManagerDeviceA = KeystoreCryptoManager(allowSoftwareFallbackForTests = true)
        val repoDeviceA = FinancialRepositoryImpl(
            financialDao = dbDeviceA.financialDao(),
            auditLogDao = dbDeviceA.auditSecurityLogDao(),
            firebaseDataSource = cloudDataSourceDeviceA,
            keystoreCryptoManager = cryptoManagerDeviceA
        )

        // Add real user accounts on Device A
        val acc1 = AccountEntity(
            id = "acc_bancolombia",
            userId = userId,
            name = "Bancolombia Ahorros",
            balance = 12_500_000.0,
            availableBalance = 12_500_000.0,
            type = "SAVINGS"
        )
        val acc2 = AccountEntity(
            id = "acc_nequi",
            userId = userId,
            name = "Nequi",
            balance = 3_200_000.0,
            availableBalance = 3_200_000.0,
            type = "WALLET"
        )
        dbDeviceA.financialDao().insertAccounts(listOf(acc1, acc2))

        val tx1 = TransactionEntity(
            id = "tx_sueldo_oct",
            userId = userId,
            accountId = acc1.id,
            accountName = acc1.name,
            categoryId = "cat_salario",
            categoryName = "Nómina",
            amount = 12_500_000.0,
            currency = "COP",
            type = "INCOME",
            date = "2026-10-24",
            description = "Pago quincena",
            receiptUrl = null,
            idempotencyKey = "tx_sueldo_oct_idem"
        )
        dbDeviceA.financialDao().insertTransaction(tx1)

        // Device A triggers cloud backup
        val backupResult = repoDeviceA.backupFullPortfolioToCloud(userId)
        assertTrue(backupResult.isSuccess)
        assertTrue(inMemoryCloudStorage.containsKey(userId))
        val storedDoc = inMemoryCloudStorage[userId]!!
        assertTrue(storedDoc.containsKey("encryptedPayload"))

        dbDeviceA.close()

        // -------------------------------------------------------------
        // SIMULATE UNINSTALL & REINSTALL:
        // Fresh database (empty, 0 accounts), fresh KeystoreCryptoManager
        // -------------------------------------------------------------
        val dbReinstalled = Room.inMemoryDatabaseBuilder(context, SecurityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val cryptoManagerReinstalled = KeystoreCryptoManager(allowSoftwareFallbackForTests = true)

        val cloudDataSourceReinstalled = object : FirebaseDataSource(context) {
            override fun getCurrentUserId(): String = userId
            override suspend fun syncOfflineFinancialBackup(targetUserId: String, backupPayload: Map<String, Any>): Boolean {
                inMemoryCloudStorage[targetUserId] = backupPayload
                return true
            }
            override suspend fun getFinancialBackup(targetUserId: String): Map<String, Any>? {
                return inMemoryCloudStorage[targetUserId]
            }
        }

        val repoReinstalled = FinancialRepositoryImpl(
            financialDao = dbReinstalled.financialDao(),
            auditLogDao = dbReinstalled.auditSecurityLogDao(),
            firebaseDataSource = cloudDataSourceReinstalled,
            keystoreCryptoManager = cryptoManagerReinstalled
        )

        // Initially on reinstallation, local accounts are 0
        val accountsBefore = dbReinstalled.financialDao().getAccounts(userId)
        assertEquals(0, accountsBefore.size)

        // Reinstall runs restore from cloud
        val restoreResult = repoReinstalled.restoreFinancialBackupFromCloudIfNeeded(userId)
        assertTrue("La restauración desde la nube debe ser exitosa", restoreResult.isSuccess)
        assertTrue("Debe retornar true indicando que se restauraron datos", restoreResult.getOrDefault(false))

        // Assert accounts are fully restored with exact balances
        val accountsAfter = dbReinstalled.financialDao().getAccounts(userId)
        assertEquals(2, accountsAfter.size)
        val bancolombia = accountsAfter.find { it.id == "acc_bancolombia" }
        assertNotNull(bancolombia)
        assertEquals(12_500_000.0, bancolombia!!.balance, 0.001)

        val nequi = accountsAfter.find { it.id == "acc_nequi" }
        assertNotNull(nequi)
        assertEquals(3_200_000.0, nequi!!.balance, 0.001)

        val txsAfter = dbReinstalled.financialDao().getTransactions(userId)
        assertEquals(1, txsAfter.size)
        assertEquals("tx_sueldo_oct", txsAfter.first().id)
        assertEquals(12_500_000.0, txsAfter.first().amount, 0.001)

        dbReinstalled.close()
    }
}
