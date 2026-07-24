package com.example

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DynamicCapsuleNotifListener : NotificationListenerService() {

    companion object {
        var instance: DynamicCapsuleNotifListener? = null
            private set

        private val _progressDataFlow = MutableStateFlow<ProgressData?>(null)
        val progressDataFlow: StateFlow<ProgressData?> = _progressDataFlow.asStateFlow()

        private val _timerDataFlow = MutableStateFlow<TimerData?>(null)
        val timerDataFlow: StateFlow<TimerData?> = _timerDataFlow.asStateFlow()

        private val _notifDataFlow = MutableStateFlow<NotificationCapsuleData?>(null)
        val notifDataFlow: StateFlow<NotificationCapsuleData?> = _notifDataFlow.asStateFlow()

        fun clearNotifData() {
            _notifDataFlow.value = null
        }

        fun clearProgressData() {
            _progressDataFlow.value = null
        }

        fun isConnected(context: Context): Boolean {
            val enabledListeners = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            return enabledListeners.contains(context.packageName)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        checkActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        processNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        val pkg = sbn.packageName
        if (_progressDataFlow.value?.packageName == pkg) {
            _progressDataFlow.value = null
        }
        if (_notifDataFlow.value?.key == sbn.key) {
            _notifDataFlow.value = null
        }
    }

    private fun checkActiveNotifications() {
        try {
            val activeNotifs = activeNotifications ?: return
            for (sbn in activeNotifs) {
                processNotification(sbn)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun processNotification(sbn: StatusBarNotification) {
        val notif = sbn.notification ?: return
        val extras = notif.extras ?: return
        val pkg = sbn.packageName

        // 1. Check for Progress bar (e.g. downloads)
        val maxProg = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val curProg = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        val isIndeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)

        if (maxProg > 0 || isIndeterminate) {
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                ?: getAppName(pkg)
            val icon = getAppBitmapIcon(pkg)

            _progressDataFlow.value = ProgressData(
                packageName = pkg,
                appName = title,
                appIcon = icon,
                progress = curProg,
                maxProgress = maxProg,
                isIndeterminate = isIndeterminate
            )
            return
        }

        // 2. Check for Timer / Clock notifications
        val isClockPkg = pkg.contains("deskclock", ignoreCase = true) ||
                pkg.contains("clockpackage", ignoreCase = true) ||
                pkg.contains("alarm", ignoreCase = true)

        if (isClockPkg) {
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "Timer"
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            // Look for time format inside title or text (e.g. "01:30" or "00:45")
            val timeMatch = Regex("\\d{1,2}:\\d{2}(::\\d{2})?").find("$title $text")?.value

            if (timeMatch != null) {
                _timerDataFlow.value = TimerData(
                    title = title,
                    formattedTime = timeMatch,
                    isRunning = true,
                    isStopwatch = title.contains("stopwatch", ignoreCase = true),
                    pendingPauseIntent = notif.contentIntent
                )
                return
            }
        }

        // 3. Regular Notification Pill
        if (!sbn.isOngoing && !sbn.isClearable.not()) {
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: getAppName(pkg)
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            if (text.isNotEmpty() || title.isNotEmpty()) {
                val icon = getAppBitmapIcon(pkg)

                val actionsList = mutableListOf<NotifAction>()
                notif.actions?.take(3)?.forEach { act ->
                    actionsList.add(
                        NotifAction(
                            title = act.title?.toString() ?: "Action",
                            intent = act.actionIntent
                        )
                    )
                }

                _notifDataFlow.value = NotificationCapsuleData(
                    key = sbn.key,
                    packageName = pkg,
                    title = title,
                    text = text,
                    appIcon = icon,
                    timestamp = "Now",
                    contentIntent = notif.contentIntent,
                    actions = actionsList
                )
            }
        }
    }

    private fun getAppName(pkg: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            pkg.substringAfterLast('.')
        }
    }

    private fun getAppBitmapIcon(pkg: String): Bitmap? {
        return try {
            val drawable = packageManager.getApplicationIcon(pkg)
            if (drawable is BitmapDrawable) drawable.bitmap
            else {
                val bmp = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bmp
            }
        } catch (e: Exception) {
            null
        }
    }
}
