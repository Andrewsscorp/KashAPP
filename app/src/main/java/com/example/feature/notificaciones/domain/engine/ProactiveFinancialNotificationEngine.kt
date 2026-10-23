package com.example.feature.notificaciones.domain.engine

import com.example.data.local.entity.AccountCreditCardEntity
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.PendingAutoCaptureEntity
import com.example.data.local.entity.ScheduledPaymentEntity
import com.example.feature.impuestosdian.domain.model.DianTaxRadarReport
import com.example.feature.impuestosdian.domain.model.TaxAlertLevel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Locale

data class QuietHoursPolicy(
    val enabled: Boolean = true,
    val startHour24: Int = 22, // 10:00 PM
    val endHour24: Int = 7     // 7:00 AM
) {
    fun shouldSuppressSystemPush(currentTime: LocalTime, severity: String): Boolean {
        if (!enabled || severity.equals("CRITICAL", ignoreCase = true)) return false
        val hour = currentTime.hour
        return if (startHour24 > endHour24) {
            hour >= startHour24 || hour < endHour24
        } else {
            hour in startHour24 until endHour24
        }
    }
}

class ProactiveFinancialNotificationEngine {

    private val numberFormat = NumberFormat.getNumberInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }

    /**
     * Evalúa transversalmente y con aislamiento estricto por [userId]:
     * 1. Los 5 topes UVT del Radar Tributario DIAN (70% WARNING y 100% EXCEEDED)
     * 2. El consumo mensual del 4x1000 (350 UVT cuenta bancaria / 65 UVT billetera digital)
     * 3. Vencimiento cercano de Tarjetas de Crédito (0 a 3 días antes de la fecha límite de pago)
     * 4. Riesgo de sobregiro / iliquidez a 7 días cruzando pagos programados vs saldo disponible
     * 5. Movimientos bancarios pendientes de aprobación en Captura Automática
     */
    fun evaluateAllModules(
        userId: String,
        taxReport: DianTaxRadarReport?,
        accounts: List<AccountEntity>,
        creditCards: List<AccountCreditCardEntity>,
        scheduledPayments: List<ScheduledPaymentEntity>,
        pendingAutoCaptures: List<PendingAutoCaptureEntity>,
        today: LocalDate = LocalDate.now(),
        nowEpochMs: Long = System.currentTimeMillis()
    ): List<NotificationEntity> {
        val safeUser = userId.trim()
        if (safeUser.isBlank()) return emptyList()

        val generated = mutableListOf<NotificationEntity>()

        // Filtrar estrictamente todas las colecciones de entrada por safeUser para impedir mezcla multi-usuario
        val userAccounts = accounts.filter { it.userId == safeUser }
        val userScheduledPayments = scheduledPayments.filter { it.userId == safeUser }
        val userPendingAutoCaptures = pendingAutoCaptures.filter { it.userId == safeUser }

        // =========================================================================
        // 1. RADAR TRIBUTARIO DIAN: 5 TOPES OFICIALES EN UVT
        // =========================================================================
        if (taxReport != null && (taxReport.userId == safeUser || taxReport.userId.isBlank())) {
            val cur = taxReport.configProfile.currency
            val unit = taxReport.configProfile.taxUnitName
            val auth = taxReport.configProfile.jurisdiction.authorityAcronym

            taxReport.thresholds.forEach { threshold ->
                when (threshold.alertLevel) {
                    TaxAlertLevel.EXCEEDED -> {
                        generated += NotificationEntity(
                            id = "${safeUser}_notif_tax_exc_${threshold.type.name}_${taxReport.fiscalYear}",
                            userId = safeUser,
                            dedupKey = "TAX_${taxReport.fiscalYear}_${threshold.type.name}_EXCEEDED",
                            title = "🚨 Tope $auth superado: ${threshold.title}",
                            body = "Alcanzaste ${"%.1f".format(Locale.US, threshold.accumulatedUvt)} / ${threshold.limitUvt.toInt()} $unit ($${numberFormat.format(threshold.accumulatedCop)} $cur). Quedas obligado a presentar Declaración de Renta (${taxReport.estimatedFilingDeadlineDescription}).",
                            category = "TAX_DIAN",
                            severity = "CRITICAL",
                            sourceOrigin = "LOCAL_ENGINE",
                            deepLinkRoute = "RADAR_DIAN",
                            actionLabel = "Exportar Pre-Renta PDF",
                            createdAtEpochMs = nowEpochMs
                        )
                    }
                    TaxAlertLevel.WARNING -> {
                        generated += NotificationEntity(
                            id = "${safeUser}_notif_tax_warn_${threshold.type.name}_${taxReport.fiscalYear}",
                            userId = safeUser,
                            dedupKey = "TAX_${taxReport.fiscalYear}_${threshold.type.name}_WARNING",
                            title = "⚠️ Zona preventiva $auth (${"%.0f".format(Locale.US, threshold.consumptionPercentage)}%): ${threshold.title}",
                            body = "Te quedan $${numberFormat.format(threshold.remainingCopBeforeLimit)} $cur antes de cruzar el tope de ${threshold.limitUvt.toInt()} $unit.",
                            category = "TAX_DIAN",
                            severity = "WARNING",
                            sourceOrigin = "LOCAL_ENGINE",
                            deepLinkRoute = "RADAR_DIAN",
                            actionLabel = "Ver Radar Tributario",
                            createdAtEpochMs = nowEpochMs
                        )
                    }
                    TaxAlertLevel.SAFE -> Unit
                }
            }

            // =========================================================================
            // 2. CONTROL MENSUAL DEL 4x1000 (350 UVT BANCO / 65 UVT BILLETERAS)
            // =========================================================================
            taxReport.accountGmfStatuses.forEach { gmf ->
                if (gmf.gmfAlertLevel == TaxAlertLevel.EXCEEDED && gmf.estimatedGmfTaxPaidCop > 0.0) {
                    generated += NotificationEntity(
                        id = "${safeUser}_notif_gmf_exc_${gmf.accountId}_${taxReport.currentYearMonth}",
                        userId = safeUser,
                        dedupKey = "GMF_${taxReport.currentYearMonth}_${gmf.accountId}_EXCEEDED",
                        title = "💸 4x1000 activado en ${gmf.accountName} (${gmf.bankName})",
                        body = "Superaste el cupo exento mensual y has causado $${numberFormat.format(gmf.estimatedGmfTaxPaidCop)} $cur de impuesto bancario en ${taxReport.currentYearMonth}.",
                        category = "BANK_GMF",
                        severity = "WARNING",
                        sourceOrigin = "LOCAL_ENGINE",
                        deepLinkRoute = "RADAR_DIAN",
                        actionLabel = "Optimizar Cuentas 4x1000",
                        createdAtEpochMs = nowEpochMs
                    )
                } else if (gmf.gmfAlertLevel == TaxAlertLevel.WARNING && gmf.exemptLimitCop > 0.0) {
                    val pct = ((gmf.monthlyOutflowsCop / gmf.exemptLimitCop) * 100.0).toInt()
                    generated += NotificationEntity(
                        id = "${safeUser}_notif_gmf_warn_${gmf.accountId}_${taxReport.currentYearMonth}",
                        userId = safeUser,
                        dedupKey = "GMF_${taxReport.currentYearMonth}_${gmf.accountId}_WARNING",
                        title = "🏦 Cupo libre de 4x1000 al $pct% en ${gmf.accountName}",
                        body = "Solo te quedan $${numberFormat.format(gmf.remainingExemptCop)} $cur exentos este mes (${gmf.exemptLimitUvt.toInt()} $unit).",
                        category = "BANK_GMF",
                        severity = "INFO",
                        sourceOrigin = "LOCAL_ENGINE",
                        deepLinkRoute = "RADAR_DIAN",
                        actionLabel = "Revisar cupo",
                        createdAtEpochMs = nowEpochMs
                    )
                }
            }
        }

        // =========================================================================
        // 3. TARJETAS DE CRÉDITO: FECHA DE CORTE Y FECHA LÍMITE DE PAGO
        // =========================================================================
        val accountsById = userAccounts.associateBy { it.id }
        creditCards.forEach { card ->
            val parentAccount = accountsById[card.accountId] ?: return@forEach
            val currentDebt = kotlin.math.abs(parentAccount.balance.coerceAtMost(0.0))
            val paymentDay = card.paymentDueDay.coerceIn(1, 28)
            val dueDateThisMonth = LocalDate.of(today.year, today.month, paymentDay)
            val nextDueDate = if (dueDateThisMonth.isBefore(today)) dueDateThisMonth.plusMonths(1) else dueDateThisMonth
            val daysUntilDue = ChronoUnit.DAYS.between(today, nextDueDate)

            if (currentDebt > 0.0 && daysUntilDue in 0..3) {
                generated += NotificationEntity(
                    id = "${safeUser}_notif_cc_due_${card.accountId}_$nextDueDate",
                    userId = safeUser,
                    dedupKey = "CC_DUE_${card.accountId}_$nextDueDate",
                    title = "💳 Pago de ${parentAccount.name} vence en $daysUntilDue día(s)",
                    body = "Saldo pendiente: $${numberFormat.format(currentDebt)} ${parentAccount.currency}. Evita intereses de mora registrando el pago a tiempo.",
                    category = "CREDIT_CARD",
                    severity = "CRITICAL",
                    sourceOrigin = "LOCAL_ENGINE",
                    deepLinkRoute = "PAY_CREDIT_CARD",
                    actionLabel = "Pagar Tarjeta",
                    actionPayloadJson = "{\"accountId\":\"${card.accountId}\",\"amount\":$currentDebt}",
                    createdAtEpochMs = nowEpochMs
                )
            }
        }

        // =========================================================================
        // 4. RIESGO DE LIQUIDEZ PREDICTIVA A 7 DÍAS (PREVENCIÓN DE SOBREGIRO)
        // =========================================================================
        val sevenDaysAhead = today.plusDays(7)
        val activeScheduled = userScheduledPayments.filter { sp ->
            !sp.isPaid && !sp.isIncome && !sp.sourceAccountId.isNullOrBlank() &&
                runCatching { LocalDate.parse(sp.dueDate.take(10)) }.getOrNull()
                    ?.let { !it.isBefore(today) && !it.isAfter(sevenDaysAhead) } == true
        }

        activeScheduled.groupBy { it.sourceAccountId.orEmpty() }.forEach { (accId, payments) ->
            val account = accountsById[accId] ?: return@forEach
            if (account.type.equals("CREDIT_CARD", ignoreCase = true)) return@forEach
            val totalUpcoming = payments.sumOf { it.amount }
            if (totalUpcoming > account.balance) {
                val deficit = totalUpcoming - account.balance
                generated += NotificationEntity(
                    id = "${safeUser}_notif_liq_${accId}_$today",
                    userId = safeUser,
                    dedupKey = "LIQUIDITY_DEFICIT_${accId}_${today.year}_${today.monthValue}",
                    title = "⚡ Alerta de Liquidez en ${account.name}",
                    body = "Tus ${payments.size} pagos programados de los próximos 7 días suman $${numberFormat.format(totalUpcoming)} ${account.currency}, superando tu saldo disponible por $${numberFormat.format(deficit)} ${account.currency}.",
                    category = "LIQUIDITY_AI",
                    severity = "CRITICAL",
                    sourceOrigin = "LOCAL_ENGINE",
                    deepLinkRoute = "DASHBOARD",
                    actionLabel = "Transferir Fondos",
                    createdAtEpochMs = nowEpochMs
                )
            }
        }

        // =========================================================================
        // 5. CAPTURA AUTOMÁTICA BANCARIA PENDIENTE DE CONFIRMACIÓN
        // =========================================================================
        val pendingList = userPendingAutoCaptures.filter { it.status.equals("PENDING", ignoreCase = true) }
        val pendingCount = pendingList.size
        if (pendingCount > 0) {
            val latest = pendingList.first()
            generated += NotificationEntity(
                id = "${safeUser}_notif_autocap_${latest.id}",
                userId = safeUser,
                dedupKey = "AUTOCAP_PENDING_${latest.id}",
                title = "📲 $pendingCount movimiento(s) bancario(s) detectado(s)",
                body = "Último: ${latest.merchantOrCounterparty} por $${numberFormat.format(latest.amount)} (${latest.detectedBankName}). Confírmalo en 1 toque para actualizar tus saldos y topes DIAN.",
                category = "AUTO_CAPTURE",
                severity = "INFO",
                sourceOrigin = "LOCAL_ENGINE",
                deepLinkRoute = "AUTO_CAPTURE",
                actionLabel = "Revisar y Confirmar",
                createdAtEpochMs = nowEpochMs
            )
        }

        return generated
    }
}
