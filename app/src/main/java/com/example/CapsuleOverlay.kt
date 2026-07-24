package com.example

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

enum class CapsuleMode {
    MEDIA,
    VOLUME,
    PROGRESS,
    TIMER,
    NOTIFICATION,
    NONE
}

@Composable
fun CapsuleOverlay(
    mode: CapsuleMode,
    isExpanded: Boolean,
    prefs: CapsulePreferences.State,
    mediaData: MediaPlaybackData,
    volCurrent: Int,
    volMax: Int,
    volMuted: Boolean,
    progressData: ProgressData?,
    timerData: TimerData?,
    notifData: NotificationCapsuleData?,
    onExpandToggle: () -> Unit,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onVolumeChanged: (Int) -> Unit,
    onMuteToggle: () -> Unit,
    onOpenNotification: () -> Unit,
    onNotifActionClick: (android.app.PendingIntent) -> Unit,
    onSwipeDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (mode == CapsuleMode.NONE) return

    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "capsule_swipe_offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX > 120f) {
                            onSwipeDismiss()
                        }
                        offsetX = 0f
                    },
                    onDragCancel = {
                        offsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val next = offsetX + dragAmount
                        offsetX = next.coerceAtLeast(0f)
                    }
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.92f) togetherWith
                        fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.92f)
            },
            label = "capsule_mode_transition"
        ) { currentMode ->
            when (currentMode) {
                CapsuleMode.MEDIA -> {
                    MediaPlayerCapsule(
                        data = mediaData,
                        prefs = prefs,
                        isExpanded = isExpanded,
                        onExpandToggle = onExpandToggle,
                        onPlayPause = onPlayPause,
                        onSkipPrevious = onSkipPrevious,
                        onSkipNext = onSkipNext
                    )
                }
                CapsuleMode.VOLUME -> {
                    VolumeCapsule(
                        currentVolume = volCurrent,
                        maxVolume = volMax,
                        isMuted = volMuted,
                        isExpanded = isExpanded,
                        onVolumeChanged = onVolumeChanged,
                        onMuteToggle = onMuteToggle,
                        onExpandToggle = onExpandToggle
                    )
                }
                CapsuleMode.PROGRESS -> {
                    if (progressData != null) {
                        ProgressCapsule(
                            data = progressData,
                            isExpanded = isExpanded,
                            onExpandToggle = onExpandToggle
                        )
                    }
                }
                CapsuleMode.TIMER -> {
                    if (timerData != null) {
                        TimerCapsule(
                            data = timerData,
                            isExpanded = isExpanded,
                            onExpandToggle = onExpandToggle,
                            onActionClick = {
                                timerData.pendingPauseIntent?.let {
                                    try { it.send() } catch (e: Exception) { }
                                }
                            }
                        )
                    }
                }
                CapsuleMode.NOTIFICATION -> {
                    if (notifData != null) {
                        NotificationCapsule(
                            data = notifData,
                            prefs = prefs,
                            isExpanded = isExpanded,
                            onExpandToggle = onExpandToggle,
                            onOpenNotification = onOpenNotification,
                            onActionClick = onNotifActionClick
                        )
                    }
                }
                CapsuleMode.NONE -> {}
            }
        }
    }
}
