package com.example.domain.model.fixedassets

import com.example.data.local.entity.AssetMovementEntity
import com.example.data.local.entity.FixedAssetEntity

data class DepreciationBatchItem(
    val assetId: String,
    val assetCode: String,
    val assetName: String,
    val category: String,
    val depreciationMethod: String,
    val amount: Double,
    val previousAccumulated: Double,
    val newAccumulated: Double,
    val bookValueAfter: Double,
    val movementId: String
)

data class DepreciationBatch(
    val id: String,
    val title: String,
    val date: String,
    val periodLabel: String,
    val totalAmount: Double,
    val assetsCount: Int,
    val isCompleted: Boolean = true,
    val items: List<DepreciationBatchItem>
)
