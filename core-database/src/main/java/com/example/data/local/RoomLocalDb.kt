package com.example.data.local

import android.content.Context
import android.util.Log
import androidx.room.Room
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

/**
 * Factory and provider for encrypted SecurityDatabase using SQLCipher with fail-closed recovery.
 * Provides safe initialization and preservation of corrupted data for explicit recovery,
 * and SQLCipher 256-bit AES encryption at rest secured with a user-defined or Keystore master passphrase.
 */
object RoomLocalDb {
    private const val TAG = "RoomLocalDb"
    private const val ENCRYPTED_DB_NAME = "kashapp_encrypted_vault_v2.db"
    private const val LEGACY_V1_DB_NAME = "kashapp_vault_v1.db"
    private const val LEGACY_DB_NAME = "kashapp_security.db"

    @Volatile
    private var INSTANCE: SecurityDatabase? = null

    @Volatile
    private var isUsingFallbackPlainDb: Boolean = false

    @Volatile
    var vaultUnlockErrorMessage: String? = null
        private set

    @Volatile
    private var lastEncryptedDbError: Throwable? = null

    fun clearVaultUnlockError() {
        vaultUnlockErrorMessage = null
    }

    private val foreignKeyCallback = object : androidx.room.RoomDatabase.Callback() {
        override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            super.onOpen(db)
            db.execSQL("PRAGMA foreign_keys = ON;")
            SecurityDatabase.installIntegrityGuards(db)
            ensureSharedExpenseSchema(db)
            try {
                db.execSQL("DELETE FROM accounts WHERE id IN ('acc_ahorros', 'acc_corriente', 'acc_billetera', 'acc_efectivo') AND balance = 0.0 AND NOT EXISTS (SELECT 1 FROM transactions WHERE transactions.accountId = accounts.id)")
            } catch (_: Throwable) {}
            try {
                db.execSQL("ALTER TABLE fixed_assets ADD COLUMN noAffectInventory INTEGER NOT NULL DEFAULT 0")
            } catch (_: Throwable) {}
            try {
                db.execSQL("ALTER TABLE fixed_assets ADD COLUMN noAffectDashboard INTEGER NOT NULL DEFAULT 0")
            } catch (_: Throwable) {}
        }

        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            super.onCreate(db)
            SecurityDatabase.installIntegrityGuards(db)
            ensureSharedExpenseSchema(db)
            try {
                db.execSQL("ALTER TABLE fixed_assets ADD COLUMN noAffectInventory INTEGER NOT NULL DEFAULT 0")
            } catch (_: Throwable) {}
            try {
                db.execSQL("ALTER TABLE fixed_assets ADD COLUMN noAffectDashboard INTEGER NOT NULL DEFAULT 0")
            } catch (_: Throwable) {}
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
     * Re-initializes and opens the database with a newly set user-defined passphrase,
     * executing an authenticated PRAGMA rekey on the active database before updating configuration.
     */
    @Synchronized
    fun openWithUserPassphrase(
        context: Context,
        passphraseManager: PassphraseManager,
        newUserPassphrase: String
    ): SecurityDatabase {
        val newKeyBytes = passphraseManager.deriveKeyFromPassphrase(newUserPassphrase)
        val newKeyHex = newKeyBytes.joinToString("") { "%02x".format(it) }

        // If an encrypted vault file already exists, execute PRAGMA rekey on the open database
        val encryptedDbFile = context.getDatabasePath(ENCRYPTED_DB_NAME)
        if (encryptedDbFile.exists()) {
            val currentDb = INSTANCE ?: getDatabase(context, passphraseManager)
            try {
                val writable = currentDb.openHelper.writableDatabase
                writable.execSQL("PRAGMA rekey = \"x'$newKeyHex'\";")
                Log.i(TAG, "SQLCipher PRAGMA rekey executed successfully to user passphrase.")
            } catch (e: Throwable) {
                Log.e(TAG, "Error executing PRAGMA rekey to user passphrase", e)
                throw IllegalStateException("No fue posible recifrar la base de datos con la nueva frase de paso.", e)
            }
        }

        passphraseManager.setUserPassphrase(newUserPassphrase)
        closeDatabase()

        val db = buildDatabaseWithRecovery(context.applicationContext, newKeyBytes)
        INSTANCE = db
        return db
    }

