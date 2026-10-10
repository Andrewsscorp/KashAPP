package com.example.data.local

import android.content.Context
import android.util.Log
import androidx.room.Room
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

/**
 * Factory and provider for encrypted SecurityDatabase using SQLCipher with graceful fallback.
 * Provides safe initialization, automatic corruption recovery, destructive migration fallback,
 * and SQLCipher 256-bit AES encryption at rest secured with a user-defined or Keystore master passphrase.
 */
object RoomLocalDb {
    private const val TAG = "RoomLocalDb"
    private const val ENCRYPTED_DB_NAME = "kashapp_encrypted_vault_v2.db"
    private const val FALLBACK_DB_NAME = "kashapp_security_v3.db"
    private const val LEGACY_V1_DB_NAME = "kashapp_vault_v1.db"
    private const val LEGACY_DB_NAME = "kashapp_security.db"

    @Volatile
    private var INSTANCE: SecurityDatabase? = null

    @Volatile
    private var isUsingFallbackPlainDb: Boolean = false

    @Volatile
    var vaultUnlockErrorMessage: String? = null
        private set

    fun clearVaultUnlockError() {
        vaultUnlockErrorMessage = null
    }

    private val foreignKeyCallback = object : androidx.room.RoomDatabase.Callback() {
        override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            super.onOpen(db)
            db.execSQL("PRAGMA foreign_keys = ON;")
        }
    }

    fun isEncryptedActive(): Boolean = !isUsingFallbackPlainDb

    fun getDatabase(
        context: Context,
        passphraseManager: PassphraseManager? = null,
        cryptoManager: KeystoreCryptoManager = KeystoreCryptoManager()
    ): SecurityDatabase {
        val manager = passphraseManager ?: PassphraseManager.getInstance(context)
        val passphraseBytes = manager.getDatabasePassphrase()

        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: buildDatabaseWithRecovery(context.applicationContext, passphraseBytes).also {
                INSTANCE = it
            }
        }
    }

    /**
     * Re-initializes and opens the database with a newly set user-defined passphrase.
     */
    @Synchronized
    fun openWithUserPassphrase(
        context: Context,
        passphraseManager: PassphraseManager,
        newUserPassphrase: String
    ): SecurityDatabase {
        passphraseManager.setUserPassphrase(newUserPassphrase)
        val passphraseBytes = passphraseManager.getDatabasePassphrase()

        closeDatabase()

        val db = buildDatabaseWithRecovery(context.applicationContext, passphraseBytes)
        INSTANCE = db
        return db
    }

    /**
     * Resets passphrase to hardware master seed and reopens database.
     */
    @Synchronized
    fun resetToHardwareMaster(
        context: Context,
        passphraseManager: PassphraseManager
    ): SecurityDatabase {
        passphraseManager.resetToHardwareMasterPassphrase()
        val passphraseBytes = passphraseManager.getDatabasePassphrase()

        closeDatabase()

        val db = buildDatabaseWithRecovery(context.applicationContext, passphraseBytes)
        INSTANCE = db
        return db
    }

    @Synchronized
    fun closeDatabase() {
        try {
            INSTANCE?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Notice closing database instance: ${e.message}")
        } finally {
            INSTANCE = null
        }
    }

    private fun buildDatabaseWithRecovery(context: Context, passphraseBytes: ByteArray): SecurityDatabase {
        // Clean up legacy unencrypted files from outdated app versions
        cleanLegacyDatabaseFiles(context)

        val encryptedDbFile = context.getDatabasePath(ENCRYPTED_DB_NAME)
        val encryptedFileExists = encryptedDbFile.exists()

        // Tier 1: Attempt to open or create SQLCipher encrypted database
        val encryptedDb = tryCreateEncryptedDb(context, passphraseBytes)
        if (encryptedDb != null) {
            isUsingFallbackPlainDb = false
            Log.i(TAG, "Successfully opened encrypted SQLCipher database vault.")
            return encryptedDb
        }

        // Tier 1b: If encrypted DB exists but failed to open (passphrase key mismatch after Keystore glitch),
        // PRESERVE the encrypted database files to protect user data from being wiped into an empty vault.
        if (encryptedFileExists) {
            Log.e(TAG, "Encrypted database vault exists but failed to open with active key. Preserving vault files without wiping.")
            vaultUnlockErrorMessage = "Error al desbloquear la base de datos cifrada. La clave de seguridad no coincide. Tus datos están a salvo. Ingresa tu clave maestra o contacta soporte."
        }

        // Tier 2: If SQLCipher native libraries cannot be loaded on this device/ABI or fails, fall back to standard Room DB
        Log.w(TAG, "SQLCipher native libraries or encryption unavailable. Initializing standard Room DB without crashing.")
        isUsingFallbackPlainDb = true

        return try {
            buildStandardDatabase(context, FALLBACK_DB_NAME).also { db ->
                db.openHelper.writableDatabase
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Standard fallback DB failed to open: ${t.message}. Cleaning and retrying.", t)
            backupAndRemoveDbFiles(context, FALLBACK_DB_NAME)
            try {
                buildStandardDatabase(context, FALLBACK_DB_NAME).also { db ->
                    db.openHelper.writableDatabase
                }
            } catch (fatal: Throwable) {
                Log.e(TAG, "Emergency fallback to in-memory database: ${fatal.message}", fatal)
                Room.inMemoryDatabaseBuilder(context, SecurityDatabase::class.java)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build().also { db ->
                        db.openHelper.writableDatabase
                    }
            }
        }
    }

    val MIGRATION_24_25 = object : androidx.room.migration.Migration(24, 25) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE fixed_assets ADD COLUMN includeInNetWorth INTEGER NOT NULL DEFAULT 1")
            } catch (e: Throwable) {
                Log.w(TAG, "Migration 24->25 notice (column may already exist): ${e.message}")
            }
        }
    }

    val MIGRATION_25_26 = object : androidx.room.migration.Migration(25, 26) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE fixed_assets ADD COLUMN depreciableBase TEXT NOT NULL DEFAULT 'Valor de adquisición'")
                db.execSQL("ALTER TABLE fixed_assets ADD COLUMN startConvention TEXT NOT NULL DEFAULT 'Desde fecha de adquisición'")
                db.execSQL("ALTER TABLE fixed_assets ADD COLUMN frequency TEXT NOT NULL DEFAULT 'Mensual'")
                db.execSQL("ALTER TABLE fixed_assets ADD COLUMN rounding TEXT NOT NULL DEFAULT 'Al peso'")
                db.execSQL("ALTER TABLE fixed_assets ADD COLUMN impairmentTracking INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE fixed_assets ADD COLUMN revaluationEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE fixed_assets ADD COLUMN allowLifeChange INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE fixed_assets ADD COLUMN allowResidualChange INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE fixed_assets ADD COLUMN allowMethodChange INTEGER NOT NULL DEFAULT 0")
            } catch (e: Throwable) {
                Log.w(TAG, "Migration 25->26 notice: ${e.message}")
            }
        }
    }

    val MIGRATION_26_27 = object : androidx.room.migration.Migration(26, 27) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE asset_movements ADD COLUMN usefulLifeIncrementMonths INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE asset_movements ADD COLUMN newResidualValue REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE asset_movements ADD COLUMN costCenter TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE asset_movements ADD COLUMN accountingAccount TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE asset_movements ADD COLUMN providerOrVendor TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE asset_movements ADD COLUMN invoiceOrDocNumber TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE asset_movements ADD COLUMN documentUri TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE asset_movements ADD COLUMN disposalReason TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE asset_movements ADD COLUMN impairmentReason TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE asset_movements ADD COLUMN financialImpact TEXT DEFAULT NULL")
            } catch (e: Throwable) {
                Log.w(TAG, "Migration 26->27 notice: ${e.message}")
            }
        }
    }

    val MIGRATION_27_28 = object : androidx.room.migration.Migration(27, 28) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE fixed_assets ADD COLUMN deletedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE fixed_assets ADD COLUMN stateNotes TEXT DEFAULT NULL")
            } catch (e: Throwable) {
                Log.w(TAG, "Migration 27->28 notice: ${e.message}")
            }
        }
    }

    val MIGRATION_28_29 = object : androidx.room.migration.Migration(28, 29) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            try {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `asset_audit_logs` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `assetId` TEXT,
                        `assetName` TEXT,
                        `actionType` TEXT NOT NULL,
                        `actionTitle` TEXT NOT NULL,
                        `details` TEXT NOT NULL,
                        `financialImpact` TEXT,
                        `userRole` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `formattedDate` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_asset_audit_logs_assetId` ON `asset_audit_logs` (`assetId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_asset_audit_logs_userId` ON `asset_audit_logs` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_asset_audit_logs_actionType` ON `asset_audit_logs` (`actionType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_asset_audit_logs_timestamp` ON `asset_audit_logs` (`timestamp`)")
            } catch (e: Throwable) {
                Log.w(TAG, "Migration 28->29 notice: ${e.message}")
            }
        }
    }

    val MIGRATION_29_30 = object : androidx.room.migration.Migration(29, 30) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            try {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_userId` ON `accounts` (`userId`)")
            } catch (e: Throwable) {
                Log.w(TAG, "Migration 29->30 notice: ${e.message}")
            }
        }
    }

    private fun tryCreateEncryptedDb(context: Context, passphraseBytes: ByteArray): SecurityDatabase? {
        return try {
            // Attempt to load native SQLCipher library if available
            try {
                System.loadLibrary("sqlcipher")
            } catch (_: Throwable) {}

            val openHelperFactory = SupportOpenHelperFactory(passphraseBytes)
            val db = Room.databaseBuilder(
                context,
                SecurityDatabase::class.java,
                ENCRYPTED_DB_NAME
            )
                .openHelperFactory(openHelperFactory)
                .addMigrations(MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .addCallback(foreignKeyCallback)
                .build()

            // Verify writableDatabase can actually execute queries
            db.openHelper.writableDatabase
            db
        } catch (t: Throwable) {
            Log.w(TAG, "Encrypted database initialization check failed: ${t.message}")
            null
        }
    }

    private fun buildStandardDatabase(context: Context, dbName: String): SecurityDatabase {
        return Room.databaseBuilder(
            context,
            SecurityDatabase::class.java,
            dbName
        )
            .addMigrations(MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .addCallback(foreignKeyCallback)
            .build()
    }

    private fun cleanLegacyDatabaseFiles(context: Context) {
        val legacyNames = listOf(LEGACY_DB_NAME, LEGACY_V1_DB_NAME)
        for (legacyName in legacyNames) {
            try {
                val legacyDb = context.getDatabasePath(legacyName)
                if (legacyDb.exists()) {
                    context.deleteDatabase(legacyName)
                    deleteDbFiles(context, legacyName)
                    Log.i(TAG, "Cleaned legacy database $legacyName")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Notice cleaning legacy db files: ${e.message}")
            }
        }
    }

    private fun backupAndRemoveDbFiles(context: Context, dbName: String) {
        try {
            val dbFile = context.getDatabasePath(dbName)
            val parent = dbFile.parentFile ?: return
            val timestamp = System.currentTimeMillis()
            val suffixes = listOf("", "-journal", "-shm", "-wal", "-wal2")
            for (suffix in suffixes) {
                val file = File(parent, "$dbName$suffix")
                if (file.exists()) {
                    val backupFile = File(parent, "$dbName$suffix.bak_$timestamp")
                    file.renameTo(backupFile)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice backing up and removing db files: ${e.message}")
        }
    }

    private fun deleteDbFiles(context: Context, dbName: String) {
        try {
            val dbFile = context.getDatabasePath(dbName)
            val parent = dbFile.parentFile ?: return
            val suffixes = listOf("", "-journal", "-shm", "-wal", "-wal2", ".bak")
            for (suffix in suffixes) {
                val file = File(parent, "$dbName$suffix")
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }
}
