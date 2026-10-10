package com.example.presentation.fixedassets.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AssetMovementEntity
import com.example.data.local.entity.FixedAssetEntity
import com.example.domain.model.fixedassets.AssetMovementType
import com.example.presentation.fixedassets.components.movement.DisposalMovementForm
import com.example.presentation.fixedassets.components.movement.FinancialImpactBanner
import com.example.presentation.fixedassets.components.movement.ImpairmentMovementForm
import com.example.presentation.fixedassets.components.movement.ImprovementMovementForm
import com.example.presentation.fixedassets.components.movement.MaintenanceMovementForm
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import com.example.presentation.fixedassets.components.movement.MovementTypeSelector
import com.example.presentation.fixedassets.components.movement.RevaluationMovementForm
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Professional IPhone-style modal sheet for registering asset movements and novelties.
 * Features dynamic form mutation under IFRS / NIIF rules:
 * - Improvement / Addition (Capitalizable)
 * - Technical Revaluation (Fair value appraisal)
 * - Maintenance (100% Operational Expense)
 * - Impairment loss (NIC 36)
 * - Disposal and derecognition (Scrap, Theft, Donation)
 * - Asset Sale
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterMovementIPhoneSheet(
    asset: FixedAssetEntity,
    initialType: AssetMovementType = AssetMovementType.IMPROVEMENT,
    onSaveMovement: (AssetMovementEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val effectiveInitial = remember(asset.status, initialType) {
        if (asset.status == "EN_MANTENIMIENTO" && (initialType == AssetMovementType.IMPROVEMENT || initialType == AssetMovementType.REVALUATION)) {
            AssetMovementType.MAINTENANCE
        } else {
            initialType
        }
    }
    var selectedType by remember(effectiveInitial) { mutableStateOf(effectiveInitial) }

    // Shared Fields
    var title by remember(selectedType) { mutableStateOf(selectedType.defaultTitle) }
    var amount by remember(selectedType) { mutableStateOf<Double?>(null) }
    var dateText by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))) }
    var notes by remember { mutableStateOf("") }

    // Improvement-specific fields
    var usefulLifeIncrementMonths by remember { mutableIntStateOf(0) }
    var newResidualValue by remember { mutableStateOf<Double?>(null) }
    var improvementProvider by remember { mutableStateOf("") }
    var improvementInvoice by remember { mutableStateOf("") }

    // Revaluation-specific fields
    var appraiserName by remember { mutableStateOf("") }
    var appraiserLicense by remember { mutableStateOf("") }
    var appraisalDocumentUri by remember { mutableStateOf<String?>(null) }

    // Maintenance-specific fields
    var costCenter by remember { mutableStateOf(asset.location ?: "CC-Operaciones") }
    var accountingAccount by remember { mutableStateOf("5145 Mantenimientos") }
    var maintenanceProvider by remember { mutableStateOf("") }
    var workOrderNumber by remember { mutableStateOf("") }

    // Impairment-specific fields
    var impairmentReason by remember { mutableStateOf("Daño físico o accidente no asegurable") }
    var recoverableAmount by remember { mutableStateOf<Double?>(null) }

    // Disposal-specific fields
    var disposalReason by remember { mutableStateOf("Chatarrización y desmantelamiento") }
    var actNumber by remember { mutableStateOf("") }
    var disposalDocumentUri by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    IPhoneActionModal(
        title = "Registrar Movimiento / Novedad",
        subtitle = "${asset.name} (${asset.assetCode})",
        icon = Icons.Default.AddCard,
        iconTint = Color(0xFF10B981),
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(scrollState)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. DYNAMIC TYPE SELECTOR (Respects accounting policies)
            MovementTypeSelector(
                asset = asset,
                selectedType = selectedType,
                onTypeSelected = { newType ->
                    selectedType = newType
                    title = newType.defaultTitle
                },
                initialType = initialType
            )

            // 2. REAL-TIME FINANCIAL IMPACT BANNER
            FinancialImpactBanner(
                asset = asset,
                type = selectedType,
                amount = amount,
                usefulLifeIncrementMonths = usefulLifeIncrementMonths,
                newResidualValue = newResidualValue
            )

            // 3. MUTATING DYNAMIC FORM BASED ON SELECTED TYPE
            when (selectedType) {
                AssetMovementType.IMPROVEMENT -> {
                    ImprovementMovementForm(
                        asset = asset,
                        title = title,
                        onTitleChange = { title = it },
                        amount = amount,
                        onAmountChange = { amount = it },
                        usefulLifeIncrementMonths = usefulLifeIncrementMonths,
                        onUsefulLifeIncrementChange = { usefulLifeIncrementMonths = it },
                        newResidualValue = newResidualValue,
                        onNewResidualValueChange = { newResidualValue = it },
                        dateText = dateText,
                        onDateChange = { dateText = it },
                        notes = notes,
                        onNotesChange = { notes = it },
                        provider = improvementProvider,
                        onProviderChange = { improvementProvider = it },
                        invoiceNumber = improvementInvoice,
                        onInvoiceNumberChange = { improvementInvoice = it }
                    )
                }

                AssetMovementType.REVALUATION -> {
                    RevaluationMovementForm(
                        asset = asset,
                        title = title,
                        onTitleChange = { title = it },
                        amount = amount,
                        onAmountChange = { amount = it },
                        newResidualValue = newResidualValue,
                        onNewResidualValueChange = { newResidualValue = it },
                        appraiserName = appraiserName,
                        onAppraiserNameChange = { appraiserName = it },
                        appraiserLicense = appraiserLicense,
                        onAppraiserLicenseChange = { appraiserLicense = it },
                        documentUri = appraisalDocumentUri,
                        onDocumentUriChange = { appraisalDocumentUri = it },
                        dateText = dateText,
                        onDateChange = { dateText = it },
                        notes = notes,
                        onNotesChange = { notes = it }
                    )
                }

                AssetMovementType.MAINTENANCE -> {
                    MaintenanceMovementForm(
                        asset = asset,
                        title = title,
                        onTitleChange = { title = it },
                        amount = amount,
                        onAmountChange = { amount = it },
                        costCenter = costCenter,
                        onCostCenterChange = { costCenter = it },
                        accountingAccount = accountingAccount,
                        onAccountingAccountChange = { accountingAccount = it },
                        provider = maintenanceProvider,
                        onProviderChange = { maintenanceProvider = it },
                        workOrderOrInvoice = workOrderNumber,
                        onWorkOrderOrInvoiceChange = { workOrderNumber = it },
                        dateText = dateText,
                        onDateChange = { dateText = it },
                        notes = notes,
                        onNotesChange = { notes = it }
                    )
                }

                AssetMovementType.IMPAIRMENT -> {
                    ImpairmentMovementForm(
                        asset = asset,
                        title = title,
                        onTitleChange = { title = it },
                        amount = amount,
                        onAmountChange = { amount = it },
                        recoverableAmount = recoverableAmount,
                        onRecoverableAmountChange = { recoverableAmount = it },
                        impairmentReason = impairmentReason,
                        onImpairmentReasonChange = { impairmentReason = it },
                        dateText = dateText,
                        onDateChange = { dateText = it },
                        notes = notes,
                        onNotesChange = { notes = it }
                    )
                }

                AssetMovementType.DISPOSAL, AssetMovementType.SALE -> {
                    DisposalMovementForm(
                        asset = asset,
                        title = title,
                        onTitleChange = { title = it },
                        amount = amount,
                        onAmountChange = { amount = it },
                        disposalReason = disposalReason,
                        onDisposalReasonChange = { disposalReason = it },
                        actNumber = actNumber,
                        onActNumberChange = { actNumber = it },
                        documentUri = disposalDocumentUri,
                        onDocumentUriChange = { disposalDocumentUri = it },
                        dateText = dateText,
                        onDateChange = { dateText = it },
                        notes = notes,
                        onNotesChange = { notes = it },
                        isSale = selectedType == AssetMovementType.SALE
                    )
                }

                else -> {
                    // Generic fallback
                    ImprovementMovementForm(
                        asset = asset,
                        title = title,
                        onTitleChange = { title = it },
                        amount = amount,
                        onAmountChange = { amount = it },
                        usefulLifeIncrementMonths = usefulLifeIncrementMonths,
                        onUsefulLifeIncrementChange = { usefulLifeIncrementMonths = it },
                        newResidualValue = newResidualValue,
                        onNewResidualValueChange = { newResidualValue = it },
                        dateText = dateText,
                        onDateChange = { dateText = it },
                        notes = notes,
                        onNotesChange = { notes = it },
                        provider = improvementProvider,
                        onProviderChange = { improvementProvider = it },
                        invoiceNumber = improvementInvoice,
                        onInvoiceNumberChange = { improvementInvoice = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. ACTION BUTTON
            val isFormValid = when (selectedType) {
                AssetMovementType.DISPOSAL -> true // Disposals may have $0 recovery amount
                else -> (amount ?: 0.0) > 0.0
            }

            Button(
                onClick = {
                    val finalAmount = amount ?: 0.0

                    // Construct detailed audit notes
                    val auditNotesBuilder = StringBuilder(notes.trim())
                    when (selectedType) {
                        AssetMovementType.IMPROVEMENT -> {
                            if (usefulLifeIncrementMonths > 0) {
                                auditNotesBuilder.append(" | +${usefulLifeIncrementMonths}m vida útil")
                            }
                            if (newResidualValue != null) {
                                auditNotesBuilder.append(" | Nuevo residual: $newResidualValue")
                            }
                            if (improvementProvider.isNotBlank()) {
                                auditNotesBuilder.append(" | Proveedor: $improvementProvider")
                            }
                            if (improvementInvoice.isNotBlank()) {
                                auditNotesBuilder.append(" | Factura: $improvementInvoice")
                            }
                        }
                        AssetMovementType.REVALUATION -> {
                            if (appraiserName.isNotBlank()) {
                                auditNotesBuilder.append(" | Perito: $appraiserName ($appraiserLicense)")
                            }
                            if (newResidualValue != null) {
                                auditNotesBuilder.append(" | Nuevo residual: $newResidualValue")
                            }
                        }
                        AssetMovementType.MAINTENANCE -> {
                            if (costCenter.isNotBlank()) {
                                auditNotesBuilder.append(" | Centro Costo: $costCenter")
                            }
                            if (accountingAccount.isNotBlank()) {
                                auditNotesBuilder.append(" | Cuenta: $accountingAccount")
                            }
                            if (maintenanceProvider.isNotBlank()) {
                                auditNotesBuilder.append(" | Taller: $maintenanceProvider")
                            }
                            if (workOrderNumber.isNotBlank()) {
                                auditNotesBuilder.append(" | OT/Factura: $workOrderNumber")
                            }
                        }
                        AssetMovementType.IMPAIRMENT -> {
                            auditNotesBuilder.append(" | Causa: $impairmentReason")
                            if (recoverableAmount != null) {
                                auditNotesBuilder.append(" | Val. Recuperable: $recoverableAmount")
                            }
                        }
                        AssetMovementType.DISPOSAL, AssetMovementType.SALE -> {
                            auditNotesBuilder.append(" | Motivo: $disposalReason")
                            if (actNumber.isNotBlank()) {
                                auditNotesBuilder.append(" | Doc: $actNumber")
                            }
                        }
                        else -> {}
                    }

                    val entity = AssetMovementEntity(
                        id = "mov_${UUID.randomUUID()}",
                        assetId = asset.id,
                        userId = asset.userId,
                        type = selectedType.name,
                        title = title.ifBlank { selectedType.defaultTitle },
                        date = dateText.ifBlank { LocalDate.now().toString() },
                        amount = finalAmount,
                        notes = auditNotesBuilder.toString().ifBlank { null },
                        usefulLifeIncrementMonths = usefulLifeIncrementMonths,
                        newResidualValue = newResidualValue,
                        costCenter = costCenter.ifBlank { null },
                        accountingAccount = accountingAccount.ifBlank { null },
                        providerOrVendor = when (selectedType) {
                            AssetMovementType.IMPROVEMENT -> improvementProvider.ifBlank { null }
                            AssetMovementType.REVALUATION -> appraiserName.ifBlank { null }
                            AssetMovementType.MAINTENANCE -> maintenanceProvider.ifBlank { null }
                            else -> null
                        },
                        invoiceOrDocNumber = when (selectedType) {
                            AssetMovementType.IMPROVEMENT -> improvementInvoice.ifBlank { null }
                            AssetMovementType.REVALUATION -> appraiserLicense.ifBlank { null }
                            AssetMovementType.MAINTENANCE -> workOrderNumber.ifBlank { null }
                            AssetMovementType.DISPOSAL, AssetMovementType.SALE -> actNumber.ifBlank { null }
                            else -> null
                        },
                        documentUri = when (selectedType) {
                            AssetMovementType.REVALUATION -> appraisalDocumentUri
                            AssetMovementType.DISPOSAL, AssetMovementType.SALE -> disposalDocumentUri
                            else -> null
                        },
                        disposalReason = if (selectedType == AssetMovementType.DISPOSAL || selectedType == AssetMovementType.SALE) disposalReason else null,
                        impairmentReason = if (selectedType == AssetMovementType.IMPAIRMENT) impairmentReason else null,
                        financialImpact = when (selectedType) {
                            AssetMovementType.IMPROVEMENT -> "Capitalizable (+ Base depreciable)"
                            AssetMovementType.REVALUATION -> "Superávit por Revaluación (Patrimonio)"
                            AssetMovementType.MAINTENANCE -> "Gasto Operacional (100% P&L)"
                            AssetMovementType.IMPAIRMENT -> "Pérdida por Deterioro (NIC 36)"
                            AssetMovementType.DISPOSAL -> "Desincorporación / Baja Definitiva"
                            AssetMovementType.SALE -> "Venta y enajenación del activo"
                            else -> "Movimiento Contable"
                        }
                    )

                    onSaveMovement(entity)
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (selectedType) {
                        AssetMovementType.IMPROVEMENT -> Color(0xFF0284C7)
                        AssetMovementType.REVALUATION -> Color(0xFF8B5CF6)
                        AssetMovementType.MAINTENANCE -> Color(0xFFF59E0B)
                        AssetMovementType.IMPAIRMENT -> Color(0xFFEF4444)
                        AssetMovementType.DISPOSAL -> Color(0xFF64748B)
                        AssetMovementType.SALE -> Color(0xFF10B981)
                        else -> Color(0xFF007AFF)
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_movement_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Guardar ${selectedType.label}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
