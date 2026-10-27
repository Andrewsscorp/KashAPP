package com.example.feature.sync.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Enterprise reactive network connectivity monitor.
 * Automatically detects when device reconnects to Wi-Fi/Cellular
 * and emits events to trigger the Outbox sync engine.
 */
class NetworkConnectivityMonitor(
    private val context: Context,
    private val onReconnected: (suspend () -> Unit)? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isConnected = MutableStateFlow(checkInitialConnectivity())
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        registerNetworkCallback()
    }

    private fun checkInitialConnectivity(): Boolean {
        return try {
            val cm = connectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            Log.w(TAG, "Error checking initial network connectivity: ${e.message}")
            false
        }
    }

    private fun registerNetworkCallback() {
        val cm = connectivityManager ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val wasDisconnected = !_isConnected.value
                Log.i(TAG, "Network connection became available (wasDisconnected=$wasDisconnected)")
                _isConnected.value = true

                if (wasDisconnected && onReconnected != null) {
                    scope.launch {
                        try {
                            Log.i(TAG, "Triggering automatic reconnection sync...")
                            onReconnected.invoke()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error during reconnection sync callback: ${e.message}", e)
                        }
                    }
                }
            }

            override fun onLost(network: Network) {
                Log.w(TAG, "Network connection lost.")
                _isConnected.value = false
            }

            override fun onUnavailable() {
                Log.w(TAG, "Network unavailable.")
                _isConnected.value = false
            }
        }

        try {
            cm.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback: ${e.message}", e)
        }
    }

    fun unregister() {
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
            networkCallback = null
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering network callback: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "NetworkMonitor"
    }
}
