package com.example

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class DynamicCapsuleService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        const val CHANNEL_ID = "vvpixel_capsule_channel"
        const val NOTIF_ID = 9001
        var isServiceRunning = false
            private set
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = store

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Media & Volume state
    private var mediaSessionManager: MediaSessionManager? = null
    private var activeMediaController: MediaController? = null
    private var audioManager: AudioManager? = null

    private var mediaDataState by mutableStateOf(MediaPlaybackData())
    private var volCurrentState by mutableIntStateOf(0)
    private var volMaxState by mutableIntStateOf(15)
    private var volMutedState by mutableStateOf(false)
    private var preMuteVolume = 5

    private var isExpandedState by mutableStateOf(false)
    private var currentModeState by mutableStateOf(CapsuleMode.NONE)

    // Delay auto-collapse handler
    private val collapseHandler = Handler(Looper.getMainLooper())
    private val collapseRunnable = Runnable {
        isExpandedState = false
        updateWindowFlags()
    }

    private var volumeReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        isServiceRunning = true

        CapsulePreferences.init(this)
        createNotificationChannel()
        startForeground(NOTIF_ID, createNotification())

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        setupVolumeListener()
        setupMediaListener()
        setupOverlayView()

        // Observe Preference changes
        serviceScope.launch {
            CapsulePreferences.stateFlow.collectLatest { prefs ->
                checkSelfTermination(prefs)
                reevaluateCapsuleMode(prefs)
            }
        }

        // Observe Notification Listener state
        serviceScope.launch {
            combineFlows()
        }

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    private suspend fun combineFlows() {
        coroutineScope {
            launch {
                DynamicCapsuleNotifListener.progressDataFlow.collectLatest {
                    reevaluateCapsuleMode(CapsulePreferences.stateFlow.value)
                }
            }
            launch {
                DynamicCapsuleNotifListener.timerDataFlow.collectLatest {
                    reevaluateCapsuleMode(CapsulePreferences.stateFlow.value)
                }
            }
            launch {
                DynamicCapsuleNotifListener.notifDataFlow.collectLatest {
                    reevaluateCapsuleMode(CapsulePreferences.stateFlow.value)
                }
            }
        }
    }

    private fun checkSelfTermination(prefs: CapsulePreferences.State) {
        val allDisabled = !prefs.playerEnabled &&
                !prefs.volumeEnabled &&
                !prefs.progressEnabled &&
                !prefs.timerEnabled &&
                !prefs.notificationEnabled

        if (allDisabled) {
            stopSelf()
        }
    }

    private fun setupVolumeListener() {
        audioManager?.let { am ->
            volCurrentState = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            volMaxState = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            volMutedState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) am.isStreamMute(AudioManager.STREAM_MUSIC) else volCurrentState == 0
        }

        volumeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    audioManager?.let { am ->
                        val newVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                        val isMute = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) am.isStreamMute(AudioManager.STREAM_MUSIC) else newVol == 0

                        val changed = newVol != volCurrentState || isMute != volMutedState
                        volCurrentState = newVol
                        volMutedState = isMute

                        if (changed && CapsulePreferences.stateFlow.value.volumeEnabled) {
                            triggerVolumeCapsule()
                        }
                    }
                }
            }
        }

        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        registerReceiver(volumeReceiver, filter)
    }

    private fun triggerVolumeCapsule() {
        currentModeState = CapsuleMode.VOLUME
        isExpandedState = true
        updateWindowFlags()
        scheduleAutoCollapse(CapsulePreferences.stateFlow.value.collapseDelay)
    }

    private fun setupMediaListener() {
        try {
            mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val componentName = android.content.ComponentName(this, DynamicCapsuleNotifListener::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaSessionManager?.addOnActiveSessionsChangedListener(
                    { controllers ->
                        controllers?.firstOrNull()?.let { attachMediaController(it) }
                    },
                    componentName
                )

                val controllers = mediaSessionManager?.getActiveSessions(componentName)
                controllers?.firstOrNull()?.let { attachMediaController(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun attachMediaController(controller: MediaController) {
        activeMediaController = controller
        updateMediaMetadata(controller.metadata)
        updateMediaPlaybackState(controller.playbackState)

        controller.registerCallback(object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                updateMediaMetadata(metadata)
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                updateMediaPlaybackState(state)
            }
        })
    }

    private fun updateMediaMetadata(metadata: MediaMetadata?) {
        if (metadata == null) return
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Title"
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"
        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        val bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)

        val appName = activeMediaController?.packageName?.let { pkg ->
            try {
                val info = packageManager.getApplicationInfo(pkg, 0)
                packageManager.getApplicationLabel(info).toString()
            } catch (e: Exception) { null }
        }

        mediaDataState = mediaDataState.copy(
            title = title,
            artist = artist,
            album = album,
            albumArt = bitmap,
            durationMs = duration,
            appName = appName
        )
        reevaluateCapsuleMode(CapsulePreferences.stateFlow.value)
    }

    private fun updateMediaPlaybackState(state: PlaybackState?) {
        val playing = state?.state == PlaybackState.STATE_PLAYING
        val pos = state?.position ?: 0L

        mediaDataState = mediaDataState.copy(
            isPlaying = playing,
            positionMs = pos
        )
        reevaluateCapsuleMode(CapsulePreferences.stateFlow.value)
    }

    private fun reevaluateCapsuleMode(prefs: CapsulePreferences.State) {
        val notifData = DynamicCapsuleNotifListener.notifDataFlow.value
        val progressData = DynamicCapsuleNotifListener.progressDataFlow.value
        val timerData = DynamicCapsuleNotifListener.timerDataFlow.value

        currentModeState = when {
            prefs.notificationEnabled && notifData != null -> CapsuleMode.NOTIFICATION
            prefs.progressEnabled && progressData != null -> CapsuleMode.PROGRESS
            prefs.timerEnabled && timerData != null -> CapsuleMode.TIMER
            prefs.volumeEnabled && currentModeState == CapsuleMode.VOLUME -> CapsuleMode.VOLUME
            prefs.playerEnabled && (mediaDataState.isPlaying || mediaDataState.title != "No Media Playing") -> CapsuleMode.MEDIA
            else -> CapsuleMode.NONE
        }

        updateWindowFlags()
    }

    private fun scheduleAutoCollapse(seconds: Int) {
        collapseHandler.removeCallbacks(collapseRunnable)
        if (seconds > 0) {
            collapseHandler.postDelayed(collapseRunnable, seconds * 1000L)
        }
    }

    private fun setupOverlayView() {
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 12
        }

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DynamicCapsuleService)
            setViewTreeViewModelStoreOwner(this@DynamicCapsuleService)
            setViewTreeSavedStateRegistryOwner(this@DynamicCapsuleService)

            setContent {
                MyApplicationTheme {
                    val prefs by CapsulePreferences.stateFlow.collectAsState()
                    val progressData by DynamicCapsuleNotifListener.progressDataFlow.collectAsState()
                    val timerData by DynamicCapsuleNotifListener.timerDataFlow.collectAsState()
                    val notifData by DynamicCapsuleNotifListener.notifDataFlow.collectAsState()

                    CapsuleOverlay(
                        mode = currentModeState,
                        isExpanded = isExpandedState,
                        prefs = prefs,
                        mediaData = mediaDataState,
                        volCurrent = volCurrentState,
                        volMax = volMaxState,
                        volMuted = volMutedState,
                        progressData = progressData,
                        timerData = timerData,
                        notifData = notifData,
                        onExpandToggle = {
                            isExpandedState = !isExpandedState
                            updateWindowFlags()
                            if (isExpandedState) {
                                scheduleAutoCollapse(prefs.collapseDelay)
                            }
                        },
                        onCollapse = {
                            isExpandedState = false
                            updateWindowFlags()
                        },
                        onPlayPause = {
                            val state = activeMediaController?.playbackState?.state
                            if (state == PlaybackState.STATE_PLAYING) {
                                activeMediaController?.transportControls?.pause()
                            } else {
                                activeMediaController?.transportControls?.play()
                            }
                        },
                        onSkipPrevious = {
                            activeMediaController?.transportControls?.skipToPrevious()
                        },
                        onSkipNext = {
                            activeMediaController?.transportControls?.skipToNext()
                        },
                        onVolumeChanged = { newVol ->
                            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                            volCurrentState = newVol
                            volMutedState = newVol == 0
                            scheduleAutoCollapse(prefs.collapseDelay)
                        },
                        onMuteToggle = {
                            if (volMutedState) {
                                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, preMuteVolume, 0)
                                volCurrentState = preMuteVolume
                                volMutedState = false
                            } else {
                                preMuteVolume = if (volCurrentState > 0) volCurrentState else 5
                                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                                volCurrentState = 0
                                volMutedState = true
                            }
                            scheduleAutoCollapse(prefs.collapseDelay)
                        },
                        onOpenNotification = {
                            notifData?.contentIntent?.let { intent ->
                                try { intent.send() } catch (e: Exception) { }
                            }
                        },
                        onNotifActionClick = { intent ->
                            try { intent.send() } catch (e: Exception) { }
                        }
                    )
                }
            }
        }

        try {
            windowManager?.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateWindowFlags() {
        val view = overlayView ?: return
        val lp = layoutParams ?: return

        if (isExpandedState) {
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        } else {
            lp.width = WindowManager.LayoutParams.WRAP_CONTENT
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }

        try {
            windowManager?.updateViewLayout(view, lp)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Dynamic Capsule Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs the Dynamic Capsule floating overlay"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Dynamic Capsule Active")
            .setContentText("Overlay active")
            .setSmallIcon(R.drawable.ic_volume_equalizer)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceScope.cancel()

        volumeReceiver?.let {
            try { unregisterReceiver(it) } catch (e: Exception) { }
        }

        overlayView?.let {
            try { windowManager?.removeView(it) } catch (e: Exception) { }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
