package com.example.feature.ocr

import com.example.feature.ocr.parser.ReceiptTextParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptTextParserTest {

    @Test
    fun parseExitoSupermarketReceipt_extractsCorrectTotalsAndTax() {
        val exitoText = """
            ÉXITO SUPERMERCADO S.A.
            NIT: 890.900.608-9
            FECHA: 24/09/2026 14:32
            
            LECHE DESLACTOSADA 1L   18.500
            PAN INTEGRAL ARTESANAL    9.200
            CAFE COLOMBIANO 500G     24.800
            ACEITE DE OLIVA 500ML    32.000
            
            SUBTOTAL: $ 84.500
            IVA 19%: $ 16.055
            TOTAL A PAGAR: $ 100.555
        """.trimIndent()

        val result = ReceiptTextParser.parse(exitoText)

        assertNotNull(result)
        assertTrue(result.merchantName.contains("ÉXITO", ignoreCase = true))
        assertEquals(100555.0, result.totalAmount, 0.01)
        assertEquals(84500.0, result.subtotal, 0.01)
        assertEquals(16055.0, result.taxAmount, 0.01)
        assertEquals("24/09/2026", result.dateString)
        assertEquals("Alimentación", result.suggestedCategory)
    }

    @Test
    fun parseD1DiscountStoreReceipt_extractsData() {
        val d1Text = """
            TIENDAS D1 S.A.S.
            NIT: 900.276.962-1
            FECHA: 18/08/2026
            
            HUEVOS AA X30     16.900
            ARROZ BLANCO 1KG   4.200
            ATUN EN AGUA       5.100
            
            SUBTOTAL: 26.200
            IVA 5%: 1.310
            TOTAL: $ 27.510
        """.trimIndent()

        val result = ReceiptTextParser.parse(d1Text)

        assertTrue(result.merchantName.contains("D1", ignoreCase = true))
        assertEquals(27510.0, result.totalAmount, 0.01)
        assertEquals(26200.0, result.subtotal, 0.01)
        assertEquals(1310.0, result.taxAmount, 0.01)
        assertEquals("18/08/2026", result.dateString)
    }

    @Test
    fun parseCarullaReceipt_identifiesMerchantAndTotal() {
        val carullaText = """
            CARULLA EXPRESS
            CALLE 85 # 11-53 BOGOTA
            FECHA: 12/07/2026
            
            QUESO PARMESANO 250G    28.900
            VINO TINTO RESERVA      65.000
            
            TOTAL A PAGAR: $ 93.900
        """.trimIndent()

        val result = ReceiptTextParser.parse(carullaText)

        assertTrue(result.merchantName.contains("CARULLA", ignoreCase = true))
        assertEquals(93900.0, result.totalAmount, 0.01)
        assertEquals("12/07/2026", result.dateString)
        assertEquals("Alimentación", result.suggestedCategory)
    }

    @Test
    fun parseTerpelGasStation_categorizesAsTransport() {
        val terpelText = """
            ESTACION DE SERVICIO TERPEL NORTE
            BOGOTA D.C.
            FECHA: 05/09/2026
            GASOLINA CORRIENTE 8.5 GL  135.000
            TOTAL A PAGAR: $ 135.000
        """.trimIndent()

        val result = ReceiptTextParser.parse(terpelText)

        assertTrue(result.merchantName.contains("TERPEL", ignoreCase = true))
        assertEquals(135000.0, result.totalAmount, 0.01)
        assertEquals("05/09/2026", result.dateString)
        assertEquals("Transporte", result.suggestedCategory)
    }

    @Test
    fun parseEmptyReceipt_handlesGracefullyWithoutException() {
        val emptyResult = ReceiptTextParser.parse("")
        assertEquals(0.0, emptyResult.totalAmount, 0.01)
        assertEquals("", emptyResult.dateString)
    }
}
