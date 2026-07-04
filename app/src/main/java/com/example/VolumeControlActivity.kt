package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme

class VolumeControlActivity : ComponentActivity() {

    private lateinit var audioManager: AudioManager
    private var isReceiverRegistered = false

    private var mediaVolState = mutableIntStateOf(0)
    private var isMutedState  = mutableStateOf(false)
    private var preMuteVolume = 0

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                refreshFromSystem()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setBackgroundBlurRadius(80)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        refreshFromSystem()

        setContent {
            MyApplicationTheme {
                // FIX (root cause 4): volume change callbacks defined INSIDE
                // the composable scope so they are never stale closures.
                // The old code used remember<(Int)->Unit> {} which froze the
                // lambda at first composition — if the activity state changed
                // before the user's first interaction, the callback was
                // calling the original (now-stale) onVolumeChange. Defining
                // them here means they always close over the current AudioManager
                // and mutableState references, which are stable across
                // recompositions.
                val cs = MaterialTheme.colorScheme
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

                VolumeBarOverlayScreen(
                    currentVolume = mediaVolState.intValue,
                    maxVolume     = maxVol,
                    isMuted       = isMutedState.value,
                    onVolumeChange = { newValue ->
                        if (isMutedState.value) isMutedState.value = false
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newValue, 0)
                        refreshFromSystem()
                    },
                    onMuteToggle = {
                        if (isMutedState.value) {
                            audioManager.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                preMuteVolume.coerceAtLeast(1),
                                0
                            )
                            isMutedState.value = false
                        } else {
                            preMuteVolume = mediaVolState.intValue
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                            isMutedState.value = true
                        }
                        refreshFromSystem()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!isReceiverRegistered) {
            val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(volumeReceiver, filter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(volumeReceiver, filter)
            }
            isReceiverRegistered = true
        }
    }

    override fun onStop() {
        super.onStop()
        if (isReceiverRegistered) {
            try { unregisterReceiver(volumeReceiver) } catch (e: Exception) {}
            isReceiverRegistered = false
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        refreshFromSystem()
    }

    private fun refreshFromSystem() {
        val vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        mediaVolState.intValue = vol
        if (vol == 0 && !isMutedState.value) isMutedState.value = true
        else if (vol > 0 && isMutedState.value) isMutedState.value = false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (isMutedState.value) isMutedState.value = false
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND
                )
                refreshFromSystem()
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND
                )
                refreshFromSystem()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            try { unregisterReceiver(volumeReceiver) } catch (e: Exception) {}
            isReceiverRegistered = false
        }
    }
}

@Composable
fun VolumeBarOverlayScreen(
    currentVolume: Int,
    maxVolume: Int,
    isMuted: Boolean,
    onVolumeChange: (Int) -> Unit,
    onMuteToggle: () -> Unit,
    onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    // Auto-dismiss: single polling loop keyed on Unit, not re-triggered
    // on every recomposition. Interaction timestamp updated explicitly.
    var lastInteractionAt by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            val remaining = 3500L - (System.currentTimeMillis() - lastInteractionAt)
            if (remaining <= 0L) { onDismiss(); break }
            kotlinx.coroutines.delay(remaining.coerceAtLeast(100L))
        }
    }

    // Scrim — tap anywhere outside the card to dismiss
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // ── iOS 26 liquid-glass card ──────────────────────────────────────
        // window.setBackgroundBlurRadius(80) in onCreate already blurs
        // what's behind the entire window. This card sits on top of that
        // blurred layer. The layered-gradient approach here adds the
        // translucent frosted-glass fill, top-edge specular rim, and a
        // subtle border — matching the iOS 26 reference without needing
        // a second live-blur pass.
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(32.dp))
                // Layer 1: frosted translucent base
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            cs.surface.copy(alpha = 0.45f),
                            cs.surfaceVariant.copy(alpha = 0.30f)
                        )
                    )
                )
                // Layer 2: specular highlight along the top edge, same as
                // the iOS glass capsule treatment
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = 0.18f),
                        0.15f to Color.White.copy(alpha = 0.04f),
                        1.0f to Color.Transparent
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* consume — don't dismiss */ }
        ) {
            // Hairline border rendered as a nested Box so it sits above both
            // gradient layers without affecting layout dimensions.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                VolumeProgressBar(
                    currentVolume = currentVolume,
                    maxVolume     = maxVolume,
                    isMuted       = isMuted,
                    onVolumeChange = { v ->
                        lastInteractionAt = System.currentTimeMillis()
                        onVolumeChange(v)
                    },
                    onMuteToggle = {
                        lastInteractionAt = System.currentTimeMillis()
                        onMuteToggle()
                    }
                )
            }
        }
    }
}
