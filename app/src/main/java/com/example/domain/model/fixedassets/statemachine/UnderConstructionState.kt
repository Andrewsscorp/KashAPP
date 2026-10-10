package com.example.domain.model.fixedassets.statemachine

/**
 * State: EN_CONSTRUCCION (Obra en Curso / En Montaje / Ensamblaje)
 * The asset is being fabricated or assembled. Costs are capitalized but depreciation is zero.
 */
data object UnderConstructionState : AssetState {
    override val code: String = "EN_CONSTRUCCION"
    override val label: String = "En construcción (Obra en curso)"
    override val shortBadgeLabel: String = "Obra en curso"
    override val colorHex: String = "#8B5CF6"

    override val allowsDepreciation: Boolean = false // Crucial: No depreciation until ready for intended use
    override val allowsImprovements: Boolean = true // Capitalizes materials, engineering and labor
    override val allowsRevaluation: Boolean = false
    override val allowsDisposal: Boolean = true
    override val allowsMaintenance: Boolean = false
    override val isTerminal: Boolean = false
    override val isReadOnly: Boolean = false

    override val title: String = "Obra en Curso / Montaje (NIC 16)"
    override val financialRationale: String =
        "El activo se encuentra en fase de construcción, ensamblaje o acondicionamiento. " +
        "Bajo la NIC 16, acumula y capitaliza todos los desembolsos necesarios para ponerlo en funcionamiento " +
        "(materiales, honorarios de ingeniería, fletes, montaje), pero NO inicia su depreciación " +
        "hasta encontrarse en la ubicación y condiciones necesarias para operar."

    override val calculationEngineBehavior: String =
        "El motor CRON omite por completo este activo. Las cuotas mensuales permanecen en $ 0. " +
        "El valor en libros permanece idéntico al acumulado de costos capitalizados."

    override val uiRestrictionsDescription: String =
        "• Permite registrar comprobantes y adiciones que aumenten el costo acumulado.\n" +
        "• Al concluir la obra, la acción 'Puesta en Marcha' lo transiciona a 'EN_USO' y fija la fecha de inicio de amortización."

    override val triggerDescription: String =
        "Alta inicial como proyecto de inversión patrimonial, edificación o instalación de maquinaria compleja."

    override val allowedTargetStates: List<String> = listOf(
        "EN_USO",
        "EN_SINIESTRO"
    )
}
