package com.example.features.settings.presentation.screens

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ThirdPartyEntity
import com.example.presentation.thirdparty.ThirdPartyViewModel
import com.example.presentation.thirdparty.components.ThirdPartyFormModalSheet
import com.example.presentation.thirdparty.components.resolveThirdPartyIcon
import com.example.presentation.thirdparty.model.ThirdPartyCategoryFilter
import com.example.presentation.thirdparty.model.ThirdPartyFormState
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy

@Composable
fun SettingsThirdPartiesSubScreen(
    viewModel: ThirdPartyViewModel,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Contact Picker via Android's native contract (Zero permission prompts required)
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri: Uri? ->
        if (contactUri != null) {
            try {
                var contactName = ""
                var contactPhone: String? = null
                var contactEmail: String? = null

                context.contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                        if (nameIndex != -1) contactName = cursor.getString(nameIndex) ?: ""
                        val contactId = if (idIndex != -1) cursor.getString(idIndex) else null

                        // Query Phone number
                        if (contactId != null) {
                            context.contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                arrayOf(contactId),
                                null
                            )?.use { phoneCursor ->
                                if (phoneCursor.moveToFirst()) {
                                    val phoneIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    if (phoneIdx != -1) contactPhone = phoneCursor.getString(phoneIdx)
                                }
                            }

                            // Query Email
                            context.contentResolver.query(
                                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                                null,
                                "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                                arrayOf(contactId),
                                null
                            )?.use { emailCursor ->
                                if (emailCursor.moveToFirst()) {
                                    val emailIdx = emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)
                                    if (emailIdx != -1) contactEmail = emailCursor.getString(emailIdx)
                                }
                            }
                        }
                    }
                }

                if (contactName.isNotBlank()) {
                    viewModel.onImportFromContact(contactName, contactPhone, contactEmail)
                }
            } catch (e: Exception) {
                onShowMessage("No se pudo leer el contacto: ${e.message}")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Top Action Bar
        Surface(
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = KashNavy)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("Terceros y Contactos", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = KashNavy)
                            Text("Gestión de proveedores, concesionarios y comercios", fontSize = 11.5.sp, color = Color(0xFF64748B))
                        }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Buscar por nombre, documento, teléfono...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color(0xFF94A3B8))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF0284C7),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThirdPartyCategoryFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = uiState.selectedFilter == filter,
                            onClick = { viewModel.onSelectFilter(filter) },
                            label = { Text(filter.label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE0F2FE),
                                selectedLabelColor = Color(0xFF0284C7)
                            )
                        )
                    }
                }

                // Action Buttons: + Nuevo Tercero & Importar Contacto
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.onOpenCreateDialog(isExpress = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_add_third_party")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Nuevo Tercero", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = { contactPickerLauncher.launch(null) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF0284C7)),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_import_contact")
                    ) {
                        Icon(Icons.Default.ContactPhone, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Importar Contacto", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF0284C7))
                    }
                }
            }
        }

        // Content Area: List of Third Parties
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF0284C7))
            }
        } else if (uiState.filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔍", fontSize = 42.sp)
                    Text("No se encontraron terceros", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KashNavy)
                    Text(
                        text = if (uiState.searchQuery.isNotBlank()) "Intenta con otro término de búsqueda." else "Crea un tercero con '+ Nuevo Tercero' o importa desde tus contactos.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.filteredList, key = { it.id }) { item ->
                    ThirdPartyCard(
                        entity = item,
                        onToggleFavorite = { viewModel.onToggleFavorite(item.id, !item.isFavorite) },
                        onClick = { viewModel.onSelectThirdPartyForDetail(item) },
                        onExportPdf = {
                            viewModel.onExportPdf(context, item) { msg ->
                                onShowMessage(msg)
                            }
                        }
                    )
                }
            }
        }
    }

    // Modal Sheet for Creation
    if (uiState.showCreateDialog) {
        ThirdPartyFormModalSheet(
            formState = uiState.formState,
            onUpdateForm = { viewModel.onUpdateForm(it) },
            onSave = {
                viewModel.onSaveThirdParty {
                    onShowMessage("Tercero guardado correctamente")
                }
            },
            onDismiss = { viewModel.onDismissCreateDialog() },
            errorMessage = uiState.errorMessage
        )
    }

    // Detail Dialog
    uiState.selectedThirdPartyForDetail?.let { detail ->
        ThirdPartyDetailDialog(
            entity = detail,
            onDismiss = { viewModel.onSelectThirdPartyForDetail(null) },
            onEdit = {
                viewModel.onOpenCreateDialog(isExpress = false, prefilledState = ThirdPartyFormState.fromEntity(detail))
                viewModel.onSelectThirdPartyForDetail(null)
            },
            onDelete = {
                viewModel.onDeleteThirdParty(detail.id)
                onShowMessage("Tercero eliminado")
            },
            onExportPdf = {
                viewModel.onExportPdf(context, detail) { msg ->
                    onShowMessage(msg)
                }
            }
        )
    }
}

