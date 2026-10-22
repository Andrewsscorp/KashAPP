package com.example.domain.repository

import com.example.data.local.dao.FinancialDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BankEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CityEntity
import com.example.data.local.entity.CountryEntity
import com.example.data.local.entity.CurrencyEntity
import com.example.data.local.entity.IncomeSourceEntity
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Interface for offline-first transactional financial data access.
 */
interface FinancialRepository {
    fun getAccounts(userId: String): Flow<List<AccountEntity>>
    suspend fun getAccountsSnapshot(userId: String): List<AccountEntity> = emptyList()
    fun getCategories(): Flow<List<CategoryEntity>>
    fun getCategories(userId: String): Flow<List<CategoryEntity>>
    fun getTransactions(userId: String): Flow<List<TransactionEntity>>
    suspend fun getTransactionsSnapshot(userId: String): List<TransactionEntity> = emptyList()
    fun getIncomeSources(userId: String): Flow<List<IncomeSourceEntity>>
    suspend fun getAccountById(accountId: String): AccountEntity?
    suspend fun recordExpense(
        userId: String,
        accountId: String,
        accountName: String,
        categoryId: String,
        categoryName: String,
        amount: Double,
        currency: String,
        dateFormatted: String,
        description: String?,
        receiptUrl: String?,
        isRecurring: Boolean,
        saveAsTemplate: Boolean,
        tags: List<String>,
        idempotencyKey: String = UUID.randomUUID().toString(),
        affectsBudget: Boolean = true
    ): Result<TransactionEntity>
    suspend fun recordIncome(
        userId: String,
        accountId: String,
        accountName: String,
        categoryId: String,
        categoryName: String,
        sourceId: String?,
        sourceName: String,
        amount: Double,
        currency: String,
        dateFormatted: String,
        description: String?,
        receiptUrl: String?,
        isRecurring: Boolean,
        recurrenceFrequency: String = "Mensual",
        saveAsTemplate: Boolean,
        tags: List<String>,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): Result<TransactionEntity>
    suspend fun recordTransfer(
        userId: String,
        sourceAccountId: String,
        sourceAccountName: String,
        destinationAccountId: String,
        destinationAccountName: String,
        amount: Double,
        currency: String,
        dateFormatted: String,
        concept: String?,
        note: String?,
        receiptUrl: String?,
        saveAsTemplate: Boolean,
        tags: List<String>,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): Result<Pair<TransactionEntity, TransactionEntity>>
    suspend fun createCategory(
        name: String,
        colorHex: String = "#0284C7",
        icon: String = "category",
        userId: String? = null,
        type: String = "EXPENSE"
    ): CategoryEntity
    suspend fun createAccount(
        name: String,
        type: String,
        balance: Double,
        currency: String = "COP",
        userId: String,
        bankName: String = "Bancolombia",
        isFavorite: Boolean = false,
        accountNumber: String? = null,
        openingDate: String? = null,
        kashappDate: String = "2026-09-26",
        generatesInterest: Boolean = false,
        interestRateEa: Double? = null,
        calcFrequency: String? = null,
        payoutFrequency: String? = null,
        compoundInterest: Boolean = false,
        purposes: String = "Ahorro",
        customColorHex: String = "#0284C7",
        iconName: String = "ic_bank_default",
        minimumBalance: Double = 0.0,
        monthlyFee: Double = 0.0,
        feeChargeDay: Int = 30,
        syncProvider: String? = null,
        syncInstitutionId: String? = null,
        syncAccessToken: String? = null,
        supportingDocumentUri: String? = null,
        supportingDocumentType: String? = null,
        availableBalance: Double? = null,
        holdBalance: Double = 0.0,
        country: String = "CO",
        cityName: String? = null,
        favoriteRouting: String = "GENERAL",
        favoriteForIncomes: Boolean = false,
        favoriteForExpenses: Boolean = false,
        employerName: String? = null,
        accountOwner: String? = null,
        alias: String? = null,
        hasOverdraft: Boolean = false,
        overdraftLimit: Double = 0.0,
        overdraftRateEa: Double = 0.0,
        isTaxExempt: Boolean = false,
        payrollConfig: com.example.data.local.entity.AccountPayrollConfigEntity? = null
    ): AccountEntity

    /**
     * Encrypts a sensitive account field (account number, token) using hardware-backed AES-256-GCM
     * with an "ENCv1:" envelope prefix.
     */
    fun encryptSensitiveField(rawValue: String?): String? = rawValue

