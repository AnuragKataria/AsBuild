package com.rbt.survey.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.rbt.survey.data.model.GpItem
import com.rbt.survey.data.model.GpStatus
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.rbt.survey.R
import com.rbt.survey.data.helper.LocationHelper
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    formId: Int,
    blockCode: String?,
    gpStatusList: List<GpItem>,
    surveyRadius: Int?,
    onBack: () -> Unit,
    onMarkerClick: (Int, String?,String?) -> Unit
) {

    val gpList by viewModel.gpList.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(20.5937, 78.9629),
            4f
        )
    }

    val mapType by viewModel.mapType.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val locationHelper = remember { LocationHelper(context) }
    val coroutineScope = rememberCoroutineScope()
    var isCheckingLocation by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf("Please wait...") }
    val bypassValidation by viewModel.bypassValidation.collectAsState()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            Toast.makeText(
                context,
                "Location permission is required to show your location",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Load data
    LaunchedEffect(formId, blockCode) {
        viewModel.setCompletedGpList(gpStatusList)
        viewModel.loadGpLocations(formId, blockCode)
    }

    // Move camera to first GP
    LaunchedEffect(gpList) {
        if (gpList.isNotEmpty()) {
            val first = gpList.first()
            cameraPositionState.position =
                CameraPosition.fromLatLngZoom(
                    LatLng(first.lat, first.lng),
                    10f
                )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("GP Points") },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->

        val context = androidx.compose.ui.platform.LocalContext.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            //  MAP
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(mapType = mapType,isMyLocationEnabled = hasPermission)
            ) {

                gpList.forEach { gp ->

                    val markerBitmap = createLabeledSquareMarker(
                        text = gp.name,
                        squareColor = if (gp.isCompleted)
                            android.graphics.Color.GREEN
                        else
                            android.graphics.Color.RED,
                        squareSize = 30,
                        textSizeSp = 12f,
                        textColor = android.graphics.Color.RED,
                        context = context
                    )

                    Marker(
                        state = MarkerState(
                            position = LatLng(gp.lat, gp.lng)
                        ),
                        icon = BitmapDescriptorFactory.fromBitmap(markerBitmap),
                        onClick = {

                            if (gp.isCompleted) {
                                Toast.makeText(
                                    context,
                                    "You already filled the form for this GP",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Marker true
                            }

                            //this is for testing we have to remove later on production
                            if (bypassValidation) {
                                onMarkerClick(formId, blockCode, gp.name)
                                return@Marker true
                            }

                            coroutineScope.launch {

                                isCheckingLocation = true
                                try {
                                    loadingMessage = "Checking GPS..."
                                    //  Check GPS enabled
                                    if (!locationHelper.isLocationEnabled()) {
                                        Toast.makeText(
                                            context,
                                            "Please enable location",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@launch
                                    }

                                    loadingMessage = "Fetching current location..."
                                    //  Get current location
                                    val location = locationHelper.getCurrentLocation()

                                    if (location == null) {
                                        Toast.makeText(
                                            context,
                                            "Unable to fetch current location",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@launch
                                    }

                                    loadingMessage = "Calculating distance..."
                                    //  Check radius
                                    val isInside = viewModel.isInsideRadius(
                                        userLat = location.latitude,
                                        userLng = location.longitude,
                                        gpLat = gp.lat,
                                        gpLng = gp.lng,
                                        radius = surveyRadius ?: 0
                                    )

                                    if (isInside) {
                                        // allow
                                        onMarkerClick(formId, blockCode, gp.name)
                                    } else {
                                        // block
                                        Toast.makeText(
                                            context,
                                            "You are outside survey area",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }finally {
                                    isCheckingLocation = false
                                    loadingMessage = "Please wait..."
                                }
                            }
                            true
                        }
                    )
                }
            }

            // ✅ LOADING ON TOP OF MAP
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomEnd
            ) {
                // 🔘 Floating Button
                FloatingActionButton(
                    onClick = { expanded = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 10.dp)
                        .size(45.dp),
                    containerColor = androidx.compose.ui.graphics.Color.White,
                    contentColor = androidx.compose.ui.graphics.Color.Black
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Map Type"
                    )
                }

                if (expanded) {
                    androidx.compose.ui.window.Dialog(
                        onDismissRequest = { expanded = false }
                    ) {
                        androidx.compose.material3.Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            color = androidx.compose.ui.graphics.Color.White
                        ) {

                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .width(300.dp)
                            ) {

                                // 🔝 HEADER (Title + Close)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Text(
                                        text = "Map Type",
                                        style = MaterialTheme.typography.titleLarge,
                                                color = androidx.compose.ui.graphics.Color.Black
                                    )

                                    IconButton(onClick = { expanded = false }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = androidx.compose.ui.graphics.Color.Black
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // 🗺️ OPTIONS
                                MapTypeItem(
                                    title = "Normal",
                                    imageRes = R.drawable.defaultmap
                                ) {
                                    viewModel.setMapType(MapType.NORMAL)
                                    expanded = false
                                }

                                MapTypeItem(
                                    title = "Satellite",
                                    imageRes = R.drawable.satellite
                                ) {
                                    viewModel.setMapType(MapType.SATELLITE)
                                    expanded = false
                                }

                                MapTypeItem(
                                    title = "Terrain",
                                    imageRes = R.drawable.terrain
                                ) {
                                    viewModel.setMapType(MapType.TERRAIN)
                                    expanded = false
                                }

                                MapTypeItem(
                                    title = "Hybrid",
                                    imageRes = R.drawable.hybrid
                                ) {
                                    viewModel.setMapType(MapType.HYBRID)
                                    expanded = false
                                }
                            }
                        }
                    }
                }
            }
            if (isCheckingLocation) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        CircularProgressIndicator()

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = loadingMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            color = androidx.compose.ui.graphics.Color.White,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    androidx.compose.material3.Checkbox(
                        checked = bypassValidation,
                        onCheckedChange = { viewModel.setBypassValidation(it) }
                    )

                    Text(
                        text = "Test Mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.Black
                    )
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
            modifier = Modifier
                .size(50.dp)
        )

        Spacer(modifier = Modifier.width(20.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = androidx.compose.ui.graphics.Color.Black
        )
    }
}

