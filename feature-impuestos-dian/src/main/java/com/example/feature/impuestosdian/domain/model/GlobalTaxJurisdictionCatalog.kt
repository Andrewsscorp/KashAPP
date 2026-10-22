package com.example.feature.impuestosdian.domain.model

/**
 * Jurisdicciones fiscales soportadas por KashApp para cálculo de impuestos locales e internacionales.
 */
enum class TaxJurisdiction(
    val code: String,
    val countryName: String,
    val flagEmoji: String,
    val authorityAcronym: String,
    val authorityFullName: String,
    val defaultCurrency: String,
    val taxUnitName: String,
    val defaultTaxUnitValue: Double,
    val annualIncomeFilingThresholdUnits: Double,
    val annualWealthFilingThresholdUnits: Double,
    val standardVatOrSalesTaxRate: Double,
    val financialTransactionTaxRate: Double,
    val bankExemptMonthlyUnits: Double,
    val walletExemptMonthlyUnits: Double,
    val electronicInvoiceDeductionRate: Double,
    val electronicInvoiceMaxUnits: Double,
    val investmentYieldWithholdingRate: Double,
    val effectiveIncomeTaxEstimateRate: Double
) {
    CO_DIAN(
        code = "CO_DIAN",
        countryName = "Colombia",
        flagEmoji = "🇨🇴",
        authorityAcronym = "DIAN",
        authorityFullName = "Dirección de Impuestos y Aduanas Nacionales",
        defaultCurrency = "COP",
        taxUnitName = "UVT",
        defaultTaxUnitValue = ColombianUvtCatalog.UVT_2026_DEFAULT_COP,
        annualIncomeFilingThresholdUnits = ColombianUvtCatalog.THRESHOLD_GROSS_INCOME_UVT, // 1.400 UVT
        annualWealthFilingThresholdUnits = ColombianUvtCatalog.THRESHOLD_GROSS_WEALTH_UVT, // 4.500 UVT
        standardVatOrSalesTaxRate = 0.19,        // IVA 19%
        financialTransactionTaxRate = 0.004,     // 4x1000 (0.4%)
        bankExemptMonthlyUnits = 350.0,          // 350 UVT
        walletExemptMonthlyUnits = 65.0,         // 65 UVT (Nequi, DaviPlata, etc.)
        electronicInvoiceDeductionRate = 0.01,   // 1% Ley 2277/2022
        electronicInvoiceMaxUnits = 240.0,       // Máx. 240 UVT
        investmentYieldWithholdingRate = 0.04,   // 4% Retefuente CDT
        effectiveIncomeTaxEstimateRate = 0.12
    ),
    MX_SAT(
        code = "MX_SAT",
        countryName = "México",
        flagEmoji = "🇲🇽",
        authorityAcronym = "SAT",
        authorityFullName = "Servicio de Administración Tributaria",
        defaultCurrency = "MXN",
        taxUnitName = "UMA Anual",
        defaultTaxUnitValue = 41_273.52,         // 1 UMA anual referencia MXN
        annualIncomeFilingThresholdUnits = 9.69, // ~400,000 MXN para Declaración Anual ISR
        annualWealthFilingThresholdUnits = 50.0, // Referencia patrimonial informativa
        standardVatOrSalesTaxRate = 0.16,        // IVA 16%
        financialTransactionTaxRate = 0.0,       // Sin GMF general (solo IDE efectivo > 15k MXN)
        bankExemptMonthlyUnits = 0.0,
        walletExemptMonthlyUnits = 0.0,
        electronicInvoiceDeductionRate = 0.15,   // Deducciones personales con CFDI hasta 5 UMAs / 15%
        electronicInvoiceMaxUnits = 5.0,         // Tope 5 UMAs anuales (Art. 151 LISR)
        investmentYieldWithholdingRate = 0.005,  // Retención ISR sobre intereses reales
        effectiveIncomeTaxEstimateRate = 0.18
    ),
    US_IRS(
        code = "US_IRS",
        countryName = "Estados Unidos",
        flagEmoji = "🇺🇸",
        authorityAcronym = "IRS",
        authorityFullName = "Internal Revenue Service",
        defaultCurrency = "USD",
        taxUnitName = "Std. Deduction",
        defaultTaxUnitValue = 15_000.0,          // Standard Deduction Single Filer (USD)
        annualIncomeFilingThresholdUnits = 1.0,  // 1x Standard Deduction ($15,000 USD)
        annualWealthFilingThresholdUnits = 100.0,
        standardVatOrSalesTaxRate = 0.0725,      // Sales Tax promedio configurable (7.25%)
        financialTransactionTaxRate = 0.0,
        bankExemptMonthlyUnits = 0.0,
        walletExemptMonthlyUnits = 0.0,
        electronicInvoiceDeductionRate = 0.05,   // Gastos deducibles documentados (Itemized / Business)
        electronicInvoiceMaxUnits = 2.0,
        investmentYieldWithholdingRate = 0.15,   // Capital Gains / Dividend withholding
        effectiveIncomeTaxEstimateRate = 0.15
    ),
    ES_AEAT(
        code = "ES_AEAT",
        countryName = "España",
        flagEmoji = "🇪🇸",
        authorityAcronym = "AEAT",
        authorityFullName = "Agencia Estatal de Administración Tributaria",
        defaultCurrency = "EUR",
        taxUnitName = "Mín. IRPF",
        defaultTaxUnitValue = 22_000.0,          // Límite general 1 pagador (22.000 EUR)
        annualIncomeFilingThresholdUnits = 1.0,  // 22.000 EUR
        annualWealthFilingThresholdUnits = 31.8, // ~700.000 EUR Mínimo exento Impuesto Patrimonio
        standardVatOrSalesTaxRate = 0.21,        // IVA General 21%
        financialTransactionTaxRate = 0.0,
        bankExemptMonthlyUnits = 0.0,
        walletExemptMonthlyUnits = 0.0,
        electronicInvoiceDeductionRate = 0.05,   // Deducciones autonómicas/generales con factura
        electronicInvoiceMaxUnits = 0.5,
        investmentYieldWithholdingRate = 0.19,   // Retención Base del Ahorro 19%
        effectiveIncomeTaxEstimateRate = 0.19
    ),
    PE_SUNAT(
        code = "PE_SUNAT",
        countryName = "Perú",
        flagEmoji = "🇵🇪",
        authorityAcronym = "SUNAT",
        authorityFullName = "Superintendencia Nacional de Aduanas y de Administración Tributaria",
        defaultCurrency = "PEN",
        taxUnitName = "UIT",
        defaultTaxUnitValue = 5_350.0,           // 1 UIT en Soles (PEN)
        annualIncomeFilingThresholdUnits = 7.0,  // Tramo exento hasta 7 UIT anuales
        annualWealthFilingThresholdUnits = 100.0,
        standardVatOrSalesTaxRate = 0.18,        // IGV 18%
        financialTransactionTaxRate = 0.00005,   // ITF 0.005% sobre movimientos bancarios
        bankExemptMonthlyUnits = 10.0,           // Cuentas de remuneraciones exentas de ITF
        walletExemptMonthlyUnits = 5.0,
        electronicInvoiceDeductionRate = 0.15,   // Deducción adicional en hoteles/restaurantes con boleta electrónica
        electronicInvoiceMaxUnits = 3.0,         // Tope de 3 UIT adicionales
        investmentYieldWithholdingRate = 0.05,   // Renta de Segunda Categoría (5%)
        effectiveIncomeTaxEstimateRate = 0.14
    ),
    CL_SII(
        code = "CL_SII",
        countryName = "Chile",
        flagEmoji = "🇨🇱",
        authorityAcronym = "SII",
        authorityFullName = "Servicio de Impuestos Internos",
        defaultCurrency = "CLP",
        taxUnitName = "UTA",
        defaultTaxUnitValue = 807_000.0,         // 1 Unidad Tributaria Anual (UTA en CLP)
        annualIncomeFilingThresholdUnits = 13.5, // Exento hasta 13,5 UTA (Impuesto Global Complementario)
        annualWealthFilingThresholdUnits = 150.0,
        standardVatOrSalesTaxRate = 0.19,        // IVA 19%
        financialTransactionTaxRate = 0.0,
        bankExemptMonthlyUnits = 0.0,
        walletExemptMonthlyUnits = 0.0,
        electronicInvoiceDeductionRate = 0.05,
        electronicInvoiceMaxUnits = 10.0,
        investmentYieldWithholdingRate = 0.10,
        effectiveIncomeTaxEstimateRate = 0.13
    ),
    AR_ARCA(
        code = "AR_ARCA",
        countryName = "Argentina",
        flagEmoji = "🇦🇷",
        authorityAcronym = "ARCA",
        authorityFullName = "Agencia de Recaudación y Control Aduanero (ex AFIP)",
        defaultCurrency = "ARS",
        taxUnitName = "Mín. Anual",
        defaultTaxUnitValue = 15_000_000.0,
        annualIncomeFilingThresholdUnits = 1.0,
        annualWealthFilingThresholdUnits = 6.6,  // Mínimo no imponible Bienes Personales
        standardVatOrSalesTaxRate = 0.21,        // IVA 21%
        financialTransactionTaxRate = 0.006,     // Impuesto a los Débitos y Créditos Bancarios (0.6%)
        bankExemptMonthlyUnits = 1.0,            // Cuentas sueldo exentas
        walletExemptMonthlyUnits = 0.2,
        electronicInvoiceDeductionRate = 0.05,
        electronicInvoiceMaxUnits = 1.0,
        investmentYieldWithholdingRate = 0.15,
        effectiveIncomeTaxEstimateRate = 0.20
    ),
    GLOBAL_CUSTOM(
        code = "GLOBAL_CUSTOM",
        countryName = "Internacional (Personalizado)",
        flagEmoji = "🌍",
        authorityAcronym = "TAX",
        authorityFullName = "Régimen Fiscal Internacional Configurable",
        defaultCurrency = "USD",
        taxUnitName = "Unidad Fiscal",
        defaultTaxUnitValue = 1_000.0,
        annualIncomeFilingThresholdUnits = 20.0,
        annualWealthFilingThresholdUnits = 100.0,
        standardVatOrSalesTaxRate = 0.15,
        financialTransactionTaxRate = 0.0,
        bankExemptMonthlyUnits = 0.0,
        walletExemptMonthlyUnits = 0.0,
        electronicInvoiceDeductionRate = 0.05,
        electronicInvoiceMaxUnits = 10.0,
        investmentYieldWithholdingRate = 0.10,
        effectiveIncomeTaxEstimateRate = 0.15
    );

    companion object {
        fun fromCode(code: String?): TaxJurisdiction {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: CO_DIAN
        }
    }
}

