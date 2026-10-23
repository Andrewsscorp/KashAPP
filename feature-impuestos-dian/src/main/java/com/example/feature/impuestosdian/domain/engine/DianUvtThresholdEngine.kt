package com.example.feature.impuestosdian.domain.engine

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.FixedAssetEntity
import com.example.data.local.entity.InvestmentEntity
import com.example.data.local.entity.TransactionEntity
import com.example.feature.impuestosdian.domain.model.ColombianUvtCatalog
import com.example.feature.impuestosdian.domain.model.DianCriterionType
import com.example.feature.impuestosdian.domain.model.DianThresholdStatus
import com.example.feature.impuestosdian.domain.model.ElectronicInvoiceBenefitSummary
import com.example.feature.impuestosdian.domain.model.TaxAlertLevel
import com.example.feature.impuestosdian.domain.model.TaxJurisdiction
import kotlin.math.max
import kotlin.math.min

class DianUvtThresholdEngine {

    private fun isOpeningOrInternalTransfer(tx: TransactionEntity): Boolean {
        val cat = tx.categoryId.lowercase()
        val id = tx.id.lowercase()
        val type = tx.type.uppercase()
        return cat == "cat_initial_balance" ||
            cat == "cat_opening_balance" ||
            id.startsWith("tx_init_") ||
            id.startsWith("tx_cc_init_") ||
            id.startsWith("tx_cdt_init_") ||
            type == "TRANSFER_IN" ||
            type == "TRANSFER_OUT" ||
            !tx.transferGroupId.isNullOrBlank()
    }

