package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity

/**
 * Lightweight translucent trampoline activity launched by the Home Screen Widget,
 * Pinned Home Screen Shortcut, or Dashboard trigger. Arms the Accessibility
 * automation session and launches the active data SIM's Mobile Network Settings
 * screen cleanly without triggering Android 14+ Background Activity Launch blocks.
 */
class NetworkModeToggleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetModeName = intent?.getStringExtra(NetworkToggleManager.EXTRA_TARGET_MODE)
        val targetMode = runCatching {
            if (targetModeName != null) NetworkToggleManager.TargetMode.valueOf(targetModeName)
            else NetworkToggleManager.TargetMode.AUTO_TOGGLE
        }.getOrDefault(NetworkToggleManager.TargetMode.AUTO_TOGGLE)

        try {
            val settingsIntent = NetworkToggleManager.prepareToggleAndBuildLaunchIntent(this, targetMode)
            startActivity(settingsIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch mobile network settings", e)
            NetworkToggleManager.disarmSession()
        } finally {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        private const val TAG = "NetworkModeToggleAct"
    }
}
