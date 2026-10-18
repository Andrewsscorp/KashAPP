package com.example.presentation.thirdparty.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ThirdPartyEntity
import com.example.presentation.thirdparty.model.ThirdPartyFormState
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy

/**
 * Resolves an icon imageVector based on stored iconName.
 */
fun resolveThirdPartyIcon(iconName: String, category: String): ImageVector {
    return when {
        iconName == "DirectionsCar" || category == "CONCESIONARIO" -> Icons.Default.DirectionsCar
        iconName == "Storefront" || iconName == "LocalGroceryStore" || category == "TIENDA_COMERCIO" -> Icons.Default.Storefront
        iconName == "LocalGasStation" -> Icons.Default.LocalGasStation
        iconName == "LocalShipping" -> Icons.Default.LocalShipping
        else -> Icons.Default.Person
    }
}

/**
 * Modal BottomSheet for Express (Fast 3-5 sec) or Full Enterprise Third-Party Creation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThirdPartyFormModalSheet(
    formState: ThirdPartyFormState,
    onUpdateForm: ((ThirdPartyFormState) -> ThirdPartyFormState) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (formState.isExpressMode) "Creación Rápida de Tercero" else "Registro Empresarial de Tercero",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = KashNavy
                    )
                    Text(
                        text = if (formState.isExpressMode) "Modo Express (3 campos esenciales)" else "Gestión completa con datos fiscales y bancarios",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF64748B))
                }
            }

            // Mode Toggle Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        onUpdateForm { it.copy(isExpressMode = !it.isExpressMode) }
                    }
                ) {
                    Text(
                        text = if (formState.isExpressMode) "⚙️ Pasar a Modo Completo" else "⚡ Cambiar a Modo Rápido",
                        color = Color(0xFF0284C7),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp
                    )
                }
            }

            // Error callout
            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    color = Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ $errorMessage",
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (formState.isExpressMode) {
                // ==========================================
                // MODO EXPRESS (RÁPIDO)
                // ==========================================
                OutlinedTextField(
                    value = formState.name,
                    onValueChange = { str -> onUpdateForm { it.copy(name = str) } },
                    label = { Text("Nombre / Razón Social *") },
                    placeholder = { Text("Ej: Automotores Toyota o Juan Pérez") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Document Type Selector Chips
                    Column(modifier = Modifier.weight(0.35f)) {
                        Text("Tipo Doc.", fontSize = 11.5.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            listOf("NIT", "CC", "CE").forEach { type ->
                                FilterChip(
                                    selected = formState.idType == type,
                                    onClick = { onUpdateForm { it.copy(idType = type) } },
                                    label = { Text(type, fontSize = 11.sp) },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = formState.idNumber,
                        onValueChange = { str -> onUpdateForm { it.copy(idNumber = str) } },
                        label = { Text("Número / Cédula / NIT *") },
                        placeholder = { Text("Ej: 12345678") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.65f)
                    )
                }

                // Categoría
                Text("Categoría / Rol", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "PROVEEDOR" to "Proveedor",
                        "CONCESIONARIO" to "Concesionario",
                        "TIENDA_COMERCIO" to "Tienda / Comercio",
                        "CLIENTE" to "Cliente"
                    ).forEach { (code, label) ->
                        FilterChip(
                            selected = formState.category == code,
                            onClick = { onUpdateForm { it.copy(category = code) } },
                            label = { Text(label, fontSize = 11.5.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = formState.primaryPhone,
                    onValueChange = { str -> onUpdateForm { it.copy(primaryPhone = str) } },
                    label = { Text("Teléfono / WhatsApp (Opcional)") },
                    placeholder = { Text("Ej: 300 123 4567") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

            } else {
                // ==========================================
                // MODO COMPLETO ENTERPRISE
                // ==========================================
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF8FAFC),
                    contentColor = Color(0xFF0284C7)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("General", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Contacto", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Financiero", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        // General Info
                        OutlinedTextField(
                            value = formState.name,
                            onValueChange = { str -> onUpdateForm { it.copy(name = str) } },
                            label = { Text("Nombre Completo / Razón Social *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = formState.commercialName,
                                onValueChange = { str -> onUpdateForm { it.copy(commercialName = str) } },
                                label = { Text("Nombre Comercial / Alias") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = formState.idType,
                                onValueChange = { str -> onUpdateForm { it.copy(idType = str.uppercase()) } },
                                label = { Text("Tipo") },
                                singleLine = true,
                                modifier = Modifier.weight(0.35f)
                            )
                            OutlinedTextField(
                                value = formState.idNumber,
                                onValueChange = { str -> onUpdateForm { it.copy(idNumber = str) } },
                                label = { Text("Número Identificación *") },
                                singleLine = true,
                                modifier = Modifier.weight(0.65f)
                            )
                            if (formState.idType.equals("NIT", ignoreCase = true)) {
                                OutlinedTextField(
                                    value = formState.verificationDigit,
                                    onValueChange = { str -> onUpdateForm { it.copy(verificationDigit = str.take(1)) } },
                                    label = { Text("DV") },
                                    singleLine = true,
                                    modifier = Modifier.width(55.dp)
                                )
                            }
                        }

                        // Persona y Categoría
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("NATURAL" to "Persona Natural", "JURIDICA" to "Persona Jurídica").forEach { (code, label) ->
                                FilterChip(
                                    selected = formState.personType == code,
                                    onClick = { onUpdateForm { it.copy(personType = code) } },
                                    label = { Text(label, fontSize = 11.5.sp) }
                                )
                            }
                        }
                    }

                    1 -> {
                        // Contact Info
                        OutlinedTextField(
                            value = formState.primaryPhone,
                            onValueChange = { str -> onUpdateForm { it.copy(primaryPhone = str) } },
                            label = { Text("Teléfono Principal") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = formState.secondaryPhone,
                            onValueChange = { str -> onUpdateForm { it.copy(secondaryPhone = str) } },
                            label = { Text("Teléfono Secundario (Opcional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = formState.email,
                            onValueChange = { str -> onUpdateForm { it.copy(email = str) } },
                            label = { Text("Correo Electrónico") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = formState.city,
                                onValueChange = { str -> onUpdateForm { it.copy(city = str) } },
                                label = { Text("Ciudad / Municipio") },
                                singleLine = true,
                                modifier = Modifier.weight(0.5f)
                            )
                            OutlinedTextField(
                                value = formState.address,
                                onValueChange = { str -> onUpdateForm { it.copy(address = str) } },
                                label = { Text("Dirección") },
                                singleLine = true,
                                modifier = Modifier.weight(0.5f)
                            )
                        }
                    }

                    2 -> {
                        // Financial Info
                        OutlinedTextField(
                            value = formState.paymentTermsDays,
                            onValueChange = { str -> onUpdateForm { it.copy(paymentTermsDays = str.filter { c -> c.isDigit() }) } },
                            label = { Text("Plazo de Pago Acordado (Días)") },
                            placeholder = { Text("0 = Contado, 30, 60...") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = formState.defaultBank,
                            onValueChange = { str -> onUpdateForm { it.copy(defaultBank = str) } },
                            label = { Text("Banco Predilecto") },
                            placeholder = { Text("Ej: Bancolombia, Davivienda") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = formState.bankAccountType,
                                onValueChange = { str -> onUpdateForm { it.copy(bankAccountType = str) } },
                                label = { Text("Tipo Cuenta") },
                                placeholder = { Text("Ahorros / Corriente") },
                                singleLine = true,
                                modifier = Modifier.weight(0.45f)
                            )
                            OutlinedTextField(
                                value = formState.bankAccountNumber,
                                onValueChange = { str -> onUpdateForm { it.copy(bankAccountNumber = str) } },
                                label = { Text("Número de Cuenta") },
                                singleLine = true,
                                modifier = Modifier.weight(0.55f)
                            )
                        }
                        OutlinedTextField(
                            value = formState.notes,
                            onValueChange = { str -> onUpdateForm { it.copy(notes = str) } },
                            label = { Text("Notas u Observaciones") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_save_third_party")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (formState.isExpressMode) "Guardar Tercero Rápido" else "Guardar Registro Completo",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Reusable Selector Field for anywhere in the app (e.g., in CreateFixedAssetScreen for supplier).
 */
