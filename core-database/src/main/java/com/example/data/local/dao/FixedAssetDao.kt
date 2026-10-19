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

    @Query("SELECT * FROM fixed_assets WHERE userId = :userId AND :userId != '' ORDER BY createdAt DESC")
    fun observeAssets(userId: String): Flow<List<FixedAssetEntity>>

    @Query("SELECT * FROM fixed_assets WHERE userId = :userId AND :userId != '' ORDER BY createdAt DESC")
    fun getAllAssetsFlow(userId: String = "user_default"): Flow<List<FixedAssetEntity>>

    @Query("UPDATE fixed_assets SET noAffectInventory = :noAffectInventory WHERE userId = :userId AND :userId != ''")
    suspend fun updateNoAffectInventoryAll(userId: String, noAffectInventory: Boolean): Int

    @Query("UPDATE fixed_assets SET noAffectInventory = :noAffectInventory")
    suspend fun updateNoAffectInventoryGlobal(noAffectInventory: Boolean): Int

    @Query("UPDATE fixed_assets SET noAffectInventory = :noAffectInventory WHERE id = :assetId")
    suspend fun updateAssetNoAffectInventory(assetId: String, noAffectInventory: Boolean): Int

    @Query("UPDATE fixed_assets SET noAffectDashboard = :noAffectDashboard WHERE userId = :userId AND :userId != ''")
    suspend fun updateNoAffectDashboardAll(userId: String, noAffectDashboard: Boolean): Int

    @Query("UPDATE fixed_assets SET noAffectDashboard = :noAffectDashboard")
    suspend fun updateNoAffectDashboardGlobal(noAffectDashboard: Boolean): Int

    @Query("UPDATE fixed_assets SET noAffectDashboard = :noAffectDashboard WHERE id = :assetId")
    suspend fun updateAssetNoAffectDashboard(assetId: String, noAffectDashboard: Boolean): Int

    @Query("SELECT * FROM fixed_assets WHERE id = :assetId LIMIT 1")
    suspend fun getAssetById(assetId: String): FixedAssetEntity?

    @Query("SELECT * FROM fixed_assets WHERE userId = :userId AND :userId != '' ORDER BY createdAt DESC")
    suspend fun getAssets(userId: String): List<FixedAssetEntity>

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

    @Query("DELETE FROM fixed_assets WHERE userId = :userId AND :userId != ''")
    suspend fun deleteAssetsByUserId(userId: String): Int

    @Query("UPDATE fixed_assets SET userId = :targetUserId WHERE userId = 'user_default' AND :targetUserId != ''")
    suspend fun migrateAssetsUserDefault(targetUserId: String): Int

    // Movements
    @Query("SELECT * FROM asset_movements WHERE userId = :userId AND :userId != '' ORDER BY date DESC, createdAt DESC")
    fun observeAllMovements(userId: String): Flow<List<AssetMovementEntity>>

    @Query("SELECT * FROM asset_movements WHERE userId = :userId AND :userId != '' ORDER BY date DESC, createdAt DESC")
    suspend fun getAllMovements(userId: String): List<AssetMovementEntity>

    @Query("SELECT * FROM asset_movements WHERE userId = :userId AND :userId != '' ORDER BY date DESC, createdAt DESC")
    fun getAllMovementsFlow(userId: String = "user_default"): Flow<List<AssetMovementEntity>>

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

    @Query("DELETE FROM asset_movements WHERE userId = :userId AND :userId != ''")
    suspend fun deleteMovementsByUserId(userId: String): Int

    @Query("UPDATE asset_movements SET userId = :targetUserId WHERE userId = 'user_default' AND :targetUserId != ''")
    suspend fun migrateMovementsUserDefault(targetUserId: String): Int
}
