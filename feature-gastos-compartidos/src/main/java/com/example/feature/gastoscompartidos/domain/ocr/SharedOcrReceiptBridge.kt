package com.example.feature.gastoscompartidos.domain.ocr

import com.example.domain.security.FirebaseApiProtectionGuard
import com.example.feature.gastoscompartidos.domain.engine.SharedSplitCalculatorEngine
import com.example.feature.ocr.model.ReceiptItem
import com.example.feature.ocr.model.ReceiptScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Puente inteligente entre `:feature-ocr` (Google ML Kit Vision) y `:feature-gastos-compartidos`.
 * Extrae metadatos fiscales (NIT, Factura POS/DIAN, IVA 19%, Impoconsumo 8%, Propina 10%),
 * prepara los ítems para división interactiva por comensal y desambigua facturas borrosas
 * mediante Firebase AI bajo la protección de FirebaseApiProtectionGuard.
 */
object SharedOcrReceiptBridge {

    /**
     * Proveedor inyectable desde `:app` (`SharedExpenseFirebaseAiEngine`) que invoca
     * `Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel("gemini-2.5-flash")`.
     */
    @Volatile
    var cloudAiContentGenerator: (suspend (prompt: String) -> String)? = null

    private val NIT_REGEX = Regex(
        """(?i)\b(?:nit|n\.i\.t\.?|rut)\s*[:#-]?\s*([0-9]{3}[.\s]?[0-9]{3}[.\s]?[0-9]{3}(?:-[0-9kK])?|[0-9]{8,10}(?:-[0-9kK])?)\b"""
    )

    private val INVOICE_NUMBER_REGEX = Regex(
        """(?i)\b(?:factura(?:\s+electr[oó]nica)?(?:\s+de\s+venta)?|fe|pos|sett|tiquete|recibo|nro\.?|no\.?)\s*[:#-]?\s*([A-Z]{1,6}[- ]?[0-9]{3,10})\b"""
    )

    private val TIP_PATTERNS = listOf(
        Regex("""(?i)\b(?:propina(?:\s+voluntaria)?|servicio(?:\s+voluntario)?|tip|10%\s+sugerido)(?:\s*\d+%)?\s*[:=]?\s*[$]?\s*([0-9.,]+)""")
    )

    private val CONSUMPTION_TAX_PATTERNS = listOf(
        Regex("""(?i)\b(?:impoconsumo|ipoconsumo|imp\.?\s*consumo|inc)(?:\s*\d+%)?\s*[:=]?\s*[$]?\s*([0-9.,]+)""")
    )

    private val DISCOUNT_PATTERNS = listOf(
        Regex("""(?i)\b(?:descuento|dcto|ahorro|bono|promo)\s*[:=]?\s*[-$]?\s*([0-9.,]+)""")
    )

    private val MULTI_QUANTITY_LINE_REGEX = Regex(
        """^\s*(\d+(?:[.,]\d+)?)\s*[xX*]?\s+([A-Za-zÀ-ÿ0-9\s./-]{3,38})\s+[$]?\s*([0-9.,]+)\s*$"""
    )

    data class EnrichedOcrExpenseDraft(
        val merchantName: String,
        val merchantNit: String?,
        val detectedExternalInvoiceNumber: String?,
        val suggestedCategoryName: String,
        val expenseDateString: String,
        val detectedCurrency: String,
        val subtotalAmount: Double,
        val taxIvaAmount: Double,
        val taxConsumptionAmount: Double,
        val tipAmount: Double,
        val discountAmount: Double,
        val totalAmount: Double,
        val lineItems: List<SharedSplitCalculatorEngine.OcrLineItemInput>,
        val sumOfLineItemsMatchesSubtotal: Boolean,
        val reconciliationDiscrepancyAmount: Double,
        val wasEnhancedByFirebaseAi: Boolean,
        val ocrConfidence: Float
    )

