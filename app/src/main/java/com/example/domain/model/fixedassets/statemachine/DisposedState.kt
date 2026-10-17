package com.example.domain.model.fixedassets.statemachine

/**
 * State: DADO_DE_BAJA (Retirado / Desincorporado / Soft-Deleted)
 * Terminal lifecycle state. The asset is permanently derecognized from company books.
 */
data object DisposedState : AssetState {
    override val code: String = "DADO_DE_BAJA"
    override val label: String = "Dado de baja (Retirado)"
    override val shortBadgeLabel: String = "De baja"
    override val colorHex: String = "#64748B"

    override val allowsDepreciation: Boolean = false
    override val allowsImprovements: Boolean = false
    override val allowsRevaluation: Boolean = false
    override val allowsDisposal: Boolean = false
    override val allowsMaintenance: Boolean = false
    override val isTerminal: Boolean = true // Terminal state: irreversible by UI
    override val isReadOnly: Boolean = true // UI is strictly read-only

    override val title: String = "Baja Definitiva y Desincorporación (NIC 16)"
    override val financialRationale: String =
        "El ciclo de vida contable finalizó. En software financiero un activo jamás se borra físicamente (DELETE). " +
        "Si el activo conservaba saldo en libros (no estaba 100% amortizado), dicho saldo remanente se reconoce " +
        "inmediatamente como una 'Pérdida por Retiro de Activos' en el Estado de Resultados. " +
        "La depreciación cesa para siempre."

    override val calculationEngineBehavior: String =
        "El motor CRON y cálculo por lotes excluye definitivamente este registro (WHERE deleted_at IS NULL). " +
        "No se vuelve a generar ningún cargo contable futuro."

    override val uiRestrictionsDescription: String =
        "• MODO SOLO LECTURA (Read-Only): Todos los botones de mutación quedan bloqueados.\n" +
        "• Se ocultan las opciones de 'Registrar Movimiento', 'Editar Ficha' y 'Configurar Parámetros'.\n" +
        "• La pantalla conserva intacto todo el historial de auditoría y movimientos para peritajes fiscales."

    override val triggerDescription: String =
        "Activado mediante el proceso de 'Dar de Baja / Retirar Activo' con acta de desincorporación o siniestro."

    override val allowedTargetStates: List<String> = emptyList() // Terminal: no transitions allowed
}
