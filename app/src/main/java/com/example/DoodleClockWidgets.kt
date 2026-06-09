package com.example

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper utility to read system-wide next alarm and format nicely.
 */
object AlarmFetcher {
    fun getEarliestAlarm(context: Context, fallbackText: String = ""): String {
        return try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val nextAlarm = alarmManager.nextAlarmClock
            if (nextAlarm != null) {
                val triggerTime = nextAlarm.triggerTime
                val date = Date(triggerTime)
                // Force 12-hour format without am/pm for a clean minimalist style
                val formatPattern = "EEE, h:mm"
                val sdf = SimpleDateFormat(formatPattern, Locale.getDefault())
                sdf.format(date)
            } else {
                fallbackText
            }
        } catch (e: Exception) {
            Log.e("AlarmFetcher", "Error reading next alarm clock", e)
            fallbackText
        }
    }
}

/**
 * Helper to launch the device's system clock app.
 */
object WidgetUtils {
    fun getClockIntent(context: Context): Intent {
        val pm = context.packageManager
        
        // 1. Direct explicit system clock components (ensures direct targeting on Pixel, Samsung, etc. bypassing any generic filters)
        val clockComponents = listOf(
            Pair("com.google.android.deskclock", "com.android.deskclock.DeskClock"), // Google Pixel / Nexus / AOSP
            Pair("com.sec.android.app.clockpackage", "com.sec.android.app.clockpackage.ClockPackage"), // Samsung Clock
            Pair("com.android.deskclock", "com.android.deskclock.DeskClock"), // Generic AOSP / Moto / Sony
            Pair("com.oneplus.deskclock", "com.oneplus.deskclock.DeskClock"), // OnePlus
            Pair("com.xiaomi.deskclock", "com.android.deskclock.DeskClock"), // Xiaomi
            Pair("com.huawei.deskclock", "com.android.deskclock.AlarmsMainActivity") // Huawei
        )
        for (comp in clockComponents) {
            try {
                val intent = Intent().setClassName(comp.first, comp.second).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (intent.resolveActivity(pm) != null) {
                    Log.d("WidgetUtils", "Successfully resolved clock app via explicit class component: ${comp.first}/${comp.second}")
                    return intent
                }
            } catch (e: Exception) {
                // Keep exploring
            }
        }
        
        // 2. Fallback to package launcher intents
        val clockPackages = listOf(
            "com.google.android.deskclock",
            "com.sec.android.app.clockpackage",
            "com.android.deskclock",
            "com.huawei.deskclock",
            "com.xiaomi.deskclock",
            "com.oppo.deskclock",
            "com.oneplus.deskclock",
            "com.coloros.deskclock",
            "com.htc.android.worldclock",
            "com.zte.deskclock",
            "com.lenovo.deskclock"
        )
        
        for (pkg in clockPackages) {
            try {
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    Log.d("WidgetUtils", "Resolved clock app launch intent for custom package: $pkg")
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    return launchIntent
                }
            } catch (e: Exception) {
                Log.e("WidgetUtils", "Failed to get launch intent for package: $pkg", e)
            }
        }

        // 3. Last Fallback: Try standard system Alarm Clock actions
        val actions = listOf(
            android.provider.AlarmClock.ACTION_SHOW_ALARMS,
            "android.intent.action.SHOW_ALARMS",
            "android.intent.action.SET_ALARM"
        )
        for (action in actions) {
            val intent = Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(pm) != null) {
                val resolvedActivity = intent.resolveActivity(pm)
                if (resolvedActivity != null && resolvedActivity.packageName != context.packageName) {
                    Log.d("WidgetUtils", "Found system handler for action: $action -> ${resolvedActivity.className}")
                    return intent
                }
            }
        }

        // 4. Final Fallback: standard Alarm Clock ACTION_SHOW_ALARMS intent
        Log.w("WidgetUtils", "No clock packages or action handlers resolved. Using ACTION_SHOW_ALARMS direct fallback.")
        return Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}

/**
 * 1. Doodle Face Cozy Clock Widget
 */
class DoodleFaceClockWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val alarmString = AlarmFetcher.getEarliestAlarm(context, "")
        
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.doodle_face_widget_layout)
            
            if (alarmString.isEmpty()) {
                views.setViewVisibility(R.id.alarm_box, View.GONE)
            } else {
                views.setViewVisibility(R.id.alarm_box, View.VISIBLE)
                views.setTextViewText(R.id.alarm_text, alarmString)
            }
            
            // Intent to launch system clock app on tapping widget card
            val clockIntent = WidgetUtils.getClockIntent(context)
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                clockIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, DoodleFaceClockWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }
}

/**
 * 2. Blob Monster Clock Widget
 */
class BlobMonsterClockWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val alarmString = AlarmFetcher.getEarliestAlarm(context, "")
        
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.blob_monster_widget_layout)
            
            if (alarmString.isEmpty()) {
                views.setViewVisibility(R.id.alarm_box, View.GONE)
            } else {
                views.setViewVisibility(R.id.alarm_box, View.VISIBLE)
                views.setTextViewText(R.id.alarm_text, alarmString)
            }
            
            // Intent to launch system clock app on tapping widget card
            val clockIntent = WidgetUtils.getClockIntent(context)
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId + 1000,
                clockIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, BlobMonsterClockWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }
}

/**
 * 3. Dot Matrix Clock Widget
 */
/**
 * Utility to programmatically construct custom dot matrix time faces for homescreen widgets
 */
object DotMatrixDrawer {
    val dotMatrices = mapOf(
        '0' to arrayOf(
            booleanArrayOf(false, true,  true,  true,  false),
            booleanArrayOf(true,  false, false, false, true),
            booleanArrayOf(true,  false, false, false, true),
            booleanArrayOf(true,  false, false, false, true),
            booleanArrayOf(true,  false, false, false, true),
            booleanArrayOf(true,  false, false, false, true),
            booleanArrayOf(false, true,  true,  true,  false)
        ),
        '1' to arrayOf(
            booleanArrayOf(false, false, true,  false, false),
            booleanArrayOf(false, true,  true,  false, false),
            booleanArrayOf(false, false, true,  false, false),
            booleanArrayOf(false, false, true,  false, false),
            booleanArrayOf(false, false, true,  false, false),
            booleanArrayOf(false, false, true,  false, false),
            booleanArrayOf(false, true,  true,  true,  false)
        ),
        '2' to arrayOf(
            booleanArrayOf(false, true,  true,  true,  false),
            booleanArrayOf(true,  false, false, false, true),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(false, true,  true,  true,  false),
            booleanArrayOf(true,  false, false, false, false),
            booleanArrayOf(true,  false, false, false, false),
            booleanArrayOf(true,  true,  true,  true,  true)
        ),
        '3' to arrayOf(
            booleanArrayOf(false, true,  true,  true,  false),
            booleanArrayOf(true,  false, false, false, true),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(false, false, true,  true,  false),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(true,  false, false, false, true),
            booleanArrayOf(false, true,  true,  true,  false)
        ),
        '4' to arrayOf(
            booleanArrayOf(true,  false, false, false, true),
            booleanArrayOf(true,  false, false, false, true),
            booleanArrayOf(true,  false, false, false, true),
            booleanArrayOf(true,  true,  true,  true,  true),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(false, false, false, false, true)
        ),
        '5' to arrayOf(
            booleanArrayOf(true,  true,  true,  true,  true),
            booleanArrayOf(true,  false, false, false, false),
            booleanArrayOf(true,  true,  true,  true,  false),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(true,  false, false, false, true),
            booleanArrayOf(false, true,  true,  true,  false)
        ),
        '6' to arrayOf(
            booleanArrayOf(false, true,  true,  true,  false),
            booleanArrayOf(true,  false, false, false, true),
            booleanArrayOf(true,  false, false, false, false),
            booleanArrayOf(true,  true,  true,  true,  false),
            booleanArrayOf(true,  false, false, false, true),
            booleanArrayOf(true,  false, false, false, true),
            booleanArrayOf(false, true,  true,  true,  false)
        ),
        '7' to arrayOf(
            booleanArrayOf(true,  true,  true,  true,  true),
            booleanArrayOf(true,  false, false, false, true),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(false, false, false, true,  false),
            booleanArrayOf(false, false, true,  false, false),
            booleanArrayOf(false, false, true,  false, false),
            booleanArrayOf(false, false, true,  false, false)
        ),
        '8' to arrayOf(
            booleanArrayOf(false, true,  true,  true,  false),
            booleanArrayOf(true,  false, false, false, true),
            booleanArrayOf(true,  false, false, false, true),
            booleanArrayOf(false, true,  true,  true,  false),
            booleanArrayOf(true,  false, false, false, true),
            booleanArrayOf(true,  false, false, false, true),
            booleanArrayOf(false, true,  true,  true,  false)
        ),
        '9' to arrayOf(
            booleanArrayOf(false, true,  true,  true,  false),
            booleanArrayOf(true,  false, false, false, true),
            booleanArrayOf(true,  false, false, false, true),
            booleanArrayOf(false, true,  true,  true,  true),
            booleanArrayOf(false, false, false, false, true),
            booleanArrayOf(true,  false, false, false, true),
            booleanArrayOf(false, true,  true,  true,  false)
        ),
        ':' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, true,  false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, true,  false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false)
        ),
        ' ' to arrayOf(
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false)
        )
    )

    fun createDotMatrixBitmap(text: String, density: Float, fontScaleFactor: Float = 1.0f): android.graphics.Bitmap {
        val baseDotSize = 3.6f * density * fontScaleFactor
        val baseDotSpacing = 1.0f * density * fontScaleFactor
        val charSpacing = 3.6f * density * fontScaleFactor
        
        val charWidth = 5 * baseDotSize + 4 * baseDotSpacing
        val charHeight = 7 * baseDotSize + 6 * baseDotSpacing
        
        val totalWidth = text.length * charWidth + (text.length - 1) * charSpacing
        
        val bmpWidth = Math.max(1, Math.round(totalWidth))
        val bmpHeight = Math.max(1, Math.round(charHeight))
        
        val bmp = android.graphics.Bitmap.createBitmap(bmpWidth, bmpHeight, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        
        val paintLit = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }
        val paintUnlit = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(25, 255, 255, 255)
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }
        
        text.forEachIndexed { charIndex, char ->
            val grid = dotMatrices[char] ?: dotMatrices[' ']!!
            val charX = charIndex * (charWidth + charSpacing)
            
            for (r in 0 until 7) {
                for (c in 0 until 5) {
                    val dotX = charX + c * (baseDotSize + baseDotSpacing) + baseDotSize / 2f
                    val dotY = r * (baseDotSize + baseDotSpacing) + baseDotSize / 2f
                    val isActive = grid[r][c]
                    
                    canvas.drawCircle(dotX, dotY, baseDotSize / 2f, if (isActive) paintLit else paintUnlit)
                }
            }
        }
        
        return bmp
    }
}

