package com.example.feature.capturaautomatica.di

import android.content.Context
import android.util.Log
import com.example.feature.capturaautomatica.domain.repository.AutoCaptureRepository

/**
 * Entry point for decoupling Android system components (NotificationListenerService,
 * BroadcastReceiver, TileService) from the Application DI container.
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

        // Fallback: Attempt reflection on Application.appContainer
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
        return null
    }

    suspend fun getActiveUserId(context: Context? = null): String? {
        val provider = activeUserIdProvider
        if (provider != null) {
            return provider.invoke()
        }
        if (context == null) return null

        // Fallback: Attempt reflection on Application.appContainer.authRepository.observeCurrentUserFlow() or similar
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
                    val session = userSessionDao?.getActiveSession()
                    return session?.userId
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Reflection fallback to get active user ID failed: ${e.message}")
        }
        return null
    }
}
