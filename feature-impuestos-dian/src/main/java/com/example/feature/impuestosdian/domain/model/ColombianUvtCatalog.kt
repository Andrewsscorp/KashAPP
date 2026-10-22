package com.example.feature.impuestosdian.domain.model

/**
 * Catálogo oficial de valores de la UVT (Unidad de Valor Tributario) y topes legales
 * del Estatuto Tributario Colombiano (DIAN — Ley 2277 de 2022 y decretos reglamentarios).
 */
object ColombianUvtCatalog {
    // Valores oficiales e históricos de referencia de la UVT en Colombia (COP)
    const val UVT_2024_COP = 47_065.0 // Resolución DIAN 000187 de 2023
    const val UVT_2025_COP = 49_799.0 // Resolución DIAN 000193 de 2024
    const val UVT_2026_DEFAULT_COP = 52_374.0 // Valor base configurable en Ajustes Tributarios

    // Topes legales Personas Naturales (Estatuto Tributario Arts. 592, 593, 594-3)
    const val THRESHOLD_GROSS_WEALTH_UVT = 4_500.0
    const val THRESHOLD_GROSS_INCOME_UVT = 1_400.0
    const val THRESHOLD_CREDIT_CARD_EXPENSES_UVT = 1_400.0
    const val THRESHOLD_TOTAL_PURCHASES_UVT = 1_400.0
    const val THRESHOLD_BANK_DEPOSITS_UVT = 1_400.0

    // Topes mensuales de exención del 4x1000 (Art. 879 Estatuto Tributario)
    const val GMF_EXEMPT_BANK_ACCOUNT_MONTHLY_UVT = 350.0
    const val GMF_EXEMPT_LOW_AMOUNT_WALLET_MONTHLY_UVT = 65.0
    const val GMF_TAX_RATE = 0.004 // 4 por cada 1.000 pesos (0.4%)
    const val GMF_DEDUCTIBLE_RENTA_PERCENTAGE = 0.50 // Art. 115 E.T.: 50% del GMF certificado es deducible

    // Beneficio Ley 2277 de 2022 (Art. 336 Num. 5 E.T.)
    const val ELECTRONIC_INVOICE_DEDUCTION_RATE = 0.01 // 1% de compras con Factura Electrónica
    const val ELECTRONIC_INVOICE_MAX_DEDUCTION_UVT = 240.0

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
}
