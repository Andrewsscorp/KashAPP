package com.example.domain.model.fixedassets.statemachine

/**
 * State: EN_TRANSITO (En Ruta / Importación en Tránsito)
 * Invoiced asset with transferred risks/rewards, shipping to company premises.
 */
data object InTransitState : AssetState {
    override val code: String = "EN_TRANSITO"
    override val label: String = "En tránsito (Importación / Ruta)"
    override val shortBadgeLabel: String = "En tránsito"
    override val colorHex: String = "#6366F1"

    override val allowsDepreciation: Boolean = false // Wear has not started
    override val allowsImprovements: Boolean = false
    override val allowsRevaluation: Boolean = false
    override val allowsDisposal: Boolean = true
    override val allowsMaintenance: Boolean = false
    override val isTerminal: Boolean = false
    override val isReadOnly: Boolean = false

    override val title: String = "Activo en Tránsito / Importación (NIIF)"
    override val financialRationale: String =
        "El bien ya ha sido facturado con traspaso de riesgos y beneficios legales (Incoterm acordado). " +
        "Figura en el balance general como activo no corriente en tránsito, pero su desgaste físico " +
        "u operacional aún no ha comenzado. La depreciación se mantiene formalmente congelada."

    override val calculationEngineBehavior: String =
        "Excluido de los cargos mensuales de amortización hasta que se realice el acta de recepción física."

    override val uiRestrictionsDescription: String =
        "• Bloquea mantenimientos y revalúos por encontrarse fuera de instalaciones.\n" +
        "• Presenta el botón de acción 'Registrar Recepción / Poner en Uso' para activar el cronograma."

    override val triggerDescription: String =
        "Registrado al confirmar la compra o despacho de importación con guía de transporte / B/L."

    override val allowedTargetStates: List<String> = listOf(
        "EN_USO",
        "DISPONIBLE",
        "EN_SINIESTRO"
    )
}
