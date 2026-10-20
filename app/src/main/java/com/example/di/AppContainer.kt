package com.example.di

import android.content.Context
import com.example.data.datasource.BiometricDataSource
import com.example.data.datasource.FirebaseDataSource
import com.example.data.datasource.GoogleAuthDataSource
import com.example.data.datasource.GoogleDriveManager
import com.example.data.local.KeystoreCryptoManager
import com.example.data.local.PassphraseManager
import com.example.data.local.RoomLocalDb
import com.example.data.local.SecurityDatabase
import com.example.data.repository.AuthRepositoryImpl
import com.example.domain.repository.AuthRepository
import com.example.domain.usecase.AuthenticateUserUseCase

/**
 * Dependency container providing clean injection of data sources,
 * database, repositories and use cases.
 */
class AppContainer(private val context: Context) {

    val keystoreCryptoManager: KeystoreCryptoManager by lazy {
        KeystoreCryptoManager()
    }

    val passphraseManager: PassphraseManager by lazy {
        PassphraseManager(context, keystoreCryptoManager)
    }

    val securityDatabase: SecurityDatabase by lazy {
        RoomLocalDb.getDatabase(context, passphraseManager, keystoreCryptoManager).also { db ->
            passphraseManager.setAuditSecurityLogDao(db.auditSecurityLogDao())
        }
    }

    val firebaseDataSource: FirebaseDataSource by lazy {
        FirebaseDataSource(context)
    }

    val googleDriveManager: GoogleDriveManager by lazy {
        GoogleDriveManager(context)
    }

    val googleAuthDataSource: GoogleAuthDataSource by lazy {
        GoogleAuthDataSource(context)
    }

    val biometricDataSource: BiometricDataSource by lazy {
        BiometricDataSource(context, keystoreCryptoManager)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(
            localAuthConfigDao = securityDatabase.localAuthConfigDao(),
            auditSecurityLogDao = securityDatabase.auditSecurityLogDao(),
            firebaseDataSource = firebaseDataSource,
            googleDriveManager = googleDriveManager,
            userSessionDao = securityDatabase.userSessionDao(),
            financialDao = securityDatabase.financialDao(),
            fixedAssetDao = securityDatabase.fixedAssetDao(),
            sharedExpenseDao = securityDatabase.sharedExpenseDao(),
            thirdPartyDao = securityDatabase.thirdPartyDao(),
            thirdPartyCategoryDao = securityDatabase.thirdPartyCategoryDao(),
            merchantDao = securityDatabase.merchantDao(),
            outboxDao = securityDatabase.outboxDao(),
            assetAuditLogDao = securityDatabase.assetAuditLogDao(),
            assetCategoryDao = securityDatabase.assetCategoryDao(),
            autoCaptureDao = securityDatabase.autoCaptureDao(),
            securityDatabase = securityDatabase,
            context = context,
            keystoreCryptoManager = keystoreCryptoManager
        )
    }

    val financialRepository: com.example.domain.repository.FinancialRepository by lazy {
        com.example.data.repository.FinancialRepositoryImpl(
            financialDao = securityDatabase.financialDao(),
            auditLogDao = securityDatabase.auditSecurityLogDao(),
            firebaseDataSource = firebaseDataSource,
            merchantDao = securityDatabase.merchantDao(),
            outboxDao = securityDatabase.outboxDao(),
            keystoreCryptoManager = keystoreCryptoManager
        )
    }

    val recordExpenseUseCase: com.example.domain.usecase.RecordExpenseUseCase by lazy {
        com.example.domain.usecase.RecordExpenseUseCase(financialRepository)
    }

    val recordIncomeUseCase: com.example.domain.usecase.RecordIncomeUseCase by lazy {
        com.example.domain.usecase.RecordIncomeUseCase(financialRepository)
    }

    val recordTransferUseCase: com.example.domain.usecase.RecordTransferUseCase by lazy {
        com.example.domain.usecase.RecordTransferUseCase(financialRepository)
    }