    /**
     * Convierte de forma síncrona y determinista un `ReceiptScanResult` de `:feature-ocr`
     * en un borrador enriquecido listo para dividir ítem por ítem.
     */
    fun buildDraftFromOcrScan(
        scanResult: ReceiptScanResult,
        expandMultiQuantityItemsIntoSingleUnits: Boolean = false,
        defaultParticipantIdsForAllItems: List<String> = emptyList()
    ): EnrichedOcrExpenseDraft {
        val rawText = scanResult.rawText
        val nit = NIT_REGEX.find(rawText)?.groupValues?.getOrNull(1)?.trim()
        val invoiceNumber = INVOICE_NUMBER_REGEX.find(rawText)?.groupValues?.getOrNull(1)
            ?.trim()
            ?.uppercase()
            ?.replace(" ", "-")

        val extractedTip = extractFirstAmount(rawText, TIP_PATTERNS)
        val extractedImpoconsumo = extractFirstAmount(rawText, CONSUMPTION_TAX_PATTERNS)
        val extractedDiscount = extractFirstAmount(rawText, DISCOUNT_PATTERNS)

        // Si el parser base detectó un impuesto global y también detectamos impoconsumo explícito, separamos IVA de Impoconsumo
        val rawTax = scanResult.taxAmount.coerceAtLeast(0.0)
        val (ivaAmount, impoconsumoAmount) = if (extractedImpoconsumo > 0.0 && rawTax >= extractedImpoconsumo) {
            roundCents(rawTax - extractedImpoconsumo) to roundCents(extractedImpoconsumo)
        } else {
            roundCents(rawTax) to roundCents(extractedImpoconsumo)
        }

        // Construir o re-parsear líneas con soporte de cantidades (ej. "3x Cerveza $27.000")
        val parsedItems = extractDetailedItems(
            rawText = rawText,
            fallbackItems = scanResult.items,
            expandMultiUnits = expandMultiQuantityItemsIntoSingleUnits,
            defaultAssignedIds = defaultParticipantIdsForAllItems
        )

        val itemsSum = roundCents(parsedItems.sumOf { it.lineTotalPrice })
        val total = when {
            scanResult.totalAmount > 0.0 -> roundCents(scanResult.totalAmount)
            itemsSum > 0.0 -> roundCents(itemsSum + ivaAmount + impoconsumoAmount + extractedTip - extractedDiscount)
            else -> 0.0
        }

        val subtotal = when {
            scanResult.subtotal > 0.0 && scanResult.subtotal < total -> roundCents(scanResult.subtotal)
            itemsSum > 0.0 && itemsSum <= total -> itemsSum
            total > 0.0 -> roundCents((total - ivaAmount - impoconsumoAmount - extractedTip + extractedDiscount).coerceAtLeast(0.0))
            else -> 0.0
        }

        val discrepancy = if (parsedItems.isNotEmpty()) {
            roundCents(subtotal - itemsSum)
        } else 0.0

        // Si hay una diferencia positiva entre el subtotal de la factura y los ítems leídos por el OCR,
        // se añade automáticamente un ítem de ajuste ("Otros cargos / Ítems no legibles en tirilla") para no perder cuadre
        val finalLineItems = if (parsedItems.isNotEmpty() && abs(discrepancy) >= 1.0) {
            if (discrepancy > 0.0) {
                parsedItems + SharedSplitCalculatorEngine.OcrLineItemInput(
                    itemId = "ocr_adj_${UUID.randomUUID().toString().take(6)}",
                    description = "Saldo de ítems adicionales en factura",
                    quantity = 1.0,
                    unitPrice = discrepancy,
                    lineTotalPrice = discrepancy,
                    isTaxExempt = false,
                    assignedParticipantIds = defaultParticipantIdsForAllItems
                )
            } else {
                parsedItems
            }
        } else {
            parsedItems
        }

        return EnrichedOcrExpenseDraft(
            merchantName = scanResult.merchantName.ifBlank { "Establecimiento Comercial" },
            merchantNit = nit,
            detectedExternalInvoiceNumber = invoiceNumber,
            suggestedCategoryName = scanResult.suggestedCategory.ifBlank { "Alimentación" },
            expenseDateString = scanResult.dateString,
            detectedCurrency = scanResult.detectedCurrency.ifBlank { "COP" },
            subtotalAmount = subtotal,
            taxIvaAmount = ivaAmount,
            taxConsumptionAmount = impoconsumoAmount,
            tipAmount = roundCents(extractedTip),
            discountAmount = roundCents(extractedDiscount),
            totalAmount = total,
            lineItems = finalLineItems,
            sumOfLineItemsMatchesSubtotal = abs(discrepancy) < 1.0,
            reconciliationDiscrepancyAmount = discrepancy,
            wasEnhancedByFirebaseAi = false,
            ocrConfidence = scanResult.confidence
        )
    }

