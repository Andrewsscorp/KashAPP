package com.example.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de pruebas unitarias para ColombianPiiRedactor (Ley 1581 de 2012 / OWASP MASVS-PRIVACY).
 * Valida la sanitización determinista de cuentas, tarjetas, celulares, documentos y tokens
 * asegurando la preservación intacta de montos monetarios, fechas y horas.
 */
class ColombianPiiRedactorTest {

    @Test
    fun testBancolombiaAccountAndCardRedactionPreservesLast4() {
        val input = "Bancolombia le informa Compra por $125.400,00 en EXITO 18:30 19/10/2026 T.Deb *4821 desde cta 00000012345"
        val redacted = ColombianPiiRedactor.redactFinancialText(input)

        // Verificaciones
        assertTrue("Debe preservar el monto monetario con $", redacted.contains("$125.400,00"))
        assertTrue("Debe preservar la hora", redacted.contains("18:30"))
        assertTrue("Debe preservar la fecha", redacted.contains("19/10/2026"))
        assertTrue("Debe preservar los ultimos 4 digitos de tarjeta", redacted.contains("****4821"))
        assertTrue("Debe enmascarar la cuenta preservando los ultimos 4 digitos", redacted.contains("cta ****2345"))
        assertFalse("No debe contener el numero de cuenta completo", redacted.contains("00000012345"))
    }

    @Test
    fun testNequiAndDaviPlataPhoneRedactionDoesNotTouchAmounts() {
        val input = "Pasaste plata por $30.000 a 3001234567 desde tu disponible"
        val redacted = ColombianPiiRedactor.redactFinancialText(input)

        assertTrue("Debe preservar el monto monetario", redacted.contains("$30.000"))
        assertTrue("Debe enmascarar el celular colombiano en formato 300*****67", redacted.contains("300*****67"))
        assertFalse("No debe contener el celular en texto claro", redacted.contains("3001234567"))
    }

    @Test
    fun testColombianCedulaAndNitRedaction() {
        val input = "Transferencia a favor de CC 1032456789 y NIT 900.123.456-7 por $500.000"
        val redacted = ColombianPiiRedactor.redactFinancialText(input)

        assertTrue("Debe enmascarar la cedula", redacted.contains("CC [DOC-PROTEGIDO]"))
        assertTrue("Debe enmascarar el NIT", redacted.contains("NIT [DOC-PROTEGIDO]"))
        assertTrue("Debe preservar el monto", redacted.contains("$500.000"))
        assertFalse("No debe contener el numero de cedula", redacted.contains("1032456789"))
        assertFalse("No debe contener el NIT en claro", redacted.contains("900.123.456-7"))
    }

    @Test
    fun testJwtAndEmailRedactionInStackTraces() {
        val input = "Usuario andres.suarez@empresa.com.co fallo con bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U"
        val redacted = ColombianPiiRedactor.redactFinancialText(input)

        assertTrue("Debe enmascarar el correo electronico", redacted.contains("[EMAIL-PROTEGIDO]"))
        assertTrue("Debe enmascarar el token JWT", redacted.contains("[TOKEN-REDACTED]"))
        assertFalse("No debe contener el email en claro", redacted.contains("andres.suarez@empresa.com.co"))
        assertFalse("No debe contener el JWT en claro", redacted.contains("eyJhbGciOiJIUzI1NiIs"))
    }

    @Test
    fun testDatesAndCopCurrencyAmountsRemainIntact() {
        val input = "Pago PSE Aprob: 849201 CUS 981273645 por COP 450.000 el 2026-10-26"
        val redacted = ColombianPiiRedactor.redactFinancialText(input)

        assertTrue("Debe preservar el monto COP", redacted.contains("COP 450.000"))
        assertTrue("Debe preservar la fecha ISO", redacted.contains("2026-10-26"))
        assertTrue("Debe redactar la aprobacion", redacted.contains("Aprob [REF-PROTEGIDA]"))
        assertTrue("Debe redactar el CUS", redacted.contains("CUS [REF-PROTEGIDA]"))
    }

    @Test
    fun testMaskAccountOrCardSuffix() {
        assertEquals("****4821", ColombianPiiRedactor.maskAccountOrCardSuffix("12345674821", 4))
        assertEquals("****21", ColombianPiiRedactor.maskAccountOrCardSuffix("4821", 2))
        assertEquals("****", ColombianPiiRedactor.maskAccountOrCardSuffix(null))
        assertEquals("****", ColombianPiiRedactor.maskAccountOrCardSuffix(""))
    }
}
