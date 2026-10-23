package com.example.feature.notificaciones.service

import com.example.feature.impuestosdian.domain.model.TaxJurisdiction
import com.google.firebase.messaging.FirebaseMessaging

object FcmTopicSubscriptionManager {

    private const val TOPIC_GLOBAL = "kash_global_announcements"
    private const val TOPIC_TRM_ALERTS = "trm_cop_usd_alerts"

    fun syncTopicsForJurisdiction(activeJurisdiction: TaxJurisdiction, enableTrmAlerts: Boolean = true) {
        runCatching {
            val fcm = FirebaseMessaging.getInstance()
            fcm.subscribeToTopic(TOPIC_GLOBAL)

            if (enableTrmAlerts) {
                fcm.subscribeToTopic(TOPIC_TRM_ALERTS)
            } else {
                fcm.unsubscribeFromTopic(TOPIC_TRM_ALERTS)
            }

            // Suscribir solo al topic del país fiscal activo y desuscribir de los demás
            TaxJurisdiction.entries.forEach { jur ->
                val topicName = "tax_${jur.code.lowercase()}"
                if (jur == activeJurisdiction) {
                    fcm.subscribeToTopic(topicName)
                } else {
                    fcm.unsubscribeFromTopic(topicName)
                }
            }
        }
    }

    fun unsubscribeAllOnLogout() {
        runCatching {
            val fcm = FirebaseMessaging.getInstance()
            fcm.unsubscribeFromTopic(TOPIC_GLOBAL)
            fcm.unsubscribeFromTopic(TOPIC_TRM_ALERTS)
            TaxJurisdiction.entries.forEach { jur ->
                fcm.unsubscribeFromTopic("tax_${jur.code.lowercase()}")
            }
        }
    }
}