    /**
     * Decrypts a sensitive account field. Transparently handles "ENCv1:" AES-GCM ciphertext
     * as well as legacy plaintext Base64 records for zero-loss backwards compatibility.
     */
    fun decryptSensitiveField(storedValue: String?): String? = storedValue
    suspend fun updateAccountAllowNegative(accountId: String, allowNegative: Boolean)
    suspend fun updateAccount(account: AccountEntity): Result<Unit>
    suspend fun deleteAccount(accountId: String): Result<Unit>
    suspend fun deleteAccountWithAudit(accountId: String, preserveAuditTrail: Boolean = true): Result<Unit>
    fun getTransactionsByAccount(accountId: String): Flow<List<TransactionEntity>>
    suspend fun createIncomeSource(
        userId: String,
        name: String
    ): IncomeSourceEntity
    fun getBanks(userId: String): Flow<List<BankEntity>>
    suspend fun existsBankByCode(code: String): Boolean
    suspend fun getBankByClearingCode(code: String): BankEntity?
    suspend fun createBank(
        userId: String,
        name: String,
        clearingCode: String,
        countryCode: String = "CO",
        countryName: String = "Colombia",
        currencyCode: String = "COP",
        cityName: String? = null,
        logoUrl: String? = null,
        colorHex: String = "#0284C7",
        iconName: String = "account_balance",
        additionalInfo: String? = null
    ): Result<BankEntity>
    suspend fun deleteBank(bankId: String, bankName: String): Result<Unit>
    suspend fun reorderBanks(banks: List<BankEntity>): Result<Unit>

    // Currencies Master Data & FX Engine
    fun getCurrencies(userId: String): Flow<List<CurrencyEntity>>
    suspend fun createCurrency(
        userId: String,
        code: String,
        name: String,
        symbol: String = "$",
        manualExchangeRate: Double = 1.0
    ): Result<CurrencyEntity>
    suspend fun deleteCurrency(currencyId: String, currencyCode: String): Result<Unit>

    // Countries Master Data
    fun getCountries(userId: String): Flow<List<CountryEntity>>
    suspend fun createCountry(
        userId: String,
        isoCode: String,
        name: String,
        flagEmoji: String = "🌐",
        defaultCurrencyCode: String = "COP",
        clearingSystemName: String = "ACH",
        isNumericClearingCode: Boolean = true
    ): Result<CountryEntity>
    suspend fun deleteCountry(countryId: String, countryCode: String): Result<Unit>

    // Cities Master Data
    fun getCities(countryCode: String, userId: String): Flow<List<CityEntity>>
    fun getAllCities(userId: String): Flow<List<CityEntity>>
    suspend fun createCity(
        userId: String,
        countryCode: String,
        name: String,
        stateOrDepartment: String? = null
    ): Result<CityEntity>
    suspend fun deleteCity(cityId: String): Result<Unit>

    // Payroll Concepts Master Data
    fun getPayrollConcepts(): Flow<List<com.example.data.local.entity.PayrollConceptEntity>>
    suspend fun createPayrollConcept(name: String): Result<com.example.data.local.entity.PayrollConceptEntity>

    // CDT Investments
    fun getAccountCdt(accountId: String): Flow<com.example.data.local.entity.AccountCdtEntity?>
    suspend fun createCdtAccount(
        cdtAccount: AccountEntity,
        cdtConfig: com.example.data.local.entity.AccountCdtEntity,
        sourceAccountId: String? = null
    ): Result<Unit>

    // Credit Cards (Pasivos Rotativos)
    fun getAccountCreditCard(accountId: String): Flow<com.example.data.local.entity.AccountCreditCardEntity?>
    fun getAllAccountCreditCards(): Flow<List<com.example.data.local.entity.AccountCreditCardEntity>>
    fun getAccountCreditCards(userId: String): Flow<List<com.example.data.local.entity.AccountCreditCardEntity>>
    suspend fun createCreditCardAccount(
        cardAccount: AccountEntity,
        cardConfig: com.example.data.local.entity.AccountCreditCardEntity
    ): Result<Pair<AccountEntity, com.example.data.local.entity.AccountCreditCardEntity>>

    // Financial Goals & Mental Accounting Virtual Pockets
    fun getGoals(userId: String): Flow<List<com.example.data.local.entity.FinancialGoalEntity>>
    suspend fun getGoalById(id: String): com.example.data.local.entity.FinancialGoalEntity?
    suspend fun createGoal(
        goal: com.example.data.local.entity.FinancialGoalEntity,
        freezeAmount: Double
    ): Result<com.example.data.local.entity.FinancialGoalEntity>
    suspend fun contributeToGoal(goalId: String, amount: Double, sourceAccountId: String? = null): Result<Unit>
    suspend fun updateGoal(goal: com.example.data.local.entity.FinancialGoalEntity): Result<Unit>
    suspend fun setGoalActive(goalId: String, isActive: Boolean): Result<Unit>
    suspend fun deleteGoal(goalId: String): Result<Unit>

