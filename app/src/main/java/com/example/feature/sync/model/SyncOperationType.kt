package com.example.feature.sync.model

/**
 * Enterprise sync operation and entity types for the Transactional Outbox.
 */
enum class SyncOperationType(val value: String) {
    CREATE("CREATE"),
    UPDATE("UPDATE"),
    DELETE("DELETE");

    companion object {
        fun fromString(value: String): SyncOperationType =
            entries.find { it.value.equals(value, ignoreCase = true) } ?: CREATE
    }
}

enum class SyncEntityType(val value: String) {
    TRANSACTION("TRANSACTION"),
    ACCOUNT("ACCOUNT"),
    CREDIT_CARD("CREDIT_CARD"),
    FIXED_ASSET("FIXED_ASSET"),
    CATEGORY("CATEGORY"),
    BUDGET("BUDGET"),
    GOAL("GOAL");

    companion object {
        fun fromString(value: String): SyncEntityType =
            entries.find { it.value.equals(value, ignoreCase = true) } ?: TRANSACTION
    }
}
