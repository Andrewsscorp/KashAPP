package com.example.presentation.account.components

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SecureDocumentManager
import com.example.data.local.entity.BankEntity
import com.example.domain.repository.FinancialRepository
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import kotlinx.coroutines.launch

/**
 * Country and Currency configuration model for Master Data Management.
 */
data class CountryOption(
    val code: String,
    val name: String,
    val flag: String,
    val defaultCurrency: String,
    val clearingSystemName: String,
    val isNumericClearingCode: Boolean
)

val SupportedCountries = listOf(
    CountryOption("CO", "Colombia", "🇨🇴", "COP", "ACH / Redeban / PSE", true),
    CountryOption("US", "Estados Unidos", "🇺🇸", "USD", "Fedwire / ABA Routing", true),
    CountryOption("ES", "España", "🇪🇸", "EUR", "SEPA / IBAN Banco", false),
    CountryOption("MX", "México", "🇲🇽", "MXN", "SPEI / CLABE", true),
    CountryOption("PE", "Perú", "🇵🇪", "PEN", "CCE / Código Interbancario", true),
    CountryOption("CL", "Chile", "🇨🇱", "CLP", "CCA / Código Bancario", true)
)

val FinancialPalette = listOf(
    "#0284C7" to "Azul Corporativo",
    "#10B981" to "Verde Ahorro",
    "#EF4444" to "Rojo Alerta",
    "#8B5CF6" to "Púrpura Fintech",
    "#F59E0B" to "Ámbar Liquidez",
    "#06B6D4" to "Cian Digital",
    "#0F172A" to "Negro Premium",
    "#EC4899" to "Rosa Neobanco"
)

val EntityIconOptions = listOf(
    "account_balance" to Icons.Default.AccountBalance,
    "wallet" to Icons.Default.AccountBalanceWallet,
    "credit_card" to Icons.Default.CreditCard,
    "savings" to Icons.Default.Savings,
    "payments" to Icons.Default.Payments,
    "domain" to Icons.Default.Domain
)

