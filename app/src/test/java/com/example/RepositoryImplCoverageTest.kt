package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.datasource.FirebaseDataSource
import com.example.data.local.SecurityDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.ExchangeRateCacheEntity
import com.example.data.local.entity.FixedAssetEntity
import com.example.data.local.entity.ManualExchangeRateEntity
import com.example.data.remote.FrankfurterApiService
import com.example.data.repository.ExchangeRateRepositoryImpl
import com.example.data.repository.FinancialRepositoryImpl
import com.example.data.repository.FixedAssetRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RepositoryImplCoverageTest {

    private lateinit var database: SecurityDatabase
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SecurityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        SecurityDatabase.installIntegrityGuards(database.openHelper.writableDatabase)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun financialRepository_recordsExpenseRejectsInvalidAmountAndHonorsIdempotency() = runBlocking {
        val account = AccountEntity(
            id = "account-1",
            userId = "user-1",
            name = "Cash",
            balance = 1_000.0,
            availableBalance = 1_000.0
        )
        database.financialDao().insertAccount(account)
        val repository = FinancialRepositoryImpl(
            financialDao = database.financialDao(),
            auditLogDao = database.auditSecurityLogDao(),
            firebaseDataSource = FirebaseDataSource(context)
        )

        val invalid = repository.recordExpense(
            userId = "user-1", accountId = account.id, accountName = account.name,
            categoryId = "cat", categoryName = "Food", amount = 0.0, currency = "COP",
            dateFormatted = "2026-10-12", description = "bad", receiptUrl = null,
            isRecurring = false, saveAsTemplate = false, tags = emptyList(), idempotencyKey = "bad"
        )
        assertTrue(invalid.isFailure)

        val first = repository.recordExpense(
            userId = "user-1", accountId = account.id, accountName = account.name,
            categoryId = "cat", categoryName = "Food", amount = 100.0, currency = "COP",
            dateFormatted = "2026-10-12", description = "  Lunch  ", receiptUrl = null,
            isRecurring = false, saveAsTemplate = false, tags = listOf("food"), idempotencyKey = "same-key",
            affectsBudget = false
        )
        val duplicate = repository.recordExpense(
            userId = "user-1", accountId = account.id, accountName = account.name,
            categoryId = "cat", categoryName = "Food", amount = 200.0, currency = "COP",
            dateFormatted = "2026-10-12", description = "second", receiptUrl = null,
            isRecurring = false, saveAsTemplate = false, tags = emptyList(), idempotencyKey = "same-key",
            affectsBudget = false
        )
        assertTrue(first.isSuccess)
        assertTrue(duplicate.isSuccess)
        assertEquals(first.getOrThrow().id, duplicate.getOrThrow().id)
        assertEquals("Lunch", first.getOrThrow().description)
        assertEquals(900.0, database.financialDao().getAccountById(account.id)?.balance ?: 0.0, 0.001)
    }

    @Test
    fun financialRepository_recordsIncomeAndScopesAccountsByUser() = runBlocking {
        val account = AccountEntity("account-income", "user-1", "Income", balance = 100.0, availableBalance = 100.0)
        database.financialDao().insertAccounts(listOf(account, account.copy(id = "other", userId = "user-2")))
        val repository = FinancialRepositoryImpl(
            database.financialDao(), database.auditSecurityLogDao(), FirebaseDataSource(context)
        )
        val result = repository.recordIncome(
            userId = "user-1", accountId = account.id, accountName = account.name,
            categoryId = "income", categoryName = "Salary", sourceId = null, sourceName = "Employer",
            amount = 500.0, currency = "COP", dateFormatted = "2026-10-12", description = null,
            receiptUrl = null, isRecurring = false, recurrenceFrequency = "Monthly",
            saveAsTemplate = false, tags = emptyList(), idempotencyKey = "income-1"
        )
        assertTrue(result.isSuccess)
        assertEquals(600.0, database.financialDao().getAccountById(account.id)?.balance ?: 0.0, 0.001)
        assertEquals(listOf("account-income"), repository.getAccounts("user-1").first().map { it.id })
    }

    @Test
    fun exchangeRateRepository_prioritizesManualRateAndReadsCache() = runBlocking {
        val dao = database.exchangeRateDao()
        dao.insertCachedRate(
            ExchangeRateCacheEntity(
                id = "USD_COP",
                baseCurrency = "USD",
                targetCurrency = "COP",
                rate = 4_000.0,
                rateDate = "2026-10-12",
                provider = "test",
                lastUpdatedMillis = System.currentTimeMillis(),
                isStale = false
            )
        )
        val repository = ExchangeRateRepositoryImpl(dao, FrankfurterApiService(), FirebaseDataSource(context))
        assertEquals(4_000.0, repository.getInstantEffectiveRate("USD", "COP").rate, 0.001)

        dao.insertManualRate(
            ManualExchangeRateEntity(
                id = "USD_COP",
                fromCurrency = "USD",
                toCurrency = "COP",
                rate = 4_200.0,
                validFromDate = "2026-01-01",
                validToDate = "2099-12-31"
            )
        )
        val manual = repository.getInstantEffectiveRate("USD", "COP")
        assertEquals(4_200.0, manual.rate, 0.001)
        assertTrue(manual.sourceType.name.contains("MANUAL"))
    }

    @Test
    fun fixedAssetRepository_createsAcquisitionMovementAndAuditThenDeletes() = runBlocking {
        val repository = FixedAssetRepositoryImpl(
            fixedAssetDao = database.fixedAssetDao(),
            assetCategoryDao = database.assetCategoryDao(),
            assetAuditLogDao = database.assetAuditLogDao()
        )
        val asset = FixedAssetEntity(
            id = "AF-001",
            userId = "user-1",
            name = "Laptop",
            type = "TANGIBLE",
            category = "Tecnología",
            acquisitionDate = "2026-10-12",
            acquisitionValue = 2_000.0
        )

        assertTrue(repository.saveAsset(asset).isSuccess)
        assertNotNull(repository.getAssetById(asset.id))
        assertEquals(1, database.fixedAssetDao().getMovementsForAsset(asset.id).count { it.type == "ACQUISITION" })
        assertEquals(1, database.assetAuditLogDao().getAllAuditLogs("user-1").size)

        val deleteResult = repository.deleteAsset(asset.id)
        assertTrue(deleteResult.isFailure)
        assertNotNull(repository.getAssetById(asset.id))
    }
}
