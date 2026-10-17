package com.example.domain.models.shared

/**
 * Configuración global personalizada del usuario para el módulo de Gastos Compartidos.
 */
data class SharedExpenseSettingsConfig(
    val userId: String = "user_default",
    // 1. Preferencias de Consecutivos
    val expenseConsecutiveMode: ConsecutiveMode = ConsecutiveMode.HYBRID_ALLOW_OVERRIDE,
    val expensePrefix: String = "GC",
    val expensePatternTemplate: String = "{PREFIX}-{YYYY}-{SEQ:4}",
    val expenseConsecutivePattern: String = "{PREFIX}-{YYYY}-{SEQ:4}",
    val expensePaddingDigits: Int = 4,
    val expenseResetPolicy: SequenceResetPolicy = SequenceResetPolicy.YEARLY,
    val ticketPrefix: String = "TK",
    val receiptTicketPattern: String = "{PREFIX}-{YYYY}-{SEQ:4}",
    val paymentVoucherPrefix: String = "PG",
    val settlementVoucherPattern: String = "{PREFIX}-{YYYY}{MM}-{SEQ:4}",
    val settlementActPrefix: String = "LQ",
    val allowManualConsecutiveOverride: Boolean = true,
    // 2. Preferencias de "Mi Dinero Real" y Contabilidad NIIF
    val defaultRealMoneyImpactMode: RealMoneyImpactMode = RealMoneyImpactMode.ACCRUAL_RECEIVABLE,
    val defaultReimbursementTreatment: ReimbursementAccountingTreatment = ReimbursementAccountingTreatment.OFFSET_RECEIVABLE_ASSET,
    val defaultBudgetImpactPolicy: BudgetImpactPolicy = BudgetImpactPolicy.MY_NET_SHARE_ONLY,
    val autoReserveOwedInHoldBalance: Boolean = true,
    val preferredHoldReserveAccountId: String? = null,
    val autoCalculateColombiaGmf4x1000: Boolean = true,
    val prorateGmfAmongDebtors: Boolean = false,
    val passCreditCardInterestToGroupByDefault: Boolean = false,
    // 3. Preferencias de División, Impuestos y Redondeo
    val defaultSplitMethod: SplitCalculationMethod = SplitCalculationMethod.EQUAL,
    val defaultTaxAndTipRule: TaxAndTipProrationRule = TaxAndTipProrationRule.PROPORTIONAL_TO_SUBTOTAL,
    val defaultRoundingStep: RoundingStepRule = RoundingStepRule.ROUND_1,
    val defaultRemainderTarget: RoundingRemainderTarget = RoundingRemainderTarget.PAYER,
    val enableGraphDebtSimplification: Boolean = true,
    val enableBilateralAutoNetting: Boolean = true,
    // 4. Preferencias de Tickets, QR y Cobro
    val defaultCollectionBankName: String = "Bancolombia / Nequi",
    val defaultCollectionAccountOrKey: String = "",
    val defaultPaymentInstructionsText: String = "",
    val maskUserAccountBalancesOnTicket: Boolean = true,
    val includeIntegrityHashOnTickets: Boolean = true,
    val includeQrCodeOnTickets: Boolean = true,
    val reminderDaysAfterDue: Int = 5
)