/**
 * Shared central animation ticker thread to synchronize animations and conserve battery life.
 */
object WidgetAnimator {
    private var handler: android.os.Handler? = null
    private var runnable: Runnable? = null
    
    var nothingLedProgress = 0f
    
    fun start(context: Context) {
        if (handler != null) return
        val appContext = context.applicationContext
        handler = android.os.Handler(android.os.Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                val appWidgetManager = AppWidgetManager.getInstance(appContext)
                val nothingIds = appWidgetManager.getAppWidgetIds(ComponentName(appContext, NothingPixelClockWidget::class.java))
                
                // If no widgets are placed on the homescreen, kill the background run loop immediately!
                if (nothingIds.isEmpty()) {
                    stop()
                    return
                }
                
                // Advance animations
                nothingLedProgress = (nothingLedProgress + 0.04f) % 1.0f
                
                if (nothingIds.isNotEmpty()) {
                    updateNothingPixel(appContext, appWidgetManager, nothingIds)
                }
                
                handler?.postDelayed(this, 150) // Smooth, battery-optimized 150ms updates
            }
        }
        handler?.post(runnable!!)
    }
    
    fun stop() {
        handler?.removeCallbacks(runnable ?: return)
        handler = null
        runnable = null
    }
    
    private fun updateNothingPixel(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val alarmString = AlarmFetcher.getEarliestAlarm(context, "")
        val density = context.resources.displayMetrics.density
        
        // Large and exceptionally crisp centered time text dot matrix bitmap (fontScaleFactor set to 2.0f)
        val timeStr = SimpleDateFormat("h:mm", Locale.getDefault()).format(Date())
        val timeBmp = DotMatrixDrawer.createDotMatrixBitmap(timeStr, density, 2.0f)
        
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.nothing_pixel_widget_layout)
            
            if (alarmString.isEmpty()) {
                views.setViewVisibility(R.id.alarm_box, View.GONE)
            } else {
                views.setViewVisibility(R.id.alarm_box, View.VISIBLE)
                views.setTextViewText(R.id.alarm_text, alarmString)
            }
            
            views.setImageViewBitmap(R.id.widget_time_img, timeBmp)
            
            val clockIntent = WidgetUtils.getClockIntent(context)
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId + 3000,
                clockIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

/**
 * 3. Nothing Pixel Clock Widget
 */
class NothingPixelClockWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetAnimator.start(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIME_TICK ||
            action == Intent.ACTION_TIMEZONE_CHANGED) {
            WidgetAnimator.start(context)
        }
    }
}

/**
 * 4. iOS Liquid Glass Clock Widget
 */
class LiquidGlassClockWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val alarmString = AlarmFetcher.getEarliestAlarm(context, "")
        
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.liquid_glass_widget_layout)
            
            if (alarmString.isEmpty()) {
                views.setViewVisibility(R.id.alarm_box, View.GONE)
            } else {
                views.setViewVisibility(R.id.alarm_box, View.VISIBLE)
                views.setTextViewText(R.id.alarm_text, alarmString)
            }
            
            val clockIntent = WidgetUtils.getClockIntent(context)
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId + 4000,
                clockIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, LiquidGlassClockWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }
}