    /**
     * Desambigua facturas térmicas borrosas o descuadradas invocando Firebase AI (Gemini 2.5 Flash)
     * bajo el guardián Zero-Trust `FirebaseApiProtectionGuard` (App Check + Sanitización PII + Rate Limit).
     * Si el dispositivo está sin internet o el guardián bloquea la llamada, devuelve el borrador determinista local.
     */
    suspend fun enrichBlurryOrComplexReceiptWithAi(
        scanResult: ReceiptScanResult,
        defaultParticipantIds: List<String> = emptyList()
    ): EnrichedOcrExpenseDraft = withContext(Dispatchers.IO) {
        val localDraft = buildDraftFromOcrScan(
            scanResult = scanResult,
            expandMultiQuantityItemsIntoSingleUnits = false,
            defaultParticipantIdsForAllItems = defaultParticipantIds
        )

        // Si el OCR local ya cuadró perfectamente con alta confianza, evitamos gastar cuota de API en la nube
        if (localDraft.sumOfLineItemsMatchesSubtotal && localDraft.lineItems.isNotEmpty() && localDraft.ocrConfidence >= 0.90f) {
            return@withContext localDraft
        }

        val verdict = FirebaseApiProtectionGuard.authorizeAndSanitizeRequest(
            rawPrompt = scanResult.rawText,
            participantRealNames = emptyList(),
            requireAuthenticatedUser = false
        )

        if (verdict is FirebaseApiProtectionGuard.GuardVerdict.Allowed) {
            if (verdict.cachedResponseIfAny != null) {
                return@withContext mergeAiReplyWithLocalDraft(verdict.cachedResponseIfAny!!, localDraft, defaultParticipantIds)
            }
            val generator = cloudAiContentGenerator
            if (generator != null) {
                try {
                    val prompt = listOf(
                        "Eres el analizador contable de facturas POS y DIAN de KashApp.",
                        "Reconstruye el tiquete escaneado y responde SOLO con líneas en este formato exacto:",
                        "MERCHANT=<nombre comercio>",
                        "NIT=<nit o vacío>",
                        "INVOICE=<número factura o vacío>",
                        "SUBTOTAL=<número>",
                        "IVA=<número>",
                        "IMPOCONSUMO=<número>",
                        "TIP=<número>",
                        "DISCOUNT=<número>",
                        "TOTAL=<número>",
                        "ITEM=<cantidad>|<descripción>|<precio_total_linea>|<EXEMPT o TAXED>",
                        "Texto OCR Sanitizado:",
                        verdict.sanitizedPrompt
                    ).joinToString("\n")

                    val aiReply = withTimeout(7_000L) {
                        generator.invoke(prompt)
                    }

                    if (aiReply.isNotBlank()) {
                        FirebaseApiProtectionGuard.recordSuccessfulResponse(verdict.promptHashKey, aiReply)
                        return@withContext mergeAiReplyWithLocalDraft(aiReply, localDraft, defaultParticipantIds)
                    }
                } catch (e: Throwable) {
                    FirebaseApiProtectionGuard.reportUpstreamApiFailure(e)
                }
            }
        }

        return@withContext localDraft
    }

