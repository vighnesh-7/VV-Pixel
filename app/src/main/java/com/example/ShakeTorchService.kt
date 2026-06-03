package com.example

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.concurrent.atomic.AtomicBoolean

class ShakeTorchService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null

    // Torch State
    private var isTorchOn = false

    // Registration flag to prevent multiple listener cycles
    private var isSensorRegistered = false

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Screen State Broadcast: ${intent?.action}")
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> registerSensor()
                Intent.ACTION_SCREEN_OFF -> unregisterSensor()
            }
        }
    }

    private fun registerSensor() {
        if (!isSensorRegistered) {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
            isSensorRegistered = true
            Log.d(TAG, "Battery-optimized listener REGISTERED (Screen ON)")
        }
    }

    private fun unregisterSensor() {
        if (isSensorRegistered) {
            sensorManager.unregisterListener(this)
            isSensorRegistered = false
            shakeState = 0
            Log.d(TAG, "Battery-optimized listener UNREGISTERED (Screen OFF)")
        }
    }

    // Shake State Machine for Lateral Oscillation (sidewise Right -> Left -> Right)
    // Thresholds
    private fun getShakeThreshold(): Float {
        val prefs = getSharedPreferences("com.example.vvpixel.SETTINGS", Context.MODE_PRIVATE)
        return prefs.getFloat("shake_torch_threshold", 12.0f)
    }
    private val WINDOW_SIZE = 450L        // Match window between stages in ms
    private val COOLDOWN_TIME = 1000L      // Cooldown after trigger in ms

    private var shakeState = 0
    private var lastStateTime = 0L
    private var lastTriggerTime = 0L

    private var torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            super.onTorchModeChanged(id, enabled)
            if (id == cameraId) {
                isTorchOn = enabled
                Log.d(TAG, "System Torch State Updated: $isTorchOn")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ShakeTorchService Created")
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager.cameraIdList.getOrNull(0)

            cameraId?.let {
                cameraManager.registerTorchCallback(torchCallback, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize CameraManager flashlight", e)
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Torch shake gesture listener running"))
        isServiceRunning = true

        // Register screen state receiver for dynamic sensor management
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenStateReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(screenStateReceiver, filter)
        }

        registerSensor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "ShakeTorchService Started running")
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ShakeTorchService Destroyed")
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering screen receiver", e)
        }
        unregisterSensor()
        try {
            cameraId?.let {
                cameraManager.unregisterTorchCallback(torchCallback)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up torch callback", e)
        }
        isServiceRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val currentTime = System.currentTimeMillis()

        // Ignore sensor events within the cooldown period
        if (currentTime - lastTriggerTime < COOLDOWN_TIME) {
            return
        }

        // Accelerometer Lateral Shake Detection (unchanged & highly optimized)
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0] // Lateral acceleration (Left-Right)
            val absX = kotlin.math.abs(x)
            val currentThreshold = getShakeThreshold()

            // Safe battery optimization: if resting and force is below threshold, exit immediately
            if (shakeState == 0 && absX < currentThreshold) {
                return
            }

            // State Machine to find 3-step sidewise oscillation: Right -> Left -> Right
            // Phase 1: High acceleration to the Side (+X or -X)
            // Let's analyze X axis.
            when (shakeState) {
                0 -> {
                    // Look for positive rightward swing
                    if (x > currentThreshold) {
                        shakeState = 1
                        lastStateTime = currentTime
                        Log.d(TAG, "Shake Phase 1: Swing Right detected (Accel: $x)")
                    } else if (x < -currentThreshold) {
                        // Also accept starting swing left
                        shakeState = 10
                        lastStateTime = currentTime
                        Log.d(TAG, "Shake Phase 1: Swing Left detected (Accel: $x)")
                    }
                }
                1 -> {
                    // State 1 was Swing Right. Next step must be Swing Left within time.
                    if (currentTime - lastStateTime > WINDOW_SIZE) {
                        // Reset to idle on timeout
                        shakeState = 0
                    } else if (x < -currentThreshold) {
                        shakeState = 2
                        lastStateTime = currentTime
                        Log.d(TAG, "Shake Phase 2: Swing Left detected (Accel: $x)")
                    }
                }
                2 -> {
                    // State 2 was Swing Left. Final step must be Swing Right within time.
                    if (currentTime - lastStateTime > WINDOW_SIZE) {
                        shakeState = 0
                    } else if (x > currentThreshold) {
                        Log.d(TAG, "Shake Phase 3: Swing Right detected (Accel: $x) -> Triggering Torch!")
                        triggerTorchToggle()
                        shakeState = 0
                        lastTriggerTime = currentTime
                    }
                }
                10 -> {
                    // State 10 was Swing Left. Next step must be Swing Right within time.
                    if (currentTime - lastStateTime > WINDOW_SIZE) {
                        shakeState = 0
                    } else if (x > currentThreshold) {
                        shakeState = 11
                        lastStateTime = currentTime
                        Log.d(TAG, "Shake Phase 2: Swing Right detected (Accel: $x)")
                    }
                }
                11 -> {
                    // State 11 was Swing Right. Final step must be Swing Left within time.
                    if (currentTime - lastStateTime > WINDOW_SIZE) {
                        shakeState = 0
                    } else if (x < -currentThreshold) {
                        Log.d(TAG, "Shake Phase 3: Swing Left detected (Accel: $x) -> Triggering Torch!")
                        triggerTorchToggle()
                        shakeState = 0
                        lastTriggerTime = currentTime
                    }
                }
            }
        }
    }

    private fun triggerTorchToggle() {
        val id = cameraId
        if (id != null) {
            val prefs = getSharedPreferences("com.example.vvpixel.SETTINGS", Context.MODE_PRIVATE)
            val isShakeTorchEnabled = prefs.getBoolean("shake_torch_enabled", true)
            if (!isShakeTorchEnabled) {
                Log.d(TAG, "Shake to Torch is disabled in settings. Skipping toggle.")
                return
            }

            try {
                val nextState = !isTorchOn
                cameraManager.setTorchMode(id, nextState)
                isTorchOn = nextState
                vibrateFeedback()
                Log.d(TAG, "Torch state successfully toggled to: $isTorchOn")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle torch mode", e)
            }
        }
    }

    private fun vibrateFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(80)
            }
        } catch (e: Exception) {
            // Ignore if vibration fails
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Shake Torch Service Channel",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_SECRET
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(content: String): Notification {
        val stopIntent = Intent(this, ShakeTorchService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VV Pixel — Shake to Torch")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_compass) // use system compass icon as placeholder
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disable Service", stopPendingIntent)
            .build()
    }

    companion object {
        private const val TAG = "ShakeTorchService"
        private const val CHANNEL_ID = "ShakeTorchServiceChannel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_STOP = "com.example.vvpixel.ACTION_STOP"
        var isServiceRunning = false
    }
}
