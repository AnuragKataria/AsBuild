package com.rbt.survey.ui.inventory

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberMarkerState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.maps.android.compose.Polyline
import com.rbt.survey.data.model.*
import kotlin.text.clear
import android.util.Log
import android.util.TypedValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.compose.ui.window.Dialog
import coil.ImageLoader
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.clustering.Clustering
import com.rbt.survey.R
import com.rbt.survey.data.local.UserPreferences
import com.rbt.survey.data.remote.RetrofitClient
import com.rbt.survey.ui.map.MapTypeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.floor
import org.json.JSONArray
import org.json.JSONObject

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun InventoryMapScreen(
    onBackClick: () -> Unit,
    viewModel: InventoryMapViewModel
) {

    val context = LocalContext.current
    val base_URL = "https://webgis.rbt-ltd.com"
    val cameraPositionState = rememberCameraPositionState()
    val imageLoader = remember { ImageLoader.Builder(context).okHttpClient(RetrofitClient.getBasicUnsafeOkHttpClient(context)).build() }

    val projects by viewModel.projects.collectAsState()
    val connectivityRules by viewModel.connectivityRules.collectAsState()
    val createdAssets by viewModel.createdAssets.collectAsState()
    val createdAssetDetail by viewModel.createdAssetDetails.collectAsState()
    val allassetDetails by viewModel.allassetDetails.collectAsState()
    val assetDetails by viewModel.assetDetails.collectAsState()
    val assetConfig by viewModel.assetConfig.collectAsState()
    val saveMessage by viewModel.saveMessage.collectAsState()
    val mapType by viewModel.mapType.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val fmsUtilization by viewModel.fmsutilization.collectAsState()
    val fibercoreStructure by viewModel.fibercorestructure.collectAsState()
    val fibercoreUtilization by viewModel.fibercoreutilization.collectAsState()
    val terminationResult by viewModel.terminationResult.collectAsState()

    var selectedProject by remember { mutableStateOf<ProjectResponse?>(null) }
    var selectedAsset by remember { mutableStateOf<AssetDetailResponse?>(null) }
    var parentAssetTypeId by remember { mutableStateOf<Int?>(null) }
    var isFilterExpanded by remember {
        mutableStateOf(false)
    }

    var isSidePanelOpen by remember { mutableStateOf(false) }
    var mapexpanded by remember { mutableStateOf(false) }

    val projectNames = projects.map { it.projectName }

    val isAddAssetEnabled = selectedProject != null

    var showAssetDialog by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.collectAsState()
    val loadingMessage by viewModel.isLoadingmessage.collectAsState()
    val isAssetLoading by viewModel.isAssetLoading.collectAsState()

    val createdAssetMarkers = remember { mutableStateListOf<Pair<LatLng, CreatedAssetData>>() }
    val createdAssetPolylines = remember { mutableStateListOf<Pair<List<LatLng>, CreatedAssetData>>() }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val allClusterItems = remember { mutableStateListOf<AssetClusterItem>() }
    val assetsByLocation = remember { mutableStateMapOf<LocationKey, List<CreatedAssetData>>() }
    var showLocationAssetsDialog by remember { mutableStateOf(false) }
    var sameLocationAssets by remember { mutableStateOf<List<CreatedAssetData>>(emptyList()) }

    val visibleClusterItems = remember { mutableStateListOf<AssetClusterItem>() }
    var mapLoading by remember { mutableStateOf(false) }

    var showTerminationDialog by remember { mutableStateOf(false) }
    var selectedLocationAssets by remember { mutableStateOf<List<CreatedAssetData>>(emptyList()) }
    var userEmail by remember { mutableStateOf("") }


    var assetName by remember { mutableStateOf("") }
    var assetCode by remember { mutableStateOf("") }
    var assetCategory by remember { mutableStateOf("") }
    var geometryType by remember { mutableStateOf("") }
    var assetDetailGeometryType by remember { mutableStateOf("") }
    var imageurl by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var fieldcount by remember { mutableStateOf(0) }
    var assetDescription by remember { mutableStateOf("") }

    var selectedStatus by remember { mutableStateOf("Planned") }
    var installDate by remember { mutableStateOf("") }
    var isDrawingMode by remember { mutableStateOf(false) }
    var geometrySaved by remember { mutableStateOf(false) }
    var capturedPoint by remember { mutableStateOf<LatLng?>(null) }
    val linePoints = remember { mutableStateListOf<LatLng>() }
    var currentStep by remember { mutableIntStateOf(1) }

    val attributeValues = remember { mutableStateMapOf<String, Any?>() }
    val dropdownSnapshots = remember { mutableStateMapOf<String, DropdownSnapshot>() }
    val fieldErrors = remember { mutableStateMapOf<String, Boolean>() }
    val dynamicData = attributeValues.toMap()

    var assetNameError by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf(false) }

    var selectedMarkerAsset by remember { mutableStateOf<CreatedAssetData?>(null) }
    var markerAssetDialog by remember { mutableStateOf(false) }

    val clearAssetForm = {
        selectedAsset = null
        isSidePanelOpen = false
        currentStep = 1
        assetName = ""
        assetCode = ""
        assetCategory = ""
        geometryType = ""
        imageurl = ""
        status = ""
        assetDescription = ""
        installDate = ""
        capturedPoint = null
        linePoints.clear()
        geometrySaved = false
        isDrawingMode = false
        assetNameError = false
        locationError = false
        attributeValues.clear()
        dropdownSnapshots.clear()
    }

    LaunchedEffect(Unit) {
        userEmail = UserPreferences(context)
            .userEmail
            .first() ?: ""
    }

    LaunchedEffect(Unit) {
        viewModel.loadProjects()
        viewModel.loadConnectivityRules()
    }

    LaunchedEffect(assetConfig) {
        fieldcount = assetConfig?.data?.currentVersion?.schema?.fields?.size ?: 0
    }

    LaunchedEffect(assetDetails) {
        assetDetailGeometryType = assetDetails?.data?.geometryType.toString()
        imageurl = base_URL + assetDetails?.data?.imageUrl.toString()
    }

    LaunchedEffect(selectedAsset) {
        selectedAsset?.data?.let { asset ->
            assetName = asset.assetName
            assetCode = asset.assetCode
            assetCategory = asset.assetCategory
            geometryType = asset.geometryType
            imageurl = base_URL + asset.imageUrl.toString()
            status = if (asset.isActive) "Active" else "Deactive"
        }
    }

    LaunchedEffect(Unit) {

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            val locationRequest = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()

            fusedLocationClient.getCurrentLocation(
                locationRequest,
                null
            ).addOnSuccessListener { location ->

                location?.let {
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(it.latitude, it.longitude),
                            18f
                        )
                    )
                }
            }
        }
    }

    LaunchedEffect(saveMessage) {
        saveMessage?.let { message ->
            Toast.makeText(
                context,
                message,
                Toast.LENGTH_SHORT
            ).show()
            if (message == "Asset saved successfully") {
                clearAssetForm()
            }
            viewModel.resetSaveMessage()
        }
    }

    LaunchedEffect(createdAssets) {

        val result = withContext(Dispatchers.Default) {

            val pointItems = mutableListOf<AssetClusterItem>()

            val polylines =
                mutableListOf<Pair<List<LatLng>, CreatedAssetData>>()

            val groupedAssets =
                mutableMapOf<LocationKey, MutableList<CreatedAssetData>>()

            createdAssets.forEach { asset ->

                val wkt = asset.data
                    ?.get("wkt")
                    ?.takeIf { !it.isJsonNull }
                    ?.asString
                    ?.trim()
                    ?: return@forEach

                when {

                    wkt.startsWith("POINT", true) -> {

                        runCatching {

                            val coords = wkt
                                .substringAfter("(")
                                .substringBefore(")")
                                .trim()
                                .split(Regex("\\s+"))

                            if (coords.size >= 2) {

                                pointItems.add(
                                    AssetClusterItem(
                                        latLng = LatLng(
                                            coords[1].toDouble(),
                                            coords[0].toDouble()
                                        ),
                                        asset = asset
                                    )
                                )

                                val key = LocationKey(
                                    lat = truncateTo5Decimals(coords[1].toDouble()),
                                    lng = truncateTo5Decimals(coords[0].toDouble())
                                )

                                groupedAssets
                                    .getOrPut(key) { mutableListOf() }
                                    .add(asset)
                            }
                        }
                    }

                    wkt.startsWith("LINESTRING", true) -> {

                        runCatching {

                            val latLngs = wkt
                                .substringAfter("(")
                                .substringBefore(")")
                                .split(",")
                                .mapNotNull { point ->

                                    val coords = point
                                        .trim()
                                        .split(Regex("\\s+"))

                                    if (coords.size >= 2) {

                                        LatLng(
                                            coords[1].toDouble(),
                                            coords[0].toDouble()
                                        )

                                    } else {
                                        null
                                    }
                                }

                            if (latLngs.isNotEmpty()) {

                                val startPoint = latLngs.first()

                                val key = LocationKey(
                                    lat = truncateTo5Decimals(startPoint.latitude),
                                    lng = truncateTo5Decimals(startPoint.longitude)
                                )

                                groupedAssets
                                    .getOrPut(key) { mutableListOf() }
                                    .add(asset)

                                polylines.add(
                                    Pair(
                                        latLngs,
                                        asset
                                    )
                                )
                            }
                        }
                    }
                }
            }

            val finalGroupedAssets =
                groupedAssets.mapValues { it.value.toList() }

            Triple(
                pointItems,
                polylines,
                finalGroupedAssets
            )
        }

        allClusterItems.clear()
        allClusterItems.addAll(result.first)

        createdAssetPolylines.clear()
        createdAssetPolylines.addAll(result.second)

        assetsByLocation.clear()
        assetsByLocation.putAll(result.third)
    }

    LaunchedEffect(cameraPositionState, allClusterItems.size) {

        snapshotFlow {
            cameraPositionState.isMoving
        }.collectLatest { moving ->

            if (moving) return@collectLatest

            mapLoading = true

            delay(500)

            val projection =
                cameraPositionState.projection
                    ?: return@collectLatest

            val bounds =
                projection.visibleRegion.latLngBounds

            val filtered =
                withContext(Dispatchers.Default) {

                    allClusterItems.filter {

                        bounds.contains(
                            it.position
                        )
                    }
                }

            visibleClusterItems.clear()
            visibleClusterItems.addAll(filtered)

            Log.d(
                "MAP_DEBUG",
                "Visible Items = ${filtered.size}"
            )

            mapLoading = false
        }
    }

    LaunchedEffect(terminationResult) {

        terminationResult?.let { result ->

            if (result == "SUCCESS") {

                Toast.makeText(
                    context,
                    "Termination Created Successfully",
                    Toast.LENGTH_SHORT
                ).show()

                showTerminationDialog = false

            } else {

                Toast.makeText(
                    context,
                    result,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Asset Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        },
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            Text(
                text = if (isFilterExpanded) "Click to close" else "Click to open",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .clickable {
                        isFilterExpanded = !isFilterExpanded
                    }
                    .padding(vertical = 5.dp),
                textAlign = TextAlign.Center,
                color = Color.Black,
            )

            AnimatedVisibility(
                visible = isFilterExpanded
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(12.dp)
                ) {

                    CustomDropdown(
                        selectedValue = selectedProject?.projectName ?: "",
                        placeholder = "Select Project",
                        items = projectNames,
                        onValueSelected = { projectName ->

                            isFilterExpanded = false

                            selectedProject = projects.find {
                                it.projectName == projectName
                            }

                            createdAssetMarkers.clear()
                            createdAssetPolylines.clear()
                            clearAssetForm()

                            selectedProject?.projectId?.let {
                                viewModel.loadCreatedAssets(it)
                            }
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        mapType = mapType,
                        isMyLocationEnabled = true
                    ),
                    uiSettings = MapUiSettings(
                        myLocationButtonEnabled = true
                    ),
                    onMapClick = { latLng ->

                        if (!isDrawingMode) return@GoogleMap

                        when (geometryType) {

                            "POINT" -> {
                                capturedPoint = latLng
                            }

                            "LINE" -> {
                                linePoints.add(latLng)
                            }
                        }
                    }
                ) {
                    capturedPoint?.let { point ->

                        val markerState = rememberMarkerState(
                            position = point
                        )

                        Marker(
                            state = markerState
                        )
                    }

                    linePoints.forEach { point ->
                        Marker(
                            state = rememberMarkerState(position = point)
                        )
                    }

                    if (linePoints.size >= 2) {

                        Polyline(
                            points = linePoints.toList(),
                            color = Color.Blue,
                            width = 8f
                        )
                    }
                    Clustering(
                        items = visibleClusterItems,
                        clusterItemContent = { item ->
                            val key = LocationKey(
                                lat = truncateTo5Decimals(item.position.latitude),
                                lng = truncateTo5Decimals(item.position.longitude)
                            )

                            val assets = assetsByLocation[key].orEmpty()

                            val label = assets
                                .mapNotNull {
                                    it.data?.get("assetName")?.asString
                                }
                                .distinct()
                                .joinToString("/")

                            val isElectricPole =
                                item.asset.assetTypeId == 43

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {

                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                if (isElectricPole) {

                                    Image(
                                        painter = painterResource(
                                            R.drawable.electric_pole
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(35.dp)
                                    )

                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(Color.Red)
                                    )
                                }
                            }
                        },
                        onClusterItemClick = { item ->

                            val key = LocationKey(
                                lat = truncateTo5Decimals(item.position.latitude),
                                lng = truncateTo5Decimals(item.position.longitude)
                            )

                            val assets =
                                assetsByLocation[key].orEmpty()

                            if (assets.size <= 1) {
                                viewModel.loadAssetDetails(item.asset.assetTypeId)
                                viewModel.loadCreatedAssetDetails(item.asset.assetId)
                                selectedMarkerAsset = item.asset
                                markerAssetDialog = true

                            } else {
                                sameLocationAssets = assets
                                showLocationAssetsDialog = true
                            }

                            true
                        }
                    )
                    if (cameraPositionState.position.zoom >= 14f) {

                        createdAssetPolylines.forEach { (line, asset) ->

                            Polyline(
                                points = line,
                                width = 8f,
                                color = Color.Blue,
                                clickable = true,
                                onClick = {
                                    viewModel.loadAssetDetails(asset.assetTypeId)
                                    viewModel.loadCreatedAssetDetails(asset.assetId)
                                    selectedMarkerAsset = asset
                                    markerAssetDialog = true
                                }
                            )
                        }
                    }
                }

                if (mapLoading) {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color.Black.copy(alpha = 0.3f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                FloatingActionButton(
                    onClick = {
                        if (isAddAssetEnabled) {
                            showAssetDialog = true
                            viewModel.loadAssets()
                        }else{
                            Toast.makeText(context, "First Select Project", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(
                            top = 55.dp,
                            end = 10.dp
                        )
                        .size(43.dp),
                    containerColor = if (isAddAssetEnabled)
                        Color(0xFFC79AF8)
                    else
                        Color.LightGray
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Asset",
                        tint = if (isAddAssetEnabled)
                            Color.White
                        else
                            Color.DarkGray
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible =
                        isDrawingMode &&
                                (
                                        (geometryType == "POINT" && capturedPoint != null) ||
                                                (geometryType == "LINE" && linePoints.size >= 2)
                                        ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                ) {

                    Button(
                        onClick = {

                            geometrySaved = true
                            locationError = false
                            isDrawingMode = false
                            isSidePanelOpen = true
                        }
                    ) {
                        Text("Save Geometry")
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    // 🔘 Floating Button
                    FloatingActionButton(
                        onClick = { mapexpanded = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 105.dp, end = 10.dp)
                            .size(43.dp),
                        containerColor = androidx.compose.ui.graphics.Color.White,
                        contentColor = androidx.compose.ui.graphics.Color.Black
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Map Type"
                        )
                    }

                    if (mapexpanded) {
                        androidx.compose.ui.window.Dialog(
                            onDismissRequest = { mapexpanded = false }
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

                                        IconButton(onClick = { mapexpanded = false }) {
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
                                        mapexpanded = false
                                    }

                                    MapTypeItem(
                                        title = "Satellite",
                                        imageRes = R.drawable.satellite
                                    ) {
                                        viewModel.setMapType(MapType.SATELLITE)
                                        mapexpanded = false
                                    }

                                    MapTypeItem(
                                        title = "Terrain",
                                        imageRes = R.drawable.terrain
                                    ) {
                                        viewModel.setMapType(MapType.TERRAIN)
                                        mapexpanded = false
                                    }

                                    MapTypeItem(
                                        title = "Hybrid",
                                        imageRes = R.drawable.hybrid
                                    ) {
                                        viewModel.setMapType(MapType.HYBRID)
                                        mapexpanded = false
                                    }
                                }
                            }
                        }
                    }
                }

                val panelWidth by animateDpAsState(
                    targetValue = if (selectedAsset != null && isSidePanelOpen) 300.dp else 0.dp,
                    label = "side_panel"
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                ) {

                    Card(
                        modifier = Modifier
                            .width(panelWidth)
                            .fillMaxHeight()
                            .align(Alignment.CenterEnd),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            bottomStart = 16.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {

                        if (selectedAsset != null && isSidePanelOpen) {

                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {

                                // Scrollable Content
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState())
                                ) {

                                    AssetHeader(
                                        assetName,
                                        assetCode,
                                        assetCategory,
                                        geometryType,
                                        imageurl,
                                        fieldcount,
                                        status,
                                        imageLoader
                                    )

                                    AssetStepper(currentStep = currentStep)
                                    when (currentStep) {
                                        1 -> {
                                            AssetIdentityCard(
                                                assetName = assetName,
                                                onAssetNameChange = {
                                                    assetName = it
                                                    assetNameError = false
                                                },
                                                assetDescription = assetDescription,
                                                onAssetDescriptionChange = {
                                                    assetDescription = it
                                                },
                                                assetNameError = assetNameError,
                                            )

                                            StatusDateCard(
                                                selectedStatus = selectedStatus,
                                                onStatusSelected = {
                                                    selectedStatus = it
                                                },
                                                installDate = installDate,
                                                onInstallDateChange = { installDate = it }
                                            )
                                        }

                                        2 -> {
                                            LocationStepContent(
                                                geometryType = geometryType,
                                                geometrySaved = geometrySaved,
                                                capturedPoint = capturedPoint,
                                                linePoints = linePoints,
                                                onStartDrawing = {

                                                    isDrawingMode = true
                                                    isSidePanelOpen = false
                                                },
                                                onReplace = {

                                                    geometrySaved = false

                                                    capturedPoint = null
                                                    linePoints.clear()

                                                    isDrawingMode = true
                                                    isSidePanelOpen = false
                                                },
                                                onRemove = {

                                                    geometrySaved = false

                                                    capturedPoint = null
                                                    linePoints.clear()
                                                },
                                                locationError = locationError
                                            )
                                        }

                                        3 -> {
                                            AttributesCard(
                                                fields = assetConfig?.data?.currentVersion?.schema?.fields
                                                    ?: emptyList(),
                                                values = attributeValues,
                                                dropdownSnapshots = dropdownSnapshots,
                                                fieldErrors = fieldErrors
                                            )
                                        }
                                    }

                                }

                                // Bottom Buttons
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp, 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {

                                    OutlinedButton(
                                        onClick = {

                                            if (currentStep == 1) {

                                                clearAssetForm()

                                            } else {

                                                currentStep--
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                    ) {
                                        Text(
                                            if (currentStep == 1) "Cancel" else "Back",
                                            fontSize = 12.sp
                                        )
                                    }

                                    Button(
                                        onClick = {

                                            when (currentStep) {

                                                1 -> {

                                                    if (assetName.isBlank()) {

                                                        assetNameError = true

                                                    } else {

                                                        assetNameError = false
                                                        currentStep = 2
                                                    }
                                                }

                                                2 -> {

                                                    val isLocationValid =
                                                        when (geometryType) {

                                                            "POINT" -> capturedPoint != null

                                                            "LINE" -> linePoints.size >= 2

                                                            else -> false
                                                        }

                                                    if (!isLocationValid) {

                                                        locationError = true

                                                    } else {

                                                        locationError = false
                                                        currentStep = 3
                                                    }
                                                }

                                                3 -> {

                                                    fieldErrors.clear()

                                                    var hasError = false

                                                    assetConfig?.data?.currentVersion?.schema?.fields?.forEach { field ->

                                                        if (field.required) {

                                                            val value = attributeValues[field.id]

                                                            val isEmpty =
                                                                value == null ||
                                                                        value.toString().isBlank()

                                                            if (isEmpty) {

                                                                fieldErrors[field.id] = true
                                                                hasError = true
                                                            }
                                                        }
                                                    }

                                                    if (hasError) {
                                                        return@Button
                                                    }


                                                    val wkt =
                                                        if (geometryType == "POINT") {

                                                            "POINT (${capturedPoint!!.longitude} ${capturedPoint!!.latitude})"

                                                        } else {

                                                            "LINESTRING (" +
                                                                    linePoints.joinToString(",") {
                                                                        "${it.longitude} ${it.latitude}"
                                                                    } +
                                                                    ")"
                                                        }

                                                    val request = CreateAddAssetRequest(
                                                        regionId = 0,
                                                        ParentAssetId = parentAssetTypeId,
                                                        projectId = selectedProject!!.projectId,
                                                        assetTypeId = selectedAsset!!.data.assetTypeId!!,
                                                        assetCode = assetCode,
                                                        assetName = assetName,
                                                        description = assetDescription,
                                                        status = selectedStatus.uppercase(),
                                                        installedOn = installDate.ifBlank { null },
                                                        fields = emptyList(),
                                                        dynamicFields = DynamicFieldsRequest(
                                                            data = attributeValues.toMap(),
                                                            dropdownSnapshot = dropdownSnapshots
                                                        ),

                                                        geometry = GeometryRequest(
                                                            wkt = wkt
                                                        ),
                                                    )

                                                    viewModel.saveAddAsset(
                                                        request = request
                                                    ) { assetId ->

                                                        val dynamicRequest =
                                                            UpdateDynamicFieldsRequest(
                                                                data = attributeValues.toMap(),
                                                                dropdownSnapshot = dropdownSnapshots
                                                            )

                                                        viewModel.updateDynamicFields(
                                                            assetId = assetId,
                                                            request = dynamicRequest
                                                        )

                                                        selectedProject?.projectId?.let { projectId ->
                                                            viewModel.loadCreatedAssets(projectId)
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                    ) {
                                        Text(
                                            if (currentStep == 3) "Save" else "Next",
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (selectedAsset != null) {

                        Surface(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = (-20).dp),
                            shape = CircleShape,
                            shadowElevation = 8.dp,
                            color = Color.White
                        ) {

                            IconButton(
                                onClick = {
                                    isSidePanelOpen = !isSidePanelOpen
                                }
                            ) {
                                Icon(
                                    imageVector =
                                        if (isSidePanelOpen)
                                            Icons.Default.KeyboardArrowRight
                                        else
                                            Icons.Default.KeyboardArrowLeft,
                                    contentDescription = null,
                                    tint = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
        if (showTerminationDialog) {

//            viewModel.loadcustomers()

            AddTerminationDialog(
                locationAssets = selectedLocationAssets,
//                customers = customers,
                fmsUtilization = fmsUtilization,
                fiberCoreStructure = fibercoreStructure,
                fiberCoreUtilization = fibercoreUtilization,
                onFmsSelected = { assetId ->
                    viewModel.loadFMSUtilization(assetId)
                },
                onFiberSelected = { assetId ->
                    viewModel.loadFiberCoreStructure(assetId)
                    viewModel.loadFiberCoreUtilization(assetId)
                },
                onClose = {
                    viewModel.clearFMSandFiberUtilization()
                    showTerminationDialog = false
                },
                onCreateTermination = { json ->
                    json.put("createdBy", userEmail)

                    Log.d(
                        "TERMINATION_JSON",
                        json.toString(4)
                    )

                  viewModel.createTermination(json)
                }
            )
        }
        if (showAssetDialog) {

            AlertDialog(
                onDismissRequest = {
                    showAssetDialog = false
                },
                title = {
                    Text("Select Asset")
                },
                text = {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    ) {
                        if (isAssetLoading) {

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {

                                CircularProgressIndicator()
                            }

                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.fillMaxSize()
                            ) {

                                items(allassetDetails) { asset ->

                                    Column(
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .clickable {
                                                assetDescription = ""
                                                selectedStatus = "Planned"
                                                installDate = ""

                                                capturedPoint = null
                                                linePoints.clear()

                                                geometrySaved = false
                                                isDrawingMode = false

                                                assetNameError = false
                                                locationError = false

                                                currentStep = 1

                                                selectedAsset = asset

                                                val rule = connectivityRules.firstOrNull {
                                                    it.childAssetTypeId == asset.data.assetTypeId
                                                }
                                                parentAssetTypeId = rule?.parentAssetTypeId

                                                showAssetDialog = false
                                                asset.data.assetTypeId?.let { assetTypeId ->
                                                    viewModel.loadAssetConfig(assetTypeId)
                                                }
                                                isSidePanelOpen = true
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {

                                        AsyncImage(
                                            model = base_URL + (asset.data.imageUrl ?: ""),
                                            imageLoader = imageLoader,
                                            contentDescription = null,
                                            modifier = Modifier.size(70.dp),
                                            onSuccess = {
                                                Log.d("IMAGE", "Loaded")
                                            },
                                            onError = {
                                                Log.e("IMAGE", "Failed", it.result.throwable)
                                            }
                                        )

                                        Spacer(
                                            modifier = Modifier.height(4.dp)
                                        )

                                        Text(
                                            text = asset.data.assetName,
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(
                        onClick = {
                            showAssetDialog = false
                        }
                    ) {
                        Text("Close")
                    }
                }
            )
        }
    }
    if (showLocationAssetsDialog) {

        AlertDialog(
            onDismissRequest = {
                showLocationAssetsDialog = false
            },
            title = {
                Text("Assets at this location")
            },
            text = {

                LazyColumn {

                    items(sameLocationAssets) { asset ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {

                                    showLocationAssetsDialog = false

                                    viewModel.loadAssetDetails(
                                        asset.assetTypeId
                                    )

                                    viewModel.loadCreatedAssetDetails(
                                        asset.assetId
                                    )

                                    selectedMarkerAsset = asset

                                    markerAssetDialog = true
                                }
                        ) {

                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "Asset ID : ${asset.assetId}"
                                )
                                Text(
                                    text = "Asset Name : ${asset.data?.get("assetName")?.asString}"
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {

                TextButton(
                    onClick = {
                        showLocationAssetsDialog = false
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    if (
        markerAssetDialog &&
        selectedMarkerAsset != null &&
        createdAssetDetail != null
    ) {

        Dialog(
            onDismissRequest = {
                markerAssetDialog = false
            }
        ) {

            AssetDetailsDialog(
                asset = selectedMarkerAsset!!,
                createdAssetDetail = createdAssetDetail!!,
                imageurl = imageurl,
                assetDetailGeometryType = assetDetailGeometryType,
                imageLoader = imageLoader,
                onAddTermination = {

                    val currentAsset = selectedMarkerAsset

                    if (currentAsset != null) {

                        val wkt = currentAsset.data
                            ?.get("wkt")
                            ?.asString
                            ?.trim()

                        if (!wkt.isNullOrBlank()) {

                            val coords = wkt
                                .substringAfter("(")
                                .substringBefore(")")
                                .trim()
                                .split(Regex("\\s+"))

                            if (coords.size >= 2) {

                                val locationKey = LocationKey(
                                    lat = truncateTo5Decimals(
                                        coords[1].toDouble()
                                    ),
                                    lng = truncateTo5Decimals(
                                        coords[0].toDouble()
                                    )
                                )

                                val sameLocationAssets =
                                    assetsByLocation[locationKey] ?: emptyList()

                                selectedLocationAssets = sameLocationAssets

                                Log.d(
                                    "TERMINATION_DEBUG",
                                    "Assets Found = ${sameLocationAssets.size}"
                                )
                            }
                        }
                    }
                    markerAssetDialog = false
                    showTerminationDialog = true
                },
                onCreateSpliceClosure = {

                },
                onClose = {
                    markerAssetDialog = false
                },
                onExpPdf = {
                    viewModel.exportPdf(
                        assetId = selectedMarkerAsset!!.assetId,
                        context = context
                    )
                },
                onModify = {

                },
                onPortHealthApply = { json, onSuccess ->
                    viewModel.updatePortHealthStatus(
                        json,
                        selectedMarkerAsset!!.assetId,
                        onSuccess = {
                            onSuccess()
                    })

                }
            )
        }
    }
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    enabled = true,
                    indication = null,
                    interactionSource = remember {
                        MutableInteractionSource()
                    }
                ) { },
            contentAlignment = Alignment.Center
        ){
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                CircularProgressIndicator()

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = loadingMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            }
        }
    }
}

fun truncateTo6Decimals(value: Double): Double {
    return floor(value * 1_000_000) / 1_000_000
}

fun truncateTo5Decimals(value: Double): Double {
    return floor(value * 100_000) / 100_000
}

@Composable
fun AssetDetailsDialog(
    asset: CreatedAssetData,
    createdAssetDetail: CreatedAssetDetailsData,
    imageurl: String,
    assetDetailGeometryType: String,
    imageLoader: ImageLoader,
    onAddTermination: () -> Unit,
    onCreateSpliceClosure: () -> Unit,
    onClose: () -> Unit,
    onExpPdf: () -> Unit,
    onModify: () -> Unit,
    onPortHealthApply: (JSONObject,() -> Unit) -> Unit,
) {

    var selectedTab by remember { mutableStateOf(AssetDetailTab.DETAILS) }
    val hasPortUtilization = createdAssetDetail.portUtilization.isNotEmpty()
    val hasCoreUtilization = createdAssetDetail.coreUtilization.isNotEmpty()
    val showTabs = hasPortUtilization || hasCoreUtilization
    val tabs = buildList {
        add(AssetDetailTab.DETAILS)
        if (hasPortUtilization) {
            add(AssetDetailTab.PORTS)
            add(AssetDetailTab.HEALTH_STATUS)
            add(AssetDetailTab.CUSTOMER_MAPPING)
        }
        if (hasCoreUtilization) {
            add(AssetDetailTab.CORES)
        }
    }
    var selectedPortIds by remember {
        mutableStateOf(setOf<Int>())
    }

    var selectedHealthStatus by remember {
        mutableStateOf("")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f),
        shape = RoundedCornerShape(24.dp)
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                        ){
                            AsyncImage(
                                model = imageurl,
                                imageLoader = imageLoader,
                                contentDescription = null,
                                modifier = Modifier.size(70.dp),
                                onSuccess = {
                                    Log.d("IMAGE", "Loaded")
                                },
                                onError = {
                                    Log.e("IMAGE", "Failed", it.result.throwable)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {

                            Text(
                                text = asset.data?.get("assetName")?.asString ?: "-",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )

                            Text(
                                text = asset.assetCode,
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(
                            onClick = onClose
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.horizontalScroll(
                            rememberScrollState()
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    assetDetailGeometryType,
                                    fontSize = 10.sp
                                )
                            }
                        )

                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    "#${asset.assetId}",
                                    fontSize = 10.sp
                                )
                            }
                        )
                    }
                }

            if (showTabs) {

                ScrollableTabRow(
                    selectedTabIndex = tabs.indexOf(selectedTab),
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    tabs.forEach { tab ->

                        Tab(
                            selected = selectedTab == tab,
                            onClick = {
                                selectedTab = tab
                            },
                            modifier = Modifier.height(40.dp),
                            text = {

                                Text(
                                    text = when (tab) {

                                        AssetDetailTab.DETAILS ->
                                            "Details"

                                        AssetDetailTab.PORTS ->
                                            "Ports"

                                        AssetDetailTab.HEALTH_STATUS ->
                                            "Health Status"

                                        AssetDetailTab.CUSTOMER_MAPPING ->
                                            "Customer Mapping"

                                        AssetDetailTab.CORES ->
                                            "Cores"
                                    },
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }


                    Box(
                        modifier = Modifier.weight(1f)
                    ) {

                    when (selectedTab) {

                        AssetDetailTab.DETAILS -> {

                            // SCROLLABLE CONTENT
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                            ) {
                                // ASSET DETAILS CARD
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp)
                                ) {

                                    Text(
                                        text = "ASSET DETAILS",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )

                                    Column(
                                        modifier = Modifier.padding(start = 16.dp)
                                    ) {

                                        Spacer(
                                            modifier = Modifier.height(12.dp)
                                        )

                                        Text(
                                            "Asset ID   : ${asset.assetId}",
                                            fontSize = 12.sp
                                        )

                                        Text(
                                            "Asset Name     : ${asset.data?.get("assetName")?.asString ?: "-"}",
                                            fontSize = 12.sp
                                        )

                                        Text(
                                            "Asset Code     : ${asset.assetCode}",
                                            fontSize = 12.sp
                                        )

                                        //                        Text(
                                        //                            "Description    : ${asset.asset?.description ?: "-"}",
                                        //                            fontSize = 12.sp
                                        //                        )

                                        Text(
                                            "Asset Parent   : ${createdAssetDetail.parentAssetId ?: "-"}",
                                            fontSize = 12.sp
                                        )

                                        //                        Text(
                                        //                            "Status : ${asset.asset?.status ?: "-"}",
                                        //                            fontSize = 12.sp
                                        //                        )

                                        Text(
                                            "Geometry Type   : $assetDetailGeometryType",
                                            fontSize = 12.sp
                                        )

                                        Text(
                                            "WKT    : ${asset.data?.get("wkt")?.asString ?: "-"}",
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Spacer(
                                    modifier = Modifier.height(12.dp)
                                )

                                // FIELD VALUES CARD

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {

                                    Text(
                                        text = "FIELD VALUES",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )

                                    Column(
                                        modifier = Modifier.padding(start = 16.dp)
                                    ) {

                                        Spacer(
                                            modifier = Modifier.height(12.dp)
                                        )

                                        createdAssetDetail.data?.forEach { (key, value) ->

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement =
                                                    Arrangement.SpaceBetween
                                            ) {

                                                Text(
                                                    text = key,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 12.sp
                                                )

                                                Text(
                                                    text = value.toString()
                                                        .replace("\"", ""),
                                                    fontSize = 12.sp
                                                )
                                            }

                                            HorizontalDivider()
                                        }
                                    }
                                }
                            }

                        }

                        AssetDetailTab.PORTS -> {

                            // SCROLLABLE CONTENT
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                            ) {
                                PortsTabContent(
                                    ports = createdAssetDetail.portUtilization
                                )
                            }
                        }

                        AssetDetailTab.HEALTH_STATUS -> {

                            HealthStatusTabContent(
                                ports = createdAssetDetail.portUtilization,
                                selectedPortIds = selectedPortIds,
                                onPortToggle = { portId ->

                                    selectedPortIds =
                                        if (selectedPortIds.contains(portId)) {
                                            selectedPortIds - portId
                                        } else {
                                            selectedPortIds + portId
                                        }
                                },
                                onSelectAll = {

                                    selectedPortIds =
                                        if (selectedPortIds.size ==
                                            createdAssetDetail.portUtilization.size
                                        ) {
                                            emptySet()
                                        } else {
                                            createdAssetDetail.portUtilization
                                                .mapNotNull { it.portId }
                                                .toSet()
                                        }
                                },
                                selectedHealthStatus = selectedHealthStatus,
                                onHealthStatusChange = {
                                    selectedHealthStatus = it
                                },
                                onPortHealthApply = {

                                    val requestBody = JSONObject().apply {
                                        put("portIds", JSONArray(selectedPortIds.toList()))
                                        put("healthStatus", selectedHealthStatus)
                                    }

                                    onPortHealthApply(requestBody){
                                        selectedPortIds = emptySet()
                                        selectedHealthStatus = ""
                                    }

                                }
                            )
                        }

                        AssetDetailTab.CUSTOMER_MAPPING -> {

                            Text(
                                text = "Customer Mapping",
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        AssetDetailTab.CORES -> {

                            Text(
                                text = "Cores Screen",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

            // FIXED BUTTONS AT BOTTOM

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(
                        rememberScrollState()
                    )
                    .padding(16.dp),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                when (asset.assetCode) {

                    "FMS" -> {

                        Button(
                            onClick = onAddTermination
                        ) {
                            Text("Add Termination")
                        }
                    }

                    "SPLICE_CLOSURE" -> {

                        Button(
                            onClick = {
                                // Create Splice Closure logic
                            }
                        ) {
                            Text("Create Splice Closure")
                        }
                    }
                }
//                OutlinedButton(
//                    onClick = onClose,
////                    modifier = Modifier.weight(1f)
//                ) {
//                    Text("Close")
//                }

                Button(
                    onClick = onExpPdf,
//                    modifier = Modifier.weight(1f)
                ) {
                    Text("Export PDF")
                }

                Button(
                    onClick = onModify,
                    enabled = false,
//                    modifier = Modifier.weight(1f)
                ) {
                    Text("Modify")
                }
            }
        }
    }
}


@Composable
fun PortsTabContent(
    ports: List<PortUtilization>
) {

    if (ports.isEmpty()) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No Port Data Available")
        }

        return
    }

    Text(
        text = "PORT UTILIZATION",
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier = Modifier.padding(16.dp)
    )

    val horizontalScrollState =
        rememberScrollState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {

        Column {
            HorizontalDivider()

            // HEADER

            Row(
                modifier = Modifier
                    .horizontalScroll(horizontalScrollState)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {

                TableCell(
                    text = "PORT NO",
                    width = 120.dp,
                    isHeader = true
                )

                TableCell(
                    text = "STATUS",
                    width = 120.dp,
                    isHeader = true
                )

                TableCell(
                    text = "ATTACHED ASSET",
                    width = 140.dp,
                    isHeader = true
                )

                TableCell(
                    text = "CONNECTED CORE",
                    width = 140.dp,
                    isHeader = true
                )

                TableCell(
                    text = "TYPE",
                    width = 80.dp,
                    isHeader = true
                )
            }

            HorizontalDivider()

            // DATA

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {

                items(ports) { port ->

                    Row(
                        modifier = Modifier
                            .horizontalScroll(
                                horizontalScrollState
                            )
                    ) {

                        TableCell(
                            text = "Port ${port.portNo ?: "-"}",
                            width = 120.dp
                        )

                        TableCell(
                            text = port.status ?: "-",
                            width = 120.dp
                        )

                        TableCell(
                            text = "-",
                            width = 140.dp
                        )

                        TableCell(
                            text = port.pairedCoreId?.toString()
                                ?: "-",
                            width = 140.dp
                        )

                        TableCell(
                            text = "-",
                            width = 80.dp
                        )
                    }

                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthStatusTabContent(
    ports: List<PortUtilization>,
    selectedPortIds: Set<Int>,
    onPortToggle: (Int) -> Unit,
    onSelectAll: () -> Unit,
    selectedHealthStatus: String,
    onHealthStatusChange: (String) -> Unit,
    onPortHealthApply: () -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }
    val healthOptions = listOf(
        "Select health status",
        "LIVE",
        "FAULTY",
        "UNKNOWN"
    )

    if (ports.isEmpty()) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No Port Health Data Available")
        }

        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        Column {
            Text(
                text = "PORT HEALTH STATUS",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Checkbox(
                    checked =
                        selectedPortIds.size == ports.size,
                    onCheckedChange = {
                        onSelectAll()
                    }
                )

                Text("Select All")

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                Text(
                    "${selectedPortIds.size} of ${ports.size} selected"
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ports) { port ->

                    PortHealthCard(
                        port = port,
                        isSelected =
                            selectedPortIds.contains(
                                port.portId ?: -1
                            ),
                        onToggle = {
                            port.portId?.let {
                                onPortToggle(it)
                            }
                        }
                    )
                }
            }



            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 8.dp,
                        end = 8.dp,
                        top = 8.dp,
                        bottom = 0.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = {
                        expanded = !expanded
                    },
                    modifier = Modifier.weight(1f)
                ) {

                    OutlinedTextField(
                        value = selectedHealthStatus,
                        onValueChange = {},
                        readOnly = true,
                        placeholder = {
                            Text("Select health status", fontSize = 13.sp)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expanded
                            )
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .height(50.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {
                            expanded = false
                        }
                    ) {

                        healthOptions.forEach { status ->

                            DropdownMenuItem(
                                text = {
                                    Text(status)
                                },
                                onClick = {

                                    onHealthStatusChange(status)

                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(
                    modifier = Modifier.width(8.dp)
                )
                Button(
                    onClick = onPortHealthApply,
                    enabled =
                        selectedPortIds.isNotEmpty() &&
                                selectedHealthStatus.isNotBlank() && selectedHealthStatus != "Select health status"
                ) {
                    Text("Apply")
                }
            }
        }
    }
}


@Composable
fun PortHealthCard(
    port: PortUtilization,
    isSelected: Boolean,
    onToggle: () -> Unit
) {

    val utilizationStatus = port.utilizationStatus ?: "UNKNOWN"
    val healthStatus = port.healthStatus ?: "UNKNOWN"

    val utilizationBg =
        if (utilizationStatus.equals("FREE", true))
            Color(0xFFE8F5E9)
        else
            Color(0xFFF3E5F5)

    val utilizationText =
        if (utilizationStatus.equals("FREE", true))
            Color(0xFF2E7D32)
        else
            Color(0xFF8E24AA)

    val healthDotColor = when (healthStatus.uppercase()) {
        "LIVE" -> Color(0xFF22C55E)
        "FAULTY" -> Color(0xFFEF4444)
        else -> Color(0xFF94A3B8)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            Color(0xFFE2E8F0)
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Port ${port.portNo}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                Checkbox(
                    checked = isSelected,
                    onCheckedChange = {
                        onToggle()
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Box(
                modifier = Modifier
                    .background(
                        utilizationBg,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    )
            ) {
                Text(
                    text = utilizationStatus,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = utilizationText
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            healthDotColor,
                            CircleShape
                        )
                )

                Spacer(
                    modifier = Modifier.width(6.dp)
                )

                Text(
                    text = healthStatus.replaceFirstChar {
                        it.uppercase()
                    },
                    fontSize = 11.sp,
                    color = Color(0xFF475569)
                )
            }
        }
    }
}


@Composable
fun TableCell(
    text: String,
    width: Dp,
    isHeader: Boolean = false
) {
    Text(
        text = text,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        fontSize = if (isHeader) 12.sp else 11.sp,
        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
        maxLines = 1
    )
}

@Composable
fun RowScope.DataCell(
    text: String,
    columnWeight: Float
) {

    Text(
        text = text,
        modifier = Modifier
            .weight(columnWeight)
            .padding(horizontal = 8.dp),
        fontSize = 12.sp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDropdown(
    selectedValue: String,
    placeholder: String,
    items: List<String>,
    onValueSelected: (String) -> Unit
) {

    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {

        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            placeholder = {
                Text(placeholder)
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,

                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,

                focusedBorderColor = Color.Gray,
                unfocusedBorderColor = Color.LightGray,

                focusedLabelColor = Color.Black,
                unfocusedLabelColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(14.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onValueSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AssetHeader(
    assetName: String,
    assetCode: String,
    assetCategory: String,
    geometryType: String,
    imageurl: String,
    fieldcount: Int,
    status: String,
    imageLoader : ImageLoader
) {

    Column(
        modifier = Modifier.padding(16.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(32.dp)
            ){
                AsyncImage(
                    model = imageurl,
                    imageLoader = imageLoader,
                    contentDescription = null,
                    modifier = Modifier.size(70.dp),
                    onSuccess = {
                        Log.d("IMAGE", "Loaded")
                    },
                    onError = {
                        Log.e("IMAGE", "Failed", it.result.throwable)
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {

                Text(
                    "CREATE ASSET",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Text(
                    assetName,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp
                )

                Text(
                    assetCode,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            AssistChip(
                onClick = {},
                label = { Text(assetCategory,fontSize = 10.sp,color = Color.Black) }
            )

            AssistChip(
                onClick = {},
                label = { Text(geometryType,fontSize = 10.sp,color = Color.Black) }
            )

            AssistChip(
                onClick = {},
                label = { Text(status,fontSize = 10.sp,color = Color.Black) }
            )

            AssistChip(
                onClick = {},
                label = { Text("$fieldcount Fields",fontSize = 10.sp,color = Color.Black) }
            )
        }
    }
}

@Composable
private fun AssetStepper(currentStep: Int) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 0.dp, 16.dp, 16.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {

        StepItem(
            number = 1,
            title = "IDENTITY",
            isSelected = currentStep == 1
        )

        StepItem(
            number = 2,
            title = "LOCATION",
            isSelected = currentStep == 2
        )

        StepItem(
            number = 3,
            title = "ATTRIBUTES",
            isSelected = currentStep == 3
        )
    }

    HorizontalDivider()
}

@Composable
private fun StepItem(
    number: Int,
    title: String,
    isSelected: Boolean
) {

    val color =
        if (isSelected)
            Color(0xFF7C3AED)
        else
            Color.Gray

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    color,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = number.toString(),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = title,
            color = color,
            fontSize = 11.sp,
            fontWeight =
                if (isSelected)
                    FontWeight.Bold
                else
                    FontWeight.Normal
        )
    }
}

@Composable
private fun AssetIdentityCard(
    assetName: String,
    onAssetNameChange: (String) -> Unit,
    assetDescription: String,
    onAssetDescriptionChange: (String) -> Unit,
    assetNameError: Boolean
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                "ASSET IDENTITY",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("ASSET NAME", fontSize = 12.sp)

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = assetName,
                onValueChange = onAssetNameChange,
                isError = assetNameError,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            )
            if (assetNameError) {
                Text(
                    text = "Asset Name is required",
                    color = Color.Red,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("ASSET DESCRIPTION", fontSize = 12.sp)

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = assetDescription,
                onValueChange = onAssetDescriptionChange,
                placeholder = {
                    Text("Enter Asset Description")
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusDateCard(
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
    installDate: String,
    onInstallDateChange: (String) -> Unit
) {

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                "STATUS & DATE",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("STATUS", fontSize = 12.sp)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                listOf(
                    "Planned",
                    "Installed",
                    "Retired"
                ).forEach { status ->

                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = {
                            onStatusSelected(status)
                        },
                        label = {
                            Text(status)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("INSTALLED ON", fontSize = 12.sp)

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showDatePicker = true
                    }
            ) {
                OutlinedTextField(
                    value = installDate,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    placeholder = {
                        Text("Select install date...")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )
            }
        }
    }
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onInstallDateChange(
                                SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.getDefault()
                                ).format(Date(millis))
                            )
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun LocationStepContent(
    geometryType: String,
    geometrySaved: Boolean,
    capturedPoint: LatLng?,
    linePoints: List<LatLng>,
    onStartDrawing: () -> Unit,
    onReplace: () -> Unit,
    onRemove: () -> Unit,
    locationError: Boolean
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = "GEOMETRY — $geometryType",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!geometrySaved) {

                Button(
                    onClick = onStartDrawing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (geometryType == "LINE")
                            "Start Drawing Line On Map"
                        else
                            "Start Marking Point On Map"
                    )
                }
            } else {

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE3F2FD)
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {

                        Text(
                            "WKT CAPTURED",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            when (geometryType) {

                                "POINT" ->
                                    "POINT(${capturedPoint?.longitude} ${capturedPoint?.latitude})"

                                else ->
                                    "LINESTRING(${linePoints.joinToString { "${it.longitude} ${it.latitude}" }})"
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    OutlinedButton(
                        onClick = onReplace,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Replace Geometry")
                    }

                    OutlinedButton(
                        onClick = onRemove,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Remove")
                    }
                }
            }
            if (locationError) {
                Text(
                    text = if (geometryType == "POINT")
                        "Please mark a point on map"
                    else
                        "Please mark at least 2 points on map",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun AttributesCard(
    fields: List<DynamicField>,
    values: MutableMap<String, Any?>,
    dropdownSnapshots: MutableMap<String, DropdownSnapshot>,
    fieldErrors: Map<String, Boolean>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                "ATTRIBUTES",
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            fields.forEach { field ->

                DynamicFieldItem(
                    field = field,
                    values = values,
                    dropdownSnapshots = dropdownSnapshots,
                    fieldErrors = fieldErrors
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTerminationDialog(
    locationAssets: List<CreatedAssetData>,
//    customers: List<CustomerResponse>,
    fmsUtilization: List<FmsPortUtilizationResponse>,
    fiberCoreStructure: FiberStructureResponse?,
    fiberCoreUtilization: List<FiberCoreUtilizationResponse>,
    onFmsSelected: (Int) -> Unit,
    onFiberSelected: (Int) -> Unit,
    onClose: () -> Unit,
    onCreateTermination: (JSONObject) -> Unit
) {
    val siteAssets = remember(locationAssets) {
        locationAssets.filter { it.data?.get("assetName")?.asString?.equals("SITE", true) == true }
    }
    val fmsAssets = remember(locationAssets) {
        locationAssets.filter { it.assetCode.equals("FMS", true) }
    }
    val fibreAssets = remember(locationAssets) {
        locationAssets.filter { it.data?.get("assetName")?.asString?.contains("FIBRE", true) == true }
    }
    var selectedSite by remember { mutableStateOf(siteAssets.firstOrNull()) }
    var selectedFms by remember { mutableStateOf<CreatedAssetData?>(null) }
    var selectedCable by remember { mutableStateOf<CreatedAssetData?>(null) }
    var fmsExpanded by remember { mutableStateOf(false) }
    var cableExpanded by remember { mutableStateOf(false) }
    val siteDirections = listOf("Inbound", "Outbound")
    var selectedDirection by remember { mutableStateOf("Inbound") }
    var directionExpanded by remember { mutableStateOf(false) }
    var customerExpanded by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<CustomerResponse?>(null) }
    val selectedPorts = remember { mutableStateListOf<Int>() }
    val selectedCores = remember { mutableStateListOf<Int>() }

    val selectedCount = selectedPorts.size
    val occupiedCount = fmsUtilization.count {
        it.utilizationStatus.equals("UTILIZED", true)
    }
    val faultyCount = fmsUtilization.count {
        it.healthStatus.equals("FAULTY", true)
    }
    val liveCount = fmsUtilization.count {
        it.healthStatus.equals("LIVE", true)
    }
    val availableCount = fmsUtilization.count {
        !it.utilizationStatus.equals("UTILIZED", true) &&
                !it.healthStatus.equals("FAULTY", true)
    }
    val coreMap = remember(fiberCoreStructure) {

        fiberCoreStructure?.tubes
            ?.flatMap { tube ->

                tube.cores.orEmpty().map { core ->

                    core.coreId to Triple(
                        tube.tubeNo ?: 0,
                        core.coreNo ?: 0,
                        core
                    )
                }
            }
            ?.toMap()
            ?: emptyMap()
    }
    val portMap = remember(fmsUtilization) {

        fmsUtilization.associateBy {
            it.portId
        }
    }
    val pairCount = maxOf(
        selectedPorts.size,
        selectedCores.size
    )
    val isPairingValid =
        selectedPorts.isNotEmpty() &&
                selectedCores.isNotEmpty() &&
                selectedPorts.size == selectedCores.size



    var notes by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onClose
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text = "Create Termination",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = onClose
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Site Dropdown
                    OutlinedTextField(
                        value = selectedSite?.let {
                            "${it.data?.get("assetName")?.asString} (${it.assetId})"
                        } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text("Site")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    // FMS Dropdown
                    ExposedDropdownMenuBox(
                        expanded = fmsExpanded,
                        onExpandedChange = {
                            fmsExpanded = !fmsExpanded
                        }
                    ) {

                        OutlinedTextField(
                            value = selectedFms?.let {
                                "${it.data?.get("assetName")?.asString} (${it.assetId})"
                            } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text("FMS Asset")
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = fmsExpanded
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = fmsExpanded,
                            onDismissRequest = {
                                fmsExpanded = false
                            }
                        ) {

                            fmsAssets.forEach { asset ->

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "${asset.data?.get("assetName")?.asString} (${asset.assetId})"
                                        )
                                    },
                                    onClick = {

                                        selectedFms = asset
                                        fmsExpanded = false

                                        onFmsSelected(asset.assetId)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    // FIber Cable Dropdown
                    ExposedDropdownMenuBox(
                        expanded = cableExpanded,
                        onExpandedChange = {
                            cableExpanded = !cableExpanded
                        }
                    ) {

                        OutlinedTextField(
                            value = selectedCable?.let {
                                "${it.data?.get("assetName")?.asString} (${it.assetId})"
                            } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text("Fibre Cable")
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = cableExpanded
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = cableExpanded,
                            onDismissRequest = {
                                cableExpanded = false
                            }
                        ) {

                            fibreAssets.forEach { asset ->

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "${asset.data?.get("assetName")?.asString} (${asset.assetId})"
                                        )
                                    },
                                    onClick = {

                                        selectedCable = asset
                                        cableExpanded = false

                                        onFiberSelected(asset.assetId)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    // Site Direction Dropdown

                    ExposedDropdownMenuBox(
                        expanded = directionExpanded,
                        onExpandedChange = {
                            directionExpanded = !directionExpanded
                        }
                    ) {

                        OutlinedTextField(
                            value = selectedDirection,
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text("Site Direction")
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = directionExpanded
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = directionExpanded,
                            onDismissRequest = {
                                directionExpanded = false
                            }
                        ) {

                            siteDirections.forEach { direction ->

                                DropdownMenuItem(
                                    text = {
                                        Text(direction)
                                    },
                                    onClick = {
                                        selectedDirection = direction
                                        directionExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

//                    // Customer Dropdown
//
//                    ExposedDropdownMenuBox(
//                        expanded = customerExpanded,
//                        onExpandedChange = {
//                            customerExpanded = !customerExpanded
//                        }
//                    ) {
//
//                        OutlinedTextField(
//                            value = selectedCustomer?.customerName ?: "",
//                            onValueChange = {},
//                            readOnly = true,
//                            label = { Text("Customer") },
//                            trailingIcon = {
//                                ExposedDropdownMenuDefaults.TrailingIcon(
//                                    expanded = customerExpanded
//                                )
//                            },
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .menuAnchor()
//                        )
//
//                        ExposedDropdownMenu(
//                            expanded = customerExpanded,
//                            onDismissRequest = {
//                                customerExpanded = false
//                            }
//                        ) {
//
//                            customers.forEach { customer ->
//
//                                DropdownMenuItem(
//                                    text = {
//                                        Text(customer.customerName)
//                                    },
//                                    onClick = {
//                                        selectedCustomer = customer
//                                        customerExpanded = false
//                                    }
//                                )
//                            }
//                        }
//                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "FMS Ports (${fmsUtilization.size} Units)",
                                    fontWeight = FontWeight.Bold
                                )

                                if (fmsUtilization.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {

                                        StatusChip(
                                            label = "Selected",
                                            count = selectedCount,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        StatusChip(
                                            label = "Available",
                                            count = availableCount,
                                            color = Color.Black
                                        )

                                        StatusChip(
                                            label = "Occupied",
                                            count = occupiedCount,
                                            color = Color.Gray
                                        )

                                        StatusChip(
                                            label = "Live",
                                            count = liveCount,
                                            color = Color(0xFF00C853)
                                        )

                                        StatusChip(
                                            label = "Faulty",
                                            count = faultyCount,
                                            color = Color.Red
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (fmsUtilization.isEmpty()) {

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {

                                        Text(
                                            text = "Select an FMS asset to view available ports",
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(6),
                                        modifier = Modifier.height(220.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {

                                        items(
                                            fmsUtilization,
                                            key = { it.portId ?: it.portNo ?: 0 }
                                        ) { port ->

                                            val utilizationStatus =
                                                port.utilizationStatus?.uppercase() ?: ""

                                            val healthStatus =
                                                port.healthStatus?.uppercase() ?: ""

                                            val isUtilized =
                                                utilizationStatus == "UTILIZED"

                                            val isFaulty =
                                                healthStatus == "FAULTY"

                                            val isLive =
                                                healthStatus == "LIVE"

                                            val isSelectable =
                                                !isUtilized && !isFaulty

                                            val portId = port.portId ?: 0

                                            val isSelected =
                                                selectedPorts.contains(portId)

                                            val selectionNumber =
                                                selectedPorts.indexOf(portId) + 1

                                            Card(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clickable(
                                                        enabled = isSelectable
                                                    ) {

                                                        if (selectedPorts.contains(portId)) {
                                                            selectedPorts.remove(portId)
                                                        } else {
                                                            selectedPorts.add(portId)
                                                        }
                                                    },
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isSelected)
                                                        MaterialTheme.colorScheme.primary
                                                    else
                                                        Color.Gray.copy(alpha = 0.4f)
                                                ),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = when {
                                                        isSelected -> MaterialTheme.colorScheme.primary
                                                        isUtilized -> Color.LightGray
                                                        isFaulty -> Color.LightGray
                                                        else -> Color.White
                                                    }
                                                )
                                            ) {

                                                Box(
                                                    modifier = Modifier.fillMaxSize()
                                                ) {

                                                    // Selection Number Badge
                                                    if (selectionNumber > 0) {

                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .size(16.dp)
                                                                .background(
                                                                    MaterialTheme.colorScheme.primary,
                                                                    CircleShape
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {

                                                            Text(
                                                                text = selectionNumber.toString(),
                                                                color = Color.White,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }

                                                    // Status Dot
                                                    else if (isLive || isFaulty) {

                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .padding(1.dp)
                                                                .size(14.dp)
                                                                .background(
                                                                    if (isLive)
                                                                        Color(0xFF00C853)
                                                                    else
                                                                        Color.Red,
                                                                    CircleShape
                                                                )
                                                                .border(
                                                                    1.dp,
                                                                    Color.White,
                                                                    CircleShape
                                                                )
                                                        )
                                                    }

                                                    Text(
                                                        text = port.portNo?.toString() ?: "-",
                                                        modifier = Modifier.align(Alignment.Center),
                                                        color =
                                                            if (isSelected)
                                                                Color.White
                                                            else
                                                                Color.Black,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f)
                        ) {

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {

                                Text(
                                    text = "Fibre Cores",
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                if (fiberCoreStructure == null ||
                                    fiberCoreStructure.tubes?.isEmpty() == true
                                ) {

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {

                                        Text(
                                            text = "Select a fibre cable first",
                                            color = Color.Gray
                                        )
                                    }

                                } else {

                                    Column(
                                        modifier = Modifier
                                            .heightIn(max = 400.dp)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {

                                        fiberCoreStructure.tubes?.forEach { tube ->

                                            TubeCard(
                                                tube = tube,
                                                selectedCores = selectedCores
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {

                            Text(
                                text = "Pairing Preview",
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            if (pairCount == 0) {

                                Text(
                                    text = "Select FMS ports and Fibre cores",
                                    color = Color.Gray
                                )

                            } else {

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {

                                    if (!isPairingValid &&
                                        (selectedPorts.isNotEmpty() || selectedCores.isNotEmpty())
                                    ) {
                                        Text(
                                            text = when {

                                                selectedPorts.size > selectedCores.size ->
                                                    "Please select ${selectedPorts.size - selectedCores.size} more fibre core(s)"

                                                selectedCores.size > selectedPorts.size ->
                                                    "Please select ${selectedCores.size - selectedPorts.size} more FMS port(s)"

                                                else ->
                                                    "Ports and Fibre cores count must match"
                                            },
                                            color = Color.Red,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    repeat(pairCount) { index ->

                                        val portId =
                                            selectedPorts.getOrNull(index)

                                        val port =
                                            portId?.let {
                                                portMap[it]
                                            }

                                        val coreId =
                                            selectedCores.getOrNull(index)

                                        val coreInfo =
                                            coreId?.let {
                                                coreMap[it]
                                            }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement =
                                                Arrangement.SpaceBetween
                                        ) {

                                            Text(
                                                text =
                                                    port?.portNo?.let {
                                                        "Port $it"
                                                    } ?: "-"
                                            )

                                            Text("↔")

                                            Text(
                                                text =
                                                    if (coreInfo != null)
                                                        "T${coreInfo.first}/C${coreInfo.second}"
                                                    else
                                                        "-"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = {
                            notes = it
                        },
                        label = {
                            Text("Notes")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Button(
                    onClick = {

                        val json = JSONObject().apply {

                            put("siteAssetId", selectedSite?.assetId)
                            put("fmsAssetId", selectedFms?.assetId)
                            put("fiberAssetId", selectedCable?.assetId)
                            put("confirmAttachToExistingCircuit", true)
                            put("siteDirection", selectedDirection.uppercase())
                            put("notes", notes)
                            put(
                                "fmsPortIds",
                                JSONArray(selectedPorts)
                            )
                            put(
                                "fiberCoreIds",
                                JSONArray(selectedCores)
                            )
                        }

                        onCreateTermination(json)
                    },
                    enabled = isPairingValid,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Termination")
                }
            }
        }

    }
}


fun String.toComposeColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color.LightGray
    }
}

@Composable
fun StatusChip(
    label: String,
    count: Int,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color)
    ) {
        Text(
            text = "$label ($count)",
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 6.dp
            ),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
fun TubeCard(
    tube: FiberTubeStructure,
    selectedCores: SnapshotStateList<Int>
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Color.LightGray,
                RoundedCornerShape(12.dp)
            )
            .padding(10.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            tube.hexCode?.toComposeColor()?.let {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            it,
                            CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Tube ${tube.tubeNo}",
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier.heightIn(max = 300.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {

            items(
                tube.cores ?: emptyList(),
                key = { it.coreId ?: 0 }
            ) { core ->

                val coreId = core.coreId ?: 0
                val isFree =
                    core.status.equals("FREE", true)
                val isSelected =
                    selectedCores.contains(coreId)

                val selectionNumber =
                    selectedCores.indexOf(coreId) + 1

                Box {

                    core.hexCode?.toComposeColor()?.let { coreColor ->

                        Card(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable(
                                    enabled = isFree
                                ) {

                                    if (isSelected) {
                                        selectedCores.remove(coreId)
                                    } else {
                                        selectedCores.add(coreId)
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isSelected ->
                                        MaterialTheme.colorScheme.primary

                                    isFree ->
                                        coreColor

                                    else ->
                                        Color.LightGray
                                }
                            ),
                            border = BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                when {
                                    isSelected ->
                                        MaterialTheme.colorScheme.primary

                                    isFree ->
                                        Color.Gray

                                    else ->
                                        Color.DarkGray
                                }
                            )
                        ) {

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {

                                Text(
                                    text = core.coreNo?.toString() ?: "-",
                                    color =
                                        if (
                                            core.hexCode.equals(
                                                "#FFFFFF",
                                                true
                                            )
                                        )
                                            Color.Black
                                        else
                                            Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (selectionNumber > 0) {

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(1.dp)
                                .size(14.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                text = selectionNumber.toString(),
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicFieldItem(
    field: DynamicField,
    values: MutableMap<String, Any?>,
    dropdownSnapshots: MutableMap<String, DropdownSnapshot>,
    fieldErrors: Map<String, Boolean>
) {

    Text(
        text = field.label,
        fontWeight = FontWeight.Medium
    )

    Spacer(modifier = Modifier.height(4.dp))

    when (field.type.lowercase()) {

        "dropdown" -> {

            var expanded by remember {
                mutableStateOf(false)
            }

            val selectedValue =
                values[field.id]?.toString() ?: ""

            val selectedLabel =
                field.options.find {
                    it.value == selectedValue
                }?.label ?: ""

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = {
                    expanded = !expanded
                }
            ) {

                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text(field.label)
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = expanded
                        )
                    },
                    isError = fieldErrors[field.id] == true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                    }
                ) {

                    field.options.forEach { option ->

                        DropdownMenuItem(
                            text = {
                                Text(option.label)
                            },
                            onClick = {

                                values[field.id] = option.value
                                dropdownSnapshots[field.id] = DropdownSnapshot(value = option.value, label = option.label)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        "text" -> {

            OutlinedTextField(
                value = values[field.id]?.toString() ?: "",
                onValueChange = {
                    values[field.id] = it
                },
                label = {
                    Text(field.label)
                },
                placeholder = {
                    Text(field.placeholder ?: "")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        "email" -> {

            OutlinedTextField(
                value = values[field.id]?.toString() ?: "",
                onValueChange = {
                    values[field.id] = it
                },
                label = {
                    Text(field.label)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        "phone" -> {

            OutlinedTextField(
                value = values[field.id]?.toString() ?: "",
                onValueChange = {
                    values[field.id] = it
                },
                label = {
                    Text(field.label)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        "url" -> {

            OutlinedTextField(
                value = values[field.id]?.toString() ?: "",
                onValueChange = {
                    values[field.id] = it
                },
                label = {
                    Text(field.label)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        "password" -> {

            var visible by remember {
                mutableStateOf(false)
            }

            OutlinedTextField(
                value = values[field.id]?.toString() ?: "",
                onValueChange = {
                    values[field.id] = it
                },
                label = {
                    Text(field.label)
                },
                singleLine = true,
                visualTransformation =
                    if (visible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                trailingIcon = {

                    IconButton(
                        onClick = {
                            visible = !visible
                        }
                    ) {

                        Icon(
                            imageVector =
                                if (visible)
                                    Icons.Default.Visibility
                                else
                                    Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        "integer" -> {

            OutlinedTextField(
                value = values[field.id]?.toString() ?: "",
                onValueChange = {

                    if (it.all(Char::isDigit)) {
                        values[field.id] = it
                    }
                },
                label = {
                    Text(field.label)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        "decimal" -> {

            OutlinedTextField(
                value = values[field.id]?.toString() ?: "",
                onValueChange = {
                    values[field.id] = it
                },
                label = {
                    Text(field.label)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        "currency" -> {

            OutlinedTextField(
                value = values[field.id]?.toString() ?: "",
                onValueChange = {
                    values[field.id] = it
                },
                label = {
                    Text(field.label)
                },
                leadingIcon = {
                    Text("₹")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        "percentage" -> {

            OutlinedTextField(
                value = values[field.id]?.toString() ?: "",
                onValueChange = {
                    values[field.id] = it
                },
                label = {
                    Text(field.label)
                },
                trailingIcon = {
                    Text("%")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        "textarea" -> {

            OutlinedTextField(
                value = values[field.id]?.toString() ?: "",
                onValueChange = {
                    values[field.id] = it
                },
                label = {
                    Text(field.label)
                },
                minLines = 4,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )
        }
        "richtext" -> {

            OutlinedTextField(
                value = values[field.id]?.toString() ?: "",
                onValueChange = {
                    values[field.id] = it
                },
                label = {
                    Text(field.label)
                },
                minLines = 6,
                modifier = Modifier.fillMaxWidth()
            )
        }
        "tags" -> {

            OutlinedTextField(
                value = values[field.id]?.toString() ?: "",
                onValueChange = {
                    values[field.id] = it
                },
                placeholder = {
                    Text("tag1, tag2, tag3")
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        "rating" -> {

            var rating by remember {
                mutableIntStateOf(
                    (values[field.id] as? Int) ?: 0
                )
            }

            Row {

                (1..5).forEach { star ->

                    IconButton(
                        onClick = {

                            rating = star
                            values[field.id] = star
                        }
                    ) {

                        Icon(
                            imageVector =
                                if (star <= rating)
                                    Icons.Default.Star
                                else
                                    Icons.Default.StarBorder,
                            contentDescription = null
                        )
                    }
                }
            }
        }

        "slider" -> {

            var sliderValue by remember {
                mutableFloatStateOf(
                    (values[field.id] as? Float) ?: 0f
                )
            }

            Slider(
                value = sliderValue,
                onValueChange = {

                    sliderValue = it
                    values[field.id] = it
                }
            )

            Text(sliderValue.toInt().toString())
        }
        "radio" -> {

            Column {

                field.options.forEach { option ->

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        RadioButton(
                            selected =
                                values[field.id] == option.value,
                            onClick = {
                                values[field.id] = option.value
                            }
                        )

                        Text(option.label)
                    }
                }
            }
        }
        "toggle" -> {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Switch(
                    checked = values[field.id] as? Boolean ?: false,
                    onCheckedChange = {
                        values[field.id] = it
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(field.label)
            }
        }
        "hidden" -> {

            values[field.id] =
                values[field.id]
                    ?: field.defaultValue
        }

//        // TEXT
//        "text",
//        "email",
//        "url",
//        "password",
//        "phone",
//        "currency",
//        "percentage",
//        "computed",
//        "hidden" -> {
//
//            OutlinedTextField(
//                value = values[field.id]?.toString() ?: "",
//                onValueChange = {
//                    values[field.id] = it
//                },
//                modifier = Modifier.fillMaxWidth()
//            )
//        }
//
//        // TEXTAREA
//        "textarea",
//        "richtext" -> {
//
//            OutlinedTextField(
//                value = values[field.id]?.toString() ?: "",
//                onValueChange = {
//                    values[field.id] = it
//                },
//                modifier = Modifier.fillMaxWidth(),
//                minLines = 4
//            )
//        }
//
//        // INTEGER
//        "integer" -> {
//
//            OutlinedTextField(
//                value = values[field.id]?.toString() ?: "",
//                onValueChange = {
//                    values[field.id] = it
//                },
//                keyboardOptions = KeyboardOptions(
//                    keyboardType = KeyboardType.Number
//                ),
//                modifier = Modifier.fillMaxWidth()
//            )
//        }
//
//        // DECIMAL
//        "decimal" -> {
//
//            OutlinedTextField(
//                value = values[field.id]?.toString() ?: "",
//                onValueChange = {
//                    values[field.id] = it
//                },
//                keyboardOptions = KeyboardOptions(
//                    keyboardType = KeyboardType.Decimal
//                ),
//                modifier = Modifier.fillMaxWidth()
//            )
//        }
//
//        // CHECKBOX
//        "checkbox" -> {
//
//            Row(
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//
//                Checkbox(
//                    checked = values[field.id] as? Boolean ?: false,
//                    onCheckedChange = {
//                        values[field.id] = it
//                    }
//                )
//
//                Text(field.label)
//            }
//        }
//
//        // TOGGLE
//        "toggle" -> {
//
//            Row(
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//
//                Switch(
//                    checked = values[field.id] as? Boolean ?: false,
//                    onCheckedChange = {
//                        values[field.id] = it
//                    }
//                )
//
//                Spacer(modifier = Modifier.width(8.dp))
//
//                Text(field.label)
//            }
//        }
//
//        // DROPDOWN
//        "dropdown" -> {
//
//            var expanded by remember {
//                mutableStateOf(false)
//            }
//
//            OutlinedTextField(
//                value = values[field.id]?.toString() ?: "",
//                onValueChange = {},
//                readOnly = true,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .clickable {
//                        expanded = true
//                    }
//            )
//
//            DropdownMenu(
//                expanded = expanded,
//                onDismissRequest = {
//                    expanded = false
//                }
//            ) {
//
//                field.options.forEach { option ->
//
//                    DropdownMenuItem(
//                        text = {
//                            Text(option.label)
//                        },
//                        onClick = {
//
//                            values[field.id] = option.value
//                            expanded = false
//                        }
//                    )
//                }
//            }
//        }
//
//        // RADIO
//        "radio" -> {
//
//            Column {
//
//                field.options.forEach { option ->
//
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//
//                        RadioButton(
//                            selected =
//                                values[field.id] == option.value,
//                            onClick = {
//                                values[field.id] = option.value
//                            }
//                        )
//
//                        Text(option.label)
//                    }
//                }
//            }
//        }
//
//        // DATE
//        "date",
//        "datetime",
//        "time",
//        "daterange" -> {
//
//            OutlinedTextField(
//                value = values[field.id]?.toString() ?: "",
//                onValueChange = {
//                    values[field.id] = it
//                },
//                modifier = Modifier.fillMaxWidth(),
//                placeholder = {
//                    Text(field.type.uppercase())
//                }
//            )
//        }
//
//        // TAGS
//        "tags" -> {
//
//            OutlinedTextField(
//                value = values[field.id]?.toString() ?: "",
//                onValueChange = {
//                    values[field.id] = it
//                },
//                placeholder = {
//                    Text("tag1, tag2, tag3")
//                },
//                modifier = Modifier.fillMaxWidth()
//            )
//        }
//
//        // RATING
//        "rating" -> {
//
//            var rating by remember {
//                mutableIntStateOf(
//                    (values[field.id] as? Int) ?: 0
//                )
//            }
//
//            Row {
//
//                (1..5).forEach { star ->
//
//                    IconButton(
//                        onClick = {
//
//                            rating = star
//                            values[field.id] = star
//                        }
//                    ) {
//
//                        Icon(
//                            imageVector =
//                                if (star <= rating)
//                                    Icons.Default.Star
//                                else
//                                    Icons.Default.StarBorder,
//                            contentDescription = null
//                        )
//                    }
//                }
//            }
//        }
//
//        // SLIDER
//        "slider" -> {
//
//            var sliderValue by remember {
//                mutableFloatStateOf(
//                    (values[field.id] as? Float) ?: 0f
//                )
//            }
//
//            Slider(
//                value = sliderValue,
//                onValueChange = {
//
//                    sliderValue = it
//                    values[field.id] = it
//                }
//            )
//
//            Text(sliderValue.toInt().toString())
//        }
//
//        // COLOR
//        "color" -> {
//
//            OutlinedTextField(
//                value = values[field.id]?.toString() ?: "",
//                onValueChange = {
//                    values[field.id] = it
//                },
//                placeholder = {
//                    Text("#FFFFFF")
//                },
//                modifier = Modifier.fillMaxWidth()
//            )
//        }
//
//        // FILE TYPES
//        "file",
//        "image",
//        "document",
//        "video" -> {
//
//            Button(
//                onClick = {
//                    // file picker later
//                }
//            ) {
//                Text("Select ${field.type}")
//            }
//
//            values[field.id]?.let {
//                Text(it.toString())
//            }
//        }
//
//        // MAP
//        "map" -> {
//
//            Card(
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text(
//                    text = "Map Field",
//                    modifier = Modifier.padding(16.dp)
//                )
//            }
//        }

        else -> {

            Text(
                text = "Unsupported Type : ${field.type}",
                color = Color.Red
            )
        }
    }
    if (fieldErrors[field.id] == true) {
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${field.label} is required",
            color = Color.Red,
            fontSize = 11.sp
        )
    }
}

fun createLabeledSquareMarker(
    text: String,
    squareColor: Int,
    squareSize: Int,
    textSizeSp: Float,
    textColor: Int,
    context: Context
): Bitmap {

    val density = context.resources.displayMetrics

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            textSizeSp,
            density
        )
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val textBounds = Rect()
    textPaint.getTextBounds(text, 0, text.length, textBounds)

    val textWidth = textPaint.measureText(text)
    val textHeight = textBounds.height().toFloat()

    val horizontalPadding = 20
    val verticalPadding = 10

    val bitmapWidth = (textWidth + horizontalPadding * 2).toInt()
    val bitmapHeight = (textHeight + verticalPadding + squareSize + verticalPadding).toInt()

    val bitmap = Bitmap.createBitmap(
        bitmapWidth,
        bitmapHeight,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(bitmap)

    // Draw TEXT (top)
    val textX = bitmapWidth / 2f
    val textY = textHeight + verticalPadding
    canvas.drawText(text, textX, textY, textPaint)

    // Draw RED SQUARE (below text)
    val squarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = squareColor
    }

    val squareLeft = (bitmapWidth - squareSize) / 2f
    val squareTop = textY + verticalPadding
    val squareRight = squareLeft + squareSize
    val squareBottom = squareTop + squareSize

    canvas.drawRect(squareLeft, squareTop, squareRight, squareBottom, squarePaint)

    return bitmap
}

enum class AssetDetailTab {
    DETAILS,
    PORTS,
    HEALTH_STATUS,
    CUSTOMER_MAPPING,
    CORES
}