    suspend fun enrichBlurryOrMisalignedReceiptWithAi(
        scanResult: ReceiptScanResult,
        defaultParticipantIds: List<String> = emptyList()
    ): EnrichedOcrExpenseDraft = enrichBlurryOrComplexReceiptWithAi(scanResult, defaultParticipantIds)

    private fun mergeAiReplyWithLocalDraft(
        aiReply: String,
        localDraft: EnrichedOcrExpenseDraft,
        defaultParticipantIds: List<String>
    ): EnrichedOcrExpenseDraft {
        val kvMap = mutableMapOf<String, String>()
        val aiItems = mutableListOf<SharedSplitCalculatorEngine.OcrLineItemInput>()

        for (line in aiReply.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("ITEM=", ignoreCase = true)) {
                val parts = trimmed.substringAfter("=").split("|")
                if (parts.size >= 3) {
                    val qty = parts[0].trim().toDoubleOrNull()?.coerceAtLeast(1.0) ?: 1.0
                    val desc = parts[1].trim().ifBlank { "Producto" }
                    val lineTotal = parseFlexibleMoney(parts[2])
                    val isExempt = parts.getOrNull(3)?.trim()?.equals("EXEMPT", ignoreCase = true) == true
                    if (lineTotal > 0.0) {
                        aiItems.add(
                            SharedSplitCalculatorEngine.OcrLineItemInput(
                                itemId = "ai_item_${aiItems.size + 1}",
                                description = desc,
                                quantity = qty,
                                unitPrice = roundCents(lineTotal / qty),
                                lineTotalPrice = lineTotal,
                                isTaxExempt = isExempt,
                                assignedParticipantIds = defaultParticipantIds
                            )
                        )
                    }
                }
            } else {
                val idx = trimmed.indexOf('=')
                if (idx > 0) {
                    kvMap[trimmed.substring(0, idx).trim().uppercase()] = trimmed.substring(idx + 1).trim()
                }
            }
        }

        val total = kvMap["TOTAL"]?.let { parseFlexibleMoney(it) }?.takeIf { it > 0.0 } ?: localDraft.totalAmount
        val subtotal = kvMap["SUBTOTAL"]?.let { parseFlexibleMoney(it) }?.takeIf { it > 0.0 } ?: localDraft.subtotalAmount
        val iva = kvMap["IVA"]?.let { parseFlexibleMoney(it) } ?: localDraft.taxIvaAmount
        val impo = kvMap["IMPOCONSUMO"]?.let { parseFlexibleMoney(it) } ?: localDraft.taxConsumptionAmount
        val tip = kvMap["TIP"]?.let { parseFlexibleMoney(it) } ?: localDraft.tipAmount
        val discount = kvMap["DISCOUNT"]?.let { parseFlexibleMoney(it) } ?: localDraft.discountAmount
        val resolvedItems = if (aiItems.isNotEmpty()) aiItems else localDraft.lineItems
        val itemsSum = roundCents(resolvedItems.sumOf { it.lineTotalPrice })

