package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AuditSecurityLogDao
import com.example.data.local.dao.FinancialDao
import com.example.data.local.dao.LocalAuthConfigDao
import com.example.data.local.dao.UserSessionDao
import com.example.data.local.entity.AccountCdtEntity
import com.example.data.local.entity.AccountCreditCardEntity
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountPayrollConfigEntity
import com.example.data.local.entity.AuditSecurityLog
import com.example.data.local.entity.BankEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CityEntity
import com.example.data.local.entity.CountryEntity
import com.example.data.local.entity.CurrencyEntity
import com.example.data.local.entity.IncomeSourceEntity
import com.example.data.local.entity.LocalAuthConfig
import com.example.data.local.entity.PayrollConceptEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransferGroupEntity
import com.example.data.local.entity.UserSessionEntity

/**
 * Encrypted Room Database instance for KashApp access control, audit trail, user sessions,
 * and ACID financial persistence (Accounts, Credit Cards, CDTs, Banks, Categories, IncomeSources, Transactions, TransferGroups, Currencies, Countries, Cities, Payroll, Payroll Concepts).
 */
@Database(
    entities = [
        LocalAuthConfig::class,
        AuditSecurityLog::class,
        AccountEntity::class,
        AccountCreditCardEntity::class,
        AccountPayrollConfigEntity::class,
        AccountCdtEntity::class,
        BankEntity::class,
        CategoryEntity::class,
        IncomeSourceEntity::class,
        TransactionEntity::class,
        TransferGroupEntity::class,
        CurrencyEntity::class,
        CountryEntity::class,
        CityEntity::class,
        PayrollConceptEntity::class,
        com.example.data.local.entity.FinancialGoalEntity::class,
        com.example.data.local.entity.BudgetEntity::class,
        com.example.data.local.entity.BudgetItemEntity::class,
        com.example.data.local.entity.InvestmentEntity::class,
        com.example.data.local.entity.InvestmentDocumentEntity::class,
        com.example.data.local.entity.MerchantEntity::class,
        com.example.data.local.entity.OutboxOperationEntity::class,
        com.example.data.local.entity.TagEntity::class,
        com.example.data.local.entity.ExchangeRateCacheEntity::class,
        com.example.data.local.entity.ManualExchangeRateEntity::class,
        com.example.data.local.entity.ManualRateHistoryLogEntity::class,
        com.example.data.local.entity.FixedAssetEntity::class,
        com.example.data.local.entity.AssetMovementEntity::class,
        com.example.data.local.entity.AssetCategoryEntity::class,
        com.example.data.local.entity.AssetAuditLogEntity::class,
        com.example.data.local.entity.ScheduledPaymentEntity::class,
        UserSessionEntity::class,
        // Nuevas Entidades Etapa 1 - Gastos Compartidos (Room v32):
        com.example.data.local.entity.SharedSequenceConfigEntity::class,
        com.example.data.local.entity.SharedParticipantEntity::class,
        com.example.data.local.entity.SharedGroupEntity::class,
        com.example.data.local.entity.SharedGroupMemberEntity::class,
        com.example.data.local.entity.SharedExpenseEntity::class,
        com.example.data.local.entity.SharedExpensePayerEntity::class,
        com.example.data.local.entity.SharedExpenseSplitEntity::class,
        com.example.data.local.entity.SharedExpenseLineItemEntity::class,
        com.example.data.local.entity.SharedSettlementPaymentEntity::class,
        com.example.data.local.entity.ThirdPartyEntity::class,
        com.example.data.local.entity.ThirdPartyCategoryEntity::class
    ],
    version = 37,
    exportSchema = false
)
abstract class SecurityDatabase : RoomDatabase() {
    abstract fun localAuthConfigDao(): LocalAuthConfigDao
    abstract fun auditSecurityLogDao(): AuditSecurityLogDao
    abstract fun financialDao(): FinancialDao
    abstract fun userSessionDao(): UserSessionDao
    abstract fun merchantDao(): com.example.data.local.dao.MerchantDao
    abstract fun outboxDao(): com.example.data.local.dao.OutboxDao
    abstract fun exchangeRateDao(): com.example.data.local.dao.ExchangeRateDao
    abstract fun fixedAssetDao(): com.example.data.local.dao.FixedAssetDao
    abstract fun assetCategoryDao(): com.example.data.local.dao.AssetCategoryDao
    abstract fun assetAuditLogDao(): com.example.data.local.dao.AssetAuditLogDao
    abstract fun sharedExpenseDao(): com.example.data.local.dao.SharedExpenseDao
    abstract fun thirdPartyDao(): com.example.data.local.dao.ThirdPartyDao
    abstract fun thirdPartyCategoryDao(): com.example.data.local.dao.ThirdPartyCategoryDao


