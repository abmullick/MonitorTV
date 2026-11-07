package com.monitortv

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.monitortv.discovery.TvDiscoveryManager
import com.monitortv.model.TvDevice

class MainActivity : ComponentActivity() {
    private lateinit var discovery: TvDiscoveryManager
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) discovery.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        discovery = TvDiscoveryManager(applicationContext)
        setContent {
            MaterialTheme {
                val devices by discovery.devices.collectAsState()
                val status by discovery.status.collectAsState()
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("MonitorTV", style = MaterialTheme.typography.headlineLarge)
                        Text("Smart TV audio and volume manager")
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Phase 1 · TV discovery", style = MaterialTheme.typography.titleMedium)
                                Text(status)
                                Button(onClick = { scan() }) { Text("Scan for TVs") }
                                OutlinedButton(onClick = { discovery.stop() }) { Text("Stop scan") }
                            }
                        }
                        Text("Discovered devices (${devices.size})", style = MaterialTheme.typography.titleLarge)
                        if (devices.isEmpty()) {
                            Text("No compatible TVs found yet. Check that both devices share the same Wi-Fi network and that multicast is not blocked.")
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(devices, key = { it.serviceName }) { DeviceCard(it) }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Text("Pairing and volume control are the next milestone. Discovery does not yet control the TV.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    private fun scan() {
        if (Build.VERSION.SDK_INT >= 37) {
            val permission = "android.permission.ACCESS_LOCAL_NETWORK"
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(permission)
                return
            }
        }
        discovery.start()
    }

    override fun onDestroy() { discovery.stop(); super.onDestroy() }
}

@androidx.compose.runtime.Composable
private fun DeviceCard(device: TvDevice) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(device.displayName, style = MaterialTheme.typography.titleMedium)
            Text(device.endpoint, style = MaterialTheme.typography.bodyMedium)
            Text("Android TV Remote v2 service detected", style = MaterialTheme.typography.bodySmall)
        }
    }
}
