package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AccountReconciliationLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Account Reconciliation audit logs.
 */
@Dao
interface ReconciliationDao {

    @Query("SELECT * FROM account_reconciliation_logs WHERE userId = :userId ORDER BY reconciliationTimestamp DESC")
    fun observeReconciliations(userId: String): Flow<List<AccountReconciliationLogEntity>>

    @Query("SELECT * FROM account_reconciliation_logs WHERE userId = :userId AND accountId = :accountId ORDER BY reconciliationTimestamp DESC")
    fun observeReconciliationsForAccount(userId: String, accountId: String): Flow<List<AccountReconciliationLogEntity>>

    @Query("SELECT * FROM account_reconciliation_logs WHERE userId = :userId AND accountId = :accountId ORDER BY reconciliationTimestamp DESC")
    suspend fun getReconciliationsForAccount(userId: String, accountId: String): List<AccountReconciliationLogEntity>

    @Query("SELECT * FROM account_reconciliation_logs WHERE id = :id LIMIT 1")
    suspend fun getReconciliationById(id: String): AccountReconciliationLogEntity?

    @Query("SELECT * FROM account_reconciliation_logs WHERE syncStatus = 'PENDING' ORDER BY reconciliationTimestamp ASC")
    suspend fun getPendingSyncReconciliations(): List<AccountReconciliationLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReconciliation(log: AccountReconciliationLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReconciliations(logs: List<AccountReconciliationLogEntity>)

    @Query("UPDATE account_reconciliation_logs SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("DELETE FROM account_reconciliation_logs WHERE userId = :userId")
    suspend fun deleteReconciliationsByUserId(userId: String): Int
}
