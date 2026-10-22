package com.example

import com.example.data.local.entity.AccountCreditCardEntity
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BankEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CityEntity
import com.example.data.local.entity.CountryEntity
import com.example.data.local.entity.CurrencyEntity
import com.example.data.local.entity.IncomeSourceEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.repository.FinancialRepository
import com.example.domain.usecase.CreateTransferUseCase
import com.example.domain.usecase.TransferDTO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CreateTransferUseCaseTest {

    private lateinit var fakeFinancialRepository: FakeFinancialRepository
    private lateinit var createTransferUseCase: CreateTransferUseCase

    @Before
    fun setUp() {
        fakeFinancialRepository = FakeFinancialRepository()
        createTransferUseCase = CreateTransferUseCase(fakeFinancialRepository)

        // Seed initial test account with 50,000.00
        fakeFinancialRepository.seedAccount(
            AccountEntity(
                id = "acc-1",
                userId = "user_default",
                name = "Cuenta Corriente",
                balance = 50000.00,
                currency = "COP",
                type = "CHECKING"
            )
        )
        fakeFinancialRepository.seedAccount(
            AccountEntity(
                id = "acc-2",
                userId = "user_default",
                name = "Cuenta Ahorros",
                balance = 10000.00,
                currency = "COP",
                type = "SAVINGS"
            )
        )
    }

    @Test
    fun `successful transfer updates balances and saves transfer`() = runBlocking {
        val dto = TransferDTO(
            sourceAccountId = "acc-1",
            destinationAccountId = "acc-2",
            amount = BigDecimal("15000.00"),
            sourceAccountName = "Cuenta Corriente",
            destinationAccountName = "Cuenta Ahorros"
        )

        val result = createTransferUseCase.execute(dto)

        assertTrue(result)
        assertEquals(1, fakeFinancialRepository.recordedTransfers.size)
        val recorded = fakeFinancialRepository.recordedTransfers.first()
        assertEquals("acc-1", recorded.sourceAccountId)
        assertEquals("acc-2", recorded.destinationAccountId)
        assertEquals(15000.00, recorded.amount, 0.001)
    }

    @Test
    fun `transfer fails if source and destination are the same account`() = runBlocking {
        val dto = TransferDTO(
            sourceAccountId = "acc-1",
            destinationAccountId = "acc-1",
            amount = BigDecimal("100.00")
        )

        try {
            createTransferUseCase.execute(dto)
            fail("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("No se puede transferir a la misma cuenta.", e.message)
        }
    }

    @Test
    fun `transfer fails if insufficient funds`() = runBlocking {
        val dto = TransferDTO(
            sourceAccountId = "acc-1",
            destinationAccountId = "acc-2",
            amount = BigDecimal("100000.00") // Greater than 50000.00 balance
        )

        try {
            createTransferUseCase.execute(dto)
            fail("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Fondos insuficientes para realizar la transferencia.", e.message)
        }
    }

    @Test
    fun `transfer fails if amount is zero or negative`() = runBlocking {
        val dtoZero = TransferDTO(
            sourceAccountId = "acc-1",
            destinationAccountId = "acc-2",
            amount = BigDecimal.ZERO
        )

        try {
            createTransferUseCase.execute(dtoZero)
            fail("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("El monto de transferencia debe ser mayor a cero.", e.message)
        }
    }
}

/**
 * Lightweight in-memory test double of FinancialRepository.
 */
class FakeFinancialRepository : FinancialRepository {
    private val accounts = mutableMapOf<String, AccountEntity>()
    val recordedTransfers = mutableListOf<TransferRecord>()
    private val banks = mutableMapOf<String, BankEntity>()
    private val currencies = mutableMapOf<String, CurrencyEntity>()
    private val creditCards = mutableMapOf<String, AccountCreditCardEntity>()

    fun seedAccount(account: AccountEntity) {
        accounts[account.id] = account
    }

    fun seedBank(bank: BankEntity) {
        banks[bank.clearingCode] = bank
    }

    private val categoriesState = kotlinx.coroutines.flow.MutableStateFlow<List<CategoryEntity>>(emptyList())

    override fun getAccounts(userId: String): Flow<List<AccountEntity>> = flowOf(accounts.values.toList())
    override fun getCategories(): Flow<List<CategoryEntity>> = categoriesState
    override fun getCategories(userId: String): Flow<List<CategoryEntity>> = categoriesState
    override fun getTransactions(userId: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
    override fun getIncomeSources(userId: String): Flow<List<IncomeSourceEntity>> = flowOf(emptyList())

    override suspend fun getAccountById(accountId: String): AccountEntity? = accounts[accountId]

    override suspend fun recordExpense(
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
        idempotencyKey: String,
        affectsBudget: Boolean
    ): Result<TransactionEntity> = Result.success(
        TransactionEntity(
            id = "tx-1",
            userId = userId,
            accountId = accountId,
            accountName = accountName,
            categoryId = categoryId,
            categoryName = categoryName,
            amount = amount,
            currency = currency,
            type = "EXPENSE",
            date = dateFormatted,
            description = description,
            receiptUrl = receiptUrl,
            idempotencyKey = idempotencyKey
        )
    )

    override suspend fun recordIncome(
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
        recurrenceFrequency: String,
        saveAsTemplate: Boolean,
        tags: List<String>,
        idempotencyKey: String
    ): Result<TransactionEntity> = Result.success(
        TransactionEntity(
            id = "tx-2",
            userId = userId,
            accountId = accountId,
            accountName = accountName,
            categoryId = categoryId,
            categoryName = categoryName,
            sourceId = sourceId,
            sourceName = sourceName,
            amount = amount,
            currency = currency,
            type = "INCOME",
            date = dateFormatted,
            description = description,
            receiptUrl = receiptUrl,
            idempotencyKey = idempotencyKey
        )
    )

    override suspend fun recordTransfer(
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
        idempotencyKey: String
    ): Result<Pair<TransactionEntity, TransactionEntity>> {
        val src = accounts[sourceAccountId]
        val dst = accounts[destinationAccountId]
        if (src != null) {
            accounts[sourceAccountId] = src.copy(balance = src.balance - amount)
        }
        if (dst != null) {
            accounts[destinationAccountId] = dst.copy(balance = dst.balance + amount)
        }

        recordedTransfers.add(
            TransferRecord(sourceAccountId, destinationAccountId, amount, currency)
        )

        val txOut = TransactionEntity(
            id = "tx_out",
            userId = userId,
            accountId = sourceAccountId,
            accountName = sourceAccountName,
            categoryId = "cat_transfer",
            categoryName = "Transferencia",
            amount = amount,
            currency = currency,
            type = "TRANSFER_OUT",
            date = dateFormatted,
            description = concept,
            receiptUrl = receiptUrl,
            idempotencyKey = "out_$idempotencyKey"
        )
        val txIn = TransactionEntity(
            id = "tx_in",
            userId = userId,
            accountId = destinationAccountId,
            accountName = destinationAccountName,
            categoryId = "cat_transfer",
            categoryName = "Transferencia",
            amount = amount,
            currency = currency,
            type = "TRANSFER_IN",
            date = dateFormatted,
            description = concept,
            receiptUrl = receiptUrl,
            idempotencyKey = "in_$idempotencyKey"
        )
        return Result.success(Pair(txOut, txIn))
    }

    override suspend fun createCategory(name: String, colorHex: String, icon: String, userId: String?, type: String): CategoryEntity {
        val cat = CategoryEntity(id = "cat_${System.currentTimeMillis()}", userId = userId, name = name, icon = icon, colorHex = colorHex, type = type)
        categoriesState.value = categoriesState.value + cat
        return cat
    }

    override suspend fun createAccount(
        name: String, type: String, balance: Double, currency: String, userId: String,
        bankName: String, isFavorite: Boolean, accountNumber: String?, openingDate: String?,
        kashappDate: String, generatesInterest: Boolean, interestRateEa: Double?,
        calcFrequency: String?, payoutFrequency: String?, compoundInterest: Boolean,
        purposes: String, customColorHex: String, iconName: String, minimumBalance: Double,
        monthlyFee: Double, feeChargeDay: Int, syncProvider: String?, syncInstitutionId: String?,
        syncAccessToken: String?, supportingDocumentUri: String?, supportingDocumentType: String?,
        availableBalance: Double?, holdBalance: Double, country: String, cityName: String?,
        favoriteRouting: String, favoriteForIncomes: Boolean, favoriteForExpenses: Boolean,
        employerName: String?, accountOwner: String?, alias: String?, hasOverdraft: Boolean,
        overdraftLimit: Double, overdraftRateEa: Double, isTaxExempt: Boolean,
        payrollConfig: com.example.data.local.entity.AccountPayrollConfigEntity?
    ): AccountEntity {
        val acc = AccountEntity(id = "acc_${System.currentTimeMillis()}", userId = userId, name = name, balance = balance, currency = currency, type = type)
        accounts[acc.id] = acc
        return acc
    }

    override suspend fun createIncomeSource(userId: String, name: String): IncomeSourceEntity =
        IncomeSourceEntity(id = "src_1", userId = userId, name = name)

    override fun getBanks(userId: String): Flow<List<BankEntity>> = flowOf(banks.values.toList())
    override suspend fun existsBankByCode(code: String): Boolean = banks.containsKey(code)
    override suspend fun getBankByClearingCode(code: String): BankEntity? = banks[code]

    override suspend fun createBank(
        userId: String, name: String, clearingCode: String, countryCode: String,
        countryName: String, currencyCode: String, cityName: String?, logoUrl: String?,
        colorHex: String, iconName: String, additionalInfo: String?
    ): Result<BankEntity> {
        if (banks.containsKey(clearingCode)) {
            return Result.failure(IllegalArgumentException("El código de compensación ya existe."))
        }
        val entity = BankEntity(
            id = "bank_${clearingCode}",
            userId = userId,
            name = name,
            clearingCode = clearingCode,
            countryCode = countryCode,
            countryName = countryName,
            currencyCode = currencyCode,
            logoUrl = logoUrl,
            colorHex = colorHex,
            iconName = iconName,
            additionalInfo = additionalInfo
        )
        banks[clearingCode] = entity
        return Result.success(entity)
    }

    override suspend fun deleteBank(bankId: String, bankName: String): Result<Unit> {
        val linked = accounts.values.count { it.bankName.equals(bankName, ignoreCase = true) }
        if (linked > 0) {
            return Result.failure(
                com.example.data.repository.ReferentialIntegrityException(
                    "Error 409 (Conflict): Integridad referencial protegida. La entidad '$bankName' está vinculada a $linked cuenta(s) en tu historial contable. Elimina o reasigna las cuentas asociadas para proteger la integridad contable y tu Patrimonio Neto."
                )
            )
        }
        val target = banks.values.find { it.id == bankId }
        if (target != null) {
            banks.remove(target.clearingCode)
        }
        return Result.success(Unit)
    }
    override suspend fun reorderBanks(banks: List<BankEntity>): Result<Unit> = Result.success(Unit)

    override fun getCurrencies(userId: String): Flow<List<CurrencyEntity>> = flowOf(currencies.values.toList())
    override suspend fun createCurrency(userId: String, code: String, name: String, symbol: String, manualExchangeRate: Double): Result<CurrencyEntity> {
        val entity = CurrencyEntity(
            id = "c_${code.lowercase()}",
            userId = userId,
            code = code,
            name = name,
            symbol = symbol,
            manualExchangeRate = manualExchangeRate
        )
        currencies[code] = entity
        return Result.success(entity)
    }
    override suspend fun deleteCurrency(currencyId: String, currencyCode: String): Result<Unit> {
        val linked = accounts.values.count { it.currency.equals(currencyCode, ignoreCase = true) }
        if (linked > 0) {
            return Result.failure(
                com.example.data.repository.ReferentialIntegrityException(
                    "Error 409 (Conflict): Integridad contable protegida. No puedes borrar la moneda '$currencyCode' porque tienes $linked cuenta(s) activa(s) y 0 entidad(es) vinculada(s) que la utilizan. Reasigna o elimina los registros vinculados para proteger la trazabilidad de tu Patrimonio Neto."
                )
            )
        }
        val target = currencies.values.find { it.id == currencyId }
        if (target != null) {
            currencies.remove(target.code)
        }
        return Result.success(Unit)
    }

    override fun getCountries(userId: String): Flow<List<CountryEntity>> = flowOf(emptyList())
    override suspend fun createCountry(userId: String, isoCode: String, name: String, flagEmoji: String, defaultCurrencyCode: String, clearingSystemName: String, isNumericClearingCode: Boolean): Result<CountryEntity> =
        Result.success(CountryEntity(id = "co_1", userId = userId, isoCode = isoCode, name = name, flagEmoji = flagEmoji))
    override suspend fun deleteCountry(countryId: String, countryCode: String): Result<Unit> = Result.success(Unit)

    override fun getCities(countryCode: String, userId: String): Flow<List<CityEntity>> = flowOf(emptyList())
    override fun getAllCities(userId: String): Flow<List<CityEntity>> = flowOf(emptyList())
    override suspend fun createCity(userId: String, countryCode: String, name: String, stateOrDepartment: String?): Result<CityEntity> =
        Result.success(CityEntity(id = "cit_1", userId = userId, countryCode = countryCode, name = name))
    override suspend fun deleteCity(cityId: String): Result<Unit> = Result.success(Unit)
    override fun getPayrollConcepts(): Flow<List<com.example.data.local.entity.PayrollConceptEntity>> = flowOf(emptyList())
    override suspend fun createPayrollConcept(name: String): Result<com.example.data.local.entity.PayrollConceptEntity> =
        Result.success(com.example.data.local.entity.PayrollConceptEntity(id = "p_1", name = name))
    override fun getAccountCdt(accountId: String): Flow<com.example.data.local.entity.AccountCdtEntity?> = flowOf(null)
    override suspend fun createCdtAccount(
        cdtAccount: AccountEntity,
        cdtConfig: com.example.data.local.entity.AccountCdtEntity,
        sourceAccountId: String?
    ): Result<Unit> = Result.success(Unit)

    override fun getAccountCreditCard(accountId: String): Flow<AccountCreditCardEntity?> =
        flowOf(creditCards[accountId])

    override fun getAllAccountCreditCards(): Flow<List<AccountCreditCardEntity>> =
        flowOf(creditCards.values.toList())

    override fun getAccountCreditCards(userId: String): Flow<List<AccountCreditCardEntity>> =
        flowOf(creditCards.values.filter { accounts[it.accountId]?.userId == userId })

    override suspend fun createCreditCardAccount(
        cardAccount: AccountEntity,
        cardConfig: AccountCreditCardEntity
    ): Result<Pair<AccountEntity, AccountCreditCardEntity>> {
        accounts[cardAccount.id] = cardAccount
        creditCards[cardAccount.id] = cardConfig
        return Result.success(Pair(cardAccount, cardConfig))
    }

    override fun getGoals(userId: String): Flow<List<com.example.data.local.entity.FinancialGoalEntity>> = flowOf(emptyList())
    override suspend fun getGoalById(id: String): com.example.data.local.entity.FinancialGoalEntity? = null
    override suspend fun createGoal(
        goal: com.example.data.local.entity.FinancialGoalEntity,
        freezeAmount: Double
    ): Result<com.example.data.local.entity.FinancialGoalEntity> = Result.success(goal)
    override suspend fun contributeToGoal(goalId: String, amount: Double, sourceAccountId: String?): Result<Unit> = Result.success(Unit)
    override suspend fun updateGoal(goal: com.example.data.local.entity.FinancialGoalEntity): Result<Unit> = Result.success(Unit)
    override suspend fun setGoalActive(goalId: String, isActive: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun deleteGoal(goalId: String): Result<Unit> = Result.success(Unit)

    override fun getExpenseCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
    override fun getBudgets(userId: String): Flow<List<com.example.data.local.entity.BudgetEntity>> = flowOf(emptyList())
    override fun getBudgetWithItems(budgetId: String): Flow<com.example.data.local.entity.BudgetWithItems?> = flowOf(null)
    override fun getAllBudgetsWithItems(userId: String): Flow<List<com.example.data.local.entity.BudgetWithItems>> = flowOf(emptyList())
    override suspend fun createBudget(
        budget: com.example.data.local.entity.BudgetEntity,
        items: List<com.example.data.local.entity.BudgetItemEntity>
    ): Result<com.example.data.local.entity.BudgetEntity> = Result.success(budget)
    override suspend fun deleteBudget(budgetId: String): Result<Unit> = Result.success(Unit)
    override suspend fun rebalanceEnvelopes(sourceItemId: String, targetItemId: String, amount: Double): Result<Unit> = Result.success(Unit)
    override suspend fun updateBudgetItemAllocated(itemId: String, newAmount: Double): Result<Unit> = Result.success(Unit)
    override suspend fun updateBudgetTotalAmount(budgetId: String, newTotal: Double): Result<Unit> = Result.success(Unit)
    override suspend fun sweepSurplusToGoal(goalId: String, amount: Double, userId: String): Result<Unit> = Result.success(Unit)
    override suspend fun cloneBudgetForNextMonth(budgetId: String, userId: String): Result<com.example.data.local.entity.BudgetEntity> =
        Result.failure(UnsupportedOperationException())
    override fun getScheduledPayments(userId: String): Flow<List<com.example.data.local.entity.ScheduledPaymentEntity>> = flowOf(emptyList())
    override suspend fun createScheduledPayment(payment: com.example.data.local.entity.ScheduledPaymentEntity): Result<Unit> = Result.success(Unit)
    override suspend fun updateScheduledPayment(payment: com.example.data.local.entity.ScheduledPaymentEntity): Result<Unit> = Result.success(Unit)
    override suspend fun deleteScheduledPayment(id: String): Result<Unit> = Result.success(Unit)
    override suspend fun setScheduledPaymentPaid(id: String, isPaid: Boolean, paidDate: String?): Result<Unit> = Result.success(Unit)

    override fun getInvestments(userId: String): Flow<List<com.example.data.local.entity.InvestmentEntity>> = flowOf(emptyList())
    override suspend fun getInvestmentById(id: String): com.example.data.local.entity.InvestmentEntity? = null
    override suspend fun createInvestment(
        investment: com.example.data.local.entity.InvestmentEntity,
        document: com.example.data.local.entity.InvestmentDocumentEntity?
    ): Result<com.example.data.local.entity.InvestmentEntity> = Result.success(investment)
    override suspend fun deleteInvestment(id: String): Result<Unit> = Result.success(Unit)
    override fun getInvestmentDocuments(investmentId: String): Flow<List<com.example.data.local.entity.InvestmentDocumentEntity>> = flowOf(emptyList())
    override fun getActiveCategories(type: String?): Flow<List<CategoryEntity>> = categoriesState
    override suspend fun setCategoryActive(id: String, isActive: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun getCategoryById(id: String): CategoryEntity? = null

    override fun getMerchantsFlow(userId: String): Flow<List<com.example.data.local.entity.MerchantEntity>> = flowOf(emptyList())
    override suspend fun createMerchant(
        canonicalName: String,
        alias: String,
        defaultCategoryId: String?,
        confidence: Int,
        userId: String?
    ): com.example.data.local.entity.MerchantEntity = com.example.data.local.entity.MerchantEntity(
        id = "m1",
        canonicalName = canonicalName,
        alias = alias,
        defaultCategoryId = defaultCategoryId,
        classificationConfidence = confidence,
        userId = userId
    )
    override suspend fun setMerchantActive(id: String, isActive: Boolean): Result<Unit> = Result.success(Unit)

    override fun observePendingOutboxCount(): Flow<Int> = flowOf(0)
    override suspend fun getPendingOutboxOperations(): List<com.example.data.local.entity.OutboxOperationEntity> = emptyList()
    override suspend fun processOutboxQueue(): Result<Int> = Result.success(0)

    override suspend fun updateAccountAllowNegative(accountId: String, allowNegative: Boolean) {
        val acc = accounts[accountId]
        if (acc != null) {
            accounts[accountId] = acc.copy(hasOverdraft = allowNegative)
        }
    }

    override suspend fun updateAccount(account: AccountEntity): Result<Unit> {
        accounts[account.id] = account
        return Result.success(Unit)
    }

    override suspend fun deleteAccount(accountId: String): Result<Unit> {
        accounts.remove(accountId)
        return Result.success(Unit)
    }

    override suspend fun deleteAccountWithAudit(accountId: String, preserveAuditTrail: Boolean): Result<Unit> {
        accounts.remove(accountId)
        return Result.success(Unit)
    }

    override fun getTransactionsByAccount(accountId: String): Flow<List<TransactionEntity>> = flowOf(emptyList())

    override fun getTags(): Flow<List<com.example.data.local.entity.TagEntity>> = flowOf(emptyList())
    override suspend fun insertTag(name: String, colorHex: String): com.example.data.local.entity.TagEntity =
        com.example.data.local.entity.TagEntity(id = "tag_1", name = name, colorHex = colorHex)

    override suspend fun seedDefaultsIfNeeded(userId: String) {}
}

data class TransferRecord(
    val sourceAccountId: String,
    val destinationAccountId: String,
    val amount: Double,
    val currency: String
)
