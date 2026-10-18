package com.example.security

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.datasource.FirebaseDataSource
import com.example.data.local.KeystoreCryptoManager
import com.example.data.local.SecurityDatabase
import com.example.data.repository.FinancialRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AccountFieldEncryptionTest {

    private lateinit var database: SecurityDatabase
    private lateinit var context: Context
    private lateinit var cryptoManager: KeystoreCryptoManager
    private lateinit var repository: FinancialRepositoryImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SecurityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        SecurityDatabase.installIntegrityGuards(database.openHelper.writableDatabase)
        cryptoManager = KeystoreCryptoManager(allowSoftwareFallbackForTests = true)
        repository = FinancialRepositoryImpl(
            financialDao = database.financialDao(),
            auditLogDao = database.auditSecurityLogDao(),
            firebaseDataSource = FirebaseDataSource(context),
            keystoreCryptoManager = cryptoManager
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun encryptSensitiveField_encryptsWithEncV1PrefixAndCanBeDecrypted() {
        val rawAccountNumber = "1234-5678-9012-3456"
        val encrypted = repository.encryptSensitiveField(rawAccountNumber)

        assertNotNull(encrypted)
        assertTrue("El campo cifrado debe iniciar con el prefijo ENCv1:", encrypted!!.startsWith("ENCv1:"))
        assertNotEquals("El texto cifrado no debe contener el número de cuenta en claro", rawAccountNumber, encrypted)

        val decrypted = repository.decryptSensitiveField(encrypted)
        assertEquals("El número de cuenta descifrado debe coincidir con el original", rawAccountNumber, decrypted)
    }

    @Test
    fun decryptSensitiveField_transparentlyDecryptsLegacyBase64Records() {
        val rawAccountNumber = "9876543210123456"
        // Simular registro legado almacenado previamente como Base64 plano sin prefijo
        val legacyBase64 = Base64.getEncoder().encodeToString(rawAccountNumber.toByteArray(Charsets.UTF_8))

        val decrypted = repository.decryptSensitiveField(legacyBase64)
        assertEquals("Debe descifrar registros legados en Base64 sin pérdida de datos", rawAccountNumber, decrypted)
    }

    @Test
    fun decryptSensitiveField_returnsPlaintextIfNeitherEncV1NorBase64() {
        val plainText = "plain_unencoded_value"
        val decrypted = repository.decryptSensitiveField(plainText)
        assertEquals(plainText, decrypted)
    }

    @Test
    fun encryptAndDecrypt_handlesNullAndBlankGracefully() {
        assertNull(repository.encryptSensitiveField(null))
        assertNull(repository.encryptSensitiveField(""))
        assertNull(repository.encryptSensitiveField("   "))

        assertNull(repository.decryptSensitiveField(null))
        assertNull(repository.decryptSensitiveField(""))
        assertNull(repository.decryptSensitiveField("   "))
    }

    @Test
    fun createAccount_persistsEncryptedAccountNumberAndSyncTokenWithEncV1() = runBlocking {
        val rawAcc = "5555-4444-3333-2222"
        val rawToken = "oauth2_bearer_sync_token_secure_xyz"

        val account = repository.createAccount(
            name = "Cuenta de Ahorros Principal",
            type = "SAVINGS",
            balance = 2_500_000.0,
            currency = "COP",
            userId = "user_test_sec_01",
            bankName = "Bancolombia",
            accountNumber = rawAcc,
            syncAccessToken = rawToken
        )

        // 1. Verificar en memoria y base de datos que los campos están cifrados con ENCv1:
        assertNotNull(account.accountNumberEncrypted)
        assertTrue(account.accountNumberEncrypted!!.startsWith("ENCv1:"))
        assertFalse(account.accountNumberEncrypted!!.contains("5555"))

        assertNotNull(account.syncAccessTokenEncrypted)
        assertTrue(account.syncAccessTokenEncrypted!!.startsWith("ENCv1:"))
        assertFalse(account.syncAccessTokenEncrypted!!.contains("oauth2"))

        // 2. Verificar que la máscara PII es correcta
        assertEquals("•••• 2222", account.accountNumberMasked)

        // 3. Verificar que se puede recuperar y descifrar desde el repositorio
        val retrieved = repository.getAccountById(account.id)
        assertNotNull(retrieved)
        assertEquals(rawAcc, repository.decryptSensitiveField(retrieved!!.accountNumberEncrypted))
        assertEquals(rawToken, repository.decryptSensitiveField(retrieved.syncAccessTokenEncrypted))
    }
}
