package com.example.feature.gastoscompartidos.domain.ticket

import com.example.domain.engine.ConsecutiveSequenceEngine
import com.example.domain.models.shared.RealMoneyImpactMode
import com.example.domain.models.shared.SplitCalculationMethod
import com.example.feature.gastoscompartidos.domain.engine.DebtSimplificationGraphEngine
import java.util.Locale
import kotlin.math.abs

/**
 * Motor generador de los 3 tipos de Tickets Oficiales de KashApp:
 * 1. Ticket de Cobro y Desglose de Gasto Compartido (TK-xxxx)
 * 2. Comprobante de Abono / Liquidación Individual (PG-xxxx)
 * 3. Acta de Liquidación Grupal y Paz y Salvo Contable (LQ-xxxx)
 */
object SharedReceiptTicketEngine {

    enum class TicketKind(val badgeTitle: String, val primaryColorHex: String) {
        EXPENSE_BREAKDOWN_TICKET("COMPROBANTE OFICIAL DE GASTO COMPARTIDO", "#0284C7"),
        PAYMENT_VOUCHER_TICKET("RECIBO OFICIAL DE ABONO / LIQUIDACIÓN", "#10B981"),
        GROUP_SETTLEMENT_ACT("ACTA DE LIQUIDACIÓN Y PAZ Y SALVO CONTABLE", "#0F172A")
    }

    data class TicketLineItemRow(
        val quantityText: String,
        val description: String,
        val consumersInitials: List<String>,
        val formattedLineTotal: String
    )

    data class TicketParticipantRow(
        val participantId: String,
        val displayName: String,
        val avatarInitials: String,
        val paidInRegisterFormatted: String,
        val subtotalConsumedFormatted: String,
        val taxesAndTipFormatted: String,
        val totalOwedShareFormatted: String,
        val netBalanceFormatted: String,
        val remainingPendingFormatted: String,
        val statusBadgeText: String,
        val statusColorHex: String,
        val isCreditor: Boolean,
        val isSettled: Boolean
    )

    data class CollectionPaymentInstructions(
        val collectorDisplayName: String,
        val preferredMethodLabel: String, // Ej. "Nequi / Llave Bre-B / Ahorros Bancolombia"
        val accountOrKeyDisplay: String,  // Ej. "Llave Bre-B: @andres.kash • Nequi: 300 123 4567"
        val qrPayloadString: String,      // Payload estructurado para dibujar el código QR en el ticket
        val customQrImageLocalUri: String? = null
    )

    data class PrivateOwnerFinancialFooter(
        val isVisibleInExportedTicket: Boolean,
        val realMoneyModeBadge: String,
        val sourceAccountNameMasked: String,
        val privateAccountBalanceAfterFormatted: String?, // null cuando maskUserAccountBalancesOnTicket == true
        val recognizedPnlExpenseFormatted: String,
        val receivableOrPayableFormatted: String
    )

    /**
     * Modelo maestro unificado que representa cualquiera de los 3 tipos de Tickets de Recibo.
     */
    data class SharedDigitalTicketModel(
        val kind: TicketKind,
        val primaryTicketConsecutive: String,   // "TK-2026-0042" | "PG-202610-0008" | "LQ-2026-0003"
        val relatedExpenseConsecutive: String?, // "GC-2026-0042"
        val externalInvoiceNumber: String?,     // "FE-98412"
        val groupName: String?,                 // "Apartamento Chapinero"
        val groupCode: String?,                 // "APTO"
        val headlineTitle: String,              // "Cena Crepes & Waffles" o "Abono de Ana María"
        val merchantName: String?,
        val merchantNit: String?,
        val categoryName: String,
        val dateTimeFormatted: String,          // "15 Oct 2026 • 09:45 AM"
        val currencyCode: String,
        val exchangeRateNote: String?,          // "TRM Oficial aplicada: $4.180,00 COP/USD"
        val splitMethodLabel: String,
        // Totales numéricos formateados
        val subtotalFormatted: String,
        val ivaFormatted: String?,
        val impoconsumoFormatted: String?,
        val tipFormatted: String?,
        val discountFormatted: String?,
        val gmfOrFinancialFormatted: String?,
        val grandTotalFormatted: String,
        val grandTotalRaw: Double,
        // Tablas de detalle
        val itemizedRows: List<TicketLineItemRow> = emptyList(),
        val participantRows: List<TicketParticipantRow> = emptyList(),
        // Campos específicos para Ticket de Abono PG-xxxx
        val settlementFromName: String? = null,
        val settlementToName: String? = null,
        val settlementMethodLabel: String? = null,
        val previousDebtBalanceFormatted: String? = null,
        val amountPaidNowFormatted: String? = null,
        val remainingDebtBalanceFormatted: String? = null,
        val debtAmortizationProgressPct: Float = 0f,
        val externalBankReference: String? = null,
        // Campos específicos para Acta de Liquidación de Grupo LQ-xxxx
        val simplifiedTransfers: List<DebtSimplificationGraphEngine.SimplifiedTransferInstruction> = emptyList(),
        val transactionsSavedByGraphCount: Int = 0,
        // Recaudo, Privacidad y Seguridad Criptográfica
        val collectionInstructions: CollectionPaymentInstructions?,
        val privateOwnerFooter: PrivateOwnerFinancialFooter?,
        val attachedOriginalReceiptUri: String? = null,
        val sha256IntegrityHash: String,
        val verificationFooterText: String
    )

