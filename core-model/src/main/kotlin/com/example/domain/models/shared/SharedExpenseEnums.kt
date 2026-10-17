package com.example.domain.models.shared

/**
 * 1. Tipos de series documentales con consecutivo independiente en Gastos Compartidos.
 */
enum class SequenceDocumentType(
    val code: String,
    val defaultPrefix: String,
    val defaultPattern: String,
    val displayName: String
) {
    SHARED_EXPENSE("SHARED_EXPENSE", "GC", "{PREFIX}-{YYYY}-{SEQ:4}", "Gasto Compartido"),
    RECEIPT_TICKET("RECEIPT_TICKET", "TK", "{PREFIX}-{YYYY}-{SEQ:4}", "Ticket de Recibo / Cobro"),
    PAYMENT_VOUCHER("PAYMENT_VOUCHER", "PG", "{PREFIX}-{YYYY}{MM}-{SEQ:4}", "Comprobante de Pago / Abono"),
    SETTLEMENT_ACT("SETTLEMENT_ACT", "LQ", "{PREFIX}-{YYYY}-{SEQ:3}", "Acta de Paz y Salvo Grupal")
}

/**
 * 2. Modo de generación y edición del consecutivo.
 */
enum class ConsecutiveMode(val code: String, val label: String) {
    AUTO_INCREMENT("AUTO_INCREMENT", "Automático estricto (Secuencial)"),
    BY_GROUP_SERIES("BY_GROUP_SERIES", "Serie automática por Grupo / Proyecto"),
    HYBRID_ALLOW_OVERRIDE("HYBRID_ALLOW_OVERRIDE", "Híbrido (Sugiere automático y permite editar)"),
    MANUAL_STRICT("MANUAL_STRICT", "Manual / Número de Factura Externa")
}

/**
 * 3. Política de reinicio periódico del contador numérico.
 */
enum class SequenceResetPolicy(val code: String, val label: String) {
    NEVER("NEVER", "Continuo histórico (Nunca se reinicia)"),
    YEARLY("YEARLY", "Reinicio Anual (0001 cada 1 de Enero)"),
    MONTHLY("MONTHLY", "Reinicio Mensual (0001 cada mes)"),
    PER_GROUP_CYCLE("PER_GROUP_CYCLE", "Por Ciclo de Liquidación del Grupo")
}

/**
 * 4. Los 7 algoritmos matemáticos de división de un gasto compartido.
 */
enum class SplitCalculationMethod(
    val code: String,
    val shortLabel: String,
    val description: String
) {
    EQUAL("EQUAL", "= Partes Iguales", "Divide el monto equitativamente entre los participantes seleccionados"),
    EXACT_AMOUNT("EXACT_AMOUNT", "$ Monto Exacto", "Asigna un valor fijo en pesos/divisa a cada persona"),
    PERCENTAGE("PERCENTAGE", "% Porcentaje", "Reparte según porcentajes que suman exactamente 100.00%"),
    SHARES("SHARES", "x Partes / Acciones", "Pondera por unidades, días de estadía, cabezas de familia o acciones"),
    ADJUSTMENT("ADJUSTMENT", "+/- Ajuste Previo", "Asigna consumos exclusivos primero y divide el resto en partes iguales"),
    ITEMIZED_OCR("ITEMIZED_OCR", "Ítem por Ítem (OCR)", "Cada participante paga exactamente los platos o ítems que consumió"),
    INCOME_PROPORTIONAL("INCOME_PROPORTIONAL", "Equidad Salarial", "Prorratea proporcionalmente al nivel de ingresos de cada miembro")
}

/**
 * 5. Regla de prorrateo de Impuestos (IVA / Impoconsumo) y Propina.
 */
enum class TaxAndTipProrationRule(val code: String, val label: String) {
    PROPORTIONAL_TO_SUBTOTAL("PROPORTIONAL_TO_SUBTOTAL", "Proporcional al consumo base de cada uno"),
    EQUAL_AMONG_CONSUMERS("EQUAL_AMONG_CONSUMERS", "En partes iguales entre quienes consumieron"),
    PAYER_ABSORBS("PAYER_ABSORBS", "Asumido 100% por el pagador principal")
}

/**
 * 6. Regla de escalón de redondeo comercial colombiano / internacional.
 */
enum class RoundingStepRule(val code: String, val stepValue: Double, val label: String) {
    EXACT_CENT("EXACT_CENT", 0.01, "Al centavo exacto ($0.01)"),
    ROUND_1("ROUND_1", 1.0, "Al peso entero ($1 COP)"),
    ROUND_10("ROUND_10", 10.0, "Múltiplos de $10 COP"),
    ROUND_50("ROUND_50", 50.0, "Múltiplos de $50 COP"),
    ROUND_100("ROUND_100", 100.0, "Múltiplos de $100 COP"),
    ROUND_1000("ROUND_1000", 1000.0, "Al mil más cercano ($1.000 COP)")
}

/**
 * 7. Destinatario del centavo o peso residual (Largest Remainder Method).
 */
enum class RoundingRemainderTarget(val code: String, val label: String) {
    PAYER("PAYER", "Lo asume quien pagó la factura"),
    SELF("SELF", "Lo asumo yo (Titular)"),
    LARGEST_SHARE("LARGEST_SHARE", "Lo asume quien tuvo mayor consumo"),
    ROTATING("ROTATING", "Rotación equitativa automática por turno")
}

