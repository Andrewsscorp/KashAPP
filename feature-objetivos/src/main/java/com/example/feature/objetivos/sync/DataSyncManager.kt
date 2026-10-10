package com.example.feature.objetivos.sync

import com.example.data.local.dao.FinancialDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.FinancialGoalEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

/**
 * Snapshot inmutable emitido en tiempo real por [DataSyncManager].
 */
data class GoalSyncSnapshot(
    val goals: List<FinancialGoalEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val isSyncing: Boolean = false,
    val syncVersion: Long = 0L,
    val sourceTrigger: String = "INITIAL"
)

/**
 * Gestor de sincronización en tiempo real para el módulo :feature-objetivos.
 * Observa de forma reactiva la base de datos Room (`FinancialDao`) asegurando que
 * cualquier creación, edición o débito realizado desde el Dashboard o cualquier otra
 * pantalla se refleje de inmediato en la interfaz de usuario de Objetivos.
 */
class DataSyncManager(
    private val financialDao: FinancialDao,
    val userId: String = "user_default",
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val _manualSyncTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val manualSyncTrigger = _manualSyncTrigger.asSharedFlow()

    private val _syncState = MutableStateFlow(GoalSyncSnapshot(isSyncing = true))
    val syncState = _syncState.asStateFlow()

    private var currentVersion = 0L

    /**
     * Flujo reactivo continuo que combina las metas financieras y las cuentas bancarias de Room.
     * Cualquier modificación en la tabla `financial_goals` o `accounts` disparará una emisión
     * inmediata en este Flow.
     */
    fun observeSyncSnapshot(): Flow<GoalSyncSnapshot> {
        val goalsFlow = financialDao.getAllGoalsFlow(userId).flowOn(ioDispatcher)
        val accountsFlow = financialDao.getAccountsFlow(userId).flowOn(ioDispatcher)

        return combine(goalsFlow, accountsFlow) { goals, accounts ->
            currentVersion++
            GoalSyncSnapshot(
                goals = goals,
                accounts = accounts,
                lastSyncTimestamp = System.currentTimeMillis(),
                isSyncing = false,
                syncVersion = currentVersion,
                sourceTrigger = "ROOM_DATABASE_EMISSION"
            )
        }
            .distinctUntilChanged()
            .onStart {
                _syncState.value = _syncState.value.copy(isSyncing = true)
            }
            .onEach { snapshot ->
                _syncState.value = snapshot
            }
            .flowOn(ioDispatcher)
    }

    /**
     * Observa directamente las metas de Room.
     */
    fun observeGoals(): Flow<List<FinancialGoalEntity>> {
        return financialDao.getAllGoalsFlow(userId)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    /**
     * Observa directamente las cuentas de Room.
     */
    fun observeAccounts(): Flow<List<AccountEntity>> {
        return financialDao.getAccountsFlow(userId)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    /**
     * Notifica un evento externo de mutación (por ejemplo, cuando se crea un objetivo
     * desde el Dashboard) forzando a los observadores a verificar los datos inmediatamente.
     */
    fun notifyExternalMutation(origin: String = "DASHBOARD") {
        currentVersion++
        _manualSyncTrigger.tryEmit(Unit)
        _syncState.value = _syncState.value.copy(
            syncVersion = currentVersion,
            lastSyncTimestamp = System.currentTimeMillis(),
            sourceTrigger = origin
        )
    }

    /**
     * Dispara una sincronización manual inmediata.
     */
    fun triggerSync() {
        notifyExternalMutation("MANUAL_SYNC_TRIGGER")
    }

    /**
     * Obtiene una consulta puntual directa del estado actual de metas en Room.
     */
    suspend fun getGoalById(goalId: String): FinancialGoalEntity? = withContext(ioDispatcher) {
        financialDao.getGoalById(goalId)
    }

    /**
     * Obtiene una cuenta puntual de Room.
     */
    suspend fun getAccountById(accountId: String): AccountEntity? = withContext(ioDispatcher) {
        financialDao.getAccountById(accountId)
    }
}
