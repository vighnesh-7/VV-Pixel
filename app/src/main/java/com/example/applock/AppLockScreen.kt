package com.example.applock

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppLockScreen(
    reminderText: String,
    usageTimeToday: String = "",
    onPatternConfirmed: (List<Int>) -> Boolean,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
    var currentPattern by remember { mutableStateOf(listOf<Int>()) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }
    var failedAttempts by remember { mutableStateOf(0) }
    var cooldownSeconds by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // Countdown timer for 1-minute cooling period
    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            isError = true
            errorMessage = "3 failed attempts. Try again in ${cooldownSeconds}s"
            delay(1000)
            cooldownSeconds--
            if (cooldownSeconds == 0) {
                failedAttempts = 0
                isError = false
                errorMessage = ""
                currentPattern = emptyList()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .clickable(enabled = false) { /* consume touches */ }
    ) {
        // Top Right Materialistic Pill for App Usage Time Today
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 24.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Usage Time Today",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = usageTimeToday.ifBlank { "0m used today" },
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }

        // Center Solid Material You Card
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .align(Alignment.Center),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "App Lock Icon",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "App Locked",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                if (reminderText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "\"$reminderText\"",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                PatternLockGrid(
                    modifier = Modifier.size(260.dp),
                    isError = isError,
                    isSuccess = isSuccess,
                    onPatternStart = {
                        if (cooldownSeconds == 0) {
                            currentPattern = emptyList()
                            isError = false
                            errorMessage = ""
                        }
                    },
                    onDotSelected = { index ->
                        if (cooldownSeconds == 0 && index !in currentPattern) {
                            currentPattern = currentPattern + index
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator?.vibrate(
                                        VibrationEffect.createOneShot(14, VibrationEffect.DEFAULT_AMPLITUDE)
                                    )
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator?.vibrate(14)
                                }
                            } catch (e: Exception) {
                                // Ignored
                            }
                        }
                    },
                    onPatternCompleted = {
                        if (cooldownSeconds > 0) return@PatternLockGrid

                        if (currentPattern.size < 4) {
                            failedAttempts++
                            if (failedAttempts >= 3) {
                                cooldownSeconds = 60
                            } else {
                                isError = true
                                errorMessage = "Minimum 4 dots required ($failedAttempts/3 failed)"
                                triggerErrorVibration(vibrator)
                                coroutineScope.launch {
                                    delay(700)
                                    currentPattern = emptyList()
                                    isError = false
                                }
                            }
                        } else {
                            val success = onPatternConfirmed(currentPattern)
                            if (success) {
                                isSuccess = true
                                isError = false
                                failedAttempts = 0
                            } else {
                                failedAttempts++
                                if (failedAttempts >= 3) {
                                    cooldownSeconds = 60
                                } else {
                                    isError = true
                                    errorMessage = "Incorrect pattern ($failedAttempts/3 failed)"
                                    triggerErrorVibration(vibrator)
                                    coroutineScope.launch {
                                        delay(750)
                                        currentPattern = emptyList()
                                        isError = false
                                    }
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = isError,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onCancel) {
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun triggerErrorVibration(vibrator: Vibrator?) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 50), -1)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 50, 50, 50), -1)
        }
    } catch (e: Exception) {
        // Ignored
    }
}
