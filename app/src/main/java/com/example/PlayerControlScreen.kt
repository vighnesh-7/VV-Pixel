package com.example

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerControlScreen(
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val state by CapsulePreferences.stateFlow.collectAsState()

    var showCollapseDelayDialog by remember { mutableStateOf(false) }

    val mockData = remember {
        MediaPlaybackData(
            title = "Over the Horizon",
            artist = "Samsung • Brand Sound",
            album = "Official Audio",
            albumArt = null,
            isPlaying = true,
            positionMs = 45000L,
            durationMs = 180000L,
            appName = "Spotify"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Player Control Customization", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Interactive Preview
            Text(
                text = "Live Preview",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                MediaPlayerCapsule(
                    data = mockData,
                    prefs = state,
                    isExpanded = true,
                    onExpandToggle = {},
                    onPlayPause = {},
                    onSkipPrevious = {},
                    onSkipNext = {}
                )
            }

            HorizontalDivider()

            // 1. Size
            Text("Player Card Size", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("COMPACT", "FULL").forEach { sizeOpt ->
                    FilterChip(
                        selected = state.playerSize == sizeOpt,
                        onClick = { CapsulePreferences.update { it.copy(playerSize = sizeOpt) } },
                        label = { Text(sizeOpt) }
                    )
                }
            }

            // 2. Colors & Palette
            Text("Colors & Dynamic Palette", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Use Album Cover Palette", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = state.useCoverColors,
                    onCheckedChange = { checked -> CapsulePreferences.update { it.copy(useCoverColors = checked) } }
                )
            }

            if (!state.useCoverColors) {
                Text("Fallback Color Preset", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    COLOR_PRESETS.forEachIndexed { idx, pair ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(pair.first)
                                .border(
                                    width = if (state.colorPreset == idx) 3.dp else 1.dp,
                                    color = if (state.colorPreset == idx) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { CapsulePreferences.update { it.copy(colorPreset = idx) } }
                        )
                    }
                }
            }

            HorizontalDivider()

            // 3. Album Cover Shape
            Text("Album Cover Shape", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            val shapes = listOf("BLOB", "SQUARE", "ROUNDED_RECT", "HEXAGON", "SHIELD", "OCTAGON")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                shapes.chunked(3).forEach { rowShapes ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowShapes.forEach { s ->
                            FilterChip(
                                selected = state.albumShape == s,
                                onClick = { CapsulePreferences.update { it.copy(albumShape = s) } },
                                label = { Text(s, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            // 4. Album Cover Type
            Text("Album Cover Display", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("STANDARD", "VINYL").forEach { typeOpt ->
                    FilterChip(
                        selected = state.coverType == typeOpt,
                        onClick = { CapsulePreferences.update { it.copy(coverType = typeOpt) } },
                        label = { Text(typeOpt) }
                    )
                }
            }

            HorizontalDivider()

            // 5. Button Shapes
            Text("Control Button Shapes", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            val btnShapes = listOf("CIRCLE", "STAR8", "STAR12", "BLOB", "CLOVER")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                btnShapes.take(3).forEach { bs ->
                    FilterChip(
                        selected = state.buttonsType == bs,
                        onClick = { CapsulePreferences.update { it.copy(buttonsType = bs) } },
                        label = { Text(bs, fontSize = 11.sp) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                btnShapes.drop(3).forEach { bs ->
                    FilterChip(
                        selected = state.buttonsType == bs,
                        onClick = { CapsulePreferences.update { it.copy(buttonsType = bs) } },
                        label = { Text(bs, fontSize = 11.sp) }
                    )
                }
            }

            HorizontalDivider()

            // 6. Media App Source Name/Icon
            Text("Show Media Source", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("NONE", "ICON_ONLY", "ICON_AND_NAME").forEach { opt ->
                    FilterChip(
                        selected = state.showMediaApp == opt,
                        onClick = { CapsulePreferences.update { it.copy(showMediaApp = opt) } },
                        label = { Text(opt, fontSize = 11.sp) }
                    )
                }
            }

            // 7. Background Style
            Text("Card Background", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("AMOLED_BLACK", "TRANSPARENT").forEach { opt ->
                    FilterChip(
                        selected = state.cardBg == opt,
                        onClick = { CapsulePreferences.update { it.copy(cardBg = opt) } },
                        label = { Text(opt) }
                    )
                }
            }

            // 8. Seek bar & Vibration switches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show Seek Bar", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = state.showSlider,
                    onCheckedChange = { checked -> CapsulePreferences.update { it.copy(showSlider = checked) } }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Haptic Feedback on Taps", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = state.vibration,
                    onCheckedChange = { checked -> CapsulePreferences.update { it.copy(vibration = checked) } }
                )
            }

            HorizontalDivider()

            // 9. Collapse Delay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCollapseDelayDialog = true }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Auto Collapse Delay", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = if (state.collapseDelay < 0) "Never collapse" else "${state.collapseDelay} seconds",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    }

    if (showCollapseDelayDialog) {
        val delayOptions = listOf(0 to "Instantly", 1 to "1 sec", 2 to "2 sec", 3 to "3 sec", 5 to "5 sec", 10 to "10 sec", -1 to "Never")
        AlertDialog(
            onDismissRequest = { showCollapseDelayDialog = false },
            title = { Text("Auto Collapse Delay") },
            text = {
                Column {
                    delayOptions.forEach { (secs, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    CapsulePreferences.update { it.copy(collapseDelay = secs) }
                                    showCollapseDelayDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.collapseDelay == secs,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCollapseDelayDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
