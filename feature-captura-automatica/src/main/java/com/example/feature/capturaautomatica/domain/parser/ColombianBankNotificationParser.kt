package com.example.feature.capturaautomatica.domain.parser

import com.example.feature.capturaautomatica.domain.model.ParsedBankNotification
import com.example.feature.capturaautomatica.domain.model.SupportedColombianBank
import java.util.Locale
import kotlin.math.roundToLong

class ColombianBankNotificationParser {

    /**
     * Analiza el paquete, título y texto de una notificación de la barra de estado de Android.
     * Retorna [ParsedBankNotification] si corresponde a una transacción financiera válida,
     * o `null` si es una clave dinámica, OTP, publicidad o paquete no soportado.
     */
    fun parse(
        packageName: String,
        title: String?,
        text: String?,
        postTime: Long = System.currentTimeMillis()
    ): ParsedBankNotification? {
        val safeTitle = title?.trim().orEmpty()
        val safeText = text?.trim().orEmpty()

        if (safeText.isBlank() && safeTitle.isBlank()) return null

        // 1. Escudo Anti-OTP: descartar de inmediato cualquier código de seguridad o clave dinámica
        if (OtpSafetyFilter.isSensitiveAuthMessage(safeTitle, safeText)) {
            return null
        }

        // 2. Validar que el paquete pertenezca a la lista blanca de bancos soportados
        val bank = SupportedColombianBank.findByPackage(packageName) ?: return null

        val combined = "$safeTitle $safeText".trim()

        return when (bank) {
            SupportedColombianBank.BANCOLOMBIA -> parseBancolombia(packageName, bank, combined, postTime)
            SupportedColombianBank.NEQUI -> parseNequi(packageName, bank, combined, postTime)
            SupportedColombianBank.NU_COLOMBIA -> parseNu(packageName, bank, combined, postTime)
            SupportedColombianBank.DAVIPLATA -> parseDaviPlata(packageName, bank, combined, postTime)
            SupportedColombianBank.DAVIVIENDA -> parseDavivienda(packageName, bank, combined, postTime)
            SupportedColombianBank.BBVA -> parseBbva(packageName, bank, combined, postTime)
            SupportedColombianBank.LULO_BANK -> parseLulo(packageName, bank, combined, postTime)
        }
    }

    private fun parseBancolombia(
        pkg: String,
        bank: SupportedColombianBank,
        text: String,
        postTime: Long
    ): ParsedBankNotification? {
        // A. Compra Bancolombia:
        // "Bancolombia le informa Compra por $125.400,00 en EXITO WOW COUNTRY 18:30 19/10/2026 T.Deb *4821"
        // "Bancolombia le informa Compra por $48.500,00 en D1 TUNJA CENTRO 14:22 19/10/2026 T.Deb *4821"
        val compraRegex = Regex(
            """(?iu)compra\s+por\s+\$?\s*([0-9.,]+)\s+en\s+(.+?)(?:\s+\d{1,2}:\d{2}|\s+T\.(?:Deb|Cred)|\s+\*(\d{4})|$)"""
        )
        val compraMatch = compraRegex.find(text)
        if (compraMatch != null) {
            val amount = parseAmount(compraMatch.groupValues[1])
            val rawMerchant = compraMatch.groupValues[2].trim()
            val cardDigits = extractLastFourDigits(text) ?: compraMatch.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }
            if (amount > 0.0) {
                val merchant = cleanMerchantName(rawMerchant)
                return buildResult(pkg, bank, "EXPENSE", amount, merchant, cardDigits, postTime)
            }
        }

        // B. Transferencia enviada Bancolombia:
        // "Bancolombia le informa Transferencia por $120.000,00 desde cta *9012 a cta 00000012345"
        val transEnvRegex = Regex("""(?iu)transferencia\s+por\s+\$?\s*([0-9.,]+)\s+desde\s+(?:cta|cuenta)\s+\*?(\d{4})""")
        val transEnvMatch = transEnvRegex.find(text)
        if (transEnvMatch != null) {
            val amount = parseAmount(transEnvMatch.groupValues[1])
            val digits = transEnvMatch.groupValues[2]
            if (amount > 0.0) {
                return buildResult(pkg, bank, "EXPENSE", amount, "Transferencia Bancolombia", digits, postTime)
            }
        }

