package com.example

import android.graphics.Bitmap
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette

// Presets for default colors
val COLOR_PRESETS = listOf(
    Pair(Color(0xFF6B42C0), Color(0xFFD4BBFF)), // Purple/Dark
    Pair(Color(0xFF2255AA), Color(0xFFAACCEE)), // Blue/Dark
    Pair(Color(0xFF444444), Color(0xFFE0E0E0)), // Mono/Dark
    Pair(Color(0xFF882222), Color(0xFFFFB3B3)), // Red/Warm
    Pair(Color(0xFF992266), Color(0xFFFFB3DD)), // Pink/Dark
    Pair(Color(0xFF1E6A38), Color(0xFFAAF2C2))  // Green/Dark
)

data class MediaPlaybackData(
    val title: String = "No Media Playing",
    val artist: String = "Unknown Artist",
    val album: String = "",
    val albumArt: Bitmap? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val appName: String? = null
)

@Composable
fun MediaPlayerCapsule(
    data: MediaPlaybackData,
    prefs: CapsulePreferences.State,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeek: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    // Extract Palette colors asynchronously on IO thread
    var paletteWaveColor by remember { mutableStateOf<Color?>(null) }
    var paletteTextColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(data.albumArt) {
        val art = data.albumArt
        if (art != null && !art.isRecycled) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val palette = Palette.from(art).generate()
                    val domSwatch = palette.dominantSwatch
                    val vibSwatch = palette.vibrantSwatch ?: palette.lightVibrantSwatch

                    val waveC = domSwatch?.rgb?.let { Color(it) } ?: Color(0xFF6B42C0)
                    val textC = vibSwatch?.rgb?.let { Color(it) } ?: Color.White

                    paletteWaveColor = waveC
                    paletteTextColor = textC
                } catch (e: Exception) {
                    paletteWaveColor = null
                    paletteTextColor = null
                }
            }
        } else {
            paletteWaveColor = null
            paletteTextColor = null
        }
    }

    // Determine final colors based on preferences
    val preset = COLOR_PRESETS.getOrElse(prefs.colorPreset) { COLOR_PRESETS[0] }
    val waveColor = if (prefs.useCoverColors && paletteWaveColor != null) paletteWaveColor!! else preset.first
    val accentTextColor = if (prefs.useCoverColors && paletteTextColor != null) paletteTextColor!! else preset.second

    fun triggerVibration() {
        if (prefs.vibration) {
            try {
                view.performHapticFeedback(
                    HapticFeedbackConstants.KEYBOARD_TAP,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            } catch (e: Exception) { }
        }
    }

    if (!isExpanded) {
        // ================= COLLAPSED PILL STATE =================
        Row(
            modifier = modifier
                .height(38.dp)
                .widthIn(min = 110.dp)
                .clip(CircleShape)
                .background(Color(0xFF0A0A0A))
                .clickable {
                    triggerVibration()
                    onExpandToggle()
                }
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Album art thumbnail
            val imgBitmap = remember(data.albumArt) { data.albumArt?.asImageBitmap() }
            if (imgBitmap != null) {
                Image(
                    bitmap = imgBitmap,
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(waveColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Palette-colored circle container with music note icon
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accentTextColor.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    } else {
        // ================= EXPANDED CARD STATE =================
        val isFull = prefs.playerSize == "FULL"
        val cardBgAlpha = if (prefs.cardBg == "TRANSPARENT") 0.72f else 1.0f
        val imgBitmap = remember(data.albumArt) { data.albumArt?.asImageBitmap() }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(if (isFull) 124.dp else 102.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF000000).copy(alpha = cardBgAlpha))
        ) {
            // Wave animation background
            WaveBackground(
                waveColor = waveColor,
                isPlaying = data.isPlaying,
                isExpanded = true,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album Cover (Flat shape or Vinyl)
                    val artSize = if (isFull) 52.dp else 42.dp
                    if (prefs.coverType == "VINYL") {
                        VinylDiscView(
                            bitmap = imgBitmap,
                            isPlaying = data.isPlaying,
                            size = artSize
                        )
                    } else {
                        val shape = remember(prefs.albumShape) { CustomShape(prefs.albumShape) }
                        Box(
                            modifier = Modifier
                                .size(artSize)
                                .clip(shape)
                                .background(waveColor)
                        ) {
                            if (imgBitmap != null) {
                                Image(
                                    bitmap = imgBitmap,
                                    contentDescription = "Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Song Title & Artist
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = data.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = accentTextColor,
                                fontSize = if (isFull) 15.sp else 14.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (data.album.isNotEmpty()) "${data.artist} • ${data.album}" else data.artist,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (prefs.showMediaApp != "NONE" && !data.appName.isNullOrEmpty()) {
                            Text(
                                text = data.appName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = accentTextColor.copy(alpha = 0.9f),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Control Buttons (skipPrev, playPause, skipNext)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MediaControlButton(
                            icon = Icons.Default.SkipPrevious,
                            buttonType = prefs.buttonsType,
                            tint = accentTextColor,
                            onClick = {
                                triggerVibration()
                                onSkipPrevious()
                            }
                        )

                        MediaControlButton(
                            icon = if (data.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            buttonType = prefs.buttonsType,
                            tint = accentTextColor,
                            onClick = {
                                triggerVibration()
                                onPlayPause()
                            }
                        )

                        MediaControlButton(
                            icon = Icons.Default.SkipNext,
                            buttonType = prefs.buttonsType,
                            tint = accentTextColor,
                            onClick = {
                                triggerVibration()
                                onSkipNext()
                            }
                        )
                    }
                }

                // Optional Seek Bar
                if (prefs.showSlider && data.durationMs > 0L) {
                    val progress = (data.positionMs.toFloat() / data.durationMs.toFloat()).coerceIn(0f, 1f)
                    if (prefs.sliderStyle == "PILL") {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = accentTextColor,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .clip(CircleShape),
                            color = accentTextColor,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MediaControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    buttonType: String,
    tint: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "button_scale"
    )

    val shape = remember(buttonType) { ButtonCustomShape(buttonType) }

    Box(
        modifier = Modifier
            .size(36.dp)
            .scale(scale)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.22f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}
