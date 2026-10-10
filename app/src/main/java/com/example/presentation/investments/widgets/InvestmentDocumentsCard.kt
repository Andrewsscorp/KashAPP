package com.example.presentation.investments.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.investments.state.InvestmentFormState
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy

@Composable
fun InvestmentDocumentsCard(
    state: InvestmentFormState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDocumentSelected: (uri: String?, name: String?, size: Long) -> Unit,
    onDocumentCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasDocument = state.selectedDocUri != null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("investment_documents_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Accordion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEDE9FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Documentos y Comprobantes",
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "6. Documentos y Comprobantes",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashNavy
                        )
                        Text(
                            text = if (hasDocument) "${state.selectedDocName} (${state.selectedDocCategory})" else "Opcional (PDF, JPG, PNG hasta 10 MB)",
                            fontSize = 12.sp,
                            color = if (hasDocument) Color(0xFF8B5CF6) else Color(0xFF64748B)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                    tint = Color(0xFF64748B)
                )
            }

            // Expanded Body
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Categoría de Documento
                    Text(
                        text = "Tipo de documento",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KashNavy
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "COMPROBANTE" to "Comprobante",
                            "EXTRACTO" to "Extracto",
                            "TITULO_VALOR" to "Título Valor",
                            "CONTRATO" to "Contrato"
                        ).forEach { (cat, label) ->
                            val isSelected = state.selectedDocCategory == cat
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFFEDE9FE) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF8B5CF6) else Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onDocumentCategorySelected(cat) }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFF6D28D9) else Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!hasDocument) {
                        // Upload Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDocumentSelected(
                                        "content://media/external/file/inv_comprobante_1.pdf",
                                        "Comprobante_Inversion_${state.name.ifBlank { "KashApp" }}.pdf",
                                        2048576L // ~2 MB
                                    )
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEDE9FE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.UploadFile,
                                        contentDescription = "Subir archivo",
                                        tint = Color(0xFF8B5CF6),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Adjuntar comprobante o extracto",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KashNavy
                                )
                                Text(
                                    text = "Formatos soportados: PDF, JPG, PNG (Máx 10 MB)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    } else {
                        // Attached File Chip
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF5F3FF),
                            border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AttachFile,
                                        contentDescription = "Archivo adjunto",
                                        tint = Color(0xFF8B5CF6),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = state.selectedDocName ?: "documento.pdf",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = KashNavy
                                        )
                                        Text(
                                            text = "Tamaño: 2.0 MB • Verificado",
                                            fontSize = 11.sp,
                                            color = Color(0xFF16A34A)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onDocumentSelected(null, null, 0L) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Eliminar archivo",
                                        tint = Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
