package com.example.domain.analytics

import com.example.domain.models.shared.RealMoneyImpactMode
import com.example.domain.models.shared.SplitCalculationMethod
import com.example.domain.security.FirebaseApiProtectionGuard
import com.example.feature.gastoscompartidos.domain.ocr.SharedOcrReceiptBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.Locale

/**
 * Copiloto de Inteligencia Artificial para Gastos Compartidos impulsado por Firebase AI (Gemini 2.5 Flash)
 * con protección Zero-Trust (App Check + Sanitización PII + Rate Limit + Fallback Determinista Offline).
 */
class SharedExpenseFirebaseAiEngine {

    companion object {
        /**
         * Proveedor cloud inyectado por `:app` (`FirebaseAiCloudProviderBridge`) que ejecuta
         * `Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel("gemini-2.5-flash")`.
         */
        @Volatile
        var cloudContentGenerator: (suspend (prompt: String, temperature: Float, maxTokens: Int) -> String)? = null
    }

    init {
        if (SharedOcrReceiptBridge.cloudAiContentGenerator == null && cloudContentGenerator != null) {
            SharedOcrReceiptBridge.cloudAiContentGenerator = { prompt ->
                cloudContentGenerator?.invoke(prompt, 0.05f, 768).orEmpty()
            }
        }
    }

    data class AiParsedSharedExpenseDraft(
        val detectedTitle: String,
        val detectedTotalAmount: Double,
        val detectedTipAmount: Double,
        val suggestedCategoryName: String,
        val suggestedSplitMethod: SplitCalculationMethod,
        val suggestedRealMoneyMode: RealMoneyImpactMode,
        val participantAdjustmentsOrAmounts: Map<String, Double>,
        val aiExplanation: String,
        val resolvedViaCloudFirebaseAi: Boolean
    )

    data class AiAccountingModeRecommendation(
        val recommendedMode: RealMoneyImpactMode,
        val recommendHoldInMentalAccounting: Boolean,
        val confidenceScore: Float,
        val justification: String,
        val liquidityWarning: String?
    )

    /**
     * 1. Analiza una instrucción en lenguaje natural (ej. "Cena en Crepes por $180.000 con Ana y Luis,
     * Luis pidió un vino extra de $30.000 y yo pagué todo con Bancolombia") usando Firebase AI protegido,
     * o el parser heurístico local si no hay conexión.
     */
    suspend fun parseNaturalLanguageExpensePrompt(
        userPrompt: String,
        participantNames: List<String>,
        userAvailableLiquidity: Double
    ): AiParsedSharedExpenseDraft = withContext(Dispatchers.IO) {
        val guardVerdict = FirebaseApiProtectionGuard.authorizeAndSanitizeRequest(
            rawPrompt = userPrompt,
            participantRealNames = participantNames,
            requireAuthenticatedUser = true
        )

        if (guardVerdict is FirebaseApiProtectionGuard.GuardVerdict.Allowed) {
            if (guardVerdict.cachedResponseIfAny != null) {
                return@withContext parseStructuredResponseOrFallback(
                    rawResponse = guardVerdict.cachedResponseIfAny!!,
                    originalPrompt = userPrompt,
                    participantNames = participantNames,
                    fromCloud = true
                )
            }

            val cloudGen = cloudContentGenerator
            if (cloudGen != null) {
                try {
                    val structuredInstruction = listOf(
                        "Eres el motor contable NIIF de Gastos Compartidos de KashApp.",
                        "Analiza el texto sanitizado y responde SOLO en formato clave=valor separado por saltos de línea:",
                        "TITLE=<título corto>",
                        "TOTAL=<monto numérico total>",
                        "TIP=<propina numérica o 0>",
                        "CATEGORY=<Alimentación|Transporte|Hogar|Viajes|Entretenimiento|Servicios|Otros>",
                        "METHOD=<EQUAL|EXACT_AMOUNT|PERCENTAGE|SHARES|ADJUSTMENT|ITEMIZED_OCR|INCOME_PROPORTIONAL>",
                        "MODE=<ACCRUAL_RECEIVABLE|GROSS_CASHFLOW|NET_SHARE_ONLY|DEFERRED_ON_SETTLEMENT>",
                        "EXPLANATION=<resumen de 1 frase>",
                        "Texto del usuario: ${guardVerdict.sanitizedPrompt}"
                    ).joinToString("\n")

                    val rawCloudText = withTimeout(6_500L) {
                        cloudGen.invoke(structuredInstruction, 0.1f, 512)
                    }

                    if (rawCloudText.isNotBlank()) {
                        FirebaseApiProtectionGuard.recordSuccessfulResponse(
                            promptHashKey = guardVerdict.promptHashKey,
                            sanitizedResponseText = rawCloudText
                        )
                        val rehydrated = FirebaseApiProtectionGuard.rehydratePiiAliases(
                            responseText = rawCloudText,
                            aliasToRealName = guardVerdict.aliasMapForRehydration
                        )
                        return@withContext parseStructuredResponseOrFallback(
                            rawResponse = rehydrated,
                            originalPrompt = userPrompt,
                            participantNames = participantNames,
                            fromCloud = true
                        )
                    }
                } catch (e: Throwable) {
                    FirebaseApiProtectionGuard.reportUpstreamApiFailure(e)
                }
            }
        }

        return@withContext buildOfflineHeuristicDraft(
            prompt = userPrompt,
            participantNames = participantNames,
            userAvailableLiquidity = userAvailableLiquidity
        )
    }