    // Budgets & Category Envelopes (Presupuestos y Sobres)
    fun getExpenseCategories(): Flow<List<CategoryEntity>>
    fun getBudgets(userId: String): Flow<List<com.example.data.local.entity.BudgetEntity>>
    fun getBudgetWithItems(budgetId: String): Flow<com.example.data.local.entity.BudgetWithItems?>
    fun getAllBudgetsWithItems(userId: String): Flow<List<com.example.data.local.entity.BudgetWithItems>>
    suspend fun createBudget(
        budget: com.example.data.local.entity.BudgetEntity,
        items: List<com.example.data.local.entity.BudgetItemEntity>
    ): Result<com.example.data.local.entity.BudgetEntity>
    suspend fun deleteBudget(budgetId: String): Result<Unit>
    suspend fun rebalanceEnvelopes(sourceItemId: String, targetItemId: String, amount: Double): Result<Unit>
    suspend fun updateBudgetItemAllocated(itemId: String, newAmount: Double): Result<Unit>
    suspend fun updateBudgetTotalAmount(budgetId: String, newTotal: Double): Result<Unit>
    suspend fun sweepSurplusToGoal(goalId: String, amount: Double, userId: String): Result<Unit>
    suspend fun cloneBudgetForNextMonth(budgetId: String, userId: String): Result<com.example.data.local.entity.BudgetEntity>

    // Investments (Inversiones y Activos)
    fun getInvestments(userId: String): Flow<List<com.example.data.local.entity.InvestmentEntity>>
    suspend fun getInvestmentById(id: String): com.example.data.local.entity.InvestmentEntity?
    suspend fun createInvestment(
        investment: com.example.data.local.entity.InvestmentEntity,
        document: com.example.data.local.entity.InvestmentDocumentEntity? = null
    ): Result<com.example.data.local.entity.InvestmentEntity>
    suspend fun deleteInvestment(id: String): Result<Unit>
    fun getInvestmentDocuments(investmentId: String): Flow<List<com.example.data.local.entity.InvestmentDocumentEntity>>

    // Canonical Categories (Soft-delete & Filtered)
    fun getActiveCategories(type: String? = null): Flow<List<CategoryEntity>>
    suspend fun setCategoryActive(id: String, isActive: Boolean): Result<Unit>
    suspend fun getCategoryById(id: String): CategoryEntity?

    // Merchants & Normalization
    fun getMerchantsFlow(userId: String): Flow<List<com.example.data.local.entity.MerchantEntity>>
    suspend fun createMerchant(
        canonicalName: String,
        alias: String,
        defaultCategoryId: String? = null,
        confidence: Int = 90,
        userId: String? = null
    ): com.example.data.local.entity.MerchantEntity
    suspend fun setMerchantActive(id: String, isActive: Boolean): Result<Unit>

    // Transactional Outbox & Durable Sync Queue
    fun observePendingOutboxCount(): Flow<Int>
    suspend fun getPendingOutboxOperations(): List<com.example.data.local.entity.OutboxOperationEntity>
    suspend fun processOutboxQueue(): Result<Int>

    // Tags
    fun getTags(): Flow<List<com.example.data.local.entity.TagEntity>>
    suspend fun insertTag(name: String, colorHex: String = "#0284C7"): com.example.data.local.entity.TagEntity

    // Scheduled Payments & Reminders
    fun getScheduledPayments(userId: String): Flow<List<com.example.data.local.entity.ScheduledPaymentEntity>>
    suspend fun createScheduledPayment(payment: com.example.data.local.entity.ScheduledPaymentEntity): Result<Unit>
    suspend fun updateScheduledPayment(payment: com.example.data.local.entity.ScheduledPaymentEntity): Result<Unit>
    suspend fun deleteScheduledPayment(id: String): Result<Unit>
    suspend fun setScheduledPaymentPaid(id: String, isPaid: Boolean, paidDate: String? = null): Result<Unit>

    suspend fun seedDefaultsIfNeeded(userId: String)
    suspend fun restoreFinancialBackupFromCloudIfNeeded(userId: String): Result<Boolean> = Result.success(false)
    suspend fun restoreFullPortfolioFromCloud(userId: String): Result<Boolean> = Result.success(false)
    suspend fun backupFullPortfolioToCloud(userId: String): Result<Boolean> = Result.success(false)
    suspend fun getCloudBackupMetadata(userId: String): Map<String, Any>? = null
    fun clearMemoryCaches() {}

    /**
     * Reverses and deletes a transaction with ACID atomic double-entry guarantees.
     */
    suspend fun reverseAndDeleteTransaction(transactionId: String, userId: String): DeleteTransactionResult
}

sealed class DeleteTransactionResult {
    data class Success(val deletedTransactionIds: List<String>, val affectedAccountIds: List<String>) : DeleteTransactionResult()
    data class BlockedByBusinessRule(val reason: String) : DeleteTransactionResult()
    data class NotFoundOrUnauthorized(val message: String) : DeleteTransactionResult()
    data class Error(val cause: Throwable) : DeleteTransactionResult()
}
