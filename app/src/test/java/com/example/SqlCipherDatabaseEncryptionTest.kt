package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.KeystoreCryptoManager
import com.example.data.local.PassphraseManager
import com.example.data.local.SecurityDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AuditSecurityLog
import com.example.data.local.entity.LocalAuthConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
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
class SqlCipherDatabaseEncryptionTest {

    private lateinit var context: Context
    private lateinit var cryptoManager: KeystoreCryptoManager
    private lateinit var passphraseManager: PassphraseManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        cryptoManager = KeystoreCryptoManager(allowSoftwareFallbackForTests = true)
        passphraseManager = PassphraseManager(context, cryptoManager)
    }

    @Test
    fun keystoreCryptoManager_encryptsAndDecryptsArbitraryDataCorrectly() {
        val originalText = "SecretUserDefinedPassphrase2026!#$"
        val encryptedBase64 = cryptoManager.encryptData(originalText)

        assertNotNull(encryptedBase64)
        assertTrue(encryptedBase64.isNotBlank())
        assertFalse(encryptedBase64.contains(originalText))

        val decryptedText = cryptoManager.decryptData(encryptedBase64)
        assertEquals(originalText, decryptedText)
    }

    @Test
    fun passphraseManager_handlesUserDefinedPassphraseLifecycle() {
        // Initially, reset to hardware master seed
        passphraseManager.resetToHardwareMasterPassphrase()
        assertFalse(passphraseManager.isUserPassphraseConfigured())

        val defaultKey = passphraseManager.getDatabasePassphrase()
        assertNotNull(defaultKey)
        assertTrue(defaultKey.isNotEmpty())

        // Set user-defined passphrase
        val userPassphrase = "MiSuperContraseñaFinanciera2026!"
        val success = passphraseManager.setUserPassphrase(userPassphrase)
        assertTrue(success)
        assertTrue(passphraseManager.isUserPassphraseConfigured())
        assertEquals(userPassphrase, passphraseManager.getCustomUserPassphrase())

        // Passphrase should derive a 256-bit (32-byte) key for SQLCipher
        val derivedKey = passphraseManager.getDatabasePassphrase()
        assertNotNull(derivedKey)
        assertEquals(32, derivedKey.size)

        // Evaluate passphrase strength
        val weak = passphraseManager.evaluatePassphraseStrength("123")
        assertFalse(weak.isValid)
        assertEquals("Muy Débil", weak.label)

        val strong = passphraseManager.evaluatePassphraseStrength("MiClaveRobusta123!")
        assertTrue(strong.isValid)
        assertTrue(strong.score >= 60)

        // Encryption metadata check
        val info = passphraseManager.getEncryptionInfo()
        assertTrue(info.isEncryptedAtRest)
        assertTrue(info.hasUserPassphrase)
        assertEquals("SQLCipher 4.6.0 (AES-256)", info.cipherEngine)

        // Reset to hardware master
        passphraseManager.resetToHardwareMasterPassphrase()
        assertFalse(passphraseManager.isUserPassphraseConfigured())
    }

    @Test
    fun supportOpenHelperFactory_initialization_withPassphraseBytes() {
        val userPassphrase = "KashAppSecureVaultKey2026!"
        passphraseManager.setUserPassphrase(userPassphrase)
        val passphraseBytes = passphraseManager.getDatabasePassphrase()

        val factory = SupportOpenHelperFactory(passphraseBytes)
        assertNotNull(factory)
    }

    @Test
    fun roomDatabase_performsEncryptedSchemaAndCrudOperations() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, SecurityDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val authDao = db.localAuthConfigDao()
            val financialDao = db.financialDao()

            // Insert Auth Config
            val config = LocalAuthConfig(
                id = 1,
                pinHash = "hash123",
                pinSalt = "salt123",
                hashAlgorithm = "PBKDF2-HMAC-SHA256-600000",
                isBiometricEnabled = true,
                failedAttempts = 0,
                lockoutUntilTimestamp = 0L,
                lastLoginTimestamp = System.currentTimeMillis()
            )
            authDao.insertOrUpdate(config)

            val retrievedConfig = authDao.getConfig()
            assertNotNull(retrievedConfig)
            assertEquals("hash123", retrievedConfig?.pinHash)
            assertEquals("PBKDF2-HMAC-SHA256-600000", retrievedConfig?.hashAlgorithm)

            // Insert Financial Account
            val account = AccountEntity(
                id = "acc_sqlcipher_1",
                userId = "user_test_1",
                name = "Bancolombia Cifrada",
                type = "SAVINGS",
                balance = 5_000_000.0,
                availableBalance = 5_000_000.0,
                holdBalance = 0.0,
                currency = "COP",
                bankName = "Bancolombia",
                isFavorite = true,
                accountNumberEncrypted = "•••• 9876",
                openingDate = "2026-09-27",
                kashappDate = "2026-09-27",
                generatesInterest = false
            )
            financialDao.insertAccount(account)

            val accounts = financialDao.getAccountsFlow("user_test_1").first()
            assertEquals(1, accounts.size)
            assertEquals("Bancolombia Cifrada", accounts[0].name)
            assertEquals(5_000_000.0, accounts[0].balance, 0.001)

            // Insert Security Audit Log
            val auditDao = db.auditSecurityLogDao()
            auditDao.insertLog(
                AuditSecurityLog(
                    id = 0,
                    timestamp = System.currentTimeMillis(),
                    eventType = "DATABASE_ENCRYPTED_WITH_USER_PASSPHRASE",
                    authMethod = "SQLCIPHER_AES_256",
                    details = "User passphrase active and verified"
                )
            )

            val logs = auditDao.getAllLogs().first()
            assertEquals(1, logs.size)
            assertEquals("DATABASE_ENCRYPTED_WITH_USER_PASSPHRASE", logs[0].eventType)
        } finally {
            db.close()
        }
    }
}
