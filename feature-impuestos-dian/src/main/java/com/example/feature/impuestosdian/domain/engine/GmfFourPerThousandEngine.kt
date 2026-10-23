package com.example.feature.impuestosdian.domain.engine

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.feature.impuestosdian.domain.model.AccountGmfMonthlyStatus
import com.example.feature.impuestosdian.domain.model.ColombianUvtCatalog
import com.example.feature.impuestosdian.domain.model.TaxAlertLevel
import com.example.feature.impuestosdian.domain.model.TaxJurisdiction
import kotlin.math.max
import kotlin.math.min

class GmfFourPerThousandEngine {

    private val lowAmountWalletBrands = listOf(
        "nequi", "daviplata", "dale", "movii", "powwi", "rappipay", "tpaga", "uala"
    )

    /**
     * Calcula cuenta por cuenta el consumo del tope mensual exento del 4x1000 (350 UVT o 65 UVT)
     * o del impuesto a las transacciones financieras del país activo en el mes [yearMonth] (formato "YYYY-MM").
     */
    fun evaluateMonthlyGmf(
        yearMonth: String,
        uvtValueCop: Double,
        accounts: List<AccountEntity>,
        monthlyTransactions: List<TransactionEntity>,
        jurisdiction: TaxJurisdiction = TaxJurisdiction.CO_DIAN,
        customTaxRate: Double? = null,
        enforceSingleExemptAccount: Boolean = true
    ): List<AccountGmfMonthlyStatus> {
        val safeUvt = if (uvtValueCop > 0.0) uvtValueCop else jurisdiction.defaultTaxUnitValue
        val effectiveTaxRate = customTaxRate ?: jurisdiction.financialTransactionTaxRate

        val gmfSubjectAccounts = accounts.filter { account ->
            !account.type.equals("CASH", ignoreCase = true) &&
                !account.type.equals("CREDIT_CARD", ignoreCase = true) &&
                !account.type.equals("CDT", ignoreCase = true)
        }

        // Modo Bolsa Unificada de 350 UVT (Art. 65 Ley 2277/2022) cuando enforceSingleExemptAccount == false en Colombia
        var shared350UvtRemainingCop = ColombianUvtCatalog.GMF_EXEMPT_BANK_ACCOUNT_MONTHLY_UVT * safeUvt

        val accountsWithOutflows = gmfSubjectAccounts.map { account ->
            val outflowsCop = monthlyTransactions
                .filter { tx ->
                    tx.accountId == account.id &&
                        tx.date.startsWith(yearMonth) &&
                        !tx.categoryId.equals("cat_initial_balance", ignoreCase = true) &&
                        !tx.categoryId.equals("cat_opening_balance", ignoreCase = true) &&
                        (tx.type.equals("EXPENSE", ignoreCase = true) ||
                            tx.type.equals("TRANSFER", ignoreCase = true) ||
                            tx.type.equals("TRANSFER_OUT", ignoreCase = true))
                }
                .sumOf { max(0.0, it.amount) }
            account to outflowsCop
        }.sortedByDescending { it.second }

        return accountsWithOutflows.map { (account, outflowsCop) ->
            val isWallet = isLowAmountWallet(account)
            val usesUnifiedPool = jurisdiction == TaxJurisdiction.CO_DIAN && !enforceSingleExemptAccount && !isWallet

            val exemptLimitUvt: Double
            val exemptLimitCop: Double
            val remainingExemptCop: Double
            val taxableExcessCop: Double

            if (usesUnifiedPool) {
                exemptLimitUvt = ColombianUvtCatalog.GMF_EXEMPT_BANK_ACCOUNT_MONTHLY_UVT
                val coveredBySharedPool = min(outflowsCop, shared350UvtRemainingCop)
                shared350UvtRemainingCop = max(0.0, shared350UvtRemainingCop - coveredBySharedPool)
                exemptLimitCop = coveredBySharedPool + shared350UvtRemainingCop
                remainingExemptCop = shared350UvtRemainingCop
                taxableExcessCop = if (effectiveTaxRate <= 0.0) 0.0 else max(0.0, outflowsCop - coveredBySharedPool)
            } else {
                exemptLimitUvt = when {
                    jurisdiction == TaxJurisdiction.CO_DIAN -> when {
                        account.isTaxExempt -> ColombianUvtCatalog.GMF_EXEMPT_BANK_ACCOUNT_MONTHLY_UVT
                        isWallet -> ColombianUvtCatalog.GMF_EXEMPT_LOW_AMOUNT_WALLET_MONTHLY_UVT
                        else -> 0.0
                    }
                    account.isTaxExempt -> Double.MAX_VALUE / safeUvt.coerceAtLeast(1.0)
                    else -> 0.0
                }
                exemptLimitCop = if (jurisdiction != TaxJurisdiction.CO_DIAN && account.isTaxExempt) {
                    outflowsCop
                } else {
                    exemptLimitUvt * safeUvt
                }
                remainingExemptCop = max(0.0, exemptLimitCop - outflowsCop)
                taxableExcessCop = if (effectiveTaxRate <= 0.0 || (jurisdiction != TaxJurisdiction.CO_DIAN && account.isTaxExempt)) {
                    0.0
                } else {
                    max(0.0, outflowsCop - exemptLimitCop)
                }
            }

            val estimatedGmfTaxPaidCop = taxableExcessCop * effectiveTaxRate

            val alertLevel = when {
                effectiveTaxRate <= 0.0 -> TaxAlertLevel.SAFE
                !usesUnifiedPool && exemptLimitCop == 0.0 && outflowsCop > 0.0 -> TaxAlertLevel.EXCEEDED
                taxableExcessCop > 0.0 -> TaxAlertLevel.EXCEEDED
                exemptLimitCop > 0.0 && (outflowsCop / exemptLimitCop) >= 0.75 -> TaxAlertLevel.WARNING
                else -> TaxAlertLevel.SAFE
            }

            AccountGmfMonthlyStatus(
                accountId = account.id,
                accountName = account.name,
                bankName = account.bankName,
                accountType = account.type,
                isMarkedTaxExempt350Uvt = account.isTaxExempt || usesUnifiedPool,
                isLowAmountDigitalWallet65Uvt = isWallet && !account.isTaxExempt,
                monthlyOutflowsCop = outflowsCop,
                monthlyOutflowsUvt = if (safeUvt > 0.0) outflowsCop / safeUvt else 0.0,
                exemptLimitUvt = if (jurisdiction != TaxJurisdiction.CO_DIAN && account.isTaxExempt) 0.0 else exemptLimitUvt,
                exemptLimitCop = exemptLimitCop,
                remainingExemptCop = remainingExemptCop,
                taxableExcessCop = taxableExcessCop,
                estimatedGmfTaxPaidCop = estimatedGmfTaxPaidCop,
                gmfAlertLevel = alertLevel,
                sharesUnified350UvtPool = usesUnifiedPool
            )
        }
    }

