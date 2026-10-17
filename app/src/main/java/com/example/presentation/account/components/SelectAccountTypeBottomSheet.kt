package com.example.presentation.account.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.account.components.CurrencyItemInfo
import com.example.presentation.account.components.CurrencyPickerModal
import com.example.presentation.account.components.WORLD_CURRENCIES_CATALOG
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Account type item model matching the KashApp design reference.
 */
data class AccountTypeItem(
    val id: String,
    val typeCode: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconTint: Color,
    val iconBackground: Color,
    val defaultNamePlaceholder: String
)

/**
 * Visual transformation for formatting numbers with dots as thousands separators.
 */
private class SheetCurrencyDotVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val parts = raw.split(".")
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) parts[1] else null

        val formattedInt = try {
            val num = integerPart.toLongOrNull() ?: 0L
            val symbols = DecimalFormatSymbols(Locale.GERMAN).apply { groupingSeparator = '.' }
            DecimalFormat("#,###", symbols).format(num)
        } catch (_: Exception) {
            integerPart
        }

        val formatted = if (decimalPart != null) "$formattedInt,$decimalPart" else formattedInt

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = formatted.length.coerceAtMost(formatted.length)
            override fun transformedToOriginal(offset: Int): Int = raw.length.coerceAtMost(raw.length)
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

/**
 * Bottom Sheet Modal for selecting and creating an account.
 * Styled with the exact luxury Dark Navy aesthetic of the Left Drawer Menu:
 * - Gradient header with KashApp branding
 * - Sleek translucent dark cards with glow accents
 * - 164 ISO currencies picker integration
 * - Real-time currency formatted inputs with high-contrast text
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectAccountTypeBottomSheet(
    onDismiss: () -> Unit,
    onOpenDetailedCreateAccount: ((typeCode: String) -> Unit)? = null,
    onCreateAccount: (name: String, typeCode: String, initialBalance: Double, currency: String) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var selectedType by remember { mutableStateOf<AccountTypeItem?>(null) }

    val accountTypes = remember {
        listOf(
            AccountTypeItem(
                id = "savings",
                typeCode = "SAVINGS",
                title = "Cuenta de ahorros",
                description = "Ideal para guardar tu dinero y generar rendimientos seguros.",
                icon = Icons.Outlined.CreditCard,
                iconTint = Color(0xFF10B981),
                iconBackground = Color(0xFF10B981).copy(alpha = 0.18f),
                defaultNamePlaceholder = "Ej. Ahorros Bancolombia"
            ),
            AccountTypeItem(
                id = "checking",
                typeCode = "CHECKING",
                title = "Cuenta corriente",
                description = "Para tus movimientos diarios, cheques y sobregiros.",
                icon = Icons.Default.CreditCard,
                iconTint = Color(0xFF38BDF8),
                iconBackground = Color(0xFF0284C7).copy(alpha = 0.20f),
                defaultNamePlaceholder = "Ej. Corriente BBVA"
            ),
            AccountTypeItem(
                id = "payroll",
                typeCode = "PAYROLL",
                title = "Cuenta de nómina",
                description = "Recibe tu salario mensual y gestiona deducciones automáticas.",
                icon = Icons.Default.Business,
                iconTint = Color(0xFF818CF8),
                iconBackground = Color(0xFF6366F1).copy(alpha = 0.20f),
                defaultNamePlaceholder = "Ej. Nómina Principal"
            ),
            AccountTypeItem(
                id = "cdt",
                typeCode = "CDT",
                title = "Cuenta CDT (Depósito a término)",
                description = "Inversión a plazo fijo con tasa garantizada y alta rentabilidad.",
                icon = Icons.Default.Lock,
                iconTint = Color(0xFFF59E0B),
                iconBackground = Color(0xFFF59E0B).copy(alpha = 0.20f),
                defaultNamePlaceholder = "Ej. CDT Bancolombia 180 días"
            ),
            AccountTypeItem(
                id = "credit_card",
                typeCode = "CREDIT_CARD",
                title = "Tarjeta de crédito",
                description = "Línea de crédito rotativa con fechas de corte y pagos diferidos.",
                icon = Icons.Default.CreditCard,
                iconTint = Color(0xFFA855F7),
                iconBackground = Color(0xFFA855F7).copy(alpha = 0.20f),
                defaultNamePlaceholder = "Ej. Visa Platinum"
            ),
            AccountTypeItem(
                id = "other",
                typeCode = "OTHER",
                title = "Otra cuenta / Billetera digital",
                description = "Billeteras virtuales, fondos colectivos o efectivo en mano.",
                icon = Icons.Default.AccountBalance,
                iconTint = Color(0xFF94A3B8),
                iconBackground = Color(0xFF64748B).copy(alpha = 0.22f),
                defaultNamePlaceholder = "Ej. Nequi / Daviplata / Efectivo"
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF334155))
            )
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .testTag("select_account_type_sheet")
        ) {
            if (selectedType == null) {
                // Step 1: Type Selection Screen matching Drawer aesthetic
                AccountTypeSelectionView(
                    accountTypes = accountTypes,
                    onSelectType = { item ->
                        if (onOpenDetailedCreateAccount != null) {
                            onOpenDetailedCreateAccount(item.typeCode)
                            onDismiss()
                        } else {
                            selectedType = item
                        }
                    },
                    onDismiss = onDismiss
                )
            } else {
                // Step 2: Name & Balance creation form
                AccountDetailsCreationView(
                    typeItem = selectedType!!,
                    onBack = { selectedType = null },
                    onSave = { name, initialBalance, currency ->
                        onCreateAccount(name, selectedType!!.typeCode, initialBalance, currency)
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Step 1: Select Account Type Menu matching the Left Drawer visual style
 */