    /**
     * Evalúa los 5 topes oficiales de la DIAN (o su equivalente en la jurisdicción seleccionada)
     * para determinar la obligación de presentar Declaración de Renta de Personas Naturales
     * en el año gravable [fiscalYear].
     *
     * Nota normativa: Según el Estatuto Tributario (Arts. 592–594-3), para el Patrimonio Bruto
     * no se descuentan las deudas (eso corresponde al Patrimonio Líquido), por lo que se suman
     * los activos positivos (cuentas con saldo > 0, activos fijos activos e inversiones vigentes).
     */
    fun evaluateAnnualThresholds(
        fiscalYear: Int,
        uvtValueCop: Double,
        accounts: List<AccountEntity>,
        transactionsInYear: List<TransactionEntity>,
        fixedAssets: List<FixedAssetEntity>,
        investments: List<InvestmentEntity>,
        jurisdiction: TaxJurisdiction = TaxJurisdiction.CO_DIAN
    ): List<DianThresholdStatus> {
        val safeUvt = if (uvtValueCop > 0.0) {
            uvtValueCop
        } else if (jurisdiction == TaxJurisdiction.CO_DIAN) {
            ColombianUvtCatalog.getDefaultUvtForYear(fiscalYear)
        } else {
            jurisdiction.defaultTaxUnitValue
        }

        val accountsById = accounts.associateBy { it.id }
        val yearPrefix = "$fiscalYear-"

        val yearTx = transactionsInYear.filter {
            it.date.startsWith(yearPrefix) && !isOpeningOrInternalTransfer(it)
        }

        // 1. PATRIMONIO BRUTO (Tope: 4.500 UVT en Colombia — Art. 592 Num. 1 y Art. 261 E.T.)
        val positiveAccountBalancesCop = accounts
            .filter { !it.type.equals("CREDIT_CARD", ignoreCase = true) && it.balance > 0.0 }
            .sumOf { it.balance }

        val activeAssetStatuses = setOf("ACTIVE", "EN_USO", "DISPONIBLE", "EN_MANTENIMIENTO")
        val activeFixedAssetsCop = fixedAssets
            .filter { it.status.uppercase() in activeAssetStatuses && it.includeInNetWorth && it.deletedAt == null }
            .sumOf { asset ->
                val bookVal = (asset.acquisitionValue - asset.accumulatedDepreciation).coerceAtLeast(asset.residualValue)
                max(0.0, if (asset.currentMarketValue > 0.0) asset.currentMarketValue else bookVal)
            }

        val activeInvestmentsCop = investments
            .filter { it.status.equals("ACTIVE", ignoreCase = true) && it.includeInNetWorth }
            .sumOf { max(0.0, it.currentValue) }

        val grossWealthCop = positiveAccountBalancesCop + activeFixedAssetsCop + activeInvestmentsCop

        // 2. INGRESOS BRUTOS ANUALES (Tope: 1.400 UVT en Colombia)
        val grossIncomeCop = yearTx
            .filter { it.type.equals("INCOME", ignoreCase = true) }
            .sumOf { max(0.0, it.amount) }

        // 3. CONSUMOS CON TARJETA DE CRÉDITO (Tope: 1.400 UVT en Colombia)
        val creditCardExpensesCop = yearTx
            .filter { tx ->
                tx.type.equals("EXPENSE", ignoreCase = true) &&
                    accountsById[tx.accountId]?.type?.equals("CREDIT_CARD", ignoreCase = true) == true
            }
            .sumOf { max(0.0, it.amount) }

        // 4. COMPRAS Y CONSUMOS TOTALES DEL AÑO (Tope: 1.400 UVT en Colombia)
        val totalPurchasesAndExpensesCop = yearTx
            .filter { it.type.equals("EXPENSE", ignoreCase = true) }
            .sumOf { max(0.0, it.amount) }

        // 5. CONSIGNACIONES BANCARIAS, DEPÓSITOS O INVERSIONES FINANCIERAS DEL AÑO (Art. 594-3 Lit. c) E.T.)
        val bankDepositsInYearCop = yearTx
            .filter { tx ->
                val account = accountsById[tx.accountId]
                val isBankOrWallet = account != null &&
                    !account.type.equals("CASH", ignoreCase = true) &&
                    !account.type.equals("CREDIT_CARD", ignoreCase = true)
                isBankOrWallet && tx.type.equals("INCOME", ignoreCase = true)
            }
            .sumOf { max(0.0, it.amount) }

        // Solo suman inversiones constituidas en el año gravable actual que no sean traslados internos ya contados
        val newInvestmentsInFiscalYearCop = investments
            .filter { inv ->
                inv.status.equals("ACTIVE", ignoreCase = true) &&
                    inv.acquisitionDate.startsWith(yearPrefix) &&
                    !inv.tags.contains("INTERNAL_TRANSFER", ignoreCase = true) &&
                    !inv.tags.contains("RENEWAL_SAME_YEAR", ignoreCase = true)
            }
            .sumOf { inv -> max(0.0, inv.initialAmount + inv.contributions) }

        val bankDepositsAndInvestmentsCop = bankDepositsInYearCop + newInvestmentsInFiscalYearCop

        val wealthLimitUnits = if (jurisdiction == TaxJurisdiction.CO_DIAN) {
            ColombianUvtCatalog.THRESHOLD_GROSS_WEALTH_UVT
        } else {
            jurisdiction.annualWealthFilingThresholdUnits
        }
        val incomeLimitUnits = if (jurisdiction == TaxJurisdiction.CO_DIAN) {
            ColombianUvtCatalog.THRESHOLD_GROSS_INCOME_UVT
        } else {
            jurisdiction.annualIncomeFilingThresholdUnits
        }
        val cardLimitUnits = if (jurisdiction == TaxJurisdiction.CO_DIAN) {
            ColombianUvtCatalog.THRESHOLD_CREDIT_CARD_EXPENSES_UVT
        } else {
            jurisdiction.annualIncomeFilingThresholdUnits
        }
        val purchasesLimitUnits = if (jurisdiction == TaxJurisdiction.CO_DIAN) {
            ColombianUvtCatalog.THRESHOLD_TOTAL_PURCHASES_UVT
        } else {
            jurisdiction.annualIncomeFilingThresholdUnits
        }
        val depositsLimitUnits = if (jurisdiction == TaxJurisdiction.CO_DIAN) {
            ColombianUvtCatalog.THRESHOLD_BANK_DEPOSITS_UVT
        } else {
            jurisdiction.annualIncomeFilingThresholdUnits
        }

        val authorityRef = if (jurisdiction == TaxJurisdiction.CO_DIAN) {
            null
        } else {
            "${jurisdiction.authorityAcronym} (${jurisdiction.countryName}) • En ${jurisdiction.taxUnitName}"
        }

        return listOf(
            buildThresholdStatus(
                type = DianCriterionType.PATRIMONIO_BRUTO,
                title = "Patrimonio Bruto Acumulado",
                legalReference = authorityRef ?: "Art. 592 Num. 1 E.T. (Cuentas + Activos NIC 16 + Inversiones)",
                accumulatedCop = grossWealthCop,
                limitUvt = wealthLimitUnits,
                uvtValueCop = safeUvt
            ),
            buildThresholdStatus(
                type = DianCriterionType.INGRESOS_BRUTOS,
                title = "Ingresos Brutos del Año",
                legalReference = authorityRef ?: "Art. 592 y 593 E.T. (Ingresos ordinarios sin ganancias ocasionales)",
                accumulatedCop = grossIncomeCop,
                limitUvt = incomeLimitUnits,
                uvtValueCop = safeUvt
            ),
            buildThresholdStatus(
                type = DianCriterionType.CONSUMOS_TARJETA_CREDITO,
                title = "Consumos con Tarjeta de Crédito",
                legalReference = authorityRef ?: "Art. 594-3 Lit. a) E.T. (Compras realizadas con T. de Crédito)",
                accumulatedCop = creditCardExpensesCop,
                limitUvt = cardLimitUnits,
                uvtValueCop = safeUvt
            ),
            buildThresholdStatus(
                type = DianCriterionType.COMPRAS_Y_CONSUMOS_TOTALES,
                title = "Compras y Consumos Totales",
                legalReference = authorityRef ?: "Art. 594-3 Lit. b) E.T. (Total de gastos y compras del período)",
                accumulatedCop = totalPurchasesAndExpensesCop,
                limitUvt = purchasesLimitUnits,
                uvtValueCop = safeUvt
            ),
            buildThresholdStatus(
                type = DianCriterionType.CONSIGNACIONES_E_INVERSIONES,
                title = "Consignaciones, Depósitos e Inversiones",
                legalReference = authorityRef ?: "Art. 594-3 Lit. c) E.T. (Entradas bancarias, Nequi, DaviPlata y CDTs)",
                accumulatedCop = bankDepositsAndInvestmentsCop,
                limitUvt = depositsLimitUnits,
                uvtValueCop = safeUvt
            )
        )
    }

