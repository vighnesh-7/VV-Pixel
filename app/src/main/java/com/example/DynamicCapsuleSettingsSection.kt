package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DynamicCapsuleSettingsSection(
    onOpenPlayerCustomization: () -> Unit,
    onOpenNotificationCustomization: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by CapsulePreferences.stateFlow.collectAsState()

    var isServiceRunning by remember { mutableStateOf(DynamicCapsuleService.isServiceRunning) }
    val hasOverlayPermission = remember(context) { Settings.canDrawOverlays(context) }

    fun startOrStopService(enable: Boolean) {
        if (enable) {
            if (!Settings.canDrawOverlays(context)) {
                Toast.makeText(context, "Overlay permission required for Dynamic Capsule!", Toast.LENGTH_LONG).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
                return
            }
            val intent = Intent(context, DynamicCapsuleService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            isServiceRunning = true
        } else {
            context.stopService(Intent(context, DynamicCapsuleService::class.java))
            isServiceRunning = false
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Dynamic Capsule",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Main Switch Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dynamic Capsule Overlay",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = if (isServiceRunning) "Active floating island" else "Disabled",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = { enable -> startOrStopService(enable) }
                    )
                }

                if (isServiceRunning || true) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Row 1: Player Control
                    CapsuleFeatureRow(
                        title = "Player Control",
                        subtitle = "Media playback wave & controls",
                        icon = Icons.Default.PlayCircle,
                        checked = state.playerEnabled,
                        onCheckedChange = { enabled ->
                            CapsulePreferences.update { it.copy(playerEnabled = enabled) }
                        },
                        onClick = onOpenPlayerCustomization
                    )

                    // Row 2: Volume Control
                    CapsuleFeatureRow(
                        title = "Volume Control",
                        subtitle = "Visual floating volume slider board",
                        icon = Icons.Default.VolumeUp,
                        checked = state.volumeEnabled,
                        onCheckedChange = { enabled ->
                            CapsulePreferences.update { it.copy(volumeEnabled = enabled) }
                        }
                    )

                    // Row 3: App Progress
                    CapsuleFeatureRow(
                        title = "App Progress",
                        subtitle = "Live download & install progress pill",
                        icon = Icons.Default.Download,
                        checked = state.progressEnabled,
                        onCheckedChange = { enabled ->
                            CapsulePreferences.update { it.copy(progressEnabled = enabled) }
                        }
                    )

                    // Row 4: Timer / Stopwatch
                    CapsuleFeatureRow(
                        title = "Timer / Stopwatch",
                        subtitle = "Clock countdowns & stopwatch overlay",
                        icon = Icons.Default.Timer,
                        checked = state.timerEnabled,
                        onCheckedChange = { enabled ->
                            CapsulePreferences.update { it.copy(timerEnabled = enabled) }
                        }
                    )

                    // Row 5: Notification Capsule
                    CapsuleFeatureRow(
                        title = "Notification Capsule",
                        subtitle = "Nothing Phone black pill notifications",
                        icon = Icons.Default.Notifications,
                        checked = state.notificationEnabled,
                        onCheckedChange = { enabled ->
                            CapsulePreferences.update { it.copy(notificationEnabled = enabled) }
                        },
                        onClick = onOpenNotificationCustomization
                    )
                }
            }
        }
    }
}

@Composable
private fun CapsuleFeatureRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                if (onClick != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