@Composable
private fun AccountTypeSelectionView(
    accountTypes: List<AccountTypeItem>,
    onSelectType: (AccountTypeItem) -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // ==========================================
        // HEADER: Dark Gradient Brand Header
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF032B43),
                            Color(0xFF061E33),
                            Color(0xFF0F172A)
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Glowing Bank Brand Badge
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = Color(0xFF007BFF),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NUEVA CUENTA",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "Selecciona el tipo",
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }

                // Close button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .testTag("close_select_account_sheet_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Elige la categoría financiera de la cuenta que deseas agregar a tu patrimonio:",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            // Account Type Cards List
            accountTypes.forEach { item ->
                AccountTypeRowCard(
                    item = item,
                    onClick = { onSelectType(item) }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Footer info card: "¿No encuentras tu banco?"
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        val otherType = accountTypes.firstOrNull { it.id == "other" } ?: accountTypes.first()
                        onSelectType(otherType)
                    }
                    .testTag("help_find_bank_card"),
                color = Color(0xFF0284C7).copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "¿No encuentras tu entidad?",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Puedes registrar cualquier banco o billetera manualmente.",
                            style = TextStyle(
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Individual Account Type Card matching the Left Drawer design with subtle glow.
 */
@Composable
private fun AccountTypeRowCard(
    item: AccountTypeItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("account_type_item_${item.id}"),
        color = Color(0xFF1E293B).copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Pill
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(item.iconBackground)
                    .border(1.dp, item.iconTint.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 15.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Seleccionar",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Step 2: Name and Balance configuration for the selected account type.
 * Features black text inside input fields, full 164 currency picker, and currency formatting.
 */
@Composable
private fun AccountDetailsCreationView(
    typeItem: AccountTypeItem,
    onBack: () -> Unit,
    onSave: (name: String, initialBalance: Double, currency: String) -> Unit
) {
    var accountName by remember { mutableStateOf("") }
    var balanceText by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("COP") }
    var showCurrencyModal by remember { mutableStateOf(false) }

    val selectedCurrencyInfo = remember(selectedCurrency) {
        WORLD_CURRENCIES_CATALOG.find { it.code.equals(selectedCurrency, ignoreCase = true) }
            ?: CurrencyItemInfo(code = selectedCurrency, name = selectedCurrency, symbol = "$", flag = "🌐")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        // Back Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f))
                    .testTag("back_from_account_form_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Regresar",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Configura tu ${typeItem.title}",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Ingresa los datos para registrar la cuenta en KashApp.",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Selected Type Badge Preview
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, typeItem.iconTint.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(typeItem.iconBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = typeItem.icon,
                        contentDescription = null,
                        tint = typeItem.iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = typeItem.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = typeItem.description,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Account Name Input
        Text(
            text = "NOMBRE DE LA CUENTA",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8),
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = accountName,
            onValueChange = { accountName = it },
            placeholder = { Text(typeItem.defaultNamePlaceholder, color = Color(0xFF94A3B8), fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("account_name_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF0F172A),
                unfocusedTextColor = Color(0xFF0F172A),
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFFCBD5E1),
                cursorColor = Color(0xFF0284C7),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFF8FAFC)
            ),
            textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Initial Balance Input with Currency Dot Formatting
        Text(
            text = "SALDO INICIAL DISPONIBLE (FORMATO MONEDA)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8),
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = balanceText,
            onValueChange = { input ->
                val filtered = input.filter { it.isDigit() || it == '.' }
                if (filtered.count { it == '.' } <= 1) {
                    balanceText = filtered
                }
            },
            placeholder = { Text("0", color = Color(0xFF94A3B8), fontSize = 13.sp) },
            visualTransformation = SheetCurrencyDotVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            leadingIcon = {
                Text(
                    text = selectedCurrencyInfo.symbol,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("account_initial_balance_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF0F172A),
                unfocusedTextColor = Color(0xFF0F172A),
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFFCBD5E1),
                cursorColor = Color(0xFF0284C7),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFF8FAFC)
            ),
            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Currency Selector (Full 164 ISO catalog + custom currency creation)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MONEDA DE LA CUENTA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8),
                letterSpacing = 0.5.sp
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0284C7).copy(alpha = 0.2f),
                modifier = Modifier.clickable { showCurrencyModal = true }
            ) {
                Text(
                    text = "164 Monedas",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .clickable { showCurrencyModal = true }
                .testTag("currency_picker_trigger_btn"),
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = selectedCurrencyInfo.flag,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${selectedCurrencyInfo.code} (${selectedCurrencyInfo.symbol})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = selectedCurrencyInfo.name,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Cambiar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Seleccionar moneda",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Live Currency Conversion Preview (e.g. USD -> COP or viceversa)
        val balNum = com.example.domain.currency.CurrencyConversionEngine.parseUserInputAmount(balanceText)
        Spacer(modifier = Modifier.height(14.dp))
        LiveCurrencyConversionPreview(
            amount = balNum,
            inputCurrency = selectedCurrency,
            isDarkTheme = true
        )

        Spacer(modifier = Modifier.height(26.dp))

        // Save Account Button
        val isFormValid = accountName.isNotBlank()
        Button(
            onClick = {
                val initBal = com.example.domain.currency.CurrencyConversionEngine.parseUserInputAmount(balanceText)
                onSave(accountName.trim(), initBal, selectedCurrency)
            },
            enabled = isFormValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_account_submit_btn"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF007BFF),
                disabledContainerColor = Color(0xFF334155)
            )
        ) {
            Text(
                text = "Guardar cuenta",
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFormValid) Color.White else Color(0xFF64748B)
                )
            )
        }
    }

    // Modal: Currency Picker Modal (164 ISO Currencies + Custom creation)
    if (showCurrencyModal) {
        CurrencyPickerModal(
            selectedCurrencyCode = selectedCurrency,
            onCurrencySelected = { code ->
                selectedCurrency = code
                showCurrencyModal = false
            },
            onDismiss = { showCurrencyModal = false }
        )
    }
}
