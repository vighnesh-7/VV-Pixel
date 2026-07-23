package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TimerData(
    val title: String = "Timer",
    val formattedTime: String = "00:00",
    val isRunning: Boolean = true,
    val isStopwatch: Boolean = false,
    val pendingPauseIntent: android.app.PendingIntent? = null
)

@Composable
fun TimerCapsule(
    data: TimerData,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onActionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme

    if (!isExpanded) {
        // ================= COLLAPSED PILL =================
        Row(
            modifier = modifier
                .height(38.dp)
                .widthIn(min = 100.dp)
                .clip(CircleShape)
                .background(Color(0xFF0A0A0A))
                .clickable { onExpandToggle() }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (data.isStopwatch) Icons.Default.Timer else Icons.Default.HourglassBottom,
                contentDescription = null,
                tint = Color(0xFFFFB74D), // Warm Amber
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = data.formattedTime,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    fontSize = 13.sp
                )
            )
        }
    } else {
        // ================= EXPANDED CARD =================
        Box(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF0A0A0A))
                .clickable { onExpandToggle() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2D2010)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (data.isStopwatch) Icons.Default.Timer else Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = data.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = data.formattedTime,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                        )
                    }
                }

                if (data.pendingPauseIntent != null) {
                    IconButton(
                        onClick = onActionClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = if (data.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
