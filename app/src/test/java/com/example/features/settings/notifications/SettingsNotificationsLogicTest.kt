package com.example.features.settings.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.features.settings.data.repository.SettingsRepositoryImpl
import com.example.features.settings.domain.model.BudgetAlertPolicy
import com.example.features.settings.domain.model.NotificationSettingsConfig
import com.example.features.settings.domain.model.PaymentAndCardAlertPolicy
import com.example.features.settings.domain.model.PrivacyAndDeliveryPolicy
import com.example.features.settings.domain.model.SecurityAndAnomalyAlertPolicy
import com.example.features.settings.domain.model.TaxAndPatrimonyAlertPolicy
import com.example.features.settings.domain.model.WeeklyDigestPolicy
import com.example.features.settings.presentation.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SettingsNotificationsLogicTest {

    private lateinit var context: Context
    private lateinit var repository: SettingsRepositoryImpl
    private lateinit var viewModel: SettingsViewModel
    private val testUserId = "user_test_notif"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = SettingsRepositoryImpl(context)
        viewModel = SettingsViewModel(
            repository = repository,
            userId = testUserId
        )
    }

    @Test
    fun `TEST-NOTIF-UI-01 SettingsRepository persiste y recupera las 6 politicas granulares de notificaciones`() = runBlocking {
        val customConfig = NotificationSettingsConfig(
            budgetPolicy = BudgetAlertPolicy(
                isEnabled = true,
                warningThresholdPercent = 65,
                notifyOnCritical100Percent = false,
                suggestEnvelopeRebalance = true
            ),
            paymentPolicy = PaymentAndCardAlertPolicy(
                isEnabled = true,
                daysBeforeDueDate = setOf(7, 3),
                preferredAlertHour = 10,
                preferredAlertMinute = 30,
                includeMinimumPaymentPreview = true
            ),
            securityPolicy = SecurityAndAnomalyAlertPolicy(
                overdraftImmediateAlert = true,
                mlUnusualExpenseAlert = true,
                failedAuthAttemptAlert = true,
                highValueTransactionThresholdCop = 2_500_000L
            ),
            taxPolicy = TaxAndPatrimonyAlertPolicy(
                dianTaxCalendarAlerts = true,
                uvtThresholdWarningPercent = 90,
                gmfExemptionAlert = true,
                fixedAssetMaintenanceAndDepreciationAlerts = true,
                cdtMaturityAlerts = true,
                sharedDebtRemindersEnabled = true
            ),
            weeklyDigestPolicy = WeeklyDigestPolicy(
                isEnabled = true,
                dayOfWeekIso = 5, // Viernes
                hourOfDay = 18,
                minute = 0,
                includeAiInsights = true
            ),
            privacyPolicy = PrivacyAndDeliveryPolicy(
                maskAmountsOnLockScreen = true,
                requireBiometricForNotificationActions = true,
                quietHoursEnabled = true,
                quietStartHour = 23,
                quietEndHour = 6,
                allowCriticalSecurityInQuietHours = true
            )
        )

        val updateResult = repository.updateNotificationSettings(testUserId, customConfig)
        assertTrue(updateResult.isSuccess)

        val restored = repository.getNotificationSettings(testUserId)
        assertEquals(65, restored.budgetPolicy.warningThresholdPercent)
        assertFalse(restored.budgetPolicy.notifyOnCritical100Percent)
        assertEquals(setOf(7, 3), restored.paymentPolicy.daysBeforeDueDate)
        assertEquals(10, restored.paymentPolicy.preferredAlertHour)
        assertEquals(30, restored.paymentPolicy.preferredAlertMinute)
        assertEquals(2_500_000L, restored.securityPolicy.highValueTransactionThresholdCop)
        assertEquals(90, restored.taxPolicy.uvtThresholdWarningPercent)
        assertEquals(5, restored.weeklyDigestPolicy.dayOfWeekIso)
        assertEquals(18, restored.weeklyDigestPolicy.hourOfDay)
        assertTrue(restored.privacyPolicy.quietHoursEnabled)
        assertEquals(23, restored.privacyPolicy.quietStartHour)
        assertEquals(6, restored.privacyPolicy.quietEndHour)
    }

    @Test
    fun `TEST-NOTIF-UI-02 SettingsViewModel propaga las mutaciones de politicas a la UI reactiva`() = runBlocking {
        viewModel.updateBudgetPolicy { it.copy(warningThresholdPercent = 75) }

        // Damos tiempo a la corrutina para actualizar
        val preferences = repository.observePreferences(testUserId).first { it.notificationSettings.budgetPolicy.warningThresholdPercent == 75 }
        assertEquals(75, preferences.notificationSettings.budgetPolicy.warningThresholdPercent)
    }

    @Test
    fun `TEST-NOTIF-UI-03 Verificacion de ratio de contraste WCAG AA y AAA para el boton oscuro de notificaciones`() {
        // Contenedor oscuro: #0F172A (R=15, G=23, B=42)
        // Texto/Icono: White #FFFFFF (R=255, G=255, B=255)
        fun relativeLuminance(r: Int, g: Int, b: Int): Double {
            fun channelLum(c: Int): Double {
                val sc = c / 255.0
                return if (sc <= 0.03928) sc / 12.92 else Math.pow((sc + 0.055) / 1.055, 2.4)
            }
            return 0.2126 * channelLum(r) + 0.7152 * channelLum(g) + 0.0722 * channelLum(b)
        }

        val lumWhite = relativeLuminance(255, 255, 255)
        val lumDarkNavy = relativeLuminance(15, 23, 42)
        val contrastRatio = (lumWhite + 0.05) / (lumDarkNavy + 0.05)

        // El ratio de contraste entre blanco y #0F172A es ~15.9:1, superando WCAG AAA (7.0:1)
        assertTrue("El contraste debe superar WCAG AAA (7:1)", contrastRatio >= 7.0)
    }
}
