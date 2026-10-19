package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BankEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CityEntity
import com.example.data.local.entity.CountryEntity
import com.example.data.local.entity.CurrencyEntity
import com.example.data.local.entity.IncomeSourceEntity
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object providing atomic, ACID-compliant financial operations.
 */
@Dao
interface FinancialDao {

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions WHERE (userId = :userId OR userId = 'user_default' OR :userId = '') ORDER BY createdAt DESC")
    fun getTransactionsFlow(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE (userId = :userId OR userId = 'user_default' OR :userId = '') ORDER BY createdAt DESC")
    suspend fun getTransactions(userId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE idempotencyKey = :idempotencyKey LIMIT 1")
    suspend fun getTransactionByIdempotencyKey(idempotencyKey: String): TransactionEntity?

    @Query("SELECT * FROM accounts WHERE (userId = :userId OR userId = 'user_default' OR :userId = '') ORDER BY isFavorite DESC, balance DESC")
    fun getAccountsFlow(userId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE (userId = :userId OR userId = 'user_default' OR :userId = '') ORDER BY isFavorite DESC, balance DESC")
    suspend fun getAccounts(userId: String): List<AccountEntity>

    @Query("SELECT * FROM accounts ORDER BY isFavorite DESC, balance DESC")
    fun getAllAccountsFlow(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :accountId LIMIT 1")
    suspend fun getAccountById(accountId: String): AccountEntity?

    @androidx.room.Upsert
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @androidx.room.Upsert
    suspend fun insertAccount(account: AccountEntity)

    @Query("UPDATE accounts SET balance = COALESCE(balance, 0.0) - :amount, availableBalance = CASE WHEN availableBalance IS NOT NULL THEN availableBalance - :amount ELSE COALESCE(balance, 0.0) - :amount END WHERE id = :accountId")
    suspend fun deductAccountBalance(accountId: String, amount: Double): Int

    @Query("UPDATE accounts SET balance = COALESCE(balance, 0.0) + :amount, availableBalance = CASE WHEN availableBalance IS NOT NULL THEN availableBalance + :amount ELSE COALESCE(balance, 0.0) + :amount END WHERE id = :accountId")
    suspend fun addAccountBalance(accountId: String, amount: Double): Int

    @Query("UPDATE accounts SET hasOverdraft = :allowNegative WHERE id = :accountId")
    suspend fun updateAccountAllowNegative(accountId: String, allowNegative: Boolean)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun deleteAccountById(accountId: String)

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteTransactionsByAccountId(accountId: String)

    @Query("DELETE FROM accounts WHERE userId = :userId")
    suspend fun deleteAccountsByUserId(userId: String)

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun deleteTransactionsByUserId(userId: String)

    @Query("DELETE FROM financial_goals WHERE userId = :userId")
    suspend fun deleteGoalsByUserId(userId: String)

    @Query("DELETE FROM budgets WHERE userId = :userId")
    suspend fun deleteBudgetsByUserId(userId: String)

    @Query("DELETE FROM investments WHERE userId = :userId")
    suspend fun deleteInvestmentsByUserId(userId: String)

    @Query("DELETE FROM scheduled_payments WHERE user_id = :userId")
    suspend fun deleteScheduledPaymentsByUserId(userId: String)

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC, createdAt DESC")
    fun getTransactionsByAccountFlow(accountId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC, createdAt DESC")
    suspend fun getTransactionsByAccount(accountId: String): List<TransactionEntity>

    // ==========================================
    // BANKS MASTER DATA DAO OPERATIONS
    // ==========================================

    @Query("SELECT * FROM banks WHERE userId IS NULL OR userId = :userId ORDER BY orderIndex ASC, name ASC")
    fun getBanksFlow(userId: String): Flow<List<BankEntity>>

    @Query("SELECT * FROM banks WHERE userId IS NULL OR userId = :userId ORDER BY orderIndex ASC, name ASC")
    suspend fun getBanks(userId: String): List<BankEntity>

    @Query("SELECT * FROM banks WHERE id = :id LIMIT 1")
    suspend fun getBankById(id: String): BankEntity?

    @Query("SELECT * FROM banks WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getBankByName(name: String): BankEntity?

    @Query("SELECT * FROM banks WHERE clearingCode = :clearingCode LIMIT 1")
    suspend fun getBankByClearingCode(clearingCode: String): BankEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBank(bank: BankEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanks(banks: List<BankEntity>)

    @Delete
    suspend fun deleteBank(bank: BankEntity)

    @Query("DELETE FROM banks WHERE id = :id")
    suspend fun deleteBankById(id: String)

    @Query("SELECT COUNT(*) FROM accounts WHERE LOWER(bankName) = LOWER(:bankName)")
    suspend fun getAccountsCountByBankName(bankName: String): Int

    // ==========================================
    // CURRENCIES MASTER DATA DAO OPERATIONS
    // ==========================================
    @Query("SELECT * FROM currencies WHERE userId IS NULL OR userId = :userId ORDER BY CASE WHEN code = 'COP' THEN 0 WHEN code = 'USD' THEN 1 WHEN code = 'EUR' THEN 2 ELSE 3 END, name ASC")
    fun getCurrenciesFlow(userId: String): Flow<List<CurrencyEntity>>

    @Query("SELECT * FROM currencies WHERE userId IS NULL OR userId = :userId ORDER BY CASE WHEN code = 'COP' THEN 0 WHEN code = 'USD' THEN 1 WHEN code = 'EUR' THEN 2 ELSE 3 END, name ASC")
    suspend fun getCurrencies(userId: String): List<CurrencyEntity>

    @Query("SELECT * FROM currencies WHERE id = :id LIMIT 1")
    suspend fun getCurrencyById(id: String): CurrencyEntity?

    @Query("SELECT * FROM currencies WHERE UPPER(code) = UPPER(:code) AND (userId IS NULL OR userId = :userId) LIMIT 1")
    suspend fun getCurrencyByCode(code: String, userId: String): CurrencyEntity?

    @Query("SELECT * FROM currencies WHERE UPPER(code) = UPPER(:code) LIMIT 1")
    suspend fun findAnyCurrencyByCode(code: String): CurrencyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrency(currency: CurrencyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrencies(currencies: List<CurrencyEntity>)

    @Query("DELETE FROM currencies WHERE id = :id")
    suspend fun deleteCurrencyById(id: String)

    @Query("SELECT COUNT(*) FROM accounts WHERE UPPER(currency) = UPPER(:code)")
    suspend fun getAccountsCountByCurrency(code: String): Int

    @Query("SELECT COUNT(*) FROM banks WHERE UPPER(currencyCode) = UPPER(:code)")
    suspend fun getBanksCountByCurrency(code: String): Int

    // ==========================================
    // COUNTRIES MASTER DATA DAO OPERATIONS
    // ==========================================
    @Query("SELECT * FROM countries WHERE userId IS NULL OR userId = :userId ORDER BY CASE WHEN isoCode = 'CO' THEN 0 WHEN isoCode = 'US' THEN 1 ELSE 2 END, name ASC")
    fun getCountriesFlow(userId: String): Flow<List<CountryEntity>>

    @Query("SELECT * FROM countries WHERE userId IS NULL OR userId = :userId ORDER BY CASE WHEN isoCode = 'CO' THEN 0 WHEN isoCode = 'US' THEN 1 ELSE 2 END, name ASC")
    suspend fun getCountries(userId: String): List<CountryEntity>

    @Query("SELECT * FROM countries WHERE id = :id LIMIT 1")
    suspend fun getCountryById(id: String): CountryEntity?

    @Query("SELECT * FROM countries WHERE UPPER(isoCode) = UPPER(:isoCode) LIMIT 1")
    suspend fun getCountryByCode(isoCode: String): CountryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCountry(country: CountryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCountries(countries: List<CountryEntity>)

    @Query("DELETE FROM countries WHERE id = :id")
    suspend fun deleteCountryById(id: String)

    @Query("SELECT COUNT(*) FROM accounts WHERE UPPER(country) = UPPER(:isoCode)")
    suspend fun getAccountsCountByCountry(isoCode: String): Int

    @Query("SELECT COUNT(*) FROM banks WHERE UPPER(countryCode) = UPPER(:isoCode)")
    suspend fun getBanksCountByCountry(isoCode: String): Int

    // ==========================================
    // CITIES MASTER DATA DAO OPERATIONS
    // ==========================================
    @Query("SELECT * FROM cities WHERE UPPER(countryCode) = UPPER(:countryCode) AND (userId IS NULL OR userId = :userId) ORDER BY name ASC")
    fun getCitiesByCountryFlow(countryCode: String, userId: String): Flow<List<CityEntity>>

    @Query("SELECT * FROM cities WHERE UPPER(countryCode) = UPPER(:countryCode) AND (userId IS NULL OR userId = :userId) ORDER BY name ASC")
    suspend fun getCitiesByCountry(countryCode: String, userId: String): List<CityEntity>

    @Query("SELECT * FROM cities WHERE userId IS NULL OR userId = :userId ORDER BY name ASC")
    fun getAllCitiesFlow(userId: String): Flow<List<CityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCity(city: CityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCities(cities: List<CityEntity>)

    @Query("DELETE FROM cities WHERE id = :id")
    suspend fun deleteCityById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE TRIM(name) != '' ORDER BY name ASC")
    fun getCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE TRIM(name) != '' ORDER BY name ASC")
    suspend fun getCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE isActive = 1 AND TRIM(name) != '' ORDER BY name ASC")
    fun getActiveCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = 'EXPENSE' AND TRIM(name) != '' ORDER BY name ASC")
    fun getExpenseCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = 'EXPENSE' AND isActive = 1 AND TRIM(name) != '' ORDER BY name ASC")
    fun getActiveExpenseCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = 'EXPENSE' AND TRIM(name) != '' ORDER BY name ASC")
    suspend fun getExpenseCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE type = 'INCOME' AND TRIM(name) != '' ORDER BY name ASC")
    suspend fun getIncomeCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE type = 'INCOME' AND isActive = 1 AND TRIM(name) != '' ORDER BY name ASC")
    fun getActiveIncomeCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("DELETE FROM categories WHERE TRIM(name) = ''")
    suspend fun deleteBlankCategories()

    @Query("UPDATE categories SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setCategoryActive(id: String, isActive: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun getTransactionsCountByCategoryId(categoryId: String): Int

    @Query("SELECT COUNT(*) FROM budget_items WHERE categoryId = :categoryId")
    suspend fun getBudgetItemsCountByCategoryId(categoryId: String): Int

    @Query("SELECT * FROM categories WHERE (LOWER(name) = LOWER(:name) OR name = :name) AND (type = :type OR type = 'BOTH') LIMIT 1")
    suspend fun findCategoryByName(name: String, type: String = "EXPENSE"): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeSource(source: IncomeSourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeSources(sources: List<IncomeSourceEntity>)

    @Query("SELECT * FROM income_sources WHERE userId = :userId AND isActive = 1 ORDER BY name ASC")
    fun getIncomeSourcesFlow(userId: String): Flow<List<IncomeSourceEntity>>

    @Query("SELECT * FROM income_sources WHERE userId = :userId AND isActive = 1 ORDER BY name ASC")
    suspend fun getIncomeSources(userId: String): List<IncomeSourceEntity>

    /**
     * ACID Atomic Transaction: Inserts the expense record and deducts money from the account
     * within a single atomic database commit.
     */
    @Transaction
    suspend fun executeExpenseTransaction(
        transaction: TransactionEntity,
        userId: String,
        affectsBudget: Boolean = true
    ) {
        require(transaction.amount > 0.0) { "El monto de la transacción debe ser mayor que cero." }
        insertTransaction(transaction)
        check(deductAccountBalance(
            accountId = transaction.accountId,
            amount = transaction.amount
        ) == 1) { "La cuenta del gasto no existe o fue modificada concurrentemente." }
        if (affectsBudget) {
            // Automatically accumulate spent amount in matching active budget envelopes for this period & category
            updateBudgetItemsSpentForExpense(
                userId = userId,
                categoryId = transaction.categoryId,
                categoryName = transaction.categoryName,
                expenseDate = transaction.date,
                amount = transaction.amount
            )
        }
        // Synchronize credit card quota if this account is a credit card
        check(updateCardQuotaOnExpense(
            accountId = transaction.accountId,
            amount = transaction.amount
        ) <= 1) { "La cuenta de tarjeta no es consistente." }
    }

    /**
     * ACID Atomic Transaction: Inserts the income record and increments money in the destination
     * account within a single atomic database commit (UPDATE accounts SET balance = balance + amount).
     */
    @Transaction
    suspend fun executeIncomeTransaction(
        transaction: TransactionEntity,
        userId: String
    ) {
        require(transaction.amount > 0.0) { "El monto de la transacción debe ser mayor que cero." }
        insertTransaction(transaction)
        check(addAccountBalance(
            accountId = transaction.accountId,
            amount = transaction.amount
        ) == 1) { "La cuenta del ingreso no existe o fue modificada concurrentemente." }
        // Restitute credit card quota if income/payment was directed to a credit card
        check(updateCardQuotaOnPayment(
            accountId = transaction.accountId,
            amount = transaction.amount
        ) <= 1) { "La cuenta de tarjeta no es consistente." }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransferGroup(group: com.example.data.local.entity.TransferGroupEntity)

    /**
     * ACID Atomic 4-step Double-Entry Transfer Transaction:
     * 1. Inserts transfer_groups record.
     * 2. Inserts TRANSFER_OUT transaction linked to source account.
     * 3. Inserts TRANSFER_IN transaction linked to destination account.
     * 4. Debits source account (UPDATE accounts SET balance = balance - amount).
     * 5. Credits destination account (UPDATE accounts SET balance = balance + amount).
     * All execute within a single atomic database transaction (BEGIN ... COMMIT / ROLLBACK).
     */
    @Transaction
    suspend fun executeTransferTransaction(
        transferGroup: com.example.data.local.entity.TransferGroupEntity,
        transferOut: TransactionEntity,
        transferIn: TransactionEntity,
        userId: String
    ) {
        require(transferOut.amount > 0.0) { "El monto de la transferencia debe ser mayor que cero." }
        require(transferOut.accountId != transferIn.accountId) { "Una transferencia requiere cuentas distintas." }
        insertTransferGroup(transferGroup)
        insertTransaction(transferOut)
        insertTransaction(transferIn)
        check(deductAccountBalance(
            accountId = transferOut.accountId,
            amount = transferOut.amount
        ) == 1) { "La cuenta origen de la transferencia no existe." }
        check(addAccountBalance(
            accountId = transferIn.accountId,
            amount = transferIn.amount
        ) == 1) { "La cuenta destino de la transferencia no existe." }
        // If transfer out was from a credit card (cash advance), increase used quota
        check(updateCardQuotaOnExpense(
            accountId = transferOut.accountId,
            amount = transferOut.amount
        ) <= 1) { "La cuenta origen de tarjeta no es consistente." }
        // If transfer in was to a credit card (credit card payment), restitute available quota
        check(updateCardQuotaOnPayment(
            accountId = transferIn.accountId,
            amount = transferIn.amount
        ) <= 1) { "La cuenta destino de tarjeta no es consistente." }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayrollConfig(config: com.example.data.local.entity.AccountPayrollConfigEntity)

    @Query("SELECT * FROM account_payroll_configs WHERE account_id = :accountId LIMIT 1")
    suspend fun getPayrollConfig(accountId: String): com.example.data.local.entity.AccountPayrollConfigEntity?

    @Query("SELECT * FROM account_payroll_configs WHERE auto_register = 1")
    suspend fun getActivePayrollConfigs(): List<com.example.data.local.entity.AccountPayrollConfigEntity>

    @Query("SELECT * FROM account_payroll_configs")
    suspend fun getAllPayrollConfigs(): List<com.example.data.local.entity.AccountPayrollConfigEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId AND type = 'INCOME' AND (categoryName = 'Nómina' OR description LIKE '%Nómina%') AND strftime('%Y-%m', date) = :yearMonth")
    suspend fun countPayrollTransactionsThisMonth(accountId: String, yearMonth: String): Int

    /**
     * ACID Atomic Transaction: Creates the Account, registers the Payroll Config,
     * and optionally posts the Initial Balance transaction in a single commit.
     */
    @Transaction
    suspend fun executePayrollAccountCreation(
        account: AccountEntity,
        payrollConfig: com.example.data.local.entity.AccountPayrollConfigEntity,
        initialTransaction: TransactionEntity? = null
    ) {
        insertAccount(account)
        insertPayrollConfig(payrollConfig)
        if (initialTransaction != null) {
            insertTransaction(initialTransaction)
        }
    }

    // Payroll Concepts (Conceptos habituales de nómina)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayrollConcept(concept: com.example.data.local.entity.PayrollConceptEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPayrollConcepts(concepts: List<com.example.data.local.entity.PayrollConceptEntity>)

    @Query("SELECT * FROM payroll_concepts ORDER BY isDefault DESC, createdAt ASC")
    suspend fun getAllPayrollConcepts(): List<com.example.data.local.entity.PayrollConceptEntity>

    @Query("SELECT * FROM payroll_concepts ORDER BY isDefault DESC, createdAt ASC")
    fun getAllPayrollConceptsFlow(): kotlinx.coroutines.flow.Flow<List<com.example.data.local.entity.PayrollConceptEntity>>

    // CDT Investments (Certificados de Depósito a Término)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccountCdt(cdt: com.example.data.local.entity.AccountCdtEntity)

    @Query("SELECT * FROM account_cdts WHERE account_id = :accountId LIMIT 1")
    suspend fun getAccountCdt(accountId: String): com.example.data.local.entity.AccountCdtEntity?

    @Query("SELECT * FROM account_cdts WHERE account_id = :accountId LIMIT 1")
    fun getAccountCdtFlow(accountId: String): kotlinx.coroutines.flow.Flow<com.example.data.local.entity.AccountCdtEntity?>

    @Query("SELECT * FROM account_cdts")
    suspend fun getAllAccountCdts(): List<com.example.data.local.entity.AccountCdtEntity>

    /**
     * ACID Atomic Transaction: Creates the Account (type=CDT), registers the CDT metadata,
     * updates source account if funding from existing account, and logs the transfer or initial transaction.
     */
    @Transaction
    suspend fun executeCdtAccountCreation(
        cdtAccount: AccountEntity,
        cdtConfig: com.example.data.local.entity.AccountCdtEntity,
        sourceAccountToDebit: AccountEntity? = null,
        transferDebitTx: TransactionEntity? = null,
        transferCreditTx: TransactionEntity? = null,
        transferGroup: com.example.data.local.entity.TransferGroupEntity? = null
    ) {
        insertAccount(cdtAccount)
        insertAccountCdt(cdtConfig)
        if (sourceAccountToDebit != null && transferDebitTx != null) {
            check(deductAccountBalance(sourceAccountToDebit.id, transferDebitTx.amount) == 1) {
                "La cuenta origen del CDT no existe."
            }
        }
        if (transferGroup != null) {
            insertTransferGroup(transferGroup)
        }
        if (transferDebitTx != null) {
            insertTransaction(transferDebitTx)
        }
        if (transferCreditTx != null) {
            insertTransaction(transferCreditTx)
        }
    }

    // ==========================================
    // CREDIT CARD (PASIVOS) DAO OPERATIONS
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccountCreditCard(creditCard: com.example.data.local.entity.AccountCreditCardEntity)

    @Query("SELECT * FROM account_credit_cards WHERE account_id = :accountId LIMIT 1")
    suspend fun getAccountCreditCard(accountId: String): com.example.data.local.entity.AccountCreditCardEntity?

    @Query("SELECT * FROM account_credit_cards WHERE account_id = :accountId LIMIT 1")
    fun getAccountCreditCardFlow(accountId: String): kotlinx.coroutines.flow.Flow<com.example.data.local.entity.AccountCreditCardEntity?>

    @Query("SELECT * FROM account_credit_cards")
    suspend fun getAllAccountCreditCards(): List<com.example.data.local.entity.AccountCreditCardEntity>

    @Query("SELECT * FROM account_credit_cards")
    fun getAllAccountCreditCardsFlow(): kotlinx.coroutines.flow.Flow<List<com.example.data.local.entity.AccountCreditCardEntity>>

    @Query("UPDATE account_credit_cards SET quota_used = quota_used + :amount, quota_available = quota_total - (quota_used + :amount) WHERE account_id = :accountId")
    suspend fun updateCardQuotaOnExpense(accountId: String, amount: Double): Int

    @Query("UPDATE account_credit_cards SET quota_used = CASE WHEN (quota_used - :amount) < 0.0 THEN 0.0 ELSE (quota_used - :amount) END, quota_available = CASE WHEN (quota_used - :amount) < 0.0 THEN quota_total ELSE quota_total - (quota_used - :amount) END WHERE account_id = :accountId")
    suspend fun updateCardQuotaOnPayment(accountId: String, amount: Double): Int

    /**
     * ACID Atomic Transaction: Creates the Account (type=CREDIT_CARD, balance = -quotaUsed as liability),
     * persists the Credit Card metadata, and registers an initial liability balance transaction if needed.
     */
    @Transaction
    suspend fun executeCreditCardAccountCreation(
        cardAccount: AccountEntity,
        cardConfig: com.example.data.local.entity.AccountCreditCardEntity,
        initialLiabilityTx: TransactionEntity? = null
    ) {
        insertAccount(cardAccount)
        insertAccountCreditCard(cardConfig)
        if (initialLiabilityTx != null) {
            insertTransaction(initialLiabilityTx)
        }
    }

    // ==========================================
    // FINANCIAL GOALS & VIRTUAL POCKETS DAO OPERATIONS
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: com.example.data.local.entity.FinancialGoalEntity)

    @Query("SELECT * FROM financial_goals WHERE userId = :userId OR userId = 'user_default' ORDER BY createdAt DESC")
    fun getAllGoalsFlow(userId: String): kotlinx.coroutines.flow.Flow<List<com.example.data.local.entity.FinancialGoalEntity>>

    @Query("SELECT * FROM financial_goals WHERE (userId = :userId OR userId = 'user_default') AND isActive = 1 ORDER BY createdAt DESC")
    fun getGoalsFlow(userId: String): kotlinx.coroutines.flow.Flow<List<com.example.data.local.entity.FinancialGoalEntity>>

    @Query("SELECT * FROM financial_goals WHERE id = :id LIMIT 1")
    suspend fun getGoalById(id: String): com.example.data.local.entity.FinancialGoalEntity?

    @Query("UPDATE accounts SET availableBalance = CASE WHEN availableBalance - :freezeAmount < 0 THEN 0.0 ELSE availableBalance - :freezeAmount END, holdBalance = holdBalance + :freezeAmount WHERE id = :accountId")
    suspend fun freezeAccountVirtualPocket(accountId: String, freezeAmount: Double): Int

    @Query("UPDATE financial_goals SET savedAmount = savedAmount + :amount WHERE id = :goalId")
    suspend fun contributeToGoal(goalId: String, amount: Double)

    @androidx.room.Update
    suspend fun updateGoal(goal: com.example.data.local.entity.FinancialGoalEntity)

    @Query("UPDATE financial_goals SET isActive = :isActive WHERE id = :goalId")
    suspend fun setGoalActive(goalId: String, isActive: Boolean)

    @Query("DELETE FROM financial_goals WHERE id = :goalId")
    suspend fun deleteGoalById(goalId: String)

    /**
     * ACID Atomic Transaction:
     * 1. Inserts the Goal record.
     * 2. If virtual_pocket_enabled is TRUE and savedAmount > 0, freezes the liquidity from available_balance
     *    in the target account into hold_balance, preserving actual total balance intact.
     */
    @Transaction
    suspend fun executeGoalCreationWithVirtualPocket(
        goal: com.example.data.local.entity.FinancialGoalEntity,
        freezeAmount: Double
    ) {
        insertGoal(goal)
        if (goal.virtualPocketEnabled && freezeAmount > 0) {
            check(freezeAccountVirtualPocket(goal.targetAccountId, freezeAmount) == 1) {
                "La cuenta objetivo de la meta no existe."
            }
        }
    }

    // ==========================================
    // BUDGETS & BUDGET ITEMS (SOBRES) DAO OPERATIONS
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: com.example.data.local.entity.BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetItems(items: List<com.example.data.local.entity.BudgetItemEntity>)

    @Query("SELECT * FROM budgets WHERE (userId = :userId OR userId = 'user_default') AND isActive = 1 ORDER BY createdAt DESC")
    fun getBudgetsFlow(userId: String): Flow<List<com.example.data.local.entity.BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE id = :budgetId LIMIT 1")
    suspend fun getBudgetById(budgetId: String): com.example.data.local.entity.BudgetEntity?

    @Transaction
    @Query("SELECT * FROM budgets WHERE id = :budgetId LIMIT 1")
    fun getBudgetWithItemsFlow(budgetId: String): Flow<com.example.data.local.entity.BudgetWithItems?>

    @Transaction
    @Query("SELECT * FROM budgets WHERE (userId = :userId OR userId = 'user_default') AND isActive = 1 ORDER BY createdAt DESC")
    fun getAllBudgetsWithItemsFlow(userId: String): Flow<List<com.example.data.local.entity.BudgetWithItems>>

    @Transaction
    @Query("SELECT * FROM budgets WHERE (userId = :userId OR userId = 'user_default') AND isActive = 1 ORDER BY createdAt DESC")
    suspend fun getAllBudgetsWithItems(userId: String): List<com.example.data.local.entity.BudgetWithItems>

    @Query("DELETE FROM budgets WHERE id = :budgetId")
    suspend fun deleteBudgetById(budgetId: String)

    @Query("DELETE FROM budget_items WHERE budgetId = :budgetId")
    suspend fun deleteBudgetItemsByBudgetId(budgetId: String)

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE (userId = :userId OR userId = 'user_default') AND type = 'EXPENSE' AND (categoryId = :categoryId OR (categoryName IS NOT NULL AND categoryName != '' AND LOWER(categoryName) = LOWER(:categoryName))) AND substr(date, 1, 10) >= :startDate AND substr(date, 1, 10) <= :endDate")
    suspend fun calculateSpentForCategoryInPeriod(
        userId: String,
        categoryId: String,
        categoryName: String,
        startDate: String,
        endDate: String
    ): Double

    @Query("UPDATE budget_items SET spentAmount = spentAmount + :amount WHERE (categoryId = :categoryId OR (categoryName IS NOT NULL AND categoryName != '' AND LOWER(categoryName) = LOWER(:categoryName))) AND budgetId IN (SELECT id FROM budgets WHERE (userId = :userId OR userId = 'user_default') AND isActive = 1 AND substr(:expenseDate, 1, 10) >= startDate AND substr(:expenseDate, 1, 10) <= endDate)")
    suspend fun updateBudgetItemsSpentForExpense(
        userId: String,
        categoryId: String,
        categoryName: String,
        expenseDate: String,
        amount: Double
    )

    @Query("SELECT * FROM budget_items WHERE budgetId = :budgetId")
    suspend fun getBudgetItems(budgetId: String): List<com.example.data.local.entity.BudgetItemEntity>

    @Query("UPDATE budget_items SET allocatedAmount = :newAmount WHERE id = :itemId")
    suspend fun updateBudgetItemAllocated(itemId: String, newAmount: Double)

    @Query("UPDATE budget_items SET allocatedAmount = allocatedAmount - :amount WHERE id = :sourceId")
    suspend fun deductBudgetItemAllocated(sourceId: String, amount: Double)

    @Query("UPDATE budget_items SET allocatedAmount = allocatedAmount + :amount WHERE id = :targetId")
    suspend fun addBudgetItemAllocated(targetId: String, amount: Double)

    @Transaction
    suspend fun rebalanceEnvelopes(sourceId: String, targetId: String, amount: Double) {
        deductBudgetItemAllocated(sourceId, amount)
        addBudgetItemAllocated(targetId, amount)
    }

    @Query("UPDATE budgets SET totalAmount = :newTotal WHERE id = :budgetId")
    suspend fun updateBudgetTotalAmount(budgetId: String, newTotal: Double)

    /**
     * ACID Atomic Transaction:
     * 1. Inserts the Budget header.
     * 2. Inserts all category envelopes (budget items).
     */
    @Transaction
    suspend fun executeBudgetCreation(
        budget: com.example.data.local.entity.BudgetEntity,
        items: List<com.example.data.local.entity.BudgetItemEntity>
    ) {
        insertBudget(budget)
        if (items.isNotEmpty()) {
            insertBudgetItems(items)
        }
    }

    // ==========================================
    // INVESTMENTS DAO OPERATIONS
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestment(investment: com.example.data.local.entity.InvestmentEntity)

    @Query("SELECT * FROM investments WHERE userId = :userId ORDER BY isFavorite DESC, createdAt DESC")
    fun getInvestmentsFlow(userId: String): Flow<List<com.example.data.local.entity.InvestmentEntity>>

    @Query("SELECT * FROM investments WHERE id = :id LIMIT 1")
    suspend fun getInvestmentById(id: String): com.example.data.local.entity.InvestmentEntity?

    @Query("UPDATE investments SET currentValue = :newVal, valueUpdateDate = :updateDate WHERE id = :id")
    suspend fun updateInvestmentCurrentValue(id: String, newVal: Double, updateDate: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestmentDocument(doc: com.example.data.local.entity.InvestmentDocumentEntity)

    @Query("SELECT * FROM investment_documents WHERE investmentId = :investmentId ORDER BY createdAt DESC")
    fun getInvestmentDocumentsFlow(investmentId: String): Flow<List<com.example.data.local.entity.InvestmentDocumentEntity>>

    @Query("DELETE FROM investments WHERE id = :id")
    suspend fun deleteInvestmentById(id: String)

    /**
     * ACID Atomic Transaction (Partida Doble):
     * 1. Inserts the new Investment record.
     * 2. Deducts the initial amount from the origin account balance & availableBalance.
     * 3. Inserts the expense transaction in the ledger.
     * 4. Persists the attached document if provided.
     */
    @Transaction
    suspend fun executeInvestmentCreation(
        investment: com.example.data.local.entity.InvestmentEntity,
        originTx: TransactionEntity,
        document: com.example.data.local.entity.InvestmentDocumentEntity? = null
    ) {
        insertInvestment(investment)
        check(deductAccountBalance(
            accountId = investment.originAccountId,
            amount = investment.initialAmount
        ) == 1) { "La cuenta origen de la inversión no existe." }
        insertTransaction(originTx)
        if (document != null) {
            insertInvestmentDocument(document)
        }
    }

    // ==========================================
    // TAGS DAO OPERATIONS
    // ==========================================
    @Query("SELECT * FROM tags ORDER BY usageCount DESC, name ASC")
    fun getTagsFlow(): Flow<List<com.example.data.local.entity.TagEntity>>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun findTagByName(name: String): com.example.data.local.entity.TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: com.example.data.local.entity.TagEntity)

    // ==========================================
    // SCHEDULED PAYMENTS DAO OPERATIONS
    // ==========================================
    @Query("SELECT * FROM scheduled_payments WHERE (user_id = :userId OR user_id = 'user_default' OR :userId = '') ORDER BY is_paid ASC, due_date ASC")
    fun getScheduledPaymentsFlow(userId: String): Flow<List<com.example.data.local.entity.ScheduledPaymentEntity>>

    @Query("SELECT * FROM scheduled_payments WHERE (user_id = :userId OR user_id = 'user_default' OR :userId = '') AND is_paid = 0 ORDER BY due_date ASC")
    suspend fun getPendingScheduledPayments(userId: String): List<com.example.data.local.entity.ScheduledPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledPayment(payment: com.example.data.local.entity.ScheduledPaymentEntity)

    @Update
    suspend fun updateScheduledPayment(payment: com.example.data.local.entity.ScheduledPaymentEntity)

    @Query("DELETE FROM scheduled_payments WHERE id = :id")
    suspend fun deleteScheduledPaymentById(id: String)

    @Query("UPDATE scheduled_payments SET is_paid = :isPaid, paid_date = :paidDate WHERE id = :id")
    suspend fun setScheduledPaymentPaid(id: String, isPaid: Boolean, paidDate: String?)
}


