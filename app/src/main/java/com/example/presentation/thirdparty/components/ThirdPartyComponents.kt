package com.example.presentation.thirdparty.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ThirdPartyCategoryEntity
import com.example.data.local.entity.ThirdPartyEntity
import com.example.presentation.thirdparty.model.ThirdPartyFormState

// ============================================================================
// KASHAPP PREMIUM BLUE & DARK NAVY ENTERPRISE PALETTE (ASSETS & THIRD PARTIES)
// ============================================================================
private val KashPrimaryBlue = Color(0xFF0284C7)
private val KashNavyBg = Color(0xFF07101E)
private val KashCardBg = Color(0xFF0B172B)
private val KashSurfaceItem = Color(0xFF0F1E36)
private val KashBorderColor = Color(0xFF132B4F)
private val KashBorderHighlight = Color(0xFF1E3A5F)
private val KashAccentCyan = Color(0xFF38BDF8)
private val KashTextPrimary = Color.White
private val KashTextSecondary = Color(0xFF8EA5C4)
private val KashMutedText = Color(0xFF64748B)
private val KashErrorRed = Color(0xFFEF4444)
private val KashSuccessGreen = Color(0xFF10B981)

/**
 * Resolves an icon ImageVector based on stored iconName and category.
 */
fun resolveThirdPartyIcon(iconName: String?, category: String?): ImageVector {
    val clean = (iconName ?: "").lowercase()
    val cat = (category ?: "").uppercase()
    return when {
        clean.contains("store") || clean.contains("tienda") || cat.contains("TIENDA") -> Icons.Default.Storefront
        clean.contains("car") || clean.contains("concesionario") || cat.contains("CONCESIONARIO") -> Icons.Default.DirectionsCar
        clean.contains("bank") || clean.contains("banco") || clean.contains("acreedor") || cat.contains("ACREEDOR") -> Icons.Default.AccountBalance
        clean.contains("group") || clean.contains("empleado") || cat.contains("EMPLEADO") -> Icons.Default.Groups
        clean.contains("credit") || clean.contains("card") -> Icons.Default.CreditCard
        clean.contains("phone") || clean.contains("call") -> Icons.Default.Call
        clean.contains("build") || clean.contains("taller") || cat.contains("TALLER") -> Icons.Default.Build
        clean.contains("bag") || clean.contains("comercio") -> Icons.Default.ShoppingBag
        clean.contains("work") || clean.contains("contratista") || cat.contains("CONTRATISTA") -> Icons.Default.AssignmentInd
        clean.contains("deal") || clean.contains("socio") -> Icons.Default.Handshake
        clean.contains("person") || cat.contains("CLIENTE") -> Icons.Default.Person
        cat.contains("PROVEEDOR") -> Icons.Default.Business
        else -> Icons.Default.Business
    }
}

/**
 * Grouped Inset Card (iOS Style) in enterprise blue palette.
 */
@Composable
fun IPhoneGroupedCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = KashAccentCyan,
                letterSpacing = 0.6.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
        Surface(
            color = KashCardBg,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, KashBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

/**
 * Inset Text Field Row (iOS Style) in enterprise blue palette.
 */
@Composable
fun IPhoneTextFieldRow(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    icon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = KashTextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = KashAccentCyan,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = placeholder,
                        fontSize = 13.sp,
                        color = KashMutedText
                    )
                },
                singleLine = true,
                keyboardOptions = keyboardOptions,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = KashTextPrimary,
                    unfocusedTextColor = KashTextPrimary
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KashTextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Reusable Dialog to create a new commercial role / category for third parties.
 * Persists in Room Database and updates the category list in real-time.
 */
