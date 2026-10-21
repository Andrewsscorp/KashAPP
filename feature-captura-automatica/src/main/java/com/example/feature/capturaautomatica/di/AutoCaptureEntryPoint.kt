package com.example.feature.capturaautomatica.di

import android.content.Context
import android.util.Log
import com.example.feature.capturaautomatica.data.repository.AutoCaptureRepositoryImpl
import com.example.feature.capturaautomatica.domain.repository.AutoCaptureRepository

/**
 * Entry point para desacoplar componentes del sistema Android (NotificationListenerService,
 * BroadcastReceiver, TileService) del ciclo de vida de la UI, garantizando funcionamiento
 * incluso si la aplicación se encuentra totalmente CERRADA.
 */
object AutoCaptureEntryPoint {
    private const val TAG = "AutoCaptureEntryPoint"

    @Volatile
    private var repository: AutoCaptureRepository? = null

    @Volatile
    private var activeUserIdProvider: (suspend () -> String?)? = null

    fun setRepository(repo: AutoCaptureRepository) {
        this.repository = repo
    }

    fun setActiveUserIdProvider(provider: suspend () -> String?) {
        this.activeUserIdProvider = provider
    }

    fun getRepository(context: Context? = null): AutoCaptureRepository? {
        val existing = repository
        if (existing != null) return existing
        if (context == null) return null

        // Fallback 1: Intento por reflexión en Application.appContainer
        try {
            val app = context.applicationContext
            val getAppContainerMethod = app.javaClass.getMethod("getAppContainer")
            val container = getAppContainerMethod.invoke(app)
            if (container != null) {
                val getRepoMethod = container.javaClass.getMethod("getAutoCaptureRepository")
                val resolved = getRepoMethod.invoke(container) as? AutoCaptureRepository
                if (resolved != null) {
                    repository = resolved
                    return resolved
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Reflection fallback to get AutoCaptureRepository failed: ${e.message}")
        }

        // Fallback 2: Instanciación directa con RoomLocalDb (Garantía crítica ante app cerrada/muerta)
        try {
            val db = com.example.data.local.RoomLocalDb.getDatabase(context.applicationContext)
            val fallbackRepo = AutoCaptureRepositoryImpl(
                autoCaptureDao = db.autoCaptureDao(),
                financialDao = db.financialDao(),
                merchantDao = db.merchantDao(),
                auditSecurityLogDao = db.auditSecurityLogDao(),
                accountProvider = { uid -> db.financialDao().getAccounts(uid) },
                categoryProvider = { emptyList() }
            )
            repository = fallbackRepo
            Log.i(TAG, "AutoCaptureRepository creado exitosamente mediante fallback directo a RoomLocalDb")
            return fallbackRepo
        } catch (e: Throwable) {
            Log.e(TAG, "Fallback 2 to create AutoCaptureRepository failed: ${e.message}", e)
        }

        return null
    }

    suspend fun getActiveUserId(context: Context? = null): String {
        val provider = activeUserIdProvider
        if (provider != null) {
            val id = provider.invoke()
            if (!id.isNullOrBlank()) return id
        }
        if (context == null) return "user_default"

        // Fallback 1: Intento por reflexión en Application.appContainer
        try {
            val app = context.applicationContext
            val getAppContainerMethod = app.javaClass.getMethod("getAppContainer")
            val container = getAppContainerMethod.invoke(app)
            if (container != null) {
                val getSecurityDbMethod = container.javaClass.getMethod("getSecurityDatabase")
                val securityDb = getSecurityDbMethod.invoke(container)
                if (securityDb != null) {
                    val getUserSessionDaoMethod = securityDb.javaClass.getMethod("userSessionDao")
                    val userSessionDao = getUserSessionDaoMethod.invoke(securityDb) as? com.example.data.local.dao.UserSessionDao
                    val session = userSessionDao?.getActiveSession() ?: userSessionDao?.getLastSession()
                    val id = session?.userId
                    if (!id.isNullOrBlank()) return id
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Reflection fallback to get active user ID failed: ${e.message}")
        }

        // Fallback 2: Acceso directo a RoomLocalDb
        try {
            val db = com.example.data.local.RoomLocalDb.getDatabase(context.applicationContext)
            val session = db.userSessionDao().getActiveSession() ?: db.userSessionDao().getLastSession()
            val id = session?.userId
            if (!id.isNullOrBlank()) return id
        } catch (e: Throwable) {
            Log.w(TAG, "Direct DB fallback to get active user ID failed: ${e.message}")
        }

        return "user_default"
    }
}
