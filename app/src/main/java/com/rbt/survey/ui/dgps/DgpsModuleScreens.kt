package com.rbt.survey.ui.dgps

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rbt.survey.dgps.BaseSettings
import com.rbt.survey.dgps.DeviceInformation
import com.rbt.survey.dgps.DeviceSettings
import com.rbt.survey.dgps.DgpsLocation
import com.rbt.survey.dgps.DgpsStatus
import com.rbt.survey.dgps.GnssSystemSettings
import com.rbt.survey.dgps.InspectionAccuracySettings
import com.rbt.survey.dgps.NmeaSettings
import com.rbt.survey.dgps.RoverSettings
import com.rbt.survey.dgps.SatelliteConstellation
import com.rbt.survey.dgps.SatelliteInfo
import com.rbt.survey.dgps.StaticSurveySettings
import com.rbt.survey.dgps.getFixQualityString
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val DgpsTopBarColor = Color(0xFF2F2F2F)
private val DgpsAccent = Color(0xFFFF9F43)
private val DgpsSection = Color(0xFF73C992)
private val DgpsSurface = Color(0xFFF7F7F7)
private val DgpsBorder = Color(0xFFE2E2E2)

data class DgpsTile(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DgpsHomeScreen(
    viewModel: DgpsViewModel,
    onBack: () -> Unit,
    onCommunicationClick: () -> Unit,
    onRoverClick: () -> Unit,
    onBaseClick: () -> Unit,
    onStaticClick: () -> Unit,
    onInspectionAccuracyClick: () -> Unit,
    onDeviceInformationClick: () -> Unit,
    onDeviceSettingsClick: () -> Unit,
    onNmeaSettingsClick: () -> Unit,
    onPositionInformationClick: () -> Unit,
    onGnssSystemClick: () -> Unit
) {
    val location by viewModel.dgpsLocation.collectAsState()
    val status by viewModel.dgpsStatus.collectAsState()
    val isNtripConnected by viewModel.isNtripConnected.collectAsState()
    val settings by viewModel.uiSettings.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.errorFlow.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    val tiles = listOf(
        DgpsTile("Communication", Icons.Default.Bluetooth, onCommunicationClick),
        DgpsTile("Rover", Icons.Default.MyLocation, onRoverClick),
        DgpsTile("Base", Icons.Default.Radio, onBaseClick),
        DgpsTile("Static", Icons.Default.Straighten, onStaticClick),
        DgpsTile("Inspection Accuracy", Icons.Default.Route, onInspectionAccuracyClick),
        DgpsTile("Device Information", Icons.Default.Info, onDeviceInformationClick),
        DgpsTile("Device Settings", Icons.Default.Settings, onDeviceSettingsClick),
        DgpsTile("NMEA Settings", Icons.Default.Tune, onNmeaSettingsClick),
        DgpsTile("Position Information", Icons.Default.Map, onPositionInformationClick),
        DgpsTile("GNSS System", Icons.Default.Public, onGnssSystemClick)
    )

    Scaffold(
        topBar = {
            DgpsTopBar(
                title = "DGPS Device",
                onBack = onBack
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            DgpsStatusHeader(
                status = status,
                location = location,
                isNtripConnected = isNtripConnected,
                batteryText = settings.deviceInfo.batteryPower
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(tiles.size) { index ->
                    DgpsTileCard(tile = tiles[index])
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoverModeSettingsScreen(
    viewModel: DgpsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.uiSettings.collectAsState()
    val mountpoints by viewModel.mountpoints.collectAsState()
    val isFetchingMountpoints by viewModel.isFetchingMountpoints.collectAsState()
    val isNtripConnected by viewModel.isNtripConnected.collectAsState()
    val context = LocalContext.current
    var form by remember(settings) { mutableStateOf(settings.rover) }

    LaunchedEffect(Unit) {
        viewModel.errorFlow.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    DgpsFormScaffold(title = "Rover Mode Settings", onBack = onBack) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            SettingsSection("General Parameters") {
                DualFieldRow(
                    leftLabel = "Cut-Off Angle",
                    leftValue = form.cutOffAngle,
                    onLeftValueChange = { form = form.copy(cutOffAngle = it) },
                    rightLabel = "Diff Delay",
                    rightValue = form.diffDelay,
                    onRightValueChange = { form = form.copy(diffDelay = it) }
                )
                SwitchRow(
                    title = "Disable PPK",
                    checked = form.disablePpk,
                    onCheckedChange = { form = form.copy(disablePpk = it) }
                )
            }

            SettingsSection("Datalink Settings") {
                SettingTextField("Datalink", form.datalink) { form = form.copy(datalink = it) }
                SettingTextField("Connecting Mode", form.connectingMode) { form = form.copy(connectingMode = it) }
                DualFieldRow(
                    leftLabel = "IP / Host",
                    leftValue = form.host,
                    onLeftValueChange = { form = form.copy(host = it) },
                    rightLabel = "Server Port",
                    rightValue = form.port,
                    onRightValueChange = { form = form.copy(port = it) }
                )
                DualFieldRow(
                    leftLabel = "User",
                    leftValue = form.user,
                    onLeftValueChange = { form = form.copy(user = it) },
                    rightLabel = "Password",
                    rightValue = form.password,
                    onRightValueChange = { form = form.copy(password = it) }
                )
                SettingTextField("PPP Type", form.pppType) { form = form.copy(pppType = it) }
            }

            SettingsSection("Mountpoint Settings") {
                SettingTextField("Mountpoint", form.mountpoint) { form = form.copy(mountpoint = it) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.fetchMountpoints(form.host, form.port) },
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isFetchingMountpoints) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Get")
                    }
                    OutlinedButton(
                        onClick = {
                            if (mountpoints.isNotEmpty()) {
                                form = form.copy(mountpoint = mountpoints.first())
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = mountpoints.isNotEmpty()
                    ) {
                        Text("Use First")
                    }
                }
                if (mountpoints.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        mountpoints.forEach { mountpoint ->
                            FilterChip(
                                selected = form.mountpoint == mountpoint,
                                onClick = { form = form.copy(mountpoint = mountpoint) },
                                label = { Text(mountpoint) }
                            )
                        }
                    }
                }
            }

            SettingsSection("RX Data Status") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusIndicatorCircle(isActive = isNtripConnected)
                    Button(
                        onClick = {
                            if (isNtripConnected) viewModel.stopRover() else viewModel.startRover(form)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DgpsAccent)
                    ) {
                        Text(if (isNtripConnected) "Stop" else "Start")
                    }
                }
                SwitchRow(
                    title = "Auto Connect to Network",
                    checked = form.autoConnectToNetwork,
                    onCheckedChange = { form = form.copy(autoConnectToNetwork = it) }
                )
                SwitchRow(
                    title = "Base Coordinates Change Alert",
                    subtitle = "(VRS excluded)",
                    checked = form.baseCoordinatesChangeAlert,
                    onCheckedChange = { form = form.copy(baseCoordinatesChangeAlert = it) }
                )
            }

            BottomActionRow(
                leftText = "Share",
                centerText = "Save",
                rightText = if (isNtripConnected) "Stop" else "Apply",
                onLeftClick = {
                    shareText(context, "Rover Mode Settings", buildRoverShareText(form))
                },
                onCenterClick = {
                    viewModel.saveRoverSettings(form)
                    Toast.makeText(context, "Rover settings saved.", Toast.LENGTH_SHORT).show()
                },
                onRightClick = {
                    if (isNtripConnected) viewModel.stopRover() else viewModel.startRover(form)
                }
            )
        }
    }
}

@Composable
fun BaseModeSettingsScreen(
    viewModel: DgpsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.uiSettings.collectAsState()
    val context = LocalContext.current
    var form by remember(settings) { mutableStateOf(settings.base) }

    DgpsFormScaffold(title = "Base Mode Settings", onBack = onBack) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            SettingsSection("General Parameters") {
                DualFieldRow(
                    leftLabel = "Base ID",
                    leftValue = form.baseId,
                    onLeftValueChange = { form = form.copy(baseId = it) },
                    rightLabel = "Diff Format",
                    rightValue = form.diffFormat,
                    onRightValueChange = { form = form.copy(diffFormat = it) }
                )
                DualFieldRow(
                    leftLabel = "Cut-Off Angle",
                    leftValue = form.cutOffAngle,
                    onLeftValueChange = { form = form.copy(cutOffAngle = it) },
                    rightLabel = "PDOP",
                    rightValue = form.pdop,
                    onRightValueChange = { form = form.copy(pdop = it) }
                )
                SettingTextField("Base Startup Mode", form.baseStartupMode) { form = form.copy(baseStartupMode = it) }
            }

            SettingsSection("PPK Settings") {
                SettingTextField("Mode", form.ppkMode) { form = form.copy(ppkMode = it) }
            }

            SettingsSection("Datalink Settings") {
                SettingTextField("Datalink", form.datalink) { form = form.copy(datalink = it) }
                DualFieldRow(
                    leftLabel = "Channel",
                    leftValue = form.channel,
                    onLeftValueChange = { form = form.copy(channel = it) },
                    rightLabel = "Protocol",
                    rightValue = form.protocol,
                    onRightValueChange = { form = form.copy(protocol = it) }
                )
                SettingTextField("Power", form.power) { form = form.copy(power = it) }
            }

            SettingsSection("Caster Settings") {
                SwitchRow(
                    title = "Ntrip Caster",
                    checked = form.ntripCasterEnabled,
                    onCheckedChange = { form = form.copy(ntripCasterEnabled = it) }
                )
                DualFieldRow(
                    leftLabel = "Port",
                    leftValue = form.ntripPort,
                    onLeftValueChange = { form = form.copy(ntripPort = it) },
                    rightLabel = "Base Access Point",
                    rightValue = form.baseAccessPoint,
                    onRightValueChange = { form = form.copy(baseAccessPoint = it) }
                )
            }

            BottomActionRow(
                leftText = "Share",
                centerText = "Save",
                rightText = "Start Base",
                onLeftClick = {
                    shareText(context, "Base Mode Settings", buildBaseShareText(form))
                },
                onCenterClick = {
                    viewModel.saveBaseSettings(form)
                    Toast.makeText(context, "Base settings saved.", Toast.LENGTH_SHORT).show()
                },
                onRightClick = { viewModel.startBase(form) }
            )
        }
    }
}

