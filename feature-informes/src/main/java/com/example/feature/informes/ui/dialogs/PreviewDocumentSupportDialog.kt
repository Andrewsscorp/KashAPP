package com.example.feature.informes.ui.dialogs

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.feature.informes.domain.model.DocumentSupportItem
import com.example.feature.informes.ui.components.InformesFormatters

@Composable
fun PreviewDocumentSupportDialog(
    doc: DocumentSupportItem,
    currency: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Visor de Soporte",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF94A3B8))
                    }
                }

                Text(
                    text = doc.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tipo:", color = Color(0xFF64748B), fontSize = 12.sp)
                            Text(doc.type.label, color = Color(0xFFCBD5E1), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Formato:", color = Color(0xFF64748B), fontSize = 12.sp)
                            Text("${doc.fileFormat.extension} (${doc.fileSizeFormatted})", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Fecha Emisión:", color = Color(0xFF64748B), fontSize = 12.sp)
                            Text(doc.date, color = Color(0xFFCBD5E1), fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Vínculo:", color = Color(0xFF64748B), fontSize = 12.sp)
                            Text("${doc.relatedEntityName} (${doc.relatedEntityType})", color = Color(0xFF38BDF8), fontSize = 12.sp)
                        }
                        doc.relatedAmount?.let { amt ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Monto:", color = Color(0xFF64748B), fontSize = 12.sp)
                                Text(InformesFormatters.formatMoney(amt, currency), color = Color(0xFF34D399), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Estado:", color = Color(0xFF64748B), fontSize = 12.sp)
                            Text(doc.status.label, color = Color(0xFFFBBF24), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (doc.notes.isNotBlank()) {
                    Text("Notas:", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(doc.notes, color = Color(0xFFCBD5E1), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Soporte Financiero: ${doc.title}\nTipo: ${doc.type.label}\nFecha: ${doc.date}\nVínculo: ${doc.relatedEntityName}")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartir Soporte"))
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compartir Soporte", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
