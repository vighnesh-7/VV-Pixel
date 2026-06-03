package com.example

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

class RingerModeDetailActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setBackgroundBlurRadius(80)
        }

        setContent {
            MyApplicationTheme {
                RingerModeDialogScreen {
                    finish()
                }
            }
        }
    }
}

@Composable
fun RingerModeDialogScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var currentMode by remember { mutableStateOf(audioManager.ringerMode) }

    // Outer backdrop with liquid reflections and gradients
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x1F121214),
                        Color(0x06888888),
                        Color(0x28121214)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Consume clicks */ },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ringer Mode Choices",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Choice list
                RingerModeItem(
                    title = "Muted (Silent)",
                    subtitle = "All ringers and alerts silent",
                    icon = Icons.Default.NotificationsOff,
                    isActive = currentMode == AudioManager.RINGER_MODE_SILENT,
                    onClick = {
                        try {
                            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                            currentMode = AudioManager.RINGER_MODE_SILENT
                        } catch (e: SecurityException) {
                            android.widget.Toast.makeText(context, "Do Not Disturb permission required for Silent", android.widget.Toast.LENGTH_LONG).show()
                            try {
                                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                                currentMode = AudioManager.RINGER_MODE_VIBRATE
                            } catch (ex: Exception) {}
                        } catch (e: Exception) {
                            currentMode = audioManager.ringerMode
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                RingerModeItem(
                    title = "Vibrate Only",
                    subtitle = "Alerts vibrate, ringer disabled",
                    icon = Icons.Default.Vibration,
                    isActive = currentMode == AudioManager.RINGER_MODE_VIBRATE,
                    onClick = {
                        try {
                            audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                            currentMode = AudioManager.RINGER_MODE_VIBRATE
                        } catch (e: Exception) {
                            currentMode = audioManager.ringerMode
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                RingerModeItem(
                    title = "Unmuted (Ringing)",
                    subtitle = "Audible ringtone and sound alert",
                    icon = Icons.Default.Notifications,
                    isActive = currentMode == AudioManager.RINGER_MODE_NORMAL,
                    onClick = {
                        try {
                            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                            currentMode = AudioManager.RINGER_MODE_NORMAL
                        } catch (e: Exception) {
                            currentMode = audioManager.ringerMode
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onDismiss() },
                    modifier = Modifier.fillMaxWidth(0.6f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        text = "Dismiss",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun RingerModeItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "containerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "contentColor"
    )

    val verticalPadding by animateDpAsState(
        targetValue = if (isActive) 18.dp else 12.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "verticalPadding"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = verticalPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = contentColor.copy(alpha = 0.8f)
                    )
                )
            }

            if (isActive) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
