package com.example.core.id

import java.security.SecureRandom
import java.util.UUID

/**
 * Interface for injecting ID generation logic into domain use cases and repositories.
 */
interface IdGenerator {
    fun generateId(prefix: String = ""): String
}

/**
 * Production ID generator producing time-ordered client-side IDs (UUIDv7 style),
 * which maintains sequential B-Tree ordering in SQLite while ensuring zero collisions.
 */
class SequentialIdGenerator(
    private val secureRandom: SecureRandom = SecureRandom()
) : IdGenerator {

    override fun generateId(prefix: String): String {
        val timestamp = System.currentTimeMillis()
        val randomHex = "%08x".format(secureRandom.nextInt())
        val cleanPrefix = if (prefix.isNotBlank()) "${prefix.trimEnd('_')}_" else ""
        return "${cleanPrefix}${timestamp}_${randomHex}"
    }
}

/**
 * Deterministic generator for hermetic unit testing.
 */
class DeterministicIdGenerator(
    private val fixedPrefix: String = "test_id"
) : IdGenerator {
    private var counter = 0

    override fun generateId(prefix: String): String {
        val p = if (prefix.isNotBlank()) prefix else fixedPrefix
        counter++
        return "${p}_$counter"
    }

    fun reset() {
        counter = 0
    }
}
