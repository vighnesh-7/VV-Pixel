@file:OptIn(ExperimentalMaterial3Api::class)
package com.example

import android.Manifest
import androidx.compose.material3.ExperimentalMaterial3Api
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.MyApplicationTheme
import kotlin.math.abs
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.app.AlarmManager
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path

class MainActivity : ComponentActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    // State flow for dashboard real-time shake preview
    private val sensorX = mutableFloatStateOf(0f)
    private val sensorY = mutableFloatStateOf(0f)
    private val sensorZ = mutableFloatStateOf(0f)

    private var ringerReceiver: android.content.BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request to turn the screen on, dismiss lockscreen/keyguard, and keep the screen awake
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        }
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Setup SensorManager for in-app physics visualizer preview
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    "VV Pixel Enhancer",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif
                                )
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                            )
                        )
                    }
                ) { innerPadding ->
                    DashboardScreen(
                        modifier = Modifier.padding(innerPadding),
                        sensorX = sensorX.floatValue,
                        sensorY = sensorY.floatValue,
                        sensorZ = sensorZ.floatValue
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        if (ringerReceiver == null) {
            ringerReceiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: android.content.Intent?) {
                    context?.let {
                        RingerToggleWidget.updateAllWidgetsAndTile(it)
                    }
                }
            }
            val filter = android.content.IntentFilter(android.media.AudioManager.RINGER_MODE_CHANGED_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(ringerReceiver, filter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(ringerReceiver, filter)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)

        ringerReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                // Ignore
            }
            ringerReceiver = null
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        sensorX.floatValue = event.values[0]
        sensorY.floatValue = event.values[1]
        sensorZ.floatValue = event.values[2]
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val service = "${context.packageName}/${VVPixelAccessibilityService::class.java.name}"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServices.contains(service)
}

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    sensorX: Float,
    sensorY: Float,
    sensorZ: Float
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Permission States
    var hasWriteSettings by remember { mutableStateOf(Settings.System.canWrite(context)) }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasDndPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                notificationManager?.isNotificationPolicyAccessGranted == true
            } else {
                true
            }
        )
    }


    val sharedPrefs = remember(context) { context.getSharedPreferences("com.example.vvpixel.SETTINGS", Context.MODE_PRIVATE) }
    var isDoubleTapEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("double_tap_to_lock_enabled", true)) }
    var isLockVibrationEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("lock_vibration_enabled", true)) }
    var isShakeTorchEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("shake_torch_enabled", true)) }

    // Checking of background services state
    var isAccessibilityRunning by remember {
        mutableStateOf(VVPixelAccessibilityService.isServiceRunning || isAccessibilityServiceEnabled(context))
    }
    var isShakeTorchRunning by remember { mutableStateOf(ShakeTorchService.isServiceRunning) }



    // Refresh states instantly when window becomes active/resumed
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasWriteSettings = Settings.System.canWrite(context)
                hasOverlayPermission = Settings.canDrawOverlays(context)
                isAccessibilityRunning = VVPixelAccessibilityService.isServiceRunning || isAccessibilityServiceEnabled(context)
                isShakeTorchRunning = ShakeTorchService.isServiceRunning
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                    hasDndPermission = nm?.isNotificationPolicyAccessGranted == true
                } else {
                    hasDndPermission = true
                }
                

            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Function to request automatic lock/calculator widget pinning
    fun requestPinWidget(providerClass: Class<*>, widgetName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val myProvider = ComponentName(context, providerClass)
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val successIntent = Intent(context, providerClass)
                val successCallback = PendingIntent.getBroadcast(
                    context,
                    101,
                    successIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
                Toast.makeText(context, "Requested homescreen placement for $widgetName!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Automatic pin not supported. Long-press home screen to add $widgetName manually.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Please add the widget from your homescreen manually.", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ================= SECTION 1: LOCK =================
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Lock",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            // Double Tap to Lock Card (First)
            PixelEnhancerCard(
                title = "Double-Tap to Lock",
                subtitle = "Pins the lock launcher. Double-tap to secure device instantly.",
                icon = Icons.Default.Lock,
                rightElement = {
                    FilledTonalButton(
                        onClick = {
                            requestPinWidget(DoubleTapLockWidget::class.java, "Double-Tap to Lock")
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Add", style = MaterialTheme.typography.labelLarge)
                    }
                }
            )

            // Lock Widget 1x1 (Second)
            PixelEnhancerCard(
                title = "Lock Widget",
                subtitle = "Long-press homescreen → Widgets",
                icon = Icons.Default.Lock,
                rightElement = {
                    FilledTonalButton(
                        onClick = {
                            requestPinWidget(LockWidget2x1::class.java, "Lock Device")
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Add", style = MaterialTheme.typography.labelLarge)
                    }
                }
            )

            // Subtle Vibration Lock with Slider (Third)
            var lockVibrationIntensity by remember { mutableStateOf(sharedPrefs.getFloat("lock_vibration_intensity", 0.5f)) }

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
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Subtle Vibration Lock",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Vibrate mildly when lock triggers are fired",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                )
                            )
                        }
                        Switch(
                            checked = isLockVibrationEnabled,
                            onCheckedChange = { enable ->
                                isLockVibrationEnabled = enable
                                sharedPrefs.edit().putBoolean("lock_vibration_enabled", enable).apply()
                            }
                        )
                    }

                    if (isLockVibrationEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Vibration Intensity",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = when {
                                        lockVibrationIntensity < 0.35f -> "Mild"
                                        lockVibrationIntensity < 0.7f -> "Medium"
                                        else -> "Strong"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Slider(
                                value = lockVibrationIntensity,
                                onValueChange = { newValue ->
                                    lockVibrationIntensity = newValue
                                    sharedPrefs.edit().putFloat("lock_vibration_intensity", newValue).apply()
                                },
                                valueRange = 0.1f..1.0f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // ================= SECTION 2: QUICK SETTINGS =================
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Quick Settings Tiles",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            // Tile Info Item 2: Ringer Mode
            PixelEnhancerTileInfoCard(
                title = "Ringer Mode Tile",
                subtitle = "Tap to cycle through Sound, Vibrate, and Silent modes",
                iconPainter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_unmute)
            )

            // Tile Info Item 3: Adaptive Brightness
            PixelEnhancerTileInfoCard(
                title = "Adaptive Brightness Tile",
                subtitle = "Automatically calibrate and match ambient screen luminance levels",
                iconPainter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_brightness_auto)
            )

            // Tile Info Item 4: Volume QS Tile
            PixelEnhancerTileInfoCard(
                title = "Volume QS Tile",
                subtitle = "Tap to expand a custom visual slider board for media and ring levels",
                iconPainter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_volume_equalizer)
            )

            // Tile Info Item 5: Caffeinate Tile
            PixelEnhancerTileInfoCard(
                title = "Caffeinate Tile",
                subtitle = "Keep screen awake and bypass automatic sleep/lock indefinitely",
                iconPainter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_coffee)
            )
        }

        // ================= SECTION 3: WIDGETS =================
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Widgets",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            // Calculator Widget
            PixelEnhancerCard(
                title = "Calculator Widget",
                subtitle = "Long-press homescreen → Widgets",
                icon = Icons.Default.Add,
                rightElement = {
                    FilledTonalButton(
                        onClick = {
                            requestPinWidget(CalculatorWidget::class.java, "Pocket Calculator")
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Add", style = MaterialTheme.typography.labelLarge)
                    }
                }
            )

            // Ringer Toggle Widget (Now with semantic unmute bell icon)
            PixelEnhancerCard(
                title = "Unmute/Mute/Vibrate Widget",
                subtitle = "One-tap homescreen sound state switcher",
                iconPainter = painterResource(id = R.drawable.ic_unmute),
                rightElement = {
                    FilledTonalButton(
                        onClick = {
                            requestPinWidget(RingerToggleWidget::class.java, "Volume Switcher")
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Add", style = MaterialTheme.typography.labelLarge)
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ================= SUB-SECTION: MINIMALIST CLOCK PLAYGROUND =================
            Text(
                text = "Minimalist Clock Widgets",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            var forceDarkPreview by remember { mutableStateOf(true) }
            val dynamicAlarmText = remember { mutableStateOf("Sleep Well") }
            LaunchedEffect(Unit) {
                dynamicAlarmText.value = AlarmFetcher.getEarliestAlarm(context, "Sleep Well")
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header / Theme toggle bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Doodle Widget Playground",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Adaptive dynamic design",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Light/Dark Toggle pill
                        Button(
                            onClick = { forceDarkPreview = !forceDarkPreview },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (forceDarkPreview) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (forceDarkPreview) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = if (forceDarkPreview) Icons.Default.Check /* Dark indicator */ else Icons.Default.Favorite /* Light Indicator */,
                                contentDescription = "Toggle Preview Theme",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (forceDarkPreview) "Dark View" else "Light View",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 1. Cozy Face Clock Column
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1. Cozy Sleepy Face",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            FilledTonalButton(
                                onClick = {
                                    requestPinWidget(DoodleFaceClockWidget::class.java, "Cozy Face Clock")
                                },
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Add", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        CozyFacePreview(alarmTime = dynamicAlarmText.value, forceDark = forceDarkPreview)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 2. Blob Monster Clock Column
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "2. Cute Blob Peek-a-boo",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            FilledTonalButton(
                                onClick = {
                                    requestPinWidget(BlobMonsterClockWidget::class.java, "Blob Monster Clock")
                                },
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Add", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        BlobMonsterPreview(alarmTime = dynamicAlarmText.value, forceDark = forceDarkPreview)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 3. Nothing LED Dot Grid Column
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "3. Nothing LED Dot Grid",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            FilledTonalButton(
                                onClick = {
                                    requestPinWidget(NothingPixelClockWidget::class.java, "Nothing LED Clock")
                                },
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Add", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        NothingPixelPreview(alarmTime = dynamicAlarmText.value, forceDark = forceDarkPreview)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 4. iOS Liquid Glass Clock Column
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "4. iOS Liquid Glass",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            FilledTonalButton(
                                onClick = {
                                    requestPinWidget(LiquidGlassClockWidget::class.java, "iOS Liquid Glass Clock")
                                },
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Add", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        LiquidGlassPreview(alarmTime = dynamicAlarmText.value, forceDark = forceDarkPreview)
                    }
                }
            }
        }

        // ================= SECTION 4: GESTURES =================
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Gestures",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            // Shake to Toggle Torch Card (Now using semantic flashlight Lightbulb icon)
            PixelEnhancerCard(
                title = "Shake to Toggle Torch",
                subtitle = if (isShakeTorchRunning && isShakeTorchEnabled) "Active — shake to toggle" else "Disabled — shake to toggle",
                icon = Icons.Default.Lightbulb,
                rightElement = {
                    Switch(
                        checked = isShakeTorchEnabled && isShakeTorchRunning,
                        onCheckedChange = { enable ->
                            isShakeTorchEnabled = enable
                            sharedPrefs.edit().putBoolean("shake_torch_enabled", enable).apply()

                            val intent = Intent(context, ShakeTorchService::class.java)
                            if (enable) {
                                context.startForegroundService(intent)
                            } else {
                                context.stopService(intent)
                            }
                            isShakeTorchRunning = enable
                        }
                    )
                }
            )

            // Interactive Shake Force Visualizer
            AnimatedVisibility(visible = isShakeTorchEnabled && isShakeTorchRunning) {
                var shakeThreshold by remember { mutableStateOf(sharedPrefs.getFloat("shake_torch_threshold", 12.0f)) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Real-time Force Sensor Vector",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Gray.copy(alpha = 0.2f))
                        ) {
                            val widthPercentage = (abs(sensorX) / 20.0f).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(widthPercentage)
                                    .background(if (abs(sensorX) > shakeThreshold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                            )
                        }

                        Text(
                            text = "Shake Threshold Intensity: ${String.format("%.1f m/s²", shakeThreshold)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Slider(
                            value = shakeThreshold,
                            onValueChange = { newValue ->
                                shakeThreshold = newValue
                                sharedPrefs.edit().putFloat("shake_torch_threshold", newValue).apply()
                            },
                            valueRange = 8.0f..22.0f,
                            steps = 14
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Current Accel: ${String.format("%.2f m/s²", sensorX)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                            Text(
                                text = "Trigger Target: ${String.format("%.2f m/s²", shakeThreshold)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
        }

        // ================= SECTION 5: PERMISSIONS COLLAPSIBLE CARD =================
        var showPermissionsCenter by remember { mutableStateOf(false) }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Config Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Core Setup Permissions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = { showPermissionsCenter = !showPermissionsCenter }) {
                        Icon(
                            imageVector = if (showPermissionsCenter) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand Permissions"
                        )
                    }
                }

                AnimatedVisibility(visible = showPermissionsCenter) {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Divider()

                        PermissionItem(
                            title = "System Settings Writer",
                            description = "Required to adjust screen brightness and display preferences.",
                            isGranted = hasWriteSettings,
                            onRequestGrant = {
                                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            }
                        )

                        PermissionItem(
                            title = "Mute Mode (DND) Access",
                            description = "Required to switch the system into total mute (silent) mode.",
                            isGranted = hasDndPermission,
                            onRequestGrant = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, "Mute mode is fully supported.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        PermissionItem(
                            title = "Overlay Sliders",
                            description = "Required to display custom volume overlays.",
                            isGranted = hasOverlayPermission,
                            onRequestGrant = {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            }
                        )


                    }
                }
            }
        }



        Spacer(modifier = Modifier.height(16.dp))

        // Author subtle watermark
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp, bottom = 8.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = " - V_Vighnesh😉",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

    }
}

@Composable
fun PixelEnhancerCard(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    rightElement: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Squircle Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (iconPainter != null) {
                    Icon(
                        painter = iconPainter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Middle: Text Area
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Right: Element
            rightElement()
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String = "Grant",
    onRequestGrant: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Warning,
                    contentDescription = if (isGranted) "Granted" else "Required",
                    tint = if (isGranted) Color(0xFF388E3C) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        
        Button(
            onClick = onRequestGrant,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGranted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                contentColor = if (isGranted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isGranted) "Granted" else actionLabel, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun PixelEnhancerTileInfoCard(
    title: String,
    subtitle: String,
    iconPainter: androidx.compose.ui.graphics.painter.Painter
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Squircle Icon container with secondaryContainer tint (matches systems theme alignment)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = iconPainter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Middle: Text details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                )
            }
        }
    }
}

// ================= CLOCK PLAYGROUND COMPOSABLES =================

@Composable
fun rememberTimeFormatted(pattern: String): String {
    var timeFormatted by remember { mutableStateOf("") }
    LaunchedEffect(pattern) {
        while (true) {
            timeFormatted = SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
            kotlinx.coroutines.delay(1000L)
        }
    }
    return if (timeFormatted.isEmpty()) SimpleDateFormat(pattern, Locale.getDefault()).format(Date()) else timeFormatted
}

@Composable
fun CozyFacePreview(alarmTime: String, forceDark: Boolean) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "FloatFace")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatAnim"
    )

    val timeText = rememberTimeFormatted("h:mm")
    val dateText = rememberTimeFormatted("EEEE\nd MMM")

    val bgGradient = if (forceDark) {
        Brush.linearGradient(listOf(Color(0xBF1C1A22), Color(0x990E0E12)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFE2EDFE), Color(0xFFC9DDFB)))
    }
    val textColor = if (forceDark) Color.White else Color(0xFF1D1B20)
    val descColor = if (forceDark) Color.White.copy(alpha = 0.75f) else Color(0xFF49454F)
    val borderCol = if (forceDark) Color(0x4DFFFFFF) else Color(0x33000000)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(bgGradient)
            .border(1.2.dp, borderCol, RoundedCornerShape(28.dp))
            .clickable {
                try {
                    context.startActivity(WidgetUtils.getClockIntent(context))
                } catch (e: Exception) {
                    Toast.makeText(context, "Opening clock app...", Toast.LENGTH_SHORT).show()
                }
            }
    ) {
        // Left Column representing time, date, and alarm, padded perfectly
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.55f)
                .padding(start = 20.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.W500,
                    color = textColor,
                    fontSize = 68.sp,
                    letterSpacing = (-2).sp
                ),
                modifier = Modifier.offset(y = (-8).dp)
            )
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = descColor,
                    lineHeight = 18.sp,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            if (alarmTime.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = if (forceDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Alarm",
                        tint = if (forceDark) Color(0xFFFFF9C4) else Color(0xFFFFA000),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = alarmTime,
                        color = if (forceDark) Color(0xFFFFF9C4) else Color(0xFFFFA000),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Image(
            painter = painterResource(id = R.drawable.ic_doodle_face_cozy),
            contentDescription = "Doodle Face Cozy",
            modifier = Modifier
                .size(138.dp)
                .offset(y = floatAnim.dp)
                .align(Alignment.BottomEnd)
        )
    }
}

