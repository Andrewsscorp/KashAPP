package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.MerchantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantDao {

    @Query("SELECT * FROM merchants WHERE (userId IS NULL OR (userId = :userId AND :userId != '')) AND isActive = 1 ORDER BY canonicalName ASC")
    fun getActiveMerchantsFlow(userId: String): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchants WHERE userId IS NULL OR (userId = :userId AND :userId != '') ORDER BY canonicalName ASC")
    fun getAllMerchantsFlow(userId: String): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchants WHERE id = :id LIMIT 1")
    suspend fun getMerchantById(id: String): MerchantEntity?

    @Query("SELECT * FROM merchants WHERE LOWER(canonicalName) = LOWER(:name) OR LOWER(alias) = LOWER(:name) LIMIT 1")
    suspend fun findMerchantByNameOrAlias(name: String): MerchantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMerchant(merchant: MerchantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMerchants(merchants: List<MerchantEntity>)

    @Update
    suspend fun updateMerchant(merchant: MerchantEntity)

    @Query("UPDATE merchants SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setMerchantActive(id: String, isActive: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM merchants")
    suspend fun getCount(): Int

    @Query("DELETE FROM merchants WHERE userId = :userId AND :userId != ''")
    suspend fun deleteMerchantsByUserId(userId: String): Int
}
