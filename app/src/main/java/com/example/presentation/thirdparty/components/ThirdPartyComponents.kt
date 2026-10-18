package com.example.presentation.thirdparty.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocationOn
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
import com.example.data.local.entity.ThirdPartyEntity
import com.example.presentation.thirdparty.model.ThirdPartyFormState

// ============================================================================
// KASHAPP PREMIUM BLUE & IOS DESIGN TOKENS
// ============================================================================
private val KashPrimaryBlue = Color(0xFF0284C7)
private val KashNavyBlue = Color(0xFF0F172A)
private val KashSoftIceBlue = Color(0xFFF0F9FF)
private val KashCardBg = Color.White
private val KashBorderColor = Color(0xFFE2E8F0)
private val KashAccentCyan = Color(0xFF38BDF8)
private val KashTextSecondary = Color(0xFF64748B)

/**
 * Resolves an icon ImageVector based on stored iconName and category.
 */
fun resolveThirdPartyIcon(iconName: String, category: String): ImageVector {
    return when {
        iconName == "DirectionsCar" || category.equals("CONCESIONARIO", ignoreCase = true) -> Icons.Default.DirectionsCar
        iconName == "Storefront" || category.equals("TIENDA_COMERCIO", ignoreCase = true) -> Icons.Default.Storefront
        category.equals("CLIENTE", ignoreCase = true) -> Icons.Default.Person
        else -> Icons.Default.Person
    }
}

