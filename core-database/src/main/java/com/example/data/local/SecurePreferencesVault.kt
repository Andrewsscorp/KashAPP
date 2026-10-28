package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.io.File

/**
 * Proveedor centralizado de SharedPreferences cifradas (AES256_SIV para claves, AES256_GCM para valores)
 * con migración atómica y purga física de archivos XML legados en texto plano.
 */
object SecurePreferencesVault {

    private const val TAG = "SecurePreferencesVault"
    private const val MIGRATION_MARKER_KEY = "__kash_encrypted_migration_completed_v1"

    @Volatile
    private var cachedVaultPrefs: SharedPreferences? = null

    @Volatile
    private var cachedSettingsPrefs: SharedPreferences? = null

    /**
     * Obtiene las preferencias cifradas para la bóveda criptográfica (PassphraseManager / Keystore).
     */
    fun getEncryptedVaultPreferences(context: Context): SharedPreferences {
        return cachedVaultPrefs ?: synchronized(this) {
            cachedVaultPrefs ?: createAndMigrate(
                context = context.applicationContext,
                legacyPlainName = "kashapp_security_vault_prefs",
                encryptedFileName = "kashapp_security_vault_encrypted_prefs"
            ).also { cachedVaultPrefs = it }
        }
    }

    /**
     * Obtiene las preferencias cifradas para la configuración empresarial del usuario (SettingsRepository).
     */
    fun getEncryptedSettingsPreferences(context: Context): SharedPreferences {
        return cachedSettingsPrefs ?: synchronized(this) {
            cachedSettingsPrefs ?: createAndMigrate(
                context = context.applicationContext,
                legacyPlainName = "kashapp_enterprise_settings_prefs",
                encryptedFileName = "kashapp_enterprise_settings_encrypted_prefs"
            ).also { cachedSettingsPrefs = it }
        }
    }

    internal fun createAndMigrate(
        context: Context,
        legacyPlainName: String,
        encryptedFileName: String,
        customEncryptedPrefsForTest: SharedPreferences? = null
    ): SharedPreferences {
        val encryptedPrefs = customEncryptedPrefsForTest ?: buildEncryptedPrefs(context, encryptedFileName)

        if (!encryptedPrefs.getBoolean(MIGRATION_MARKER_KEY, false)) {
            migrateLegacyPreferencesAtomically(
                context = context,
                legacyPlainName = legacyPlainName,
                targetEncryptedPrefs = encryptedPrefs
            )
        }
        return encryptedPrefs
    }

    private fun buildEncryptedPrefs(context: Context, encryptedFileName: String): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            encryptedFileName,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Transfiere todas las claves de un SharedPreferences plano legado hacia el contenedor cifrado
     * y destruye el archivo XML plano original.
     */
    @Suppress("UNCHECKED_CAST")
    internal fun migrateLegacyPreferencesAtomically(
        context: Context,
        legacyPlainName: String,
        targetEncryptedPrefs: SharedPreferences
    ): Int {
        val legacyPrefs = context.getSharedPreferences(legacyPlainName, Context.MODE_PRIVATE)
        val allEntries = legacyPrefs.all

        if (allEntries.isEmpty()) {
            targetEncryptedPrefs.edit().putBoolean(MIGRATION_MARKER_KEY, true).commit()
            deleteLegacyXmlFile(context, legacyPlainName)
            return 0
        }

        val editor = targetEncryptedPrefs.edit()
        var migratedCount = 0

        for ((key, value) in allEntries) {
            when (value) {
                is Boolean -> { editor.putBoolean(key, value); migratedCount++ }
                is Int -> { editor.putInt(key, value); migratedCount++ }
                is Long -> { editor.putLong(key, value); migratedCount++ }
                is Float -> { editor.putFloat(key, value); migratedCount++ }
                is String -> { editor.putString(key, value); migratedCount++ }
                is Set<*> -> {
                    val stringSet = value.filterIsInstance<String>().toSet()
                    editor.putStringSet(key, stringSet)
                    migratedCount++
                }
            }
        }

        editor.putBoolean(MIGRATION_MARKER_KEY, true)
        val committed = editor.commit()
        if (!committed) {
            throw IllegalStateException("Fallo al confirmar la migración atómica hacia EncryptedSharedPreferences ($legacyPlainName).")
        }

        // Purgar en memoria y eliminar el archivo XML plano del disco
        legacyPrefs.edit().clear().commit()
        deleteLegacyXmlFile(context, legacyPlainName)
        Log.i(TAG, "Migradas $migratedCount entradas de '$legacyPlainName' a almacenamiento cifrado AES-256.")
        return migratedCount
    }

    private fun deleteLegacyXmlFile(context: Context, legacyPlainName: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.deleteSharedPreferences(legacyPlainName)
            } else {
                val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
                val xmlFile = File(prefsDir, "$legacyPlainName.xml")
                if (xmlFile.exists()) xmlFile.delete()
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Aviso al eliminar XML legado '$legacyPlainName': ${e.message}")
        }
    }
}
