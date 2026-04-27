package com.rbt.survey.ui.dgps

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.rbt.survey.dgps.DgpsStatus
import com.rbt.survey.dgps.getFixQualityString

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun DgpsSettingsScreen(
    viewModel: DgpsViewModel,
    onBack: () -> Unit,
    onNavigateToSatelliteView: () -> Unit,
    onNavigateToBluetoothList: () -> Unit
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
    val useCorsEnabled by viewModel.useCors.collectAsState(true)

    var address by remember(savedAddress) { mutableStateOf(savedAddress ?: "") }
    var host by remember(savedHost) { mutableStateOf(savedHost ?: "103.205.244.106") }
    var port by remember(savedPort) { mutableStateOf(savedPort ?: "2010") }
    var mountpoint by remember(savedMountpoint) { mutableStateOf(savedMountpoint ?: "") }
    var user by remember(savedUser) { mutableStateOf(savedUser ?: "rbtonline021") }
    var pass by remember(savedPass) { mutableStateOf(savedPass ?: "cors@2022") }
    var enabled by remember(useDgpsEnabled) { mutableStateOf(useDgpsEnabled) }
    var enabledCors by remember(useCorsEnabled) { mutableStateOf(useCorsEnabled) }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.errorFlow.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

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
            if (status is DgpsStatus.Connected) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Location Data", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        location?.let {
                            Text("Lat: ${it.latitude}")
                            Text("Lon: ${it.longitude}")
                            Text("Fix: ${getFixQualityString(it.fixQuality)} | Sats: ${it.satellites} \n Acc: ${String.format("%.3f", it.accuracy)}m")
                        }
                    }
                }
            }

            // --- BLUETOOTH SECTION ---
            Text("Bluetooth Connection", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Status: ${when(status) {
                            is DgpsStatus.Idle -> "Disconnected"
                            is DgpsStatus.Connecting -> "Connecting..."
                            is DgpsStatus.Connected -> "Connected"
                            is DgpsStatus.Error -> "Error"
                        }}",
                        fontWeight = FontWeight.Bold,
                        color = if (status is DgpsStatus.Connected) Color(0xFF4CAF50) else Color.Red
                    )
                    
                    if (savedAddress != null) {
                        Text("Device: $savedAddress", style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onNavigateToBluetoothList,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Bluetooth, null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Select Device")
                        }
                        
                        OutlinedButton(
                            onClick = { viewModel.disconnectBluetooth() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                        ) {
                            Text("Disconnect Bluetooth")
                        }
                    }

                    if (status is DgpsStatus.Connected) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToSatelliteView,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Satellite, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View Satellite Status")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- CORS SECTION ---
            Text("CORS / NTRIP Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            
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
                        Text("Enable CORS Corrections", style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = enabledCors, onCheckedChange = { enabledCors = it })
                    }
                    
                    Text(
                        text = "NTRIP: ${if (isNtripConnected) "Streaming Corrections" else "Disconnected"}",
                        color = if (isNtripConnected) Color(0xFF4CAF50) else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("HOST") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabledCors
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("PORT") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabledCors
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var showMountpointDropdown by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = mountpoint,
                            onValueChange = { mountpoint = it },
                            label = { Text("Mountpoint") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = enabledCors,
                            trailingIcon = {
                                if (isFetchingMountpoints) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else if (enabledCors) {
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
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabledCors
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabledCors
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isNtripConnected) {
                        Button(
                            onClick = {
                                viewModel.connectCors(host, port, mountpoint, user, pass)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = enabledCors
                        ) {
                            Text("Connect CORS")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.disconnectCors() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                        ) {
                            Text("Disconnect CORS")
                        }
                    }
                }
            }

        }
    }
}
