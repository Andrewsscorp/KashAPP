package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AssetCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetCategoryDao {

    @Query("SELECT * FROM asset_categories WHERE userId = :userId OR isSystem = 1 ORDER BY isSystem DESC, createdAt ASC")
    fun observeCategories(userId: String): Flow<List<AssetCategoryEntity>>

    @Query("SELECT * FROM asset_categories WHERE id = :categoryId LIMIT 1")
    suspend fun getCategoryById(categoryId: String): AssetCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: AssetCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<AssetCategoryEntity>)

    @Update
    suspend fun updateCategory(category: AssetCategoryEntity)

    @Delete
    suspend fun deleteCategory(category: AssetCategoryEntity)

    @Query("DELETE FROM asset_categories WHERE id = :categoryId AND isSystem = 0")
    suspend fun deleteCategoryById(categoryId: String)

    @Query("SELECT COUNT(*) FROM asset_categories WHERE userId = :userId OR isSystem = 1")
    suspend fun getCategoriesCount(userId: String): Int
}
