package com.example.feature.informes.domain.model

enum class DocumentSupportType(val label: String, val iconName: String) {
    FACTURA("Factura", "Receipt"),
    RECIBO("Recibo", "ReceiptLong"),
    EXTRACTO("Extracto Bancario", "AccountBalance"),
    COMPROBANTE("Comprobante Pago", "CheckCircle"),
    CONTRATO("Contrato", "Description"),
    GARANTIA("Garantía", "VerifiedUser"),
    DOCUMENTO_ACTIVO("Documento de Activo", "Inventory"),
    POLIZA("Póliza de Seguro", "Security"),
    OTRO("Otro Documento", "Attachment")
}

enum class DocumentFileFormat(val extension: String, val mimeType: String) {
    PDF("PDF", "application/pdf"),
    IMAGE("IMG", "image/jpeg"),
    EXCEL("XLSX", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    CSV("CSV", "text/csv")
}

enum class SupportVerificationStatus(val label: String, val colorHex: String) {
    VERIFICADO("Verificado", "#10B981"),
    PENDIENTE("Pendiente de Revisión", "#F59E0B"),
    OBSERVADO("Con Observación", "#EF4444")
}

data class DocumentSupportItem(
    val id: String,
    val userId: String = "kash_user_01",
    val title: String,
    val type: DocumentSupportType,
    val fileFormat: DocumentFileFormat,
    val date: String,
    val relatedAmount: Double?,
    val relatedEntityType: String,
    val relatedEntityName: String,
    val tags: List<String>,
    val status: SupportVerificationStatus,
    val notes: String,
    val fileSizeFormatted: String = "240 KB",
    val documentUri: String? = null
)