@Composable
fun StaticSurveySettingsScreen(
    viewModel: DgpsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.uiSettings.collectAsState()
    val context = LocalContext.current
    var form by remember(settings) { mutableStateOf(settings.staticSurvey) }

    DgpsFormScaffold(title = "Static Survey Settings", onBack = onBack) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection("Option Settings") {
                SettingTextField("Point Name", form.pointName) { form = form.copy(pointName = it) }
                DualFieldRow(
                    leftLabel = "PDOP",
                    leftValue = form.pdop,
                    onLeftValueChange = { form = form.copy(pdop = it) },
                    rightLabel = "Cut-Off Angle",
                    rightValue = form.cutOffAngle,
                    onRightValueChange = { form = form.copy(cutOffAngle = it) }
                )
                DualFieldRow(
                    leftLabel = "Interval",
                    leftValue = form.interval,
                    onLeftValueChange = { form = form.copy(interval = it) },
                    rightLabel = "Observation Time",
                    rightValue = form.observationTime,
                    onRightValueChange = { form = form.copy(observationTime = it) }
                )
            }

            SettingsSection("Antenna Parameters") {
                SettingTextField("Antenna Measuring Height", form.antennaMeasuringHeight) {
                    form = form.copy(antennaMeasuringHeight = it)
                }
                SettingTextField("Antenna Measuring Type", form.antennaMeasuringType) {
                    form = form.copy(antennaMeasuringType = it)
                }
                SettingTextField("Antenna Height", form.antennaHeight) { form = form.copy(antennaHeight = it) }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.saveStaticSurveySettings(form)
                    Toast.makeText(context, "Static survey settings saved.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DgpsAccent)
            ) {
                Text("Start")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun InspectionAccuracyScreen(
    viewModel: DgpsViewModel,
    onBack: () -> Unit,
    onPoleCalibrationClick: () -> Unit
) {
    val settings by viewModel.uiSettings.collectAsState()
    val context = LocalContext.current
    var form by remember(settings) { mutableStateOf(settings.inspectionAccuracy) }

    DgpsFormScaffold(title = "Inspection Accuracy", onBack = onBack) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection("") {
                SettingTextField("Antenna Height", form.antennaHeight) { form = form.copy(antennaHeight = it) }
                SettingTextField("Average Points", form.averagePoints) { form = form.copy(averagePoints = it) }
                SettingTextField("Average Interval", form.averageInterval) { form = form.copy(averageInterval = it) }
                SettingTextField("Exclusion Abnormal Point Ratio (%)", form.exclusionAbnormalRatio) {
                    form = form.copy(exclusionAbnormalRatio = it)
                }
            }

            BottomActionRow(
                leftText = "Start",
                centerText = "Pole Calibration",
                rightText = "Save",
                onLeftClick = {
                    viewModel.saveInspectionAccuracySettings(form)
                    Toast.makeText(context, "Inspection accuracy settings saved.", Toast.LENGTH_SHORT).show()
                },
                onCenterClick = {
                    viewModel.saveInspectionAccuracySettings(form)
                    onPoleCalibrationClick()
                },
                onRightClick = {
                    viewModel.saveInspectionAccuracySettings(form)
                    Toast.makeText(context, "Inspection accuracy settings saved.", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun PoleCalibrationScreen(
    viewModel: DgpsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.uiSettings.collectAsState()
    val status by viewModel.dgpsStatus.collectAsState()
    val location by viewModel.dgpsLocation.collectAsState()
    val context = LocalContext.current
    var antennaHeight by remember(settings) { mutableStateOf(settings.inspectionAccuracy.antennaHeight) }

    DgpsFormScaffold(title = "Pole Calibration", onBack = onBack) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            SettingTextField(
                label = "Antenna Height",
                value = antennaHeight,
                onValueChange = { antennaHeight = it }
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    Text(
                        text = "calibration steps:",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text("1. In the case where the solution status is Ready, click Start to calibrate.", fontSize = 16.sp)
                    Text("2. Fix the pole tip and shake the receiver back and forth to collect 50 points.", fontSize = 16.sp)
                    Text(
                        "3. Keep the pole tip fixed, rotate yourself and the receiver 90 degrees, then repeat until all four directions are completed.",
                        fontSize = 16.sp
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .border(3.dp, DgpsAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A", color = DgpsAccent, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                    }
                    Text("Direction", fontSize = 20.sp)
                    Text(
                        if (status is DgpsStatus.Connected) getFixQualityString(location?.fixQuality ?: 0) else "No Solution",
                        color = DgpsAccent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                CompassGraphic()
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    viewModel.saveInspectionAccuracySettings(
                        settings.inspectionAccuracy.copy(antennaHeight = antennaHeight)
                    )
                    Toast.makeText(
                        context,
                        "Pole calibration session prepared. Start moving the receiver once the solution is ready.",
                        Toast.LENGTH_LONG
                    ).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DgpsAccent)
            ) {
                Text("Start")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInformationScreen(
    viewModel: DgpsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.uiSettings.collectAsState()
    val context = LocalContext.current
    val info = settings.deviceInfo.copy(currentDatalink = settings.rover.datalink)

    Scaffold(
        topBar = {
            DgpsTopBar(
                title = "Device Information",
                onBack = onBack
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SettingsSection("Basic Information") {
                InfoRow("Device SN", info.deviceSn)
                InfoRow("Device Firmware", info.deviceFirmware)
                InfoRow("Sensor Version", info.sensorVersion)
                InfoRow("GNSS Firmware", info.gnssFirmware.ifBlank { "-" })
                InfoRow("Current Datalink", info.currentDatalink)
                InfoRow("Battery Power", info.batteryPower)
                InfoRow("Expiration Date", info.expirationDate)
            }

            SettingsSection("Antenna Parameters") {
                Text(
                    text = info.antennaModel,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                )
                DualInfoRow("R:${info.antennaRadiusMm} mm", "H:${info.antennaHeightMm} mm")
                DualInfoRow("HL1:${info.antennaHl1Mm} mm", "HL2:${info.antennaHl2Mm} mm")
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    Toast.makeText(context, "Latest version check needs live receiver command support.", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DgpsAccent)
            ) {
                Text("Check Latest Version")
            }
        }
    }
}

@Composable
fun DeviceSettingsScreen(
    viewModel: DgpsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.uiSettings.collectAsState()
    val context = LocalContext.current
    var form by remember(settings) { mutableStateOf(settings.deviceSettings) }

    DgpsFormScaffold(title = "Device Settings", onBack = onBack) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SettingTextField("Positioning Output Frequency", form.positioningOutputFrequency) {
                form = form.copy(positioningOutputFrequency = it)
            }
            SettingTextField("IMU Data Output Frequency", form.imuOutputFrequency) {
                form = form.copy(imuOutputFrequency = it)
            }
            SwitchRow(
                title = "UGypsophila Technology",
                checked = form.uGypsophilaTechnology,
                onCheckedChange = { form = form.copy(uGypsophilaTechnology = it) }
            )
            SettingTextField("Time Zone", form.timeZone) { form = form.copy(timeZone = it) }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.saveDeviceSettings(form)
                    Toast.makeText(context, "Device settings saved.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DgpsAccent)
            ) {
                Text("OK")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun NmeaSettingsScreen(
    viewModel: DgpsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.uiSettings.collectAsState()
    val context = LocalContext.current
    var form by remember(settings) { mutableStateOf(settings.nmea) }

    DgpsFormScaffold(title = "NMEA Settings", onBack = onBack) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SettingTextField("GPGGA", form.gpgga) { form = form.copy(gpgga = it) }
            SettingTextField("GPGSA", form.gpgsa) { form = form.copy(gpgsa = it) }
            SettingTextField("GPGSV", form.gpgsv) { form = form.copy(gpgsv = it) }
            SettingTextField("GPGST", form.gpgst) { form = form.copy(gpgst = it) }
            SettingTextField("GPZDA", form.gpzda) { form = form.copy(gpzda = it) }
            SettingTextField("GPRMC", form.gprmc) { form = form.copy(gprmc = it) }
            SettingTextField("GPVTG", form.gpvtg) { form = form.copy(gpvtg = it) }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.saveNmeaSettings(form)
                    Toast.makeText(context, "NMEA settings applied.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DgpsAccent)
            ) {
                Text("Apply")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun GnssSystemScreen(
    viewModel: DgpsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.uiSettings.collectAsState()
    val context = LocalContext.current
    var form by remember(settings) { mutableStateOf(settings.gnssSystems) }

    DgpsFormScaffold(title = "GNSS System", onBack = onBack) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SwitchRow("GPS", checked = form.gps, onCheckedChange = { form = form.copy(gps = it) })
            SwitchRow("GLONASS", checked = form.glonass, onCheckedChange = { form = form.copy(glonass = it) })
            SwitchRow("BEIDOU", checked = form.beidou, onCheckedChange = { form = form.copy(beidou = it) })
            SwitchRow("GALILEO", checked = form.galileo, onCheckedChange = { form = form.copy(galileo = it) })
            SwitchRow("SBAS", checked = form.sbas, onCheckedChange = { form = form.copy(sbas = it) })
            SwitchRow("QZSS", checked = form.qzss, onCheckedChange = { form = form.copy(qzss = it) })
            SwitchRow("IRNSS", checked = form.irnss, onCheckedChange = { form = form.copy(irnss = it) })

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.saveGnssSystems(form)
                    Toast.makeText(context, "GNSS system settings saved.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DgpsAccent)
            ) {
                Text("OK")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PositionInformationScreen(
    viewModel: DgpsViewModel,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val location by viewModel.dgpsLocation.collectAsState()
    val status by viewModel.dgpsStatus.collectAsState()
    val settings by viewModel.uiSettings.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Position Information", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = onSettingsClick) {
                        Text("Settings", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DgpsTopBarColor)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = DgpsAccent
            ) {
                listOf("Details", "Base", "SAT Info", "SAT Map").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                color = if (selectedTab == index) DgpsAccent else Color.Black,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> PositionDetailsTab(location, status, settings.deviceSettings.timeZone)
                1 -> PositionBaseTab(location, settings)
                2 -> SatelliteInfoTab(location?.satellitesList.orEmpty())
                else -> SatelliteMapTab(
                    satellites = filterSatellites(location?.satellitesList.orEmpty(), settings.gnssSystems),
                    gnssSystems = settings.gnssSystems,
                    onToggleSystem = { viewModel.saveGnssSystems(it) }
                )
            }
        }
    }
}

@Composable
private fun PositionDetailsTab(
    location: DgpsLocation?,
    status: DgpsStatus,
    timeZone: String
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            ValueRow("Solution", formatSolution(status, location))
            ValueRow("B", formatDms(location?.latitude, true))
            ValueRow("L", formatDms(location?.longitude, false))
            ValueRow("H", formatHeight(location?.altitude))
            DualValueRow("Speed", formatSpeed(location?.speedMps), "Heading", formatHeading(location?.headingDegrees))
            DualValueRow("PDOP", formatOptionalDecimal(location?.pdop), "HRMS", formatMetric(location?.hrms))
            DualValueRow("HDOP", formatOptionalDecimal(location?.hdop), "VRMS", formatMetric(location?.vrms))
            DualValueRow("VDOP", formatOptionalDecimal(location?.vdop), "AGE", location?.ageSeconds?.roundToInt()?.toString() ?: "0")
            ValueRow("UTC Time", location?.utcDateTime ?: "0-00-00 00:00:00.000")
            ValueRow("Local Time", formatLocalTime(location?.utcDateTime, timeZone))
            ValueRow("Dist. to Ref", location?.distanceToReferenceMeters?.let { String.format("%.2f m", it) } ?: "None")
        }
    }
}

@Composable
private fun PositionBaseTab(
    location: DgpsLocation?,
    settings: com.rbt.survey.dgps.DgpsUiSettings
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            ValueRow("Base ID", location?.baseStationId ?: settings.base.baseId.ifBlank { "?" })
            ValueRow("B", if (location?.baseStationId.isNullOrBlank()) "?" else formatDms(location?.latitude, true))
            ValueRow("L", if (location?.baseStationId.isNullOrBlank()) "?" else formatDms(location?.longitude, false))
            ValueRow("H", if (location?.baseStationId.isNullOrBlank()) "?" else formatHeight(location?.altitude))
            ValueRow("Local Time", formatLocalTime(location?.utcDateTime, settings.deviceSettings.timeZone))
            ValueRow("Antenna Height", settings.inspectionAccuracy.antennaHeight)
            ValueRow("Dist. to Ref", location?.distanceToReferenceMeters?.let { String.format("%.2f m", it) } ?: "?")
        }
    }
}

@Composable
private fun SatelliteInfoTab(satellites: List<SatelliteInfo>) {
    if (satellites.isEmpty()) {
        EmptySatState()
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(satellites.size) { index ->
            val sat = satellites.sortedWith(compareBy<SatelliteInfo> { it.constellation.displayName }.thenBy { it.prn })[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("${sat.constellation.shortLabel}-${sat.prn}", fontWeight = FontWeight.SemiBold)
                    Text("El ${sat.elevation}°  Az ${sat.azimuth}°", color = Color.Gray, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("SNR ${sat.snr}", fontWeight = FontWeight.SemiBold)
                    Text(if (sat.usedInFix) "Used" else "Tracked", color = if (sat.usedInFix) DgpsAccent else Color.Gray)
                }
            }
            HorizontalDivider(color = DgpsBorder)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SatelliteMapTab(
    satellites: List<SatelliteInfo>,
    gnssSystems: GnssSystemSettings,
    onToggleSystem: (GnssSystemSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
            contentAlignment = Alignment.Center
        ) {
            DetailedSkyPlot(satellites)
        }

        if (satellites.isEmpty()) {
            Text(
                "No chart data available.",
                color = Color(0xFFE0B74D),
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConstellationCheckbox("BD", gnssSystems.beidou, Color(0xFF2E7D32)) {
                onToggleSystem(gnssSystems.copy(beidou = it))
            }
            ConstellationCheckbox("GPS", gnssSystems.gps, Color(0xFF90A4AE)) {
                onToggleSystem(gnssSystems.copy(gps = it))
            }
            ConstellationCheckbox("GLN", gnssSystems.glonass, Color(0xFFC56A7C)) {
                onToggleSystem(gnssSystems.copy(glonass = it))
            }
            ConstellationCheckbox("GAL", gnssSystems.galileo, Color(0xFFD569A2)) {
                onToggleSystem(gnssSystems.copy(galileo = it))
            }
            ConstellationCheckbox("SBAS", gnssSystems.sbas, Color(0xFF2E7D32)) {
                onToggleSystem(gnssSystems.copy(sbas = it))
            }
            ConstellationCheckbox("QZSS", gnssSystems.qzss, Color(0xFF2E7D32)) {
                onToggleSystem(gnssSystems.copy(qzss = it))
            }
            ConstellationCheckbox("IRNSS", gnssSystems.irnss, Color(0xFF2E7D32)) {
                onToggleSystem(gnssSystems.copy(irnss = it))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DetailedSkyPlot(satellites: List<SatelliteInfo>) {
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(320.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 12.dp.toPx()

            drawCircle(Color.Black.copy(alpha = 0.55f), radius = radius, center = center, style = Stroke(width = 1.dp.toPx()))
            drawCircle(Color.Black.copy(alpha = 0.55f), radius = radius * 0.66f, center = center, style = Stroke(width = 1.dp.toPx()))
            drawCircle(Color.Black.copy(alpha = 0.55f), radius = radius * 0.33f, center = center, style = Stroke(width = 1.dp.toPx()))

            for (degrees in listOf(0, 30, 60, 90, 120, 150, 180, 210, 240, 270, 300, 330)) {
                val angle = Math.toRadians((degrees - 90).toDouble())
                val x = center.x + (radius * cos(angle)).toFloat()
                val y = center.y + (radius * sin(angle)).toFloat()
                drawLine(
                    color = Color.Black.copy(alpha = if (degrees % 90 == 0) 0.65f else 0.45f),
                    start = center,
                    end = Offset(x, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            satellites.forEach { sat ->
                val angle = Math.toRadians((sat.azimuth - 90).toDouble())
                val distance = radius * (1f - sat.elevation / 90f)
                val x = center.x + (distance * cos(angle)).toFloat()
                val y = center.y + (distance * sin(angle)).toFloat()
                drawCircle(
                    color = constellationColor(sat.constellation),
                    radius = 7.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
        Column(
            modifier = Modifier
                .size(320.dp)
                .padding(4.dp)
        ) {
            Text("0", color = Color(0xFF1E32E0), modifier = Modifier.align(Alignment.CenterHorizontally), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("330°", color = Color(0xFF1E32E0), fontWeight = FontWeight.Bold)
                Text("30°", color = Color(0xFF1E32E0), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(44.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("300°", color = Color(0xFF1E32E0), fontWeight = FontWeight.Bold)
                Text("60°", color = Color(0xFF1E32E0), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(54.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("270°", color = Color(0xFF1E32E0), fontWeight = FontWeight.Bold)
                Text("90°", color = Color(0xFF1E32E0), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(54.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("240°", color = Color(0xFF1E32E0), fontWeight = FontWeight.Bold)
                Text("120°", color = Color(0xFF1E32E0), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(44.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("210°", color = Color(0xFF1E32E0), fontWeight = FontWeight.Bold)
                Text("150°", color = Color(0xFF1E32E0), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("180°", color = Color(0xFF1E32E0), modifier = Modifier.align(Alignment.CenterHorizontally), fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DgpsTopBar(
    title: String,
    onBack: () -> Unit
) {
    TopAppBar(
        title = { Text(title, color = Color.White) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = DgpsTopBarColor)
    )
}

@Composable
private fun DgpsFormScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        topBar = {
            DgpsTopBar(title = title, onBack = onBack)
        },
        containerColor = Color.White
    ) { padding ->
        content(Modifier.padding(padding))
    }
}

@Composable
private fun DgpsTileCard(tile: DgpsTile) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = tile.onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(20.dp),
                color = DgpsSurface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        tile.icon,
                        contentDescription = tile.title,
                        tint = DgpsAccent,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Text(
                tile.title,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun DgpsStatusHeader(
    status: DgpsStatus,
    location: DgpsLocation?,
    isNtripConnected: Boolean,
    batteryText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DgpsTopBarColor)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusIndicatorCircle(isActive = status is DgpsStatus.Connected)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = formatSolution(status, location),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Age ${location?.ageSeconds?.roundToInt() ?: 0}   V: ${formatMetric(location?.vrms)}",
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            SummaryBadge(label = "CORS", value = if (isNtripConnected) "ON" else "OFF")
            SummaryBadge(label = "SAT", value = "${location?.satellites ?: 0}")
            SummaryBadge(label = "BAT", value = batteryText)
        }
    }
}

@Composable
private fun SummaryBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatusIndicatorCircle(isActive: Boolean) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .border(2.dp, if (isActive) DgpsAccent else Color.LightGray, CircleShape)
    )
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    if (title.isNotBlank()) {
        Text(
            text = title,
            color = DgpsSection,
            fontSize = 22.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        content()
    }
}

@Composable
private fun SettingTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun DualFieldRow(
    leftLabel: String,
    leftValue: String,
    onLeftValueChange: (String) -> Unit,
    rightLabel: String,
    rightValue: String,
    onRightValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = leftValue,
            onValueChange = onLeftValueChange,
            label = { Text(leftLabel) },
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = rightValue,
            onValueChange = onRightValueChange,
            label = { Text(rightLabel) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp)
            if (subtitle != null) {
                Text(subtitle, color = Color.Gray, fontSize = 13.sp)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun BottomActionRow(
    leftText: String,
    centerText: String,
    rightText: String,
    onLeftClick: () -> Unit,
    onCenterClick: () -> Unit,
    onRightClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionButton(leftText, onLeftClick, Modifier.weight(1f), Icons.Default.Share)
        ActionButton(centerText, onCenterClick, Modifier.weight(1f), Icons.Default.Save)
        ActionButton(rightText, onRightClick, Modifier.weight(1f), Icons.Default.NetworkCheck)
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = DgpsAccent)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text)
    }
}

@Composable
private fun ValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 18.sp)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
    HorizontalDivider(color = DgpsBorder)
}

@Composable
private fun DualValueRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(leftLabel, fontSize = 18.sp)
            Text(leftValue, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(rightLabel, fontSize = 18.sp)
            Text(rightValue, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
    HorizontalDivider(color = DgpsBorder)
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 18.sp)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DualInfoRow(left: String, right: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(left, fontSize = 18.sp)
        Text(right, fontSize = 18.sp)
    }
}

@Composable
private fun EmptySatState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("No satellite data available.", color = Color.Gray)
    }
}

@Composable
private fun ConstellationCheckbox(
    label: String,
    checked: Boolean,
    color: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, color = color, fontSize = 16.sp)
    }
}

@Composable
private fun CompassGraphic() {
    Canvas(modifier = Modifier.size(180.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2.5f
        drawCircle(Color.LightGray, radius = radius, center = center, style = Stroke(width = 2.dp.toPx()))
        drawLine(Color.LightGray, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), strokeWidth = 2.dp.toPx())
        drawLine(Color.LightGray, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), strokeWidth = 2.dp.toPx())
    }
}

private fun filterSatellites(
    satellites: List<SatelliteInfo>,
    settings: GnssSystemSettings
): List<SatelliteInfo> {
    return satellites.filter { satellite ->
        when (satellite.constellation) {
            SatelliteConstellation.BEIDOU -> settings.beidou
            SatelliteConstellation.GPS -> settings.gps
            SatelliteConstellation.GLONASS -> settings.glonass
            SatelliteConstellation.GALILEO -> settings.galileo
            SatelliteConstellation.SBAS -> settings.sbas
            SatelliteConstellation.QZSS -> settings.qzss
            SatelliteConstellation.IRNSS -> settings.irnss
            SatelliteConstellation.UNKNOWN -> true
        }
    }
}

private fun constellationColor(constellation: SatelliteConstellation): Color {
    return when (constellation) {
        SatelliteConstellation.BEIDOU -> Color(0xFF2E7D32)
        SatelliteConstellation.GPS -> Color(0xFF607D8B)
        SatelliteConstellation.GLONASS -> Color(0xFFC56A7C)
        SatelliteConstellation.GALILEO -> Color(0xFFD569A2)
        SatelliteConstellation.SBAS -> Color(0xFF388E3C)
        SatelliteConstellation.QZSS -> Color(0xFF1B5E20)
        SatelliteConstellation.IRNSS -> Color(0xFF00695C)
        SatelliteConstellation.UNKNOWN -> DgpsAccent
    }
}

private fun formatSolution(status: DgpsStatus, location: DgpsLocation?): String {
    return when {
        status is DgpsStatus.Connected && location != null -> getFixQualityString(location.fixQuality)
        status is DgpsStatus.Connecting -> "Connecting"
        status is DgpsStatus.Error -> "No Solution"
        else -> "No Solution"
    }
}

private fun formatDms(value: Double?, isLatitude: Boolean): String {
    if (value == null) {
        return if (isLatitude) "0°00'00.0000\"N" else "0°00'00.0000\"E"
    }
    val direction = when {
        isLatitude && value < 0 -> "S"
        isLatitude -> "N"
        value < 0 -> "W"
        else -> "E"
    }
    val absValue = kotlin.math.abs(value)
    val degrees = absValue.toInt()
    val minutesFull = (absValue - degrees) * 60
    val minutes = minutesFull.toInt()
    val seconds = (minutesFull - minutes) * 60
    return String.format("%d°%02d'%07.4f\"%s", degrees, minutes, seconds, direction)
}

private fun formatHeight(value: Double?): String {
    return if (value == null) "-1.892m" else String.format("%.3fm", value)
}

private fun formatSpeed(value: Float?): String {
    return String.format("%.3fm/s", value ?: 0f)
}

private fun formatHeading(value: Float?): String {
    return String.format("%.2f°", value ?: 0f)
}

private fun formatMetric(value: Float?): String {
    return if (value == null) "NA" else String.format("%.3f", value)
}

private fun formatOptionalDecimal(value: Float?): String {
    return value?.let { String.format("%.3f", it) } ?: "NA"
}

private fun formatLocalTime(utcDateTime: String?, timeZone: String): String {
    if (utcDateTime.isNullOrBlank()) return "2-11-29 19:00:00.000"
    return try {
        val targetZone = parseZoneId(timeZone)
        if (utcDateTime.contains("-")) {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val utc = LocalDateTime.parse(utcDateTime, formatter)
                .atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(targetZone)
            utc.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } else {
            utcDateTime
        }
    } catch (_: Exception) {
        utcDateTime
    }
}

private fun parseZoneId(zoneText: String): ZoneId {
    return try {
        val offset = zoneText.removePrefix("UTC")
        ZoneOffset.of(offset)
    } catch (_: Exception) {
        ZoneId.systemDefault()
    }
}

private fun shareText(context: android.content.Context, title: String, body: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

private fun buildRoverShareText(settings: RoverSettings): String = buildString {
    appendLine("Rover Mode Settings")
    appendLine("Cut-Off Angle: ${settings.cutOffAngle}")
    appendLine("Diff Delay: ${settings.diffDelay}")
    appendLine("Datalink: ${settings.datalink}")
    appendLine("Connecting Mode: ${settings.connectingMode}")
    appendLine("Host: ${settings.host}")
    appendLine("Port: ${settings.port}")
    appendLine("Mountpoint: ${settings.mountpoint}")
    appendLine("User: ${settings.user}")
    appendLine("PPP Type: ${settings.pppType}")
}

private fun buildBaseShareText(settings: BaseSettings): String = buildString {
    appendLine("Base Mode Settings")
    appendLine("Base ID: ${settings.baseId}")
    appendLine("Diff Format: ${settings.diffFormat}")
    appendLine("Cut-Off Angle: ${settings.cutOffAngle}")
    appendLine("PDOP: ${settings.pdop}")
    appendLine("Datalink: ${settings.datalink}")
    appendLine("Channel: ${settings.channel}")
    appendLine("Protocol: ${settings.protocol}")
    appendLine("Port: ${settings.ntripPort}")
    appendLine("Access Point: ${settings.baseAccessPoint}")
}
