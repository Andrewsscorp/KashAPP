package com.example.domain.model.fixedassets

import com.example.data.local.entity.AssetMovementEntity
import com.example.data.local.entity.FixedAssetEntity

/**
 * A milestone point in financial projections.
 */
data class AssetProjectionPoint(
    val yearLabel: String,
    val yearOffset: Int,
    val totalAcquisitionCost: Double,
    val projectedBookValue: Double,
    val projectedAccumulatedDepreciation: Double,
    val projectedMarketValue: Double,
    val depreciationInYear: Double
)

/**
 * Represents an asset approaching complete obsolescence / end of useful life,
 * requiring replacement planning or reinvestment.
 */
data class ObsolescenceMilestone(
    val assetId: String,
    val assetName: String,
    val category: AssetCategory,
    val currentAgeMonths: Int,
    val totalUsefulLifeMonths: Int,
    val remainingLifeMonths: Int,
    val estimatedReplacementYear: Int,
    val estimatedReplacementCost: Double,
    val urgencyStatus: String // "CRÍTICO", "PRÓXIMO A VENCER", "ÓPTIMO"
)

/**
 * Consolidated multi-year projection result.
 */
data class ConsolidatedAssetProjection(
    val horizonYears: Int,
    val points: List<AssetProjectionPoint>,
    val currentTotalBookValue: Double,
    val projectedEndBookValue: Double,
    val projectedEndMarketValue: Double,
    val totalProjectedDepreciation: Double,
    val netCapitalAppreciationOrLoss: Double,
    val milestones: List<ObsolescenceMilestone>
)

/**
 * Result of running automatic batch depreciation for user's assets.
 */
data class DepreciationRunSummary(
    val assetsProcessed: Int,
    val assetsDepreciatedCount: Int,
    val totalDepreciationAmount: Double,
    val createdMovements: List<AssetMovementEntity>,
    val updatedAssets: List<FixedAssetEntity>,
    val executionDate: String,
    val message: String
)