    /**
     * 2. Recomienda el modo óptimo de afectación al Dinero Real según la liquidez y el presupuesto actual.
     */
    fun recommendOptimalRealMoneyMode(
        expenseTotal: Double,
        myPaidAmount: Double,
        myConsumptionShare: Double,
        accountAvailableBalance: Double,
        categoryRemainingBudget: Double
    ): AiAccountingModeRecommendation {
        val netDelta = myPaidAmount - myConsumptionShare
        return when {
            myPaidAmount > 0.0 && netDelta > 0.0 && myPaidAmount > categoryRemainingBudget && myConsumptionShare <= categoryRemainingBudget -> {
                AiAccountingModeRecommendation(
                    recommendedMode = RealMoneyImpactMode.ACCRUAL_RECEIVABLE,
                    recommendHoldInMentalAccounting = false,
                    confidenceScore = 0.98f,
                    justification = "Se recomienda el modo Devengo NIIF (Gasto + Cuenta por Cobrar): si registraras el 100% bruto ($${myPaidAmount.toLong()}), romperías el tope disponible de tu presupuesto ($${categoryRemainingBudget.toLong()}), mientras que tu cuota real ($${myConsumptionShare.toLong()}) sí cabe dentro de tu meta.",
                    liquidityWarning = if (myPaidAmount > accountAvailableBalance * 0.7) {
                        "Atención: Este desembolso consume más del 70% de la liquidez inmediata de la cuenta seleccionada."
                    } else null
                )
            }
            myPaidAmount == 0.0 && netDelta < 0.0 -> {
                AiAccountingModeRecommendation(
                    recommendedMode = RealMoneyImpactMode.ACCRUAL_RECEIVABLE,
                    recommendHoldInMentalAccounting = true,
                    confidenceScore = 0.96f,
                    justification = "Como otra persona pagó la cuenta hoy, se recomienda activar la Reserva Mental Accounting (holdBalance) por $${(-netDelta).toLong()} para apartar ese dinero de tu saldo disponible libre y evitar gastarlo antes de transferirle.",
                    liquidityWarning = null
                )
            }
            else -> {
                AiAccountingModeRecommendation(
                    recommendedMode = RealMoneyImpactMode.ACCRUAL_RECEIVABLE,
                    recommendHoldInMentalAccounting = true,
                    confidenceScore = 0.92f,
                    justification = "El modo Devengo NIIF mantiene tu Balance General exacto reflejando la salida real del banco y el activo por cobrar a tu favor.",
                    liquidityWarning = null
                )
            }
        }
    }

    /**
     * 3. Genera un mensaje cordial y transparente para WhatsApp con el desglose del ticket, consecutivo y llave de pago.
     */
    fun generateFriendlyCollectionMessage(
        participantName: String,
        expenseTitle: String,
        consecutiveCode: String,
        receiptTicketCode: String,
        owedAmount: Double,
        totalExpenseAmount: Double,
        myPaymentRailLabel: String,
        tone: String = "CORDIAL"
    ): String {
        val formattedOwed = "$%,.0f".format(Locale("es", "CO"), owedAmount)
        val formattedTotal = "$%,.0f".format(Locale("es", "CO"), totalExpenseAmount)

        return when (tone.uppercase()) {
            "FORMAL" -> buildString {
                appendLine("Hola $participantName, espero que te encuentres muy bien.")
                appendLine("Te comparto el resumen del gasto compartido registrado en KashApp:")
                appendLine("• Concepto: $expenseTitle")
                appendLine("• Consecutivo: $consecutiveCode (Ticket $receiptTicketCode)")
                appendLine("• Total factura: $formattedTotal COP")
                appendLine("• Tu cuota correspondiente: *$formattedOwed COP*")
                if (myPaymentRailLabel.isNotBlank()) {
                    appendLine("• Medio sugerido de transferencia: $myPaymentRailLabel")
                }
                append("¡Muchas gracias!")
            }
            else -> buildString {
                appendLine("¡Hola $participantName! 👋")
                appendLine("Te paso el dato de *$expenseTitle* ($consecutiveCode):")
                appendLine("🧾 Total cuenta: $formattedTotal")
                appendLine("✨ Tu parte es: *$formattedOwed*")
                if (myPaymentRailLabel.isNotBlank()) {
                    appendLine("📲 Puedes pasarme por: $myPaymentRailLabel")
                }
                append("¡Gracias! 🙌")
            }
        }
    }

