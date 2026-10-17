package com.example.domain.repository

import com.example.data.local.entity.AssetMovementEntity
import com.example.data.local.entity.FixedAssetEntity
import com.example.domain.model.fixedassets.DepreciationRunSummary
import kotlinx.coroutines.flow.Flow

interface FixedAssetRepository {
    fun observeAssets(userId: String): Flow<List<FixedAssetEntity>>
    suspend fun getAssetById(assetId: String): FixedAssetEntity?
    fun observeAssetById(assetId: String): Flow<FixedAssetEntity?>
    suspend fun saveAsset(asset: FixedAssetEntity): Result<Unit>
    suspend fun deleteAsset(assetId: String): Result<Unit>

    fun observeMovements(assetId: String): Flow<List<AssetMovementEntity>>
    fun observeAllMovements(userId: String): Flow<List<AssetMovementEntity>>
    suspend fun recordMovement(movement: AssetMovementEntity): Result<Unit>

    suspend fun runAutomaticDepreciation(
        userId: String,
        asOfDate: String
    ): Result<DepreciationRunSummary>

    // Category Persistence
    fun observeCategories(userId: String): Flow<List<com.example.data.local.entity.AssetCategoryEntity>>
    suspend fun saveCategory(category: com.example.data.local.entity.AssetCategoryEntity): Result<Unit>
    suspend fun deleteCategory(categoryId: String): Result<Unit>
    suspend fun seedDefaultCategoriesIfNeeded(userId: String)
    suspend fun seedDefaultAssetsIfNeeded(userId: String)

    // Audit Trail (NIIF / NIC 16)
    fun observeAuditLogs(userId: String): Flow<List<com.example.data.local.entity.AssetAuditLogEntity>>
    suspend fun recordAuditLog(log: com.example.data.local.entity.AssetAuditLogEntity): Result<Unit>
}
