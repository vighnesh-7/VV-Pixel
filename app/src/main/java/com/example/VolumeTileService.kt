package com.example

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

class VolumeTileService : TileService() {

    // Cached once — Icon.createWithResource is cheap but pointless to
    // re-allocate on every onStartListening().
    private val tileIcon: Icon by lazy {
        Icon.createWithResource(this, R.drawable.ic_volume_equalizer)
    }

    // FIX (root cause 1): Build the Intent + PendingIntent once, lazily,
    // instead of allocating fresh objects inside onClick() on every tap.
    // Each PendingIntent.getActivity() call involves a Binder IPC to
    // ActivityManagerService — doing it lazily means the first tap pays
    // that cost, but subsequent taps (the repeated taps the user is doing)
    // return the cached object instantly.
    private val overlayIntent: Intent by lazy {
        Intent(this, VolumeControlActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_HISTORY
        }
    }

    private val overlayPendingIntent: PendingIntent by lazy {
        PendingIntent.getActivity(
            this, 0, overlayIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onTileAdded() {
        super.onTileAdded()
        // FIX (root cause 2 — "3+ clicks" main cause):
        // Eagerly evaluate both lazy properties the moment the tile is added
        // to the user's QS panel. This means by the first click, the IPC
        // call to build the PendingIntent has already happened and the result
        // is cached. Without this, the FIRST tap always pays the cold IPC
        // cost and appears to "do nothing," prompting repeated taps.
        overlayPendingIntent // access forces lazy init
    }

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return

        // FIX (root cause 3): removed the two redundant updateTile() calls
        // that were in the old onClick() (STATE_INACTIVE flip + STATE_ACTIVE
        // flip in finally). Each updateTile() is a synchronous Binder call
        // that blocked the onClick() thread before startActivityAndCollapse
        // could fire. Now updateTile() only runs here in onStartListening —
        // the tile refreshes when the panel opens, not on every click.
        tile.state = Tile.STATE_ACTIVE
        tile.label = "Volume"
        tile.icon = tileIcon
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = "Media"
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        Log.d(TAG, "Volume tile clicked")
        // FIX: onClick() now does EXACTLY one thing — fire the pending intent.
        // No updateTile() calls, no state flips, no object allocation.
        // startActivityAndCollapse is itself async (it posts to the system
        // handler), so the sooner we call it after onClick(), the sooner
        // the activity appears. Every line removed from onClick() directly
        // reduces perceived latency.
        try {
            startActivityAndCollapse(overlayPendingIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch volume overlay", e)
        }
    }

    companion object {
        private const val TAG = "VolumeTileService"
    }
}
