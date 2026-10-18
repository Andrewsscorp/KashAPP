package com.example.presentation.fixedassets.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AssetCategoryEntity
import com.example.domain.model.fixedassets.AssetCategory
import com.example.domain.model.fixedassets.AssetFinancialCalculations
import com.example.domain.model.fixedassets.AssetType
import com.example.domain.model.fixedassets.DepreciationMethod
import com.example.presentation.expense.model.CurrencyDotVisualTransformation
import com.example.presentation.fixedassets.create.components.AssetImagePickerSection
import com.example.presentation.fixedassets.create.components.CategoryPickerBottomSheet
import com.example.presentation.fixedassets.create.components.CompactCategorySelector
import com.example.presentation.fixedassets.create.components.CreateAssetCategoryDialog
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import com.example.presentation.fixedassets.components.AdvancedDepreciationOptionsIPhoneSheet
import com.example.presentation.fixedassets.components.toAdvancedSettings
import com.example.presentation.fixedassets.create.state.FixedAssetFormState
import com.example.presentation.fixedassets.create.components.IPhoneAdaptiveDatePickerField
import com.example.presentation.fixedassets.ai.AssetAiSuggestionEngine
import com.example.presentation.fixedassets.ai.AssetAiSuggestion
import com.example.KashAppApplication
import com.example.presentation.thirdparty.components.ThirdPartyFormModalSheet
import com.example.presentation.thirdparty.components.SelectThirdPartyPopUpDialog
import com.example.data.local.entity.ThirdPartyEntity
import androidx.compose.material.icons.filled.List
import com.example.presentation.thirdparty.model.ThirdPartyFormState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@Composable
fun CreateFixedAssetScreen(
    initialState: FixedAssetFormState = FixedAssetFormState(),
    availableCategories: List<AssetCategoryEntity> = emptyList(),
    onCreateCategory: ((name: String, code: String, colorHex: String, iconName: String) -> Unit)? = null,
    onSaveAsset: (FixedAssetFormState) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appContainer = remember(context) { (context.applicationContext as? KashAppApplication)?.appContainer }
    val thirdPartiesFlow = remember(appContainer) {
        appContainer?.thirdPartyRepository?.observeThirdParties("user_default")
    }
    val thirdParties by (thirdPartiesFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })
    var showExpressThirdPartySheet by remember { mutableStateOf(false) }
    var showSelectThirdPartyDialog by remember { mutableStateOf(false) }
    var expressFormState by remember { mutableStateOf(ThirdPartyFormState(isExpressMode = true, category = "PROVEEDOR")) }
    var expressErrorMessage by remember { mutableStateOf<String?>(null) }

    var formState by remember { mutableStateOf(initialState) }
    var usefulLifeInputText by remember { mutableStateOf(formState.usefulLifeMonths.toString()) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var showAdvancedSettingsSheet by remember { mutableStateOf(false) }
    var showAiSuggestionDialog by remember { mutableStateOf(false) }
    var currentAiSuggestion by remember { mutableStateOf<AssetAiSuggestion?>(null) }
    var showFinancialImpactDialog by remember { mutableStateOf(false) }
    var showDiscardConfirmDialog by remember { mutableStateOf(false) }
    var autoDepreciateMonthEnd by remember { mutableStateOf(true) }
    var showHistoricalDepreciationDialog by remember { mutableStateOf(false) }
    var calculatedHistoricalDepreciation by remember { mutableStateOf(0.0) }
    var elapsedMonthsForHistorical by remember { mutableStateOf(0) }

    val persistSupplierIfNew = {
        val trimmedSupplier = formState.supplier.trim()
        if (trimmedSupplier.isNotBlank()) {
            coroutineScope.launch {
                try {
                    val exists = thirdParties.any {
                        it.name.equals(trimmedSupplier, ignoreCase = true) ||
                        (it.commercialName?.equals(trimmedSupplier, ignoreCase = true) == true)
                    }
                    if (!exists) {
                        val isVehicle = formState.categoryName.contains("Vehículo", ignoreCase = true)
                        val generatedId = "tp_" + (kotlin.math.abs(trimmedSupplier.hashCode()) % 1000000).toString().padStart(6, '0')
                        val autoEntity = ThirdPartyEntity(
                            userId = "user_default",
                            name = trimmedSupplier,
                            normalizedName = ThirdPartyEntity.normalizeName(trimmedSupplier),
                            personType = "NATURAL",
                            idType = "CC",
                            idNumber = generatedId,
                            commercialName = trimmedSupplier,
                            category = if (isVehicle) "CONCESIONARIO" else "PROVEEDOR",
                            iconName = if (isVehicle) "DirectionsCar" else "Person",
                            colorHex = "#0284C7",
                            notes = "Registrado automáticamente desde Activos Fijos"
                        )
                        appContainer?.thirdPartyRepository?.saveThirdParty(autoEntity)
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    val handleSaveAttempt = {
        val acqDate = formState.acquisitionDate
        val elapsed = AssetFinancialCalculations.calculateMonthsElapsed(acqDate)
        val acqVal = formState.acquisitionValue
        val resVal = formState.residualValue
        val lifeMonths = formState.usefulLifeMonths
        val method = formState.depreciationMethod

        if (elapsed > 0 && formState.hasDepreciation && acqVal > resVal && lifeMonths > 0 && formState.accumulatedDepreciation == 0.0) {
            val historicalDep = AssetFinancialCalculations.calculateHistoricalDepreciation(
                acquisitionValue = acqVal,
                residualValue = resVal,
                usefulLifeMonths = lifeMonths,
                method = method,
                acquisitionDateStr = acqDate
            )
            if (historicalDep > 0.0) {
                calculatedHistoricalDepreciation = historicalDep
                elapsedMonthsForHistorical = elapsed
                showHistoricalDepreciationDialog = true
            } else {
                persistSupplierIfNew()
                onSaveAsset(formState)
            }
        } else {
            persistSupplierIfNew()
            onSaveAsset(formState)
        }
    }

    val isDirty = remember(formState, initialState) {
        formState.name.isNotBlank() || formState.acquisitionValueText.isNotBlank() || formState != initialState
    }

    val handleBackPress = {
        if (isDirty) {
            showDiscardConfirmDialog = true
        } else {
            onDismiss()
        }
    }

    BackHandler { handleBackPress() }

    val moneyFmt = DecimalFormat("$ #,##0")

    val bgDark = Color(0xFF07101E)
    val cardBg = Color(0xFF0B172B)
    val cardBorder = Color(0xFF132B4F)
    val primaryBlue = Color(0xFF007AFF)
    val accentCyan = Color(0xFF38BDF8)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF8EA5C4)

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = textPrimary,
        unfocusedTextColor = textPrimary,
        focusedBorderColor = primaryBlue,
        unfocusedBorderColor = cardBorder,
        focusedLabelColor = accentCyan,
        unfocusedLabelColor = textSecondary,
        cursorColor = primaryBlue
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // TOP APP BAR
        Surface(
            color = Color(0xFF091424),
            border = BorderStroke(1.dp, Color(0xFF102644)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = handleBackPress) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = if (formState.assetId == null) "Nuevo Activo Fijo" else "Editar Activo Fijo",
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Inventario Patrimonial y NIIF",
                            fontSize = 11.5.sp,
                            color = textSecondary
                        )
                    }
                }

                Button(
                    onClick = handleSaveAttempt,
                    enabled = formState.isValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryBlue,
                        disabledContainerColor = Color(0xFF132B4F)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("save_asset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Guardar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // FORM BODY
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. CATEGORY SELECTOR (POP-UP SELECTOR - NO LONGER EXPANDED)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CompactCategorySelector(
                    selectedCategoryName = formState.categoryName.ifBlank { formState.category.title },
                    selectedColorHex = formState.categoryColorHex.ifBlank { formState.colorHex },
                    selectedCode = formState.categoryCode,
                    selectedIconName = formState.categoryIconName,
                    onClickChange = { showCategorySheet = true }
                )

                // Tangible / Intangible toggle selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssetType.entries.forEach { t ->
                        val isSel = formState.type == t
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) primaryBlue else Color(0xFF081628),
                            border = BorderStroke(1.dp, if (isSel) primaryBlue else cardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { formState = formState.copy(type = t) }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = t.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 2. ASSET PHOTO UPLOAD AND PREVIEW (NEW FEATURE)
            AssetImagePickerSection(
                imageUrl = formState.imageUrl,
                onImageSelected = { formState = formState.copy(imageUrl = it) }
            )

            // 3. INFORMACIÓN GENERAL DEL ACTIVO (CON CAMPOS EN FORMATO MONEDA)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "INFORMACIÓN GENERAL DEL ACTIVO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary,
                        letterSpacing = 0.5.sp
                    )

                    OutlinedTextField(
                        value = formState.name,
                        onValueChange = { formState = formState.copy(name = it) },
                        label = { Text("Nombre o Descripción Principal *") },
                        placeholder = { Text("Ej: MacBook Pro M3 / Toyota Hilux") },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("asset_name_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Valor de compra en formato moneda
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = formState.acquisitionValueText,
                            onValueChange = {
                                val clean = it.filter { ch -> ch.isDigit() }
                                formState = formState.copy(acquisitionValueText = clean)
                            },
                            label = { Text("Valor de Compra *") },
                            placeholder = { Text("0") },
                            prefix = {
                                Text(
                                    text = "$ ",
                                    color = accentCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            },
                            visualTransformation = CurrencyDotVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = textFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("asset_acquisition_value_input"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        if (formState.acquisitionValue > 0) {
                            Text(
                                text = "Valor monetario: ${moneyFmt.format(formState.acquisitionValue)} COP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentCyan,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        IPhoneAdaptiveDatePickerField(
                            value = formState.acquisitionDate,
                            onValueChange = { formState = formState.copy(acquisitionDate = it) },
                            label = "Fecha de Compra",
                            modifier = Modifier.weight(1.35f)
                        )

                        Column(
                            modifier = Modifier.weight(0.65f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Moneda",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF38BDF8)
                            )
                            OutlinedTextField(
                                value = formState.currency,
                                onValueChange = { formState = formState.copy(currency = it.uppercase()) },
                                placeholder = { Text("COP", color = Color(0xFF64748B)) },
                                singleLine = true,
                                colors = textFieldColors,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = formState.description,
                        onValueChange = { formState = formState.copy(description = it) },
                        label = { Text("Detalle o Uso del Bien (Opcional)") },
                        maxLines = 2,
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            // 3.1 CONTROL DE IMPACTO EN EL DASHBOARD Y PATRIMONIO
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (formState.noAffectDashboard) Color(0xFF1E293B) else Color(0xFF0D1E36)
                ),
                border = BorderStroke(
                    1.dp,
                    if (formState.noAffectDashboard) Color(0xFFF59E0B).copy(alpha = 0.55f) else Color(0xFF0284C7).copy(alpha = 0.55f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        val newNoAffect = !formState.noAffectDashboard
                        formState = formState.copy(
                            noAffectDashboard = newNoAffect,
                            includeInNetWorth = !newNoAffect
                        )
                    }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header con icono, título y switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        if (formState.noAffectDashboard) Color(0xFFF59E0B).copy(alpha = 0.22f) else Color(0xFF0284C7).copy(alpha = 0.22f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (formState.noAffectDashboard) Icons.Default.VisibilityOff else Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = if (formState.noAffectDashboard) Color(0xFFFBBF24) else Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "No Afecta Dashboard",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (formState.noAffectDashboard) "Excluido: No suma al total del dashboard" else "Afecta: Suma al total del dashboard",
                                    fontSize = 11.5.sp,
                                    color = if (formState.noAffectDashboard) Color(0xFFFBBF24) else Color(0xFF38BDF8)
                                )
                            }
                        }

                        Switch(
                            checked = formState.noAffectDashboard,
                            onCheckedChange = { checked ->
                                formState = formState.copy(
                                    noAffectDashboard = checked,
                                    includeInNetWorth = !checked
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFF59E0B),
                                uncheckedThumbColor = Color(0xFF94A3B8),
                                uncheckedTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }

                    // Estado Visual Responsivo
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (formState.noAffectDashboard) Color(0xFFF59E0B).copy(alpha = 0.12f) else Color(0xFF00C853).copy(alpha = 0.12f),
                        border = BorderStroke(
                            1.dp,
                            if (formState.noAffectDashboard) Color(0xFFFBBF24).copy(alpha = 0.3f) else Color(0xFF00E676).copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (formState.noAffectDashboard) Icons.Default.Info else Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = if (formState.noAffectDashboard) Color(0xFFFBBF24) else Color(0xFF00E676),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (formState.noAffectDashboard) {
                                    "Activado: El valor de este activo (ej. \$100.000.000) NO afectará ni sumará al total de activos ni patrimonio en el Dashboard."
                                } else {
                                    "Desactivado: El valor neto del activo se sumará a los activos y al Patrimonio Neto global del Dashboard."
                                },
                                fontSize = 11.5.sp,
                                color = Color(0xFFE2E8F0),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // CONTROL DE INVENTARIO OPERATIVO ("No afecta inventarios")
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_asset_inventory_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
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
                                    .background(if (formState.noAffectInventory) Color(0xFF0284C7).copy(alpha = 0.2f) else Color(0xFF64748B).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = if (formState.noAffectInventory) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "No Afecta Inventarios",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (formState.noAffectInventory) "Fuera de control de inventario de almacén" else "Afecta y descuenta de inventario operativo",
                                    fontSize = 11.5.sp,
                                    color = if (formState.noAffectInventory) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                                )
                            }
                        }

                        Switch(
                            checked = formState.noAffectInventory,
                            onCheckedChange = { formState = formState.copy(noAffectInventory = it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF0284C7),
                                uncheckedThumbColor = Color(0xFF94A3B8),
                                uncheckedTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (formState.noAffectInventory) Color(0xFF0284C7).copy(alpha = 0.12f) else Color(0xFF1E293B).copy(alpha = 0.6f),
                        border = BorderStroke(
                            1.dp,
                            if (formState.noAffectInventory) Color(0xFF38BDF8).copy(alpha = 0.3f) else Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (formState.noAffectInventory) Icons.Default.Info else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (formState.noAffectInventory) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (formState.noAffectInventory) {
                                    "Marcado: Este activo se registrará para cálculo de depreciación y valor contable, pero NO alterará el control de inventarios de existencias/almacén."
                                } else {
                                    "Desmarcado: El activo forma parte del inventario operativo y de existencias de la empresa."
                                },
                                fontSize = 11.5.sp,
                                color = if (formState.noAffectInventory) Color(0xFFE2E8F0) else Color(0xFF94A3B8),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // 4. PARÁMETROS DE DEPRECIACIÓN NIIF (CON FORMATO MONEDA)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PARÁMETROS DE DEPRECIACIÓN NIIF",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (formState.hasDepreciation) "Depreciación contable habilitada" else "Activo no depreciable (Terreno/Colección)",
                                fontSize = 11.5.sp,
                                color = textSecondary
                            )
                        }

                        Switch(
                            checked = formState.hasDepreciation,
                            onCheckedChange = { formState = formState.copy(hasDepreciation = it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = primaryBlue,
                                uncheckedThumbColor = Color(0xFF94A3B8),
                                uncheckedTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }

                    if (formState.hasDepreciation) {
                        val isEditing = formState.assetId != null
                        val methodBlocked = isEditing && !formState.allowMethodChange
                        val lifeBlocked = isEditing && !formState.allowLifeChange
                        val residualBlocked = isEditing && !formState.allowResidualChange

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Método Contable",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                            if (methodBlocked) {
                                Text(
                                    text = "🔒 Bloqueado por política",
                                    fontSize = 11.sp,
                                    color = Color(0xFFF59E0B),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DepreciationMethod.entries.forEach { m ->
                                val isSel = formState.depreciationMethod == m
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) primaryBlue else Color(0xFF081628),
                                    border = BorderStroke(1.dp, if (isSel) primaryBlue else cardBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = !methodBlocked) { formState = formState.copy(depreciationMethod = m) }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = m.name.replace("_", " "),
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) Color.White else textSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // Useful Life Header + Magic AI Button
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (lifeBlocked) "Vida Útil (🔒 Bloqueada)" else "Vida Útil Estimada NIIF",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (lifeBlocked) Color(0xFFF59E0B) else textPrimary
                                )

                                // BOTÓN MÁGICO IA
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1E1B4B),
                                    border = BorderStroke(1.dp, Color(0xFF6366F1)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            val sug = AssetAiSuggestionEngine.suggestParameters(
                                                name = formState.name,
                                                category = formState.category,
                                                type = formState.type,
                                                acquisitionValue = formState.acquisitionValue
                                            )
                                            currentAiSuggestion = sug
                                            showAiSuggestionDialog = true
                                        }
                                        .testTag("btn_magico_ia_sugerencia")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFFA5B4FC),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Sugerir con IA",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFA5B4FC)
                                        )
                                    }
                                }
                            }

                            // Input manual de meses + Slider sincronizado (sin quitar el deslizador)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = usefulLifeInputText,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }.take(4)
                                        usefulLifeInputText = digits
                                        val parsed = digits.toIntOrNull()
                                        if (parsed != null && parsed > 0 && !lifeBlocked) {
                                            formState = formState.copy(usefulLifeMonths = parsed)
                                        }
                                    },
                                    readOnly = lifeBlocked,
                                    label = { Text("Meses (manual)", fontSize = 11.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = textFieldColors,
                                    modifier = Modifier.width(130.dp),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${formState.usefulLifeMonths} meses (${"%.1f".format(formState.usefulLifeMonths / 12.0)} años)",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentCyan
                                    )
                                    Text(
                                        text = "Escribe la cantidad exacta o usa el deslizador abajo",
                                        fontSize = 10.sp,
                                        color = textSecondary
                                    )
                                }
                            }

                            Slider(
                                value = formState.usefulLifeMonths.toFloat().coerceIn(6f, 360f),
                                onValueChange = { newVal ->
                                    if (!lifeBlocked) {
                                        val rounded = newVal.toInt()
                                        usefulLifeInputText = rounded.toString()
                                        formState = formState.copy(usefulLifeMonths = rounded)
                                    }
                                },
                                enabled = !lifeBlocked,
                                valueRange = 6f..360f,
                                steps = 58,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (lifeBlocked) Color(0xFF64748B) else primaryBlue,
                                    activeTrackColor = if (lifeBlocked) Color(0xFF64748B) else primaryBlue,
                                    inactiveTrackColor = Color(0xFF132B4F)
                                )
                            )
                        }

                        // Residual Value Input con Formato Moneda
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = formState.residualValueText,
                                onValueChange = {
                                    if (!residualBlocked) {
                                        val clean = it.filter { ch -> ch.isDigit() }
                                        formState = formState.copy(residualValueText = clean)
                                    }
                                },
                                readOnly = residualBlocked,
                                label = { Text(if (residualBlocked) "Valor Residual (🔒 Bloqueado)" else "Valor Residual / Salvamento") },
                                placeholder = { Text("0") },
                                prefix = {
                                    Text(
                                        text = "$ ",
                                        color = accentCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                },
                                visualTransformation = CurrencyDotVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = textFieldColors,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )

                            if (formState.residualValue > 0) {
                                Text(
                                    text = "Salvamento proyectado: ${moneyFmt.format(formState.residualValue)} COP",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentCyan,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }

                        // LIVE DEPRECIATION PREVIEW CARD CON BOTÓN DE IMPLICACIONES FINANCIERAS
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF062A48),
                            border = BorderStroke(1.dp, Color(0xFF0E4A7D)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { showFinancialImpactDialog = true }
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
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
                                                .size(36.dp)
                                                .background(primaryBlue, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Calculate,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "CUOTA DEPRECIACIÓN MENSUAL",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = accentCyan,
                                                letterSpacing = 0.5.sp
                                            )
                                            Text(
                                                text = moneyFmt.format(formState.monthlyDepreciationPreview),
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black,
                                                color = textPrimary
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0B3A60),
                                        modifier = Modifier.clickable { showFinancialImpactDialog = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Assessment, null, tint = accentCyan, modifier = Modifier.size(13.dp))
                                            Text("Impacto", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Base depreciable: ${moneyFmt.format(formState.acquisitionValue - formState.residualValue)} COP",
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                    Text(
                                        text = "Toca para ver impacto contable",
                                        fontSize = 10.sp,
                                        color = accentCyan
                                    )
                                }
                            }
                        }

                        // APARTADO INTEGRADO DE CONFIGURACIÓN AVANZADA NIIF
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF071527),
                            border = BorderStroke(1.dp, Color(0xFF163259)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "CONFIGURACIÓN AVANZADA Y POLÍTICAS DE CIERRE",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentCyan,
                                    letterSpacing = 0.5.sp
                                )

                                // Opción: Depreciar automáticamente a fin de mes
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = "Depreciar automáticamente (a fin de mes)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary
                                        )
                                        Text(
                                            text = "Incluye este activo en la corrida automática mensual bajo NIC 16.",
                                            fontSize = 11.sp,
                                            color = textSecondary
                                        )
                                    }
                                    Switch(
                                        checked = autoDepreciateMonthEnd,
                                        onCheckedChange = { autoDepreciateMonthEnd = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = primaryBlue,
                                            uncheckedThumbColor = Color(0xFF94A3B8),
                                            uncheckedTrackColor = Color(0xFF1E293B)
                                        )
                                    )
                                }

                                HorizontalDivider(color = Color(0xFF132B4F))

                                // Botón para desplegar sheet completo de políticas
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0E223D),
                                    border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { showAdvancedSettingsSheet = true }
                                        .testTag("btn_abrir_politicas_detalladas")
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
                                            Icon(Icons.Default.Settings, null, tint = accentCyan, modifier = Modifier.size(16.dp))
                                            Column {
                                                Text("Políticas Contables Detalladas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                Text("${formState.depreciationMethod.name} • ${formState.frequency} • ${formState.rounding}", fontSize = 10.5.sp, color = textSecondary)
                                            }
                                        }
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = textSecondary, modifier = Modifier.size(18.dp))
                                    }
                            }
                        }
                    }
                }
            }

            // 5. TECHNICAL / INVENTORY SPECS
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "FICHA TÉCNICA E IDENTIFICACIÓN (OPCIONAL)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = formState.brand,
                            onValueChange = { formState = formState.copy(brand = it) },
                            label = { Text("Marca") },
                            singleLine = true,
                            colors = textFieldColors,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            value = formState.model,
                            onValueChange = { formState = formState.copy(model = it) },
                            label = { Text("Modelo / Año") },
                            singleLine = true,
                            colors = textFieldColors,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    OutlinedTextField(
                        value = formState.serialOrPlate,
                        onValueChange = { formState = formState.copy(serialOrPlate = it) },
                        label = {
                            Text(
                                if (formState.category == AssetCategory.VEHICLES || formState.categoryName.contains("Vehículo", ignoreCase = true)) {
                                    "Placa del Vehículo"
                                } else {
                                    "Número de Serie / Placa"
                                }
                            )
                        },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = formState.location,
                        onValueChange = { formState = formState.copy(location = it) },
                        label = { Text("Ubicación Física (Oficina / Inmueble / Sede)") },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Selector / Autocompletado de Terceros
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Proveedor / Concesionario", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { showSelectThirdPartyDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(9.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.List, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Seleccionar", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(
                                    onClick = {
                                        expressErrorMessage = null
                                        expressFormState = ThirdPartyFormState(
                                            name = formState.supplier,
                                            isExpressMode = true,
                                            category = if (formState.categoryName.contains("Vehículo", ignoreCase = true)) "CONCESIONARIO" else "PROVEEDOR"
                                        )
                                        showExpressThirdPartySheet = true
                                    },
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("+ Crear Rápido", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = formState.supplier,
                            onValueChange = { formState = formState.copy(supplier = it) },
                            placeholder = { Text("Escribe o selecciona de terceros...", color = Color(0xFF64748B)) },
                            singleLine = true,
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        // Chips de sugerencia rápida de terceros registrados
                        if (thirdParties.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val relevantThirdParties = remember(thirdParties, formState.categoryName) {
                                    val isVehicle = formState.categoryName.contains("Vehículo", ignoreCase = true)
                                    thirdParties.filter { 
                                        if (isVehicle) it.category == "CONCESIONARIO" || it.isFavorite else true 
                                    }.take(6)
                                }
                                relevantThirdParties.forEach { tp ->
                                    val isSelected = formState.supplier.equals(tp.displayName, ignoreCase = true)
                                    Surface(
                                        color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)),
                                        modifier = Modifier.clickable {
                                            formState = formState.copy(supplier = tp.displayName)
                                        }
                                    ) {
                                        Text(
                                            text = "${tp.displayName} (${tp.fullDocument})",
                                            fontSize = 11.sp,
                                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // BOTÓN PRINCIPAL DE GUARDADO AMPLIO Y RESPONSIVO
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(
                    onClick = handleSaveAttempt,
                    enabled = formState.isValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryBlue,
                        disabledContainerColor = Color(0xFF132B4F)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp)
                        .height(52.dp)
                        .testTag("save_asset_button_bottom")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (formState.assetId == null) "Registrar Activo Fijo" else "Actualizar Activo Fijo",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // CATEGORY PICKER POP-UP MODAL BOTTOM SHEET
    if (showCategorySheet) {
        CategoryPickerBottomSheet(
            categories = availableCategories,
            selectedCategoryName = formState.categoryName.ifBlank { formState.category.title },
            onSelectCategory = { cat ->
                formState = formState.copy(
                    categoryName = cat.name,
                    categoryCode = cat.code,
                    categoryColorHex = cat.colorHex,
                    categoryIconName = cat.iconName,
                    colorHex = cat.colorHex,
                    category = AssetCategory.fromString(cat.name)
                )
            },
            onCreateCategoryClick = {
                showCreateCategoryDialog = true
            },
            onDismiss = { showCategorySheet = false }
        )
    }

    // CREATE CATEGORY DIALOG (WITH COLORS & ICONS IN ROOM DATABASE)
    if (showCreateCategoryDialog) {
        CreateAssetCategoryDialog(
            onDismiss = { showCreateCategoryDialog = false },
            onSaveCategory = { name, code, colorHex, iconName ->
                onCreateCategory?.invoke(name, code, colorHex, iconName)
                formState = formState.copy(
                    categoryName = name,
                    categoryCode = code,
                    categoryColorHex = colorHex,
                    categoryIconName = iconName,
                    colorHex = colorHex,
                    category = AssetCategory.fromString(name)
                )
                showCreateCategoryDialog = false
                showCategorySheet = false
            }
        )
    }

    // ADVANCED DEPRECIATION OPTIONS MODAL (CONFIGURACIÓN AVANZADA NIIF)
    if (showAdvancedSettingsSheet) {
        val tempAsset = formState.toEntity(userId = "user_default")
        AdvancedDepreciationOptionsIPhoneSheet(
            asset = tempAsset,
            initialSettings = tempAsset.toAdvancedSettings(),
            onSaveSettings = { settings ->
                val newMethod = when (settings.method) {
                    "Suma de dígitos" -> DepreciationMethod.DIGITS_SUM
                    "Saldos decrecientes" -> DepreciationMethod.DECLINING_BALANCE
                    else -> DepreciationMethod.STRAIGHT_LINE
                }
                formState = formState.copy(
                    depreciationMethod = newMethod,
                    depreciableBase = settings.depreciableBase,
                    startConvention = settings.startConvention,
                    frequency = settings.frequency,
                    rounding = settings.rounding,
                    impairmentTracking = settings.impairmentTracking,
                    revaluationEnabled = settings.revaluationEnabled,
                    allowLifeChange = settings.allowLifeChange,
                    allowResidualChange = settings.allowResidualChange,
                    allowMethodChange = settings.allowMethodChange
                )
                showAdvancedSettingsSheet = false
            },
            onDismiss = { showAdvancedSettingsSheet = false }
        )
    }

    // 1. DIÁLOGO DE CONFIRMACIÓN AL DAR ATRÁS SIN GUARDAR
    if (showDiscardConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmDialog = false },
            containerColor = Color(0xFF0F1E36),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1),
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "¿Descartar o guardar cambios?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "Tienes datos sin guardar en el activo fijo. ¿Deseas guardar los cambios antes de salir o descartarlos?",
                    fontSize = 13.5.sp,
                    color = Color(0xFF94A3B8)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardConfirmDialog = false
                        if (formState.isValid) {
                            onSaveAsset(formState)
                        } else {
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (formState.isValid) "Guardar y Salir" else "Entendido", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDiscardConfirmDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Descartar")
                }
            }
        )
    }

    // 2. POP-UP DE IMPLICACIONES FINANCIERAS Y CONTABLES (NIC 16 / NIIF)
    if (showFinancialImpactDialog) {
        val depreciableBase = (formState.acquisitionValue - formState.residualValue).coerceAtLeast(0.0)
        val monthlyCuota = if (formState.usefulLifeMonths > 0) depreciableBase / formState.usefulLifeMonths else 0.0
        val annualCuota = monthlyCuota * 12.0
        val isExtended = formState.usefulLifeMonths > (formState.category.defaultUsefulLifeMonths)

        AlertDialog(
            onDismissRequest = { showFinancialImpactDialog = false },
            containerColor = Color(0xFF0F1E36),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1),
            icon = {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    tint = accentCyan,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Implicaciones Financieras y Contables",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.5.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0A1526),
                        border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Gasto mensual estimado:", fontSize = 12.sp, color = textSecondary)
                                Text("${moneyFmt.format(monthlyCuota)} COP", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Impacto anual en P&L:", fontSize = 12.sp, color = textSecondary)
                                Text("${moneyFmt.format(annualCuota)} COP/año", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = accentCyan)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Valor residual al final:", fontSize = 12.sp, color = textSecondary)
                                Text("${moneyFmt.format(formState.residualValue)} COP", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF10B981))
                            }
                        }
                    }

                    Text(
                        text = if (isExtended) {
                            "⚡ Mayor vida útil (${formState.usefulLifeMonths} meses): Reduce la cuota mensual de depreciación mejorando el margen operativo a corto plazo, pero mantendrá el activo más tiempo registrado en libros."
                        } else {
                            "⚡ Menor vida útil (${formState.usefulLifeMonths} meses): Incrementa la cuota de desgaste mensual acelerando la amortización contable y el beneficio tributario bajo NIC 16."
                        },
                        fontSize = 12.sp,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFinancialImpactDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Entendido", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 3. DIÁLOGO MÁGICO DE SUGERENCIA IA
    if (showAiSuggestionDialog && currentAiSuggestion != null) {
        val sug = currentAiSuggestion!!
        AlertDialog(
            onDismissRequest = { showAiSuggestionDialog = false },
            containerColor = Color(0xFF0F1E36),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1),
            icon = {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFA5B4FC),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Sugerencia Inteligente NIIF",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.5.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = sug.justification,
                        fontSize = 12.5.sp,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 17.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0B172B),
                        border = BorderStroke(1.dp, Color(0xFF312E81)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Vida útil sugerida: ${sug.suggestedUsefulLifeMonths} meses (${sug.suggestedUsefulLifeYears} años)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5B4FC))
                            Text("Método sugerido: ${sug.suggestedDepreciationMethod.name}", fontSize = 11.sp, color = textSecondary)
                            Text("Salvamento sugerido: ${(sug.suggestedResidualPercent * 100).toInt()}%", fontSize = 11.sp, color = textSecondary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        formState = formState.copy(
                            usefulLifeMonths = sug.suggestedUsefulLifeMonths,
                            depreciationMethod = sug.suggestedDepreciationMethod,
                            residualValueText = (formState.acquisitionValue * sug.suggestedResidualPercent).toLong().toString()
                        )
                        usefulLifeInputText = sug.suggestedUsefulLifeMonths.toString()
                        showAiSuggestionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Aplicar Sugerencia", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiSuggestionDialog = false }) {
                    Text("Cancelar", color = textSecondary)
                }
            }
        )
    }

    // 4. DIÁLOGO DE RECALCULAR DEPRECIACIÓN HISTÓRICA POR FECHA DE COMPRA ANTERIOR
    if (showHistoricalDepreciationDialog) {
        val formattedHistorical = moneyFmt.format(calculatedHistoricalDepreciation)
        val remainingBookValue = (formState.acquisitionValue - calculatedHistoricalDepreciation).coerceAtLeast(formState.residualValue)
        val formattedBookValue = moneyFmt.format(remainingBookValue)

        AlertDialog(
            onDismissRequest = { showHistoricalDepreciationDialog = false },
            containerColor = Color(0xFF0F1E36),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1),
            icon = {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = null,
                    tint = accentCyan,
                    modifier = Modifier.size(34.dp)
                )
            },
            title = {
                Text(
                    text = "Fecha de compra anterior detectada",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.5.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "La fecha de compra ingresada (${formState.acquisitionDate}) es anterior a la fecha actual ($elapsedMonthsForHistorical meses transcurridos).",
                        fontSize = 13.5.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    Surface(
                        color = Color(0xFF081324),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Depreciación acumulada:", fontSize = 12.sp, color = textSecondary)
                                Text(formattedHistorical, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF87171))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Nuevo valor neto en libros:", fontSize = 12.sp, color = textSecondary)
                                Text(formattedBookValue, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                            }
                        }
                    }
                    Text(
                        text = "¿Deseas recalcular automáticamente la depreciación acumulada desde la fecha de compra hasta hoy?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showHistoricalDepreciationDialog = false
                        val updated = formState.copy(
                            accumulatedDepreciation = calculatedHistoricalDepreciation,
                            currentMarketValue = remainingBookValue
                        )
                        persistSupplierIfNew()
                        onSaveAsset(updated)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Sí, recalcular depreciación", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showHistoricalDepreciationDialog = false
                        persistSupplierIfNew()
                        onSaveAsset(formState)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("No, guardar sin depreciar", fontSize = 12.5.sp)
                }
            }
        )
    }

    if (showExpressThirdPartySheet) {
        ThirdPartyFormModalSheet(
            formState = expressFormState,
            onUpdateForm = { reducer -> expressFormState = reducer(expressFormState) },
            onSave = {
                if (expressFormState.name.isBlank()) {
                    expressErrorMessage = "El nombre o razón social es obligatorio"
                    return@ThirdPartyFormModalSheet
                }
                if (expressFormState.idNumber.isBlank()) {
                    expressErrorMessage = "La identificación o NIT es obligatoria"
                    return@ThirdPartyFormModalSheet
                }
                coroutineScope.launch {
                    val entity = expressFormState.toEntity("user_default")
                    val result = appContainer?.thirdPartyRepository?.saveThirdParty(entity)
                    result?.onSuccess { saved ->
                        formState = formState.copy(supplier = saved.displayName)
                        showExpressThirdPartySheet = false
                        expressErrorMessage = null
                    }?.onFailure { err ->
                        expressErrorMessage = err.message ?: "Error al guardar tercero"
                    }
                }
            },
            onDismiss = { showExpressThirdPartySheet = false },
            errorMessage = expressErrorMessage
        )
    }

    if (showSelectThirdPartyDialog) {
        SelectThirdPartyPopUpDialog(
            thirdParties = thirdParties,
            onSelectThirdParty = { tp ->
                formState = formState.copy(supplier = tp.displayName)
                showSelectThirdPartyDialog = false
            },
            onOpenCreateNew = {
                showSelectThirdPartyDialog = false
                expressErrorMessage = null
                expressFormState = ThirdPartyFormState(
                    name = "",
                    isExpressMode = true,
                    category = if (formState.categoryName.contains("Vehículo", ignoreCase = true)) "CONCESIONARIO" else "PROVEEDOR"
                )
                showExpressThirdPartySheet = true
            },
            onDismiss = { showSelectThirdPartyDialog = false }
        )
    }
}
}
