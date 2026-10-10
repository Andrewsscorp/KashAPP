package com.example.domain.model.fixedassets.statemachine

/**
 * State: EN_MANTENIMIENTO (En Taller / Arreglo)
 * The asset is temporarily out of service undergoing corrective or preventive overhaul.
 */
data object InMaintenanceState : AssetState {
    override val code: String = "EN_MANTENIMIENTO"
    override val label: String = "En mantenimiento"
    override val shortBadgeLabel: String = "En taller"
    override val colorHex: String = "#F59E0B"

    override val allowsDepreciation: Boolean = true // IFRS/NIIF rule: depreciation does NOT pause during maintenance
    override val allowsImprovements: Boolean = false // Blocked: an asset under repair cannot receive capital additions
    override val allowsRevaluation: Boolean = false // Blocked: disassembled or repaired asset cannot be appraised
    override val allowsDisposal: Boolean = true // Allowed: if technician declares total unrepairable loss
    override val allowsMaintenance: Boolean = true
    override val isTerminal: Boolean = false
    override val isReadOnly: Boolean = false

    override val title: String = "Mantenimiento en Curso (Regla NIIF NIC 16)"
    override val financialRationale: String =
        "Regla crítica internacional: La depreciación NO se detiene cuando un bien entra a mantenimiento o taller. " +
        "El desgaste contable por el paso del tiempo y obsolescencia sigue corriendo en el balance. " +
        "Los costos y repuestos incurridos van 100% como gasto del periodo al Estado de Resultados."

    override val calculationEngineBehavior: String =
        "El motor CRON continúa calculando y acumulando la cuota mensual normal de amortización. " +
        "Solo se suspendería si el método fuese por 'Unidades de Producción'."

    override val uiRestrictionsDescription: String =
        "• BLOQUEADO: Registro de 'Mejora / Adición' y 'Revalúo Técnico' (HTTP 409 Conflict).\n" +
        "• El botón de cabecera 'Mantenimiento' muta dinámicamente a 'Finalizar Mantenimiento'.\n" +
        "• Se permite registrar bitácoras adicionales de servicio técnico o dar de baja si no tiene arreglo."

    override val triggerDescription: String =
        "Iniciado mediante la orden de servicio con concepto técnico y fecha de ingreso a taller."

    override val allowedTargetStates: List<String> = listOf(
        "EN_USO",
        "DADO_DE_BAJA"
    )
}
