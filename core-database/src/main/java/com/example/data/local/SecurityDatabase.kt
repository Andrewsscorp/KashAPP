package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
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
        UserSessionEntity::class
    ],
    version = 30,
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
}

