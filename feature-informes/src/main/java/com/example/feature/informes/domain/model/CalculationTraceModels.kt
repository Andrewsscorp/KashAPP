package com.example.feature.informes.domain.model

data class CalculationTraceDetail(
    val metricKey: String,
    val title: String,
    val subtitle: String? = null,
    val currentFormattedValue: String,
    val currency: String = "COP",
    val accountingFormula: String,
    val mathematicalOperation: String,
    val dataSources: List<DataSourceItem>,
    val stepByStepExplanation: List<String>,
    val accountingStandardNote: String,
    val auditConfidenceStatus: String = "100% Conciliado con Libros Locales",
    val relatedEntityName: String? = null
)

data class DataSourceItem(
    val tableName: String,
    val entityType: String,
    val description: String,
    val recordCount: Int,
    val aggregatedAmount: Double? = null
)
