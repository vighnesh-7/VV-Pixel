package com.example

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

object NetworkToggleManager {

    private const val TAG = "NetworkToggleManager"
    const val PREFS_NAME = "com.example.vvpixel.SETTINGS"
    const val KEY_NETWORK_MODE = "network_mode_pref"

    const val ACTION_TOGGLE_NETWORK_MODE = "com.example.vvpixel.ACTION_TOGGLE_NETWORK_MODE"
    const val EXTRA_TARGET_MODE = "extra_target_mode"
    private const val SHORTCUT_ID = "vv_pixel_network_5g_lte"
    private const val SESSION_TTL_MS = 8_000L

    enum class NetworkMode(val prefValue: String, val label: String) {
        FIVE_G("5G", "5G"),
        LTE("LTE", "LTE");

        companion object {
            fun fromPref(value: String?): NetworkMode {
                return if (value == LTE.prefValue) LTE else FIVE_G
            }
        }
    }

    enum class TargetMode {
        AUTO_TOGGLE,
        FORCE_5G,
        FORCE_LTE
    }

    enum class AutomationStep {
        IDLE,
        WAIT_SETTINGS_LIST,
        WAIT_PREF_DIALOG,
        DONE
    }

    data class SessionSnapshot(
        val step: AutomationStep,
        val targetMode: TargetMode,
        val expiresAtElapsed: Long,
        val scrollAttempts: Int,
        val lastScrollAtElapsed: Long,
        val lastClickAtElapsed: Long,
        val enteredSettings: Boolean
    )

    private val sessionLock = Any()
    private var currentStep: AutomationStep = AutomationStep.IDLE
    private var currentTarget: TargetMode = TargetMode.AUTO_TOGGLE
    private var expiresAtElapsed: Long = 0L
    private var scrollAttempts: Int = 0
    private var lastScrollAtElapsed: Long = 0L
    private var lastClickAtElapsed: Long = 0L
    private var enteredSettings: Boolean = false