/**
 * Catálogo y asistente de vinculación transversal entre el Módulo de Configuración Tributaria
 * y los demás módulos de KashApp (Creación de Cuentas, Registro de Gastos/Ingresos, Activos e Informes).
 */
object GlobalTaxJurisdictionCatalog {

    /**
     * Genera la guía fiscal contextual para el módulo de Creación/Edición de Cuentas (`CreateAccountModal`).
     */
    fun getAccountCreationTaxGuidance(
        profile: TaxConfigurationProfile,
        accountType: String,
        isMarkedTaxExempt: Boolean
    ): AccountTaxCreationHint {
        val jurisdiction = profile.jurisdiction
        val unitVal = profile.taxUnitValue
        val isWallet = accountType.equals("WALLET", ignoreCase = true)

        return when (jurisdiction) {
            TaxJurisdiction.CO_DIAN -> {
                val limit350 = ColombianUvtCatalog.GMF_EXEMPT_BANK_ACCOUNT_MONTHLY_UVT * unitVal
                val limit65 = ColombianUvtCatalog.GMF_EXEMPT_LOW_AMOUNT_WALLET_MONTHLY_UVT * unitVal
                AccountTaxCreationHint(
                    jurisdictionCode = jurisdiction.code,
                    switchLabel = "Cuenta Exenta de 4x1000 (Tope 350 UVT/mes)",
                    switchSubtitle = if (isMarkedTaxExempt) {
                        "Exenta hasta 350 UVT/mes (${limit350.toLong()} ${profile.currency}). Solo puedes tener 1 cuenta bancaria marcada con 350 UVT."
                    } else if (isWallet) {
                        "Billetera de bajo monto: goza de 65 UVT/mes exentas (${limit65.toLong()} ${profile.currency}) sin gastar tu cupo de 350 UVT."
                    } else {
                        "Si no está exenta, cada retiro o transferencia gravará 4x1000 (0.4%) desde el primer peso."
                    },
                    enforcesSingleExemptAccountRule = true,
                    financialTaxRate = profile.financialTransactionTaxRate
                )
            }
            TaxJurisdiction.PE_SUNAT -> AccountTaxCreationHint(
                jurisdictionCode = jurisdiction.code,
                switchLabel = "Cuenta Sueldo Exenta de ITF (0.005%)",
                switchSubtitle = if (isMarkedTaxExempt) {
                    "Exenta del Impuesto a las Transacciones Financieras (ITF) por acreditación de remuneraciones."
                } else {
                    "Afecta a ITF (0.005%) por cada acreditación o débito bancario."
                },
                enforcesSingleExemptAccountRule = false,
                financialTaxRate = profile.financialTransactionTaxRate
            )
            TaxJurisdiction.AR_ARCA -> AccountTaxCreationHint(
                jurisdictionCode = jurisdiction.code,
                switchLabel = "Cuenta Sueldo Exenta Ley 25.413 (0.6%)",
                switchSubtitle = if (isMarkedTaxExempt) {
                    "Exenta del Impuesto a los Débitos y Créditos Bancarios (0.6%)."
                } else {
                    "Afecta al 0.6% sobre débitos y créditos bancarios."
                },
                enforcesSingleExemptAccountRule = false,
                financialTaxRate = profile.financialTransactionTaxRate
            )
            else -> AccountTaxCreationHint(
                jurisdictionCode = jurisdiction.code,
                switchLabel = "Cuenta Exenta de Impuestos Bancarios (${jurisdiction.authorityAcronym})",
                switchSubtitle = if (profile.financialTransactionTaxRate > 0.0) {
                    "Exime esta cuenta de la tasa de movimientos financieros (${"%.2f".format(profile.financialTransactionTaxRate * 100)}%)."
                } else {
                    "En ${jurisdiction.countryName} no aplica gravamen general a transferencias bancarias (0%)."
                },
                enforcesSingleExemptAccountRule = false,
                financialTaxRate = profile.financialTransactionTaxRate
            )
        }
    }

