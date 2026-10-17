package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * 1. Configuración y contador atómico de Consecutivos por tipo de documento y serie/grupo.
 */
@Entity(
    tableName = "shared_sequence_configs",
    indices = [
        Index(value = ["userId", "sequenceType", "scopeGroupCode"], unique = true)
    ]
)
data class SharedSequenceConfigEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val sequenceType: String,                  // "SHARED_EXPENSE" | "RECEIPT_TICKET" | "PAYMENT_VOUCHER" | "SETTLEMENT_ACT"
    val scopeGroupCode: String = "GLOBAL",     // "GLOBAL" o código de grupo ej. "HOG", "VIA"
    val mode: String = "HYBRID_ALLOW_OVERRIDE",// "AUTO_INCREMENT" | "BY_GROUP_SERIES" | "HYBRID_ALLOW_OVERRIDE" | "MANUAL_STRICT"
    val prefix: String = "GC",
    val patternTemplate: String = "{PREFIX}-{YYYY}-{SEQ:4}",
    val currentValue: Long = 0L,
    val stepBy: Int = 1,
    val paddingDigits: Int = 4,
    val resetPolicy: String = "YEARLY",        // "NEVER" | "YEARLY" | "MONTHLY" | "PER_GROUP_CYCLE"
    val lastResetPeriodKey: String = "2026",   // Ej. "2026" o "2026-10"
    val allowManualGaps: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
)

/**
 * 2. Participante en gastos compartidos (incluye el registro SELF_USER para el propio titular).
 */
