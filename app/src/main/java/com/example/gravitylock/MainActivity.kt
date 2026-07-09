package com.example.gravitylock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gravitylock.ui.theme.GravityLockTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GravityLockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GravityLockScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GravityLockScreen(viewModel: MainViewModel) {
    val isMasterEnabled by viewModel.isMasterEnabled.collectAsState()
    val gravityZ by viewModel.gravityZ.collectAsState()
    val proximity by viewModel.proximity.collectAsState()
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsState()
    val isBatteryOptimizationIgnored by viewModel.isBatteryOptimizationIgnored.collectAsState()

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GravityLock") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Master Toggle
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Master Toggle", style = MaterialTheme.typography.titleLarge)
                        Text("Enable or disable auto-lock", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = isMasterEnabled,
                        onCheckedChange = { viewModel.setMasterEnabled(it) }
                    )
                }
            }

            // Sensor Live Feed
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Sensor Live Feed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Gravity Z-axis: %.2f m/s²".format(gravityZ))
                    Text("Proximity: %.2f cm".format(proximity))
                }
            }

            // Permission Dashboard
            Text(
                "HyperOS Permission Dashboard",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
            )

            // 1. Accessibility Service
            PermissionCard(
                title = "Accessibility Service",
                description = "Required to lock the screen without disabling biometric unlock.",
                isGranted = isAccessibilityEnabled,
                buttonText = "Open Settings",
                onClick = {
                    context.startActivity(PermissionHelper.getAccessibilitySettingsIntent())
                }
            )

            // 2. Battery Optimization
            PermissionCard(
                title = "Disable Battery Optimization",
                description = "Prevents HyperOS from killing the background service.",
                isGranted = isBatteryOptimizationIgnored,
                buttonText = "Ignore Optimization",
                onClick = {
                    context.startActivity(PermissionHelper.getBatteryOptimizationIntent(context))
                }
            )

            // 3. Autostart
            PermissionCard(
                title = "Autostart / Background Execution",
                description = "Ensure the app can start automatically and run in the background. (Manual check required)",
                isGranted = false, // We can't definitively check this programmatically on MIUI
                buttonText = "Open Security Center",
                onClick = {
                    try {
                        context.startActivity(PermissionHelper.getAutostartSettingsIntent())
                    } catch (e: Exception) {
                        // Fallback if component not found
                        e.printStackTrace()
                    }
                }
            )
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = if (isGranted) "Granted" else "Missing",
                    color = if (isGranted) Color(0xFF4CAF50) else Color(0xFFF44336),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            if (!isGranted || title.contains("Autostart")) {
                Button(onClick = onClick) {
                    Text(buttonText)
                }
            }
        }
    }
}
