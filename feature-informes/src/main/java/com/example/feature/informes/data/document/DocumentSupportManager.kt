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

    private val _documents = MutableStateFlow<List<DocumentSupportItem>>(emptyList())
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
        return emptyList()
    }
}
