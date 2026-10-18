package com.example.feature.informes.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.informes.domain.model.*
import com.example.feature.informes.ui.components.InformesFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentosSoportesTab(
    documents: List<DocumentSupportItem>,
    currency: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedTypeFilter: DocumentSupportType?,
    onSelectTypeFilter: (DocumentSupportType?) -> Unit,
    onOpenAddDocumentModal: () -> Unit,
    onSelectPreviewDocument: (DocumentSupportItem) -> Unit,
    onDeleteDocument: (String) -> Unit,
    onUpdateStatus: (String, SupportVerificationStatus) -> Unit
) {
    val filteredDocs = remember(documents, searchQuery, selectedTypeFilter) {
        documents.filter { doc ->
            val matchesQuery = searchQuery.isBlank() ||
                    doc.title.contains(searchQuery, ignoreCase = true) ||
                    doc.relatedEntityName.contains(searchQuery, ignoreCase = true) ||
                    doc.notes.contains(searchQuery, ignoreCase = true) ||
                    doc.tags.any { it.contains(searchQuery, ignoreCase = true) }

            val matchesType = selectedTypeFilter == null || doc.type == selectedTypeFilter
            matchesQuery && matchesType
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header & Action Button
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CENTRO DOCUMENTAL",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Soportes y Evidencias (${documents.size})",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onOpenAddDocumentModal,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Subir Soporte", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Buscar por nombre, activo, banco, póliza...", color = Color(0xFF64748B), fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = null, tint = Color(0xFF94A3B8))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }

        // Type filter chips
        ScrollableTabRow(
            selectedTabIndex = if (selectedTypeFilter == null) 0 else selectedTypeFilter.ordinal + 1,
            containerColor = Color.Transparent,
            contentColor = Color(0xFF38BDF8),
            edgePadding = 0.dp,
            divider = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = selectedTypeFilter == null,
                onClick = { onSelectTypeFilter(null) },
                label = { Text("Todos (${documents.size})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF1E293B),
                    labelColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier.padding(end = 6.dp)
            )

            DocumentSupportType.values().forEach { t ->
                val isSel = selectedTypeFilter == t
                val count = documents.count { it.type == t }
                FilterChip(
                    selected = isSel,
                    onClick = { onSelectTypeFilter(t) },
                    label = { Text("${t.label} ($count)", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF38BDF8),
                        selectedLabelColor = Color(0xFF0F172A),
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }

        // Document Cards List
        if (filteredDocs.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.FindInPage, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No se encontraron soportes con el filtro actual", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }
            }
        } else {
            filteredDocs.forEach { doc ->
                DocumentCardItem(
                    doc = doc,
                    currency = currency,
                    onClick = { onSelectPreviewDocument(doc) },
                    onDelete = { onDeleteDocument(doc.id) },
                    onStatusToggle = {
                        val nextStatus = when (doc.status) {
                            SupportVerificationStatus.VERIFICADO -> SupportVerificationStatus.PENDIENTE
                            SupportVerificationStatus.PENDIENTE -> SupportVerificationStatus.OBSERVADO
                            SupportVerificationStatus.OBSERVADO -> SupportVerificationStatus.VERIFICADO
                        }
                        onUpdateStatus(doc.id, nextStatus)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun DocumentCardItem(
    doc: DocumentSupportItem,
    currency: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onStatusToggle: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E293B),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type & Format Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = doc.type.label,
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF334155)
                    ) {
                        Text(
                            text = doc.fileFormat.extension,
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Verification Status Pill
                Surface(
                    onClick = onStatusToggle,
                    shape = RoundedCornerShape(8.dp),
                    color = when (doc.status) {
                        SupportVerificationStatus.VERIFICADO -> Color(0xFF064E3B)
                        SupportVerificationStatus.PENDIENTE -> Color(0xFF78350F)
                        SupportVerificationStatus.OBSERVADO -> Color(0xFF7F1D1D)
                    }
                ) {
                    Text(
                        text = doc.status.label,
                        color = when (doc.status) {
                            SupportVerificationStatus.VERIFICADO -> Color(0xFF34D399)
                            SupportVerificationStatus.PENDIENTE -> Color(0xFFFBBF24)
                            SupportVerificationStatus.OBSERVADO -> Color(0xFFF87171)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = doc.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Vínculo: ${doc.relatedEntityName} (${doc.relatedEntityType})",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )

            doc.relatedAmount?.let { amt ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Monto asociado: ${InformesFormatters.formatMoney(amt, currency)}",
                    color = Color(0xFF34D399),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF334155))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fecha: ${doc.date} • ${doc.fileSizeFormatted}",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )

                Row {
                    IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = "Ver", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
