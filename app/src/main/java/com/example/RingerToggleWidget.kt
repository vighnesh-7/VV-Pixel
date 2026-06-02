package com.example

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import android.view.View
import android.widget.RemoteViews

class RingerToggleWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        Log.d(TAG, "onReceive action -> $action")

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        when (action) {
            ACTION_SET_RINGER_VIBRATE -> {
                try {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting vibrate", e)
                }
            }
            ACTION_SET_RINGER_MUTE -> {
                try {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                } catch (e: SecurityException) {
                    Log.e(TAG, "Error setting silent (requires DND permissions), falling back to vibrate", e)
                    android.widget.Toast.makeText(
                        context,
                        "Mute mode requires Do Not Disturb permission. Grant it in VV Pixel Enhancer app's Setup list.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    try {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    } catch (ex: Exception) {}
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting silent", e)
                }
            }
            ACTION_SET_RINGER_UNMUTE -> {
                try {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting normal", e)
                }
            }
        }

        // Update all ringer widgets
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, RingerToggleWidget::class.java)
        val allIds = appWidgetManager.getAppWidgetIds(thisWidget)
        for (id in allIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    companion object {
        private const val TAG = "RingerToggleWidget"
        const val ACTION_SET_RINGER_VIBRATE = "com.example.vvpixel.ACTION_SET_RINGER_VIBRATE"
        const val ACTION_SET_RINGER_MUTE = "com.example.vvpixel.ACTION_SET_RINGER_MUTE"
        const val ACTION_SET_RINGER_UNMUTE = "com.example.vvpixel.ACTION_SET_RINGER_UNMUTE"

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt("appWidgetMinWidth")
            val minHeight = options.getInt("appWidgetMinHeight")

            // Dynamic layout switching: If height exceeds width, render in vertical stack
            val isVertical = minHeight > minWidth && minWidth > 0
            val layoutId = if (isVertical) {
                R.layout.ringer_widget_layout_vertical
            } else {
                R.layout.ringer_widget_layout
            }

            val views = RemoteViews(context.packageName, layoutId)
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val currentMode = audioManager.ringerMode

            val isVibrate = currentMode == AudioManager.RINGER_MODE_VIBRATE
            val isMute = currentMode == AudioManager.RINGER_MODE_SILENT
            val isUnmute = currentMode == AudioManager.RINGER_MODE_NORMAL

            // Update active state view visibilities
            views.setViewVisibility(R.id.img_ringer_mute_active, if (isMute) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.img_ringer_mute_dot, if (isMute) View.GONE else View.VISIBLE)

            views.setViewVisibility(R.id.img_ringer_vibrate_active, if (isVibrate) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.img_ringer_vibrate_dot, if (isVibrate) View.GONE else View.VISIBLE)

            views.setViewVisibility(R.id.img_ringer_unmute_active, if (isUnmute) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.img_ringer_unmute_dot, if (isUnmute) View.GONE else View.VISIBLE)

            // Dynamic Pending Intents triggers on the clickable FrameLayout containers
            views.setOnClickPendingIntent(
                R.id.btn_ringer_mute,
                getPendingIntent(context, ACTION_SET_RINGER_MUTE, 202)
            )
            views.setOnClickPendingIntent(
                R.id.btn_ringer_vibrate,
                getPendingIntent(context, ACTION_SET_RINGER_VIBRATE, 201)
            )
            views.setOnClickPendingIntent(
                R.id.btn_ringer_unmute,
                getPendingIntent(context, ACTION_SET_RINGER_UNMUTE, 203)
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun getPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, RingerToggleWidget::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}
