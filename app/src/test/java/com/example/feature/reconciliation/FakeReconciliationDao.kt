package com.example.feature.reconciliation

import com.example.data.local.dao.ReconciliationDao
import com.example.data.local.entity.AccountReconciliationLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeReconciliationDao : ReconciliationDao {
    private val logs = MutableStateFlow<List<AccountReconciliationLogEntity>>(emptyList())

    override fun observeReconciliations(userId: String): Flow<List<AccountReconciliationLogEntity>> {
        return logs.asStateFlow().map { list -> list.filter { it.userId == userId } }
    }

    override suspend fun getReconciliations(userId: String): List<AccountReconciliationLogEntity> {
        return logs.value.filter { it.userId == userId }
    }

    override fun observeReconciliationsForAccount(userId: String, accountId: String): Flow<List<AccountReconciliationLogEntity>> {
        return logs.asStateFlow().map { list -> list.filter { it.userId == userId && it.accountId == accountId } }
    }

    override suspend fun getReconciliationsForAccount(userId: String, accountId: String): List<AccountReconciliationLogEntity> {
        return logs.value.filter { it.userId == userId && it.accountId == accountId }
    }

    override suspend fun getReconciliationById(id: String): AccountReconciliationLogEntity? {
        return logs.value.find { it.id == id }
    }

    override suspend fun getPendingSyncReconciliations(): List<AccountReconciliationLogEntity> {
        return logs.value.filter { it.syncStatus == "PENDING" }
    }

    override suspend fun insertReconciliation(log: AccountReconciliationLogEntity) {
        logs.value = logs.value.filterNot { it.id == log.id } + log
    }

    override suspend fun insertReconciliations(logsList: List<AccountReconciliationLogEntity>) {
        val ids = logsList.map { it.id }.toSet()
        logs.value = logs.value.filterNot { it.id in ids } + logsList
    }

    override suspend fun updateSyncStatus(id: String, status: String) {
        logs.value = logs.value.map {
            if (it.id == id) it.copy(syncStatus = status) else it
        }
    }

    override suspend fun deleteReconciliationsByUserId(userId: String): Int {
        val count = logs.value.count { it.userId == userId }
        logs.value = logs.value.filterNot { it.userId == userId }
        return count
    }

    fun getAllLogs(): List<AccountReconciliationLogEntity> = logs.value
}
