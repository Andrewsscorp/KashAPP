package com.example.domain.model

enum class RateSourceType(val displayName: String) {
    AUTOMATIC_FRANKFURTER("Tasa de mercado (Frankfurter)"),
    MANUAL("Tasa manual personalizada"),
    OFFICIAL_TRM_REF("TRM oficial de referencia")
}

enum class HistoryRange(val label: String) {
    HOY("Hoy"),
    DAYS_7("7 días"),
    DAYS_30("30 días"),
    MONTHS_3("3 meses"),
    YEAR_1("1 año"),
    CUSTOM("Personalizado")
}

data class EffectiveRate(
    val fromCurrency: String,
    val toCurrency: String,
    val rate: Double,
    val sourceType: RateSourceType,
    val isManual: Boolean,
    val isStale: Boolean,
    val lastUpdatedDate: String,
    val lastUpdatedMillis: Long,
    val variationPercent24h: Double? = null,
    val variationDiff24h: Double? = null,
    val providerName: String = if (isManual) "Ajuste manual · Usuario" else "Frankfurter"
)

data class RateHistoryPoint(
    val date: String, // "YYYY-MM-DD"
    val rate: Double,
    val displayDate: String // "28 sep"
)

data class CurrencyRateCardItem(
    val pairSymbol: String, // "USD / COP"
    val fromCurrency: String,
    val toCurrency: String,
    val formattedRate: String,
    val rateValue: Double,
    val variationPercent: Double,
    val variationDiff: Double,
    val isPositive: Boolean,
    val isManual: Boolean
)

data class TestSuiteItemResult(
    val id: String,
    val name: String,
    val category: String,
    val status: TestStatus,
    val latencyMs: Long,
    val summary: String,
    val technicalDetail: String,
    val payloadSnippet: String? = null,
    val executedAt: Long = System.currentTimeMillis()
) {
    enum class TestStatus {
        PENDING,
        RUNNING,
        OK,
        ERROR
    }
}
