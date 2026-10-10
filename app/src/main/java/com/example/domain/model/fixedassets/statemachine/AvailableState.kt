package com.example.domain.model.fixedassets.statemachine

/**
 * State: DISPONIBLE (En Bodega / Ocioso / Sin Asignar)
 * Fully functional asset stored in warehouse awaiting reassignment to staff or department.
 */
data object AvailableState : AssetState {
    override val code: String = "DISPONIBLE"
    override val label: String = "Disponible (En Bodega)"
    override val shortBadgeLabel: String = "Disponible"
    override val colorHex: String = "#0284C7"

    override val allowsDepreciation: Boolean = true // IFRS: Idle assets continue depreciating
    override val allowsImprovements: Boolean = true
    override val allowsRevaluation: Boolean = true
    override val allowsDisposal: Boolean = true
    override val allowsMaintenance: Boolean = true
    override val isTerminal: Boolean = false
    override val isReadOnly: Boolean = false

    override val title: String = "Activo en Bodega / Ocioso (Norma NIIF)"
    override val financialRationale: String =
        "Un activo ocioso en depósito continúa perdiendo valor por el mero paso del tiempo y obsolescencia técnica. " +
        "La depreciación no se suspende; contablemente el gasto se redirige al centro de costos corporativo " +
        "o administrativo en lugar del área operativa previa."

    override val calculationEngineBehavior: String =
        "El motor CRON incluye el activo en las corridas mensuales periódicas de amortización."

    override val uiRestrictionsDescription: String =
        "• Habilita la acción rápida de 'Asignar a Responsable / Ubicación' para retornarlo a operación.\n" +
        "• Permite mantenimientos preventivos previos a la entrega y revisiones periódicas."

    override val triggerDescription: String =
        "Se dispara al desasignar un custodio o trasladar el bien a almacén central sin que presente averías."

    override val allowedTargetStates: List<String> = listOf(
        "EN_USO",
        "EN_MANTENIMIENTO",
        "EN_COMODATO",
        "MANTENIDO_PARA_LA_VENTA",
        "DADO_DE_BAJA"
    )
}