@Composable
fun BlobMonsterPreview(alarmTime: String, forceDark: Boolean) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "ScaleMonster")
    val wiggleAnim by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WiggleAnim"
    )

    val timeText = rememberTimeFormatted("h:mm")
    val dayText = rememberTimeFormatted("EEEE").uppercase()
    val dateText = rememberTimeFormatted("d MMM")

    val bgGradient = if (forceDark) {
        Brush.linearGradient(listOf(Color(0xBF1C1A22), Color(0x990E0E12)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFFDE8E6), Color(0xFFFCD0CD)))
    }
    val textColor = if (forceDark) Color.White else Color(0xFF202124)
    val borderCol = if (forceDark) Color(0x4DFFFFFF) else Color(0x33000000)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(bgGradient)
            .border(1.2.dp, borderCol, RoundedCornerShape(28.dp))
            .clickable {
                try {
                    context.startActivity(WidgetUtils.getClockIntent(context))
                } catch (e: Exception) {
                    Toast.makeText(context, "Opening clock app...", Toast.LENGTH_SHORT).show()
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.55f)
                .padding(start = 20.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.W500,
                    color = textColor,
                    fontSize = 68.sp,
                    letterSpacing = (-2).sp
                ),
                modifier = Modifier.offset(y = (-8).dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = if (forceDark) Color.White else Color(0xFFD93025),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = dayText,
                        color = if (forceDark) Color.Black else Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
                Text(
                    text = dateText,
                    color = if (forceDark) Color.White.copy(alpha = 0.82f) else Color.DarkGray,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                )
            }
            if (alarmTime.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = if (forceDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Alarm",
                        tint = Color(0xFFD93025),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = alarmTime,
                        color = Color(0xFFD93025),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Image(
            painter = painterResource(id = R.drawable.ic_doodle_monster),
            contentDescription = "Doodle Blob Monster",
            modifier = Modifier
                .size(138.dp)
                .scale(wiggleAnim)
                .offset(x = (wiggleAnim * 2 - 2).dp)
                .align(Alignment.BottomEnd)
        )
    }
}

@Composable
fun DotMatrixTimeText(
    timeText: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    dotScale: Float = 1.0f
) {
    val density = LocalDensity.current
    val dotSize = with(density) { 5.dp.toPx() } * dotScale
    val dotSpacing = with(density) { 1.dp.toPx() } * dotScale * 1.5f
    val charWidth = 5f * dotSize + 4f * dotSpacing
    val charHeight = 7f * dotSize + 6f * dotSpacing
    val charSpacing = with(density) { 5.dp.toPx() } * dotScale

    Canvas(modifier = modifier) {
        val dotMatrices = mapOf(
            '0' to arrayOf(
                booleanArrayOf(false, true,  true,  true,  false),
                booleanArrayOf(true,  false, false, false, true),
                booleanArrayOf(true,  false, false, false, true),
                booleanArrayOf(true,  false, false, false, true),
                booleanArrayOf(true,  false, false, false, true),
                booleanArrayOf(true,  false, false, false, true),
                booleanArrayOf(false, true,  true,  true,  false)
            ),
            '1' to arrayOf(
                booleanArrayOf(false, false, true,  false, false),
                booleanArrayOf(false, true,  true,  false, false),
                booleanArrayOf(false, false, true,  false, false),
                booleanArrayOf(false, false, true,  false, false),
                booleanArrayOf(false, false, true,  false, false),
                booleanArrayOf(false, false, true,  false, false),
                booleanArrayOf(false, true,  true,  true,  false)
            ),
            '2' to arrayOf(
                booleanArrayOf(false, true,  true,  true,  false),
                booleanArrayOf(true,  false, false, false, true),
                booleanArrayOf(false, false, false, false, true),
                booleanArrayOf(false, true,  true,  true,  false),
                booleanArrayOf(true,  false, false, false, false),
                booleanArrayOf(true,  false, false, false, false),
                booleanArrayOf(true,  true,  true,  true,  true)
            ),
            '3' to arrayOf(
                booleanArrayOf(false, true,  true,  true,  false),
                booleanArrayOf(true,  false, false, false, true),
                booleanArrayOf(false, false, false, false, true),
                booleanArrayOf(false, false, true,  true,  false),
                booleanArrayOf(false, false, false, false, true),
                booleanArrayOf(true,  false, false, false, true),
                booleanArrayOf(false, true,  true,  true,  false)
            ),
            '4' to arrayOf(
                booleanArrayOf(true,  false, false, false, true),
                booleanArrayOf(true,  false, false, false, true),
                booleanArrayOf(true,  false, false, false, true),
                booleanArrayOf(true,  true,  true,  true,  true),
                booleanArrayOf(false, false, false, false, true),
                booleanArrayOf(false, false, false, false, true),
                booleanArrayOf(false, false, false, false, true)
            ),
            '5' to arrayOf(
                booleanArrayOf(true,  true,  true,  true,  true),
                booleanArrayOf(true,  false, false, false, false),
                booleanArrayOf(true,  true,  true,  true,  false),
                booleanArrayOf(false, false, false, false, true),
                booleanArrayOf(false, false, false, false, true),
                booleanArrayOf(true,  false, false, false, true),
                booleanArrayOf(false, true,  true,  true,  false)
            ),
            '6' to arrayOf(
                booleanArrayOf(false, true,  true,  true,  false),
                booleanArrayOf(true,  false, false, false, true),
                booleanArrayOf(true,  false, false, false, false),
                booleanArrayOf(true,  true,  true,  true,  false),
                booleanArrayOf(true,  false, false, false, true),
                booleanArrayOf(true,  false, false, false, true),
                booleanArrayOf(false, true,  true,  true,  false)
            ),
            '7' to arrayOf(
                booleanArrayOf(true,  true,  true,  true,  true),
                booleanArrayOf(true,  false, false, false, true),
                booleanArrayOf(false, false, false, false, true),
                booleanArrayOf(false, false, false, true,  false),
                booleanArrayOf(false, false, true,  false, false),
                booleanArrayOf(false, false, true,  false, false),
                booleanArrayOf(false, false, true,  false, false)
            ),
            '8' to arrayOf(
                booleanArrayOf(false, true,  true,  true,  false),
                booleanArrayOf(true,  false, false, false, true),
                booleanArrayOf(true,  false, false, false, true),
                booleanArrayOf(false, true,  true,  true,  false),
                booleanArrayOf(true,  false, false, false, true),
                booleanArrayOf(true,  false, false, false, true),
                booleanArrayOf(false, true,  true,  true,  false)
            ),
            '9' to arrayOf(
                booleanArrayOf(false, true,  true,  true,  false),
                booleanArrayOf(true,  false, false, false, true),
                booleanArrayOf(true,  false, false, false, true),
                booleanArrayOf(false, true,  true,  true,  true),
                booleanArrayOf(false, false, false, false, true),
                booleanArrayOf(true,  false, false, false, true),
                booleanArrayOf(false, true,  true,  true,  false)
            ),
            ':' to arrayOf(
                booleanArrayOf(false, false, false, false, false),
                booleanArrayOf(false, false, false, false, false),
                booleanArrayOf(false, false, true,  false, false),
                booleanArrayOf(false, false, false, false, false),
                booleanArrayOf(false, false, true,  false, false),
                booleanArrayOf(false, false, false, false, false),
                booleanArrayOf(false, false, false, false, false)
            ),
            ' ' to arrayOf(
                booleanArrayOf(false, false, false, false, false),
                booleanArrayOf(false, false, false, false, false),
                booleanArrayOf(false, false, false, false, false),
                booleanArrayOf(false, false, false, false, false),
                booleanArrayOf(false, false, false, false, false),
                booleanArrayOf(false, false, false, false, false),
                booleanArrayOf(false, false, false, false, false)
            )
        )

        // Draw centered horizontally and vertically
        val totalWidth = timeText.length * charWidth + (timeText.length - 1) * charSpacing
        val startX = (size.width - totalWidth) / 2f
        val startY = (size.height - charHeight) / 2f

        timeText.forEachIndexed { charIndex, char ->
            val grid = dotMatrices[char] ?: dotMatrices[' ']!!
            val charX = startX + charIndex.toFloat() * (charWidth + charSpacing)
            
            for (r in 0 until 7) {
                for (c in 0 until 5) {
                    val dotX = charX + c.toFloat() * (dotSize + dotSpacing) + dotSize / 2f
                    val dotY = startY + r.toFloat() * (dotSize + dotSpacing) + dotSize / 2f
                    val isActive = grid[r][c]
                    if (isActive) {
                        drawCircle(
                            color = color,
                            radius = dotSize / 2f,
                            center = Offset(dotX, dotY)
                        )
                    } else {
                        drawCircle(
                            color = color.copy(alpha = 0.08f),
                            radius = dotSize / 2f,
                            center = Offset(dotX, dotY)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NothingPixelPreview(alarmTime: String, forceDark: Boolean) {
    val context = LocalContext.current
    val timeText = rememberTimeFormatted("h:mm")
    val dateText = rememberTimeFormatted("d MMM yyyy")

    val bgGradient = if (forceDark) {
        Brush.linearGradient(listOf(Color(0xFF0C0D0E), Color(0xFF15181B)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFF2F4F7), Color(0xFFE4E7EC)))
    }
    
    val textColor = if (forceDark) Color.White else Color(0xFF101828)
    val descColor = if (forceDark) Color(0xFF888888) else Color(0xFF667085)
    val borderCol = if (forceDark) Color(0x4DFFFFFF) else Color(0x33000000)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(bgGradient)
            .border(1.2.dp, borderCol, RoundedCornerShape(28.dp))
            .clickable {
                try {
                    context.startActivity(WidgetUtils.getClockIntent(context))
                } catch (e: Exception) {
                    Toast.makeText(context, "Opening clock app...", Toast.LENGTH_SHORT).show()
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Expanded high-fidelity Dot Matrix Clock displaying centered in full width
            DotMatrixTimeText(
                timeText = timeText,
                color = textColor,
                dotScale = 1.9f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = dateText,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = descColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    fontSize = 12.sp
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            if (alarmTime.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = Color(0x19FF2D55),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0x33FF2D55),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFFF2D55))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = alarmTime,
                        color = Color(0xFFFF2D55),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun LiquidGlassPreview(alarmTime: String, forceDark: Boolean) {
    val context = LocalContext.current
    val timeText = rememberTimeFormatted("h:mm").replace(":", " : ")
    val dateText = rememberTimeFormatted("EEEE, MMMM d")

    val dateColor = Color(0xCCFFFFFF) // Refined premium solid translucent white for authentic iOS look
    val alarmColor = Color(0xFFFFFFFF) // Crisp solid white for readability

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(32.dp))
            .drawBehind {
                val w = this.size.width
                val h = this.size.height

                // 1. Sleek midnight space navy/indigo background
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF030114), // Infinite depth violet black
                            Color(0xFF060D4D)  // Deep midnight indigo
                        )
                    )
                )

                // 2. High-fidelity fluid satin diagonal wave (recreating the iOS 26 reference exactly)
                val wavePath = Path().apply {
                    moveTo(0f, h * 0.95f)
                    cubicTo(
                        w * 0.35f, h * 0.65f,
                        w * 0.65f, h * 0.15f,
                        w, h * 0.22f
                    )
                    lineTo(w, 0f)
                    lineTo(0f, 0f)
                    close()
                }

                drawPath(
                    path = wavePath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF4C21CA), // Rich luminous dark purple
                            Color(0xFF1637C4), // Smooth vibrant cobalt blue
                            Color(0xFF0B91EA)  // Bright electric glass blue
                        ),
                        start = Offset(0f, h),
                        end = Offset(w, 0f)
                    ),
                    alpha = 0.9f
                )

                // 3. Secondary deep folding wave accent providing layered depth and refraction contrast
                val shadowWave = Path().apply {
                    moveTo(0f, h * 0.52f)
                    cubicTo(
                        w * 0.4f, h * 0.52f,
                        w * 0.62f, h * 0.88f,
                        w * 0.95f, h * 0.72f
                    )
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }

                drawPath(
                    path = shadowWave,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x000F2188), // Ambient clear blue transition
                            Color(0xCC0F2188), // Dense indigo highlight
                            Color(0xFA040621)  // Bottom-most velvet black
                        )
                    )
                )

                // 4. Fine diagonal light ray streak matching the silky gloss in the reference image
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0x00FFFFFF),
                            Color(0xB3FFFFFF), // Highly reflective sheen streak
                            Color(0x00FFFFFF)
                        )
                    ),
                    start = Offset(0f, h * 0.95f),
                    end = Offset(w, h * 0.22f),
                    strokeWidth = 3f
                )
            }
            .clickable {
                try {
                    context.startActivity(WidgetUtils.getClockIntent(context))
                } catch (e: Exception) {
                    Toast.makeText(context, "Opening clock app...", Toast.LENGTH_SHORT).show()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            // 1. Current Time (Centered Prominently on Top) using premium Liquid Glass 3D Extrusion
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.wrapContentSize()
            ) {
                // Layer A: Soft, realistic ambient drop shadow with a lens refraction glow
                Text(
                    text = timeText,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 82.sp,
                        letterSpacing = (-4).sp,
                        color = Color.Black.copy(alpha = 0.4f),
                        shadow = Shadow(
                            color = Color(0x7F0091FF), // Soft cobalt refraction lens glow
                            offset = Offset(0f, 8f),
                            blurRadius = 18f
                        )
                    ),
                    modifier = Modifier.offset(y = 2.dp)
                )

                // Layer B: Subtle light-scattering background refraction (inherited backdrop colors)
                Text(
                    text = timeText,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 82.sp,
                        letterSpacing = (-4).sp,
                        color = Color(0x3B4682FF) // Refracted wave blue light bleed
                    )
                )

                // Layer C: Core Frosted Liquid Glass face with high-precision, 6-stop realistic vertical light reflections
                Text(
                    text = timeText,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 82.sp,
                        letterSpacing = (-4).sp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xF2FFFFFF), // Top specular reflection rim
                                Color(0x36FFFFFF), // High-transparency clear cavity (21% white)
                                Color(0x1AFFFFFF), // Mid inner cavity transparency
                                Color(0x52FFFFFF), // Sub-surface horizontal light scatter highlight
                                Color(0x36FFFFFF), // Frosted refraction body
                                Color(0xA9FFFFFF)  // Intense frosted bottom reflection extrusion base
                            )
                        )
                    )
                )

                // Layer D: Sharp edge outline highlight representing the crisp outer curved bevel reflection
                Text(
                    text = timeText,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 82.sp,
                        letterSpacing = (-4).sp,
                        color = Color(0x40FFFFFF), // Fine bezel edge
                        drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1f
                        )
                    ),
                    modifier = Modifier.offset(x = (-0.5).dp, y = (-0.5).dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 2. Date displayed BENEATH the time matching native iOS lock screen typography
            Text(
                text = dateText,
                fontFamily = FontFamily.SansSerif,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = dateColor,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.35f),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 4f
                    )
                )
            )

            // 3. Alarm Information displayed BENEATH the date inside an exquisite glass capsule
            if (alarmTime.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = Color(0x24FFFFFF), // Frosted light glass body
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 0.8.dp,
                            color = Color(0x3DFFFFFF), // Exquisite edge highlights
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Alarm",
                        tint = alarmColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = alarmTime,
                        fontFamily = FontFamily.SansSerif,
                        color = alarmColor,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 0.1.sp
                        )
                    )
                }
            }
        }
    }
}



