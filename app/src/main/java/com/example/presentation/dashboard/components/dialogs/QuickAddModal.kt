package com.example.presentation.dashboard.components.dialogs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KashNavy

data class QuickAddOption(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconColor: Color,
    val backgroundColor: Color
)

/**
 * Modal Bottom Sheet: "¿Qué quieres agregar?"
 * Exact reproduction of user design with two categorized sections (MOVIMIENTOS & ORGANIZA TUS FINANZAS),
 * elegant 2-column cards with styled rounded icon containers, and bottom info banner.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddModal(
    onDismiss: () -> Unit,
    onOpenCreateExpense: (() -> Unit)? = null,
    onOpenCreateIncome: (() -> Unit)? = null,
    onOpenCreateTransfer: (() -> Unit)? = null,
    onOpenCreateCardPurchase: (() -> Unit)? = null,
    onOpenSelectAccountType: (() -> Unit)? = null,
    onOpenCreateCreditCard: (() -> Unit)? = null,
    onOpenCreateGoal: (() -> Unit)? = null,
    onOpenCreateBudget: (() -> Unit)? = null,
    onOpenCreateInvestment: (() -> Unit)? = null,
    onOpenReceiptScanner: (() -> Unit)? = null,
    onOpenSharedExpenses: (() -> Unit)? = null,
    onAddTransaction: (String, String, String, Double, Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedOption by remember { mutableStateOf<QuickAddOption?>(null) }

    // Section 1: MOVIMIENTOS
    val movimientosOptions = remember {
        listOf(
            QuickAddOption(
                id = "gasto",
                title = "Gasto",
                description = "Registra un nuevo gasto o compra.",
                icon = Icons.Default.ReceiptLong,
                iconColor = Color(0xFFEF4444),
                backgroundColor = Color(0xFFFEE2E2)
            ),
            QuickAddOption(
                id = "gasto_compartido",
                title = "Gasto Compartido",
                description = "Divide cuenta, reserva Hold y emite Ticket QR.",
                icon = Icons.Default.ReceiptLong,
                iconColor = Color(0xFF059669),
                backgroundColor = Color(0xFFD1FAE5)
            ),
            QuickAddOption(
                id = "ingreso",
                title = "Ingreso",
                description = "Registra un ingreso o abono.",
                icon = Icons.Default.ArrowUpward,
                iconColor = Color(0xFF10B981),
                backgroundColor = Color(0xFFD1FAE5)
            ),
            QuickAddOption(
                id = "transferencia",
                title = "Transferencia",
                description = "Mueve dinero entre tus cuentas.",
                icon = Icons.Default.SwapHoriz,
                iconColor = Color(0xFF0284C7),
                backgroundColor = Color(0xFFE0F2FE)
            ),
            QuickAddOption(
                id = "compra_tarjeta",
                title = "Compra con tarjeta",
                description = "Registra un gasto en tu tarjeta.",
                icon = Icons.Default.CreditCard,
                iconColor = Color(0xFF8B5CF6),
                backgroundColor = Color(0xFFF3E8FF)
            ),
            QuickAddOption(
                id = "ocr_scanner",
                title = "Escanear Factura OCR",
                description = "Digitaliza recibo automáticamente.",
                icon = Icons.Default.DocumentScanner,
                iconColor = Color(0xFF0284C7),
                backgroundColor = Color(0xFFE0F2FE)
            )
        )
    }

    // Section 2: ORGANIZA TUS FINANZAS
    val organizacionOptions = remember {
        listOf(
            QuickAddOption(
                id = "cuenta",
                title = "Cuenta",
                description = "Agrega una cuenta bancaria.",
                icon = Icons.Default.AccountBalance,
                iconColor = Color(0xFF0891B2),
                backgroundColor = Color(0xFFCFFAFE)
            ),
            QuickAddOption(
                id = "deuda",
                title = "Deuda",
                description = "Registra una deuda o préstamo.",
                icon = Icons.Default.MonetizationOn,
                iconColor = Color(0xFFD97706),
                backgroundColor = Color(0xFFFEF3C7)
            ),
            QuickAddOption(
                id = "objetivo",
                title = "Objetivo",
                description = "Crea una meta de ahorro.",
                icon = Icons.Default.Savings,
                iconColor = Color(0xFF9333EA),
                backgroundColor = Color(0xFFEDE9FE)
            ),
            QuickAddOption(
                id = "presupuesto",
                title = "Presupuesto",
                description = "Define límites para tus gastos.",
                icon = Icons.Default.CalendarMonth,
                iconColor = Color(0xFF059669),
                backgroundColor = Color(0xFFD1FAE5)
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState())
                .testTag("quick_add_modal_sheet")
        ) {
            AnimatedContent(
                targetState = selectedOption,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "QuickAddContentTransition"
            ) { option ->
                if (option == null) {
                    // Main Categorized Grid View
                    QuickAddCategorizedView(
                        movimientosOptions = movimientosOptions,
                        organizacionOptions = organizacionOptions,
                        onSelectOption = { selected ->
                            when (selected.id) {
                                "gasto" -> {
                                    onDismiss()
                                    onOpenCreateExpense?.invoke()
                                }
                                "gasto_compartido" -> {
                                    onDismiss()
                                    onOpenSharedExpenses?.invoke()
                                }
                                "ingreso" -> {
                                    onDismiss()
                                    onOpenCreateIncome?.invoke()
                                }
                                "transferencia" -> {
                                    onDismiss()
                                    onOpenCreateTransfer?.invoke()
                                }
                                "compra_tarjeta" -> {
                                    onDismiss()
                                    onOpenCreateCardPurchase?.invoke() ?: onOpenCreateCreditCard?.invoke() ?: onOpenCreateExpense?.invoke()
                                }
                                "ocr_scanner" -> {
                                    onDismiss()
                                    onOpenReceiptScanner?.invoke()
                                }
                                "cuenta" -> {
                                    onDismiss()
                                    onOpenSelectAccountType?.invoke()
                                }
                                "deuda" -> {
                                    onDismiss()
                                    onOpenCreateCreditCard?.invoke()
                                }
                                "objetivo" -> {
                                    onDismiss()
                                    onOpenCreateGoal?.invoke()
                                }
                                "presupuesto" -> {
                                    onDismiss()
                                    onOpenCreateBudget?.invoke()
                                }
                                else -> {
                                    selectedOption = selected
                                }
                            }
                        },
                        onDismiss = onDismiss
                    )
                } else {
                    // Detailed Dynamic Registration Form fallback
                    QuickAddDetailForm(
                        option = option,
                        onBack = { selectedOption = null },
                        onDismiss = onDismiss,
                        onAddTransaction = onAddTransaction
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QuickAddCategorizedView(
    movimientosOptions: List<QuickAddOption>,
    organizacionOptions: List<QuickAddOption>,
    onSelectOption: (QuickAddOption) -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Top Header: Title & Subtitle + Close 'X' Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "¿Qué quieres agregar?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Selecciona una opción para registrar o crear un nuevo elemento.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
                    .testTag("quick_add_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 1: MOVIMIENTOS
        Text(
            text = "MOVIMIENTOS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF007BFF),
            letterSpacing = 0.6.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2x2 Grid for Movimientos
        OptionsGrid2Column(
            options = movimientosOptions,
            onSelectOption = onSelectOption
        )

        Spacer(modifier = Modifier.height(22.dp))

        // SECTION 2: ORGANIZA TUS FINANZAS
        Text(
            text = "ORGANIZA TUS FINANZAS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF007BFF),
            letterSpacing = 0.6.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2x2 Grid for Organiza tus finanzas
        OptionsGrid2Column(
            options = organizacionOptions,
            onSelectOption = onSelectOption
        )

        Spacer(modifier = Modifier.height(22.dp))

        // BOTTOM INFORMATION BANNER
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFECFEFF),
            border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFBAE6FD)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Tus registros se reflejarán de inmediato en el panorama financiero de KashAPP.",
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF0369A1),
                    lineHeight = 16.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OptionsGrid2Column(
    options: List<QuickAddOption>,
    onSelectOption: (QuickAddOption) -> Unit
) {
    val rows = options.chunked(2)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowOptions.forEach { option ->
                    Box(modifier = Modifier.weight(1f)) {
                        QuickAddGridItem(
                            option = option,
                            onClick = { onSelectOption(option) }
                        )
                    }
                }
                if (rowOptions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickAddGridItem(
    option: QuickAddOption,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("quick_add_item_${option.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rounded Square Icon Container
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(option.backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = option.title,
                    tint = option.iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = option.description,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuickAddDetailForm(
    option: QuickAddOption,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onAddTransaction: (String, String, String, Double, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        // Back Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = KashNavy,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Volver",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = KashNavy
                    )
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Option Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(option.backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = option.title,
                    tint = option.iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Registrar ${option.title}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = option.description,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Concept / Name Field
        Text(
            text = "Concepto / Descripción",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF334155)
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Ej: Supermercado, Salario, Transferencia", color = Color(0xFF94A3B8), fontSize = 14.sp) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF007BFF),
                unfocusedBorderColor = Color(0xFFE2E8F0)
            ),
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontSize = 14.sp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Amount Field
        Text(
            text = "Monto (COP)",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF334155)
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
            placeholder = { Text("0.00", color = Color(0xFF94A3B8), fontSize = 14.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF007BFF),
                unfocusedBorderColor = Color(0xFFE2E8F0)
            ),
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Optional Notes Field
        Text(
            text = "Notas adicionales (opcional)",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF334155)
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            placeholder = { Text("Añade detalles o categorías...", color = Color(0xFF94A3B8), fontSize = 14.sp) },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF007BFF),
                unfocusedBorderColor = Color(0xFFE2E8F0)
            ),
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontSize = 14.sp),
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Submit Button
        Button(
            onClick = {
                val amount = amountText.toDoubleOrNull() ?: 0.0
                onAddTransaction(option.id, title, notes, amount, isRecurring)
                onDismiss()
            },
            enabled = title.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0A192F),
                disabledContainerColor = Color(0xFFE2E8F0)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = "Guardar ${option.title}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
