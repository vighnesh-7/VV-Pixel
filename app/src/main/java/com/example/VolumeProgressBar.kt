package com.example

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Brightness-bar-style media volume control with:
 *  - Thinner profile (40dp, down from 64dp)
 *  - iOS 26 liquid-glass pill: translucent layered gradient + specular rim
 *  - Glass mute toggle button matching the same surface treatment
 *  - Haptic tick on every volume step change during drag
 *  - Distinct haptic on mute toggle
 *  - Single unified gesture detector — no competing pointerInput blocks
 */
@Composable
fun VolumeProgressBar(
    currentVolume: Int,
    maxVolume: Int,
    isMuted: Boolean,
    onVolumeChange: (Int) -> Unit,
    onMuteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs      = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val view    = LocalView.current   // needed for CLOCK_TICK haptic constant

    val rawFraction    = if (maxVolume > 0) currentVolume.toFloat() / maxVolume else 0f
    val targetFraction = if (isMuted) 0f else rawFraction.coerceIn(0f, 1f)

    var isDragging by remember { mutableStateOf(false) }
    val animatedFraction by animateFloatAsState(
        targetValue    = targetFraction,
        // Instant during drag (matches brightness bar behavior),
        // 180ms smooth for external changes (physical button presses).
        animationSpec  = if (isDragging) tween(0) else tween(180),
        label          = "volumeFill"
    )

    var barWidthPx       by remember { mutableIntStateOf(0) }
    var lastHapticVolume by remember { mutableIntStateOf(currentVolume) }

    // ── Shared glass surface brush ────────────────────────────────────────
    // Same layered-gradient glass treatment used on the card, applied here
    // to both the track and the mute button so they share the same surface
    // language — consistent with iOS 26's "all capsules look like the same
    // glass material" approach.
    val glassFill = Brush.verticalGradient(
        colors = listOf(
            cs.surfaceVariant.copy(alpha = 0.55f),
            cs.surface.copy(alpha = 0.35f)
        )
    )
    val specularRim = Brush.verticalGradient(
        0.0f  to Color.White.copy(alpha = 0.22f),
        0.25f to Color.White.copy(alpha = 0.05f),
        1.0f  to Color.Transparent
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            // Thinner profile — 40dp height, down from 64dp
            .height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {

        // ── Glass bar track ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                // Frosted base layer
                .background(glassFill)
                // Specular top-edge rim
                .background(specularRim)
                .onSizeChanged { barWidthPx = it.width }
                .pointerInput(maxVolume, onVolumeChange) {
                    // Single awaitEachGesture loop — handles tap-to-jump and
                    // drag-to-scrub in one pass, eliminating the gesture-
                    // arbitration delay that came from two competing pointerInput
                    // blocks. applyPosition() is called immediately on DOWN so
                    // a simple tap jumps to that position without needing a drag.
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isDragging = true

                        fun applyPosition(x: Float) {
                            if (barWidthPx <= 0 || maxVolume <= 0) return
                            val fraction  = (x / barWidthPx).coerceIn(0f, 1f)
                            val newVolume = (fraction * maxVolume).roundToInt()
                            if (newVolume != lastHapticVolume) {
                                // CLOCK_TICK is the sharpest, most subtle
                                // haptic constant — ideal for continuous
                                // scrubbing (same feel as the iOS volume HUD).
                                // Falls back to Compose's TextHandleMove on
                                // devices that don't support CLOCK_TICK.
                                try {
                                    view.performHapticFeedback(
                                        HapticFeedbackConstants.CLOCK_TICK,
                                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                                    )
                                } catch (e: Exception) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                lastHapticVolume = newVolume
                            }
                            onVolumeChange(newVolume)
                        }

                        // Instant feedback on touch-down — tap-to-jump works
                        applyPosition(down.position.x)

                        var pointer = down
                        while (true) {
                            val next = awaitDragOrCancelledUp(pointer.id) { change ->
                                applyPosition(change.position.x)
                            } ?: break
                            pointer = next
                            if (next.changedToUp()) break
                        }
                        isDragging = false
                    }
                }
        ) {
            // Filled portion — primary color at slight transparency so the
            // glass gradient behind it subtly bleeds through at the edges,
            // reinforcing the liquid look.
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction.coerceAtLeast(0.001f))
                    .clip(RoundedCornerShape(14.dp))
                    .background(cs.primary.copy(alpha = 0.88f))
            )

            // Top-edge specular on the fill too, so it also reads as glass
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction.coerceAtLeast(0.001f))
                    .height(1.dp)
                    .align(Alignment.TopCenter)
                    .background(Color.White.copy(alpha = 0.3f))
            )

            // Volume icon, visible inside the fill or the empty track
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector     = volumeIconFor(currentVolume, maxVolume, isMuted),
                    contentDescription = null,
                    tint            = if (animatedFraction > 0.16f) cs.onPrimary
                                      else cs.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier        = Modifier.size(16.dp)
                )
            }
        }

        // ── Hairline divider — matches system brightness bar ──────────────
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight(0.6f)
                .background(cs.outlineVariant.copy(alpha = 0.5f))
        )

        // ── Glass mute button ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                // Active-mute state gets secondaryContainer instead of glass
                // so it's visually distinct — tap target is obvious even when
                // squinting at the QS panel.
                .let { modifier ->
                    if (isMuted) {
                        modifier.background(cs.secondaryContainer.copy(alpha = 0.9f))
                    } else {
                        modifier
                            .background(glassFill)
                            .background(specularRim)
                    }
                }
                .pointerInput(onMuteToggle) {
                    detectTapGestures {
                        // Distinct heavier haptic for mute toggle vs
                        // the lighter CLOCK_TICK used during scrubbing.
                        try {
                            view.performHapticFeedback(
                                HapticFeedbackConstants.CONFIRM,
                                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                            )
                        } catch (e: Exception) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onMuteToggle()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector     = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                contentDescription = if (isMuted) "Unmute" else "Mute",
                tint            = if (isMuted) cs.onSecondaryContainer
                                  else cs.onSurfaceVariant.copy(alpha = 0.85f),
                modifier        = Modifier.size(18.dp)
            )
        }
    }
}

// ── Gesture primitive ─────────────────────────────────────────────────────────

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitDragOrCancelledUp(
    pointerId: androidx.compose.ui.input.pointer.PointerId,
    onMove: (PointerInputChange) -> Unit
): PointerInputChange? {
    while (true) {
        val event  = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == pointerId } ?: return null
        if (change.changedToUp()) return change
        if (change.positionDelta()) {
            onMove(change)
            change.consume()
        }
    }
}

private fun PointerInputChange.positionDelta(): Boolean {
    val d = position - previousPosition
    return abs(d.x) > 0.5f || abs(d.y) > 0.5f
}

private fun volumeIconFor(current: Int, max: Int, muted: Boolean): ImageVector {
    if (muted || current == 0) return Icons.Filled.VolumeMute
    val f = if (max > 0) current.toFloat() / max else 0f
    return if (f < 0.5f) Icons.Filled.VolumeDown else Icons.Filled.VolumeUp
}