    val createTransferUseCase: com.example.domain.usecase.CreateTransferUseCase by lazy {
        com.example.domain.usecase.CreateTransferUseCase(financialRepository)
    }

    val createBankEntityUseCase: com.example.domain.usecase.CreateBankEntityUseCase by lazy {
        com.example.domain.usecase.CreateBankEntityUseCase(financialRepository)
    }

    val financialDao: com.example.data.local.dao.FinancialDao by lazy {
        securityDatabase.financialDao()
    }

    val payrollAutomationUseCase: com.example.domain.usecase.PayrollAutomationUseCase by lazy {
        com.example.domain.usecase.PayrollAutomationUseCase(financialDao)
    }

    val validateCreditLimitsUseCase: com.example.domain.usecase.ValidateCreditLimitsUseCase by lazy {
        com.example.domain.usecase.ValidateCreditLimitsUseCase()
    }

    val calculateNextBillingDatesUseCase: com.example.domain.usecase.CalculateNextBillingDatesUseCase by lazy {
        com.example.domain.usecase.CalculateNextBillingDatesUseCase()
    }

    val createCreditCardUseCase: com.example.domain.usecase.CreateCreditCardUseCase by lazy {
        com.example.domain.usecase.CreateCreditCardUseCase(
            financialRepository = financialRepository,
            validateCreditLimitsUseCase = validateCreditLimitsUseCase,
            calculateNextBillingDatesUseCase = calculateNextBillingDatesUseCase
        )
    }

    val authenticateUserUseCase: AuthenticateUserUseCase by lazy {
        AuthenticateUserUseCase(authRepository)
    }

    val observeCategoriesUseCase: com.example.domain.usecase.ObserveCategoriesUseCase by lazy {
        com.example.domain.usecase.ObserveCategoriesUseCase(financialRepository)
    }

    val createCategoryUseCase: com.example.domain.usecase.CreateCategoryUseCase by lazy {
        com.example.domain.usecase.CreateCategoryUseCase(financialRepository)
    }

    val archiveCategoryUseCase: com.example.domain.usecase.ArchiveCategoryUseCase by lazy {
        com.example.domain.usecase.ArchiveCategoryUseCase(financialRepository)
    }

    val settingsRepository: com.example.features.settings.domain.repository.SettingsRepository by lazy {
        com.example.features.settings.data.repository.SettingsRepositoryImpl(
            context = context,
            auditSecurityLogDao = securityDatabase.auditSecurityLogDao(),
            fixedAssetDao = securityDatabase.fixedAssetDao()
        )
    }

    val frankfurterApiService: com.example.data.remote.FrankfurterApiService by lazy {
        com.example.data.remote.FrankfurterApiService()
    }

    val exchangeRateDao: com.example.data.local.dao.ExchangeRateDao by lazy {
        securityDatabase.exchangeRateDao()
    }

    val exchangeRateRepository: com.example.domain.repository.ExchangeRateRepository by lazy {
        com.example.data.repository.ExchangeRateRepositoryImpl(
            exchangeRateDao = exchangeRateDao,
            apiService = frankfurterApiService,
            firebaseDataSource = firebaseDataSource
        )
    }

    val exchangeRateTestSuite: com.example.util.testsuite.ExchangeRateTestSuite by lazy {
        com.example.util.testsuite.ExchangeRateTestSuite(
            apiService = frankfurterApiService,
            repository = exchangeRateRepository,
            exchangeRateDao = exchangeRateDao,
            firebaseDataSource = firebaseDataSource
        )
    }

    val fixedAssetDao: com.example.data.local.dao.FixedAssetDao by lazy {
        securityDatabase.fixedAssetDao()
    }

    val assetCategoryDao: com.example.data.local.dao.AssetCategoryDao by lazy {
        securityDatabase.assetCategoryDao()
    }

    val calculateAssetDepreciationUseCase: com.example.domain.usecase.fixedassets.CalculateAssetDepreciationUseCase by lazy {
        com.example.domain.usecase.fixedassets.CalculateAssetDepreciationUseCase()
    }

