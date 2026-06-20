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
import java.net.NetworkInterface
import java.net.InetAddress
import java.net.Inet4Address
import java.util.Collections
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class HotspotClient(
    val ip: String,
    val hostname: String?,
    val isReachable: Boolean,
    val nickname: String? = null,
    val mac: String? = null,
    val manufacturer: String? = null,
    val pingMs: Long? = null
)

val MAC_OUI_MAP = mapOf(
    "00:00:0c" to "Cisco",
    "00:03:7f" to "Atheros",
    "00:05:5d" to "D-Link",
    "00:0d:4b" to "Roku",
    "00:10:e0" to "Intel",
    "00:11:22" to "Sony",
    "00:14:22" to "Dell",
    "00:16:3e" to "Xen",
    "00:1c:c0" to "HP",
    "00:1e:c9" to "Dell",
    "00:21:70" to "Dell",
    "00:23:45" to "Apple",
    "00:25:00" to "Apple / Airport",
    "00:26:bb" to "Apple",
    "24:a0:74" to "Apple iDevice",
    "28:cf:da" to "Apple iPhone",
    "38:ca:da" to "Apple MacBook",
    "3c:15:c2" to "Apple iPad",
    "40:4d:7f" to "Apple",
    "48:d7:05" to "Apple",
    "50:bc:96" to "Apple",
    "54:26:96" to "Apple",
    "5c:ad:cf" to "Apple",
    "60:03:08" to "Apple",
    "64:20:0c" to "Apple",
    "64:b9:e8" to "Apple",
    "68:5b:35" to "Apple",
    "6c:40:08" to "Apple",
    "70:11:24" to "Apple",
    "70:56:81" to "Apple",
    "74:e1:b6" to "Apple",
    "78:31:c1" to "Apple",
    "7c:04:d0" to "Apple",
    "7c:c5:37" to "Apple",
    "80:49:71" to "Apple",
    "80:ea:96" to "Apple",
    "84:38:35" to "Sony",
    "84:8e:0c" to "Apple",
    "8c:2d:aa" to "Apple",
    "8c:fe:57" to "Apple",
    "90:27:e4" to "Apple",
    "90:72:40" to "Apple",
    "94:94:26" to "Apple",
    "9c:04:eb" to "Apple",
    "a4:77:33" to "Apple",
    "a8:5b:78" to "Apple",
    "a8:fa:d8" to "Apple",
    "b8:09:8a" to "Apple",
    "b8:17:c2" to "Apple",
    "b8:27:eb" to "Raspberry Pi",
    "b8:e8:56" to "Apple",
    "c0:1a:da" to "Apple",
    "c0:cc:f8" to "Apple",
    "c8:1e:e7" to "Apple",
    "c8:6e:31" to "Apple",
    "c8:85:50" to "Apple",
    "c8:b5:b7" to "Apple",
    "cc:25:ef" to "Apple",
    "cc:29:f5" to "Apple",
    "d0:03:4b" to "Apple",
    "d0:25:98" to "Apple",
    "d0:a6:37" to "Apple",
    "d4:dc:cd" to "Apple",
    "d8:1c:79" to "Apple",
    "d8:30:62" to "Apple",
    "d8:a2:5e" to "Apple",
    "d8:bb:2c" to "Apple",
    "dc:2b:61" to "Apple",
    "dc:41:5f" to "Apple",
    "e0:b9:ba" to "Apple",
    "e0:c9:7a" to "Apple",
    "e0:db:55" to "Apple",
    "e4:25:e9" to "Apple",
    "e4:e4:ab" to "Apple",
    "ec:2c:e2" to "Apple",
    "ec:35:86" to "Apple",
    "ec:ad:b8" to "Apple",
    "f0:18:98" to "Apple",
    "f0:79:60" to "Apple",
    "f0:99:bf" to "Apple",
    "f0:c1:f1" to "Apple",
    "f4:0f:24" to "Apple",
    "f4:1b:a1" to "Apple",
    "f4:37:b7" to "Apple",
    "f4:f9:51" to "Apple",
    "fc:fc:48" to "Apple",
    "00:1a:11" to "Google",
    "3c:5a:37" to "Google Pixel",
    "3c:5c:c4" to "Google",
    "d8:eb:97" to "Google",
    "f4:f5:d8" to "Google Nest",
    "ec:1a:59" to "Samsung",
    "1c:5a:3e" to "Samsung",
    "dc:e5:35" to "Samsung",
    "d0:37:42" to "Samsung",
    "38:2d:c8" to "Samsung Galaxy",
    "cc:3a:61" to "Samsung",
    "00:23:76" to "HTC",
    "00:0e:35" to "Intel",
    "00:1b:21" to "Intel",
    "00:21:5a" to "Intel Desktop/Laptop",
    "10:4a:7d" to "Intel",
    "a4:17:31" to "Intel",
    "2c:56:dc" to "Intel Centrino",
    "e4:a7:a0" to "Intel",
    "fc:77:74" to "Intel WiFi Card",
    "e8:4e:06" to "Intel",
    "30:52:cb" to "Intel",
    "e0:d5:5e" to "Intel",
    "14:91:82" to "Intel Wireless",
    "4c:34:88" to "Intel",
    "70:cd:0d" to "Intel",
    "7c:d3:0a" to "Intel",
    "80:c5:f2" to "Intel Core",
    "a0:c5:89" to "Intel",
    "00:14:d1" to "TRENDnet",
    "00:1d:73" to "Arista",
    "34:57:60" to "Xiaomi",
    "60:eed:f2" to "Xiaomi",
    "9c:99:a0" to "Xiaomi Redmi",
    "ac:c1:ee" to "Xiaomi Poco",
    "d4:97:0b" to "Xiaomi",
    "fc:64:3a" to "Xiaomi Smart Home",
    "24:df:6a" to "Xiaomi",
    "64:9a:12" to "Xiaomi",
    "b0:3c:e5" to "Xiaomi",
    "c0:2a:4d" to "Xiaomi",
    "d8:15:0d" to "Xiaomi",
    "e4:46:da" to "Xiaomi",
    "00:1e:e5" to "Espressif IoT",
    "24:0a:c4" to "Espressif ESP32",
    "30:ae:a4" to "Espressif ESP8266",
    "4c:11:ae" to "Espressif Systems",
    "54:5a:a6" to "Espressif",
    "68:c6:3a" to "Espressif",
    "70:03:9f" to "Espressif",
    "74:ec:b2" to "Espressif Systems",
    "80:7d:3a" to "Espressif",
    "84:0d:8e" to "Espressif",
    "84:f3:eb" to "Espressif Systems",
    "90:97:d5" to "Espressif ESP32-S3",
    "a0:20:a6" to "Espressif",
    "a4:7b:2c" to "Espressif Systems",
    "ac:0b:fb" to "Espressif Systems",
    "ac:d0:74" to "Espressif",
    "b4:e6:2d" to "Espressif",
    "c4:4f:33" to "Espressif Systems",
    "c8:2b:96" to "Espressif",
    "d8:a0:1d" to "Espressif Systems",
    "e0:5a:1b" to "Espressif Systems",
    "e8:68:e7" to "Espressif IoT"
)

