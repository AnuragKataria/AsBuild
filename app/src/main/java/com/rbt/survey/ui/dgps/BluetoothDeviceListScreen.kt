package com.rbt.survey.ui.dgps

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rbt.survey.dgps.DgpsStatus
import com.rbt.survey.dgps.RoverSettings
import kotlin.math.roundToInt

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
    val isNtripConnected by viewModel.isNtripConnected.collectAsState()
    val settings by viewModel.uiSettings.collectAsState()
    val mountpoints by viewModel.mountpoints.collectAsState()
    val isFetchingMountpoints by viewModel.isFetchingMountpoints.collectAsState()
    val lastRawSentence by viewModel.lastRawSentence.collectAsState()
    val lastRawSentenceAt by viewModel.lastRawSentenceAt.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var corsForm by remember(settings.rover) { mutableStateOf(settings.rover) }

    LaunchedEffect(Unit) {
        viewModel.scanDevices()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Connect") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.scanDevices() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            DeviceConnectStatusCard(
                status = status,
                isNtripConnected = isNtripConnected,
                selectedAddress = settings.selectedBluetoothAddress.ifBlank { savedAddress.orEmpty() },
                lastRawSentence = lastRawSentence,
                lastRawSentenceAt = lastRawSentenceAt
            )

            TabRow(selectedTabIndex = selectedTab) {
                listOf("Receiver", "CORS").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                        icon = {
                            Icon(
                                imageVector = if (index == 0) Icons.Default.Bluetooth else Icons.Default.Language,
                                contentDescription = null
                            )
                        }
                    )
                }
            }

            if (selectedTab == 0) {
                ReceiverTab(
                    devices = devices,
                    status = status,
                    savedAddress = savedAddress,
                    onRefresh = { viewModel.scanDevices() },
                    onSelectDevice = viewModel::selectBluetoothDevice,
                    onDisconnect = viewModel::disconnectBluetooth
                )
            } else {
                CorsTab(
                    form = corsForm,
                    mountpoints = mountpoints,
                    isFetchingMountpoints = isFetchingMountpoints,
                    isConnected = isNtripConnected,
                    onFormChange = { corsForm = it },
                    onFetchMountpoints = { viewModel.fetchMountpoints(corsForm.host, corsForm.port) },
                    onConnect = {
                        viewModel.connectCors(
                            host = corsForm.host,
                            port = corsForm.port,
                            mountpoint = corsForm.mountpoint,
                            user = corsForm.user,
                            pass = corsForm.password
                        )
                    },
                    onDisconnect = viewModel::disconnectCors
                )
            }
        }
    }
}

@Composable
private fun DeviceConnectStatusCard(
    status: DgpsStatus,
    isNtripConnected: Boolean,
    selectedAddress: String,
    lastRawSentence: String,
    lastRawSentenceAt: Long?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DeviceStatePill(
                    label = "Receiver",
                    value = when (status) {
                        is DgpsStatus.Connected -> "Connected"
                        is DgpsStatus.Connecting -> "Connecting"
                        is DgpsStatus.Error -> "Error"
                        is DgpsStatus.Idle -> "Idle"
                    },
                    color = when (status) {
                        is DgpsStatus.Connected -> Color(0xFF2E7D32)
                        is DgpsStatus.Connecting -> Color(0xFFF9A825)
                        is DgpsStatus.Error -> Color(0xFFC62828)
                        is DgpsStatus.Idle -> Color.Gray
                    }
                )
                DeviceStatePill(
                    label = "CORS",
                    value = if (isNtripConnected) "Connected" else "Idle",
                    color = if (isNtripConnected) Color(0xFF2E7D32) else Color.Gray
                )
            }

            Text(
                text = if (selectedAddress.isBlank()) "No paired receiver selected yet." else "Receiver: $selectedAddress",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = buildString {
                    append("Last data: ")
                    append(
                        when {
                            lastRawSentenceAt == null -> "No receiver data"
                            else -> {
                                val ageSeconds = ((System.currentTimeMillis() - lastRawSentenceAt) / 1000f).roundToInt()
                                "$ageSeconds s ago"
                            }
                        }
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (lastRawSentence.isNotBlank()) {
                Text(
                    text = lastRawSentence,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DeviceStatePill(
    label: String,
    value: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ReceiverTab(
    devices: List<android.bluetooth.BluetoothDevice>,
    status: DgpsStatus,
    savedAddress: String?,
    onRefresh: () -> Unit,
    onSelectDevice: (String) -> Unit,
    onDisconnect: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) {
                Text("Refresh Paired")
            }
            Button(
                onClick = onDisconnect,
                modifier = Modifier.weight(1f),
                enabled = status is DgpsStatus.Connected || status is DgpsStatus.Connecting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCA6A47))
            ) {
                Text("Disconnect")
            }
        }

        Text(
            "PAIRED RECEIVERS",
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
                        headlineContent = {
                            Text(device.name ?: "Unknown Device", fontWeight = FontWeight.SemiBold)
                        },
                        supportingContent = { Text(device.address) },
                        leadingContent = {
                            Icon(
                                Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = if (savedAddress == device.address) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        },
                        trailingContent = {
                            if (savedAddress == device.address) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = if (status is DgpsStatus.Connected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier
                            .clickable { onSelectDevice(device.address) }
                            .padding(horizontal = 8.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun CorsTab(
    form: RoverSettings,
    mountpoints: List<String>,
    isFetchingMountpoints: Boolean,
    isConnected: Boolean,
    onFormChange: (RoverSettings) -> Unit,
    onFetchMountpoints: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        OutlinedTextField(
            value = form.host,
            onValueChange = { onFormChange(form.copy(host = it)) },
            label = { Text("Host") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = form.port,
                onValueChange = { onFormChange(form.copy(port = it)) },
                label = { Text("Port") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = form.mountpoint,
                onValueChange = { onFormChange(form.copy(mountpoint = it)) },
                label = { Text("Mountpoint") },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = form.user,
                onValueChange = { onFormChange(form.copy(user = it)) },
                label = { Text("User") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = form.password,
                onValueChange = { onFormChange(form.copy(password = it)) },
                label = { Text("Password") },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onFetchMountpoints, modifier = Modifier.weight(1f)) {
                if (isFetchingMountpoints) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Get Mountpoints")
            }
            Button(
                onClick = if (isConnected) onDisconnect else onConnect,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) Color(0xFFCA6A47) else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isConnected) "Disconnect" else "Connect")
            }
        }

        if (mountpoints.isNotEmpty()) {
            Text(
                text = "AVAILABLE MOUNTPOINTS",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isConnected) "Corrections are active." else "Select a mountpoint to connect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mountpoints.forEach { mountpoint ->
                    FilterChip(
                        selected = form.mountpoint == mountpoint,
                        onClick = { onFormChange(form.copy(mountpoint = mountpoint)) },
                        label = { Text(mountpoint) }
                    )
                }
            }
        }
    }
}
