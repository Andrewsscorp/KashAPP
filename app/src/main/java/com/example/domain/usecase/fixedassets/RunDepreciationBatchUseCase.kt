package com.example.domain.usecase.fixedassets

import com.example.data.local.entity.AssetMovementEntity
import com.example.data.local.entity.FixedAssetEntity
import com.example.domain.model.fixedassets.AssetMovementType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Clean Architecture Use Case for executing monthly depreciation batch (NIC 16).
 * Iterates through all operational assets ("En Uso"), calculates monthly quota according
 * to depreciation method, and prepares batch records.
 */
class RunDepreciationBatchUseCase(
    private val calculateDepreciationUseCase: CalculateAssetDepreciationUseCase = CalculateAssetDepreciationUseCase()
) {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    data class BatchExecutionResult(
        val batchId: String,
        val executionDate: String,
        val periodName: String,
        val totalAmount: Double,
        val assetsProcessedCount: Int,
        val assetsDepreciatedCount: Int,
        val createdMovements: List<AssetMovementEntity>,
        val updatedAssets: List<FixedAssetEntity>,
        val summaryMessage: String
    )

    fun execute(
        assets: List<FixedAssetEntity>,
        userId: String = "user_default",
        asOfDate: String = LocalDate.now().format(dateFormatter)
    ): BatchExecutionResult {
        val pendingList = calculateDepreciationUseCase.calculateBatchPending(assets, asOfDate)
        val batchId = "batch_${UUID.randomUUID().toString().take(8)}"

        val parsedDate = try {
            LocalDate.parse(asOfDate, dateFormatter)
        } catch (_: Exception) {
            LocalDate.now()
        }
        val monthNameSpanish = when (parsedDate.monthValue) {
            1 -> "Enero"
            2 -> "Febrero"
            3 -> "Marzo"
            4 -> "Abril"
            5 -> "Mayo"
            6 -> "Junio"
            7 -> "Julio"
            8 -> "Agosto"
            9 -> "Septiembre"
            10 -> "Octubre"
            11 -> "Noviembre"
            else -> "Diciembre"
        }
        val periodName = "$monthNameSpanish ${parsedDate.year}"

        val createdMovements = mutableListOf<AssetMovementEntity>()
        val updatedAssets = mutableListOf<FixedAssetEntity>()
        var totalAmount = 0.0

        for (item in pendingList) {
            val mov = AssetMovementEntity(
                id = "dep_${UUID.randomUUID()}",
                assetId = item.asset.id,
                userId = userId,
                type = AssetMovementType.DEPRECIATION.name,
                title = "Depreciación $periodName: ${item.asset.name}",
                date = asOfDate,
                amount = item.totalPendingAmount,
                notes = "Cierre contable mensual bajo NIC 16. Cuota cuatrimestral/mensual calculada.",
                costCenter = batchId,
                financialImpact = "Gasto por Depreciación NIIF (Estado de Resultados)"
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

        val message = if (updatedAssets.isNotEmpty()) {
            "Se ejecutó el cierre contable de $periodName. Se depreciaron ${updatedAssets.size} activos por un total de $${"%,.0f".format(totalAmount)}."
        } else {
            "Todos los activos están al día con su depreciación para $periodName."
        }

        return BatchExecutionResult(
            batchId = batchId,
            executionDate = asOfDate,
            periodName = periodName,
            totalAmount = totalAmount,
            assetsProcessedCount = assets.size,
            assetsDepreciatedCount = updatedAssets.size,
            createdMovements = createdMovements,
            updatedAssets = updatedAssets,
            summaryMessage = message
        )
    }
}