    /**
     * Resets passphrase to hardware master seed and reopens database,
     * executing an authenticated PRAGMA rekey before clearing user passphrase.
     */
    @Synchronized
    fun resetToHardwareMaster(
        context: Context,
        passphraseManager: PassphraseManager,
        cryptoManager: KeystoreCryptoManager = KeystoreCryptoManager()
    ): SecurityDatabase {
        val masterBytes = cryptoManager.getHardwareMasterPassphrase(context)
        val masterHex = masterBytes.joinToString("") { "%02x".format(it) }

        val encryptedDbFile = context.getDatabasePath(ENCRYPTED_DB_NAME)
        if (encryptedDbFile.exists()) {
            val currentDb = INSTANCE ?: getDatabase(context, passphraseManager, cryptoManager)
            try {
                val writable = currentDb.openHelper.writableDatabase
                writable.execSQL("PRAGMA rekey = \"x'$masterHex'\";")
                Log.i(TAG, "SQLCipher PRAGMA rekey executed successfully back to hardware master key.")
            } catch (e: Throwable) {
                Log.e(TAG, "Error executing PRAGMA rekey to hardware master key", e)
                throw IllegalStateException("No fue posible recifrar la base de datos a la clave maestra de hardware.", e)
            }
        }

        passphraseManager.resetToHardwareMasterPassphrase()
        closeDatabase()

        val db = buildDatabaseWithRecovery(context.applicationContext, masterBytes)
        INSTANCE = db
        return db
    }

    /**
     * Complete recovery: resets vault storage cleanly and reinitializes with master seed.
     */
    @Synchronized
    fun resetVaultAndRecreate(context: Context): SecurityDatabase {
        Log.i(TAG, "Executing full vault reset and re-initialization.")
        vaultUnlockErrorMessage = null
        backupAndRemoveDbFiles(context, ENCRYPTED_DB_NAME)
        val manager = PassphraseManager.getInstance(context)
        manager.resetToHardwareMasterPassphrase()
        val passphraseBytes = manager.getDatabasePassphrase()

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
            vaultUnlockErrorMessage = null
            Log.i(TAG, "Successfully opened encrypted SQLCipher database vault.")
            return encryptedDb
        }

        // Tier 1b: If encrypted DB exists but failed to open
        if (encryptedFileExists) {
            val err = lastEncryptedDbError
            Log.e(TAG, "Encrypted database vault failed to open. Preserving vault files without wiping.", err)
            vaultUnlockErrorMessage = "Error al desbloquear la base de datos cifrada. La clave de seguridad no coincide. Tus datos están a salvo. Inicia sesión con tus credenciales maestras autorizadas para acceder."
        }

