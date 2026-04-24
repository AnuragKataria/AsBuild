package com.rbt.survey.ui.form

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rbt.survey.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    type: String,
    fieldId: String,
    initialValue: String,
    onBack: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val context = LocalContext.current
    val gson = Gson()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val isMultiPointType = type.lowercase().contains("polygon") || type.lowercase().contains("polyline")

    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    var showMapTypeDialog by remember { mutableStateOf(false) }

    // Parse initial value
    val initialPoints = remember {
        try {
            if (isMultiPointType) {
                val listType = object : TypeToken<List<LatLng>>() {}.type
                gson.fromJson<List<LatLng>>(initialValue, listType) ?: emptyList()
            } else if (initialValue.contains(",")) {
                val parts = initialValue.split(",")
                listOf(LatLng(parts[0].toDouble(), parts[1].toDouble()))
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    var points by remember { mutableStateOf(initialPoints) }
    var isTracking by remember { mutableStateOf(false) }
    var currentAccuracy by remember { mutableStateOf<Float?>(null) }
    var hasCenteredInitially by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(points.firstOrNull() ?: LatLng(20.5937, 78.9629), 12f)
    }

    // Permission handling
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
    }

    // Location Tracking and Accuracy Logic
    DisposableEffect(hasLocationPermission, isTracking) {
        if (hasLocationPermission) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L).build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        currentAccuracy = location.accuracy
                        if (isTracking) {
                            val newLatLng = LatLng(location.latitude, location.longitude)
                            if (points.isEmpty() || distanceBetween(points.last(), newLatLng) > 2) {
                                points = points + newLatLng
                            }
                        }
                    }
                }
            }

            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                if (isTracking) isTracking = false
            }

            onDispose {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        } else {
            onDispose {}
        }
    }

    // Auto-center once on current location if points are empty and accuracy is acquired
    LaunchedEffect(currentAccuracy) {
        if (!hasCenteredInitially && points.isEmpty() && currentAccuracy != null) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(it.latitude, it.longitude), 16f)
                    hasCenteredInitially = true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(when {
                        type.lowercase().contains("polygon") -> "Map Polygon"
                        type.lowercase().contains("polyline") -> "Map Line"
                        else -> "Map Point"
                    })
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val resultValue = if (isMultiPointType) {
                            gson.toJson(points)
                        } else {
                            points.firstOrNull()?.let { "${it.latitude},${it.longitude}" } ?: ""
                        }
                        onSave(fieldId, resultValue)
                    }) {
                        Text("Save", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission,mapType = mapType),
                uiSettings = MapUiSettings(myLocationButtonEnabled = true),
                onMapClick = { latLng ->
                    if (!isTracking) {
                        if (isMultiPointType) {
                            points = points + latLng
                        } else {
                            points = listOf(latLng)
                        }
                    }
                }
            ) {
                if (type.lowercase().contains("polygon")) {
                    points.forEach { point ->
                        Marker(state = MarkerState(position = point))
                    }
                    if (points.size >= 2) {
                        Polygon(
                            points = points,
                            fillColor = Color.Blue.copy(alpha = 0.2f),
                            strokeColor = Color.Blue,
                            strokeWidth = 5f
                        )
                    }
                } else if (type.lowercase().contains("polyline")) {
                    points.forEach { point -> Marker(state = MarkerState(position = point)) }
                    if (points.size >= 2) {
                        Polyline(
                            points = points,
                            color = Color.Blue,
                            width = 10f
                        )
                    }
                } else {
                    points.firstOrNull()?.let { point ->
                        Marker(state = MarkerState(position = point))
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 65.dp, end = 10.dp), // position under location button
                contentAlignment = Alignment.TopEnd
            ) {
                FloatingActionButton(
                    onClick = { showMapTypeDialog = true },
                    modifier = Modifier.size(45.dp),
                    containerColor = Color.White,
                    contentColor = Color.Black
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Map Type"
                    )
                }
                if (showMapTypeDialog) {
                    androidx.compose.ui.window.Dialog(
                        onDismissRequest = { showMapTypeDialog = false }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White
                        ) {

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .padding(16.dp)
                            ) {

                                // Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Map Type",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.Black
                                    )

                                    IconButton(onClick = { showMapTypeDialog = false }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            tint = Color.Black
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                MapTypeItem("Normal", R.drawable.defaultmap) {
                                    mapType = MapType.NORMAL
                                    showMapTypeDialog = false
                                }

                                MapTypeItem("Satellite", R.drawable.satellite) {
                                    mapType = MapType.SATELLITE
                                    showMapTypeDialog = false
                                }

                                MapTypeItem("Terrain", R.drawable.terrain) {
                                    mapType = MapType.TERRAIN
                                    showMapTypeDialog = false
                                }

                                MapTypeItem("Hybrid", R.drawable.hybrid) {
                                    mapType = MapType.HYBRID
                                    showMapTypeDialog = false
                                }
                            }
                        }
                    }
                }
            }

            // Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isMultiPointType) {
                    FloatingActionButton(
                        onClick = {
                            if (!hasLocationPermission) {
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            } else {
                                isTracking = !isTracking
                            }
                        },
                        containerColor = if (isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Tracking",
                            tint = Color.White
                        )
                    }
                }

                if (!isTracking) {
                    if (points.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = { points = points.dropLast(1) },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(Icons.Default.Undo, "Undo")
                        }
                    }

                    FloatingActionButton(
                        onClick = { points = emptyList() },
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Icon(Icons.Default.Delete, "Clear")
                    }
                }
            }

            // Accuracy and Tracking Status Indicator
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Accuracy Badge
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp).copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = if (currentAccuracy != null && currentAccuracy!! < 10) Color(0xFF4CAF50) else Color(0xFFFF9800),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Accuracy: ${currentAccuracy?.let { String.format("%.1f m", it) } ?: "..."}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }

                if (isTracking) {
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Auto-Tracking Active", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapTypeItem(
    title: String,
    imageRes: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Image(
            painter = painterResource(id = imageRes),
            contentDescription = title,
            modifier = Modifier.size(50.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            color = Color.Black
        )
    }
}

private fun distanceBetween(p1: LatLng, p2: LatLng): Float {
    val results = FloatArray(1)
    android.location.Location.distanceBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude, results)
    return results[0]
}
