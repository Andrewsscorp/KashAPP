package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ThirdPartyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThirdPartyDao {

    @Query("SELECT * FROM third_parties WHERE (userId = :userId OR userId = 'user_default') AND isActive = 1 ORDER BY isFavorite DESC, name ASC")
    fun getActiveThirdPartiesFlow(userId: String = "user_default"): Flow<List<ThirdPartyEntity>>

    @Query("SELECT * FROM third_parties WHERE (userId = :userId OR userId = 'user_default') ORDER BY isFavorite DESC, name ASC")
    fun getAllThirdPartiesFlow(userId: String = "user_default"): Flow<List<ThirdPartyEntity>>

    @Query("SELECT * FROM third_parties WHERE (userId = :userId OR userId = 'user_default') AND isFavorite = 1 AND isActive = 1 ORDER BY name ASC")
    fun getFavoriteThirdPartiesFlow(userId: String = "user_default"): Flow<List<ThirdPartyEntity>>

    @Query("SELECT * FROM third_parties WHERE (userId = :userId OR userId = 'user_default') AND category = :category AND isActive = 1 ORDER BY name ASC")
    fun getThirdPartiesByCategoryFlow(userId: String = "user_default", category: String): Flow<List<ThirdPartyEntity>>

    @Query("""
        SELECT * FROM third_parties 
        WHERE (userId = :userId OR userId = 'user_default') AND isActive = 1 
          AND (name LIKE '%' || :query || '%' 
               OR commercialName LIKE '%' || :query || '%' 
               OR idNumber LIKE '%' || :query || '%'
               OR primaryPhone LIKE '%' || :query || '%')
        ORDER BY isFavorite DESC, name ASC
    """)
    fun searchThirdPartiesFlow(userId: String = "user_default", query: String): Flow<List<ThirdPartyEntity>>

    @Query("SELECT DISTINCT category FROM third_parties WHERE (userId = :userId OR userId = 'user_default') AND category IS NOT NULL AND category != ''")
    fun getDistinctCategoriesFlow(userId: String = "user_default"): Flow<List<String>>

    @Query("SELECT * FROM third_parties WHERE id = :id LIMIT 1")
    suspend fun getThirdPartyById(id: String): ThirdPartyEntity?

    @Query("""
        SELECT * FROM third_parties 
        WHERE (userId = :userId OR userId = 'user_default') AND normalizedName = :normalizedName AND idNumber = :idNumber 
        LIMIT 1
    """)
    suspend fun findDuplicate(userId: String, normalizedName: String, idNumber: String): ThirdPartyEntity?

    @Query("SELECT * FROM third_parties WHERE (userId = :userId OR userId = 'user_default') AND idNumber = :idNumber")
    suspend fun findByIdNumber(userId: String, idNumber: String): List<ThirdPartyEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertThirdParty(entity: ThirdPartyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThirdParties(entities: List<ThirdPartyEntity>)

    @Update
    suspend fun updateThirdParty(entity: ThirdPartyEntity)

    @Query("UPDATE third_parties SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE third_parties SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setActive(id: String, isActive: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM third_parties WHERE id = :id")
    suspend fun deleteThirdParty(id: String)

    @Query("DELETE FROM third_parties WHERE id LIKE 'tp_seed_%' OR name LIKE '%Supermercados Éxito%' OR name LIKE '%Carulla%' OR name LIKE '%Terpel%' OR name LIKE '%Automotores Toyota%' OR name LIKE '%Los Coches%' OR name LIKE '%Servientrega%'")
    suspend fun purgeLegacySeedThirdParties()

    @Query("SELECT COUNT(*) FROM third_parties WHERE userId = :userId AND isActive = 1")
    suspend fun countActiveThirdParties(userId: String = "user_default"): Int
}
