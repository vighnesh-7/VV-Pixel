package com.example.applock

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.lifecycleScope
import com.example.R
import com.example.VVPixelAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest

class AppLockActivity : ComponentActivity() {

    private val prefs by lazy { getSharedPreferences("com.example.vvpixel.SETTINGS", Context.MODE_PRIVATE) }
    private var targetPackage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        targetPackage = intent.getStringExtra("TARGET_PACKAGE") ?: ""

        onBackPressedDispatcher.addCallback(this) {
            finishAffinity()
        }

        val usageTimeToday = getAppUsageToday(this, targetPackage)

        setContent {
            com.example.ui.theme.MyApplicationTheme {
                AppLockScreen(
                    reminderText = prefs.getString("app_lock_reminder_text", "Stay focused. You got this.") ?: "",
                    usageTimeToday = usageTimeToday,
                    onPatternConfirmed = { pattern -> validatePattern(pattern) },
                    onCancel = { finishAffinity() }
                )
            }
        }
    }

    private fun getAppUsageToday(context: Context, packageName: String): String {
        if (packageName.isBlank()) return "0m used today"
        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            val stats = usageStatsManager?.queryUsageStats(
                android.app.usage.UsageStatsManager.INTERVAL_DAILY, startTime, endTime
            )
            val appStat = stats?.find { it.packageName == packageName }
            val totalTimeMs = appStat?.totalTimeInForeground ?: 0L

            val minutes = (totalTimeMs / (1000 * 60)) % 60
            val hours = totalTimeMs / (1000 * 60 * 60)

            if (hours > 0) {
                "${hours}h ${minutes}m used today"
            } else {
                "${minutes}m used today"
            }
        } catch (e: Exception) {
            "0m used today"
        }
    }

    private fun validatePattern(pattern: List<Int>): Boolean {
        val patternString = pattern.joinToString(",")
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(patternString.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val storedHash = prefs.getString("app_lock_pattern_hash", null)

        return if (storedHash != null && storedHash == hash) {
            lifecycleScope.launch {
                delay(200)
                VVPixelAccessibilityService.notifyAppUnlocked(targetPackage, applicationContext)
                finish()
                overridePendingTransition(R.anim.hold, R.anim.fade_scale_out)
            }
            true
        } else {
            false
        }
    }
}
