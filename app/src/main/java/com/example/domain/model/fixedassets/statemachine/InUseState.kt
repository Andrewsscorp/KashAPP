package com.example.domain.model.fixedassets.statemachine

/**
 * State: EN_USO (Operativo)
 * The natural operating state of an active asset generating economic benefits.
 */
data object InUseState : AssetState {
    override val code: String = "EN_USO"
    override val label: String = "En uso (Operativo)"
    override val shortBadgeLabel: String = "En uso"
    override val colorHex: String = "#10B981"

    override val allowsDepreciation: Boolean = true
    override val allowsImprovements: Boolean = true
    override val allowsRevaluation: Boolean = true
    override val allowsDisposal: Boolean = true
    override val allowsMaintenance: Boolean = true
    override val isTerminal: Boolean = false
    override val isReadOnly: Boolean = false

    override val title: String = "Estado Operativo Activo (NIIF / NIC 16)"
    override val financialRationale: String =
        "El activo está en condiciones de operar y generando flujo económico para la empresa. " +
        "Conforme a la NIC 16, la depreciación se calcula y consume normalmente mes a mes sobre la base amortizable."

    override val calculationEngineBehavior: String =
        "El motor contable procesa normalmente el activo en cada cierre mensual o corrida por lotes. " +
        "La cuota de depreciación reduce el valor en libros e incrementa la depreciación acumulada."

    override val uiRestrictionsDescription: String =
        "Todas las acciones operativas están habilitadas: Registrar Mantenimiento, Registrar Mejora / Adición capitalizable, " +
        "Revalúo Técnico pericial, Ajustes por Deterioro y Solicitud de Baja."

    override val triggerDescription: String =
        "Se asigna al dar de alta el activo, recibir importaciones en tránsito, culminar una obra en curso, o finalizar exitosamente un mantenimiento."

    override val allowedTargetStates: List<String> = listOf(
        "EN_MANTENIMIENTO",
        "DISPONIBLE",
        "EN_COMODATO",
        "MANTENIDO_PARA_LA_VENTA",
        "EN_SINIESTRO",
        "DADO_DE_BAJA"
    )
}
