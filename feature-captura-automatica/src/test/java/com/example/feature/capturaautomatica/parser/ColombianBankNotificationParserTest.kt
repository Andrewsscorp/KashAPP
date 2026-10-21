package com.example.feature.capturaautomatica.parser

import com.example.feature.capturaautomatica.domain.model.SupportedColombianBank
import com.example.feature.capturaautomatica.domain.parser.ColombianBankNotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ColombianBankNotificationParserTest {

    private lateinit var parser: ColombianBankNotificationParser

    @Before
    fun setUp() {
        parser = ColombianBankNotificationParser()
    }

    @Test
    fun testAc01_bancolombiaPurchaseWithCentsAndCardDigits() {
        // TEST-AC-01
        val pkg = "com.todo1.mobile"
        val title = "Bancolombia"
        val text = "Bancolombia le informa Compra por $125.400,00 en EXITO WOW COUNTRY 18:30 19/10/2026 T.Deb *4821"

        val parsed = parser.parse(pkg, title, text)
        assertNotNull(parsed)
        assertEquals("EXPENSE", parsed!!.transactionType)
        assertEquals(125400.0, parsed.amount, 0.001)
        assertEquals("EXITO WOW COUNTRY", parsed.merchantOrCounterparty)
        assertEquals("4821", parsed.lastFourDigits)
        assertEquals(SupportedColombianBank.BANCOLOMBIA, parsed.detectedBank)
    }

    @Test
    fun testAc02_nequiPaymentAtMerchant() {
        // TEST-AC-02
        val pkg = "com.nequi.MobileApp"
        val title = "Nequi"
        val text = "Pagaste $18.500,00 en JUAN VALDEZ CAFE con tu Nequi."

        val parsed = parser.parse(pkg, title, text)
        assertNotNull(parsed)
        assertEquals("EXPENSE", parsed!!.transactionType)
        assertEquals(18500.0, parsed.amount, 0.001)
        assertEquals("JUAN VALDEZ CAFE", parsed.merchantOrCounterparty)
        assertNull(parsed.lastFourDigits)
        assertEquals(SupportedColombianBank.NEQUI, parsed.detectedBank)
    }

    @Test
    fun testAc03_nequiReceivedMoney() {
        // TEST-AC-03
        val pkg = "com.nequi.MobileApp"
        val title = "Nequi"
        val text = "Recibiste $250.000,00 de ANDRES GOMEZ en tu Nequi"

        val parsed = parser.parse(pkg, title, text)
        assertNotNull(parsed)
        assertEquals("INCOME", parsed!!.transactionType)
        assertEquals(250000.0, parsed.amount, 0.001)
        assertEquals("ANDRES GOMEZ", parsed.merchantOrCounterparty)
        assertEquals(SupportedColombianBank.NEQUI, parsed.detectedBank)
    }

    @Test
    fun testAc04_nuColombiaCardPurchaseApproved() {
        // TEST-AC-04
        val pkg = "com.nu.production"
        val title = "Nu"
        val text = "Compra aprobada por $ 42.000,00 en DIDI RIDES con tu tarjeta ...9104"

        val parsed = parser.parse(pkg, title, text)
        assertNotNull(parsed)
        assertEquals("EXPENSE", parsed!!.transactionType)
        assertEquals(42000.0, parsed.amount, 0.001)
        assertEquals("DIDI RIDES", parsed.merchantOrCounterparty)
        assertEquals("9104", parsed.lastFourDigits)
        assertEquals(SupportedColombianBank.NU_COLOMBIA, parsed.detectedBank)
    }

    @Test
    fun testAc05_strictOtpBlockReturnsNull() {
        // TEST-AC-05: El parser debe retornar null y no procesar mensajes OTP
        val pkg = "com.todo1.mobile"
        val title = "Bancolombia"
        val text = "Bancolombia: Tu clave dinamica es 839201. No la compartas con nadie."

        val parsed = parser.parse(pkg, title, text)
        assertNull("Mensaje con clave dinámica debe ser descartado", parsed)
    }

    @Test
    fun testAc06_idempotencyKeyDeduplicationWithinTwoMinutes() {
        // TEST-AC-06: Misma notificación 90 segundos después genera la misma idempotencyKey
        val pkg = "com.todo1.mobile"
        val title = "Bancolombia"
        val text = "Bancolombia le informa Compra por $48.500,00 en D1 TUNJA CENTRO T.Deb *4821"

        val baseTime = 1776510000000L // Minuto alineado
        val p1 = parser.parse(pkg, title, text, postTime = baseTime)
        val p2 = parser.parse(pkg, title, text, postTime = baseTime + 90_000L) // 90 segundos después

        assertNotNull(p1)
        assertNotNull(p2)
        assertEquals("La idempotencyKey debe ser idéntica dentro de la ventana de 2 minutos", p1!!.idempotencyKey, p2!!.idempotencyKey)
    }

    @Test
    fun testDaviPlataAndDaviviendaAndBbvaAndLulo() {
        // DaviPlata compra
        val dp = parser.parse("com.davivienda.daviplataapp", "DaviPlata", "Compra por $45.000 en ALMACENES EXITO")
        assertNotNull(dp)
        assertEquals("EXPENSE", dp!!.transactionType)
        assertEquals(45000.0, dp.amount, 0.001)
        assertEquals("ALMACENES EXITO", dp.merchantOrCounterparty)

        // Davivienda compra
        val dv = parser.parse("com.davivienda.daviviendaapp", "Davivienda", "Davivienda informa compra por $68.000 en RESTAURANTE con tarjeta terminada en 1234")
        assertNotNull(dv)
        assertEquals("EXPENSE", dv!!.transactionType)
        assertEquals(68000.0, dv.amount, 0.001)
        assertEquals("1234", dv.lastFourDigits)

        // BBVA compra
        val bbva = parser.parse("com.bbva.bbvacolombia", "BBVA", "BBVA: Compra autorizada por $55.000 en HOME CENTER con tarjeta *5678")
        assertNotNull(bbva)
        assertEquals("EXPENSE", bbva!!.transactionType)
        assertEquals(55000.0, bbva.amount, 0.001)
        assertEquals("5678", bbva.lastFourDigits)

        // Lulo Bank compra
        val lulo = parser.parse("com.lulobank.app", "Lulo", "Compra con tu Lulo Card por $32.000 en STARBUCKS")
        assertNotNull(lulo)
        assertEquals("EXPENSE", lulo!!.transactionType)
        assertEquals(32000.0, lulo.amount, 0.001)
        assertEquals("STARBUCKS", lulo.merchantOrCounterparty)
    }

    @Test
    fun testAc07_nequiBreBNotificationExactUserCase() {
        // Caso exacto reportado por el usuario: $5 por Bre-B en Nequi
        val pkg = "com.nequi.MobileApp"
        val title = "Te enviaron plata por Bre-B"
        val text = "Te enviaron $5. Entra a tu app y revisa tu saldo."

        val parsed = parser.parse(pkg, title, text)
        assertNotNull("La notificación de Bre-B de Nequi debe ser capturada", parsed)
        assertEquals("INCOME", parsed!!.transactionType)
        assertEquals(5.0, parsed.amount, 0.001)
        assertEquals("Transferencia Bre-B", parsed.merchantOrCounterparty)
        assertEquals(SupportedColombianBank.NEQUI, parsed.detectedBank)
    }

    @Test
    fun testAc08_nequiTeEnviaronWithSender() {
        val pkg = "com.nequi.MobileApp"
        val title = "Nequi"
        val text = "Te enviaron $50.000 de CARLOS PEREZ. Entra a tu app"

        val parsed = parser.parse(pkg, title, text)
        assertNotNull(parsed)
        assertEquals("INCOME", parsed!!.transactionType)
        assertEquals(50000.0, parsed.amount, 0.001)
        assertEquals("CARLOS PEREZ", parsed.merchantOrCounterparty)
    }

    @Test
    fun testAc09_bancolombiaBreBNotification() {
        val pkg = "com.todo1.mobile"
        val title = "Bancolombia"
        val text = "Bancolombia le informa: Te enviaron $20.000 por Bre-B"

        val parsed = parser.parse(pkg, title, text)
        assertNotNull(parsed)
        assertEquals("INCOME", parsed!!.transactionType)
        assertEquals(20000.0, parsed.amount, 0.001)
        assertEquals("Transferencia Bre-B Bancolombia", parsed.merchantOrCounterparty)
    }

    @Test
    fun testAc10_nuAndDaviPlataBreB() {
        // Nu Bre-B
        val nu = parser.parse("com.nu.production", "Nu", "Te enviaron $15.000 por Bre-B")
        assertNotNull(nu)
        assertEquals("INCOME", nu!!.transactionType)
        assertEquals(15000.0, nu.amount, 0.001)
        assertEquals("Transferencia Bre-B Cuenta Nu", nu.merchantOrCounterparty)

        // DaviPlata Bre-B
        val dp = parser.parse("com.davivienda.daviplataapp", "DaviPlata", "Te enviaron $30.000 por Bre-B")
        assertNotNull(dp)
        assertEquals("INCOME", dp!!.transactionType)
        assertEquals(30000.0, dp.amount, 0.001)
        assertEquals("Transferencia Bre-B DaviPlata", dp.merchantOrCounterparty)
    }
}