@Entity(
    tableName = "shared_participants",
    indices = [
        Index(value = ["userId", "nameNormalized"], unique = true),
        Index(value = ["userId", "isActive"])
    ]
)
data class SharedParticipantEntity(
    @PrimaryKey
    val id: String,                            // "SELF_USER" para el titular de la app, o UUID para terceros
    val userId: String,
    val isSelf: Boolean = false,
    val displayName: String,
    val nameNormalized: String,
    val aliasOrRelation: String? = null,       // Ej. "Pareja", "Roommate", "Hermano", "Socio"
    val phoneOrWhatsApp: String? = null,
    val email: String? = null,
    val preferredPaymentMethod: String? = null,// "NEQUI", "BANCOLOMBIA", "DAVIPLATA", "BRE_B", "PSE"
    val bankName: String? = null,
    val accountType: String? = null,           // "AHORROS", "CORRIENTE", "BILLETERA", "LLAVE_BRE_B"
    val accountNumberOrKey: String? = null,    // Número o llave Bre-B / celular para pagarle o cobrarle
    val qrImageLocalUri: String? = null,
    val monthlyIncomeEstimate: Double = 0.0,   // Usado en modo INCOME_PROPORTIONAL (Equidad Salarial)
    val defaultIncomeShareWeight: Double = 1.0,
    val avatarColorHex: String = "#0284C7",
    val avatarInitials: String = "KP",
    val creditTrustLimit: Double? = null,      // Tope máximo que el usuario está dispuesto a prestarle
    val isActive: Boolean = true,
    val archivedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 3. Grupo o Proyecto de Gastos Compartidos (Hogar, Pareja, Viaje, Evento, Negocio).
 */
@Entity(
    tableName = "shared_groups",
    indices = [
        Index(value = ["userId", "nameNormalized"], unique = true),
        Index(value = ["userId", "groupCode"], unique = true),
        Index(value = ["userId", "status"])
    ]
)
data class SharedGroupEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val nameNormalized: String,
    val groupCode: String,                     // Código corto para la serie de consecutivos: ej. "APTO", "CANCUN"
    val groupType: String = "HOME",            // "HOME" | "COUPLE" | "TRIP" | "EVENT" | "BUSINESS" | "OTHER"
    val baseCurrency: String = "COP",
    val iconName: String = "ic_group_default",
    val colorHex: String = "#0EA5E9",
    val defaultRealMoneyImpactMode: String = "ACCRUAL_RECEIVABLE",
    val defaultSplitMethod: String = "EQUAL",
    val defaultSourceAccountId: String? = null,
    val defaultCategoryId: String? = null,
    val simplifyDebtsEnabled: Boolean = true,
    val autoReserveOwedInHold: Boolean = true,
    val budgetImpactPolicy: String = "MY_NET_SHARE_ONLY",
    val settlementCycleDay: Int? = null,       // Ej. día 15 o 30 de cada mes para corte del grupo
    val status: String = "ACTIVE",             // "ACTIVE" | "SETTLED" | "ARCHIVED"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 4. Relación N:M de miembros pertenecientes a un Grupo con sus pesos por defecto.
 */
@Entity(
    tableName = "shared_group_members",
    primaryKeys = ["groupId", "participantId"],
    foreignKeys = [
        ForeignKey(
            entity = SharedGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SharedParticipantEntity::class,
            parentColumns = ["id"],
            childColumns = ["participantId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["participantId"])
    ]
)
data class SharedGroupMemberEntity(
    val groupId: String,
    val participantId: String,
    val defaultShareUnits: Double = 1.0,
    val defaultPercentage: Double? = null,
    val defaultAdjustmentAmount: Double = 0.0,
    val joinedAt: Long = System.currentTimeMillis(),
    val isActiveInGroup: Boolean = true
)

/**
 * 5. Cabecera Maestra de un Gasto Compartido con doble consecutivo (GC + TK), desglose fiscal
 *    y parámetros exactos de afectación al Dinero Real.
 */
@Entity(
    tableName = "shared_expenses",
    indices = [
        Index(value = ["userId", "consecutiveCode"], unique = true),
        Index(value = ["userId", "receiptTicketCode"], unique = true),
        Index(value = ["groupId", "expenseDate"]),
        Index(value = ["userId", "status"]),
        Index(value = ["idempotencyKey"], unique = true)
    ]
)
data class SharedExpenseEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    // Identificación y Consecutivos
    val consecutiveCode: String,               // Ej. "GC-2026-0042" o manual "FAC-9012"
    val consecutiveNormalized: String,
    val isManualConsecutive: Boolean = false,
    val receiptTicketCode: String,             // Ej. "TK-2026-0042"
    val externalInvoiceNumber: String? = null, // Número de factura DIAN / POS externo opcional
    // Clasificación y Contexto
    val groupId: String? = null,               // Null si es un gasto rápido 1-a-1 fuera de grupo
    val title: String,
    val description: String? = null,
    val merchantId: String? = null,
    val merchantNameSnapshot: String? = null,
    val merchantNit: String? = null,
    val categoryId: String,
    val categoryNameSnapshot: String,
    val expenseDate: String,                   // ISO-8601 "YYYY-MM-DD"
    val expenseTime: String,                   // "HH:mm:ss"
    // Moneda y Desglose Fiscal Colombiano / Internacional
    val currency: String = "COP",
    val baseCurrency: String = "COP",
    val appliedExchangeRate: Double = 1.0,
    val exchangeRateSource: String = "OFFICIAL_TRM",
    val subtotalAmount: Double,                // Valor antes de impuestos y propina
    val taxIvaAmount: Double = 0.0,            // IVA (ej. 19% o 5%)
    val taxConsumptionAmount: Double = 0.0,    // Impoconsumo (ej. 8%)
    val tipAmount: Double = 0.0,               // Propina voluntaria (ej. 10%)
    val discountAmount: Double = 0.0,          // Descuento o cashback aplicado en factura
    val gmfAmount: Double = 0.0,               // 4x1000 generado por el desembolso
    val creditCardInterestSurcharge: Double = 0.0, // Intereses si se financió a >1 cuota con TC
    val totalAmount: Double,                   // Total neto a repartir: subtotal + IVA + Impoconsumo + propina - descuento (+ recargos)
    // Reglas Matemáticas Aplicadas
    val splitMethod: String = "EQUAL",
    val taxAndTipProrationRule: String = "PROPORTIONAL_TO_SUBTOTAL",
    val roundingRule: String = "ROUND_1",
    val roundingRemainderTarget: String = "PAYER",
    // Reglas Contables de "Mi Dinero Real"
    val realMoneyImpactMode: String = "ACCRUAL_RECEIVABLE",
    val budgetImpactPolicy: String = "MY_NET_SHARE_ONLY",
    val holdReserveAccountId: String? = null,  // Cuenta donde se congeló holdBalance si yo debo
    val holdReservedAmount: Double = 0.0,      // Monto actualmente retenido en holdBalance por este gasto
    // Resumen instantáneo de la posición del titular (SELF_USER)
    val myPaidAmount: Double = 0.0,            // Cuánto puse yo de mis cuentas reales
    val myConsumptionShareAmount: Double = 0.0,// Cuánto me correspondía por mi consumo propio
    val myNetPositionDelta: Double = 0.0,      // myPaidAmount - myConsumptionShareAmount (+ me deben / - yo debo)
    val settledAmountTotal: Double = 0.0,      // Suma de abonos ya liquidados sobre este gasto
    // Ciclo de Vida, Soportes y Seguridad
    val status: String = "PENDING_SETTLEMENT", // "DRAFT" | "PENDING_SETTLEMENT" | "PARTIALLY_SETTLED" | "SETTLED" | "DISPUTED" | "VOIDED"
    val voidReason: String? = null,
    val voidedAt: Long? = null,
    val receiptImageLocalUri: String? = null,  // URI cifrada en SecureDocumentManager
    val generatedTicketPdfUri: String? = null,
    val generatedTicketPngUri: String? = null,
    val ticketIntegrityHash: String,           // Firma SHA-256 truncada de 16 caracteres hexadecimales
    val isRecurring: Boolean = false,
    val recurrenceFrequency: String? = null,
    val nextRecurrenceDate: String? = null,
    val dueDateForSettlement: String? = null,  // Fecha límite acordada para el pago
    val idempotencyKey: String,
    val syncStatus: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 6. Desembolsos en caja por uno o múltiples pagadores y su vínculo con cuentas reales de KashApp.
 */
@Entity(
    tableName = "shared_expense_payers",
    foreignKeys = [
        ForeignKey(
            entity = SharedExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["sharedExpenseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SharedParticipantEntity::class,
            parentColumns = ["id"],
            childColumns = ["participantId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["sharedExpenseId"]),
        Index(value = ["participantId"]),
        Index(value = ["kashAccountId"])
    ]
)
data class SharedExpensePayerEntity(
    @PrimaryKey
    val id: String,
    val sharedExpenseId: String,
    val participantId: String,                 // "SELF_USER" si pagó el usuario, o ID del amigo pagador
    val paidAmount: Double,
    val kashAccountId: String? = null,         // ID de AccountEntity si pagó SELF_USER
    val kashAccountNameSnapshot: String? = null,
    val paymentInstrumentType: String = "SAVINGS", // "SAVINGS" | "CHECKING" | "WALLET" | "CASH" | "CREDIT_CARD"
    val creditCardInstallments: Int = 1,
    val passCreditCardInterestToGroup: Boolean = false,
    val gmfGeneratedAmount: Double = 0.0,
    val linkedTransactionId: String? = null    // FK lógica a TransactionEntity creada en el libro mayor
)

/**
 * 7. Cuota parte asignada a cada participante y control de saldo pendiente sobre ese gasto.
 */
@Entity(
    tableName = "shared_expense_splits",
    foreignKeys = [
        ForeignKey(
            entity = SharedExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["sharedExpenseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SharedParticipantEntity::class,
            parentColumns = ["id"],
            childColumns = ["participantId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["sharedExpenseId"]),
        Index(value = ["participantId", "settlementStatus"])
    ]
)
data class SharedExpenseSplitEntity(
    @PrimaryKey
    val id: String,
    val sharedExpenseId: String,
    val participantId: String,
    val isIncluded: Boolean = true,
    val rawInputWeightOrPct: Double = 1.0,
    val baseSubtotalShare: Double,
    val allocatedTaxAmount: Double = 0.0,
    val allocatedTipAmount: Double = 0.0,
    val allocatedDiscountAmount: Double = 0.0,
    val allocatedGmfAmount: Double = 0.0,
    val roundingAdjustment: Double = 0.0,
    val totalOwedShare: Double,                 // Cuota final de consumo de este participante
    val paidByThisParticipant: Double = 0.0,    // Cuánto puso este participante en caja
    val netBalanceForExpense: Double,           // paidByThisParticipant - totalOwedShare (+ acreedor / - deudor)
    val amountAlreadySettled: Double = 0.0,
    val remainingPendingAmount: Double,
    val settlementStatus: String = "PENDING"    // "NOT_APPLICABLE" | "PENDING" | "PARTIAL" | "SETTLED" | "FORGIVEN"
)

/**
 * 8. Ítems individuales de factura (OCR o manuales) vinculados a sus consumidores.
 */
@Entity(
    tableName = "shared_expense_line_items",
    foreignKeys = [
        ForeignKey(
            entity = SharedExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["sharedExpenseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sharedExpenseId"])
    ]
)
data class SharedExpenseLineItemEntity(
    @PrimaryKey
    val id: String,
    val sharedExpenseId: String,
    val itemOrder: Int,
    val description: String,
    val quantity: Double = 1.0,
    val unitPrice: Double,
    val lineTotalPrice: Double,
    val isTaxExempt: Boolean = false,
    val assignedParticipantIdsCsv: String,      // IDs separados por coma: "SELF_USER,part_01"
    val splitModeWithinItem: String = "EQUAL"
)

/**
 * 9. Pagos de liquidación, abonos parciales, cruces (netting) y condonaciones con su consecutivo PG/LQ.
 */
@Entity(
    tableName = "shared_settlement_payments",
    indices = [
        Index(value = ["userId", "voucherConsecutiveCode"], unique = true),
        Index(value = ["groupId", "paymentDate"]),
        Index(value = ["sharedExpenseId"]),
        Index(value = ["fromParticipantId", "toParticipantId"]),
        Index(value = ["idempotencyKey"], unique = true)
    ]
)
data class SharedSettlementPaymentEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val voucherConsecutiveCode: String,         // Ej. "PG-202610-0001" o "LQ-2026-001"
    val isFullGroupSettlementAct: Boolean = false,
    val groupId: String? = null,
    val sharedExpenseId: String? = null,
    val fromParticipantId: String,              // Quien entrega el dinero (Deudor)
    val toParticipantId: String,                // Quien recibe el dinero (Acreedor)
    val amount: Double,
    val currency: String = "COP",
    val appliedExchangeRate: Double = 1.0,
    val fxGainOrLossAmount: Double = 0.0,
    val settlementMethod: String,               // "BANK_TRANSFER" | "NEQUI" | "DAVIPLATA" | "BRE_B" | "CASH" | "OFFSET_NETTING" | "FORGIVENESS"
    val reimbursementTreatmentApplied: String = "OFFSET_RECEIVABLE_ASSET",
    val kashAccountId: String? = null,
    val kashAccountNameSnapshot: String? = null,
    val linkedTransactionId: String? = null,
    val releasedHoldAmount: Double = 0.0,
    val externalReferenceCode: String? = null,
    val proofDocumentLocalUri: String? = null,
    val generatedVoucherPdfUri: String? = null,
    val ticketIntegrityHash: String,
    val notes: String? = null,
    val paymentDate: String,
    val paymentTime: String,
    val idempotencyKey: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Agregado Relacional de Lectura Completa de un Gasto Compartido.
 */
data class SharedExpenseWithFullDetails(
    @Embedded val expense: SharedExpenseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sharedExpenseId"
    )
    val payers: List<SharedExpensePayerEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "sharedExpenseId"
    )
    val splits: List<SharedExpenseSplitEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "sharedExpenseId"
    )
    val lineItems: List<SharedExpenseLineItemEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "sharedExpenseId"
    )
    val settlements: List<SharedSettlementPaymentEntity>
)
