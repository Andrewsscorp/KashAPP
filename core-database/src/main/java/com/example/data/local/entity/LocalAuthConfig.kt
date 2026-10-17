package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for local authentication security parameters.
 * Encrypted and stored locally.
 */
@Entity(tableName = "local_auth_config")
data class LocalAuthConfig(
    @PrimaryKey
    val id: Int = 1,

    @ColumnInfo(name = "pin_hash")
    val pinHash: String = "",

    @ColumnInfo(name = "pin_salt")
    val pinSalt: String = "",

    @ColumnInfo(name = "is_biometric_enabled")
    val isBiometricEnabled: Boolean = true,

    @ColumnInfo(name = "failed_attempts")
    val failedAttempts: Int = 0,

    @ColumnInfo(name = "lockout_until_timestamp")
    val lockoutUntilTimestamp: Long = 0L,

    @ColumnInfo(name = "last_login_timestamp")
    val lastLoginTimestamp: Long = 0L,

    @ColumnInfo(name = "hash_algorithm")
    val hashAlgorithm: String = "PBKDF2-HMAC-SHA256-600000"
)
