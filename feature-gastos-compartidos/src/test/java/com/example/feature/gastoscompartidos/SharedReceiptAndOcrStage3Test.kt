package com.example.feature.gastoscompartidos

import com.example.domain.models.shared.RealMoneyImpactMode
import com.example.domain.models.shared.SplitCalculationMethod
import com.example.feature.gastoscompartidos.domain.ocr.SharedOcrReceiptBridge
import com.example.feature.gastoscompartidos.domain.ticket.SharedReceiptTicketEngine
import com.example.feature.ocr.model.ReceiptItem
import com.example.feature.ocr.model.ReceiptScanResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedReceiptAndOcrStage3Test {

    @Test
    fun `SharedOcrReceiptBridge extracts NIT, DIAN invoice number, Impoconsumo 8pct and Tip 10pct`() {
        val rawPosReceipt = """
            CREPES & WAFFLES S.A.
            NIT: 860.076.915-2
            Factura Electronica de Venta: SETT-49182
            Fecha: 15/10/2026
            2x Crepe Lomo Arabe 76.000
            1x Wafle Arequipe 24.000
            Subtotal: 100.000
            Impoconsumo 8%: 8.000
            Propina Voluntaria: 10.000
            TOTAL A PAGAR: $118.000
        """.trimIndent()

        val scanResult = ReceiptScanResult(
            merchantName = "CREPES & WAFFLES S.A.",
            totalAmount = 118000.0,
            subtotal = 100000.0,
            taxAmount = 8000.0,
            dateString = "15/10/2026",
            items = listOf(
                ReceiptItem("Crepe Lomo Arabe", quantity = 2.0, unitPrice = 38000.0, totalPrice = 76000.0),
                ReceiptItem("Wafle Arequipe", quantity = 1.0, unitPrice = 24000.0, totalPrice = 24000.0)
            ),
            rawText = rawPosReceipt
        )

        val draft = SharedOcrReceiptBridge.buildDraftFromOcrScan(
            scanResult = scanResult,
            expandMultiQuantityItemsIntoSingleUnits = true,
            defaultParticipantIdsForAllItems = listOf("SELF_USER")
        )

        assertEquals("860.076.915-2", draft.merchantNit)
        assertEquals("SETT-49182", draft.detectedExternalInvoiceNumber)
        assertEquals(100000.0, draft.subtotalAmount, 0.01)
        assertEquals(8000.0, draft.taxConsumptionAmount, 0.01)
        assertEquals(10000.0, draft.tipAmount, 0.01)
        assertEquals(118000.0, draft.totalAmount, 0.01)
        // 2x Crepe expandido en 2 unidades individuales + 1x Wafle = 3 líneas interactivas
        assertEquals(3, draft.lineItems.size)
        assertTrue(draft.sumOfLineItemsMatchesSubtotal)
    }

    @Test
    fun `SharedReceiptTicketEngine masks private user bank balance when maskPrivateBalancesInExport is true`() {
        val ticket = SharedReceiptTicketEngine.buildExpenseBreakdownTicket(
            expenseConsecutive = "GC-2026-0042",
            ticketConsecutive = "TK-2026-0042",
            externalInvoiceNumber = "SETT-49182",
            groupName = "Cena Amigos",
            groupCode = "CENA",
            title = "Cena Crepes & Waffles",
            merchantName = "Crepes & Waffles",
            merchantNit = "860.076.915-2",
            categoryName = "Alimentación",
            dateFormatted = "2026-10-15",
            timeFormatted = "20:15:00",
            currency = "COP",
            appliedExchangeRate = 1.0,
            splitMethod = SplitCalculationMethod.ITEMIZED_OCR,
            realMoneyMode = RealMoneyImpactMode.ACCRUAL_RECEIVABLE,
            subtotalAmount = 100000.0,
            taxIvaAmount = 0.0,
            taxConsumptionAmount = 8000.0,
            tipAmount = 10000.0,
            discountAmount = 0.0,
            gmfAndFinancialAmount = 0.0,
            totalAmount = 118000.0,
            itemizedRows = emptyList(),
            participantRows = listOf(
                SharedReceiptTicketEngine.TicketParticipantRow(
                    participantId = "SELF_USER",
                    displayName = "Andrés",
                    avatarInitials = "AN",
                    paidInRegisterFormatted = "$ 118.000 COP",
                    subtotalConsumedFormatted = "$ 50.000 COP",
                    taxesAndTipFormatted = "$ 9.000 COP",
                    totalOwedShareFormatted = "$ 59.000 COP",
                    netBalanceFormatted = "+ $ 59.000 COP",
                    remainingPendingFormatted = "$ 0 COP",
                    statusBadgeText = "PAGADOR",
                    statusColorHex = "#10B981",
                    isCreditor = true,
                    isSettled = true
                )
            ),
            collectorName = "Andrés Felipe",
            collectorBankName = "Nequi / Llave Bre-B",
            collectorAccountOrKey = "@andres.kash",
            collectorCustomQrUri = null,
            sourceAccountName = "Ahorros Bancolombia 9981",
            sourceAccountBalanceAfter = 14820000.0,
            recognizedPnlExpense = 59000.0,
            receivableOrPayableDelta = 59000.0,
            maskPrivateBalancesInExport = true
        )

        assertEquals("TK-2026-0042", ticket.primaryTicketConsecutive)
        assertNotNull(ticket.collectionInstructions)
        assertTrue(ticket.collectionInstructions!!.qrPayloadString.contains("tk=TK-2026-0042"))
        // El saldo privado de $14.820.000 queda estrictamente oculto (null) en el ticket exportado
        assertNull(ticket.privateOwnerFooter?.privateAccountBalanceAfterFormatted)
        assertFalse(ticket.privateOwnerFooter!!.isVisibleInExportedTicket)
        assertEquals(16, ticket.sha256IntegrityHash.length)
    }

    @Test
    fun `SharedReceiptTicketEngine builds Payment Voucher PG ticket and calculates amortization progress`() {
        val voucher = SharedReceiptTicketEngine.buildPaymentVoucherTicket(
            voucherConsecutive = "PG-202610-0008",
            relatedExpenseConsecutive = "GC-2026-0042",
            expenseTitle = "Cena Crepes & Waffles",
            groupName = "Cena Amigos",
            fromDebtorName = "Carlos Pérez",
            toCreditorName = "Andrés",
            settlementMethodLabel = "Nequi",
            previousPendingAmount = 100000.0,
            amountPaidNow = 60000.0,
            newRemainingAmount = 40000.0,
            currency = "COP",
            paymentDate = "2026-10-15",
            paymentTime = "21:30:00",
            externalBankReference = "M9182736"
        )

        assertEquals("PG-202610-0008", voucher.primaryTicketConsecutive)
        assertEquals("ABONO PARCIAL", voucher.splitMethodLabel)
        assertEquals(60.0f, voucher.debtAmortizationProgressPct, 0.01f)
        assertEquals(16, voucher.sha256IntegrityHash.length)
    }
}