/**
 * iPhone / iOS Grouped Inset Modal BottomSheet for Third-Party Creation and Editing.
 * Styled with KashApp Blue palette, segmented pill pickers, grouped inset cards and clean layout.
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
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedFullTab by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KashSoftIceBlue,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 38.dp, height = 4.5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
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
            // IOS TOP NAVIGATION BAR
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
                        text = if (formState.isExpressMode) "Nuevo Tercero" else "Registro Empresarial",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = KashNavyBlue
                    )
                    Text(
                        text = if (formState.isExpressMode) "Modo Express Rápido" else "Ficha Fiscal y Financiera",
                        fontSize = 11.5.sp,
                        color = KashPrimaryBlue,
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
                color = Color(0xFFE2E8F0),
                shape = RoundedCornerShape(14.dp),
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
                        color = if (expressSelected) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(11.dp),
                        shadowElevation = if (expressSelected) 2.dp else 0.dp,
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
                                tint = if (expressSelected) KashPrimaryBlue else KashTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Modo Rápido",
                                fontWeight = if (expressSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.5.sp,
                                color = if (expressSelected) KashPrimaryBlue else KashTextSecondary
                            )
                        }
                    }

                    // Option 2: Full
                    Surface(
                        color = if (!expressSelected) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(11.dp),
                        shadowElevation = if (!expressSelected) 2.dp else 0.dp,
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
                                tint = if (!expressSelected) KashPrimaryBlue else KashTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Modo Completo",
                                fontWeight = if (!expressSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.5.sp,
                                color = if (!expressSelected) KashPrimaryBlue else KashTextSecondary
                            )
                        }
                    }
                }
            }

            // Error banner callout
            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ $errorMessage",
                            color = Color(0xFFDC2626),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Quick Import from Phone Contacts Pill (if callback supplied)
            if (onImportContactClick != null) {
                Surface(
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
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
                                    .background(Color(0xFFE0F2FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContactPhone,
                                    contentDescription = null,
                                    tint = KashPrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Importar desde Contactos",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KashNavyBlue
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
                            tint = KashPrimaryBlue,
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

                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

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
                                    color = if (isSelected) KashPrimaryBlue else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.clickable {
                                        onUpdateForm { it.copy(idType = type) }
                                    }
                                ) {
                                    Text(
                                        text = type,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else KashNavyBlue,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                    IPhoneTextFieldRow(
                        value = formState.idNumber,
                        onValueChange = { str -> onUpdateForm { it.copy(idNumber = str) } },
                        label = "Número / Cédula / NIT *",
                        placeholder = "Ej: 12345678",
                        icon = Icons.Default.CreditCard,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                    // Teléfono opcional
                    IPhoneTextFieldRow(
                        value = formState.primaryPhone ?: "",
                        onValueChange = { str -> onUpdateForm { it.copy(primaryPhone = str) } },
                        label = "Teléfono / WhatsApp (Opcional)",
                        placeholder = "Ej: 300 123 4567",
                        icon = Icons.Default.Call,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }

                // ------------------------------------------
                // CARD 2: CATEGORÍA / ROL COMERCIAL
                // ------------------------------------------
                IPhoneGroupedCard(title = "ROL O CATEGORÍA COMERCIAL") {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val categories = listOf(
                            "PROVEEDOR" to "Proveedor",
                            "CONCESIONARIO" to "Concesionario",
                            "TIENDA_COMERCIO" to "Tienda / Comercio",
                            "CLIENTE" to "Cliente",
                            "ACREEDOR" to "Acreedor"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { (code, label) ->
                                val isSelected = formState.category.equals(code, ignoreCase = true)
                                Surface(
                                    color = if (isSelected) KashPrimaryBlue else Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (isSelected) KashPrimaryBlue else Color(0xFFE2E8F0)),
                                    modifier = Modifier.clickable {
                                        onUpdateForm { it.copy(category = code) }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = resolveThirdPartyIcon("", code),
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else KashPrimaryBlue,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else KashNavyBlue
                                        )
                                    }
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
                    color = Color(0xFFE2E8F0),
                    shape = RoundedCornerShape(14.dp),
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
                                color = if (isSel) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(11.dp),
                                shadowElevation = if (isSel) 2.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedFullTab = index }
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) KashPrimaryBlue else KashTextSecondary,
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

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                            IPhoneTextFieldRow(
                                value = formState.commercialName ?: "",
                                onValueChange = { str -> onUpdateForm { it.copy(commercialName = str) } },
                                label = "Nombre Comercial / Alias",
                                placeholder = "Ej: Concesionario Toyota Calle 100",
                                icon = Icons.Default.Storefront
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

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
                                            color = if (isSel) KashPrimaryBlue else Color(0xFFF1F5F9),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onUpdateForm { it.copy(personType = code) } }
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSel) Color.White else KashNavyBlue,
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                            IPhoneTextFieldRow(
                                value = formState.idNumber,
                                onValueChange = { str -> onUpdateForm { it.copy(idNumber = str) } },
                                label = "Número de Identificación / NIT *",
                                placeholder = "12345678",
                                icon = Icons.Default.CreditCard,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            if (formState.idType.equals("NIT", ignoreCase = true)) {
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                                IPhoneTextFieldRow(
                                    value = formState.verificationDigit ?: "",
                                    onValueChange = { str -> onUpdateForm { it.copy(verificationDigit = str.take(1)) } },
                                    label = "Dígito de Verificación (DV)",
                                    placeholder = "0 a 9",
                                    icon = Icons.Default.Check,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                    }

                    1 -> {
                        // Contacto y Ubicación
                        IPhoneGroupedCard(title = "CONTACTO Y UBICACIÓN") {
                            IPhoneTextFieldRow(
                                value = formState.primaryPhone ?: "",
                                onValueChange = { str -> onUpdateForm { it.copy(primaryPhone = str) } },
                                label = "Teléfono Principal",
                                placeholder = "Ej: (601) 745-0000",
                                icon = Icons.Default.Call,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                            IPhoneTextFieldRow(
                                value = formState.email ?: "",
                                onValueChange = { str -> onUpdateForm { it.copy(email = str) } },
                                label = "Correo Electrónico",
                                placeholder = "contacto@empresa.com",
                                icon = Icons.Default.Email,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                            IPhoneTextFieldRow(
                                value = formState.city ?: "",
                                onValueChange = { str -> onUpdateForm { it.copy(city = str) } },
                                label = "Ciudad / Municipio",
                                placeholder = "Ej: Bogotá, Medellín, Cali",
                                icon = Icons.Default.LocationOn
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                            IPhoneTextFieldRow(
                                value = formState.address ?: "",
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

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                            IPhoneTextFieldRow(
                                value = formState.defaultBank ?: "",
                                onValueChange = { str -> onUpdateForm { it.copy(defaultBank = str) } },
                                label = "Banco Predilecto",
                                placeholder = "Ej: Bancolombia, Davivienda",
                                icon = Icons.Default.AccountBalance
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                            IPhoneTextFieldRow(
                                value = formState.bankAccountNumber ?: "",
                                onValueChange = { str -> onUpdateForm { it.copy(bankAccountNumber = str) } },
                                label = "Número de Cuenta Bancaria",
                                placeholder = "Número de cuenta para transferencias",
                                icon = Icons.Default.CreditCard
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                            IPhoneTextFieldRow(
                                value = formState.notes ?: "",
                                onValueChange = { str -> onUpdateForm { it.copy(notes = str) } },
                                label = "Notas u Observaciones",
                                placeholder = "Detalles comerciales o acuerdos",
                                icon = Icons.Default.Edit
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

/**
 * Clean iOS Inset Grouped Card Container with KashApp Blue accents.
 */
