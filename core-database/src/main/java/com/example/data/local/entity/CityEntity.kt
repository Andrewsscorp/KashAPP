package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing Cities for master data and branch locations.
 */
@Entity(tableName = "cities")
data class CityEntity(
    @PrimaryKey
    val id: String,
    val userId: String? = null,
    val countryCode: String,
    val name: String,
    val stateOrDepartment: String? = null
)