/**
 * Screen / Modal for "Agregar entidad bancaria" (Master Data Management).
 *
 * Implements the updated architectural UX order:
 * 1. Nombre de la entidad (Inputs first -> real-time search activation).
 * 2. Buscar en catálogo oficial (Early validator & autocomplete).
 * 3. Código de la entidad (Clearing Code / ACH with In-App numeric keyboard).
 * 4. Jurisdicción (País).
 * 5. Divisa (Moneda) con auto-selección paramétrica.
 * 6. Subir archivo (Logotipo / Comprobante / Extracto) con soporte para imágenes y PDF <= 2MB,
 *    guardado seguro en sandbox local (`hidden_docs/`) y sincronización.
 * 7. Personalización visual (Colores & Icono).
 * 8. Notas adicionales con sanitización anti-XSS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBankEntityScreen(
    preloadedBanks: List<BankEntity> = emptyList(),
    financialRepository: FinancialRepository? = null,
    userId: String = "user_default",
    onBankCreated: ((BankEntity) -> Unit)? = null,
    onSaveBank: (
        name: String,
        clearingCode: String,
        countryCode: String,
        countryName: String,
        currencyCode: String,
        logoUri: String?,
        colorHex: String,
        iconName: String,
        additionalInfo: String?
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val secureDocManager = remember { SecureDocumentManager(context) }

    // Reactive Master Data: live banks from single source of truth (Room Flow)
    val liveBanks by (financialRepository?.getBanks(userId))
        ?.collectAsStateWithLifecycle(initialValue = emptyList())
        ?: remember { mutableStateOf(emptyList()) }
    val effectivePreloadedBanks = remember(preloadedBanks, liveBanks) {
        if (liveBanks.isNotEmpty()) liveBanks else preloadedBanks
    }

    // Core Form Fields (Updated Flow Order)
    var bankName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var clearingCode by remember { mutableStateOf("") }
    var isOfficialBankLocked by remember { mutableStateOf(false) }

    var selectedCountry by remember { mutableStateOf(SupportedCountries[0]) }
    var selectedCurrency by remember { mutableStateOf(SupportedCountries[0].defaultCurrency) }
    var showCountryMenu by remember { mutableStateOf(false) }
    var showCurrencyMenu by remember { mutableStateOf(false) }

    // Attached Document (Logo / Extracto / Comprobante - Image or PDF max 2MB)
    var attachedFileUri by remember { mutableStateOf<Uri?>(null) }
    var attachedFileName by remember { mutableStateOf<String?>(null) }
    var attachedFileType by remember { mutableStateOf<String?>(null) } // "IMAGE" or "PDF"
    var attachedFileSizeText by remember { mutableStateOf<String?>(null) }
    var fileErrorMessage by remember { mutableStateOf<String?>(null) }

    // Visual Branding (Mental Accounting)
    var selectedColorHex by remember { mutableStateOf(FinancialPalette[0].first) }
    var selectedIconName by remember { mutableStateOf(EntityIconOptions[0].first) }

    // Additional Info (0/200 with XSS sanitizer)
    var additionalInfo by remember { mutableStateOf("") }

    // In-App Keyboard State
    var isInAppKeyboardVisible by remember { mutableStateOf(false) }

    // Validation & Error Handling
    var validationErrorMessage by remember { mutableStateOf<String?>(null) }

    // Autocomplete suggestions based on either bankName or searchQuery
    val effectiveSearchTerm = remember(bankName, searchQuery) {
        if (searchQuery.isNotBlank()) searchQuery.trim() else bankName.trim()
    }

    val filteredCatalog = remember(effectiveSearchTerm, effectivePreloadedBanks) {
        if (effectiveSearchTerm.length >= 2) {
            effectivePreloadedBanks.filter {
                it.name.contains(effectiveSearchTerm, ignoreCase = true) ||
                it.clearingCode.contains(effectiveSearchTerm)
            }
        } else {
            emptyList()
        }
    }

    // Document & Image Picker for Files <= 2MB (Images and PDF documents only)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val saveResult = secureDocManager.saveDocumentSafely(uri, userId = "user_default")
            if (saveResult.isSuccess) {
                val saved = saveResult.getOrThrow()
                attachedFileUri = Uri.parse(saved.localUri)
                attachedFileName = saved.originalName
                attachedFileType = if (saved.mimeType.contains("pdf", ignoreCase = true)) "PDF" else "IMAGE"
                val kb = saved.sizeBytes / 1024.0
                attachedFileSizeText = if (kb >= 1024) String.format("%.2f MB", kb / 1024.0) else String.format("%.0f KB", kb)
                fileErrorMessage = null
            } else {
                fileErrorMessage = saveResult.exceptionOrNull()?.message ?: "Error al procesar el archivo"
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("add_bank_entity_screen"),
        color = Color(0xFFF8FAFC)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                                .testTag("cancel_add_bank_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Cancelar",
                                tint = KashNavy,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Agregar entidad bancaria",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashNavy
                        )
                    }

                    Button(
                        onClick = {
                            if (bankName.trim().isBlank()) {
                                validationErrorMessage = "Por favor ingresa el nombre de la entidad."
                                return@Button
                            }
                            if (clearingCode.trim().isBlank()) {
                                validationErrorMessage = "Por favor ingresa el código de la entidad (Compensación / ACH)."
                                return@Button
                            }

                            // Anti-XSS Sanitizer
                            val sanitizedNotes = additionalInfo
                                .replace(Regex("<script[^>]*>.*?</script>", RegexOption.IGNORE_CASE), "")
                                .replace(Regex("<iframe[^>]*>.*?</iframe>", RegexOption.IGNORE_CASE), "")
                                .replace(Regex("<[^>]*>"), "")
                                .trim()

                            if (financialRepository != null) {
                                coroutineScope.launch {
                                    val result = financialRepository.createBank(
                                        userId = userId,
                                        name = bankName.trim(),
                                        clearingCode = clearingCode.trim(),
                                        countryCode = selectedCountry.code,
                                        countryName = selectedCountry.name,
                                        currencyCode = selectedCurrency,
                                        cityName = null,
                                        logoUrl = attachedFileUri?.toString(),
                                        colorHex = selectedColorHex,
                                        iconName = selectedIconName,
                                        additionalInfo = sanitizedNotes.ifBlank { null }
                                    )
                                    result.onSuccess { createdEntity ->
                                        onSaveBank(
                                            createdEntity.name,
                                            createdEntity.clearingCode,
                                            createdEntity.countryCode,
                                            createdEntity.countryName,
                                            createdEntity.currencyCode,
                                            createdEntity.logoUrl,
                                            createdEntity.colorHex,
                                            createdEntity.iconName,
                                            createdEntity.additionalInfo
                                        )
                                        onBankCreated?.invoke(createdEntity)
                                        onDismiss()
                                    }.onFailure { err ->
                                        validationErrorMessage = err.message ?: "Error al registrar la entidad bancaria."
                                    }
                                }
                            } else {
                                val createdEntity = BankEntity(
                                    id = "bank_${java.util.UUID.randomUUID()}",
                                    userId = userId,
                                    name = bankName.trim(),
                                    clearingCode = clearingCode.trim(),
                                    countryCode = selectedCountry.code,
                                    countryName = selectedCountry.name,
                                    currencyCode = selectedCurrency,
                                    cityName = null,
                                    logoUrl = attachedFileUri?.toString(),
                                    colorHex = selectedColorHex,
                                    iconName = selectedIconName,
                                    isGlobal = false,
                                    orderIndex = 0,
                                    additionalInfo = sanitizedNotes.ifBlank { null }
                                )
                                onSaveBank(
                                    bankName.trim(),
                                    clearingCode.trim(),
                                    selectedCountry.code,
                                    selectedCountry.name,
                                    selectedCurrency,
                                    attachedFileUri?.toString(),
                                    selectedColorHex,
                                    selectedIconName,
                                    sanitizedNotes.ifBlank { null }
                                )
                                onBankCreated?.invoke(createdEntity)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_bank_button")
                    ) {
                        Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Scrollable Content Form
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // LIVE PREVIEW CARD WITH MARQUEE
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.5.dp, Color(android.graphics.Color.parseColor(selectedColorHex)).copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "VISTA PREVIA DE LA ENTIDAD",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 1.sp
                            )
                            Surface(
                                color = Color(android.graphics.Color.parseColor(selectedColorHex)).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Personalizada",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(android.graphics.Color.parseColor(selectedColorHex)),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(android.graphics.Color.parseColor(selectedColorHex))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = EntityIconOptions.find { it.first == selectedIconName }?.second ?: Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (bankName.isBlank()) "Nombre de la entidad bancaria" else bankName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KashNavy,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${selectedCountry.flag} ${selectedCountry.name}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "ACH: ${if (clearingCode.isBlank()) "---" else clearingCode}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF334155),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color(0xFFDCFCE7),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = selectedCurrency,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF166534),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // 1. NOMBRE DE LA ENTIDAD * (NUEVO ORDEN: PRIMER CAMPO DEL FORMULARIO)
                // =========================================================================
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1. Nombre de la entidad *",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = KashNavy
                            )
                            Text(
                                text = "${bankName.length}/100",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        OutlinedTextField(
                            value = bankName,
                            onValueChange = {
                                if (it.length <= 100) {
                                    bankName = it
                                    // Sincroniza término para buscar catálogo en vivo
                                    if (isOfficialBankLocked) {
                                        isOfficialBankLocked = false
                                    }
                                }
                            },
                            placeholder = {
                                Text(
                                    text = "Ej. Bancolombia, Fondo de Empleados, Nu...",
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            singleLine = true,
                            textStyle = TextStyle(color = KashNavy, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = KashNavy,
                                unfocusedTextColor = KashNavy,
                                focusedBorderColor = KashBlue,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                cursorColor = KashBlue
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bank_name_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // =========================================================================
                // 2. BUSCAR EN CATÁLOGO OFICIAL (VALIDADOR TEMPRANO / SUGERENCIAS)
                // =========================================================================
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = KashBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "2. Buscar en catálogo oficial",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = KashNavy
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = "Escribe para filtrar bancos oficiales (ej. Davivienda, Nu...)",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            singleLine = true,
                            textStyle = TextStyle(color = KashNavy, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = KashNavy,
                                unfocusedTextColor = KashNavy,
                                focusedBorderColor = KashBlue,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                cursorColor = KashBlue
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_bank_catalog_input"),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Limpiar",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        )

                        // Autocomplete suggestion chips based on Name or Search Query
                        if (filteredCatalog.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Entidades sugeridas del catálogo oficial (Click para auto-completar):",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                filteredCatalog.take(4).forEach { suggestion ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                bankName = suggestion.name
                                                clearingCode = suggestion.clearingCode
                                                selectedCountry = SupportedCountries.find { it.code == suggestion.countryCode }
                                                    ?: SupportedCountries[0]
                                                selectedCurrency = suggestion.currencyCode
                                                selectedColorHex = suggestion.colorHex
                                                selectedIconName = suggestion.iconName
                                                isOfficialBankLocked = true
                                                searchQuery = ""
                                            },
                                        color = Color(0xFFEFF6FF),
                                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "🏦", fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = suggestion.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = KashNavy,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                color = Color.White,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "ACH ${suggestion.clearingCode}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = KashBlue,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // 3. CÓDIGO DE LA ENTIDAD * (CLEARING CODE / ACH CON TECLADO IN-APP)
                // =========================================================================
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "3. Código de la entidad *",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KashNavy
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = KashBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Surface(
                                color = if (isOfficialBankLocked) Color(0xFFDCFCE7) else Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isOfficialBankLocked) "Oficial Verificado 🔒" else selectedCountry.clearingSystemName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOfficialBankLocked) Color(0xFF166534) else KashBlue,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Input with Clickable overlay to trigger In-App Keyboard
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    1.dp,
                                    if (isInAppKeyboardVisible) KashBlue else Color(0xFFCBD5E1),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    isInAppKeyboardVisible = true
                                }
                                .testTag("clearing_code_input_field"),
                            color = if (isInAppKeyboardVisible) Color(0xFFEFF6FF) else Color.White
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (clearingCode.isEmpty()) "Ej. 001 (Bancolombia) o 051 (Davivienda)" else clearingCode,
                                    fontSize = 14.sp,
                                    fontWeight = if (clearingCode.isEmpty()) FontWeight.Normal else FontWeight.Bold,
                                    color = if (clearingCode.isEmpty()) Color(0xFF94A3B8) else KashNavy,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isInAppKeyboardVisible) {
                                    Text(
                                        text = "Teclado activo ⌨️",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KashBlue
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Código de compensación interbancaria requerido para enrutar transferencias automáticas.",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 14.sp
                        )
                    }
                }

                // =========================================================================
                // 4 & 5. JURISDICCIÓN (PAÍS) Y DIVISA (MONEDA)
                // =========================================================================
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "4 & 5. Jurisdicción y Divisa (FX Paramétrico)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashNavy
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 4. País Selector
                            Box(modifier = Modifier.weight(1f)) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                        .clickable { showCountryMenu = true }
                                        .testTag("country_picker_button"),
                                    color = Color(0xFFF8FAFC)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = selectedCountry.flag, fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = selectedCountry.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = KashNavy,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showCountryMenu,
                                    onDismissRequest = { showCountryMenu = false },
                                    modifier = Modifier
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                                    containerColor = Color.White
                                ) {
                                    SupportedCountries.forEach { country ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = country.flag, fontSize = 16.sp)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "${country.name} (${country.code})",
                                                        color = KashNavy,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = KashNavy,
                                                leadingIconColor = KashNavy
                                            ),
                                            onClick = {
                                                selectedCountry = country
                                                // Auto-selección paramétrica de moneda
                                                selectedCurrency = country.defaultCurrency
                                                showCountryMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // 5. Moneda Selector
                            Box(modifier = Modifier.weight(1f)) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                        .clickable { showCurrencyMenu = true }
                                        .testTag("currency_picker_button"),
                                    color = Color(0xFFF8FAFC)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Divisa: $selectedCurrency",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = KashNavy
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showCurrencyMenu,
                                    onDismissRequest = { showCurrencyMenu = false },
                                    modifier = Modifier
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                                    containerColor = Color.White
                                ) {
                                    listOf("COP", "USD", "EUR", "MXN", "PEN", "CLP", "ARS", "BRL").forEach { currency ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(text = currency, color = KashNavy, fontWeight = FontWeight.Medium)
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = KashNavy
                                            ),
                                            onClick = {
                                                selectedCurrency = currency
                                                showCurrencyMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // 6. SUBIR ARCHIVO (LOGOTIPO / COMPROBANTE / EXTRACTO - MÁX 2MB, PDF/IMG)
                // =========================================================================
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "6. Subir archivo (Logotipo, extracto o soporte)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sube la imagen oficial o extracto (JPG, PNG, WEBP o PDF - Máx. 2 MB). Se guarda cifrado en el almacenamiento local seguro de la app.",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    documentPickerLauncher.launch(arrayOf("image/*", "application/pdf"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("upload_document_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    tint = KashNavy,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (attachedFileUri == null) "Subir archivo (Img / PDF)" else "Cambiar archivo",
                                    color = KashNavy,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (attachedFileUri != null) {
                                TextButton(onClick = {
                                    attachedFileUri = null
                                    attachedFileName = null
                                    attachedFileType = null
                                    attachedFileSizeText = null
                                }) {
                                    Text("Quitar", color = Color(0xFFEF4444), fontSize = 12.sp)
                                }
                            }
                        }

                        // Preview of attached document
                        if (attachedFileUri != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (attachedFileType == "PDF") Icons.Default.PictureAsPdf else Icons.Default.Description,
                                        contentDescription = null,
                                        tint = if (attachedFileType == "PDF") Color(0xFFEF4444) else KashBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = attachedFileName ?: "archivo_adjunto",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = KashNavy,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${attachedFileType ?: "Documento"} • ${attachedFileSizeText ?: "Guardado en hidden_docs"}",
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }

                        if (fileErrorMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = fileErrorMessage ?: "",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // =========================================================================
                // 7. PERSONALIZACIÓN VISUAL (COLOR E ICONO)
                // =========================================================================
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "7. Personalización visual",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashNavy
                        )

                        // Paleta de colores
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FinancialPalette.forEach { (colorHex, _) ->
                                val isSelected = selectedColorHex == colorHex
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(colorHex)))
                                        .clickable { selectedColorHex = colorHex }
                                        .border(
                                            if (isSelected) 2.5.dp else 0.dp,
                                            if (isSelected) KashNavy else Color.Transparent,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Iconos de la entidad
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            EntityIconOptions.forEach { (iconKey, vector) ->
                                val isSelected = selectedIconName == iconKey
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                                        .border(
                                            1.dp,
                                            if (isSelected) KashBlue else Color(0xFFE2E8F0),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedIconName = iconKey },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = vector,
                                        contentDescription = iconKey,
                                        tint = if (isSelected) KashBlue else Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // 8. NOTAS O INFORMACIÓN ADICIONAL (0/200 SANITIZADO)
                // =========================================================================
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "8. Notas o información adicional",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = KashNavy
                            )
                            Text(
                                text = "${additionalInfo.length}/200",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = additionalInfo,
                            onValueChange = { if (it.length <= 200) additionalInfo = it },
                            placeholder = {
                                Text(
                                    text = "Ej. Ruteo de cuenta de nómina o cooperativa regional...",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            minLines = 2,
                            maxLines = 3,
                            textStyle = TextStyle(color = KashNavy, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = KashNavy,
                                unfocusedTextColor = KashNavy,
                                focusedBorderColor = KashBlue,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                cursorColor = KashBlue
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("additional_info_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // In-App Custom Numeric Keypad for Clearing Codes
            AnimatedVisibility(
                visible = isInAppKeyboardVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF1F5F9),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Código ACH: $clearingCode",
                                fontWeight = FontWeight.Bold,
                                color = KashNavy,
                                fontSize = 13.sp
                            )
                            TextButton(onClick = { isInAppKeyboardVisible = false }) {
                                Text("Ocultar teclado ✕", color = KashBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        val numRows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("CLEAR", "0", "BACKSPACE")
                        )

                        numRows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { key ->
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                when (key) {
                                                    "CLEAR" -> clearingCode = ""
                                                    "BACKSPACE" -> if (clearingCode.isNotEmpty()) clearingCode = clearingCode.dropLast(1)
                                                    else -> if (clearingCode.length < 15) clearingCode += key
                                                }
                                            },
                                        color = when (key) {
                                            "CLEAR" -> Color(0xFFFEE2E2)
                                            "BACKSPACE" -> Color(0xFFE2E8F0)
                                            else -> Color.White
                                        },
                                        shadowElevation = 1.dp
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            when (key) {
                                                "CLEAR" -> Text("C", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                                "BACKSPACE" -> Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Borrar", tint = KashNavy, modifier = Modifier.size(18.dp))
                                                else -> Text(key, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = KashNavy)
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

    // Validation Error Dialog
    validationErrorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { validationErrorMessage = null },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(28.dp))
            },
            title = {
                Text("Datos requeridos", fontWeight = FontWeight.Bold, color = KashNavy, fontSize = 16.sp)
            },
            text = {
                Text(msg, color = Color(0xFF475569), fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = { validationErrorMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = KashBlue)
                ) {
                    Text("Aceptar", color = Color.White)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
