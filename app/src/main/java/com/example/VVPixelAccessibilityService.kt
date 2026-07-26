package com.example

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.applock.AppLockActivity

class VVPixelAccessibilityService : AccessibilityService() {

    private var isRegistered = false
    private val handler = Handler(Looper.getMainLooper())
    private var lastForegroundPackage: String? = null

    private fun triggerFeedback() {
        try {
            val prefs = getSharedPreferences("com.example.vvpixel.SETTINGS", Context.MODE_PRIVATE)
            val vibrationEnabled = prefs.getBoolean("lock_vibration_enabled", true)
            if (vibrationEnabled) {
                val intensity = prefs.getFloat("lock_vibration_intensity", 0.5f)
                val duration = (10 + (intensity * 40)).toLong()
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                vibrator?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val amplitude = (50 + (intensity * 205)).toInt().coerceIn(1, 255)
                        try {
                            it.vibrate(android.os.VibrationEffect.createOneShot(duration, amplitude))
                        } catch (e: Exception) {
                            try { it.vibrate(duration) } catch (ex: Exception) {}
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        try {
                            it.vibrate(duration)
                        } catch (ex: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering lock vibration feedback", e)
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Screen Lock broadcast received: ${intent?.action}")
            if (intent?.action == ACTION_LOCK) {
                triggerFeedback()
                val success = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                Log.d(TAG, "Lock screen execution result: $success")
            }
        }
    }

    private val ringerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Ringer mode changed broadcast received dynamically in AccessibilityService.")
            context?.let {
                RingerToggleWidget.updateAllWidgetsAndTile(it)
            }
        }
    }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                sessionUnlocked.clear()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "VV Pixel Accessibility Service Connected.")

        val filter = IntentFilter(ACTION_LOCK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
        isRegistered = true

        val ringerFilter = IntentFilter(android.media.AudioManager.RINGER_MODE_CHANGED_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(ringerReceiver, ringerFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(ringerReceiver, ringerFilter)
        }

        val screenOffFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenOffReceiver, screenOffFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(screenOffReceiver, screenOffFilter)
        }

        isServiceRunning = true
    }

    private var lastHomeClickTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val evt = event ?: return

        // 1. Double tap home to lock gesture detection
        if (evt.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val pkg = evt.packageName?.toString() ?: ""
            if (pkg.contains("launcher") || pkg.contains("nexuslauncher")) {
                val className = evt.className?.toString() ?: ""
                if (className.contains("Workspace") || className.contains("CellLayout") ||
                    className.contains("DragLayer") || className.contains("Launcher") ||
                    className.contains("NoClickThroughLayout")) {

                    val currentTime = System.currentTimeMillis()
                    val diff = currentTime - lastHomeClickTime

                    val prefs = getSharedPreferences("com.example.vvpixel.SETTINGS", Context.MODE_PRIVATE)
                    val isDoubleTapEnabled = prefs.getBoolean("double_tap_to_lock_enabled", true)

                    if (isDoubleTapEnabled && diff in 80..400) {
                        triggerFeedback()
                        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                    }
                    lastHomeClickTime = currentTime
                }
            }
        }

        // 2. App Lock Gate Interception
        if (evt.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = evt.packageName?.toString() ?: return

            // Ignore system UI and self
            if (packageName == this.packageName || packageName.contains("systemui") || packageName.contains("launcher") || packageName.contains("nexuslauncher")) {
                scheduleRelockCheck()
                return
            }

            val prefs = getSharedPreferences("com.example.vvpixel.SETTINGS", Context.MODE_PRIVATE)
            val isMasterEnabled = prefs.getBoolean("app_lock_master_enabled", false)
            val lockedPackages = prefs.getStringSet("app_lock_locked_packages", emptySet()) ?: emptySet()

            if (!isMasterEnabled || !lockedPackages.contains(packageName)) {
                lastForegroundPackage = packageName
                return
            }

            if (sessionUnlocked.contains(packageName)) {
                lastForegroundPackage = packageName
                return
            }

            // Launch App Lock Gate
            lastForegroundPackage = packageName
            launchAppLockGate(packageName)
        }
    }

    private fun launchAppLockGate(targetPackage: String) {
        performGlobalAction(GLOBAL_ACTION_HOME)
        handler.postDelayed({
            val intent = Intent(this, AppLockActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("TARGET_PACKAGE", targetPackage)
            }
            startActivity(intent)
        }, 150)
    }

    private fun scheduleRelockCheck() {
        val checkRunnable = Runnable {
            try {
                val am = getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                val runningProcesses = am?.runningAppProcesses?.map { it.processName }?.toSet() ?: emptySet()

                sessionUnlocked.toList().forEach { pkg ->
                    val isRunning = runningProcesses.any { it == pkg || it.startsWith("$pkg:") }
                    if (!isRunning) {
                        sessionUnlocked.remove(pkg)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Relock check error", e)
            }
        }
        handler.postDelayed(checkRunnable, 300)
        handler.postDelayed(checkRunnable, 1500)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "VV Pixel Accessibility Service Destroyed.")
        if (isRegistered) {
            try { unregisterReceiver(receiver) } catch (e: Exception) {}
            try { unregisterReceiver(ringerReceiver) } catch (e: Exception) {}
            try { unregisterReceiver(screenOffReceiver) } catch (e: Exception) {}
            isRegistered = false
        }
        isServiceRunning = false
    }

    companion object {
        private const val TAG = "VVPixelAccessService"
        const val ACTION_LOCK = "com.example.vvpixel.ACTION_LOCK"
        var isServiceRunning = false
        val sessionUnlocked = mutableSetOf<String>()

        fun notifyAppUnlocked(packageName: String, context: Context) {
            if (packageName.isNotBlank()) {
                sessionUnlocked.add(packageName)
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                launchIntent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    context.startActivity(it)
                }
            }
        }
    }
}
