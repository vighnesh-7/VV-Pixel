package com.example

import android.content.Context
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun VolumeCapsule(
    currentVolume: Int,
    maxVolume: Int,
    isMuted: Boolean,
    isExpanded: Boolean,
    onVolumeChanged: (Int) -> Unit,
    onMuteToggle: () -> Unit,
    onExpandToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val cs = MaterialTheme.colorScheme

    if (!isExpanded) {
        // ================= COLLAPSED VOLUME INDICATOR (Raw Icons only) =================
        // Per spec & screenshot: Standalone icons floating in the status bar area, pure transparent background
        Row(
            modifier = modifier
                .wrapContentSize()
                .clickable { onExpandToggle() }
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Speaker Icon
            Icon(
                imageVector = if (isMuted || currentVolume == 0) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                contentDescription = "Volume",
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(20.dp)
            )

            // Media / Stack Icon
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = "Media Session",
                tint = cs.secondary,
                modifier = Modifier.size(22.dp)
            )
        }
    } else {
        // ================= EXPANDED VOLUME BAR CARD =================
        val volumeFraction = if (maxVolume > 0) (currentVolume.toFloat() / maxVolume.toFloat()).coerceIn(0f, 1f) else 0f
        val animatedFraction by animateFloatAsState(targetValue = volumeFraction, label = "vol_bar")

        Box(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(cs.surfaceVariant.copy(alpha = 0.85f))
                .pointerInput(maxVolume) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val newFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val newVol = (newFraction * maxVolume).roundToInt()
                        if (newVol != currentVolume) {
                            onVolumeChanged(newVol)
                            try {
                                view.performHapticFeedback(
                                    HapticFeedbackConstants.CLOCK_TICK,
                                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                                )
                            } catch (e: Exception) { }
                        }
                    }
                }
                .pointerInput(maxVolume) {
                    detectTapGestures { offset ->
                        val newFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val newVol = (newFraction * maxVolume).roundToInt()
                        onVolumeChanged(newVol)
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Glass surface & specular rim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Active volume level fill bar
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        if (isMuted) cs.outline.copy(alpha = 0.5f)
                        else cs.primary
                    )
            )

            // Content row overlay
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when {
                            isMuted || currentVolume == 0 -> Icons.Default.VolumeOff
                            currentVolume < maxVolume / 2 -> Icons.Default.VolumeDown
                            else -> Icons.Default.VolumeUp
                        },
                        contentDescription = null,
                        tint = if (animatedFraction > 0.15f) cs.onPrimary else cs.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(animatedFraction * 100).roundToInt()}%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = if (animatedFraction > 0.35f) cs.onPrimary else cs.onSurface,
                            fontSize = 15.sp
                        )
                    )
                }

                // Mute button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable {
                            try {
                                view.performHapticFeedback(
                                    HapticFeedbackConstants.CONFIRM,
                                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                                )
                            } catch (e: Exception) { }
                            onMuteToggle()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Mute Toggle",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
