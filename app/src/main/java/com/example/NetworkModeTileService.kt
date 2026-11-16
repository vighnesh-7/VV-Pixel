package com.example

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

class NetworkModeTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = NetworkToggleManager.buildTileLaunchPendingIntent(this)
                startActivityAndCollapse(pendingIntent)
            } else {
                val launchIntent = NetworkToggleManager.prepareToggleAndBuildLaunchIntent(
                    this,
                    NetworkToggleManager.TargetMode.AUTO_TOGGLE
                )
                @Suppress("DEPRECATION")
                startActivityAndCollapse(launchIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling 5G/LTE Quick Settings tile click", e)
            NetworkToggleManager.disarmSession()
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val currentMode = NetworkToggleManager.getSavedMode(this)
        val is5g = currentMode == NetworkToggleManager.NetworkMode.FIVE_G

        tile.state = if (is5g) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.network_tile_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = currentMode.label
        }
        tile.icon = Icon.createWithResource(
            this,
            if (is5g) R.drawable.ic_network_5g else R.drawable.ic_network_lte
        )
        tile.updateTile()
    }

    companion object {
        private const val TAG = "NetworkModeTileService"

        fun requestRefresh(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    requestListeningState(
                        context.applicationContext,
                        ComponentName(context.applicationContext, NetworkModeTileService::class.java)
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Could not request QS tile refresh", e)
                }
            }
        }
    }
}