    val projectAssetValuesUseCase: com.example.domain.usecase.fixedassets.ProjectAssetValuesUseCase by lazy {
        com.example.domain.usecase.fixedassets.ProjectAssetValuesUseCase()
    }

    val assetAuditLogDao: com.example.data.local.dao.AssetAuditLogDao by lazy {
        securityDatabase.assetAuditLogDao()
    }

    val fixedAssetRepository: com.example.domain.repository.FixedAssetRepository by lazy {
        com.example.data.repository.FixedAssetRepositoryImpl(
            fixedAssetDao = fixedAssetDao,
            assetCategoryDao = assetCategoryDao,
            assetAuditLogDao = assetAuditLogDao,
            calculateDepreciationUseCase = calculateAssetDepreciationUseCase
        )
    }

    val financialReportsRepository: com.example.feature.informes.domain.repository.FinancialReportsRepository by lazy {
        com.example.feature.informes.data.repository.FinancialReportsRepositoryImpl(
            context = context,
            financialDao = securityDatabase.financialDao(),
            fixedAssetDao = securityDatabase.fixedAssetDao(),
            sharedExpenseDao = sharedExpenseDao
        )
    }

    val thirdPartyDao: com.example.data.local.dao.ThirdPartyDao by lazy {
        securityDatabase.thirdPartyDao()
    }

    val thirdPartyCategoryDao: com.example.data.local.dao.ThirdPartyCategoryDao by lazy {
        securityDatabase.thirdPartyCategoryDao()
    }

    val thirdPartyRepository: com.example.domain.repository.ThirdPartyRepository by lazy {
        com.example.data.repository.ThirdPartyRepositoryImpl(
            thirdPartyDao = thirdPartyDao,
            thirdPartyCategoryDao = thirdPartyCategoryDao
        )
    }

    // =========================================================================
    // MÓDULO DE GASTOS COMPARTIDOS, CONSECUTIVOS Y TICKETS (:feature-gastos-compartidos)
    // =========================================================================

    val sharedExpenseDao: com.example.data.local.dao.SharedExpenseDao by lazy {
        securityDatabase.sharedExpenseDao()
    }

    val sharedExpenseFirebaseAiEngine: com.example.domain.analytics.SharedExpenseFirebaseAiEngine by lazy {
        com.example.domain.analytics.FirebaseAiCloudProviderBridge.installIfNeeded()
        com.example.domain.analytics.SharedExpenseFirebaseAiEngine()
    }

    val createSharedExpenseUseCase: com.example.feature.gastoscompartidos.domain.usecase.CreateSharedExpenseUseCase by lazy {
        com.example.feature.gastoscompartidos.domain.usecase.CreateSharedExpenseUseCase(
            sharedExpenseDao = sharedExpenseDao,
            financialDao = financialDao,
            outboxDao = securityDatabase.outboxDao(),
            idGenerator = com.example.core.id.SequentialIdGenerator()
        )
    }

    val recordSharedSettlementUseCase: com.example.feature.gastoscompartidos.domain.usecase.RecordSharedSettlementUseCase by lazy {
        com.example.feature.gastoscompartidos.domain.usecase.RecordSharedSettlementUseCase(
            sharedExpenseDao = sharedExpenseDao,
            financialDao = financialDao,
            idGenerator = com.example.core.id.SequentialIdGenerator()
        )
    }

    val voidSharedExpenseUseCase: com.example.feature.gastoscompartidos.domain.usecase.VoidSharedExpenseUseCase by lazy {
        com.example.feature.gastoscompartidos.domain.usecase.VoidSharedExpenseUseCase(
            sharedExpenseDao = sharedExpenseDao,
            financialDao = financialDao
        )
    }

    val sharedTicketExporter: com.example.feature.gastoscompartidos.data.export.SharedTicketExporter by lazy {
        com.example.feature.gastoscompartidos.data.export.SharedTicketExporter(
            context = context,
            secureDocumentManager = com.example.data.local.SecureDocumentManager(context)
        )
    }
}

