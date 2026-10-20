package com.example.feature.capturaautomatica.domain.model

data class ParsedBankNotification(
    val sourcePackage: String,
    val detectedBank: SupportedColombianBank,
    val transactionType: String, // "EXPENSE", "INCOME", "TRANSFER"
    val amount: Double,
    val currency: String = "COP",
    val merchantOrCounterparty: String,
    val lastFourDigits: String? = null,
    val confidenceScore: Int = 90,
    val idempotencyKey: String,
    val notificationTimestamp: Long
)
