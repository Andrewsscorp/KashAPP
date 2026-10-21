package com.example.feature.capturaautomatica.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.PendingAutoCaptureEntity
import com.example.feature.capturaautomatica.domain.model.AutoCaptureAccountOption
import com.example.feature.capturaautomatica.domain.model.AutoCaptureCategoryOption
import com.example.feature.capturaautomatica.domain.model.AutoCaptureTagOption
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bandeja modal de revisión de capturas automáticas bancarias adaptada al tema azul de KashApp.
 * Permite seleccionar interactivamente la Cuenta Bancaria destino, la Categoría real de la base de datos
 * y las Etiquetas (tags) creadas por el usuario, con persistencia inmediata en Room SQLite ante apagados.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCaptureInboxSheet(
    pendingCaptures: List<PendingAutoCaptureEntity>,
    availableAccounts: List<AutoCaptureAccountOption> = emptyList(),
    availableCategories: List<AutoCaptureCategoryOption> = emptyList(),
    availableTags: List<AutoCaptureTagOption> = emptyList(),
    onConfirmCapture: (
        captureId: String,
        customAccountId: String?,
        customAccountName: String?,
        customCategoryId: String?,
        customCategoryName: String?,
        customTags: List<String>
    ) -> Unit,
    onDraftChange: (
        captureId: String,
        accountId: String?,
        accountName: String?,
        categoryId: String?,
        categoryName: String?
    ) -> Unit = { _, _, _, _, _ -> },
    onDismissCapture: (captureId: String) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF07172B), // KashApp Fondo Azul Oscuro
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header con temática azul
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Movimientos Detectados",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Lectura 100% en dispositivo • Cifrado en disco • Ley 1581",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF38BDF8)
                        )
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (pendingCaptures.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "¡Bandeja al día!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "No tienes notificaciones bancarias pendientes por confirmar",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8)
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(pendingCaptures, key = { it.id }) { capture ->
                        PendingCaptureCard(
                            capture = capture,
                            availableAccounts = availableAccounts,
                            availableCategories = availableCategories,
                            availableTags = availableTags,
                            onDraftChange = { accId, accName, catId, catName ->
                                onDraftChange(capture.id, accId, accName, catId, catName)
                            },
                            onConfirm = { accId, accName, catId, catName, tags ->
                                onConfirmCapture(capture.id, accId, accName, catId, catName, tags)
                            },
                            onDismiss = { onDismissCapture(capture.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sobrecarga de compatibilidad anterior para llamadas que no suministran categorías o tags.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCaptureInboxSheet(
    pendingCaptures: List<PendingAutoCaptureEntity>,
    availableAccounts: List<AutoCaptureAccountOption> = emptyList(),
    onConfirmCapture: (captureId: String, customAccountId: String?, customAccountName: String?) -> Unit,
    onDismissCapture: (captureId: String) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    AutoCaptureInboxSheet(
        pendingCaptures = pendingCaptures,
        availableAccounts = availableAccounts,
        availableCategories = emptyList(),
        availableTags = emptyList(),
        onConfirmCapture = { id, accId, accName, _, _, _ ->
            onConfirmCapture(id, accId, accName)
        },
        onDismissCapture = onDismissCapture,
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PendingCaptureCard(
    capture: PendingAutoCaptureEntity,
    availableAccounts: List<AutoCaptureAccountOption>,
    availableCategories: List<AutoCaptureCategoryOption>,
    availableTags: List<AutoCaptureTagOption>,
    onDraftChange: (selectedAccountId: String?, selectedAccountName: String?, selectedCategoryId: String?, selectedCategoryName: String?) -> Unit,
    onConfirm: (
        selectedAccountId: String?,
        selectedAccountName: String?,
        selectedCategoryId: String?,
        selectedCategoryName: String?,
        selectedTags: List<String>
    ) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val localeCo = Locale.Builder().setLanguage("es").setRegion("CO").build()
    val formattedAmount = try {
        val format = NumberFormat.getCurrencyInstance(localeCo)
        format.maximumFractionDigits = 0
        format.format(capture.amount)
    } catch (e: Exception) {
        "$${capture.amount.toLong()}"
    }

    val dateStr = try {
        SimpleDateFormat("dd MMM, hh:mm a", localeCo).format(Date(capture.notificationTimestamp))
    } catch (e: Exception) {
        ""
    }

    val isIncome = capture.transactionType == "INCOME"

    // Resolver cuenta inicial
    var selectedAccount by remember(capture.id, availableAccounts) {
        mutableStateOf(
            availableAccounts.find { it.id == capture.matchedAccountId }
                ?: availableAccounts.find {
                    it.bankName.contains(capture.detectedBankName, ignoreCase = true) ||
                        it.name.contains(capture.detectedBankName, ignoreCase = true)
                }
                ?: availableAccounts.firstOrNull()
        )
    }

    val defaultCatName = capture.matchedCategoryName
    // Resolver categoría inicial desde la base de datos de KashApp
    var selectedCategory by remember(capture.id, availableCategories) {
        mutableStateOf(
            availableCategories.find { it.id == capture.matchedCategoryId }
                ?: availableCategories.find {
                    !defaultCatName.isNullOrBlank() &&
                        it.name.contains(defaultCatName, ignoreCase = true)
                }
                ?: availableCategories.find { it.type == capture.transactionType || it.type == "BOTH" }
                ?: availableCategories.firstOrNull()
        )
    }

    // Etiquetas seleccionadas interactivamente para esta captura
    val selectedTags = remember(capture.id) { mutableStateListOf<String>() }

    var isAccountSelectorOpen by remember { mutableStateOf(false) }
    var isCategorySelectorOpen by remember { mutableStateOf(false) }
    var isTagSelectorOpen by remember { mutableStateOf(false) }

    // Color de acento del banco
    val bankAccentColor = when {
        capture.detectedBankName.contains("Nequi", ignoreCase = true) -> Color(0xFFE00070)
        capture.detectedBankName.contains("Bancolombia", ignoreCase = true) -> Color(0xFFFDDA24)
        capture.detectedBankName.contains("Nu", ignoreCase = true) -> Color(0xFF820AD1)
        capture.detectedBankName.contains("Davivienda", ignoreCase = true) ||
            capture.detectedBankName.contains("DaviPlata", ignoreCase = true) -> Color(0xFFED1C24)
        capture.detectedBankName.contains("BBVA", ignoreCase = true) -> Color(0xFF004481)
        capture.detectedBankName.contains("Lulo", ignoreCase = true) -> Color(0xFF00E5FF)
        capture.detectedBankName.contains("Bogot", ignoreCase = true) -> Color(0xFF004884)
        capture.detectedBankName.contains("Falabella", ignoreCase = true) -> Color(0xFF00843D)
        capture.detectedBankName.contains("Colpatria", ignoreCase = true) -> Color(0xFFE31837)
        capture.detectedBankName.contains("Dale", ignoreCase = true) -> Color(0xFFFFCC00)
        capture.detectedBankName.contains("Rappi", ignoreCase = true) -> Color(0xFFFF441F)
        else -> Color(0xFF38BDF8)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F2440) // Azul oscuro KashApp
        ),
        border = BorderStroke(1.dp, Color(0xFF1E3A66)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Bank badge, date, amount
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
                            .background(bankAccentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                capture.detectedBankName.contains("Nequi", ignoreCase = true) ||
                                    capture.detectedBankName.contains("DaviPlata", ignoreCase = true) ||
                                    capture.detectedBankName.contains("Dale", ignoreCase = true) ||
                                    capture.detectedBankName.contains("Movii", ignoreCase = true) -> Icons.Default.Wallet
                                capture.lastFourDigits != null -> Icons.Default.CreditCard
                                else -> Icons.Default.AccountBalance
                            },
                            contentDescription = null,
                            tint = bankAccentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = capture.detectedBankName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFBAE6FD)
                            )
                        )
                    }
                }

                // Amount
                Text(
                    text = (if (isIncome) "+ " else "- ") + formattedAmount,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isIncome) Color(0xFF34D399) else Color(0xFFF87171)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Merchant / Details
            Text(
                text = capture.merchantOrCounterparty,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )

            if (!capture.lastFourDigits.isNullOrBlank()) {
                Text(
                    text = "Tarjeta: •••• ${capture.lastFourDigits}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF94A3B8)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Selectores Interactivos: Cuenta y Categoría (desde DB)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selector de Cuenta Bancaria Destino
                Surface(
                    onClick = { isAccountSelectorOpen = true },
                    color = Color(0xFF132D54),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E4976)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedAccount?.name
                                ?: capture.matchedAccountName
                                ?: "Elegir cuenta",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Cambiar cuenta",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Selector de Categoría (desde la base de datos de KashApp)
                Surface(
                    onClick = { isCategorySelectorOpen = true },
                    color = Color(0xFF132D54),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E4976)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedCategory?.name
                                ?: capture.matchedCategoryName
                                ?: "Categoría",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Cambiar categoría",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selector interactivo de Etiquetas (Tags desde base de datos)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Botón interactivo para añadir/editar etiquetas
                Surface(
                    onClick = { isTagSelectorOpen = true },
                    color = Color(0xFF132D54),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E4976))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Label,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (selectedTags.isEmpty()) "+ Etiqueta" else "Etiquetas (${selectedTags.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFBAE6FD),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                // Render de chips de etiquetas seleccionadas
                selectedTags.forEach { tagName ->
                    Surface(
                        color = Color(0xFF0284C7).copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#$tagName",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFBAE6FD),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Eliminar $tagName",
                                tint = Color(0xFFBAE6FD),
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable { selectedTags.remove(tagName) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Botones de Acción (Descartar y Confirmar con distribución simétrica y texto legible)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Descartar (No cortado, con padding controlado)
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF4444)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Descartar",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFEF4444)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Descartar",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }

                // Botón Confirmar
                Button(
                    onClick = {
                        onConfirm(
                            selectedAccount?.id,
                            selectedAccount?.name,
                            selectedCategory?.id,
                            selectedCategory?.name,
                            selectedTags.toList()
                        )
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirmar",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Confirmar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }

    // Modal de Selección de Cuenta Bancaria Destino con Glassmorphism Azul y Buscador
    if (isAccountSelectorOpen) {
        FrostedGlassAccountSelectorDialog(
            accounts = availableAccounts,
            selectedAccountId = selectedAccount?.id,
            onSelectAccount = { acc ->
                selectedAccount = acc
                onDraftChange(
                    acc.id,
                    acc.name,
                    selectedCategory?.id,
                    selectedCategory?.name
                )
                isAccountSelectorOpen = false
            },
            onDismissRequest = { isAccountSelectorOpen = false }
        )
    }

    // Modal de Selección de Categoría con Glassmorphism Azul y Buscador
    if (isCategorySelectorOpen) {
        FrostedGlassCategorySelectorDialog(
            categories = availableCategories,
            selectedCategoryId = selectedCategory?.id,
            onSelectCategory = { cat ->
                selectedCategory = cat
                onDraftChange(
                    selectedAccount?.id,
                    selectedAccount?.name,
                    cat.id,
                    cat.name
                )
                isCategorySelectorOpen = false
            },
            onDismissRequest = { isCategorySelectorOpen = false }
        )
    }

    // Modal de Selección de Etiquetas con Glassmorphism Azul y Buscador
    if (isTagSelectorOpen) {
        FrostedGlassTagSelectorDialog(
            tags = availableTags,
            initialSelectedTags = selectedTags.toList(),
            onConfirmTags = { newTags ->
                selectedTags.clear()
                selectedTags.addAll(newTags)
                isTagSelectorOpen = false
            },
            onDismissRequest = { isTagSelectorOpen = false }
        )
    }
}

/**
 * Diálogo modal Glassmorphism / Frosted Glass Azul para selección de Cuenta Destino con buscador en tiempo real.
 */
@Composable
fun FrostedGlassAccountSelectorDialog(
    accounts: List<AutoCaptureAccountOption>,
    selectedAccountId: String?,
    onSelectAccount: (AutoCaptureAccountOption) -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFA07172B),
                            Color(0xFA0A1E36),
                            Color(0xFD040D1A)
                        )
                    )
                )
                .border(
                    BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0x8038BDF8),
                                Color(0x330284C7),
                                Color(0x8038BDF8)
                            )
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0284C7).copy(alpha = 0.2f))
                                .border(BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Seleccionar Cuenta Destino",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Elige la cuenta para debitar o acreditar",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Buscador en tiempo real
                var searchQuery by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Buscar por nombre, banco o tipo...",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar búsqueda",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0F2440),
                        unfocusedContainerColor = Color(0xFF0B1B30),
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF1E3A66),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF38BDF8)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                val filteredAccounts = remember(searchQuery, accounts) {
                    if (searchQuery.isBlank()) {
                        accounts
                    } else {
                        val q = searchQuery.trim().lowercase()
                        accounts.filter { acc ->
                            acc.name.lowercase().contains(q) ||
                                acc.bankName.lowercase().contains(q) ||
                                acc.type.lowercase().contains(q)
                        }
                    }
                }

                // Resumen de resultados
                Text(
                    text = "Mostrando ${filteredAccounts.size} de ${accounts.size} cuentas",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (accounts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tienes cuentas bancarias registradas en KashApp todavía.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8))
                        )
                    }
                } else if (filteredAccounts.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F2440).copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, Color(0xFF1E3A66))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No se encontraron cuentas para '$searchQuery'",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = "Intenta buscar por el nombre de tu banco (ej: Bancolombia, Nequi)",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredAccounts, key = { it.id }) { acc ->
                            val isSelected = selectedAccountId == acc.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectAccount(acc) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFF16365C) else Color(0xFF0C1F38),
                                border = if (isSelected) {
                                    BorderStroke(1.5.dp, Color(0xFF38BDF8))
                                } else {
                                    BorderStroke(1.dp, Color(0xFF1E3A66))
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Icono de Cuenta
                                    val iconVector = when {
                                        acc.type.contains("CREDIT", ignoreCase = true) -> Icons.Default.CreditCard
                                        acc.type.contains("WALLET", ignoreCase = true) -> Icons.Default.Wallet
                                        else -> Icons.Default.AccountBalance
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) Color(0xFF0284C7).copy(alpha = 0.35f)
                                                else Color(0xFF132D54)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = iconVector,
                                            contentDescription = null,
                                            tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Detalles de la Cuenta
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = acc.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            ),
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF132D54),
                                                border = BorderStroke(0.5.dp, Color(0xFF38BDF8).copy(alpha = 0.3f))
                                            ) {
                                                Text(
                                                    text = acc.bankName,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFFBAE6FD),
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 10.sp
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = acc.type,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 10.sp
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Saldo y Checkmark
                                    Column(horizontalAlignment = Alignment.End) {
                                        val formattedBalance = try {
                                            val localeCo = Locale.Builder().setLanguage("es").setRegion("CO").build()
                                            val fmt = NumberFormat.getCurrencyInstance(localeCo).apply {
                                                maximumFractionDigits = 0
                                            }
                                            fmt.format(acc.balance)
                                        } catch (e: Exception) {
                                            "$${acc.balance.toLong()}"
                                        }
                                        Text(
                                            text = formattedBalance,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF34D399)
                                            )
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Seleccionada",
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(16.dp)
                                            )
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

