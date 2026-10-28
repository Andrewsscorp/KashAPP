package com.example.core.security

/**
 * Redactor determinista de Información Personal Identificable (PII) adaptado al sistema
 * financiero colombiano (Ley 1581 de 2012) y trazas de diagnóstico (SIEM / Crash Logs).
 */
object ColombianPiiRedactor {

    // 1. Tokens JWT y OAuth Bearer
    private val JWT_OR_OAUTH_REGEX = Regex(
        "\\b(eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}|ya29\\.[A-Za-z0-9_-]{20,})\\b"
    )

    // 2. Correos electrónicos
    private val EMAIL_REGEX = Regex(
        "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"
    )

    // 3. Documentos de identidad colombianos (CC, CE, NIT, TI, PEP)
    private val COLOMBIAN_ID_REGEX = Regex(
        "(?i)\\b(CC|C\\.C\\.|CE|C\\.E\\.|NIT|TI|DOC|DOCUMENTO|CEDULA|CÉDULA)\\s*[:#.]?\\s*([0-9]{1,3}(?:[.,][0-9]{3}){1,3}(?:-[0-9])?|[0-9]{6,12}(?:-[0-9])?)\\b"
    )

    // 4. Números de celular colombianos (10 dígitos iniciando en 3, con o sin +57)
    private val COLOMBIAN_PHONE_REGEX = Regex(
        "(?<!\\$)(?:\\+57[\\s-]?)?\\b(3\\d{2})[\\s-]?(\\d{3})[\\s-]?(\\d{2})(\\d{2})\\b"
    )

    // 5. Números de tarjeta PAN completos (13 a 19 dígitos con o sin espacios/guiones)
    private val CARD_PAN_REGEX = Regex(
        "\\b(?:\\d{4}[\\s-]?){3}\\d{1,7}\\b"
    )

    // 6. Cuentas bancarias mencionadas explícitamente ("cta 00000012345", "cuenta 245-889102-45")
    private val BANK_ACCOUNT_CONTEXT_REGEX = Regex(
        "(?i)\\b(cta|cuenta|ahorros|corriente|producto)\\s*(?:no\\.?|#|nro\\.?)?\\s*(\\*+|\\.{2,})?([0-9][0-9-]{4,18}[0-9])\\b"
    )

    // 7. Códigos CUS de PSE, aprobaciones y referencias alfanuméricas largas
    private val REFERENCE_CONTEXT_REGEX = Regex(
        "(?i)\\b(CUS|aprobaci[oó]n|aprob|referencia|ref|comprobante)\\s*[:#.]?\\s*([A-Za-z0-9-]{5,24})\\b"
    )

    // 8. Tarjetas con prefijo y asterisco o puntos ("T.Deb *4821", "tarjeta ...9104", "tc *1234")
    private val CARD_SUFFIX_CONTEXT_REGEX = Regex(
        "(?i)\\b(T\\.Deb|T\\.Cred|tarjeta|tc|td|debito|débito|credito|crédito)\\s*[:#.]?\\s*(\\*+|\\.{2,})?([0-9]{4})\\b"
    )

    /**
     * Redacta PII de un mensaje bancario, notificación o log preservando montos ($),
     * fechas y los últimos 4 dígitos de cuentas/tarjetas para conciliación contable.
     */
    fun redactFinancialText(rawInput: String?): String {
        if (rawInput.isNullOrBlank()) return ""

        return rawInput
            .replace(JWT_OR_OAUTH_REGEX, "[TOKEN-REDACTED]")
            .replace(EMAIL_REGEX, "[EMAIL-PROTEGIDO]")
            .replace(COLOMBIAN_ID_REGEX) { match ->
                val docType = match.groupValues[1].uppercase().replace(".", "")
                "$docType [DOC-PROTEGIDO]"
            }
            .replace(CARD_PAN_REGEX) { match ->
                val digitsOnly = match.value.filter { it.isDigit() }
                "****${digitsOnly.takeLast(4)}"
            }
            .replace(CARD_SUFFIX_CONTEXT_REGEX) { match ->
                val last4 = match.groupValues[3]
                "****$last4"
            }
            .replace(BANK_ACCOUNT_CONTEXT_REGEX) { match ->
                val label = match.groupValues[1]
                val rawNumber = match.groupValues[3].filter { it.isDigit() }
                val last4 = rawNumber.takeLast(4)
                "$label ****$last4"
            }
            .replace(COLOMBIAN_PHONE_REGEX) { match ->
                val prefix = match.groupValues[1]
                val end = match.groupValues[4]
                "${prefix}*****$end"
            }
            .replace(REFERENCE_CONTEXT_REGEX) { match ->
                val label = match.groupValues[1]
                "$label [REF-PROTEGIDA]"
            }
    }

    /**
     * Formatea de forma segura un sufijo de cuenta o tarjeta dejando únicamente los últimos 2 o 4 dígitos.
     */
    fun maskAccountOrCardSuffix(rawNumberOrSuffix: String?, visibleDigits: Int = 4): String {
        if (rawNumberOrSuffix.isNullOrBlank()) return "****"
        val digits = rawNumberOrSuffix.filter { it.isDigit() }
        if (digits.isEmpty()) return "****"
        val keep = digits.takeLast(visibleDigits.coerceIn(2, 4))
        return "****$keep"
    }
}
