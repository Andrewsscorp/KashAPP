package com.example.data.repository

import com.example.data.local.dao.ThirdPartyDao
import com.example.data.local.entity.ThirdPartyEntity
import com.example.domain.repository.DuplicateThirdPartyException
import com.example.domain.repository.ThirdPartyRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ThirdPartyRepositoryImpl(
    private val thirdPartyDao: ThirdPartyDao
) : ThirdPartyRepository {

    override fun observeThirdParties(userId: String): Flow<List<ThirdPartyEntity>> {
        return thirdPartyDao.getActiveThirdPartiesFlow(userId)
    }

    override fun observeFavorites(userId: String): Flow<List<ThirdPartyEntity>> {
        return thirdPartyDao.getFavoriteThirdPartiesFlow(userId)
    }

    override fun observeByCategory(userId: String, category: String): Flow<List<ThirdPartyEntity>> {
        return thirdPartyDao.getThirdPartiesByCategoryFlow(userId, category)
    }

    override fun searchThirdParties(userId: String, query: String): Flow<List<ThirdPartyEntity>> {
        val trimmed = query.trim()
        return if (trimmed.isBlank()) {
            thirdPartyDao.getActiveThirdPartiesFlow(userId)
        } else {
            thirdPartyDao.searchThirdPartiesFlow(userId, trimmed)
        }
    }

    override suspend fun getThirdPartyById(id: String): ThirdPartyEntity? {
        return thirdPartyDao.getThirdPartyById(id)
    }

    override suspend fun saveThirdParty(entity: ThirdPartyEntity): Result<ThirdPartyEntity> {
        return try {
            val normalizedName = ThirdPartyEntity.normalizeName(entity.name)
            val normalizedId = ThirdPartyEntity.normalizeId(entity.idNumber)

            if (normalizedName.isBlank()) {
                return Result.failure(IllegalArgumentException("El nombre o razón social no puede estar vacío."))
            }
            if (normalizedId.isBlank()) {
                return Result.failure(IllegalArgumentException("El número de documento o NIT no puede estar vacío."))
            }

            // Check duplicate by tuple (userId, normalizedName, idNumber)
            val existing = thirdPartyDao.findDuplicate(entity.userId, normalizedName, normalizedId)
            if (existing != null && existing.id != entity.id) {
                return Result.failure(
                    DuplicateThirdPartyException(
                        "Ya existe un tercero registrado con el nombre '${existing.name}' y documento '${existing.idNumber}' (Código: ${existing.displayCode})."
                    )
                )
            }

            val finalEntity = entity.copy(
                normalizedName = normalizedName,
                idNumber = normalizedId,
                updatedAt = System.currentTimeMillis()
            )

            val isNew = thirdPartyDao.getThirdPartyById(finalEntity.id) == null
            if (isNew) {
                thirdPartyDao.insertThirdParty(finalEntity)
            } else {
                thirdPartyDao.updateThirdParty(finalEntity)
            }

            Result.success(finalEntity)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun setFavorite(id: String, isFavorite: Boolean): Result<Unit> {
        return try {
            thirdPartyDao.setFavorite(id, isFavorite)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun deleteThirdParty(id: String): Result<Unit> {
        return try {
            // Soft-delete to preserve foreign-key audit and history references
            thirdPartyDao.setActive(id, false)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun seedDefaultThirdPartiesIfNeeded(userId: String) {
        try {
            val count = thirdPartyDao.countActiveThirdParties(userId)
            if (count == 0) {
                val now = System.currentTimeMillis()
                val defaults = listOf(
                    ThirdPartyEntity(
                        id = "tp_seed_exito",
                        userId = userId,
                        name = "Supermercados Éxito S.A.",
                        normalizedName = "supermercados exito s.a.",
                        personType = "JURIDICA",
                        idType = "NIT",
                        idNumber = "890900608",
                        verificationDigit = "9",
                        commercialName = "Grupo Éxito",
                        category = "TIENDA_COMERCIO",
                        primaryPhone = "(604) 339-6565",
                        email = "atencion@exito.com",
                        city = "Medellín",
                        iconName = "Storefront",
                        colorHex = "#EAB308",
                        isFavorite = true,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ThirdPartyEntity(
                        id = "tp_seed_carulla",
                        userId = userId,
                        name = "Carulla FreshMarket",
                        normalizedName = "carulla freshmarket",
                        personType = "JURIDICA",
                        idType = "NIT",
                        idNumber = "890900608",
                        verificationDigit = "1",
                        commercialName = "Carulla",
                        category = "TIENDA_COMERCIO",
                        primaryPhone = "(601) 745-5000",
                        city = "Bogotá",
                        iconName = "LocalGroceryStore",
                        colorHex = "#16A34A",
                        isFavorite = true,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ThirdPartyEntity(
                        id = "tp_seed_terpel",
                        userId = userId,
                        name = "Organización Terpel S.A.",
                        normalizedName = "organizacion terpel s.a.",
                        personType = "JURIDICA",
                        idType = "NIT",
                        idNumber = "860005224",
                        verificationDigit = "6",
                        commercialName = "Terpel",
                        category = "TIENDA_COMERCIO",
                        primaryPhone = "018000918027",
                        city = "Bogotá",
                        iconName = "LocalGasStation",
                        colorHex = "#DC2626",
                        isFavorite = true,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ThirdPartyEntity(
                        id = "tp_seed_toyota",
                        userId = userId,
                        name = "Automotores Toyota Colombia S.A.S.",
                        normalizedName = "automotores toyota colombia s.a.s.",
                        personType = "JURIDICA",
                        idType = "NIT",
                        idNumber = "860032904",
                        verificationDigit = "2",
                        commercialName = "Toyota Concesionario",
                        category = "CONCESIONARIO",
                        primaryPhone = "018000123692",
                        city = "Bogotá",
                        iconName = "DirectionsCar",
                        colorHex = "#0284C7",
                        isFavorite = true,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ThirdPartyEntity(
                        id = "tp_seed_loscoches",
                        userId = userId,
                        name = "Los Coches La Sabana S.A.S.",
                        normalizedName = "los coches la sabana s.a.s.",
                        personType = "JURIDICA",
                        idType = "NIT",
                        idNumber = "860505199",
                        verificationDigit = "1",
                        commercialName = "Concesionario Los Coches",
                        category = "CONCESIONARIO",
                        primaryPhone = "(601) 486-5000",
                        city = "Bogotá",
                        iconName = "DirectionsCar",
                        colorHex = "#4F46E5",
                        isFavorite = false,
                        createdAt = now,
                        updatedAt = now
                    ),
                    ThirdPartyEntity(
                        id = "tp_seed_servientrega",
                        userId = userId,
                        name = "Servientrega S.A.",
                        normalizedName = "servientrega s.a.",
                        personType = "JURIDICA",
                        idType = "NIT",
                        idNumber = "860512330",
                        verificationDigit = "3",
                        commercialName = "Servientrega Envíos",
                        category = "PROVEEDOR",
                        primaryPhone = "(601) 770-0200",
                        city = "Bogotá",
                        iconName = "LocalShipping",
                        colorHex = "#059669",
                        isFavorite = false,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                thirdPartyDao.insertThirdParties(defaults)
            }
        } catch (_: Throwable) {}
    }
}
