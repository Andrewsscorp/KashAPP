package com.example.domain.model.fixedassets.statemachine

/**
 * State: EN_SINIESTRO (En Investigación / Reclamación de Seguro / Hurto)
 * The asset suffered severe damage, theft or accident under formal insurance investigation.
 */
data object InClaimState : AssetState {
    override val code: String = "EN_SINIESTRO"
    override val label: String = "En siniestro / Investigación de seguro"
    override val shortBadgeLabel: String = "En siniestro"
    override val colorHex: String = "#E11D48"

    override val allowsDepreciation: Boolean = false // Paused during insurance settlement
    override val allowsImprovements: Boolean = false
    override val allowsRevaluation: Boolean = false
    override val allowsDisposal: Boolean = false // Blocked: cannot dispose until insurance adjustor renders formal resolution
    override val allowsMaintenance: Boolean = false
    override val isTerminal: Boolean = false
    override val isReadOnly: Boolean = false

    override val title: String = "Siniestro / Proceso de Aseguradora (NIIF)"
    override val financialRationale: String =
        "Estado transitorio de salvaguarda contable. El activo sufrió un accidente grave, pérdida o hurto. " +
        "Bajo normas de auditoría, NO se puede dar de baja definitiva inmediatamente porque la aseguradora " +
        "puede reponer el bien o liquidar una indemnización financiera. El activo permanece congelado."

    override val calculationEngineBehavior: String =
        "El motor CRON suspende preventivamente el cálculo de depreciación mientras dura el peritaje del ajustador."

    override val uiRestrictionsDescription: String =
        "• BLOQUEADO: El botón de baja definitiva ('Eliminar') permanece inhabilitado hasta que se emita la resolución.\n" +
        "• Al concluir la reclamación:\n" +
        "  - Si es pérdida total: transiciona a 'DADO_DE_BAJA' registrando indemnización vs valor en libros.\n" +
        "  - Si es reparable: transiciona a 'EN_MANTENIMIENTO'.\n" +
        "  - Si se recupera intacto: retorna a 'EN_USO'."

    override val triggerDescription: String =
        "Radicación de denuncia penal o notificación formal de siniestro ante la compañía de seguros con N° de póliza y radicado."

    override val allowedTargetStates: List<String> = listOf(
        "DADO_DE_BAJA",
        "EN_MANTENIMIENTO",
        "EN_USO"
    )
}
