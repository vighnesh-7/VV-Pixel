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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.MyApplicationTheme
import kotlin.math.abs

class MainActivity : ComponentActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    // State flow for dashboard real-time shake preview
    private val sensorX = mutableFloatStateOf(0f)
    private val sensorY = mutableFloatStateOf(0f)
    private val sensorZ = mutableFloatStateOf(0f)

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
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
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
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
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

    // Launcher for Notification permission (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

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
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
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

            // Double Tap to Lock
            PixelEnhancerCard(
                title = "Double-Tap to Lock",
                subtitle = if (isAccessibilityRunning) {
                    if (isDoubleTapEnabled) "Active — double-tap empty areas to lock" else "Disabled — double-tap empty areas to lock"
                } else {
                    "Inactive — tap to open Accessibility setup"
                },
                icon = Icons.Default.Lock,
                rightElement = {
                    Switch(
                        checked = isDoubleTapEnabled && isAccessibilityRunning,
                        onCheckedChange = { enable ->
                            if (!isAccessibilityRunning) {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                                Toast.makeText(context, "Please enable the VV Pixel Lock Helper service", Toast.LENGTH_LONG).show()
                            } else {
                                isDoubleTapEnabled = enable
                                sharedPrefs.edit().putBoolean("double_tap_to_lock_enabled", enable).apply()
                            }
                        }
                    )
                }
            )

            // Subtle Vibration Feedback Toggle
            PixelEnhancerCard(
                title = "Subtle Gesture/Widget Vibration",
                subtitle = "Vibrate mildly when lock triggers are fired",
                icon = Icons.Default.Notifications,
                rightElement = {
                    Switch(
                        checked = isLockVibrationEnabled,
                        onCheckedChange = { enable ->
                            isLockVibrationEnabled = enable
                            sharedPrefs.edit().putBoolean("lock_vibration_enabled", enable).apply()
                        }
                    )
                }
            )

            // Lock Widget 1x1
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
        }

        // ================= SECTION 2: QUICK SETTINGS =================
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Quick Settings",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            // Volume QS Tile
            PixelEnhancerCard(
                title = "Volume QS Tile",
                subtitle = "Active — tap to test volume slider board",
                icon = Icons.Default.Notifications,
                rightElement = {
                    Switch(
                        checked = hasOverlayPermission,
                        onCheckedChange = { enable ->
                            if (!hasOverlayPermission) {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                                Toast.makeText(context, "Overlay Permission required first", Toast.LENGTH_SHORT).show()
                            } else {
                                val intent = Intent(context, VolumeControlActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                }
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            )
        }

        // ================= SECTION 3: WIDGETS =================
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Widgets",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
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

            Spacer(modifier = Modifier.height(4.dp))

            // Ringer Toggle Widget
            PixelEnhancerCard(
                title = "Unmute/Mute/Vibrate Widget",
                subtitle = "One-tap homescreen sound state switcher",
                iconPainter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_volume_equalizer),
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

            // Shake to Toggle Torch Card
            PixelEnhancerCard(
                title = "Shake to Toggle Torch",
                subtitle = if (isShakeTorchRunning && isShakeTorchEnabled) "Active — shake to toggle" else "Disabled — shake to toggle",
                icon = Icons.Default.Refresh,
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
                                    .background(if (abs(sensorX) > 12.0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Current: ${String.format("%.2f m/s²", sensorX)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                            Text(
                                text = "Trigger Target: 12.00 m/s²",
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
                            title = "System Brightness Write",
                            description = "Required to adjust brightness values.",
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

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            PermissionItem(
                                title = "Post Notifications",
                                description = "Required for single-swipe brightness overlay.",
                                isGranted = hasNotificationPermission,
                                onRequestGrant = {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
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
            Text(if (isGranted) "Granted" else "Grant", style = MaterialTheme.typography.labelSmall)
        }
    }
}
