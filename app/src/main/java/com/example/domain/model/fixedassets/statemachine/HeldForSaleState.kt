package com.example.domain.model.fixedassets.statemachine

/**
 * State: MANTENIDO_PARA_LA_VENTA (Norma Internacional NIIF 5)
 * Non-current assets held for sale and discontinued operations.
 */
data object HeldForSaleState : AssetState {
    override val code: String = "MANTENIDO_PARA_LA_VENTA"
    override val label: String = "Mantenido para la venta (NIIF 5)"
    override val shortBadgeLabel: String = "En venta (NIIF 5)"
    override val colorHex: String = "#EAB308"

    override val allowsDepreciation: Boolean = false // IFRS 5: Depreciation ceases immediately
    override val allowsImprovements: Boolean = false // Blocked: capital additions prohibited
    override val allowsRevaluation: Boolean = false // Blocked: measured at lower of book value or fair value less costs to sell
    override val allowsDisposal: Boolean = true
    override val allowsMaintenance: Boolean = false
    override val isTerminal: Boolean = false
    override val isReadOnly: Boolean = false

    override val title: String = "Mantenido para la Venta (NIIF 5)"
    override val financialRationale: String =
        "La dirección de la entidad aprobó un plan firme para enajenar el bien y la venta es altamente probable. " +
        "Bajo la NIIF 5, la depreciación CESA INMEDIATAMENTE al clasificarse en este estado. " +
        "El bien se reclasifica contablemente de Activo No Corriente a Activo Corriente y se valúa " +
        "al menor entre su valor en libros y su valor razonable menos costos de venta."

    override val calculationEngineBehavior: String =
        "Corta y detiene permanentemente el cronograma de amortización desde el mes de adopción de la política."

    override val uiRestrictionsDescription: String =
        "• BLOQUEADO: 'Mejora / Adición' y 'Revalúo Técnico'.\n" +
        "• Habilita el flujo de 'Formalizar Venta / Enajenación' capturando el precio final y calculando la utilidad o pérdida contable."

    override val triggerDescription: String =
        "Aprobación de la junta directiva para desincorporación comercial del bien en un plazo inferior a 12 meses."

    override val allowedTargetStates: List<String> = listOf(
        "DADO_DE_BAJA",
        "EN_USO" // If sale agreement is cancelled and asset returns to service
    )
}