@Composable
fun IPhoneGroupedCard(
    title: String? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0369A1),
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
            )
        }
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = KashCardBg),
            border = BorderStroke(1.dp, KashBorderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

/**
 * Reusable iOS Text Field Row inside an inset card.
 */
@Composable
fun IPhoneTextFieldRow(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFFE0F2FE)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = KashPrimaryBlue,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = KashTextSecondary,
                letterSpacing = 0.3.sp
            )
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = placeholder,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                },
                singleLine = true,
                keyboardOptions = keyboardOptions,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KashNavyBlue
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Comprehensive iOS-style Detail Dialog for viewing and acting on an inspected Third Party.
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
        containerColor = Color.White,
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
                            color = KashNavyBlue
                        )
                        Text(
                            text = thirdParty.displayCode,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KashPrimaryBlue
                        )
                    }
                }

                IconButton(onClick = { onToggleFavorite(!thirdParty.isFavorite) }) {
                    Icon(
                        imageVector = if (thirdParty.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorito",
                        tint = if (thirdParty.isFavorite) Color(0xFFEAB308) else Color(0xFF94A3B8)
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
                        color = Color(0xFFE0F2FE),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = thirdParty.category,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashPrimaryBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (thirdParty.personType == "JURIDICA") "Persona Jurídica" else "Persona Natural",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KashTextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Detail Items
                DetailItemRow(label = "Documento / NIT", value = thirdParty.fullDocument)

                val phone = thirdParty.primaryPhone
                if (!phone.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DetailItemRow(label = "Teléfono", value = phone, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Llamar", tint = KashPrimaryBlue, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                val email = thirdParty.email
                if (!email.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DetailItemRow(label = "Correo", value = email, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = "Enviar Email", tint = KashPrimaryBlue, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                if (!thirdParty.city.isNullOrBlank() || !thirdParty.address.isNullOrBlank()) {
                    val locationStr = listOfNotNull(thirdParty.city, thirdParty.address).joinToString(" • ")
                    DetailItemRow(label = "Ubicación", value = locationStr)
                }

                if (!thirdParty.defaultBank.isNullOrBlank() || !thirdParty.bankAccountNumber.isNullOrBlank()) {
                    val bankStr = "${thirdParty.defaultBank ?: "Banco"} - ${thirdParty.bankAccountType ?: "Cuenta"}: ${thirdParty.bankAccountNumber ?: ""}"
                    DetailItemRow(label = "Datos Bancarios", value = bankStr)
                }

                if (thirdParty.paymentTermsDays > 0) {
                    DetailItemRow(label = "Plazo de Pago", value = "${thirdParty.paymentTermsDays} días crédito")
                }

                val notes = thirdParty.notes
                if (!notes.isNullOrBlank()) {
                    DetailItemRow(label = "Observaciones", value = notes)
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete Button
                IconButton(
                    onClick = { showDeleteConfirm = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color(0xFFDC2626)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Export PDF Button
                    Button(
                        onClick = onExportPdf,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0F2FE)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = KashPrimaryBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", color = KashPrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }

                    // Edit Button
                    Button(
                        onClick = onEdit,
                        colors = ButtonDefaults.buttonColors(containerColor = KashPrimaryBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = KashTextSecondary)
            }
        },
        modifier = modifier
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar Tercero?", fontWeight = FontWeight.Bold) },
            text = { Text("Se desactivará '${thirdParty.displayName}' del directorio. Sus referencias históricas se conservarán para trazabilidad.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Sí, Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun DetailItemRow(
    label: String,
    value: String?,
    modifier: Modifier = Modifier
) {
    if (value.isNullOrBlank()) return
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = KashTextSecondary,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = KashNavyBlue
        )
    }
}

/**
 * Reusable Selector Pop-up Dialog for Activos Fijos or other transaction forms.
 * Allows searching, choosing an existing third party in 1 tap, or continuing freely.
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
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Seleccionar Tercero",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = KashNavyBlue
                    )
                    Text(
                        text = "${thirdParties.size} registrados en la base de datos",
                        fontSize = 12.sp,
                        color = KashTextSecondary
                    )
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
                    placeholder = { Text("Buscar por nombre, NIT, concesionario...", fontSize = 12.5.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = KashPrimaryBlue) },
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
                        focusedBorderColor = KashPrimaryBlue,
                        unfocusedBorderColor = KashBorderColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Button to create a new third party right from the dialog
                Button(
                    onClick = onOpenCreateNew,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0F2FE)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = KashPrimaryBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Crear Nuevo Tercero", color = KashPrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // List of filtered third parties
                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron terceros para '$searchQuery'. Puedes crearlo o escribir el nombre libremente.",
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
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filtered.forEach { tp ->
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                                                .background(Color(0xFFE0F2FE)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = resolveThirdPartyIcon(tp.iconName, tp.category),
                                                contentDescription = null,
                                                tint = KashPrimaryBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = tp.displayName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = KashNavyBlue
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
                                        tint = KashPrimaryBlue,
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
                Text("Listo", fontWeight = FontWeight.Bold, color = KashPrimaryBlue)
            }
        },
        modifier = modifier
    )
}
