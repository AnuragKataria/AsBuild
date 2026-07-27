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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.style.TextAlign
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.compose.ui.window.Dialog

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun InventoryMapScreen(
    onBackClick: () -> Unit,
    viewModel: InventoryMapViewModel
) {

    val context = LocalContext.current
    val base_URL = "https://webgis.rbt-ltd.com/api"
    val cameraPositionState = rememberCameraPositionState()

    val projects by viewModel.projects.collectAsState()
    val connectivityRules by viewModel.connectivityRules.collectAsState()
    val createdAssets by viewModel.createdAssets.collectAsState()
    val assetDetails by viewModel.assetDetails.collectAsState()
    val assetConfig by viewModel.assetConfig.collectAsState()
    val saveMessage by viewModel.saveMessage.collectAsState()
    val canvasFields by viewModel.canvasFields.collectAsState()

    var selectedProject by remember { mutableStateOf<ProjectResponse?>(null) }
    var selectedAsset by remember { mutableStateOf<AssetDetailResponse?>(null) }
    var parentAssetTypeId by remember { mutableStateOf<Int?>(null) }
    var isFilterExpanded by remember {
        mutableStateOf(false)
    }

    var isSidePanelOpen by remember { mutableStateOf(false) }

    val projectNames = projects.map { it.projectName }

    val isAddAssetEnabled = selectedProject != null

    var showAssetDialog by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.collectAsState()
    val loadingMessage by viewModel.isLoadingmessage.collectAsState()
    val isAssetLoading by viewModel.isAssetLoading.collectAsState()

    val createdAssetMarkers = remember { mutableStateListOf<Pair<LatLng, CreatedAssetData>>() }
    val createdAssetPolylines = remember { mutableStateListOf<Pair<List<LatLng>, CreatedAssetData>>() }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var assetName by remember { mutableStateOf("") }
    var assetCode by remember { mutableStateOf("") }
    var assetCategory by remember { mutableStateOf("") }
    var geometryType by remember { mutableStateOf("") }
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
        viewModel.loadProjects()
        viewModel.loadConnectivityRules()
    }

    LaunchedEffect(assetConfig) {
        fieldcount = assetConfig?.data?.currentVersion?.schema?.fields?.size ?: 0
    }

    LaunchedEffect(selectedAsset) {
        selectedAsset?.data?.let { asset ->
            assetName = asset.assetName
            assetCode = asset.assetCode
            assetCategory = asset.assetCategory
            geometryType = asset.geometryType
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

        Log.d(
            "ASSETS_COUNT",
            createdAssets.size.toString()
        )
    }

    LaunchedEffect(createdAssets) {

        createdAssets.forEach { asset ->

            val wkt = asset.assetGeometry?.wkt ?: return@forEach

            when {

                wkt.startsWith("POINT", true) -> {

                    val coords = wkt
                        .substringAfter("(")
                        .substringBefore(")")
                        .trim()
                        .split(" ")

                    if (coords.size >= 2) {

                        createdAssetMarkers.add(
                            Pair(
                                LatLng(
                                    coords[1].toDouble(),
                                    coords[0].toDouble()
                                ),
                                asset
                            )
                        )
                    }
                }

                wkt.startsWith("LINESTRING", true) -> {

                    val points = wkt
                        .substringAfter("(")
                        .substringBefore(")")
                        .split(",")

                    val latLngs = points.mapNotNull { point ->

                        val coords = point.trim().split(" ")

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
                        createdAssetPolylines.add(
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

                    createdAssetMarkers.forEach { (point, asset) ->

                        val bitmap = remember(asset.assetId) {

                            createLabeledSquareMarker(
                                text = asset.assetId.toString(),
                                squareColor = android.graphics.Color.RED,
                                squareSize = 25,
                                textSizeSp = 10f,
                                textColor = android.graphics.Color.BLACK,
                                context = context
                            )
                        }

                        Marker(
                            state = rememberMarkerState(
                                position = point
                            ),
                            icon = BitmapDescriptorFactory.fromBitmap(bitmap),
                            onClick = {
                                selectedMarkerAsset = asset
                                markerAssetDialog = true
                                true
                            }
                        )
                    }

                    createdAssetPolylines.forEach { (line, asset) ->

                        Polyline(
                            points = line,
                            width = 8f,
                            color = Color.Blue,
                            clickable = true,
                            onClick = {
                                selectedMarkerAsset = asset
                                markerAssetDialog = true
                            }
                        )
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
                                        fieldcount,
                                        status
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

                                items(assetDetails) { asset ->

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
                                            model =
                                                base_URL + (asset.data.imageUrl ?: ""),
                                            contentDescription = null,
                                            modifier = Modifier.size(70.dp)
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
    if (
        markerAssetDialog &&
        selectedMarkerAsset != null
    ) {

        Dialog(
            onDismissRequest = {
                markerAssetDialog = false
            }
        ) {

            AssetDetailsDialog(
                asset = selectedMarkerAsset!!,
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
            )
        }
    }
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize()
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

@Composable
fun AssetDetailsDialog(
    asset: CreatedAssetData,
    onClose: () -> Unit,
    onExpPdf: () -> Unit,
    onModify: () -> Unit
) {

    val geometryType =
        asset.assetGeometry?.wkt
            ?.substringBefore("(")
            ?.uppercase()
            ?: ""

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
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    Color(0xFFE8F5E9),
                                    CircleShape
                                )
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {

                            Text(
                                text = asset.asset?.assetName ?: "-",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )

                            Text(
                                text = asset.assetCode,
                                color = Color.Gray,
                                fontSize = 14.sp
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
                                    asset.asset?.status ?: "-",
                                    fontSize = 10.sp
                                )
                            }
                        )

                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    geometryType,
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

                // SCROLLABLE CONTENT
                Column(
                    modifier = Modifier
                        .weight(1f)
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
                            "Asset Name     : ${asset.asset?.assetName ?: "-"}",
                            fontSize = 12.sp
                        )

                        Text(
                            "Asset Code     : ${asset.assetCode}",
                            fontSize = 12.sp
                        )

                        Text(
                            "Description    : ${asset.asset?.description ?: "-"}",
                            fontSize = 12.sp
                        )

                        Text(
                            "Asset Parent   : ${asset.parentAssetId ?: "-"}",
                            fontSize = 12.sp
                        )

                        Text(
                            "Status : ${asset.asset?.status ?: "-"}",
                            fontSize = 12.sp
                        )

                        Text(
                            "Geometry Type   : $geometryType",
                            fontSize = 12.sp
                        )

                        Text(
                            "WKT : ${asset.assetGeometry?.wkt ?: "-"}",
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

                        asset.data?.forEach { (key, value) ->

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

                Spacer(
                    modifier = Modifier.height(16.dp)
                )
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

                OutlinedButton(
                    onClick = onClose,
//                    modifier = Modifier.weight(1f)
                ) {
                    Text("Close")
                }

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
    fieldcount: Int,
    status: String,
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
                    .background(
                        Color(0xFFE8F5E9),
                        CircleShape
                    )
            )

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
                label = { Text(assetCategory,fontSize = 10.sp) }
            )

            AssistChip(
                onClick = {},
                label = { Text(geometryType,fontSize = 10.sp) }
            )

            AssistChip(
                onClick = {},
                label = { Text(status,fontSize = 10.sp) }
            )

            AssistChip(
                onClick = {},
                label = { Text("$fieldcount Fields",fontSize = 10.sp) }
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