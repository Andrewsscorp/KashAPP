package com.example.domain.repository

import com.example.data.local.entity.ThirdPartyEntity
import kotlinx.coroutines.flow.Flow

class DuplicateThirdPartyException(message: String) : Exception(message)

interface ThirdPartyRepository {
    fun observeThirdParties(userId: String = "user_default"): Flow<List<ThirdPartyEntity>>
    fun observeFavorites(userId: String = "user_default"): Flow<List<ThirdPartyEntity>>
    fun observeByCategory(userId: String = "user_default", category: String): Flow<List<ThirdPartyEntity>>
    fun searchThirdParties(userId: String = "user_default", query: String): Flow<List<ThirdPartyEntity>>
    suspend fun getThirdPartyById(id: String): ThirdPartyEntity?
    suspend fun saveThirdParty(entity: ThirdPartyEntity): Result<ThirdPartyEntity>
    suspend fun setFavorite(id: String, isFavorite: Boolean): Result<Unit>
    suspend fun deleteThirdParty(id: String): Result<Unit>
    suspend fun seedDefaultThirdPartiesIfNeeded(userId: String = "user_default")
}