    /**
     * Calcula la deducción especial del 1% de compras soportadas con Factura Electrónica de Venta
     * pagadas mediante tarjeta débito/crédito o medio electrónico (Art. 336 Num. 5 E.T. - Ley 2277/2022),
     * limitada hasta 240 UVT anuales (o el porcentaje/límite de la jurisdicción activa).
     */
    fun calculateElectronicInvoiceBenefit(
        fiscalYear: Int,
        uvtValueCop: Double,
        accounts: List<AccountEntity>,
        transactionsInYear: List<TransactionEntity>,
        deductionRate: Double = ColombianUvtCatalog.ELECTRONIC_INVOICE_DEDUCTION_RATE,
        maxDeductionUnits: Double = ColombianUvtCatalog.ELECTRONIC_INVOICE_MAX_DEDUCTION_UVT,
        autoTagOcrReceiptsAsElectronicInvoice: Boolean = true
    ): ElectronicInvoiceBenefitSummary {
        val safeUvt = if (uvtValueCop > 0.0) uvtValueCop else ColombianUvtCatalog.getDefaultUvtForYear(fiscalYear)
        val accountsById = accounts.associateBy { it.id }
        val yearPrefix = "$fiscalYear-"

        val eligibleTransactions = transactionsInYear.filter { tx ->
            if (!tx.date.startsWith(yearPrefix) || !tx.type.equals("EXPENSE", ignoreCase = true) || isOpeningOrInternalTransfer(tx)) {
                return@filter false
            }
            val account = accountsById[tx.accountId]
            // Requisito legal Ley 2277/2022: Pago por medio electrónico (no efectivo CASH)
            val paidElectronically = account != null && !account.type.equals("CASH", ignoreCase = true)
            val hasExplicitTag = tx.tags.contains("FE_DIAN", ignoreCase = true) ||
                tx.tags.contains("FACTURA_ELECTRONICA", ignoreCase = true) ||
                tx.tags.contains("TAX_DEDUCTIBLE", ignoreCase = true)
            val hasOcrReceiptSupport = autoTagOcrReceiptsAsElectronicInvoice && !tx.receiptUrl.isNullOrBlank()

            paidElectronically && (hasExplicitTag || hasOcrReceiptSupport)
        }

        val eligibleTotalCop = eligibleTransactions.sumOf { max(0.0, it.amount) }
        val rawDeductionCop = eligibleTotalCop * deductionRate
        val maxDeductionCop = maxDeductionUnits * safeUvt
        val appliedDeductionCop = min(rawDeductionCop, maxDeductionCop)

        return ElectronicInvoiceBenefitSummary(
            eligiblePurchasesTotalCop = eligibleTotalCop,
            deductionOnePercentCop = rawDeductionCop,
            deductionOnePercentUvt = if (safeUvt > 0.0) appliedDeductionCop / safeUvt else 0.0,
            maxAllowedDeductionUvt = maxDeductionUnits,
            maxAllowedDeductionCop = maxDeductionCop,
            appliedDeductionCop = appliedDeductionCop,
            remainingBenefitCop = max(0.0, maxDeductionCop - appliedDeductionCop),
            eligibleTransactionsCount = eligibleTransactions.size
        )
    }

    private fun buildThresholdStatus(
        type: DianCriterionType,
        title: String,
        legalReference: String,
        accumulatedCop: Double,
        limitUvt: Double,
        uvtValueCop: Double
    ): DianThresholdStatus {
        val limitCop = limitUvt * uvtValueCop
        val accumulatedUvt = if (uvtValueCop > 0.0) accumulatedCop / uvtValueCop else 0.0
        val percentage = if (limitCop > 0.0) (accumulatedCop / limitCop) * 100.0 else 0.0
        val remainingCop = max(0.0, limitCop - accumulatedCop)

        val alertLevel = when {
            percentage >= 100.0 -> TaxAlertLevel.EXCEEDED
            percentage >= 70.0 -> TaxAlertLevel.WARNING
            else -> TaxAlertLevel.SAFE
        }

        return DianThresholdStatus(
            type = type,
            title = title,
            legalReference = legalReference,
            accumulatedCop = accumulatedCop,
            accumulatedUvt = accumulatedUvt,
            limitUvt = limitUvt,
            limitCop = limitCop,
            consumptionPercentage = percentage,
            remainingCopBeforeLimit = remainingCop,
            alertLevel = alertLevel
        )
    }
}