    /**
     * Calcula el 4x1000 / impuesto bancario real acumulado en los 12 meses del año gravable [fiscalYear]
     * para alimentar con exactitud el 50% deducible en Renta (Art. 115 E.T.).
     */
    fun calculateAnnualGmfPaid(
        fiscalYear: Int,
        uvtValueCop: Double,
        accounts: List<AccountEntity>,
        transactionsInYear: List<TransactionEntity>,
        jurisdiction: TaxJurisdiction = TaxJurisdiction.CO_DIAN,
        customTaxRate: Double? = null,
        enforceSingleExemptAccount: Boolean = true
    ): Double {
        return (1..12).sumOf { month ->
            val ym = "%04d-%02d".format(fiscalYear, month)
            evaluateMonthlyGmf(
                yearMonth = ym,
                uvtValueCop = uvtValueCop,
                accounts = accounts,
                monthlyTransactions = transactionsInYear,
                jurisdiction = jurisdiction,
                customTaxRate = customTaxRate,
                enforceSingleExemptAccount = enforceSingleExemptAccount
            ).sumOf { it.estimatedGmfTaxPaidCop }
        }
    }

    fun buildGmfOptimizationAdvice(
        statuses: List<AccountGmfMonthlyStatus>,
        uvtValueCop: Double,
        jurisdiction: TaxJurisdiction = TaxJurisdiction.CO_DIAN
    ): String {
        if (jurisdiction != TaxJurisdiction.CO_DIAN && jurisdiction.financialTransactionTaxRate <= 0.0) {
            return "En ${jurisdiction.countryName} (${jurisdiction.authorityAcronym}) las transferencias y retiros bancarios ordinarios están exentos de gravamen financiero (0%)."
        }

        val has350UvtAccount = statuses.any { it.isMarkedTaxExempt350Uvt }
        val totalGmfPaid = statuses.sumOf { it.estimatedGmfTaxPaidCop }

        if (!has350UvtAccount && statuses.isNotEmpty()) {
            val highestOutflowAccount = statuses.maxByOrNull { it.monthlyOutflowsCop }
            val potentialSavingCop = ((highestOutflowAccount?.monthlyOutflowsCop ?: 0.0)
                .coerceAtMost(ColombianUvtCatalog.GMF_EXEMPT_BANK_ACCOUNT_MONTHLY_UVT * uvtValueCop)) *
                jurisdiction.financialTransactionTaxRate.coerceAtLeast(ColombianUvtCatalog.GMF_TAX_RATE)
            return "No tienes ninguna cuenta marcada como exenta en Creación/Edición de Cuentas. Si marcas '${highestOutflowAccount?.accountName ?: "tu cuenta principal"}' como exenta en tu banco y en KashApp, ahorrarás hasta $${potentialSavingCop.toLong()} ${jurisdiction.defaultCurrency} este mes."
        }

        val exceededWallet = statuses.firstOrNull { it.isLowAmountDigitalWallet65Uvt && it.taxableExcessCop > 0.0 }
        if (exceededWallet != null) {
            return "Tu billetera '${exceededWallet.accountName}' superó el tope mensual de 65 UVT y ya generó $${exceededWallet.estimatedGmfTaxPaidCop.toLong()} COP de 4x1000. Usa tu cuenta exenta de 350 UVT para el resto del mes."
        }

        if (totalGmfPaid > 0.0) {
            return "Este mes has generado un estimado de $${totalGmfPaid.toLong()} ${jurisdiction.defaultCurrency} en gravamen bancario (en Colombia el 50% del 4x1000 certificado es deducible en Renta bajo el Art. 115 del E.T.)."
        }

        return "Excelente control de impuestos bancarios: todas tus salidas del mes están dentro de tus topes exentos ($0 causados)."
    }

    private fun isLowAmountWallet(account: AccountEntity): Boolean {
        if (account.type.equals("WALLET", ignoreCase = true)) return true
        val combined = "${account.name} ${account.bankName}".lowercase()
        return lowAmountWalletBrands.any { combined.contains(it) }
    }
}
