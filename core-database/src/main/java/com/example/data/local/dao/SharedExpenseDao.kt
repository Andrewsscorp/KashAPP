package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.SharedExpenseEntity
import com.example.data.local.entity.SharedExpenseLineItemEntity
import com.example.data.local.entity.SharedExpensePayerEntity
import com.example.data.local.entity.SharedExpenseSplitEntity
import com.example.data.local.entity.SharedExpenseWithFullDetails
import com.example.data.local.entity.SharedGroupEntity
import com.example.data.local.entity.SharedGroupMemberEntity
import com.example.data.local.entity.SharedParticipantEntity
import com.example.data.local.entity.SharedSequenceConfigEntity
import com.example.data.local.entity.SharedSettlementPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SharedExpenseDao {

    // =========================================================
    // 1. GESTIÓN DE SERIES Y CONSECUTIVOS (AUTOMÁTICOS / MANUALES)
    // =========================================================

    @Query("SELECT * FROM shared_sequence_configs WHERE (userId = :userId OR userId = 'user_default' OR :userId = '') ORDER BY sequenceType ASC")
    fun observeSequenceConfigs(userId: String): Flow<List<SharedSequenceConfigEntity>>

    @Query("SELECT * FROM shared_sequence_configs WHERE (userId = :userId OR userId = 'user_default' OR :userId = '') AND sequenceType = :sequenceType AND scopeGroupCode = :scopeGroupCode LIMIT 1")
    suspend fun getSequenceConfig(
        userId: String,
        sequenceType: String,
        scopeGroupCode: String = "GLOBAL"
    ): SharedSequenceConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSequenceConfig(config: SharedSequenceConfigEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM shared_expenses WHERE (userId = :userId OR userId = 'user_default' OR :userId = '') AND (UPPER(consecutiveCode) = UPPER(:code) OR consecutiveNormalized = :normalizedCode))")
    suspend fun existsExpenseConsecutive(userId: String, code: String, normalizedCode: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM shared_expenses WHERE (userId = :userId OR userId = 'user_default' OR :userId = '') AND UPPER(receiptTicketCode) = UPPER(:ticketCode))")
    suspend fun existsReceiptTicketCode(userId: String, ticketCode: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM shared_settlement_payments WHERE (userId = :userId OR userId = 'user_default' OR :userId = '') AND UPPER(voucherConsecutiveCode) = UPPER(:voucherCode))")
    suspend fun existsSettlementVoucherCode(userId: String, voucherCode: String): Boolean

    // =========================================================
    // 2. DIRECTORIO DE PARTICIPANTES Y GRUPOS
    // =========================================================

    @Query("SELECT * FROM shared_participants WHERE (userId = :userId OR userId = 'user_default' OR :userId = '') AND isActive = 1 ORDER BY isSelf DESC, displayName ASC")
    fun observeActiveParticipants(userId: String): Flow<List<SharedParticipantEntity>>

    @Query("SELECT * FROM shared_participants WHERE (userId = :userId OR userId = 'user_default' OR :userId = '') AND isActive = 1 ORDER BY isSelf DESC, displayName ASC")
    suspend fun getActiveParticipants(userId: String): List<SharedParticipantEntity>

    @Query("SELECT * FROM shared_participants WHERE id = :participantId LIMIT 1")
    suspend fun getParticipantById(participantId: String): SharedParticipantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertParticipant(participant: SharedParticipantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertParticipants(participants: List<SharedParticipantEntity>)

    @Query("SELECT * FROM shared_groups WHERE (userId = :userId OR userId = 'user_default' OR :userId = '') AND status != 'ARCHIVED' ORDER BY updatedAt DESC")
    fun observeGroups(userId: String): Flow<List<SharedGroupEntity>>

    @Query("SELECT * FROM shared_groups WHERE id = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: String): SharedGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroup(group: SharedGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroupMembers(members: List<SharedGroupMemberEntity>)

    @Query("SELECT p.* FROM shared_participants p INNER JOIN shared_group_members gm ON p.id = gm.participantId WHERE gm.groupId = :groupId AND gm.isActiveInGroup = 1 ORDER BY p.isSelf DESC, p.displayName ASC")
    fun observeGroupParticipants(groupId: String): Flow<List<SharedParticipantEntity>>

    // =========================================================
    // 3. GASTOS COMPARTIDOS, PAGADORES, SPLITS E ÍTEMS OCR
    // =========================================================

    @Transaction
    @Query("SELECT * FROM shared_expenses WHERE (userId = :userId OR userId = 'user_default' OR :userId = '') ORDER BY expenseDate DESC, createdAt DESC")
    fun observeAllSharedExpensesWithDetails(userId: String): Flow<List<SharedExpenseWithFullDetails>>

    @Transaction
    @Query("SELECT * FROM shared_expenses WHERE groupId = :groupId ORDER BY expenseDate DESC, createdAt DESC")
    fun observeGroupSharedExpensesWithDetails(groupId: String): Flow<List<SharedExpenseWithFullDetails>>

    @Transaction
    @Query("SELECT * FROM shared_expenses WHERE id = :sharedExpenseId LIMIT 1")
    suspend fun getSharedExpenseWithDetailsById(sharedExpenseId: String): SharedExpenseWithFullDetails?

    @Transaction
    @Query("SELECT * FROM shared_expenses WHERE (userId = :userId OR userId = 'user_default' OR :userId = '') AND UPPER(consecutiveCode) = UPPER(:consecutiveCode) LIMIT 1")
    suspend fun getSharedExpenseByConsecutive(userId: String, consecutiveCode: String): SharedExpenseWithFullDetails?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSharedExpenseHeader(expense: SharedExpenseEntity)

    @Update
    suspend fun updateSharedExpenseHeader(expense: SharedExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharedExpensePayers(payers: List<SharedExpensePayerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharedExpenseSplits(splits: List<SharedExpenseSplitEntity>)

    @Update
    suspend fun updateSharedExpenseSplits(splits: List<SharedExpenseSplitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharedExpenseLineItems(items: List<SharedExpenseLineItemEntity>)

    /**
     * Escritura Atómica ACID de un Gasto Compartido Completo con bloqueo de Consecutivo.
     */
    @Transaction
    suspend fun insertCompleteSharedExpenseTransactional(
        updatedExpenseSeqConfig: SharedSequenceConfigEntity?,
        updatedTicketSeqConfig: SharedSequenceConfigEntity?,
        expense: SharedExpenseEntity,
        payers: List<SharedExpensePayerEntity>,
        splits: List<SharedExpenseSplitEntity>,
        lineItems: List<SharedExpenseLineItemEntity>
    ) {
        if (updatedExpenseSeqConfig != null) {
            upsertSequenceConfig(updatedExpenseSeqConfig)
        }
        if (updatedTicketSeqConfig != null) {
            upsertSequenceConfig(updatedTicketSeqConfig)
        }
        insertSharedExpenseHeader(expense)
        insertSharedExpensePayers(payers)
        insertSharedExpenseSplits(splits)
        if (lineItems.isNotEmpty()) {
            insertSharedExpenseLineItems(lineItems)
        }
    }

    /**
     * Anulación Contable NIIF: Marca el gasto como VOIDED conservando el consecutivo en la bitácora.
     */
    @Query("UPDATE shared_expenses SET status = 'VOIDED', voidReason = :reason, voidedAt = :timestamp, holdReservedAmount = 0.0, updatedAt = :timestamp WHERE id = :sharedExpenseId")
    suspend fun voidSharedExpenseById(
        sharedExpenseId: String,
        reason: String,
        timestamp: Long = System.currentTimeMillis()
    )

    // =========================================================
    // 4. LIQUIDACIONES, ABONOS PARCIALES Y MÉTRICAS NIIF (CxC / CxP)
    // =========================================================

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSettlementPayment(payment: SharedSettlementPaymentEntity)

    @Query("SELECT * FROM shared_settlement_payments WHERE (userId = :userId OR userId = 'user_default' OR :userId = '') ORDER BY paymentDate DESC, createdAt DESC")
    fun observeSettlementPayments(userId: String): Flow<List<SharedSettlementPaymentEntity>>

    @Query("""
        SELECT COALESCE(SUM(s.remainingPendingAmount), 0.0)
        FROM shared_expense_splits s
        INNER JOIN shared_expenses e ON e.id = s.sharedExpenseId
        WHERE (e.userId = :userId OR e.userId = 'user_default' OR :userId = '')
          AND e.status IN ('PENDING_SETTLEMENT', 'PARTIALLY_SETTLED')
          AND e.realMoneyImpactMode = 'ACCRUAL_RECEIVABLE'
          AND s.participantId != 'SELF_USER'
          AND s.netBalanceForExpense < 0
          AND s.settlementStatus IN ('PENDING', 'PARTIAL')
    """)
    fun observeTotalAccountsReceivableNiif(userId: String): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(s.remainingPendingAmount), 0.0)
        FROM shared_expense_splits s
        INNER JOIN shared_expenses e ON e.id = s.sharedExpenseId
        WHERE (e.userId = :userId OR e.userId = 'user_default' OR :userId = '')
          AND e.status IN ('PENDING_SETTLEMENT', 'PARTIALLY_SETTLED')
          AND s.participantId = 'SELF_USER'
          AND s.netBalanceForExpense < 0
          AND s.settlementStatus IN ('PENDING', 'PARTIAL')
    """)
    fun observeTotalAccountsPayableNiif(userId: String): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(holdReservedAmount), 0.0)
        FROM shared_expenses
        WHERE (userId = :userId OR userId = 'user_default' OR :userId = '')
          AND status IN ('PENDING_SETTLEMENT', 'PARTIALLY_SETTLED')
    """)
    fun observeTotalMentalHoldReserved(userId: String): Flow<Double>

    @Transaction
    @Query("SELECT * FROM shared_expenses WHERE (userId = :userId OR userId = 'user_default' OR :userId = '') ORDER BY expenseDate DESC, createdAt DESC")
    fun observeSharedExpensesWithDetails(userId: String): Flow<List<SharedExpenseWithFullDetails>>
}