    /**
     * Calcula en vivo el impacto fiscal de un movimiento al registrarlo en `CreateExpenseScreen` o `:feature-ocr`.
     */
    fun previewTransactionTaxImpact(
        profile: TaxConfigurationProfile,
        amount: Double,
        isExpense: Boolean,
        isCashAccount: Boolean,
        isAccountTaxExempt: Boolean,
        hasElectronicInvoiceSupport: Boolean
    ): TransactionTaxImpactPreview {
        if (amount <= 0.0) {
            return TransactionTaxImpactPreview(0.0, 0.0, 0.0, "Ingresa un monto para estimar el impacto fiscal.")
        }

        val vatRate = profile.vatOrSalesTaxRate
        val estimatedIncludedVat = if (isExpense && vatRate > 0.0) {
            amount - (amount / (1.0 + vatRate))
        } else {
            0.0
        }

        val estimatedBankTax = if (isExpense && !isCashAccount && !isAccountTaxExempt && profile.financialTransactionTaxRate > 0.0) {
            amount * profile.financialTransactionTaxRate
        } else {
            0.0
        }

        val estimatedDeduction = if (isExpense && !isCashAccount && hasElectronicInvoiceSupport) {
            amount * profile.electronicInvoiceDeductionRate
        } else {
            0.0
        }

        val summary = buildString {
            append("${profile.jurisdiction.flagEmoji} ${profile.jurisdiction.authorityAcronym}: ")
            if (estimatedIncludedVat > 0.0) {
                append("IVA/Tax incl. ~${estimatedIncludedVat.toLong()} ${profile.currency}")
            }
            if (estimatedDeduction > 0.0) {
                append(" • Deducción factura: +${estimatedDeduction.toLong()} ${profile.currency}")
            }
            if (estimatedBankTax > 0.0) {
                append(" • Imp. bancario: ${estimatedBankTax.toLong()} ${profile.currency}")
            }
        }

        return TransactionTaxImpactPreview(
            estimatedIncludedVatAmount = estimatedIncludedVat,
            estimatedBankTransactionTaxAmount = estimatedBankTax,
            estimatedTaxDeductionBenefitAmount = estimatedDeduction,
            summaryMessage = summary
        )
    }
}
