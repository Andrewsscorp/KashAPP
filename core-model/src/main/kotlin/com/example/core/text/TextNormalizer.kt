package com.example.core.text

import java.text.Normalizer
import java.util.Locale

/**
 * Enterprise Canonical Text Normalizer for SQLite uniqueness and search indexing.
 * Ensures consistent canonical representations across all device locales and Unicode variations.
 */
object TextNormalizer {

    private val multipleSpacesRegex = Regex("\\s+")
    private val diacriticalMarksRegex = Regex("\\p{InCombiningDiacriticalMarks}+")

    /**
     * Normalizes a string for uniqueness constraints and indexing:
     * 1. Trims leading/trailing whitespace and collapses multiple spaces into one.
     * 2. Decomposes Unicode characters into NFKD form (e.g. 'é' -> 'e' + combining acute accent).
     * 3. Strips all diacritical marks/accents ('ñ' -> 'n', 'á' -> 'a', etc.).
     * 4. Converts to lowercase using Locale.ROOT to prevent language-dependent casing anomalies.
     */
    fun normalize(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val singleSpaced = input.trim().replace(multipleSpacesRegex, " ")
        val nfkd = Normalizer.normalize(singleSpaced, Normalizer.Form.NFKD)
        val withoutDiacritics = nfkd.replace(diacriticalMarksRegex, "")
        return withoutDiacritics.lowercase(Locale.ROOT)
    }

    /**
     * Cleans visible display strings (trims and collapses spaces without stripping accents or case).
     */
    fun sanitizeDisplayName(input: String?): String {
        if (input.isNullOrBlank()) return ""
        return input.trim().replace(multipleSpacesRegex, " ")
    }
}
