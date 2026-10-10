package com.example.domain.model.fixedassets.statemachine

/**
 * State: EN_COMODATO (En Préstamo / Cesión a Terceros o Clientes)
 * Asset owned by the enterprise operating under third-party custody or concession.
 */
data object OnLoanState : AssetState {
    override val code: String = "EN_COMODATO"
    override val label: String = "En comodato / Préstamo externo"
    override val shortBadgeLabel: String = "Comodato"
    override val colorHex: String = "#0D9488"

    override val allowsDepreciation: Boolean = true // Depreciation continues on company balance sheet
    override val allowsImprovements: Boolean = false // Outside premises
    override val allowsRevaluation: Boolean = true
    override val allowsDisposal: Boolean = true
    override val allowsMaintenance: Boolean = false // Internal physical maintenance blocked while in client's possession
    override val isTerminal: Boolean = false
    override val isReadOnly: Boolean = false

    override val title: String = "Activo en Comodato / Terceros (NIIF)"
    override val financialRationale: String =
        "El activo permanece en el patrimonio de la empresa y genera beneficios económicos indirectos (ej. fidelización, " +
        "concesión comercial de insumos). La depreciación contable continúa mes a mes sin interrupciones en los libros de la entidad."

    override val calculationEngineBehavior: String =
        "El motor CRON procesa el activo con cargo periódico al gasto o costo de comodatos."

    override val uiRestrictionsDescription: String =
        "• Bloquea mantenimientos físicos internos en talleres propios mientras se mantenga fuera de sede.\n" +
        "• Exige la vinculación de un Tercero Beneficiario y acta de entrega / comodato.\n" +
        "• Habilita la acción de 'Registrar Devolución a Sede' para reincorporarlo a 'EN_USO' o 'DISPONIBLE'."

    override val triggerDescription: String =
        "Firma de contrato de comodato o entrega de equipo a cliente/proveedor externo con acta de custodia."

    override val allowedTargetStates: List<String> = listOf(
        "EN_USO",
        "DISPONIBLE",
        "EN_MANTENIMIENTO",
        "EN_SINIESTRO"
    )
}
