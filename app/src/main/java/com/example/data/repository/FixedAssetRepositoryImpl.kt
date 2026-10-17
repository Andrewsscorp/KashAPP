package com.example.data.repository

import com.example.data.local.dao.AssetAuditLogDao
import com.example.data.local.entity.AssetAuditLogEntity
import com.example.data.local.dao.AssetCategoryDao
import com.example.data.local.dao.FixedAssetDao
import com.example.data.local.entity.AssetCategoryEntity
import com.example.data.local.entity.AssetMovementEntity
import com.example.data.local.entity.FixedAssetEntity
import com.example.domain.model.fixedassets.AssetCategory
import com.example.domain.model.fixedassets.AssetMovementType
import com.example.domain.model.fixedassets.DepreciationRunSummary
import com.example.domain.repository.FixedAssetRepository
import com.example.domain.usecase.fixedassets.CalculateAssetDepreciationUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class FixedAssetRepositoryImpl(
    private val fixedAssetDao: FixedAssetDao,
    private val assetCategoryDao: AssetCategoryDao? = null,
    private val assetAuditLogDao: AssetAuditLogDao? = null,
    private val calculateDepreciationUseCase: CalculateAssetDepreciationUseCase = CalculateAssetDepreciationUseCase()
) : FixedAssetRepository {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val auditDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override fun observeAssets(userId: String): Flow<List<FixedAssetEntity>> {
        return fixedAssetDao.observeAssets(userId)
    }

    override suspend fun getAssetById(assetId: String): FixedAssetEntity? {
        return fixedAssetDao.getAssetById(assetId)
    }

    override fun observeAssetById(assetId: String): Flow<FixedAssetEntity?> {
        return fixedAssetDao.observeAssetById(assetId)
    }

    override suspend fun saveAsset(asset: FixedAssetEntity): Result<Unit> = runCatching {
        val existing = fixedAssetDao.getAssetById(asset.id)
        if (existing == null) {
            fixedAssetDao.insertAsset(asset)
        } else {
            fixedAssetDao.updateAsset(asset)
        }

        // Garantizar que SIEMPRE exista el movimiento contable inicial de ADQUISICIÓN
        val existingMovements = fixedAssetDao.getMovementsForAsset(asset.id)
        val hasAcquisition = existingMovements.any { it.type.equals("ACQUISITION", ignoreCase = true) }
        if (!hasAcquisition) {
            val acqMovement = AssetMovementEntity(
                id = "mov_acq_${UUID.randomUUID()}",
                assetId = asset.id,
                userId = asset.userId,
                type = AssetMovementType.ACQUISITION.name,
                title = "Alta y Adquisición: ${asset.name}",
                date = asset.acquisitionDate,
                amount = asset.acquisitionValue,
                notes = "Registro inicial y capitalización de activo fijo (NIC 16). Categoría: ${asset.category}",
                financialImpact = "Capitalizable (+ Base depreciable)"
            )
            fixedAssetDao.insertMovement(acqMovement)
        }

        // Registrar en Bitácora de Auditoría
        val now = System.currentTimeMillis()
        val formattedDate = LocalDateTime.now().format(auditDateFormatter)
        val isNew = existing == null
        val auditLog = AssetAuditLogEntity(
            id = "audit_${UUID.randomUUID()}",
            userId = asset.userId,
            assetId = asset.id,
            assetName = asset.name,
            actionType = if (isNew) "CREACION" else "MODIFICACION",
            actionTitle = if (isNew) "Alta de Activo Fijo: ${asset.name}" else "Modificación de Activo: ${asset.name}",
            details = if (isNew) {
                "Creación y capitalización de ${asset.name} (${asset.category}). Valor: $${asset.acquisitionValue} COP. Vida útil: ${asset.usefulLifeMonths} meses. Método: ${asset.depreciationMethod}."
            } else {
                "Actualización de parámetros contables en ${asset.name}. Método: ${asset.depreciationMethod}, Vida útil: ${asset.usefulLifeMonths} meses, Residual: $${asset.residualValue} COP."
            },
            financialImpact = if (isNew) "Alta patrimonial (+ $${asset.acquisitionValue} COP)" else "Ajuste prospectivo NIC 16",
            timestamp = now,
            formattedDate = formattedDate
        )
        assetAuditLogDao?.insertAuditLog(auditLog)
    }

    override suspend fun deleteAsset(assetId: String): Result<Unit> = runCatching {
        val asset = fixedAssetDao.getAssetById(assetId)
        val now = System.currentTimeMillis()
        val formattedDate = LocalDateTime.now().format(auditDateFormatter)
        if (asset != null) {
            val auditLog = AssetAuditLogEntity(
                id = "audit_${UUID.randomUUID()}",
                userId = asset.userId,
                assetId = asset.id,
                assetName = asset.name,
                actionType = "BAJA",
                actionTitle = "Baja / Retiro de Activo: ${asset.name}",
                details = "Desincorporación contable del inventario. Valor histórico: $${asset.acquisitionValue} COP.",
                financialImpact = "Baja de activo del patrimonio",
                timestamp = now,
                formattedDate = formattedDate
            )
            assetAuditLogDao?.insertAuditLog(auditLog)
        }
        fixedAssetDao.deleteMovementsByAssetId(assetId)
        fixedAssetDao.deleteAssetById(assetId)
    }

    override fun observeMovements(assetId: String): Flow<List<AssetMovementEntity>> {
        return fixedAssetDao.observeMovementsForAsset(assetId)
            .onEach { list ->
                if (list.isEmpty()) {
                    ensureBaseMovementsForAsset(assetId)
                }
            }
    }

    private suspend fun ensureBaseMovementsForAsset(assetId: String) {
        val asset = fixedAssetDao.getAssetById(assetId) ?: return
        val currentCount = fixedAssetDao.observeMovementsForAsset(assetId).firstOrNull()?.size ?: 0
        if (currentCount > 0) return

        val initialList = mutableListOf<AssetMovementEntity>()
        initialList.add(
            AssetMovementEntity(
                id = "mov_init_${asset.id}",
                assetId = asset.id,
                userId = asset.userId,
                type = AssetMovementType.ACQUISITION.name,
                title = "Alta y Adquisición: ${asset.name}",
                date = asset.acquisitionDate,
                amount = asset.acquisitionValue,
                notes = "Registro inicial del activo en libros contables NIIF. Categoría: ${asset.category}",
                financialImpact = "Capitalizable (+ Base depreciable)"
            )
        )
        if (asset.accumulatedDepreciation > 0) {
            initialList.add(
                AssetMovementEntity(
                    id = "mov_dep_init_${asset.id}",
                    assetId = asset.id,
                    userId = asset.userId,
                    type = AssetMovementType.DEPRECIATION.name,
                    title = "Depreciación Acumulada Contable",
                    date = asset.lastDepreciationDate ?: asset.acquisitionDate,
                    amount = asset.accumulatedDepreciation,
                    notes = "Amortización histórica acumulada bajo NIIF / NIC 16",
                    financialImpact = "Gasto del Periodo (P&L)"
                )
            )
        }
        if (asset.status == "MAINTENANCE" || asset.status == "EN_MANTENIMIENTO") {
            initialList.add(
                AssetMovementEntity(
                    id = "mov_maint_init_${asset.id}",
                    assetId = asset.id,
                    userId = asset.userId,
                    type = AssetMovementType.MAINTENANCE.name,
                    title = "Ingreso a Taller / Mantenimiento",
                    date = LocalDate.now().toString(),
                    amount = 0.0,
                    notes = "Activo en proceso de mantenimiento preventivo/correctivo.",
                    financialImpact = "Gasto Operacional (100% P&L)"
                )
            )
        } else if (asset.status == "WRITTEN_OFF" || asset.status == "DADO_DE_BAJA") {
            initialList.add(
                AssetMovementEntity(
                    id = "mov_disp_init_${asset.id}",
                    assetId = asset.id,
                    userId = asset.userId,
                    type = AssetMovementType.DISPOSAL.name,
                    title = "Baja Definitiva y Desincorporación",
                    date = asset.lastDepreciationDate ?: asset.acquisitionDate,
                    amount = 0.0,
                    notes = "Activo retirado del servicio y dado de baja contable.",
                    disposalReason = "Obsolescencia o fin de vida útil",
                    financialImpact = "Pérdida por Retiro de Activos"
                )
            )
        }
        fixedAssetDao.insertMovements(initialList)
    }

    override fun observeAllMovements(userId: String): Flow<List<AssetMovementEntity>> {
        return fixedAssetDao.observeAllMovements(userId)
    }

    override suspend fun recordMovement(movement: AssetMovementEntity): Result<Unit> = runCatching {
        fixedAssetDao.insertMovement(movement)
        val asset = fixedAssetDao.getAssetById(movement.assetId) ?: return@runCatching

        // If it's an improvement, increase acquisition or market value
        val movementType = AssetMovementType.fromString(movement.type)
        val updated = when (movementType) {
            AssetMovementType.IMPROVEMENT -> {
                // Mejora / Adición (Capitalizable NIIF NIC 16):
                // 1. El Monto no va al gasto, se suma al saldo del activo (Nueva Base Depreciable)
                // 2. Aumento de vida útil (meses) si fue registrado
                val newUsefulLife = if (movement.usefulLifeIncrementMonths > 0) {
                    asset.usefulLifeMonths + movement.usefulLifeIncrementMonths
                } else {
                    asset.usefulLifeMonths
                }
                // 3. Ajuste al nuevo valor residual si se especificó
                val newResidual = movement.newResidualValue ?: asset.residualValue
                asset.copy(
                    acquisitionValue = asset.acquisitionValue + movement.amount,
                    currentMarketValue = asset.currentMarketValue + movement.amount,
                    usefulLifeMonths = newUsefulLife,
                    residualValue = newResidual,
                    updatedAt = System.currentTimeMillis()
                )
            }
            AssetMovementType.REVALUATION -> {
                // Revalúo Técnico (Modelo de Revaluación NIIF NIC 16):
                // El Monto reemplaza o ajusta la base al valor de mercado tasado por perito.
                // Se reexpresa el activo y se actualiza el valor residual si fue reestimado.
                val newResidual = movement.newResidualValue ?: asset.residualValue
                asset.copy(
                    acquisitionValue = movement.amount,
                    accumulatedDepreciation = 0.0,
                    currentMarketValue = movement.amount,
                    residualValue = newResidual,
                    updatedAt = System.currentTimeMillis()
                )
            }
            AssetMovementType.IMPAIRMENT -> {
                // Deterioro de Valor (NIC 36):
                // Reconoce pérdida por deterioro aumentando la amortización/deterioro acumulado,
                // reduciendo el valor neto en libros y ajustando prospectivamente las cuotas.
                val newAccum = asset.accumulatedDepreciation + movement.amount
                val newMarket = kotlin.math.max(asset.residualValue, asset.currentMarketValue - movement.amount)
                asset.copy(
                    accumulatedDepreciation = newAccum,
                    currentMarketValue = newMarket,
                    updatedAt = System.currentTimeMillis()
                )
            }
            AssetMovementType.MAINTENANCE -> {
                // Si el activo estaba en uso, pasa a En Mantenimiento
                if (asset.status == "EN_USO" || asset.status == "ACTIVE" || movement.title.contains("Ingreso", ignoreCase = true) || movement.title.contains("taller", ignoreCase = true)) {
                    asset.copy(
                        status = "EN_MANTENIMIENTO",
                        stateNotes = movement.notes,
                        updatedAt = System.currentTimeMillis()
                    )
                } else if (movement.title.contains("Conclusión", ignoreCase = true) || movement.title.contains("finaliz", ignoreCase = true)) {
                    asset.copy(
                        status = "EN_USO",
                        stateNotes = movement.notes,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    null
                }
            }
            AssetMovementType.SALE -> {
                // Enajenación / Venta
                asset.copy(
                    status = "MANTENIDO_PARA_LA_VENTA",
                    updatedAt = System.currentTimeMillis()
                )
            }
            AssetMovementType.DISPOSAL -> {
                // Baja Definitiva / Desincorporación
                asset.copy(
                    status = "DADO_DE_BAJA",
                    stateNotes = movement.disposalReason ?: movement.notes,
                    updatedAt = System.currentTimeMillis()
                )
            }
            AssetMovementType.ACQUISITION -> {
                asset.copy(
                    acquisitionValue = movement.amount,
                    currentMarketValue = movement.amount,
                    updatedAt = System.currentTimeMillis()
                )
            }
            AssetMovementType.STATUS_CHANGE -> {
                val resolved = com.example.domain.model.fixedassets.statemachine.AssetStateMachine.resolveState(movement.title)
                asset.copy(
                    status = resolved.code,
                    stateNotes = movement.notes,
                    updatedAt = System.currentTimeMillis()
                )
            }
            else -> null
        }
        if (updated != null) {
            fixedAssetDao.updateAsset(updated)
        }

        val now = System.currentTimeMillis()
        val formattedDate = LocalDateTime.now().format(auditDateFormatter)
        val auditLog = AssetAuditLogEntity(
            id = "audit_${UUID.randomUUID()}",
            userId = movement.userId,
            assetId = movement.assetId,
            assetName = asset.name,
            actionType = when (movement.type) {
                "STATUS_CHANGE" -> "CAMBIO_ESTADO"
                "MAINTENANCE" -> "MANTENIMIENTO"
                "IMPROVEMENT" -> "MODIFICACION"
                "DEPRECIATION" -> "DEPRECIACION"
                "DISPOSAL" -> "BAJA"
                else -> "MOVIMIENTO"
            },
            actionTitle = "${movement.title} (${asset.name})",
            details = "Movimiento [${movement.type}] por $${movement.amount} COP. ${movement.notes ?: ""}".trim(),
            financialImpact = movement.financialImpact,
            timestamp = now,
            formattedDate = formattedDate
        )
        assetAuditLogDao?.insertAuditLog(auditLog)
    }

    override suspend fun runAutomaticDepreciation(
        userId: String,
        asOfDate: String
    ): Result<DepreciationRunSummary> = runCatching {
        val allAssets = fixedAssetDao.observeAssets(userId).firstOrNull() ?: emptyList()
        val pendingList = calculateDepreciationUseCase.calculateBatchPending(allAssets, asOfDate)

        val createdMovements = mutableListOf<AssetMovementEntity>()
        val updatedAssets = mutableListOf<FixedAssetEntity>()
        var totalAmount = 0.0

        for (item in pendingList) {
            val mov = AssetMovementEntity(
                id = "dep_${UUID.randomUUID()}",
                assetId = item.asset.id,
                userId = userId,
                type = AssetMovementType.DEPRECIATION.name,
                title = "Depreciación Periódica: ${item.asset.name}",
                date = asOfDate,
                amount = item.totalPendingAmount,
                notes = "Cierre contable automático (${item.monthsToDepreciate} mes/es). Cuota mensual: ${"%.2f".format(item.monthlyAmount)}"
            )
            createdMovements.add(mov)

            val updated = item.asset.copy(
                accumulatedDepreciation = item.newAccumulatedDepreciation,
                lastDepreciationDate = asOfDate,
                updatedAt = System.currentTimeMillis()
            )
            updatedAssets.add(updated)
            totalAmount += item.totalPendingAmount
        }

        if (createdMovements.isNotEmpty()) {
            fixedAssetDao.insertMovements(createdMovements)
            for (updated in updatedAssets) {
                fixedAssetDao.updateAsset(updated)
            }

            val now = System.currentTimeMillis()
            val formattedDate = LocalDateTime.now().format(auditDateFormatter)
            val auditLog = AssetAuditLogEntity(
                id = "audit_${UUID.randomUUID()}",
                userId = userId,
                assetId = null,
                assetName = "Lote de ${updatedAssets.size} Activos",
                actionType = "DEPRECIACION",
                actionTitle = "Cierre Mensual de Depreciación Contable NIC 16",
                details = "Ejecutado cierre al $asOfDate. Se aplicó cuota mensual de depreciación a ${updatedAssets.size} activos.",
                financialImpact = "Gasto del periodo (- $${"%,.0f".format(totalAmount)} COP)",
                timestamp = now,
                formattedDate = formattedDate
            )
            assetAuditLogDao?.insertAuditLog(auditLog)
        }

        DepreciationRunSummary(
            assetsProcessed = allAssets.size,
            assetsDepreciatedCount = updatedAssets.size,
            totalDepreciationAmount = totalAmount,
            createdMovements = createdMovements,
            updatedAssets = updatedAssets,
            executionDate = asOfDate,
            message = if (updatedAssets.isNotEmpty()) {
                "Se depreciaron ${updatedAssets.size} activos exitosamente por un total de $${"%,.0f".format(totalAmount)}."
            } else {
                "Todos los activos están al día con su depreciación contable."
            }
        )
    }

    override suspend fun seedDefaultAssetsIfNeeded(userId: String) {
        // En versión de producción 9.0.0, eliminamos cualquier activo o movimiento de prueba/mock previo
        val existing = fixedAssetDao.observeAssets(userId).firstOrNull() ?: emptyList()
        val mockAssets = existing.filter { it.id.startsWith("AF-00") }
        for (mockAsset in mockAssets) {
            fixedAssetDao.deleteAssetById(mockAsset.id)
            fixedAssetDao.deleteMovementsByAssetId(mockAsset.id)
        }
        // Base de datos limpia sin mock data: los activos son creados únicamente por el usuario
    }

    override fun observeCategories(userId: String): Flow<List<AssetCategoryEntity>> {
        return assetCategoryDao?.observeCategories(userId) ?: flowOf(emptyList())
    }

    override suspend fun saveCategory(category: AssetCategoryEntity): Result<Unit> = runCatching {
        assetCategoryDao?.insertCategory(category)
            ?: throw IllegalStateException("AssetCategoryDao is not initialized")
    }

    override suspend fun deleteCategory(categoryId: String): Result<Unit> = runCatching {
        assetCategoryDao?.deleteCategoryById(categoryId)
            ?: throw IllegalStateException("AssetCategoryDao is not initialized")
    }

    override suspend fun seedDefaultCategoriesIfNeeded(userId: String) {
        if (assetCategoryDao == null) return
        val count = assetCategoryDao.getCategoriesCount(userId)
        if (count > 0) return

        val defaults = listOf(
            AssetCategoryEntity("cat_inmuebles", userId, "Bienes Raíces e Inmuebles", "INM", "apartment", "#10B981", true),
            AssetCategoryEntity("cat_vehiculos", userId, "Vehículos y Transporte", "VEH", "directions_car", "#0284C7", true),
            AssetCategoryEntity("cat_tecnologia", userId, "Tecnología y Cómputo", "TEC", "laptop_chromebook", "#6366F1", true),
            AssetCategoryEntity("cat_maquinaria", userId, "Maquinaria y Equipo", "MAQ", "precision_manufacturing", "#F59E0B", true),
            AssetCategoryEntity("cat_muebles", userId, "Muebles y Enseres", "MUE", "chair", "#EC4899", true),
            AssetCategoryEntity("cat_software", userId, "Software y Licencias", "SOF", "terminal", "#8B5CF6", true),
            AssetCategoryEntity("cat_marcas", userId, "Marcas y Patentes", "MAR", "verified", "#14B8A6", true),
            AssetCategoryEntity("cat_otros", userId, "Otros Activos Fijos", "OTR", "inventory_2", "#64748B", true)
        )
        assetCategoryDao.insertCategories(defaults)
    }

    override fun observeAuditLogs(userId: String): Flow<List<AssetAuditLogEntity>> {
        return assetAuditLogDao?.observeAuditLogs(userId) ?: flowOf(emptyList())
    }

    override suspend fun recordAuditLog(log: AssetAuditLogEntity): Result<Unit> = runCatching {
        assetAuditLogDao?.insertAuditLog(log)
            ?: throw IllegalStateException("AssetAuditLogDao is not initialized")
    }
}
