package com.example.presentation.thirdparty.model

import com.example.data.local.entity.ThirdPartyEntity
import java.util.UUID

enum class ThirdPartyCategoryFilter(val code: String, val label: String) {
    TODOS("TODOS", "Todos"),
    FAVORITOS("FAVORITOS", "⭐ Favoritos"),
    PROVEEDOR("PROVEEDOR", "Proveedores"),
    CONCESIONARIO("CONCESIONARIO", "Concesionarios"),
    TIENDA_COMERCIO("TIENDA_COMERCIO", "Tiendas y Comercios"),
    CLIENTE("CLIENTE", "Clientes"),
    ACREEDOR("ACREEDOR", "Acreedores")
}

data class ThirdPartyFormState(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val personType: String = "NATURAL", // "NATURAL", "JURIDICA"
    val idType: String = "CC",          // "CC", "NIT", "CE", "PASAPORTE", "RUT"
    val idNumber: String = "",
    val verificationDigit: String = "",
    val commercialName: String = "",
    val category: String = "PROVEEDOR",
    val primaryPhone: String = "",
    val secondaryPhone: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val paymentTermsDays: String = "0",
    val defaultBank: String = "",
    val bankAccountType: String = "AHORROS",
    val bankAccountNumber: String = "",
    val iconName: String = "Person",
    val colorHex: String = "#0284C7",
    val notes: String = "",
    val isFavorite: Boolean = false,
    val isExpressMode: Boolean = true
) {
    fun toEntity(userId: String = "user_default"): ThirdPartyEntity {
        return ThirdPartyEntity(
            id = id,
            userId = userId,
            name = name.trim(),
            normalizedName = ThirdPartyEntity.normalizeName(name),
            personType = personType,
            idType = idType,
            idNumber = ThirdPartyEntity.normalizeId(idNumber),
            verificationDigit = verificationDigit.trim().takeIf { it.isNotBlank() },
            commercialName = commercialName.trim().takeIf { it.isNotBlank() },
            category = category,
            primaryPhone = primaryPhone.trim().takeIf { it.isNotBlank() },
            secondaryPhone = secondaryPhone.trim().takeIf { it.isNotBlank() },
            email = email.trim().takeIf { it.isNotBlank() },
            address = address.trim().takeIf { it.isNotBlank() },
            city = city.trim().takeIf { it.isNotBlank() },
            paymentTermsDays = paymentTermsDays.toIntOrNull() ?: 0,
            defaultBank = defaultBank.trim().takeIf { it.isNotBlank() },
            bankAccountType = bankAccountType.takeIf { it.isNotBlank() },
            bankAccountNumber = bankAccountNumber.trim().takeIf { it.isNotBlank() },
            iconName = iconName,
            colorHex = colorHex,
            notes = notes.trim().takeIf { it.isNotBlank() },
            isFavorite = isFavorite,
            isActive = true
        )
    }

    companion object {
        fun fromEntity(entity: ThirdPartyEntity): ThirdPartyFormState {
            return ThirdPartyFormState(
                id = entity.id,
                name = entity.name,
                personType = entity.personType,
                idType = entity.idType,
                idNumber = entity.idNumber,
                verificationDigit = entity.verificationDigit ?: "",
                commercialName = entity.commercialName ?: "",
                category = entity.category,
                primaryPhone = entity.primaryPhone ?: "",
                secondaryPhone = entity.secondaryPhone ?: "",
                email = entity.email ?: "",
                address = entity.address ?: "",
                city = entity.city ?: "",
                paymentTermsDays = entity.paymentTermsDays.toString(),
                defaultBank = entity.defaultBank ?: "",
                bankAccountType = entity.bankAccountType ?: "AHORROS",
                bankAccountNumber = entity.bankAccountNumber ?: "",
                iconName = entity.iconName,
                colorHex = entity.colorHex,
                notes = entity.notes ?: "",
                isFavorite = entity.isFavorite,
                isExpressMode = false
            )
        }
    }
}

data class ThirdPartyUiState(
    val thirdParties: List<ThirdPartyEntity> = emptyList(),
    val filteredList: List<ThirdPartyEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: ThirdPartyCategoryFilter = ThirdPartyCategoryFilter.TODOS,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showCreateDialog: Boolean = false,
    val selectedThirdPartyForDetail: ThirdPartyEntity? = null,
    val formState: ThirdPartyFormState = ThirdPartyFormState(),
    val isExportingPdf: Boolean = false
)
