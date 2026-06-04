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

class CaffeinateTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        try {
            val prefs = getSharedPreferences("caffeinate_prefs", Context.MODE_PRIVATE)
            val isActive = prefs.getBoolean("is_active", false)
            
            val currentTimeout = Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 30000)
            val actuallyLong = currentTimeout >= CAFFEINATE_TIMEOUT
            
            val active = isActive && actuallyLong

            tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = "Caffeinate"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (active) "Keeping screen awake" else "Standard Screen"
            }
            tile.updateTile()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating Caffeinate Tile state", e)
        }
    }

    override fun onClick() {
        super.onClick()
        val context = applicationContext
        if (!Settings.System.canWrite(context)) {
            Toast.makeText(context, "System Settings permission required for Caffeinate", Toast.LENGTH_LONG).show()
            try {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivityAndCollapse(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error launching write settings", e)
            }
            return
        }

        try {
            val prefs = getSharedPreferences("caffeinate_prefs", Context.MODE_PRIVATE)
            val currentTimeout = Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 30000)
            val isActive = prefs.getBoolean("is_active", false)
            val actuallyLong = currentTimeout >= CAFFEINATE_TIMEOUT
            
            val wasCaffeinated = isActive && actuallyLong
            val targetCaffeinated = !wasCaffeinated

            if (targetCaffeinated) {
                // Save current timeout, unless it's already a caffeinated timeout!
                if (!actuallyLong) {
                    prefs.edit().putInt("original_timeout", currentTimeout).apply()
                }
                
                // Set to 10 hours
                val success = Settings.System.putInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, CAFFEINATE_TIMEOUT)
                // In some Rom layers, writing a setting returns success or we verify by reading
                prefs.edit().putBoolean("is_active", true).apply()
                Toast.makeText(context, "Caffeinate ON: Screen will keep awake", Toast.LENGTH_SHORT).show()
            } else {
                // Restore original
                val originalTimeout = prefs.getInt("original_timeout", 30000) // Default to 30s
                
                // Set back to original
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, originalTimeout)
                prefs.edit().putBoolean("is_active", false).apply()
                Toast.makeText(context, "Caffeinate OFF: Screen sleep restored", Toast.LENGTH_SHORT).show()
            }
            updateTile()
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling caffeinate tile", e)
            Toast.makeText(context, "Error toggling Caffeinate tile", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "CaffeinateTile"
        // 10 hours in milliseconds = 36000000 (36M ms)
        private const val CAFFEINATE_TIMEOUT = 36000000
    }
}
