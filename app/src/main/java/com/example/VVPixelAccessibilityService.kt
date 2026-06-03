package com.example

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class VVPixelAccessibilityService : AccessibilityService() {

    private var isRegistered = false

    private fun triggerFeedback() {
        try {
            val prefs = getSharedPreferences("com.example.vvpixel.SETTINGS", Context.MODE_PRIVATE)
            val vibrationEnabled = prefs.getBoolean("lock_vibration_enabled", true)
            if (vibrationEnabled) {
                val intensity = prefs.getFloat("lock_vibration_intensity", 0.5f)
                val duration = (10 + (intensity * 40)).toLong() // 10ms to 50ms (subtle locks are shorter)
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
        isServiceRunning = true
    }

    private var lastHomeClickTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val evt = event ?: return
        if (evt.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val pkg = evt.packageName?.toString() ?: ""
            if (pkg.contains("launcher") || pkg.contains("nexuslauncher")) {
                val className = evt.className?.toString() ?: ""
                Log.d(TAG, "Launcher click detected: class=$className")
                // Intercept clicks on homescreen background container layouts where launcher icons do not reside
                if (className.contains("Workspace") || className.contains("CellLayout") || 
                    className.contains("DragLayer") || className.contains("Launcher") || 
                    className.contains("NoClickThroughLayout")) {
                    
                    val currentTime = System.currentTimeMillis()
                    val diff = currentTime - lastHomeClickTime
                    
                    val prefs = getSharedPreferences("com.example.vvpixel.SETTINGS", Context.MODE_PRIVATE)
                    val isDoubleTapEnabled = prefs.getBoolean("double_tap_to_lock_enabled", true)
                    
                    if (isDoubleTapEnabled && diff in 80..400) {
                        Log.d(TAG, "Empty space double tap detected! Performing Lock screen global action.")
                        triggerFeedback()
                        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                    }
                    lastHomeClickTime = currentTime
                }
            }
        }
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "VV Pixel Accessibility Service Destroyed.")
        if (isRegistered) {
            try {
                unregisterReceiver(receiver)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver", e)
            }
            isRegistered = false
        }
        isServiceRunning = false
    }

    companion object {
        private const val TAG = "VVPixelAccessService"
        const val ACTION_LOCK = "com.example.vvpixel.ACTION_LOCK"
        var isServiceRunning = false
    }
}
