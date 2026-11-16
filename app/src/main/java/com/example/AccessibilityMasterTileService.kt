package com.example

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

class AccessibilityMasterTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        val isSystemEnabled = VVPixelAccessibilityService.isServiceEnabledInSystem(this)
        val isMasterPrefEnabled = AccessibilityMasterState.isEnabled(this)

        if (isSystemEnabled && isMasterPrefEnabled) {
            // Service is currently active -> Turn it OFF
            AccessibilityMasterState.setEnabled(this, false)
            val disabledDirectly = VVPixelAccessibilityService.disableCurrentService(this)

            val tile = qsTile
            if (tile != null) {
                tile.state = Tile.STATE_INACTIVE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Off"
                }
                tile.updateTile()
            }

            if (!disabledDirectly) {
                openAccessibilitySettings()
            }
        } else {
            // Service is currently off/paused -> Turn it ON
            AccessibilityMasterState.setEnabled(this, true)

            if (!isSystemEnabled) {
                // Not enabled in OS -> Open Accessibility Settings to allow enabling
                openAccessibilitySettings()
            }
        }

        refreshTile()
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching accessibility settings", e)
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val isSystemEnabled = VVPixelAccessibilityService.isServiceEnabledInSystem(this)
        val isMasterPrefEnabled = AccessibilityMasterState.isEnabled(this)
        val isEffectivelyActive = isSystemEnabled && isMasterPrefEnabled

        tile.state = if (isEffectivelyActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "VV Features"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_a11y_tile)

        // API 29+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isEffectivelyActive) "Active" else if (isSystemEnabled && !isMasterPrefEnabled) "Paused" else "Off"
        }

        tile.updateTile()
    }

    companion object {
        const val TAG = "A11yMasterTileService"

        fun requestRefresh(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    requestListeningState(
                        context,
                        ComponentName(context, AccessibilityMasterTileService::class.java)
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed requesting listening state", e)
                }
            }
        }
    }
}