        return localDraft.copy(
            merchantName = kvMap["MERCHANT"]?.takeIf { it.isNotBlank() } ?: localDraft.merchantName,
            merchantNit = kvMap["NIT"]?.takeIf { it.isNotBlank() } ?: localDraft.merchantNit,
            detectedExternalInvoiceNumber = kvMap["INVOICE"]?.takeIf { it.isNotBlank() } ?: localDraft.detectedExternalInvoiceNumber,
            subtotalAmount = subtotal,
            taxIvaAmount = iva,
            taxConsumptionAmount = impo,
            tipAmount = tip,
            discountAmount = discount,
            totalAmount = total,
            lineItems = resolvedItems,
            sumOfLineItemsMatchesSubtotal = abs(subtotal - itemsSum) < 1.0,
            reconciliationDiscrepancyAmount = roundCents(subtotal - itemsSum),
            wasEnhancedByFirebaseAi = true
        )
    }

    private fun extractDetailedItems(
        rawText: String,
        fallbackItems: List<ReceiptItem>,
        expandMultiUnits: Boolean,
        defaultAssignedIds: List<String>
    ): List<SharedSplitCalculatorEngine.OcrLineItemInput> {
        val result = mutableListOf<SharedSplitCalculatorEngine.OcrLineItemInput>()
        val ignoredKeywords = listOf("subtotal", "sub-total", "total", "iva", "impoconsumo", "propina", "cambio", "efectivo", "tarjeta", "descuento")

        for (line in rawText.lines().map { it.trim() }) {
            val match = MULTI_QUANTITY_LINE_REGEX.find(line)
            if (match != null) {
                val qty = match.groupValues[1].replace(",", ".").toDoubleOrNull()?.coerceAtLeast(1.0) ?: 1.0
                val desc = match.groupValues[2].trim()
                val lineTotal = parseFlexibleMoney(match.groupValues[3])
                val lowerDesc = desc.lowercase()
                if (lineTotal > 0.0 && ignoredKeywords.none { lowerDesc.contains(it) }) {
                    val wholeUnits = qty.toInt()
                    if (expandMultiUnits && wholeUnits in 2..12 && abs(qty - wholeUnits) < 0.01) {
                        val unitPrice = roundCents(lineTotal / wholeUnits)
                        for (u in 1..wholeUnits) {
                            result.add(
                                SharedSplitCalculatorEngine.OcrLineItemInput(
                                    itemId = "item_${result.size + 1}",
                                    description = "$desc ($u/$wholeUnits)",
                                    quantity = 1.0,
                                    unitPrice = unitPrice,
                                    lineTotalPrice = unitPrice,
                                    assignedParticipantIds = defaultAssignedIds
                                )
                            )
                        }
                    } else {
                        result.add(
                            SharedSplitCalculatorEngine.OcrLineItemInput(
                                itemId = "item_${result.size + 1}",
                                description = if (qty > 1.0) "${qty.toInt()}x $desc" else desc,
                                quantity = qty,
                                unitPrice = roundCents(lineTotal / qty),
                                lineTotalPrice = lineTotal,
                                assignedParticipantIds = defaultAssignedIds
                            )
                        )
                    }
                }
            }
        }

        if (result.isNotEmpty()) return result

        return fallbackItems.mapIndexed { idx, item ->
            val q = item.quantity.coerceAtLeast(1.0)
            val tot = item.totalPrice
            val uPrice = if (item.unitPrice > 0.0) item.unitPrice else roundCents(tot / q)
            SharedSplitCalculatorEngine.OcrLineItemInput(
                itemId = "item_${idx + 1}",
                description = item.description,
                quantity = q,
                unitPrice = uPrice,
                lineTotalPrice = tot,
                assignedParticipantIds = defaultAssignedIds
            )
        }
    }

    private fun extractFirstAmount(rawText: String, patterns: List<Regex>): Double {
        for (p in patterns) {
            val match = p.find(rawText)
            if (match != null) {
                val amount = parseFlexibleMoney(match.groupValues.last())
                if (amount > 0.0) return amount
            }
        }
        return 0.0
    }

    internal fun parseFlexibleMoney(raw: String): Double {
        val cleaned = raw.replace("$", "").replace(" ", "").trim()
        if (cleaned.isEmpty()) return 0.0
        val normalized = when {
            cleaned.matches(Regex("""^\d{1,3}(\.\d{3})+(,\d{1,2})?$""")) ->
                cleaned.replace(".", "").replace(",", ".")
            cleaned.matches(Regex("""^\d{1,3}(,\d{3})+(\.\d{1,2})?$""")) ->
                cleaned.replace(",", "")
            cleaned.matches(Regex("""^\d{1,3}\.\d{3}$""")) ->
                cleaned.replace(".", "")
            cleaned.count { it == '.' } > 1 ->
                cleaned.replace(".", "")
            cleaned.contains(",") && !cleaned.contains(".") ->
                cleaned.replace(",", ".")
            else -> cleaned
        }
        return normalized.toDoubleOrNull() ?: 0.0
    }

    private fun roundCents(v: Double): Double = (v * 100.0).roundToLong() / 100.0
}
