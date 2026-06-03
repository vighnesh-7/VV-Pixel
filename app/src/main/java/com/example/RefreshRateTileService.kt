package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast

class RefreshRateTileService : TileService() {

    private val TAG = "RefreshRateTile"

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun getPeakRefreshRate(): Float {
        // Highly resilient reading across different manufacturer implementations
        return try {
            Settings.System.getFloat(contentResolver, "peak_refresh_rate")
        } catch (e: Exception) {
            try {
                Settings.System.getInt(contentResolver, "peak_refresh_rate").toFloat()
            } catch (ex: Exception) {
                try {
                    val strVal = Settings.System.getString(contentResolver, "peak_refresh_rate")
                    strVal?.toFloatOrNull() ?: 60f
                } catch (e3: Exception) {
                    60f
                }
            }
        }
    }

    override fun onClick() {
        super.onClick()
        val context = applicationContext
        if (Settings.System.canWrite(context)) {
            try {
                val currentPeak = getPeakRefreshRate()
                val isSmoothEnabled = currentPeak > 60.1f

                val targetRate = if (isSmoothEnabled) 60f else 120f
                var writeSuccessful = false

                // 1. Try to toggle peak_refresh_rate as Float
                try {
                    Settings.System.putFloat(contentResolver, "peak_refresh_rate", targetRate)
                    writeSuccessful = true
                } catch (e: Exception) {
                    // Fallback to Int
                    try {
                        Settings.System.putInt(contentResolver, "peak_refresh_rate", targetRate.toInt())
                        writeSuccessful = true
                    } catch (e2: Exception) {
                        Log.e(TAG, "Failed to write peak_refresh_rate to Settings.System", e2)
                    }
                }

                // 2. Try to toggle min_refresh_rate in isolation (Does NOT block the overall success if it fails)
                try {
                    Settings.System.putFloat(contentResolver, "min_refresh_rate", targetRate)
                } catch (e: Exception) {
                    try {
                        Settings.System.putInt(contentResolver, "min_refresh_rate", targetRate.toInt())
                    } catch (e2: Exception) {
                        Log.d(TAG, "Optional min_refresh_rate failed to write (this is normal on some hardware)")
                    }
                }

                // 3. Try to toggle user_refresh_rate as Int in isolation (Optional ROM fallback)
                try {
                    Settings.System.putInt(contentResolver, "user_refresh_rate", targetRate.toInt())
                } catch (e: Exception) {}

                if (writeSuccessful) {
                    val stateText = if (isSmoothEnabled) "60Hz" else "120Hz (Smooth)"
                    Log.d(TAG, "Toggled Peak Refresh Rate to $targetRate")
                    Toast.makeText(context, "Refresh Rate: $stateText", Toast.LENGTH_SHORT).show()
                    updateTileState()
                } else {
                    // Try setting user_refresh_rate only as last resort if peak_refresh_rate couldn't be written
                    throw Exception("Could not write custom refresh rate setting")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing system refresh rate settings", e)
                Toast.makeText(context, "Error toggling Refresh Rate", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Write System Settings permission required to toggle Refresh Rate", Toast.LENGTH_LONG).show()
            try {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivityAndCollapse(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error launching write settings", e)
            }
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        try {
            val currentPeak = getPeakRefreshRate()
            val isEnabled = currentPeak > 60.1f

            if (isEnabled) {
                tile.state = Tile.STATE_ACTIVE
                tile.subtitle = "Smooth (120Hz)"
            } else {
                tile.state = Tile.STATE_INACTIVE
                tile.subtitle = "Standard (60Hz)"
            }
            tile.updateTile()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating RefreshRate tile state", e)
            tile.state = Tile.STATE_INACTIVE
            tile.subtitle = "Standard"
            tile.updateTile()
        }
    }
}
