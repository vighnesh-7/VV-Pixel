package com.example

import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

class AdaptiveBrightnessTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        try {
            val isAuto = Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC

            tile.state = if (isAuto) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = "Adaptive Brightness"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (isAuto) "Adaptive" else "Manual"
            }
            tile.updateTile()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating Adaptive Brightness QS Tile state", e)
        }
    }

    override fun onClick() {
        super.onClick()
        if (!Settings.System.canWrite(this)) {
            Log.w(TAG, "Cannot toggle adaptive brightness: WRITE_SETTINGS permission missing")
            // Launch main activity to prompt permissions
            return
        }

        try {
            val currentMode = Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            val nextMode = if (currentMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            } else {
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            }

            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                nextMode
            )
            updateTile()
            Log.d(TAG, "Adaptive Brightness QS Tile toggled. New mode: $nextMode")
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling mode via QS Tile", e)
        }
    }

    companion object {
        private const val TAG = "AdaptiveBrightnessTile"
    }
}