    /**
     * 1. Genera el Ticket Oficial de Cobro y Desglose de Gasto Compartido (TK-xxxx).
     */
    fun buildExpenseBreakdownTicket(
        expenseConsecutive: String,
        ticketConsecutive: String,
        externalInvoiceNumber: String?,
        groupName: String?,
        groupCode: String?,
        title: String,
        merchantName: String?,
        merchantNit: String?,
        categoryName: String,
        dateFormatted: String,
        timeFormatted: String,
        currency: String,
        appliedExchangeRate: Double,
        splitMethod: SplitCalculationMethod,
        realMoneyMode: RealMoneyImpactMode,
        subtotalAmount: Double,
        taxIvaAmount: Double,
        taxConsumptionAmount: Double,
        tipAmount: Double,
        discountAmount: Double,
        gmfAndFinancialAmount: Double,
        totalAmount: Double,
        itemizedRows: List<TicketLineItemRow>,
        participantRows: List<TicketParticipantRow>,
        collectorName: String,
        collectorBankName: String,
        collectorAccountOrKey: String,
        collectorCustomQrUri: String?,
        sourceAccountName: String?,
        sourceAccountBalanceAfter: Double?,
        recognizedPnlExpense: Double,
        receivableOrPayableDelta: Double,
        maskPrivateBalancesInExport: Boolean = true,
        attachedReceiptUri: String? = null
    ): SharedDigitalTicketModel {
        val sharesSig = participantRows.joinToString(";") { "${it.participantId}:${it.totalOwedShareFormatted}" }
        val hash = ConsecutiveSequenceEngine.computeTicketIntegrityHash(
            consecutiveCode = expenseConsecutive,
            receiptTicketCode = ticketConsecutive,
            totalAmount = totalAmount,
            currency = currency,
            expenseDate = dateFormatted,
            participantSharesSignature = sharesSig
        )

        val qrPayload = buildCollectionQrPayload(
            ticketCode = ticketConsecutive,
            expenseCode = expenseConsecutive,
            collectorName = collectorName,
            accountOrKey = collectorAccountOrKey,
            totalAmount = totalAmount,
            currency = currency
        )

        val maskedAccountLabel = sourceAccountName?.let { name ->
            if (maskPrivateBalancesInExport && name.length > 4) {
                "${name.take(12)} ••••"
            } else name
        } ?: "Cuenta KashApp"

        return SharedDigitalTicketModel(
            kind = TicketKind.EXPENSE_BREAKDOWN_TICKET,
            primaryTicketConsecutive = ticketConsecutive,
            relatedExpenseConsecutive = expenseConsecutive,
            externalInvoiceNumber = externalInvoiceNumber,
            groupName = groupName,
            groupCode = groupCode,
            headlineTitle = title,
            merchantName = merchantName,
            merchantNit = merchantNit,
            categoryName = categoryName,
            dateTimeFormatted = "$dateFormatted • $timeFormatted",
            currencyCode = currency,
            exchangeRateNote = if (appliedExchangeRate != 1.0 && appliedExchangeRate > 0.0) {
                "TRM Aplicada: ${formatMoney(appliedExchangeRate, "COP")}"
            } else null,
            splitMethodLabel = splitMethod.shortLabel,
            subtotalFormatted = formatMoney(subtotalAmount, currency),
            ivaFormatted = if (taxIvaAmount > 0.0) formatMoney(taxIvaAmount, currency) else null,
            impoconsumoFormatted = if (taxConsumptionAmount > 0.0) formatMoney(taxConsumptionAmount, currency) else null,
            tipFormatted = if (tipAmount > 0.0) formatMoney(tipAmount, currency) else null,
            discountFormatted = if (discountAmount > 0.0) "- ${formatMoney(discountAmount, currency)}" else null,
            gmfOrFinancialFormatted = if (gmfAndFinancialAmount > 0.0) formatMoney(gmfAndFinancialAmount, currency) else null,
            grandTotalFormatted = formatMoney(totalAmount, currency),
            grandTotalRaw = totalAmount,
            itemizedRows = itemizedRows,
            participantRows = participantRows,
            collectionInstructions = if (collectorAccountOrKey.isNotBlank()) {
                CollectionPaymentInstructions(
                    collectorDisplayName = collectorName,
                    preferredMethodLabel = collectorBankName,
                    accountOrKeyDisplay = collectorAccountOrKey,
                    qrPayloadString = qrPayload,
                    customQrImageLocalUri = collectorCustomQrUri
                )
            } else null,
            privateOwnerFooter = PrivateOwnerFinancialFooter(
                isVisibleInExportedTicket = !maskPrivateBalancesInExport,
                realMoneyModeBadge = realMoneyMode.shortBadge,
                sourceAccountNameMasked = maskedAccountLabel,
                privateAccountBalanceAfterFormatted = if (maskPrivateBalancesInExport || sourceAccountBalanceAfter == null) {
                    null
                } else {
                    formatMoney(sourceAccountBalanceAfter, currency)
                },
                recognizedPnlExpenseFormatted = formatMoney(recognizedPnlExpense, currency),
                receivableOrPayableFormatted = formatSignedMoney(receivableOrPayableDelta, currency)
            ),
            attachedOriginalReceiptUri = attachedReceiptUri,
            sha256IntegrityHash = hash,
            verificationFooterText = "Emitido por KashApp Enterprise • Sello SHA-256: #$hash"
        )
    }

