package com.example.feature.versiongate.model

/**
 * Remote version control configuration retrieved from Firebase Firestore (app_config/version_control).
 */
data class VersionGatePolicy(
    val minSupportedVersionCode: Int = 1,
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0.0",
    val forceUpdateTitle: String = "Actualización Obligatoria de Seguridad",
    val forceUpdateMessage: String = "Tu versión actual de KashApp ya no es compatible con los protocolos de seguridad y conciliación de la plataforma. Por favor actualiza a la versión más reciente para continuar.",
    val updateUrl: String = "https://github.com/Andrewsscorp/KashAPP/releases",
    val isMaintenanceMode: Boolean = false,
    val maintenanceMessage: String = "El sistema se encuentra en mantenimiento preventivo. Por favor intenta más tarde."
)