@Composable
fun ThirdPartySelectorField(
    selectedThirdPartyName: String,
    onSelectThirdParty: (ThirdPartyEntity) -> Unit,
    thirdParties: List<ThirdPartyEntity>,
    onOpenCreateExpress: () -> Unit,
    label: String = "Proveedor / Concesionario",
    placeholder: String = "Seleccionar o crear tercero...",
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var searchFilter by remember { mutableStateOf("") }

    val filtered = remember(searchFilter, thirdParties) {
        if (searchFilter.isBlank()) thirdParties else {
            thirdParties.filter {
                it.name.contains(searchFilter, ignoreCase = true) ||
                (it.commercialName?.contains(searchFilter, ignoreCase = true) == true) ||
                it.idNumber.contains(searchFilter)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A)
        )
        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (selectedThirdPartyName.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0F2FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = selectedThirdPartyName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color(0xFF0F172A)
                        )
                    }
                } else {
                    Text(
                        text = placeholder,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color(0xFF64748B)
                )
            }
        }

        // Expanded Dropdown / Quick Picker List
        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Search bar inside picker
                    OutlinedTextField(
                        value = searchFilter,
                        onValueChange = { searchFilter = it },
                        placeholder = { Text("Buscar por nombre o documento...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick Create Button
                    Button(
                        onClick = {
                            expanded = false
                            onOpenCreateExpress()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0F2FE)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Crear Tercero Rápido", color = Color(0xFF0284C7), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // Third parties list
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (filtered.isEmpty()) {
                            Text(
                                text = "No se encontraron terceros.",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(8.dp)
                            )
                        } else {
                            filtered.forEach { tp ->
                                Surface(
                                    color = if (tp.displayName == selectedThirdPartyName) Color(0xFFF0F9FF) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expanded = false
                                            onSelectThirdParty(tp)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(tp.displayName, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = Color(0xFF0F172A))
                                            Text("${tp.fullDocument} • ${tp.category}", fontSize = 10.5.sp, color = Color(0xFF64748B))
                                        }
                                        if (tp.isFavorite) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
