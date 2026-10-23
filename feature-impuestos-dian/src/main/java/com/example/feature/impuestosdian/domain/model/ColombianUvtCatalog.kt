package com.example.feature.impuestosdian.domain.model

import kotlin.math.max

/**
 * Catálogo oficial de valores de la UVT (Unidad de Valor Tributario), topes legales y tabla
 * progresiva del Estatuto Tributario Colombiano (DIAN — Ley 2277 de 2022 y Resolución 000238 de 2025).
 */
object ColombianUvtCatalog {
    // Valores oficiales de la UVT en Colombia (COP)
    const val UVT_2024_COP = 47_065.0 // Resolución DIAN 000187 de 2023
    const val UVT_2025_COP = 49_799.0 // Resolución DIAN 000193 de 2024
    const val UVT_2026_DEFAULT_COP = 52_374.0 // Resolución DIAN 000238 del 15 de diciembre de 2025

    // Topes legales Personas Naturales para estar obligado a declarar (Arts. 592, 593, 594-3 E.T.)
    const val THRESHOLD_GROSS_WEALTH_UVT = 4_500.0
    const val THRESHOLD_GROSS_INCOME_UVT = 1_400.0
    const val THRESHOLD_CREDIT_CARD_EXPENSES_UVT = 1_400.0
    const val THRESHOLD_TOTAL_PURCHASES_UVT = 1_400.0
    const val THRESHOLD_BANK_DEPOSITS_UVT = 1_400.0
    const val THRESHOLD_VAT_RESPONSIBLE_UVT = 3_500.0 // Art. 437 Par. 3 E.T.

    // Topes mensuales de exención del 4x1000 (Art. 879 E.T. y Art. 65 Ley 2277/2022)
    const val GMF_EXEMPT_BANK_ACCOUNT_MONTHLY_UVT = 350.0
    const val GMF_EXEMPT_LOW_AMOUNT_WALLET_MONTHLY_UVT = 65.0
    const val GMF_TAX_RATE = 0.004 // 4 por cada 1.000 pesos (0.4%)
    const val GMF_DEDUCTIBLE_RENTA_PERCENTAGE = 0.50 // Art. 115 E.T.: 50% del GMF certificado es deducible

    // Beneficio Ley 2277 de 2022 (Art. 336 Num. 5 E.T.)
    const val ELECTRONIC_INVOICE_DEDUCTION_RATE = 0.01 // 1% de compras con Factura Electrónica
    const val ELECTRONIC_INVOICE_MAX_DEDUCTION_UVT = 240.0

    // Depuración Cédula General Personas Naturales Residentes (Art. 336 y Art. 206 Num. 10 E.T.)
    const val DEFAULT_MANDATORY_INCRGO_RATE = 0.08 // 4% Salud + 4% Pensión obligatoria estándar
    const val LABOR_EXEMPT_INCOME_RATE = 0.25 // 25% renta exenta laboral
    const val LABOR_EXEMPT_INCOME_MAX_UVT = 790.0 // Límite anual de 790 UVT (Ley 2277/2022)
    const val CEDULA_GENERAL_GLOBAL_CAP_RATE = 0.40 // Límite del 40% sobre ingresos netos
    const val CEDULA_GENERAL_GLOBAL_MAX_UVT = 1_340.0 // Tope absoluto de 1.340 UVT (Ley 2277/2022)
    const val DEPENDENT_DEDUCTION_UVT_PER_PERSON = 72.0 // 72 UVT por dependiente (hasta 4 dependientes)
    const val MAX_DEPENDENTS_COUNT = 4

    // IVA estándar y retención rendimientos financieros Colombia
    const val STANDARD_VAT_RATE_CO = 0.19 // 19% IVA general
    const val INVESTMENT_YIELD_WITHHOLDING_RATE_CO = 0.04 // 4% Retefuente rendimientos financieros (CDT)

    fun getDefaultUvtForYear(year: Int): Double {
        return when (year) {
            2024 -> UVT_2024_COP
            2025 -> UVT_2025_COP
            else -> UVT_2026_DEFAULT_COP
        }
    }

    /**
     * Calcula el Impuesto sobre la Renta en UVT aplicando la tabla marginal progresiva
     * del Artículo 241 del Estatuto Tributario para Personas Naturales Residentes.
     */
    fun calculateProgressiveIncomeTaxUvt(taxableNetIncomeUvt: Double): Double {
        val uvt = max(0.0, taxableNetIncomeUvt)
        return when {
            uvt <= 1_090.0 -> 0.0
            uvt <= 1_700.0 -> (uvt - 1_090.0) * 0.19
            uvt <= 4_100.0 -> ((uvt - 1_700.0) * 0.28) + 115.90
            uvt <= 8_670.0 -> ((uvt - 4_100.0) * 0.33) + 787.90
            uvt <= 18_970.0 -> ((uvt - 8_670.0) * 0.35) + 2_296.00
            uvt <= 31_000.0 -> ((uvt - 18_970.0) * 0.37) + 5_901.00
            else -> ((uvt - 31_000.0) * 0.39) + 10_352.10
        }
    }
}