    private fun parseStructuredResponseOrFallback(
        rawResponse: String,
        originalPrompt: String,
        participantNames: List<String>,
        fromCloud: Boolean
    ): AiParsedSharedExpenseDraft {
        val map = rawResponse.lines()
            .mapNotNull { line ->
                val idx = line.indexOf('=')
                if (idx > 0) line.substring(0, idx).trim().uppercase() to line.substring(idx + 1).trim() else null
            }.toMap()

        val fallback = buildOfflineHeuristicDraft(originalPrompt, participantNames, 1_000_000.0)
        val parsedTotal = map["TOTAL"]?.replace(Regex("[^0-9.]"), "")?.toDoubleOrNull() ?: fallback.detectedTotalAmount
        val parsedTip = map["TIP"]?.replace(Regex("[^0-9.]"), "")?.toDoubleOrNull() ?: fallback.detectedTipAmount
        val method = SplitCalculationMethod.entries.find { it.code.equals(map["METHOD"], ignoreCase = true) }
            ?: fallback.suggestedSplitMethod
        val mode = RealMoneyImpactMode.entries.find { it.code.equals(map["MODE"], ignoreCase = true) }
            ?: fallback.suggestedRealMoneyMode

        return AiParsedSharedExpenseDraft(
            detectedTitle = map["TITLE"]?.takeIf { it.isNotBlank() } ?: fallback.detectedTitle,
            detectedTotalAmount = parsedTotal,
            detectedTipAmount = parsedTip,
            suggestedCategoryName = map["CATEGORY"] ?: fallback.suggestedCategoryName,
            suggestedSplitMethod = method,
            suggestedRealMoneyMode = mode,
            participantAdjustmentsOrAmounts = fallback.participantAdjustmentsOrAmounts,
            aiExplanation = map["EXPLANATION"] ?: fallback.aiExplanation,
            resolvedViaCloudFirebaseAi = fromCloud
        )
    }

    internal fun buildOfflineHeuristicDraft(
        prompt: String,
        participantNames: List<String>,
        userAvailableLiquidity: Double
    ): AiParsedSharedExpenseDraft {
        val lower = prompt.lowercase()
        val numbers = Regex("\\$?\\s*([0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]{4,9})")
            .findAll(prompt)
            .mapNotNull { it.groupValues[1].replace(".", "").replace(",", "").toDoubleOrNull() }
            .toList()

        val totalAmount = numbers.maxOrNull() ?: 0.0
        val secondaryAmounts = numbers.filter { it < totalAmount }

        val method = when {
            lower.contains("%") || lower.contains("por ciento") -> SplitCalculationMethod.PERCENTAGE
            lower.contains("extra") || lower.contains("adicional") || secondaryAmounts.isNotEmpty() -> SplitCalculationMethod.ADJUSTMENT
            lower.contains("día") || lower.contains("dias") || lower.contains("noches") -> SplitCalculationMethod.SHARES
            lower.contains("ingreso") || lower.contains("sueldo") -> SplitCalculationMethod.INCOME_PROPORTIONAL
            else -> SplitCalculationMethod.EQUAL
        }

        val category = when {
            lower.contains("cena") || lower.contains("almuerzo") || lower.contains("restaurante") || lower.contains("mercado") -> "Alimentación"
            lower.contains("uber") || lower.contains("taxi") || lower.contains("gasolina") || lower.contains("peaje") -> "Transporte"
            lower.contains("arriendo") || lower.contains("servicios") || lower.contains("internet") -> "Hogar"
            lower.contains("viaje") || lower.contains("hotel") || lower.contains("airbnb") -> "Viajes"
            else -> "Gastos Compartidos"
        }

        val adjustments = mutableMapOf<String, Double>()
        if (method == SplitCalculationMethod.ADJUSTMENT && secondaryAmounts.isNotEmpty()) {
            val mentionedParticipant = participantNames.find { lower.contains(it.lowercase()) }
            if (mentionedParticipant != null) {
                adjustments[mentionedParticipant] = secondaryAmounts.first()
            }
        }

        return AiParsedSharedExpenseDraft(
            detectedTitle = prompt.take(36).trim().ifBlank { "Gasto Compartido" },
            detectedTotalAmount = totalAmount,
            detectedTipAmount = 0.0,
            suggestedCategoryName = category,
            suggestedSplitMethod = method,
            suggestedRealMoneyMode = RealMoneyImpactMode.ACCRUAL_RECEIVABLE,
            participantAdjustmentsOrAmounts = adjustments,
            aiExplanation = "Detectado monto total de $${totalAmount.toLong()} en categoría $category con método ${method.shortLabel}.",
            resolvedViaCloudFirebaseAi = false
        )
    }
}
