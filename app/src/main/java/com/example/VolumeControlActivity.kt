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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

class VolumeControlActivity : ComponentActivity() {

    private lateinit var audioManager: AudioManager
    private var isReceiverRegistered = false

    // State flows to update UI when physical buttons are pressed
    private var mediaVolState = mutableIntStateOf(0)
    private var ringVolState = mutableIntStateOf(0)
    private var notificationVolState = mutableIntStateOf(0)
    private var alarmVolState = mutableIntStateOf(0)

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                updateVolumeStates()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

        updateVolumeStates()

        // Register broadcast receiver for physical volume button updates
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(volumeReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(volumeReceiver, filter)
        }
        isReceiverRegistered = true

        setContent {
            MyApplicationTheme {
                VolumeOverlayScreen(
                    currentMedia = mediaVolState.intValue,
                    maxMedia = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                    currentRing = ringVolState.intValue,
                    maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING),
                    currentNotification = notificationVolState.intValue,
                    maxNotification = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION),
                    currentAlarm = alarmVolState.intValue,
                    maxAlarm = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                    onVolumeChanged = { streamType, newValue ->
                        audioManager.setStreamVolume(streamType, newValue, 0)
                        updateVolumeStates()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }

    private fun updateVolumeStates() {
        mediaVolState.intValue = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        ringVolState.intValue = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        notificationVolState.intValue = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        alarmVolState.intValue = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Intercept volume buttons to show changes dynamically and play standard system sounds
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_PLAY_SOUND
                )
                updateVolumeStates()
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_PLAY_SOUND
                )
                updateVolumeStates()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(volumeReceiver)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}

@Composable
fun VolumeOverlayScreen(
    currentMedia: Int,
    maxMedia: Int,
    currentRing: Int,
    maxRing: Int,
    currentNotification: Int,
    maxNotification: Int,
    currentAlarm: Int,
    maxAlarm: Int,
    onVolumeChanged: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    // Automatically dismiss the overlay after 3.5 seconds of inactivity (such as physical button presses or slider changes)
    val stateTracker = remember(currentMedia, currentRing, currentNotification, currentAlarm) {
        System.currentTimeMillis() // Triggers LaunchedEffect cancellation/re-trigger when states change
    }
    
    LaunchedEffect(stateTracker) {
        kotlinx.coroutines.delay(3500)
        onDismiss()
    }

    // Fill the screen with semi-transparent backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0x22FFFFFF), // highly reflective light gloss
                        Color(0x06888888), // neutral liquid shimmer
                        Color(0x1F121214)  // subtle glass base
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // Outer interactive dialog box, styling aligned with Google Pixel volume slider aesthetics
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Consume clicks to avoid dismiss */ },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "VV Volume Panel",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Slider Row - Media
                VolumeSliderItem(
                    title = "Media",
                    icon = Icons.Default.PlayArrow,
                    current = currentMedia,
                    max = maxMedia,
                    onValueChange = { onVolumeChanged(AudioManager.STREAM_MUSIC, it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Slider Row - Ring
                VolumeSliderItem(
                    title = "Ringtone",
                    icon = Icons.Default.Notifications,
                    current = currentRing,
                    max = maxRing,
                    onValueChange = { onVolumeChanged(AudioManager.STREAM_RING, it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Slider Row - Notifications
                if (maxNotification > 0) {
                    VolumeSliderItem(
                        title = "Notifications",
                        icon = Icons.Default.Notifications,
                        current = currentNotification,
                        max = maxNotification,
                        onValueChange = { onVolumeChanged(AudioManager.STREAM_NOTIFICATION, it) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Slider Row - Alarm
                VolumeSliderItem(
                    title = "Alarm",
                    icon = Icons.Default.Star,
                    current = currentAlarm,
                    max = maxAlarm,
                    onValueChange = { onVolumeChanged(AudioManager.STREAM_ALARM, it) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onDismiss() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Dismiss", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun VolumeSliderItem(
    title: String,
    icon: ImageVector,
    current: Int,
    max: Int,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = "$title Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$current / $max",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
        
        Slider(
            value = current.toFloat(),
            valueRange = 0f..max.toFloat(),
            steps = if (max > 1) max - 1 else 0,
            onValueChange = { onValueChange(it.toInt()) },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
