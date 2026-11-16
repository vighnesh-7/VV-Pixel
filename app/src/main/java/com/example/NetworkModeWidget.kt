package com.example

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.ContextCompat

class NetworkModeWidget : AppWidgetProvider() {

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
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    companion object {
        private const val TAG = "NetworkModeWidget"

        fun updateAllWidgets(context: Context) {
            val appContext = context.applicationContext
            try {
                val appWidgetManager = AppWidgetManager.getInstance(appContext)
                val component = ComponentName(appContext, NetworkModeWidget::class.java)
                val ids = appWidgetManager.getAppWidgetIds(component)
                for (id in ids) {
                    updateWidget(appContext, appWidgetManager, id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating 5G/LTE widgets", e)
            }
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.network_mode_widget_layout)
            val currentMode = NetworkToggleManager.getSavedMode(context)
            val is5g = currentMode == NetworkToggleManager.NetworkMode.FIVE_G

            views.setImageViewResource(
                R.id.img_network_mode,
                if (is5g) R.drawable.ic_network_5g else R.drawable.ic_network_lte
            )
            views.setInt(
                R.id.btn_network_toggle,
                "setBackgroundResource",
                if (is5g) R.drawable.network_widget_bg_5g else R.drawable.network_widget_bg_lte
            )
            val tintColor = ContextCompat.getColor(
                context,
                if (is5g) R.color.widget_btn_active_icon else R.color.lock_widget_icon
            )
            views.setInt(R.id.img_network_mode, "setColorFilter", tintColor)
            views.setContentDescription(
                R.id.img_network_mode,
                if (is5g) "5G Active, tap to switch to LTE" else "LTE Active, tap to switch to 5G"
            )

            val pendingIntent = NetworkToggleManager.buildTrampolinePendingIntent(
                context = context,
                requestCode = 402,
                targetMode = NetworkToggleManager.TargetMode.AUTO_TOGGLE
            )
            views.setOnClickPendingIntent(R.id.btn_network_toggle, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