fun createLabeledSquareMarker(
    text: String,
    squareColor: Int,
    squareSize: Int,
    textSizeSp: Float,
    textColor: Int,
    context: android.content.Context
): android.graphics.Bitmap {

    val density = context.resources.displayMetrics

    val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_SP,
            textSizeSp,
            density
        )
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    val textBounds = android.graphics.Rect()
    textPaint.getTextBounds(text, 0, text.length, textBounds)

    val textWidth = textPaint.measureText(text)
    val textHeight = textBounds.height().toFloat()

    val horizontalPadding = 20
    val verticalPadding = 10

    val bitmapWidth = (textWidth + horizontalPadding * 2).toInt()
    val bitmapHeight = (textHeight + verticalPadding + squareSize + verticalPadding).toInt()

    val bitmap = android.graphics.Bitmap.createBitmap(
        bitmapWidth,
        bitmapHeight,
        android.graphics.Bitmap.Config.ARGB_8888
    )

    val canvas = android.graphics.Canvas(bitmap)

    // Draw TEXT (top)
    val textX = bitmapWidth / 2f
    val textY = textHeight + verticalPadding
    canvas.drawText(text, textX, textY, textPaint)

    // Draw RED SQUARE (below text)
    val squarePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = squareColor
    }

    val squareLeft = (bitmapWidth - squareSize) / 2f
    val squareTop = textY + verticalPadding
    val squareRight = squareLeft + squareSize
    val squareBottom = squareTop + squareSize

    canvas.drawRect(squareLeft, squareTop, squareRight, squareBottom, squarePaint)

    return bitmap
}
