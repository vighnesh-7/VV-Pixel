package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WaveBackground(
    waveColor: Color,
    isPlaying: Boolean,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    val shouldAnimate = isPlaying && isExpanded

    val infiniteTransition = rememberInfiniteTransition(label = "wave_animation")
    val phase1 by if (shouldAnimate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase1"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val phase2 by if (shouldAnimate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(5500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase2"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // Horizon line starts at ~45% from top (wave fills bottom 55%)
        val baseLine = h * 0.45f
        val amplitude = h * 0.12f

        // First wave path (background layer wave)
        val path1 = Path().apply {
            moveTo(0f, h)
            val step = w / 20f
            for (i in 0..20) {
                val x = i * step
                // Wave function
                val angle = (x / w) * (2 * PI).toFloat() + phase1
                val y = baseLine + amplitude * sin(angle)
                if (i == 0) lineTo(0f, y) else lineTo(x, y)
            }
            lineTo(w, h)
            close()
        }

        // Draw Wave 1 (darker/more transparent)
        drawPath(
            path = path1,
            brush = Brush.verticalGradient(
                colors = listOf(
                    waveColor.copy(alpha = 0.35f),
                    waveColor.copy(alpha = 0.60f)
                )
            )
        )

        // Second wave path (foreground layer wave) - phase shifted
        val path2 = Path().apply {
            moveTo(0f, h)
            val step = w / 20f
            for (i in 0..20) {
                val x = i * step
                val angle = (x / w) * (2 * PI).toFloat() + phase2 + (PI / 2).toFloat()
                val y = baseLine + 4.dp.toPx() + (amplitude * 0.85f) * sin(angle)
                if (i == 0) lineTo(0f, y) else lineTo(x, y)
            }
            lineTo(w, h)
            close()
        }

        // Draw Wave 2 (brighter/main wave fill)
        drawPath(
            path = path2,
            brush = Brush.verticalGradient(
                colors = listOf(
                    waveColor.copy(alpha = 0.45f),
                    waveColor.copy(alpha = 0.85f)
                )
            )
        )
    }
}
