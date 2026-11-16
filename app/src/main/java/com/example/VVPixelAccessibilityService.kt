package com.example

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager

class VVPixelAccessibilityService : AccessibilityService() {

    private var isRegistered = false
    private var lastHomeClickTime = 0L

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
            if (!AccessibilityMasterState.isEnabled(this@VVPixelAccessibilityService)) return
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
            if (!AccessibilityMasterState.isEnabled(this@VVPixelAccessibilityService)) return
            Log.d(TAG, "Ringer mode changed broadcast received dynamically in AccessibilityService.")
            context?.let {
                RingerToggleWidget.updateAllWidgetsAndTile(it)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "VV Pixel Accessibility Service Connected.")
        instance = this
        isServiceRunning = true

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

        AccessibilityMasterTileService.requestRefresh(this)
        ShakeTorchService.startServiceIfEnabled(this)
    }

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!AccessibilityMasterState.isEnabled(this)) return
        val evt = event ?: return

        // 0. Armed 5G / LTE Network Preference Automation
        val networkSession = NetworkToggleManager.getActiveSessionSnapshot()
        if (networkSession != null &&
            (evt.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                evt.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
        ) {
            val eventPkg = evt.packageName?.toString() ?: ""
            if (isSettingsPackage(eventPkg)) {
                NetworkToggleManager.markEnteredSettings()
                handleNetworkToggleAutomation(networkSession)
                return
            } else if (networkSession.enteredSettings &&
                eventPkg.isNotBlank() &&
                !eventPkg.contains("systemui") &&
                eventPkg != this.packageName
            ) {
                NetworkToggleManager.disarmSession()
            }
        }

        // 1. Auto-disable trigger for selected sensitive/banking apps
        if (evt.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = evt.packageName?.toString() ?: return
            if (packageName.isNotBlank() &&
                packageName != this.packageName &&
                !packageName.contains("systemui") &&
                !packageName.contains("launcher") &&
                !packageName.contains("nexuslauncher")
            ) {
                if (AccessibilityMasterState.isAutoDisableOnAppsEnabled(this)) {
                    val targetPackages = AccessibilityMasterState.getAutoDisablePackages(this)
                    if (targetPackages.contains(packageName)) {
                        Log.d(TAG, "Selected target app opened: $packageName -> Triggering auto disableSelf()")
                        disableCurrentService(this)
                        return
                    }
                }
            }
        }

        // 2. Double tap home/launcher area to lock screen
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
    }

    private fun isSettingsPackage(pkg: String): Boolean {
        return pkg == "com.android.settings" ||
            pkg == "com.google.android.settings" ||
            pkg == "com.android.phone"
    }

    private fun handleNetworkToggleAutomation(session: NetworkToggleManager.SessionSnapshot) {
        val root = rootInActiveWindow ?: return
        val now = android.os.SystemClock.elapsedRealtime()

        // Check if the network choice dialog is already visible on screen
        val dialogOptions = findNetworkDialogOptions(root)
        if (dialogOptions.fiveGNode != null && dialogOptions.lteNode != null) {
            if (now - session.lastClickAtElapsed < 250L &&
                session.step == NetworkToggleManager.AutomationStep.WAIT_SETTINGS_LIST
            ) {
                // Allow click transition to settle briefly
            }
            NetworkToggleManager.advanceToDialogStep()
            performDialogSelection(dialogOptions, session.targetMode)
            return
        }

        if (session.step == NetworkToggleManager.AutomationStep.WAIT_SETTINGS_LIST) {
            if (now - session.lastClickAtElapsed < 400L) return

            val prefRowNode = findPreferredNetworkRow(root)
            if (prefRowNode != null) {
                val clickable = findClickableAncestor(prefRowNode) ?: prefRowNode
                val clicked = clickable.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "Clicked 'Preferred network type' row -> $clicked")
                if (clicked) {
                    NetworkToggleManager.advanceToDialogStep()
                }
                return
            }

            // Not visible yet: scroll forward up to 6 times
            if (session.scrollAttempts < 6 && (now - session.lastScrollAtElapsed) >= 350L) {
                val scrollable = findScrollableNode(root)
                if (scrollable != null) {
                    val scrolled = scrollable.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                    Log.d(TAG, "Scrolling Mobile Network settings to find Preferred network type (attempt ${session.scrollAttempts + 1}) -> $scrolled")
                    NetworkToggleManager.recordScrollAttempt()
                }
            }
        }
    }

    private data class NetworkOptionNode(
        val clickableNode: android.view.accessibility.AccessibilityNodeInfo,
        val isChecked: Boolean
    )

    private data class NetworkDialogOptions(
        val fiveGNode: NetworkOptionNode?,
        val lteNode: NetworkOptionNode?
    )

    private fun performDialogSelection(
        options: NetworkDialogOptions,
        targetMode: NetworkToggleManager.TargetMode
    ) {
        val fiveG = options.fiveGNode ?: return
        val lte = options.lteNode ?: return

        val chosenMode: NetworkToggleManager.NetworkMode
        val nodeToClick: android.view.accessibility.AccessibilityNodeInfo

        when (targetMode) {
            NetworkToggleManager.TargetMode.FORCE_5G -> {
                chosenMode = NetworkToggleManager.NetworkMode.FIVE_G
                nodeToClick = fiveG.clickableNode
            }
            NetworkToggleManager.TargetMode.FORCE_LTE -> {
                chosenMode = NetworkToggleManager.NetworkMode.LTE
                nodeToClick = lte.clickableNode
            }
            NetworkToggleManager.TargetMode.AUTO_TOGGLE -> {
                if (fiveG.isChecked) {
                    chosenMode = NetworkToggleManager.NetworkMode.LTE
                    nodeToClick = lte.clickableNode
                } else if (lte.isChecked) {
                    chosenMode = NetworkToggleManager.NetworkMode.FIVE_G
                    nodeToClick = fiveG.clickableNode
                } else {
                    val saved = NetworkToggleManager.getSavedMode(this)
                    if (saved == NetworkToggleManager.NetworkMode.FIVE_G) {
                        chosenMode = NetworkToggleManager.NetworkMode.LTE
                        nodeToClick = lte.clickableNode
                    } else {
                        chosenMode = NetworkToggleManager.NetworkMode.FIVE_G
                        nodeToClick = fiveG.clickableNode
                    }
                }
            }
        }

        NetworkToggleManager.disarmSession()
        val clicked = nodeToClick.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
        Log.d(TAG, "Selected network option ${chosenMode.label} -> $clicked")

        if (clicked) {
            NetworkToggleManager.confirmModeAndSyncUi(this, chosenMode)
            scheduleSafeReturnBack()
        }
    }

    private fun scheduleSafeReturnBack() {
        mainHandler.postDelayed({
            try {
                val activeRoot = rootInActiveWindow
                val pkg = activeRoot?.packageName?.toString() ?: ""
                if (!isSettingsPackage(pkg)) return@postDelayed

                val stillInDialog = activeRoot != null &&
                    findNetworkDialogOptions(activeRoot).let { it.fiveGNode != null && it.lteNode != null }

                performGlobalAction(GLOBAL_ACTION_BACK)
                if (stillInDialog) {
                    mainHandler.postDelayed({
                        val afterPkg = rootInActiveWindow?.packageName?.toString() ?: ""
                        if (isSettingsPackage(afterPkg)) {
                            performGlobalAction(GLOBAL_ACTION_BACK)
                        }
                    }, 280L)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error navigating back after network switch", e)
            }
        }, 420L)
    }

    private fun findPreferredNetworkRow(root: android.view.accessibility.AccessibilityNodeInfo): android.view.accessibility.AccessibilityNodeInfo? {
        val allNodes = collectNodes(root)
        for (node in allNodes) {
            val text = node.text?.toString()?.trim() ?: continue
            if (text.equals("Preferred network type", ignoreCase = true) ||
                text.contains("Preferred network type", ignoreCase = true) ||
                text.equals("Preferred network", ignoreCase = true)
            ) {
                return node
            }
        }
        return null
    }

    private fun findNetworkDialogOptions(root: android.view.accessibility.AccessibilityNodeInfo): NetworkDialogOptions {
        val allNodes = collectNodes(root)
        var fiveG: NetworkOptionNode? = null
        var lte: NetworkOptionNode? = null

        for (node in allNodes) {
            val rawText = node.text?.toString()?.trim() ?: continue
            if (rawText.isEmpty() || rawText.length > 48) continue
            val lower = rawText.lowercase()
            if (lower.contains("preferred")) continue

            val is5g = lower == "5g" ||
                lower.startsWith("5g ") ||
                lower.startsWith("5g(") ||
                lower.contains("5g (recommended)") ||
                (lower.contains("5g") && !lower.contains("turn off"))

            val isLte = !is5g && (
                lower == "lte" ||
                    lower.startsWith("lte ") ||
                    lower.startsWith("lte(") ||
                    lower.startsWith("lte/") ||
                    lower == "4g" ||
                    lower.startsWith("4g ") ||
                    lower.contains("4g (recommended)") ||
                    lower.contains("lte (recommended)")
                )

            if (is5g && fiveG == null) {
                val clickable = findClickableAncestor(node) ?: node
                val checked = isRowChecked(node, clickable)
                fiveG = NetworkOptionNode(clickable, checked)
            } else if (isLte && lte == null) {
                val clickable = findClickableAncestor(node) ?: node
                val checked = isRowChecked(node, clickable)
                lte = NetworkOptionNode(clickable, checked)
            }
        }
        return NetworkDialogOptions(fiveG, lte)
    }

    private fun isRowChecked(
        labelNode: android.view.accessibility.AccessibilityNodeInfo,
        rowContainer: android.view.accessibility.AccessibilityNodeInfo
    ): Boolean {
        if (labelNode.isChecked || rowContainer.isChecked) return true
        val rowChildren = collectNodes(rowContainer)
        return rowChildren.any { it.isChecked }
    }

    private fun findClickableAncestor(node: android.view.accessibility.AccessibilityNodeInfo): android.view.accessibility.AccessibilityNodeInfo? {
        var current: android.view.accessibility.AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 5) {
            if (current.isClickable) return current
            current = current.parent
            depth++
        }
        return null
    }

    private fun findScrollableNode(root: android.view.accessibility.AccessibilityNodeInfo): android.view.accessibility.AccessibilityNodeInfo? {
        val allNodes = collectNodes(root)
        return allNodes.firstOrNull { it.isScrollable }
    }

    private fun collectNodes(root: android.view.accessibility.AccessibilityNodeInfo): List<android.view.accessibility.AccessibilityNodeInfo> {
        val result = ArrayList<android.view.accessibility.AccessibilityNodeInfo>(64)
        val queue = ArrayDeque<android.view.accessibility.AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty() && result.size < 400) {
            val node = queue.removeFirst()
            result.add(node)
            val count = node.childCount
            for (i in 0 until count) {
                val child = node.getChild(i)
                if (child != null) queue.add(child)
            }
        }
        return result
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "VV Pixel Accessibility Service Destroyed.")
        mainHandler.removeCallbacksAndMessages(null)
        NetworkToggleManager.disarmSession()
        instance = null
        if (isRegistered) {
            try { unregisterReceiver(receiver) } catch (e: Exception) {}
            try { unregisterReceiver(ringerReceiver) } catch (e: Exception) {}
            isRegistered = false
        }
        isServiceRunning = false
        AccessibilityMasterTileService.requestRefresh(this)
    }

    companion object {
        private const val TAG = "VVPixelAccessService"
        const val ACTION_LOCK = "com.example.vvpixel.ACTION_LOCK"
        var isServiceRunning = false
        var instance: VVPixelAccessibilityService? = null

        fun disableCurrentService(context: Context): Boolean {
            val currentInstance = instance
            if (currentInstance != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    currentInstance.disableSelf()
                    isServiceRunning = false
                    instance = null
                    AccessibilityMasterTileService.requestRefresh(context)
                    return true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to call disableSelf()", e)
                }
            }
            return false
        }

        fun isServiceEnabledInSystem(context: Context): Boolean {
            if (isServiceRunning && instance != null) return true

            try {
                val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                val runningServices = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                if (runningServices != null) {
                    for (info in runningServices) {
                        val serviceInfo = info.resolveInfo?.serviceInfo
                        if (serviceInfo != null && serviceInfo.packageName == context.packageName &&
                            (serviceInfo.name == VVPixelAccessibilityService::class.java.name || serviceInfo.name.endsWith("VVPixelAccessibilityService"))) {
                            return true
                        }
                    }
                }

                val expectedComponentName = ComponentName(context, VVPixelAccessibilityService::class.java).flattenToString()
                val expectedShortName = "${context.packageName}/.${VVPixelAccessibilityService::class.java.simpleName}"
                val expectedFullName = "${context.packageName}/${VVPixelAccessibilityService::class.java.name}"

                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: ""

                val colonSplitter = TextUtils.SimpleStringSplitter(':')
                colonSplitter.setString(enabledServices)
                while (colonSplitter.hasNext()) {
                    val componentNameString = colonSplitter.next()
                    if (componentNameString.equals(expectedComponentName, ignoreCase = true) ||
                        componentNameString.equals(expectedShortName, ignoreCase = true) ||
                        componentNameString.equals(expectedFullName, ignoreCase = true)) {
                        return true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking accessibility status", e)
            }

            return false
        }
    }
}
