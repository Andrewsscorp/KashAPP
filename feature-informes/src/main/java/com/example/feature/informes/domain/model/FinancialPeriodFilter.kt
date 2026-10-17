package com.example.feature.informes.domain.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class PeriodPreset(val label: String) {
    THIS_MONTH("Este mes"),
    LAST_MONTH("Mes anterior"),
    LAST_3_MONTHS("Últimos 3 meses"),
    LAST_6_MONTHS("Últimos 6 meses"),
    THIS_YEAR("Este año"),
    LAST_YEAR("Año anterior"),
    CUSTOM("Personalizado")
}

enum class ComparisonType(val label: String) {
    PREVIOUS_PERIOD("Período anterior"),
    SAME_PERIOD_LAST_YEAR("Mismo período año anterior"),
    CUSTOM("Personalizado"),
    NONE("Sin comparación")
}

enum class ReportFrequency(val label: String) {
    MONTHLY("Mensual"),
    QUARTERLY("Trimestral"),
    SEMI_ANNUALLY("Semestral"),
    ANNUALLY("Anual")
}

data class FinancialPeriodFilter(
    val preset: PeriodPreset = PeriodPreset.THIS_MONTH,
    val startDate: LocalDate = LocalDate.now().withDayOfMonth(1),
    val endDate: LocalDate = LocalDate.now(),
    val comparison: ComparisonType = ComparisonType.PREVIOUS_PERIOD,
    val frequency: ReportFrequency = ReportFrequency.MONTHLY,
    val currency: String = "COP"
) {
    val periodLabel: String
        get() = when (preset) {
            PeriodPreset.THIS_MONTH -> "Este mes (${startDate.format(DateTimeFormatter.ofPattern("MMM yyyy"))})"
            PeriodPreset.LAST_MONTH -> "Mes anterior (${startDate.format(DateTimeFormatter.ofPattern("MMM yyyy"))})"
            PeriodPreset.LAST_3_MONTHS -> "Últimos 3 meses"
            PeriodPreset.LAST_6_MONTHS -> "Últimos 6 meses"
            PeriodPreset.THIS_YEAR -> "Año ${startDate.year}"
            PeriodPreset.LAST_YEAR -> "Año ${startDate.year}"
            PeriodPreset.CUSTOM -> "${startDate.format(DateTimeFormatter.ofPattern("dd/MM/yy"))} - ${endDate.format(DateTimeFormatter.ofPattern("dd/MM/yy"))}"
        }

    fun getComparisonRange(): Pair<LocalDate, LocalDate>? {
        return when (comparison) {
            ComparisonType.PREVIOUS_PERIOD -> {
                val days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1
                Pair(startDate.minusDays(days), startDate.minusDays(1))
            }
            ComparisonType.SAME_PERIOD_LAST_YEAR -> {
                Pair(startDate.minusYears(1), endDate.minusYears(1))
            }
            ComparisonType.CUSTOM -> {
                Pair(startDate.minusMonths(1), endDate.minusMonths(1))
            }
            ComparisonType.NONE -> null
        }
    }
}
