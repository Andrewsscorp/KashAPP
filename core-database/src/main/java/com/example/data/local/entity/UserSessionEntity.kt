package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.models.UserSession

/**
 * Encrypted Room entity representing the persisted user authentication session.
 * Bridges Firebase Authentication with the local encrypted Room database for offline-first resilience.
 */
@Entity(tableName = "user_sessions")
data class UserSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "email")
    val email: String? = null,

    @ColumnInfo(name = "display_name")
    val displayName: String? = null,

    @ColumnInfo(name = "photo_url")
    val photoUrl: String? = null,

    @ColumnInfo(name = "auth_provider")
    val authProvider: String = "firebase",

    @ColumnInfo(name = "is_email_verified")
    val isEmailVerified: Boolean = false,

    @ColumnInfo(name = "id_token")
    val idToken: String? = null,

    @ColumnInfo(name = "session_created_at")
    val sessionCreatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_login_timestamp")
    val lastLoginTimestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_active_timestamp")
    val lastActiveTimestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "is_anonymous")
    val isAnonymous: Boolean = false,

    @ColumnInfo(name = "metadata")
    val metadata: String? = null
) {
    fun toDomain(): UserSession {
        return UserSession(
            userId = userId,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
            authProvider = authProvider,
            isEmailVerified = isEmailVerified,
            sessionCreatedAt = sessionCreatedAt,
            lastActiveTimestamp = lastActiveTimestamp,
            isActive = isActive,
            isAnonymous = isAnonymous,
            idToken = idToken
        )
    }

    companion object {
        fun fromDomain(session: UserSession, metadata: String? = null): UserSessionEntity {
            return UserSessionEntity(
                userId = session.userId,
                email = session.email,
                displayName = session.displayName,
                photoUrl = session.photoUrl,
                authProvider = session.authProvider,
                isEmailVerified = session.isEmailVerified,
                sessionCreatedAt = session.sessionCreatedAt,
                lastLoginTimestamp = session.lastActiveTimestamp,
                lastActiveTimestamp = session.lastActiveTimestamp,
                isActive = session.isActive,
                isAnonymous = session.isAnonymous,
                idToken = session.idToken,
                metadata = metadata
            )
        }
    }
}
