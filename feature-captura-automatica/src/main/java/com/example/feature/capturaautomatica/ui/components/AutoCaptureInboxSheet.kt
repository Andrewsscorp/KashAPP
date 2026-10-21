package com.example.feature.capturaautomatica.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    // Modal de Selección de Cuenta Bancaria Destino
    if (isAccountSelectorOpen) {
        AlertDialog(
            onDismissRequest = { isAccountSelectorOpen = false },
            title = {
                Text(
                    text = "Seleccionar Cuenta Destino",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            },
            text = {
                if (availableAccounts.isEmpty()) {
                    Text(
                        text = "No tienes cuentas registradas en KashApp todavía.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8))
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableAccounts, key = { it.id }) { acc ->
                            val isSelected = selectedAccount?.id == acc.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedAccount = acc
                                        onDraftChange(
                                            acc.id,
                                            acc.name,
                                            selectedCategory?.id,
                                            selectedCategory?.name
                                        )
                                        isAccountSelectorOpen = false
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF1E3A66) else Color(0xFF132D54),
                                border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF38BDF8)) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                selectedAccount = acc
                                                onDraftChange(
                                                    acc.id,
                                                    acc.name,
                                                    selectedCategory?.id,
                                                    selectedCategory?.name
                                                )
                                                isAccountSelectorOpen = false
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = Color(0xFF38BDF8),
                                                unselectedColor = Color(0xFF64748B)
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = acc.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            )
                                            Text(
                                                text = "${acc.bankName} • ${acc.type}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF94A3B8)
                                                )
                                            )
                                        }
                                    }
                                    Text(
                                        text = "$${acc.balance.toLong()}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF38BDF8)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isAccountSelectorOpen = false }) {
                    Text("Cerrar", color = Color(0xFF38BDF8))
                }
            },
            containerColor = Color(0xFF0B192C),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Modal de Selección de Categoría desde la Base de Datos
    if (isCategorySelectorOpen) {
        AlertDialog(
            onDismissRequest = { isCategorySelectorOpen = false },
            title = {
                Text(
                    text = "Seleccionar Categoría",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            },
            text = {
                if (availableCategories.isEmpty()) {
                    Text(
                        text = "No hay categorías disponibles en la base de datos.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8))
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableCategories, key = { it.id }) { cat ->
                            val isSelected = selectedCategory?.id == cat.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCategory = cat
                                        onDraftChange(
                                            selectedAccount?.id,
                                            selectedAccount?.name,
                                            cat.id,
                                            cat.name
                                        )
                                        isCategorySelectorOpen = false
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF1E3A66) else Color(0xFF132D54),
                                border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF38BDF8)) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            selectedCategory = cat
                                            onDraftChange(
                                                selectedAccount?.id,
                                                selectedAccount?.name,
                                                cat.id,
                                                cat.name
                                            )
                                            isCategorySelectorOpen = false
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = Color(0xFF38BDF8),
                                            unselectedColor = Color(0xFF64748B)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isCategorySelectorOpen = false }) {
                    Text("Cerrar", color = Color(0xFF38BDF8))
                }
            },
            containerColor = Color(0xFF0B192C),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Modal de Selección de Etiquetas (Tags) desde la Base de Datos
    if (isTagSelectorOpen) {
        AlertDialog(
            onDismissRequest = { isTagSelectorOpen = false },
            title = {
                Text(
                    text = "Asignar Etiquetas (Tags)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            },
            text = {
                if (availableTags.isEmpty()) {
                    Text(
                        text = "No tienes etiquetas creadas en la base de datos de KashApp.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8))
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableTags, key = { it.id }) { tag ->
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
                                color = if (isChecked) Color(0xFF1E3A66) else Color(0xFF132D54),
                                border = if (isChecked) BorderStroke(1.5.dp, Color(0xFF38BDF8)) else null
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
            },
            confirmButton = {
                TextButton(onClick = { isTagSelectorOpen = false }) {
                    Text("Listo", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF0B192C),
            shape = RoundedCornerShape(20.dp)
        )
    }
}
