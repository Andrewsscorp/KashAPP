package com.example.feature.informes.data.document

import com.example.feature.informes.domain.model.DocumentFileFormat
import com.example.feature.informes.domain.model.DocumentSupportItem
import com.example.feature.informes.domain.model.DocumentSupportType
import com.example.feature.informes.domain.model.SupportVerificationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class DocumentSupportManager {

    private val _documents = MutableStateFlow<List<DocumentSupportItem>>(createInitialDocuments())
    val documents: Flow<List<DocumentSupportItem>> = _documents.asStateFlow()

    fun addDocument(doc: DocumentSupportItem) {
        val current = _documents.value.toMutableList()
        current.add(0, doc)
        _documents.value = current
    }

    fun deleteDocument(docId: String) {
        val current = _documents.value.toMutableList()
        current.removeAll { it.id == docId }
        _documents.value = current
    }

    fun updateStatus(docId: String, status: SupportVerificationStatus) {
        val current = _documents.value.map { doc ->
            if (doc.id == docId) doc.copy(status = status) else doc
        }
        _documents.value = current
    }

    private fun createInitialDocuments(): List<DocumentSupportItem> {
        return listOf(
            DocumentSupportItem(
                id = "doc-001",
                title = "Factura Notaría - Escritura Inmueble Principal",
                type = DocumentSupportType.DOCUMENTO_ACTIVO,
                fileFormat = DocumentFileFormat.PDF,
                date = "2026-10-02",
                relatedAmount = 450_000.0,
                relatedEntityType = "Activo Fijo",
                relatedEntityName = "Apartamento 402 - Edificio Los Álamos",
                tags = listOf("Inmueble", "Notaría", "NIIF"),
                status = SupportVerificationStatus.VERIFICADO,
                notes = "Escritura pública debidamente registrada ante la oficina de instrumentos públicos.",
                fileSizeFormatted = "1.8 MB"
            ),
            DocumentSupportItem(
                id = "doc-002",
                title = "Extracto Bancario Bancolombia - Cuenta Corriente",
                type = DocumentSupportType.EXTRACTO,
                fileFormat = DocumentFileFormat.PDF,
                date = "2026-09-30",
                relatedAmount = 9_600_000.0,
                relatedEntityType = "Cuenta",
                relatedEntityName = "Cuenta Principal Bancolombia",
                tags = listOf("Bancos", "Conciliación", "Septiembre"),
                status = SupportVerificationStatus.VERIFICADO,
                notes = "Conciliación bancaria cuadrada al centavo con el libro contable de KashApp.",
                fileSizeFormatted = "420 KB"
            ),
            DocumentSupportItem(
                id = "doc-003",
                title = "Factura Electrónica Mantenimiento Mazda 3",
                type = DocumentSupportType.FACTURA,
                fileFormat = DocumentFileFormat.PDF,
                date = "2026-09-22",
                relatedAmount = 450_000.0,
                relatedEntityType = "Gasto / Activo",
                relatedEntityName = "Vehículo Mazda 3 Sedan",
                tags = listOf("Vehículo", "Taller", "DIAN"),
                status = SupportVerificationStatus.VERIFICADO,
                notes = "Mantenimiento preventivo 20.000 km con cambio de fluidos y pastillas.",
                fileSizeFormatted = "310 KB"
            ),
            DocumentSupportItem(
                id = "doc-004",
                title = "Comprobante de Pago Préstamo Bogotá",
                type = DocumentSupportType.COMPROBANTE,
                fileFormat = DocumentFileFormat.IMAGE,
                date = "2026-09-15",
                relatedAmount = 1_150_000.0,
                relatedEntityType = "Deuda",
                relatedEntityName = "Banco de Bogotá - Crédito Vehicular",
                tags = listOf("Deuda", "Amortización", "Comprobante"),
                status = SupportVerificationStatus.VERIFICADO,
                notes = "Cuota #18 con desglose de capital $850.000 e intereses $300.000.",
                fileSizeFormatted = "650 KB"
            ),
            DocumentSupportItem(
                id = "doc-005",
                title = "Contrato de Apertura CDT Digital",
                type = DocumentSupportType.CONTRATO,
                fileFormat = DocumentFileFormat.PDF,
                date = "2026-08-10",
                relatedAmount = 6_000_000.0,
                relatedEntityType = "Inversión",
                relatedEntityName = "CDT Digital 360 Días Davivienda",
                tags = listOf("CDT", "Inversión", "Tasa Fija"),
                status = SupportVerificationStatus.VERIFICADO,
                notes = "Tasa pactada al 11.3% E.A. con vencimiento en agosto 2027.",
                fileSizeFormatted = "890 KB"
            ),
            DocumentSupportItem(
                id = "doc-006",
                title = "Recibo Pago Impuesto Predial Unificado",
                type = DocumentSupportType.RECIBO,
                fileFormat = DocumentFileFormat.PDF,
                date = "2026-06-18",
                relatedAmount = 1_850_000.0,
                relatedEntityType = "Gasto Anual",
                relatedEntityName = "Secretaría de Hacienda Distrital",
                tags = listOf("Impuestos", "Predial", "Soporte"),
                status = SupportVerificationStatus.VERIFICADO,
                notes = "Pago con descuento por pronto pago aplicado.",
                fileSizeFormatted = "540 KB"
            ),
            DocumentSupportItem(
                id = "doc-007",
                title = "Póliza Todo Riesgo Vehicular Allianz",
                type = DocumentSupportType.POLIZA,
                fileFormat = DocumentFileFormat.PDF,
                date = "2026-05-14",
                relatedAmount = 2_400_000.0,
                relatedEntityType = "Garantía / Activo",
                relatedEntityName = "Vehículo Mazda 3 Sedan",
                tags = listOf("Seguros", "Póliza", "Vigente"),
                status = SupportVerificationStatus.VERIFICADO,
                notes = "Vigencia hasta mayo 2027. Cobertura al 100% valor comercial.",
                fileSizeFormatted = "1.2 MB"
            ),
            DocumentSupportItem(
                id = "doc-008",
                title = "Recibo Compra Tecnología MacBook M2",
                type = DocumentSupportType.FACTURA,
                fileFormat = DocumentFileFormat.PDF,
                date = "2026-04-10",
                relatedAmount = 11_500_000.0,
                relatedEntityType = "Activo Fijo",
                relatedEntityName = "MacBook Pro M2 16\"",
                tags = listOf("Apple", "Garantía", "Activo"),
                status = SupportVerificationStatus.PENDIENTE,
                notes = "Garantía AppleCare vigente por 3 años.",
                fileSizeFormatted = "780 KB"
            )
        )
    }
}