fun lookupManufacturer(mac: String?): String {
    if (mac.isNullOrEmpty() || mac == "00:00:00:00:00:00") return "Unknown Vendor"
    val cleanMac = mac.replace(":", "").replace("-", "").lowercase()
    if (cleanMac.length >= 6) {
        val prefix = cleanMac.substring(0, 6)
        val formattedPrefix = "${prefix.substring(0, 2)}:${prefix.substring(2, 4)}:${prefix.substring(4, 6)}"
        return MAC_OUI_MAP[formattedPrefix] ?: "Generic Client"
    }
    return "Generic Client"
}

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

fun getActiveSubnets(): List<String> {
    val subnets = mutableListOf<String>()
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        if (interfaces != null) {
            for (netInterface in Collections.list(interfaces)) {
                if (netInterface.isLoopback) continue
                val addresses = netInterface.inetAddresses ?: continue
                for (address in Collections.list(addresses)) {
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        val ipStr = address.hostAddress ?: continue
                        if (ipStr.startsWith("192.168.") || ipStr.startsWith("10.") || ipStr.startsWith("172.")) {
                            val lastDot = ipStr.lastIndexOf('.')
                            if (lastDot > 0) {
                                subnets.add(ipStr.substring(0, lastDot + 1))
                            }
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    val list = subnets.distinct()
    return if (list.isEmpty()) listOf("192.168.43.") else list
}

fun ipToNumericValue(ip: String): Long {
    return try {
        val parts = ip.split(".")
        if (parts.size == 4) {
            (parts[0].toLong() shl 24) + (parts[1].toLong() shl 16) + (parts[2].toLong() shl 8) + parts[3].toLong()
        } else {
            0L
        }
    } catch (e: Exception) {
        0L
    }
}

fun getPingLatency(ip: String): Long? {
    try {
        val startTime = System.currentTimeMillis()
        val process = Runtime.getRuntime().exec(arrayOf("ping", "-c", "1", "-w", "1", ip))
        val exitVal = process.waitFor()
        if (exitVal == 0) {
            val endTime = System.currentTimeMillis()
            return (endTime - startTime).coerceAtLeast(1)
        }
    } catch (e: Exception) {}
    return null
}

fun parseSystemHotspotDevices(sharedPrefs: android.content.SharedPreferences): List<HotspotClient> {
    val deviceList = mutableMapOf<String, HotspotClient>()
    
    // Method A: Parse '/proc/net/arp'
    try {
        val arpReader = java.io.BufferedReader(java.io.FileReader("/proc/net/arp"))
        arpReader.use { reader ->
            var line: String? = reader.readLine()
            while (line != null) {
                // Ignore headers
                if (!line.contains("IP address") && !line.contains("IP")) {
                    val parts = line.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                    if (parts.size >= 4) {
                        val ip = parts[0]
                        val flags = parts[2]
                        val mac = parts[3]
                        
                        if (ip.matches(Regex("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"))) {
                            val cleanMac = if (mac != "00:00:00:00:00:00") mac else null
                            val manufacturer = if (cleanMac != null) lookupManufacturer(cleanMac) else "Connected Guest"
                            val savedNickname = sharedPrefs.getString("device_nickname_$ip", null)
                            
                            deviceList[ip] = HotspotClient(
                                ip = ip,
                                hostname = null,
                                isReachable = flags != "0x0",
                                nickname = savedNickname,
                                mac = cleanMac,
                                manufacturer = manufacturer,
                                pingMs = null
                            )
                        }
                    }
                }
                line = reader.readLine()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // Method B: Parse 'ip neighbor show'
    try {
        val process = Runtime.getRuntime().exec("ip neighbor show")
        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val parts = line.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                if (parts.isNotEmpty()) {
                    val ip = parts[0]
                    if (ip.matches(Regex("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) && !line.contains("FAILED")) {
                        var mac: String? = null
                        val lladdrIdx = parts.indexOf("lladdr")
                        if (lladdrIdx != -1 && lladdrIdx + 1 < parts.size) {
                            mac = parts[lladdrIdx + 1]
                        }
                        
                        val isReachable = line.contains("REACHABLE") || line.contains("DELAY") || line.contains("STALE")
                        val cleanMac = if (mac != "00:00:00:00:00:00") mac else null
                        val manufacturer = if (cleanMac != null) lookupManufacturer(cleanMac) else "Connected Guest"
                        val savedNickname = sharedPrefs.getString("device_nickname_$ip", null)
                        
                        val existing = deviceList[ip]
                        if (existing == null) {
                            deviceList[ip] = HotspotClient(
                                ip = ip,
                                hostname = null,
                                isReachable = isReachable,
                                nickname = savedNickname,
                                mac = cleanMac,
                                manufacturer = manufacturer,
                                pingMs = null
                            )
                        } else {
                            deviceList[ip] = existing.copy(
                                mac = existing.mac ?: cleanMac,
                                manufacturer = existing.manufacturer ?: manufacturer,
                                isReachable = existing.isReachable || isReachable
                            )
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    return deviceList.values.toList()
}

suspend fun scanHotspotSubnet(
    subnetPrefix: String,
    onProgress: (Int, Int) -> Unit,
    sharedPrefs: android.content.SharedPreferences
): List<HotspotClient> = withContext(Dispatchers.IO) {
    // 1. Fetch instantly known system neighbor clients
    val systemFound = parseSystemHotspotDevices(sharedPrefs)
    val systemFoundMap = systemFound.associateBy { it.ip }

    val totalIps = 253 // .2 to .254
    var completedCount = 0
    val progressMutex = Any()
    val semaphore = Semaphore(60) // 60 parallel tasks for rapid resolution

    val jobs = (2..254).map { lastOctet ->
        async {
            semaphore.withPermit {
                val ip = subnetPrefix + lastOctet
                val knownExisting = systemFoundMap[ip]
                var isAlive = knownExisting != null
                var hostname: String? = null
                val mac: String? = knownExisting?.mac
                var manufacturer: String? = knownExisting?.manufacturer
                var measuredPing: Long? = null

                if (isAlive) {
                    measuredPing = getPingLatency(ip)
                }

                if (!isAlive) {
                    try {
                        // Method 1: Quick native command line ping
                        val process = Runtime.getRuntime().exec(arrayOf("ping", "-c", "1", "-w", "1", ip))
                        val exitVal = process.waitFor()
                        if (exitVal == 0) {
                            isAlive = true
                        }
                    } catch (e: Exception) {}

                    if (!isAlive) {
                        try {
                            // Method 2: Standard isReachable
                            val address = InetAddress.getByName(ip)
                            isAlive = address.isReachable(120)
                        } catch (e: Exception) {}
                    }

                    if (!isAlive) {
                        // Method 3: Port Probing for silent-firewall devices (e.g. Windows/Mac laptops)
                        val portsToProbe = intArrayOf(5353, 135, 445, 139, 80, 443, 22, 62078)
                        for (port in portsToProbe) {
                            var socket: java.net.Socket? = null
                            try {
                                socket = java.net.Socket()
                                socket.connect(java.net.InetSocketAddress(ip, port), 100)
                                isAlive = true
                                break
                            } catch (e: java.net.ConnectException) {
                                isAlive = true
                                break
                            } catch (e: Exception) {
                                val msg = e.message ?: ""
                                if (msg.contains("refused", ignoreCase = true) || msg.contains("reset", ignoreCase = true)) {
                                    isAlive = true
                                    break
                                }
                            } finally {
                                try { socket?.close() } catch (ex: Exception) {}
                            }
                        }
                    }

                    if (isAlive) {
                        measuredPing = getPingLatency(ip)
                    }
                }

                if (isAlive) {
                    try {
                        val address = InetAddress.getByName(ip)
                        val resolvedHost = address.hostName
                        val canonical = address.canonicalHostName
                        if (!resolvedHost.isNullOrEmpty() && resolvedHost != ip) {
                            hostname = resolvedHost
                        } else if (!canonical.isNullOrEmpty() && canonical != ip) {
                            hostname = canonical
                        }
                    } catch (e: Exception) {}
                }

                synchronized(progressMutex) {
                    completedCount++
                    onProgress(completedCount, totalIps)
                }

                if (isAlive) {
                    val savedNickname = sharedPrefs.getString("device_nickname_$ip", null)
                    HotspotClient(
                        ip = ip,
                        hostname = hostname,
                        isReachable = true,
                        nickname = savedNickname,
                        mac = mac,
                        manufacturer = manufacturer ?: lookupManufacturer(mac),
                        pingMs = measuredPing
                    )
                } else {
                    null
                }
            }
        }
    }

    val activeScanned = jobs.awaitAll().filterNotNull()
    val combinedMap = mutableMapOf<String, HotspotClient>()
    
    // Seed with our system neighbor scan first
    systemFound.forEach { client ->
        combinedMap[client.ip] = client
    }
    
    // Overlay scanning results (adds hostname + ping ms)
    activeScanned.forEach { client ->
        val existing = combinedMap[client.ip]
        if (existing == null) {
            combinedMap[client.ip] = client
        } else {
            combinedMap[client.ip] = existing.copy(
                hostname = client.hostname ?: existing.hostname,
                isReachable = true,
                pingMs = client.pingMs ?: existing.pingMs
            )
        }
    }

    val results = combinedMap.values.toList().sortedBy { ipToNumericValue(it.ip) }
    results
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
    
    // Hotspot Clients states
    val coroutineScope = rememberCoroutineScope()
    var isScanningHotspot by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(0f) }
    val initialClients = remember(sharedPrefs) { parseSystemHotspotDevices(sharedPrefs) }
    var detectedClients by remember { mutableStateOf<List<HotspotClient>>(initialClients) }
    var selectedSubnetPrefix by remember { mutableStateOf("") }
    var hasScannedInitially by remember { mutableStateOf(initialClients.isNotEmpty()) }
    val detectedSubnets = remember(context) { getActiveSubnets() }
    
    if (selectedSubnetPrefix.isEmpty() && detectedSubnets.isNotEmpty()) {
        selectedSubnetPrefix = detectedSubnets.first()
    }
    
    // Nickname dialog state
    var editingClientForNickname by remember { mutableStateOf<HotspotClient?>(null) }
    var pendingNicknameInput by remember { mutableStateOf("") }

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
                
                // Live refresh connected devices on screen focus
                val res = parseSystemHotspotDevices(sharedPrefs)
                if (res.isNotEmpty() || !hasScannedInitially) {
                    detectedClients = res
                    hasScannedInitially = true
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

        // ================= SECTION 6: HOTSPOT CONNECTED DEVICES SCANNER =================
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Hotspot Clients",
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
                        .padding(16.dp)
                ) {
                    // Header Row with Router/Tethering representation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hotspot Client Monitor",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "See who is connected to your portable Pixel hotspot",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Subnet selector chip list
                    if (detectedSubnets.size > 1) {
                        Text(
                            text = "Choose subnet interface to scan:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            detectedSubnets.forEach { prefix ->
                                val isSelected = selectedSubnetPrefix == prefix
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .clickable {
                                            if (!isScanningHotspot) {
                                                selectedSubnetPrefix = prefix
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${prefix}x",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    } else {
                        // Single subnet info label
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Active adapter subnet range: ${selectedSubnetPrefix}x",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Progress indicators if scanning
                    if (isScanningHotspot) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Scanning network clients...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${String.format("%.0f%%", scanProgress * 100)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { scanProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Scan actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isScanningHotspot) {
                                    isScanningHotspot = false
                                } else {
                                    isScanningHotspot = true
                                    scanProgress = 0f
                                    coroutineScope.launch {
                                        try {
                                            val results = scanHotspotSubnet(
                                                selectedSubnetPrefix,
                                                onProgress = { current, total ->
                                                    scanProgress = current.toFloat() / total
                                                },
                                                sharedPrefs
                                            )
                                            if (isScanningHotspot) {
                                                detectedClients = results
                                                hasScannedInitially = true
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        } finally {
                                            isScanningHotspot = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScanningHotspot) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                contentColor = if (isScanningHotspot) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = if (isScanningHotspot) Icons.Default.Close else Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isScanningHotspot) "Stop Scan" else "Scan Connections",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    // Scan Results area
                    if (hasScannedInitially || detectedClients.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Detected Hosts (${detectedClients.size})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        if (detectedClients.isEmpty()) {
                            // Empty States
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WifiOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "No active clients found on this range",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Double check that your device's hotspot is toggled on, and a guest device is connected, then try scanning again.",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        } else {
                            // Active list of clients
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                detectedClients.forEach { client ->
                                    val ipLastOctet = client.ip.substringAfterLast(".").toIntOrNull() ?: 0
                                    val avatarColors = listOf(
                                        Color(0xFFE57373), Color(0xFFF06292), Color(0xFFBA68C8),
                                        Color(0xFF9575CD), Color(0xFF7986CB), Color(0xFF64B5F6),
                                        Color(0xFF4FC3F7), Color(0xFF4DD0E1), Color(0xFF4DB6AC),
                                        Color(0xFF81C784), Color(0xFFAED581), Color(0xFFFFB74D)
                                    )
                                    val avatarBgColor = avatarColors[ipLastOctet % avatarColors.size]

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left: IP Last Octet Avatar with a dynamic status pulse indicator
                                        Box(
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(avatarBgColor),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = client.nickname?.take(1)?.uppercase() ?: (client.hostname?.take(1)?.uppercase() ?: ipLastOctet.toString()),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            }
                                            
                                            // Status pulsing green dot on bottom-right corner of avatar to show active connection
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .align(Alignment.BottomEnd)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF4CAF50))
                                                    .border(2.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        // Center: IP, resolved details, and copy handler
                                        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    try {
                                                        clipboardManager.setText(androidx.compose.ui.text.buildAnnotatedString { append(client.ip) })
                                                        Toast.makeText(context, "Copied IP: ${client.ip}", Toast.LENGTH_SHORT).show()
                                                    } catch (e: Exception) {}
                                                }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = client.nickname ?: client.hostname ?: "Guest Client",
                                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                // Optional manufacturer indicator
                                                val mfg = client.manufacturer ?: "Generic"
                                                if (mfg != "Generic Client" && mfg != "Generic") {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = mfg,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(2.dp))
                                            
                                            Text(
                                                text = "IP: ${client.ip} (Tap to copy)" + if (client.hostname != null && client.nickname != null) " • ${client.hostname}" else "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            
                                            if (!client.mac.isNullOrEmpty()) {
                                                Text(
                                                    text = "MAC: ${client.mac.uppercase()}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                                )
                                            }
                                        }

                                        // Latency visualizer & Nickname Edit button
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (client.pingMs != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "⚡ ${client.pingMs} ms",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            }
                                            
                                            IconButton(
                                                onClick = {
                                                    editingClientForNickname = client
                                                    pendingNicknameInput = client.nickname ?: ""
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Nickname",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialog for editing device nicknames
        editingClientForNickname?.let { client ->
            AlertDialog(
                onDismissRequest = { editingClientForNickname = null },
                title = { Text("Set Device Nickname") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Assign a friendly name for ${client.ip} to recognize it on future scans.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = pendingNicknameInput,
                            onValueChange = { pendingNicknameInput = it },
                            label = { Text("Device Nickname") },
                            placeholder = { Text("e.g. My Laptop, Dad's Phone") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val ip = client.ip
                            sharedPrefs.edit().putString("device_nickname_$ip", pendingNicknameInput.trim()).apply()
                            
                            // Instantly update current view's list
                            detectedClients = detectedClients.map {
                                if (it.ip == ip) {
                                    it.copy(nickname = pendingNicknameInput.trim().takeIf { s -> s.isNotEmpty() })
                                } else {
                                    it
                                }
                            }
                            editingClientForNickname = null
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingClientForNickname = null }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
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



