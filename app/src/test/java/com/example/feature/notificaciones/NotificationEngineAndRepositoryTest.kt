package com.example.feature.notificaciones

import com.example.data.local.entity.AccountCreditCardEntity
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.PendingAutoCaptureEntity
import com.example.data.local.entity.ScheduledPaymentEntity
import com.example.feature.impuestosdian.domain.model.AccountGmfMonthlyStatus
import com.example.feature.impuestosdian.domain.model.ColombianUvtCatalog
import com.example.feature.impuestosdian.domain.model.DianCriterionType
import com.example.feature.impuestosdian.domain.model.DianTaxRadarReport
import com.example.feature.impuestosdian.domain.model.DianThresholdStatus
import com.example.feature.impuestosdian.domain.model.ElectronicInvoiceBenefitSummary
import com.example.feature.impuestosdian.domain.model.TaxAlertLevel
import com.example.feature.notificaciones.domain.engine.ProactiveFinancialNotificationEngine
import com.example.feature.notificaciones.domain.engine.QuietHoursPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class NotificationEngineAndRepositoryTest {

    private val engine = ProactiveFinancialNotificationEngine()
    private val uvt2026 = ColombianUvtCatalog.UVT_2026_DEFAULT_COP

    private fun sampleTaxReport(
        userId: String = "u1",
        thresholdAlert: TaxAlertLevel = TaxAlertLevel.SAFE,
        gmfAlert: TaxAlertLevel = TaxAlertLevel.SAFE,
        gmfTaxPaid: Double = 0.0
    ): DianTaxRadarReport {
        val threshold = DianThresholdStatus(
            type = DianCriterionType.INGRESOS_BRUTOS,
            title = "Ingresos Brutos del Año",
            legalReference = "Art. 592 E.T.",
            accumulatedCop = if (thresholdAlert == TaxAlertLevel.EXCEEDED) 80_000_000.0 else 60_000_000.0,
            accumulatedUvt = if (thresholdAlert == TaxAlertLevel.EXCEEDED) 1527.0 else 1145.0,
            limitUvt = 1400.0,
            limitCop = 1400.0 * uvt2026,
            consumptionPercentage = if (thresholdAlert == TaxAlertLevel.EXCEEDED) 109.0 else 81.8,
            remainingCopBeforeLimit = if (thresholdAlert == TaxAlertLevel.EXCEEDED) 0.0 else 13_323_600.0,
            alertLevel = thresholdAlert
        )
        val gmfStatus = AccountGmfMonthlyStatus(
            accountId = "acc_1",
            accountName = "Ahorros Principal",
            bankName = "Bancolombia",
            accountType = "SAVINGS",
            isMarkedTaxExempt350Uvt = true,
            isLowAmountDigitalWallet65Uvt = false,
            monthlyOutflowsCop = 16_000_000.0,
            monthlyOutflowsUvt = 305.5,
            exemptLimitUvt = 350.0,
            exemptLimitCop = 350.0 * uvt2026,
            remainingExemptCop = 2_330_900.0,
            taxableExcessCop = if (gmfTaxPaid > 0) 2_000_000.0 else 0.0,
            estimatedGmfTaxPaidCop = gmfTaxPaid,
            gmfAlertLevel = gmfAlert
        )
        val fe = ElectronicInvoiceBenefitSummary(0.0, 0.0, 0.0, 240.0, 240.0 * uvt2026, 0.0, 240.0 * uvt2026, 0)

        return DianTaxRadarReport(
            userId = userId,
            fiscalYear = 2026,
            currentYearMonth = "2026-10",
            uvtValueCop = uvt2026,
            lastTwoDigitsNitOrCedula = "61",
            estimatedFilingDeadlineDescription = "Cuarta semana de Septiembre de 2027",
            thresholds = listOf(threshold),
            isObligatedToDeclareRenta = thresholdAlert == TaxAlertLevel.EXCEEDED,
            exceededThresholdsCount = if (thresholdAlert == TaxAlertLevel.EXCEEDED) 1 else 0,
            warningThresholdsCount = if (thresholdAlert == TaxAlertLevel.WARNING) 1 else 0,
            accountGmfStatuses = listOf(gmfStatus),
            totalMonthlyGmfTaxCop = gmfTaxPaid,
            hasMarked350UvtAccount = true,
            optimizationRecommendation = "OK",
            electronicInvoiceBenefit = fe
        )
    }

    @Test
    fun `TEST-NOTIF-01 Tope DIAN en EXCEEDED genera notificacion CRITICAL con deepLink RADAR_DIAN`() {
        val report = sampleTaxReport(thresholdAlert = TaxAlertLevel.EXCEEDED)
        val notifications = engine.evaluateAllModules(
            userId = "u1",
            taxReport = report,
            accounts = emptyList(),
            creditCards = emptyList(),
            scheduledPayments = emptyList(),
            pendingAutoCaptures = emptyList()
        )

        assertEquals(1, notifications.size)
        val item = notifications.first()
        assertEquals("CRITICAL", item.severity)
        assertEquals("TAX_DIAN", item.category)
        assertEquals("RADAR_DIAN", item.deepLinkRoute)
        assertEquals("TAX_2026_INGRESOS_BRUTOS_EXCEEDED", item.dedupKey)
    }

    @Test
    fun `TEST-NOTIF-02 Tope DIAN en WARNING genera alerta preventiva con monto restante`() {
        val report = sampleTaxReport(thresholdAlert = TaxAlertLevel.WARNING)
        val notifications = engine.evaluateAllModules(
            userId = "u1",
            taxReport = report,
            accounts = emptyList(),
            creditCards = emptyList(),
            scheduledPayments = emptyList(),
            pendingAutoCaptures = emptyList()
        )

        assertEquals(1, notifications.size)
        val item = notifications.first()
        assertEquals("WARNING", item.severity)
        assertTrue(item.body.contains("13.323.600"))
    }

    @Test
    fun `TEST-NOTIF-03 Exceso de 4x1000 mensual genera alerta BANK_GMF con impuesto causado`() {
        val report = sampleTaxReport(gmfAlert = TaxAlertLevel.EXCEEDED, gmfTaxPaid = 8_000.0)
        val notifications = engine.evaluateAllModules(
            userId = "u1",
            taxReport = report,
            accounts = emptyList(),
            creditCards = emptyList(),
            scheduledPayments = emptyList(),
            pendingAutoCaptures = emptyList()
        )

        val gmfNotif = notifications.first { it.category == "BANK_GMF" }
        assertEquals("WARNING", gmfNotif.severity)
        assertTrue(gmfNotif.body.contains("8.000"))
    }

    @Test
    fun `TEST-NOTIF-04 Alerta preventiva de 4x1000 al superar 75 por ciento del cupo de 350 UVT`() {
        val report = sampleTaxReport(gmfAlert = TaxAlertLevel.WARNING, gmfTaxPaid = 0.0)
        val notifications = engine.evaluateAllModules(
            userId = "u1",
            taxReport = report,
            accounts = emptyList(),
            creditCards = emptyList(),
            scheduledPayments = emptyList(),
            pendingAutoCaptures = emptyList()
        )

        val gmfNotif = notifications.first { it.category == "BANK_GMF" }
        assertEquals("INFO", gmfNotif.severity)
        assertTrue(gmfNotif.title.contains("87%"))
    }

    @Test
    fun `TEST-NOTIF-05 Tarjeta de credito con deuda que vence en 2 dias dispara alerta CRITICAL`() {
        val ccAccount = AccountEntity(
            id = "acc_cc_1",
            userId = "u1",
            name = "Visa Signature",
            balance = -4_500_000.0,
            currency = "COP",
            type = "CREDIT_CARD"
        )
        val ccDetails = AccountCreditCardEntity(
            accountId = "acc_cc_1",
            cutOffDay = 5,
            paymentDueDay = 24
        )

        val notifications = engine.evaluateAllModules(
            userId = "u1",
            taxReport = null,
            accounts = listOf(ccAccount),
            creditCards = listOf(ccDetails),
            scheduledPayments = emptyList(),
            pendingAutoCaptures = emptyList(),
            today = LocalDate.of(2026, 10, 22)
        )

        assertEquals(1, notifications.size)
        val ccNotif = notifications.first()
        assertEquals("CREDIT_CARD", ccNotif.category)
        assertEquals("CRITICAL", ccNotif.severity)
        assertEquals("PAY_CREDIT_CARD", ccNotif.deepLinkRoute)
        assertTrue(ccNotif.body.contains("4.500.000"))
    }

    @Test
    fun `TEST-NOTIF-06 Tarjeta de credito con saldo 0 NO genera alerta aunque este en fecha de pago`() {
        val paidCcAccount = AccountEntity(
            id = "acc_cc_1",
            userId = "u1",
            name = "Visa Signature",
            balance = 0.0,
            currency = "COP",
            type = "CREDIT_CARD"
        )
        val ccDetails = AccountCreditCardEntity(accountId = "acc_cc_1", paymentDueDay = 24)

        val notifications = engine.evaluateAllModules(
            userId = "u1",
            taxReport = null,
            accounts = listOf(paidCcAccount),
            creditCards = listOf(ccDetails),
            scheduledPayments = emptyList(),
            pendingAutoCaptures = emptyList(),
            today = LocalDate.of(2026, 10, 22)
        )

        assertTrue(notifications.isEmpty())
    }

    @Test
    fun `TEST-NOTIF-07 Pagos programados en proximos 7 dias mayores al saldo disparan alerta de Liquidez`() {
        val checking = AccountEntity(
            id = "acc_chk",
            userId = "u1",
            name = "Cuenta Nómina",
            balance = 1_200_000.0,
            currency = "COP",
            type = "CHECKING"
        )
        val rentPayment = ScheduledPaymentEntity(
            id = "sp_1",
            userId = "u1",
            entityName = "Arriendo Apartamento",
            category = "ARRIENDO",
            amount = 2_000_000.0,
            currency = "COP",
            dueDate = "2026-10-25",
            recurrence = "MONTHLY",
            isIncome = false,
            isPaid = false,
            sourceAccountId = "acc_chk"
        )

        val notifications = engine.evaluateAllModules(
            userId = "u1",
            taxReport = null,
            accounts = listOf(checking),
            creditCards = emptyList(),
            scheduledPayments = listOf(rentPayment),
            pendingAutoCaptures = emptyList(),
            today = LocalDate.of(2026, 10, 22)
        )

        assertEquals(1, notifications.size)
        val liq = notifications.first()
        assertEquals("LIQUIDITY_AI", liq.category)
        assertEquals("CRITICAL", liq.severity)
        assertTrue(liq.body.contains("800.000"))
    }

    @Test
    fun `TEST-NOTIF-08 Captura automatica bancaria pendiente genera notificacion AUTO_CAPTURE`() {
        val pending = PendingAutoCaptureEntity(
            id = "cap_99",
            userId = "u1",
            sourcePackage = "com.todo1.mobile",
            detectedBankName = "Bancolombia",
            transactionType = "EXPENSE",
            amount = 185_000.0,
            currency = "COP",
            merchantOrCounterparty = "Exito Calle 80",
            status = "PENDING",
            idempotencyKey = "idem_cap_99",
            notificationTimestamp = 1700000000L
        )

        val notifications = engine.evaluateAllModules(
            userId = "u1",
            taxReport = null,
            accounts = emptyList(),
            creditCards = emptyList(),
            scheduledPayments = emptyList(),
            pendingAutoCaptures = listOf(pending)
        )

        assertEquals(1, notifications.size)
        val capNotif = notifications.first()
        assertEquals("AUTO_CAPTURE", capNotif.category)
        assertEquals("AUTO_CAPTURE", capNotif.deepLinkRoute)
        assertTrue(capNotif.body.contains("Exito Calle 80"))
    }

    @Test
    fun `TEST-NOTIF-09 QuietHoursPolicy silencia alertas INFO y WARNING en horario nocturno pero permite CRITICAL`() {
        val policy = QuietHoursPolicy(enabled = true, startHour24 = 22, endHour24 = 7)
        val nightTime = LocalTime.of(23, 30)
        val dayTime = LocalTime.of(14, 15)

        assertTrue(policy.shouldSuppressSystemPush(nightTime, "INFO"))
        assertTrue(policy.shouldSuppressSystemPush(nightTime, "WARNING"))
        assertFalse(policy.shouldSuppressSystemPush(nightTime, "CRITICAL"))
        assertFalse(policy.shouldSuppressSystemPush(dayTime, "INFO"))
    }

    @Test
    fun `TEST-NOTIF-10 DedupKey es determinista e idempotente para evitar alertas repetidas en el mismo periodo`() {
        val report = sampleTaxReport(thresholdAlert = TaxAlertLevel.EXCEEDED)
        val firstRun = engine.evaluateAllModules("u1", report, emptyList(), emptyList(), emptyList(), emptyList())
        val secondRun = engine.evaluateAllModules("u1", report, emptyList(), emptyList(), emptyList(), emptyList())

        assertEquals(firstRun.first().dedupKey, secondRun.first().dedupKey)
    }

    @Test
    fun `TEST-NOTIF-11 Aislamiento Multi-Usuario impide que datos de otro usuario generen alertas en la sesion activa`() {
        val otherUserReport = sampleTaxReport(userId = "user_B", thresholdAlert = TaxAlertLevel.EXCEEDED)
        val otherUserAccount = AccountEntity(
            id = "acc_cc_other",
            userId = "user_B",
            name = "Mastercard Black",
            balance = -9_000_000.0,
            currency = "COP",
            type = "CREDIT_CARD"
        )
        val otherUserCard = AccountCreditCardEntity(accountId = "acc_cc_other", paymentDueDay = 24)
        val otherUserCapture = PendingAutoCaptureEntity(
            id = "cap_other",
            userId = "user_B",
            sourcePackage = "com.davivienda",
            detectedBankName = "Davivienda",
            transactionType = "EXPENSE",
            amount = 500_000.0,
            merchantOrCounterparty = "Carulla",
            status = "PENDING",
            idempotencyKey = "idem_other",
            notificationTimestamp = 1700000000L
        )

        val notificationsForUserA = engine.evaluateAllModules(
            userId = "user_A",
            taxReport = otherUserReport,
            accounts = listOf(otherUserAccount),
            creditCards = listOf(otherUserCard),
            scheduledPayments = emptyList(),
            pendingAutoCaptures = listOf(otherUserCapture),
            today = LocalDate.of(2026, 10, 22)
        )

        assertTrue(
            "Ninguna alerta de user_B puede filtrarse en la bandeja de user_A",
            notificationsForUserA.isEmpty()
        )
    }

    @Test
    fun `TEST-NOTIF-12 Claves primarias id incluyen prefijo de userId para evitar colisiones entre dos usuarios en el mismo dispositivo`() {
        val reportUserA = sampleTaxReport(userId = "user_A", thresholdAlert = TaxAlertLevel.EXCEEDED)
        val reportUserB = sampleTaxReport(userId = "user_B", thresholdAlert = TaxAlertLevel.EXCEEDED)

        val notifsA = engine.evaluateAllModules("user_A", reportUserA, emptyList(), emptyList(), emptyList(), emptyList())
        val notifsB = engine.evaluateAllModules("user_B", reportUserB, emptyList(), emptyList(), emptyList(), emptyList())

        assertEquals(1, notifsA.size)
        assertEquals(1, notifsB.size)
        assertEquals("user_A", notifsA.first().userId)
        assertEquals("user_B", notifsB.first().userId)
        assertNotEquals(
            "Los IDs primarios en Room deben ser distintos para cada usuario",
            notifsA.first().id,
            notifsB.first().id
        )
        assertTrue(notifsA.first().id.startsWith("user_A_"))
        assertTrue(notifsB.first().id.startsWith("user_B_"))
    }

    @Test
    fun `TEST-NOTIF-13 Sanitizacion para pantalla de bloqueo (ISO 27001) enmascara montos de dinero y numeros de tarjeta`() {
        val rawBody = "Tarjeta Visa *4512 registró una compra por $ 1.450.000 COP en Éxito. Saldo disponible al día."
        val sanitized = com.example.feature.notificaciones.dispatcher.RichSystemNotificationDispatcher.sanitizeForLockScreen(rawBody)

        assertFalse("El texto sanitizado no debe exponer la cifra exacta de dinero", sanitized.contains("1.450.000"))
        assertFalse("El texto sanitizado no debe exponer los dígitos de la tarjeta", sanitized.contains("4512"))
        assertTrue("El texto sanitizado debe contener el indicador de confidencialidad", sanitized.contains("$ ••••••"))
    }

    @Test
    fun `TEST-NOTIF-14 QuietHoursPolicy suprime alertas no criticas durante la noche pero permite sobregiro o alertas CRITICAL`() {
        val policy = QuietHoursPolicy(enabled = true, startHour24 = 22, endHour24 = 7)

        // A las 23:30 (noche): no crítica se suprime, crítica atraviesa
        assertTrue("Alerta normal a las 23:30 debe ser silenciada", policy.shouldSuppressSystemPush(LocalTime.of(23, 30), "DEFAULT"))
        assertFalse("Alerta CRITICAL a las 23:30 debe sonar", policy.shouldSuppressSystemPush(LocalTime.of(23, 30), "CRITICAL"))

        // A las 03:15 (madrugada): no crítica se suprime, crítica atraviesa
        assertTrue("Alerta normal a las 03:15 debe ser silenciada", policy.shouldSuppressSystemPush(LocalTime.of(3, 15), "DEFAULT"))
        assertFalse("Alerta CRITICAL a las 03:15 debe sonar", policy.shouldSuppressSystemPush(LocalTime.of(3, 15), "CRITICAL"))

        // A las 14:00 (tarde): ninguna se suprime
        assertFalse("Alerta normal a las 14:00 no debe ser silenciada", policy.shouldSuppressSystemPush(LocalTime.of(14, 0), "DEFAULT"))
        assertFalse("Alerta CRITICAL a las 14:00 no debe ser silenciada", policy.shouldSuppressSystemPush(LocalTime.of(14, 0), "CRITICAL"))
    }

    @Test
    fun `TEST-NOTIF-15 Canales de notificaciones estan configurados formalmente para respetar prioridades de Android`() {
        assertEquals("kash_security_critical", com.example.feature.notificaciones.dispatcher.RichSystemNotificationDispatcher.CHANNEL_SECURITY_CRITICAL)
        assertEquals("kash_payments_due", com.example.feature.notificaciones.dispatcher.RichSystemNotificationDispatcher.CHANNEL_PAYMENTS_DUE)
        assertEquals("kash_budgets_goals", com.example.feature.notificaciones.dispatcher.RichSystemNotificationDispatcher.CHANNEL_BUDGETS_GOALS)
        assertEquals("kash_tax_and_assets", com.example.feature.notificaciones.dispatcher.RichSystemNotificationDispatcher.CHANNEL_TAX_AND_ASSETS)
        assertEquals("kash_weekly_digest", com.example.feature.notificaciones.dispatcher.RichSystemNotificationDispatcher.CHANNEL_WEEKLY_DIGEST)
    }

    @Test
    fun `TEST-NOTIF-16 Modelo de Dominio NotificationSettingsConfig contiene valores por defecto bancarios consistentes`() {
        val config = com.example.features.settings.domain.model.NotificationSettingsConfig()

        assertTrue("Presupuestos activos por defecto", config.budgetPolicy.isEnabled)
        assertEquals("Umbral preventivo por defecto 80%", 80, config.budgetPolicy.warningThresholdPercent)
        assertTrue("Tarjetas activas por defecto", config.paymentPolicy.isEnabled)
        assertTrue("Aviso a 3 días incluido", config.paymentPolicy.daysBeforeDueDate.contains(3))
        assertTrue("Ocultar saldos en pantalla de bloqueo activo por defecto (ISO 27001)", config.privacyPolicy.maskAmountsOnLockScreen)
    }
}
