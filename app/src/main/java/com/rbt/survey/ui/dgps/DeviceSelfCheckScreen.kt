package com.rbt.survey.ui.dgps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rbt.survey.dgps.DgpsStatus
import com.rbt.survey.dgps.getFixQualityString
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private data class SelfCheckItem(
    val title: String,
    val detail: String,
    val passed: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSelfCheckScreen(
    viewModel: DgpsViewModel,
    onBack: () -> Unit,
    onOpenCommunication: () -> Unit
) {
    val status by viewModel.dgpsStatus.collectAsState()
    val location by viewModel.dgpsLocation.collectAsState()
    val isNtripConnected by viewModel.isNtripConnected.collectAsState()
    val settings by viewModel.uiSettings.collectAsState()
    val lastRawSentence by viewModel.lastRawSentence.collectAsState()
    val lastRawSentenceAt by viewModel.lastRawSentenceAt.collectAsState()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    val receiverSelected = settings.selectedBluetoothAddress.isNotBlank()
    val bluetoothConnected = status is DgpsStatus.Connected
    val dataAgeSeconds = lastRawSentenceAt?.let { (now - it) / 1000f }
    val hasFreshReceiverData = dataAgeSeconds != null && dataAgeSeconds <= 5f
    val hasFix = (location?.fixQuality ?: 0) > 0
    val hasSatelliteCoverage = (location?.satellites ?: 0) >= 4
    val needsCors = settings.rover.connectingMode.equals("NTRIP", ignoreCase = true)
    val correctionHealthy = !needsCors || isNtripConnected

    val checks = listOf(
        SelfCheckItem(
            title = "Receiver selected",
            detail = if (receiverSelected) settings.selectedBluetoothAddress else "No receiver paired in this profile.",
            passed = receiverSelected
        ),
        SelfCheckItem(
            title = "Bluetooth link",
            detail = when (status) {
                is DgpsStatus.Connected -> "Receiver link is active."
                is DgpsStatus.Connecting -> "Receiver is still connecting."
                is DgpsStatus.Error -> "Receiver connection failed."
                is DgpsStatus.Idle -> "Receiver is disconnected."
            },
            passed = bluetoothConnected
        ),
        SelfCheckItem(
            title = "Receiver data stream",
            detail = when {
                dataAgeSeconds == null -> "No NMEA data has arrived yet."
                else -> "Last receiver packet ${dataAgeSeconds.roundToInt()} seconds ago."
            },
            passed = hasFreshReceiverData
        ),
        SelfCheckItem(
            title = "Position solution",
            detail = if (hasFix) {
                getFixQualityString(location?.fixQuality ?: 0)
            } else {
                "No valid fix yet."
            },
            passed = hasFix
        ),
        SelfCheckItem(
            title = "Satellite coverage",
            detail = "${location?.satellites ?: 0} satellites tracked.",
            passed = hasSatelliteCoverage
        ),
        SelfCheckItem(
            title = "CORS corrections",
            detail = if (needsCors) {
                if (isNtripConnected) "NTRIP correction link is active." else "NTRIP is configured but not connected."
            } else {
                "Receiver profile does not require NTRIP."
            },
            passed = correctionHealthy
        )
    )

    val overallHealthy = checks.all(SelfCheckItem::passed)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Self Check") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DgpsMetricsStrip(
                status = status,
                location = location,
                isNtripConnected = isNtripConnected
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (overallHealthy) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (overallHealthy) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = if (overallHealthy) Color(0xFF2E7D32) else Color(0xFFEF6C00)
                    )
                    Column {
                        Text(
                            text = if (overallHealthy) "Receiver is healthy" else "Receiver needs attention",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (overallHealthy) {
                                "The selected receiver, data feed, and correction path all look good."
                            } else {
                                "Open the communication page or reconnect the receiver to clear the failed checks."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            checks.forEach { check ->
                SelfCheckCard(check)
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Live receiver sample", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (lastRawSentence.isBlank()) "No raw NMEA sentence received yet." else lastRawSentence,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onOpenCommunication,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.SettingsRemote, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Open Communication")
                }
                Button(
                    onClick = {
                        if (receiverSelected) {
                            viewModel.selectBluetoothDevice(settings.selectedBluetoothAddress)
                        }
                        if (needsCors) {
                            viewModel.connectCors(
                                settings.rover.host,
                                settings.rover.port,
                                settings.rover.mountpoint,
                                settings.rover.user,
                                settings.rover.password
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F6FED))
                ) {
                    Text("Retry Checks")
                }
            }
            }
        }
    }
}

@Composable
private fun SelfCheckCard(check: SelfCheckItem) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (check.passed) Color(0xFFF1F8E9) else Color(0xFFFFEBEE)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(if (check.passed) Color(0xFF43A047) else Color(0xFFE53935), CircleShape)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(check.title, fontWeight = FontWeight.SemiBold)
                Text(check.detail, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = if (check.passed) "PASS" else "FAIL",
                color = if (check.passed) Color(0xFF2E7D32) else Color(0xFFC62828),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
