package com.rbt.survey.ui.dgps

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rbt.survey.dgps.SatelliteInfo
import com.rbt.survey.dgps.getFixQualityString
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatelliteViewScreen(
    viewModel: DgpsViewModel,
    onBack: () -> Unit
) {
    val location by viewModel.dgpsLocation.collectAsState()
    val status by viewModel.dgpsStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Satellite Status") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Top Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusItem("Satellites", "${location?.satellites ?: 0}")
                        StatusItem("Fix Quality", getFixQualityString(location?.fixQuality ?: 0))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusItem("HRMS", String.format("%.3f m", location?.hrms ?: 0f))
                        StatusItem("VRMS", String.format("%.3f m", location?.vrms ?: 0f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusItem("Accuracy", String.format("%.3f m", location?.accuracy ?: 0f))
                        StatusItem("Altitude", String.format("%.2f m", location?.altitude ?: 0.0))
                    }
                }
            }

            // Skyplot and List
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                // Skyplot
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Skyplot(location?.satellitesList ?: emptyList())
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Satellite List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(location?.satellitesList ?: emptyList()) { sat ->
                        SatelliteListItem(sat)
                    }
                }
            }
        }
    }
}

@Composable
fun Skyplot(satellites: List<SatelliteInfo>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 2f

        // Draw circles for elevation
        drawCircle(
            color = Color.Gray.copy(alpha = 0.3f),
            radius = radius,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = Color.Gray.copy(alpha = 0.3f),
            radius = radius * 0.66f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = Color.Gray.copy(alpha = 0.3f),
            radius = radius * 0.33f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Draw cross lines
        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = Offset(center.x, 0f),
            end = Offset(center.x, size.height),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = Offset(0f, center.y),
            end = Offset(size.width, center.y),
            strokeWidth = 1.dp.toPx()
        )

        // Draw satellites
        satellites.forEach { sat ->
            // Convert polar (elevation, azimuth) to Cartesian
            // Azimuth is clockwise from North (0 deg)
            // Elevation is 0 (horizon) to 90 (zenith)
            val angleRad = Math.toRadians((sat.azimuth - 90).toDouble())
            val r = radius * (1.0 - sat.elevation / 90.0)
            
            val x = center.x + (r * cos(angleRad)).toFloat()
            val y = center.y + (r * sin(angleRad)).toFloat()

            drawCircle(
                color = if (sat.snr > 30) Color(0xFF4CAF50) else if (sat.snr > 15) Color(0xFFFFC107) else Color(0xFFF44336),
                radius = 6.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun SatelliteListItem(sat: SatelliteInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${sat.prn}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SNR: ${sat.snr}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = (sat.snr / 50f).coerceIn(0f, 1f),
                    modifier = Modifier
                        .height(4.dp)
                        .weight(1f),
                    color = if (sat.snr > 30) Color(0xFF4CAF50) else if (sat.snr > 15) Color(0xFFFFC107) else Color(0xFFF44336),
                    trackColor = Color.LightGray.copy(alpha = 0.3f)
                )
            }
            Text(
                text = "El: ${sat.elevation}° Az: ${sat.azimuth}°",
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun StatusItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

