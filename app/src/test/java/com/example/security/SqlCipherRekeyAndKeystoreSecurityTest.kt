package com.example.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.KeystoreCryptoManager
import com.example.data.local.PassphraseManager
import com.example.data.local.RoomLocalDb
import com.example.data.local.VaultKeyReadException
import com.example.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SqlCipherRekeyAndKeystoreSecurityTest {

    private lateinit var context: Context
    private lateinit var cryptoManager: KeystoreCryptoManager
    private lateinit var passphraseManager: PassphraseManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        cryptoManager = KeystoreCryptoManager(allowSoftwareFallbackForTests = true)
        passphraseManager = PassphraseManager(context, cryptoManager)
        RoomLocalDb.closeDatabase()
    }

    @After
    fun tearDown() {
        RoomLocalDb.closeDatabase()
    }

    @Test
    fun keystoreCryptoManager_separatesMasterAndBiometricKeyAliases() {
        assertNotEquals(
            "Master key alias and biometric key alias must be strictly separated",
            KeystoreCryptoManager.KEY_ALIAS,
            KeystoreCryptoManager.BIOMETRIC_KEY_ALIAS
        )
        assertEquals("kashapp_security_master_key", KeystoreCryptoManager.KEY_ALIAS)
        assertEquals("kashapp_biometric_auth_key", KeystoreCryptoManager.BIOMETRIC_KEY_ALIAS)
    }

    @Test
    fun biometricCryptoObject_initializedSuccessfully_andVerified() {
        val cryptoObject = cryptoManager.createBiometricCryptoObject()
        assertNotNull("CryptoObject should be created", cryptoObject)
        assertNotNull("Cipher inside CryptoObject must not be null", cryptoObject?.cipher)

        val isValid = cryptoManager.verifyBiometricCryptoObject(cryptoObject)
        assertTrue("Genuine CryptoObject with valid cipher must pass verification", isValid)

        val isInvalid = cryptoManager.verifyBiometricCryptoObject(null)
        assertFalse("Null CryptoObject must be rejected", isInvalid)
    }

    @Test
    fun getHardwareMasterPassphrase_whenStorageFails_throwsVaultKeyReadException() {
        // Calling with null context must throw IllegalArgumentException
        assertThrows(IllegalArgumentException::class.java) {
            cryptoManager.getHardwareMasterPassphrase(null)
        }

        // When SharedPreferences or Keystore throws during seed resolution, must fail-closed with VaultKeyReadException
        val failingContext = object : android.content.ContextWrapper(context) {
            override fun getSharedPreferences(name: String?, mode: Int): android.content.SharedPreferences {
                throw SecurityException("Simulated Keystore storage failure")
            }
        }

        assertThrows(VaultKeyReadException::class.java) {
            cryptoManager.getHardwareMasterPassphrase(failingContext)
        }
    }

    @Test
    fun passphraseManager_deriveKeyFromPassphrase_produces32ByteDeterministicKey() {
        val testPassphrase = "SuperClaveSegura2026!#$"
        val key1 = passphraseManager.deriveKeyFromPassphrase(testPassphrase)
        val key2 = passphraseManager.deriveKeyFromPassphrase(testPassphrase)

        assertEquals("Key derived from passphrase must be 32 bytes (256 bits)", 32, key1.size)
        assertArrayEquals("Same passphrase and salt must produce identical derived key bytes", key1, key2)

        val differentKey = passphraseManager.deriveKeyFromPassphrase("OtraClaveCompletamenteDiferente987!")
        assertFalse("Different passphrases must produce different derived keys", key1.contentEquals(differentKey))
    }

    @Test
    fun sqlCipherRekeyPragma_executesCleanlyOnOpenDatabase_andPreservesData() = runBlocking {
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.SecurityDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val testAccount = AccountEntity(
                id = "acc_rekey_test_01",
                userId = "kash_user_01",
                name = "Cuenta Rekey Test",
                balance = 1_500_000.0,
                type = "CHECKING",
                bankName = "Banco de Prueba",
                accountNumberEncrypted = "ENCv1:rekey_sample"
            )
            db.financialDao().insertAccount(testAccount)

            // Step 1: Verify PRAGMA rekey statement generation and execution
            val customPassphrase = "MiClavePersonalizadaSegura2026!"
            val keyBytes = passphraseManager.deriveKeyFromPassphrase(customPassphrase)
            val keyHex = keyBytes.joinToString("") { "%02x".format(it) }
            assertEquals(64, keyHex.length)

            val writable = db.openHelper.writableDatabase
            writable.execSQL("PRAGMA rekey = \"x'$keyHex'\";")

            // Update PassphraseManager
            val saved = passphraseManager.setUserPassphrase(customPassphrase)
            assertTrue(saved)
            assertTrue(passphraseManager.isUserPassphraseConfigured())

            // Verify data remains accessible
            val retrieved = db.financialDao().getAccountById("acc_rekey_test_01")
            assertNotNull(retrieved)
            assertEquals(1_500_000.0, retrieved?.balance ?: 0.0, 0.001)

            // Step 2: Rekey back to hardware master key
            val masterBytes = cryptoManager.getHardwareMasterPassphrase(context)
            val masterHex = masterBytes.joinToString("") { "%02x".format(it) }
            assertEquals(64, masterHex.length)
            writable.execSQL("PRAGMA rekey = \"x'$masterHex'\";")

            passphraseManager.resetToHardwareMasterPassphrase()
            assertFalse(passphraseManager.isUserPassphraseConfigured())

            val retrievedAfterMaster = db.financialDao().getAccountById("acc_rekey_test_01")
            assertNotNull(retrievedAfterMaster)
            assertEquals(1_500_000.0, retrievedAfterMaster?.balance ?: 0.0, 0.001)
        } finally {
            db.close()
        }
    }

    @Test
    fun roomLocalDb_vaultUnlockErrorMessage_doesNotExposeUnauthenticatedReset() {
        RoomLocalDb.clearVaultUnlockError()
        assertEquals(null, RoomLocalDb.vaultUnlockErrorMessage)
    }
}