/**
 * Diálogo modal Glassmorphism / Frosted Glass Azul para selección de Categoría desde Room SQLite con buscador en tiempo real.
 */
@Composable
fun FrostedGlassCategorySelectorDialog(
    categories: List<AutoCaptureCategoryOption>,
    selectedCategoryId: String?,
    onSelectCategory: (AutoCaptureCategoryOption) -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFA07172B),
                            Color(0xFA0A1E36),
                            Color(0xFD040D1A)
                        )
                    )
                )
                .border(
                    BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0x80818CF8),
                                Color(0x330284C7),
                                Color(0x8038BDF8)
                            )
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF818CF8).copy(alpha = 0.2f))
                                .border(BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.4f)), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Seleccionar Categoría",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Categorías reales de tu base de datos",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                var searchQuery by remember { mutableStateOf("") }
                var filterType by remember { mutableStateOf<String?>(null) } // null = Todas, "EXPENSE", "INCOME"

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Buscar categoría o tipo...",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0F2440),
                        unfocusedContainerColor = Color(0xFF0B1B30),
                        focusedBorderColor = Color(0xFF818CF8),
                        unfocusedBorderColor = Color(0xFF1E3A66),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF818CF8)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filtros rápidos de Tipo: Todas / Gastos / Ingresos
                val expenseCount = remember(categories) { categories.count { it.type.equals("EXPENSE", ignoreCase = true) } }
                val incomeCount = remember(categories) { categories.count { it.type.equals("INCOME", ignoreCase = true) } }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isAllSelected = filterType == null
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAllSelected) Color(0xFF1E3A66) else Color(0xFF0B1B30),
                        border = BorderStroke(1.dp, if (isAllSelected) Color(0xFF38BDF8) else Color(0xFF1E3A66)),
                        modifier = Modifier.clickable { filterType = null }
                    ) {
                        Text(
                            text = "Todas (${categories.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isAllSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }

                    val isExpenseSelected = filterType == "EXPENSE"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isExpenseSelected) Color(0xFF3F1922) else Color(0xFF0B1B30),
                        border = BorderStroke(1.dp, if (isExpenseSelected) Color(0xFFEF4444) else Color(0xFF1E3A66)),
                        modifier = Modifier.clickable { filterType = "EXPENSE" }
                    ) {
                        Text(
                            text = "Gastos ($expenseCount)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isExpenseSelected) Color(0xFFFCA5A5) else Color(0xFF94A3B8),
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }

                    val isIncomeSelected = filterType == "INCOME"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isIncomeSelected) Color(0xFF0D3327) else Color(0xFF0B1B30),
                        border = BorderStroke(1.dp, if (isIncomeSelected) Color(0xFF10B981) else Color(0xFF1E3A66)),
                        modifier = Modifier.clickable { filterType = "INCOME" }
                    ) {
                        Text(
                            text = "Ingresos ($incomeCount)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isIncomeSelected) Color(0xFF86EFAC) else Color(0xFF94A3B8),
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val filteredCategories = remember(searchQuery, filterType, categories) {
                    val q = searchQuery.trim().lowercase()
                    categories.filter { cat ->
                        val matchesSearch = q.isBlank() || cat.name.lowercase().contains(q) || cat.type.lowercase().contains(q)
                        val matchesType = filterType == null || cat.type.equals(filterType, ignoreCase = true)
                        matchesSearch && matchesType
                    }
                }

                if (categories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay categorías disponibles en la base de datos de KashApp.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8))
                        )
                    }
                } else if (filteredCategories.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F2440).copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, Color(0xFF1E3A66))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No se encontraron categorías para '$searchQuery'",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCategories, key = { it.id }) { cat ->
                            val isSelected = selectedCategoryId == cat.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectCategory(cat) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFF16365C) else Color(0xFF0C1F38),
                                border = if (isSelected) {
                                    BorderStroke(1.5.dp, Color(0xFF818CF8))
                                } else {
                                    BorderStroke(1.dp, Color(0xFF1E3A66))
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Círculo de color real de la categoría
                                    val catColor = try {
                                        if (cat.colorHex != null) {
                                            Color(android.graphics.Color.parseColor(cat.colorHex))
                                        } else {
                                            Color(0xFF818CF8)
                                        }
                                    } catch (e: Exception) {
                                        Color(0xFF818CF8)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(catColor.copy(alpha = 0.25f))
                                            .border(BorderStroke(1.5.dp, catColor), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cat.name.take(1).uppercase(),
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = catColor
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val isExpense = cat.type.equals("EXPENSE", ignoreCase = true)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isExpense) Color(0xFF3F1922) else Color(0xFF0D3327),
                                            border = BorderStroke(
                                                0.5.dp,
                                                if (isExpense) Color(0xFFEF4444).copy(alpha = 0.4f)
                                                else Color(0xFF10B981).copy(alpha = 0.4f)
                                            )
                                        ) {
                                            Text(
                                                text = if (isExpense) "GASTO" else "INGRESO",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isExpense) Color(0xFFFCA5A5) else Color(0xFF86EFAC),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Seleccionada",
                                            tint = Color(0xFF818CF8),
                                            modifier = Modifier.size(18.dp)
                                        )
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

/**
 * Diálogo modal Glassmorphism / Frosted Glass Azul para asignación de Etiquetas (Tags) desde Room SQLite.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FrostedGlassTagSelectorDialog(
    tags: List<AutoCaptureTagOption>,
    initialSelectedTags: List<String>,
    onConfirmTags: (List<String>) -> Unit,
    onDismissRequest: () -> Unit
) {
    val selectedTags = remember { mutableStateListOf<String>().apply { addAll(initialSelectedTags) } }
    var searchQuery by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFA07172B),
                            Color(0xFA0A1E36),
                            Color(0xFD040D1A)
                        )
                    )
                )
                .border(
                    BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0x8034D399),
                                Color(0x330284C7),
                                Color(0x8038BDF8)
                            )
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                .border(BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.4f)), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Label,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Asignar Etiquetas (Tags)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Organiza y filtra tus movimientos",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Buscador de Etiquetas
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Buscar etiqueta (#viajes, #super)...",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0F2440),
                        unfocusedContainerColor = Color(0xFF0B1B30),
                        focusedBorderColor = Color(0xFF34D399),
                        unfocusedBorderColor = Color(0xFF1E3A66),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF34D399)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Vista previa de seleccionadas
                if (selectedTags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        selectedTags.forEach { tagName ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#$tagName",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFBAE6FD),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Quitar",
                                        tint = Color(0xFFBAE6FD),
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable { selectedTags.remove(tagName) }
                                    )
                                }
                            }
                        }
                    }
                }

                val filteredTags = remember(searchQuery, tags) {
                    val q = searchQuery.trim().lowercase()
                    if (q.isBlank()) tags
                    else tags.filter { it.name.lowercase().contains(q) }
                }

                if (tags.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tienes etiquetas creadas en la base de datos de KashApp.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8))
                        )
                    }
                } else if (filteredTags.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F2440).copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, Color(0xFF1E3A66))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No se encontraron etiquetas para '$searchQuery'",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredTags, key = { it.id }) { tag ->
                            val isChecked = selectedTags.contains(tag.name)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) {
                                            selectedTags.remove(tag.name)
                                        } else {
                                            selectedTags.add(tag.name)
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isChecked) Color(0xFF16365C) else Color(0xFF0C1F38),
                                border = if (isChecked) {
                                    BorderStroke(1.5.dp, Color(0xFF38BDF8))
                                } else {
                                    BorderStroke(1.dp, Color(0xFF1E3A66))
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                if (!selectedTags.contains(tag.name)) selectedTags.add(tag.name)
                                            } else {
                                                selectedTags.remove(tag.name)
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(0xFF38BDF8),
                                            uncheckedColor = Color(0xFF64748B)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "#${tag.name}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón Confirmar Selección
                Button(
                    onClick = {
                        onConfirmTags(selectedTags.toList())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedTags.isEmpty()) "Guardar sin etiquetas" else "Guardar (${selectedTags.size} etiquetas)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