        companion object {
                /** Enforces references that cannot be added with ALTER TABLE on existing SQLite tables. */
                fun installIntegrityGuards(db: SupportSQLiteDatabase) {
                        val statements = listOf(
                                """
                                CREATE TRIGGER IF NOT EXISTS transactions_account_insert_guard
                                BEFORE INSERT ON transactions
                                WHEN NOT EXISTS (SELECT 1 FROM accounts WHERE id = NEW.accountId)
                                BEGIN SELECT RAISE(ABORT, 'transaction account does not exist'); END
                                """.trimIndent(),
                                """
                                CREATE TRIGGER IF NOT EXISTS transactions_account_update_guard
                                BEFORE UPDATE OF accountId ON transactions
                                WHEN NOT EXISTS (SELECT 1 FROM accounts WHERE id = NEW.accountId)
                                BEGIN SELECT RAISE(ABORT, 'transaction account does not exist'); END
                                """.trimIndent(),
                                """
                                CREATE TRIGGER IF NOT EXISTS goals_target_account_insert_guard
                                BEFORE INSERT ON financial_goals
                                WHEN NOT EXISTS (SELECT 1 FROM accounts WHERE id = NEW.targetAccountId)
                                BEGIN SELECT RAISE(ABORT, 'goal target account does not exist'); END
                                """.trimIndent(),
                                """
                                CREATE TRIGGER IF NOT EXISTS goals_target_account_update_guard
                                BEFORE UPDATE OF targetAccountId ON financial_goals
                                WHEN NOT EXISTS (SELECT 1 FROM accounts WHERE id = NEW.targetAccountId)
                                BEGIN SELECT RAISE(ABORT, 'goal target account does not exist'); END
                                """.trimIndent(),
                                """
                                CREATE TRIGGER IF NOT EXISTS investments_origin_account_insert_guard
                                BEFORE INSERT ON investments
                                WHEN NOT EXISTS (SELECT 1 FROM accounts WHERE id = NEW.originAccountId)
                                BEGIN SELECT RAISE(ABORT, 'investment origin account does not exist'); END
                                """.trimIndent(),
                                """
                                CREATE TRIGGER IF NOT EXISTS investments_origin_account_update_guard
                                BEFORE UPDATE OF originAccountId ON investments
                                WHEN NOT EXISTS (SELECT 1 FROM accounts WHERE id = NEW.originAccountId)
                                BEGIN SELECT RAISE(ABORT, 'investment origin account does not exist'); END
                                """.trimIndent(),
                                """
                                CREATE TRIGGER IF NOT EXISTS asset_movements_asset_insert_guard
                                BEFORE INSERT ON asset_movements
                                WHEN NOT EXISTS (SELECT 1 FROM fixed_assets WHERE id = NEW.assetId)
                                BEGIN SELECT RAISE(ABORT, 'asset movement parent does not exist'); END
                                """.trimIndent(),
                                """
                                CREATE TRIGGER IF NOT EXISTS asset_movements_asset_update_guard
                                BEFORE UPDATE OF assetId ON asset_movements
                                WHEN NOT EXISTS (SELECT 1 FROM fixed_assets WHERE id = NEW.assetId)
                                BEGIN SELECT RAISE(ABORT, 'asset movement parent does not exist'); END
                                """.trimIndent(),
                                """
                                CREATE TRIGGER IF NOT EXISTS scheduled_payments_account_insert_guard
                                BEFORE INSERT ON scheduled_payments
                                WHEN NEW.source_account_id IS NOT NULL
                                    AND NOT EXISTS (SELECT 1 FROM accounts WHERE id = NEW.source_account_id)
                                BEGIN SELECT RAISE(ABORT, 'scheduled payment account does not exist'); END
                                """.trimIndent(),
                                """
                                CREATE TRIGGER IF NOT EXISTS scheduled_payments_account_update_guard
                                BEFORE UPDATE OF source_account_id ON scheduled_payments
                                WHEN NEW.source_account_id IS NOT NULL
                                    AND NOT EXISTS (SELECT 1 FROM accounts WHERE id = NEW.source_account_id)
                                BEGIN SELECT RAISE(ABORT, 'scheduled payment account does not exist'); END
                                """.trimIndent(),
                                """
                                CREATE TRIGGER IF NOT EXISTS accounts_delete_history_guard
                                BEFORE DELETE ON accounts
                                WHEN EXISTS (SELECT 1 FROM transactions WHERE accountId = OLD.id)
                                    OR EXISTS (SELECT 1 FROM financial_goals WHERE targetAccountId = OLD.id OR sourceAccountId = OLD.id)
                                    OR EXISTS (SELECT 1 FROM investments WHERE originAccountId = OLD.id OR yieldAccountId = OLD.id)
                                    OR EXISTS (SELECT 1 FROM scheduled_payments WHERE source_account_id = OLD.id)
                                BEGIN SELECT RAISE(ABORT, 'account has financial history'); END
                                """.trimIndent(),
                                """
                                CREATE TRIGGER IF NOT EXISTS fixed_assets_delete_history_guard
                                BEFORE DELETE ON fixed_assets
                                WHEN EXISTS (SELECT 1 FROM asset_movements WHERE assetId = OLD.id)
                                    OR EXISTS (SELECT 1 FROM asset_audit_logs WHERE assetId = OLD.id)
                                BEGIN SELECT RAISE(ABORT, 'asset has financial history'); END
                                """.trimIndent()
                        )
                        statements.forEach(db::execSQL)
                }
        }
}

