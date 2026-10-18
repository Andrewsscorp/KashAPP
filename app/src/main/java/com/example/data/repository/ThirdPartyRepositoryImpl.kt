package com.example.data.repository

import com.example.data.local.dao.ThirdPartyCategoryDao
import com.example.data.local.dao.ThirdPartyDao
import com.example.data.local.entity.ThirdPartyCategoryEntity
import com.example.data.local.entity.ThirdPartyEntity
import com.example.domain.repository.DuplicateThirdPartyException
import com.example.domain.repository.ThirdPartyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

class ThirdPartyRepositoryImpl(
    private val thirdPartyDao: ThirdPartyDao,
    private val thirdPartyCategoryDao: ThirdPartyCategoryDao? = null
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

    override fun observeCategories(userId: String): Flow<List<ThirdPartyCategoryEntity>> {
        return thirdPartyCategoryDao?.getCategoriesFlow(userId) ?: flowOf(emptyList())
    }

    override suspend fun getCategories(userId: String): List<ThirdPartyCategoryEntity> {
        return thirdPartyCategoryDao?.getCategories(userId) ?: emptyList()
    }

    override suspend fun createCategory(
        userId: String,
        name: String,
        code: String,
        iconName: String,
        colorHex: String
    ): Result<ThirdPartyCategoryEntity> {
        return try {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) {
                return Result.failure(IllegalArgumentException("El nombre del rol o categoría no puede estar vacío."))
            }
            val finalCode = if (code.isNotBlank()) code.trim().uppercase() else {
                trimmedName.uppercase().replace(Regex("[^A-Z0-9_]"), "_")
            }
            val entity = ThirdPartyCategoryEntity(
                id = "cat_custom_${UUID.randomUUID()}",
                userId = userId,
                name = trimmedName,
                code = finalCode,
                iconName = iconName.ifBlank { "Business" },
                colorHex = colorHex.ifBlank { "#0284C7" },
                isSystem = false,
                createdAt = System.currentTimeMillis()
            )
            thirdPartyCategoryDao?.insertCategory(entity)
            Result.success(entity)
        } catch (t: Throwable) {
            Result.failure(t)
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
            // Purgar cualquier semilla mock legacy previa de la base de datos
            thirdPartyDao.purgeLegacySeedThirdParties()

            val existingComun = thirdPartyDao.findDuplicate(userId, ThirdPartyEntity.normalizeName("Tercero Común"), "222222222")
                ?: thirdPartyDao.getThirdPartyById("tp_default_comun")

            if (existingComun == null) {
                val now = System.currentTimeMillis()
                val defaultComun = ThirdPartyEntity(
                    id = "tp_default_comun",
                    userId = userId,
                    name = "Tercero Común",
                    normalizedName = ThirdPartyEntity.normalizeName("Tercero Común"),
                    personType = "NATURAL",
                    idType = "CC",
                    idNumber = "222222222",
                    verificationDigit = null,
                    commercialName = "Tercero Común",
                    category = "PROVEEDOR",
                    primaryPhone = null,
                    secondaryPhone = null,
                    email = null,
                    address = null,
                    city = null,
                    paymentTermsDays = 0,
                    iconName = "Person",
                    colorHex = "#0284C7",
                    notes = "Tercero genérico por defecto para transacciones comerciales y compras",
                    isFavorite = false,
                    isActive = true,
                    createdAt = now,
                    updatedAt = now
                )
                thirdPartyDao.insertThirdParty(defaultComun)
            }

            // Seed default categories if needed
            if (thirdPartyCategoryDao != null) {
                val existingCats = thirdPartyCategoryDao.getCategories(userId)
                if (existingCats.isEmpty()) {
                    val now = System.currentTimeMillis()
                    val defaults = listOf(
                        Triple("Proveedor", "PROVEEDOR", "Storefront"),
                        Triple("Concesionario", "CONCESIONARIO", "DirectionsCar"),
                        Triple("Tienda / Comercio", "TIENDA_COMERCIO", "Business"),
                        Triple("Cliente", "CLIENTE", "Person"),
                        Triple("Acreedor", "ACREEDOR", "AccountBalance"),
                        Triple("Empleado", "EMPLEADO", "Groups"),
                        Triple("Taller / Servicio", "TALLER_SERVICIO", "Build"),
                        Triple("Contratista", "CONTRATISTA", "AssignmentInd")
                    )
                    val entities = defaults.map { (name, code, icon) ->
                        ThirdPartyCategoryEntity(
                            id = "cat_sys_$code",
                            userId = "system",
                            name = name,
                            code = code,
                            iconName = icon,
                            colorHex = "#0284C7",
                            isSystem = true,
                            createdAt = now
                        )
                    }
                    thirdPartyCategoryDao.insertCategories(entities)
                }
            }
        } catch (_: Throwable) {}
    }
}
