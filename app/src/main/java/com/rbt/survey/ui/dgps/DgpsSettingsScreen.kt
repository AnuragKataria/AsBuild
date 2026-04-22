package com.rbt.survey.ui.dgps

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rbt.survey.dgps.DgpsStatus

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun DgpsSettingsScreen(
    viewModel: DgpsViewModel,
    onBack: () -> Unit
) {
    val devices by viewModel.bluetoothDevices.collectAsState()
    val status by viewModel.dgpsStatus.collectAsState()
    val location by viewModel.dgpsLocation.collectAsState()
    val isNtripConnected by viewModel.isNtripConnected.collectAsState()
    val mountpoints by viewModel.mountpoints.collectAsState()
    val isFetchingMountpoints by viewModel.isFetchingMountpoints.collectAsState()

    val savedAddress by viewModel.savedAddress.collectAsState(null)
    val savedHost by viewModel.savedHost.collectAsState(null)
    val savedPort by viewModel.savedPort.collectAsState(null)
    val savedMountpoint by viewModel.savedMountpoint.collectAsState(null)
    val savedUser by viewModel.savedUser.collectAsState(null)
    val savedPass by viewModel.savedPass.collectAsState(null)
    val useDgpsEnabled by viewModel.useDgps.collectAsState(false)

    var address by remember(savedAddress) { mutableStateOf(savedAddress ?: "") }
    var host by remember(savedHost) { mutableStateOf(savedHost ?: "103.205.244.106") }
    var port by remember(savedPort) { mutableStateOf(savedPort ?: "2010") }
    var mountpoint by remember(savedMountpoint) { mutableStateOf(savedMountpoint ?: "") }
    var user by remember(savedUser) { mutableStateOf(savedUser ?: "rbtonline021") }
    var pass by remember(savedPass) { mutableStateOf(savedPass ?: "cors@2022") }
    var enabled by remember(useDgpsEnabled) { mutableStateOf(useDgpsEnabled) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DGPS & CORS Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enable Bluetooth DGPS", style = MaterialTheme.typography.titleMedium)
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Status: ${when(status) {
                            is DgpsStatus.Idle -> "Disconnected"
                            is DgpsStatus.Connecting -> "Connecting..."
                            is DgpsStatus.Connected -> "Connected"
                            is DgpsStatus.Error -> "Error: ${(status as DgpsStatus.Error).message}"
                        }}",
                        color = when(status) {
                            is DgpsStatus.Connected -> Color(0xFF4CAF50)
                            is DgpsStatus.Error -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (status is DgpsStatus.Connected) {
                        Text("NTRIP: ${if (isNtripConnected) "Streaming Corrections" else "Disconnected"}")
                        location?.let {
                            Text("Lat: ${it.latitude}, Lon: ${it.longitude}")
                            Text("Fix: ${it.fixQuality} | Sats: ${it.satellites} | Acc: ${String.format("%.2f", it.accuracy)}m")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Bluetooth Device", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(onClick = { viewModel.scanDevices() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Bluetooth, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refresh Paired Devices")
            }

            devices.forEach { device ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = address == device.address,
                        onClick = { address = device.address }
                    )
                    Text(
                        text = "${device.name ?: "Unknown"} (${device.address})",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("CORS / NTRIP Settings", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("HOST") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("PORT") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            var showMountpointDropdown by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = mountpoint,
                    onValueChange = { mountpoint = it },
                    label = { Text("Mountpoint") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (isFetchingMountpoints) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = { 
                                viewModel.fetchMountpoints(host, port)
                                showMountpointDropdown = true
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = "Fetch Mountpoints")
                            }
                        }
                    }
                )
                
                DropdownMenu(
                    expanded = showMountpointDropdown && mountpoints.isNotEmpty(),
                    onDismissRequest = { showMountpointDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    mountpoints.forEach { mp ->
                        DropdownMenuItem(
                            text = { Text(mp) },
                            onClick = {
                                mountpoint = mp
                                showMountpointDropdown = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))


            OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.saveAndConnect(address, host, port, mountpoint, user, pass, enabled)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Apply & Save Settings")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = { viewModel.disconnect() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Disconnect All")
            }
        }
    }
}
