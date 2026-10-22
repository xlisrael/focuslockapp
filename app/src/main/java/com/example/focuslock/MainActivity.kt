package com.example.focuslock

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focuslock.ui.theme.FocusLockTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            val darkMode = Store.isDarkMode(this)
            FocusLockTheme(darkTheme = darkMode) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Root()
                }
            }
        }
    }

    @Composable
    private fun Root() {
        val context = LocalContext.current
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(Unit) {
            while (true) {
                now = System.currentTimeMillis()
                delay(250)
            }
        }
        val endTime = Store.endTime(context)
        val active = now < endTime
        if (active) {
            BackHandler(enabled = true) {}
        }
        if (active) {
            Focus(endTime - now)
        } else {
            Setup()
        }
    }

    @Composable
    private fun Setup() {
        val context = LocalContext.current
        val pm = context.packageManager
        val allowedApps = remember { Policy.allowedPickableApps(context) }
        var minutesText by remember { mutableStateOf("25") }
        val picked = remember { mutableStateListOf<String>() }
        var darkMode by remember { mutableStateOf(Store.isDarkMode(context)) }
        fun isA11yEnabled(): Boolean {
            val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            return enabled?.contains(context.packageName) == true
        }
        var a11yEnabled by remember { mutableStateOf(isA11yEnabled()) }
        LaunchedEffect(Unit) {
            while (true) {
                a11yEnabled = isA11yEnabled()
                delay(1000)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "FOCUS LOCK",
                fontSize = 20.sp,
                letterSpacing = 8.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Minimal lock. Only what you allow.",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dark mode",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Switch(
                    checked = darkMode,
                    onCheckedChange = { checked ->
                        darkMode = checked
                        Store.setDarkMode(context, checked)
                        (context as? ComponentActivity)?.recreate()
                    }
                )
            }
            OutlinedTextField(
                value = minutesText,
                onValueChange = { value -> minutesText = value.filter { it.isDigit() }.take(4) },
                label = { Text("Minutes") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text(
                text = "Allow extra apps (educational/AI/music — no social)",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                tonalElevation = 0.dp
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(allowedApps, key = { it.packageName }) { app ->
                        val pkg = app.packageName
                        val isChecked = pkg in picked
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (!picked.contains(pkg)) picked.add(pkg)
                                    } else {
                                        picked.remove(pkg)
                                    }
                                }
                            )
                            Text(
                                text = pm.getApplicationLabel(app).toString(),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            if (!a11yEnabled) {
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enable Accessibility Service")
                }
            }
            val minutes = minutesText.toIntOrNull() ?: 0
            Button(
                onClick = {
                    if (minutes > 0 && a11yEnabled) {
                        val allowed = Policy.base(context) + picked
                        Store.start(context, minutes, allowed)
                    }
                },
                enabled = a11yEnabled && minutes > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("START — LOCKED UNTIL END")
            }
        }
    }

    @Composable
    private fun Focus(msLeft: Long) {
        val context = LocalContext.current
        val totalSeconds = msLeft / 1000
        val hours = totalSeconds / 3600
        val minutesPart = (totalSeconds % 3600) / 60
        val secondsPart = totalSeconds % 60
        var torchOn by remember { mutableStateOf(false) }
        val allowedList = Store.allowed(context)
        val allowedExtras = allowedList.filter { it !in Policy.base(context) }
        val smsPkg = Policy.smsPackage(context)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format("%02d:%02d:%02d", hours, minutesPart, secondsPart),
                fontSize = 72.sp,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "LOCKED",
                fontSize = 12.sp,
                letterSpacing = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(48.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Phone")
                }
                OutlinedButton(
                    onClick = {
                        smsPkg?.let { launchApp(it) }
                    },
                    enabled = smsPkg != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Messages")
                }
                OutlinedButton(
                    onClick = {
                        torchOn = !torchOn
                        toggleTorch(torchOn)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (torchOn) "Off" else "Torch")
                }
            }
            if (allowedExtras.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "ALLOWED",
                    fontSize = 11.sp,
                    letterSpacing = 8.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    allowedExtras.forEach { pkg ->
                        val appInfo = runCatching { context.packageManager.getApplicationInfo(pkg, 0) }.getOrNull()
                        val label = appInfo?.let { runCatching { context.packageManager.getApplicationLabel(it).toString() }.getOrNull() } ?: pkg
                        OutlinedButton(
                            onClick = { launchApp(pkg) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }
    }

    private fun launchApp(pkg: String) {
        val intent = packageManager.getLaunchIntentForPackage(pkg) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun toggleTorch(on: Boolean) {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        runCatching {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return@runCatching
            cameraManager.setTorchMode(cameraId, on)
        }
    }
}