/**
 * Individual Card representing a Third Party in the list.
 */
@Composable
fun ThirdPartyCard(
    entity: ThirdPartyEntity,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    onExportPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Box with color
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0F2FE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = resolveThirdPartyIcon(entity.iconName, entity.category),
                    contentDescription = null,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entity.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = Color(0xFF0F172A)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = entity.fullDocument,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (entity.category) {
                            "CONCESIONARIO" -> Color(0xFFEEF2FF)
                            "TIENDA_COMERCIO" -> Color(0xFFFEF3C7)
                            "PROVEEDOR" -> Color(0xFFDCFCE7)
                            else -> Color(0xFFF1F5F9)
                        }
                    ) {
                        Text(
                            text = entity.category,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (entity.category) {
                                "CONCESIONARIO" -> Color(0xFF4F46E5)
                                "TIENDA_COMERCIO" -> Color(0xFFD97706)
                                "PROVEEDOR" -> Color(0xFF16A34A)
                                else -> Color(0xFF64748B)
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (!entity.primaryPhone.isNullOrBlank() || !entity.city.isNullOrBlank()) {
                    Text(
                        text = listOfNotNull(entity.primaryPhone, entity.city).joinToString(" • "),
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Quick Actions: PDF and Favorite
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExportPdf) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (entity.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorito",
                        tint = if (entity.isFavorite) Color(0xFFEAB308) else Color(0xFF94A3B8),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * Detailed Information Dialog for an inspected Third Party.
 */
@Composable
fun ThirdPartyDetailDialog(
    entity: ThirdPartyEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExportPdf: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(entity.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KashNavy)
                    Text("Código: ${entity.displayCode}", fontSize = 11.5.sp, color = Color(0xFF64748B))
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF64748B))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HorizontalDivider(color = Color(0xFFF1F5F9))

                DetailItemRow("Documento:", entity.fullDocument)
                DetailItemRow("Tipo Persona:", if (entity.personType == "JURIDICA") "Jurídica" else "Natural")
                DetailItemRow("Categoría:", entity.category)
                DetailItemRow("Teléfono:", entity.primaryPhone)
                DetailItemRow("Teléfono 2:", entity.secondaryPhone)
                DetailItemRow("Correo:", entity.email)
                DetailItemRow("Ciudad:", entity.city)
                DetailItemRow("Dirección:", entity.address)
                DetailItemRow("Plazo de Pago:", "${entity.paymentTermsDays} días ${if (entity.paymentTermsDays == 0) "(Contado)" else ""}")
                DetailItemRow("Banco:", entity.defaultBank)
                DetailItemRow("Cuenta:", if (!entity.bankAccountNumber.isNullOrBlank()) "${entity.bankAccountType ?: ""} ${entity.bankAccountNumber}" else null)
                DetailItemRow("Notas:", entity.notes)
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onExportPdf,
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onEdit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Eliminar", color = Color(0xFFDC2626), fontSize = 12.sp)
            }
        }
    )
}

@Composable
private fun DetailItemRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = Color(0xFF64748B))
        Text(value, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, color = Color(0xFF0F172A))
    }
}