    fun getSavedMode(context: Context): NetworkMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return NetworkMode.fromPref(prefs.getString(KEY_NETWORK_MODE, NetworkMode.FIVE_G.prefValue))
    }

    fun saveMode(context: Context, mode: NetworkMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NETWORK_MODE, mode.prefValue)
            .apply()
    }

    fun isAccessibilityAutomationReady(context: Context): Boolean {
        return AccessibilityMasterState.isEnabled(context) &&
            VVPixelAccessibilityService.isServiceEnabledInSystem(context)
    }

    fun armSession(targetMode: TargetMode = TargetMode.AUTO_TOGGLE) {
        synchronized(sessionLock) {
            val now = SystemClock.elapsedRealtime()
            currentStep = AutomationStep.WAIT_SETTINGS_LIST
            currentTarget = targetMode
            expiresAtElapsed = now + SESSION_TTL_MS
            scrollAttempts = 0
            lastScrollAtElapsed = 0L
            lastClickAtElapsed = 0L
            enteredSettings = false
            Log.d(TAG, "Armed 5G/LTE toggle session (target=$targetMode, ttl=${SESSION_TTL_MS}ms)")
        }
    }

    fun disarmSession() {
        synchronized(sessionLock) {
            currentStep = AutomationStep.IDLE
            expiresAtElapsed = 0L
            scrollAttempts = 0
            enteredSettings = false
        }
    }

    fun getActiveSessionSnapshot(): SessionSnapshot? {
        synchronized(sessionLock) {
            if (currentStep == AutomationStep.IDLE || currentStep == AutomationStep.DONE) {
                return null
            }
            val now = SystemClock.elapsedRealtime()
            if (now > expiresAtElapsed) {
                Log.d(TAG, "Network toggle session expired after TTL.")
                currentStep = AutomationStep.IDLE
                enteredSettings = false
                return null
            }
            return SessionSnapshot(
                step = currentStep,
                targetMode = currentTarget,
                expiresAtElapsed = expiresAtElapsed,
                scrollAttempts = scrollAttempts,
                lastScrollAtElapsed = lastScrollAtElapsed,
                lastClickAtElapsed = lastClickAtElapsed,
                enteredSettings = enteredSettings
            )
        }
    }

    fun markEnteredSettings() {
        synchronized(sessionLock) {
            enteredSettings = true
        }
    }

    fun advanceToDialogStep() {
        synchronized(sessionLock) {
            if (currentStep == AutomationStep.WAIT_SETTINGS_LIST) {
                currentStep = AutomationStep.WAIT_PREF_DIALOG
                lastClickAtElapsed = SystemClock.elapsedRealtime()
                Log.d(TAG, "Advanced session to WAIT_PREF_DIALOG")
            }
        }
    }

    fun recordScrollAttempt() {
        synchronized(sessionLock) {
            scrollAttempts += 1
            lastScrollAtElapsed = SystemClock.elapsedRealtime()
        }
    }

    fun recordClickTimestamp() {
        synchronized(sessionLock) {
            lastClickAtElapsed = SystemClock.elapsedRealtime()
        }
    }

    fun resolveActiveDataSubscriptionId(): Int {
        return try {
            val subId = SubscriptionManager.getDefaultDataSubscriptionId()
            if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) subId else SubscriptionManager.INVALID_SUBSCRIPTION_ID
        } catch (t: Throwable) {
            Log.w(TAG, "Unable to query default data subscription ID", t)
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        }
    }

    fun buildMobileNetworkSettingsIntent(context: Context): Intent {
        val subId = resolveActiveDataSubscriptionId()
        val intent = Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                val extraKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Settings.EXTRA_SUB_ID
                } else {
                    "android.provider.extra.SUB_ID"
                }
                putExtra(extraKey, subId)
            }
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            intent
        } else {
            Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
    }

    fun prepareToggleAndBuildLaunchIntent(
        context: Context,
        targetMode: TargetMode = TargetMode.AUTO_TOGGLE
    ): Intent {
        val ready = isAccessibilityAutomationReady(context)
        if (ready) {
            armSession(targetMode)
        } else {
            disarmSession()
            showToast(
                context,
                "Accessibility is paused — opening active SIM settings for manual tap",
                Toast.LENGTH_LONG
            )
        }
        return buildMobileNetworkSettingsIntent(context)
    }

    fun buildTileLaunchPendingIntent(context: Context): PendingIntent {
        val launchIntent = prepareToggleAndBuildLaunchIntent(context, TargetMode.AUTO_TOGGLE)
        return PendingIntent.getActivity(
            context,
            401,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun buildTrampolinePendingIntent(
        context: Context,
        requestCode: Int = 402,
        targetMode: TargetMode = TargetMode.AUTO_TOGGLE
    ): PendingIntent {
        val trampoline = Intent(context, NetworkModeToggleActivity::class.java).apply {
            action = ACTION_TOGGLE_NETWORK_MODE
            putExtra(EXTRA_TARGET_MODE, targetMode.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            trampoline,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun confirmModeAndSyncUi(context: Context, newMode: NetworkMode) {
        val appContext = context.applicationContext
        saveMode(appContext, newMode)
        triggerSubtleFeedback(appContext)
        NetworkModeTileService.requestRefresh(appContext)
        NetworkModeWidget.updateAllWidgets(appContext)
        updatePinnedShortcutState(appContext, newMode)
        showToast(appContext, "Mobile network switched to ${newMode.label}", Toast.LENGTH_SHORT)
    }

    private fun buildPinnedShortcutInfo(context: Context, mode: NetworkMode): ShortcutInfoCompat {
        val shortcutIntent = Intent(context, NetworkModeToggleActivity::class.java).apply {
            action = ACTION_TOGGLE_NETWORK_MODE
            putExtra(EXTRA_TARGET_MODE, TargetMode.AUTO_TOGGLE.name)
        }
        val iconRes = if (mode == NetworkMode.FIVE_G) R.drawable.ic_network_5g else R.drawable.ic_network_lte
        return ShortcutInfoCompat.Builder(context, SHORTCUT_ID)
            .setShortLabel(mode.label)
            .setLongLabel("Network: ${mode.label}")
            .setIcon(IconCompat.createWithResource(context, iconRes))
            .setIntent(shortcutIntent)
            .build()
    }

    private fun updatePinnedShortcutState(context: Context, mode: NetworkMode) {
        try {
            val shortcutInfo = buildPinnedShortcutInfo(context, mode)
            ShortcutManagerCompat.updateShortcuts(context, listOf(shortcutInfo))
        } catch (e: Exception) {
            Log.w(TAG, "Unable to update pinned shortcut state", e)
        }
    }

    fun pinHomeShortcut(context: Context) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Toast.makeText(
                context,
                "Pinned shortcuts are not supported by your current launcher.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val currentMode = getSavedMode(context)
        val shortcutInfo = buildPinnedShortcutInfo(context, currentMode)

        val pinned = ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
        if (pinned) {
            Toast.makeText(context, "Requested ${currentMode.label} 1x1 Home Screen shortcut!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Could not pin shortcut on this launcher.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun triggerSubtleFeedback(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (!prefs.getBoolean("lock_vibration_enabled", true)) return
            val intensity = prefs.getFloat("lock_vibration_intensity", 0.5f)
            val duration = (12 + (intensity * 35)).toLong()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.let {
                if (it.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val amp = (50 + (intensity * 205)).toInt().coerceIn(1, 255)
                        it.vibrate(VibrationEffect.createOneShot(duration, amp))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(duration)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Feedback vibration skipped", e)
        }
    }

    private fun showToast(context: Context, message: String, length: Int) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext, message, length).show()
        }
    }
}
