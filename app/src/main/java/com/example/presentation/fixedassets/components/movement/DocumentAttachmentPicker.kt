package com.example.presentation.fixedassets.components.movement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Clean attachment component for appraisal documents, PDF reports, or invoice receipts.
 */
@Composable
fun DocumentAttachmentPicker(
    documentUri: String?,
    onDocumentChanged: (String?) -> Unit,
    label: String = "Documento de Soporte / Peritaje (PDF)",
    placeholderDocName: String = "informe_pericial_avalúo.pdf",
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8EA5C4),
            letterSpacing = 0.5.sp
        )

        if (!documentUri.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF064E3B),
                border = BorderStroke(1.dp, Color(0xFF059669)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = documentUri.substringAfterLast("/").ifBlank { placeholderDocName },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Documento verificado y vinculado al expediente",
                                fontSize = 10.sp,
                                color = Color(0xFFA7F3D0)
                            )
                        }
                    }

                    IconButton(onClick = { onDocumentChanged(null) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Quitar documento",
                            tint = Color(0xFFE2E8F0),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0B172B),
                border = BorderStroke(1.dp, Color(0xFF132B4F)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Attach simulated official certified document
                        onDocumentChanged("content://docs/$placeholderDocName")
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "+ Adjuntar Dictamen / PDF de Peritaje",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF38BDF8)
                        )
                        Text(
                            text = "Formatos soportados: PDF, XML, Imagen de Acta (Máx. 15 MB)",
                            fontSize = 10.5.sp,
                            color = Color(0xFF8EA5C4)
                        )
                    }
                }
            }
        }
    }
}