    /**
     * 2. Genera el Comprobante Oficial de Abono / Liquidación Individual (PG-xxxx).
     */
    fun buildPaymentVoucherTicket(
        voucherConsecutive: String,
        relatedExpenseConsecutive: String,
        expenseTitle: String,
        groupName: String?,
        fromDebtorName: String,
        toCreditorName: String,
        settlementMethodLabel: String,
        previousPendingAmount: Double,
        amountPaidNow: Double,
        newRemainingAmount: Double,
        currency: String,
        paymentDate: String,
        paymentTime: String,
        externalBankReference: String?
    ): SharedDigitalTicketModel {
        val totalOriginalDebt = (previousPendingAmount).coerceAtLeast(amountPaidNow).coerceAtLeast(1.0)
        val paidAccumulated = (totalOriginalDebt - newRemainingAmount).coerceAtLeast(0.0)
        val progressPct = ((paidAccumulated / totalOriginalDebt) * 100.0).toFloat().coerceIn(0f, 100f)

        val hash = ConsecutiveSequenceEngine.computeTicketIntegrityHash(
            consecutiveCode = relatedExpenseConsecutive,
            receiptTicketCode = voucherConsecutive,
            totalAmount = amountPaidNow,
            currency = currency,
            expenseDate = paymentDate,
            participantSharesSignature = "$fromDebtorName->$toCreditorName:$amountPaidNow"
        )

        return SharedDigitalTicketModel(
            kind = TicketKind.PAYMENT_VOUCHER_TICKET,
            primaryTicketConsecutive = voucherConsecutive,
            relatedExpenseConsecutive = relatedExpenseConsecutive,
            externalInvoiceNumber = externalBankReference,
            groupName = groupName,
            groupCode = null,
            headlineTitle = "Abono a $expenseTitle",
            merchantName = null,
            merchantNit = null,
            categoryName = "Liquidación de Cartera Compartida",
            dateTimeFormatted = "$paymentDate • $paymentTime",
            currencyCode = currency,
            exchangeRateNote = null,
            splitMethodLabel = if (newRemainingAmount < 0.01) "PAGO TOTAL (PAZ Y SALVO)" else "ABONO PARCIAL",
            subtotalFormatted = formatMoney(amountPaidNow, currency),
            ivaFormatted = null,
            impoconsumoFormatted = null,
            tipFormatted = null,
            discountFormatted = null,
            gmfOrFinancialFormatted = null,
            grandTotalFormatted = formatMoney(amountPaidNow, currency),
            grandTotalRaw = amountPaidNow,
            settlementFromName = fromDebtorName,
            settlementToName = toCreditorName,
            settlementMethodLabel = settlementMethodLabel,
            previousDebtBalanceFormatted = formatMoney(previousPendingAmount, currency),
            amountPaidNowFormatted = formatMoney(amountPaidNow, currency),
            remainingDebtBalanceFormatted = formatMoney(newRemainingAmount, currency),
            debtAmortizationProgressPct = progressPct,
            externalBankReference = externalBankReference,
            collectionInstructions = null,
            privateOwnerFooter = null,
            sha256IntegrityHash = hash,
            verificationFooterText = "Comprobante Oficial de Pago KashApp • Sello SHA-256: #$hash"
        )
    }

