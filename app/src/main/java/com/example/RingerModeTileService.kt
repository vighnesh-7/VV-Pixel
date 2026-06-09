package com.example

import android.content.Context
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast

class RingerModeTileService : TileService() {

    private var ringerReceiver: android.content.BroadcastReceiver? = null

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()

        // Register dynamic receiver for real-time ringer/silent system changes
        if (ringerReceiver == null) {
            ringerReceiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: android.content.Intent?) {
                    Log.d(TAG, "RingerModeTileService ringer receiver: system ringer mode changed, updating state.")
                    updateTileState()
                    context?.let {
                        RingerToggleWidget.updateAllWidgetsAndTile(it)
                    }
                }
            }
            val filter = android.content.IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(ringerReceiver, filter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(ringerReceiver, filter)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        ringerReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering ringer receiver", e)
            }
            ringerReceiver = null
        }
    }

    override fun onClick() {
        super.onClick()
        // Toggle series: VIBRATE -> NORMAL (Unmute) -> SILENT (Mute) -> VIBRATE
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentMode = audioManager.ringerMode
        var nextMode = when (currentMode) {
            AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_NORMAL
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_SILENT
            else -> AudioManager.RINGER_MODE_VIBRATE
        }

        // Check permission beforehand to bypass SecurityException-driven system redirects
        if (nextMode == AudioManager.RINGER_MODE_SILENT) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            val hasDnd = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                nm?.isNotificationPolicyAccessGranted == true
            } else {
                true
            }
            if (!hasDnd) {
                nextMode = AudioManager.RINGER_MODE_VIBRATE
                Toast.makeText(
                    applicationContext,
                    "Mute requires DND permission. Toggled Vibrate instead. Grant DND in VV Pixel setup.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        try {
            audioManager.ringerMode = nextMode
            val toastMsg = when (nextMode) {
                AudioManager.RINGER_MODE_NORMAL -> "Unmuted"
                AudioManager.RINGER_MODE_SILENT -> "Muted (Silent)"
                else -> "Vibrate"
            }
            Toast.makeText(applicationContext, "Ringer: $toastMsg", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set ringer mode", e)
        }
        updateTileState()
        RingerToggleWidget.updateAllWidgetsAndTile(applicationContext)
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentMode = audioManager.ringerMode

        when (currentMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                tile.state = Tile.STATE_ACTIVE
                Log.d(TAG, "Ringer normal state active")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Unmuted"
                }
                tile.icon = Icon.createWithResource(this, R.drawable.ic_unmute)
            }
            AudioManager.RINGER_MODE_VIBRATE -> {
                tile.state = Tile.STATE_ACTIVE
                Log.d(TAG, "Ringer vibrate state active")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Vibrate"
                }
                tile.icon = Icon.createWithResource(this, R.drawable.ic_vibrate)
            }
            AudioManager.RINGER_MODE_SILENT -> {
                tile.state = Tile.STATE_INACTIVE
                Log.d(TAG, "Ringer silent state active")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Muted"
                }
                tile.icon = Icon.createWithResource(this, R.drawable.ic_mute)
            }
        }
        tile.label = "Ringer Mode"
        tile.updateTile()
    }

    companion object {
        private const val TAG = "RingerModeTileService"
    }
}