        isUsingFallbackPlainDb = false
        throw IllegalStateException(
            "No se pudo abrir la base de datos cifrada SQLCipher; se rechaza continuar sin cifrado.",
            lastEncryptedDbError
        )
    }

    val MIGRATION_24_25 = object : androidx.room.migration.Migration(24, 25) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE fixed_assets ADD COLUMN includeInNetWorth INTEGER NOT NULL DEFAULT 1")
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
                throw IllegalStateException("Migration 25->26 failed", e)
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
                throw IllegalStateException("Migration 26->27 failed", e)
            }
        }
    }

    val MIGRATION_27_28 = object : androidx.room.migration.Migration(27, 28) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE fixed_assets ADD COLUMN deletedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE fixed_assets ADD COLUMN stateNotes TEXT DEFAULT NULL")
            } catch (e: Throwable) {
                throw IllegalStateException("Migration 27->28 failed", e)
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
                throw IllegalStateException("Migration 28->29 failed", e)
            }
        }
    }

    val MIGRATION_29_30 = object : androidx.room.migration.Migration(29, 30) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            try {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_userId` ON `accounts` (`userId`)")
            } catch (e: Throwable) {
                throw IllegalStateException("Migration 29->30 failed", e)
            }
        }
    }

    val MIGRATION_30_31 = object : androidx.room.migration.Migration(30, 31) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            try {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `scheduled_payments` (
                        `id` TEXT NOT NULL,
                        `user_id` TEXT NOT NULL,
                        `entity_name` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `currency` TEXT NOT NULL,
                        `due_date` TEXT NOT NULL,
                        `recurrence` TEXT NOT NULL,
                        `is_income` INTEGER NOT NULL,
                        `is_paid` INTEGER NOT NULL,
                        `paid_date` TEXT,
                        `source_account_id` TEXT,
                        `source_account_name` TEXT,
                        `notes` TEXT,
                        `reminder_days_before` INTEGER NOT NULL,
                        `notification_enabled` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            } catch (e: Throwable) {
                throw IllegalStateException("Migration 30->31 failed", e)
            }
        }
    }

    fun ensureSharedExpenseSchema(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        val alterStatements = listOf(
            "ALTER TABLE `transactions` ADD COLUMN `sharedExpenseId` TEXT DEFAULT NULL",
            "ALTER TABLE `transactions` ADD COLUMN `sharedSettlementId` TEXT DEFAULT NULL",
            "ALTER TABLE `transactions` ADD COLUMN `sharedConsecutiveCode` TEXT DEFAULT NULL",
            "ALTER TABLE `transactions` ADD COLUMN `accountingRecognizedExpenseAmount` REAL DEFAULT NULL",
            "ALTER TABLE `transactions` ADD COLUMN `receivableOrPayableDeltaAmount` REAL DEFAULT NULL"
        )
        for (stmt in alterStatements) {
            try {
                db.execSQL(stmt)
            } catch (_: Throwable) {}
        }

        val createTableStatements = listOf(
            """
            CREATE TABLE IF NOT EXISTS `shared_sequence_configs` (
                `id` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `sequenceType` TEXT NOT NULL,
                `scopeGroupCode` TEXT NOT NULL DEFAULT 'GLOBAL',
                `mode` TEXT NOT NULL DEFAULT 'HYBRID_ALLOW_OVERRIDE',
                `prefix` TEXT NOT NULL DEFAULT 'GC',
                `patternTemplate` TEXT NOT NULL DEFAULT '{PREFIX}-{YYYY}-{SEQ:4}',
                `currentValue` INTEGER NOT NULL DEFAULT 0,
                `stepBy` INTEGER NOT NULL DEFAULT 1,
                `paddingDigits` INTEGER NOT NULL DEFAULT 4,
                `resetPolicy` TEXT NOT NULL DEFAULT 'YEARLY',
                `lastResetPeriodKey` TEXT NOT NULL DEFAULT '2026',
                `allowManualGaps` INTEGER NOT NULL DEFAULT 1,
                `updatedAt` INTEGER NOT NULL,
                `version` INTEGER NOT NULL DEFAULT 1,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_shared_sequence_configs_userId_sequenceType_scopeGroupCode` ON `shared_sequence_configs` (`userId`, `sequenceType`, `scopeGroupCode`)",
            """
            CREATE TABLE IF NOT EXISTS `shared_participants` (
                `id` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `isSelf` INTEGER NOT NULL DEFAULT 0,
                `displayName` TEXT NOT NULL,
                `nameNormalized` TEXT NOT NULL,
                `aliasOrRelation` TEXT,
                `phoneOrWhatsApp` TEXT,
                `email` TEXT,
                `preferredPaymentMethod` TEXT,
                `bankName` TEXT,
                `accountType` TEXT,
                `accountNumberOrKey` TEXT,
                `qrImageLocalUri` TEXT,
                `monthlyIncomeEstimate` REAL NOT NULL DEFAULT 0.0,
                `defaultIncomeShareWeight` REAL NOT NULL DEFAULT 1.0,
                `avatarColorHex` TEXT NOT NULL DEFAULT '#0284C7',
                `avatarInitials` TEXT NOT NULL DEFAULT 'KP',
                `creditTrustLimit` REAL,
                `isActive` INTEGER NOT NULL DEFAULT 1,
                `archivedAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_shared_participants_userId_nameNormalized` ON `shared_participants` (`userId`, `nameNormalized`)",
            "CREATE INDEX IF NOT EXISTS `index_shared_participants_userId_isActive` ON `shared_participants` (`userId`, `isActive`)",
            """
            CREATE TABLE IF NOT EXISTS `shared_groups` (
                `id` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `nameNormalized` TEXT NOT NULL,
                `groupCode` TEXT NOT NULL,
                `groupType` TEXT NOT NULL DEFAULT 'HOME',
                `baseCurrency` TEXT NOT NULL DEFAULT 'COP',
                `iconName` TEXT NOT NULL DEFAULT 'ic_group_default',
                `colorHex` TEXT NOT NULL DEFAULT '#0EA5E9',
                `defaultRealMoneyImpactMode` TEXT NOT NULL DEFAULT 'ACCRUAL_RECEIVABLE',
                `defaultSplitMethod` TEXT NOT NULL DEFAULT 'EQUAL',
                `defaultSourceAccountId` TEXT,
                `defaultCategoryId` TEXT,
                `simplifyDebtsEnabled` INTEGER NOT NULL DEFAULT 1,
                `autoReserveOwedInHold` INTEGER NOT NULL DEFAULT 1,
                `budgetImpactPolicy` TEXT NOT NULL DEFAULT 'MY_NET_SHARE_ONLY',
                `settlementCycleDay` INTEGER,
                `status` TEXT NOT NULL DEFAULT 'ACTIVE',
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_shared_groups_userId_nameNormalized` ON `shared_groups` (`userId`, `nameNormalized`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_shared_groups_userId_groupCode` ON `shared_groups` (`userId`, `groupCode`)",
            "CREATE INDEX IF NOT EXISTS `index_shared_groups_userId_status` ON `shared_groups` (`userId`, `status`)",
            """
            CREATE TABLE IF NOT EXISTS `shared_group_members` (
                `groupId` TEXT NOT NULL,
                `participantId` TEXT NOT NULL,
                `defaultShareUnits` REAL NOT NULL DEFAULT 1.0,
                `defaultPercentage` REAL,
                `defaultAdjustmentAmount` REAL NOT NULL DEFAULT 0.0,
                `joinedAt` INTEGER NOT NULL,
                `isActiveInGroup` INTEGER NOT NULL DEFAULT 1,
                PRIMARY KEY(`groupId`, `participantId`),
                FOREIGN KEY(`groupId`) REFERENCES `shared_groups`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`participantId`) REFERENCES `shared_participants`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS `index_shared_group_members_groupId` ON `shared_group_members` (`groupId`)",
            "CREATE INDEX IF NOT EXISTS `index_shared_group_members_participantId` ON `shared_group_members` (`participantId`)",
            """
            CREATE TABLE IF NOT EXISTS `shared_expenses` (
                `id` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `consecutiveCode` TEXT NOT NULL,
                `consecutiveNormalized` TEXT NOT NULL,
                `isManualConsecutive` INTEGER NOT NULL DEFAULT 0,
                `receiptTicketCode` TEXT NOT NULL,
                `externalInvoiceNumber` TEXT,
                `groupId` TEXT,
                `title` TEXT NOT NULL,
                `description` TEXT,
                `merchantId` TEXT,
                `merchantNameSnapshot` TEXT,
                `merchantNit` TEXT,
                `categoryId` TEXT NOT NULL,
                `categoryNameSnapshot` TEXT NOT NULL,
                `expenseDate` TEXT NOT NULL,
                `expenseTime` TEXT NOT NULL,
                `currency` TEXT NOT NULL DEFAULT 'COP',
                `baseCurrency` TEXT NOT NULL DEFAULT 'COP',
                `appliedExchangeRate` REAL NOT NULL DEFAULT 1.0,
                `exchangeRateSource` TEXT NOT NULL DEFAULT 'OFFICIAL_TRM',
                `subtotalAmount` REAL NOT NULL,
                `taxIvaAmount` REAL NOT NULL DEFAULT 0.0,
                `taxConsumptionAmount` REAL NOT NULL DEFAULT 0.0,
                `tipAmount` REAL NOT NULL DEFAULT 0.0,
                `discountAmount` REAL NOT NULL DEFAULT 0.0,
                `gmfAmount` REAL NOT NULL DEFAULT 0.0,
                `creditCardInterestSurcharge` REAL NOT NULL DEFAULT 0.0,
                `totalAmount` REAL NOT NULL,
                `splitMethod` TEXT NOT NULL DEFAULT 'EQUAL',
                `taxAndTipProrationRule` TEXT NOT NULL DEFAULT 'PROPORTIONAL_TO_SUBTOTAL',
                `roundingRule` TEXT NOT NULL DEFAULT 'ROUND_1',
                `roundingRemainderTarget` TEXT NOT NULL DEFAULT 'PAYER',
                `realMoneyImpactMode` TEXT NOT NULL DEFAULT 'ACCRUAL_RECEIVABLE',
                `budgetImpactPolicy` TEXT NOT NULL DEFAULT 'MY_NET_SHARE_ONLY',
                `holdReserveAccountId` TEXT,
                `holdReservedAmount` REAL NOT NULL DEFAULT 0.0,
                `myPaidAmount` REAL NOT NULL DEFAULT 0.0,
                `myConsumptionShareAmount` REAL NOT NULL DEFAULT 0.0,
                `myNetPositionDelta` REAL NOT NULL DEFAULT 0.0,
                `settledAmountTotal` REAL NOT NULL DEFAULT 0.0,
                `status` TEXT NOT NULL DEFAULT 'PENDING_SETTLEMENT',
                `voidReason` TEXT,
                `voidedAt` INTEGER,
                `receiptImageLocalUri` TEXT,
                `generatedTicketPdfUri` TEXT,
                `generatedTicketPngUri` TEXT,
                `ticketIntegrityHash` TEXT NOT NULL,
                `isRecurring` INTEGER NOT NULL DEFAULT 0,
                `recurrenceFrequency` TEXT,
                `nextRecurrenceDate` TEXT,
                `dueDateForSettlement` TEXT,
                `idempotencyKey` TEXT NOT NULL,
                `syncStatus` TEXT NOT NULL DEFAULT 'PENDING',
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_shared_expenses_userId_consecutiveCode` ON `shared_expenses` (`userId`, `consecutiveCode`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_shared_expenses_userId_receiptTicketCode` ON `shared_expenses` (`userId`, `receiptTicketCode`)",
            "CREATE INDEX IF NOT EXISTS `index_shared_expenses_groupId_expenseDate` ON `shared_expenses` (`groupId`, `expenseDate`)",
            "CREATE INDEX IF NOT EXISTS `index_shared_expenses_userId_status` ON `shared_expenses` (`userId`, `status`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_shared_expenses_idempotencyKey` ON `shared_expenses` (`idempotencyKey`)",
            """
            CREATE TABLE IF NOT EXISTS `shared_expense_payers` (
                `id` TEXT NOT NULL,
                `sharedExpenseId` TEXT NOT NULL,
                `participantId` TEXT NOT NULL,
                `paidAmount` REAL NOT NULL,
                `kashAccountId` TEXT,
                `kashAccountNameSnapshot` TEXT,
                `paymentInstrumentType` TEXT NOT NULL DEFAULT 'SAVINGS',
                `creditCardInstallments` INTEGER NOT NULL DEFAULT 1,
                `passCreditCardInterestToGroup` INTEGER NOT NULL DEFAULT 0,
                `gmfGeneratedAmount` REAL NOT NULL DEFAULT 0.0,
                `linkedTransactionId` TEXT,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`sharedExpenseId`) REFERENCES `shared_expenses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`participantId`) REFERENCES `shared_participants`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS `index_shared_expense_payers_sharedExpenseId` ON `shared_expense_payers` (`sharedExpenseId`)",
            "CREATE INDEX IF NOT EXISTS `index_shared_expense_payers_participantId` ON `shared_expense_payers` (`participantId`)",
            "CREATE INDEX IF NOT EXISTS `index_shared_expense_payers_kashAccountId` ON `shared_expense_payers` (`kashAccountId`)",
            """
            CREATE TABLE IF NOT EXISTS `shared_expense_splits` (
                `id` TEXT NOT NULL,
                `sharedExpenseId` TEXT NOT NULL,
                `participantId` TEXT NOT NULL,
                `isIncluded` INTEGER NOT NULL DEFAULT 1,
                `rawInputWeightOrPct` REAL NOT NULL DEFAULT 1.0,
                `baseSubtotalShare` REAL NOT NULL,
                `allocatedTaxAmount` REAL NOT NULL DEFAULT 0.0,
                `allocatedTipAmount` REAL NOT NULL DEFAULT 0.0,
                `allocatedDiscountAmount` REAL NOT NULL DEFAULT 0.0,
                `allocatedGmfAmount` REAL NOT NULL DEFAULT 0.0,
                `roundingAdjustment` REAL NOT NULL DEFAULT 0.0,
                `totalOwedShare` REAL NOT NULL,
                `paidByThisParticipant` REAL NOT NULL DEFAULT 0.0,
                `netBalanceForExpense` REAL NOT NULL,
                `amountAlreadySettled` REAL NOT NULL DEFAULT 0.0,
                `remainingPendingAmount` REAL NOT NULL,
                `settlementStatus` TEXT NOT NULL DEFAULT 'PENDING',
                PRIMARY KEY(`id`),
                FOREIGN KEY(`sharedExpenseId`) REFERENCES `shared_expenses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`participantId`) REFERENCES `shared_participants`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS `index_shared_expense_splits_sharedExpenseId` ON `shared_expense_splits` (`sharedExpenseId`)",
            "CREATE INDEX IF NOT EXISTS `index_shared_expense_splits_participantId_settlementStatus` ON `shared_expense_splits` (`participantId`, `settlementStatus`)",
            """
            CREATE TABLE IF NOT EXISTS `shared_expense_line_items` (
                `id` TEXT NOT NULL,
                `sharedExpenseId` TEXT NOT NULL,
                `itemOrder` INTEGER NOT NULL,
                `description` TEXT NOT NULL,
                `quantity` REAL NOT NULL DEFAULT 1.0,
                `unitPrice` REAL NOT NULL,
                `lineTotalPrice` REAL NOT NULL,
                `isTaxExempt` INTEGER NOT NULL DEFAULT 0,
                `assignedParticipantIdsCsv` TEXT NOT NULL,
                `splitModeWithinItem` TEXT NOT NULL DEFAULT 'EQUAL',
                PRIMARY KEY(`id`),
                FOREIGN KEY(`sharedExpenseId`) REFERENCES `shared_expenses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS `index_shared_expense_line_items_sharedExpenseId` ON `shared_expense_line_items` (`sharedExpenseId`)",
            """
            CREATE TABLE IF NOT EXISTS `shared_settlement_payments` (
                `id` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `voucherConsecutiveCode` TEXT NOT NULL,
                `isFullGroupSettlementAct` INTEGER NOT NULL DEFAULT 0,
                `groupId` TEXT,
                `sharedExpenseId` TEXT,
                `fromParticipantId` TEXT NOT NULL,
                `toParticipantId` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `currency` TEXT NOT NULL DEFAULT 'COP',
                `appliedExchangeRate` REAL NOT NULL DEFAULT 1.0,
                `fxGainOrLossAmount` REAL NOT NULL DEFAULT 0.0,
                `settlementMethod` TEXT NOT NULL,
                `reimbursementTreatmentApplied` TEXT NOT NULL DEFAULT 'OFFSET_RECEIVABLE_ASSET',
                `kashAccountId` TEXT,
                `kashAccountNameSnapshot` TEXT,
                `linkedTransactionId` TEXT,
                `releasedHoldAmount` REAL NOT NULL DEFAULT 0.0,
                `externalReferenceCode` TEXT,
                `proofDocumentLocalUri` TEXT,
                `generatedVoucherPdfUri` TEXT,
                `ticketIntegrityHash` TEXT NOT NULL,
                `notes` TEXT,
                `paymentDate` TEXT NOT NULL,
                `paymentTime` TEXT NOT NULL,
                `idempotencyKey` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_shared_settlement_payments_userId_voucherConsecutiveCode` ON `shared_settlement_payments` (`userId`, `voucherConsecutiveCode`)",
            "CREATE INDEX IF NOT EXISTS `index_shared_settlement_payments_groupId_paymentDate` ON `shared_settlement_payments` (`groupId`, `paymentDate`)",
            "CREATE INDEX IF NOT EXISTS `index_shared_settlement_payments_sharedExpenseId` ON `shared_settlement_payments` (`sharedExpenseId`)",
            "CREATE INDEX IF NOT EXISTS `index_shared_settlement_payments_fromParticipantId_toParticipantId` ON `shared_settlement_payments` (`fromParticipantId`, `toParticipantId`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_shared_settlement_payments_idempotencyKey` ON `shared_settlement_payments` (`idempotencyKey`)"
        )

        for (sql in createTableStatements) {
            try {
                db.execSQL(sql)
            } catch (e: Throwable) {
                Log.w(TAG, "Notice creating shared expense table/index: ${e.message}")
            }
        }

        val now = System.currentTimeMillis()
        try {
            db.execSQL("""
                INSERT OR IGNORE INTO `shared_participants` (
                    `id`, `userId`, `isSelf`, `displayName`, `nameNormalized`, `aliasOrRelation`,
                    `avatarColorHex`, `avatarInitials`, `isActive`, `createdAt`, `updatedAt`
                ) VALUES (
                    'SELF_USER', 'user_default', 1, 'Yo (Titular)', 'yo titular', 'Titular de la Cuenta',
                    '#0284C7', 'YO', 1, $now, $now
                )
            """.trimIndent())
            db.execSQL("INSERT OR IGNORE INTO `shared_sequence_configs` VALUES ('seq_default_gc', 'user_default', 'SHARED_EXPENSE', 'GLOBAL', 'HYBRID_ALLOW_OVERRIDE', 'GC', '{PREFIX}-{YYYY}-{SEQ:4}', 0, 1, 4, 'YEARLY', '2026', 1, $now, 1)")
            db.execSQL("INSERT OR IGNORE INTO `shared_sequence_configs` VALUES ('seq_default_tk', 'user_default', 'RECEIPT_TICKET', 'GLOBAL', 'HYBRID_ALLOW_OVERRIDE', 'TK', '{PREFIX}-{YYYY}-{SEQ:4}', 0, 1, 4, 'YEARLY', '2026', 1, $now, 1)")
            db.execSQL("INSERT OR IGNORE INTO `shared_sequence_configs` VALUES ('seq_default_pg', 'user_default', 'PAYMENT_VOUCHER', 'GLOBAL', 'HYBRID_ALLOW_OVERRIDE', 'PG', '{PREFIX}-{YYYY}{MM}-{SEQ:4}', 0, 1, 4, 'MONTHLY', '2026-10', 1, $now, 1)")
            db.execSQL("INSERT OR IGNORE INTO `shared_sequence_configs` VALUES ('seq_default_lq', 'user_default', 'SETTLEMENT_ACT', 'GLOBAL', 'HYBRID_ALLOW_OVERRIDE', 'LQ', '{PREFIX}-{YYYY}-{SEQ:3}', 0, 1, 3, 'YEARLY', '2026', 1, $now, 1)")
        } catch (_: Throwable) {}
    }

    val MIGRATION_31_32 = object : androidx.room.migration.Migration(31, 32) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            ensureSharedExpenseSchema(db)
        }
    }

    val MIGRATION_32_33 = object : androidx.room.migration.Migration(32, 33) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            ensureSharedExpenseSchema(db)
        }
    }

    val MIGRATION_33_34 = object : androidx.room.migration.Migration(33, 34) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE fixed_assets ADD COLUMN noAffectInventory INTEGER NOT NULL DEFAULT 0")
            } catch (_: Throwable) {}
        }
    }

    val MIGRATION_34_35 = object : androidx.room.migration.Migration(34, 35) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE fixed_assets ADD COLUMN noAffectDashboard INTEGER NOT NULL DEFAULT 0")
            } catch (_: Throwable) {}
        }
    }

    val MIGRATION_35_36 = object : androidx.room.migration.Migration(35, 36) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            try {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `third_parties` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `normalizedName` TEXT NOT NULL,
                        `personType` TEXT NOT NULL DEFAULT 'NATURAL',
                        `idType` TEXT NOT NULL DEFAULT 'CC',
                        `idNumber` TEXT NOT NULL,
                        `verificationDigit` TEXT,
                        `commercialName` TEXT,
                        `category` TEXT NOT NULL DEFAULT 'PROVEEDOR',
                        `primaryPhone` TEXT,
                        `secondaryPhone` TEXT,
                        `email` TEXT,
                        `address` TEXT,
                        `city` TEXT,
                        `paymentTermsDays` INTEGER NOT NULL DEFAULT 0,
                        `defaultBank` TEXT,
                        `bankAccountType` TEXT,
                        `bankAccountNumber` TEXT,
                        `iconName` TEXT NOT NULL DEFAULT 'Person',
                        `colorHex` TEXT NOT NULL DEFAULT '#0284C7',
                        `notes` TEXT,
                        `isFavorite` INTEGER NOT NULL DEFAULT 0,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_third_parties_userId_normalizedName_idNumber` ON `third_parties` (`userId`, `normalizedName`, `idNumber`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_third_parties_userId_category` ON `third_parties` (`userId`, `category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_third_parties_userId_isFavorite` ON `third_parties` (`userId`, `isFavorite`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_third_parties_userId_isActive` ON `third_parties` (`userId`, `isActive`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_third_parties_idNumber` ON `third_parties` (`idNumber`)")
            } catch (e: Throwable) {
                Log.w(TAG, "Notice running MIGRATION_35_36: ${e.message}")
            }
        }
    }

    val MIGRATION_36_37 = object : androidx.room.migration.Migration(36, 37) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            try {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `third_party_categories` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `code` TEXT NOT NULL,
                        `iconName` TEXT NOT NULL DEFAULT 'Business',
                        `colorHex` TEXT NOT NULL DEFAULT '#0284C7',
                        `isSystem` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_third_party_categories_userId` ON `third_party_categories` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_third_party_categories_code` ON `third_party_categories` (`code`)")

                val now = System.currentTimeMillis()
                val defaults = listOf(
                    Triple("Proveedor", "PROVEEDOR", "Storefront"),
                    Triple("Concesionario", "CONCESIONARIO", "DirectionsCar"),
                    Triple("Tienda / Comercio", "TIENDA_COMERCIO", "Business"),
                    Triple("Cliente", "CLIENTE", "Person"),
                    Triple("Acreedor", "ACREEDOR", "AccountBalance"),
                    Triple("Empleado", "EMPLEADO", "Groups"),
                    Triple("Taller / Servicio", "TALLER_SERVICIO", "Build"),
                    Triple("Contratista", "CONTRATISTA", "AssignmentInd")
                )
                for ((name, code, icon) in defaults) {
                    db.execSQL("""
                        INSERT OR IGNORE INTO `third_party_categories` (`id`, `userId`, `name`, `code`, `iconName`, `colorHex`, `isSystem`, `createdAt`)
                        VALUES ('cat_sys_$code', 'system', '$name', '$code', '$icon', '#0284C7', 1, $now)
                    """.trimIndent())
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Notice running MIGRATION_36_37: ${e.message}")
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
                .addMigrations(MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33, MIGRATION_33_34, MIGRATION_34_35, MIGRATION_35_36, MIGRATION_36_37)
                .addCallback(foreignKeyCallback)
                .build()

            // Verify writableDatabase can actually execute queries
            db.openHelper.writableDatabase
            lastEncryptedDbError = null
            db
        } catch (t: Throwable) {
            Log.w(TAG, "Encrypted database initialization check failed: ${t.message}", t)
            lastEncryptedDbError = t
            null
        }
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