    /**
     * 3. Genera el Acta de Liquidación Grupal y Paz y Salvo Contable (LQ-xxxx).
     */
    fun buildGroupSettlementActTicket(
        actConsecutive: String,
        groupName: String,
        groupCode: String,
        periodDateRange: String,
        currency: String,
        totalGroupVolumeSpent: Double,
        simplificationReport: DebtSimplificationGraphEngine.GraphSimplificationReport,
        participantRows: List<TicketParticipantRow>
    ): SharedDigitalTicketModel {
        val sig = simplificationReport.optimizedInstructions.joinToString(";") {
            "${it.fromParticipantId}->${it.toParticipantId}:${it.amount}"
        }
        val hash = ConsecutiveSequenceEngine.computeTicketIntegrityHash(
            consecutiveCode = groupCode,
            receiptTicketCode = actConsecutive,
            totalAmount = simplificationReport.totalVolumeAfterSimplification,
            currency = currency,
            expenseDate = periodDateRange,
            participantSharesSignature = sig
        )

        return SharedDigitalTicketModel(
            kind = TicketKind.GROUP_SETTLEMENT_ACT,
            primaryTicketConsecutive = actConsecutive,
            relatedExpenseConsecutive = null,
            externalInvoiceNumber = null,
            groupName = groupName,
            groupCode = groupCode,
            headlineTitle = "Acta de Liquidación — $groupName",
            merchantName = "Cierre Contable de Grupo",
            merchantNit = null,
            categoryName = "Conciliación Multilateral Min-Cash-Flow",
            dateTimeFormatted = periodDateRange,
            currencyCode = currency,
            exchangeRateNote = null,
            splitMethodLabel = "${simplificationReport.simplifiedTransferCount} Pagos Optimizados (${simplificationReport.transactionsSavedCount} ahorrados)",
            subtotalFormatted = formatMoney(totalGroupVolumeSpent, currency),
            ivaFormatted = null,
            impoconsumoFormatted = null,
            tipFormatted = null,
            discountFormatted = null,
            gmfOrFinancialFormatted = null,
            grandTotalFormatted = formatMoney(simplificationReport.totalVolumeAfterSimplification, currency),
            grandTotalRaw = simplificationReport.totalVolumeAfterSimplification,
            participantRows = participantRows,
            simplifiedTransfers = simplificationReport.optimizedInstructions,
            transactionsSavedByGraphCount = simplificationReport.transactionsSavedCount,
            collectionInstructions = null,
            privateOwnerFooter = null,
            sha256IntegrityHash = hash,
            verificationFooterText = "Certificado de Paz y Salvo Contable • Sello SHA-256: #$hash"
        )
    }

    /**
     * Verifica si un ticket digital conserva intacta su firma criptográfica SHA-256.
     */
    fun verifyTicketIntegrity(
        consecutiveCode: String,
        receiptTicketCode: String,
        totalAmount: Double,
        currency: String,
        expenseDate: String,
        participantSharesSignature: String,
        expectedHash: String
    ): Boolean {
        val computed = ConsecutiveSequenceEngine.computeTicketIntegrityHash(
            consecutiveCode = consecutiveCode,
            receiptTicketCode = receiptTicketCode,
            totalAmount = totalAmount,
            currency = currency,
            expenseDate = expenseDate,
            participantSharesSignature = participantSharesSignature
        )
        return computed.equals(expectedHash.trim(), ignoreCase = true)
    }

    private fun buildCollectionQrPayload(
        ticketCode: String,
        expenseCode: String,
        collectorName: String,
        accountOrKey: String,
        totalAmount: Double,
        currency: String
    ): String {
        return "KASHAPP://PAY?tk=$ticketCode&gc=$expenseCode&to=${collectorName.take(20)}&acc=$accountOrKey&amt=${"%.2f".format(Locale.US, totalAmount)}&cur=$currency"
    }

    fun formatMoney(amount: Double, currencyCode: String): String {
        val cleanCur = currencyCode.uppercase().ifBlank { "COP" }
        return if (cleanCur == "COP") {
            "$ %,.0f COP".format(Locale.US, amount).replace(",", ".")
        } else {
            "$ %,.2f $cleanCur".format(Locale.US, amount)
        }
    }

    private fun formatSignedMoney(amount: Double, currencyCode: String): String {
        val sign = when {
            amount > 0.005 -> "+"
            amount < -0.005 -> "-"
            else -> ""
        }
        return "$sign ${formatMoney(abs(amount), currencyCode)}"
    }
}
