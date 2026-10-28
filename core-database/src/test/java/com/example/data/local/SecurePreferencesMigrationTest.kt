package com.example.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Suite de pruebas unitarias para SecurePreferencesVault y migración atómica.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SecurePreferencesMigrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testAtomicMigrationOfAllPrimitiveTypesAndStringSet() {
        val legacyName = "test_legacy_plain_prefs"
        val targetName = "test_target_encrypted_prefs"

        val legacyPrefs = context.getSharedPreferences(legacyName, Context.MODE_PRIVATE)
        legacyPrefs.edit()
            .putBoolean("allow_screenshots", false)
            .putInt("vault_kdf_version", 2)
            .putLong("timestamp", 1729000000000L)
            .putFloat("ratio", 1.618f)
            .putString("owner", "andres@kash.app")
            .putStringSet("notif_days", setOf("3", "1", "0"))
            .commit()

        val targetPrefs = context.getSharedPreferences(targetName, Context.MODE_PRIVATE)

        val migratedCount = SecurePreferencesVault.migrateLegacyPreferencesAtomically(
            context = context,
            legacyPlainName = legacyName,
            targetEncryptedPrefs = targetPrefs
        )

        assertEquals(6, migratedCount)
        assertFalse(targetPrefs.getBoolean("allow_screenshots", true))
        assertEquals(2, targetPrefs.getInt("vault_kdf_version", 0))
        assertEquals(1729000000000L, targetPrefs.getLong("timestamp", 0L))
        assertEquals(1.618f, targetPrefs.getFloat("ratio", 0f), 0.001f)
        assertEquals("andres@kash.app", targetPrefs.getString("owner", null))
        assertEquals(setOf("3", "1", "0"), targetPrefs.getStringSet("notif_days", null))
        assertTrue(targetPrefs.getBoolean("__kash_encrypted_migration_completed_v1", false))

        // El SharedPreferences legado debe haber sido purgado
        val reloadedLegacy = context.getSharedPreferences(legacyName, Context.MODE_PRIVATE)
        assertTrue(reloadedLegacy.all.isEmpty())
    }

    @Test
    fun testIdempotentMigrationDoesNotReRunIfMarkerPresent() {
        val legacyName = "test_idempotent_legacy_prefs"
        val targetName = "test_idempotent_target_prefs"

        val targetPrefs = context.getSharedPreferences(targetName, Context.MODE_PRIVATE)
        targetPrefs.edit().putBoolean("__kash_encrypted_migration_completed_v1", true).commit()

        // Ejecutar createAndMigrate con targetPrefs
        val resultPrefs = SecurePreferencesVault.createAndMigrate(
            context = context,
            legacyPlainName = legacyName,
            encryptedFileName = targetName,
            customEncryptedPrefsForTest = targetPrefs
        )

        assertTrue(resultPrefs.getBoolean("__kash_encrypted_migration_completed_v1", false))
    }
}
