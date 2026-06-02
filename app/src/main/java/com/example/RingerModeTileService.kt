package com.example

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

class RingerModeTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        showRingerChoicesDialog()
    }

    private fun showRingerChoicesDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_ringer_choices)

        val window = dialog.window
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent)
            val lp = window.attributes
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            
            // Background blur on Android 12+ (SDK 31+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.setBackgroundBlurRadius(30)
            }
            window.attributes = lp
        }

        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true) // touch outside exits dialog

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentMode = audioManager.ringerMode

        // Find dialog views
        val choiceMute = dialog.findViewById<View>(R.id.choice_mute)
        val choiceVibrate = dialog.findViewById<View>(R.id.choice_vibrate)
        val choiceUnmute = dialog.findViewById<View>(R.id.choice_unmute)
        val btnDismiss = dialog.findViewById<View>(R.id.btn_dialog_dismiss)

        val iconMute = dialog.findViewById<ImageView>(R.id.icon_mute)
        val iconVibrate = dialog.findViewById<ImageView>(R.id.icon_vibrate)
        val iconUnmute = dialog.findViewById<ImageView>(R.id.icon_unmute)

        val textMute = dialog.findViewById<TextView>(R.id.text_mute)
        val textVibrate = dialog.findViewById<TextView>(R.id.text_vibrate)
        val textUnmute = dialog.findViewById<TextView>(R.id.text_unmute)

        // Highlight the current ringer mode using pristine high-contrast Active plates
        when (currentMode) {
            AudioManager.RINGER_MODE_SILENT -> {
                choiceMute.setBackgroundResource(R.drawable.ringer_btn_active)
                iconMute.setColorFilter(Color.parseColor("#1D1B20"))
                textMute.setTextColor(Color.parseColor("#1D1B20"))
                textMute.setTypeface(null, Typeface.BOLD)
            }
            AudioManager.RINGER_MODE_VIBRATE -> {
                choiceVibrate.setBackgroundResource(R.drawable.ringer_btn_active)
                iconVibrate.setColorFilter(Color.parseColor("#1D1B20"))
                textVibrate.setTextColor(Color.parseColor("#1D1B20"))
                textVibrate.setTypeface(null, Typeface.BOLD)
            }
            AudioManager.RINGER_MODE_NORMAL -> {
                choiceUnmute.setBackgroundResource(R.drawable.ringer_btn_active)
                iconUnmute.setColorFilter(Color.parseColor("#1D1B20"))
                textUnmute.setTextColor(Color.parseColor("#1D1B20"))
                textUnmute.setTypeface(null, Typeface.BOLD)
            }
        }

        // Action listeners
        choiceMute.setOnClickListener {
            setRingerMode(AudioManager.RINGER_MODE_SILENT)
            dialog.dismiss()
        }
        choiceVibrate.setOnClickListener {
            setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
            dialog.dismiss()
        }
        choiceUnmute.setOnClickListener {
            setRingerMode(AudioManager.RINGER_MODE_NORMAL)
            dialog.dismiss()
        }

        btnDismiss.setOnClickListener {
            dialog.dismiss()
        }

        // Launch Dialog from TileService
        showDialog(dialog)
    }

    private fun setRingerMode(mode: Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try {
            audioManager.ringerMode = mode
        } catch (e: SecurityException) {
            Log.e(TAG, "DND Security exception trying to set silent mode, falling back to vibrate", e)
            Toast.makeText(
                this,
                "Mute mode requires Do Not Disturb permission. Grant it in VV Pixel Enhancer app's Setup list.",
                Toast.LENGTH_LONG
            ).show()
            try {
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            } catch (ex: Exception) {}
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set ringer mode", e)
        }
        updateTileState()
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
