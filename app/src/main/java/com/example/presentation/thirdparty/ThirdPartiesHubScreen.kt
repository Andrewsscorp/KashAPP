package com.example.presentation.thirdparty

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.entity.ThirdPartyEntity
import com.example.presentation.thirdparty.components.ThirdPartyDetailDialog
import com.example.presentation.thirdparty.components.ThirdPartyFormModalSheet
import com.example.presentation.thirdparty.components.resolveThirdPartyIcon
import com.example.presentation.thirdparty.model.ThirdPartyCategoryFilter
import com.example.presentation.thirdparty.model.ThirdPartyFormState

private val PrimaryBlue = Color(0xFF0284C7)
private val DarkNavy = Color(0xFF0F172A)
private val LightIce = Color(0xFFF0F9FF)
private val CardBg = Color.White
private val BorderStrokeColor = Color(0xFFE2E8F0)
private val TextMuted = Color(0xFF64748B)

/**
 * Enterprise Central Hub Screen for Third Parties (Terceros, Proveedores, Concesionarios y Clientes).
 * Features real-time KPIs, multi-tab analytics, contact import with runtime permission handling,
 * PDF reports, search, filters, and fluid animations.
 */
@Composable
fun ThirdPartiesHubScreen(
    viewModel: ThirdPartyViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }

    BackHandler { onDismiss() }

    // Contact Picker
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

                        if (contactId != null) {
                            try {
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
                            } catch (_: Exception) {}

                            try {
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
                            } catch (_: Exception) {}
                        }
                    }
                }

                if (contactName.isNotBlank()) {
                    viewModel.onImportFromContact(contactName, contactPhone, contactEmail)
                    snackbarMessage = "Contacto '$contactName' importado con éxito"
                }
            } catch (e: Exception) {
                snackbarMessage = "No se pudo leer el contacto: ${e.message}"
            }
        }
    }

    // Permission Launcher for READ_CONTACTS
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contactPickerLauncher.launch(null)
        } else {
            showPermissionDeniedDialog = true
        }
    }

    val triggerContactPicker: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            contactPickerLauncher.launch(null)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LightIce)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ==========================================
        // 1. TOP APP BAR (ENTERPRISE HERO HEADER)
        // ==========================================
        Surface(
            color = Color.White,
            shadowElevation = 2.dp,
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = DarkNavy
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Gestión de Terceros",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkNavy
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFE0F2FE),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${uiState.thirdParties.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Directorio comercial, proveedores y operaciones",
                                fontSize = 11.5.sp,
                                color = TextMuted
                            )
                        }
                    }

                    // Action Button: + Nuevo
                    Button(
                        onClick = { viewModel.onOpenCreateDialog(isExpress = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_hub_new_third_party")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Nuevo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Sub-Actions Bar: Importar Contacto & Exportar Directorio PDF
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = triggerContactPicker,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PrimaryBlue),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Icon(Icons.Default.ContactPhone, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Importar Contacto", fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            val first = uiState.thirdParties.firstOrNull()
                            if (first != null) {
                                viewModel.onExportPdf(context, first) { msg ->
                                    snackbarMessage = msg
                                }
                            } else {
                                snackbarMessage = "No hay terceros registrados para exportar."
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportar PDF", fontSize = 12.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // ==========================================
        // 2. METRIC KPIS CAROUSEL / ROW
        // ==========================================
        val totalCount = uiState.thirdParties.size
        val providersCount = uiState.thirdParties.count { it.category.equals("PROVEEDOR", ignoreCase = true) || it.category.equals("CONCESIONARIO", ignoreCase = true) }
        val merchantsCount = uiState.thirdParties.count { it.category.equals("TIENDA_COMERCIO", ignoreCase = true) }
        val favoritesCount = uiState.thirdParties.count { it.isFavorite }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HubKpiCard(
                title = "Total Terceros",
                count = totalCount,
                icon = Icons.Default.Groups,
                gradient = listOf(Color(0xFF0284C7), Color(0xFF0369A1))
            )
            HubKpiCard(
                title = "Proveedores",
                count = providersCount,
                icon = Icons.Default.DirectionsCar,
                gradient = listOf(Color(0xFF4F46E5), Color(0xFF3730A3))
            )
            HubKpiCard(
                title = "Tiendas",
                count = merchantsCount,
                icon = Icons.Default.Storefront,
                gradient = listOf(Color(0xFF16A34A), Color(0xFF15803D))
            )
            HubKpiCard(
                title = "Favoritos",
                count = favoritesCount,
                icon = Icons.Default.Star,
                gradient = listOf(Color(0xFFEAB308), Color(0xFFCA8A04))
            )
        }

        // ==========================================
        // 3. SEGMENTED HUB TABS (IOS STYLE)
        // ==========================================
        Surface(
            color = Color(0xFFE2E8F0),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf("👥 Directorio", "📊 Operaciones", "📈 Estadísticas")
                tabs.forEachIndexed { index, label ->
                    val isSelected = selectedTab == index
                    Surface(
                        color = if (isSelected) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(11.dp),
                        shadowElevation = if (isSelected) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = index }
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) PrimaryBlue else TextMuted,
                            modifier = Modifier.padding(vertical = 7.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // Feedback Banner
        if (!snackbarMessage.isNullOrBlank()) {
            Surface(
                color = Color(0xFFDCFCE7),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = snackbarMessage ?: "",
                        fontSize = 12.5.sp,
                        color = Color(0xFF15803D),
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = { snackbarMessage = null }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF15803D), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // ==========================================
        // 4. ANIMATED TAB CONTENT
        // ==========================================
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "hub_tab_transition",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { tabIndex ->
            when (tabIndex) {
                0 -> {
                    // TAB 0: DIRECTORIO GENERAL CON BUSCADOR Y FILTROS
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search & Filters Header
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChange(it) },
                                placeholder = { Text("Buscar por nombre, NIT, teléfono, ciudad...", fontSize = 12.5.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryBlue) },
                                trailingIcon = {
                                    if (uiState.searchQuery.isNotBlank()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                            Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = TextMuted)
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = BorderStrokeColor
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Filter Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ThirdPartyCategoryFilter.entries.forEach { filter ->
                                    FilterChip(
                                        selected = uiState.selectedFilter == filter,
                                        onClick = { viewModel.onSelectFilter(filter) },
                                        label = { Text(filter.label, fontSize = 11.5.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFE0F2FE),
                                            selectedLabelColor = PrimaryBlue
                                        )
                                    )
                                }
                            }
                        }

                        // Third Parties List
                        if (uiState.isLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = PrimaryBlue)
                            }
                        } else if (uiState.filteredList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "No se encontraron terceros registrados",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = DarkNavy
                                    )
                                    Text(
                                        text = "Usa el botón '+ Nuevo' o importa desde tus contactos.",
                                        fontSize = 12.5.sp,
                                        color = TextMuted,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(uiState.filteredList, key = { it.id }) { item ->
                                    HubThirdPartyCard(
                                        entity = item,
                                        onToggleFavorite = { viewModel.onToggleFavorite(item.id, !item.isFavorite) },
                                        onClick = { viewModel.onSelectThirdPartyForDetail(item) },
                                        onExportPdf = {
                                            viewModel.onExportPdf(context, item) { msg ->
                                                snackbarMessage = msg
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: OPERACIONES Y ACTIVOS VINCULADOS
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "OPERACIONES Y VINCULACIONES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            letterSpacing = 0.8.sp
                        )

                        uiState.thirdParties.forEach { tp ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, BorderStrokeColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(tp.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkNavy)
                                        Surface(
                                            color = Color(0xFFE0F2FE),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = tp.category,
                                                color = PrimaryBlue,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${tp.fullDocument} • Estado: Activo en Directorio",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                    if (tp.paymentTermsDays > 0) {
                                        Text(
                                            text = "Condición de crédito: ${tp.paymentTermsDays} días plazo acordado",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0284C7)
                                        )
                                    }
                                    if (!tp.defaultBank.isNullOrBlank()) {
                                        Text(
                                            text = "Banco: ${tp.defaultBank} • Cuenta: ${tp.bankAccountNumber ?: "Registrada"}",
                                            fontSize = 11.5.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: ESTADÍSTICAS Y DISTRIBUCIÓN
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "ANÁLISIS Y COMPOSICIÓN DEL DIRECTORIO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            letterSpacing = 0.8.sp
                        )

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BorderStrokeColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Composición por Tipo de Persona", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkNavy)

                                val naturalCount = uiState.thirdParties.count { it.personType == "NATURAL" }
                                val juridicaCount = uiState.thirdParties.count { it.personType == "JURIDICA" }
                                val safeTotal = if (totalCount > 0) totalCount.toFloat() else 1f

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Persona Natural: $naturalCount", fontSize = 12.5.sp, color = DarkNavy)
                                    Text("${((naturalCount / safeTotal) * 100).toInt()}%", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                }
                                LinearProgressIndicator(
                                    progress = { naturalCount / safeTotal },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                    color = PrimaryBlue,
                                    trackColor = Color(0xFFE2E8F0)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Persona Jurídica (Empresas): $juridicaCount", fontSize = 12.5.sp, color = DarkNavy)
                                    Text("${((juridicaCount / safeTotal) * 100).toInt()}%", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                                }
                                LinearProgressIndicator(
                                    progress = { juridicaCount / safeTotal },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                    color = Color(0xFF4F46E5),
                                    trackColor = Color(0xFFE2E8F0)
                                )
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BorderStrokeColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Resumen de Información Bancaria", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkNavy)
                                val withBank = uiState.thirdParties.count { !it.defaultBank.isNullOrBlank() || !it.bankAccountNumber.isNullOrBlank() }
                                Text("• Terceros con cuenta bancaria registrada: $withBank de $totalCount", fontSize = 12.5.sp, color = TextMuted)
                                val withCredit = uiState.thirdParties.count { it.paymentTermsDays > 0 }
                                Text("• Terceros con plazo de crédito acordado: $withCredit", fontSize = 12.5.sp, color = TextMuted)
                                val withPhone = uiState.thirdParties.count { !it.primaryPhone.isNullOrBlank() }
                                Text("• Terceros con teléfono directo: $withPhone", fontSize = 12.5.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet for Creation / Edit (iOS styled in KashApp blues)
    if (uiState.showCreateDialog) {
        ThirdPartyFormModalSheet(
            formState = uiState.formState,
            onUpdateForm = { viewModel.onUpdateForm(it) },
            onSave = {
                viewModel.onSaveThirdParty {
                    snackbarMessage = "Tercero guardado con éxito"
                }
            },
            onDismiss = { viewModel.onDismissCreateDialog() },
            onImportContactClick = triggerContactPicker,
            errorMessage = uiState.errorMessage
        )
    }

    // Detail Dialog (iOS styled)
    uiState.selectedThirdPartyForDetail?.let { detail ->
        ThirdPartyDetailDialog(
            thirdParty = detail,
            onDismiss = { viewModel.onSelectThirdPartyForDetail(null) },
            onEdit = {
                viewModel.onOpenCreateDialog(isExpress = false, prefilledState = ThirdPartyFormState.fromEntity(detail))
                viewModel.onSelectThirdPartyForDetail(null)
            },
            onDelete = {
                viewModel.onDeleteThirdParty(detail.id)
                snackbarMessage = "Tercero eliminado"
            },
            onToggleFavorite = { isFav ->
                viewModel.onToggleFavorite(detail.id, isFav)
            },
            onExportPdf = {
                viewModel.onExportPdf(context, detail) { msg ->
                    snackbarMessage = msg
                }
            }
        )
    }

    // Dialog for Contact Permission Required / Denied
    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContactPhone, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Permiso de Contactos Requerido", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkNavy)
                }
            },
            text = {
                Text(
                    text = "Para seleccionar e importar automáticamente el nombre, teléfono y correo de tus contactos a la gestión de terceros, autoriza el permiso de lectura de contactos.",
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDeniedDialog = false
                        try {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Abrir Ajustes", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            }
        )
    }
}

/**
 * Animated KPI Card for Hub Screen.
 */
@Composable
private fun HubKpiCard(
    title: String,
    count: Int,
    icon: ImageVector,
    gradient: List<Color>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .width(135.dp)
            .height(84.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradient))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "$count",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Hub Third Party Item Card with animations and direct contact triggers.
 */
@Composable
private fun HubThirdPartyCard(
    entity: ThirdPartyEntity,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    onExportPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "card_scale"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderStrokeColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable {
                onClick()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFE0F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = resolveThirdPartyIcon(entity.iconName, entity.category),
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = entity.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = DarkNavy
                    )
                    Text(
                        text = "${entity.fullDocument} • ${entity.category}",
                        fontSize = 11.5.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )

                    val phone = entity.primaryPhone
                    if (!phone.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = phone,
                                fontSize = 11.sp,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Quick Actions: Star Favorite & PDF
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExportPdf) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(19.dp)
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
