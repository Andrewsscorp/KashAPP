package com.example.presentation.fixedassets

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AssetMovementEntity
import com.example.data.local.entity.FixedAssetEntity
import com.example.domain.model.fixedassets.AssetFinancialCalculations
import com.example.domain.model.fixedassets.statemachine.AssetStateMachine
import com.example.domain.repository.FixedAssetRepository
import com.example.domain.usecase.fixedassets.CalculateAssetDepreciationUseCase
import com.example.domain.usecase.fixedassets.CalculateAssetDepreciationUseCase.AssetPendingDepreciation
import com.example.domain.usecase.fixedassets.ProjectAssetValuesUseCase
import com.example.data.local.entity.AssetAuditLogEntity
import com.example.data.local.entity.AssetCategoryEntity
import com.example.presentation.fixedassets.export.AssetAuditExporter
import com.example.presentation.fixedassets.export.FixedAssetExcelExporter
import com.example.presentation.fixedassets.export.FixedAssetPdfExporter
import com.example.util.FinancialReportExporter
import com.example.util.FinancialReportExporter.ExportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.domain.model.fixedassets.AssetMovementType
import com.example.domain.model.fixedassets.DepreciationBatch
import com.example.domain.model.fixedassets.DepreciationBatchItem
import com.example.domain.model.fixedassets.MaintenanceOrder
import com.example.domain.model.fixedassets.MaintenanceStatus
import com.example.domain.usecase.fixedassets.GetPatrimonioResumenUseCase
import com.example.domain.usecase.fixedassets.RunDepreciationBatchUseCase
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class FixedAssetsUiState(
    val allAssets: List<FixedAssetEntity> = emptyList(),
    val categories: List<com.example.data.local.entity.AssetCategoryEntity> = emptyList(),
    val allMovements: List<AssetMovementEntity> = emptyList(),
    val filteredAssets: List<FixedAssetEntity> = emptyList(),
    val pendingDepreciationList: List<AssetPendingDepreciation> = emptyList(),
    val patrimonioResumen: GetPatrimonioResumenUseCase.PatrimonioResumen = GetPatrimonioResumenUseCase.PatrimonioResumen(
        totalAssetsCount = 0,
        totalAcquisitionCost = 0.0,
        totalAccumulatedDepreciation = 0.0,
        totalNetBookValue = 0.0,
        totalMarketValue = 0.0,
        amortizedPercent = 0.0,
        remainingPercent = 100.0,
        monthlyDepreciationQuota = 0.0,
        activeCount = 0,
        maintenanceCount = 0,
        writtenOffCount = 0
    ),
    val depreciationBatches: List<DepreciationBatch> = emptyList(),
    val maintenanceOrders: List<MaintenanceOrder> = emptyList(),
    val auditLogs: List<AssetAuditLogEntity> = emptyList(),
    val selectedCategoryFilter: String = "Todas",
    val selectedStatusFilter: String = "Todos",
    val searchQuery: String = "",
    val totalAcquisitionCost: Double = 0.0,
    val totalAccumulatedDepreciation: Double = 0.0,
    val totalBookValue: Double = 0.0,
    val totalMarketValue: Double = 0.0,
    val isLoading: Boolean = false,
    val isRunningDepreciation: Boolean = false,
    val selectedAssetForDetail: FixedAssetEntity? = null,
    val assetToEdit: FixedAssetEntity? = null,
    val isCreatingAsset: Boolean = false,
    val isViewingProjections: Boolean = false,
    val showRunDepreciationSheet: Boolean = false,
    val showExportSheet: Boolean = false,
    val showFilterSheet: Boolean = false,
    val latestExportResult: ExportResult? = null,
    val latestExportMimeType: String = "application/pdf",
    val showExportSuccessDialog: Boolean = false,
    val toastMessage: String? = null
)

private data class AssetsQuad(
    val assets: List<FixedAssetEntity>,
    val cats: List<AssetCategoryEntity>,
    val movs: List<AssetMovementEntity>,
    val audits: List<AssetAuditLogEntity>
)