/**
 * 8. Los 4 Modos de Afectación al Dinero Real del Usuario (NIIF vs Flujo de Caja).
 */
enum class RealMoneyImpactMode(
    val code: String,
    val title: String,
    val subtitle: String,
    val shortBadge: String = title
) {
    ACCRUAL_RECEIVABLE(
        "ACCRUAL_RECEIVABLE",
        "Modo 1: Contable NIIF (Gasto Propio + Cuenta por Cobrar)",
        "Resta el 100% de tu banco hoy, pero solo tu cuota afecta tu Gasto Real y el resto queda como Activo por Cobrar.",
        "Devengo NIIF (Gasto + CxC)"
    ),
    GROSS_CASHFLOW(
        "GROSS_CASHFLOW",
        "Modo 2: Flujo de Caja Bruto Conservador (100% Gasto Hoy)",
        "Trata todo el desembolso como gasto hasta que recibas el dinero de vuelta.",
        "Flujo de Caja Bruto (100%)"
    ),
    NET_SHARE_ONLY(
        "NET_SHARE_ONLY",
        "Modo 3: Solo Mi Cuota Neta",
        "Descuenta de tu cuenta y presupuesto únicamente tu porcentaje de consumo personal.",
        "Solo Mi Cuota Neta"
    ),
    DEFERRED_ON_SETTLEMENT(
        "DEFERRED_ON_SETTLEMENT",
        "Modo 4: Informativo / Diferido al Liquidar",
        "Registra en la bitácora del grupo sin tocar tus saldos bancarios hasta el día de pago.",
        "Diferido al Liquidar"
    )
}

/**
 * 9. Tratamiento contable cuando un tercero nos reembolsa o abona una deuda.
 */
enum class ReimbursementAccountingTreatment(val code: String, val label: String) {
    OFFSET_RECEIVABLE_ASSET(
        "OFFSET_RECEIVABLE_ASSET",
        "Cancelar Cuenta por Cobrar (Recupera liquidez sin inflar ingresos)"
    ),
    NEGATIVE_EXPENSE_NETTING(
        "NEGATIVE_EXPENSE_NETTING",
        "Reverso de Gasto en la Categoría Original (Devuelve cupo al Presupuesto)"
    ),
    RECORD_AS_OTHER_INCOME(
        "RECORD_AS_OTHER_INCOME",
        "Registrar como Ingreso Extraordinario / Reembolso"
    )
}

/**
 * 10. Política de impacto en el módulo de Presupuestos (BudgetItemEntity).
 */
enum class BudgetImpactPolicy(val code: String, val label: String) {
    MY_NET_SHARE_ONLY("MY_NET_SHARE_ONLY", "Consumir presupuesto solo por mi cuota parte"),
    FULL_OUTFLOW_UNTIL_REIMBURSED("FULL_OUTFLOW_UNTIL_REIMBURSED", "Consumir el 100% pagado y liberar al cobrar"),
    EXCLUDE_FROM_BUDGET("EXCLUDE_FROM_BUDGET", "No afectar mis presupuestos mensuales")
}

/**
 * 11. Tipo de Grupo o Proyecto Compartido.
 */
enum class SharedGroupType(val code: String, val label: String, val defaultPrefixCode: String) {
    HOME("HOME", "Hogar / Roommates", "HOG"),
    COUPLE("COUPLE", "Pareja / Familia", "PAR"),
    TRIP("TRIP", "Viaje / Vacaciones", "VIA"),
    EVENT("EVENT", "Evento / Cena / Regalo", "EVT"),
    BUSINESS("BUSINESS", "Proyecto / Negocio / Viáticos", "PRJ"),
    OTHER("OTHER", "General", "GEN")
}

/**
 * 12. Estado del ciclo de vida de un Gasto Compartido.
 */
enum class SharedExpenseStatus(val code: String, val label: String) {
    DRAFT("DRAFT", "Borrador"),
    PENDING_SETTLEMENT("PENDING_SETTLEMENT", "Pendiente de Cobro / Pago"),
    PARTIALLY_SETTLED("PARTIALLY_SETTLED", "Abonado Parcialmente"),
    SETTLED("SETTLED", "Liquidado (Paz y Salvo)"),
    DISPUTED("DISPUTED", "En Revisión"),
    VOIDED("VOIDED", "Anulado (Conserva Consecutivo)")
}

/**
 * 13. Estado individual de la cuota de un participante.
 */
enum class SplitSettlementStatus(val code: String, val label: String) {
    NOT_APPLICABLE("NOT_APPLICABLE", "Pagador / Sin Deuda"),
    PENDING("PENDING", "Pendiente"),
    PARTIAL("PARTIAL", "Abono Parcial"),
    SETTLED("SETTLED", "Pagado"),
    FORGIVEN("FORGIVEN", "Condonado")
}

/**
 * 14. Método utilizado para liquidar o abonar una deuda.
 */
enum class SettlementPaymentMethod(val code: String, val label: String) {
    BANK_TRANSFER("BANK_TRANSFER", "Transferencia Bancaria / PSE"),
    NEQUI("NEQUI", "Nequi"),
    DAVIPLATA("DAVIPLATA", "DaviPlata"),
    BRE_B("BRE_B", "Llave Bre-B / Transfiya"),
    CASH("CASH", "Efectivo"),
    OFFSET_NETTING("OFFSET_NETTING", "Cruce Automático de Deudas (Netting)"),
    FORGIVENESS("FORGIVENESS", "Condonación / Cortesía")
}
