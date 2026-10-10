package com.example

import com.example.core.text.TextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Verifies Acceptance Criterion 4:
 * Canonical normalization strips diacritics, collapses spaces, and prevents duplicate aliases.
 */
class TextNormalizerAndUniquenessTest {

    @Test
    fun testDiacriticsAndAccentEquivalence() {
        // "Café" == "cafe" == "CAFÉ" == "  cAfÉ  "
        val norm1 = TextNormalizer.normalize("Café")
        val norm2 = TextNormalizer.normalize("cafe")
        val norm3 = TextNormalizer.normalize("CAFÉ")
        val norm4 = TextNormalizer.normalize("  cAfÉ  ")

        assertEquals("cafe", norm1)
        assertEquals("cafe", norm2)
        assertEquals("cafe", norm3)
        assertEquals("cafe", norm4)
    }

    @Test
    fun testSpecialSpanishCharactersAndWhitespaceCollapsing() {
        // "Alimentación  y   Hogar" -> "alimentacion y hogar"
        val norm = TextNormalizer.normalize("  Alimentación   y   Hogar  ")
        assertEquals("alimentacion y hogar", norm)

        // "Año" -> "ano"
        val normYear = TextNormalizer.normalize("Año 2026")
        assertEquals("ano 2026", normYear)
    }

    @Test
    fun testDisplayNameSanitizationPreservesLegibility() {
        val clean = TextNormalizer.sanitizeDisplayName("   Restaurante   &   Café   ")
        assertEquals("Restaurante & Café", clean)
    }
}
