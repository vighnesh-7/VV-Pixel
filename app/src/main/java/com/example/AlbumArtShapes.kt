package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

object AlbumArtShapes {

    fun getShapePath(shapeType: String, width: Float, height: Float): Path {
        val path = Path()
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy)

        when (shapeType.uppercase()) {
            "BLOB" -> {
                // 8-petal flower / organic squircle blob
                val numPetals = 8
                val innerR = radius * 0.78f
                val outerR = radius * 0.98f
                for (i in 0..360 step 5) {
                    val rad = Math.toRadians(i.toDouble())
                    val r = innerR + (outerR - innerR) * (0.5f + 0.5f * cos(numPetals * rad).toFloat())
                    val x = cx + (r * cos(rad)).toFloat()
                    val y = cy + (r * sin(rad)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
            "SQUARE" -> {
                val corner = radius * 0.25f
                path.addRoundRect(
                    RoundRect(
                        left = 0f, top = 0f, right = width, bottom = height,
                        cornerRadius = CornerRadius(corner, corner)
                    )
                )
            }
            "ROUNDED_RECT" -> {
                val corner = radius * 0.45f
                path.addRoundRect(
                    RoundRect(
                        left = 0f, top = 0f, right = width, bottom = height,
                        cornerRadius = CornerRadius(corner, corner)
                    )
                )
            }
            "HEXAGON" -> {
                for (i in 0 until 6) {
                    val angleRad = Math.toRadians((60 * i - 30).toDouble())
                    val x = cx + (radius * cos(angleRad)).toFloat()
                    val y = cy + (radius * sin(angleRad)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
            "SHIELD" -> {
                path.moveTo(cx, 0f)
                path.lineTo(width, height * 0.25f)
                path.quadraticTo(width, height * 0.75f, cx, height)
                path.quadraticTo(0f, height * 0.75f, 0f, height * 0.25f)
                path.close()
            }
            "OCTAGON" -> {
                for (i in 0 until 8) {
                    val angleRad = Math.toRadians((45 * i - 22.5).toDouble())
                    val x = cx + (radius * cos(angleRad)).toFloat()
                    val y = cy + (radius * sin(angleRad)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
            else -> { // Circle default
                path.addOval(Rect(0f, 0f, width, height))
            }
        }
        return path
    }

    fun getButtonShapePath(shapeType: String, width: Float, height: Float): Path {
        val path = Path()
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy)

        when (shapeType.uppercase()) {
            "STAR8" -> {
                val points = 16
                for (i in 0 until points) {
                    val r = if (i % 2 == 0) radius else radius * 0.7f
                    val angle = Math.toRadians((i * 360.0 / points) - 90)
                    val x = cx + (r * cos(angle)).toFloat()
                    val y = cy + (r * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
            "STAR12" -> {
                val points = 24
                for (i in 0 until points) {
                    val r = if (i % 2 == 0) radius else radius * 0.75f
                    val angle = Math.toRadians((i * 360.0 / points) - 90)
                    val x = cx + (r * cos(angle)).toFloat()
                    val y = cy + (r * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
            "BLOB" -> {
                return getShapePath("BLOB", width, height)
            }
            "CLOVER" -> {
                val numPetals = 4
                val innerR = radius * 0.7f
                val outerR = radius * 0.98f
                for (i in 0..360 step 5) {
                    val rad = Math.toRadians(i.toDouble())
                    val r = innerR + (outerR - innerR) * (0.5f + 0.5f * cos(numPetals * rad).toFloat())
                    val x = cx + (r * cos(rad)).toFloat()
                    val y = cy + (r * sin(rad)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
            else -> { // CIRCLE
                path.addOval(Rect(0f, 0f, width, height))
            }
        }
        return path
    }
}

class CustomShape(private val shapeType: String) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): Outline {
        val path = AlbumArtShapes.getShapePath(shapeType, size.width, size.height)
        return Outline.Generic(path)
    }
}

class ButtonCustomShape(private val shapeType: String) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): Outline {
        val path = AlbumArtShapes.getButtonShapePath(shapeType, size.width, size.height)
        return Outline.Generic(path)
    }
}

@Composable
fun VinylDiscView(
    bitmap: ImageBitmap?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val currentRotation = if (isPlaying) rotation else 0f

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = currentRotation },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val radius = minOf(cx, cy)

            // Outer dark vinyl disc
            drawCircle(color = Color(0xFF141414), radius = radius)

            // Concentric vinyl grooves
            val grooveColors = Color.White.copy(alpha = 0.08f)
            drawCircle(color = grooveColors, radius = radius * 0.88f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
            drawCircle(color = grooveColors, radius = radius * 0.76f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
            drawCircle(color = grooveColors, radius = radius * 0.64f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))

            // Center album label
            val artRadius = radius * 0.48f
            if (bitmap != null) {
                val clipPath = Path().apply {
                    addOval(Rect(cx - artRadius, cy - artRadius, cx + artRadius, cy + artRadius))
                }
                clipPath(clipPath) {
                    drawImage(
                        image = bitmap,
                        dstOffset = androidx.compose.ui.unit.IntOffset((cx - artRadius).toInt(), (cy - artRadius).toInt()),
                        dstSize = androidx.compose.ui.unit.IntSize((artRadius * 2).toInt(), (artRadius * 2).toInt())
                    )
                }
            } else {
                drawCircle(color = Color.DarkGray, radius = artRadius)
            }

            // Center spindle hole
            drawCircle(color = Color.Black, radius = radius * 0.10f)
            drawCircle(color = Color.White.copy(alpha = 0.3f), radius = radius * 0.10f, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
        }
    }
}
