package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AssetMovementEntity
import com.example.data.local.entity.FixedAssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FixedAssetDao {

    @Query("SELECT * FROM fixed_assets WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeAssets(userId: String): Flow<List<FixedAssetEntity>>

    @Query("SELECT * FROM fixed_assets WHERE id = :assetId LIMIT 1")
    suspend fun getAssetById(assetId: String): FixedAssetEntity?

    @Query("SELECT * FROM fixed_assets WHERE id = :assetId LIMIT 1")
    fun observeAssetById(assetId: String): Flow<FixedAssetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: FixedAssetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<FixedAssetEntity>)

    @Update
    suspend fun updateAsset(asset: FixedAssetEntity)

    @Delete
    suspend fun deleteAsset(asset: FixedAssetEntity)

    @Query("DELETE FROM fixed_assets WHERE id = :assetId")
    suspend fun deleteAssetById(assetId: String)

    // Movements
    @Query("SELECT * FROM asset_movements WHERE userId = :userId ORDER BY date DESC, createdAt DESC")
    fun observeAllMovements(userId: String): Flow<List<AssetMovementEntity>>

    @Query("SELECT * FROM asset_movements WHERE assetId = :assetId ORDER BY date DESC, createdAt DESC")
    fun observeMovementsForAsset(assetId: String): Flow<List<AssetMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: AssetMovementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<AssetMovementEntity>)

    @Query("SELECT * FROM asset_movements WHERE assetId = :assetId ORDER BY date DESC, createdAt DESC")
    suspend fun getMovementsForAsset(assetId: String): List<AssetMovementEntity>

    @Query("DELETE FROM asset_movements WHERE assetId = :assetId")
    suspend fun deleteMovementsByAssetId(assetId: String)
}