class FixedAssetsViewModel(
    private val fixedAssetRepository: FixedAssetRepository,
    private val calculateDepreciationUseCase: CalculateAssetDepreciationUseCase = CalculateAssetDepreciationUseCase(),
    val projectAssetValuesUseCase: ProjectAssetValuesUseCase = ProjectAssetValuesUseCase(),
    val userId: String = "user_default"
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("Todas")
    private val _selectedStatus = MutableStateFlow("Todos")
    private val _internalState = MutableStateFlow(FixedAssetsUiState())

    val getPatrimonioResumenUseCase = GetPatrimonioResumenUseCase()
    val runDepreciationBatchUseCase = RunDepreciationBatchUseCase(calculateDepreciationUseCase)

    val uiState: StateFlow<FixedAssetsUiState> = combine(
        combine(
            fixedAssetRepository.observeAssets(userId),
            fixedAssetRepository.observeCategories(userId),
            fixedAssetRepository.observeAllMovements(userId),
            fixedAssetRepository.observeAuditLogs(userId)
        ) { assets, cats, movs, audits -> AssetsQuad(assets, cats, movs, audits) },
        _searchQuery,
        _selectedCategory,
        _selectedStatus,
        _internalState
    ) { (assets, cats, movs, audits), query, category, status, internal ->
        val filtered = assets.filter { asset ->
            val matchCategory = category == "Todas" || asset.category.equals(category, ignoreCase = true)
            val resolvedState = AssetStateMachine.resolveState(asset.status)
            val matchStatus = status == "Todos" ||
                asset.status.equals(status, ignoreCase = true) ||
                resolvedState.code.equals(status, ignoreCase = true) ||
                resolvedState.shortBadgeLabel.equals(status, ignoreCase = true) ||
                resolvedState.label.equals(status, ignoreCase = true) ||
                (status == "Activo Operativo" && (resolvedState.code == "EN_USO" || asset.status == "ACTIVE")) ||
                (status == "En Mantenimiento" && (resolvedState.code == "EN_MANTENIMIENTO" || asset.status == "MAINTENANCE")) ||
                (status == "Vendido" && (resolvedState.code == "MANTENIDO_PARA_LA_VENTA" || asset.status == "SOLD")) ||
                (status == "Dado de Baja" && (resolvedState.code == "DADO_DE_BAJA" || asset.status == "WRITTEN_OFF"))

            val matchQuery = query.isBlank() ||
                asset.name.contains(query, ignoreCase = true) ||
                asset.category.contains(query, ignoreCase = true) ||
                (asset.brand?.contains(query, ignoreCase = true) == true) ||
                (asset.licensePlate?.contains(query, ignoreCase = true) == true) ||
                (asset.location?.contains(query, ignoreCase = true) == true)

            matchCategory && matchStatus && matchQuery
        }

        val patrimonio = getPatrimonioResumenUseCase.execute(assets)
        val pending = calculateDepreciationUseCase.calculateBatchPending(assets)

        // Compute DepreciationBatches from movements
        val depMovements = movs.filter { it.type == AssetMovementType.DEPRECIATION.name }
        val groupedBatches = depMovements.groupBy { it.date.take(7) }
        val batches = groupedBatches.map { (yearMonth, monthMovs) ->
            val parsedDate = try { java.time.LocalDate.parse("$yearMonth-01") } catch (_: Exception) { java.time.LocalDate.now() }
            val monthName = when (parsedDate.monthValue) {
                1 -> "Enero"; 2 -> "Febrero"; 3 -> "Marzo"; 4 -> "Abril"; 5 -> "Mayo"; 6 -> "Junio"
                7 -> "Julio"; 8 -> "Agosto"; 9 -> "Septiembre"; 10 -> "Octubre"; 11 -> "Noviembre"; else -> "Diciembre"
            }
            val periodLabel = "$monthName ${parsedDate.year}"
            val items = monthMovs.map { m ->
                val asset = assets.find { it.id == m.assetId }
                DepreciationBatchItem(
                    assetId = m.assetId,
                    assetCode = asset?.assetCode ?: m.assetId,
                    assetName = asset?.name ?: m.title,
                    category = asset?.category ?: "Activo Fijo",
                    depreciationMethod = asset?.depreciationMethod ?: "Línea Recta",
                    amount = m.amount,
                    previousAccumulated = (asset?.accumulatedDepreciation ?: m.amount) - m.amount,
                    newAccumulated = asset?.accumulatedDepreciation ?: m.amount,
                    bookValueAfter = asset?.let { AssetFinancialCalculations.calculateCurrentBookValue(it.acquisitionValue, it.accumulatedDepreciation, it.residualValue) } ?: 0.0,
                    movementId = m.id
                )
            }
            DepreciationBatch(
                id = "batch_$yearMonth",
                title = "Depreciación $periodLabel - Completada",
                date = monthMovs.firstOrNull()?.date ?: "$yearMonth-01",
                periodLabel = periodLabel,
                totalAmount = monthMovs.sumOf { it.amount },
                assetsCount = items.size,
                isCompleted = true,
                items = items
            )
        }.sortedByDescending { it.date }

        // Compute MaintenanceOrders from movements
        val maintMovements = movs.filter { it.type == AssetMovementType.MAINTENANCE.name || it.type == AssetMovementType.IMPROVEMENT.name }
        val orders = maintMovements.map { m ->
            val asset = assets.find { it.id == m.assetId }
            val isCap = m.type == AssetMovementType.IMPROVEMENT.name || m.financialImpact?.contains("Capitalizable", ignoreCase = true) == true
            val status = MaintenanceStatus.fromString(m.costCenter ?: if (asset?.status == "EN_MANTENIMIENTO" || asset?.status == "MAINTENANCE") "IN_PROGRESS" else "COMPLETED")
            val mType = when {
                isCap -> "Mejora Capitalizable (NIC 16)"
                m.title.contains("Preventivo", ignoreCase = true) -> "Preventivo"
                m.title.contains("Correctivo", ignoreCase = true) -> "Correctivo"
                m.title.contains("Inspecc", ignoreCase = true) -> "Inspección"
                m.title.contains("Calibrac", ignoreCase = true) -> "Calibración"
                else -> "Mantenimiento General"
            }
            MaintenanceOrder(
                id = m.id,
                assetId = m.assetId,
                assetCode = asset?.assetCode ?: m.assetId,
                assetName = asset?.name ?: "Equipo registrado",
                category = asset?.category ?: "General",
                title = m.title,
                date = m.date,
                amount = m.amount,
                status = status,
                maintenanceType = mType,
                provider = m.providerOrVendor ?: "Servicio Técnico Autorizado",
                notes = m.notes,
                isCapitalizable = isCap,
                rawMovement = m
            )
        }.sortedByDescending { it.date }

        internal.copy(
            allAssets = assets,
            categories = cats,
            allMovements = movs,
            filteredAssets = filtered,
            pendingDepreciationList = pending,
            patrimonioResumen = patrimonio,
            depreciationBatches = batches,
            maintenanceOrders = orders,
            auditLogs = audits,
            selectedCategoryFilter = category,
            selectedStatusFilter = status,
            searchQuery = query,
            totalAcquisitionCost = patrimonio.totalAcquisitionCost,
            totalAccumulatedDepreciation = patrimonio.totalAccumulatedDepreciation,
            totalBookValue = patrimonio.totalNetBookValue,
            totalMarketValue = patrimonio.totalMarketValue,
            selectedAssetForDetail = assets.firstOrNull { it.id == internal.selectedAssetForDetail?.id } ?: internal.selectedAssetForDetail
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FixedAssetsUiState()
    )

    init {
        viewModelScope.launch {
            fixedAssetRepository.seedDefaultAssetsIfNeeded(userId)
            fixedAssetRepository.seedDefaultCategoriesIfNeeded(userId)
        }
    }

    fun createCategory(name: String, code: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            val cat = com.example.data.local.entity.AssetCategoryEntity(
                id = "cat_${java.util.UUID.randomUUID()}",
                userId = userId,
                name = name,
                code = code,
                colorHex = colorHex,
                iconName = iconName,
                isSystem = false
            )
            fixedAssetRepository.saveCategory(cat)
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategoryFilterChange(category: String) {
        _selectedCategory.value = category
    }

    fun onStatusFilterChange(status: String) {
        _selectedStatus.value = status
    }

    fun resetFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = "Todas"
        _selectedStatus.value = "Todos"
    }

    fun selectAssetForDetail(asset: FixedAssetEntity?) {
        _internalState.update { it.copy(selectedAssetForDetail = asset) }
    }

    fun openCreateAsset() {
        _internalState.update { it.copy(isCreatingAsset = true, assetToEdit = null) }
    }

    fun openEditAsset(asset: FixedAssetEntity) {
        _internalState.update { it.copy(isCreatingAsset = true, assetToEdit = asset) }
    }

    fun closeCreateOrEditAsset() {
        _internalState.update { it.copy(isCreatingAsset = false, assetToEdit = null) }
    }

    fun openProjections() {
        _internalState.update { it.copy(isViewingProjections = true) }
    }

    fun closeProjections() {
        _internalState.update { it.copy(isViewingProjections = false) }
    }

    fun toggleRunDepreciationSheet(show: Boolean) {
        _internalState.update { it.copy(showRunDepreciationSheet = show) }
    }

    fun toggleExportSheet(show: Boolean) {
        _internalState.update { it.copy(showExportSheet = show) }
    }

    fun toggleFilterSheet(show: Boolean) {
        _internalState.update { it.copy(showFilterSheet = show) }
    }

    fun dismissExportSuccessDialog() {
        _internalState.update { it.copy(showExportSuccessDialog = false) }
    }

    fun saveAsset(asset: FixedAssetEntity) {
        viewModelScope.launch {
            fixedAssetRepository.saveAsset(asset)
            _internalState.update {
                it.copy(
                    isCreatingAsset = false,
                    assetToEdit = null,
                    selectedAssetForDetail = if (it.selectedAssetForDetail?.id == asset.id) asset else it.selectedAssetForDetail,
                    toastMessage = "Activo guardado exitosamente"
                )
            }
        }
    }

    fun deleteAsset(assetId: String) {
        viewModelScope.launch {
            fixedAssetRepository.deleteAsset(assetId)
            _internalState.update {
                it.copy(
                    selectedAssetForDetail = null,
                    toastMessage = "Activo eliminado del inventario"
                )
            }
        }
    }

    fun recordMovement(movement: AssetMovementEntity) {
        viewModelScope.launch {
            fixedAssetRepository.recordMovement(movement)
            val freshAsset = fixedAssetRepository.getAssetById(movement.assetId)
            _internalState.update {
                it.copy(
                    selectedAssetForDetail = freshAsset ?: it.selectedAssetForDetail,
                    toastMessage = "Movimiento registrado exitosamente"
                )
            }
        }
    }

    fun changeAssetState(
        asset: FixedAssetEntity,
        targetStateCode: String,
        reasonNotes: String,
        movement: AssetMovementEntity
    ) {
        viewModelScope.launch {
            val updated = asset.copy(
                status = targetStateCode,
                stateNotes = reasonNotes,
                updatedAt = System.currentTimeMillis()
            )
            fixedAssetRepository.saveAsset(updated)
            fixedAssetRepository.recordMovement(movement)
            val freshAsset = fixedAssetRepository.getAssetById(asset.id) ?: updated
            _internalState.update {
                it.copy(
                    selectedAssetForDetail = freshAsset,
                    toastMessage = "Estado actualizado a $targetStateCode"
                )
            }
        }
    }

    fun runAutomaticDepreciation() {
        viewModelScope.launch {
            _internalState.update { it.copy(isRunningDepreciation = true) }
            val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val result = fixedAssetRepository.runAutomaticDepreciation(userId, todayStr)
            val currentSelectedId = _internalState.value.selectedAssetForDetail?.id
            val freshSelected = currentSelectedId?.let { fixedAssetRepository.getAssetById(it) }

            _internalState.update {
                it.copy(
                    isRunningDepreciation = false,
                    showRunDepreciationSheet = false,
                    selectedAssetForDetail = freshSelected ?: it.selectedAssetForDetail,
                    toastMessage = result.getOrNull()?.message ?: "Depreciación finalizada"
                )
            }
        }
    }

    fun exportToPdf(context: Context) {
        val currentAssets = uiState.value.filteredAssets
        val result = FixedAssetPdfExporter.exportToPdf(
            context = context,
            assets = currentAssets,
            filterDesc = "Categoría: ${_selectedCategory.value}, Estado: ${_selectedStatus.value}"
        )
        _internalState.update {
            it.copy(
                showExportSheet = false,
                latestExportResult = result,
                latestExportMimeType = "application/pdf",
                showExportSuccessDialog = true
            )
        }
    }

    fun exportToExcel(context: Context) {
        val currentAssets = uiState.value.filteredAssets
        val result = FixedAssetExcelExporter.exportToExcel(
            context = context,
            assets = currentAssets,
            filterDesc = "Categoría: ${_selectedCategory.value}, Estado: ${_selectedStatus.value}"
        )
        _internalState.update {
            it.copy(
                showExportSheet = false,
                latestExportResult = result,
                latestExportMimeType = "text/csv",
                showExportSuccessDialog = true
            )
        }
    }

    fun exportAuditPdf(context: Context) {
        val logs = uiState.value.auditLogs
        val file = AssetAuditExporter.exportToPdf(context, logs)
        val uri = try {
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            android.net.Uri.fromFile(file)
        }
        val result = FinancialReportExporter.ExportResult(
            success = true,
            filePath = file.absolutePath,
            fileUri = uri,
            summaryText = "Libro Oficial de Auditoría y Trazabilidad NIIF",
            message = "Bitácora exportada exitosamente a PDF (${logs.size} eventos registrados)"
        )
        _internalState.update {
            it.copy(
                latestExportResult = result,
                latestExportMimeType = "application/pdf",
                showExportSuccessDialog = true
            )
        }
    }

    fun exportAuditExcel(context: Context) {
        val logs = uiState.value.auditLogs
        val file = AssetAuditExporter.exportToExcel(context, logs)
        val uri = try {
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            android.net.Uri.fromFile(file)
        }
        val result = FinancialReportExporter.ExportResult(
            success = true,
            filePath = file.absolutePath,
            fileUri = uri,
            summaryText = "Libro Oficial de Auditoría y Trazabilidad NIIF (Excel)",
            message = "Bitácora exportada exitosamente a formato Excel / CSV (${logs.size} eventos)"
        )
        _internalState.update {
            it.copy(
                latestExportResult = result,
                latestExportMimeType = "text/csv",
                showExportSuccessDialog = true
            )
        }
    }

    fun observeMovementsForAsset(assetId: String) = fixedAssetRepository.observeMovements(assetId)

    fun createMaintenanceOrder(
        assetId: String,
        title: String,
        amount: Double,
        provider: String,
        notes: String,
        type: String,
        isCapitalizable: Boolean,
        initialStatus: MaintenanceStatus
    ) {
        viewModelScope.launch {
            val asset = fixedAssetRepository.getAssetById(assetId) ?: return@launch
            val movType = if (isCapitalizable) AssetMovementType.IMPROVEMENT.name else AssetMovementType.MAINTENANCE.name
            val movId = "mov_maint_${java.util.UUID.randomUUID()}"
            val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

            val movement = AssetMovementEntity(
                id = movId,
                assetId = assetId,
                userId = userId,
                type = movType,
                title = title.ifBlank { "Mantenimiento $type" },
                date = todayStr,
                amount = amount,
                notes = notes.ifBlank { null },
                providerOrVendor = provider.ifBlank { null },
                costCenter = initialStatus.name,
                financialImpact = if (isCapitalizable) "Mejora Capitalizable (NIC 16) • Incrementa Base Activo" else "Gasto Operacional • Estado de Resultados"
            )

            fixedAssetRepository.recordMovement(movement)

            // Conexión Arquitectónica: Cuando un registro aquí pasa a estado "En Progreso",
            // emite un evento de dominio que actualiza automáticamente el estado del activo en fixed_assets a EN_MANTENIMIENTO
            if (initialStatus == MaintenanceStatus.IN_PROGRESS) {
                val updatedAsset = asset.copy(
                    status = "EN_MANTENIMIENTO",
                    stateNotes = "En mantenimiento activo: $title",
                    updatedAt = System.currentTimeMillis()
                )
                fixedAssetRepository.saveAsset(updatedAsset)
            } else if (isCapitalizable && amount > 0) {
                val updatedAsset = asset.copy(
                    acquisitionValue = asset.acquisitionValue + amount,
                    currentMarketValue = asset.currentMarketValue + amount,
                    updatedAt = System.currentTimeMillis()
                )
                fixedAssetRepository.saveAsset(updatedAsset)
            }

            _internalState.update {
                it.copy(toastMessage = "Orden de mantenimiento registrada exitosamente")
            }
        }
    }

    fun updateMaintenanceOrderStatus(
        movementId: String,
        assetId: String,
        newStatus: MaintenanceStatus,
        resolutionNotes: String = ""
    ) {
        viewModelScope.launch {
            val asset = fixedAssetRepository.getAssetById(assetId) ?: return@launch
            val currentMovements = fixedAssetRepository.observeMovements(assetId).firstOrNull() ?: emptyList()
            val existingMov = currentMovements.find { it.id == movementId }
            if (existingMov != null) {
                val updatedMov = existingMov.copy(
                    costCenter = newStatus.name,
                    notes = if (resolutionNotes.isNotBlank()) "${existingMov.notes ?: ""}\nResolución: $resolutionNotes".trim() else existingMov.notes
                )
                fixedAssetRepository.recordMovement(updatedMov)
            }

            // Domain Event: Cambiar estado del activo
            if (newStatus == MaintenanceStatus.IN_PROGRESS) {
                val updatedAsset = asset.copy(
                    status = "EN_MANTENIMIENTO",
                    stateNotes = "Mantenimiento en ejecución activa",
                    updatedAt = System.currentTimeMillis()
                )
                fixedAssetRepository.saveAsset(updatedAsset)
            } else if (newStatus == MaintenanceStatus.COMPLETED) {
                val updatedAsset = asset.copy(
                    status = "EN_USO",
                    stateNotes = resolutionNotes.ifBlank { "Mantenimiento finalizado satisfactoriamente" },
                    updatedAt = System.currentTimeMillis()
                )
                fixedAssetRepository.saveAsset(updatedAsset)
            }

            _internalState.update {
                it.copy(toastMessage = "Orden actualizada a ${newStatus.label}")
            }
        }
    }

    fun executeDepreciationBatch(asOfDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))) {
        viewModelScope.launch {
            _internalState.update { it.copy(isRunningDepreciation = true) }
            val allAssets = fixedAssetRepository.observeAssets(userId).firstOrNull() ?: emptyList()
            val result = runDepreciationBatchUseCase.execute(allAssets, userId, asOfDate)

            if (result.createdMovements.isNotEmpty()) {
                for (mov in result.createdMovements) {
                    fixedAssetRepository.recordMovement(mov)
                }
                for (upd in result.updatedAssets) {
                    fixedAssetRepository.saveAsset(upd)
                }
            }

            _internalState.update {
                it.copy(
                    isRunningDepreciation = false,
                    showRunDepreciationSheet = false,
                    toastMessage = result.summaryMessage
                )
            }
        }
    }

    class Factory(
        private val fixedAssetRepository: FixedAssetRepository,
        private val calculateDepreciationUseCase: CalculateAssetDepreciationUseCase,
        private val projectAssetValuesUseCase: ProjectAssetValuesUseCase,
        private val userId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FixedAssetsViewModel(
                fixedAssetRepository = fixedAssetRepository,
                calculateDepreciationUseCase = calculateDepreciationUseCase,
                projectAssetValuesUseCase = projectAssetValuesUseCase,
                userId = userId
            ) as T
        }
    }
}