@Composable
fun CreateThirdPartyCategoryDialog(
    onDismiss: () -> Unit,
    onSaveCategory: (name: String, code: String, iconName: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("Business") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val iconOptions = listOf(
        "Storefront" to Icons.Default.Storefront,
        "DirectionsCar" to Icons.Default.DirectionsCar,
        "Business" to Icons.Default.Business,
        "Person" to Icons.Default.Person,
        "AccountBalance" to Icons.Default.AccountBalance,
        "Groups" to Icons.Default.Groups,
        "Build" to Icons.Default.Build,
        "ShoppingBag" to Icons.Default.ShoppingBag,
        "AssignmentInd" to Icons.Default.AssignmentInd,
        "Handshake" to Icons.Default.Handshake
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KashCardBg,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F2B48)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = KashAccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Nuevo Rol / Categoría",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = KashTextPrimary
                    )
                    Text(
                        text = "Se guardará en la base de datos empresarial",
                        fontSize = 11.5.sp,
                        color = KashTextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage!!,
                        color = KashErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Nombre del Rol o Categoría *", color = KashTextSecondary) },
                    placeholder = { Text("Ej: Distribuidor Mayorista, Arrendador...", color = KashMutedText, fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = KashNavyBg,
                        unfocusedContainerColor = KashNavyBg,
                        focusedBorderColor = KashPrimaryBlue,
                        unfocusedBorderColor = KashBorderColor,
                        focusedTextColor = KashTextPrimary,
                        unfocusedTextColor = KashTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "ÍCONO REPRESENTATIVO",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = KashAccentCyan,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    iconOptions.forEach { (iconKey, iconVec) ->
                        val isSelected = selectedIcon == iconKey
                        Surface(
                            color = if (isSelected) KashPrimaryBlue else KashNavyBg,
                            border = BorderStroke(1.dp, if (isSelected) KashAccentCyan else KashBorderColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .size(42.dp)
                                .clickable { selectedIcon = iconKey }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = iconVec,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else KashTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isBlank()) {
                        errorMessage = "Por favor ingresa un nombre para el rol."
                        return@Button
                    }
                    val code = trimmed.uppercase().replace(Regex("[^A-Z0-9_]"), "_")
                    onSaveCategory(trimmed, code, selectedIcon, "#0284C7")
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = KashPrimaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Guardar Rol", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = KashTextSecondary)
            }
        }
    )
}

/**
 * Enterprise Form Modal BottomSheet (iOS style) for Third Party creation/editing.
 * Features Express Mode & Full Mode, dynamic role/category creation with Room persistence,
 * styled in dark navy blue palette with smooth transitions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThirdPartyFormModalSheet(
    formState: ThirdPartyFormState,
    onUpdateForm: ((ThirdPartyFormState) -> ThirdPartyFormState) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onImportContactClick: (() -> Unit)? = null,
    errorMessage: String? = null,
    availableCategories: List<ThirdPartyCategoryEntity> = emptyList(),
    onCreateCategory: ((name: String, code: String, iconName: String, colorHex: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedFullTab by remember { mutableIntStateOf(0) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }

    // Fallback default categories if empty
    val defaultCategoryTriples = listOf(
        Triple("Proveedor", "PROVEEDOR", "Storefront"),
        Triple("Concesionario", "CONCESIONARIO", "DirectionsCar"),
        Triple("Tienda / Comercio", "TIENDA_COMERCIO", "Business"),
        Triple("Cliente", "CLIENTE", "Person"),
        Triple("Acreedor", "ACREEDOR", "AccountBalance"),
        Triple("Empleado", "EMPLEADO", "Groups"),
        Triple("Taller / Servicio", "TALLER_SERVICIO", "Build"),
        Triple("Contratista", "CONTRATISTA", "AssignmentInd")
    )

    // Build the resolved list of categories to display
    val displayCategories = remember(availableCategories) {
        if (availableCategories.isNotEmpty()) {
            availableCategories.map { Triple(it.name, it.code, it.iconName) }
        } else {
            defaultCategoryTriples
        }
    }

    if (showCreateCategoryDialog) {
        CreateThirdPartyCategoryDialog(
            onDismiss = { showCreateCategoryDialog = false },
            onSaveCategory = { catName, catCode, catIcon, catColor ->
                onCreateCategory?.invoke(catName, catCode, catIcon, catColor)
                onUpdateForm { it.copy(category = catCode) }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KashNavyBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 38.dp, height = 4.5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF334155))
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // TOP NAVIGATION BAR
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_sheet_cancel")
                ) {
                    Text(
                        text = "Cancelar",
                        color = KashTextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (formState.isExpressMode) "Nuevo Tercero" else "Ficha de Tercero",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = KashTextPrimary
                    )
                    Text(
                        text = if (formState.isExpressMode) "Modo Express Rápido" else "Ficha Fiscal y Financiera",
                        fontSize = 11.5.sp,
                        color = KashAccentCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = KashPrimaryBlue),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_sheet_save")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Guardar",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // ==========================================
            // SEGMENTED MODE SELECTOR (IOS STYLE)
            // ==========================================
            Surface(
                color = KashCardBg,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, KashBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val expressSelected = formState.isExpressMode
                    // Option 1: Express
                    Surface(
                        color = if (expressSelected) KashPrimaryBlue else Color.Transparent,
                        shape = RoundedCornerShape(11.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onUpdateForm { it.copy(isExpressMode = true) }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = if (expressSelected) Color.White else KashTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Modo Rápido",
                                fontWeight = if (expressSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.5.sp,
                                color = if (expressSelected) Color.White else KashTextSecondary
                            )
                        }
                    }

                    // Option 2: Full
                    Surface(
                        color = if (!expressSelected) KashPrimaryBlue else Color.Transparent,
                        shape = RoundedCornerShape(11.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onUpdateForm { it.copy(isExpressMode = false) }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = if (!expressSelected) Color.White else KashTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Modo Completo",
                                fontWeight = if (!expressSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.5.sp,
                                color = if (!expressSelected) Color.White else KashTextSecondary
                            )
                        }
                    }
                }
            }

            // Error banner callout
            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    color = Color(0xFF2D1214),
                    border = BorderStroke(1.dp, Color(0xFF7F1D1D)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ $errorMessage",
                            color = Color(0xFFFCA5A5),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Quick Import from Phone Contacts Pill (if callback supplied)
            if (onImportContactClick != null) {
                Surface(
                    color = KashCardBg,
                    border = BorderStroke(1.dp, KashBorderHighlight),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onImportContactClick() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F2B48)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContactPhone,
                                    contentDescription = null,
                                    tint = KashAccentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Importar desde Contactos",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KashTextPrimary
                                )
                                Text(
                                    text = "Carga nombre, teléfono y correo en 1 toque",
                                    fontSize = 11.sp,
                                    color = KashTextSecondary
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = KashAccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ==========================================
            // CONTENT: EXPRESS OR FULL INSET FORMS
            // ==========================================
            if (formState.isExpressMode) {
                // ------------------------------------------
                // CARD 1: INFORMACIÓN ESENCIAL
                // ------------------------------------------
                IPhoneGroupedCard(title = "INFORMACIÓN ESENCIAL") {
                    // Nombre o Razón Social
                    IPhoneTextFieldRow(
                        value = formState.name,
                        onValueChange = { str -> onUpdateForm { it.copy(name = str) } },
                        label = "Nombre / Razón Social *",
                        placeholder = "Ej: Automotores Toyota o Juan Pérez",
                        icon = Icons.Default.Person,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )

                    HorizontalDivider(color = KashBorderColor, thickness = 1.dp)

                    // Tipo Documento + Número
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(
                            text = "TIPO DE DOCUMENTO *",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashTextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("CC", "NIT", "CE", "PASAPORTE", "RUT").forEach { type ->
                                val isSelected = formState.idType.equals(type, ignoreCase = true)
                                Surface(
                                    color = if (isSelected) KashPrimaryBlue else KashSurfaceItem,
                                    border = BorderStroke(1.dp, if (isSelected) KashAccentCyan else KashBorderColor),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.clickable {
                                        onUpdateForm { it.copy(idType = type) }
                                    }
                                ) {
                                    Text(
                                        text = type,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else KashTextSecondary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = KashBorderColor, thickness = 1.dp)

                    IPhoneTextFieldRow(
                        value = formState.idNumber,
                        onValueChange = { str -> onUpdateForm { it.copy(idNumber = str) } },
                        label = "Número de Documento / NIT *",
                        placeholder = "12345678",
                        icon = Icons.Default.CreditCard,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // DV if NIT
                    if (formState.idType.equals("NIT", ignoreCase = true)) {
                        HorizontalDivider(color = KashBorderColor, thickness = 1.dp)
                        IPhoneTextFieldRow(
                            value = formState.verificationDigit,
                            onValueChange = { str -> onUpdateForm { it.copy(verificationDigit = str.take(1)) } },
                            label = "Dígito de Verificación (DV)",
                            placeholder = "0 a 9",
                            icon = Icons.Default.Check,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    HorizontalDivider(color = KashBorderColor, thickness = 1.dp)

                    // Teléfono opcional
                    IPhoneTextFieldRow(
                        value = formState.primaryPhone,
                        onValueChange = { str -> onUpdateForm { it.copy(primaryPhone = str) } },
                        label = "Teléfono / WhatsApp (Opcional)",
                        placeholder = "Ej: 300 123 4567",
                        icon = Icons.Default.Call,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }

                // ------------------------------------------
                // CARD 2: CATEGORÍA / ROL COMERCIAL + CREAR ROL
                // ------------------------------------------
                IPhoneGroupedCard(title = "ROL O CATEGORÍA COMERCIAL") {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            displayCategories.forEach { (label, code, iconName) ->
                                val isSelected = formState.category.equals(code, ignoreCase = true)
                                Surface(
                                    color = if (isSelected) KashPrimaryBlue else KashSurfaceItem,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (isSelected) KashAccentCyan else KashBorderColor),
                                    modifier = Modifier.clickable {
                                        onUpdateForm { it.copy(category = code) }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = resolveThirdPartyIcon(iconName, code),
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else KashAccentCyan,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else KashTextPrimary
                                        )
                                    }
                                }
                            }

                            // Botón para crear un nuevo rol / categoría comercial en base de datos
                            Surface(
                                color = Color(0xFF0F2B48),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, KashPrimaryBlue),
                                modifier = Modifier.clickable { showCreateCategoryDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = KashAccentCyan,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "+ Nuevo Rol",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KashAccentCyan
                                    )
                                }
                            }
                        }
                    }
                }

            } else {
                // ==========================================
                // MODO COMPLETO CON TABS IOS
                // ==========================================
                Surface(
                    color = KashCardBg,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, KashBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("1. General", "2. Contacto", "3. Financiero").forEachIndexed { index, title ->
                            val isSel = selectedFullTab == index
                            Surface(
                                color = if (isSel) KashPrimaryBlue else Color.Transparent,
                                shape = RoundedCornerShape(11.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedFullTab = index }
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else KashTextSecondary,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                when (selectedFullTab) {
                    0 -> {
                        // General Info Card
                        IPhoneGroupedCard(title = "INFORMACIÓN GENERAL") {
                            IPhoneTextFieldRow(
                                value = formState.name,
                                onValueChange = { str -> onUpdateForm { it.copy(name = str) } },
                                label = "Nombre Completo / Razón Social *",
                                placeholder = "Nombre legal",
                                icon = Icons.Default.Person
                            )

                            HorizontalDivider(color = KashBorderColor, thickness = 1.dp)

                            IPhoneTextFieldRow(
                                value = formState.commercialName,
                                onValueChange = { str -> onUpdateForm { it.copy(commercialName = str) } },
                                label = "Nombre Comercial / Alias",
                                placeholder = "Ej: Concesionario Toyota Calle 100",
                                icon = Icons.Default.Storefront
                            )

                            HorizontalDivider(color = KashBorderColor, thickness = 1.dp)

                            // Persona Natural vs Jurídica
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                                Text(
                                    text = "TIPO DE PERSONA",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KashTextSecondary,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("NATURAL" to "Persona Natural", "JURIDICA" to "Persona Jurídica").forEach { (code, label) ->
                                        val isSel = formState.personType.equals(code, ignoreCase = true)
                                        Surface(
                                            color = if (isSel) KashPrimaryBlue else KashSurfaceItem,
                                            border = BorderStroke(1.dp, if (isSel) KashAccentCyan else KashBorderColor),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onUpdateForm { it.copy(personType = code) } }
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSel) Color.White else KashTextSecondary,
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = KashBorderColor, thickness = 1.dp)

                            IPhoneTextFieldRow(
                                value = formState.idNumber,
                                onValueChange = { str -> onUpdateForm { it.copy(idNumber = str) } },
                                label = "Número de Identificación / NIT *",
                                placeholder = "12345678",
                                icon = Icons.Default.CreditCard,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            if (formState.idType.equals("NIT", ignoreCase = true)) {
                                HorizontalDivider(color = KashBorderColor, thickness = 1.dp)
                                IPhoneTextFieldRow(
                                    value = formState.verificationDigit,
                                    onValueChange = { str -> onUpdateForm { it.copy(verificationDigit = str.take(1)) } },
                                    label = "Dígito de Verificación (DV)",
                                    placeholder = "0 a 9",
                                    icon = Icons.Default.Check,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }

                        // Category Card in Full Mode
                        IPhoneGroupedCard(title = "ROL O CATEGORÍA COMERCIAL") {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    displayCategories.forEach { (label, code, iconName) ->
                                        val isSelected = formState.category.equals(code, ignoreCase = true)
                                        Surface(
                                            color = if (isSelected) KashPrimaryBlue else KashSurfaceItem,
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, if (isSelected) KashAccentCyan else KashBorderColor),
                                            modifier = Modifier.clickable {
                                                onUpdateForm { it.copy(category = code) }
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = resolveThirdPartyIcon(iconName, code),
                                                    contentDescription = null,
                                                    tint = if (isSelected) Color.White else KashAccentCyan,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = label,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else KashTextPrimary
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        color = Color(0xFF0F2B48),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, KashPrimaryBlue),
                                        modifier = Modifier.clickable { showCreateCategoryDialog = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = KashAccentCyan,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "+ Nuevo Rol",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = KashAccentCyan
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Contacto y Ubicación
                        IPhoneGroupedCard(title = "CONTACTO Y UBICACIÓN") {
                            IPhoneTextFieldRow(
                                value = formState.primaryPhone,
                                onValueChange = { str -> onUpdateForm { it.copy(primaryPhone = str) } },
                                label = "Teléfono Principal",
                                placeholder = "Ej: (601) 745-0000",
                                icon = Icons.Default.Call,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )

                            HorizontalDivider(color = KashBorderColor, thickness = 1.dp)

                            IPhoneTextFieldRow(
                                value = formState.email,
                                onValueChange = { str -> onUpdateForm { it.copy(email = str) } },
                                label = "Correo Electrónico",
                                placeholder = "contacto@empresa.com",
                                icon = Icons.Default.Email,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )

                            HorizontalDivider(color = KashBorderColor, thickness = 1.dp)

                            IPhoneTextFieldRow(
                                value = formState.city,
                                onValueChange = { str -> onUpdateForm { it.copy(city = str) } },
                                label = "Ciudad / Municipio",
                                placeholder = "Ej: Bogotá, Medellín, Cali",
                                icon = Icons.Default.LocationOn
                            )

                            HorizontalDivider(color = KashBorderColor, thickness = 1.dp)

                            IPhoneTextFieldRow(
                                value = formState.address,
                                onValueChange = { str -> onUpdateForm { it.copy(address = str) } },
                                label = "Dirección Física",
                                placeholder = "Calle 100 # 15-20",
                                icon = Icons.Default.LocationOn
                            )
                        }
                    }

                    2 -> {
                        // Datos Financieros y de Pago
                        IPhoneGroupedCard(title = "DATOS BANCARIOS Y CONDICIONES") {
                            IPhoneTextFieldRow(
                                value = formState.paymentTermsDays,
                                onValueChange = { str -> onUpdateForm { it.copy(paymentTermsDays = str.filter { c -> c.isDigit() }) } },
                                label = "Días de Crédito / Plazo de Pago",
                                placeholder = "0 = Contado, 30, 60 días",
                                icon = Icons.Default.CreditCard,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            HorizontalDivider(color = KashBorderColor, thickness = 1.dp)

                            IPhoneTextFieldRow(
                                value = formState.defaultBank,
                                onValueChange = { str -> onUpdateForm { it.copy(defaultBank = str) } },
                                label = "Banco Predilecto",
                                placeholder = "Ej: Bancolombia, Davivienda",
                                icon = Icons.Default.AccountBalance
                            )

                            HorizontalDivider(color = KashBorderColor, thickness = 1.dp)

                            IPhoneTextFieldRow(
                                value = formState.bankAccountNumber,
                                onValueChange = { str -> onUpdateForm { it.copy(bankAccountNumber = str) } },
                                label = "Número de Cuenta Bancaria",
                                placeholder = "Número de cuenta para transferencias",
                                icon = Icons.Default.CreditCard,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Enterprise Selector Pop-up Dialog for Activos Fijos or other transaction forms.
 * Fully consistent with KashApp's dark navy & blue enterprise design.
 */
@Composable
fun SelectThirdPartyPopUpDialog(
    thirdParties: List<ThirdPartyEntity>,
    onSelectThirdParty: (ThirdPartyEntity) -> Unit,
    onOpenCreateNew: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(searchQuery, thirdParties) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) thirdParties else {
            thirdParties.filter {
                it.name.lowercase().contains(q) ||
                (it.commercialName?.lowercase()?.contains(q) == true) ||
                it.idNumber.contains(q) ||
                it.category.lowercase().contains(q)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KashCardBg,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F2B48)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = KashAccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Seleccionar Tercero",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.5.sp,
                            color = KashTextPrimary
                        )
                        Text(
                            text = "${thirdParties.size} registrados en la base de datos",
                            fontSize = 11.5.sp,
                            color = KashTextSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = KashTextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por nombre, NIT, rol...", fontSize = 12.5.sp, color = KashMutedText) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = KashAccentCyan) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = KashTextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = KashNavyBg,
                        unfocusedContainerColor = KashNavyBg,
                        focusedBorderColor = KashPrimaryBlue,
                        unfocusedBorderColor = KashBorderColor,
                        focusedTextColor = KashTextPrimary,
                        unfocusedTextColor = KashTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Button to create a new third party right from the dialog
                Button(
                    onClick = onOpenCreateNew,
                    colors = ButtonDefaults.buttonColors(containerColor = KashPrimaryBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Crear Nuevo Tercero", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                HorizontalDivider(color = KashBorderColor)

                // List of filtered third parties
                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (thirdParties.isEmpty())
                                "No hay terceros registrados aún. Pulsa '+ Crear Nuevo Tercero' o escribe el nombre directamente."
                            else
                                "No se encontraron terceros para '$searchQuery'. Puedes crearlo o escribir el nombre libremente.",
                            fontSize = 12.5.sp,
                            color = KashTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filtered.forEach { tp ->
                            Surface(
                                color = KashSurfaceItem,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, KashBorderHighlight),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectThirdParty(tp)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(KashNavyBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = resolveThirdPartyIcon(tp.iconName, tp.category),
                                                contentDescription = null,
                                                tint = KashAccentCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = tp.displayName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = KashTextPrimary
                                            )
                                            Text(
                                                text = "${tp.fullDocument} • ${tp.category}",
                                                fontSize = 11.sp,
                                                color = KashTextSecondary
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Seleccionar",
                                        tint = KashAccentCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Listo", fontWeight = FontWeight.Bold, color = KashAccentCyan)
            }
        },
        modifier = modifier
    )
}

/**
 * Enterprise Detail Dialog for viewing and acting on an inspected Third Party.
 * Consistent dark navy & blue palette.
 */
@Composable
fun ThirdPartyDetailDialog(
    thirdParty: ThirdPartyEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: (Boolean) -> Unit,
    onExportPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KashCardBg,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(KashAccentCyan, KashPrimaryBlue)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = resolveThirdPartyIcon(thirdParty.iconName, thirdParty.category),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = thirdParty.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = KashTextPrimary
                        )
                        Text(
                            text = thirdParty.displayCode,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KashAccentCyan
                        )
                    }
                }

                IconButton(onClick = { onToggleFavorite(!thirdParty.isFavorite) }) {
                    Icon(
                        imageVector = if (thirdParty.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorito",
                        tint = if (thirdParty.isFavorite) Color(0xFFEAB308) else KashTextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category & Type Badges
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = Color(0xFF0F2B48),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = thirdParty.category,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashAccentCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = KashSurfaceItem,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (thirdParty.personType == "JURIDICA") "Persona Jurídica" else "Persona Natural",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = KashTextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = KashBorderColor)

                // Legal Name
                Column {
                    Text(text = "Razón Social / Nombre Legal", fontSize = 11.sp, color = KashTextSecondary)
                    Text(text = thirdParty.name, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = KashTextPrimary)
                }

                // Document
                Column {
                    Text(text = "Identificación Fiscal", fontSize = 11.sp, color = KashTextSecondary)
                    Text(text = thirdParty.fullDocument, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = KashTextPrimary)
                }

                // Phone with direct Call action
                val phone = thirdParty.primaryPhone
                if (!phone.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Teléfono / Contacto", fontSize = 11.sp, color = KashTextSecondary)
                            Text(text = phone, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = KashTextPrimary)
                        }
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Call, contentDescription = "Llamar", tint = KashAccentCyan)
                        }
                    }
                }

                // Email with direct mail action
                val email = thirdParty.email
                if (!email.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Correo Electrónico", fontSize = 11.sp, color = KashTextSecondary)
                            Text(text = email, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = KashTextPrimary)
                        }
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Email, contentDescription = "Enviar Correo", tint = KashAccentCyan)
                        }
                    }
                }

                // Payment terms & Bank
                if (thirdParty.paymentTermsDays > 0 || !thirdParty.defaultBank.isNullOrBlank()) {
                    HorizontalDivider(color = KashBorderColor)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(text = "Condición de Pago", fontSize = 11.sp, color = KashTextSecondary)
                            Text(
                                text = if (thirdParty.paymentTermsDays == 0) "Contado" else "${thirdParty.paymentTermsDays} días",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                                color = KashTextPrimary
                            )
                        }
                        val bank = thirdParty.defaultBank
                        if (!bank.isNullOrBlank()) {
                            Column {
                                Text(text = "Banco Predilecto", fontSize = 11.sp, color = KashTextSecondary)
                                Text(text = bank, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = KashTextPrimary)
                            }
                        }
                    }
                }

                // Action buttons
                HorizontalDivider(color = KashBorderColor)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onEdit,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2B48)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = KashAccentCyan, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Editar", color = KashAccentCyan, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }

                    Button(
                        onClick = onExportPdf,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2B48)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = KashAccentCyan, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF", color = KashAccentCyan, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }

                    IconButton(
                        onClick = { showDeleteConfirm = true }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = KashErrorRed)
                    }
                }

                if (showDeleteConfirm) {
                    Surface(
                        color = Color(0xFF2D1214),
                        border = BorderStroke(1.dp, Color(0xFF7F1D1D)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("¿Eliminar este tercero?", color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            Text("No se borrarán los activos ni movimientos históricos asociados.", color = Color(0xFFFECACA), fontSize = 11.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        onDelete()
                                        showDeleteConfirm = false
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = KashErrorRed),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Sí, Eliminar", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text("Cancelar", color = KashTextSecondary, fontSize = 11.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = KashAccentCyan, fontWeight = FontWeight.Bold)
            }
        },
        modifier = modifier
    )
}
