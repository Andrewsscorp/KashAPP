package com.example.feature.capturaautomatica.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

/**
 * Android Quick Settings Tile allowing users to register cash expenses
 * directly from the system notification shade with zero friction.
 */
class QuickCashTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.label = "Gasto Efectivo"
        tile.contentDescription = "Registrar gasto en efectivo rápidamente en KashApp"
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                putExtra(EXTRA_OPEN_QUICK_CASH, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            } ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    QUICK_CASH_REQ_CODE,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(launchIntent)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error iniciando QuickCash desde el Tile: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "QuickCashTileService"
        const val EXTRA_OPEN_QUICK_CASH = "extra_open_quick_cash_dialog"
        private const val QUICK_CASH_REQ_CODE = 8821
    }
}
