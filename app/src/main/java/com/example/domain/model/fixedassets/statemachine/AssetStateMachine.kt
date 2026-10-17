package com.example.domain.model.fixedassets.statemachine

import com.example.data.local.entity.FixedAssetEntity

/**
 * State Machine engine and guardrails coordinator for Fixed Asset lifecycle.
 * Enforces IFRS / NIIF transition rules, calculation engine compatibility, and UI permissions.
 */
object AssetStateMachine {

    val allStates: List<AssetState> = listOf(
        InUseState,
        InMaintenanceState,
        DisposedState,
        AvailableState,
        UnderConstructionState,
        InTransitState,
        HeldForSaleState,
        OnLoanState,
        InClaimState
    )

    /**
     * Resolves an AssetState instance from entity status string (including legacy codes).
     */
    fun resolveState(status: String?): AssetState {
        if (status.isNullOrBlank()) return InUseState

        val normalized = status.trim().uppercase()
        // Exact code match
        allStates.firstOrNull { it.code.equals(normalized, ignoreCase = true) }?.let { return it }
        // Contains exact code
        allStates.firstOrNull { normalized.contains(it.code) }?.let { return it }

        return when {
            normalized.contains("MANTENIMIENTO") || normalized == "MAINTENANCE" || normalized.contains("TALLER") -> InMaintenanceState
            normalized.contains("BAJA") || normalized == "WRITTEN_OFF" || normalized == "DISPOSED" || normalized == "SOLD" -> DisposedState
            normalized.contains("DISPONIBLE") || normalized.contains("BODEGA") -> AvailableState
            normalized.contains("CONSTRUCCION") || normalized.contains("OBRA") -> UnderConstructionState
            normalized.contains("TRANSITO") || normalized.contains("RUTA") -> InTransitState
            normalized.contains("VENTA") -> HeldForSaleState
            normalized.contains("COMODATO") || normalized.contains("PRESTAMO") -> OnLoanState
            normalized.contains("SINIESTRO") || normalized.contains("SEGURO") -> InClaimState
            normalized.contains("USO") || normalized == "ACTIVE" || normalized.contains("OPERATIVO") -> InUseState
            else -> InUseState
        }
    }

    /**
     * Guardrail check: Verifies if the transition from current to target is allowed by the matrix.
     */
    fun canTransition(from: AssetState, to: AssetState): Boolean {
        if (from == to) return false
        if (from.isTerminal) return false // Disposed state cannot transition anywhere
        return from.allowedTargetStates.contains(to.code)
    }

    /**
     * Validates an asset transition request with human-readable error if forbidden.
     */
    fun validateTransition(asset: FixedAssetEntity, targetCode: String): Result<AssetState> {
        val current = resolveState(asset.status)
        val target = resolveState(targetCode)

        if (current.isTerminal) {
            return Result.failure(
                IllegalStateException("Transición inválida: El activo se encuentra en estado terminal '${current.label}' y no puede modificarse.")
            )
        }

        if (current.code == target.code) {
            return Result.failure(
                IllegalArgumentException("El activo ya se encuentra en estado '${target.label}'.")
            )
        }

        if (!canTransition(current, target)) {
            return Result.failure(
                IllegalStateException(
                    "Transición rechazada por reglas contables: No es posible pasar directamente de '${current.label}' a '${target.label}'. " +
                    "Transiciones válidas desde este estado: ${current.allowedTargetStates.joinToString(", ")}."
                )
            )
        }

        return Result.success(target)
    }

    /**
     * Applies a valid transition to the asset entity, updating status, timestamps, and audit notes.
     */
    fun applyTransition(
        asset: FixedAssetEntity,
        targetCode: String,
        stateReasonNotes: String? = null
    ): FixedAssetEntity {
        val targetState = resolveState(targetCode)
        val now = System.currentTimeMillis()

        return asset.copy(
            status = targetState.code,
            deletedAt = if (targetState.isTerminal) now else asset.deletedAt,
            stateNotes = stateReasonNotes?.ifBlank { null } ?: asset.stateNotes,
            updatedAt = now
        )
    }

    /**
     * Gets a human-friendly action verb for buttons (e.g., "Poner en Operación", "Enviar a Mantenimiento").
     */
    fun getActionVerbForTarget(targetState: AssetState): String {
        return when (targetState) {
            InUseState -> "Poner en Operación (En uso)"
            InMaintenanceState -> "Ingresar a Mantenimiento"
            DisposedState -> "Dar de Baja Definitiva"
            AvailableState -> "Enviar a Bodega (Disponible)"
            UnderConstructionState -> "Clasificar como Obra en Curso"
            InTransitState -> "Registrar en Tránsito"
            HeldForSaleState -> "Clasificar para la Venta (NIIF 5)"
            OnLoanState -> "Entregar en Comodato"
            InClaimState -> "Notificar Siniestro a Aseguradora"
        }
    }
}
