package com.example.feature.ocr.model

/**
 * Parsed line item from an OCR-scanned receipt or invoice.
 */
data class ReceiptItem(
    val description: String,
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0
)

/**
 * Structured result produced by OCR receipt/invoice analysis.
 */
data class ReceiptScanResult(
    val merchantName: String = "",
    val totalAmount: Double = 0.0,
    val subtotal: Double = 0.0,
    val taxAmount: Double = 0.0,
    val dateString: String = "",
    val detectedCurrency: String = "COP",
    val items: List<ReceiptItem> = emptyList(),
    val rawText: String = "",
    val confidence: Float = 0.95f,
    val suggestedCategory: String = "Alimentación"
)
