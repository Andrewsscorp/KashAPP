package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ThirdPartyCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThirdPartyCategoryDao {

    @Query("SELECT * FROM third_party_categories WHERE (userId = :userId AND :userId != '') OR isSystem = 1 ORDER BY isSystem DESC, name ASC")
    fun getCategoriesFlow(userId: String = "user_default"): Flow<List<ThirdPartyCategoryEntity>>

    @Query("SELECT * FROM third_party_categories WHERE (userId = :userId AND :userId != '') OR isSystem = 1 ORDER BY isSystem DESC, name ASC")
    suspend fun getCategories(userId: String = "user_default"): List<ThirdPartyCategoryEntity>

    @Query("SELECT * FROM third_party_categories WHERE ((userId = :userId AND :userId != '') OR isSystem = 1) AND code = :code LIMIT 1")
    suspend fun findByCode(userId: String, code: String): ThirdPartyCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(entity: ThirdPartyCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(entities: List<ThirdPartyCategoryEntity>)

    @Query("DELETE FROM third_party_categories WHERE id = :id AND isSystem = 0")
    suspend fun deleteCategory(id: String)

    @Query("DELETE FROM third_party_categories WHERE userId = :userId AND isSystem = 0 AND :userId != ''")
    suspend fun deleteCategoriesByUserId(userId: String): Int

    @Query("UPDATE third_party_categories SET userId = :targetUserId WHERE userId = 'user_default' AND isSystem = 0 AND :targetUserId != ''")
    suspend fun migrateCategoriesUserDefault(targetUserId: String): Int
}
