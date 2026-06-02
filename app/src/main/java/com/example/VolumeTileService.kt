package com.example

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

class VolumeTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_ACTIVE
        tile.label = "Quick Volume"
        tile.icon = android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_volume_equalizer)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = "Overlay Panel"
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        Log.d(TAG, "Volume QS Tile Clicked. Opening volume overlay activity.")
        
        val intent = Intent(this, VolumeControlActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or 
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or 
                    Intent.FLAG_ACTIVITY_NO_HISTORY
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Volume Control overlay activity", e)
        }
    }

    companion object {
        private const val TAG = "VolumeTileService"
    }
}
