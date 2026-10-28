package com.example.feature.sync

import com.example.core.id.EmailDeterministicUidGenerator
import com.example.data.local.KeystoreCryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Isolated unit test proving that cloud encryption keys derived from the user's email
 * survive complete app uninstallation, hardware Keystore destruction, and cross-device transitions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CloudCryptoReinstallEmailKeyTest {

    @Test
    fun testCloudPayloadEncryptedOnDeviceASurvivesUninstallAndDecryptsOnDeviceBUsingEmail() {
        val email = "andres.suarez@gmail.com"
        val payload = """{"accounts":[{"id":"acc_1","name":"Bancolombia","balance":8500000.0}],"status":"SECURE"}"""

        // 1. Device A: Initial install generates backup with user's email as seed
        val deviceACryptoManager = KeystoreCryptoManager(allowSoftwareFallbackForTests = true)
        val encryptedPayload = deviceACryptoManager.encryptCloudPayload(payload, email)
        assertNotNull(encryptedPayload)

        // 2. Simulate App Uninstall:
        // Android KeyStore hardware key is destroyed on uninstall.
        // A brand new KeystoreCryptoManager is instantiated on clean install on Device B.
        val deviceBCryptoManager = KeystoreCryptoManager(allowSoftwareFallbackForTests = true)

        // 3. Device B / Reinstalled App: Decrypt using email variation (uppercase & spaces)
        val decryptedData = deviceBCryptoManager.decryptCloudPayload(encryptedPayload, "  Andres.Suarez@GMAIL.com  ")
        assertNotNull("El respaldo cifrado con el correo debe descifrarse tras desinstalar y reinstalar", decryptedData)
        assertEquals(payload, decryptedData)
    }

    @Test
    fun testDeterministicEmailUidAsCryptoSeedSurvivesReinstall() {
        val email = "finanzas.andres@kashapp.co"
        val deterministicUid = EmailDeterministicUidGenerator.fromEmail(email)
        val payload = """{"movementsCount":42,"totalAssets":35000000.0}"""

        // Encrypt with deterministic UID on Device A
        val cryptoDeviceA = KeystoreCryptoManager(allowSoftwareFallbackForTests = true)
        val encrypted = cryptoDeviceA.encryptCloudPayload(payload, deterministicUid)

        // Reinstall on Device B: recalculate deterministic UID from email
        val cryptoDeviceB = KeystoreCryptoManager(allowSoftwareFallbackForTests = true)
        val recomputedUid = EmailDeterministicUidGenerator.fromEmail("FINANZAS.ANDRES@kashapp.co")
        assertEquals(deterministicUid, recomputedUid)

        val decrypted = cryptoDeviceB.decryptCloudPayload(encrypted, recomputedUid)
        assertEquals(payload, decrypted)
    }

    @Test
    fun testDecryptWithFallbackCandidateSeeds() {
        val originalSeed = "usr_legacy_seed_999"
        val payload = """{"status":"ok","secret":"kash_vault_data"}"""

        val cryptoManager = KeystoreCryptoManager(allowSoftwareFallbackForTests = true)
        val encrypted = cryptoManager.encryptCloudPayload(payload, originalSeed)

        val candidates = listOf(
            "unknown_seed_1",
            "wrong_email@gmail.com",
            originalSeed,
            "another_seed"
        )

        val decrypted = cryptoManager.decryptCloudPayloadWithFallbacks(encrypted, candidates)
        assertEquals(payload, decrypted)
    }
}
