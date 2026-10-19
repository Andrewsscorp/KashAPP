package com.example.security

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.KeystoreCryptoManager
import com.example.data.local.PassphraseManager
import com.example.data.local.VaultKeyDerivationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Arrays

/**
 * Suite de Pruebas de Seguridad para REM-P2-01 (HALL-08 / GATE-08).
 * Valida:
 * 1. Derivación estándar OWASP con 310.000 iteraciones PBKDF2-HMAC-SHA256 para nuevas claves.
 * 2. Migración criptográfica transparente de baúles V1 (64.000 iter) a V2 (310.000 iter) con PRAGMA rekey.
 * 3. Erradicación total del fallback silencioso a SHA-256 de 1 ronda (fail-closed con VaultKeyDerivationException).
 * 4. Eliminación de la persistencia de la frase cruda/reversible en almacenamiento secundario.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PassphraseManagerKdfMigrationTest {

    private lateinit var context: Context
    private lateinit var cryptoManager: KeystoreCryptoManager
    private lateinit var passphraseManager: PassphraseManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        cryptoManager = KeystoreCryptoManager(allowSoftwareFallbackForTests = true)
        passphraseManager = PassphraseManager(context, cryptoManager)
        passphraseManager.resetToHardwareMasterPassphrase()
    }

    @Test
    fun passphraseManager_uses310000Iterations_forNewUserPassphrases() {
        assertEquals(310_000, PassphraseManager.KDF_V2_ITERATIONS)
        assertEquals(2, PassphraseManager.CURRENT_KDF_VERSION)

        val userPass = "PasswordRobusto2026!OWASP"
        val success = passphraseManager.setUserPassphrase(userPass)
        assertTrue(success)
        assertTrue(passphraseManager.isUserPassphraseConfigured())
        assertEquals(2, passphraseManager.getKdfVersion())

        // La clave de 32 bytes debe coincidir con la derivación explícita de 310.000 iteraciones
        val derivedExpected = passphraseManager.deriveKeyFromPassphrase(userPass, PassphraseManager.KDF_V2_ITERATIONS)
        val activePassphrase = passphraseManager.getDatabasePassphrase()

        assertNotNull(activePassphrase)
        assertEquals(32, activePassphrase.size)
        assertTrue(Arrays.equals(derivedExpected, activePassphrase))

        // La frase cruda NO debe persistirse en texto plano ni reversible en V2
        assertNull(
            "VULNERABILITY: Raw user passphrase must NOT be stored in SharedPreferences in V2",
            passphraseManager.getCustomUserPassphrase()
        )

        val info = passphraseManager.getEncryptionInfo()
        assertTrue(info.keyDerivation.contains("310,000 iter"))
    }

    @Test
    fun passphraseManager_migratesLegacyV1ToV2_executingPragmaRekey() {
        val legacyPass = "MiClaveAntigua64k!"
        val legacyEncrypted = cryptoManager.encryptData(legacyPass)

        // Simulamos un estado heredado V1 en SharedPreferences
        val prefs = context.getSharedPreferences("kashapp_security_vault_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("encrypted_user_passphrase_v1", legacyEncrypted)
            .putInt("vault_kdf_version", 1)
            .putBoolean("has_custom_user_passphrase", true)
            .remove("encrypted_derived_vault_key_v2")
            .apply()

        assertEquals(1, passphraseManager.getKdfVersion())
        val v1ExpectedKey = passphraseManager.deriveKeyFromPassphrase(legacyPass, PassphraseManager.KDF_V1_ITERATIONS)
        val v1ActiveKey = passphraseManager.getDatabasePassphrase()
        assertTrue("V1 vault must resolve key using 64,000 iterations", Arrays.equals(v1ExpectedKey, v1ActiveKey))

        // Mock ligero con Proxy para verificar invocación de PRAGMA rekey sin dependencias externas
        var executedRekeySql: String? = null
        val fakeDb = java.lang.reflect.Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL") {
                executedRekeySql = args?.get(0) as? String
            }
            null
        } as SupportSQLiteDatabase

        // Ejecutar migración a V2
        val migrationSuccess = passphraseManager.migratePassphraseKdfV1ToV2(legacyPass, fakeDb)
        assertTrue("Migration from V1 to V2 must succeed", migrationSuccess)

        // Verificar que PRAGMA rekey fue ejecutado sobre la base de datos
        val v2ExpectedKey = passphraseManager.deriveKeyFromPassphrase(legacyPass, PassphraseManager.KDF_V2_ITERATIONS)
        val v2KeyHex = v2ExpectedKey.joinToString("") { "%02x".format(it) }
        assertEquals("PRAGMA rekey = \"x'$v2KeyHex'\";", executedRekeySql)

        // Verificar que la versión se actualizó a V2
        assertEquals(2, passphraseManager.getKdfVersion())

        // Verificar que ahora se usa la clave de 310.000 iteraciones
        val v2ActiveKey = passphraseManager.getDatabasePassphrase()
        assertTrue("V2 vault must now resolve key using 310,000 iterations", Arrays.equals(v2ExpectedKey, v2ActiveKey))

        // La clave V1 y la clave V2 deben ser criptográficamente diferentes
        assertFalse("V1 and V2 keys MUST NOT be identical", Arrays.equals(v1ExpectedKey, v2ExpectedKey))

        // La frase cruda heredada debe haber sido purgada
        assertNull("Legacy plaintext passphrase must be purged upon migration to V2", passphraseManager.getCustomUserPassphrase())
    }

    @Test
    fun passphraseManager_failsClosed_withoutInsecureSha256Fallback() {
        // Verificamos estáticamente que no exista MessageDigest.getInstance("SHA-256") en PassphraseManager.kt
        val possibleLocations = listOf(
            File("core-database/src/main/java/com/example/data/local/PassphraseManager.kt"),
            File("../core-database/src/main/java/com/example/data/local/PassphraseManager.kt")
        )
        val file = possibleLocations.firstOrNull { it.exists() }
        assertNotNull("PassphraseManager.kt must exist", file)

        val code = file!!.readText()
        assertFalse(
            "VULNERABILITY DETECTED: PassphraseManager MUST NOT contain MessageDigest.getInstance(\"SHA-256\") fallback",
            code.contains("MessageDigest.getInstance(\"SHA-256\")")
        )

        // Verificamos que parámetros ilegales lancen VaultKeyDerivationException y fallen de forma segura
        try {
            passphraseManager.deriveKeyFromPassphrase("valid_pass", iterations = -1)
            fail("Must throw VaultKeyDerivationException on illegal parameters")
        } catch (e: VaultKeyDerivationException) {
            assertTrue("Exception message must detail PBKDF2 failure", e.message?.contains("PBKDF2") == true)
        }
    }
}
