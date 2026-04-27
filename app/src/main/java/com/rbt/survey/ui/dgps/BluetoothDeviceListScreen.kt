package com.rbt.survey.ui.dgps

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
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
fun BluetoothDeviceListScreen(
    viewModel: DgpsViewModel,
    onBack: () -> Unit
) {
    val devices by viewModel.bluetoothDevices.collectAsState()
    val status by viewModel.dgpsStatus.collectAsState()
    val savedAddress by viewModel.savedAddress.collectAsState(null)

    LaunchedEffect(Unit) {
        viewModel.scanDevices()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Bluetooth DGPS") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.scanDevices() }) {
                        Icon(Icons.Default.Bluetooth, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Status Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (status) {
                        is DgpsStatus.Connected -> Color(0xFFE8F5E9)
                        is DgpsStatus.Error -> Color(0xFFFFEBEE)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .padding(2.dp)
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = when (status) {
                                is DgpsStatus.Connected -> Color.Green
                                is DgpsStatus.Connecting -> Color.Yellow
                                is DgpsStatus.Error -> Color.Red
                                else -> Color.Gray
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {}
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = when (status) {
                            is DgpsStatus.Idle -> "Disconnected"
                            is DgpsStatus.Connecting -> "Connecting..."
                            is DgpsStatus.Connected -> "Connected"
                            is DgpsStatus.Error -> "Connection Failed"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                "PAIRED DEVICES",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )

            if (devices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No paired Bluetooth devices found")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(devices) { device ->
                        ListItem(
                            headlineContent = { Text(device.name ?: "Unknown Device", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text(device.address) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    tint = if (savedAddress == device.address) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            },
                            trailingContent = {
                                if (savedAddress == device.address && status is DgpsStatus.Connected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Connected", tint = Color(0xFF4CAF50))
                                }
                            },
                            modifier = Modifier
                                .clickable {
                                    viewModel.selectBluetoothDevice(device.address)
                                }
                                .padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}
