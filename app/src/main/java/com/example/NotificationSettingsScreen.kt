package com.example

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val state by CapsulePreferences.stateFlow.collectAsState()

    var showAutoHideDialog by remember { mutableStateOf(false) }

    val hasListenerPermission = remember(context) {
        DynamicCapsuleNotifListener.isConnected(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Capsule Settings", fontWeight = FontWeight.Bold) },
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
            // Permission Banner
            if (!hasListenerPermission) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notification Listener Required",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Grant permission so the capsule can display incoming messages and actions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                } catch (e: Exception) { }
                            }
                        ) {
                            Text("Grant")
                        }
                    }
                }
            }

            // Master Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Enable Notification Capsule", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Show incoming message pills in Nothing Phone black style", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = state.notificationEnabled,
                    onCheckedChange = { checked -> CapsulePreferences.update { it.copy(notificationEnabled = checked) } }
                )
            }

            HorizontalDivider()

            // Auto-hide Delay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAutoHideDialog = true }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Auto-Hide Delay", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = if (state.notificationAutohideDelay < 0) "Never auto-hide" else "${state.notificationAutohideDelay} seconds",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }

            HorizontalDivider()

            // Notification Theme
            Text("Capsule Theme", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("SYSTEM", "DARK", "LIGHT").forEach { tOpt ->
                    FilterChip(
                        selected = state.notificationTheme == tOpt,
                        onClick = { CapsulePreferences.update { it.copy(notificationTheme = tOpt) } },
                        label = { Text(tOpt) }
                    )
                }
            }

            // Glow Effect
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Specular Glow Border", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Subtle rim highlight around the capsule", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = state.notificationGlow,
                    onCheckedChange = { checked -> CapsulePreferences.update { it.copy(notificationGlow = checked) } }
                )
            }
        }
    }

    if (showAutoHideDialog) {
        val delayOptions = listOf(1 to "1 sec", 2 to "2 sec", 3 to "3 sec", 5 to "5 sec", 10 to "10 sec", -1 to "Never")
        AlertDialog(
            onDismissRequest = { showAutoHideDialog = false },
            title = { Text("Auto-Hide Delay") },
            text = {
                Column {
                    delayOptions.forEach { (secs, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    CapsulePreferences.update { it.copy(notificationAutohideDelay = secs) }
                                    showAutoHideDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.notificationAutohideDelay == secs,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAutoHideDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