        // C. Recepción de transferencia / Pago de nómina / Abono:
        // "Bancolombia le informa Recepcion de transferencia por $350.000,00 en su cuenta *9012"
        // "Bancolombia le informa Pago de nomina por $2.500.000,00 en su cuenta *9012"
        val ingresoRegex = Regex(
            """(?iu)(?:recepci[oóOÓ]n\s+de\s+transferencia|pago\s+de\s+n[oóOÓ]mina|abono|consignaci[oóOÓ]n)\s+por\s+\$?\s*([0-9.,]+)(?:.*?cuenta\s+\*?(\d{4}))?"""
        )
        val ingresoMatch = ingresoRegex.find(text)
        if (ingresoMatch != null) {
            val amount = parseAmount(ingresoMatch.groupValues[1])
            val digits = ingresoMatch.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() } ?: extractLastFourDigits(text)
            if (amount > 0.0) {
                return buildResult(pkg, bank, "INCOME", amount, "Transferencia Recibida Bancolombia", digits, postTime)
            }
        }

        // D. Retiro en cajero:
        val retiroRegex = Regex("""(?iu)retiro\s+por\s+\$?\s*([0-9.,]+)(?:.*?cta\s+\*?(\d{4}))?""")
        val retiroMatch = retiroRegex.find(text)
        if (retiroMatch != null) {
            val amount = parseAmount(retiroMatch.groupValues[1])
            val digits = retiroMatch.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() } ?: extractLastFourDigits(text)
            if (amount > 0.0) {
                return buildResult(pkg, bank, "EXPENSE", amount, "Retiro Cajero Bancolombia", digits, postTime)
            }
        }

        return null
    }

    private fun parseNequi(
        pkg: String,
        bank: SupportedColombianBank,
        text: String,
        postTime: Long
    ): ParsedBankNotification? {
        // A. Pago en comercio: "Pagaste $18.500,00 en JUAN VALDEZ CAFE con tu Nequi."
        // "Pagaste $22.000,00 en CREPES Y WAFFLES con tu Nequi"
        val pagoRegex = Regex("""(?iu)pagaste\s+\$?\s*([0-9.,]+)\s+en\s+(.+?)(?:\s+con\s+tu\s+nequi|\.|$)""")
        val pagoMatch = pagoRegex.find(text)
        if (pagoMatch != null) {
            val amount = parseAmount(pagoMatch.groupValues[1])
            val merchant = cleanMerchantName(pagoMatch.groupValues[2])
            if (amount > 0.0) {
                return buildResult(pkg, bank, "EXPENSE", amount, merchant, null, postTime)
            }
        }

        // B. Envío de plata: "Enviaste $50.000,00 a MARIA LOPEZ"
        val envioRegex = Regex("""(?iu)enviaste\s+\$?\s*([0-9.,]+)\s+a\s+(.+?)(?:\s+desde|\.|$)""")
        val envioMatch = envioRegex.find(text)
        if (envioMatch != null) {
            val amount = parseAmount(envioMatch.groupValues[1])
            val dest = cleanMerchantName(envioMatch.groupValues[2])
            if (amount > 0.0) {
                return buildResult(pkg, bank, "EXPENSE", amount, dest, null, postTime)
            }
        }

        // C. Recepción de plata: "Recibiste $250.000,00 de ANDRES GOMEZ en tu Nequi"
        val reciboRegex = Regex("""(?iu)recibiste\s+\$?\s*([0-9.,]+)\s+de\s+(.+?)(?:\s+en\s+tu\s+nequi|\.|$)""")
        val reciboMatch = reciboRegex.find(text)
        if (reciboMatch != null) {
            val amount = parseAmount(reciboMatch.groupValues[1])
            val sender = cleanMerchantName(reciboMatch.groupValues[2])
            if (amount > 0.0) {
                return buildResult(pkg, bank, "INCOME", amount, sender, null, postTime)
            }
        }

        return null
    }

    private fun parseNu(
        pkg: String,
        bank: SupportedColombianBank,
        text: String,
        postTime: Long
    ): ParsedBankNotification? {
        // Compra aprobada:
        // "Compra aprobada por $ 42.000,00 en DIDI RIDES con tu tarjeta ...9104"
        // "Compra aprobada por $ 64.900,00 en UBER TRIP con tu tarjeta ...7712"
        val compraRegex = Regex(
            """(?iu)compra\s+aprobada\s+por\s+\$?\s*([0-9.,]+)\s+en\s+(.+?)(?:\s+con\s+tu\s+tarjeta\s+(?:\.+|\*)?(\d{4})|\.|$)"""
        )
        val compraMatch = compraRegex.find(text)
        if (compraMatch != null) {
            val amount = parseAmount(compraMatch.groupValues[1])
            val merchant = cleanMerchantName(compraMatch.groupValues[2])
            val digits = compraMatch.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() } ?: extractLastFourDigits(text)
            if (amount > 0.0) {
                return buildResult(pkg, bank, "EXPENSE", amount, merchant, digits, postTime)
            }
        }

        // Transferencia saliente:
        val transEnv = Regex("""(?iu)transferencia\s+enviada\s+por\s+\$?\s*([0-9.,]+)(?:\s+a\s+(.+?))?(?:\.|$)""")
        val transEnvMatch = transEnv.find(text)
        if (transEnvMatch != null) {
            val amount = parseAmount(transEnvMatch.groupValues[1])
            val dest = transEnvMatch.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "Transferencia Cuenta Nu"
            if (amount > 0.0) {
                return buildResult(pkg, bank, "EXPENSE", amount, cleanMerchantName(dest), null, postTime)
            }
        }

        // Transferencia entrante:
        val transEnt = Regex("""(?iu)recibiste\s+(?:una\s+)?transferencia\s+por\s+\$?\s*([0-9.,]+)(?:\s+de\s+(.+?))?(?:\.|$)""")
        val transEntMatch = transEnt.find(text)
        if (transEntMatch != null) {
            val amount = parseAmount(transEntMatch.groupValues[1])
            val sender = transEntMatch.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "Transferencia Recibida Nu"
            if (amount > 0.0) {
                return buildResult(pkg, bank, "INCOME", amount, cleanMerchantName(sender), null, postTime)
            }
        }

        return null
    }

    private fun parseDaviPlata(
        pkg: String,
        bank: SupportedColombianBank,
        text: String,
        postTime: Long
    ): ParsedBankNotification? {
        // "Compra por $45.000 en ALMACENES EXITO"
        val compraRegex = Regex("""(?iu)compra\s+por\s+\$?\s*([0-9.,]+)\s+en\s+(.+?)(?:\.|\s+desde|$)""")
        val compraMatch = compraRegex.find(text)
        if (compraMatch != null) {
            val amount = parseAmount(compraMatch.groupValues[1])
            val merchant = cleanMerchantName(compraMatch.groupValues[2])
            if (amount > 0.0) {
                return buildResult(pkg, bank, "EXPENSE", amount, merchant, null, postTime)
            }
        }

        // "Pasaste plata por $30.000 a 3001234567"
        val pasarPlata = Regex("""(?iu)pasaste\s+plata\s+por\s+\$?\s*([0-9.,]+)(?:\s+a\s+(.+?))?(?:\.|$)""")
        val pasarMatch = pasarPlata.find(text)
        if (pasarMatch != null) {
            val amount = parseAmount(pasarMatch.groupValues[1])
            val dest = pasarMatch.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "Transferencia DaviPlata"
            if (amount > 0.0) {
                return buildResult(pkg, bank, "EXPENSE", amount, cleanMerchantName(dest), null, postTime)
            }
        }

        // "Le pasaron $50.000 a su DaviPlata"
        val ingresoMatch = Regex("""(?iu)le\s+pasaron\s+\$?\s*([0-9.,]+)(?:\s+de\s+(.+?))?(?:\s+a\s+su\s+daviplata|\.|$)""").find(text)
        if (ingresoMatch != null) {
            val amount = parseAmount(ingresoMatch.groupValues[1])
            val sender = ingresoMatch.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "Transferencia Recibida DaviPlata"
            if (amount > 0.0) {
                return buildResult(pkg, bank, "INCOME", amount, cleanMerchantName(sender), null, postTime)
            }
        }

        return null
    }

    private fun parseDavivienda(
        pkg: String,
        bank: SupportedColombianBank,
        text: String,
        postTime: Long
    ): ParsedBankNotification? {
        val compraRegex = Regex(
            """(?iu)compra\s+por\s+\$?\s*([0-9.,]+)\s+en\s+(.+?)(?:\s+con\s+tarjeta\s+terminada\s+en\s+(\d{4})|\*(\d{4})|\.|$)"""
        )
        val match = compraRegex.find(text)
        if (match != null) {
            val amount = parseAmount(match.groupValues[1])
            val merchant = cleanMerchantName(match.groupValues[2])
            val digits = match.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }
                ?: match.groupValues.getOrNull(4)?.takeIf { it.isNotBlank() }
                ?: extractLastFourDigits(text)
            if (amount > 0.0) {
                return buildResult(pkg, bank, "EXPENSE", amount, merchant, digits, postTime)
            }
        }
        return null
    }

    private fun parseBbva(
        pkg: String,
        bank: SupportedColombianBank,
        text: String,
        postTime: Long
    ): ParsedBankNotification? {
        val compraRegex = Regex(
            """(?iu)compra\s+(?:autorizada\s+)?por\s+\$?\s*([0-9.,]+)\s+en\s+(.+?)(?:\s+con\s+tarjeta\s+\*?(\d{4})|\.|$)"""
        )
        val match = compraRegex.find(text)
        if (match != null) {
            val amount = parseAmount(match.groupValues[1])
            val merchant = cleanMerchantName(match.groupValues[2])
            val digits = match.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() } ?: extractLastFourDigits(text)
            if (amount > 0.0) {
                return buildResult(pkg, bank, "EXPENSE", amount, merchant, digits, postTime)
            }
        }
        return null
    }

    private fun parseLulo(
        pkg: String,
        bank: SupportedColombianBank,
        text: String,
        postTime: Long
    ): ParsedBankNotification? {
        val compraRegex = Regex(
            """(?iu)compra\s+(?:con\s+tu\s+lulo\s+card\s+)?por\s+\$?\s*([0-9.,]+)\s+en\s+(.+?)(?:\.|$)"""
        )
        val match = compraRegex.find(text)
        if (match != null) {
            val amount = parseAmount(match.groupValues[1])
            val merchant = cleanMerchantName(match.groupValues[2])
            if (amount > 0.0) {
                return buildResult(pkg, bank, "EXPENSE", amount, merchant, null, postTime)
            }
        }
        return null
    }

    private fun extractLastFourDigits(text: String): String? {
        val regex = Regex("""(?:\*+|\.{2,}|terminada\s+en\s+)(\d{4})\b""")
        return regex.find(text)?.groupValues?.getOrNull(1)
    }

    private fun cleanMerchantName(raw: String): String {
        return raw
            .replace(Regex("""(?i)\s+(?:con\s+tu|desde|en\s+su|cta|cuenta|tarjeta).*$"""), "")
            .replace(Regex("""[.,;:]+$"""), "")
            .trim()
            .ifBlank { "Comercio no especificado" }
    }

    /**
     * Parsea montos monetarios flexibles colombianos (ej. "$125.400,00", "48.500,00", "$ 42,000", "50.000").
     */
    internal fun parseAmount(raw: String): Double {
        val cleaned = raw.replace("$", "").replace("COP", "", ignoreCase = true).replace(" ", "").trim()
        if (cleaned.isEmpty()) return 0.0

        val normalized = when {
            // Formato estándar colombiano: puntos para miles y coma para decimales (ej. 125.400,00)
            cleaned.matches(Regex("""^\d{1,3}(?:\.\d{3})+(?:,\d{1,2})?$""")) ->
                cleaned.replace(".", "").replace(",", ".")
            // Formato inverso con comas para miles y punto para decimales (ej. 125,400.00)
            cleaned.matches(Regex("""^\d{1,3}(?:,\d{3})+(?:\.\d{1,2})?$""")) ->
                cleaned.replace(",", "")
            // Múltiples puntos de miles sin decimales (ej. 1.250.000)
            cleaned.count { it == '.' } > 1 ->
                cleaned.replace(".", "")
            // Un solo punto seguido de 3 dígitos (ej. 48.500)
            cleaned.matches(Regex("""^\d{1,3}\.\d{3}$""")) ->
                cleaned.replace(".", "")
            // Coma decimal sin puntos (ej. 4500,50)
            cleaned.contains(",") && !cleaned.contains(".") ->
                cleaned.replace(",", ".")
            else -> cleaned
        }

        val parsed = normalized.toDoubleOrNull() ?: 0.0
        return (parsed * 100.0).roundToLong() / 100.0
    }

    private fun buildResult(
        pkg: String,
        bank: SupportedColombianBank,
        type: String,
        amount: Double,
        merchant: String,
        digits: String?,
        postTime: Long
    ): ParsedBankNotification {
        val merchantNormalized = merchant.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "").take(16)
        val timeBucket = postTime / 120_000L // Ventana de deduplicación de 2 minutos
        val idempotencyKey = "auto_notif_${pkg}_${timeBucket}_${amount.toLong()}_$merchantNormalized"

        return ParsedBankNotification(
            sourcePackage = pkg,
            detectedBank = bank,
            transactionType = type,
            amount = amount,
            currency = "COP",
            merchantOrCounterparty = merchant,
            lastFourDigits = digits,
            confidenceScore = 95,
            idempotencyKey = idempotencyKey,
            notificationTimestamp = postTime
        )
    }
}
