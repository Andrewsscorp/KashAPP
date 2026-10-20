package com.example.feature.capturaautomatica.domain.parser

object OtpSafetyFilter {
    private val BLOCKED_AUTH_PATTERNS = listOf(
        Regex("""(?iu)\b(clave\s+din[aáAÁ]mica|c[oóOÓ]digo\s+de\s+(seguridad|verificaci[oóOÓ]n|acceso)|otp|token(?:\s+de\s+seguridad)?)\b"""),
        Regex("""(?iu)\b(no\s+compartas\s+(?:este\s+)?c[oóOÓ]digo|no\s+la\s+compartas|c[oóOÓ]digo\s+para\s+autorizar|inicio\s+de\s+sesi[oóOÓ]n)\b"""),
        Regex("""(?iu)\b(tu\s+c[oóOÓ]digo\s+(?:es|de)|tu\s+clave\s+(?:es|din[aáAÁ]mica)|c[oóOÓ]digo\s+de\s+activaci[oóOÓ]n)\b""")
    )

    fun isSensitiveAuthMessage(title: String, body: String): Boolean {
        val combined = "$title $body"
        return isSensitiveAuthMessage(combined)
    }

    fun isSensitiveAuthMessage(text: String): Boolean {
        return BLOCKED_AUTH_PATTERNS.any { it.containsMatchIn(text) }
    }
}
