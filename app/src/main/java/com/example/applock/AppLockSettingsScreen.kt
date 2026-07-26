package com.example.applock

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.security.MessageDigest

data class AppEntry(
    val packageName: String,
    val label: String,
    val iconBitmap: ImageBitmap? = null
)

@Composable
fun AppLockSettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("com.example.vvpixel.SETTINGS", Context.MODE_PRIVATE) }

    var isEnabled by remember { mutableStateOf(prefs.getBoolean("app_lock_master_enabled", false)) }
    var reminderText by remember { mutableStateOf(prefs.getString("app_lock_reminder_text", "") ?: "") }
    var showPatternSetup by remember { mutableStateOf(false) }
    var showPatternConfirmDisable by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var lockedCount by remember {
        mutableStateOf(prefs.getStringSet("app_lock_locked_packages", emptySet())?.size ?: 0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "App Lock Gate",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Master Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Enable App Lock", fontWeight = FontWeight.Bold)
                        Text("Intercept and lock selected applications", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        if (!checked) {
                            val storedHash = prefs.getString("app_lock_pattern_hash", null)
                            authenticateToDisableAppLock(
                                context = context,
                                onSuccess = {
                                    isEnabled = false
                                    prefs.edit().putBoolean("app_lock_master_enabled", false).apply()
                                    Toast.makeText(context, "App Lock disabled", Toast.LENGTH_SHORT).show()
                                },
                                onFailed = {
                                    if (!storedHash.isNullOrEmpty()) {
                                        showPatternConfirmDisable = true
                                    } else {
                                        isEnabled = false
                                        prefs.edit().putBoolean("app_lock_master_enabled", false).apply()
                                        Toast.makeText(context, "App Lock disabled", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        } else {
                            isEnabled = true
                            prefs.edit().putBoolean("app_lock_master_enabled", true).apply()
                        }
                    }
                )
            }
        }

        AnimatedVisibility(visible = isEnabled) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))

                // Reminder Text Input
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Reminder Quote", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = reminderText,
                            onValueChange = {
                                reminderText = it
                                prefs.edit().putString("app_lock_reminder_text", it).apply()
                            },
                            placeholder = { Text("e.g., Stay focused! Finish your work first.") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        androidx.compose.material3.Button(
                            onClick = {
                                prefs.edit().putString("app_lock_reminder_text", reminderText).apply()
                                android.widget.Toast.makeText(context, "Reminder quote saved!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Quote")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pattern Setup
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPatternSetup = true }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Password, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (prefs.getString("app_lock_pattern_hash", null) == null) "Set Pattern Lock" else "Change Pattern Lock",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("Draw 3x3 pattern password", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Select Apps
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAppPicker = true }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Select Locked Apps", fontWeight = FontWeight.SemiBold)
                            Text("$lockedCount apps currently locked", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    if (showPatternSetup) {
        PatternSetupDialog(
            onDismiss = { showPatternSetup = false },
            onPatternSet = { pattern ->
                val hash = hashPattern(pattern)
                prefs.edit().putString("app_lock_pattern_hash", hash).apply()
                showPatternSetup = false
            }
        )
    }

    if (showAppPicker) {
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            onAppsSelected = { packages ->
                prefs.edit().putStringSet("app_lock_locked_packages", packages).apply()
                lockedCount = packages.size
                showAppPicker = false
            }
        )
    }

    if (showPatternConfirmDisable) {
        val storedHash = prefs.getString("app_lock_pattern_hash", "") ?: ""
        PatternConfirmDialog(
            storedHash = storedHash,
            onDismiss = { showPatternConfirmDisable = false },
            onConfirmed = {
                showPatternConfirmDisable = false
                isEnabled = false
                prefs.edit().putBoolean("app_lock_master_enabled", false).apply()
                Toast.makeText(context, "App Lock disabled", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun PatternConfirmDialog(
    storedHash: String,
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit
) {
    var currentPattern by remember { mutableStateOf(listOf<Int>()) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isError) errorMessage else "Confirm Pattern to Disable App Lock")
        },
        text = {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .padding(8.dp)
            ) {
                PatternLockGrid(
                    modifier = Modifier.fillMaxSize(),
                    isError = isError,
                    isSuccess = false,
                    onPatternStart = {
                        currentPattern = emptyList()
                        isError = false
                        errorMessage = ""
                    },
                    onDotSelected = { idx ->
                        if (idx !in currentPattern) currentPattern = currentPattern + idx
                    },
                    onPatternCompleted = {
                        if (currentPattern.size >= 4) {
                            val hash = hashPattern(currentPattern)
                            if (hash == storedHash) {
                                onConfirmed()
                            } else {
                                isError = true
                                errorMessage = "Incorrect pattern. Try again."
                            }
                        } else {
                            isError = true
                            errorMessage = "Minimum 4 dots required"
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PatternSetupDialog(
    onDismiss: () -> Unit,
    onPatternSet: (List<Int>) -> Unit
) {
    var step by remember { mutableStateOf(0) }
    var firstPattern by remember { mutableStateOf(listOf<Int>()) }
    var currentPattern by remember { mutableStateOf(listOf<Int>()) }
    var isMismatch by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (step == 0) "Draw 3x3 Pattern" else if (isMismatch) "Patterns Mismatch - Retry" else "Confirm Pattern")
        },
        text = {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .padding(8.dp)
            ) {
                PatternLockGrid(
                    modifier = Modifier.fillMaxSize(),
                    isError = isMismatch,
                    isSuccess = false,
                    onPatternStart = {
                        currentPattern = emptyList()
                        isMismatch = false
                    },
                    onDotSelected = { idx ->
                        if (idx !in currentPattern) currentPattern = currentPattern + idx
                    },
                    onPatternCompleted = {
                        if (currentPattern.size >= 4) {
                            if (step == 0) {
                                firstPattern = currentPattern
                                step = 1
                            } else {
                                if (currentPattern == firstPattern) {
                                    onPatternSet(currentPattern)
                                } else {
                                    isMismatch = true
                                    step = 0
                                }
                            }
                        } else {
                            isMismatch = true
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AppPickerDialog(
    onDismiss: () -> Unit,
    onAppsSelected: (Set<String>) -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val prefs = remember { context.getSharedPreferences("com.example.vvpixel.SETTINGS", Context.MODE_PRIVATE) }
    val initialSelection = remember {
        prefs.getStringSet("app_lock_locked_packages", emptySet()) ?: emptySet()
    }
    val selected = remember { mutableStateListOf<String>().apply { addAll(initialSelection) } }
    var searchQuery by remember { mutableStateOf("") }

    val appEntries = remember {
        val entryMap = mutableMapOf<String, AppEntry>()

        // 1. Query all launcher apps (app drawer apps)
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveList = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }

            for (ri in resolveList) {
                val pkg = ri.activityInfo.packageName
                val label = ri.loadLabel(pm).toString()
                val bitmap = try {
                    ri.loadIcon(pm)?.toBitmap(100, 100)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
                entryMap[pkg] = AppEntry(pkg, label, bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Query all installed packages as fallback or non-launcher user apps
        try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in installedApps) {
                val pkg = app.packageName
                if (!entryMap.containsKey(pkg)) {
                    val launchIntent = pm.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        val label = pm.getApplicationLabel(app).toString()
                        val bitmap = try {
                            app.loadIcon(pm)?.toBitmap(100, 100)?.asImageBitmap()
                        } catch (e: Exception) {
                            null
                        }
                        entryMap[pkg] = AppEntry(pkg, label, bitmap)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        entryMap.values.sortedBy { it.label.lowercase() }
    }

    val filteredApps = remember(appEntries, searchQuery, selected.size) {
        val sortedList = appEntries.sortedWith(
            compareByDescending<AppEntry> { it.packageName in selected }
                .thenBy { it.label.lowercase() }
        )
        if (searchQuery.isBlank()) {
            sortedList
        } else {
            sortedList.filter {
                it.label.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Apps to Lock [${selected.size}]",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No applications found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 520.dp)) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            val isSelected = app.packageName in selected

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSelected) selected.remove(app.packageName) else selected.add(app.packageName)
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (app.iconBitmap != null) {
                                    Image(
                                        bitmap = app.iconBitmap,
                                        contentDescription = app.label,
                                        modifier = Modifier.size(38.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Apps,
                                        contentDescription = null,
                                        modifier = Modifier.size(38.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        if (isSelected) selected.remove(app.packageName) else selected.add(app.packageName)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAppsSelected(selected.toSet()) }) {
                Text("Save (${selected.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun hashPattern(pattern: List<Int>): String {
    val input = pattern.joinToString(",")
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

private fun Context.findActivity(): android.app.Activity? {
    var currentContext = this
    while (currentContext is android.content.ContextWrapper) {
        if (currentContext is android.app.Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

private fun authenticateToDisableAppLock(
    context: Context,
    onSuccess: () -> Unit,
    onFailed: () -> Unit
) {
    val prefs = context.getSharedPreferences("com.example.vvpixel.SETTINGS", Context.MODE_PRIVATE)
    val storedHash = prefs.getString("app_lock_pattern_hash", null)

    val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
    val isKeyguardSecure = km?.isKeyguardSecure == true

    if (!isKeyguardSecure && storedHash.isNullOrEmpty()) {
        onSuccess()
        return
    }

    val activity = context.findActivity()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && activity != null && isKeyguardSecure) {
        try {
            val executor = ContextCompat.getMainExecutor(context)
            val builder = android.hardware.biometrics.BiometricPrompt.Builder(activity)
                .setTitle("Disable App Lock")
                .setSubtitle("Confirm your identity using biometrics or device PIN")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bm = context.getSystemService(Context.BIOMETRIC_SERVICE) as? android.hardware.biometrics.BiometricManager
                val canBiometricOrCredential = bm?.canAuthenticate(
                    android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                ) == android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS

                val canBiometricOnly = bm?.canAuthenticate(
                    android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
                ) == android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS

                if (canBiometricOrCredential) {
                    builder.setAllowedAuthenticators(
                        android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                } else if (canBiometricOnly) {
                    builder.setAllowedAuthenticators(
                        android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
                    )
                    builder.setNegativeButton("Cancel", executor) { _, _ -> onFailed() }
                } else {
                    builder.setNegativeButton("Cancel", executor) { _, _ -> onFailed() }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                builder.setDeviceCredentialAllowed(true)
            } else {
                builder.setNegativeButton("Cancel", executor) { _, _ -> onFailed() }
            }

            val prompt = builder.build()
            val cancellationSignal = android.os.CancellationSignal()
            prompt.authenticate(
                cancellationSignal,
                executor,
                object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        onFailed()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                    }
                }
            )
            return
        } catch (e: Exception) {
            android.util.Log.e("AppLockSettings", "BiometricPrompt error", e)
        }
    }

    onFailed()
}
