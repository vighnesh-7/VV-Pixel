package com.example

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

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
    modifier: Modifier = Modifier
) {
    if (mode == CapsuleMode.NONE) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
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
